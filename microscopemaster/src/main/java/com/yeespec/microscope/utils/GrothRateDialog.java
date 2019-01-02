package com.yeespec.microscope.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;
import com.yeespec.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/1/9.
 */

public class GrothRateDialog extends Dialog {
    private static final String TAG = "GrothRateDialog";
    private Context mContext;
    private List<String> list = new ArrayList<>();
    private List<Integer> percentage = new ArrayList<>();//生长率
    private List<String> matterLister = new ArrayList<>();
    private int interval;//时间间隔
    private int xcount;
    private LineChart mLineChar;
    private LineDataSet set1;
    private ArrayList<Entry> values = new ArrayList<Entry>();
    private int MAX_NUM=10;

    public GrothRateDialog(Context context, int themeResId, List<Integer> percentage1, int interval1) {
        super(context, themeResId);

        this.interval = interval1;
        this.percentage = percentage1;
        this.xcount = percentage.size();
        View convertView = getLayoutInflater().inflate(R.layout.growth_rate_dialog, null);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(convertView);
        mLineChar = (LineChart) convertView.findViewById(R.id.mLineChar);
        mLineChar.clear();
        mLineChar.setScaleMinima(1.0f, 1.0f);
        mLineChar.getViewPortHandler().refresh(new Matrix(), mLineChar, true);
        //后台绘制
        mLineChar.setDrawGridBackground(true);
        //设置描述文本
        mLineChar.getDescription().setEnabled(false);
        //设置支持触控手势
        mLineChar.setTouchEnabled(true);
        //设置缩放
        mLineChar.setDragEnabled(true);
        //设置推动
        mLineChar.setScaleEnabled(true);
        //如果禁用,扩展可以在x轴和y轴分别完成
        mLineChar.setPinchZoom(true);
       // mLineChar.setGridBackgroundColor(Color.WHITE & 0x70FFFFFF); // 表格的的颜色，在这里是是给颜色设置一个透明度

        mLineChar.setScaleMinima(1.0f, 1.0f);
        mLineChar.getViewPortHandler().refresh(new Matrix(), mLineChar, true);
        //x轴
        LimitLine llXAxis = new LimitLine(10f, "标记");
        //设置线宽
        llXAxis.setLineWidth(4f);
        //
        llXAxis.enableDashedLine(10f, 10f, 0f);
        //设置
        llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        llXAxis.setTextSize(10f);

        XAxis xAxis = mLineChar.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.enableGridDashedLine(10f, 10f, 0f);
      //  xAxis.setDrawAxisLine(false);
       // xAxis.setDrawGridLines(false);
        //设置x轴的最大值
        if(xcount>=MAX_NUM)
            xAxis.setAxisMaximum((float)(interval*xcount)+ (100-(interval*xcount)%100));
        else
            xAxis.setAxisMaximum((float)(interval*xcount));
       // xAxis.setAxisMaximum(100f);
      //  xAxis.setAxisMaximum(50f);
        //设置x轴的最小值
        xAxis.setAxisMinimum(0f);


        float num=0;
        DecimalFormat df = new DecimalFormat("#0.00");
        list.clear();
        if (xcount > (MAX_NUM-1)) {
            xAxis.setLabelCount(MAX_NUM);
            for (int i = 0; i < MAX_NUM; i++) {
                num+=((float)(interval*xcount)+ (100-(interval*xcount)%100))/MAX_NUM;
                list.add(num+ "");
            }
        } else {

            xAxis.setLabelCount(xcount);
            for (int i = 0; i <xcount; i++) {
                num+=interval;
                list.add(num+ "");
            }
        }
        for (int i = 0; i <list.size(); i++) {
           Log.w(TAG,"list="+list.get(i));
        }
        matterLister = list;
        MyXFormatter myXFormatter = new MyXFormatter(matterLister,interval,xcount);

        xAxis.setValueFormatter(myXFormatter);

        LimitLine ll1 = new LimitLine(50f, "50%分界线");
        ll1.setLineWidth(4f);
        ll1.enableDashedLine(10f, 10f, 0f);
        ll1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll1.setTextSize(10f);
        YAxis leftAxis = mLineChar.getAxisLeft();
        //重置所有限制线,以避免重叠线
        leftAxis.removeAllLimitLines();
        //设置50%分界先
        leftAxis.addLimitLine(ll1);
        leftAxis.setGranularity(1f);
        //y轴最大
        leftAxis.setAxisMaximum(100f);
        //y轴最小
        leftAxis.setAxisMinimum(0f);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setLabelCount(10);
        // 限制数据(而不是背后的线条勾勒出了上面)
        leftAxis.setDrawLimitLinesBehindData(true);
        mLineChar.getAxisRight().setEnabled(false);
        values.clear();
        float x=0;
        for (int i = 0; i < percentage.size(); i++) {

            values.add(new Entry(x, percentage.get(i)));
            x+=interval;
            Log.w(TAG, "i * interval=" + i * interval);
        }
        //设置数据
        setData(values);

        //默认动画
        mLineChar.animateX(1000);
        //刷新
        //mChart.invalidate();
        // 得到这个文字
        Legend l = mLineChar.getLegend();

        // 修改文字 ...
        l.setForm(Legend.LegendForm.LINE);


        List<ILineDataSet> setsCubic = mLineChar.getData().getDataSets();
        for (ILineDataSet iSet : setsCubic) {
            LineDataSet set = (LineDataSet) iSet;
            set.setMode(set.getMode() == LineDataSet.Mode.CUBIC_BEZIER
                    ? LineDataSet.Mode.LINEAR
                    : LineDataSet.Mode.CUBIC_BEZIER);
        }
        mLineChar.invalidate();

    }


    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().setGravity(Gravity.BOTTOM); //显示在底部

        WindowManager m = getWindow().getWindowManager();
        Display d = m.getDefaultDisplay();
        WindowManager.LayoutParams p = getWindow().getAttributes();
        p.width = d.getWidth(); //设置dialog的宽度为当前手机屏幕的宽度
        p.height = d.getHeight() / 4 * 3;
        getWindow().setAttributes(p);

    }



    //传递数据集
    private void setData(ArrayList<Entry> values) {

        if (mLineChar.getData() != null && mLineChar.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet) mLineChar.getData().getDataSetByIndex(0);
            set1.setValues(values);
            mLineChar.getData().notifyDataChanged();
            mLineChar.notifyDataSetChanged();
        } else {
            // 创建一个数据集,并给它一个类型
            set1 = new LineDataSet(values, "细胞汇合率/minute");

            // 在这里设置线
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.BLACK);
            set1.setLineWidth(1f);
            set1.setCircleRadius(2f);
            set1.setDrawCircleHole(false);
            if (values.size() > (MAX_NUM-1)){
                set1.setValueTextSize(0);
            }else {
                set1.setValueTextSize(9f);
            }
            //set1.setValueTextSize(9f);
            set1.setDrawFilled(true);
            set1.setFormLineWidth(1f);
            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set1.setFormSize(15.f);
            if (Utils.getSDKInt() >= 18) {
                // 填充背景只支持18以上
                //Drawable drawable = ContextCompat.getDrawable(this, R.mipmap.ic_launcher);
                //set1.setFillDrawable(drawable);
                set1.setFillColor(Color.YELLOW);
            } else {
                set1.setFillColor(Color.BLACK);
            }
            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            //添加数据集
            dataSets.add(set1);

            //创建一个数据集的数据对象
            LineData data = new LineData(dataSets);
            //
            mLineChar.setData(data);
            mLineChar.invalidate();
        }
    }

    public class MyXFormatter implements IAxisValueFormatter {
        List<String> mValuess;
        int interval;
        int count;
        int i=0;
        public MyXFormatter(List<String> values, int interval, int count) {
            this.mValuess = values;
            this.interval = interval;
            this.count = (count-1);
            Log.i(TAG, "count="+count);
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            Log.i(TAG, "value="+value);
            Log.i(TAG, "(int) value / ((xcount*interval)/20) - 1="+((int) (value / ((xcount*interval)/MAX_NUM) - 1)));
            if(value==0){
                i = 0;
                return "";
            }else {
                String s="";
               // s = mValuess.get((int) value / interval - 1);
                if(count>(MAX_NUM-1)){
                    s = mValuess.get(i);
                    i++;
                }else {
                    if(i<mValuess.size())
                     s = mValuess.get(i);
                     i++;
                }
                return s;
            }

        }
    }
}
