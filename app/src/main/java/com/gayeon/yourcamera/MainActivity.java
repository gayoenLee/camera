package com.gayeon.yourcamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

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
import org.opencv.android.Utils;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {


    private static final String TAG = "메인액티비티에서";
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 9;
    private Mat matInput;
    private Mat matResult;
    ImageButton takePhotoBtn;
    ImageButton changeDirectionBtn;
    ImageButton makeFilterBtn;
    ImageButton overlayBtn;
    String filename;
    Button nothingBtn;
    Button hsvBtn;
    Button smoothBtn;
    Button grayBtn;
    Button roiBtn;
    //전면 카메라, 후면 카메라 방향 전환에 사용.
    private int cameraID;
    int cameraIndexNum = 0;
    int faceFilterNum = 1;

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

    protected float mScale = 0;
    protected FpsMeter mFpsMeter = null;
    private Bitmap mCacheBitmap;
    private Mat edgesMat;
    File dir;
    int imageIndex = 0;
    //얼굴 검출
    private boolean FACEDETECT = false;
    //얼굴에 마스크 씌우기 위해 선언한 변수
    Mat src_roi;
    Mat roi_gray;
    Mat roi_rgb;
    Rect roi;
    Button glassesBtn;
    Button mustacheBtn;
    Button catBlusherBtn;
    Button bearGlassesBtn;
    boolean glassesFilter = false;
    boolean mustacheFilter = false;
    boolean catBlusherFilter = false;
    boolean bearGlassesFilter = false;

    private Mat glasses;
    private Mat mustache;
    private Mat catBlusher;
    private Mat bearGlasses;
    Mat outputSecond;
    Mat result;
    Mat matThrid;
    double scale;

    ImageView testImage;
    private Bitmap testGlasses;
    //회색
    // public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);
    //  public native long loadCascade(String cascadeFileName);
    //public native int detect(long cascadeClassifier_face,

    //                        long cascadeClassifier_eye, long matAddrInput, long matAddrResult, long third_variable);

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
        overlayBtn = findViewById(R.id.overlay_image_btn);
        changeDirectionBtn = findViewById(R.id.change_direction_btn);
        makeFilterBtn = findViewById(R.id.magic_effect_btn);
        nothingBtn = findViewById(R.id.no_effect);
        hsvBtn = findViewById(R.id.hsv_effect);
        smoothBtn = findViewById(R.id.smoothing_effect);
        grayBtn = findViewById(R.id.gray_effect);
        roiBtn = findViewById(R.id.roi_effect);
        testImage = findViewById(R.id.image);
        glassesBtn = findViewById(R.id.glassesbtn);
        mustacheBtn = findViewById(R.id.mustachebtn);
        catBlusherBtn = findViewById(R.id.catBlusherbtn);
        bearGlassesBtn = findViewById(R.id.bearWithGlassesbtn);
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.RECORD_AUDIO}
                    , 0);
            return;
        }
        //외부 저장소 접근 허용 권한
        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
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
        scale = 1;
        try {
            Log.i(TAG, "얼굴 필터 이미지 가져오기 ");

            InputStream glassStream = getAssets().open("glasses.png");
            Bitmap bitmap = BitmapFactory.decodeStream(glassStream);
            glasses = new Mat();
            Utils.bitmapToMat(bitmap, glasses);
            //수염 이미지 가져오기
            InputStream mustachStream = getAssets().open("mustache.png");
            Bitmap mustacheBitmap = BitmapFactory.decodeStream(mustachStream);
            mustache = new Mat();
            Utils.bitmapToMat(mustacheBitmap, mustache);
            //고양이 수염 이미지 가져오기
            InputStream catBlusherStream = getAssets().open("catblusher.png");
            Bitmap blusherBitmap = BitmapFactory.decodeStream(catBlusherStream);
            catBlusher = new Mat();
            Utils.bitmapToMat(blusherBitmap, catBlusher);
            //곰돌이 안경 이미지 가져오기
            InputStream bearStream = getAssets().open("bearGlasses.png");
            Bitmap bearGlassesBitmap = BitmapFactory.decodeStream(bearStream);
            bearGlasses = new Mat();
            Utils.bitmapToMat(bearGlassesBitmap, bearGlasses);

            // testImage.setImageBitmap(bitmap);
            // glasses = Imgcodecs.imread("glasses.png", Imgcodecs.IMREAD_UNCHANGED);
            Log.i(TAG, "안경 이미지 가져왔는지 boolean값으로 확인 : " + glasses.empty());
        } catch (IOException e) {
            Log.i(TAG, "이미지 가져오기 실패 : " + e.getMessage());
        }
        Log.i(TAG, "얼굴과 눈 검출하기 위해 학습시켜 놓은 분류기 로드");
        //녹화 기능
        overlayBtn.setOnClickListener(new View.OnClickListener() {
                                          @Override
                                          public void onClick(View v) {
                                              Log.i(TAG, "얼굴 인식 시작 버튼 클릭");
                                              FACEDETECT = true;
                                              if (glassesBtn.getVisibility() == v.GONE) {
                                                  Log.i(TAG, "필터 안에 버튼 보이도록 하기");
                                                  glassesBtn.setVisibility(v.VISIBLE);
                                              } else {
                                                  glassesBtn.setVisibility(v.GONE);
                                              }
                                              if (mustacheBtn.getVisibility() == v.GONE) {
                                                  mustacheBtn.setVisibility(v.VISIBLE);
                                              } else {
                                                  mustacheBtn.setVisibility(v.GONE);
                                              }
                                              if (catBlusherBtn.getVisibility() == v.GONE) {
                                                  Log.i(TAG, "필터 안에 버튼 보이도록 하기");
                                                  catBlusherBtn.setVisibility(v.VISIBLE);
                                              } else {
                                                  catBlusherBtn.setVisibility(v.GONE);
                                              }
                                              if (bearGlassesBtn.getVisibility() == v.GONE) {
                                                  bearGlassesBtn.setVisibility(v.VISIBLE);
                                              } else {
                                                  bearGlassesBtn.setVisibility(v.GONE);
                                              }
                                          }
                                      }
        );
        glassesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                faceFilterNum++;
                if(faceFilterNum%2==0) {
                    glassesFilter = true;
                }
                else{
                    glassesFilter = false;
                }
            }
        });
        mustacheBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                faceFilterNum++;
                if(faceFilterNum%2==0) {
                    mustacheFilter = true;
                }else{
                    mustacheFilter = false;
                }
            }
        });
        catBlusherBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                faceFilterNum++;
                if(faceFilterNum%2==0) {
                    catBlusherFilter = true;
                }
                else{
                    catBlusherFilter = false;
                }

            }
        });
        bearGlassesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                faceFilterNum++;
                if(faceFilterNum%2==0) {
                    bearGlassesFilter = true;
                }else{
                    bearGlassesFilter = false;
                }

            }
        });
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
                Imgproc.cvtColor(matInput, mIntermediateMat, Imgproc.COLOR_RGBA2RGB, 3);

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
                cameraIndexNum++;
                if (cameraIndexNum % 2 == 0) {
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

    public Mat detectFace(Mat matInput, double scale, Mat glasses) {
        //씌우는 이미지 가져오기


        CascadeClassifier cascadeClassifier = new CascadeClassifier();
        CascadeClassifier cascadeClassifierEye = new CascadeClassifier();

        if (cascadeClassifier.empty()) {
            Log.i(TAG, "얼굴 검출 파일 없어서 가져오기");
            String path = Environment.getExternalStorageDirectory().getAbsolutePath();
            cascadeClassifier.load(path + "/haarcascade_frontalface_alt.xml");
        }

        if (cascadeClassifier.empty()) {
            Log.i(TAG, "얼굴 검출 파일 없음");
        }
        Log.i(TAG, "얼굴 검출할 그레이스케일 이미지 준비.");

        //  Mat mRgba = new Mat(matInput.rows(), matInput.cols(), CvType.CV_8UC3);
        Mat gray = new Mat();
        MatOfRect faces = new MatOfRect();

        //matInput.copyTo(mRgba);
        matInput.copyTo(gray);
        //outputSecond = new Mat();
        // matInput.copyTo(outputSecond);
        Imgproc.cvtColor(matInput, gray, Imgproc.COLOR_RGBA2GRAY);
        //equalizeHiststory
        Imgproc.equalizeHist(gray, gray);
        cascadeClassifier.detectMultiScale(gray, faces, 1.1, 2, 0, new Size(30, 30), new Size());

        Log.i(TAG, "그레이 이미지 리사이즈 하기");
        // resize(gray, resizingGray, new Size(1920, 1080));

        Log.i(TAG, "MatOfRect : Mat를 상속받아 만들어진 클래스.");
        Log.i(TAG, "이미지에서 얼굴 검출");
        //검출되려는 원본 이미지, 오브젝트에 검출된 이미지가 채워짐, 이미지 피라미드에서 사용되는 스케일팩터
        cascadeClassifier.detectMultiScale(gray, faces, 1.1, 2, 0, new Size(30, 30));
        Rect[] facesArray = faces.toArray();

        Log.i(TAG, "얼굴 배열 만듦");
        for (int i = 0; i < facesArray.length; i++) {

            Log.i(TAG, "얼굴 이미지들 포문 안에서 검출");
            //point
            Point centre1 = new Point(facesArray[i].x + facesArray[i].width * 0.5,
                    facesArray[i].y + facesArray[i].height * 0.5);
            Log.i(TAG, "얼굴 갯수 : " + facesArray.length);
            //타원 그리기
            // ellipse(matInput, centre1, new Size(facesArray[i].width * 0.5, facesArray[i].height * 0.5), 0, 0, 360,
            //        new Scalar(255, 0, 255), 4, 8, 0);
            if (glassesFilter) {
                matInput = putMask(matInput, centre1, new Size(facesArray[i].width, facesArray[i].height * 0.7));
            }
            if (mustacheFilter) {

                matInput = putMask(matInput, new Point(facesArray[i].x + facesArray[i].width * 0.5, facesArray[i].y + facesArray[i].height * 0.5 + 60), new Size(facesArray[i].width + 0.4, facesArray[i].height * 0.6));
            }
            if (catBlusherFilter) {
                matInput = putMask(matInput, new Point(facesArray[i].x + facesArray[i].width * 0.5, facesArray[i].y + facesArray[i].height * 0.5 + 30), new Size(facesArray[i].width + 0.4, facesArray[i].height * 0.6));
            }
            if (bearGlassesFilter) {
                matInput = putMask(matInput, new Point(facesArray[i].x + facesArray[i].width * 0.5,
                        facesArray[i].y + facesArray[i].height * 0.3), new Size(facesArray[i].width + 50, facesArray[i].height + 90));

            }
            //그레이 이미지에서 faceArray의 i번째 얼굴을 잘라낸다.
            Mat faceROI = gray.submat(facesArray[i]);
            MatOfRect eyes = new MatOfRect();

            if (cascadeClassifier.empty()) {
                Log.i(TAG, "얼굴 검출 파일 없음");
            }
            if (cascadeClassifierEye.empty()) {
                Log.i(TAG, "눈 검출 위한 파일 가져오기");
                String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                cascadeClassifierEye.load(path + "/haarcascade_eye_tree_eyeglasses.xml");
            }
            if (cascadeClassifierEye.empty()) {
                Log.i(TAG, "눈 검출 파일 없음");
            }
            //검출한 눈 배열
            cascadeClassifierEye.detectMultiScale(faceROI, eyes, 1.1, 4, 0, new Size(30, 30));
            Log.i(TAG, "눈 검출 디텍트 멀티 스케일 메소드하기");

            Rect[] eyesArray = eyes.toArray();
            for (int j = 0; j < eyesArray.length; j++) {

                Point centre2 = new Point(facesArray[i].x + eyesArray[j].x + eyesArray[j].width * 0.5,
                        facesArray[i].y + eyesArray[j].y + eyesArray[j].height * 0.5);
                //원 반지름
                int radius = (int) Math.round((eyesArray[j].width + eyesArray[j].height) * 0.25);
                //원이 그려질 이미지, 원의 중심 좌표, 원의 반지름, 원의 색, 선 굵기, 디폴트값8(선 타입), 디폴트값 0(shift)
            }
        }
        return result;

    }

    //src는 배경
    public Mat putMask(Mat src, Point center, Size face_size) {

        //마스크 사이즈 조정
        Mat mask_resized = new Mat();
//미리보기 이후 쟈른 얼굴의 roi
        src_roi = new Mat();
        roi_gray = new Mat();
        //원본 이미지, 결과 이미지 크기, 줄일 사이즈
        Log.d(TAG, "resize 호출 전" + mask_resized.empty());
        if (mustacheFilter) {
            Imgproc.resize(mustache, mask_resized, face_size);
        }
        if (glassesFilter) {
            Imgproc.resize(glasses, mask_resized, face_size);
        }
        if (catBlusherFilter) {
            Imgproc.resize(catBlusher, mask_resized, face_size);
        }
        if (bearGlassesFilter) {
            Imgproc.resize(bearGlasses, mask_resized, face_size);
        }
        Log.d(TAG, "리사이즈 호출 후" + mask_resized.empty());

        //효과가 그려질부분
        roi = new Rect((int) (center.x - face_size.width / 2), (int) (center.y - face_size.height / 2), (int) face_size.width, (int) face_size.height);
        //src에서 얼굴영억만큼 잘라서 src_roi에 붙인다.///src는 아마도 matInput과 같은 느낌'
        Log.d(TAG, "roi 가로세로길이" + roi.height + "/" + roi.width);
        src.submat(roi).copyTo(src_roi);

        //검정색 부분 투명하게 만들기
        Mat mask_grey = new Mat();
        //선글라스 결과 이미지
        roi_rgb = new Mat();
        //검정색과 흰색으로 색칠. 넣어줄 이미지를 그레이 레벨로 읽어서 나중에 마스크로 사용.
        //make binary images for masking background and foreground images
        Imgproc.cvtColor(mask_resized, mask_grey, Imgproc.COLOR_BGRA2GRAY);
        //이 값을 이용해 그레이 영상에서 배경과 물체를 분리해냄.
        //그레이스케일 이미지, 픽셀 문턱값, 픽셀 문턱값보다 클 때 적용되는 최대값, 문턱값 적용 방법 또는 스타일
        if (glassesFilter) {
            Imgproc.threshold(mask_grey, mask_grey, 230, 255, Imgproc.THRESH_BINARY_INV);
        }
        if (mustacheFilter) {
            Imgproc.threshold(mask_grey, mask_grey, 250, 255, Imgproc.THRESH_BINARY_INV);
        }
        if (catBlusherFilter) {
            Imgproc.threshold(mask_grey, mask_grey, 230, 255, Imgproc.THRESH_BINARY_INV);
        }
        if (bearGlassesFilter) {
            Imgproc.threshold(mask_grey, mask_grey, 230, 255, Imgproc.THRESH_BINARY_INV);

        }
        //마스크 채널 4개 배열 선언(rgba)
        //결과 마스크 4개 배열 만들어서 각각 mat넣음 만듦.
        ArrayList<Mat> maskChannels = new ArrayList<>(4);
        Log.i(TAG, " 마스크 채널 확인 : " + maskChannels.size());
        ArrayList<Mat> result_mask = new ArrayList<>(4);
        result_mask.add(new Mat());
        result_mask.add(new Mat());
        result_mask.add(new Mat());
        result_mask.add(new Mat());

        //4채널을 가진 mask_resized ; 원래는 3채널인 영상을 3개의 1채널 영상으로 분리해줌.(B, G, R)각각의 싱글 채널을 maskchannels에 닮음.
        Core.split(mask_resized, maskChannels);
        //OpenCV 함수: bitwise_and(이미지1, 이미지2, 결과, 마스크)..이미지2가 흑백이미지일경우 이미지2의 흰색부분만 출력
        //비트연산. 이미지에서 특정 영역을 추출할 때 유용하게 사용.
        //mask가 검정색이 아닌 경우만 통과가 되기때문에 mask영역 이외는 모두 제거됨.
        //(inputarray src1, inputarray src2, outputarray dst, inputarray mask = noarray())..mask범위 내에서 두 개의 어레이(src1, src2)의 비트연산and결과
        Core.bitwise_and(maskChannels.get(0), mask_grey, result_mask.get(0));
        Core.bitwise_and(maskChannels.get(1), mask_grey, result_mask.get(1));
        Core.bitwise_and(maskChannels.get(2), mask_grey, result_mask.get(2));
        Core.bitwise_and(maskChannels.get(3), mask_grey, result_mask.get(3));

        //분리됐던 3개의 채널을 다시 하나의 3채널 컬러 영상으로 생성.
        Core.merge(result_mask, roi_gray);

        //not은 색이 반대로 나타나는 것.
        Core.bitwise_not(mask_grey, mask_grey);

        //4개의 채널을 담을 배열 선언.
        ArrayList<Mat> srcChannels = new ArrayList<>(4);
        //4개 채널 가진 src_roi를 split해서 각 싱글 채널을 소스 채널에 담음.
        Core.split(src_roi, srcChannels);

        Core.bitwise_and(srcChannels.get(0), mask_grey, result_mask.get(0));
        Core.bitwise_and(srcChannels.get(1), mask_grey, result_mask.get(1));
        Core.bitwise_and(srcChannels.get(2), mask_grey, result_mask.get(2));
        Core.bitwise_and(srcChannels.get(3), mask_grey, result_mask.get(3));

        Core.merge(result_mask, roi_rgb);

        Core.addWeighted(roi_gray, 1, roi_rgb, 1, 0, roi_rgb);
//roi_rgb를 src의 roi영역에 붙인다.
        roi_rgb.copyTo(new Mat(src, roi));
        Log.d(TAG, "src 리턴" + src.width());

        return src;
    }

    //프레임 전달이 필요한 경우에 호출됨.
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        matInput = inputFrame.rgba();
        if (FACEDETECT) {
            detectFace(matInput, scale, glasses);
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

       /* detect(cascadeClassifier_face, cascadeClassifier_eye, matInput.getNativeObjAddr(),
                matInput.getNativeObjAddr());
*/
        // Core.flip(matInput, matInput, 1);
        return matInput;
    }


    //assets에서 copyfile로 해당 파일을 가져와 외부 저장소의 특정 위치에 저장하도록 구현.
    private void read_cascade_file() {
        copyFile("haarcascade_frontalface_alt.xml");
        copyFile("haarcascade_eye_tree_eyeglasses.xml");
        //loadCascade 메소드는 외부 저장소의 특정 위치에서 해당 파일을 읽어와서

        //CascadeClassifier 객체로 로드합니다.
        // cascadeClassifier_face = loadCascade("haarcascade_frontalface_alt.xml");
        Log.d(TAG, "read_cascade_file:");

        //cascadeClassifier_eye = loadCascade("haarcascade_eye_tree_eyeglasses.xml");
    }

    private void copyFile(String filename) {
        String baseDir = Environment.getExternalStorageDirectory().getPath();
        String pathDir = baseDir + File.separator + filename;

        AssetManager assetManager = this.getAssets();

        InputStream inputStream;
        OutputStream outputStream;

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

    //TODO List<?>: List of unknown. 유연하다는 장점
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
        //cameraViews의 아이템들을 cameraBridgeViewBase에 넣어 루프.
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