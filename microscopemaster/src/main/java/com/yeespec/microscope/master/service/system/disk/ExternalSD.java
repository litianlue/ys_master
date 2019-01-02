package com.yeespec.microscope.master.service.system.disk;

import android.content.Context;
import android.content.SharedPreferences;

import com.yeespec.microscope.master.application.BaseApplication;
import com.yeespec.microscope.utils.ConstantUtil;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * For detection and access
 */
public class ExternalSD {

    private static final String TAG = ExternalSD.class.getSimpleName();

    private String[] directories;
    private static String DIR_NAME;
    // Potential list of SD dirs
    private static final List<String> SD_DIRS = Collections.unmodifiableList(Arrays.asList(
            "external_SD",
            "Dog in the fog, don't bother" // this one is incorrect OFC
    ));

    //    private static final String STORAGE_DIR = "/storage";
    private static final String STORAGE_DIR = "/storage/sdcard1";   //2016.12.30 修改

    public ExternalSD(Context context) throws IllegalStateException {

        // find dir

        File file = new File(STORAGE_DIR);

        directories = file.list();

        //        String[] directories = file.list(new FilenameFilter() {
        //            @Override
        //            public boolean accept(File current, String name) {
        //                boolean isDirectory = new File(current, name).isDirectory();
        //                return isDirectory && SD_DIRS.contains(name);
        //            }
        //        });

        if (directories == null || directories.length == 0) {
            //            throw new IllegalStateException("Missing external SD card");
            //            throw new RuntimeException("Missing external SD card");
        }

        //        for (int i = 0; i < directories.length; i++) {
        //            Log.e(TAG, directories[i]);
        //        }
    }

    public File getDir() {
        if (directories != null)
            for (int i = 0; i < directories.length; i++) {
                if (directories[i].toLowerCase().equals("sdcard0"))
                    return new File(STORAGE_DIR, directories[i]);
            }
        return null;
    }

    public File getSDCardDir() {
        if (directories != null)
            for (int i = 0; i < directories.length; i++) {
                if (directories[i].toLowerCase().equals("sdcard1")) {
                    return new File(STORAGE_DIR, directories[i]);
                }
            }
        return null;
    }

    public File getSDCardAppDir(String text) {
        SharedPreferences sharedPreferences =BaseApplication.getContext().getSharedPreferences(ConstantUtil.CURRENT_USER_SHAR_DIR,0);
        String currentName = sharedPreferences.getString(ConstantUtil.CURRENT__USER_SHAR, null);
        DIR_NAME = currentName;
        if (directories != null)
            for (int i = 0; i < directories.length; i++) {
                try {
                    if (directories[i].toLowerCase().equals("sdcard1")) {
                        File file = new File(STORAGE_DIR, directories[i] + "/Android/data/com.yeespec.microscope.master/" + DIR_NAME);
                        if (!file.exists())
                            file.mkdir();
                        file = new File(file, text);
                        file.mkdir();
                        return file;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        return null;
    }
}
