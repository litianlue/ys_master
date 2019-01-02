package com.yeespec.microscope.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.yeespec.microscope.master.application.BaseApplication;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class NetworkUtils {

 //   private static Map<String, String> mVendors = null;

    /**
     * 计算子网主机数目
     *
     * @param int_net_mask
     * @return
     */
    public static int countHost(int int_net_mask) {
        int n = ~int_net_mask;
        return ((n >> 24) & 0xff) + (((n >> 16) & 0xff) << 8)
                + (((n >> 8) & 0xff) << 16) + ((n & 0xff) << 24);
    }

    /**
     * 根据Int类型IP返回String类型IP
     *
     * @param int_ip
     * @return
     */
    public static String netfromInt(int int_ip) {
        return new StringBuilder().append((int_ip) & 0xff).append('.')
                .append((int_ip >> 8) & 0xff).append('.')
                .append((int_ip >> 16) & 0xff).append('.')
                .append((int_ip >> 24) & 0xff).toString();
    }

    /**
     * 查找下一个IP
     *
     * @param int_ip
     * @return
     */
   /* public static int nextIntIp(int int_ip) {
        int next_ip = -1;
        byte[] ip_byte = intIpToByte(int_ip);
        int i = ip_byte.length - 1;

        while (i >= 0 && ip_byte[i] == (byte) 0xff) {
            ip_byte[i] = 0;
            i--;
        }
        if (i >= 0)
            ip_byte[i]++;
        else
            return next_ip;

        next_ip = byteIpToInt(ip_byte);

        return next_ip;
    }
*/
    /**
     * Int类型IP转成byte数组
     *
     * @param int_ip
     * @return
     */
    /*public static byte[] intIpToByte(int int_ip) {
        byte[] ip_byte = new byte[4];

        ip_byte[0] = (byte) (int_ip & 0xff);
        ip_byte[1] = (byte) (0xff & int_ip >> 8);
        ip_byte[2] = (byte) (0xff & int_ip >> 16);
        ip_byte[3] = (byte) (0xff & int_ip >> 24);

        return ip_byte;
    }
*/


    public static boolean isWifiConnected() {
        ConnectivityManager manager = (ConnectivityManager) BaseApplication
                .getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return info != null && info.isConnected() && info.isAvailable();
    }

}
