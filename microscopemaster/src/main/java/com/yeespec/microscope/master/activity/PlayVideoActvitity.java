package com.yeespec.microscope.master.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.yeespec.R;
import com.yeespec.microscope.master.BaseActivity;
import com.yeespec.microscope.utils.ConstantUtil;
import com.yeespec.microscope.utils.FileUtils;
import com.yeespec.microscope.utils.PictureUtils;
import com.yeespec.microscope.utils.UIUtil;

import java.io.File;
import java.util.List;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.Vitamio;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

import static com.yeespec.R.id.videoReName;

/**
 * Created by yuchunrong on 2017-10-10.
 */

public class PlayVideoActvitity extends BaseActivity implements View.OnClickListener {
    private String mPath;
    private String mTitle;
    private TextView vedioReName;
    private ImageView tvDel;
    private TextView titleName;

    //修改文件名dialog
    private TextView oldReName;
    private EditText reNameEt;
    private Button reNameDetermine,reNameCancel;
    private TextView mreturn;

    private   VideoView mVideoView;

    private String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Vitamio.isInitialized(getApplicationContext());

        Intent intent = getIntent();
        mPath = intent.getStringExtra("path");
        fileName = intent.getStringExtra("title");
        if (TextUtils.isEmpty(mPath)) {
            UIUtil.toast(this, "地址无效，请返回重新选择",false);
        }
        setContentView(R.layout.playvideo_activity);

        vedioReName = ((TextView) findViewById(videoReName));
        tvDel = (ImageView) findViewById(R.id.tv_del);
        titleName = ((TextView) findViewById(R.id.title_text));
        String displayFileName = fileName;
        titleName.setText(FileUtils.replaceFileName(displayFileName));
        mreturn = ((TextView) findViewById(R.id.btn_return));

        mVideoView = (VideoView) findViewById(R.id.surface_view);

        vedioReName.setOnClickListener(this);
        tvDel.setOnClickListener(this);
        mreturn.setOnClickListener(this);


       playfunction(mPath);
    }
    //文件重命名
    private void fileRemane(String oldPath,String newPath){
        File file = new File(oldPath);
        File newFile = new File(newPath);
        if(file!=null){
            file.renameTo(newFile);
        }

    }
    private void checkReFileMethed(){
        final Dialog renameDialog = new Dialog(PlayVideoActvitity.this,R.style.Dialog_Radio);
        renameDialog.setContentView(R.layout.dialog_video_filerename);
        renameDialog.setCancelable(false);
        renameDialog.setCanceledOnTouchOutside(false);
        oldReName = ((TextView) renameDialog.findViewById(R.id.oldvideoReName));
        reNameEt = ((EditText) renameDialog.findViewById(R.id.videoreFileNameEt));
        reNameDetermine = ((Button) renameDialog.findViewById(R.id.videoreNameDetermine));
        reNameCancel = ((Button) renameDialog.findViewById(R.id.videoreNameCancel));
        Intent intent = getIntent();
        String oldPath = intent.getStringExtra("path");
        String [] oldPaths =oldPath.split("/");

        final String ps[] = oldPaths[oldPaths.length-1].split("-");

        oldReName.setText(fileName);
        reNameDetermine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!ConstantUtil.isAutoPhotoFinish){
                    UIUtil.toast(PlayVideoActvitity.this, "请停止自动拍照后再重名文件 ! ", true);
                    return;
                }
                String  newpathName = reNameEt.getText().toString().trim();
                if(newpathName.contains("/")||newpathName.contains("#")||newpathName.contains("%")||newpathName.contains("?"))
                {
                    UIUtil.toast(PlayVideoActvitity.this,"文件名中不能包含 # % ? /等符号",false);
                    return;
                }
                Intent intent = getIntent();
                String path = intent.getStringExtra("path");


                String [] fileNames =path.split("/");

                // String fileName=fileNames[fileNames.length-1]+".bmp";
                // String scaledFileName = scaledFileNames[scaledFileNames.length-1]+".scaled.bmp";
                String fileDir=new String();

                for (int i = 0; i < fileNames.length-1; i++) {
                    fileDir = fileDir+fileNames[i]+"/";
                }
                List<File> mp4Files = PictureUtils.getMovies(getApplicationContext());
                for (int i1 = 0; i1 < mp4Files.size(); i1++) {
                    String mfilename = mp4Files.get(i1).getName().trim();
                    if((newpathName.trim()+".mp4").equals(mfilename.substring(7))){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                UIUtil.toast(PlayVideoActvitity.this, "该文件已经存在,请重新输入!", false);
                            }
                        });
                        return;
                    }
                }
                if(newpathName==null||newpathName.equals("")){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            UIUtil.toast(PlayVideoActvitity.this,"文件名不能为空!",false);

                        }
                    });
                    return;
                }else {

                    fileRemane(path,fileDir+ps[0]+"-"+newpathName+".mp4");

                    renameDialog.dismiss();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            UIUtil.toast(PlayVideoActvitity.this,"重命名成功!",false);
                        }
                    });

                    Intent intent1 = new Intent(PlayVideoActvitity.this, PhotoAlbumActivity.class);
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
    private void deleteFile(){
        tvDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //2016.08.10 : 修改对话弹窗的界面一致性 :
                // 初始化对话框
                final Dialog deleteAlertDialog;
                deleteAlertDialog = new Dialog(PlayVideoActvitity.this, R.style.Dialog_Radio);
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
                            UIUtil.toast(PlayVideoActvitity.this, "请停止自动拍照后再删除视频 ! ", true);
                            return;
                        }
                        //获取当前视频的图片
                        Intent intent = getIntent();
                        String path = intent.getStringExtra("path");
                        File orginFileMovies = new File(path);
                        orginFileMovies.delete();
                        deleteAlertDialog.dismiss();

                        Intent intent1 = new Intent(PlayVideoActvitity.this, PhotoAlbumActivity.class);
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
    }
    void playfunction(String path){
        //path="http://gslb.miaopai.com/stream/3D~8BM-7CZqjZscVBEYr5g__.mp4";

        if (path == "") {
            Toast.makeText(PlayVideoActvitity.this, "Please edit VideoViewDemo Activity, and set path" + " variable to your media file URL/path", Toast.LENGTH_LONG).show();
            return;
        } else {
			/*
			 * Alternatively,for streaming media you can use
			 * mVideoView.setVideoURI(Uri.parse(URLstring));
			 */
            mVideoView.setVideoPath(path);
            mVideoView.setMediaController(new MediaController(this));
            mVideoView.requestFocus();

            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    // optional need Vitamio 4.0
                    mediaPlayer.setPlaybackSpeed(1.0f);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoView = null;
    }

    @Override
    public void onClick(View v) {
           switch (v.getId()){
               case R.id.videoReName:
                   checkReFileMethed();
                   break;
               case R.id.tv_del:
                   deleteFile();
                   break;
               case R.id.btn_return:
                   finish();
                   break;
           }
    }
}
