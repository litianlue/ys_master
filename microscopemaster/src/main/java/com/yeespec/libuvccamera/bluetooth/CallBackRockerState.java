package com.yeespec.libuvccamera.bluetooth;

/**
 * Created by Administrator on 2017/5/16.
 */
//添加摇杆回调接口
public  interface CallBackRockerState{
    void callBackDirection(String direction);
    void callBackAngle(double angle, float lenXY, float lenX, float lenY);
}

