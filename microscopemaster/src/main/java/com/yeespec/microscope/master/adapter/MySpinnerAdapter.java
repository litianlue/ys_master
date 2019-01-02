package com.yeespec.microscope.master.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.yeespec.R;

import java.util.ArrayList;

/**
 * Created by Mr.Wen on 2017/4/18.
 */

public class MySpinnerAdapter extends BaseAdapter {


    private final Context mContext;
    private final ArrayList<String> mList;

    public MySpinnerAdapter(Context pContext, ArrayList<String> strings) {
        this.mContext = pContext;
        this.mList = strings;
    }


    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater _LayoutInflater=LayoutInflater.from(mContext);
        convertView=_LayoutInflater.inflate(R.layout.spinneritem, null);

        if(convertView!=null)
        {
            TextView TextView=(TextView)convertView.findViewById(R.id.stextView);
            TextView.setText(mList.get(position));
        }


        return convertView;

    }


}
