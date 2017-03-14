package com.yashoid.instacropper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.FileNotFoundException;

/**
 * Created by Yashar on 3/8/2017.
 */

public class MakeDrawableTask extends AsyncTask<Void, Void, Drawable> {

    private static final String TAG = "MakeDrawableTask";

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
        options.inSampleSize = 1;

        options.inJustDecodeBounds = true;

        try {
            BitmapFactory.decodeStream(mContext.getContentResolver().openInputStream(mUri), null, options);

            mRawWidth = options.outWidth;
            mRawHeight = options.outHeight;

            int resultWidth = mRawWidth;
            int resultHeight = mRawHeight;

            Runtime.getRuntime().gc();
            long totalMemory = Runtime.getRuntime().maxMemory();
            long allowedMemoryToUse = totalMemory / 8;
            int maximumAreaPossibleAccordingToAvailableMemory = (int) (allowedMemoryToUse / 4);

            int targetArea = Math.min(mTargetWidth * mTargetHeight, maximumAreaPossibleAccordingToAvailableMemory);

            int resultArea = resultWidth * resultHeight;

            while (resultArea > targetArea) {
                options.inSampleSize *= 2;

                resultWidth = mRawWidth / options.inSampleSize;
                resultHeight = mRawHeight / options.inSampleSize;

                resultArea = resultWidth * resultHeight;
            }

            options.inJustDecodeBounds = false;

            Bitmap bitmap = getBitmap(mContext, mUri, options);

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

    protected static Bitmap getBitmap(Context context, Uri uri, BitmapFactory.Options options) {
        Bitmap bitmap = null;

        while (true) {
            try {
                bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);
                break;
            } catch (Throwable t) {
                options.inSampleSize *= 2;

                if (options.inSampleSize >= 1024) {
                    Log.d(TAG, "Failed to optimize RAM to receive Bitmap.");

                    break;
                }
            }
        }

        return bitmap;
    }

}
