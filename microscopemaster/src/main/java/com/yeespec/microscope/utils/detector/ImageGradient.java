package com.yeespec.microscope.utils.detector;

import android.graphics.Bitmap;


/**
 * Created by Administrator on 2017/8/21.
 */

public class ImageGradient {


    public Bitmap RobertGradient(Bitmap myBitmap){
        // Create new array
        int width = myBitmap.getWidth();
        int height = myBitmap.getHeight();
        int[] pix = new int[width * height];
        myBitmap.getPixels(pix, 0, width, 0, 0, width, height);
        Matrix dataR=getDataR(pix, width, height);
        Matrix dataG=getDataG(pix, width, height);
        Matrix dataB=getDataB(pix, width, height);
        //Matrix dataGray=getDataGray(pix, width, height);
        /////////////////////////////////////////////////////////
        dataR=eachRobertGradient(dataR,width,height);
        dataG=eachRobertGradient(dataG,width,height);
        dataB=eachRobertGradient(dataB,width,height);
        ///////////////////////////////////////////////////////////////
        // Change bitmap to use new array
        Bitmap bitmap=makeToBitmap(dataR, dataG, dataB, width, height);
        myBitmap = null;
        pix = null;
        return bitmap;
    }
    private Matrix eachRobertGradient(Matrix tempM,int width,int height){
        int i,j;
        for(i=0;i<width-1;i++){
            for(j=0;j<height-1;j++){
                int temp= Math.abs((int)tempM.get(i, j)-(int)tempM.get(i,j+1))
                        + Math.abs((int)tempM.get(i+1,j)-(int)tempM.get(i,j+1));
                tempM.set(i, j, temp);
            }
        }
        return tempM;
    }
    /*
     *Sobel算子锐化
     */
    public Bitmap SobelGradient(Bitmap myBitmap){
        // Create new array
        int width = myBitmap.getWidth();
        int height = myBitmap.getHeight();
        int[] pix = new int[width * height];
        myBitmap.getPixels(pix, 0, width, 0, 0, width, height);
        Matrix dataR=getDataR(pix, width, height);
        Matrix dataG=getDataG(pix, width, height);
        Matrix dataB=getDataB(pix, width, height);
        Matrix dataGray=getDataGray(pix, width, height);
        /////////////////////////////////////////////////////////
        dataGray=eachSobelGradient(dataGray, width, height);
        dataR=dataGray.copy();
        dataG=dataGray.copy();
        dataB=dataGray.copy();
        ///////////////////////////////////////////////////////////////
        // Change bitmap to use new array
        Bitmap bitmap=makeToBitmap(dataR, dataG, dataB, width, height);
        myBitmap = null;
        pix = null;
        return bitmap;
    }
    private Matrix eachSobelGradient(Matrix tempM,int width,int height){
        int i,j;
        Matrix resultMatrix=tempM.copy();
        for(i=1;i<width-1;i++){
            for(j=1;j<height-1;j++){
                int temp1= Math.abs(((int)tempM.get(i+1, j-1)+2*(int)tempM.get(i+1, j)+(int)tempM.get(i+1,j+1))
                        -(((int)tempM.get(i-1,j-1)+2*(int)tempM.get(i-1,j)+(int)tempM.get(i-1,j-1))));
                int temp2= Math.abs(((int)tempM.get(i-1, j+1)+2*(int)tempM.get(i, j+1)+(int)tempM.get(i+1,j+1))
                        -(((int)tempM.get(i-1,j-1)+2*(int)tempM.get(i,j-1)+(int)tempM.get(i+1,j-1))));
                int temp=temp1+temp2;
                resultMatrix.set(i, j, temp);
            }
        }
        return resultMatrix;
    }
    /*
     *Laplace 锐化
     */
    public Bitmap LaplaceGradient(Bitmap myBitmap){
        // Create new array
        int width = myBitmap.getWidth();
        int height = myBitmap.getHeight();
        int[] pix = new int[width * height];
        myBitmap.getPixels(pix, 0, width, 0, 0, width, height);
        Matrix dataR=getDataR(pix, width, height);
        Matrix dataG=getDataG(pix, width, height);
        Matrix dataB=getDataB(pix, width, height);
        Matrix dataGray=getDataGray(pix, width, height);
        /////////////////////////////////////////////////////////
        dataGray=eachLaplaceGradient(dataGray,width,height);
        dataR=dataGray.copy();
        dataG=dataGray.copy();
        dataB=dataGray.copy();
        ///////////////////////////////////////////////////////////////
        // Change bitmap to use new array
        Bitmap bitmap=makeToBitmap(dataR, dataG, dataB, width, height);
        myBitmap = null;
        pix = null;
        return bitmap;
    }
    private Matrix eachLaplaceGradient(Matrix tempM,int width,int height){
        int i,j;
        Matrix resultMatrix=tempM.copy();
        for(i=1;i<width-1;i++){
            for(j=1;j<height-1;j++){
                int temp= Math.abs(5*(int)tempM.get(i, j)-(int)tempM.get(i+1,j)
                        -(int)tempM.get(i-1,j)-(int)tempM.get(i,j+1)-(int)tempM.get(i,j-1));
                resultMatrix.set(i, j, temp);
            }
        }
        return resultMatrix;
    }

    private Matrix getDataR(int[] pix,int width,int height){
        Matrix dataR=new Matrix(width,height,0.0);
        // Apply pixel-by-pixel change
        int index = 0;
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                int r = ((pix[index] >> 16) & 0xff);
                dataR.set(x, y, r);
                index++;
            } // x
        } // y
        return dataR;
    }
    private Matrix getDataG(int[] pix,int width,int height){
        Matrix dataG=new Matrix(width,height,0.0);
        // Apply pixel-by-pixel change
        int index = 0;
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                int g = ((pix[index] >> 8) & 0xff);
                dataG.set(x, y, g);
                index++;
            } // x
        } // y
        return dataG;
    }
    private Matrix getDataB(int[] pix,int width,int height){
        Matrix dataB=new Matrix(width,height,0.0);
        // Apply pixel-by-pixel change
        int index = 0;
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                int b =(pix[index] & 0xff);
                dataB.set(x, y, b);
                index++;
            } // x
        } // y
        return dataB;
    }

    private Matrix getDataGray(int[] pix,int width,int height){
        Matrix dataGray=new Matrix(width,height,0.0);
        // Apply pixel-by-pixel change
        int index = 0;
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                int r = ((pix[index] >> 16) & 0xff);
                int g = ((pix[index] >> 8) & 0xff);
                int b = (pix[index] & 0xff);
                int gray=(int)(0.3*r+0.59*g+0.11*b);
                dataGray.set(x, y, gray);
                index++;
            } // x
        } // y
        return dataGray;
    }
    private Bitmap makeToBitmap(Matrix dataR, Matrix dataG, Matrix dataB, int width, int height) {

        int[] pix = new int[width * height];
        int index=0;
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                pix[index] = 0xff000000 | ((int)dataR.get(x, y) << 16) | ((int)dataG.get(x, y) << 8) | (int)dataB.get(x, y);
                index++;
            } // x
        } // y
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap.setPixels(pix, 0, width, 0, 0, width, height);
        pix = null;
        return bitmap;
    }
    public Bitmap brighten(int brightenOffset, Bitmap myBitmap)
    {
        // Create new array
        int width = myBitmap.getWidth();
        int height = myBitmap.getHeight();
        int[] pix = new int[width * height];
        myBitmap.getPixels(pix, 0, width, 0, 0, width, height);

        // Apply pixel-by-pixel change
        int index = 0;
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                int r = (pix[index] >> 16) & 0xff;
                int g = (pix[index] >> 8) & 0xff;
                int b = pix[index] & 0xff;
                r = Math.max(0, Math.min(255, r + brightenOffset));
                g = Math.max(0, Math.min(255, g + brightenOffset));
                b = Math.max(0, Math.min(255, b + brightenOffset));
                pix[index] = 0xff000000 | (r << 16) | (g << 8) | b;
                index++;
            } // x
        } // y

        // Change bitmap to use new array
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap.setPixels(pix, 0, width, 0, 0, width, height);
        myBitmap = null;
        pix = null;
        return bitmap;
    }


}
