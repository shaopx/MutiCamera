package com.spx.muticamera.util;

import com.spx.muticamera.TmfApplication;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import java.lang.reflect.Method;

public class AppUtil {
    private static final String TAG = "flutter_AppUtil";
    private static int sScreenWidth = -1;
    private static int sScreenHeight = -1;

//    public static void simpleToast(Context context, String message) {
//        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
//    }

    public static int getNotificationBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public static int getScreenWidth(Context context) {
        if (sScreenWidth > 0) {
            return sScreenWidth;
        }
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        sScreenWidth = displayMetrics.widthPixels;
        return sScreenWidth;
    }

    public static int getScreenHeight(Context context) {
        if (sScreenHeight > 0) {
            return sScreenHeight;
        }
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        sScreenHeight = displayMetrics.heightPixels;
        return sScreenHeight;
    }

    public static int dpToPx(Context context, float dp) {
        return (int) (context.getResources().getDisplayMetrics().density * dp);
/*        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics()));*/
    }

    public static int dpToPx(float dp) {
        return (int) (TmfApplication.Companion.getContext().getResources().getDisplayMetrics().density * dp);
/*        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics()));*/
    }

    public static boolean isQualcommChipset() {
        String chipset = getSystemProperty("ro.board.platform");
        Log.i(TAG, "isQualcommChipset: ...chipset:" + chipset);
        return chipset != null && chipset.toLowerCase().contains("qcom");
    }

    private static String getSystemProperty(String propName) {
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method getMethod = systemPropertiesClass.getMethod("get", String.class);
            return (String) getMethod.invoke(null, propName);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isFullScreen(Context context) {
        String deviceName = android.os.Build.MANUFACTURER;
        if ("Xiaomi".equalsIgnoreCase(deviceName)) {
            return isXiaoMiFullScreen(context);
        }
        return android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    public static boolean isXiaoMiFullScreen(Context context) {
        boolean mIsXiaomiFull = false;
        String deviceName = android.os.Build.MANUFACTURER;
        if ("Xiaomi".equalsIgnoreCase(deviceName)) {
            if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                mIsXiaomiFull = Settings.Global.getInt(context.getContentResolver(), "force_fsg_nav_bar", 0) != 0;
            }
        }
        return mIsXiaomiFull;
    }

    public static boolean isHarmonyOS() {
        String osVersion = System.getProperty("os.version");
        if (osVersion == null) {
            return false;
        }
        return osVersion.toLowerCase().contains("harmony");
    }

    public static boolean isHuaweiDevice() {
        String manufacturer = Build.MANUFACTURER;
        if (manufacturer == null) {
            return false;
        }
        return manufacturer.equalsIgnoreCase("HUAWEI");
    }

    public static void debugDeviceInfo() {
        Log.i(TAG, "debugDeviceInfo.......");
        Log.i(TAG, "MODEL:" + Build.MODEL);
        Log.i(TAG, "MANUFACTURER:" + Build.MANUFACTURER);
        Log.i(TAG, "BOARD:" + Build.BOARD);
        Log.i(TAG, "BRAND:" + Build.BRAND);
        Log.i(TAG, "DEVICE:" + Build.DEVICE);
        Log.i(TAG, "DISPLAY:" + Build.DISPLAY);
        Log.i(TAG, "HARDWARE:" + Build.HARDWARE);
        Log.i(TAG, "ID:" + Build.ID);
        Log.i(TAG, "MODEL:" + Build.MODEL);
        Log.i(TAG, "PRODUCT:" + Build.PRODUCT);
        Log.i(TAG, "VERSION:" + Build.VERSION.SDK_INT);
    }

    private static Toast sToast;
    public static void simpleToast(String message) {
        if (sToast != null) {
            sToast.cancel();
        }
        sToast = Toast.makeText(TmfApplication.getContext(), message, Toast.LENGTH_SHORT);
        sToast.show();
    }
}
