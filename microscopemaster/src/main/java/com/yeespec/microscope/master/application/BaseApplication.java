package com.yeespec.microscope.master.application;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Process;
import android.support.multidex.MultiDex;
import android.util.Log;
import android.widget.Toast;

import com.avos.avoscloud.AVOSCloud;
import com.orhanobut.hawk.Hawk;
import com.orhanobut.hawk.HawkBuilder;
import com.squareup.leakcanary.LeakCanary;
import com.yeespec.microscope.master.entity.LanHost;
import com.yeespec.microscope.master.service.client.socket.model.MBOXConst;
import com.yeespec.microscope.utils.ConstantUtil;
import com.yeespec.microscope.utils.CrashHandler;
import com.yeespec.microscope.utils.FileUtils;
import com.yeespec.microscope.utils.NetworkUtils;
import com.yeespec.microscope.utils.UnCeHandler;
import com.yeespec.microscope.utils.fresco.tool.FrescoTool;
import com.yeespec.microscope.utils.log.LogLevel;
import com.yeespec.microscope.utils.log.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by virgilyan on 15/7/1.
 */
public class BaseApplication extends Application {
    private final String APP_ID = "895a6GU3rutpnpo1AtBAmT6a-gzGzoHsz";
    private final String APP_KEY = "6fxxrUedtuH7Bg8jUC0oC1hO";
    private  boolean isfirst_run = false;
   //20170632 定义全局变量
    private int gain=0; //记录ISO值
    private int brightness=0;//记录亮度值
    private int saturation=0;//记录激发块
    private int saturation_pos=4;//记录选中的激发块显示图
    private int CurrentSurfaceCode=0;//当前Surface id值

    private int contrast=0;//记录物镜倍数值   1
    private int current_contrast=0;
    private int currentContrast=0;//用于拍照名字记录信息
    private int recolor=0;//记录染色
    private int preRecolor = 0;
    private int recolor_pos=0;
    private String tintingString="No-Color"; //
    private String currentSaturationString="BlackLight";
    private String currentReColorString="No-Color";//染料名称
    private String autoPhoto_startTime="";
    private String autoPhoto_tTime="";
    private String autoPhoto_finishTime="";

    private int gamma=0;//记录焦点值

    public boolean getirst_run() {
        return isfirst_run;
    }
    public void setirst_run(boolean b) {
        this.isfirst_run = b;
    }
    private boolean self_check =false;
    private  Activity mActivity;
    public int getRecolor_pos() {
        return recolor_pos;
    }

    public void setRecolor_pos(int recolor_pos) {
        this.recolor_pos = recolor_pos;
    }

    public int getCurrentSurfaceCode() {
        return CurrentSurfaceCode;
    }

    public void setCurrentSurfaceCode(int currentSurfaceCode) {
        CurrentSurfaceCode = currentSurfaceCode;
    }
    public String getCurrentReColorString() {
        return currentReColorString;
    }

    public void setCurrentReColorString(String currentReColorString) {
        this.currentReColorString = currentReColorString;
    }

    public void setAutoPhoto_startTime(String autoPhoto_startTime) {
        this.autoPhoto_startTime = autoPhoto_startTime;
    }
    public void setAutoPhoto_tTime(String autoPhoto_tTime) {
        this.autoPhoto_tTime = autoPhoto_tTime;
    }

    public void setAutoPhoto_finishTime(String autoPhoto_finishTime) {
        this.autoPhoto_finishTime = autoPhoto_finishTime;
    }

    public String getAutoPhoto_startTime() {
        return autoPhoto_startTime;
    }

    public String getAutoPhoto_tTime() {
        return autoPhoto_tTime;
    }

    public String getAutoPhoto_finishTime() {
        return autoPhoto_finishTime;
    }

    private int autoPhoto_views=1;//记录拍照状体 1 为拍照 2 为准备自动拍照  3 为正在自动拍照

    public String getCurrentSaturationString() {
        return currentSaturationString;
    }

    public void setCurrentSaturationString(String currentSaturationString) {
        this.currentSaturationString = currentSaturationString;
    }

    public boolean isSelf_check() {
        return self_check;
    }

    public void setSelf_check(boolean self_check) {
        this.self_check = self_check;
    }

    public int getPreRecolor() {
        return preRecolor;
    }

    public void setPreRecolor(int preRecolor) {
        this.preRecolor = preRecolor;
    }

    public int getSaturation_pos() {
        return saturation_pos;
    }
    public int getGamma() {
        return gamma;
    }
    public void setGamma(int gamma) {
        this.gamma = gamma;
    }
    public String getTintingString() {
        return tintingString;
    }
    public void setTintingString(String tintingString) {
        this.tintingString = tintingString;
    }
    public int getRecolor() {
        return recolor;
    }
    public void setRecolor(int recolor) {
        this.recolor = recolor;
    }
    public int getCurrent_contrast() {
        return current_contrast;
    }
    public int getCurrentContrast() {
        return currentContrast;
    }
    public void setCurrent_contrast(int current_contrast) {
        this.current_contrast = current_contrast;
    }
    public void setCurrentContrast(int currentContrast) {
        this.currentContrast = currentContrast;
    }
    public void setSaturation_pos(int saturation_pos) {
        this.saturation_pos = saturation_pos;
    }
    public int getGain() {
        return gain;
    }
    public int getBrightness() {
        return brightness;
    }
    public int getSaturation() {
        return saturation;
    }
    public int getContrast() {
        return contrast;
    }
    public void setGain(int gain) {
        this.gain = gain;
    }
    public int getAutoPhoto_views() {
        return autoPhoto_views;
    }
    public void     setAutoPhoto_views(int autoPhoto_views) {
        this.autoPhoto_views = autoPhoto_views;
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }
    public void setSaturation(int saturation) {
        this.saturation = saturation;
    }
    public void setContrast(int contrast) {
        this.contrast = contrast;
    }
    protected static final boolean DEBUG = false;
    private static final String TAG = BaseApplication.class.getName();

    public static BaseApplication instance;

    public  static  String DIR_NAME ="admin";

    private static Context mContext;

    public static final List<String[]> COLORS = new ArrayList<>();
    ArrayList<Activity> list = new ArrayList<Activity>();

    public void init() {
        //设置该CrashHandler为程序的默认处理器
        UnCeHandler catchExcep = new UnCeHandler(this);
        Thread.setDefaultUncaughtExceptionHandler(catchExcep);
    }

    /**
     * Activity关闭时，删除Activity列表中的Activity对象
     */
    public void removeActivity(Activity a) {
        list.remove(a);
    }

    /**
     * 向Activity列表中添加Activity对象
     */
    public void addActivity(Activity a) {
        list.add(a);
    }

    /**
     * 关闭Activity列表中的所有Activity
     */
    public void finishActivity() {
        for (Activity activity : list) {
            if (null != activity) {
                activity.finish();
            }
        }
        //杀死该应用进程
        android.os.Process.killProcess(android.os.Process.myPid());
    }
    // 单例模式获取唯一的BaseApplication实例
    public static BaseApplication getInstance() {
        if (null == instance) {
            instance = new BaseApplication();
        }
        return instance;
    }

    private static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                Log.i("skyapp", "checkClientTrusted");
            }
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                Log.i("skyapp", "checkServerTrusted");
            }
        } };
        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void destroy(){
        FileUtils.writeFileToLogFolder("准备重启，BaseApplication");
        Intent LaunchIntent = BaseApplication.getContext().getPackageManager().getLaunchIntentForPackage(BaseApplication.getContext().getPackageName());
        LaunchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        BaseApplication.getContext().startActivity(LaunchIntent);
        ActivityManager am = (ActivityManager)BaseApplication.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo procInfo : runningApps) {
            Log.w("mytestprocess", "processname=" + procInfo.processName);
            Log.w("mytestprocess", "processid=" + procInfo.pid);
            Process.killProcess(procInfo.pid);
            am.restartPackage(procInfo.processName);
        }
        System.gc();
        System.exit(0);
    }
    @Override
    public void onCreate() {
        super.onCreate();
        MultiDex.install(this);
        trustAllHosts();//信任所有证书
        // 启用中国节点, 需要在 initialize 之前调用
        AVOSCloud.useAVCloudCN();
        AVOSCloud.initialize(this,APP_ID,APP_KEY);

        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getApplicationContext());

        Hawk.init(this)
                .setEncryptionMethod(HawkBuilder.EncryptionMethod.MEDIUM)
                .setStorage(HawkBuilder.newSqliteStorage(this))
                .build();

//        if (LeakCanary.isInAnalyzerProcess(this)) {//1
//            // This process is dedicated to LeakCanary for heap analysis.
//            // You should not init your app in this process.
//            return;
//        }
//        LeakCanary.install(this);

        //初始化腾讯bugly插件 :
        //        CrashReport.initCrashReport(this, "900017559", true);
      //  ConstantUtil.savaCurrentUser(getApplicationContext(),"CameraTest");
      //  Log.e("application","CurrentUser="+ConstantUtil.getCurrentUserName(getApplicationContext()));
       // Log.e("application","application onCreate()="+getAllUserName().size());
        COLORS.clear();
        mContext = getApplicationContext();
      //  Toast.makeText(mContext, "BaseApplication: onCreate", Toast.LENGTH_SHORT).show();
        String currentUserName = ConstantUtil.getCurrentUserName(getApplicationContext());


        //如果当前用户为空 重新获取保存目录下的所有用户
        if(currentUserName==null||currentUserName.equals("")){
            //清除数据后在保存目录下加载所以用户
            Set<String> allUserName = getAllUserName();
            if(allUserName!=null&&allUserName.size()>0){
                SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(ConstantUtil.USEUSERNAMES_DIR, 0);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putStringSet(ConstantUtil.USEUSERNAMES, allUserName);
                editor.commit();
               // Toast.makeText(mContext, "重新导入用户名成功!", Toast.LENGTH_SHORT).show();
            }
            //使用默认用户
            ConstantUtil.savaCurrentUser(getApplicationContext(),"admin");
            Log.e("application","CurrentUser1="+currentUserName);
            DIR_NAME ="admin";
        }else {
            DIR_NAME = currentUserName;

        }
        initFresco(this);

        //        Logger.Settings loggerSetting = Logger.init("Geetion.Inc");
        Logger.Settings loggerSetting = Logger.init("YeeSpec.Inc");
        /**
         * Logger 初始化
         */
        if (MBOXConst.IS_DEBUG) {
            loggerSetting.setLogLevel(LogLevel.FULL);
        } else {
            loggerSetting.setLogLevel(LogLevel.NONE);
        }
        readFromAssets(this);
        if(!NetworkUtils.isWifiConnected()){
            return;
        }
        initWifiInfo();

        initVideoCache();
       // initMethod();

       // SharedPreferences sharedPreferences1 = getApplicationContext().getSharedPreferences(ConstantUtil.CURRENT_USER_SHAR_DIR,0);
      //  String currentName1 = sharedPreferences1.getString(ConstantUtil.CURRENT__USER_SHAR, null);


    }
   /* public Context getAppcontext(){
        return getApplicationContext();
    }

  */  //获取保存目录下所有用户名

    public static Set<String> getAllUserName(){
        Set<String> users = new HashSet<>();
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        File[] files = file.listFiles();
        if(files==null){
            return null;
        }
        for (int i = 0; i < files.length; i++) {
            File mfile = files[i];
            if(mfile.isDirectory()){
                users.add(mfile.getName().trim());
            }
        }
        return  users;
    }
    //创建新用户
    public static void creatUser(String userName){
        File picturedir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File newfile = new File(picturedir,userName);

        File videodir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        File videonewfile = new File(videodir,userName);

        if(isExistancCurrentUser(userName)){
            return;//如果该用户已经存在
        }
        //否则创建该用户
        if(!newfile.exists()){
            newfile.mkdirs();
        }
        if(!videonewfile.exists()){
            videonewfile.mkdirs();
        }
    }
    //判断该用户是否已经存在
    public static boolean isExistancCurrentUser(String userName){
        File picturedir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        File[] files = picturedir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File mfile = files[i];
            //如果当前用户已经存在直接
            if(mfile.getName().equals(userName)){
                return true;
            }
        }
        return false;
    }
    public static void initFresco(Context context){
       // FrescoTool.getInstance(context).getFrescoConfig().setIMAGE_PIPELINE_CACHE_DIR("yeespec-ad");

      //  FrescoTool.getInstance(context).init();

    }

    public static void readFromAssets(Context context) {
        if (COLORS.size() == 0) {
            InputStream is = null;
            try {
                is = context.getAssets().open("colors.txt");
                String stringBuff = readTextFromSDcard(is);
                if (DEBUG)
                    Logger.v(TAG, stringBuff);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {

                try { //关闭资源输入输出流 :
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * 按行读取txt
     *
     * @param is
     * @return
     * @throws Exception
     */
    private static String readTextFromSDcard(InputStream is) {
        COLORS.clear();
        InputStreamReader reader = null;
        BufferedReader bufferedReader = null;

        reader = new InputStreamReader(is);
        bufferedReader = new BufferedReader(reader);
        StringBuffer buffer = new StringBuffer("");
        String str;
        int i = 0;
        try {
            while ((str = bufferedReader.readLine()) != null) {
                buffer.append(str);
                buffer.append("\n");
                String[] strings = str.split(",");
                COLORS.add(strings);
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            try { //关闭资源输入输出流 :
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try { //关闭资源输入输出流 :
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return buffer.toString();
    }

    /**
     * OPlayer SD卡缓存路径
     */
    public static final String OPLAYER_CACHE_BASE = Environment.getExternalStorageDirectory() + "/oplayer";
    /**
     * 视频截图缓冲路径
     */
    public static final String OPLAYER_VIDEO_THUMB = OPLAYER_CACHE_BASE + "/thumb/";
    /**
     * 首次扫描
     */
    public static final String PREF_KEY_FIRST = "application_first";

    public static void initVideoCache() {
        //创建缓存目录
        FileUtils.createIfNoExists(OPLAYER_CACHE_BASE);
        FileUtils.createIfNoExists(OPLAYER_VIDEO_THUMB);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    public static void setContext(Activity mContext) {
        BaseApplication.mContext = mContext;
    }

    public static void setContext(Service mContext) {
        BaseApplication.mContext = mContext;
    }

    public static Context getContext() {
        return mContext;
    }
    public  Activity getActivity(){
        return mActivity;
    }
    public  void setActivity(Activity activity){
        mActivity  = activity;
    }
    private static InetAddress mInetAddress;
    private static int int_gateway;
    private static int int_ip;
    private static int int_net_mask;
    private static LanHost mTarget = null;
    private static String gatewayMac;
  //  private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private int wifiConnectCount=3;
   /* private void initMethod(){

        if (!NetworkUtils.isWifiConnected()) {

            if(wifiConnectCount<1){
                Toast.makeText(this, "wifi初始化不成功请重新登录", Toast.LENGTH_SHORT).show();
                return;
            }
            executorService.schedule(new Runnable() {
                @Override
                public void run() {
                    wifiConnectCount--;
                    initMethod();
                }
            },3000,TimeUnit.MILLISECONDS);

        }else {

            initWifiInfo();

            initVideoCache();
        }
    }*/
    public static void initWifiInfo() {
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        int_ip = wifiManager.getDhcpInfo().ipAddress;
        int_net_mask = wifiManager.getDhcpInfo().netmask;
        /**获取不到子网掩码，nexus5实测，偶尔拿不到**/
        if (int_net_mask == 0) {
            int_net_mask = (0 << 24) + (0xff << 16) + (0xff << 8) + 0xff;
        }
        int_gateway = wifiManager.getDhcpInfo().gateway;
        try {
            mInetAddress = InetAddress.getByName(NetworkUtils.netfromInt(int_ip));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        gatewayMac = wifiManager.getConnectionInfo().getBSSID().replace('-', ':');

        //        HttpServer.getInstance();
        //        CustomWebSocketServer.getInstance(6000);
    }

    public static InetAddress getInetAddress() {
        return mInetAddress;
    }

    public static int getIntGateway() {
        return int_gateway;
    }

    public static String getGateway() {
        return NetworkUtils.netfromInt(int_gateway);
    }

    public static String getGatewayMac() {
        return gatewayMac;
    }

    public static int getIntIp() {
        return int_gateway;
    }

    public static String getIp() {
        return NetworkUtils.netfromInt(int_ip);
    }

    public static int getHostCount() {
        return NetworkUtils.countHost(int_net_mask);
    }

    public static int getIntNetMask() {
        return int_net_mask;
    }

    public static LanHost getTarget() {
        return mTarget;
    }

    public static void setTarget(LanHost target) {
        mTarget = target;
    }
}
