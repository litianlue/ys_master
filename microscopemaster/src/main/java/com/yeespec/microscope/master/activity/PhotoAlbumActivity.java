package com.yeespec.microscope.master.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.view.SimpleDraweeView;
import com.yeespec.R;
import com.yeespec.libuvccamera.usb.UVCCamera;
import com.yeespec.libuvccamera.uvccamera.encoder.MediaMuxerWrapper;
import com.yeespec.microscope.master.BaseActivity;
import com.yeespec.microscope.master.adapter.GalleryAdapter;
import com.yeespec.microscope.master.adapter.MyAdapter;
import com.yeespec.microscope.master.application.BaseApplication;
import com.yeespec.microscope.master.custom.MyRecyclerView;
//import com.yeespec.microscope.master.service.power.PowerService;
import com.yeespec.microscope.utils.AndroidUtil;
import com.yeespec.microscope.utils.ConstantUtil;
import com.yeespec.microscope.utils.FileUtils;
import com.yeespec.microscope.utils.MDateUtils;
import com.yeespec.microscope.utils.PictureUtils;
import com.yeespec.microscope.utils.UIUtil;
import com.yeespec.microscope.utils.fresco.tool.FrescoTool;
import com.yeespec.microscope.utils.log.Logger;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 相册页面
 */
public class PhotoAlbumActivity extends BaseActivity {

    //    private static final String TAG = PhotoAlbumActivity.class.getSimpleName();//得到类的简写名称
    private int pageNumber=1;
    private boolean isbottom=false;
    private  int onePageNumber =20;

    private int currentItem = 0;
    private MyRecyclerView mRecyclerView;
    private GalleryAdapter mAdapter;
    private MyAdapter myAdatpter;
    private ImageView tvDel;
    private ImageView tvCompound;
    private ImageView tvSelect;
    private TextView mFileName;

    private Context context;
    private CheckBox checkBox;

    private Activity activity;

    private BaseApplication application;
    /**
     * ViewPager
     */
    private ViewPager viewPager;
    /**
     * 装ImageView数组
     */
    private ImageView[] mImageViews;

    private View videwPlayIcon;

    //临时装载所有文件的集合

    //用于显示于ViewPage的文件集合 :
    public ArrayList<File> allFile = new ArrayList<>();
    //用于显示于缩放栏的文件集合 :
    public ArrayList<File> scaledAllFile = new ArrayList<>();
    private ProgressBar delay_PB;

    private void recyclerAll() {





        if(myAdatpter!=null){
            myAdatpter = null;
        }
        if(mRecyclerView!=null){
            mRecyclerView =null;
        }
        if (mImageViews != null) {
            mImageViews = null;
        }
        if (allFile != null) {

            allFile.clear();
            allFile = null;
        }
        if (scaledAllFile != null) {
            scaledAllFile.clear();
            scaledAllFile = null;
        }
        if (mAdapter != null) {

            mAdapter = null;
        }
        if (tvCompound != null) {
            tvCompound = null;
        }
        if (tvSelect != null) {
            tvSelect = null;
        }
    }

    //创建一个可重用固定线程数的线程池
    //    public static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);
    //public static final ExecutorService DELET_DELAY_SERVICE = Executors.newScheduledThreadPool(2);

    public static final ScheduledExecutorService SERVICE_DELAY = Executors.newScheduledThreadPool(1);
    // public static final ScheduledExecutorService SHOW_PB_DELAY = Executors.newScheduledThreadPool(3);

    private PictureUtils pictureUtils;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TAG = PhotoAlbumActivity.class.getSimpleName();//得到类的简写名称

        setContentView(R.layout.activity_photo_album);
        super.onCreate(savedInstanceState);
        context = this;
        activity = this;
        currentItem = 0;
        pageNumber =1;
        isbottom =false;
        pictureUtils  = new PictureUtils();
        try {
            initView();

            setListener();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Dialog delayrenameDialog;

    private void showRefileNameDialog() {
        if (delayrenameDialog != null) {
            delayrenameDialog.dismiss();
            delayrenameDialog = null;
        }
        delayrenameDialog = new Dialog(PhotoAlbumActivity.this, R.style.Dialog_Radio);
        delayrenameDialog.setContentView(R.layout.dialog_delayfilerename);
        delayrenameDialog.setCancelable(false);
        delayrenameDialog.setCanceledOnTouchOutside(false);
        delayrenameDialog.show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);


        showRefileNameDialog();

        SERVICE_DELAY.schedule(new Runnable() {
            @Override
            public void run() {
                if (delayrenameDialog != null) {
                    delayrenameDialog.dismiss();
                    delayrenameDialog = null;
                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (ConstantUtil.isAutoPhotoFinish)
                            updateAdapterList();
                        initPhotoView();
                        hideHoldLoading();
                        initListener();
                        if (mAdapter != null)
                            mAdapter.notifyDataSetChanged();
                        if(myAdatpter!=null)
                            myAdatpter.notifyDataSetChanged();
                        if (allFile.size() <= 0 || scaledAllFile.size() == 0) {
                            Intent intent = new Intent(PhotoAlbumActivity.this, MasterActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        }
                        if (allFile.size() > currentItem) {
                            //  Toast.makeText(activity, "currentItem="+currentItem, Toast.LENGTH_SHORT).show();
                            viewPager.setCurrentItem(currentItem);
                            mFileName.setText(FileUtils.replaceFileName(allFile.get(currentItem).getName().toString()));
                        } else if (allFile.size() == currentItem) {
                            viewPager.setCurrentItem(currentItem - 1);
                            mFileName.setText(FileUtils.replaceFileName(allFile.get(currentItem - 1).getName().toString()));
                        }

                    }
                });
            }
        }, 2000, TimeUnit.MILLISECONDS);


    }

    /**
     * 删除 弹窗,更新界面
     */
    private void setListener() {

        findViewById(R.id.btn_return).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



                Intent intent = new Intent(PhotoAlbumActivity.this, MasterActivity.class);
                startActivity(intent);
                if (delayrenameDialog != null) {
                    delayrenameDialog.dismiss();
                    delayrenameDialog = null;
                }
                ConstantUtil.FLAG = "0";
                // mAdapter.notifyDataSetChanged();

                mAdapter = null;
                myAdatpter = null;

                viewPager.clearOnPageChangeListeners();
                mRecyclerView.clearOnChildAttachStateChangeListeners();
                viewPager = null;
                mRecyclerView = null;

                clearImgMemory(mImageViews);
                // mImageViews = null;
               /* allFile.clear();
                allFile = null;
                scaledAllFile.clear();
                scaledAllFile = null;*/

                // recyclerAll();

                System.gc();
                finish();
            }
        });

        tvDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //2016.08.10 : 修改对话弹窗的界面一致性 :
                // 初始化对话框
                final Dialog deleteAlertDialog;
                deleteAlertDialog = new Dialog(activity, R.style.Dialog_Radio);
                deleteAlertDialog.setContentView(R.layout.dialog_alert_delete_all);
                deleteAlertDialog.setCancelable(false);
                deleteAlertDialog.setCanceledOnTouchOutside(false);

                Button deletePhotoButton = (Button) deleteAlertDialog.findViewById(R.id.btn_delete_photo);
                Button deleteVideoButton = (Button) deleteAlertDialog.findViewById(R.id.btn_delete_video);
                Button deleteSelectButton = (Button) deleteAlertDialog.findViewById(R.id.btn_delete_select);
                Button cancleButton = (Button) deleteAlertDialog.findViewById(R.id.btn_cancle);

                final LinearLayout layoutContent = (LinearLayout) deleteAlertDialog.findViewById(R.id.layout_content);
                final LinearLayout layoutButton = (LinearLayout) deleteAlertDialog.findViewById(R.id.layout_button);

                final TextView titleTV = (TextView) deleteAlertDialog.findViewById(R.id.delete_label_title);
                final TextView messageTV = (TextView) deleteAlertDialog.findViewById(R.id.delete_lable_message);

                final TextView indexView = (TextView) deleteAlertDialog.findViewById(R.id.label_index);
                final TextView countView = (TextView) deleteAlertDialog.findViewById(R.id.label_count);

                if (ConstantUtil.FLAG.equals("1")) {
                    titleTV.setText("确认删除");
                    messageTV.setText("确认删除选中图片或视频?");
                    deletePhotoButton.setVisibility(View.GONE);
                    deleteVideoButton.setVisibility(View.GONE);
                    deleteSelectButton.setVisibility(View.VISIBLE);
                }
                deleteSelectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (ConstantUtil.isAutoPhotoFinish == false) {
                            UIUtil.toast(activity, "正在自动拍照,请先停止自动拍照再进行删除!!", false);
                            return;
                        }
                        if (ConstantUtil.FLAG.equals("1")) {

                            ConstantUtil.FLAG = "0";

                            EXECUTOR_SERVICE.execute(new Runnable() {
                                @Override
                                public void run() {
                                    if (mAdapter == null) {
                                        return;
                                    }
                                    final List<Integer> lstSelected = mAdapter.getIsSelected();
                                    if (lstSelected.isEmpty()) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (progressDialog != null) {
                                                    progressDialog.dismiss();
                                                }
                                            }
                                        });
                                        return;
                                    }
                                    final List<File> photoListFiles = PictureUtils.getPictures(getApplicationContext());
                                    final List<File> scaledPhotoFiles = PictureUtils.getPicturesScaled(getApplicationContext());
                                    final List<File> scaledJpgPhotoFiles = PictureUtils.getJpgPicturesScaled(getApplicationContext());
                                    Collections.reverse(photoListFiles);
                                    Collections.reverse(scaledPhotoFiles);
                                    Collections.reverse(scaledJpgPhotoFiles);
                                    Collections.sort(photoListFiles, new FileComparator());
                                    Collections.sort(scaledPhotoFiles, new FileComparator());
                                    Collections.sort(scaledJpgPhotoFiles, new FileComparator());


                                    final int photoSize = photoListFiles.size();
                                    final List<File> moviesFiles = PictureUtils.getMovies(getApplicationContext());
                                    final int moviesSize = moviesFiles.size();
                                    if (photoSize == 0 & moviesSize == 0) {
                                        UIUtil.toast(activity, "设备中已无图片或视频文件可删除!", false);
                                    } else {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                layoutButton.setVisibility(View.GONE);
                                                layoutContent.setVisibility(View.VISIBLE);
                                                titleTV.setText("正在删除");
                                                messageTV.setText("请稍后……");
                                                countView.setText(String.valueOf(lstSelected.size()));
                                            }
                                        });

                                        //2016.08.16 : 新增 : 更新显示列表 :

                                        for (int index = 0; index < lstSelected.size(); index++) {

                                            int i = lstSelected.get(index);
                                            if (allFile.get(i).getName().contains(".mp4")) {
                                                allFile.get(i).delete();
                                            } else {
                                                // 获取要复制的源文件
                                                File originFile = allFile.get(i);
                                                //                                                String photoName[] = originFile.getName().split(".");
                                                String photoName = originFile.getName();
                                                for (int photoIndex = 0; photoIndex < photoListFiles.size(); photoIndex++) {
                                                    if (photoListFiles.get(photoIndex) != null) {
                                                        if (photoName.compareTo((photoListFiles.get(photoIndex).getName())) == 0) {
                                                            String photopath = photoListFiles.get(photoIndex).getPath();
                                                            String substring = photopath.substring(photopath.lastIndexOf("/"));
                                                            String photoname = substring.substring(0, substring.length() - 4);

                                                            //Log.w("PhotoAlbumActivity", "photoname=" + photoname);
                                                            if (scaledPhotoFiles.get(photoIndex).getPath() != null) {
                                                                String path = scaledPhotoFiles.get(photoIndex).getPath();
                                                                //String scalesubstring = path.substring(path.lastIndexOf("/"));
                                                                // String scalename = scalesubstring.substring(0, substring.length() - 4);
                                                                //Log.w("PhotoAlbumActivity", "scalename=" + scalename);

                                                                if (path.contains(photoname)) {
                                                                    scaledPhotoFiles.get(photoIndex).delete();
                                                                } else {
                                                                    for (int i1 = 0; i1 < scaledPhotoFiles.size(); i1++) {
                                                                        if (scaledPhotoFiles.get(i1).getPath().contains(photoname)) {
                                                                            scaledPhotoFiles.get(i1).delete();
                                                                            break;
                                                                        }
                                                                    }
                                                                }


                                                            }
                                                            if (photoListFiles.get(photoIndex).getPath() != null) {
                                                                photoListFiles.get(photoIndex).delete();
                                                            }
                                                            if (scaledJpgPhotoFiles.get(photoIndex).getPath() != null) {
                                                                String path = scaledJpgPhotoFiles.get(photoIndex).getPath();
                                                                // String jpgsubstring = path.substring(path.lastIndexOf("/"));
                                                                // String jpgname = jpgsubstring.substring(0, substring.length() - 4);
                                                                // Log.w("PhotoAlbumActivity", "jpgname=" + jpgname);
                                                                if (path.contains(photoname)) {
                                                                    scaledJpgPhotoFiles.get(photoIndex).delete();
                                                                } else {

                                                                    for (int i1 = 0; i1 < scaledJpgPhotoFiles.size(); i1++) {
                                                                        if (scaledJpgPhotoFiles.get(i1).getPath().contains(photoname)) {
                                                                            scaledJpgPhotoFiles.get(i1).delete();
                                                                            break;
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            continue;   //结束单次循环
                                                        }
                                                    }
                                                }


                                                originFile.delete();

                                            }

                                            final int i1 = index + 1;
                                            activity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    indexView.setText(String.valueOf(i1));
                                                }
                                            });
                                        }
                                        SERVICE_DELAY.schedule(new Runnable() {
                                            @Override
                                            public void run() {
                                                activity.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        //20170717

                                                        final List<File> moviesFiles = PictureUtils.getMovies(getApplicationContext());
                                                        final List<File> photoListFiles = PictureUtils.getPictures(getApplicationContext());
                                                        if (moviesFiles.size() > 0 | photoListFiles.size() > 0) {
                                                            Intent intent1 = new Intent(context, PhotoAlbumActivity.class);
                                                            //intent1.putExtra("refreshvalue",true);
                                                            startActivity(intent1);

                                                        } else {
                                                            Intent intent = new Intent(context, MasterActivity.class);
                                                            startActivity(intent);
                                                            //Toast.makeText(PhotoAlbumActivity.this, "暂无任何资源可以访问", Toast.LENGTH_SHORT).show();

                                                            // UIUtil.toast(activity, "暂无任何资源可以访问", false);
                                                            finish();
                                                        }
                                                        deleteAlertDialog.dismiss();
                                                    }
                                                });
                                            }
                                        }, 2000, TimeUnit.MILLISECONDS);


                                    }

                                }
                            });
                            //                    mAdapter.notifyDataSetChanged();
                        }

                    }
                });

                deletePhotoButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (ConstantUtil.isAutoPhotoFinish == false) {
                            UIUtil.toast(activity, "正在自动拍照,请先停止自动拍照再进行删除!!", false);
                            return;
                        }
                        final List<File> photoListFiles = PictureUtils.getPictures(getApplicationContext());
                        final List<File> scaledPhotoFiles = PictureUtils.getPicturesScaled(getApplicationContext());
                        final List<File> scaledJpgPhotoFiles = PictureUtils.getJpgPicturesScaled(getApplicationContext());
                        // final int photoSize = photoListFiles.size();

                        //final List<File> mAllphotoListFiles = PictureUtils.getALLPictures(getApplicationContext());
                        if (scaledPhotoFiles.size() == 0) {

                            UIUtil.toast(activity, "设备中已无图片文件可删除", false);

                        } else {

                            layoutButton.setVisibility(View.GONE);
                            layoutContent.setVisibility(View.VISIBLE);
                            titleTV.setText("正在删除");
                            messageTV.setText("请稍后……");
                            countView.setText(String.valueOf(scaledPhotoFiles.size()));

                            //2016.08.16 : 新增 : 更新显示列表 :
                            EXECUTOR_SERVICE.execute(new Runnable() {
                                @Override
                                public void run() {

                                    for (int j = 0; j < scaledPhotoFiles.size(); j++) {
                                        File file = scaledPhotoFiles.get(j);
                                        file.delete();
                                        photoListFiles.get(j).delete();

                                        scaledJpgPhotoFiles.get(j).delete();
                                        final int j1 = j + 1;
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                indexView.setText(String.valueOf(j1));

                                            }
                                        });
                                    }
                                    //避免删除不全异常，再次检查删除
                                    final List<File> scaledPic = PictureUtils.getPicturesScaled(getApplicationContext());
                                    final List<File> scaledJpgPic = PictureUtils.getJpgPicturesScaled(getApplicationContext());
                                    final List<File> bigPic = PictureUtils.getPictures(getApplicationContext());
                                    for (int i = 0; i < scaledPic.size(); i++) {
                                        scaledPic.get(i).delete();
                                    }
                                    for (int i = 0; i < scaledJpgPic.size(); i++) {
                                        scaledJpgPic.get(i).delete();
                                    }
                                    for (int i = 0; i < bigPic.size(); i++) {
                                        bigPic.get(i).delete();
                                    }

                                    SERVICE_DELAY.schedule(new Runnable() {
                                        @Override
                                        public void run() {
                                            activity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {


                                                    final List<File> moviesFiles = PictureUtils.getMovies(getApplicationContext());
                                                    if (moviesFiles.size() > 0) {
                                                        Intent intent1 = new Intent(context, PhotoAlbumActivity.class);

                                                        startActivity(intent1);
                                                        //                                        finish();
                                                    } else {
                                                        Intent intent = new Intent(context, MasterActivity.class);
                                                        startActivity(intent);
                                                        //  UIUtil.toast(activity, "暂无任何资源可以访问", false);

                                                        finish();
                                                    }
                                                    deleteAlertDialog.dismiss();

                                                }
                                            });
                                        }
                                    }, 2000, TimeUnit.MILLISECONDS);


                                }
                            });
                        }
                    }
                });

                deleteVideoButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (ConstantUtil.isAutoPhotoFinish == false) {
                            UIUtil.toast(activity, "正在自动拍照,请先停止自动拍照再进行删除!!", false);
                            return;
                        }
                        final List<File> moviesFiles = PictureUtils.getMovies(getApplicationContext());
                        final int moviesSize = moviesFiles.size();
                        if (moviesSize == 0) {
                            UIUtil.toast(activity, "设备中已无视频文件可删除", false);

                        } else {

                            layoutButton.setVisibility(View.GONE);
                            layoutContent.setVisibility(View.VISIBLE);
                            titleTV.setText("正在删除");
                            messageTV.setText("请稍后……");
                            countView.setText(String.valueOf(moviesSize));

                            //2016.08.16 : 新增 : 更新显示列表 :
                            EXECUTOR_SERVICE.execute(new Runnable() {
                                @Override
                                public void run() {

                                    for (int j = 0; j < moviesSize; j++) {
                                        File orginFileMovies = moviesFiles.get(j);

                                        final int j1 = j + 1;
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                indexView.setText(String.valueOf(j1));
                                            }
                                        });

                                        orginFileMovies.delete();
                                    }

                                    SERVICE_DELAY.schedule(new Runnable() {
                                        @Override
                                        public void run() {
                                            activity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {

                                          /*  updateAdapterList();

                                            initPhotoView();    //viewpager的重用
                                            hideHoldLoading();  //隐藏加载提示对话框 :
                                            initListener();      //设置监听，主要是设置点点的背景
                                            initData();         //初始化ViewPager当前显示第几个文件
*/
                                                    final List<File> photoListFiles = PictureUtils.getPictures(getApplicationContext());
                                                    if (photoListFiles.size() > 0) {
                                                        //更新视频，到第一个
                                                        Intent intent1 = new Intent(context, PhotoAlbumActivity.class);
                                                        //intent1.putExtra("refreshvalue",true);
                                                        startActivity(intent1);
                                                        //                                        finish();
                                                    } else {
                                                        Intent intent = new Intent(context, MasterActivity.class);
                                                        startActivity(intent);
                                                        // UIUtil.toast(activity, "暂无任何资源可以访问", false);
                                                        finish();
                                                    }
                                                    deleteAlertDialog.dismiss();

                                                }
                                            });
                                        }
                                    }, 2000, TimeUnit.MILLISECONDS);


                                }
                            });

                        }
                    }
                });

                cancleButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteAlertDialog.dismiss();
                    }
                });

                deleteAlertDialog.show();

            }
        });

        //选择跳转
        tvSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ConstantUtil.FLAG.equals("0")) {
                    tvCompound.setImageResource(R.drawable.compound);
                    ConstantUtil.FLAG = "1";
                    mAdapter.notifyDataSetChanged();
                } else if (ConstantUtil.FLAG.equals("1")) {
                    ConstantUtil.FLAG = "0";
                    mAdapter.notifyDataSetChanged();
                    /*FLAG = "0";

                    EXECUTOR_SERVICE.execute(new Runnable() {
                        @Override
                        public void run() {
                            updateAdapterList();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    initPhotoView();    //viewpager的重用
                                    hideHoldLoading();  //隐藏加载提示对话框 :
                                    initListener();      //设置监听，主要是设置点点的背景
                                    initData();         //初始化ViewPager当前显示第几个文件

                                }
                            });
                        }
                    });*/
                    //                    mAdapter.notifyDataSetChanged();
                }
            }
        });

        //合成跳转
        tvCompound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ConstantUtil.isAutoPhotoFinish == false) {
                    UIUtil.toast(activity, "正在自动拍照,请先停止自动拍照再进行合成!", false);
                    return;
                }
                if (ConstantUtil.FLAG.equals("0")) {
                    //                    FLAG = "1";
                    //                    mAdapter.notifyDataSetChanged();
                    UIUtil.toast(getApplicationContext(), "请先选择照片 ! ", false);
                } else if (ConstantUtil.FLAG.equals("1")) {
                    ConstantUtil.FLAG = "0";

                    //2016.08.10 : 修改对话弹窗的界面一致性 :
                    // 初始化对话框
                    progressDialog = new Dialog(activity, R.style.Dialog_Radio);
                    progressDialog.setContentView(R.layout.dialog_progress_compound);
                    progressDialog.setCancelable(false);
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.show();

                    //                    progressDialog = ProgressDialog.show(context, "合成图片", "请等待完成...");

                    EXECUTOR_SERVICE.execute(new Runnable() {
                        @Override
                        public void run() {
                            compound();


                        }
                    });
                    //                    mAdapter.notifyDataSetChanged();
                }
            }
        });


    }

    Dialog progressDialog;
    private Bitmap shearBitmapXY(Bitmap bmp,int shearX,int shearY){
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int startx=0,endx=0,starty=0,endy=0;
        if(shearX>0){
            startx = shearX;
        }else{
            endx = shearX;
        }
        if(shearY>0){
            starty = shearY;
        }else {
            endy = shearY;
        }
        Bitmap bitmap = Bitmap.createBitmap(bmp,0+startx,0+starty,width-startx,height-starty);
        return bitmap;
    }
    private void compound() {

        List<Integer> lstSelected = mAdapter.getIsSelected();
        if (lstSelected.size() < 2) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    ConstantUtil.FLAG = "0";
                    mAdapter.notifyDataSetChanged();
                    UIUtil.toast(activity, "最少两张以上才能合成", false);
                }
            });
            return;
        }
        if (lstSelected.isEmpty()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                }
            });
            return;
        }

        Bitmap[] bitmaps = new Bitmap[lstSelected.size()];
        int isTranlates[] = new int[lstSelected.size()];
        WeakReference<Bitmap[]> rbitmaps = new WeakReference<Bitmap[]>(bitmaps);
        int index = 0;
        for (index = 0; index < lstSelected.size(); index++) {
            int i = lstSelected.get(index);
            if (lstSelected.size() > 4) {      //从只允许2张改为最多四张;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog != null) {
                            progressDialog.dismiss();
                        }
                        ConstantUtil.FLAG = "0";
                        mAdapter.notifyDataSetChanged();
                        UIUtil.toast(activity, "图片最多选择4张", false);
                    }
                });
                return;
            }
           /* //判断是否是荧光图片
            if(allFile.get(i).getName().contains("GreenLight")||allFile.get(i).getName().contains("BlueLight")
                    ||allFile.get(i).getName().contains("PurpleLight")) {

                isTranlates[index] = 1;
            }*/
            //判断是否是荧光图片
            if(allFile.get(i).getName().toString().substring(0,1).equals("1")) {

                isTranlates[index] = 1;
            }/*else if(allFile.get(i).getName().toString().substring(0,1).equals("2")){
                isTranlates[index] =2;
            }*/
            if (allFile.get(i).getName().contains(".mp4")) {


               /* FLAG = "0";
                EXECUTOR_SERVICE.execute(new Runnable() {
                    @Override
                    public void run() {
                        updateAdapterList();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                initPhotoView();    //viewpager的重用
                                hideHoldLoading();  //隐藏加载提示对话框 :
                                initListener();      //设置监听，主要是设置点点的背景
                                initData();         //初始化ViewPager当前显示第几个文件

                            }
                        });
                    }
                });*/
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog != null) {
                            progressDialog.dismiss();
                        }
                        ConstantUtil.FLAG = "0";
                        mAdapter.notifyDataSetChanged();
                        UIUtil.toast(activity, "视频不能合成", false);
                    }
                });
                //                Intent intent = new Intent(context, PhotoAlbumActivity.class);
                //                startActivity(intent);
                //                finish();
                return;
            }
            //复制图片数据到bitmaps
            rbitmaps.get()[index] = BitmapFactory.decodeFile(allFile.get(i).getPath());

        }
        int width = rbitmaps.get()[0].getWidth();
        int height = rbitmaps.get()[0].getHeight();
        int A = 0, R = 0, G = 0, B = 0;
        int pixelColor;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);//重新建一个bitmap，存新生成的数据
        WeakReference<Bitmap> rbitmap = new WeakReference<Bitmap>(bitmap);
       /* int startx=0,endx=0,starty=0,endy=0;
        if(ConstantUtil.tranlateX>0){
            startx = ConstantUtil.tranlateX;
        }else{
            endx = ConstantUtil.tranlateX;
        }
        if(ConstantUtil.tranlateY>0){
            starty = ConstantUtil.tranlateY;
        }else {
            endy = ConstantUtil.tranlateY;
        }*/
      /*  int startx=0,picturew=0,starty=0,pictureh;
        if(ConstantUtil.tranlateX>0){
            startx = ConstantUtil.tranlateX;
            picturew = ConstantUtil.tranlateX;
        }else{
            startx = 0;
            picturew = -ConstantUtil.tranlateX;
        }
        if(ConstantUtil.tranlateY>0){
            starty = ConstantUtil.tranlateY;
            pictureh = ConstantUtil.tranlateY;
        }else {
            starty = 0;
            pictureh = -ConstantUtil.tranlateY;
        }*/
        for (int y = 0; y < height; y++) {//循环每个像素点

            for (int x = 0; x < width; x++) {

                for (int j = 0; j < lstSelected.size(); j++) {//循环每个图片

                    if(isTranlates[j]==1){//带荧光图片
                        int y1 ;
                        int x1;
                        //y坐标校准
                        if((y-ConstantUtil.tranlateY)<0) {
                            y1 = 0;
                        }else if((y-ConstantUtil.tranlateY)>(height-1)) {
                            y1 = height-1;
                        } else {
                            y1 = y -ConstantUtil.tranlateY;
                        }
                        //x坐标校准
                        if((x-ConstantUtil.tranlateX)<0) {
                            x1 = 0;
                        }else if((x-ConstantUtil.tranlateX)>(width-1)) {
                            x1 = width-1;
                        } else {
                            x1 = x -ConstantUtil.tranlateX;
                        }

                        pixelColor = rbitmaps.get()[j].getPixel(x1, y1);

                    }/*else if(isTranlates[j]==2){

                        if(x>(startx)&&x<(width-picturew)&&y>(starty)&&y<(height-pictureh))
                            pixelColor = rbitmaps.get()[j].getPixel(x-startx, y-starty);
                        else
                            pixelColor = Color.rgb(0,0,0);
                    }*/else {

                        pixelColor = rbitmaps.get()[j].getPixel(x, y);
                    }



                    // pixelColor = rbitmaps.get()[j].getPixel(x, y);
                    A += Color.alpha(pixelColor);//把每个像素点加起来
                    R += Color.red(pixelColor);
                    G += Color.green(pixelColor);
                    B += Color.blue(pixelColor);
                }
                A = A < 255 ? A : 255;
                R = R < 255 ? R : 255;
                G = G < 255 ? G : 255;
                B = B < 255 ? B : 255;
                int c = Color.argb(A, R, G, B);
                rbitmap.get().setPixel(x, y, c);
                A = 0;
                R = 0;
                G = 0;
                B = 0;
            }
        }

        for (int indexNum = 0; indexNum < rbitmaps.get().length; indexNum++) {//循环每个图片
            if (!rbitmaps.get()[indexNum].isRecycled()) {
                rbitmaps.get()[indexNum].recycle();
            }
        }
        String timerstr = MDateUtils.getDateTimeString();

        timerstr =MDateUtils.numberString(ConstantUtil.filenumber,2)+"-"+timerstr;
        FileUtils.capture(pictureUtils.shearBitmapXY(rbitmap.get(),ConstantUtil.tranlateX,ConstantUtil.tranlateY), MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, timerstr+".bmp", "big").toString(), false, false);
        FileUtils.capture(pictureUtils.shearBitmapXY(rbitmap.get(),ConstantUtil.tranlateX,ConstantUtil.tranlateY), MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, timerstr+".scaled.bmp", "scale").toString(), true, false);
        FileUtils.capture(pictureUtils.shearBitmapXY(rbitmap.get(),ConstantUtil.tranlateX,ConstantUtil.tranlateY), MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, timerstr+".scaled.jpg", "request").toString(), false, true);
        //  FileUtils.capture(rbitmap.get(), MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, timerstr+".bmp", "big").toString(), false, false);
        //  FileUtils.capture(rbitmap.get(), MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, timerstr+".scaled.bmp", "scale").toString(), true, false);
        //  FileUtils.capture(rbitmap.get(), MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, timerstr+".scaled.jpg", "request").toString(), false, true);


        if (rbitmap.get() != null) {
            rbitmap.get().recycle();
        }
        SERVICE_DELAY.schedule(new Runnable() {
            @Override
            public void run() {

                updateAdapterList();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog != null) {
                            progressDialog.dismiss();
                        }
                        currentItem = 0;
                        initPhotoView();    //viewpager的重用
                        hideHoldLoading();  //隐藏加载提示对话框 :
                        initListener();      //设置监听，主要是设置点点的背景
                        initData();         //初始化ViewPager当前显示第几个文件
                        if (mAdapter != null)
                            mAdapter.notifyDataSetChanged();

                    }
                });
            }
        }, 2000, TimeUnit.MILLISECONDS);

    }


    private void initView() {
        delay_PB = ((ProgressBar) findViewById(R.id.delay_pb));

        viewPager = (ViewPager) findViewById(R.id.viewPager);

        videwPlayIcon = findViewById(R.id.ic_video_play);//viewpager中重用的Fragment
        tvDel = (ImageView) findViewById(R.id.tv_del);
        tvCompound = (ImageView) findViewById(R.id.tv_compound);
        tvSelect = (ImageView) findViewById(R.id.tv_select);
        checkBox = (CheckBox) findViewById(R.id.cb1);
        mFileName = ((TextView) findViewById(R.id.tv_photo));
        EXECUTOR_SERVICE.execute(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showHoldLoading();//弹窗
                    }
                });

                updateAdapterList();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initPhotoView();    //viewpager的重用
                        hideHoldLoading();  //隐藏加载提示对话框 :
                        initListener();      //设置监听，主要是设置点点的背景
                        initData();         //初始化ViewPager当前显示第几个文件
                    }
                });
            }
        });
    }

    private void updateAdapterList() {
        if(allFile==null||scaledAllFile==null)
            return;
        List<File> pngFiles = PictureUtils.getPictures(getApplicationContext());
        List<File> scaledBmps = PictureUtils.getPicturesScaled(getApplicationContext());
        List<File> mp4Files = PictureUtils.getMovies(getApplicationContext());

        List<File> allFiles = new ArrayList<>();
        List<File> allScaleFiles = new ArrayList<>();

        allFiles.clear();

        allFile.clear();

        Collections.reverse(pngFiles);      //Collections.reverse()反转集合中元素的顺序
        allFiles.addAll(pngFiles);

        Collections.reverse(mp4Files);
        allFiles.addAll(mp4Files);

        Collections.sort(allFiles, new FileComparator());    //Collections.sort()按照自定义的比较规则排序 , 按照文件的创建时间先后排序 :
        if(allFiles.size()>0){
            String path[] = allFiles.get(0).getPath().split("/");
            String p[] = path[path.length-1].split("-");
            ConstantUtil.filenumber = Integer.valueOf(p[0].substring(2,6));
        }else {
            ConstantUtil.filenumber = 0;
        }

        allScaleFiles.clear();

        scaledAllFile.clear();

        Collections.reverse(scaledBmps);
        allScaleFiles.addAll(scaledBmps);
        Collections.reverse(mp4Files);
        allScaleFiles.addAll(mp4Files);
        Collections.sort(allScaleFiles, new FileComparator());  //Collections.sort()按照自定义的比较规则排序 , 按照文件的创建时间先后排序 :

        if(allFiles.size()<pageNumber*onePageNumber){
            isbottom = true;
            for (int i = 0; i < allFiles.size(); i++) {
                allFile.add(allFiles.get(i));
                scaledAllFile.add(allScaleFiles.get(i));
            }
        }else {
            for (int i = 0; i < pageNumber * onePageNumber; i++) {
                allFile.add(allFiles.get(i));
                scaledAllFile.add(allScaleFiles.get(i));
            }
        }

    }

    /**
     * 读取文件创建时间
     * 更换为读取文件的创建ID
     */
    public static class FileComparator implements Comparator<File> {
        /* public int compare(File file1, File file2) {
             if (file1.lastModified() > file2.lastModified()) {

                 return -1;
             } else {
                 return 1;
             }
         }*/
        public int compare(File file1, File file2) {

            //

            String x[] =  file1.getPath().split("/");
            String y[] = file2.getPath().split("/");
           /* String path = x[x.length-1];
            String path1 = y[y.length-1];
            Log.i("PhotoAlbumAcitivity","path="+path+"  path1="+path1);*/
            String p1[] = x[x.length-1].split("-");
            String p2[] = y[y.length-1].split("-");
            int f1 = Integer.valueOf(p1[0].substring(2,6));
            int y1= Integer.valueOf(p2[0].substring(2,6));

            if (f1 > y1) {

                return -1;
            } else {
                return 1;
            }
        }
    }
    /* private BitmapFactory.Options getBitmapOption(int inSampleSize){
         System.gc();
         BitmapFactory.Options options = new BitmapFactory.Options();
         options.inPurgeable = true;
         options.inSampleSize = inSampleSize;
         return options;
     }
   */  //viewpager的重用
    private void initPhotoView() {
        mImageViews = null;
        mImageViews = new ImageView[allFile.size()];
        FrescoTool.getInstance(getApplicationContext()).getFrescoConfig().setIMAGE_PIPELINE_CACHE_DIR("yeespec-ad");

        FrescoTool.getInstance(getApplicationContext()).init();

        for (int i = 0; i < allFile.size(); i++) {
            SimpleDraweeView imageView = new SimpleDraweeView(PhotoAlbumActivity.this);
            mImageViews[i] = null;
            mImageViews[i] = imageView;
            int height = getScreenHeight(PhotoAlbumActivity.this) - AndroidUtil.dpToPx(99, PhotoAlbumActivity.this) - AndroidUtil.dpToPx(48, PhotoAlbumActivity.this);
            int width = height * UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT;
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height);
            imageView.setLayoutParams(lp);
            imageView.getHierarchy().setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER);

            FrescoTool.displayImage("file://" + allFile.get(i), imageView, width, height, null);

            //Bitmap bitmap = BitmapFactory.decodeFile(allFile.get(i).getPath(),getBitmapOption(1));
            // imageView.setImageBitmap(bitmap);

            final int j = i;
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentItem = j;

                    if (allFile.get(j).getName().contains(".mp4")) {
                        Intent intent = new Intent(PhotoAlbumActivity.this, PlayVideoActvitity.class);
                        intent.putExtra("title", allFile.get(j).getName());
                        intent.putExtra("path", allFile.get(j).getPath());
                        startActivity(intent);

                    } else {
                        Intent intent = new Intent(PhotoAlbumActivity.this, ImageActivity.class);
                        intent.putExtra("title", allFile.get(j).getName());
                        intent.putExtra("path", allFile.get(j).getPath());
                        intent.putExtra("scaled-path", scaledAllFile.get(j).getPath());
                        startActivity(intent);

                    }
                }
            });
        }

        myAdatpter = new MyAdapter(mImageViews);
        //设置Adapter

        viewPager.setAdapter(null);
        viewPager.setAdapter(myAdatpter);

        //重复的view
        mRecyclerView = (MyRecyclerView) findViewById(R.id.id_recyclerview_horizontal);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(PhotoAlbumActivity.this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);

        mRecyclerView.setLayoutManager(linearLayoutManager);
        mAdapter = new GalleryAdapter(getApplicationContext(), scaledAllFile);
        mRecyclerView.setAdapter(null);
        mRecyclerView.setAdapter(mAdapter);         //设置缩略图栏的适配器 :

        mAdapter.notifyDataSetChanged();
        myAdatpter.notifyDataSetChanged();
        if (allFile.size() > 0) {
            if (allFile.get(0).getName().contains(".mp4")) {
                videwPlayIcon.setVisibility(View.VISIBLE);
            } else {
                videwPlayIcon.setVisibility(View.GONE);
            }
        }

    }

    private void clearImgMemory(ImageView[] views) {
        if (views != null) {
            for (View V : views) {

                if (V instanceof ImageView) {
                    Drawable d = ((ImageView) V).getDrawable();
                    if (d != null && d instanceof BitmapDrawable) {
                        Bitmap bmp = ((BitmapDrawable) d).getBitmap();
                        bmp.recycle();
                        bmp = null;
                    }
                    ((ImageView) V).setImageBitmap(null);
                    if (d != null) {
                        d.setCallback(null);
                    }
                }
            }
        }
    }

    /**
     * 冒泡法排序<br/>
     * <p/>
     * <li>比较相邻的元素。如果第一个比第二个大，就交换他们两个。</li>
     * <li>对每一对相邻元素作同样的工作，从开始第一对到结尾的最后一对。在这一点，最后的元素应该会是最大的数。</li>
     * <li>针对所有的元素重复以上的步骤，除了最后一个。</li>
     * <li>持续每次对越来越少的元素重复上面的步骤，直到没有任何一对数字需要比较。</li>
     *
     * @param
     */
    /*public static File[] bubbleSort(File[] numbers) {
        File temp; // 记录临时中间值
        int size = numbers.length; // 数组大小
        for (int i = 0; i < size - 1; i++) {
            for (int j = i + 1; j < size; j++) {
                if (numbers[i].lastModified() < numbers[j].lastModified()) { // 交换两数的位置
                    temp = numbers[i];
                    numbers[i] = numbers[j];
                    numbers[j] = temp;
                }
            }
        }
        return numbers;
    }


    //获取屏幕的宽度
    public static int getScreenWidth(Context context) {
        WindowManager manager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        return display.getWidth();
    }*/

    //获取屏幕的高度
    public  int getScreenHeight(Context context) {
        WindowManager manager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        return display.getHeight();
    }


    public boolean isViewPagerControlRecycleView = true;
    public boolean isRecycleViewControlViewPager = true;

    private void initListener() {
        //设置监听，主要是设置点击后的背景
        viewPager.setOnPageChangeListener(onPageChangeListener);    //设置监听由大图viewPage滑动带动底部的recycleView更新显示位置;

        mRecyclerView.setOnItemScrollChangeListener(new MyRecyclerView.OnItemScrollChangeListener() {
            @Override
            public void onChange(View view, int position) {
                if (DEBUG)
                    Logger.e("setOnItemScrollChangeListener-onChange-position = " + position);

                if (isRecycleViewControlViewPager) {
                    isViewPagerControlRecycleView = false;

                    // 2016.09.26 修改
                    mShowChangeHandler.removeMessages(CHANGE_VIEW_PAGE);

                    mRecyclerView.setCurrentPosition(position);

                    //创建信息 :
                    Message msg = new Message();
                    msg.what = CHANGE_VIEW_PAGE;
                    Bundle bundle = new Bundle();

                    bundle.putInt(VIEW_POSITION, position);

                    msg.setData(bundle);
                    //向新线程中的Handler发送消息 ;
                    mShowChangeHandler.sendMessageDelayed(msg, 50);    //延时50ms后发送执行;

                    isViewPagerControlRecycleView = true;
                }
            }
        });
        mRecyclerView.setOnBottomCallback(new MyRecyclerView.OnBottomCallback() {
            @Override
            public void onBottom() {
                if(isbottom){
                    Toast.makeText(activity, "已经是最后一页了！", Toast.LENGTH_SHORT).show();
                    return;
                }
                pageNumber++;
                //Toast.makeText(activity, "正在加载下一页", Toast.LENGTH_SHORT).show();
                updateAdapterList();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initPhotoView();    //viewpager的重用
                        hideHoldLoading();  //隐藏加载提示对话框 :
                        initListener();      //设置监听，主要是设置点点的背景
                        if (allFile.size() != 0) {
                            mFileName.setText(FileUtils.replaceFileName(allFile.get(currentItem).getName().toString()));
                        }
                        currentItem = currentItem;
                        viewPager.setCurrentItem(currentItem);
                    }
                });



            }
        });
        mAdapter.setOnItemClickListener(new GalleryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                if (isRecycleViewControlViewPager) {
                    isViewPagerControlRecycleView = false;
                    mRecyclerView.setCurrentPosition(position);
                    mAdapter.setCurrentPosition(position);
                    viewPager.setCurrentItem(position);
                    mAdapter.notifyDataSetChanged();
                    currentItem = position;

                    mFileName.setText(FileUtils.replaceFileName(allFile.get(position).getName().toString()));
                    if (allFile.get(position).getName().contains(".mp4")) {
                        videwPlayIcon.setVisibility(View.VISIBLE);
                    } else {
                        videwPlayIcon.setVisibility(View.GONE);
                    }
                    isViewPagerControlRecycleView = true;
                }
            }
        });
    }

    private void initData() {

        if (allFile.size() != 0) {

            mFileName.setText(FileUtils.replaceFileName(allFile.get(0).getName().toString()));
        }
        currentItem = 0;
        viewPager.setCurrentItem(0);
    }

   /* public class MyAdapter extends PagerAdapter {

        private int mChildCount = 0;


        @Override
        public int getCount() {

            return mImageViews.length;
        }

        private MyAdapter() {
            mChildCount = 0;
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

        *//**
     * 载入图片进去，用当前的position 除以 图片数组长度取余数是关键
     *//*
        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            container.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

           *//* File file = allFile.get(position);
            Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
            ImageView  imageView  = new ImageView(PhotoAlbumActivity.this);
            imageView.setImageBitmap(bitmap);*//*


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
    }*/

    private ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    if (DEBUG)
                        Logger.e("onPageScrolled-position = " + position + ",positionOffset = " + positionOffset);
                }

                @Override
                public void onPageSelected(int position) {
                    if (DEBUG)
                        Logger.e("onPageSelected-position = " + position);
                    if (isViewPagerControlRecycleView) {
                        // 底部HorizontalScrollView需要选择
                        isRecycleViewControlViewPager = false;
                        mRecyclerView.setCurrentPosition(position);
                        mRecyclerView.scrollToPosition(position);
                        mAdapter.setCurrentPosition(position);
                        mAdapter.notifyDataSetChanged();
                        currentItem = position;

                        mFileName.setText(FileUtils.replaceFileName(allFile.get(position).getName().toString()));
                        if (allFile.get(position).getName().contains(".mp4")) {
                            videwPlayIcon.setVisibility(View.VISIBLE);
                        } else {
                            videwPlayIcon.setVisibility(View.GONE);
                        }
                        isRecycleViewControlViewPager = true;
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            };

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onRestart() {
        super.onRestart();

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    public void onResume() {


        super.onResume();
    }

    @Override
    public void onPause() {
        if (DEBUG)
            Log.v(TAG, "onPause:");

        // Intent start = new Intent(this, PowerService.class);
        // stopService(start);
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        recyclerAll();
//        Intent start = new Intent(this, PowerService.class);
//        stopService(start);
        clearImgMemory(mImageViews);
        System.gc();
        pictureUtils = null;
        super.onDestroy();
        mShowChangeHandler.removeCallbacksAndMessages(null);
    }

    //2016.09.26 : 新增 : 用于在拖动底部RecycleView的缩略图时延迟更新ViewPage的大图
    public final  int CHANGE_VIEW_PAGE = 0x789;

    //2016.09.26 新增 : 用于在handle中改变ViewPage的当前显示位置 :
    public final  String VIEW_POSITION = "viewPosition";

    PhotoHandler mShowChangeHandler = new PhotoHandler(PhotoAlbumActivity.this);

    private static final class PhotoHandler extends Handler {
        WeakReference<PhotoAlbumActivity> mWeakReference;

        public PhotoHandler(PhotoAlbumActivity photoAlbumActivity) {
            mWeakReference = new WeakReference<PhotoAlbumActivity>(photoAlbumActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            switch (msg.what) {

                case 0x789:
                    if (bundle != null) {
                        int position = bundle.getInt("viewPosition", 0);     //如果不存在 , 返回0位置 ;
                        //  SPUtils.put(getApplicationContext(), "current_contrast", MasterActivity.OBJECTIVE_VALUE_A);
                        if (mWeakReference.get().mAdapter == null)
                            return;
                        mWeakReference.get().mAdapter.setCurrentPosition(position);
                        mWeakReference.get().mAdapter.notifyDataSetChanged();
                        mWeakReference.get().viewPager.setCurrentItem(position);
                        if (mWeakReference.get().allFile.get(position).getName().contains(".mp4")) {
                            mWeakReference.get().videwPlayIcon.setVisibility(View.VISIBLE);
                        } else {
                            mWeakReference.get().videwPlayIcon.setVisibility(View.GONE);
                        }
                        //                        Log.w("test_photoAActi", "handleMessage()# CHANGE_VIEW_PAGE === position = " + position + " === ");
                    }

                    break;
            }
        }
    }


}
