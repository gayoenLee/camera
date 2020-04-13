package com.gayeon.yourcamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.File;

public class galleryAdapter extends BaseAdapter {

    String TAG = "갤러리 어댑터에서";
    int galleryAdapterItemBackground;
    String basePath;
    Context mContext;
    //위의 경로 내의 파일 리스트를 스트링 배열로 저장받을 변수
    String[] images;
    Bitmap bitmap;

    public galleryAdapter(Context context, String basePath){
        this.mContext = context;
        this.basePath = basePath;
        boolean havePermission = true;

        //지정 경로의 디렉토리를 파일 변수로 받음
        File file = new File(basePath);
        if(!file.exists()){
            if(!file.mkdirs()){
                Log.i(TAG, "디렉토리 만들기 실패");
            }
        }
        //디렉토리 내 파일명들을 스트링 배열에 저장
        images = file.list();

        TypedArray typedArray = mContext.obtainStyledAttributes(R.styleable.GalleryTheme);
        galleryAdapterItemBackground = typedArray.getResourceId(R.styleable.GalleryTheme_android_galleryItemBackground, 0);
        typedArray.recycle();
    }
    public final void callImageViewer(int selectedIndex){
        Intent intent = new Intent(mContext,imagePopup.class );
        intent.putExtra("filename",basePath );
        intent.putExtra("position", images[selectedIndex]);

        ((Activity)mContext).startActivityForResult(intent, 88);
        Log.i(TAG, "콜이미지뷰어 어댑터에서 실행됨");

    }

    //attrs.xml에서 갤러리 어레이의 배경 style attribute를 받아옴.


//갤러리 어레이의 객체 갯수를 앞에서 세어 둔 파일 리스트를 받은 스트링 배열의 우너소 갯수와 동일하다는 가정 하에 반환.
    @Override
    public int getCount() {
        Log.i(TAG, "이미지 길이 확인 : " + images.length);
        return images.length;
    }

    @Override
    public Object getItem(int position) {

        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if(convertView == null){
            imageView = new ImageView(mContext);
        }else{
            imageView = (ImageView)convertView;
        }
        bitmap = BitmapFactory.decodeFile(basePath + File.separator + images[position]);
        Log.i(TAG, "갤러리어댑터에서 비트맵 확인 : "+ basePath + File.separator + images[position]);
        Bitmap thumbnail = ThumbnailUtils.extractThumbnail(bitmap, 300, 300);
        imageView.setPadding(8, 8, 8, 8);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setLayoutParams(new GridView.LayoutParams(GridView.LayoutParams.MATCH_PARENT, GridView.LayoutParams.MATCH_PARENT));
        imageView.setImageBitmap(thumbnail);

        return imageView;
    }



}
