package com.yeespec.microscope.master.adapter;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Created by Administrator on 2018/3/7.
 */

public class MyAdapter extends PagerAdapter {
    private int mChildCount = 0;

    private ImageView[] mImageViews;
    @Override
    public int getCount() {

        return mImageViews.length;
    }

    public MyAdapter(ImageView[] imageViews) {
        mChildCount = 0;
        this.mImageViews = imageViews;
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == arg1;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {

        //2016.08.16 : 添加 索引判断 , 防止数组越界 :
        if (position < mImageViews.length) {
            container.removeView(mImageViews[position]);

        }
    }

    /**
     * 载入图片进去，用当前的position 除以 图片数组长度取余数是关键
     */
    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        container.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

           /* File file = allFile.get(position);
            Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
            ImageView  imageView  = new ImageView(PhotoAlbumActivity.this);
            imageView.setImageBitmap(bitmap);*/


        container.addView(mImageViews[position], 0);
        return mImageViews[position];
    }

    //2016.06.27 修改PagerAdapter调用notifyDataSetChanged()无更新的bug , 添加覆盖getItemPosition()方法 ,
    // 当调用notifyDataSetChanged时，让getItemPosition方法人为的返回POSITION_NONE，从而达到强迫viewpager重绘所有item的目的。
    @Override
    public void notifyDataSetChanged() {
        mChildCount = getCount();
        super.notifyDataSetChanged();
    }

    @Override
    public int getItemPosition(Object object) {
        if (mChildCount > 0) {
            mChildCount--;
            return POSITION_NONE;
        }
        return super.getItemPosition(object);
    }
}
