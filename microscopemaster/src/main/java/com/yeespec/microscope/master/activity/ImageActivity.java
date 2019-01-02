package com.yeespec.microscope.master.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yeespec.R;
import com.yeespec.microscope.master.BaseActivity;
import com.yeespec.microscope.master.application.BaseApplication;
import com.yeespec.microscope.master.custom.ImageControl;
//import com.yeespec.microscope.master.service.power.PowerService;
import com.yeespec.microscope.utils.ConstantUtil;
import com.yeespec.microscope.utils.FileUtils;
import com.yeespec.microscope.utils.PictureUtils;
import com.yeespec.microscope.utils.UIUtil;

import java.io.File;
import java.util.List;

public class ImageActivity extends BaseActivity implements ImageView.OnLongClickListener,View.OnClickListener {

   // public  final ScheduledExecutorService DELAY_SCHEDULED = Executors.newScheduledThreadPool(1);


    private ImageControl imageView;
    private TextView titleView;
    private ImageView tvDel;
    private Bitmap bitmap;
    private Context context;
    private Activity activity;
    private BaseApplication application;

    private Button reNameDetermine;
    private EditText reNameEt;
    private Button reNameCancel;
    private TextView reName;
    private TextView oldReName;

    private String FileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TAG = ImageActivity.class.getSimpleName();

        setContentView(R.layout.activity_image);
        super.onCreate(savedInstanceState);

        //        application = (BaseApplication) getApplication();
        //        application.init();
        //        application.addActivity(this);

        //BaseApplication.setContext(this);
        context = this;
        activity = this;

        FileName = getIntent().getStringExtra("title");
        String path = getIntent().getStringExtra("path");
        titleView = (TextView) findViewById(R.id.title);
        String displayFileName = FileName;
        titleView.setText(FileUtils.replaceFileName(displayFileName));
        imageView = (ImageControl) findViewById(R.id.image_view);

        bitmap = BitmapFactory.decodeFile(path);
        imageView.setImageBitmap(bitmap);
        reName = ((TextView) findViewById(R.id.reName));
        reName.setOnClickListener(this);
        tvDel = (ImageView) findViewById(R.id.tv_del);
        setListener();
//        Intent start = new Intent(this, PowerService.class);
//        startService(start);

    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.reName){
            final Dialog renameDialog = new Dialog(activity,R.style.Dialog_Radio);
            renameDialog.setContentView(R.layout.dialog_filerename);
            renameDialog.setCancelable(false);
            renameDialog.setCanceledOnTouchOutside(false);
            oldReName = ((TextView) renameDialog.findViewById(R.id.oldReName));
            reNameEt = ((EditText) renameDialog.findViewById(R.id.reFileNameEt));
            reNameDetermine = ((Button) renameDialog.findViewById(R.id.reNameDetermine));
            reNameCancel = ((Button) renameDialog.findViewById(R.id.reNameCancel));
            Intent intent = getIntent();
            String oldPath = intent.getStringExtra("path");
            String [] oldPaths =oldPath.split("/");

            final String ps[] = oldPaths[oldPaths.length-1].split("-");

            oldReName.setText(FileName);
            reNameDetermine.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!ConstantUtil.isAutoPhotoFinish){
                        UIUtil.toast(activity, "请停止自动拍照后再重命名图片 ! ", true);
                        return;
                    }
                    String  newpathName = reNameEt.getText().toString().trim();
                    if(newpathName.contains("/")||newpathName.contains("#")||newpathName.contains("%")||newpathName.contains("?"))
                    {
                        UIUtil.toast(ImageActivity.this,"文件名中不能包含 # % ? / 等符号",false);
                        return;
                    }
                    String  newScaledName =newpathName+".scaled.bmp";
                    Intent intent = getIntent();
                    String path = intent.getStringExtra("path");
                    String scaledPath = intent.getStringExtra("scaled-path");

                    String [] fileNames =path.split("/");
                    String [] scaledFileNames = scaledPath.split("/");
                    // String fileName=fileNames[fileNames.length-1]+".bmp";
                    // String scaledFileName = scaledFileNames[scaledFileNames.length-1]+".scaled.bmp";
                    String fileDir=new String();
                    String scaledFileDir = new String();
                    for (int i = 0; i < scaledFileNames.length-1; i++) {
                        scaledFileDir = scaledFileDir+scaledFileNames[i]+"/";
                    }
                    for (int i = 0; i < fileNames.length-1; i++) {
                        fileDir = fileDir+fileNames[i]+"/";
                    }

                    List<File> pngFiles = PictureUtils.getPictures(getApplicationContext());
                    for (int i1 = 0; i1 < pngFiles.size(); i1++) {
                        String mfilename = pngFiles.get(i1).getName().trim();
                        if((newpathName.trim()+".bmp").equals(mfilename.substring(7))){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    UIUtil.toast(ImageActivity.this, "该文件已经存在,请重新输入!", false);
                                }
                            });
                            return;
                        }
                    }

                    if(newpathName==null||newpathName.equals("")){

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                UIUtil.toast(ImageActivity.this,"文件名不能为空",false);
                            }
                        });
                        return;
                    }else {
                        int i = scaledPath.indexOf(".");
                        final String oldname = scaledPath.substring(scaledPath.lastIndexOf("/"), i);
                        String substring = scaledPath.substring(0, scaledPath.lastIndexOf("/"));
                        String dir = substring.substring(0, substring.lastIndexOf("/"));

                      //  String oldScaledjpgpath = scaledPath.substring(0, scaledPath.lastIndexOf(".bmp"))+".jpg";
                        String oldScaledjpgpath = dir+"/request"+oldname+".scaled.jpg";

                        //Log.w("MasterActivity", "oldScaledjpgpath=" +oldScaledjpgpath);
                        FileUtils.fileRemane(path,scaledPath,fileDir+ps[0]+"-"+newpathName+".bmp",scaledFileDir+ps[0]+"-"+newScaledName,oldScaledjpgpath, dir+"/request/"+ps[0]+"-"+newpathName+".scaled.jpg");
                        renameDialog.dismiss();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                UIUtil.toast(ImageActivity.this,"重命名成功!",false);
                            }
                        });
                        if (!bitmap.isRecycled()) {
                            bitmap.recycle();
                        }
                        Intent intent1 = new Intent(ImageActivity.this, PhotoAlbumActivity.class);
                        startActivity(intent1);
                        finish();
                    }
                }
            });
            reNameCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    renameDialog.dismiss();
                }
            });
            renameDialog.show();
        }
    }
    @Override
    public boolean onLongClick(View v) {

        return false;
    }
    private void setListener() {
        tvDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //2016.08.10 : 修改对话弹窗的界面一致性 :
                // 初始化对话框
                final Dialog deleteAlertDialog;
                deleteAlertDialog = new Dialog(activity, R.style.Dialog_Radio);
                deleteAlertDialog.setContentView(R.layout.dialog_alert_delete);
                deleteAlertDialog.setCancelable(false);
                deleteAlertDialog.setCanceledOnTouchOutside(false);

                Button deleteButton = (Button) deleteAlertDialog.findViewById(R.id.btn_delete);
                //                Button deleteVideoButton = (Button) deleteAlertDialog.findViewById(R.id.btn_delete_video);
                Button cancleButton = (Button) deleteAlertDialog.findViewById(R.id.btn_cancle);

                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!ConstantUtil.isAutoPhotoFinish){
                            UIUtil.toast(activity, "请停止自动拍照后再删除图片 ! ", true);
                            return;
                        }
                        Intent intent = getIntent();
                        String path = intent.getStringExtra("path");
                        // 获取要当前的源文件
                        File originFile = new File(path);
                        originFile.delete();
                        String scaledPath = intent.getStringExtra("scaled-path");
                        if (scaledPath != null) {
                            new File(scaledPath).delete();
                            //20170614
                            String mpath = scaledPath.substring(0, scaledPath.lastIndexOf(".bmp"));

                            new File(mpath+".jpg").delete();
                        }
                        deleteAlertDialog.dismiss();
                        if (!bitmap.isRecycled()) {
                            bitmap.recycle();
                        }
                        Intent intent1 = new Intent(context, PhotoAlbumActivity.class);
                        startActivity(intent1);
                        finish();

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



        findViewById(R.id.btn_return).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                Intent intent = new Intent(ImageActivity.this, PhotoAlbumActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
    }



    @Override
    protected void onDestroy() {

//        Intent start = new Intent(this, PowerService.class);
//        stopService(start);
        imageView.setImageBitmap(null);
        if (!bitmap.isRecycled()) {
            bitmap.recycle();
        }
        System.gc();

        //        unregisterReceiver(batteryLevelReceiver);

        super.onDestroy();
    }

    @Override
    public void onResume() {
        if (DEBUG)
            Log.v(TAG, "onResume:");
//        Intent start = new Intent(this, PowerService.class);
//        startService(start);
        super.onResume();
    }

    @Override
    public void onPause() {
        if (DEBUG)
            Log.v(TAG, "onPause:");
//        Intent start = new Intent(this, PowerService.class);
//        stopService(start);
        super.onPause();
    }


}
