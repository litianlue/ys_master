package com.yeespec.microscope.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.yeespec.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.yeespec.microscope.utils.PropertiesUtils.getConfig;

/**
 * Created by Mr.Wen on 2017/1/12.
 */

public class SystemConfigUtil {

    private final static String TAG = "PropertiesUtils";

    private final static String FIRMWARE = "firmware";  //物镜的配置
    private final static String LENS = "lens";  //物镜的配置
    private final static String LENS_01 = "lens_01";
    private final static String LENS_02 = "lens_02";
    private final static String LENS_03 = "lens_03";
    private final static String LENS_01_PLANE = "lens_01_plane";   //物镜的对焦面
    private final static String LENS_02_PLANE = "lens_02_plane";
    private final static String LENS_03_PLANE = "lens_03_plane";
    private final static String LASER = "laser";  //激发光的配置
    private final static String LASER_01 = "laser_01";      //激发光的rgb颜色值
    private final static String LASER_02 = "laser_02";
    private final static String LASER_03 = "laser_03";
    private final static String LASER_04 = "laser_04";
    private final static String LASER_05 = "laser_05";
    private final static String LASER_06 = "laser_06";

    private static Dialog mSystemConfigDialog;

    private static Button settingButton;
    private static Button cancelButton;

    private static CheckBox versionCkeckBox;
    private static CheckBox lensCkeckBox01;
    private static CheckBox lensCkeckBox02;
    private static CheckBox laserCkeckBox01;
    private static CheckBox laserCkeckBox02;
    private static CheckBox laserCkeckBox03;
    private static CheckBox laserCkeckBox04;
    private static CheckBox laserCkeckBox05;
    private static CheckBox laserCkeckBox06;

    private static EditText lensEditText01;
    private static EditText lensEditText02;
    private static EditText plansEditText01;
    private static EditText plansEditText02;

    private static EditText lasersSelectEditText;

    private static int firmware = 0;     //保存获取到的硬件固件版本配置

    private static List<String> lenses = new ArrayList<>();     //保存获取到的物镜配置
    private static List<String> lasers = new ArrayList<>();     //保存获取到的激发光配置

    private static List<String> lens_01_plane = new ArrayList<>();     //保存获取到的焦面配置
    private static List<String> lens_02_plane = new ArrayList<>();
    private static List<String> lens_03_plane = new ArrayList<>();

    private static List<String> laser_01 = new ArrayList<>();     //保存获取到的激发光rgb颜色值配置
    private static List<String> laser_02 = new ArrayList<>();
    private static List<String> laser_03 = new ArrayList<>();
    private static List<String> laser_04 = new ArrayList<>();
    private static List<String> laser_05 = new ArrayList<>();

    //焦平面数据 :
    public static List<Integer> lens01PlaneValues = new ArrayList<>();
    public static List<Integer> lens02PlaneValues = new ArrayList<>();
    public static List<Integer> lens03PlaneValues = new ArrayList<>();

    //物镜倍数 :
    public static int lens01Value;
    public static int lens02Value;

    public static Context mContext;

    //激发光数据 :
    //    public static List<String> laserSelectList = new ArrayList<>();

    private static String listToFormatString(List list, String splitString) {
        String formatString = "";
        if (list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                formatString += list.get(i).toString().trim();
                if (i != (list.size() - 1))
                    formatString += splitString;
            }
        }
        return formatString;
    }

    //显示系统配置弹窗 :
    public static void showSystemConfigDialog(Activity activity) {
        mContext = activity;
        if (mSystemConfigDialog != null) {
            mSystemConfigDialog.dismiss();
            mSystemConfigDialog = null;
        }
        // 初始化对话框
        mSystemConfigDialog = new Dialog(activity, R.style.Dialog_Radio);
        mSystemConfigDialog.setContentView(R.layout.dialog_system_config_toast);
        mSystemConfigDialog.setCancelable(true);
        mSystemConfigDialog.setCanceledOnTouchOutside(true);

        settingButton = (Button) mSystemConfigDialog.findViewById(R.id.setting_btn);
        cancelButton = (Button) mSystemConfigDialog.findViewById(R.id.cancel_btn);

        versionCkeckBox = (CheckBox) mSystemConfigDialog.findViewById(R.id.check_version);
        lensCkeckBox01 = (CheckBox) mSystemConfigDialog.findViewById(R.id.check_lens_01);
        lensCkeckBox02 = (CheckBox) mSystemConfigDialog.findViewById(R.id.check_lens_02);
        laserCkeckBox01 = (CheckBox) mSystemConfigDialog.findViewById(R.id.check_laser_01);
        laserCkeckBox02 = (CheckBox) mSystemConfigDialog.findViewById(R.id.check_laser_02);
        laserCkeckBox03 = (CheckBox) mSystemConfigDialog.findViewById(R.id.check_laser_03);
        laserCkeckBox04 = (CheckBox) mSystemConfigDialog.findViewById(R.id.check_laser_04);
        laserCkeckBox05 = (CheckBox) mSystemConfigDialog.findViewById(R.id.check_laser_05);
        laserCkeckBox06 = (CheckBox) mSystemConfigDialog.findViewById(R.id.check_laser_06);

        lensEditText01 = (EditText) mSystemConfigDialog.findViewById(R.id.et_lens_01);
        lensEditText02 = (EditText) mSystemConfigDialog.findViewById(R.id.et_lens_02);
        plansEditText01 = (EditText) mSystemConfigDialog.findViewById(R.id.et_plans_01);
        plansEditText02 = (EditText) mSystemConfigDialog.findViewById(R.id.et_plans_02);

        lasersSelectEditText = (EditText) mSystemConfigDialog.findViewById(R.id.et_lasers_select);

        //初始化系统参数设置 :
        initSystemValue();

        lensEditText01.setText(String.valueOf(lens01Value));
        lensEditText02.setText(String.valueOf(lens02Value));

        plansEditText01.setText(listToFormatString(lens_01_plane, "-").trim());
        plansEditText02.setText(listToFormatString(lens_02_plane, "-").trim());

        //版本设置 :
        if (firmware != 0)
            versionCkeckBox.setChecked(true);
        else
            versionCkeckBox.setChecked(false);

        //物镜设置 :
        lensCkeckBox01.setChecked(false);
        lensCkeckBox02.setChecked(false);

        if (lenses.size() > 0) {
            for (int i = 0; i < lenses.size(); i++) {
                String tempString = lenses.get(i).trim();
                Log.w(TAG, "lenses tempString === " + tempString);
                if (LENS_01.compareTo(tempString) == 0) {
                    lensCkeckBox01.setChecked(true);
                } else if (LENS_02.compareTo(tempString) == 0) {
                    lensCkeckBox02.setChecked(true);
                }
            }
        }

        //激发光设置 :
        laserCkeckBox01.setChecked(false);
        laserCkeckBox02.setChecked(false);
        laserCkeckBox03.setChecked(false);
        laserCkeckBox04.setChecked(false);
        laserCkeckBox05.setChecked(false);
        laserCkeckBox06.setChecked(false);

        if (lasers.size() > 0) {
            for (int i = 0; i < lasers.size(); i++) {
                String tempString = lasers.get(i).trim();
                Log.w(TAG, "lasers tempString === " + tempString);
                if (LASER_01.compareTo(tempString) == 0) {
                    laserCkeckBox01.setChecked(true);
                } else if (LASER_02.compareTo(tempString) == 0) {
                    laserCkeckBox02.setChecked(true);
                } else if (LASER_03.compareTo(tempString) == 0) {
                    laserCkeckBox03.setChecked(true);
                } else if (LASER_04.compareTo(tempString) == 0) {
                    laserCkeckBox04.setChecked(true);
                } else if (LASER_05.compareTo(tempString) == 0) {
                    laserCkeckBox05.setChecked(true);
                }
            }
        }

        lasersSelectEditText.setText(listToFormatString(lasers, ","));

        //==================================================

        versionCkeckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    firmware = 1;
                else
                    firmware = 0;
            }
        });
        lensCkeckBox01.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkChangeList(isChecked, lenses, LENS_01);
                lasersSelectEditText.setText(listToFormatString(lasers, ","));
            }
        });
        lensCkeckBox02.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkChangeList(isChecked, lenses, LENS_02);
                lasersSelectEditText.setText(listToFormatString(lasers, ","));
            }
        });
        laserCkeckBox01.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkChangeList(isChecked, lasers, LASER_01);
                lasersSelectEditText.setText(listToFormatString(lasers, ","));
            }
        });
        laserCkeckBox02.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkChangeList(isChecked, lasers, LASER_02);
                lasersSelectEditText.setText(listToFormatString(lasers, ","));
            }
        });
        laserCkeckBox03.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkChangeList(isChecked, lasers, LASER_03);
                lasersSelectEditText.setText(listToFormatString(lasers, ","));
            }
        });
        laserCkeckBox04.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkChangeList(isChecked, lasers, LASER_04);
                lasersSelectEditText.setText(listToFormatString(lasers, ","));
            }
        });
        laserCkeckBox05.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkChangeList(isChecked, lasers, LASER_05);
                lasersSelectEditText.setText(listToFormatString(lasers, ","));
            }
        });
        laserCkeckBox06.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //                checkChangeList(isChecked, lasers, LASER_06);
                lasersSelectEditText.setText(listToFormatString(lasers, ","));
            }
        });

        //==========================================

        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //2017.01.11 弹出内部参数配置窗口 :
                //获取物镜倍数 :
                lens01Value = Integer.valueOf(lensEditText01.getText().toString().trim());
                lens02Value = Integer.valueOf(lensEditText02.getText().toString().trim());

                //读取各个物镜的焦平面
                String[] lens_01_planes = plansEditText01.getText().toString().trim().split("-");
                lens_01_plane.clear();
                lens01PlaneValues.clear();
                if (lens_01_planes.length > 0) {
                    int[] planeArrays = new int[lens_01_planes.length];
                    for (int i = 0; i < lens_01_planes.length; i++) {
                        lens_01_plane.add(lens_01_planes[i].trim());
                        planeArrays[i] = Integer.valueOf(lens_01_planes[i].trim());
                    }
                    Arrays.sort(planeArrays);
                    for (int i = 0; i < planeArrays.length; i++) {
                        lens01PlaneValues.add(planeArrays[i]);
                    }
                }

                String[] lens_02_planes = plansEditText02.getText().toString().trim().split("-");
                lens_02_plane.clear();
                lens02PlaneValues.clear();
                if (lens_02_planes.length > 0) {
                    int[] planeArrays = new int[lens_02_planes.length];
                    for (int i = 0; i < lens_02_planes.length; i++) {
                        lens_02_plane.add(lens_02_planes[i].trim());
                        planeArrays[i] = Integer.valueOf(lens_02_planes[i].trim());
                    }
                    Arrays.sort(planeArrays);
                    for (int i = 0; i < planeArrays.length; i++) {
                        lens02PlaneValues.add(planeArrays[i]);
                    }
                }

                saveSystemValue();

                initSystemValue();

                mSystemConfigDialog.dismiss();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mSystemConfigDialog.dismiss();
            }
        });
        mSystemConfigDialog.show();
    }

    private static void saveSystemValue() {

        //保存版本设置 :
        PropertiesUtils.setConfig(FIRMWARE, String.valueOf(firmware), mContext);

        //保存物镜配置 :
        PropertiesUtils.setConfig(LENS, listToFormatString(lenses, ","), mContext);

        //保存激发光配置 :
        PropertiesUtils.setConfig(LASER, listToFormatString(lasers, ","), mContext);

        //保存物镜设置 :
        PropertiesUtils.setConfig(LENS_01, String.valueOf(lens01Value), mContext);
        PropertiesUtils.setConfig(LENS_02, String.valueOf(lens02Value), mContext);

        //保存物镜焦面设置 :
        PropertiesUtils.setConfig(LENS_01_PLANE, listToFormatString(lens01PlaneValues, "-"), mContext);
        PropertiesUtils.setConfig(LENS_02_PLANE, listToFormatString(lens02PlaneValues, "-"), mContext);

        //保存各激发光rgb值 :
        PropertiesUtils.setConfig(LASER_01, listToFormatString(laser_01, ","), mContext);
        PropertiesUtils.setConfig(LASER_02, listToFormatString(laser_02, ","), mContext);
        PropertiesUtils.setConfig(LASER_03, listToFormatString(laser_03, ","), mContext);
        PropertiesUtils.setConfig(LASER_04, listToFormatString(laser_04, ","), mContext);
        PropertiesUtils.setConfig(LASER_05, listToFormatString(laser_05, ","), mContext);
    }


    private static void checkChangeList(boolean isChecked, List list, String containSting) {
        if (isChecked) {
            if (!list.contains(containSting))
                list.add(containSting);
        } else {
            if (list.contains(containSting))
                list.remove(containSting);
        }
    }

    private static void initSystemValue() {
        //初始化系统参数设置 :

        firmware = Integer.valueOf(getConfig(FIRMWARE, "0", mContext).trim());

        //读取物镜倍数 :
        lens01Value = Integer.valueOf(getConfig(LENS_01, "5", mContext).trim());
        lens02Value = Integer.valueOf(getConfig(LENS_02, "10", mContext).trim());

        //读取物镜
        String[] leneses = PropertiesUtils.getConfig(LENS, "0", mContext).split(",");
        lenses.clear();
        if (leneses.length > 0) {
            for (int i = 0; i < leneses.length; i++) {
                lenses.add(leneses[i].trim());
            }
        }

        //读取激发光
        String[] laserses = PropertiesUtils.getConfig(LASER, "", mContext).split(",");
        lasers.clear();
        if (laserses.length > 0) {
            for (int i = 0; i < laserses.length; i++) {
                lasers.add(laserses[i].trim());
            }
        }

        //读取各个物镜的焦平面
        String[] lens_01_planes = PropertiesUtils.getConfig(LENS_01_PLANE, "0", mContext).split("-");
        lens_01_plane.clear();
        lens01PlaneValues.clear();
        if (lens_01_planes.length > 0) {
            for (int i = 0; i < lens_01_planes.length; i++) {
                lens_01_plane.add(lens_01_planes[i].trim());
                lens01PlaneValues.add(Integer.valueOf(lens_01_planes[i].trim()));
            }
        }

        String[] lens_02_planes = PropertiesUtils.getConfig(LENS_02_PLANE, "0", mContext).split("-");
        lens_02_plane.clear();
        lens02PlaneValues.clear();
        if (lens_02_planes.length > 0) {
            for (int i = 0; i < lens_02_planes.length; i++) {
                lens_02_plane.add(lens_02_planes[i].trim());
                lens02PlaneValues.add(Integer.valueOf(lens_02_planes[i].trim()));
            }
        }

        String[] lens_03_planes = PropertiesUtils.getConfig(LENS_03_PLANE, "0", mContext).split("-");
        lens_03_plane.clear();
        lens03PlaneValues.clear();
        if (lens_03_planes.length > 0) {
            for (int i = 0; i < lens_03_planes.length; i++) {
                lens_03_plane.add(lens_03_planes[i].trim());
                lens03PlaneValues.add(Integer.valueOf(lens_03_planes[i].trim()));
            }
        }

        //读取激发光rgb值
        String[] laser_01_rgb = PropertiesUtils.getConfig(LASER_01, "", mContext).split(",");
        laser_01.clear();
        if (laser_01_rgb.length > 0) {
            for (int i = 0; i < laser_01_rgb.length; i++) {
                laser_01.add(laser_01_rgb[i].trim());
            }
        }

        String[] laser_02_rgb = PropertiesUtils.getConfig(LASER_02, "", mContext).split(",");
        laser_02.clear();
        if (laser_02_rgb.length > 0) {
            for (int i = 0; i < laser_02_rgb.length; i++) {
                laser_02.add(laser_02_rgb[i].trim());
            }
        }

        String[] laser_03_rgb = PropertiesUtils.getConfig(LASER_03, "", mContext).split(",");
        laser_03.clear();
        if (laser_03_rgb.length > 0) {
            for (int i = 0; i < laser_03_rgb.length; i++) {
                laser_03.add(laser_03_rgb[i].trim());
            }
        }

        String[] laser_04_rgb = PropertiesUtils.getConfig(LASER_04, "", mContext).split(",");
        laser_04.clear();
        if (laser_04_rgb.length > 0) {
            for (int i = 0; i < laser_04_rgb.length; i++) {
                laser_04.add(laser_04_rgb[i].trim());
            }
        }

        String[] laser_05_rgb = PropertiesUtils.getConfig(LASER_05, "", mContext).split(",");
        laser_05.clear();
        if (laser_05_rgb.length > 0) {
            for (int i = 0; i < laser_05_rgb.length; i++) {
                laser_05.add(laser_05_rgb[i].trim());
            }
        }
    }


}
