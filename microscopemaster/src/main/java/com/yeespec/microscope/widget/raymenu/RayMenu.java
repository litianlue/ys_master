package com.yeespec.microscope.widget.raymenu;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yeespec.R;
import com.yeespec.microscope.utils.SPUtils;

public class RayMenu extends RelativeLayout {
    private RayLayout mRayLayout;

    private ImageView mHintView;

    // 主按钮标记颜色
    private ImageView masterSettingColor;

    // 主按钮标记倍数
    private TextView masterSettingMultiple;

    public RayMenu(Context context) {
        super(context);
        init(context);
    }

    public RayMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        setClipChildren(false);

        LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        li.inflate(R.layout.ray_menu, this);

        mRayLayout = (RayLayout) findViewById(R.id.item_layout);

        final ViewGroup controlLayout = (ViewGroup) findViewById(R.id.control_layout);

        masterSettingColor = (ImageView) findViewById(R.id.setting_light_color);
        masterSettingMultiple = (TextView) findViewById(R.id.setting_multiple);

        controlLayout.setClickable(true);
        controlLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                //2016.06.24 新增 : 用于检测三击设置按钮 , 触发程序自检程序 :
                if (isFastTrebleClick()){
                    SPUtils.put(getContext(), "self-check", true);
                }else {
                    SPUtils.put(getContext(), "self-check", false);
                }

                if (mRayLayout.isExpanded()) {
                    controlLayout.setBackgroundResource(R.mipmap.button_1);
                } else {
                    controlLayout.setBackgroundResource(R.mipmap.button_0);
                }
                mHintView.startAnimation(createHintSwitchAnimation(mRayLayout.isExpanded()));
                mRayLayout.switchState(true);
            }
        });
        mHintView = (ImageView) findViewById(R.id.control_hint);
    }

    public void addItem(View item, OnClickListener listener) {
        mRayLayout.addView(item);
        item.setOnClickListener(getItemClickListener(listener));
    }

    public TextView getMasterSettingMultiple() {
        return masterSettingMultiple;
    }

    public ImageView getMasterSettingColor() {
        return masterSettingColor;
    }

    private OnClickListener getItemClickListener(final OnClickListener listener) {
        return new OnClickListener() {

            @Override
            public void onClick(final View viewClicked) {

                if (isHideAfterClick) {
                    Animation animation = bindItemAnimation(viewClicked, true, 200);
                    animation.setAnimationListener(new AnimationListener() {

                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            postDelayed(new Runnable() {

                                @Override
                                public void run() {
                                    itemDidDisappear();
                                }
                            }, 0);
                        }
                    });

                    final int itemCount = mRayLayout.getChildCount();
                    for (int i = 0; i < itemCount; i++) {
                        View item = mRayLayout.getChildAt(i);
                        if (viewClicked != item) {
                            bindItemAnimation(item, false, 300);
                        }
                    }

                    mRayLayout.invalidate();
                    mHintView.startAnimation(createHintSwitchAnimation(mRayLayout.isExpanded()));
                }

                if (listener != null) {
                    listener.onClick(viewClicked);
                }
            }
        };
    }

    private boolean isHideAfterClick = true;

    /**
     * 是否点击后隐藏列表
     *
     * @param isHideAfterClick
     */
    public void setHideAfterClick(boolean isHideAfterClick) {
        this.isHideAfterClick = isHideAfterClick;
    }

    private Animation bindItemAnimation(final View child, final boolean isClicked, final long duration) {
        Animation animation = createItemDisappearAnimation(duration, isClicked);
        child.setAnimation(animation);
        return animation;
    }

    private void itemDidDisappear() {
        final int itemCount = mRayLayout.getChildCount();
        for (int i = 0; i < itemCount; i++) {
            View item = mRayLayout.getChildAt(i);
            item.clearAnimation();
        }

        mRayLayout.switchState(false);
    }

    private static Animation createItemDisappearAnimation(final long duration, final boolean isClicked) {
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(new ScaleAnimation(1.0f, isClicked ? 2.0f : 0.0f, 1.0f, isClicked ? 2.0f : 0.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f));
        animationSet.addAnimation(new AlphaAnimation(1.0f, 0.0f));
        // 持续时间
        animationSet.setDuration(duration);
        animationSet.setInterpolator(new DecelerateInterpolator());
        animationSet.setFillAfter(true);
        return animationSet;
    }

    private static Animation createHintSwitchAnimation(final boolean expanded) {
        Animation animation = new RotateAnimation(expanded ? 45 : 0, expanded ? 0 : 45, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setStartOffset(0);
        // 持续时间
        animation.setDuration(100);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.setFillAfter(true);
        return animation;
    }

    // TODO: 2016/6/24 : 新增 : 用于检测三击设置按钮 , 触发程序自检程序 :
    // 解决用户连续点击造成出现多个相同的多次响应 :
    public long lastClickTime = 0;
    public int clickCount = 0;

    public boolean isFastTrebleClick() {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime;
        if (timeD >= 0 && timeD <= 500) {
            clickCount ++ ;
            if (clickCount > 1){
                clickCount = 0 ;
                return true;
            }
            return false;
        } else {
            lastClickTime = time;
            clickCount = 0 ;
            return false;
        }
    }


}
