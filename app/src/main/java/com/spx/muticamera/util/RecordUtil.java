package com.spx.muticamera.util;


import android.os.Build;
import android.util.Log;


public class RecordUtil {

    public static final int MAX_DURATION = 15 * 1000;
    private static final String TAG = "flutter_RecordUtil";

    private static boolean sSingleCameraRequest = false;

    public static void setSingleCameraRequest(boolean request) {
        sSingleCameraRequest = request;
    }

    public static boolean isSupportMutiCameraRecord() {
        if (sSingleCameraRequest) {
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return false;
        }
        if (AppUtil.isHuaweiDevice()) {
            Log.i(TAG, "is huawei device! ");
            return false;
        }
        if (AppUtil.isHarmonyOS()) {
            Log.i(TAG, "is HarmonyOS! ");
            return false;
        }
        return true;
//    return false;
    }
}
