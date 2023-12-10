package com.spx.muticamera.util;


import static android.provider.MediaStore.Video.Thumbnails.MINI_KIND;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;

import com.spx.muticamera.TmfApplication;

import java.io.File;

public class FileUtils {

    public static void clearStorageMp4() {
        File file;
        String parent = TmfApplication.getContext().getFilesDir().getAbsolutePath();
        File file1 = new File(parent);
        if (!file1.exists()) {
            file1.mkdir();
        }
        file = new File(parent, "video.mp4");
        if (file.exists()) {
            file.delete();
        }
    }

    public static String getStorageFileName(String s, String fileSuffix) {
        File file;
        String parent = TmfApplication.getContext().getFilesDir().getAbsolutePath();
        File file1 = new File(parent);
        if (!file1.exists()) {
            file1.mkdir();
        }
        file = new File(parent, s + "" + fileSuffix);

        return file.getPath();
    }

    public static String getCoverFileName() {
        String coverFileName = getStorageFileName(String.valueOf(System.currentTimeMillis()), ".jpg");
        return coverFileName;
    }

    public static String getCaptureFileName() {
        return getCoverFileName();
    }

    public static Bitmap getVideoThumbnail(String videoPath, int width, int height) {
        Bitmap bitmap = null;
        // 获取视频的缩略图
        bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, MINI_KIND); //調用ThumbnailUtils類的靜態方法createVideoThumbnail獲取視頻的截圖；
        if (bitmap != null) {
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT);//調用ThumbnailUtils類的靜態方法extractThumbnail將原圖片（即上方截取的圖片）轉化為指定大小；
        }
        return bitmap;
    }

}
