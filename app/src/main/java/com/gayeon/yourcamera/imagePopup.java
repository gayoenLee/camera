package com.gayeon.yourcamera;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

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
    LinearLayout faceLayout;


    String TAG = "이미지팝업";
    int RGBA;
    int GrayScale;
    int HSV;
    int Smoothing;
    int ROI;
    Button backgroundFilterBtn;


    private Mat glasses;
    private Mat mustache;
    private Mat catBlusher;
    private Mat bearGlasses;

    Mat src_roi;
    Mat roi_gray;
    Mat roi_rgb;
    Rect roi;
    Mat result;

    boolean glassesFilter = false;
    boolean mustacheFilter = false;
    boolean catBlusherFilter = false;
    boolean bearGlassesFilter = false;

    Button glassesBtn;
    Button mustacheBtn;
    Button catBlusherBtn;
    Button bearGlassesBtn;
    int faceFilterNum = 1;

    double scale;
    Bitmap resultBitmap;

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
      //  filterResultImageView = findViewById(R.id.imageViewOutput);

        backgroundFilterBtn = findViewById(R.id.background_filter);
        edgeBtn = findViewById(R.id.edge_button);
        faceFilterBtn = findViewById(R.id.face_filter);
        backgroundLayout = findViewById(R.id.background_filter_linear);
        faceLayout = findViewById(R.id.face_filter_linear);
        glassesBtn = findViewById(R.id.glassesbtn);
        mustacheBtn = findViewById(R.id.mustachebtn);
        catBlusherBtn = findViewById(R.id.catBlusherbtn);
        bearGlassesBtn = findViewById(R.id.bearWithGlassesbtn);
        context = this;
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
        final Bitmap resized = Bitmap.createScaledBitmap(bitmap, imageWidth, imageHeight, true);

        imageInput = new Mat();
        Bitmap bitmapResult = resized.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bitmapResult, imageInput);

        galleryImage.setImageBitmap(resized);

        //얼굴 마스크 씌우기
        //비트맵 이미지 매트로 바꿔서 메소드에 적용.

        Utils.bitmapToMat(resized, imageInput);

        faceFilterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(backgroundFilterBtn.getVisibility() == view.GONE){
                    backgroundFilterBtn.setVisibility(view.VISIBLE);
                }else{
                    backgroundFilterBtn.setVisibility(view.GONE);
                    faceLayout.setVisibility(view.VISIBLE);
                    //얼굴 인식 메소드 실행
                }
            }
        });

        glassesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                faceFilterNum++;
                if (faceFilterNum % 2 == 0) {
                    glassesFilter = true;

                    detectFace(imageInput, scale, glasses);
                    Log.i(TAG, "얼굴 인식 메소드 실행");
                    if(imageInput == null){
                        Log.i(TAG, "이미지 인풋이 널일 때");
                        imageInput = new Mat();
                    }
                    Bitmap bitmapOutput = Bitmap.createBitmap(imageInput.cols(), imageInput.rows(), Bitmap.Config.ARGB_8888, true);
                    Utils.matToBitmap(imageInput, bitmapOutput);
                    galleryImage.setImageBitmap(null);
                    galleryImage.setImageBitmap(bitmapOutput);
                } else {
                    glassesFilter = false;
                    if(imageInput == null){
                        Log.i(TAG, "이미지 인풋이 널일 때");
                        imageInput = new Mat();
                    }
                    galleryImage.setImageBitmap(resized);
                }
            }
        });
        mustacheBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                faceFilterNum++;
                if (faceFilterNum % 2 == 0) {
                    mustacheFilter = true;

                    detectFace(imageInput, scale, glasses);
                    Log.i(TAG, "얼굴 인식 메소드 실행");
                    if(imageInput == null){
                        Log.i(TAG, "이미지 인풋이 널일 때");
                        imageInput = new Mat();
                    }
                    Bitmap bitmapOutput = Bitmap.createBitmap(imageInput.cols(), imageInput.rows(), Bitmap.Config.ARGB_8888, true);
                    Utils.matToBitmap(imageInput, bitmapOutput);
                    galleryImage.setImageBitmap(null);
                    galleryImage.setImageBitmap(bitmapOutput);
                } else {
                    mustacheFilter = false;
                    if(imageInput == null){
                        Log.i(TAG, "이미지 인풋이 널일 때");
                        imageInput = new Mat();
                    }
                    galleryImage.setImageBitmap(resized);
                }
            }
        });
        catBlusherBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                faceFilterNum++;
                if (faceFilterNum % 2 == 0) {
                    catBlusherFilter = true;

                    detectFace(imageInput, scale, glasses);
                    Log.i(TAG, "얼굴 인식 메소드 실행");
                    if(imageInput == null){
                        Log.i(TAG, "이미지 인풋이 널일 때");
                        imageInput = new Mat();
                    }
                    Bitmap bitmapOutput = Bitmap.createBitmap(imageInput.cols(), imageInput.rows(), Bitmap.Config.ARGB_8888, true);
                    Utils.matToBitmap(imageInput, bitmapOutput);
                    galleryImage.setImageBitmap(null);
                    galleryImage.setImageBitmap(bitmapOutput);
                } else {
                    catBlusherFilter = false;
                    if(imageInput == null){
                        Log.i(TAG, "이미지 인풋이 널일 때");
                        imageInput = new Mat();
                    }
                    galleryImage.setImageBitmap(resized);
                }

            }
        });
        bearGlassesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                faceFilterNum++;
                if (faceFilterNum % 2 == 0) {
                    bearGlassesFilter = true;
                    detectFace(imageInput, scale, glasses);
                    Log.i(TAG, "얼굴 인식 메소드 실행");

                    if(imageInput == null){
                        Log.i(TAG, "이미지 인풋이 널일 때");
                        imageInput = new Mat();
                    }
                    Bitmap bitmapOutput = Bitmap.createBitmap(imageInput.cols(), imageInput.rows(), Bitmap.Config.ARGB_8888, true);
                    Utils.matToBitmap(imageInput, bitmapOutput);
                    galleryImage.setImageBitmap(bitmapOutput);
                } else {
                    bearGlassesFilter = false;
                    if(imageInput == null){
                        Log.i(TAG, "이미지 인풋이 널일 때");
                        imageInput = new Mat();
                    }
galleryImage.setImageBitmap(resized);

                }
            }
        });


//--------------------------------------------------------------------배경 필터 적용
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
                        facesArray[i].y + facesArray[i].height * 0.3), new Size(facesArray[i].width , facesArray[i].height ));

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

    @Override
    protected void onResume() {
        super.onResume();

        isReady = true;
    }


}
