package com.spx.muticamera

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.Camera.AutoFocusCallback
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import java.util.Arrays
import java.util.Collections


class CameraController2 @TargetApi(Build.VERSION_CODES.M) constructor(
    private val mActivity: Activity, private val name: String
) :
    ICamera {
    private var mCameraId = CameraCharacteristics.LENS_FACING_FRONT // 要打开的摄像头ID
    private var mCameraCharacteristics: CameraCharacteristics? = null // 相机属性
    private var mCameraManager: CameraManager? = null // 相机管理者
    private var mCameraDevice: CameraDevice? = null // 相机对象
    private var mCaptureSession: CameraCaptureSession? = null
    private var mPreviewRequestBuilder: CaptureRequest.Builder? = null // 相机预览请求的构造器
    private val mPreviewRequest: CaptureRequest? = null
    private var mBackgroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null
    private var mPictureImageReader: ImageReader? = null
    private var mPreviewSurface: Surface? = null
    private val mOrientationEventListener: OrientationEventListener
    private var mPreviewSize: Size? = null // 预览大小
    private var mPictureSize: Size? = null // 拍照大小
    private var mDisplayRotation = 0 // 原始Sensor画面顺时针旋转该角度后，画面朝上
    private var mDeviceOrientation = 0 // 设备方向，由相机传感器获取

    /* 缩放相关 */
    private val MAX_ZOOM = 200 // 放大的最大值，用于计算每次放大/缩小操作改变的大小
    private var mZoom = 0 // 0~mMaxZoom之间变化
    private var mStepWidth = 0f // 每次改变的宽度大小
    private var mStepHeight = 0f // 每次改变的高度大小

    /**
     * 打开摄像头的回调
     */
    private val mStateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            log(TAG, "onOpened")
            mCameraDevice = camera
            initPreviewRequest()
            createCommonSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            log(TAG, "onDisconnected")
            releaseCamera()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            log(TAG, "Camera Open failed, error: $error")
            releaseCamera()
        }
    }

    private fun log(TAG: String, message: String?) {
        Log.i("$TAG-$name", message?:"")
    }

    override fun setUpCameraOutputs(width: Int, height: Int) {
        log(TAG, "setUpCameraOutputs: width:$width, height:$height")
        mCameraManager = mActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            mCameraCharacteristics =
                mCameraManager!!.getCameraCharacteristics(Integer.toString(mCameraId))
            val map =
                mCameraCharacteristics!!.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            //            Size[] supportPictureSizes = map.getOutputSizes(ImageFormat.JPEG);
            val supportPictureSizes = map!!.getOutputSizes(ImageFormat.JPEG)
            val pictureSize =
                Collections.max(Arrays.asList(*supportPictureSizes), CompareSizesByArea())
            log(TAG, "setUpCameraOutputs: ...pictureSize.getHeight():" + pictureSize.height)
            log(TAG, "setUpCameraOutputs: ...pictureSize.getWidth():" + pictureSize.width)
            //            float aspectRatio = pictureSize.getHeight() * 1.0f / pictureSize.getWidth();
            val aspectRatio = 16 * 1.0f / 9
            val supportPreviewSizes = map.getOutputSizes(
                SurfaceTexture::class.java
            )
            // 一般相机页面都是固定竖屏，宽是短边，所以根据view的宽度来计算需要的预览大小
            val previewSize = chooseOptimalSize(supportPreviewSizes, width, aspectRatio)
            log(TAG, "pictureSize: $pictureSize")
            log(TAG, "previewSize: $previewSize")

//      mPictureSize = pictureSize;
            mPictureSize = Size(1280, 720)
            //            mPreviewSize = previewSize;
            mPreviewSize = Size(1280, 720)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun _openAndpreview(cameraId: Int) {
        mCameraId = cameraId
    }

    @SuppressLint("MissingPermission")
    override fun openAndPreview() {
        log(TAG, "preview... mCameraId:$mCameraId")
        startBackgroundThread() // 对应 releaseCamera() 方法中的 stopBackgroundThread()
        mOrientationEventListener.enable()
        try {
            mCameraCharacteristics =
                mCameraManager!!.getCameraCharacteristics(Integer.toString(mCameraId))
            // 每次切换摄像头计算一次就行，结果缓存到成员变量中
            initDisplayRotation()
            initZoomParameter()
            // 打开摄像头
            mCameraManager!!.openCamera(
                Integer.toString(mCameraId),
                mStateCallback,
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun initDisplayRotation() {
        var displayRotation = mActivity.windowManager.defaultDisplay.rotation
        when (displayRotation) {
            Surface.ROTATION_0 -> displayRotation = 90
            Surface.ROTATION_90 -> displayRotation = 0
            Surface.ROTATION_180 -> displayRotation = 270
            Surface.ROTATION_270 -> displayRotation = 180
        }
        val sensorOrientation =
            mCameraCharacteristics!!.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        mDisplayRotation = (displayRotation + sensorOrientation + 270) % 360
        log(TAG, "mDisplayRotation: $mDisplayRotation")
    }

    private fun initZoomParameter() {
        val rect = mCameraCharacteristics!!.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        log(TAG, "sensor_info_active_array_size: $rect")
        // max_digital_zoom 表示 active_rect 除以 crop_rect 的最大值
        val max_digital_zoom =
            mCameraCharacteristics!!.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)!!
        log(TAG, "max_digital_zoom: $max_digital_zoom")
        // crop_rect的最小宽高
        val minWidth = rect!!.width() / max_digital_zoom
        val minHeight = rect.height() / max_digital_zoom
        // 因为缩放时两边都要变化，所以要除以2
        mStepWidth = (rect.width() - minWidth) / MAX_ZOOM / 2
        mStepHeight = (rect.height() - minHeight) / MAX_ZOOM / 2
    }

    override fun releaseCamera() {
        log(TAG, "releaseCamera")
        //    stopPreview();
        if (null != mCaptureSession) {
            mCaptureSession!!.close()
            mCaptureSession = null
        }
        if (mCameraDevice != null) {
            mCameraDevice!!.close()
            mCameraDevice = null
        }
        if (mPictureImageReader != null) {
            mPictureImageReader!!.close()
            mPictureImageReader = null
        }
        mOrientationEventListener.disable()
        stopBackgroundThread() // 对应 openCamera() 方法中的 startBackgroundThread()
    }

    //  public void setPreviewSurface(SurfaceHolder holder) {
    //    mPreviewSurface = holder.getSurface();
    //  }
    private fun createCommonSession() {
        val outputs: MutableList<Surface> = ArrayList()
        // preview output
        if (mPreviewSurface != null) {
            log(TAG, "createCommonSession add target mPreviewSurface")
            outputs.add(mPreviewSurface!!)
        }
        // picture output
        val pictureSize = mPictureSize
        if (pictureSize != null) {
            log(TAG, "createCommonSession add target mPictureImageReader")
            mPictureImageReader =
                ImageReader.newInstance(pictureSize.width, pictureSize.height, ImageFormat.JPEG, 1)
            outputs.add(mPictureImageReader!!.surface)
        }
        try {
            // 一个session中，所有CaptureRequest能够添加的target，必须是outputs的子集，所以在创建session的时候需要都添加进来
            mCameraDevice!!.createCaptureSession(
                outputs,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        mCaptureSession = session
                        startPreview()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        log(TAG, "ConfigureFailed. session: $session")
                    }
                },
                mBackgroundHandler
            ) // handle 传入 null 表示使用当前线程的 Looper
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun initPreviewRequest() {
        if (mPreviewSurface == null) {
            log(TAG, "initPreviewRequest failed, mPreviewSurface is null")
            return
        }
        try {
            mPreviewRequestBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            // 设置预览输出的 Surface
            mPreviewRequestBuilder!!.addTarget(mPreviewSurface!!)
            // 设置连续自动对焦
            mPreviewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            // 设置自动曝光
            mPreviewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            )
            // 设置自动白平衡
            mPreviewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_AUTO
            )
            // 设置闪光灯为打开状态
//      if (name.equalsIgnoreCase("back")) {
//        mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
//        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
//        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
//      }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun startPreview() {
        log(TAG, "startPreview")
        if (mCaptureSession == null || mPreviewRequestBuilder == null) {
            Log.w(TAG, "startPreview: mCaptureSession or mPreviewRequestBuilder is null")
            return
        }
        try {
            // 开始预览，即一直发送预览的请求
            val captureRequest = mPreviewRequestBuilder!!.build()
            mCaptureSession!!.setRepeatingRequest(captureRequest, null, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun stopPreview() {
        log(TAG, "stopPreview")
        if (mCaptureSession == null) {
            Log.w(TAG, "stopPreview: mCaptureSession is null")
            return
        }
        try {
//      mCaptureSession.setRepeatingRequest()
            mCaptureSession!!.stopRepeating()
        } catch (e: CameraAccessException) {
//      e.printStackTrace();
            log(TAG, "stopPreview exception:" + e.message)
        }
    }

    override fun captureStillPicture(onImageAvailableListener: ImageReader.OnImageAvailableListener?) {
        if (mPictureImageReader == null) {
            Log.w(TAG, "captureStillPicture failed! mPictureImageReader is null")
            return
        }
        mPictureImageReader!!.setOnImageAvailableListener(
            onImageAvailableListener,
            mBackgroundHandler
        )
        try {
            // 创建一个用于拍照的Request
            val captureBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(mPictureImageReader!!.surface)
            captureBuilder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            captureBuilder.set(
                CaptureRequest.JPEG_ORIENTATION,
                getJpegOrientation(mDeviceOrientation)
            )
            // 预览如果有放大，拍照的时候也应该保存相同的缩放
            val zoomRect = mPreviewRequestBuilder!!.get(CaptureRequest.SCALER_CROP_REGION)
            if (zoomRect != null) {
                captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
            }
            stopPreview()
            mCaptureSession!!.abortCaptures()
            val time = System.currentTimeMillis()
            mCaptureSession!!.capture(
                captureBuilder.build(),
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        Log.w(
                            TAG,
                            "onCaptureCompleted, time: " + (System.currentTimeMillis() - time)
                        )
                        try {
                            mPreviewRequestBuilder!!.set(
                                CaptureRequest.CONTROL_AF_TRIGGER,
                                CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
                            )
                            mCaptureSession!!.capture(
                                mPreviewRequestBuilder!!.build(),
                                null,
                                mBackgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                        startPreview()
                    }
                },
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun getJpegOrientation(deviceOrientation: Int): Int {
        var deviceOrientation = deviceOrientation
        if (deviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) return 0
        val sensorOrientation =
            mCameraCharacteristics!!.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90
        // Reverse device orientation for front-facing cameras
        val facingFront =
            mCameraCharacteristics!!.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        if (facingFront) deviceOrientation = -deviceOrientation
        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        val jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360
        log(TAG, "jpegOrientation: $jpegOrientation")
        return jpegOrientation
    }

    val isFrontCamera: Boolean
        get() = mCameraId == CameraCharacteristics.LENS_FACING_BACK

    //  public Size getPreviewSize() {
    //    return mPreviewSize;
    //  }
    fun setPreviewSize(previewSize: Size?) {
        mPreviewSize = previewSize
    }

    //  public Size getPictureSize() {
    //    return mPictureSize;
    //  }
    override fun close(): Boolean {
        releaseCamera()
        return false
    }

    fun setPictureSize(pictureSize: Size?) {
        mPictureSize = pictureSize
    }

    fun switchCamera() {
//    mCameraId ^= 1;
        log(TAG, "switchCamera: mCameraId: $mCameraId")
        releaseCamera()
        _openAndpreview(1 - mCameraId)
    }

    fun handleZoom(isZoomIn: Boolean) {
        if (mCameraDevice == null || mCameraCharacteristics == null || mPreviewRequestBuilder == null) {
            return
        }
        if (isZoomIn && mZoom < MAX_ZOOM) { // 放大
            mZoom++
        } else if (mZoom > 0) { // 缩小
            mZoom--
        }
        log(TAG, "handleZoom: mZoom: $mZoom")
        val rect = mCameraCharacteristics!!.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val cropW = (mStepWidth * mZoom).toInt()
        val cropH = (mStepHeight * mZoom).toInt()
        val zoomRect =
            Rect(rect!!.left + cropW, rect.top + cropH, rect.right - cropW, rect.bottom - cropH)
        log(TAG, "zoomRect: $zoomRect")
        mPreviewRequestBuilder!!.set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
        startPreview() // 需要重新 start preview 才能生效
    }

    override fun trunOnFlash(on: Boolean) {
        try {
            Log.i(
                TAG,
                "trunOnFlash: " + on + ", name:" + name + ", cameraId:" + Integer.toString(mCameraId)
            )
            if (name.equals("back", ignoreCase = true)) {
                mPreviewRequestBuilder!!.set(
                    CaptureRequest.FLASH_MODE,
                    if (on) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF
                )
                mPreviewRequestBuilder!!.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CameraMetadata.CONTROL_AE_MODE_ON
                )
                val captureRequest = mPreviewRequestBuilder!!.build()
                mCaptureSession!!.setRepeatingRequest(captureRequest, null, mBackgroundHandler)
            }
        } catch (e: Exception) {
            Log.e(TAG, "trunOnFlash fail($on): ", e)
        }
    }

    fun triggerFocusAtPoint(x: Float, y: Float, width: Int, height: Int) {
        log(TAG, "triggerFocusAtPoint ($x, $y)")
        val cropRegion = mPreviewRequestBuilder!!.get(CaptureRequest.SCALER_CROP_REGION)
        val afRegion = getAFAERegion(x, y, width, height, 1f, cropRegion)
        // ae的区域比af的稍大一点，聚焦的效果比较好
        val aeRegion = getAFAERegion(x, y, width, height, 1.5f, cropRegion)
        mPreviewRequestBuilder!!.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(afRegion))
        mPreviewRequestBuilder!!.set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(aeRegion))
        mPreviewRequestBuilder!!.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_AUTO
        )
        mPreviewRequestBuilder!!.set(
            CaptureRequest.CONTROL_AF_TRIGGER,
            CameraMetadata.CONTROL_AF_TRIGGER_START
        )
        mPreviewRequestBuilder!!.set(
            CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
            CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START
        )
        try {
            mCaptureSession!!.capture(
                mPreviewRequestBuilder!!.build(),
                mAfCaptureCallback,
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun getAFAERegion(
        x: Float,
        y: Float,
        viewWidth: Int,
        viewHeight: Int,
        multiple: Float,
        cropRegion: Rect?
    ): MeteringRectangle {
        log(TAG, "getAFAERegion enter")
        log(
            TAG,
            "point: [$x, $y], viewWidth: $viewWidth, viewHeight: $viewHeight"
        )
        log(TAG, "multiple: $multiple")
        // do rotate and mirror
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val matrix1 = Matrix()
        matrix1.setRotate(mDisplayRotation.toFloat())
        matrix1.postScale((if (isFrontCamera) -1 else 1).toFloat(), 1f)
        matrix1.invert(matrix1)
        matrix1.mapRect(viewRect)
        // get scale and translate matrix
        val matrix2 = Matrix()
        val cropRect = RectF(cropRegion)
        matrix2.setRectToRect(viewRect, cropRect, Matrix.ScaleToFit.CENTER)
        log(TAG, "viewRect: $viewRect")
        log(TAG, "cropRect: $cropRect")
        // get out region
        val side = (Math.max(viewWidth, viewHeight) / 8 * multiple).toInt()
        val outRect = RectF(x - side / 2, y - side / 2, x + side / 2, y + side / 2)
        log(TAG, "outRect before: $outRect")
        matrix1.mapRect(outRect)
        matrix2.mapRect(outRect)
        log(TAG, "outRect after: $outRect")
        // 做一个clamp，测光区域不能超出cropRegion的区域
        val meteringRect = Rect(
            outRect.left.toInt(),
            outRect.top.toInt(),
            outRect.right.toInt(),
            outRect.bottom.toInt()
        )
        meteringRect.left = clamp(meteringRect.left, cropRegion!!.left, cropRegion.right)
        meteringRect.top = clamp(meteringRect.top, cropRegion.top, cropRegion.bottom)
        meteringRect.right = clamp(meteringRect.right, cropRegion.left, cropRegion.right)
        meteringRect.bottom = clamp(meteringRect.bottom, cropRegion.top, cropRegion.bottom)
        log(TAG, "meteringRegion: $meteringRect")
        return MeteringRectangle(meteringRect, 1000)
    }

    private val mAfCaptureCallback: CameraCaptureSession.CaptureCallback =
        object : CameraCaptureSession.CaptureCallback() {
            private fun process(result: CaptureResult) {
                val state = result.get(CaptureResult.CONTROL_AF_STATE)
                log(TAG, "CONTROL_AF_STATE: $state")
                if (state == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || state == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                    log(TAG, "process: start normal preview")
                    mPreviewRequestBuilder!!.set(
                        CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
                    )
                    mPreviewRequestBuilder!!.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )
                    mPreviewRequestBuilder!!.set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.FLASH_MODE_OFF
                    )
                    startPreview()
                }
            }

            override fun onCaptureProgressed(
                session: CameraCaptureSession,
                request: CaptureRequest,
                partialResult: CaptureResult
            ) {
                process(partialResult)
            }

            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                process(result)
            }
        }

    init {
        mOrientationEventListener = object : OrientationEventListener(mActivity) {
            override fun onOrientationChanged(orientation: Int) {
                mDeviceOrientation = orientation
            }
        }
    }

    private fun startBackgroundThread() {
        if (mBackgroundThread == null || mBackgroundHandler == null) {
            log(TAG, "startBackgroundThread")
            mBackgroundThread = HandlerThread("CameraBackground-$name")
            mBackgroundThread!!.start()
            mBackgroundHandler = Handler(mBackgroundThread!!.looper)
        }
    }

    private fun stopBackgroundThread() {
        log(TAG, "stopBackgroundThread")
        if (mBackgroundThread != null) {
            mBackgroundThread!!.quitSafely()
            try {
                mBackgroundThread!!.join()
                mBackgroundThread = null
                mBackgroundHandler = null
            } catch (e: InterruptedException) {
                e.printStackTrace()
                log(TAG, "stopBackgroundThread")
                log(TAG, e.message)
            }
        }
        log(TAG, "stopBackgroundThread end")
    }

    fun chooseOptimalSize(sizes: Array<Size>?, dstSize: Int, aspectRatio: Float): Size? {
        if (sizes == null || sizes.size <= 0) {
            log(TAG, "chooseOptimalSize failed, input sizes is empty")
            return null
        }
        var minDelta = Int.MAX_VALUE // 最小的差值，初始值应该设置大点保证之后的计算中会被重置
        var index = 0 // 最小的差值对应的索引坐标
        for (i in sizes.indices) {
            val size = sizes[i]
            // 先判断比例是否相等
            if (size.width * aspectRatio == size.height.toFloat()) {
                val delta = Math.abs(dstSize - size.height)
                if (delta == 0) {
                    return size
                }
                if (minDelta > delta) {
                    minDelta = delta
                    index = i
                }
            }
        }
        return sizes[index]
    }

    private fun clamp(x: Int, min: Int, max: Int): Int {
        if (x > max) return max
        return if (x < min) min else x
    }

    internal class CompareSizesByArea : Comparator<Size> {
        override fun compare(lhs: Size, rhs: Size): Int {
            // 我们在这里投放，以确保乘法不会溢出
            return java.lang.Long.signum(
                lhs.width.toLong() * lhs.height -
                        rhs.width.toLong() * rhs.height
            )
        }
    }

    override fun onFocus(point: Point?, callback: AutoFocusCallback?) {}
    override fun open(cameraId: Int) {
        mCameraId = cameraId
    }

    override fun setPreviewTexture(texture: SurfaceTexture?) {
        if (texture != null) {
//      texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
            mPreviewSurface = Surface(texture)
        }
    }

    override fun setConfig(config: ICamera.Config?) {}
    override fun setOnPreviewFrameCallback(callback: ICamera.PreviewFrameCallback?) {}
    override val previewSize: Point
        get() = Point(mPreviewSize!!.width, mPreviewSize!!.height)
    override val pictureSize: Point?
        get() = null

    fun onFocus(point: Point?) {}

    companion object {
        private const val TAG = "flutter_camera2"
    }
}
