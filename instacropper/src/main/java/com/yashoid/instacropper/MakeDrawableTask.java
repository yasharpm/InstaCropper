package com.yashoid.instacropper;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
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

            int targetArea = Math.min(mTargetWidth * mTargetHeight * 4, maximumAreaPossibleAccordingToAvailableMemory);

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

            float beforeRatio = (float) mRawWidth / (float) mRawHeight;
            float afterRatio = (float) bitmap.getWidth() / (float) bitmap.getHeight();

            if ((beforeRatio < 1 && afterRatio > 1) || (beforeRatio > 1 && afterRatio < 1)) {
                int rawWidth = mRawWidth;
                mRawWidth = mRawHeight;
                mRawHeight = rawWidth;
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

        if (bitmap != null) {
            if (isUriMatching(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, uri) || isUriMatching(MediaStore.Images.Media.INTERNAL_CONTENT_URI, uri)) {
                Cursor c = context.getContentResolver().query(uri, new String[] {MediaStore.Images.Media.ORIENTATION }, null, null, null);

                if (c.getCount() == 1) {
                    c.moveToFirst();

                    int orientation = c.getInt(0);

                    c.close();

                    Matrix matrix = new Matrix();
                    matrix.postRotate(orientation);

                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                }
                else {
                    Log.w(TAG, "Failed to get MediaStore image orientation.");

                    c.close();
                }
            }
        }

        return bitmap;
    }

    protected static Bitmap resizeBitmap(Bitmap bitmap, int newWidth, int newHeight) {
        Bitmap resizedBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        float scaleX = newWidth / (float) bitmap.getWidth();
        float scaleY = newHeight / (float) bitmap.getHeight();
        float pivotX = 0;
        float pivotY = 0;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(scaleX, scaleY, pivotX, pivotY);

        Canvas canvas = new Canvas(resizedBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap, 0, 0, new Paint(Paint.FILTER_BITMAP_FLAG));

        return resizedBitmap;
    }

    private static boolean isUriMatching(Uri path, Uri element) {
        return Uri.withAppendedPath(path, element.getLastPathSegment()).equals(element);
    }

}
