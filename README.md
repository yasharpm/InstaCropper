# InstaCropper
A View for cropping images that is similar to Instagram's crop. Also an Activity for cropping is included.

![alt tag](https://cloud.githubusercontent.com/assets/4597931/23830368/724ddf70-071e-11e7-9d7e-65615be8d5e6.gif)

##Usage

Add the dependency:
```Groovy
dependencies {
	compile 'com.yashoid:instacropper:1.0.0'
}
```

## How to use this library

Add `InstaCropperView` to your xml layout

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.yashoid.instacropper.InstaCropperView
        android:id="@+id/instacropper"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:backgroundColor="@android:color/white" />

</FrameLayout>
```

InstaCropperView receives only Uri but any Uri is possible.
```java
mInstaCropper.setImageUri(imageUri);
```

You can specify three size ratios for the crop. The narrowest allowed, the widest allowed and the ideal ratio. The View will adjust its size by the ideal ratio.
```java
mInstaCropper.setRatios(defaultRatio, minimumRatio, maximumRatio);
```

The cropped image is returned in a callback. You can specify MeasureSpec to adjust the width and height of the returned Bitmap.
```java
mInstaCropper.crop(
        View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        new InstaCropperView.BitmapCallback() {

            @Override
            public void onBitmapReady(Bitmap bitmap) {
                // Do something.
            }
            
        }
);
```

It is also possible to use the crop feature via an Intent call. There are various `getIntent()` methods defined on `InstaCropperActivity`. You will then receive the crop result in `data.getData()`.
```java
Intent intent = InstaCropperActivity.getIntent(context, srcUri, dstUri, maxWidth, outputQuality);
startActivityForResult(intent, REQUEST_CROP);
```

You can modify the crop Activity's apprearance by overriding the following resouce values:
```xml
<string name="instacropper_title">Crop</string> <!-- Title of toolbar -->
<string name="instacropper_crop">Crop</string> <!-- Title of crop action in toolbar -->

<color name="instacropper_cropper_background">@android:color/white</color> <!-- Background color of InstaCropperView in the activity -->
<color name="instacropper_crop_color">@android:color/black</color> <!-- Color of crop action in toolbar -->

<style name="InstaCropper" parent="android:style/Theme.DeviceDefault.Light"/> <!-- Style of crop Activity -->
```
