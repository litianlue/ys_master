package com.yeespec.microscope.master.service.server.http.api;

import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.yeespec.libuvccamera.uvccamera.service.UVCService;
import com.yeespec.microscope.master.activity.MasterActivity;
import com.yeespec.microscope.master.application.BaseApplication;
import com.yeespec.microscope.master.service.client.socket.model.PageModel;

import com.yeespec.microscope.master.service.client.socket.model.VedioModel;
import com.yeespec.microscope.master.service.client.socket.model.WifiModel;
import com.yeespec.microscope.master.service.server.websocket.api.channel.CameraOperationChannel;
import com.yeespec.microscope.utils.ConstantUtil;
import com.yeespec.microscope.utils.FileUtils;
import com.yeespec.microscope.utils.SPUtils;
import com.yeespec.microscope.utils.ZipUtils;
import com.yeespec.microscope.utils.log.Logger;
import com.yeespec.microscope.utils.wifi.WifiAdmin;
import com.yeespec.microscope.utils.wifi.WifiConnector;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yeespec.libuvccamera.uvccamera.service.UVCService.rockerState;

/**
 * Created by virgilyan on 15/10/12.
 */
public class HttpServerService {        //创建一个http  server , 将平板端作为一个服务器 , 手机端作为从机来访问 ;

    protected static final boolean DEBUG = false;
    private static final String TAG = HttpServerService.class.getSimpleName();

    private final int CODE_200 = 200;//访问成功
    private final int CODE_401 = 401;//参数错误
    private final int CODE_402 = 402;//其他错误信息
    /**
     * 拍照相关
     */
    public static final String CAMERA_TAKE_PHOTO = "/camera/takephoto";
    public static final String CAMERA_GET_PHOTO_NAME = "/camera/getphotolist";
    public static final String CAMERA_GET_PHOTO = "/camera/getphoto";
    public static final String CAMERA_DOWNLOAD_PHOTO = "/camera/download_photo";
    /**
     * 录像相关
     */
    public static final String VIDEO_ACTION = "/video/action";
    public static final String VIDEO_GET_VIDEO_NAME = "/video/get_video_list";
    public static final String VIDEO_GET_VIDEO = "/video/get_video";
    public static final String VIDEO_DOWNLOAD_VIDEO = "/video/download_video";

    /**
     * 取色，着色
     */
    public static final String GET_COLOR = "/color/get";
    public static final String SET_COLOR = "/color/set";
    public static final String GET_COLORSTRING = "/colorstring/get";
    public static final String SET_COLORSTRING= "/colorstring/set";
    public static final String GET_CONFIGURATION= "/configuration/get";
    public static final String SET_CONFIGURATION= "/configuration/set";
    /**
     * wifi相关
     */
    public static final String GET_WIFI_LIST = "/wifi/get";
    public static final String SET_WIFI = "/wifi/set";

    public static final String SHUT_DOWN = "/pad/shutdown";
    public static String DIR_NAME;

   // private static  String DIR_NAME = "USBCameraTest";
    public HttpServerService(AsyncHttpServer server) {

        Util.SUPRESS_DEBUG_EXCEPTIONS = true;

        SharedPreferences sharedPreferences =BaseApplication.getContext().getSharedPreferences(ConstantUtil.CURRENT_USER_SHAR_DIR,0);
        String currentName = sharedPreferences.getString(ConstantUtil.CURRENT__USER_SHAR, null);
        DIR_NAME = currentName;

        takePhoto(server);
        getPhotobyPage(server);
        getPhoto(server);
        getVediobyPage(server);
        getVideo(server);
        VedioAction(server);
        setTheColor(server);
        getTheColorString(server);
        setTheColorString(server);
        getTheColor(server);

        getWifiList(server);
        setWifi(server);
        setShutDown(server);
        downloadPicture(server);
        downloadVideo(server);

        getConfiguration(server);
        setConfiguration(server);
    }

    private String getResponJSON(int code, String msg) {
        Map<String, String> result = new HashMap<String, String>();
        result.put("code", String.valueOf(code));
        result.put("message", msg);
        return JSON.toJSONString(result);
    }



   /* public List<File> getPicturesForFile() {
        List<File> list = new ArrayList<>();
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),DIR_NAME);
        File[] allFiles = file.listFiles();
        if (allFiles == null) {
            return list;
        }
        for (int k = 0; k < allFiles.length; k++) {
            final File fi = allFiles[k];
            if (fi.isFile()) {
                int idx = fi.getPath().lastIndexOf(".");
                if (idx <= 0) {
                    continue;
                }
                if (fi.getPath().contains("scaled")) {
                  continue;
                }

                String suffix = fi.getPath().substring(idx);
                if (suffix.toLowerCase().equals(".jpg") ||
                        suffix.toLowerCase().equals(".jpeg") ||
                        suffix.toLowerCase().equals(".bmp") ||
                        suffix.toLowerCase().equals(".png") ||
                        suffix.toLowerCase().equals(".gif")) {
                    list.add(fi);
                }
            }
        }
        return list;
    }
*/
   public List<File> getPicturesForFile() {
       List<File> list = new ArrayList<>();
       File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),DIR_NAME+"/scale");
       File[] allFiles = file.listFiles();
       if (allFiles == null) {
           return list;
       }
       for (int k = 0; k < allFiles.length; k++) {
           final File fi = allFiles[k];
           if (fi.isFile()) {
               /*int idx = fi.getPath().lastIndexOf(".");
               if (idx <= 0) {
                   continue;
               }
               if (fi.getPath().contains(".scaled")&&!fi.getPath().contains(".jpg")) {
                   list.add(fi);
               }*/
               list.add(fi);

           }
       }
       return list;
   }
    public List<File> getMoviesForFile() {
        List<File> list = new ArrayList<>();
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), DIR_NAME);
        File[] allfiles = file.listFiles();
        if (allfiles == null) {
            return list;
        }
        for (int k = 0; k < allfiles.length; k++) {
            final File fi = allfiles[k];
            if (fi.isFile()) {
                int idx = fi.getPath().lastIndexOf(".");
                if (idx <= 0) {
                    continue;
                }
                String suffix = fi.getPath().substring(idx);
                if (suffix.toLowerCase().equals(".mp4")) {
                    list.add(fi);
                }
            }
        }
        return list;
    }



    public List<VedioModel> getMovies() {
        List<VedioModel> list = new ArrayList<VedioModel>();
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), DIR_NAME);
        File[] allfiles = file.listFiles();
        if (allfiles == null) {
            return null;
        }
        for (int k = 0; k < allfiles.length; k++) {
            final File fi = allfiles[k];
            if (fi.isFile()) {
                int idx = fi.getPath().lastIndexOf(DIR_NAME);
                if (idx <= 0) {
                    continue;
                }
                String filename = fi.getPath().substring(idx + 14);
                VedioModel model = new VedioModel();
                model.setVedioName(filename);
                list.add(model);
            }
        }
        return list;
    }

    /**
     * 启动相机并且拍照
     * 手机端遥控平板端拍照 :
     *
     * @param server
     */
    private void takePhoto(AsyncHttpServer server) {
        server.get(CAMERA_TAKE_PHOTO, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                //无请求头信息 , 有响应回应
                JSONObject object = new JSONObject();
                object.put("type", 0);
                object.put("param", 0);
                UVCService.WEB_SOCKET_PICTURE_SERVER.CHANNELS.get(CameraOperationChannel.CHANNEL_NAME).broadcast(object.toJSONString());
                response.send(getResponJSON(CODE_200, "拍照成功"));
            }
        });
    }

    final ArrayList<File> allFile = new ArrayList<>();

    /**
     * 获取图片list
     *
     * @param server
     */
    private void getPhotobyPage(AsyncHttpServer server) {
        server.get(CAMERA_GET_PHOTO_NAME, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                //有请求头信息 ,  有响应回应
                List<String> page = request.getQuery().get("page");
                Map<String, Object> map = new HashMap<String, Object>();
                List<File> pngFiles = getPicturesForFile();
                List<File> mp4Files = getMoviesForFile();
           //    Log.e("HttpServerService","page="+page.size());
                allFile.clear();
                Collections.reverse(pngFiles);
                allFile.addAll(pngFiles);
                Collections.reverse(mp4Files);
                allFile.addAll(mp4Files);
                Collections.sort(allFile, new FileComparator());//通过重写Comparator的实现类

                if (page != null && page.size() > 0) {
                    List<String> fileModels = new ArrayList<>();
                    int mpage = Integer.valueOf(page.get(0).toString());
                    if (allFile.size() < 10) {//数量少于10，不分页，直接返回
                        for (File file : allFile) {
                            fileModels.add(file.getName());
                        }
                        map.put("code", "200");
                        map.put("message", "访问成功");
                        map.put("list", fileModels);
                        response.send(JSON.toJSONString(map));
                    } else {
                        if (Integer.valueOf(page.get(0)) <= 0) {
                            response.send(getResponJSON(CODE_401, "参数错误"));
                        } else {
                            int startNum =0;
                            int endNum =0;
                           if((mpage-1)*10>=allFile.size()){
                               response.send(getResponJSON(CODE_401, "已经是最后一页"));
                               return;
                           }
                           if((mpage-1)*10<allFile.size())
                           {
                               startNum = (mpage-1)*10;
                               if((mpage-1)*10+10>=allFile.size())
                               {
                                   endNum = allFile.size();
                               }else
                               if((mpage-1)*10+10<allFile.size())
                               {
                                   endNum = (mpage-1)*10+10;
                               }
                           }
                           //加载指定页数据
                            // ，每页10条
                            for (int i = startNum; i <endNum; i++) {
                                fileModels.add(allFile.get(i).getName());
                            }
                            map.put("code", "200");
                            map .put("message", "访问成功");
                            map.put("list", fileModels);
                            response.send(JSON.toJSONString(map));


                        }
                    }
                } else {
                    response.send(getResponJSON(CODE_401, "参数错误"));
                }
            }
        });
    }


    /**
     * 读取文件创建时间
     * 更换为读取文件的创建的ID
     */
    public class FileComparator implements Comparator<File> {
        /* public int compare(File file1, File file2) {
             if (file1.lastModified() > file2.lastModified()) {

                 return -1;
             } else {
                 return 1;
             }
         }*/
        public int compare(File file1, File file2) {

            //Z
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
 /*   //文件转base64字符串
    public static String encodeBase64File( File  file ) throws Exception {
        FileInputStream inputFile = new FileInputStream(file);
        byte[] buffer = new byte[(int)file.length()];
        inputFile.read(buffer);
        inputFile.close();
        return android.util.Base64.encodeToString(buffer, Base64.DEFAULT);
    }*/
    /**
     * 获取一张图片
     *
     * @param server
     */
    private void getPhoto(AsyncHttpServer server) {
        server.get(CAMERA_GET_PHOTO, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                //有请求头信息 ,  有响应回应
                List<String> name = request.getQuery().get("picname");
                if (name != null && name.size() > 0) {
                    File pic =null;
                    Log.w("FileUtil","name.get(0)="+name.get(0));
                    if(name.get(0).indexOf("jpg")!=-1){
                        pic= new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/" + DIR_NAME +"/request"+ "/" + name.get(0));
                    }else
                        pic= new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/" + DIR_NAME +"/scale"+ "/" + name.get(0));

                    if (pic.isFile() && pic.exists()) {
                       // if (DEBUG)
                        //    Logger.e("图片存在");
                        response.sendFile(pic);
                    } else {
                       //if (DEBUG)
                        //    Logger.e("图片不存在");
                        response.send(getResponJSON(CODE_402, "文件不存在"));
                    }
                } else {
                    //                    ZipUtils.createZip(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/" + DIR_NAME,
                    //                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/USBCameraTest.zip");
                    //                    response.sendFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/USBCameraTest.zip"));
                    response.send(getResponJSON(CODE_401, "参数错误"));
                }
            }
        });
    }

    private void downloadPicture(AsyncHttpServer server) {
        server.get(CAMERA_DOWNLOAD_PHOTO, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                //有请求头信息 ,  有响应回应
                List<String> name = request.getQuery().get("picname");

                if (name != null && name.size() > 0) {

                    File pic = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/" + DIR_NAME +"/request"+ "/" + name.get(0));
                   // if (DEBUG)
                    //    Logger.e(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/" + DIR_NAME + "/" + name.get(0));
                    if (pic.isFile() && pic.exists()) {
                      //  if (DEBUG)
                       //     Logger.e("图片存在");
                        response.sendFile(pic);
                    } else {
                        //if (DEBUG)
                        //    Logger.e("图片不存在");
                        response.send(getResponJSON(CODE_402, "文件不存在"));
                    }
                } else {
                    ZipUtils.createZip(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/" + DIR_NAME+"/request",
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/"+DIR_NAME+"/request"+ ".zip");
                    response.sendFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/"+DIR_NAME+"/request"+ ".zip"));
                }
            }
        });
    }


    /**
     * 获取视频list
     *
     * @param server
     */
    private void getVediobyPage(AsyncHttpServer server) {
        server.get(VIDEO_GET_VIDEO_NAME, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                //有请求头信息 ,  有响应回应
                List<String> page = request.getQuery().get("page");
                if (page == null || page.size() == 0)
                    page.add("1");
                Map<String, Object> map = new HashMap<String, Object>();
                List<VedioModel> moviesList = getMovies();
                Collections.reverse(moviesList);
                if (page != null && page.size() > 0) {
                    if (moviesList.size() < 10) {//数量少于10，不分页，直接返回
                        map.put("code", "200");
                        map.put("message", "访问成功");
                        map.put("list", moviesList);
                        response.send(JSON.toJSONString(map));
                    } else {
                        if (Integer.valueOf(page.get(0)) <= 0) {
                            response.send(getResponJSON(CODE_401, "参数错误"));
                        } else {
                            PageModel pm = new PageModel(moviesList, 10);//每页显示条数
                            List sublist = pm.getObjects(Integer.valueOf(page.get(0)));//显示第几页
                            map.put("code", "200");
                            map.put("message", "访问成功");
                            map.put("list", sublist);
                            response.send(JSON.toJSONString(map));
                        }
                    }
                } else {
                    response.send(getResponJSON(CODE_401, "参数错误"));
                }
            }
        });
    }

    /**
     * 获取搜索到的wifi列表
     *
     * @param server
     */
    private void getWifiList(AsyncHttpServer server) {
        server.get(GET_WIFI_LIST, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                //无请求头信息 ,  有响应回应
                WifiAdmin wifiAdmin = new WifiAdmin(BaseApplication.getContext());
                wifiAdmin.startScan();
                Map<String, Object> map = new HashMap<String, Object>();
                final List<ScanResult> wifiScans = wifiAdmin.getWifiList();
                //if (DEBUG)
                 //   Logger.e("wifi list: " + wifiScans.size());
                List<WifiModel> result = new ArrayList<WifiModel>();
                if (wifiScans != null && wifiScans.size() > 0) {
                    for (int i = 0; i < wifiScans.size(); i++) {
                        WifiModel temp = new WifiModel();
                        temp.setWifiName(wifiScans.get(i).SSID);
                        temp.setSecurityMode(wifiScans.get(i).capabilities);
                        temp.setLevel(WifiManager.calculateSignalLevel(wifiScans.get(i).level, 100));
                        result.add(temp);
                    }
                    map.put("code", "200");
                    map.put("message", "访问成功");
                    map.put("list", result);
                    response.send(JSON.toJSONString(map));
                } else {
                    response.send(getResponJSON(CODE_402, "获取wifi失败"));
                }
            }
        });
    }

    /**
     * 选择要连接的wifi
     * http://192.168.31.106:5000/wifi/set?ssid=mi-geetion&password=iam80works&secureMode=WPA2
     *
     * @param server
     */
    private void setWifi(AsyncHttpServer server) {
        server.get(SET_WIFI, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                //有请求头信息 ,  有响应回应
                //获取SSID，密码，连接模式
                List<String> ssid = request.getQuery().get("ssid");
                List<String> pwd = request.getQuery().get("password");
                List<String> mode = request.getQuery().get("secureMode");

                if (ssid != null && ssid.size() > 0 && pwd != null && pwd.size() > 0 && mode != null && mode.size() > 0) {
                    try {
                        //连接回调
                        WifiConnector connector = new WifiConnector(BaseApplication.getContext(), new WifiConnector.WifiConnectListener() {
                            @Override
                            public void OnWifiConnectCompleted(boolean isConnected) {
                                if (isConnected) {
                                    if (DEBUG)
                                        Logger.e("连接成功");
                                } else {
                                    if (DEBUG)
                                        Logger.e("连接失败");
                                }
                            }
                        });
                        WifiConnector.SecurityMode connectType = null;
                        if (mode.get(0).contains("WPA2")) {
                            connectType = WifiConnector.SecurityMode.WPA2;
                        } else if (mode.get(0).contains("WPA")) {
                            connectType = WifiConnector.SecurityMode.WPA;
                        } else if (mode.get(0).contains("WEP")) {
                            connectType = WifiConnector.SecurityMode.WEP;
                        } else if (mode.get(0).contains("OPEN")) {
                            connectType = WifiConnector.SecurityMode.OPEN;
                        }
                        //开始连接
                        connector.connect(ssid.get(0), pwd.get(0), connectType);
                        response.send(getResponJSON(CODE_200, "配置成功，请切换到对应wifi下检查是否连接成功"));
                    } catch (Exception e) {
                        e.printStackTrace();
                        response.send(getResponJSON(CODE_402, "连接失败，请检查ssid和密码或稍后重试"));
                    }
                } else {
                    response.send(getResponJSON(CODE_401, "参数错误"));
                }

            }
        });
    }


    /**
     * 获取一个视频
     *
     * @param server
     */
    private void getVideo(AsyncHttpServer server) {
        server.get(VIDEO_GET_VIDEO, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                //有请求头信息 ,  有响应回应

                List<String> name = request.getQuery().get("video_name");
                if (name != null && name.size() > 0) {
                    File movie = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/" + DIR_NAME + "/" + name.get(0));
                    //if (DEBUG)
                     //   Logger.e(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/" + DIR_NAME + "/" + name.get(0));
                    if (movie.isFile() && movie.exists()) {
                        if (DEBUG)
                            Logger.e("视频存在");
                        response.sendFile(movie);
                    } else {
                        if (DEBUG)
                            Logger.e("视频不存在");
                        response.send(getResponJSON(CODE_402, "视频不存在"));
                    }
                } else {
                    response.send(getResponJSON(CODE_401, "参数错误"));
                }
            }
        });
    }

    private void downloadVideo(AsyncHttpServer server) {
        server.get(VIDEO_DOWNLOAD_VIDEO, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                //无请求头信息 ,  有响应回应
                ZipUtils.createZip(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/" + DIR_NAME,
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/"+DIR_NAME+".zip");
                File pic = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/"+DIR_NAME+".zip");
                if (pic.isFile() && pic.exists()) {
                    if (DEBUG)
                        Logger.e("压缩成功");
                    response.sendFile(pic);
                } else {
                    if (DEBUG)
                        Logger.e("压缩失败");
                    response.send(getResponJSON(CODE_402, "服务器异常，请联系开发者"));
                }
            }
        });
    }

    /**
     * 启动相机，跟住参数设置开始拍摄或者停止
     * 手机端遥控平板端录像 :
     * key: actionType
     * value: 0(开始拍摄)，1(停止拍摄)
     *
     * @param server
     */
    private void VedioAction(final AsyncHttpServer server) {
        server.get(VIDEO_ACTION, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                //有请求头信息 ,  有响应回应
                JSONObject object = new JSONObject();
                object.put("type", 1);
                List<String> actionCode = request.getQuery().get("actionType");
                if (actionCode != null && actionCode.size() > 0) {
                    if (actionCode.get(0).equals("0")) {
                        // TODO 开始拍摄
                        if (DEBUG)
                            Logger.e("开始录像");
                        object.put("param", 0);
                        response.send(getResponJSON(CODE_200, "开始录像"));
                    } else if (actionCode.get(0).equals("1")) {
                        // TODO 停止拍摄
                        object.put("param", 1);
                        if (DEBUG)
                            Logger.e("停止录像");
                        response.send(getResponJSON(CODE_200, "停止录像"));
                    }
                } else {
                    response.send(getResponJSON(CODE_401, "参数错误"));
                }
                UVCService.WEB_SOCKET_PICTURE_SERVER.CHANNELS.get(CameraOperationChannel.CHANNEL_NAME).broadcast(object.toJSONString());
            }
        });
    }

    /**
     * 取色
     *
     * @param server
     */
    private void getTheColor(AsyncHttpServer server) {
        server.get(GET_COLOR, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                //无请求头信息 ,  有响应回应

                Object pos = SPUtils.get(BaseApplication.getContext(), "recolor", 0);
                Map<String, Object> result = new HashMap<>();
                result.put("code", "200");
                result.put("message", "访问成功");
                result.put("recolor", pos);
                response.send(JSON.toJSONString(result));
            }
        });
    }
    private void getTheColorString(AsyncHttpServer server) {
        server.get(GET_COLORSTRING, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                //无请求头信息 ,  有响应回应

                Object pos = SPUtils.get(BaseApplication.getContext(), "recolorstring", 0);
                Map<String, Object> result = new HashMap<>();
                result.put("code", "200");
                result.put("message", "访问成功");
                result.put("recolorstring", pos);
                response.send(JSON.toJSONString(result));
            }
        });
    }
    private void getConfiguration(AsyncHttpServer server) {
        server.get(GET_CONFIGURATION, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                //无请求头信息 ,  有响应回应

                Object pos = SPUtils.get(BaseApplication.getContext(), "configuration", 0);
                Map<String, Object> result = new HashMap<>();
                result.put("code", "200");
                result.put("message", "访问成功");
                result.put("configuration", pos);
                response.send(JSON.toJSONString(result));
            }
        });
    }

    /**
     * 着色
     * 手机端遥控平板端拍照 :
     *
     * @param server
     */
    private void setTheColor(final AsyncHttpServer server) {
        server.get(SET_COLOR, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                //有请求头信息 ,  有响应回应
                JSONObject object = new JSONObject();
                object.put("type", 4);
                String pos = request.getQuery().getString("recolor");
                Map<String, String> result = new HashMap<String, String>();
                if (!TextUtils.isEmpty(pos)) {
                    object.put("param", Integer.parseInt(pos));
                    result.put("code", "200");
                    result.put("message", "访问成功");
                    response.send(JSON.toJSONString(result));
                    UVCService.WEB_SOCKET_PICTURE_SERVER.CHANNELS.get(CameraOperationChannel.CHANNEL_NAME).broadcast(object.toJSONString());
                } else {
                    response.send(getResponJSON(CODE_401, "参数错误"));
                }
            }
        });
    }
    private void setTheColorString(final AsyncHttpServer server) {
        server.get(SET_COLORSTRING, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                //有请求头信息 ,  有响应回应
                JSONObject object = new JSONObject();
                object.put("type", 24);
                String pos = request.getQuery().getString("recolorstring");
             //   Log.e("MasterIntentService","pos="+pos);
                Map<String, String> result = new HashMap<String, String>();
                if (!TextUtils.isEmpty(pos)) {
                    object.put("param", 24);
                    object.put("recolorstring", pos);
                    result.put("code", "200");
                    result.put("message", "请求参数成功");
                    response.send(JSON.toJSONString(result));
                    UVCService.WEB_SOCKET_PICTURE_SERVER.CHANNELS.get(CameraOperationChannel.CHANNEL_NAME).broadcast(object.toJSONString());
                } else {
                    response.send(getResponJSON(CODE_401, "参数错误"));
                }
            }
        });
    }

    private void setConfiguration(final AsyncHttpServer server) {
        server.get(SET_CONFIGURATION, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                //有请求头信息 ,  有响应回应
                JSONObject object = new JSONObject();
                object.put("type", 300);


                File filedir = Environment.getExternalStorageDirectory().getAbsoluteFile();
                String s = FileUtils.getFileString(filedir + "", "developerset.txt");
                String lighttype="1,2,3,4";
                String objectstring="5,20";
                if (s != null && !s.equals("")) {
                    String[] split = s.split("\n");
                    lighttype= split[0].trim();
                    objectstring= split[1].trim();

                }
                String pos = request.getQuery().getString("configuration");
              //  String sendstring  = objectstring+"&"+lighttype+"&"+UVCService.maxbrightness+"&"+UVCService.maxiso;
                String sendstring  = objectstring+"&"+lighttype+"&"+UVCService.maxbrightness+"&"+UVCService.maxiso+"&"+ConstantUtil.LONZA+"&"+rockerState;
                Map<String, String> result = new HashMap<String, String>();
                if (!TextUtils.isEmpty(pos)) {
                    object.put("param", 300);
                    object.put("configuration", pos);
                    result.put("code", "200");
                    result.put("message", sendstring);
                    response.send(JSON.toJSONString(result));
                    UVCService.WEB_SOCKET_PICTURE_SERVER.CHANNELS.get(CameraOperationChannel.CHANNEL_NAME).broadcast(object.toJSONString());
                } else {
                    response.send(getResponJSON(CODE_401, "参数错误"));
                }
            }
        });
    }
    //手机端遥控平板端关机 ;
    private void setShutDown(final AsyncHttpServer server) {
        server.get(SHUT_DOWN, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                //无请求头信息 ,  无响应回应
                JSONObject object = new JSONObject();
                object.put("type", 7);
                object.put("param", 0);
                UVCService.WEB_SOCKET_PICTURE_SERVER.CHANNELS.get(CameraOperationChannel.CHANNEL_NAME).broadcast(object.toJSONString());
            }
        });
    }
}

