package com.spx.muticamera.filter.beauty;

import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLES20;
import android.util.Log;

import com.spx.muticamera.filter.BaseFilter;


public class SmallWindowFilter extends BaseFilter {
    static final String TAG = "flutter_SmallWindowFilter";
    public static final float width_ratio = 200f / 360;
    public static final float window_ratio = 16f / 9f;
    public static int front_camera_frame_x = 30;
    public static int front_camera_frame_y = 30;
    public static final int front_camera_frame_width = (int) (720 * width_ratio);
    //    public static final int front_camera_frame_height = (int) (720 * 0.346 * 1.7777778);
    public static final int front_camera_frame_height = (int) (720 * width_ratio * window_ratio);
    //    private Bitmap mBitmap;
    private int mBitmapTextureId;
    private boolean mScaled = false;

//  private GLImageBeautyFilter beautyFilter;

    private Context context;

    public SmallWindowFilter(Context context, Resources mRes) {
        super(mRes);
        this.context = context;
        // 加载图片
//        mBitmap = BitmapFactory.decodeResource(mRes, R.drawable.ic_camera_control_bg);
//        mBitmap = BitmapFactory.decodeResource(mRes, R.drawable.vw_ic_uncheck);
        int screenWidth = mRes.getDisplayMetrics().widthPixels;
        front_camera_frame_x = 720 * 16 / 360;
        front_camera_frame_y = 720 * 80 / 360;
    }

    //  int vAlphaTextureLocation;
    int mResolationHandle;
    int mMovementHandle;
    private int[] textures = new int[1];

    @Override
    protected void onCreate() {
        createProgramByAssetsFile("shader/front_camera_vertex.glsl", "shader/small_window.glsl");


//    vAlphaTextureLocation= GLES20.glGetUniformLocation(mProgram,"alphaMaskTexture");
        mResolationHandle = GLES20.glGetUniformLocation(mProgram, "u_resolution");
        mMovementHandle = GLES20.glGetUniformLocation(mProgram, "u_movement");
//        mBitmap.recycle();
        //对画面进行矩阵旋转
//            MatrixUtils.flip(mFilter.getMatrix(),false,true);
//        setTextureId(textures[0]);
    }

    @Override
    public void setMatrix(float[] matrix) {
        super.setMatrix(matrix);
//        MatrixUtils.rotate(matrix,90);//矩阵上下翻转
        Log.i(TAG, "setMatrix: getTextureId():" + getTextureId());
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
            coord = new float[]{
                    0.0f, 1.0f,
                    0.0f, 0.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f,
            };
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
            throw new RuntimeException("aaaaxxx");
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
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, getTextureId());

        GLES20.glUniform1i(mHTexture, 0);
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
    public void draw() {
//        super.draw();
//        onClear();
//        beautyFilter.onDisplaySizeChanged(front_camera_frame_width, front_camera_frame_height);
//        beautyFilter.drawFrame(getTextureId(), mVerBuffer, mTexBuffer);

        GLES20.glViewport(front_camera_frame_x, front_camera_frame_y, front_camera_frame_width, front_camera_frame_height);
//    GLES20.glViewport(0, 0, width, height);
        onUseProgram();
        onSetExpandData();
        onBindTexture();
        onDraw();
    }

    int width, height;

    @Override
    protected void onSizeChanged(int width, int height) {
//    beautyFilter.onInputSizeChanged(width, height);
        Log.i(TAG, "onSizeChanged: size:" + width + "x" + height);
        this.width = width;
        this.height = height;
    }
}
