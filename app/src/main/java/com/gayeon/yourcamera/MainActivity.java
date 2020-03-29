package com.gayeon.yourcamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.FpsMeter;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static org.opencv.imgproc.Imgproc.circle;
import static org.opencv.imgproc.Imgproc.ellipse;

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
    Mat mYuv;

    protected float mScale = 0;
    protected FpsMeter mFpsMeter = null;
    private Bitmap mCacheBitmap;
    private Mat edgesMat;
    File dir;
    int imageIndex = 0;
    private VideoWriter videoWriter;
    private VideoCapture videoCapture;

    SurfaceHolder surfaceHolder;
    private Size frameSize;
    //녹화 진행 여부를 판단하는 변수.
    private boolean isRecording = false;

    int videoLength;
    int frameNumber;
    int framePerSecond;

    int frame_width;

    int frame_height;
    private boolean FACEDETECT = false;

    //회색
    // public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);


    public native long loadCascade(String cascadeFileName);

    public native int detect(long cascadeClassifier_face,

                             long cascadeClassifier_eye, long matAddrInput, long matAddrResult);

    public long cascadeClassifier_face = 0;
    public long cascadeClassifier_eye = 0;

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
                    , 0);
            return;
        }
        //외부 저장소 접근 허용 권한
        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setLayoutDirection(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        mOpenCvCameraView.setCvCameraViewListener(this);
        //후면 카메라 디폴트
        cameraID = 0;
        mOpenCvCameraView.setCameraIndex(cameraID);
        //scalar//////////////////////////
        scalarLow = new Scalar(120 - 10, 30, 30);
        scalarHigh = new Scalar(120 + 10, 255, 255);
        mat1 = new Mat();
        mat2 = new Mat();
        //녹화
        videoCapture = new VideoCapture(0);

        //녹화 기능
        videoBtn.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Log.i(TAG, "얼굴 인식 시작 버튼 클릭");
                                            FACEDETECT = true;


                                        }
                                    }
        );

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

        //사진 찍기 버튼.
        takePhotoBtn = findViewById(R.id.take_photo);
        takePhotoBtn.setOnClickListener(new View.OnClickListener() {
            //사진이 저장되는데 시간이 오래걸림//////////////////
            @Override
            public void onClick(View v) {
                Log.i(TAG, "사진 찍기 클릭");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                String currentDateandTime = sdf.format(new Date());

                Mat mIntermediateMat = new Mat();
                Imgproc.cvtColor(matInput, mIntermediateMat, Imgproc.COLOR_RGBA2BGR, 3);

                File path = new File(Environment.getExternalStorageDirectory() + "/Images/");
                path.mkdirs();
                File file = new File(path, currentDateandTime + "image.png");
                filename = file.toString();
                //사진 찍기
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

   /* private void detectFace(){
        Log.i(TAG, "얼굴과 눈 검출하기 위해 학습시켜 놓은 분류기 로드");
        CascadeClassifier cascadeClassifier = new CascadeClassifier();
        if(cascadeClassifier.empty()){
            Log.i(TAG, "디텍트 페이스 메소드2");
            String path = Environment.getExternalStorageDirectory().getAbsolutePath();
            cascadeClassifier.load(path + "/haarcascade_frontalface_alt.xml");
            cascadeClassifier.load(path + "/haarcascade_eye_tree_eyeglasses.xml");
        }
        if(cascadeClassifier.empty()){
            Log.i(TAG, "디텍트 페이스 메소드3");
            return;
        }
        Log.i(TAG, "얼굴 검출할 그레이스케일 이미지 준비.");
        Mat gray = new Mat();
        Mat resizingGray = new Mat();
        Imgproc.cvtColor(matInput, gray, Imgproc.COLOR_BGRA2GRAY);
        Log.i(TAG, "디텍트 페이스 메소드4");
        Imgproc.resize(gray, resizingGray,  new Size(1280, 900) );

        Log.i(TAG, "MatOfRect : Mat를 상속받아 만들어진 클래스.");
        MatOfRect faces = new MatOfRect();
        Log.i(TAG, "이미지에서 얼굴 검출");
        cascadeClassifier.detectMultiScale(resizingGray, faces, 3, 2, 0, new Size(60, 60));

        Log.i(TAG, "디텍트 페이스 메소드7");
        for(int i=0; i<faces.total(); i++){
            Log.i(TAG, "얼굴 이미지들 포문 안에서 검출");
            Rect rc = faces.toList().get(i);
            rc.x += 3;
            rc.y += 3;
            rc.width += 3;
            rc.height += 3;
            rectangle(matInput, rc, new Scalar(255, 50, 100), 2);
      //검출한 입들 배열
       Mat faceROI = resizingGray.submat(rc);
            MatOfRect eyes = new MatOfRect();

       cascadeClassifier.detectMultiScale(faceROI, eyes, 1.1, 2, 0, new Size(30, 30));
     //  Rect[] eyesArray = eyes.toArray();
       for(int j=0; j<eyes.total(); j++){
           Rect eyeRect = eyes.toList().get(i);
           eyeRect.x +=1.1;
           eyeRect.y += 1.1;
           eyeRect.width += 1.1;
           eyeRect.height += 1.1;

           int radius = (int)Math.round((eyeRect.width + eyeRect.height)*0.25);
         // Imgproc.circle(matInput, eyeRect, raduius, new Scalar(255, 202, 121), 4, 8, 0);
       }
        }



        Log.i(TAG, "디텍트 페이스 메소드8");
    }*/

    private void detectFace() {
        Log.i(TAG, "얼굴과 눈 검출하기 위해 학습시켜 놓은 분류기 로드");
        CascadeClassifier cascadeClassifier = new CascadeClassifier();
        if (cascadeClassifier.empty()) {
            Log.i(TAG, "디텍트 페이스 메소드2");
            String path = Environment.getExternalStorageDirectory().getAbsolutePath();
            cascadeClassifier.load(path + "/haarcascade_frontalface_alt.xml");
            cascadeClassifier.load(path + "/haarcascade_eye_tree_eyeglasses.xml");
        }
        if (cascadeClassifier.empty()) {
            Log.i(TAG, "디텍트 페이스 메소드3");
            return;
        }
        Log.i(TAG, "얼굴 검출할 그레이스케일 이미지 준비.");
        Mat gray = new Mat();
        Mat resizingGray = new Mat();
        Imgproc.cvtColor(matInput, gray, Imgproc.COLOR_BGRA2GRAY);
        Log.i(TAG, "디텍트 페이스 메소드4");
        Imgproc.resize(gray, resizingGray, new Size(1920, 1080));

        Log.i(TAG, "MatOfRect : Mat를 상속받아 만들어진 클래스.");
        MatOfRect faces = new MatOfRect();
        Log.i(TAG, "이미지에서 얼굴 검출");
        cascadeClassifier.detectMultiScale(resizingGray, faces, 1.1, 2, 0, new Size(30, 30));
        Rect[] facesArray = faces.toArray();

        Log.i(TAG, "디텍트 페이스 메소드7");
        for (int i = 0; i < facesArray.length; i++) {
            Log.i(TAG, "얼굴 이미지들 포문 안에서 검출");
            //point
            Point centre1 = new Point(facesArray[i].x + facesArray[i].width * 0.5,
                    facesArray[i].y + facesArray[i].height * 0.5);
            //ellipse
            ellipse(matInput, centre1, new Size(facesArray[i].width * 0.5, facesArray[i].height * 0.5), 0, 0, 360,
                    new Scalar(255, 0, 255), 4, 8, 0);

            Mat faceROI = gray.submat(facesArray[i]);
//검출한 눈 배열
            MatOfRect eyes = new MatOfRect();
            cascadeClassifier.detectMultiScale(faceROI, eyes, 2, 2, 0, new Size(30, 30));

            Rect[] eyesArray = eyes.toArray();
            for (int j = 0; j < eyesArray.length; j++) {
                Point centre2 = new Point(facesArray[i].x + eyesArray[j].x + eyesArray[j].width * 0.5,
                        facesArray[i].y + eyesArray[j].y + eyesArray[j].height * 0.5);
                int radius = (int) Math.round((eyesArray[j].width + eyesArray[j].height) * 0.25);
                circle(matInput, centre2, radius, new Scalar(255, 0, 0), 4, 8, 0);
            }

        }


        Log.i(TAG, "디텍트 페이스 메소드8");
    }


    //프레임 전달이 필요한 경우에 호출됨.
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        matInput = inputFrame.rgba();
        if (FACEDETECT) {
            detectFace();
            ;
            // Core.flip(matInput, matInput, 1);
            return matInput;
        }

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
        if (ROI == 1)
            Imgproc.cvtColor(inputFrame.rgba(), mat1, Imgproc.COLOR_RGB2HSV);
        Core.bitwise_and(matInput, matInput, mat1, mat2);
        matInput = mat1;

        detect(cascadeClassifier_face, cascadeClassifier_eye, matInput.getNativeObjAddr(),
                matInput.getNativeObjAddr());


        // Core.flip(matInput, matInput, 1);
        return matInput;
    }

    //assets에서 copyfile로 해당 파일을 가져와 외부 저장소의 특정 위치에 저장하도록 구현.
    private void read_cascade_file() {
        copyFile("haarcascade_frontalface_alt.xml");
        copyFile("haarcascade_eye_tree_eyeglasses.xml");
        //loadCascade 메소드는 외부 저장소의 특정 위치에서 해당 파일을 읽어와서

        //CascadeClassifier 객체로 로드합니다.
        cascadeClassifier_face = loadCascade("haarcascade_frontalface_alt.xml");
        Log.d(TAG, "read_cascade_file:");

        cascadeClassifier_eye = loadCascade("haarcascade_eye_tree_eyeglasses.xml");
    }

    private void copyFile(String filename) {
        String baseDir = Environment.getExternalStorageDirectory().getPath();
        String pathDir = baseDir + File.separator + filename;

        AssetManager assetManager = this.getAssets();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            Log.d(TAG, "copyFile :: 다음 경로로 파일복사 " + pathDir);
            inputStream = assetManager.open(filename);
            outputStream = new FileOutputStream(pathDir);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (Exception e) {
            Log.d(TAG, "copyFile :: 파일 복사 중 예외 발생 " + e.toString());
        }
    }

    //녹화한 동영상 파일 저장
    public File getOutputMediaFile() {
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "MyCameraApp");
        Log.d("MyCameraApp", "저장");
        if (!mediaStorageDir.exists()) {
            Log.d("MyCameraApp", "오류");
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "VID_" + timeStamp + ".mp4");

        Log.d("MyCameraApp", "리턴");
        return mediaFile;
        //  return matInput;
    }

    public void Write(Mat frame) {
        Log.i(TAG, " 녹화 라이트 메소드 안으로 들어옴");
        if (isRecording) {
            Log.i(TAG, " 녹화 라이트 메소드 안으로 들어옴 2");
            if (!videoCapture.isOpened()) {
                Size frameSize = new Size(frame_height, frame_width);
                videoCapture.read(frame);
                Log.i(TAG, "녹화 비디오 라이터 생성 다음");
                //videoWriter = new VideoWriter(recordfilepath(), VideoWriter.fourcc('M', 'P', 'E', 'G'), frameSize);
                Log.i(TAG, "녹화 비디오 캡쳐가 비디오 받음");

                while (videoCapture.read(frame)) {
                    videoWriter.write(frame);
                    Log.i(TAG, "비디오라이터 저장");
                }
                Log.i(TAG, "비디오라이터 저장 끝");
                videoCapture.release();
                Log.i(TAG, "비디오캡쳐 끝");
                videoWriter.release();
                Log.i(TAG, "비디오라이터 릴리즈");
            } else {
                Log.i(TAG, " 비디오 녹화 라이트 메소드 엘스문");
            }


        }
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
        //releaseMediaRecorder();

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

    private String recordfilepath() {
        Log.i(TAG, "레코드 파일패스로 들어옴");
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());


        File sddir = new File(Environment.getExternalStorageDirectory() + "/video/");
        sddir.mkdirs();
        // File vrdir = new File(sddir, "Myvideo");

        File file = new File(sddir, "KLI_" + timeStamp + "savedVideo.avi");
        String filepath = file.toString();
        //  Boolean bool = Imgcodecs.imwrite(filepath, matInput);
        Log.e("debug mediarecorder", filepath);
        return filepath;
    }


    public void write(Mat mat) {

        //convert from BGR to RGB
        Mat rgbMat = new Mat();
        Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_BGR2RGB);

        File file = new File(dir, "img" + imageIndex + ".mp4");

        String filename = file.toString();
        boolean success = Imgcodecs.imwrite(filename, rgbMat);
        // boolean success = MediaCodec.createDecoderByType()

        Log.i(TAG, "Success writing img" + imageIndex + ".mp4: " + success);

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

                read_cascade_file();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean havePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_REQUEST_CODE);
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
                && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
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
                requestPermissions(new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_REQUEST_CODE);
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