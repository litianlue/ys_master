package com.yeespec.microscope.master.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.yeespec.R;
import com.yeespec.microscope.master.application.BaseApplication;
import com.yeespec.microscope.utils.log.Logger;
import com.yeespec.microscope.utils.SPUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mr.Wen on 16/10/24.
 */
public class SaturationLightAdapter extends ArrayAdapter<String[]> {
    protected static final boolean DEBUG = false;
    private static final String TAG = SaturationLightAdapter.class.getName();

    static List<String[]> colors = new ArrayList<>();
    private static int mlightcount;
    private static String mlighttype;
    public SaturationLightAdapter(Context context,int lightcount,String lighttype) {
        super(context, 0, colors);
        this.mlightcount = lightcount;
        this.mlighttype = lighttype;
        readFromAssets(context);
    }

    public static void readFromAssets(Context context) {
        if (colors.size() == 0) {
            InputStream is = null;
            try {

                is = context.getAssets().open("saturation_lights_and_black.txt");

                String bufferString = readTextFromSDcard(is);
                if (DEBUG)
                    Logger.e(TAG, bufferString);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {

                try {//关闭资源输入输出流 :
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {

                }

            }
        }
    }

    //获取当前position的激发光颜色 ：
    public int getColor(int position) {
        int color = 0;
        if (colors.size() <= position) {    //说明已经超出了color的范围 .
            return color;
        }
        color = Integer.parseInt(colors.get(position)[4]);

        return color;
    }

    /**
     * 按行读取txt
     *
     * @param is
     * @return
     * @throws Exception
     */
    private static String readTextFromSDcard(InputStream is) {

        String []lightstr = mlighttype.split(",");
        int [] lights =null;
        if(lightstr.length>0){


            //如果不是4颗灯，则加上关灯
            if(lightstr.length<4){
                lights =new int[lightstr.length+1];
                for (int j = 0; j < lightstr.length; j++) {
                    lights[j] = Integer.valueOf(lightstr[j]);
                }
                lights[lightstr.length] =5;
            }else {
                lights =new int[lightstr.length];
                for (int j = 0; j < lightstr.length; j++) {
                    lights[j] = Integer.valueOf(lightstr[j]);
                }
            }
        }

        colors.clear();
        InputStreamReader reader = null;
        BufferedReader bufferedReader = null;

        reader = new InputStreamReader(is);
        bufferedReader = new BufferedReader(reader);
        StringBuffer buffer = new StringBuffer("");
        String str;
        int i = 0;
        try {
            while ((str = bufferedReader.readLine()) != null) {
                i++;
                for (int j = 0; j < lights.length; j++) {
                    if(lights[j]==i){
                        buffer.append(str);
                        buffer.append("\n");
                        String[] strings = str.split(",");
                        colors.add(strings);
                        break;
                    }
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            try {
                //关闭资源输入输出流 :
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    //关闭资源输入输出流 :
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return buffer.toString();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {


            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_saturation_lights, null);
            holder = new ViewHolder(convertView);
            holder.setMiddleGrad();
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        int colorPos = BaseApplication.getInstance().getSaturation_pos();
        if (colorPos == position) {

            holder.getBackgroundGrad().setColor(getContext().getResources().getColor(R.color.BLACK));

        } else {
            //            holder.getBottomGrad().setColor(getContext().getResources().getColor(R.color.RECOLOR_NO_SELECT));
            holder.getBackgroundGrad().setColor(getContext().getResources().getColor(R.color.RECOLOR_NO_SELECT));
            //            holder.getColorBackgroundView().setBackgroundColor(getContext().getResources().getColor(R.color.RECOLOR_NO_SELECT));
            //            holder.getTextBackgroundView().setBackgroundColor(getContext().getResources().getColor(R.color.RECOLOR_NO_SELECT));
            //            holder.getColorNameView().setTextColor(getContext().getResources().getColor(R.color.RECOLOR_TEXT));
        }
        holder.getLabelGrad().setColor(Color.rgb(Integer.parseInt(colors.get(position)[0]), Integer.parseInt(colors.get(position)[1]), Integer.parseInt(colors.get(position)[2])));
        //        holder.getColorLabelView().setBackgroundColor(Color.rgb(Integer.parseInt(colors.get(position)[0]), Integer.parseInt(colors.get(position)[1]), Integer.parseInt(colors.get(position)[2])));
        //        holder.getColorNameView().setText(colors.get(position)[4]);
        return convertView;
    }

    private class ViewHolder {
        private View view;
        private View colorLabelView;

        private View colorBackgroundView;


        private View middleView;
        private GradientDrawable backgroundGrad;

        private GradientDrawable labelGrad;

        private float[] backgroundRadii = new float[]{8, 8, 8, 8, 0, 0, 0, 0};
        private float[] bottomRadii = new float[]{0, 0, 0, 0, 8, 8, 8, 8};
        private float[] labelRadii = new float[]{8, 8, 8, 8, 8, 8, 8, 8};

        public ViewHolder(View view) {
            this.view = view;
        }

        public View getColorLabelView() {
            if (colorLabelView == null) {
                colorLabelView = view.findViewById(R.id.color_label);
                GradientDrawable grad = new GradientDrawable();
                grad.setCornerRadii(labelRadii);
                colorLabelView.setBackground(grad);
            }
            return colorLabelView;
        }

        public View getColorBackgroundView() {
            if (colorBackgroundView == null) {
                colorBackgroundView = view.findViewById(R.id.color_background);
                GradientDrawable grad = new GradientDrawable();
                grad.setCornerRadii(backgroundRadii);
                colorBackgroundView.setBackground(grad);
            }
            return colorBackgroundView;
        }



        public GradientDrawable getBackgroundGrad() {
            if (backgroundGrad == null) {
                backgroundGrad = (GradientDrawable) getColorBackgroundView().getBackground();
            }
            return backgroundGrad;
        }

        public void setMiddleGrad() {
            if (middleView == null) {
                middleView = view.findViewById(R.id.middle_view);
                GradientDrawable grad = new GradientDrawable();
                grad.setCornerRadii(backgroundRadii);
                grad.setColor(getContext().getResources().getColor(R.color.recolor_item_background));
                middleView.setBackground(grad);
            }
        }

        public GradientDrawable getLabelGrad() {
            if (labelGrad == null) {
                labelGrad = (GradientDrawable) getColorLabelView().getBackground();
            }
            return labelGrad;
        }

    }
}
