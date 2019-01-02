package com.yeespec.microscope.master.service.system.disk;

import android.util.Log;

import com.yeespec.BuildConfig;
import com.yeespec.microscope.utils.log.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 */
public class FileUtils {

    protected static final String TAG = "FileUtils";
    protected static final boolean DEBUG = false;

    private FileUtils() {
        throw new IllegalAccessError("Utils class");
    }

  /*  public static void copyDirectoryOneLocationToAnotherLocation(File sourceLocation, File targetLocation) {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                try {
                    if (sourceLocation.isDirectory()) {
                        if (!targetLocation.mkdir()) {
                            throw new IOException("Unable to create target dir " + targetLocation.getAbsolutePath());
                        }
                    } else {
                        if (!targetLocation.createNewFile()) {
                            throw new IOException("Unable to create new file " + targetLocation.getAbsolutePath());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            String[] children = sourceLocation.list();
            for (int i = 0; i < sourceLocation.listFiles().length; i++) {
                copyDirectoryOneLocationToAnotherLocation(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
            }
        } else {

            InputStream in = null;
            OutputStream out = null;
            try {
                in = new FileInputStream(sourceLocation);
                if (!targetLocation.exists()) {
                    targetLocation.mkdirs();
                    targetLocation.createNewFile();
                }
                out = new FileOutputStream(targetLocation);

                if (BuildConfig.DEBUG) {
                    System.out.println("'" + sourceLocation.getAbsoluteFile() + "' COPY TO '" + targetLocation.getAbsolutePath() + "'");
                }

                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {//关闭资源输入输出流 :
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {//关闭资源输入输出流 :
                        if (in != null) {
                            in.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
*/
    public static int copy(String fromFile, String toFile) {
        //要复制的文件目录
        File[] currentFiles;
        File root = new File(fromFile);
        //如同判断SD卡是否存在或者文件是否存在
        //如果不存在则 return出去
        if (!root.exists()) {
            return -1;
        }
        //如果存在则获取当前目录下的全部文件 填充数组
        currentFiles = root.listFiles();

        //目标目录
        File targetDir = new File(toFile);
        //创建目录
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        //遍历要复制该目录下的全部文件
        for (int i = 0; i < currentFiles.length; i++) {
            if (currentFiles[i].isDirectory())//如果当前项为子目录 进行递归
            {
                copy(currentFiles[i].getPath() + "/", toFile + currentFiles[i].getName() + "/");

            } else//如果当前项为文件则进行文件拷贝
            {
                CopySdcardFile(currentFiles[i].getPath(), toFile + currentFiles[i].getName());
            }
        }
        return 0;
    }

    //文件拷贝
    //要复制的目录下的所有非子目录(文件夹)文件拷贝
    public static int CopySdcardFile(String fromFile, String toFile) {
        InputStream fosfrom = null;
        File targetLocation = null;
        File targetDir = null;
        OutputStream fosto = null;

        try {
            fosfrom = new FileInputStream(fromFile);
            targetLocation = new File(toFile);
            targetDir = new File(targetLocation.getParent());
            Log.w(TAG, "fromFile = " + fromFile + "\ntoFile = " + toFile + "\ntoFile.getParent = " + targetLocation.getParent());
            if (!targetDir.exists()) {
                targetDir.mkdirs();
                targetDir.createNewFile();
            }
            //  targetLocation.createNewFile();
            fosto = new FileOutputStream(toFile);
            byte bt[] = new byte[1024];
            int c;
            while ((c = fosfrom.read(bt)) > 0) {
                fosto.write(bt, 0, c);
            }
            fosto.flush();
            fosfrom.close();
            fosto.close();
            return 0;

        } catch (Exception ex) {
            ex.printStackTrace();
            if (DEBUG)
                Logger.e(ex.getLocalizedMessage());
            return -1;
        } finally {
            try {
                if (fosto != null) {
                    fosto.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    if (fosfrom != null) {
                        fosfrom.close();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

   /* public static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
*/
    /**
     * Calculates file size or directory with all files contained recursively
     *
     * @param file or dir for size calculation
     * @return bytes of data
     */
    public static long size(File file) {
        return size(file, 0);
    }

    /**
     * Calculates file size or directory with all files contained recursively
     *
     * @param file        or dir for size calculation
     * @param initialSize for internal use because of recurrence
     * @return bytes of data
     */
    private static long size(File file, long initialSize) {

        long result = initialSize;

        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                for (String child : file.list()) {
                    result = result + size(new File(file, child));
                }
            } else if (file.isFile()) {
                result = result + file.length();
            }
        }

        return result;
    }
}
