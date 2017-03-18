package com.yashoid.instacropper;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.io.FileNotFoundException;

/**
 * Created by Yashar on 3/8/2017.
 */

public class InstaCropperView extends View {

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

    private float mDrawableScale;
    private float mScaleFocusX;
    private float mScaleFocusY;

    private float mDisplayDrawableLeft;
    private float mDisplayDrawableTop;

    private RectF mHelperRect = new RectF();

    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;

    private float mMaximumOverScroll;

    private ValueAnimator mAnimator;

    public InstaCropperView(Context context) {
        super(context);
        initialize(context, null, 0, 0);
    }

    public InstaCropperView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs, 0, 0);
    }

    public InstaCropperView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public InstaCropperView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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
        mDrawable = null;

        requestLayout();
        invalidate();
    }

    public void crop(final int widthSpec, final int heightSpec, final BitmapCallback callback) {
        if (mImageUri == null) {
            throw new IllegalStateException("Image uri is not set.");
        }

        if (mDrawable == null || mAnimator.isRunning()) {
            postDelayed(new Runnable() {

                @Override
                public void run() {
                    crop(widthSpec, heightSpec, callback);
                }

            }, SET_BACK_DURATION / 2);
            return;
        }

        RectF gridBounds = new RectF(mGridDrawable.getBounds());
        gridBounds.offset(-mDisplayDrawableLeft, -mDisplayDrawableTop);

        getDisplayDrawableBounds(mHelperRect);

        float leftRatio = gridBounds.left / mHelperRect.width();
        float topRatio = gridBounds.top / mHelperRect.height();
        float rightRatio = gridBounds.right / mHelperRect.width();
        float bottomRatio = gridBounds.bottom / mHelperRect.height();

        final int actualLeft = Math.max(0, (int) (leftRatio * mImageRawWidth));
        final int actualTop = Math.max(0, (int) (topRatio * mImageRawHeight));
        final int actualRight = Math.min(mImageRawWidth, (int) (rightRatio * mImageRawWidth));
        final int actualBottom = Math.min(mImageRawHeight, (int) (bottomRatio * mImageRawHeight));

        final Context context = getContext();

        new AsyncTask<Void, Void, Bitmap>() {

            @Override
            protected Bitmap doInBackground(Void... params) {
                int actualWidth = actualRight - actualLeft;
                int actualHeight = actualBottom - actualTop;
                float actualRatio = (float) actualWidth / (float) actualHeight;

                if (actualRatio < mMinimumRatio) {
                    actualRatio = mMinimumRatio;
                }

                if (actualRatio > mMaximumRatio) {
                    actualRatio = mMaximumRatio;
                }

                int widthMode = MeasureSpec.getMode(widthSpec);
                int widthSize = MeasureSpec.getSize(widthSpec);
                int heightMode = MeasureSpec.getMode(heightSpec);
                int heightSize = MeasureSpec.getSize(heightSpec);

                int targetWidth = actualWidth;
                int targetHeight = actualHeight;

                switch (widthMode) {
                    case MeasureSpec.EXACTLY:
                        targetWidth = widthSize;

                        switch (heightMode) {
                            case MeasureSpec.EXACTLY:
                                targetHeight = heightSize;
                                break;
                            case MeasureSpec.AT_MOST:
                                targetHeight = Math.min(heightSize, (int) (targetWidth / actualRatio));
                                break;
                            case MeasureSpec.UNSPECIFIED:
                                targetHeight = (int) (targetWidth / actualRatio);
                                break;
                        }
                        break;
                    case MeasureSpec.AT_MOST:
                        switch (heightMode) {
                            case MeasureSpec.EXACTLY:
                                targetHeight = heightSize;
                                targetWidth = Math.min(widthSize, (int) (targetHeight * actualRatio));
                                break;
                            case MeasureSpec.AT_MOST:
                                if (actualWidth <= widthSize && actualHeight <= heightSize) {
                                    targetWidth = actualWidth;
                                    targetHeight = actualHeight;
                                }
                                else {
                                    float specRatio = (float) widthSize / (float) heightSize;

                                    if (specRatio == actualRatio) {
                                        targetWidth = widthSize;
                                        targetHeight = heightSize;
                                    }
                                    else if (specRatio > actualRatio) {
                                        targetHeight = heightSize;
                                        targetWidth = (int) (targetHeight * actualRatio);
                                    }
                                    else {
                                        targetWidth = widthSize;
                                        targetHeight = (int) (targetWidth / actualRatio);
                                    }
                                }
                                break;
                            case MeasureSpec.UNSPECIFIED:
                                if (actualWidth <= widthSize) {
                                    targetWidth = actualWidth;
                                    targetHeight = actualHeight;
                                }
                                else {
                                    targetWidth = widthSize;
                                    targetHeight = (int) (targetWidth / actualRatio);
                                }
                                break;
                        }
                        break;
                    case MeasureSpec.UNSPECIFIED:
                        switch (heightMode) {
                            case MeasureSpec.EXACTLY:
                                targetHeight = heightSize;
                                targetWidth = (int) (targetHeight * actualRatio);
                                break;
                            case MeasureSpec.AT_MOST:
                                if (actualHeight <= heightSize) {
                                    targetHeight = actualHeight;
                                    targetWidth = actualWidth;
                                }
                                else {
                                    targetHeight = heightSize;
                                    targetWidth = (int) (targetHeight * actualRatio);
                                }
                                break;
                            case MeasureSpec.UNSPECIFIED:
                                targetWidth = actualWidth;
                                targetHeight = actualHeight;
                                break;
                        }
                        break;
                }

                return cropImageAndResize(context, actualLeft, actualTop, actualRight, actualBottom, targetWidth, targetHeight);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                callback.onBitmapReady(bitmap);
            }

        }.execute();
    }

    private Bitmap cropImageAndResize(Context context, int left, int top, int right, int bottom, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;

        int rawArea = (right - left) * (bottom - top);
        int targetArea = width * height;

        int resultArea = rawArea;

        while (resultArea > targetArea) {
            options.inSampleSize *= 2;
            resultArea = rawArea / (options.inSampleSize * options.inSampleSize) ;
        }

        if (options.inSampleSize > 1) {
            options.inSampleSize /= 2;
        }

        try {
            Bitmap rawBitmap = MakeDrawableTask.getBitmap(context, mImageUri, options);

            if (rawBitmap == null) {
                return null;
            }

            left /= options.inSampleSize;
            top /= options.inSampleSize;
            right /= options.inSampleSize;
            bottom /= options.inSampleSize;

            int croppedWidth = right - left;
            int croppedHeight = bottom - top;

            Bitmap croppedBitmap = Bitmap.createBitmap(rawBitmap, left, top, croppedWidth, croppedHeight);

            if (rawBitmap != croppedBitmap) {
                rawBitmap.recycle();
            }

            if (croppedWidth <= width && croppedHeight <= height) {
                return croppedBitmap;
            }

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, width, height, false);

            croppedBitmap.recycle();

            return resizedBitmap;
        } catch (Throwable t) {
            return null;
        }
    }

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

        scaleDrawableToFitWithinViewWithValidRatio();

        placeDrawableInTheCenter();

        updateGrid();

        invalidate();
    }

    private boolean isImageSizeRatioValid(float imageSizeRatio) {
        return imageSizeRatio >= mMinimumRatio && imageSizeRatio <= mMaximumRatio;
    }

    private float getImageSizeRatio() {
        return (float) mImageRawWidth / (float) mImageRawHeight;
    }

    private void scaleDrawableToFitWithinViewWithValidRatio() {
        float scale = getDrawableScaleToFitWithValidRatio();

        setDrawableScale(scale);
    }

    private float getDrawableScaleToFitWithValidRatio() {
        float scale;

        float drawableSizeRatio = getImageSizeRatio();
        boolean imageSizeRatioIsValid = isImageSizeRatioValid(drawableSizeRatio);

        if (imageSizeRatioIsValid) {
            float viewRatio = (float) mWidth / (float) mHeight;
            float drawableRatio = (float) mImageRawWidth / (float) mImageRawHeight;

            boolean drawableIsWiderThanView = drawableRatio > viewRatio;

            if (drawableIsWiderThanView) {
                scale =  (float) mWidth / (float) mImageRawWidth;
            }
            else {
                scale = (float) mHeight / (float) mImageRawHeight;
            }
        }
        else if (mImageRawWidth < mWidth || mImageRawHeight < mHeight) {
            if (drawableSizeRatio < mMaximumRatio) {
                getBoundsForWidthAndRatio(mImageRawWidth, mMinimumRatio, mHelperRect);
                scale = mHelperRect.height() / (float) mHeight;
            }
            else {
                getBoundsForHeightAndRatio(mImageRawHeight, mMaximumRatio, mHelperRect);
                scale = mHelperRect.width() / (float) mWidth;
            }
        }
        else {
            if (drawableSizeRatio < mMinimumRatio) {
                getBoundsForHeightAndRatio(mHeight, mMinimumRatio, mHelperRect);
                scale = mHelperRect.width() / mImageRawWidth;
            }
            else {
                getBoundsForWidthAndRatio(mWidth, mMaximumRatio, mHelperRect);
                scale = mHelperRect.height() / mImageRawHeight;
            }
        }

        return scale;
    }

    private void setDrawableScale(float scale) {
        mDrawableScale = scale;

        invalidate();
    }

    private void placeDrawableInTheCenter() {
        mDisplayDrawableLeft = (mWidth - getDisplayDrawableWidth()) / 2;
        mDisplayDrawableTop = (mHeight - getDisplayDrawableHeight()) / 2;

        invalidate();
    }

    private float getDisplayDrawableWidth() {
        return mDrawableScale * mImageRawWidth;
    }

    private float getDisplayDrawableHeight() {
        return mDrawableScale * mImageRawHeight;
    }

    private void updateGrid() {
        getDisplayDrawableBounds(mHelperRect);

        mHelperRect.intersect(0, 0, mWidth, mHeight);

        float gridLeft = mHelperRect.left;
        float gridTop = mHelperRect.top;

        float gridWidth = mHelperRect.width();
        float gridHeight = mHelperRect.height();

        mHelperRect.set(gridLeft, gridTop, gridLeft + gridWidth, gridTop + gridHeight);
        setGridBounds(mHelperRect);

        invalidate();
    }

    private void getBoundsForWidthAndRatio(float width, float ratio, RectF rect) {
        float height = width / ratio;

        rect.set(0, 0, width, height);
    }

    private void getBoundsForHeightAndRatio(float height, float ratio, RectF rect) {
        float width = height * ratio;

        rect.set(0, 0, width, height);
    }

    private void setGridBounds(RectF bounds) {
        mGridDrawable.setBounds((int) bounds.left, (int) bounds.top, (int) bounds.right, (int) bounds.bottom);

        invalidate();
    }

    private void getDisplayDrawableBounds(RectF bounds) {
        bounds.left = mDisplayDrawableLeft;
        bounds.top = mDisplayDrawableTop;
        bounds.right = bounds.left + getDisplayDrawableWidth();
        bounds.bottom = bounds.top + getDisplayDrawableHeight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mDrawable == null) {
            return;
        }

        getDisplayDrawableBounds(mHelperRect);

        mDrawable.setBounds((int) mHelperRect.left, (int) mHelperRect.top, (int) mHelperRect.right, (int) mHelperRect.bottom);
        mDrawable.draw(canvas);

        mGridDrawable.draw(canvas);
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

            getDisplayDrawableBounds(mHelperRect);

            float overScrollX = measureOverScrollX(mHelperRect);
            float overScrollY = measureOverScrollY(mHelperRect);

            distanceX = applyOverScrollFix(distanceX, overScrollX);
            distanceY = applyOverScrollFix(distanceY, overScrollY);

            mDisplayDrawableLeft += distanceX;
            mDisplayDrawableTop += distanceY;

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

    private float measureOverScrollX(RectF displayDrawableBounds) {
        boolean drawableIsSmallerThanView = displayDrawableBounds.width() <= mWidth;

        if (drawableIsSmallerThanView) {
            return displayDrawableBounds.centerX() - mWidth/2;
        }

        if (displayDrawableBounds.left <= 0 && displayDrawableBounds.right >= mWidth) {
            return 0;
        }

        if (displayDrawableBounds.left < 0) {
            return displayDrawableBounds.right - mWidth;
        }

        if (displayDrawableBounds.right > mWidth) {
            return displayDrawableBounds.left;
        }

        return 0;
    }

    private float measureOverScrollY(RectF displayDrawableBounds) {
        boolean drawableIsSmallerThanView = displayDrawableBounds.height() < mHeight;

        if (drawableIsSmallerThanView) {
            return displayDrawableBounds.centerY() - mHeight/2;
        }

        if (displayDrawableBounds.top <= 0 && displayDrawableBounds.bottom >= mHeight) {
            return 0;
        }

        if (displayDrawableBounds.top < 0) {
            return displayDrawableBounds.bottom - mHeight;
        }

        if (displayDrawableBounds.bottom > mHeight) {
            return displayDrawableBounds.top;
        }

        return 0;
    }

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

            mScaleFocusX = detector.getFocusX();
            mScaleFocusY = detector.getFocusY();

            setScaleKeepingFocus(mDrawableScale * scale, mScaleFocusX, mScaleFocusY);

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
        float maximumAllowedScale = getMaximumAllowedScale();
        float minimumAllowedScale = getMinimumAllowedScale();

        if (maximumAllowedScale < minimumAllowedScale) {
            maximumAllowedScale = minimumAllowedScale;
        }

        if (mDrawableScale < minimumAllowedScale) {
            return mDrawableScale / minimumAllowedScale;
        }
        else if (mDrawableScale > maximumAllowedScale) {
            return mDrawableScale / maximumAllowedScale;
        }
        else {
            return 1;
        }
    }

    private float getMaximumAllowedScale() {
        float maximumAllowedWidth = mImageRawWidth;
        float maximumAllowedHeight = mImageRawHeight;

        return Math.min(maximumAllowedWidth / (float) mWidth, maximumAllowedHeight / (float) mHeight);
    }

    private float getMinimumAllowedScale() {
        return getDrawableScaleToFitWithValidRatio();
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

    private void setScaleKeepingFocus(float scale, float focusX, float focusY) {
        getDisplayDrawableBounds(mHelperRect);

        float focusRatioX = (focusX - mHelperRect.left) / mHelperRect.width();
        float focusRatioY = (focusY - mHelperRect.top) / mHelperRect.height();

        mDrawableScale = scale;

        getDisplayDrawableBounds(mHelperRect);

        float scaledFocusX = mHelperRect.left + focusRatioX * mHelperRect.width();
        float scaledFocusY = mHelperRect.top + focusRatioY * mHelperRect.height();

        mDisplayDrawableLeft += focusX - scaledFocusX;
        mDisplayDrawableTop += focusY - scaledFocusY;

        updateGrid();
        invalidate();
    }

    private ValueAnimator.AnimatorUpdateListener mSettleAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            float animatedValue = (float) animation.getAnimatedValue();

            getDisplayDrawableBounds(mHelperRect);

            float overScrollX = measureOverScrollX(mHelperRect);
            float overScrollY = measureOverScrollY(mHelperRect);
            float overScale = measureOverScale();

            mDisplayDrawableLeft -= overScrollX * animatedValue;
            mDisplayDrawableTop -= overScrollY * animatedValue;

            float targetScale = mDrawableScale / overScale;
            float newScale = (1 - animatedValue) * mDrawableScale + animatedValue * targetScale;

            setScaleKeepingFocus(newScale, mScaleFocusX, mScaleFocusY);

            updateGrid();
            invalidate();
        }

    };

}
