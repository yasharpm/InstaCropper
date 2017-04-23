package com.yashoid.instacropper;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Created by Yashar on 3/8/2017.
 */

public class InstaCropperViewNew extends View {

    public static final float DEFAULT_MINIMUM_RATIO = 4F/5F;
    public static final float DEFAULT_MAXIMUM_RATIO = 1.91F;
    public static final float DEFAULT_RATIO = 1F;

    private static final float MAXIMUM_OVER_SCROLL = 144F;
    private static final float MAXIMUM_OVER_SCALE = 0.7F;

    private static final long SET_BACK_DURATION = 400;

    public interface BitmapCallback {

        void onBitmapReady(Bitmap bitmap);

    }

    private float mMinimumRatio = DEFAULT_MINIMUM_RATIO;
    private float mMaximumRatio = DEFAULT_MAXIMUM_RATIO;
    private float mDefaultRatio = DEFAULT_RATIO;

    private Uri mImageUri = null;
    private int mImageRawWidth;
    private int mImageRawHeight;

    private MakeDrawableTask mMakeDrawableTask = null;

    private int mWidth;
    private int mHeight;

    private GridDrawable mGridDrawable = new GridDrawable();

    private Drawable mDrawable = null;

    private float mFocusX;
    private float mFocusY;

    private Rectangle mRectangle = null;
    private RectF mViewBounds = new RectF();
    private Fitness mFitness = new Fitness();
    private Fix mHelperFix = new Fix();

    private RectF mHelperRect = new RectF();

    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;

    private float mMaximumOverScroll;

    private ValueAnimator mAnimator;

    public InstaCropperViewNew(Context context) {
        super(context);
        initialize(context, null, 0, 0);
    }

    public InstaCropperViewNew(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs, 0, 0);
    }

    public InstaCropperViewNew(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public InstaCropperViewNew(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mGestureDetector = new GestureDetector(context, mOnGestureListener);
        mScaleGestureDetector = new ScaleGestureDetector(context, mOnScaleGestureListener);

        mMaximumOverScroll = getResources().getDisplayMetrics().density * MAXIMUM_OVER_SCROLL;

        mAnimator = new ValueAnimator();
        mAnimator.setDuration(SET_BACK_DURATION);
        mAnimator.setFloatValues(0, 1);
        mAnimator.setInterpolator(new DecelerateInterpolator(0.25F));
        mAnimator.addUpdateListener(mSettleAnimatorUpdateListener);

        mGridDrawable.setCallback(mGridCallback);
    }

    private Drawable.Callback mGridCallback = new Drawable.Callback() {

        @Override
        public void invalidateDrawable(Drawable who) {
            invalidate();
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) { }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) { }

    };

    public void setRatios(float defaultRatio, float minimumRatio, float maximumRatio) {
        mDefaultRatio = defaultRatio;
        mMinimumRatio = minimumRatio;
        mMaximumRatio = maximumRatio;

        if (mAnimator.isRunning()) {
            mAnimator.cancel();
        }

        cancelMakingDrawableProcessIfExists();

        mDrawable = null;

        requestLayout();
    }

    public void setImageUri(Uri uri) {
        cancelMakingDrawableProcessIfExists();

        mImageUri = uri;

        requestLayout();
        invalidate();
    }

    public void setDrawableRotation(float rotation) {
        if (rotation == mRectangle.getRotation()) {
            return;
        }

        // TODO

        invalidate();
    }

    public float getDrawableRotation() {
        return mRectangle.getRotation();
    }

//    public void crop(final int widthSpec, final int heightSpec, final BitmapCallback callback) {
//        if (mImageUri == null) {
//            throw new IllegalStateException("Image uri is not set.");
//        }
//
//        if (mDrawable == null || mAnimator.isRunning()) {
//            postDelayed(new Runnable() {
//
//                @Override
//                public void run() {
//                    crop(widthSpec, heightSpec, callback);
//                }
//
//            }, SET_BACK_DURATION / 2);
//            return;
//        }
//
//        RectF gridBounds = new RectF(mGridDrawable.getBounds());
//        gridBounds.offset(-mDrawableLeft, -mDrawableTop);
//
//        getDisplayDrawableBounds(mHelperRect);
//
//        float leftRatio = gridBounds.left / mHelperRect.width();
//        float topRatio = gridBounds.top / mHelperRect.height();
//        float rightRatio = gridBounds.right / mHelperRect.width();
//        float bottomRatio = gridBounds.bottom / mHelperRect.height();
//
//        final int actualLeft = Math.max(0, (int) (leftRatio * mImageRawWidth));
//        final int actualTop = Math.max(0, (int) (topRatio * mImageRawHeight));
//        final int actualRight = Math.min(mImageRawWidth, (int) (rightRatio * mImageRawWidth));
//        final int actualBottom = Math.min(mImageRawHeight, (int) (bottomRatio * mImageRawHeight));
//
//        final Context context = getContext();
//
//        new AsyncTask<Void, Void, Bitmap>() {
//
//            @Override
//            protected Bitmap doInBackground(Void... params) {
//                int actualWidth = actualRight - actualLeft;
//                int actualHeight = actualBottom - actualTop;
//                float actualRatio = (float) actualWidth / (float) actualHeight;
//
//                if (actualRatio < mMinimumRatio) {
//                    actualRatio = mMinimumRatio;
//                }
//
//                if (actualRatio > mMaximumRatio) {
//                    actualRatio = mMaximumRatio;
//                }
//
//                int widthMode = MeasureSpec.getMode(widthSpec);
//                int widthSize = MeasureSpec.getSize(widthSpec);
//                int heightMode = MeasureSpec.getMode(heightSpec);
//                int heightSize = MeasureSpec.getSize(heightSpec);
//
//                int targetWidth = actualWidth;
//                int targetHeight = actualHeight;
//
//                switch (widthMode) {
//                    case MeasureSpec.EXACTLY:
//                        targetWidth = widthSize;
//
//                        switch (heightMode) {
//                            case MeasureSpec.EXACTLY:
//                                targetHeight = heightSize;
//                                break;
//                            case MeasureSpec.AT_MOST:
//                                targetHeight = Math.min(heightSize, (int) (targetWidth / actualRatio));
//                                break;
//                            case MeasureSpec.UNSPECIFIED:
//                                targetHeight = (int) (targetWidth / actualRatio);
//                                break;
//                        }
//                        break;
//                    case MeasureSpec.AT_MOST:
//                        switch (heightMode) {
//                            case MeasureSpec.EXACTLY:
//                                targetHeight = heightSize;
//                                targetWidth = Math.min(widthSize, (int) (targetHeight * actualRatio));
//                                break;
//                            case MeasureSpec.AT_MOST:
//                                if (actualWidth <= widthSize && actualHeight <= heightSize) {
//                                    targetWidth = actualWidth;
//                                    targetHeight = actualHeight;
//                                }
//                                else {
//                                    float specRatio = (float) widthSize / (float) heightSize;
//
//                                    if (specRatio == actualRatio) {
//                                        targetWidth = widthSize;
//                                        targetHeight = heightSize;
//                                    }
//                                    else if (specRatio > actualRatio) {
//                                        targetHeight = heightSize;
//                                        targetWidth = (int) (targetHeight * actualRatio);
//                                    }
//                                    else {
//                                        targetWidth = widthSize;
//                                        targetHeight = (int) (targetWidth / actualRatio);
//                                    }
//                                }
//                                break;
//                            case MeasureSpec.UNSPECIFIED:
//                                if (actualWidth <= widthSize) {
//                                    targetWidth = actualWidth;
//                                    targetHeight = actualHeight;
//                                }
//                                else {
//                                    targetWidth = widthSize;
//                                    targetHeight = (int) (targetWidth / actualRatio);
//                                }
//                                break;
//                        }
//                        break;
//                    case MeasureSpec.UNSPECIFIED:
//                        switch (heightMode) {
//                            case MeasureSpec.EXACTLY:
//                                targetHeight = heightSize;
//                                targetWidth = (int) (targetHeight * actualRatio);
//                                break;
//                            case MeasureSpec.AT_MOST:
//                                if (actualHeight <= heightSize) {
//                                    targetHeight = actualHeight;
//                                    targetWidth = actualWidth;
//                                }
//                                else {
//                                    targetHeight = heightSize;
//                                    targetWidth = (int) (targetHeight * actualRatio);
//                                }
//                                break;
//                            case MeasureSpec.UNSPECIFIED:
//                                targetWidth = actualWidth;
//                                targetHeight = actualHeight;
//                                break;
//                        }
//                        break;
//                }
//
//                return cropImageAndResize(context, actualLeft, actualTop, actualRight, actualBottom, targetWidth, targetHeight);
//            }
//
//            @Override
//            protected void onPostExecute(Bitmap bitmap) {
//                callback.onBitmapReady(bitmap);
//            }
//
//        }.execute();
//    }
//
//    private Bitmap cropImageAndResize(Context context, int left, int top, int right, int bottom, int width, int height) {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inSampleSize = 1;
//
//        int rawArea = (right - left) * (bottom - top);
//        int targetArea = width * height;
//
//        int resultArea = rawArea;
//
//        while (resultArea > targetArea) {
//            options.inSampleSize *= 2;
//            resultArea = rawArea / (options.inSampleSize * options.inSampleSize) ;
//        }
//
//        if (options.inSampleSize > 1) {
//            options.inSampleSize /= 2;
//        }
//
//        try {
//            Bitmap rawBitmap = MakeDrawableTask.getBitmap(context, mImageUri, options);
//
//            if (rawBitmap == null) {
//                return null;
//            }
//
//            left /= options.inSampleSize;
//            top /= options.inSampleSize;
//            right /= options.inSampleSize;
//            bottom /= options.inSampleSize;
//
//            int croppedWidth = right - left;
//            int croppedHeight = bottom - top;
//
//            Bitmap croppedBitmap = Bitmap.createBitmap(rawBitmap, left, top, croppedWidth, croppedHeight);
//
//            if (rawBitmap != croppedBitmap) {
//                rawBitmap.recycle();
//            }
//
//            if (croppedWidth <= width && croppedHeight <= height) {
//                return croppedBitmap;
//            }
//
//            Bitmap resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, width, height, false);
//
//            croppedBitmap.recycle();
//
//            return resizedBitmap;
//        } catch (Throwable t) {
//            return null;
//        }
//    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int targetWidth = 1;
        int targetHeight = 1;

        switch (widthMode) {
            case MeasureSpec.EXACTLY:
                targetWidth = widthSize;

                switch (heightMode) {
                    case MeasureSpec.EXACTLY:
                        targetHeight = heightSize;
                        break;
                    case MeasureSpec.AT_MOST:
                        targetHeight = Math.min(heightSize, (int) (targetWidth / mDefaultRatio));
                        break;
                    case MeasureSpec.UNSPECIFIED:
                        targetHeight = (int) (targetWidth / mDefaultRatio);
                        break;
                }
                break;
            case MeasureSpec.AT_MOST:
                switch (heightMode) {
                    case MeasureSpec.EXACTLY:
                        targetHeight = heightSize;
                        targetWidth = Math.min(widthSize, (int) (targetHeight * mDefaultRatio));
                        break;
                    case MeasureSpec.AT_MOST:
                        float specRatio = (float) widthSize / (float) heightSize;

                        if (specRatio == mDefaultRatio) {
                            targetWidth = widthSize;
                            targetHeight = heightSize;
                        }
                        else if (specRatio > mDefaultRatio) {
                            targetHeight = heightSize;
                            targetWidth = (int) (targetHeight * mDefaultRatio);
                        }
                        else {
                            targetWidth = widthSize;
                            targetHeight = (int) (targetWidth / mDefaultRatio);
                        }
                        break;
                    case MeasureSpec.UNSPECIFIED:
                        targetWidth = widthSize;
                        targetHeight = (int) (targetWidth / mDefaultRatio);
                        break;
                }
                break;
            case MeasureSpec.UNSPECIFIED:
                switch (heightMode) {
                    case MeasureSpec.EXACTLY:
                        targetHeight = heightSize;
                        targetWidth = (int) (targetHeight * mDefaultRatio);
                        break;
                    case MeasureSpec.AT_MOST:
                        targetHeight = heightSize;
                        targetWidth = (int) (targetHeight * mDefaultRatio);
                        break;
                    case MeasureSpec.UNSPECIFIED:
                        targetWidth = (int) mMaximumOverScroll;
                        targetHeight = (int) mMaximumOverScroll;
                        break;
                }
                break;
        }

        setMeasuredDimension(targetWidth, targetHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mWidth = right - left;
        mHeight = bottom - top;

        if (mWidth == 0 || mHeight == 0) {
            return;
        }

        if (mImageUri == null) {
            return;
        }

        mViewBounds.set(0, 0, mWidth, mHeight);

        if (currentDrawableIsSuitableForView()) {
            cancelMakingDrawableProcessIfExists();
            return;
        }

        if (isMakingDrawableForView()) {
            if (drawableBeingMadeIsSuitableForView()) {
                return;
            }

            cancelMakingDrawableProcessIfExists();
        }

        startMakingSuitableDrawable();
    }

    private boolean currentDrawableIsSuitableForView() {
        if (mDrawable == null) {
            return false;
        }

        int drawableWidth = mDrawable.getIntrinsicWidth();
        int drawableHeight = mDrawable.getIntrinsicHeight();

        return isSizeSuitableForView(drawableWidth, drawableHeight);
    }

    private void cancelMakingDrawableProcessIfExists() {
        if (mMakeDrawableTask != null) {
            mMakeDrawableTask.cancel(true);
            mMakeDrawableTask = null;
        }
    }

    private boolean isMakingDrawableForView() {
        return mMakeDrawableTask != null;
    }

    private boolean drawableBeingMadeIsSuitableForView() {
        return isSizeSuitableForView(mMakeDrawableTask.getTargetWidth(), mMakeDrawableTask.getTargetHeight());
    }

    private boolean isSizeSuitableForView(int width, int height) {
        int viewArea = mWidth * mHeight;
        int drawableArea = width * height;

        float areaRatio = (float) viewArea / (float) drawableArea;

        return areaRatio >= 0.5F && areaRatio <= 2F;
    }

    private void startMakingSuitableDrawable() {
        mMakeDrawableTask = new MakeDrawableTask(getContext(), mImageUri, mWidth, mHeight) {

            @Override
            protected void onPostExecute(Drawable drawable) {
                mDrawable = drawable;

                mImageRawWidth = getRawWidth();
                mImageRawHeight = getRawHeight();

                onDrawableChanged();
            }

        };

        mMakeDrawableTask.execute();
    }

    private void onDrawableChanged() {
        reset();
    }

    private void reset() {
        if (mAnimator.isRunning()) {
            mAnimator.cancel();
        }

        mRectangle = new Rectangle(mImageRawWidth, mImageRawHeight, mViewBounds.centerX(), mViewBounds.centerY());
        mRectangle.getFitness(mViewBounds, mFitness);
        mFitness.getFittingFix(mHelperFix);
        mHelperFix.apply(mRectangle, mViewBounds, mMinimumRatio, mMaximumRatio);

        updateGrid();

        invalidate();
    }

    private void updateGrid() {
        mHelperRect.set(mViewBounds);
        mHelperRect.intersect(0, 0, mWidth, mHeight);

        setGridBounds(mHelperRect);

        invalidate();
    }

    private void setGridBounds(RectF bounds) {
        mGridDrawable.setBounds((int) bounds.left, (int) bounds.top, (int) bounds.right, (int) bounds.bottom);

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mDrawable == null) {
            return;
        }

        canvas.save();

        canvas.translate(- mRectangle.getCenterX(), - mRectangle.getCenterY());

        mGridDrawable.draw(canvas);

        canvas.scale(mRectangle.getScale(), mRectangle.getScale(), 0, 0);
        canvas.rotate(mRectangle.getRotation(), 0, 0);

        mDrawable.setBounds(0, 0, (int) mRectangle.getWidth(), (int) mRectangle.getHeight());
        mDrawable.draw(canvas);

        canvas.restore();

        Log.d("AAA", "raw: " + mImageRawWidth + ", " + mImageRawHeight + " now: " + mRectangle.getWidth() + ", " + mRectangle.getHeight());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mDrawable == null) {
            return false;
        }

        mGestureDetector.onTouchEvent(event);
        mScaleGestureDetector.onTouchEvent(event);

        int action = event.getAction();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_OUTSIDE) {
            mAnimator.start();
        }

        return true;
    }

    private GestureDetector.OnGestureListener mOnGestureListener = new GestureDetector.OnGestureListener() {

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) { }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            distanceX = - distanceX;
            distanceY = - distanceY;

            mRectangle.getFitness(mViewBounds, mFitness);
            mFitness.getEssentialFix(mHelperFix);

            float overScrollX = mHelperFix.translateX;
            float overScrollY = mHelperFix.translateY;

            distanceX = applyOverScrollFix(distanceX, overScrollX);
            distanceY = applyOverScrollFix(distanceY, overScrollY);

            mRectangle.translateBy(distanceX, distanceY);

            updateGrid();

            invalidate();

            return true;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) { }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            return false;
        }

    };

    private float applyOverScrollFix(float distance, float overScroll) {
        if (overScroll * distance <= 0) {
            return distance;
        }

        float offRatio = Math.abs(overScroll) / mMaximumOverScroll;

        distance -= distance * Math.sqrt(offRatio);

        return distance;
    }

    private ScaleGestureDetector.OnScaleGestureListener mOnScaleGestureListener =
            new ScaleGestureDetector.OnScaleGestureListener() {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float overScale = measureOverScale();
            float scale = applyOverScaleFix(detector.getScaleFactor(), overScale);

            mFocusX = detector.getFocusX();
            mFocusY = detector.getFocusY();

            mRectangle.scaleBy(scale, mFocusX, mFocusY);

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) { }

    };

    private float measureOverScale() {
        mRectangle.getFitness(mViewBounds, mFitness);
        mFitness.getEssentialFix(mHelperFix);

        return measureOverScale(mHelperFix);
    }

    private float measureOverScale(Fix fix) {
        float maximumAllowedScale = getMaximumAllowedScale();
        float minimumAllowedScale = getMinimumAllowedScale();

        float scale = fix.getScale(mRectangle);

        if (scale < minimumAllowedScale) {
            return scale / minimumAllowedScale;
        }
        else if (scale > maximumAllowedScale) {
            return scale / maximumAllowedScale;
        }
        else {
            return 1;
        }
    }

    private float getMaximumAllowedScale() {
        float maximumAllowedWidth = mImageRawWidth;
        float maximumAllowedHeight = mImageRawHeight;
        // TODO
        return Math.min(maximumAllowedWidth / (float) mWidth, maximumAllowedHeight / (float) mHeight);
    }

    private float getMinimumAllowedScale() {
        mRectangle.getFitness(mViewBounds, mFitness);
        mFitness.getFittingFix(mHelperFix);

        return mHelperFix.getScale(mRectangle);
    }

    private float applyOverScaleFix(float scale, float overScale) {
        if (overScale == 1) {
            return scale;
        }

        if (overScale > 1) {
            overScale = 1F / overScale;
        }

        float wentOverScaleRatio = (overScale - MAXIMUM_OVER_SCALE) / (1 - MAXIMUM_OVER_SCALE);

        if (wentOverScaleRatio < 0) {
            wentOverScaleRatio = 0;
        }

        // 1 -> scale , 0 -> 1
        // scale * f(1) = scale
        // scale * f(0) = 1

        // f(1) = 1
        // f(0) = 1/scale

        scale *= wentOverScaleRatio + (1 - wentOverScaleRatio) / scale;

        return scale;
    }

    private ValueAnimator.AnimatorUpdateListener mSettleAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            float animatedValue = (float) animation.getAnimatedValue();

            mRectangle.getFitness(mViewBounds, mFitness);
            mFitness.getEssentialFix(mHelperFix);

            float overScrollX = mHelperFix.translateX;
            float overScrollY = mHelperFix.translateY;
            float overScale = measureOverScale(mHelperFix);

            float translateX = - overScrollX * animatedValue;
            float translateY = - overScrollY * animatedValue;
            mRectangle.translateBy(translateX, translateY);

            float scale = mRectangle.getScale();

            float targetScale = scale / overScale;

            Log.d("AAA", "scale="+scale + " targetScale=" + targetScale);

            float newScale = (1 - animatedValue) * scale + animatedValue * targetScale;
            mRectangle.scaleBy(newScale / scale , mFocusX, mFocusY);

            updateGrid();
            invalidate();
        }

    };

    private static class Fix {

        protected float translateX;
        protected float translateY;

        protected float sizeChangeX;
        protected float sizeChangeY;

        protected Fix() {

        }

        protected void apply(Rectangle rectangle, RectF inside, float minimumRatio, float maximumRatio) {
            rectangle.translateBy(translateX, translateY);

            float rotation = rectangle.getRotation();

            if (rotation == 0 || rotation == 180) {
                float width = rectangle.getWidth();
                float height = rectangle.getHeight();

                float newWidth = width + sizeChangeX;
                float newHeight = height + sizeChangeY;

                // TODO
            }

            float scale = getScale(rectangle);

            rectangle.scaleBy(scale, rectangle.getCenterX(), rectangle.getCenterY());
        }

        protected float getScale(Rectangle rectangle) {
            float width = rectangle.getWidth();
            float height = rectangle.getHeight();
            float ratio = width / height;

            float rotation = rectangle.getRotation();
            double r = rotation / (2 * Math.PI);
            double sin = Math.sin(-r);
            double cos = Math.cos(-r);

            double widthChange = sizeChangeX * cos - sizeChangeY * sin;
            double heightChange = sizeChangeX * sin + sizeChangeY * cos;

            float newWidth = (float) (width + widthChange);
            float newHeight = (float) (height + heightChange);
            float newRatio = newWidth / newHeight;

            if (newRatio < ratio) {
                newHeight = newWidth / ratio;
            }
            else {
                newWidth = newHeight * ratio;
            }

            return newWidth / width;
        }

        @Override
        public String toString() {
            return "dx=" + translateX + " dy=" + translateY + " dSizeX=" + sizeChangeX + " dSizeY=" + sizeChangeY;
        }
    }

    private static class Fitness {

        private RectF mEssentialBias = new RectF();
        private RectF mOptionalBias = new RectF();

        protected Fitness() {

        }

        protected void set(float essentialInPositiveX, float essentialInNegativeX, float essentialInPositiveY, float essentialInNegativeY,
                           float optionalInPositiveX, float optionalInNegativeX, float optionalInPositiveY, float optionalInNegativeY) {
            mEssentialBias.set(essentialInNegativeX, essentialInNegativeY, essentialInPositiveX, essentialInPositiveY);
            mOptionalBias.set(optionalInNegativeX, optionalInNegativeY, optionalInPositiveX, optionalInPositiveY);

            Log.d("AAA", "fitness set. " + toString());
        }

        protected void getFittingFix(Fix fix) {
            fix.translateX = mEssentialBias.centerX() + mOptionalBias.centerX();
            fix.translateY = mEssentialBias.centerY() + mOptionalBias.centerY();

            fix.sizeChangeX = mEssentialBias.width() - mOptionalBias.width();
            fix.sizeChangeY = mEssentialBias.height() - mOptionalBias.height();

            Log.d("AAA", "Fitting fix is: " + fix);
        }

        protected void getEssentialFix(Fix fix) {
            if (mOptionalBias.left >= mEssentialBias.left && mOptionalBias.right <= mEssentialBias.right) {
                fix.translateX = mEssentialBias.centerX();
                fix.sizeChangeX = mEssentialBias.width();
            }
            else if (mOptionalBias.left <= mEssentialBias.left && mOptionalBias.right >= mEssentialBias.right) {
                fix.translateX = 0;
                fix.sizeChangeX = 0;
            }
            else if (mEssentialBias.left < mOptionalBias.left) {
                fix.translateX = mEssentialBias.left;
                fix.sizeChangeX = Math.max(0, mEssentialBias.right - mOptionalBias.right);
            }
            else if (mEssentialBias.right > mOptionalBias.right) {
                fix.translateX = mEssentialBias.right;
                fix.sizeChangeX = Math.max(0, mOptionalBias.left - mEssentialBias.left);
            }

            if (mOptionalBias.top >= mEssentialBias.top && mOptionalBias.bottom <= mEssentialBias.bottom) {
                fix.translateY = mEssentialBias.centerY();
                fix.sizeChangeY = mEssentialBias.height();
            }
            else if (mOptionalBias.top <= mEssentialBias.top && mOptionalBias.bottom >= mEssentialBias.bottom) {
                fix.translateY = 0;
                fix.sizeChangeY = 0;
            }
            else if (mEssentialBias.top < mOptionalBias.top) {
                fix.translateY = mEssentialBias.top;
                fix.sizeChangeY = Math.max(0, mEssentialBias.bottom - mOptionalBias.bottom);
            }
            else if (mEssentialBias.bottom > mOptionalBias.bottom) {
                fix.translateY = mEssentialBias.bottom;
                fix.sizeChangeY = Math.max(0, mOptionalBias.top - mEssentialBias.top);
            }
        }

        @Override
        public String toString() {
            return "Essential bias: " + mEssentialBias.toString() + " Optional bias: " + mOptionalBias.toString();
        }

    }

    private static class Rectangle {

        private float mWidth;
        private float mHeight;

        private float mCenterX;
        private float mCenterY;

        private float mScale = 1;
        private float mRotation = 0;

        private Line[] mLines;

        protected Rectangle(float width, float height, float centerX, float centerY) {
            mWidth = width;
            mHeight = height;

            mCenterX = centerX;
            mCenterY = centerY;

            mLines = new Line[4];

            mLines[0] = new Line(centerX - width/2, centerY - height/2, 1, 0);
            mLines[1] = new Line(centerX - width/2, centerY - height/2, 0, 1);
            mLines[2] = new Line(centerX + width/2, centerY + height/2, -1, 0);
            mLines[3] = new Line(centerX + width/2, centerY + height/2, 0, -1);
        }

        protected float getWidth() {
            return mWidth;
        }

        protected float getHeight() {
            return mHeight;
        }

        protected float getCenterX() {
            return mCenterX;
        }

        protected float getCenterY() {
            return mCenterY;
        }

        protected float getScale() {
            return mScale;
        }

        protected void getFitness(RectF bounds, Fitness fitness) {
            float essentialInPositiveX = 0;
            float essentialInNegativeX = 0;
            float essentialInPositiveY = 0;
            float essentialInNegativeY = 0;

            float optionalInPositiveX = 0;
            float optionalInNegativeX = 0;
            float optionalInPositiveY = 0;
            float optionalInNegativeY = 0;

            for (Line line: mLines) {
                float lineFitness = line.getFitness(bounds);
//                Log.d("AAA", "Line fitness: " + lineFitness);

                boolean isEssential = lineFitness < 0;

                float dx = lineFitness * line.directionX;
                float dy = lineFitness * line.directionY;

                if (isEssential) {
                    if (dx > 0) {
                        essentialInPositiveX = Math.max(essentialInPositiveX, dx);
                    }
                    else {
                        essentialInNegativeX = Math.min(essentialInNegativeX, dx);
                    }

                    if (dy > 0) {
                        essentialInPositiveY = Math.max(essentialInPositiveY, dy);
                    }
                    else {
                        essentialInNegativeY = Math.min(essentialInNegativeY, dy);
                    }
                }
                else {
                    if (dx > 0) {
                        optionalInPositiveX = Math.min(optionalInPositiveX, dx);
                    }
                    else {
                        optionalInNegativeX = Math.max(optionalInNegativeX, dx);
                    }

                    if (dy > 0) {
                        optionalInPositiveY = Math.min(optionalInPositiveY, dy);
                    }
                    else {
                        optionalInNegativeY = Math.max(optionalInNegativeY, dy);
                    }
                }
            }

            fitness.set(essentialInPositiveX, essentialInNegativeX, essentialInPositiveY, essentialInNegativeY,
                    optionalInPositiveX, optionalInNegativeX, optionalInPositiveY, optionalInNegativeY);
        }

        private float getRotation() {
            return mRotation;
        }

        private void updateRotation() {
            while (mRotation >= 360) {
                mRotation -= 360;
            }

            while (mRotation < 0) {
                mRotation += 360;
            }
        }

//        protected void rotateBy(float degrees) {
//            mRotation += degrees;
//            updateRotation();
//
//            double r = degrees / (2 * Math.PI);
//
//            double sin = Math.sin(r);
//            double cos = Math.cos(r);
//
//            for (Line line: mLines) {
//                line.rotateBy(sin, cos);
//            }
//        }

        protected void translateBy(float dx, float dy) {
            mCenterX += dx;
            mCenterY += dy;

            for (Line line: mLines) {
                line.translateBy(dx, dy);
            }
        }

        protected void scaleBy(float scale, float pivotX, float pivotY) {
            mScale *= scale;

            mWidth *= scale;
            mHeight *= scale;

            for (Line line: mLines) {
                float fitness = line.getFitness(pivotX, pivotY);

                float translateX = (scale - 1) * fitness * line.directionX;
                float translateY = (scale - 1) * fitness * line.directionY;

                line.translateBy(translateX, translateY);
            }

            calcCenter();
        }

        private void calcCenter() {
            float sumX = 0;
            float sumY = 0;

            for (Line line: mLines) {
                sumX += line.x;
                sumY += line.y;
            }

            mCenterX = sumX / mLines.length;
            mCenterY = sumY / mLines.length;
        }

    }

    private static class Line {

        private float x;
        private float y;
        private float directionX;
        private float directionY;

        protected Line(float x, float y, float directionX, float directionY) {
            this.x = x;
            this.y = y;
            this.directionX = directionX;
            this.directionY = directionY;
        }

        protected float getFitness(RectF bounds) {
            float lt = getFitness(bounds.left, bounds.top);
            float rt = getFitness(bounds.right, bounds.top);
            float rb = getFitness(bounds.right, bounds.bottom);
            float lb = getFitness(bounds.left, bounds.bottom);

            return Math.min(Math.min(lt, rt), Math.min(rb, lb));
        }

        private float getFitness(float pointX, float pointY) {
            // x = x - dy*t , y = y + dx*t
            // x = pointX + dx*q , y = pointY + dy*q

            float q = directionX*(x - pointX) + directionY*(y - pointY);

            float crossX = pointX + directionX * q;
            float crossY = pointY + directionY * q;

            float distance = PointF.length(crossX - pointX, crossY - pointY);

            return - Math.signum(q) * distance;
        }

        protected void rotateBy(double sin, double cos) {
            double newDirectionX = directionX * cos - directionY * sin;
            double newDirectionY = directionX * sin + directionY * cos;

            directionX = (float) newDirectionX;
            directionY = (float) newDirectionY;
        }

        protected void translateBy(float dx, float dy) {
            x += dx;
            y += dy;
        }

    }

}
