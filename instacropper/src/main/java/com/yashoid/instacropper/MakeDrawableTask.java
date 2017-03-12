package com.yashoid.instacropper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.FileNotFoundException;

/**
 * Created by Yashar on 3/8/2017.
 */

public class MakeDrawableTask extends AsyncTask<Void, Void, Drawable> {

    private Context mContext;

    private Uri mUri;

    private int mTargetWidth;
    private int mTargetHeight;

    private int mRawWidth;
    private int mRawHeight;

    protected MakeDrawableTask(Context context, Uri uri, int targetWidth, int targetHeight) {
        mContext = context;

        mUri = uri;

        mTargetWidth = targetWidth;
        mTargetHeight = targetHeight;
    }

    protected int getTargetWidth() {
        return mTargetWidth;
    }

    protected int getTargetHeight() {
        return mTargetHeight;
    }

    @Override
    protected Drawable doInBackground(Void... params) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inJustDecodeBounds = true;

        try {
            BitmapFactory.decodeStream(mContext.getContentResolver().openInputStream(mUri), null, options);

            mRawWidth = options.outWidth;
            mRawHeight = options.outHeight;

            int resultWidth = mRawWidth;
            int resultHeight = mRawHeight;

            Runtime.getRuntime().gc();
            long availableMemory = Runtime.getRuntime().freeMemory();
            long allowedMemoryToUse = availableMemory / 8;
            int maximumAreaPossibleAccordingToAvailableMemory = (int) (allowedMemoryToUse / 4);

            int targetArea = Math.min(mTargetWidth * mTargetHeight, maximumAreaPossibleAccordingToAvailableMemory);

            int resultArea = resultWidth * resultHeight;

            while (resultArea > targetArea) {
                options.inSampleSize *= 2;

                resultWidth /= 2;
                resultHeight /= 2;

                resultArea = resultWidth * resultHeight;
            }

            options.inJustDecodeBounds = false;

            Bitmap bitmap = BitmapFactory.decodeStream(mContext.getContentResolver().openInputStream(mUri), null, options);

            if (bitmap == null) {
                return null;
            }

            return new BitmapDrawable(mContext.getResources(), bitmap);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    protected int getRawWidth() {
        return mRawWidth;
    }

    protected int getRawHeight() {
        return mRawHeight;
    }

}
