package com.yeespec.microscope.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by virgilyan on 15/12/4.
 */
public class ByteZip {

    /***
     * 压缩GZip
     *
     * @param data
     * @return
     */
    public static byte[] gZip(byte[] data) {
        byte[] b = null;
        ByteArrayOutputStream bos = null;
        GZIPOutputStream gzip = null;
        try {
            bos = new ByteArrayOutputStream();
            gzip = new GZIPOutputStream(bos);
            gzip.write(data);
            gzip.finish();
            gzip.close();
            b = bos.toByteArray();
            bos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                //关闭资源输入输出流 :
                if (gzip != null) {
                    gzip.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    //关闭资源输入输出流 :
                    if (bos != null) {
                        bos.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {

                }
            }
        }
        return b;
    }

    /***
     * 解压GZip
     *
     * @param data
     * @return
     */
    public static byte[] unGZip(byte[] data) {
        byte[] b = null;
        ByteArrayInputStream bis = null;
        GZIPInputStream gzip = null;
        ByteArrayOutputStream baos = null;
        try {
            byte[] buf = new byte[1024];
            bis = new ByteArrayInputStream(data);
            gzip = new GZIPInputStream(bis);
            baos = new ByteArrayOutputStream();
            int num = -1;
            while ((num = gzip.read(buf, 0, buf.length)) != -1) {
                baos.write(buf, 0, num);
            }
            b = baos.toByteArray();
            baos.flush();
            baos.close();
            gzip.close();
            bis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                //关闭资源输入输出流 :
                if (baos != null) {
                    baos.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    //关闭资源输入输出流 :
                    if (gzip != null) {
                        gzip.close();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    try {
                        //关闭资源输入输出流 :
                        if (bis != null) {
                            bis.close();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        return b;
    }

    /***
     * 压缩Zip
     *
     * @param data
     * @return
     */
    public static byte[] zip(byte[] data) {
        byte[] b = null;
        ByteArrayOutputStream bos = null;
        ZipOutputStream zip = null;
        try {
            bos = new ByteArrayOutputStream();
            zip = new ZipOutputStream(bos);
            ZipEntry entry = new ZipEntry("zip");
            entry.setSize(data.length);
            zip.putNextEntry(entry);
            zip.write(data);
            zip.closeEntry();
            zip.close();
            b = bos.toByteArray();
            bos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                //关闭资源输入输出流 :
                if (zip != null) {
                    zip.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    //关闭资源输入输出流 :
                    if (bos != null) {
                        bos.close();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }finally {

                }
            }
        }
        return b;
    }

    /***
     * 解压Zip
     *
     * @param data
     * @return
     */
    public static byte[] unZip(byte[] data) {
        byte[] b = null;
        ByteArrayInputStream bis = null;
        ZipInputStream zip = null;
        ByteArrayOutputStream baos = null;
        try {
            bis = new ByteArrayInputStream(data);
            zip = new ZipInputStream(bis);
            while (zip.getNextEntry() != null) {
                byte[] buf = new byte[1024];
                int num = -1;
                baos = new ByteArrayOutputStream();
                while ((num = zip.read(buf, 0, buf.length)) != -1) {
                    baos.write(buf, 0, num);
                }
                b = baos.toByteArray();
                baos.flush();
                baos.close();
            }
            zip.close();
            bis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                //关闭资源输入输出流 :
                if (baos != null) {
                    baos.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    //关闭资源输入输出流 :
                    if (zip != null) {
                        zip.close();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    try {
                        //关闭资源输入输出流 :
                        if (bis != null) {
                            bis.close();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {

                    }
                }
            }
        }
        return b;
    }

    /**
     * 把字节数组转换成16进制字符串
     *
     * @param bArray
     * @return
     */
    public static String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        String s = "this is a test";

        byte[] b1 = zip(s.getBytes());
        System.out.println("zip:" + bytesToHexString(b1));
        byte[] b2 = unZip(b1);
        System.out.println("unZip:" + new String(b2));
        byte[] b5 = gZip(s.getBytes());
        System.out.println("bZip2:" + bytesToHexString(b5));
        byte[] b6 = unGZip(b5);
        System.out.println("unBZip2:" + new String(b6));
    }
}
