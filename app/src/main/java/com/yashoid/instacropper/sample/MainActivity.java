package com.yashoid.instacropper.sample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.yashoid.instacropper.InstaCropperActivity;
import com.yashoid.instacropper.InstaCropperView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private InstaCropperView mInstaCropper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInstaCropper = (InstaCropperView) findViewById(R.id.instacropper);
    }

    public void pickPhoto(View v) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(intent, 1);

//        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(getFile()));
//        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    Intent intent = InstaCropperActivity.getIntent(this, data.getData(), Uri.fromFile(new File(getExternalCacheDir(), "test.jpg")), 720, 50);
//                    Intent intent = InstaCropperActivity.getIntent(this, Uri.fromFile(getFile()), Uri.fromFile(getFile()), 720, 50);
                    startActivityForResult(intent, 2);
                }
                return;
            case 2:
                if (resultCode == RESULT_OK) {
                    mInstaCropper.setImageUri(data.getData());
                }
                return;
        }
    }

    public void rotate(View v) {
//        mInstaCropper.setDrawableRotation(mInstaCropper.getDrawableRotation() + 15);
    }

    public void crop(View v) {
        mInstaCropper.crop(View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), new InstaCropperView.BitmapCallback() {

            @Override
            public void onBitmapReady(Bitmap bitmap) {
                if (bitmap == null) {
                    Toast.makeText(MainActivity.this, "Returned bitmap is null.", Toast.LENGTH_SHORT).show();
                    return;
                }

                File file = getFile();

                try {
                    FileOutputStream os = new FileOutputStream(file);

                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, os);

                    os.flush();
                    os.close();

                    mInstaCropper.setImageUri(Uri.fromFile(file));

                    Log.i(TAG, "Image updated.");
                } catch (IOException e) {
                    Log.e(TAG, "Failed to compress bitmap.", e);
                }
            }

        });
    }

    private File getFile() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "instaCropper.jpg");
    }

}
