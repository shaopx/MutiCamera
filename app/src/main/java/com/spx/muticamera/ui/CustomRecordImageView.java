package com.spx.muticamera.ui;


import static com.spx.muticamera.util.AppUtil.dpToPx;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spx.muticamera.TmfApplication;

public class CustomRecordImageView extends View {

    public interface RecordingCallback {
        void onFinishRecording();

        void onStartRecording();
    }

    public void setRecordingCallback(RecordingCallback finishCallback) {
        this.mRecordingCallback = finishCallback;
    }

    private RecordingCallback mRecordingCallback;

    private static final String TAG = "flutter_CustomRecordImageView";

    public static final long RECORDING_DURATION_PER_TIME = 14800;
    public static final long RECORDING_MAX_TIMES = 1;
    private static final int outter_cycle_big_width = dpToPx(TmfApplication.getContext(), 90);
    private static final int outter_cycle_small_width = dpToPx(TmfApplication.getContext(), 72);
    private static final int inner_fill_cycle_width = dpToPx(TmfApplication.getContext(), 66);

    private static final int STATUS_RECORDING = 0;
    private static final int STATUS_INIT = 1;
    private static final int STATUS_ANIMATION = 2;

    boolean isRecording;
    int cuurStatus = STATUS_INIT;

    public static final int START = 1;
    public static final int STOP = 2;
    public static final int PROCESS = 3;
    private ValueAnimator valueAnimator;
    private ValueAnimator mExpandAnimator;
    private float progressPercentage;
    private long mRecordStartTime = 0;

    private Paint fillPaint;
    private Paint strokePaint;
    private Paint innerRoundRectFillPaint;
    private Paint ringBgFillPaint;
    private Path innerPath;
    private Path ringBgPath;
    private Path ringBgPath2;
    //  private float cornerRadius;
    private float strokeWidth;
    private RectF innerRectF;
    private RectF ringBgRectF;
    private RectF ringBgRectF2;

    private long maxDuration = RECORDING_DURATION_PER_TIME * RECORDING_MAX_TIMES;

    Path partialPath;
    Path whiteRingPath;

    PathMeasure ringPath1Measure;
    PathMeasure ringPath2Measure;

    Path finalPath = new Path();
    float totalLength;
    float path1Length;
    boolean isPathInited = false;

    public CustomRecordImageView(Context context) {
        this(context, null);
    }

    public CustomRecordImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomRecordImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        fillPaint = new Paint();
        fillPaint.setColor(Color.WHITE);
        fillPaint.setStyle(Paint.Style.FILL);

        strokeWidth = dpToPx(10);

        strokePaint = new Paint();
        strokePaint.setColor(Color.WHITE);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidth);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);

        partialPath = new Path();


        innerRoundRectFillPaint = new Paint();
        innerRoundRectFillPaint.setColor(Color.GREEN);
        innerRoundRectFillPaint.setStyle(Paint.Style.FILL);
        ringBgFillPaint = new Paint();
        ringBgFillPaint.setColor(Color.parseColor("#5affffff"));

        innerPath = new Path();
        ringBgPath = new Path();
        ringBgPath2 = new Path();

        innerRectF = new RectF();
        ringBgRectF = new RectF();
        ringBgRectF2 = new RectF();
    }

    public boolean isRecording() {
        return cuurStatus != 1;
    }

    private void initPaths() {
        int width = getWidth();
        int height = getHeight();
        ringBgRectF.set(0, 0, width, height);
        ringBgPath.addRoundRect(ringBgRectF, height / 2, height / 2, Path.Direction.CW);

        float padding = strokeWidth / 2;
        ringBgRectF2.set(padding, padding, width - padding, height - padding);
        ringBgPath2.addRoundRect(ringBgRectF2, (height - padding * 2) / 2, (height - padding * 2) / 2, Path.Direction.CCW);


        innerRectF.set(strokeWidth, strokeWidth, width - strokeWidth, height - strokeWidth);
        innerPath.addRoundRect(innerRectF, (height - strokeWidth * 2) / 2, (height - strokeWidth * 2) / 2, Path.Direction.CW);

        whiteRingPath = new Path();
        // 使用 op() 方法将 path1 减去 path2 的部分，将结果保存到 differencePath 中
        whiteRingPath.op(ringBgPath2, innerPath, Path.Op.DIFFERENCE);
        ringPath2Measure = new PathMeasure(whiteRingPath, false);
        totalLength = ringPath2Measure.getLength();
        isPathInited = true;
        float start = 0;
        if (width > height) {
            start = (float) (((Math.PI - 1f) / 2f * height + width / 2) / (2f * width + (Math.PI - 2f) * height));
            start = totalLength * start;
        } else {
            start = (float) (height * 1f / (2f * height + Math.PI * width / 2));
            start = totalLength * start;
        }

        android.util.Log.i(TAG, "onDraw: totalLength:" + totalLength);
        ringPath2Measure.getSegment(start, totalLength, finalPath, true);
        android.util.Log.i(TAG, "onDraw: finish finalPath!");
        ringPath1Measure = new PathMeasure(finalPath, false);
        path1Length = ringPath1Measure.getLength();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isPathInited) {
            initPaths();
            isPathInited = true;
        }

        int width = getWidth();
        int height = getHeight();
        if (cuurStatus == STATUS_RECORDING) {
            canvas.drawPath(ringBgPath, ringBgFillPaint);

            canvas.drawPath(innerPath, innerRoundRectFillPaint);
            float partialLength = totalLength * progressPercentage / 100;
            partialPath.reset();
            ringPath1Measure.getSegment(0, partialLength, partialPath, true);
            // 绘制部分路径
            canvas.drawPath(partialPath, strokePaint);
            if (partialLength >= path1Length) {
                float secondLength = partialLength - path1Length;
                partialPath.reset();
                ringPath2Measure.getSegment(0, secondLength, partialPath, true);
                // 绘制部分路径
                canvas.drawPath(partialPath, strokePaint);
            }

        } else if (cuurStatus != STATUS_ANIMATION) {
            innerRectF.set(strokeWidth, strokeWidth, width - strokeWidth, height - strokeWidth);
            innerPath.addRoundRect(innerRectF, (height - strokeWidth * 2) / 2, (height - strokeWidth * 2) / 2, Path.Direction.CW);
            canvas.drawPath(innerPath, innerRoundRectFillPaint);
        } else {

        }
    }

    public boolean getRecordStatus() {
        return isRecording;
    }

    public void startRecord() {
        Log.i(TAG, "startRecord: ...");
        isRecording = true;
        cuurStatus = STATUS_ANIMATION;
        startExpandAnimation();
    }

    private void startExpandAnimation() {
        if (mExpandAnimator != null) {
            mExpandAnimator.removeAllUpdateListeners();
            mExpandAnimator.cancel();
        }

        mExpandAnimator = ValueAnimator.ofFloat(0.0f, outter_cycle_big_width - outter_cycle_small_width);
        mExpandAnimator.setDuration(250); // 设置动画时长为 300 毫秒
//        mExpandAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animator) {
//                float animatedValue = (float) animator.getAnimatedValue();
//                mOutterRingRadius = (int) (outter_cycle_small_width/2 + animatedValue/2);
//                Log.i(TAG, "onAnimationUpdate: mOutterRingRadius:"+mOutterRingRadius);
//                invalidate();
//            }
//        });
        mExpandAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animation) {
                if (mRecordingCallback != null) {
                    mRecordingCallback.onStartRecording();
                }
            }

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                Log.i(TAG, "expand animation end.");
                startGapAnimation();
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {

            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {

            }
        });
        mExpandAnimator.start();
    }

    private void startGapAnimation() {
        Log.i(TAG, "start gap animation ....");
        mRecordStartTime = System.currentTimeMillis();
        cuurStatus = STATUS_RECORDING;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        valueAnimator = ValueAnimator.ofFloat(0, 100);
        valueAnimator.setDuration(14700);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.setRepeatCount(ValueAnimator.INFINITE); // 设置循环模式为无限循环
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // 监听动画数值的变化，并更新进度百分比
                float value = (float) animation.getAnimatedValue();
                progressPercentage = value;
//        Log.i(TAG, "onAnimationUpdate: progressPercentage:"+progressPercentage);
                invalidate();

                if (System.currentTimeMillis() - mRecordStartTime > maxDuration
                        && mRecordingCallback != null) {
                    mRecordingCallback.onFinishRecording();
                }
            }
        });
        valueAnimator.start();
    }

    public void stopRecord() {
        Log.i(TAG, "stopRecord: ...");
        isRecording = false;
        cuurStatus = STATUS_INIT;
        if (mExpandAnimator != null) {
            mExpandAnimator.cancel();
        }
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        invalidate();
    }

}
