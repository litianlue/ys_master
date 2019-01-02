package com.yeespec.microscope.widget;

import android.content.Context;
import android.widget.ImageView;

/**
 * Created by Administrator on 2018/3/6.
 */

public class ExplosionView extends ImageView {
    public ExplosionView(Context context) {
        super(context);
    }

    public void setLocation(int top, int left) {
        this.setFrame(left, top, left + 144, top + 144);
    }
}
