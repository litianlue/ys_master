package com.yeespec.microscope.master.receiver;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.yeespec.microscope.utils.bluetooth.ClsUtils;

/**
 * Created by Administrator on 2018/1/17.
 */

public class BluetoothConnectActivityReceiver extends BroadcastReceiver {
    String strPsw = "000000";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
            BluetoothDevice mBluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            try {

                Toast.makeText(context, "需要配对", Toast.LENGTH_SHORT).show();
                //(三星)4.3版本测试手机还是会弹出用户交互页面(闪一下)，如果不注释掉下面这句页面不会取消但可以配对成功。(中兴，魅族4(Flyme 6))5.1版本手机两中情况下都正常
                ClsUtils.setPairingConfirmation(mBluetoothDevice.getClass(), mBluetoothDevice, true);
                abortBroadcast();//如果没有将广播终止，则会出现一个一闪而过的配对框。
                ClsUtils.createBond(mBluetoothDevice.getClass(), mBluetoothDevice);
                //3.调用setPin方法进行配对...
               // boolean ret = ClsUtils.setPin(mBluetoothDevice.getClass(), mBluetoothDevice,strPsw);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
