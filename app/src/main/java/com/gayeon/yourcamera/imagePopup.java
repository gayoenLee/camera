package com.gayeon.yourcamera;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.IOException;

public class imagePopup extends AppCompatActivity {

    ImageView galleryImage;
    ImageView filterResultImageView;
    private Mat imageInput;
    private Mat imageResult;
    private int threshold1 = 50;
    private int threshold2 = 150;
    private Context context;
    private final int imageWidth = 320;
    private final int imageHeight = 372;
    private final int GET_GALLERY_IMAGE = 200;
    Button edgeBtn;
    Button faceFilterBtn;
    boolean isReady = false;
    LinearLayout backgroundLayout;


    String TAG = "이미지팝업";
    int RGBA;
    int GrayScale;
    int HSV;
    int Smoothing;
    int ROI;
    Button backgroundFilterBtn;


    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    public native void imageprocessing(long inputImage, long outputImage, int th1, int th2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_popup);
        galleryImage = findViewById(R.id.bigPhoto);
        filterResultImageView = findViewById(R.id.imageViewOutput);

        backgroundFilterBtn = findViewById(R.id.background_filter);
        edgeBtn = findViewById(R.id.edge_button);
        faceFilterBtn = findViewById(R.id.face_filter);
        backgroundLayout = findViewById(R.id.background_filter_linear);
        context = this;

        //이미지 가져오기
        Intent pickedIntent = getIntent();
        String imagePath = pickedIntent.getStringExtra("filename");
        Log.i(TAG, imagePath);
        String position = pickedIntent.getStringExtra("position");

        //이미지 보여주기
        BitmapFactory.Options bitmapFactory = new BitmapFactory.Options();
        bitmapFactory.inSampleSize = 2;

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath + File.separator + position, bitmapFactory);

        Log.i(TAG, imagePath + File.separator + position);
        Log.i(TAG, bitmap.getWidth() + "");
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, imageWidth, imageHeight, true);

        imageInput = new Mat();
        Bitmap bitmapResult = resized.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bitmapResult, imageInput);

        galleryImage.setImageBitmap(resized);
//--------------------------------------------------------------------
        backgroundFilterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (faceFilterBtn.getVisibility() == view.GONE) {
                    faceFilterBtn.setVisibility(view.VISIBLE);

                } else {
                    faceFilterBtn.setVisibility(view.GONE);
                    //배경화면 필터를 감싸고 있는 리니어 레이아웃 보이게 하기
                    edgeBtn.setVisibility(view.VISIBLE);
                    backgroundLayout.setVisibility(view.VISIBLE);
                }

            }
        });


        edgeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                imageprocess_and_showResult(threshold1, threshold2);
            }
        });
        final TextView textView1 = (TextView) findViewById(R.id.textView_threshold1);
        SeekBar seekBar1 = (SeekBar) findViewById(R.id.seekBar_threshold1);
        seekBar1.setProgress(threshold1);
        seekBar1.setMax(200);
        seekBar1.setMin(0);
        seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                threshold1 = progress;
                textView1.setText(threshold1 + "");
                imageprocess_and_showResult(threshold1, threshold2);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        final TextView textView2 = (TextView) findViewById(R.id.textView_threshold2);
        SeekBar seekBar2 = (SeekBar) findViewById(R.id.seekBar_threshold2);
        seekBar2.setProgress(threshold2);
        seekBar2.setMax(200);
        seekBar2.setMin(0);
        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                threshold2 = progress;
                textView2.setText(threshold2 + "");
                imageprocess_and_showResult(threshold1, threshold2);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();

        isReady = true;
    }


    private void imageprocess_and_showResult(int th1, int th2) {

        if (isReady == false) return;

        if (imageResult == null)
            imageResult = new Mat();

        imageprocessing(imageInput.getNativeObjAddr(), imageResult.getNativeObjAddr(), th1, th2);


        Bitmap bitmapOutput = Bitmap.createBitmap(imageResult.cols(), imageResult.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageResult, bitmapOutput);
        galleryImage.setImageBitmap(bitmapOutput);
    }

    public int getOrientationOfImage(String filepath) {
        ExifInterface exif = null;

        try {
            exif = new ExifInterface(filepath);
        } catch (IOException e) {
            Log.d("@@@", e.toString());
            return -1;
        }

        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);

        if (orientation != -1) {
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
            }
        }

        return 0;
    }

    public Bitmap getRotatedBitmap(Bitmap bitmap, int degrees) throws Exception {
        if (bitmap == null) return null;
        if (degrees == 0) return bitmap;

        Matrix m = new Matrix();
        m.setRotate(degrees, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }


}
