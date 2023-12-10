package com.spx.muticamera.filter;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import com.spx.muticamera.util.EasyGlUtils;
import com.tmf.filter.GLImageBeautyFilter;
import com.tmf.filter.OpenGLUtils;
import com.tmf.filter.TextureRotationUtils;

import java.nio.FloatBuffer;

public class FrontCameraFilter extends BaseFilter {
    static final String TAG = "flutter_FrontCameraFilter1";
    public static final float width_ratio = 116f/360;
    public static final float window_ratio = 16f/9f;
    public static int front_camera_frame_x = 30;
    public static int front_camera_frame_y = 80;
    public static final int front_camera_frame_width = (int) (720 * width_ratio);
//    public static final int front_camera_frame_height = (int) (720 * 0.346 * 1.7777778);
    public static final int front_camera_frame_height = (int) (720 * width_ratio * window_ratio);
//    private Bitmap mBitmap;
    private int mBitmapTextureId;
    private boolean mScaled = false;

    private GLImageBeautyFilter beautyFilter;

    private Context context;

    private int[] fFrame = new int[1];
    private int[] fRender = new int[1];
    private int[] fTexture = new int[1];

    public static final float CubeVertices[] = {
        -1.0f, -1.0f,  // 0 bottom left
        1.0f,  -1.0f,  // 1 bottom right
        -1.0f,  1.0f,  // 2 top left
        1.0f,   1.0f,  // 3 top right
    };

    public static final float TextureVertices[] = {
        0.0f, 0.0f,     // 0 left bottom
        1.0f, 0.0f,     // 1 right bottom
        0.0f, 1.0f,     // 2 left top
        1.0f, 1.0f      // 3 right top
    };
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTextureBuffer;

    public FrontCameraFilter(Context context, Resources mRes) {
        super(mRes);
        this.context = context;
        // 加载图片
//        mBitmap = BitmapFactory.decodeResource(mRes, R.drawable.ic_camera_control_bg);
//        mBitmap = BitmapFactory.decodeResource(mRes, R.drawable.vw_ic_uncheck);
        int screenWidth = mRes.getDisplayMetrics().widthPixels;
        front_camera_frame_x = 720 * 16 / 360;
        front_camera_frame_y = 720 * 80 / 360;
        mVertexBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.CubeVertices);
        mTextureBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.TextureVertices);
    }

    int vAlphaTextureLocation;
    int mResolationHandle;
    int mMovementHandle;
//    private int[] textures=new int[1];
    @Override
    protected void onCreate() {
        createProgramByAssetsFile("shader/front_camera_vertex.glsl", "shader/front_camera_fragment.glsl");
        beautyFilter = new GLImageBeautyFilter(context);

//        GLES20.glUniform2f(vTextureSizeLocation, 720, 720);
//        vcornerRadiusLocation= GLES20.glGetUniformLocation(mProgram,"cornerRadius");
//        GLES20.glUniform1f(vcornerRadiusLocation, 10);
//        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
//
//        // 创建纹理
//        int[] textures = new int[1];
//        GLES20.glGenTextures(1, textures, 0);
//        mBitmapTextureId = textures[0];
//
//        // 绑定纹理
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBitmapTextureId);
//
//        // 设置过滤方式和环绕方式
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//
//        // 绑定图片到纹理
//        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);

//        GLES20.glGenTextures(1,textures,0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textures[0]);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

// 加载图像数据到纹理对象
//        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);

        vAlphaTextureLocation= GLES20.glGetUniformLocation(mProgram,"alphaMaskTexture");
        mResolationHandle = GLES20.glGetUniformLocation(mProgram, "u_resolution");
        mMovementHandle = GLES20.glGetUniformLocation(mProgram, "u_movement");
//        mBitmap.recycle();
        //对画面进行矩阵旋转
//            MatrixUtils.flip(mFilter.getMatrix(),false,true);
//        setTextureId(textures[0]);

//        deleteFrameBuffer();

    }

    @Override
    public void setMatrix(float[] matrix) {
        super.setMatrix(matrix);
//        MatrixUtils.rotate(matrix,90);//矩阵上下翻转
        Log.i(TAG, "setMatrix: getTextureId_front():" + getTextureId_front());

//        if (getFlag() == 0) {
//            MatrixUtils.flip(matrix, true, false);//矩阵上下翻转
//        } else {
////            MatrixUtils.flip(matrix, false, true);//矩阵上下翻转
//            MatrixUtils.flip(matrix, true, false);//矩阵上下翻转
//        }

//        if (!mScaled) {
//            MatrixUtils.scale(matrix, 1f, window_ratio);
//            mScaled = true;
//        }
    }

    @Override
    public void setFlag(int flag) {
        super.setFlag(flag);
        float[] coord;
        if (getFlag() == 0) {    //前置摄像头 顺时针旋转90,并上下颠倒
            coord = new float[]{
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    0.0f, 0.0f,
                    1.0f, 0.0f,
            };
//            coord = new float[]{
//                0.0f, 0.0f,
//                0.0f, 1.0f,
//                1.0f, 0.0f,
//                1.0f, 1.0f,
//            };
        } else {               //后置摄像头 顺时针旋转90度
//            coord = new float[]{
//                    0.0f, 1.0f,
//                    1.0f, 1.0f,
//                    0.0f, 0.0f,
//                    1.0f, 0.0f,
//            };

//            coord = new float[]{
//                1.0f, 0.0f,
//                0.0f, 0.0f,
//                1.0f, 1.0f,
//                0.0f, 1.0f,
//            };
            coord = new float[]{
                1.0f, 1.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 0.0f,
            };
        }
        mTexBuffer.clear();
        mTexBuffer.put(coord);
        mTexBuffer.position(0);
    }

    @Override
    public int getTextureType() {
        return 0;
    }

    @Override
    protected void onBindTexture() {
        /// 激活纹理单元0 (因为getTextureType()返回的也是0, 所以最终这里激活的就是纹理单元0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + getTextureType());
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, getTextureId_front());
        /// 给着色器中的vTexture设置纹理单元的序号0,  vTexture就会对应到上面这句绑定的GL_TEXTURE_EXTERNAL_OES纹理, 也就是getTextureId_front()
        GLES20.glUniform1i(mHTexture, getTextureType());
        GLES20.glUniform2f(mResolationHandle, front_camera_frame_width, front_camera_frame_height);
        GLES20.glUniform2f(mMovementHandle, front_camera_frame_x, front_camera_frame_y);
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,getTextureId());
//
//        GLES20.glUniform1i(vAlphaTextureLocation,1);
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, getTextureId());
//        GLES20.glUniform1i(vAlphaTextureLocation,1);
//        GLES20.glEnable(GLES20.GL_BLEND);
//        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public int getOutputTexture() {
//        return fTexture[0];
        return ouputTexture;
    }

    int ouputTexture = 0;
    @Override
    public void draw() {
//        super.draw();
//        onClear();
//        beautyFilter.onDisplaySizeChanged(front_camera_frame_width, front_camera_frame_height);
//        beautyFilter.drawFrame(getTextureId(), mVerBuffer, mTexBuffer);

//        GLES20.glViewport(front_camera_frame_x, front_camera_frame_y, front_camera_frame_width, front_camera_frame_height);
        GLES20.glViewport(0, 0, width, height);
        EasyGlUtils.bindFrameTexture(fFrame[0], fTexture[0]);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
            GLES20.GL_RENDERBUFFER, fRender[0]);


        GLES20.glViewport(0, 0, width, height);
        onUseProgram();
        onSetExpandData();
        onBindTexture();
        onDraw();

        EasyGlUtils.unBindFrameBuffer();

//        beautyFilter.onDisplaySizeChanged(front_camera_frame_width, front_camera_frame_height);
        beautyFilter.onDisplaySizeChanged(width, height);
//        ouputTexture = fTexture[0];
        ouputTexture = beautyFilter.drawFrameBuffer(fTexture[0], mVertexBuffer, mTextureBuffer);
//        beautyFilter.drawFrame(getTextureId(), );
//        onSetExpandData();

//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0+getTextureType());
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,getTextureId());
//        GLES20.glUniform1i(mHTexture,getTextureType());
//
//        int textureFront= GLES20.glGetUniformLocation(mProgram,"vTexture2");
//
////        Log.i("shaopx_debug", "draw: getTextureId_front():"+getTextureId_front());
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0+1);
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, getTextureId_front());
//        GLES20.glUniform1i(textureFront, 1);

//        int mHTexture2Rect= GLES20.glGetUniformLocation(mProgram,"vTexture2Rect");
//        int mHTexture2Size= GLES20.glGetUniformLocation(mProgram,"vTexture2Size");
//        float[] texture2Rect = new float[] { 0.2f, 0.2f, 0.3f, 0.4f };
//        GLES20.glUniform4fv(mHTexture2Rect, 1, texture2Rect, 0);
//        float[] texture2Size = new float[] { 1280.0f, 720.0f };
//        GLES20.glUniform2fv(mHTexture2Size, 1, texture2Size, 0);

//        onDraw();
    }

    int width , height;
    @Override
    protected void onSizeChanged(int width, int height) {
        beautyFilter.initFrameBuffer(width, height);
        beautyFilter.onInputSizeChanged(width, height);
        Log.i(TAG, "onSizeChanged: size:"+width+"x"+height);
        this.width = width;
        this.height = height;

        GLES20.glGenFramebuffers(1, fFrame, 0);
        GLES20.glGenRenderbuffers(1, fRender, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, fRender[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
            width, height);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
            GLES20.GL_RENDERBUFFER, fRender[0]);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
        EasyGlUtils.genTexturesWithParameter(1, fTexture, 0, GLES20.GL_RGBA, width, height);
    }
}