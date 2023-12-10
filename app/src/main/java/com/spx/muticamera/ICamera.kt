package com.spx.muticamera

import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.Camera.AutoFocusCallback
import android.media.ImageReader


interface ICamera {
    /**open the camera */
    fun open(cameraId: Int)
    fun setPreviewTexture(texture: SurfaceTexture?)

    /**set the camera config */
    fun setConfig(config: Config?)
    fun setOnPreviewFrameCallback(callback: PreviewFrameCallback?)
    fun openAndPreview()
    val previewSize: Point?
    val pictureSize: Point?

    /**close the camera */
    fun close(): Boolean
    fun releaseCamera()
    fun trunOnFlash(on: Boolean)
    fun onFocus(point: Point?, callback: AutoFocusCallback?)
    fun setUpCameraOutputs(mSurfaceWidth: Int, mSurfaceHeight: Int)
    fun captureStillPicture(onImageAvailableListener: ImageReader.OnImageAvailableListener?)
    class Config {
        //        public float rate=1280f/720f; //宽高比
        var rate = 720 / 720f //宽高比
        var minPreviewWidth = 0
        var minPictureWidth = 0
    }

    interface PreviewFrameCallback {
        fun onPreviewFrame(bytes: ByteArray?, width: Int, height: Int)
    }
}
