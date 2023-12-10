package com.spx.muticamera

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.hardware.Camera.AutoFocusCallback
import android.opengl.GLSurfaceView
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.spx.muticamera.encode.EncodeCallback
import com.spx.muticamera.filter.SlideGpuFilterGroup
import com.spx.muticamera.util.RecordUtil.isSupportMutiCameraRecord
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class CameraView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) :
    GLSurfaceView(context, attrs), GLSurfaceView.Renderer, OnFrameAvailableListener {
    interface MutiCameraOpenFailCallback {
        fun onMutiCameraOpenFail(ex: Exception?)
    }

    lateinit var mCameraDrawer: CameraDrawer
    private var encodeCallback: EncodeCallback? = null
    private var mSurfaceWidth = 0
    private var mSurfaceHeight = 0
    var mCameraController_back: ICamera? = null
    var mCameraController_front: ICamera? = null

    //    private int dataWidth = 0, dataHeight = 0;
    var cameraId = 0
        private set
    private var autoOpenWhenSurfaceReady = false
    var picture: Bitmap? = null
        private set
    private val uiHandler = Handler()
    private var takePicture = false
    private var enableMutiCameraRecord = false
    var hasSwitched = false
    var isSupportMutiCamera = true
    var isRecording = false;
    private var mutiCameraOpenFailCallback: MutiCameraOpenFailCallback? = null

    init {
        init()
    }

    private fun init() {
        Log.i(TAG, "init: ...")
        // 默认后置摄像头
        cameraId = 0;
        /**初始化OpenGL的相关信息 */
        setEGLContextClientVersion(2) //设置版本
        setRenderer(this) //设置Renderer
        renderMode = RENDERMODE_WHEN_DIRTY //主动调用渲染
        preserveEGLContextOnPause = true //保存Context当pause时
        cameraDistance = 0f //相机距离
        /**初始化Camera的绘制类 */
        mCameraDrawer = CameraDrawer(context, resources)
        /**初始化相机的管理类 */
        mCameraController_back = CameraController2(context as Activity, "back")
        mCameraController_front = CameraController2(context as Activity, "front")
    }

    fun setEncodeCallback(callback: EncodeCallback) {
        this.encodeCallback = callback?.also {
            mCameraDrawer.setEncodeCallback(it)
        }
    }

    fun enableMutiCameraRecord(enable: Boolean) {
        enableMutiCameraRecord = enable
        enableShowMutiCameraTexture(enable)
    }

    fun setMutiCameraOpenFailCallback(callback: MutiCameraOpenFailCallback?) {
        mutiCameraOpenFailCallback = callback
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        Log.i(TAG, "onSurfaceCreated: getWidth():$width, getHeight():$height")
        mSurfaceWidth = 720
        mSurfaceHeight = 1280
        mCameraDrawer.onSurfaceCreated(gl, config)
        if (autoOpenWhenSurfaceReady) {
            open(cameraId, width, height, false)
        }
        Log.i(TAG, "onSurfaceCreated: width:$mSurfaceWidth, height:$mSurfaceHeight")
        mCameraDrawer.setPreviewSize(mSurfaceWidth, mSurfaceHeight)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        Log.i(TAG, "onSurfaceChanged: width:$width, height:$height")
        mCameraDrawer.onSurfaceChanged(gl, width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        super.surfaceDestroyed(holder)
        if (mCameraController_back != null) {
            mCameraController_back!!.close()
            mCameraController_back = null
        }
        if (mCameraController_front != null) {
            mCameraController_front!!.close()
            mCameraController_front = null
        }
    }

    override fun onDrawFrame(gl: GL10) {
        mCameraDrawer?.onDrawFrame(gl)
        if (takePicture) {
            Log.i(TAG, "onDrawFrame: ...takePicture:$takePicture")
            picture = captureGLSurfaceView(gl)
            takePicture = false
            Log.i(TAG, "onDrawFrame: ...screenCap X:" + picture)
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
//        Log.i(TAG, "onFrameAvailable: ...");
        requestRender()
    }

    private fun open(cameraId: Int, width: Int, height: Int, isSwitch: Boolean) {
        Log.i(TAG, "open: ...cameraId:$cameraId")
        mCameraDrawer.setCameraId(cameraId)
        if (mCameraDrawer.mSurfaceTextrue == null) {
            Log.i(TAG, "open: pending. texture not ready!")
            autoOpenWhenSurfaceReady = true
            return;
        }
        val texture: SurfaceTexture = mCameraDrawer.mSurfaceTextrue!!
        val texture_front: SurfaceTexture = mCameraDrawer!!.mSurfaceTextrue_front!!
        _open_camera(mCameraController_back!!, cameraId, texture, isSwitch)
        if (mCameraController_front != null && enableMutiCameraRecord) {
            uiHandler.postDelayed({
                try {
                    _open_camera(mCameraController_front!!, 1 - cameraId, texture_front, isSwitch)
                } catch (ex: Exception) {
                    Log.e(TAG, "打开前置摄像头失败: ", ex)
                    Log.i(TAG, "回退到单摄像头")
                    enableMutiCameraRecord = false
                    isSupportMutiCamera = false
                    if (mutiCameraOpenFailCallback != null) {
                        mutiCameraOpenFailCallback!!.onMutiCameraOpenFail(ex)
                    }
                }
            }, 500)
        }
    }

    //    private void _open_camera(CameraController2 cameraController, int cameraId, SurfaceTexture texture) {
    //        Log.i(TAG, "_open_camera: ...cameraId:"+cameraId);
    //        try {
    //            cameraController.close();
    //            cameraController.open(cameraId);//打开相机
    //            cameraController.setUpCameraOutputs(mSurfaceWidth, mSurfaceHeight);
    //            cameraController.setPreviewTexture(texture);
    //            if (cameraId == 0) {
    //                texture.setOnFrameAvailableListener(this);
    //            }
    //
    //            cameraController.openAndpreview();
    //        } catch (Exception ex) {
    //            Log.e(TAG, "_open_camera(" + cameraId + "):exception", ex);
    //            Log.i(TAG, ex.toString());
    //            ex.printStackTrace();
    //            return;
    //        }
    //    }
    private fun _open_camera(
        cameraController: ICamera,
        cameraId: Int,
        texture: SurfaceTexture,
        isSwitch: Boolean
    ) {
        Log.i(TAG, "_open_camera: cameraId:$cameraId")
        try {
            cameraController.close()
            cameraController.open(cameraId) //打开相机
            cameraController.setUpCameraOutputs(mSurfaceWidth, mSurfaceHeight)
            cameraController.setPreviewTexture(texture)
            if (cameraController === mCameraController_back) {
                texture.setOnFrameAvailableListener(this)
            }
            cameraController.openAndPreview()
        } catch (ex: Exception) {
            Log.e(TAG, "_open_camera($cameraId):exception", ex)
            Log.i(TAG, ex.toString())
            ex.printStackTrace()
            return
        }
    }

    fun trunOnFlash(on: Boolean) {
        if (isSupportMutiCamera) {
            if (hasSwitched) {
                if (mCameraController_front != null) {
                    mCameraController_front!!.trunOnFlash(on)
                }
            } else {
                if (mCameraController_back != null) {
                    mCameraController_back!!.trunOnFlash(on)
                }
            }
        } else {
            if (mCameraController_back != null) {
                mCameraController_back!!.trunOnFlash(on)
            }
        }
    }

    fun startTakePicture() {
        Log.i(TAG, "startTakePicture: ...")
        takePicture = true
    }

    //
    //    private int getCameraDisplayOrientation(int cameraId, int deviceRotation) {
    //        Camera.CameraInfo info = new Camera.CameraInfo();
    //        Camera.getCameraInfo(cameraId, info);
    //        int degrees = 0;
    //        switch (deviceRotation) {
    //            case Surface.ROTATION_0:
    //                degrees = 0;
    //                break;
    //            case Surface.ROTATION_90:
    //                degrees = 90;
    //                break;
    //            case Surface.ROTATION_180:
    //                degrees = 180;
    //                break;
    //            case Surface.ROTATION_270:
    //                degrees = 270;
    //                break;
    //        }
    //
    //        int result;
    //        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
    //            result = (info.orientation + degrees) % 360;
    //            result = (360 - result) % 360;  // Compensate for the mirror effect of the front camera
    //        } else {  // Back-facing camera
    //            result = (info.orientation - degrees + 360) % 360;
    //        }
    //        return result;
    //    }


    override fun onResume() {
        super.onResume()
        if (autoOpenWhenSurfaceReady) {
            open(cameraId, 0, 0, false)
        }
    }

    fun onDestroy() {
        Log.i(TAG, "onDestroy: ...")
        if (mCameraController_back != null) {
            mCameraController_back!!.close()
            mCameraController_back = null
        }
        if (mCameraController_front != null) {
            mCameraController_front!!.close()
            mCameraController_front = null
        }
    }

    val beautyLevel: Int
        get() = mCameraDrawer.beautyLevel

    fun changeBeautyLevel(level: Int) {
        queueEvent { mCameraDrawer!!.changeBeautyLevel(level) }
    }

    fun startRecord() {
//        uiHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                captureBitmap();
//            }
//        }, 2000);
        queueEvent { mCameraDrawer.startRecord() }
        isRecording = true;
    }

    fun captureGLSurfaceView(gl10: GL10): Bitmap {
        // 获取 GLSurfaceView 的宽高
        val width = this.width
        val height = this.height

        // 创建一个 IntBuffer 用于存储像素数据
        val pixelBuffer = IntBuffer.allocate(width * height)

        // 创建一个 Bitmap 对象用于存储截图结果
        val screenshotBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // 设置当前的 OpenGL 上下文为 glSurfaceView 的上下文
        gl10.glReadPixels(0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, pixelBuffer)

        // 将像素数据绘制到 Bitmap
        screenshotBitmap.copyPixelsFromBuffer(pixelBuffer)

        // 翻转 Bitmap
        val matrix = Matrix()
        matrix.setScale(1f, -1f)
        val flippedBitmap = Bitmap.createBitmap(screenshotBitmap, 0, 0, width, height, matrix, true)

        // 计算裁剪后的尺寸和偏移量
        val targetHeight = (width * 16f / 9f).toInt()
        val targetRatio = width.toFloat() / targetHeight
        val bitmapRatio = width.toFloat() / height
        val cropWidth: Int
        val cropHeight: Int
        var offsetX = 0
        var offsetY = 0
        if (bitmapRatio > targetRatio) {
            cropWidth = (height * targetRatio).toInt()
            cropHeight = height
            offsetX = (width - cropWidth) / 2
        } else {
            cropWidth = width
            cropHeight = (width / targetRatio).toInt()
            offsetY = (height - cropHeight) / 2
        }

        // 创建裁剪后的 Bitmap
        val croppedBitmap = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888)

        // 绘制裁剪后的 Bitmap
        val canvas = Canvas(croppedBitmap)
        canvas.drawBitmap(flippedBitmap, offsetX.toFloat(), offsetY.toFloat(), null)

        // 创建最终的结果 Bitmap
        val resultBitmap = Bitmap.createBitmap(
            width,
            targetHeight,
            Bitmap.Config.ARGB_8888
        )

        // 计算居中显示的偏移量
        val resultOffsetX = (width - cropWidth) / 2
        val resultOffsetY = (targetHeight - cropHeight) / 2

        // 绘制裁剪后的 Bitmap 到最终的结果 Bitmap
        val resultCanvas = Canvas(resultBitmap)
        resultCanvas.drawBitmap(
            croppedBitmap,
            resultOffsetX.toFloat(),
            resultOffsetY.toFloat(),
            null
        )

        // 释放资源
        flippedBitmap.recycle()
        screenshotBitmap.recycle()
        croppedBitmap.recycle()
        return resultBitmap
    }

    fun stopRecord() {
        queueEvent { mCameraDrawer.stopRecord() }
        isRecording = false;
    }

    fun setSavePath(path: String?) {
        mCameraDrawer.setSavePath(path)
    }

    fun resume(auto: Boolean) {
        queueEvent { mCameraDrawer.onResume(auto) }
    }

    fun pause(auto: Boolean) {
        queueEvent { mCameraDrawer.onPause(auto) }
    }

    fun onTouch(event: MotionEvent?) {
        queueEvent { mCameraDrawer.onTouch(event) }
    }

    fun setOnFilterChangeListener(listener: SlideGpuFilterGroup.OnFilterChangeListener?) {
        mCameraDrawer.setOnFilterChangeListener(listener)
    }

    /**
     * 摄像头聚焦
     */
    fun onFocus(point: Point?, callback: AutoFocusCallback?) {
        try {
            if (mCameraController_back != null) {
                mCameraController_back!!.onFocus(point, callback)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            Log.e(TAG, "onFocus: error", ex)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = TmfApplication.screenWidth
        val height = width * 16 / 9
        setMeasuredDimension(width, height)
    }

    fun enableShowMutiCameraTexture(enable: Boolean) {
        mCameraDrawer.updateMutiCamera(enable)
    }

    fun setSwitchCamera(_switch: Boolean) {
        mCameraDrawer.setSwitchCamera(_switch)
    }

    fun startPreview() {
        open(cameraId, width, height, false)
    }

    //
    //    private int getCameraDisplayOrientation(int cameraId, int deviceRotation) {
    //        Camera.CameraInfo info = new Camera.CameraInfo();
    //        Camera.getCameraInfo(cameraId, info);
    //        int degrees = 0;
    //        switch (deviceRotation) {
    //            case Surface.ROTATION_0:
    //                degrees = 0;
    //                break;
    //            case Surface.ROTATION_90:
    //                degrees = 90;
    //                break;
    //            case Surface.ROTATION_180:
    //                degrees = 180;
    //                break;
    //            case Surface.ROTATION_270:
    //                degrees = 270;
    //                break;
    //        }
    //
    //        int result;
    //        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
    //            result = (info.orientation + degrees) % 360;
    //            result = (360 - result) % 360;  // Compensate for the mirror effect of the front camera
    //        } else {  // Back-facing camera
    //            result = (info.orientation - degrees + 360) % 360;
    //        }
    //        return result;
    //    }
    fun switchCamera() {
        cameraId = if (cameraId == 0) 1 else 0
        //        open(cameraId, 0, 0, true);
        if (isSupportMutiCameraRecord()) {
            mCameraDrawer.switchCamera()
            hasSwitched = !hasSwitched
        } else {
            open(cameraId, 0, 0, true)
        }
    }

    companion object {
        const val TAG = "tmf_CameraView"
    }
}
