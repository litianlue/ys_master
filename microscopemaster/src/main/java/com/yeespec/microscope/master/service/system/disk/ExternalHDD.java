package com.yeespec.microscope.master.service.system.disk;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.yeespec.microscope.utils.ConstantUtil;

import java.io.File;

/**
 * For detection and access to external HDD connected via USB OTG
 */
public class ExternalHDD {

    protected static final String TAG = "ExternalHDD";

    private final String storageDir;
    private String externalHddDir;

    private final File externalHddPath;

    private static String STORAGE_DIR = "/storage";

    /**
     * USBPath=file:///mnt/usb_storage/USB_DISK2
     * USBPath=file:///storage/usbcard1
     * ...
     */
    public ExternalHDD(Context context) {
        String dir ="/storage/";
        String hdir ="usbcard1";
        // TODO detect main storage dir
        if(!TextUtils.isEmpty(ConstantUtil.USB_Path)) {
            Log.w("MasterIntentService", "ConstantUtil.USB_Path=" + ConstantUtil.USB_Path);
            String pathstr =ConstantUtil.USB_Path.replace("file://","");
            String mdirs[] = pathstr.split("/");
            hdir = mdirs[mdirs.length-1];
            dir = pathstr.replace(hdir,"");
            String storage_dir =dir.substring(0,dir.length()-1);
            STORAGE_DIR = storage_dir;
        }
        Log.w("MasterIntentService", "dir=" + dir);
        Log.w("MasterIntentService", "hdir=" + hdir);
        Log.w("MasterIntentService", "storage_dir=" + STORAGE_DIR);
        storageDir = dir;//"/storage/";

        // TODO detect external HDD subdirectory
        externalHddDir =hdir; //"usbcard1";

        File file = new File(STORAGE_DIR);

        String[] directories = file.list();
        if (directories != null)
            for (int i = 0; i < directories.length; i++) {
                //  if (directories[i].toLowerCase().contains("usbcard1")) {
                Log.w("MasterIntentService", "directories[i].toLowerCase()=" + directories[i].toLowerCase());
                if (directories[i].toLowerCase().equals(hdir)) {
                    //   new File(STORAGE_DIR, directories[i]);
                    externalHddDir = directories[i];
                    Log.w(TAG, externalHddDir);
                } else if (directories[i].toLowerCase().equals("usbdisk")) {
                    externalHddDir = directories[i];
                }
            }
        //        Log.w(TAG, directories.toString());
        //        externalHddDir = "usbdisk";

        externalHddPath = new File(storageDir, externalHddDir);
        Log.w("MasterIntentService", "externalHddPath=" + externalHddPath.getPath());
        Log.w("MasterIntentService", "externalHddPath=" + externalHddPath.getAbsolutePath());
    }

    /**
     * Access to external HDD connected to device
     *
     * @return File pointing to HDD
     */
    public File getUSBCardPath() {
        externalHddPath.mkdir();
        return externalHddPath;
    }

    public File getUSBAppPath(String externalHddDir) {
        File file = null;
        if(ConstantUtil.USB_Path.indexOf("mnt")!=-1){
            file = new File(externalHddPath.getPath()+"/udisk0", "USBCamera-" + externalHddDir);
        }else
         file = new File(externalHddPath.getPath(), "USBCamera-" + externalHddDir);
        file.mkdir();
        return file;
    }
}
