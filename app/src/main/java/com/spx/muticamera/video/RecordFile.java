package com.spx.muticamera.video;


public class RecordFile {
    public int index;
    public String videoPath;
    public String coverPath;
    public long videoTime;

    @Override
    public String toString() {
        return "RecordFile{" +
                "index='" + index + '\'' +
                "videoPath='" + videoPath + '\'' +
                ", coverPath='" + coverPath + '\'' +
                ", videoTime=" + videoTime +
                '}';
    }
}