package com.yeespec.microscope.master.adapter;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.yeespec.R;
import com.yeespec.microscope.master.activity.SettingActivity;
import com.yeespec.microscope.utils.SettingUtils;

import java.util.List;

/**
 * Created by Administrator on 2018/3/6.
 */

public class WifiSelectAdapter extends ArrayAdapter<ScanResult> {
    private LayoutInflater mInflater;
    private Context context;
    public WifiSelectAdapter(Context context, List<ScanResult> objects) {
        super(context, -1, objects);
        mInflater = LayoutInflater.from(context);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
       ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.item_wifi_content, parent, false);
            viewHolder.wifiNameView = (TextView) convertView.findViewById(R.id.wifi_name_view);
            viewHolder.lockView = (ImageView) convertView.findViewById(R.id.label_lock);
            viewHolder.levelView = (ImageView) convertView.findViewById(R.id.label_level);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        String ssid = SettingUtils.getCurrentSSID(context);
        if (ssid != null && getItem(position).SSID.equals(ssid)) {
            viewHolder.wifiNameView.setTextColor(context.getResources().getColor(android.R.color.holo_blue_dark));
        } else {
            viewHolder.wifiNameView.setTextColor(context.getResources().getColor(R.color.LIGHTBLACK));
        }
        viewHolder.wifiNameView.setText(getItem(position).SSID);
        if (getItem(position).capabilities.contains("WPA") || getItem(position).capabilities.contains("WEP")) {
            viewHolder.lockView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.lockView.setVisibility(View.GONE);
        }
        int nSigLevel = WifiManager.calculateSignalLevel(getItem(position).level, 100);
        if (nSigLevel > 0 && nSigLevel <= 50) {
            viewHolder.levelView.setImageResource(R.mipmap.wifi_1);
        } else if (nSigLevel > 50 && nSigLevel <= 70) {
            viewHolder.levelView.setImageResource(R.mipmap.wifi_2);
        } else if (nSigLevel > 70 && nSigLevel <= 100) {
            viewHolder.levelView.setImageResource(R.mipmap.wifi_3);
        }
        return convertView;
    }

    private class ViewHolder {
        TextView wifiNameView;
        ImageView lockView;
        ImageView levelView;
    }
}
