package com.spx.muticamera.video;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.SystemClock;
import android.util.Log;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MutiFileMuxer {
    private static final String TAG = "flutter_MutiFileMuxer";
    private static final int Max_File_Count = 6;
    private static final int Max_File_Duration = 10 * 1000;
    List<MediaMuxerWrapper> muxerList = new ArrayList<>();
    List<RecordFile> recordResults = new ArrayList<>();

    int muxelIndex = 0;
    MediaMuxerWrapper currentMuxer;
    int count = 0;
    boolean secondMuxerHasData = false;

    volatile boolean switchToNextFileRequested = false;

    public MutiFileMuxer(String path, int muxerOutputMpeg4) {
        for (int i = 0; i < Max_File_Count; i++) {
            try {
                String newPath = "";
                if (i == 0) {
                    newPath = path;
                } else {
                    if (path.endsWith(".mp4")) {
                        newPath = path.substring(0, path.length() - 4) + "_" + i + ".mp4";
                    } else {
                        throw new RuntimeException("录制文件格式不正确:" + path);
                    }
                }
                Log.i(TAG, "MutiFileMuxer[" + i + "]: path:" + newPath);
                MediaMuxer muxer = new MediaMuxer(newPath, muxerOutputMpeg4);
                MediaMuxerWrapper muxerWrapper = new MediaMuxerWrapper();
                muxerWrapper.muxer = muxer;
                muxerWrapper.path = newPath;
                muxerList.add(muxerWrapper);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        currentMuxer = muxerList.get(0);
        onNewResult();
    }

    private void onNewResult() {
        for (int i = 0; i < recordResults.size(); i++) {
            RecordFile recordFile = recordResults.get(i);
            if (recordFile.videoPath.equals(currentMuxer.path)) {
                return;
            }
        }
        RecordFile recordFile = new RecordFile();
        recordFile.videoPath = currentMuxer.path;
        recordFile.index = recordResults.size();
        recordResults.add(recordFile);
    }

    private void updateFileDuration() {
        for (int i = 0; i < recordResults.size(); i++) {
            RecordFile recordFile = recordResults.get(i);
            if (recordFile.videoPath.equals(currentMuxer.path)) {
                recordFile.videoTime = SystemClock.uptimeMillis() - currentMuxer.startTimeMs;
                return;
            }
        }
    }

    public int addVideoTrack(MediaFormat newFormat) {
        return addTrack(newFormat, true);
    }

    public int addAudioTrack(MediaFormat newFormat) {
        return addTrack(newFormat, false);
    }

    public int addTrack(MediaFormat newFormat, boolean isVideoTrack) {
        for (int i = 0; i < muxerList.size(); i++) {
            MediaMuxerWrapper muxer = muxerList.get(i);
            int trackIndex = muxer.muxer.addTrack(newFormat);
            if (isVideoTrack) {
                muxer.videoTrackIndex = trackIndex;
            } else {
                muxer.audioTrackIndex = trackIndex;
            }
        }
        if (isVideoTrack) {
            return currentMuxer.videoTrackIndex;
        } else {
            return currentMuxer.audioTrackIndex;
        }
    }

    public void start() {
        for (int i = 0; i < muxerList.size(); i++) {
            MediaMuxer muxer = muxerList.get(i).muxer;
            muxer.start();
        }
    }

    public void stop() {
        updateFileDuration();
        for (int i = 0; i < muxerList.size(); i++) {
            MediaMuxer muxer = muxerList.get(i).muxer;
            if (i == 0 || secondMuxerHasData) {
                muxer.stop();
            }
        }
    }

    public void release() {
        for (int i = 0; i < muxerList.size(); i++) {
            MediaMuxer muxer = muxerList.get(i).muxer;
            if (i == 0 || secondMuxerHasData) {
                muxer.release();
            }
        }
    }

    public void switchToNextFile(){
        switchToNextFileRequested = true;
    }

    public synchronized void writeSampleData(int trackIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo, MediaCodec.BufferInfo videoBuffInfo, boolean videoData) {
        if (currentMuxer.startTimeMs == 0) {
            currentMuxer.startTimeMs = SystemClock.uptimeMillis();
        }
        int index = muxelIndex;
        if (videoData) {
            boolean isKeyFrame = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
            if (isKeyFrame) {
                Log.i(TAG, "writeSampleData:" + (isKeyFrame ? "关键帧" : "非关键帧"));
            }
            if (switchToNextFileRequested && isKeyFrame) {
                Log.i(TAG, "writeSampleData: 切换muxer!");

                muxelIndex++;
                count = 0;
                if (muxelIndex >= muxerList.size()) {
                    muxelIndex--;
                } else {
                    // 为当前要被切换出去的文件补足这个关键帧
                    currentMuxer.muxer.writeSampleData(trackIndex, encodedData, bufferInfo);
                }
                updateFileDuration();
                currentMuxer = muxerList.get(muxelIndex);
                switchToNextFileRequested = false;
                onNewResult();
            }
        }
        if (!videoData) {
            bufferInfo.presentationTimeUs -= 400 * 1000;
            if (bufferInfo.presentationTimeUs < 100 * 1000) {
                bufferInfo.presentationTimeUs = 100 * 1000;
            }
//      bufferInfo.presentationTimeUs = videoBuffInfo.presentationTimeUs;
        }
//    if (videoData) {
//      Log.i(TAG, "writeSampleData 视频:  "+videoData+" trackIndex:" + trackIndex + ",flags:" + bufferInfo.flags + ",vts:" + videoBuffInfo.presentationTimeUs + ",presentationTimeUs:" + bufferInfo.presentationTimeUs);
//    } else {
//      Log.i(TAG, "writeSampleData 音频: "+videoData+" trackIndex:" + trackIndex + ",flags:" + bufferInfo.flags + ",vts:" + videoBuffInfo.presentationTimeUs + ",presentationTimeUs:" + bufferInfo.presentationTimeUs);
//    }
        currentMuxer.muxer.writeSampleData(trackIndex, encodedData, bufferInfo);
        currentMuxer.hasData = true;

        if (muxelIndex == 1) {
            secondMuxerHasData = true;
        }
    }

    public List<RecordFile> getRecordResults() {
        return recordResults;
    }

    public List<String> getOutputPaths() {
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < muxerList.size(); i++) {
            MediaMuxerWrapper muxerWrapper = muxerList.get(i);
            if (muxerWrapper.hasData) {
                paths.add(muxerWrapper.path);
            } else {
                break;
            }
        }
        Log.i(TAG, "getOutputPaths: paths:" + paths);
        return paths;
    }
}

class MediaMuxerWrapper {
    MediaMuxer muxer;
    String path;
    int videoTrackIndex;
    int audioTrackIndex;
    boolean hasData;
    long startTimeMs;
}