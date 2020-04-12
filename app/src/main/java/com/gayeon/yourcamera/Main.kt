package com.gayeon.yourcamera

import android.Manifest
import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.*
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class Main : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private var mOpenCVcameraview: CameraBridgeViewBase? = null
    val scale: Double = 1.0
    var FACEDETECT: Boolean = false
    private lateinit var matResult: Mat
    private lateinit var matInput: Mat
    private var mat1: Mat? = null
    private var mat2: Mat? = null
    private val glasses: Mat? = null
    private val mustache:Mat? = null
    private val catBlusher:Mat? = null
    private val bearGlasses:Mat? = null
    var RGBA: Int? = null
    var GrayScale: Int? = null
    var HSV: Int? = null
    var Smoothing: Int? = null
    var ROI: Int? = null
    private var fileName: String? = null
    val TAG: String = "메인에서"
    private var directionChangeNum: Int = 0;
    private var faceFilterNum: Int = 1;
    private var result: Mat? = null
    private lateinit var scalarLow: Scalar
    private lateinit var scalarHigh: Scalar
    //미리보기 이후 쟈른 얼굴의 roi
    private lateinit var src_roi: Mat
    private lateinit var roi_gray: Mat
    private lateinit var roi: Rect
    private lateinit var roi_rgb: Mat
    private var glassesFilter = false
    private var mustacheFilter = false
    private var catBlusherFilter = false
    private var bearGlassesFilter = false;


    init {
        System.loadLibrary("opencv_java4")
        System.loadLibrary("native-lib")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            ActivityCompat.requestPermissions(this, permissions, 0)
            return
        }

        val secondPermission = arrayOf(WRITE_EXTERNAL_STORAGE)
        //권한 부여됐는지 확인
        // if(ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
        //권한이 허용되지 않음
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, secondPermission, 1);
            return
        }

//상태바 없애기
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mOpenCVcameraview = findViewById(R.id.activity_surface_view)
        mOpenCVcameraview!!.visibility = SurfaceView.VISIBLE
        mOpenCVcameraview!!.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        mOpenCVcameraview!!.setCvCameraViewListener(this)
        var cameraID: Int = 0
        mOpenCVcameraview!!.setCameraIndex(0)
        scalarLow = Scalar((120 - 10.0), 30.0, 30.0)
        scalarHigh = Scalar((120 + 10.0), 255.0, 255.0)

        try {
            val glassesStream: InputStream = assets.open("glasses.png")
            val bitmap: Bitmap = BitmapFactory.decodeStream(glassesStream)


            Utils.bitmapToMat(bitmap, glasses)
            Imgcodecs.imread("glasses.png", Imgcodecs.IMREAD_UNCHANGED)

            val mustacheStream: InputStream = assets.open("mustache.png")
            val mustacheBitmap = BitmapFactory.decodeStream(mustacheStream)
            val mustacheMat:Mat? = null
            Utils.bitmapToMat(mustacheBitmap, mustacheMat)
            Imgcodecs.imread("mustache.png", Imgcodecs.IMREAD_UNCHANGED)

            val blusherStream:InputStream = assets.open("catblusher.png")
            val blusherBitmap = BitmapFactory.decodeStream(blusherStream)
            val blusherMat:Mat? = null
            Utils.bitmapToMat(blusherBitmap, blusherMat)
            Imgcodecs.imread("catblusher.png", Imgcodecs.IMREAD_UNCHANGED)

            val bearGlassesStream:InputStream = assets.open("bearGlasses.png")
            val bearGlassesBitmap = BitmapFactory.decodeStream(bearGlassesStream)
            val bearGlassesMat:Mat? = null
            Utils.bitmapToMat(bearGlassesBitmap, bearGlassesMat)
            Imgcodecs.imread("bearGlasses.png", Imgcodecs.IMREAD_UNCHANGED)

        } catch (e: IOException) {

        }

        overlay_image_btn.setOnClickListener {

            FACEDETECT = true
            if(glassesbtn.visibility == View.GONE){
                View.VISIBLE
            }
            if(mustachebtn.visibility == View.GONE){
                View.VISIBLE
            }
            if(catBlusherbtn.visibility == View.GONE){
                View.VISIBLE
            }
            if(bearWithGlassesbtn.visibility == View.GONE){
                View.VISIBLE
            }else{
                View.GONE
            }
        }
        glassesbtn.setOnClickListener {
            faceFilterNum++
            glassesFilter = faceFilterNum % 2 == 0
        }
        mustachebtn.setOnClickListener{
            faceFilterNum++
            mustacheFilter = faceFilterNum % 2 == 0
        }

        catBlusherbtn.setOnClickListener{
            faceFilterNum++
            catBlusherFilter = faceFilterNum % 2 == 0
        }
        bearWithGlassesbtn.setOnClickListener {
            faceFilterNum ++
            bearGlassesFilter = faceFilterNum % 2 == 0
        }

        magic_effect_btn.setOnClickListener {
            if (no_effect!!.visibility == View.GONE) {
                View.VISIBLE
            }
            if (hsv_effect!!.visibility == View.GONE) {
                 View.VISIBLE
            }
            if (smoothing_effect!!.visibility == View.GONE) {
                 View.VISIBLE
            }
            if (gray_effect!!.visibility == View.GONE) {
                View.VISIBLE
            }
            if (roi_effect!!.visibility == View.GONE) {
               View.VISIBLE
            }
        }

        no_effect.setOnClickListener {
            RGBA = 1
            GrayScale = 0
            HSV = 0
            Smoothing = 0
            ROI = 0

        }
        hsv_effect.setOnClickListener {
            RGBA = 0
            GrayScale = 0
            HSV = 1
            Smoothing = 0
            ROI = 0
        }
        smoothing_effect.setOnClickListener {
            RGBA = 0
            GrayScale = 0
            HSV = 0
            Smoothing = 1
            ROI = 0
        }
        gray_effect.setOnClickListener {
            RGBA = 0
            GrayScale = 1
            HSV = 0
            Smoothing = 0
            ROI = 0
        }
        roi_effect.setOnClickListener {
            RGBA = 0
            GrayScale = 0
            HSV = 0
            Smoothing = 0
            ROI = 1
        }

        take_photo.setOnClickListener {
            val simpleDateFormat: SimpleDateFormat? = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            val currentDateAndTime: String = simpleDateFormat!!.format(Date())
            val intermediateMat: Mat? = null
            Imgproc.cvtColor(matInput, intermediateMat, Imgproc.COLOR_RGBA2BGR, 3)
            val path: File = File(Environment.getExternalStorageDirectory().toString() + "/Images/")
            path.mkdirs()
            val file: File = File(path, currentDateAndTime + "image.png");
            fileName = file.toString()
            //사진 찍기
            mOpenCVcameraview!!.takePicture(fileName)
            val boolean: Boolean = Imgcodecs.imwrite(fileName, intermediateMat)

            if (boolean)
                Log.i(TAG, "SUCCESS writing image to external storage")
            else
                Log.i(TAG, "Fail writing image to external storage")

        }
        change_direction_btn.setOnClickListener {
            directionChangeNum++;
            if (directionChangeNum % 2 == 0) {
                cameraID = 0;
                mOpenCVcameraview!!.setCameraIndex(cameraID)
                mOpenCVcameraview!!.disableView()
                mOpenCVcameraview!!.enableView()
            } else {
                cameraID = 1
                mOpenCVcameraview!!.disableView()
                mOpenCVcameraview!!.setCameraIndex(cameraID)
                //TODO 확인
                mOpenCVcameraview!!.scaleX = (-1).toFloat()
                mOpenCVcameraview!!.enableView()
            }
        }

    }

   fun detectFace(matInput: Mat, scale: Double, glasses: Mat): Mat? {
lateinit var matInput:Mat
        val cascadeClassifier: CascadeClassifier? = null
        val cascadeClassifierEye: CascadeClassifier? = null

        if (cascadeClassifier!!.empty()) {
            Log.i(TAG, "얼굴 검출 파일 없어서 가져오기")

            val path: String = Environment.getExternalStorageDirectory().absolutePath
            cascadeClassifier.load(path + "/haarcascade_frontalface_alt.xml")

        }

        if (cascadeClassifier!!.empty()) {
            Log.i(TAG, "얼굴 검출 파일 없음");
        }

        var mRgba: Mat? = null
        var gray: Mat? = null
        var faces: MatOfRect? = null

        matInput.copyTo(mRgba)
        matInput.copyTo(gray)

        Imgproc.cvtColor(matInput, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.equalizeHist(gray, gray)
        cascadeClassifier.detectMultiScale(gray, faces, 1.1, 2, 0, Size(30.0, 30.0), Size())

        cascadeClassifier.detectMultiScale(gray, faces, 1.1, 2, 0, Size(30.0, 30.0))
        //var faceArray: Rect[]
       //배열의 인덱스를 사용해서 값을 가지고 오고 싶을 때 indices사용.
        var facesArray = faces!!.toArray()
        for (item: Int in facesArray.indices) {
            var centerFace = Point(facesArray[item].x + facesArray[item].width * 0.5,
                    facesArray[item].y + facesArray[item].height * 0.5)

            if(glassesFilter){
                matInput = putMask(matInput, centerFace, Size(facesArray[item].width * 0.8, facesArray[item].height * 0.4))
            }
            if(mustacheFilter){
matInput = putMask(matInput, Point(facesArray[item].x + facesArray[item].width * 0.5, facesArray[item].y + facesArray[item].height * 0.5 + 60), Size(facesArray[item].width + 0.4, facesArray[item].height * 0.6))
                
            }
            if(catBlusherFilter){
matInput = putMask(matInput, Point(facesArray[item].x + facesArray[item].width * 0.5, facesArray[item].y + facesArray[item].height * 0.5 + 30), Size(facesArray[item].width + 0.4, facesArray[item].height * 0.6) )
            }
            if(bearGlassesFilter){
                matInput = putMask(matInput, centerFace, Size(facesArray[item].width * 0.8, facesArray[item].height * 0.4))

            }
            var faceROI = gray?.submat(facesArray[item])
            var eyes: MatOfRect? = null

            if (cascadeClassifier.empty()) {
                Log.i(TAG, "얼굴 검출 파일 없음");
            }
            if (cascadeClassifierEye!!.empty()) {
                var path = Environment.getExternalStorageDirectory().absolutePath
                cascadeClassifierEye.load(path + "/haarcascade_eye_tree_eyeglasses.xml")
            }
            if (cascadeClassifierEye.empty()) {
                Log.i(TAG, "눈 검출 파일 없음")
            }
            cascadeClassifierEye.detectMultiScale(faceROI, eyes, 1.1, 4, 0, Size(30.0, 30.0))
            var eyesArray = eyes!!.toArray()


                }
       return result
            }

    fun putMask(src: Mat, center: Point, face_size: Size): Mat {

        var mask_resized = Mat()
        src_roi = Mat()
        roi_gray = Mat()
        if(glassesFilter) {
            Imgproc.resize(glasses, mask_resized, face_size)
        }
        if(mustacheFilter){
            Imgproc.resize(mustache, mask_resized, face_size)

        }
        if(catBlusherFilter){
            Imgproc.resize(catBlusher, mask_resized, face_size)

        }
        if(bearGlassesFilter){
            Imgproc.resize(bearGlasses, mask_resized, face_size)

        }
        roi = Rect((center.x - face_size.width / 2).toInt(), (center.y - face_size.height / 2).toInt(), face_size.width.toInt(), face_size.height.toInt())
        src.submat(roi).copyTo(src_roi)

        var mask_grey = Mat()
        roi_rgb = Mat()

        Imgproc.cvtColor(mask_resized, mask_resized, Imgproc.COLOR_BGRA2GRAY)
        Imgproc.threshold(mask_grey, mask_grey, 230.0, 255.0, Imgproc.THRESH_BINARY_INV)

        val maskChannels: ArrayList<Mat> = ArrayList(4)
        val result_mask: ArrayList<Mat> = ArrayList(4)
        result_mask.add(Mat())
        result_mask.add(Mat())
        result_mask.add(Mat())
        result_mask.add(Mat())

        Core.split(mask_resized, maskChannels)

        Core.bitwise_and(maskChannels.get(0), mask_grey, result_mask.get(0))
        Core.bitwise_and(maskChannels.get(1), mask_grey, result_mask.get(1))
        Core.bitwise_and(maskChannels.get(2), mask_grey, result_mask.get(2))
        Core.bitwise_and(maskChannels.get(3), mask_grey, result_mask.get(3))

        //분리됐던 3개의 채널을 다시 하나의 3채널 컬러 영상으로 생성.
        Core.merge(result_mask, roi_gray)

        //not은 색이 반대로 나타나는 것.
        //not은 색이 반대로 나타나는 것.
        Core.bitwise_not(mask_grey, mask_grey)

        val srcChannels: ArrayList<Mat> = ArrayList(4)
        //4개 채널 가진 src_roi를 split해서 각 싱글 채널을 소스 채널에 담음.
        Core.split(src_roi, srcChannels)
        Core.bitwise_and(srcChannels[0], mask_grey, result_mask[0])
        Core.bitwise_and(srcChannels[1], mask_grey, result_mask[1])
        Core.bitwise_and(srcChannels[2], mask_grey, result_mask[2])
        Core.bitwise_and(srcChannels[3], mask_grey, result_mask[3])

        Core.merge(result_mask, roi_rgb)

        Core.addWeighted(roi_gray, 1.0, roi_rgb, 1.0, 0.0, roi_rgb)
//roi_rgb를 src의 roi영역에 붙인다.
        //roi_rgb를 src의 roi영역에 붙인다.
        roi_rgb.copyTo(Mat(src, roi))
        Log.d(TAG, "src 리턴" + src.width())

        return src


    }


    private fun convertMatToBitmap(input: Mat): Bitmap {
        var bitmap: Bitmap? = null
        var rgb = Mat()
        Imgproc.cvtColor(input, rgb, Imgproc.COLOR_BGR2RGB)
        try {
            bitmap = Bitmap.createBitmap(rgb.cols(), rgb.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(rgb, bitmap)
        } catch (e: Exception) {

        }
        return bitmap!!
    }

    private fun read_cascade_file() {
        copyFile("haarcascade_frontalface_alt.xml")
        copyFile("haarcascade_eye_tree_eyeglasses.xml")
    }

    private fun copyFile(filename: String) {
        var baseDir = Environment.getExternalStorageDirectory().path
        var pathDir = baseDir + File.separator + filename

        var assetManager: AssetManager = this.assets;



        try {
            val inputStream: InputStream = assets.open(filename)
            var outputStream = FileOutputStream(pathDir)

            var buffer = ByteArray(1024)
            var read: Int
//TODO also, it
            while (inputStream.read(buffer).also { read = it } != -1)
                outputStream.write(buffer, 0, read)

            inputStream.close()
            outputStream.flush()
            outputStream.close()

        } catch (e: IOException) {

        }
    }

    private val loader = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    mOpenCVcameraview!!.enableView()
                }
                else -> super.onManagerConnected(status)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (mOpenCVcameraview != null) {
            mOpenCVcameraview!!.disableView()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, loader)
        } else {
            loader.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mOpenCVcameraview != null) {
            mOpenCVcameraview!!.disableView()
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCameraViewStopped() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        matInput = inputFrame!!.rgba()
        if (FACEDETECT) {
            detectFace(matInput!!, scale, glasses!!)
            return matInput
        }

        if (GrayScale == 1) {
            matInput = inputFrame.gray()
        }
        if (RGBA == 1) {
            matInput = inputFrame.rgba()
        }
        if (HSV == 1) {
            Imgproc.cvtColor(inputFrame.rgba(), mat1, Imgproc.COLOR_RGB2HSV)
            Core.inRange(mat1, scalarLow, scalarHigh, mat2)
            matInput = this.mat2!!
        }
        if (Smoothing == 1) {
            val size: Size = Size(21.0, 21.0)
            Imgproc.boxFilter(matInput, matInput, -1, size)
        }
        if (ROI == 1) {
            Imgproc.cvtColor(inputFrame.rgba(), mat1, Imgproc.COLOR_RGB2HSV)
            Core.bitwise_and(matInput, matInput, mat1, mat2)
            matInput = this.mat1!!
        }


        return matInput!!
    }

    protected fun getCameraViewList(): MutableList<out CameraBridgeViewBase?> {
        return java.util.Collections.singletonList(mOpenCVcameraview)
    }

    protected fun onCameraPermissionGranted() {
        val cameraViews: MutableList<out CameraBridgeViewBase?> = getCameraViewList()
        if (mOpenCVcameraview == null) {
            return
        }
        for (cameraBridegeViewBase: CameraBridgeViewBase? in cameraViews) {
            if (cameraBridegeViewBase != null) {
                cameraBridegeViewBase.setCameraPermissionGranted()
                read_cascade_file()
            }
        }
    }

    private val CAMERA_PERMISSION_REQUEST_CODE = 200

    protected override fun onStart() {
        super.onStart()
        var havePermission = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //TODO 코드 확인하기
                requestPermissions(arrayOf(CAMERA, WRITE_EXTERNAL_STORAGE), CAMERA_PERMISSION_REQUEST_CODE)
                havePermission = false
            }
        }
        if (havePermission) {
            onCameraPermissionGranted()
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            onCameraPermissionGranted()
        } else {
            showDialogForPermission("앱을 싫애하려면 퍼미션을 허가하셔야합니다.")
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showDialogForPermission(msg: String) {
        var builder = AlertDialog.Builder(this)
        builder.setTitle("알림")
        builder.setMessage(msg)
        builder.setCancelable(false)
        builder.setPositiveButton("예") { dialogInterface: DialogInterface, i: Int ->
            requestPermissions(arrayOf(CAMERA, WRITE_EXTERNAL_STORAGE), CAMERA_PERMISSION_REQUEST_CODE)
        }
        builder.setNegativeButton("아니요") { dialogInterface: DialogInterface, i: Int ->
            finish()
        }
        builder.create().show()
    }
}