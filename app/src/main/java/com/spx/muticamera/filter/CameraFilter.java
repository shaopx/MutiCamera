package com.spx.muticamera.filter;


import android.content.res.Resources;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

public class CameraFilter extends OESFilter {

    int mResolationHandle;
    int mMovementHandle;

    public CameraFilter(Resources mRes) {
        super(mRes);
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mResolationHandle = GLES20.glGetUniformLocation(mProgram, "u_resolution");
        mMovementHandle = GLES20.glGetUniformLocation(mProgram, "u_movement");
    }

    @Override
    public void setFlag(int flag) {
        super.setFlag(flag);
        float[] coord;
        if (getFlag() == 1) {    //前置摄像头 顺时针旋转90,并上下颠倒
            coord = new float[]{
                    1.0f, 1.0f,
                    0.0f, 1.0f,
                    1.0f, 0.0f,
                    0.0f, 0.0f,
            };
        } else {               //后置摄像头 顺时针旋转90度
            coord = new float[]{
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    0.0f, 0.0f,
                    1.0f, 0.0f,
            };
        }
        mTexBuffer.clear();
        mTexBuffer.put(coord);
        mTexBuffer.position(0);
    }

    @Override
    public void setMatrix(float[] matrix) {
        super.setMatrix(matrix);

//        MatrixUtils.scale(matrix, 1f, 1.778f);
    }

    @Override
    public void draw() {
//        super.draw();
        onClear();
        onUseProgram();
        onSetExpandData();
//        Log.i("shaopx_debug", "draw: getTextureType:"+getTextureType());
//        onBindTexture();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + getTextureType());
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, getTextureId());
        GLES20.glUniform1i(mHTexture, getTextureType());

        GLES20.glUniform2f(mResolationHandle, 720, 1280);
        GLES20.glUniform2f(mMovementHandle, 0, 0);

//        int textureFront= GLES20.glGetUniformLocation(mProgram,"vTexture2");

//        Log.i("shaopx_debug", "draw: getTextureId_front():"+getTextureId_front());
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0+1);
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, getTextureId_front());
//        GLES20.glUniform1i(textureFront, 1);

//        int mHTexture2Rect= GLES20.glGetUniformLocation(mProgram,"vTexture2Rect");
//        int mHTexture2Size= GLES20.glGetUniformLocation(mProgram,"vTexture2Size");
//        float[] texture2Rect = new float[] { 0.2f, 0.2f, 0.3f, 0.4f };
//        GLES20.glUniform4fv(mHTexture2Rect, 1, texture2Rect, 0);
//        float[] texture2Size = new float[] { 1280.0f, 720.0f };
//        GLES20.glUniform2fv(mHTexture2Size, 1, texture2Size, 0);

        onDraw();
    }
}
