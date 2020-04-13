#include <jni.h>
#include<stdio.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <opencv2/core/mat.hpp>

using namespace cv;
using namespace std;




extern "C"
JNIEXPORT void JNICALL
Java_com_gayeon_yourcamera_imagePopup_imageprocessing(JNIEnv *env,
                                                                           jobject instance,
                                                                           jlong inputImage,
                                                                           jlong outputImage,

                                                                           jint th1,

                                                                           jint th2) {



    Mat &imageInput = *(Mat *) inputImage;

    Mat &imageResult = *(Mat *) outputImage;



    cvtColor( imageInput, imageResult, COLOR_RGB2GRAY);


    blur( imageResult, imageResult, Size(5,5) );

    Canny( imageResult, imageResult, th1, th2);


}


/*extern "C"
JNIEXPORT void JNICALL
Java_com_gayeon_yourcamera_MainActivity_ConvertRGBtoGray(JNIEnv *env, jobject thiz,
                                                         jlong mat_addr_input,
                                                         jlong mat_addr_result) {
    Mat &matInput = *(Mat *)mat_addr_input;
    Mat &matResult = *(Mat *)mat_addr_result;

    cvtColor(matInput, matResult, COLOR_RGBA2GRAY);



}*/

/*
extern "C"
JNIEXPORT void JNICALL
Java_com_gayeon_yourcamera_MainActivity_detect(JNIEnv *env, jobject instance,
        jlong cascadeClassifier_face,
jlong cascadeClassifier_eye,
                                               jobject img, jdouble scale, jboolean tryflip,jobject glasses) {

    Mat &matInput = *(Mat *) img;
    Mat &glasses = *(Mat *) glasses;



    float resize(Mat img_src, Mat &img_resize, int resize_width) {


        float scale = resize_width / (float) img_src.cols;

        if (img_src.cols > resize_width) {

            int new_height = cvRound(img_src.rows * scale);

            resize(img_src, img_resize, Size(resize_width, new_height));

        } else {

            img_resize = img_src;

        }

        return scale;

    }
*/
