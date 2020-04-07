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

class Main : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private var mOpenCVcameraview: CameraBridgeViewBase? = null
    val scale: Double = 1.0
    var FACEDETECT: Boolean = false
    lateinit var matResult: Mat
    var matInput: Mat? = null
    var mat1: Mat? = null
    var mat2: Mat? = null
    private val glasses: Mat? = null
    var RGBA: Int? = null
    var GrayScale: Int? = null
    var HSV: Int? = null
    var Smoothing: Int? = null
    var ROI: Int? = null
    var fileName: String? = null
    val TAG: String = "메인에서"
    var directionChangeNum: Int = 0;
    var result: Mat? = null
    lateinit var scalarLow: Scalar
    lateinit var scalarHigh: Scalar

    companion object {

    }

    protected override fun onCreate(savedInstanceState: Bundle?) {
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
            val inputStream: InputStream = assets.open("glasses.png")
            val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)

            val glasses: Mat? = null
            Utils.bitmapToMat(bitmap, glasses)
            Imgcodecs.imread("glasses.png", Imgcodecs.IMREAD_UNCHANGED)

        } catch (e: IOException) {

        }

        video_record_btn.findViewById<View>(R.id.video_record_btn).setOnClickListener {

            FACEDETECT = true
            detectFace(matInput!!, scale, glasses!!)
        }

        magic_effect_btn.findViewById<View>(R.id.magic_effect_btn).setOnClickListener {
            if (no_effect.findViewById<View>(R.id.no_effect)!!.visibility == View.GONE) {
                no_effect.visibility = View.VISIBLE
            }
            if (hsv_effect.findViewById<View>(R.id.hsv_effect)!!.visibility == View.GONE) {
                hsv_effect.visibility = View.VISIBLE
            }
            if (smoothing_effect.findViewById<View>(R.id.smoothing_effect)!!.visibility == View.GONE) {
                smoothing_effect.visibility = View.VISIBLE
            }
            if (gray_effect.findViewById<View>(R.id.gray_effect)!!.visibility == View.GONE) {
                gray_effect.visibility = View.VISIBLE
            }
            if (roi_effect.findViewById<View>(R.id.roi_effect)!!.visibility == View.GONE) {
                roi_effect.visibility = View.VISIBLE
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
            var simpleDateFormat: SimpleDateFormat? = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            var currentDateAndTime: String = simpleDateFormat!!.format(Date())
            var intermediateMat: Mat? = null
            Imgproc.cvtColor(matInput, intermediateMat, Imgproc.COLOR_RGBA2BGR, 3)
            var path: File = File(Environment.getExternalStorageDirectory().toString() + "/Images/")
            path.mkdirs()
            var file: File = File(path, currentDateAndTime + "image.png");
            fileName = file.toString()
            //사진 찍기
            mOpenCVcameraview!!.takePicture(fileName)
            var boolean: Boolean = Imgcodecs.imwrite(fileName, intermediateMat)

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

    public fun detectFace(matInput: Mat, scale: Double, glasses: Mat) {

        var cascadeClassifier: CascadeClassifier? = null
        var cascadeClassifierEye: CascadeClassifier? = null

        if (cascadeClassifier!!.empty()) {
            Log.i(TAG, "얼굴 검출 파일 없어서 가져오기")

            var path: String = Environment.getExternalStorageDirectory().absolutePath
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

        Imgproc.cvtColor(matInput, gray, Imgproc.COLOR_BGRA2GRAY)
        Imgproc.equalizeHist(gray, gray)
        cascadeClassifier.detectMultiScale(gray, faces, 1.1, 2, 0, Size(30.0, 30.0), Size())

        cascadeClassifier.detectMultiScale(gray, faces, 1.1, 2, 0, Size(30.0, 30.0))
        //var faceArray: Rect[]
        var facesArray = faces!!.toArray()
        for (item: Int in facesArray.indices) {
            var centerFace: Point = Point(facesArray[item].x + facesArray[item].width * 0.5,
                    facesArray[item].y + facesArray[item].height * 0.5)

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

            if (eyesArray.size == 2) {
                var centerFirst: Point = Point(facesArray[item].x + eyesArray[0].x + eyesArray[0].width * 0.5,
                        facesArray[item].y + eyesArray[0].y + eyesArray[0].height * 0.5);

                var centerSecond: Point = Point(facesArray[item].x + eyesArray[1].x + eyesArray[1].width * 0.5,
                        facesArray[item].y + eyesArray[1].y + eyesArray[1].height * 0.5)

                if (centerFirst.x > centerSecond.x) {
                    var temporary: Point? = null
                    temporary = centerFirst
                    centerFirst = centerSecond
                    centerSecond = temporary
                }

                var width: Int = Math.abs(centerSecond.x - centerFirst.x).toInt()
                var height: Int = Math.abs(centerSecond.y - centerFirst.y).toInt()

                if (width > height) {
                    var imageScale: Float = (width / 330.0).toFloat()

                    var glassSizeWidth: Int
                    var glassSizeHeight: Int

                    glassSizeWidth = glasses.cols() * imageScale.toInt()
                    glassSizeHeight = glasses.rows() * imageScale.toInt()

                    var offsetX: Int = 150 * imageScale.toInt()
                    var offsetY: Int = 160 * imageScale.toInt()

                    var outputSecond: Mat? = null
                    faceROI?.copyTo(outputSecond)

                    val resizedGlasses: Mat? = null
                    Imgproc.resize(glasses, resizedGlasses, Size(glassSizeWidth.toDouble(), glassSizeHeight.toDouble()))
                    overlayImage(outputSecond!!, resizedGlasses!!, result!!, Point(centerFirst.x - offsetX, centerFirst.y - offsetY))
                    outputSecond = result
                }


            }


        }
    }

    fun overlayImage(background: Mat, foreground: Mat, output: Mat, location: Point) {

        background.copyTo(output)
        var y = location.y.toInt().coerceAtLeast(0)
        val backgroundRows: Int? = null
        for (y in 0 until backgroundRows!!) {
            var fY: Int = (y - location.y).toInt()
            if (fY >= foreground.rows()) {
                break
            }

            var x = location.x.toInt().coerceAtLeast(0)
            val backgroundCols: Int? = null
            for (x in 0 until backgroundCols!!) {
                var fX: Int = (x - location.x).toInt()
                if (fX >= foreground.cols()) {
                    break
                }

                var opacity: Double
                val finalPixelValue = DoubleArray(4)
                opacity = foreground.get(fY, fX)[3]

                finalPixelValue[0] = background.get(y, x)[0]
                finalPixelValue[1] = background[y, x][1]
                Log.i(TAG, " 오버레이 백그라운도 파이널픽셀밸류 번째: " + finalPixelValue[1])
                finalPixelValue[2] = background[y, x][2]
                Log.i(TAG, " 오버레이 백그라운도 파이널픽셀밸류 번째: " + finalPixelValue[2])
                finalPixelValue[3] = background[y, x][3]
                Log.i(TAG, " 오버레이 백그라운도 파이널픽셀밸류 세번째: " + finalPixelValue[3])


                for (c in 0 until output.channels()) {
                    if (opacity > 0) {
                        var foregroundPx: Double = foreground.get(fY, fX)[c]
                        var backgroundPx: Double = background.get(y, x)[c]
                        var fOpacity: Float = (opacity / 255).toFloat()

                        if (c == 3) {
                            finalPixelValue[c] = foreground.get(fY, fX)[3]
                        }
                    }
                }
                output.put(y, x, finalPixelValue.last())
            }
        }


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
            return matResult
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
            matInput = mat2
        }
        if (Smoothing == 1) {
            val size: Size = Size(21.0, 21.0)
            Imgproc.boxFilter(matInput, matInput, -1, size)
        }
        if (ROI == 1) {
            Imgproc.cvtColor(inputFrame.rgba(), mat1, Imgproc.COLOR_RGB2HSV)
            Core.bitwise_and(matInput, matInput, mat1, mat2)
            matInput = mat1
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
            requestPermissions(arrayOf(CAMERA, WRITE_EXTERNAL_STORAGE),CAMERA_PERMISSION_REQUEST_CODE)
        }
        builder.setNegativeButton("아니요") { dialogInterface: DialogInterface, i: Int ->
            finish()
        }
        builder.create().show()
    }
}