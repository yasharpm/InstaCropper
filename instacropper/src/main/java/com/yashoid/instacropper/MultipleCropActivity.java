package com.yashoid.instacropper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class MultipleCropActivity extends Activity {

    public static final String EXTRA_OUTPUT = InstaCropperActivity.EXTRA_OUTPUT;
    public static final String EXTRA_COUNT = "count";

    public static final String EXTRA_PREFERRED_RATIO = InstaCropperActivity.EXTRA_PREFERRED_RATIO;
    public static final String EXTRA_MINIMUM_RATIO = InstaCropperActivity.EXTRA_MINIMUM_RATIO;
    public static final String EXTRA_MAXIMUM_RATIO = InstaCropperActivity.EXTRA_MAXIMUM_RATIO;

    public static final String EXTRA_WIDTH_SPEC = InstaCropperActivity.EXTRA_WIDTH_SPEC;
    public static final String EXTRA_HEIGHT_SPEC = InstaCropperActivity.EXTRA_HEIGHT_SPEC;

    public static final String EXTRA_OUTPUT_QUALITY = InstaCropperActivity.EXTRA_OUTPUT_QUALITY;

    private static final String KEY_INDEX = "index";

    public static Intent getIntent(Context context, MultipleUris src, MultipleUris dst,
                                   int maxWidth, int outputQuality) {
        Intent intent = new Intent(context, MultipleCropActivity.class);

        intent.setData(src.toUri());

        intent.putExtra(EXTRA_OUTPUT, dst.toUri());

        intent.putExtra(EXTRA_PREFERRED_RATIO, InstaCropperView.DEFAULT_RATIO);
        intent.putExtra(EXTRA_MINIMUM_RATIO, InstaCropperView.DEFAULT_MINIMUM_RATIO);
        intent.putExtra(EXTRA_MAXIMUM_RATIO, InstaCropperView.DEFAULT_MAXIMUM_RATIO);

        intent.putExtra(EXTRA_WIDTH_SPEC, View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST));
        intent.putExtra(EXTRA_HEIGHT_SPEC, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        intent.putExtra(EXTRA_OUTPUT_QUALITY, outputQuality);

        return intent;
    }

    public static Intent getIntent(Context context, MultipleUris src, MultipleUris dst,
                                   int maxWidth, int maxHeight, float aspectRatio) {
        Intent intent = new Intent(context, MultipleCropActivity.class);

        intent.setData(src.toUri());

        intent.putExtra(EXTRA_OUTPUT, dst.toUri());

        intent.putExtra(EXTRA_PREFERRED_RATIO, aspectRatio);
        intent.putExtra(EXTRA_MINIMUM_RATIO, aspectRatio);
        intent.putExtra(EXTRA_MAXIMUM_RATIO, aspectRatio);

        intent.putExtra(EXTRA_WIDTH_SPEC, View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST));
        intent.putExtra(EXTRA_HEIGHT_SPEC, View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST));
        intent.putExtra(EXTRA_OUTPUT_QUALITY, InstaCropperActivity.DEFAULT_OUTPUT_QUALITY);

        return intent;
    }

    private MultipleUris mSources;
    private MultipleUris mDestinations;

    private int mIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        Uri srcUri = intent.getData();
        Uri dstUri = intent.getParcelableExtra(EXTRA_OUTPUT);

        if (srcUri == null || dstUri == null) {
            throw new IllegalArgumentException("Source or destination is not provided.");
        }

        mSources = new MultipleUris(srcUri);
        mDestinations = new MultipleUris(dstUri);

        if (mSources.size() != mDestinations.size()) {
            throw new IllegalArgumentException("Source and destination URIs must have the same length.");
        }

        if (savedInstanceState == null) {
            mIndex = 0;
            goNext();
        }
        else {
            mIndex = savedInstanceState.getInt(KEY_INDEX);
        }
    }

    private void goNext() {
        if (mIndex == mSources.size()) {
            Intent output = new Intent();
            output.setData(mDestinations.toUri());
            output.putExtra(EXTRA_COUNT, mIndex);
            setResult(RESULT_OK, output);
            finish();
            return;
        }

        Uri source = mSources.getUris().get(mIndex);
        Uri destination = mDestinations.getUris().get(mIndex);

        Intent intent = new Intent(getIntent());
        intent.setClass(this, InstaCropperActivity.class);
        intent.setData(source);
        intent.putExtra(EXTRA_OUTPUT, destination);

        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            Intent output = new Intent();
            output.setData(mDestinations.toUri());
            output.putExtra(EXTRA_COUNT, mIndex);
            setResult(RESULT_CANCELED, output);
            finish();
            return;
        }

        mIndex++;

        goNext();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_INDEX, mIndex);
    }

}
