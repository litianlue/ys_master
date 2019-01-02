package com.yeespec.microscope.master.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.yeespec.R;
import com.yeespec.libuvccamera.usb.UVCCamera;
import com.yeespec.microscope.master.activity.PhotoAlbumActivity;
import com.yeespec.microscope.utils.AndroidUtil;
import com.yeespec.microscope.utils.ConstantUtil;
import com.yeespec.microscope.utils.fresco.tool.FrescoTool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    private int currentPosition = 0;

    //    public static String FLAG="0";
    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    private LayoutInflater mInflater;
    private List<File> mDatas;
    private Context context;


    public List<Integer> getIsSelected() {
        return isSelected;
    }

    private List<Integer> isSelected;

    //2016.06.27 HashSet 无序不可重复 ; 将isSelected改为HashSet :

    public GalleryAdapter(Context context, List<File> datats) {
        mInflater = LayoutInflater.from(context);

        mDatas = datats;

        isSelected = new ArrayList<>();

        this.context = context;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View arg0) {
            super(arg0);
        }

        SimpleDraweeView mImg;
        ImageView textView;     //定义viewHolder的类型 ; 是视频文件 or 图片文件
        View itemBgView;
        CheckBox checkBox;
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int i) {
        View view = mInflater.inflate(R.layout.item_index_gallery,
                viewGroup, false);
        ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.textView = (ImageView) view.findViewById(R.id.type);
        viewHolder.mImg = (SimpleDraweeView) view.findViewById(R.id.id_index_gallery_item_image);
        viewHolder.itemBgView = view.findViewById(R.id.content_bg);         //加载itemView
        viewHolder.checkBox = (CheckBox) view.findViewById(R.id.cb1);        //加载checkBox

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        if (mDatas.get(position).getName().contains(".mp4")) {
            viewHolder.textView.setImageDrawable(context.getResources().getDrawable(R.mipmap.luxiang_s));
        } else {
            viewHolder.textView.setImageDrawable(null);
        }

        int height = getScreenHeight(context) - AndroidUtil.dpToPx(99, context) - AndroidUtil.dpToPx(48, context);
        int width = height * UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT;



        FrescoTool.displayImage("file://" + mDatas.get(position), viewHolder.mImg, width, height, null);

        if (position == currentPosition) {
            viewHolder.itemBgView.setBackground(context.getResources().getDrawable(R.drawable.photo_content_bg_red));
        } else {
            viewHolder.itemBgView.setBackground(context.getResources().getDrawable(R.drawable.photo_content_bg_white));
        }

        if (mOnItemClickListener != null) {
            viewHolder.itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {


                    if (viewHolder.checkBox.isChecked()) {   //被勾选
                        //                    viewHolder.checkBox.setChecked(false);

                        int size = isSelected.size();
                        for (int j = 0; j < size; j++) {
                            if (isSelected.get(j).compareTo(position) == 0) {
                                //                            isSelected.remove(position);      //position需要转换成Integer , 不然会崩溃报错 ;
                                isSelected.remove(Integer.valueOf(position));
                                break;
                            }
                        }
                    } else {           //取消勾选

                        //                    viewHolder.checkBox.setChecked(true);
                        //                    Log.i("test", "checkBox.setOnClickListener === " + true);
                        isSelected.add(position);

                    }

                    mOnItemClickListener.onItemClick(viewHolder.itemView, position);
                }
            });

        }
        if (ConstantUtil.FLAG.equals("1")) {
            viewHolder.checkBox.setVisibility(View.VISIBLE);
            viewHolder.checkBox.setChecked(false);
            if (isSelected != null) {

                int size = isSelected.size();
                for (int j = 0; j < size; j++) {
                    if (isSelected.get(j) == position) {
                        viewHolder.checkBox.setChecked(true);   //从记录中读取多选框的数据 , 如果有被勾选上 , 就显示勾选 ;
                        //                            break;
                        //                        Log.i("test", "checkBox.setChecked === " + position);
                    }
                }
                //                Log.i("test", "isSelected.toString() === " + isSelected.toString());

            }

        } else {
            viewHolder.checkBox.setVisibility(View.GONE);
            if (isSelected != null) {
                isSelected.clear();     //当退出合成选择界面时 , 清空选择记录 ;

            }
        }

        viewHolder.checkBox.setOnClickListener(new OnClickListener() {      //此处检测checkBox原先使用setOnCheckedChangeListener , 后出现问题 , 改为用setOnClickListener ;
            @Override
            public void onClick(View v) {


                if (viewHolder.checkBox.isChecked()) {   //被勾选
                    //                    viewHolder.checkBox.setChecked(true);
                    //                    Log.i("test", "checkBox.setOnClickListener === " + true);
                    isSelected.add(position);
                } else {           //取消勾选
                    //                    viewHolder.checkBox.setChecked(false);
                    //                    Log.i("test", "checkBox.setOnClickListener === " + false);
                    for (int j = 0; j < isSelected.size(); j++) {
                        if (isSelected.get(j).compareTo(position) == 0) {
                            //                            isSelected.remove(position);      //position需要转换成Integer , 不然会崩溃报错 ;
                            isSelected.remove(Integer.valueOf(position));
                            //                            break;
                        }
                    }

                }

            }
        });




    }

    //获取屏幕的高度
    public static int getScreenHeight(Context context) {
        WindowManager manager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        return display.getHeight();
    }


}
