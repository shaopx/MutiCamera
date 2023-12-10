package com.spx.muticamera

import android.content.Context
import android.content.res.Resources
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import com.spx.muticamera.encode.EncodeCallback
import com.spx.muticamera.encode.TextureMovieEncoder
import com.spx.muticamera.filter.BaseFilter
import com.spx.muticamera.filter.CameraDrawProcessFilter
import com.spx.muticamera.filter.CameraFilter
import com.spx.muticamera.filter.FrontCameraFilter
import com.spx.muticamera.filter.FrontCameraFilter.front_camera_frame_height
import com.spx.muticamera.filter.FrontCameraFilter.front_camera_frame_width
import com.spx.muticamera.filter.FrontCameraFilter.front_camera_frame_x
import com.spx.muticamera.filter.FrontCameraFilter.front_camera_frame_y
import com.spx.muticamera.filter.GroupFilter
import com.spx.muticamera.filter.MagicBeautyFilter
import com.spx.muticamera.filter.NoneFilter
import com.spx.muticamera.filter.SlideGpuFilterGroup
import com.spx.muticamera.filter.WaterMarkFilter
import com.spx.muticamera.filter.beauty.SmallWindowFilter
import com.spx.muticamera.util.EasyGlUtils
import com.spx.muticamera.util.RecordUtil
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class CameraDrawer(context: Context, resources: Resources?) : GLSurfaceView.Renderer {

    private val OM: FloatArray
    private var isSwitched = false

    /**
     * 显示画面的filter
     */
    private val showFilter: BaseFilter

    /**
     * 后台绘制的filter
     */
    private val backCameraDrawFilter: CameraFilter
    private val frontCameraFilter: FrontCameraFilter
    private var smallWindowFilter: SmallWindowFilter
    private val waterMarkFilterO: WaterMarkFilter
    //    private final WaterMarkFilter waterMarkFilter1;
    /**
     * 绘制水印的filter组
     */
    private val mBeFilter: GroupFilter
    private val mAfFilter: GroupFilter

    /**
     * 用于绘制美白效果的filter
     */
    private val mProcessFilter: BaseFilter

    /**
     * 美白的filter
     */
    private val mBeautyFilter: MagicBeautyFilter?

    /**
     * 多种滤镜切换
     */
    private val mSlideFilterGroup: SlideGpuFilterGroup
    var mSurfaceTextrue: SurfaceTexture? = null
    var mSurfaceTextrue_front: SurfaceTexture? = null
//    var texture: SurfaceTexture? = null
//        private set
//    var texture_front: SurfaceTexture? = null
//        private set

    /**
     * 预览数据的宽高
     */
    private var mPreviewWidth = 0
    private var mPreviewHeight = 0

    /**
     * 控件的宽高
     */
    private var width = 0
    private var height = 0
    private var videoEncoder: TextureMovieEncoder? = null

    @Volatile
    private var recordingEnabled = false
    private var recordingStatus = 0
    private var recordStartTimeMs: Long = 0
    private var savePath: String? = null
    private var textureID = 0
    private var textureID_front = 0
    private val fFrame = IntArray(1)
    private val fTexture = IntArray(1)
    private val SM = FloatArray(16) //用于显示的变换矩阵
    private var enableMuticamera = false
    private var switchCamera = false
    private var encodeCallback: EncodeCallback? = null
//    private var encodeCallback: EncodeCallback? = null

    init {
        //初始化一个滤镜 也可以叫控制器
        showFilter = NoneFilter(resources)
        backCameraDrawFilter = CameraFilter(resources)
        frontCameraFilter = FrontCameraFilter(context, resources)
        smallWindowFilter = SmallWindowFilter(context, resources)
        waterMarkFilterO = WaterMarkFilter(
            resources,
            R.drawable.vw_ic_uncheck,
            intArrayOf(
                front_camera_frame_x - 3,
                front_camera_frame_y - 3,
                front_camera_frame_width + 6,
                front_camera_frame_height + 6
            )
        )
        //        waterMarkFilter1 = new WaterMarkFilter(resources, R.drawable.ic_bubble_triangle, new int[]{650,1159, 42, 36});
        mProcessFilter = CameraDrawProcessFilter(resources)
        mBeFilter = GroupFilter(resources)
        mAfFilter = GroupFilter(resources)
        mBeautyFilter = MagicBeautyFilter()
        //        mBeautyFilter = new MagicAntiqueFilter();
        mSlideFilterGroup = SlideGpuFilterGroup()
        OM = MatrixUtils.getOriginalMatrix()
        MatrixUtils.flip(OM, false, false) //矩阵上下翻转
        showFilter.setMatrix(OM)
        val OM_front: FloatArray = MatrixUtils.getOriginalMatrix()
        //        MatrixUtils.scale(OM_front, 0.2f, 0.2f);
        frontCameraFilter.setMatrix(OM_front)
        smallWindowFilter.setMatrix(OM_front)
        backCameraDrawFilter.setMatrix(backCameraDrawFilter.getMatrix())
    }

    fun setEncodeCallback(callback: EncodeCallback) {
        this.encodeCallback = callback
    }

    fun updateMutiCamera(enable: Boolean) {
        enableMuticamera = enable
    }

    fun setSwitchCamera(_switch: Boolean) {
        switchCamera = _switch
        Log.i(TAG, "setSwitchCamera: switchCamera:$switchCamera")
        //        if (switchCamera) {
//            frontCameraFilter.setMatrix(frontCameraFilter.getMatrix());
//        }
        frontCameraFilter.setMatrix(frontCameraFilter.getMatrix())
        smallWindowFilter.setMatrix(smallWindowFilter.getMatrix())
        //        if (switchCamera) {
//            int textureId = backCameraDrawFilter.getTextureId();
//            Log.i(TAG, "setSwitchCamera: back:"+textureId);
//            backCameraDrawFilter.setTextureId(frontCameraFilter.getTextureId());
//            frontCameraFilter.setTextureId(textureId);
//        } else {
//            int textureId = backCameraDrawFilter.getTextureId();
//            backCameraDrawFilter.setTextureId(frontCameraFilter.getTextureId());
//            frontCameraFilter.setTextureId(textureId);
//        }
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        Log.i(TAG, "onSurfaceCreated: ...")
        textureID = createTextureID()
        mSurfaceTextrue = SurfaceTexture(textureID)
        textureID_front = createTextureID()
        mSurfaceTextrue_front = SurfaceTexture(textureID_front)
        Log.i(TAG, "onSurfaceCreated: back camera textureId: $textureID")
        Log.i(
            TAG,
            "onSurfaceCreated: front camera textureId: $textureID_front"
        )
        backCameraDrawFilter.create()
        backCameraDrawFilter.setTextureId(textureID)
        backCameraDrawFilter.setTextureId_front(textureID_front)
        frontCameraFilter.create()
        frontCameraFilter.setTextureId(textureID)
        frontCameraFilter.setTextureId_front(textureID_front)
        smallWindowFilter.create()
        waterMarkFilterO.create()
        //        waterMarkFilter1.create();
        mProcessFilter.create()
        showFilter.create()
        mBeFilter.create()
        mAfFilter.create()
        mBeautyFilter?.init()
        mSlideFilterGroup.init()
        recordingStatus = if (recordingEnabled) {
            RECORDING_RESUMED
        } else {
            RECORDING_OFF
        }
    }

    /**
     * 创建显示的texture
     */
    private fun createTextureID(): Int {
        val texture = IntArray(1)
        GLES20.glGenTextures(1, texture, 0) //第一个参数表示创建几个纹理对象，并将创建好的纹理对象放置到第二个参数中去
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0])
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE
        )
        return texture[0]
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        Log.i(TAG, "onSurfaceChanged: ....width:$mPreviewWidth,height:$mPreviewHeight")
        this.width = width
        this.height = height
        //清除遗留的
        GLES20.glDeleteFramebuffers(1, fFrame, 0)
        GLES20.glDeleteTextures(1, fTexture, 0)
        /**创建一个帧染缓冲区对象 */
        GLES20.glGenFramebuffers(1, fFrame, 0)
        /**根据纹理数量 返回的纹理索引 */
        GLES20.glGenTextures(1, fTexture, 0)
        /* GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width,
                height);*/
        /**将生产的纹理名称和对应纹理进行绑定 */
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fTexture[0])
        /**根据指定的参数 生产一个2D的纹理 调用该函数前  必须调用glBindTexture以指定要操作的纹理 */
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mPreviewWidth, mPreviewHeight,
            0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
        )
        useTexParameter()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        mProcessFilter.setSize(mPreviewWidth, mPreviewHeight)
        mBeFilter.setSize(mPreviewWidth, mPreviewHeight)
        mAfFilter.setSize(mPreviewWidth, mPreviewHeight)
        backCameraDrawFilter.setSize(mPreviewWidth, mPreviewHeight)
        frontCameraFilter.setSize(mPreviewWidth, mPreviewHeight)
        smallWindowFilter.setSize(mPreviewWidth, mPreviewHeight)
        waterMarkFilterO.setSize(mPreviewWidth, mPreviewHeight)
        //        waterMarkFilter1.setSize(mPreviewWidth,mPreviewHeight);
        mBeautyFilter?.onDisplaySizeChanged(mPreviewWidth, mPreviewHeight)
        mBeautyFilter?.onInputSizeChanged(mPreviewWidth, mPreviewHeight)
        mSlideFilterGroup.onSizeChanged(mPreviewWidth, mPreviewHeight)
        MatrixUtils.getShowMatrix(SM, mPreviewWidth, mPreviewHeight, width, height)
        showFilter.setMatrix(SM)
    }

    override fun onDrawFrame(gl: GL10) {
        /**更新界面中的数据 */
//        texture!!.updateTexImage()
        mSurfaceTextrue?.updateTexImage();
        mSurfaceTextrue_front?.updateTexImage();
        if (enableMuticamera) {
            frontCameraFilter.draw();
            smallWindowFilter.setTextureId(frontCameraFilter.getOutputTexture());
        }

        GLES20.glViewport(0, 0, mPreviewWidth, mPreviewHeight);
        EasyGlUtils.bindFrameTexture(fFrame[0], fTexture[0]);
        backCameraDrawFilter.draw();
//        frontCameraFilter.draw();
        if (enableMuticamera) {
            smallWindowFilter.draw();
        }

        EasyGlUtils.unBindFrameBuffer();

//        waterMarkFilterO.textureId = 0;
//        waterMarkFilterO.draw();
//        waterMarkFilterO.outputTexture

        mBeFilter.setTextureId(fTexture[0]);
        mBeFilter.draw();
//        if (mBeautyFilter != null && mBeautyFilter.getBeautyLevel() !== 0) {
////            EasyGlUtils.bindFrameTexture(fFrame[0],fTexture[0]);
////            GLES20.glViewport(0,0,mPreviewWidth,mPreviewHeight);
//            mBeautyFilter.onDrawFrame(mBeFilter.getOutputTexture())
//            //            EasyGlUtils.unBindFrameBuffer();
//            mProcessFilter.setTextureId(fTexture[0])
//        } else {
//            mProcessFilter.setTextureId(mBeFilter.getOutputTexture())
//        }
        mProcessFilter.setTextureId(mBeFilter.getOutputTexture());
        mProcessFilter.draw();

        mSlideFilterGroup.onDrawFrame(mProcessFilter.getOutputTexture());
        mAfFilter.setTextureId(mSlideFilterGroup.getOutputTexture());
        mAfFilter.draw();
        recording();
        /**绘制显示的filter*/
        GLES20.glViewport(0, 0, width, height);
//        Log.i(TAG, "onDrawFrame: width:"+width+", height:"+height);
        showFilter.setTextureId(mAfFilter.getOutputTexture());
//        showFilter.setTextureId(fTexture[0]);
        showFilter.draw();
        if (videoEncoder != null && recordingEnabled && recordingStatus == RECORDING_ON) {
            videoEncoder?.setTextureId(mAfFilter.getOutputTexture());
//            videoEncoder.setTextureId(fTexture[0]);
            videoEncoder?.frameAvailable(mSurfaceTextrue);
        }
    }

    private fun recording() {
//        Log.i(TAG, "recording: ...")
        if (recordingEnabled) {
            /**说明是录制状态 */
            when (recordingStatus) {
                RECORDING_OFF -> {
                    videoEncoder = TextureMovieEncoder()
                    videoEncoder?.setEncodeCallback(encodeCallback)
                    videoEncoder?.setPreviewSize(mPreviewWidth, mPreviewHeight)
                    Log.i(TAG, "recording: startRecording...path:${savePath}, size:${mPreviewWidth}x${mPreviewHeight}")
                    videoEncoder?.startRecording(
                        TextureMovieEncoder.EncoderConfig(
                            savePath, mPreviewWidth, mPreviewHeight,
                            20000000, EGL14.eglGetCurrentContext()
                        )
                    )
                    recordStartTimeMs = SystemClock.uptimeMillis()
                    recordingStatus = RECORDING_ON
                }

                RECORDING_RESUMED -> {
                    videoEncoder?.updateSharedContext(EGL14.eglGetCurrentContext())
                    videoEncoder?.resumeRecording()
                    recordingStatus = RECORDING_ON
                }

                RECORDING_ON -> {
                    val timeMs = SystemClock.uptimeMillis()
                    if (timeMs - recordStartTimeMs > RecordUtil.MAX_DURATION) {
                        videoEncoder?.switchNextOutputFile()
                        recordStartTimeMs = SystemClock.uptimeMillis()
                    }
                }

                RECORDING_PAUSED -> {}
                RECORDING_PAUSE -> {
                    videoEncoder?.pauseRecording()
                    recordingStatus = RECORDING_PAUSED
                }

                RECORDING_RESUME -> {
                    videoEncoder?.resumeRecording()
                    recordingStatus = RECORDING_ON
                }

                else -> throw RuntimeException("unknown recording status $recordingStatus")
            }
        } else {
            when (recordingStatus) {
                RECORDING_ON, RECORDING_RESUMED, RECORDING_PAUSE, RECORDING_RESUME, RECORDING_PAUSED -> {
                    Log.i(TAG, "recording: stop video encoder...")
                    videoEncoder?.stopRecording()
                    recordingStatus = RECORDING_OFF
                }

                RECORDING_OFF -> {}
                else -> throw RuntimeException("unknown recording status $recordingStatus")
            }
        }
    }

    /**
     * 触摸事件的传递
     */
    fun onTouch(event: MotionEvent?) {
        mSlideFilterGroup.onTouchEvent(event)
    }

    /**
     * 滤镜切换的事件监听
     */
    fun setOnFilterChangeListener(listener: SlideGpuFilterGroup.OnFilterChangeListener?) {
        mSlideFilterGroup.setOnFilterChangeListener(listener)
    }

    /**
     * 设置预览效果的size
     */
    fun setPreviewSize(width: Int, height: Int) {
        Log.i(TAG, "setPreviewSize: ...width:$width, height:$height")
        if (mPreviewWidth != width || mPreviewHeight != height) {
            mPreviewWidth = width
            mPreviewHeight = height
        }
    }

    /**
     * 提供修改美白等级的接口
     */
    fun changeBeautyLevel(level: Int) {
        mBeautyFilter?.setBeautyLevel(level)
    }

    val beautyLevel: Int
        get() = mBeautyFilter!!.getBeautyLevel()

    /**
     * 根据摄像头设置纹理映射坐标
     */
    fun setCameraId(id: Int) {
        backCameraDrawFilter.setFlag(id)
        frontCameraFilter.setFlag(1)
    }

    fun startRecord() {
        Log.i(TAG, "startRecord: ...")
        recordingEnabled = true
    }

    fun stopRecord() {
        Log.i(TAG, "stopRecord: ...")
        recordingEnabled = false
    }

    fun setSavePath(path: String?) {
        savePath = path
    }

    fun onPause(auto: Boolean) {
        if (auto) {
            videoEncoder?.pauseRecording()
            if (recordingStatus == RECORDING_ON) {
                recordingStatus = RECORDING_PAUSED
            }
            return
        }
        if (recordingStatus == RECORDING_ON) {
            recordingStatus = RECORDING_PAUSE
        }
    }

    fun onResume(auto: Boolean) {
        if (auto) {
            if (recordingStatus == RECORDING_PAUSED) {
                recordingStatus = RECORDING_RESUME
            }
            return
        }
        if (recordingStatus == RECORDING_PAUSED) {
            recordingStatus = RECORDING_RESUME
        }
    }

    fun useTexParameter() {
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST.toFloat()
        )
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE.toFloat()
        )
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE.toFloat()
        )
    }

    fun switchCamera() {
        if (!isSwitched) {
            backCameraDrawFilter.setFlag(1)
            frontCameraFilter.setFlag(0)
            backCameraDrawFilter.setTextureId(textureID_front)
            backCameraDrawFilter.setTextureId_front(textureID)
            frontCameraFilter.setTextureId(textureID_front)
            frontCameraFilter.setTextureId_front(textureID)
            val OM_front: FloatArray = MatrixUtils.getOriginalMatrix()
            frontCameraFilter.setMatrix(OM_front)
            backCameraDrawFilter.setMatrix(MatrixUtils.getOriginalMatrix())
        } else {
            backCameraDrawFilter.setFlag(0)
            frontCameraFilter.setFlag(1)
            backCameraDrawFilter.setTextureId(textureID)
            backCameraDrawFilter.setTextureId_front(textureID_front)
            frontCameraFilter.setTextureId(textureID)
            frontCameraFilter.setTextureId_front(textureID_front)
            val OM_front: FloatArray = MatrixUtils.getOriginalMatrix()
            frontCameraFilter.setMatrix(OM_front)
            backCameraDrawFilter.setMatrix(MatrixUtils.getOriginalMatrix())
        }
        isSwitched = !isSwitched
    }

    companion object {
        private const val TAG = "tmf_CameraDrawer"
        private const val RECORDING_OFF = 0
        private const val RECORDING_ON = 1
        private const val RECORDING_RESUMED = 2
        private const val RECORDING_PAUSE = 3
        private const val RECORDING_RESUME = 4
        private const val RECORDING_PAUSED = 5
    }
}
