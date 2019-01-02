package com.yeespec.microscope.utils.bluetooth;

/**
 * Created by Administrator on 2017/12/5.
 */

public class DataUtil {
    public  static  boolean isConnectBlue = false;
    public  static  String CONTRASTKEY= "contrastkey";
    // 方案1 先对一个对照组进行一轮拍照再进入下一个对照组拍照
    //方案2 先对所有的对照组在同一个激发快下拍完再进入下一个激发快拍照
    public static  int CONTRASTPLAN =1;//对照组拍照方案一
    public static  boolean isStartContract=false;//是否启动对照组

    public static   int stateI = 0;//电机一当前位置  X轴
    public static   int stateII =0;//电机二当前位置  Y轴

    public static  int  CONTRACKCOUNT =10;//设置对照组总数
    public static  int [][] moveSate = new int[CONTRACKCOUNT][2];//记录对照组移动坐标
    public static  boolean   []CheckNums = new boolean[CONTRACKCOUNT];//记录已选择的对照组
    public static  int[][] getContracts(){
        int leng=0;
        for (int i = 0; i < CheckNums.length; i++) {
            if(CheckNums[i]==true)
                leng++;
        }
        if(leng==0)
            return null;
        int contacts[][] = new int[leng][2];
        int number=0;
        for (int i = 0; i < CheckNums.length; i++) {
            if(CheckNums[i]==true){
                contacts[number][0] = moveSate[i][0];
                contacts[number][1] = moveSate[i][1];
                number++;
            }

        }
        return contacts;
    }
}
