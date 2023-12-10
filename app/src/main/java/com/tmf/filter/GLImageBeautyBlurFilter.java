package com.tmf.filter;


import android.content.Context;

/**
 * 美颜用的高斯模糊
 */
class GLImageBeautyBlurFilter extends GLImageGaussianBlurFilter {

  public GLImageBeautyBlurFilter(Context context) {
    this(context, OpenGLUtils.getShaderFromAssets(context, "shader/beauty/vertex_beauty_blur.glsl"),
        OpenGLUtils.getShaderFromAssets(context, "shader/beauty/fragment_beauty_blur.glsl"));
  }

  public GLImageBeautyBlurFilter(Context context, String vertexShader, String fragmentShader) {
    super(context, vertexShader, fragmentShader);
  }

}
