package com.spx.muticamera

import android.annotation.SuppressLint
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.spx.muticamera.filter.MagicFilterType
import com.spx.muticamera.filter.SlideGpuFilterGroup
import com.spx.muticamera.permission.onRequestPermissionResult
import com.spx.muticamera.permission.setupCameraWithPermissionCheck
import com.spx.muticamera.ui.CustomRecordImageView
import com.spx.muticamera.util.AppUtil.simpleToast
import com.spx.muticamera.util.FileUtils.getStorageFileName
import com.spx.muticamera.util.RecordUtil.isSupportMutiCameraRecord
import com.spx.muticamera.video.RecordFile

class CameraFragment : Fragment(), OnTouchListener, SlideGpuFilterGroup.OnFilterChangeListener {
    private val TAG = "tmf_CameraFragment"

    private lateinit var mRecordCameraView: CameraView
    private lateinit var mRecordCameraViewText: TextView
    private lateinit var mRecordButtonImageView: CustomRecordImageView
    private lateinit var mProcessingView: View
    private lateinit var mSwitchCamera: View
    private lateinit var mEnableBeautyView: TextView
    private lateinit var mEnableStickerView: TextView

    private var mIsLongPressing = false
    private var mRecordStartTime: Long = 0
    private var mIsInTakePhoto = false
    private var isRecording = false
    private var mIsStopRecording = false
    private var mIsSwitching = false
    private var mIsBackCamera = true
    private var mEnableBeauty = false
    private var mEnableSticker = false

    private var mIsMutiCameraSupported = isSupportMutiCameraRecord()

    private var storageMp4: String? = null;
    private var mRecordFiles: List<RecordFile> = mutableListOf()
    private val mHandler = Handler()
    var finishRecodTask =
        Runnable { //      Log.i(TAG, "run: finish record task.  mRecordFiles.size:" + mRecordFiles.size());
            mIsStopRecording = false
            handleRecordingFinish()
        }

    private fun handleRecordingFinish() {
        if (mRecordFiles == null || mRecordFiles.isEmpty()) {
            hideProgressView()
            simpleToast("拍摄失败,请重新拍摄")
            mIsStopRecording = false
            return
        }
        Log.i(TAG, "handleRecordingFinish: mRecordFiles:${mRecordFiles.get(0)}")
        simpleToast("拍摄成功, 文件地址:${mRecordFiles.get(0)}")

        //跳转到播放页面播放刚才录制的视频
        val bundle = Bundle()
        bundle.putSerializable("recording", mRecordFiles.get(0).videoPath)
        findNavController().navigate(R.id.action_record_to_play, bundle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        GlobalSetting.reset()
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: ...")
        mRecordCameraView = view.findViewById(R.id.record_camera_view)
        mRecordCameraViewText = view.findViewById(R.id.tv_record_button_text)
        mProcessingView = view.findViewById(R.id.processing_view)
        mRecordButtonImageView = view.findViewById(R.id.record_button)
        mSwitchCamera = view.findViewById(R.id.switch_camera)
        mRecordButtonImageView.setOnClickListener {
            onRecordClicked();
        }
//        mRecordButtonImageView.setOnTouchListener { v, event ->
//            handleRecordButtonTouch(v, event)
//            true
//        }
        mRecordCameraView.setEncodeCallback { paths ->
            if (paths != null && !paths.isEmpty()) {
                mRecordFiles = paths
            }
            if (mIsStopRecording) {
                mHandler.post {
                    if (mIsStopRecording) {
                        finishRecodTask.run()
                    }
                }
            }
        }

        mRecordCameraView.setOnTouchListener(this)
        mRecordCameraView.setOnFilterChangeListener(this)

        if (isSupportMutiCameraRecord()) {
            mRecordCameraView.enableMutiCameraRecord(true)
            mRecordCameraView.setMutiCameraOpenFailCallback(object :
                CameraView.MutiCameraOpenFailCallback {
                override fun onMutiCameraOpenFail(ex: Exception?) {
                    mRecordCameraView.enableMutiCameraRecord(false)
                    mIsMutiCameraSupported = false
                }

            })
        } else {
            mRecordCameraView.enableMutiCameraRecord(false)
        }
        mSwitchCamera.setOnClickListener {
            switchCamera();
        }
        mEnableBeautyView = view.findViewById<TextView>(R.id.button_beauty)
        mEnableBeautyView.setOnClickListener {
            GlobalSetting.enableBeauty = !GlobalSetting.enableBeauty
            mEnableBeautyView.text = if (GlobalSetting.enableBeauty) "关闭美颜" else "开启美颜"
        }
        mEnableStickerView = view.findViewById<TextView>(R.id.button_sticker)
        mEnableStickerView.setOnClickListener {
            GlobalSetting.enableSticker = !GlobalSetting.enableSticker
            mEnableStickerView.text = if (GlobalSetting.enableSticker) "关闭贴纸" else "开启贴纸"
        }

        setupCameraWithPermissionCheck()
    }

    private fun switchCamera() {
        if (mIsSwitching) {
            return
        }
        mIsSwitching = true

        if (mIsMutiCameraSupported) {
            mRecordCameraView.switchCamera()
            mIsSwitching = false
            mIsBackCamera = !mIsBackCamera
        } else {
            mRecordCameraView.switchCamera()
            mIsSwitching = false
        }
    }

    private fun onRecordClicked() {
        if (isRecording) {
            if (System.currentTimeMillis() - mRecordStartTime < 1200) {
                simpleToast("拍摄时间太短, 请重新拍摄")
                // 只是结束拍摄, 不去操作的录制结果
                stopRecording()
            } else {
                onStopRecording()
            }
            mRecordCameraViewText.text = "点击录制"
        } else {
            onStartRecording()
        }
    }

    private fun onStartRecording() {
        mRecordStartTime = System.currentTimeMillis()
        isRecording = true

        mRecordButtonImageView.startRecord()
        storageMp4 = getStorageFileName(System.currentTimeMillis().toString(), ".mp4")
        Log.i(TAG, "onStartRecording: storageMp4:${storageMp4}")
        mRecordCameraView.setSavePath(storageMp4)
        mRecordCameraView.startRecord()
        mRecordCameraViewText.text = "再次点击停止录制"
    }

    private fun showProgressView() {
        mProcessingView.setVisibility(View.VISIBLE)
    }

    private fun hideProgressView() {
        mProcessingView.visibility = View.GONE
    }

    private fun onStopRecording() {
        mIsStopRecording = true
        //应该先显示一个转圈圈, 避免操作过快
        showProgressView()
        stopRecording()

        // 最多等待2秒, 还没结束的话, 强制结束
        mHandler.postDelayed(Runnable {
            if (mIsStopRecording) {
                finishRecodTask.run()
            }
        }, 5000)
    }

    private fun stopRecording() {
        mRecordCameraView.stopRecord()
        mRecordButtonImageView.stopRecord()
//        takePicture()
    }

    private fun takePicture() {
        // 拍照
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mRecordCameraView?.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionResult(requestCode, grantResults)
    }

    @SuppressLint("ClickableViewAccessibility")
    internal fun setupCamera() {
        mRecordCameraView.startPreview()
    }

    internal fun onCameraPermissionsDenied() {
        Log.i(TAG, "onCameraPermissionsDenied: ...")
        simpleToast("需要摄像头录制权限和录音权限")
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        mRecordCameraView.onTouch(event)
//        if (mRecordCameraView.getCameraId() === 1) {
//            return false
//        }
        when (event!!.action) {
            MotionEvent.ACTION_UP -> {
                val sRawX = event!!.rawX
                val sRawY = event!!.rawY
                var rawY = sRawY * TmfApplication.screenWidth / TmfApplication.screenHeight
                val rawX = rawY
                rawY =
                    (TmfApplication.screenWidth - sRawX) * TmfApplication.screenHeight / TmfApplication.screenWidth
                val point = Point(rawX.toInt(), rawY.toInt())
                mRecordCameraView.onFocus(
                    point
                ) { success, camera -> }
            }
        }
        return true
    }

    override fun onFilterChange(type: MagicFilterType?) {
        mHandler.post(Runnable {
            if (type === MagicFilterType.NONE) {
                simpleToast("无滤镜")
            } else {
                simpleToast("滤镜切换为" + type)
            }
        })
    }
}