package com.yashoid.instacropper;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.animation.LinearInterpolator;

/**
 * Created by Yashar on 3/8/2017.
 */

public class GridDrawable extends Drawable {

    private static final int LINE_COLOR = Color.WHITE;
    private static final int LINE_BORDER_COLOR = 0x44888888;
    private static final float LINE_STROKE_WIDTH = 1;
    private static final long TIME_BEFORE_FADE = 300;
    private static final long TIME_TO_FADE = 300;

    private Handler mHandler;

    private Rect mPreviousBounds = new Rect();

    private Paint mLinePaint;
    private Paint mLineBorderPaint;

    private ValueAnimator mAnimator = new ValueAnimator();

    private float mAlpha = 1;

    protected GridDrawable() {
        mHandler = new Handler();

        mLinePaint = new Paint();
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setColor(LINE_COLOR);
        mLinePaint.setStrokeWidth(LINE_STROKE_WIDTH);

        mLineBorderPaint = new Paint();
        mLineBorderPaint.setStyle(Paint.Style.STROKE);
        mLineBorderPaint.setColor(LINE_BORDER_COLOR);
        mLineBorderPaint.setStrokeWidth(LINE_STROKE_WIDTH);

        mAnimator.setDuration(TIME_TO_FADE);
        mAnimator.setStartDelay(TIME_BEFORE_FADE);
        mAnimator.setFloatValues(1, 0);
        mAnimator.addUpdateListener(mAnimatorUpdateListener);
        mAnimator.setInterpolator(new LinearInterpolator());

        mAnimator.start();
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);

        mAlpha = 1;
        invalidateSelf();

        mAnimator.cancel();
        mAnimator.start();
    }

    @Override
    public void draw(Canvas canvas) {
        mLinePaint.setAlpha(Math.round(mAlpha*255));
        mLineBorderPaint.setAlpha(Math.round(mAlpha*0x44));

        Rect bounds = getBounds();

        int width = bounds.width();
        int height = bounds.height();

        int left = bounds.left + width / 3;
        int right = left + width / 3;
        int top = bounds.top + height / 3;
        int bottom = top + height / 3;

        canvas.drawLine(left - 1, bounds.top, left - 1, bounds.bottom, mLineBorderPaint);
        canvas.drawLine(left + 1, bounds.top, left + 1, bounds.bottom, mLineBorderPaint);

        canvas.drawLine(right - 1, bounds.top, right - 1, bounds.bottom, mLineBorderPaint);
        canvas.drawLine(right + 1, bounds.top, right + 1, bounds.bottom, mLineBorderPaint);

        canvas.drawLine(bounds.left, top - 1, bounds.right, top - 1, mLineBorderPaint);
        canvas.drawLine(bounds.left, top + 1, bounds.right, top + 1, mLineBorderPaint);

        canvas.drawLine(bounds.left, bottom - 1, bounds.right, bottom - 1, mLineBorderPaint);
        canvas.drawLine(bounds.left, bottom + 1, bounds.right, bottom + 1, mLineBorderPaint);

        canvas.drawLine(left, bounds.top, left, bounds.bottom, mLinePaint);
        canvas.drawLine(right, bounds.top, right, bounds.bottom, mLinePaint);
        canvas.drawLine(bounds.left, top, bounds.right, top, mLinePaint);
        canvas.drawLine(bounds.left, bottom, bounds.right, bottom, mLinePaint);
    }

    @Override
    public void setAlpha(int alpha) { }

    @Override
    public void setColorFilter(ColorFilter colorFilter) { }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    private ValueAnimator.AnimatorUpdateListener mAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mAlpha = (float) animation.getAnimatedValue();

            invalidateSelf();
        }

    };

}
