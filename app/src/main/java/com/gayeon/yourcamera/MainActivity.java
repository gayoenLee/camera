package com.gayeon.yourcamera;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoWriter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static android.Manifest.permission.CAMERA;



public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "메인액티비티에서";
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 9;
    private Mat matInput;
    private Mat matResult;
    ImageButton takePhotoBtn;
    ImageButton changeDirectionBtn;
    ImageButton makeFilterBtn;
    String filename;
    Button nothingBtn;
    Button hsvBtn;
    Button smoothBtn;
    Button grayBtn;
    Button roiBtn;
    //전면 카메라, 후면 카메라 방향 전환에 사용.
    private int cameraID;
    int i=0;

    //필터 효과 관련 변수들
    int RGBA;
    int GrayScale;
    int HSV;
    int Smoothing;
    int ROI;
    Scalar scalarLow, scalarHigh;
    Mat mat1, mat2, gray;
    private VideoWriter videoWriter;

    private CameraBridgeViewBase mOpenCvCameraView;
    //회색
    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }



    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        changeDirectionBtn = findViewById(R.id.change_direction_btn);
        makeFilterBtn = findViewById(R.id.magic_effect_btn);
        nothingBtn = findViewById(R.id.no_effect);
        hsvBtn = findViewById(R.id.hsv_effect);
        smoothBtn = findViewById(R.id.smoothing_effect);
        grayBtn = findViewById(R.id.gray_effect);
        roiBtn = findViewById(R.id.roi_effect);
        //scalar//////////////////////////
        scalarLow = new Scalar(120-10, 30, 30);
        scalarHigh = new Scalar(120+10, 255, 255);
        mat1 = new Mat();
        mat2 = new Mat();
        makeFilterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "필터 버튼 클릭");
                if(nothingBtn.getVisibility()==v.GONE){
                    Log.i(TAG, "필터 안에 버튼 보이도록 하기");
                    nothingBtn.setVisibility(v.VISIBLE);
                }else{
                    nothingBtn.setVisibility(v.GONE);
                }
                if(hsvBtn.getVisibility()==v.GONE){
                    hsvBtn.setVisibility(v.VISIBLE);
                }else{
                    hsvBtn.setVisibility(v.GONE);
                }
                if(smoothBtn.getVisibility()==v.GONE){
                    smoothBtn.setVisibility(v.VISIBLE);
                }else{
                    smoothBtn.setVisibility(v.GONE);
                }
                if(grayBtn.getVisibility()==v.GONE){
                    grayBtn.setVisibility(v.VISIBLE);
                }else{
                    grayBtn.setVisibility(v.GONE);
                }
                if(roiBtn.getVisibility()==v.GONE){
                    roiBtn.setVisibility(v.VISIBLE);
                }else{
                    roiBtn.setVisibility(v.GONE);
                }
            }
        });
        //필터 버튼듪
        nothingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              RGBA = 1;
              GrayScale = 0;
              HSV = 0;
              Smoothing = 0;
              ROI = 0;
            }
        });
        hsvBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RGBA = 0;
                GrayScale = 0;
                HSV = 1;
                Smoothing = 0;
                ROI = 0;

            }
        });
        smoothBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
RGBA = 0;
GrayScale = 0;
HSV = 0;
Smoothing = 1;
ROI = 0;
            }
        });
        grayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RGBA = 0;
                GrayScale = 1;
                HSV = 0;
                Smoothing = 0;
                ROI = 0;
            }
        });

        roiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RGBA = 0;
                GrayScale = 0;
                HSV = 0;
                Smoothing = 0;
                ROI = 1;
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
        takePhotoBtn = findViewById(R.id.take_photo);
        takePhotoBtn.setOnClickListener(new View.OnClickListener() {
            //사진이 저장되는데 시간이 오래걸림//////////////////
            @Override
            public void onClick(View v) {
                Log.i(TAG,"사진 찍기 클릭");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                String currentDateandTime = sdf.format(new Date());
                //String fileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath()/*getExternalStorageDirectory().getPath()*/ +
                  //      "/sample_picture_" + currentDateandTime + ".jpg";

               // Imgcodecs.imwrite(fileName, matResult);
                Mat mIntermediateMat = new Mat();
                Imgproc.cvtColor(matInput, mIntermediateMat, Imgproc.COLOR_RGBA2BGR, 3);

                File path = new File(Environment.getExternalStorageDirectory() + "/Images/");
                path.mkdirs();
                File file = new File(path, currentDateandTime+"image.png");
                filename = file.toString();
                mOpenCvCameraView.takePicture(filename);
                Boolean bool = Imgcodecs.imwrite(filename, mIntermediateMat);
                Log.i(TAG, "이미지 파일 경로 확인 : "+filename);
                if (bool)
                    Log.i(TAG, "SUCCESS writing image to external storage");
                else
                    Log.i(TAG, "Fail writing image to external storage");
              //  Log.i(TAG, "파일 경로 확인 "+ fileName);
               // Toast.makeText(MainActivity.this, "사진 파일 경로 확인 "+fileName, Toast.LENGTH_SHORT).show();
            }
        });
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        cameraID = 0;
        mOpenCvCameraView.setCameraIndex(cameraID);


        // front-camera(1),  back-camera(0)
        changeDirectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "렌즈 방향 전환 클릭");

                i++;
                if(i%2==0){
                    Log.i(TAG, "전면 카메라");
                cameraID = 0;

                    mOpenCvCameraView.setCameraIndex(cameraID);
                    mOpenCvCameraView.disableView();
                    //거꾸로 나오는 이미지를 제대로 맞추기 위해 사용.
                    mOpenCvCameraView.enableView();
                }
                else{
                    Log.i(TAG, "후면 카메라");
                    cameraID = 1;

                    mOpenCvCameraView.disableView();
                    mOpenCvCameraView.setCameraIndex(cameraID);
                    mOpenCvCameraView.setScaleX(-1);
                    mOpenCvCameraView.enableView();
                }
            }
        });
    }

   /* public void SaveImage (Mat mat) {
        Mat mIntermediateMat = new Mat();
        Imgproc.cvtColor(matResult, mIntermediateMat, Imgproc.COLOR_RGBA2BGR, 3);

        File path = new File(Environment.getExternalStorageDirectory() + "/Images/");
        path.mkdirs();
        File file = new File(path, "image.png");

        filename = file.toString();
        Boolean bool = Imgcodecs.imwrite(filename, mIntermediateMat);

        if (bool)
            Log.i(TAG, "SUCCESS writing image to external storage");
        else
            Log.i(TAG, "Fail writing image to external storage");
    }*/
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    public void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
//카메라 프리뷰가 시작되면 호출
    @Override
    public void onCameraViewStarted(int width, int height) {

    }
//카메라 프리뷰가 어떤 이유로 멈추면 호출.
    @Override
    public void onCameraViewStopped() {

    }

    public void leviBlueFilter (Mat input, Mat mask){
        List<Mat> channels = new ArrayList<>();

        Imgproc.cvtColor(input, input, Imgproc.COLOR_RGB2Lab);
        Imgproc.GaussianBlur(input,input,new Size(3,3),0);
        Core.split(input, channels);
        Imgproc.threshold(channels.get(1), mask, 145, 255, Imgproc.THRESH_BINARY);

        for(int i=0;i<channels.size();i++){
            channels.get(i).release();
        }
    }



    //프레임 전달이 필요한 경우에 호출됨.
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        matInput = inputFrame.rgba();
        if(GrayScale == 1){
            matInput = inputFrame.gray();
        }
        if(RGBA ==1){
            matInput = inputFrame.rgba();
        }
        if(HSV ==1){
            Imgproc.cvtColor(inputFrame.rgba(), mat1, Imgproc.COLOR_RGB2HSV);
            //inRange는 그 범위안에 들어가게 되면 0으로 만들어주고, 나머지는 1로 만들어 흑백사진을 만듦.
            Core.inRange(mat1, scalarLow, scalarHigh, mat2);
            matInput = mat2;
        }
        if(Smoothing == 1){
            org.opencv.core.Size size = new Size(21, 21);
            Imgproc.boxFilter(matInput, matInput, -1, size);
        }
        if(ROI == 1){
            Imgproc.cvtColor(inputFrame.rgba(), mat1, Imgproc.COLOR_RGB2HSV);
            Core.bitwise_and(matInput, matInput, mat1, mat2);
            matInput = mat1;
        }
        return matInput;


       /* if ( matResult == null )
            matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());
        Core.flip(matInput, matResult, 1);*/
       //회색 음영 적용
        //ConvertRGBtoGray(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
       // return matResult;
    }

    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    //여기서부턴 퍼미션 관련 메소드
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;

    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase: cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean havePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                havePermission = false;
            }
        }
        if (havePermission) {
            onCameraPermissionGranted();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onCameraPermissionGranted();
        }else{
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder( MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
                requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }


}