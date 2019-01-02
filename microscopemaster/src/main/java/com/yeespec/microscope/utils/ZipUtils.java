package com.yeespec.microscope.utils;

import com.yeespec.microscope.utils.log.Logger;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Beary on 15/12/8.
 */
public class ZipUtils {

    protected static final boolean DEBUG = false;

    private ZipUtils() {
        throw new IllegalStateException("不用初始化的啊，直接静态调用");
    }

    /**
     * 创建ZIP文件
     *
     * @param sourcePath 文件或文件夹路径
     * @param zipPath    生成的zip文件存在路径（包括文件名）
     */
    public static void createZip(String sourcePath, String zipPath) {
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            fos = new FileOutputStream(zipPath);
            zos = new ZipOutputStream(fos);
            writeZip(new File(sourcePath), "", zos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            if (DEBUG)
                Logger.e("创建ZIP文件失败  " + e);
        } finally {
            try {
                if (zos != null) {
                    zos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (DEBUG)
                    Logger.e("创建ZIP文件失败  " + e);
            } finally {
                try {
                    //关闭资源输入输出流 :
                    if (fos != null) {
                        fos.close();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {

                }
            }

        }
    }

    private static void writeZip(File file, String parentPath, ZipOutputStream zos) {
        if (file.exists()) {
            if (file.isDirectory()) {//处理文件夹
                parentPath += file.getName() + File.separator;
                File[] files = file.listFiles();
                for (File f : files) {
                    writeZip(f, parentPath, zos);
                }
            } else {
                FileInputStream fis = null;
                DataInputStream dis = null;
                try {
                    fis = new FileInputStream(file);
                    dis = new DataInputStream(new BufferedInputStream(fis));
                    ZipEntry ze = new ZipEntry(parentPath + file.getName());
                    zos.putNextEntry(ze);
                    byte[] content = new byte[1024];
                    int len;
                    while ((len = fis.read(content)) != -1) {
                        zos.write(content, 0, len);
                        zos.flush();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    if (DEBUG)
                        Logger.e("创建ZIP文件失败  " + e);
                } catch (IOException e) {
                    e.printStackTrace();
                    if (DEBUG)
                        Logger.e("创建ZIP文件失败  " + e);
                } finally {
                    try {
                        if (dis != null) {
                            dis.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (DEBUG)
                            Logger.e("创建ZIP文件失败  " + e);
                    } finally {
                        try {
                            //关闭资源输入输出流 :
                            if (fis != null) {
                                fis.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}