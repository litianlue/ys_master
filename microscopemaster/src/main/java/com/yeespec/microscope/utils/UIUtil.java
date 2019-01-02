package com.yeespec.microscope.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.yeespec.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * @author Mark
 * @version 0.1
 */

public class UIUtil {

    /**
     * 弹出提示函数封装Toast
     *
     * @param context
     * Activity活动
     * @param str
     * 要弹出提示的内容
     */
    public static Toast toast = null;

    /**
     * 选择数字
     *
     * @param context
     * @param text
     * @param min
     * @param max
     */
   /* public static void numberPicker(Context context, final EditText text, int min, int max) {
        Builder builder = new Builder(context);
        int position = 0;
        final String[] items = new String[max - min + 1];
        for (int i = 0; i < items.length; i++) {
            String value = String.valueOf(i + 30);
            items[i] = value;
            if (text.getText().toString().equals(value)) {
                position = i;
            }
        }
        builder.setSingleChoiceItems(items, position, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                text.setText(items[which]);
                dialog.dismiss();
            }
        });
        builder.show();
    }

    *//**
     * @param context
     * @param date    文本编辑器，规定日期格式：yyyy-MM-dd
     *//*
    public static void datePicker(Context context, final EditText date) {
        String[] dateArray = null;
        if (date.getText() != null && !date.getText().toString().equals("")) {
            dateArray = date.getText().toString().split("-");
        } else {
            dateArray = new SimpleDateFormat("yyyy-MM-dd").format(new Date()).split("-");
        }
        Builder builder = new Builder(context);
        DatePicker datePicker = new DatePicker(context);
        // datePicker.setMaxDate(System.currentTimeMillis());
        datePicker.init(Integer.valueOf(dateArray[0]), Integer.valueOf(dateArray[1]) - 1, Integer.valueOf(dateArray[2]), new OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker picker, int year, int month, int day) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, day);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                date.setText(sdf.format(calendar.getTime()));
            }
        });
        builder.setView(datePicker);
        builder.setNeutralButton("确定", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
*/
    /**
     * @param context
     * @param //date    文本编辑器，规定日期格式：yyyy-MM-dd
     */
   /* public static void datePicker(Context context, final TextView date) {
        String[] dateArray = null;
        if (date.getText() != null && !date.getText().toString().equals("")) {
            dateArray = date.getText().toString().split("-");
        } else {
            dateArray = new SimpleDateFormat("yyyy-MM-dd").format(new Date()).split("-");
        }
        Builder builder = new Builder(context);
        DatePicker datePicker = new DatePicker(context);
        // datePicker.setMaxDate(System.currentTimeMillis());
        datePicker.init(Integer.valueOf(dateArray[0]), Integer.valueOf(dateArray[1]) - 1, Integer.valueOf(dateArray[2]), new OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker picker, int year, int month, int day) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, day);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                date.setText(sdf.format(calendar.getTime()));
            }
        });
        builder.setView(datePicker);
        builder.setNeutralButton("确定", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void showPopupWindow(View parent, View child, int width, int height) {
        final PopupWindow popupWindow = new PopupWindow(child, width, height);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        int xPos = parent.getWidth() / 2 - popupWindow.getWidth() / 2;
        popupWindow.showAsDropDown(parent, xPos, 0);
    }

    public static PopupWindow getPopupWindow(View parent, View child, int width, int height) {
        final PopupWindow popupWindow = new PopupWindow(child, width, height);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        int xPos = parent.getWidth() / 2 - popupWindow.getWidth() / 2;
        popupWindow.showAsDropDown(parent, xPos, 0);
        return popupWindow;
    }
*/
    public static void toast(final Activity context, final String str, final boolean iscenter) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View v = inflater.inflate(R.layout.toast_layout,null);
        final TextView tv = (TextView) v.findViewById(R.id.toastText);
        tv.setTextSize(30);
        tv.setText(str);
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (toast == null) {
                    toast = new Toast(context);

                    toast.setView(v);
                    toast.setDuration(Toast.LENGTH_SHORT);
                    if(iscenter)
                        toast.setGravity(Gravity.CENTER, 0, 0);



                } else {
                    toast.setView(v);
                    toast.setDuration(Toast.LENGTH_SHORT);
                    if(iscenter)
                        toast.setGravity(Gravity.CENTER, 0, 0);

                }

                toast.show();
            }
        });
    }

    public static void toast(final Context context, final String str, final boolean iscenter) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View v = inflater.inflate(R.layout.toast_layout,null);
        final TextView tv = (TextView) v.findViewById(R.id.toastText);
        tv.setTextSize(30);
        tv.setText(str);
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (toast == null) {
                    toast = new Toast(context);
                    toast.setView(v);
                    toast.setDuration(Toast.LENGTH_SHORT);
                    if(iscenter)
                        toast.setGravity(Gravity.CENTER, 0, 0);
                    //toast = Toast.makeText(context, str, Toast.LENGTH_SHORT);
                    //toast.setGravity(Gravity.CENTER, 0, 0);

                } else {
                    toast.setView(v);
                    toast.setDuration(Toast.LENGTH_SHORT);
                    if(iscenter)
                        toast.setGravity(Gravity.CENTER, 0, 0);
                }
                toast.show();
            }
        });
    }

    /**
     * 带确定按钮的提示框
     *
     * @param context Activity活动
     * @param title   标题
     * @param message 弹出提示的内容
     */
   /* public static void alert(Activity context, String title, String message) {
        Builder builder = new Builder(context);
        builder.setTitle("提示");
        builder.setMessage(message);
        builder.setPositiveButton("关闭", new OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
            }
        });
        AlertDialog ad = builder.create();
        ad.show();

    }

    public static void showAlert(Context context, String title, String message, OnClickListener listener) {
        Builder builder = new Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNeutralButton("取消", new OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                arg0.dismiss();
            }
        });
        builder.setPositiveButton("查看", listener);
        AlertDialog ad = builder.create();
        ad.show();
    }

    public static void showDialog(final Activity context, final String message) {
        final Builder builder = new Builder(context);
        builder.setMessage(message);
        builder.setTitle("提示");
        builder.setPositiveButton("确认", new OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    public static void showDialog(final Context context, final String message) {
        final Builder builder = new Builder(context);
        builder.setMessage(message);
        builder.setTitle("提示");
        builder.setPositiveButton("确认", new OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }


    public static void confirm(Activity context, String title, String message, OnClickListener enter) {
        Builder builder = new Builder(context);
        if (title != null)
            builder.setTitle(title);
        if (message != null)
            builder.setMessage(message);
        builder.setPositiveButton("确定", enter);
        builder.setNegativeButton("取消", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog ad = builder.create();
        ad.show();
    }
*/

   /* public static void confirmEdit(Activity context, String title, String message, OnClickListener enter, String buttonOK, String buttonCancel) {
        Builder builder = new Builder(context);
        if (title != null)
            builder.setTitle(title);
        if (message != null)
            builder.setMessage(message);
        builder.setPositiveButton("确定", enter);
        builder.setNegativeButton("取消", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog ad = builder.create();
        ad.show();
    }
*/
    /**
     * 通知管理器
     *
     * @param context Activity活动
     * @return 通知管理器
     */
   /* public static NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }*/

//    /**
//     * 通知
//     *
//     * @param context      Activity活动
//     * @param contentTitle 标题
//     * @param contentText  内容
//     * @param tickerText   显示在顶部的弹出信息描述
//     * @param intent       点击通知后的行为
//     * @return 通知
//     */
//    public static Notification getNotification(Context context, String contentTitle, String contentText, String tickerText, Intent intent) {
//        Notification notification = new Notification(R.drawable.ic_launcher, tickerText, System.currentTimeMillis());
//        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);
//        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
//        return notification;
//    }

    /**
     * 释放bmp资源
     *
     * @param bmp
     */
    /*public static void recycleBmp(Bitmap bmp) {
        if (null != bmp && !bmp.isRecycled()) {
            bmp.recycle();
        }
    }
*/
    /**
     * @param //context Activity活动
     * @param //title   确认框标题
     * @param// view    视图
     */
    /*public static void menuDialog(Activity context, String title, View view, String buttonText) {
        Builder builder = new Builder(context);
        builder.setTitle(title);
        builder.setView(view);
        builder.setPositiveButton(buttonText, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog ad = builder.create();
        ad.show();
    }
*/
    public interface OnSelectAlter {
        public void onSelect(String select);
    }

    /*public static void showSelectAlert(Activity context, final String[] arrayString, final OnSelectAlter onSelectAlter) {
        Builder builder = new Builder(context);
        builder.setItems(arrayString, new OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                onSelectAlter.onSelect(arrayString[arg1]);
            }
        });
        builder.show();
    }
*/
    /**
     * 应用于Activity的获取控件实例
     *
     * @param activity
     * @param id
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T extends View> T query(Activity activity, int id) {
        return (T) activity.findViewById(id);
    }

    /**
     * 应用于View的获取控件实例
     *
     * @param view
     * @param id
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T extends View> T query(View view, int id) {
        return (T) view.findViewById(id);
    }

}
