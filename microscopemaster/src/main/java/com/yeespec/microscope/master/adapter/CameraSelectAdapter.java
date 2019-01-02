package com.yeespec.microscope.master.adapter;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.yeespec.R;
import com.yeespec.microscope.master.activity.SettingActivity;

import java.util.List;

/**
 * Created by Administrator on 2018/3/6.
 */

public class CameraSelectAdapter  extends ArrayAdapter<UsbDevice> {
    private LayoutInflater mInflater;
    private Activity context;
    public CameraSelectAdapter(Activity context, List<UsbDevice> objects) {
        super(context, -1, objects);
        mInflater = LayoutInflater.from(context);
        this.context = context;
    }

    /**
     * 显示灯光，wifi，xiangji,那几项的adapter
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
       ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.item_camera_content, parent, false);
            viewHolder.cameraNameView = (TextView) convertView.findViewById(R.id.camera_name_view);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        UsbDevice currentDevice =  context.getIntent().getParcelableExtra("current_device");
        if (currentDevice != null && !currentDevice.getDeviceName().equals(getItem(position).getDeviceName())) {
            viewHolder.cameraNameView.setTextColor(context.getResources().getColor(R.color.material_blue_grey_950));
        } else {
            viewHolder.cameraNameView.setTextColor(context.getResources().getColor(R.color.LIGHTBLACK));
        }
        viewHolder.cameraNameView.setText(getItem(position).getDeviceName());
        return convertView;
    }

    private class ViewHolder {
        TextView cameraNameView;
    }
}
