package com.yeespec.microscope.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract;
import android.util.Log;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.AVSaveOption;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.GetCallback;


import com.yeespec.microscope.master.activity.PhotoAlbumActivity;
import com.yeespec.microscope.utils.bluetooth.DataUtil;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Created by Administrator on 2017/12/23.
 */

public class RequesUtils {

    public static final String CONNECT_STATE="ConnectState";
    public static final String CLIENT_INSTURCTION="ClientInstruction";
    public static final String PRIVIEW_PHOTO="PriviewPhoto";
    public static final String EQUIPMENT_CONNECT_STATE="EquipmentConnectState";
    public static final String PARAMETER="Parameter";
    public static final String CONFIGURATION="Configuration";

    public static final String PARAM_TYPE_CONNECT_NETWORK= "param_type_connect_network";
    public static final int PARAM_TYPE_CONNECT_NETWORK_TYPE=100;

    public static  String PIC_NAME = "null";//记录查看图片文件名
    public static  String SCAL_PIC_NAME = "null";//记录查看缩略图片文件名
    public static final String CAPTURE = "capture";
    public static final int CAPTURE_TYPE=0;
    public static final String CAPTURE_STILL = "capture_still";
    public static final int CAPTURE_STILL_TYPE=1;
    public static final String PARAM_TYPE_SATURATION = "param_type_saturation";
    public static final int PARAM_TYPE_SATURATION_TYPE=2;
    public static final String PARAM_TYPE_CONTRAST = "param_type_contrast";
    public static final int PARAM_TYPE_CONTRAST_TYPE=3;
    public static final String COLORS = "colors";
    public static final int COLORS_TYPE=4;
    private static final String RECOLORS_STRING = "recolors_string";
    public static final int RECOLORS_STRING_TYPE=5;
    public static final String PARAM_TYPE_ISO = "param_type_iso";
    public static final int PARAM_TYPE_ISO_TYPE = 6;
    public static final String PARAM_TYPE_BRIGHTNESS = "param_type_brightness";
    public static final int PARAM_TYPE_BRIGHTNESS_TYPE=7;
    public static final String SHUT_DOWN = "shut_down";
    public static final int SHUT_DOWN_TYPE=8;
    public static final String PARAM_TYPE_MANUAL_FOCUS = "param_type_manual_focus";
    public static final int PARAM_TYPE_MANUAL_FOCUS_TYPE=9;
    public static final String PARAM_TYPE_GAMMA = "param_type_gamma";
    public static final int PARAM_TYPE_GAMMA_TYPE=10;
    public static final String PARAM_TYPE_GAMMA_DOMN = "param_type_gamma_domn";
    public static final int PARAM_TYPE_GAMMA_DOMN_TYPE=11;
    public static final String PARAM_TYPE_MANUAL_FOCUS_STOP = "param_type_manual_focus_stop";
    public static final int PARAM_TYPE_MANUAL_FOCUS_STOP_TYPE=12;
    public static final String PARAM_TYPE_VESSELS = "param_type_vessels";
    public static final int PARAM_TYPE_VESSELS_TYPE=13;
    public static final String PARAM_TYPE_UpDATE_PICTER = "param_type_update_picter";//更新右下角缩略图
    public static final int PARAM_TYPE_UpDATE_PICTER_TYPE=14;
    public static final String PARAM_TYPE_AUTOPHOTO = "param_type_autophoto";//自动拍照
    public static final int PARAM_TYPE_AUTOPHOTO_TYPE=15;
    public static final String PARAM_TYPE_STOPAUTOPHOTO = "param_type_stopautophoto";//停止自动拍照
    public static final int PARAM_TYPE_STOPAUTOPHOTO_TYPE=16;
    public static final String PARAM_TYPE_AUTOPHOTOPREPARE = "param_type_autophotoprepare";//准备自动拍照
    public static final int PARAM_TYPE_AUTOPHOTOPREPARE_TYPE=17;
    public static final String PARAM_TYPE_CONSTAST = "param_type_constast";//对照组定位设定
    public static final int PARAM_TYPE_CONSTAST_TYPE=18;
    public static final String PARAM_TYPE_ROCKERVIEW = "param_type_rockerview";//摇杆控制
    public static final int PARAM_TYPE_ROCKERVIEW_TYPE=19;

    public static final String PARAM_TYPE_SCAL_PIC_LOAD= "param_type_scal_picture";//缩略图片
    public static final String PARAM_TYPE_SCAL_PIC_NAME_LOAD= "param_type_scal_picture_name";//缩略图片名称
    public static final int PARAM_TYPE_SCAL_PIC_LOAD_TYPE=22;
    public static final String PARAM_TYPE_PIC_LOAD= "param_type_picture";//图片
    public static final String PARAM_TYPE_PIC_NAME_LOAD= "param_type_picture_name";//图片名称
    public static final int PARAM_TYPE_PIC_LOAD_TYPE=20;
    public static final String PARAM_TYPE_LOAD_PIC_NAME= "param_type_load_pic_name";//加载图片名
    public static final int PARAM_TYPE_LOAD_PIC_NAME_TYPE=21;
    public static final String PARAM_TYPE_LOAD_SCAL_PIC_NAME= "param_type_load_scal_pic_name";//加载缩略图图片名
    public static final int PARAM_TYPE_LOAD_SCAL_PIC_NAME_TYPE=23;
    public static final String PARAM_TYPE_OFF= "param_type_off";//关机
    public static final int PARAM_TYPE_OFF_TYPE=24;
    public static final String PARAM_TYPE_EXPORT = "param_type_export";//导出
    public static final int PARAM_TYPE_EXPORT_TYPE=25;
    static final int constasts []= new int[DataUtil.CONTRACKCOUNT];
    public static  void padLogin(){
        AVQuery<AVObject> query = new AVQuery<>(CONNECT_STATE);
        query.whereEqualTo("user", ConstantUtil.remoteUserName);
        query.getFirstInBackground(new GetCallback<AVObject>() {
            @Override
            public void done(final AVObject account, AVException e) {
                if(e==null) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("flage",true);
                        jsonObject.put("date",account.getUpdatedAt().getTime());
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                    account.put("padstate", jsonObject);
                    AVSaveOption option = new AVSaveOption();
                    option.query(new AVQuery<>(CONNECT_STATE).whereGreaterThanOrEqualTo("user", ConstantUtil.remoteUserName));
                    option.setFetchWhenSave(true);
                    account.saveInBackground();
                }
            }
        });
    }
    public static void padUnRegist(){
        //通知服务器平板端退出
        AVQuery<AVObject> query = new AVQuery<>(CONNECT_STATE);
        query.whereEqualTo("user", ConstantUtil.remoteUserName);
        query.getFirstInBackground(new GetCallback<AVObject>() {
            @Override
            public void done(final AVObject account, AVException e) {

                if(e==null&&account!=null) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("flage",false);
                        jsonObject.put("date",account.getUpdatedAt().getTime());
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                    account.put("padstate", jsonObject);
                    AVSaveOption option = new AVSaveOption();
                    option.query(new AVQuery<>(CONNECT_STATE).whereEqualTo("user", ConstantUtil.remoteUserName));
                    option.setFetchWhenSave(true);
                    account.saveInBackground();
                }
            }
        });
    }
    //查询是否有该用户记录，没有则创建一条
    public static void IsUserInstruction(){
        if(ConstantUtil.remoteUserName.equals("")){
            return;
        }
        AVQuery<AVObject> query = new AVQuery<>(CLIENT_INSTURCTION);
        query.whereEqualTo("user",ConstantUtil.remoteUserName);

        query.findInBackground(new FindCallback<AVObject>() {
            @Override
            public void done(List<AVObject> list, AVException e) {
                if(e==null) {

                    if (list.size() < 1) {
                        addUserInstruction();
                    }
                }else if(e.equals("com.avos.avoscloud.AVException: java.net.SocketTimeoutException: timeout")) {
                    addUserInstruction();
                }
            }
        });
        AVQuery<AVObject> query_pv = new AVQuery<>(PRIVIEW_PHOTO);
        query_pv.whereEqualTo("user",ConstantUtil.remoteUserName);
        query_pv.findInBackground(new FindCallback<AVObject>() {
            @Override
            public void done(List<AVObject> list, AVException e) {
                if(e==null) {
                    if (list.size() < 1) {
                        addUserPriviewPhoto();
                    }
                }else if(e.equals("com.avos.avoscloud.AVException: java.net.SocketTimeoutException: timeout")) {
                    addUserPriviewPhoto();
                }
            }
        });

        AVQuery<AVObject> query_cs = new AVQuery<>(CONNECT_STATE);
        query_cs.whereEqualTo("user",ConstantUtil.remoteUserName);
        query_cs.findInBackground(new FindCallback<AVObject>() {
            @Override
            public void done(List<AVObject> list, AVException e) {
                if(e==null) {
                    if (list.size() < 1) {
                        addUserConnectState();
                    }/*else{
                        AVQuery<AVObject> query_cs = new AVQuery<>(CONNECT_STATE);
                        query_cs.whereEqualTo("user", ConstantUtil.remoteUserName);
                        query_cs.getFirstInBackground(new GetCallback<AVObject>() {
                            @Override
                            public void done(final AVObject account, AVException e) {
                                if(e==null) {
                                    JSONObject jsonObject = new JSONObject();
                                    try {
                                        jsonObject.put("flage",true);
                                        jsonObject.put("date",System.currentTimeMillis());
                                    } catch (JSONException e1) {
                                        e1.printStackTrace();
                                    }
                                    account.put("padstate", jsonObject);
                                    AVSaveOption option = new AVSaveOption();
                                    option.query(new AVQuery<>(CONNECT_STATE).whereEqualTo("user", ConstantUtil.remoteUserName));
                                    option.setFetchWhenSave(true);
                                    account.saveInBackground();
                                }
                            }
                        });
                    }*/
                }else if(e.equals("com.avos.avoscloud.AVException: java.net.SocketTimeoutException: timeout")) {
                    addUserConnectState();
                }
            }
        });
        AVQuery<AVObject> query_ecs = new AVQuery<>(EQUIPMENT_CONNECT_STATE);
        query_ecs.whereEqualTo("user",ConstantUtil.remoteUserName);
        query_ecs.findInBackground(new FindCallback<AVObject>() {
            @Override
            public void done(List<AVObject> list, AVException e) {
                if(e==null) {
                    if (list.size() < 1) {
                        addEquipmentConnectState();
                    }
                }else if(e.equals("com.avos.avoscloud.AVException: java.net.SocketTimeoutException: timeout")) {
                    addEquipmentConnectState();
                }
            }
        });

    }
    private static void addEquipmentConnectState(){
        AVObject avObject = new AVObject(EQUIPMENT_CONNECT_STATE);
        avObject.put("user",ConstantUtil.remoteUserName);
        JSONObject equipment = new JSONObject();
        try {
            equipment.put("user",ConstantUtil.remoteUserName);
            equipment.put("password",ConstantUtil.remotePassword);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject equiment_one = new JSONObject();
        JSONObject equiment_two = new JSONObject();
        JSONObject equiment_tree = new JSONObject();
        JSONObject equiment_four = new JSONObject();
        JSONObject equiment_five = new JSONObject();
        avObject.put("equipment",equipment);
        avObject.put("equipment_one",equiment_one);
        avObject.put("equipment_two",equiment_two);
        avObject.put("equipment_tree",equiment_tree);
        avObject.put("equipment_four",equiment_four);
        avObject.put("equipment_five",equiment_five);
        avObject.saveInBackground();
    }
    private static void addUserConnectState(){
        AVObject avObject = new AVObject(CONNECT_STATE);
        avObject.put("user",ConstantUtil.remoteUserName);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("flage",false);
            jsonObject.put("data",System.currentTimeMillis());
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
        avObject.put("padstate", jsonObject);
        avObject.put("phonestate",jsonObject);
        avObject.put("heartbeat",false);
        avObject.saveInBackground();
    }
    private static void addUserPriviewPhoto(){
        AVObject avObject = new AVObject(PRIVIEW_PHOTO);
        avObject.put("user",ConstantUtil.remoteUserName);
        JSONObject pictureName = new JSONObject();
        try {
            pictureName.put("picture",false);
            pictureName.put("filename","null");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject scalPicture = new JSONObject();
        try {
            pictureName.put("picture",new byte[1]);
            pictureName.put("filename","null");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject Picture = new JSONObject();
        try {
            pictureName.put("picture",new byte[1]);
            pictureName.put("filename","null");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        avObject.put(PARAM_TYPE_UpDATE_PICTER,pictureName);//更新缩略图文件名
        avObject.put(PARAM_TYPE_PIC_NAME_LOAD,"null");
        avObject.put(PARAM_TYPE_PIC_LOAD,new byte[1]);
        avObject.put(PARAM_TYPE_SCAL_PIC_NAME_LOAD,"null");
        avObject.put(PARAM_TYPE_SCAL_PIC_LOAD,new byte[1]);
        avObject.put(PARAM_TYPE_LOAD_PIC_NAME,"null");
        avObject.saveInBackground();
    }
    private static void addUserInstruction(){

        AVObject avObject = new AVObject(CLIENT_INSTURCTION);

        avObject.put("user",ConstantUtil.remoteUserName);
        JSONObject constast = new JSONObject();
        try {
            constast.put("postion",-1);
            constast.put("result",false);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject autophoto = new JSONObject();
        try {
            autophoto.put("autophoto",false);
            autophoto.put("autophotolights","null");
            autophoto.put("autofocus","null");
            autophoto.put("synthetic","null");
            autophoto.put("autophoto_starttimer","null");
            autophoto.put("autophoto_ttimer","null");
            autophoto.put("autophoto_endtimer","null");
            autophoto.put("autophoto_stopstr","null");
            autophoto.put("autophoto_count","null");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject autofocus = new JSONObject();
        try {
            autofocus.put("autofocus",false);
            autofocus.put("x",-1);
            autofocus.put("y",-1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject pictureName = new JSONObject();
        try {
            pictureName.put("picture",false);
            pictureName.put("filename","null");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject rocketview = new JSONObject();
        try {
            rocketview.put("rocketview",false);
            rocketview.put("command","null");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject export = new JSONObject();
        try {
            export.put("param",0);

        } catch (JSONException e) {
            e.printStackTrace();
        }
      //  avObject.put(PARAM_TYPE_CONNECT_NETWORK,"");
        avObject.put(PARAM_TYPE_SATURATION,-1);
        avObject.put(PARAM_TYPE_CONTRAST,-1);
        avObject.put(PARAM_TYPE_VESSELS,-1);
        avObject.put(PARAM_TYPE_CONSTAST,constast);
        avObject.put(PARAM_TYPE_ISO,-1);
        avObject.put(PARAM_TYPE_BRIGHTNESS,-1);
        avObject.put(PARAM_TYPE_GAMMA,-1);
        avObject.put(PARAM_TYPE_GAMMA_DOMN,-1);
        avObject.put(CAPTURE,false);
        avObject.put(PARAM_TYPE_AUTOPHOTO,autophoto);
        avObject.put(PARAM_TYPE_EXPORT,export);
        avObject.put(PARAM_TYPE_STOPAUTOPHOTO,false);

        avObject.put(PARAM_TYPE_MANUAL_FOCUS,autofocus);//自动对焦
        avObject.put(PARAM_TYPE_MANUAL_FOCUS_STOP,false);//停止自动对焦

        avObject.put(CAPTURE_STILL,false);//录制视频
        avObject.put(PARAM_TYPE_UpDATE_PICTER,pictureName);//更新缩略图文件名
        avObject.put(PARAM_TYPE_ROCKERVIEW,rocketview);//摇杆
        avObject.put(PARAM_TYPE_PIC_LOAD,new byte[1]);
        avObject.put(PARAM_TYPE_LOAD_PIC_NAME,"null");
        avObject.put(PARAM_TYPE_LOAD_SCAL_PIC_NAME,"null");
        avObject.put(PARAM_TYPE_OFF,false);
        avObject.saveInBackground();
    }
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
    public static void updatePrivewPhoto(final int type, final Context context){
        final AVQuery<AVObject> query = new AVQuery<>(PRIVIEW_PHOTO);
        query.whereEqualTo("user", ConstantUtil.remoteUserName);
        query.getFirstInBackground(new GetCallback<AVObject>() {
            @Override
            public void done(final AVObject account, AVException e) {
                if (e == null) {
                    switch (type){

                        case PARAM_TYPE_UpDATE_PICTER_TYPE://
                            JSONObject pictureName = new JSONObject();
                            try {
                                final List<File> pngFiles = PictureUtils.getPicturesScaled(context);
                                Collections.sort(pngFiles, new FileComparator());
                                StringBuffer buffer = new StringBuffer();
                                for (int i = 0; i < pngFiles.size(); i++) {
                                    buffer.append(pngFiles.get(i).getPath()+"&");
                                }
                                Log.w("abcde","buffer="+buffer);
                                pictureName.put("picture",false);
                                pictureName.put("filename",buffer);

                            } catch (JSONException e4) {
                                e4.printStackTrace();
                            }
                            account.put(PARAM_TYPE_UpDATE_PICTER, pictureName);
                            break;

                        case PARAM_TYPE_PIC_LOAD_TYPE://图片

                            Bitmap bitmap = BitmapFactory.decodeFile(RequesUtils.PIC_NAME);
                            if(bitmap!=null) {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                account.put(PARAM_TYPE_PIC_LOAD, baos.toByteArray());
                                account.put(PARAM_TYPE_PIC_NAME_LOAD, RequesUtils.PIC_NAME);
                            }
                            break;
                        case PARAM_TYPE_LOAD_SCAL_PIC_NAME_TYPE://缩略图片
                          //  PARAM_TYPE_LOAD_SCAL_PIC_NAME_TYPE
                            Bitmap scal_bitmap = BitmapFactory.decodeFile(RequesUtils.SCAL_PIC_NAME);
                            if(scal_bitmap!=null) {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                scal_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                account.put(PARAM_TYPE_SCAL_PIC_LOAD, baos.toByteArray());
                                account.put(PARAM_TYPE_SCAL_PIC_NAME_LOAD, RequesUtils.SCAL_PIC_NAME);
                            }
                            break;
                        case PARAM_TYPE_LOAD_PIC_NAME_TYPE://图片文件名
                            account.put(PARAM_TYPE_LOAD_PIC_NAME,"null");
                            break;
                    }

                    AVSaveOption option = new AVSaveOption();
                    option.query(new AVQuery<>(PRIVIEW_PHOTO).whereEqualTo("user", ConstantUtil.remoteUserName));
                    option.setFetchWhenSave(true);
                    account.saveInBackground();
                }
            }
        });
    }
    public static void updateInstrcution(final int type, final Context context){
        final AVQuery<AVObject> query = new AVQuery<>(CLIENT_INSTURCTION);
        query.whereEqualTo("user", ConstantUtil.remoteUserName);
        query.getFirstInBackground(new GetCallback<AVObject>() {
            @Override
            public void done(final AVObject account, AVException e) {
                if (e == null) {

                        switch (type){
                            case PARAM_TYPE_CONNECT_NETWORK_TYPE:
                                account.put(PARAM_TYPE_CONNECT_NETWORK, "");
                                break;
                            case PARAM_TYPE_SATURATION_TYPE://激发快
                                account.put(PARAM_TYPE_SATURATION, -1);
                                break;
                            case PARAM_TYPE_CONTRAST_TYPE://物镜
                                account.put(PARAM_TYPE_CONTRAST, -1);
                                break;
                            case PARAM_TYPE_VESSELS_TYPE://器皿
                                account.put(PARAM_TYPE_VESSELS, -1);
                                break;

                            case PARAM_TYPE_ISO_TYPE://ISO
                                account.put(PARAM_TYPE_ISO, -1);
                                break;
                            case PARAM_TYPE_BRIGHTNESS_TYPE://brightness
                                account.put(PARAM_TYPE_BRIGHTNESS, -1);
                                break;
                            case PARAM_TYPE_GAMMA_TYPE://gamma +
                                account.put(PARAM_TYPE_GAMMA, -1);
                                break;
                            case PARAM_TYPE_GAMMA_DOMN_TYPE://gamma -
                                account.put(PARAM_TYPE_GAMMA_DOMN, -1);
                                break;
                            case PARAM_TYPE_CONSTAST_TYPE:
                                JSONObject object = new JSONObject();

                                try {
                                    object.put("postion",-1);
                                    object.put("result",false);
                                } catch (JSONException e1) {
                                    e1.printStackTrace();
                                }
                                account.put(PARAM_TYPE_CONSTAST, -1);
                                break;
                            case CAPTURE_TYPE://拍照
                                account.put(CAPTURE, false);
                                break;
                            case PARAM_TYPE_AUTOPHOTO_TYPE://自动拍照
                                JSONObject autophoto = new JSONObject();
                                try {
                                    autophoto.put("autophoto",false);
                                    autophoto.put("autophotolights","null");
                                    autophoto.put("autofocus","null");
                                    autophoto.put("synthetic","null");
                                    autophoto.put("autophoto_starttimer","null");
                                    autophoto.put("autophoto_ttimer","null");
                                    autophoto.put("autophoto_endtimer","null");
                                    autophoto.put("autophoto_stopstr","null");
                                    autophoto.put("autophoto_count","null");
                                } catch (JSONException e2) {
                                    e2.printStackTrace();
                                }
                                account.put(PARAM_TYPE_AUTOPHOTO,autophoto);
                                break;
                            case PARAM_TYPE_STOPAUTOPHOTO_TYPE://停止自动拍照
                                account.put(PARAM_TYPE_STOPAUTOPHOTO,false);
                                break;
                            case PARAM_TYPE_MANUAL_FOCUS_TYPE://自动对焦
                                JSONObject autofocus = new JSONObject();
                                try {
                                    autofocus.put("autofocus",false);
                                    autofocus.put("x",-1);
                                    autofocus.put("y",-1);
                                } catch (JSONException e3) {
                                    e3.printStackTrace();
                                }
                                account.put(PARAM_TYPE_MANUAL_FOCUS, autofocus);
                            case PARAM_TYPE_MANUAL_FOCUS_STOP_TYPE://停止自动对焦
                                account.put(PARAM_TYPE_MANUAL_FOCUS_STOP, false);
                            case CAPTURE_STILL_TYPE://录制视频
                                account.put(CAPTURE_STILL, false);
                                break;
                            case PARAM_TYPE_UpDATE_PICTER_TYPE://更新右下角缩略图
                                JSONObject pictureName = new JSONObject();
                                try {
                                    final List<File> pngFiles = PictureUtils.getPicturesScaled(context);
                                    StringBuffer buffer = new StringBuffer();
                                    for (int i = 0; i < pngFiles.size(); i++) {
                                        buffer.append(pngFiles.get(i).getPath()+"&");
                                    }
                                    Log.w("abcde","buffer="+buffer);
                                    pictureName.put("picture",false);
                                    pictureName.put("filename",buffer);

                                } catch (JSONException e4) {
                                    e4.printStackTrace();
                                }
                                account.put(PARAM_TYPE_UpDATE_PICTER, pictureName);
                                break;
                            case PARAM_TYPE_ROCKERVIEW_TYPE://摇杆状态
                                JSONObject rocketview = new JSONObject();
                                try {
                                    rocketview.put("rocketview",false);
                                    rocketview.put("command","null");

                                } catch (JSONException e5) {
                                    e5.printStackTrace();
                                }
                                account.put(PARAM_TYPE_ROCKERVIEW,rocketview);
                                break;
                            case PARAM_TYPE_PIC_LOAD_TYPE://图片

                                Bitmap bitmap = BitmapFactory.decodeFile(RequesUtils.PIC_NAME);
                                if(bitmap!=null) {
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                    account.put(PARAM_TYPE_PIC_LOAD, baos.toByteArray());
                                }
                                break;
                            case PARAM_TYPE_LOAD_PIC_NAME_TYPE://图片文件名
                                account.put(PARAM_TYPE_LOAD_PIC_NAME,"null");
                                break;
                        }

                    AVSaveOption option = new AVSaveOption();
                    option.query(new AVQuery<>(CLIENT_INSTURCTION).whereEqualTo("user", ConstantUtil.remoteUserName));
                    option.setFetchWhenSave(true);
                    account.saveInBackground();
                }
            }
        });
    }
}
