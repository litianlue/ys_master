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
 * Created by virgilyan on 15/11/23.
 */
public class RecolorAdapter extends ArrayAdapter<String[]> {
    protected static final boolean DEBUG = false;
    private static final String TAG = RecolorAdapter.class.getName();

    static List<String[]> colors = new ArrayList<>();

    public RecolorAdapter(Context context) {
        super(context, 0, colors);
        readFromAssets(context);
    }

    public static void readFromAssets(Context context) {
        if (colors.size() == 0) {
            InputStream is = null;
            try {
                is = context.getAssets().open("colors.txt");
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

    public int getColor(int position) {
        int color = 0;
        if (colors.size() <= position) {    //说明已经超出了color的范围 .
            return color;
        }

        color = Color.rgb(Integer.parseInt(colors.get(position)[0]),
                Integer.parseInt(colors.get(position)[1]),
                Integer.parseInt(colors.get(position)[2]));

        return color;
    }

    public String getColorString(int position) {
        String colorString = "No-Color";
        if (colors.size() <= position) {    //说明已经超出了color的范围 .
            return colorString;
        }
        colorString = colors.get(position)[4];
        return colorString;
    }

    /**
     * 按行读取txt
     *
     * @param is
     * @return
     * @throws Exception
     */
    private static String readTextFromSDcard(InputStream is) {
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
                buffer.append(str);
                buffer.append("\n");
                String[] strings = str.split(",");
                colors.add(strings);
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                //关闭资源输入输出流 :
                if (bufferedReader != null)
                    bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    //关闭资源输入输出流 :
                    if (reader != null)
                        reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return buffer.toString();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_recolor_setting, null);
            holder = new ViewHolder(convertView);
            holder.setMiddleGrad();
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        int colorPos = BaseApplication.getInstance().getRecolor_pos();
        if (colorPos == position) {
            holder.getBottomGrad().setColor(getContext().getResources().getColor(R.color.BLACK));
            holder.getBackgroundGrad().setColor(getContext().getResources().getColor(R.color.BLACK));
            //            holder.getColorBackgroundView().setBackgroundColor(getContext().getResources().getColor(R.color.BLACK));
            //            holder.getTextBackgroundView().setBackgroundColor(getContext().getResources().getColor(R.color.BLACK));
            holder.getColorNameView().setTextColor(getContext().getResources().getColor(R.color.WHITE));
        } else {
            holder.getBottomGrad().setColor(getContext().getResources().getColor(R.color.RECOLOR_NO_SELECT));
            holder.getBackgroundGrad().setColor(getContext().getResources().getColor(R.color.RECOLOR_NO_SELECT));
            //            holder.getColorBackgroundView().setBackgroundColor(getContext().getResources().getColor(R.color.RECOLOR_NO_SELECT));
            //            holder.getTextBackgroundView().setBackgroundColor(getContext().getResources().getColor(R.color.RECOLOR_NO_SELECT));
            holder.getColorNameView().setTextColor(getContext().getResources().getColor(R.color.RECOLOR_TEXT));
        }
        holder.getLabelGrad().setColor(Color.rgb(Integer.parseInt(colors.get(position)[0]), Integer.parseInt(colors.get(position)[1]), Integer.parseInt(colors.get(position)[2])));
        //        holder.getColorLabelView().setBackgroundColor(Color.rgb(Integer.parseInt(colors.get(position)[0]), Integer.parseInt(colors.get(position)[1]), Integer.parseInt(colors.get(position)[2])));
        holder.getColorNameView().setText(colors.get(position)[4]);
        return convertView;
    }

    private class ViewHolder {
        private View view;
        private View colorLabelView;
        private TextView colorNameView;
        private View colorBackgroundView;
        private View textBackgroundView;

        private View middleView;
        private GradientDrawable backgroundGrad;
        private GradientDrawable middleGrad;
        private GradientDrawable labelGrad;
        private GradientDrawable bottomGrad;
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

        public TextView getColorNameView() {
            if (colorNameView == null)
                colorNameView = (TextView) view.findViewById(R.id.color_text);
            return colorNameView;
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

        public View getTextBackgroundView() {
            if (textBackgroundView == null) {
                textBackgroundView = view.findViewById(R.id.text_background);
                GradientDrawable grad = new GradientDrawable();
                grad.setCornerRadii(bottomRadii);
                textBackgroundView.setBackground(grad);
            }
            return textBackgroundView;
        }

        public View getMiddleView() {
            if (middleView == null) {
                middleView = view.findViewById(R.id.middle_view);
                GradientDrawable grad = new GradientDrawable();
                grad.setCornerRadii(backgroundRadii);
                middleView.setBackground(grad);
            }
            return middleView;
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

        public GradientDrawable getBottomGrad() {
            if (bottomGrad == null) {
                bottomGrad = (GradientDrawable) getTextBackgroundView().getBackground();
            }
            return bottomGrad;
        }
    }
}
