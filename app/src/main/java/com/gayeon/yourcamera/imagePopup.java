package com.gayeon.yourcamera;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class imagePopup extends AppCompatActivity {

    ImageView galleryImage;
    private Context context;
    private final int imageWidth = 320;
    private final int imageHeight = 372;
String TAG = "이미지팝업";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_popup);
        galleryImage = findViewById(R.id.bigPhoto);
        context = this;

        Intent pickedIntent = getIntent();
        String imagePath = pickedIntent.getStringExtra("filename");
        Log.i(TAG,imagePath);
        String position = pickedIntent.getStringExtra("position");


        //이미지 보여주기
        BitmapFactory.Options bitmapFactory = new BitmapFactory.Options();
        bitmapFactory.inSampleSize = 2;


        Bitmap bitmap = BitmapFactory.decodeFile(imagePath + File.separator + position, bitmapFactory);

        Log.i(TAG,imagePath + File.separator + position);
        Log.i(TAG,bitmap.getWidth()+"");
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, imageWidth, imageHeight, true);

        galleryImage.setImageBitmap(resized);

    }
}
