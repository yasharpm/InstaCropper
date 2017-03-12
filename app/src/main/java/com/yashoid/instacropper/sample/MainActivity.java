package com.yashoid.instacropper.sample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.yashoid.instacropper.InstaCropperActivity;
import com.yashoid.instacropper.InstaCropperView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    Intent intent = InstaCropperActivity.getIntent(this, data.getData(), Uri.fromFile(new File(getExternalCacheDir(), "test.jpg")), 720, 50);
                    startActivityForResult(intent, 2);
                }
                return;
            case 2:
                if (resultCode == RESULT_OK) {
                    mInstaCropper.setImageUri((Uri) data.getData());
                }
                return;
        }
        if (resultCode == RESULT_OK) {
        }
    }

    public void crop(View v) {
        mInstaCropper.crop(View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), new InstaCropperView.BitmapCallback() {

            @Override
            public void onBitmapReady(Bitmap bitmap) {
                File file = new File(getExternalCacheDir(), "test.jpg");

                try {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, new FileOutputStream(file));

                    mInstaCropper.setImageUri(Uri.fromFile(file));
                } catch (FileNotFoundException e) { }
            }

        });
    }

}
