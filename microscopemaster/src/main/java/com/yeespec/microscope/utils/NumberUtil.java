/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.yeespec.microscope.utils;

/**
 *
 * @author panzhen
 */
public class NumberUtil {

   /* public static byte[] byte2byte(byte d) {
        byte[] t = new byte[1];
        return byte2byte(d, t, 0);
    }*/

   /* public static byte[] byte2byte(byte d, byte[] t, int position) {
        if (position + 1 > t.length) {
            throw new RuntimeException("Array length < position + 1 for to byte value.");
        }
        t[position] = (byte) (0xFF & d);
        return t;
    }

    public static byte byte2byte(byte[] d) {
        return byte2byte(d, 0);
    }*/

   /* public static byte byte2byte(byte[] d, int position) {
        if (position + 1 > d.length) {
            throw new RuntimeException("Array length < position + 1 for to byte value.");
        }
        return (byte)(0xFF & d[position]);
    }

    public static byte[] int2byte(int d) {
        byte[] t = new byte[4];
        return int2byte(d, t, 0);
    }*/

  /*  public static byte[] int2byte(long d, byte[] t, int position) {
        if (position + 4 > t.length) {
            throw new RuntimeException("Array length < position + 4 for to int value.");
        }
        t[position + 0] = (byte) (0xFF & (d >> 24));
        t[position + 1] = (byte) (0xFF & (d >> 16));
        t[position + 2] = (byte) (0xFF & (d >> 8));
        t[position + 3] = (byte) (0xFF & d);
        return t;
    }
*/
   /* public static int byte2int(byte[] d) {
        return byte2int(d, 0);
    }*/

    /* public static int byte2int(byte[] d, int position) {
         if (position + 1 > d.length) {
             throw new RuntimeException("Array length < position + 4  for to int value.");
         }
         return (((0xFF & (int) d[position]) << 24) | ((0xFF & (int) d[position + 1]) << 16) | ((0xFF & (int) d[position + 2]) << 8)
                 | ((0xFF & (int) d[position + 3])));
     }
 */
    public static byte[] long2byte(long d, byte[] t, int position) {
        if (position + 8 > t.length) {
            throw new RuntimeException("Array length < position + 8");
        }
        t[position] = (byte) (0xFF & (d >> 56));
        t[position + 1] = (byte) (0xFF & (d >> 48));
        t[position + 2] = (byte) (0xFF & (d >> 40));
        t[position + 3] = (byte) (0xFF & (d >> 32));
        t[position + 4] = (byte) (0xFF & (d >> 24));
        t[position + 5] = (byte) (0xFF & (d >> 16));
        t[position + 6] = (byte) (0xFF & (d >> 8));
        t[position + 7] = (byte) (0xFF & d);
        return t;
    }

    public static byte[] long2byte(long d) {
        byte[] t = new byte[8];
        return long2byte(d, t, 0);
    }

    public static long byte2long(byte[] d) {
        return byte2long(d, 0);
    }

    public static long byte2long(byte[] d, int position) {
        if (position + 8 > d.length) {
            throw new RuntimeException("Array length < position + 8");
        }
        return (((0xFF & (long) d[position]) << 56) | ((0xFF & (long) d[position + 1]) << 48) | ((0xFF & (long) d[position + 2]) << 40)
                | ((0xFF & (long) d[position + 3]) << 32) | ((0xFF & (long) d[position + 4]) << 24) | ((0xFF & (long) d[position + 5]) << 16)
                | ((0xFF & (long) d[position + 6]) << 8) | (0xFF & (long) d[position + 7]));
    }

    public static void main(String[] args) {
        byte[] b = long2byte(1234567890123456789L);
        for (int i = 0; i < b.length; i++) {
            System.out.println(i + ":" + (0xFF & b[i]));
        }

        System.out.println(byte2long(b));
    }
}
