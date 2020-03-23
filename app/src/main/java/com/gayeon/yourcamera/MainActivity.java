package com.gayeon.yourcamera;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.FpsMeter;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.JavaCameraView;
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
import java.io.IOException;
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
    ImageButton videoBtn;
    String filename;
    Button nothingBtn;
    Button hsvBtn;
    Button smoothBtn;
    Button grayBtn;
    Button roiBtn;
    //전면 카메라, 후면 카메라 방향 전환에 사용.
    private int cameraID;
    int i = 0;

    //필터 효과 관련 변수들
    int RGBA;
    int GrayScale;
    int HSV;
    int Smoothing;
    int ROI;
    Scalar scalarLow, scalarHigh;
    Mat mat1, mat2, gray;

    public static final int RECORD_AUDIO = 0;

    private CameraBridgeViewBase mOpenCvCameraView;
    private CameraBridgeViewBase.CvCameraViewListener2 mListener;
    //비디오 녹화 관련 변수들
    public MediaRecorder mMediaRecorder;
    protected Surface surface = null;
    protected float mScale = 0;
    protected FpsMeter mFpsMeter = null;
    private Bitmap mCacheBitmap;
    private Mat edgesMat;
    File dir;
    int imageIndex = 0;
    //녹화 진행 여부를 판단하는 변수.
    private boolean isRecording = false;
    //회색
    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);
    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    @SuppressLint("WrongConstant")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        videoBtn = findViewById(R.id.video_record_btn);
        changeDirectionBtn = findViewById(R.id.change_direction_btn);
        makeFilterBtn = findViewById(R.id.magic_effect_btn);
        nothingBtn = findViewById(R.id.no_effect);
        hsvBtn = findViewById(R.id.hsv_effect);
        smoothBtn = findViewById(R.id.smoothing_effect);
        grayBtn = findViewById(R.id.gray_effect);
        roiBtn = findViewById(R.id.roi_effect);
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.RECORD_AUDIO}
            ,0);

            return;
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setLayoutDirection(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        mOpenCvCameraView.setCvCameraViewListener(this);
        cameraID = 0;
        mOpenCvCameraView.setCameraIndex(cameraID);
        //scalar//////////////////////////
        scalarLow = new Scalar(120 - 10, 30, 30);
        scalarHigh = new Scalar(120 + 10, 255, 255);
        mat1 = new Mat();
        mat2 = new Mat();
//녹화 기능
        videoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
Log.i(TAG, "비디오 녹화하기 버튼 클릭");

                new recordingTask().execute();

            }});
       // videoWriter = new VideoWriter("saved_video.avi", VideoWriter.fourcc('M', 'J', 'P', 'G'), 25.0D, new Size(mOpenCvCameraView.getWidth(), mOpenCvCameraView.getHeight()));
        //videoWriter.open("saved_video.avi", VideoWriter.fourcc('M', 'J', 'P', 'G'), 25.0D, new Size(mOpenCvCameraView.getWidth(), mOpenCvCameraView.getHeight()));
        //필터 선택 버튼
        makeFilterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "필터 버튼 클릭");
                if (nothingBtn.getVisibility() == v.GONE) {
                    Log.i(TAG, "필터 안에 버튼 보이도록 하기");
                    nothingBtn.setVisibility(v.VISIBLE);
                } else {
                    nothingBtn.setVisibility(v.GONE);
                }
                if (hsvBtn.getVisibility() == v.GONE) {
                    hsvBtn.setVisibility(v.VISIBLE);
                } else {
                    hsvBtn.setVisibility(v.GONE);
                }
                if (smoothBtn.getVisibility() == v.GONE) {
                    smoothBtn.setVisibility(v.VISIBLE);
                } else {
                    smoothBtn.setVisibility(v.GONE);
                }
                if (grayBtn.getVisibility() == v.GONE) {
                    grayBtn.setVisibility(v.VISIBLE);
                } else {
                    grayBtn.setVisibility(v.GONE);
                }
                if (roiBtn.getVisibility() == v.GONE) {
                    roiBtn.setVisibility(v.VISIBLE);
                } else {
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
        //외부 저장소 접근 허용 권한
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
        //사진 찍기 버튼.
        takePhotoBtn = findViewById(R.id.take_photo);
        takePhotoBtn.setOnClickListener(new View.OnClickListener() {
            //사진이 저장되는데 시간이 오래걸림//////////////////
            @Override
            public void onClick(View v) {
                Log.i(TAG, "사진 찍기 클릭");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                String currentDateandTime = sdf.format(new Date());
                //String fileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath()/*getExternalStorageDirectory().getPath()*/ +
                //      "/sample_picture_" + currentDateandTime + ".jpg";

                // Imgcodecs.imwrite(fileName, matResult);
                Mat mIntermediateMat = new Mat();
                Imgproc.cvtColor(matInput, mIntermediateMat, Imgproc.COLOR_RGBA2BGR, 3);

                File path = new File(Environment.getExternalStorageDirectory() + "/Images/");
                path.mkdirs();
                File file = new File(path, currentDateandTime + "image.png");
                filename = file.toString();
                mOpenCvCameraView.takePicture(filename);
                Boolean bool = Imgcodecs.imwrite(filename, mIntermediateMat);
                Log.i(TAG, "이미지 파일 경로 확인 : " + filename);
                if (bool)
                    Log.i(TAG, "SUCCESS writing image to external storage");
                else
                    Log.i(TAG, "Fail writing image to external storage");
            }
        });


        //렌즈 방향 전환하기
        changeDirectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "렌즈 방향 전환 클릭");
                i++;
                if (i % 2 == 0) {
                    Log.i(TAG, "전면 카메라");
                    cameraID = 0;
                    mOpenCvCameraView.setCameraIndex(cameraID);
                    mOpenCvCameraView.disableView();
                    //거꾸로 나오는 이미지를 제대로 맞추기 위해 사용.
                    mOpenCvCameraView.enableView();
                } else {
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


public class recordingTask extends AsyncTask<Void, Void, String>{




    @Override
    protected String doInBackground(Void... voids) {
        if (!isRecording) {
            Log.i(TAG, "비디오 녹화하기 번재 이프문 들어옴");
            isRecording = true;
//카메라가 처음에 녹화 상태가 아닌 경우 녹화 상태를 true로 바꿔줌.
            //그리고 prepareVideoRecorder로 카메라 세팅
            if (prepareVideoRecorder()) {
                Log.i(TAG, "비디오 녹화하기 a번재 이프문 안");
                mOpenCvCameraView.getRecorder();

                Log.i(TAG, "비디오 녹화하기 b번재 이프문 안");
                mOpenCvCameraView.setRecorder(mMediaRecorder);

                Log.i(TAG, "비디오 녹화하기 c번재 이프문 안");
                // prepareVideoRecorder();

                Log.i(TAG, "비디오 녹화하기 d번재 이프문 안");
                mMediaRecorder.start();

                Log.i(TAG, "비디오 녹화하기 e번재 이프문 안");
            } else {

                Log.i(TAG, "비디오 녹화하기 두번재 이프문 안");
                releaseMediaRecorder();

            }
        } else {
            Log.i(TAG, "비디오 녹화하기 세번재 이프문 안");
            isRecording = false;

            try {
                if (mMediaRecorder != null) {

                    mMediaRecorder.stop();// stop the recording

                    mMediaRecorder = null;

                } else
                    Log.e(TAG, "onRecordSignal mediaRecorder is null");

            } catch (RuntimeException e) {
                // RuntimeException is thrown when stop() is called immediately after start().
                // In this case the output file is not properly constructed ans should be deleted.
                Log.d(TAG, "RuntimeException: stop() is called immediately after start()");
                //noinspection ResultOfMethodCallIgnored*/
            }
            releaseMediaRecorder(); // release the MediaRecorder object
            //}

        }
        return null;
    }
    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

    }
}

  public void  releaseMediaRecorder(){

        if(mMediaRecorder != null){
            Log.i(TAG, "리리즈 미디어 리코더");
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;

           // mOpenCvCameraView.disableView();
            //c.lock 안먹음
            //JavaCameraView.mCamera.lock();
   mOpenCvCameraView.releaseRecord();
        }
  }

  @SuppressLint("NewApi")
  public boolean prepareVideoRecorder() {

          releaseMediaRecorder();
         // JavaCameraView.mCamera = getCameraInstance(JavaCameraView.mCamera);
          Log.i(TAG, "프리페어 메소드 트라이 안에 들어옴");
          mMediaRecorder = new MediaRecorder();

          JavaCameraView.mCamera.lock();
          //JavaCameraView.mCamera.unlock();

          //  mMediaRecorder.reset();

          mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
          mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
     // CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
     // mMediaRecorder.setProfile(profile);
      mMediaRecorder.setProfile(CamcorderProfile
              .get(CamcorderProfile.QUALITY_720P));

      //mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
          // mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
          mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
          mMediaRecorder.setAudioSamplingRate(16000);
          mMediaRecorder.setVideoFrameRate(30);

          // CamcorderProfile cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);

          //  mMediaRecorder.setProfile(cpHigh);
        //  mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        //  mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);

         // mMediaRecorder.setOutputFile(Environment.getExternalStorageDirectory() +"/video/"+ "1.mp4");

          Log.i(TAG, "미디어 리코더 프리페어 두번");
          mMediaRecorder.setVideoSize(mOpenCvCameraView.getWidth(), mOpenCvCameraView.getHeight());
          Log.i(TAG, "미디어 리코더 프리페어 세본");
          //  mMediaRecorder.setOnInfoListener((MediaRecorder.OnInfoListener) this);
          //   mMediaRecorder.setOnErrorListener((MediaRecorder.OnErrorListener) this);
          mMediaRecorder.setPreviewDisplay(mOpenCvCameraView.getHolder().getSurface());
          Log.i(TAG, "미디어 리코더 프리페어 네번째");
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          mMediaRecorder.setOutputFile(getOutputMediaFile());
          Log.i(TAG, "비디오 파일 저장");
      }


      // mMediaRecorder.start();
          try {
              mMediaRecorder.prepare();
          } catch (IllegalStateException e) {
              releaseMediaRecorder();
              return false;
          } catch (IOException e) {
              releaseMediaRecorder();
              return false;
          }
          return true;
      }
    public static Camera getCameraInstance(Camera c) {
        c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
        }
        return c;
    }

    //녹화한 동영상 파일 저장
    public static File getOutputMediaFile() {

        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "MyCameraApp");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "VID_" + timeStamp + ".mp4");

        return mediaFile;
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();

                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        //녹화
        Log.i(TAG, "온포우즈 ");
      releaseMediaRecorder();

        ;
    }

    @Override
    public void onResume() {
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

    //프레임 전달이 필요한 경우에 호출됨.
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        matInput = inputFrame.rgba();

        if (GrayScale == 1) {
            matInput = inputFrame.gray();
        }
        if (RGBA == 1) {
            matInput = inputFrame.rgba();
        }
        if (HSV == 1) {
            Imgproc.cvtColor(inputFrame.rgba(), mat1, Imgproc.COLOR_RGB2HSV);
            //inRange는 그 범위안에 들어가게 되면 0으로 만들어주고, 나머지는 1로 만들어 흑백사진을 만듦.
            Core.inRange(mat1, scalarLow, scalarHigh, mat2);
            matInput = mat2;
        }
        if (Smoothing == 1) {
            org.opencv.core.Size size = new Size(21, 21);
            Imgproc.boxFilter(matInput, matInput, -1, size);
        }
        if (ROI == 1) {
            Imgproc.cvtColor(inputFrame.rgba(), mat1, Imgproc.COLOR_RGB2HSV);
            Core.bitwise_and(matInput, matInput, mat1, mat2);
            matInput = mat1;
            Core.flip(matInput, matInput, 1);

            if(isRecording){
                Log.i(TAG, " 온카메라프레임 안");
                write(matInput);
            }

        }
        return matInput;



    }

    public void write(Mat mat){

        //convert from BGR to RGB
        Mat rgbMat = new Mat();
        Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_BGR2RGB);

        File file = new File(dir, "img" + imageIndex + ".png");

        String filename = file.toString();
        boolean success = Imgcodecs.imwrite(filename, rgbMat);

        Log.i(TAG, "Success writing img" + imageIndex +".png: " + success);

        imageIndex++;
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
        for (CameraBridgeViewBase cameraBridgeViewBase : cameraViews) {
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
        } else {
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
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