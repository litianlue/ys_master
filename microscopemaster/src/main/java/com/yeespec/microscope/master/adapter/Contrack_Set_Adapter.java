package com.yeespec.microscope.master.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.yeespec.R;
import com.yeespec.libuvccamera.bluetooth.DeviceActivity;
import com.yeespec.microscope.utils.bluetooth.DataUtil;

/**
 * Created by Administrator on 2017/12/6.
 */

public class Contrack_Set_Adapter extends BaseAdapter {
    private int defaultSelection = -1;
    private String strs[];
    private boolean [] checks;
    private Context context;
    public Contrack_Set_Adapter(Context context,String[] strs,boolean []checks) {
        this.strs = strs;
        this.context = context;
        this.checks = checks;

    }

    @Override
    public int getCount() {
        return strs.length;
    }

    @Override
    public Object getItem(int position) {
        return strs[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    private  ViewHolder mholder;
    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
         //final ViewHolder mholder;

        //观察convertView随ListView滚动情况

        if (convertView == null) {
            convertView =  LayoutInflater.from(context).inflate(R.layout.item_contract,
                    null);
            mholder = new ViewHolder();
             /*得到各个控件的对象*/
            mholder.title = (TextView) convertView.findViewById(R.id.itemtitle);
            mholder.itemsate = (TextView) convertView.findViewById(R.id.itemsate);
            mholder.itemcb = (CheckBox) convertView.findViewById(R.id.itemcheck);
            mholder.testCheck = (TextView) convertView.findViewById(R.id.testcheck);
            convertView.setTag(mholder);//绑定ViewHolder对象

        }else
        {
            mholder = (ViewHolder)convertView.getTag();//取出ViewHolder对象

        }
        mholder.title.setText(strs[position]);


        if(DataUtil.CheckNums[position]){
            mholder.testCheck.setText("已确定");
            mholder.testCheck.setTextColor(Color.argb(255,255,0,0));
          //  mholder.itemsate.setText("位置："+DataUtil.moveSate[position][0]+":"+DataUtil.moveSate[position][1]);
        }else {
            mholder.testCheck.setText("确定");
            mholder.testCheck.setTextColor(Color.argb(255,0,0,0));
          //  mholder.itemsate.setText("位置："+DataUtil.moveSate[position][0]+":"+DataUtil.moveSate[position][1]);
        }
      //  Log.w("test","DataUtil.CheckNums[position]="+DataUtil.CheckNums[position]);






        return convertView;
    }
    /**
     * @param position
     *            设置的tem
     */
    public boolean setSelectPosition(int position) {
        boolean result =false;
        if (!(position < 0 || position > strs.length)) {
            //defaultSelection = position;
            if (!DataUtil.CheckNums[position]) {// 选中时设置单纯颜
                mholder.testCheck.setText("已确定");
                mholder.testCheck.setTextColor(Color.argb(255,255,0,0));
                DataUtil.moveSate[position][0] =DataUtil.stateI;
                DataUtil.moveSate[position][1] =DataUtil.stateII;
                DataUtil.CheckNums[position] = true;
                notifyDataSetChanged();
                return  true;
            } else {// 未选中时设置selector
                mholder.testCheck.setText("确定");
                mholder.testCheck.setTextColor(Color.argb(0,0,0,0));
                DataUtil.moveSate[position][0] =0;
                DataUtil.moveSate[position][1] =0;
                DataUtil.CheckNums[position] = false;
                notifyDataSetChanged();
                return false;
            }


        }
        return  result;
    }
    public final class ViewHolder{
        public TextView title;
        public TextView itemsate;
        public CheckBox itemcb;
        public TextView testCheck;
    }
}
