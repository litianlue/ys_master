package com.yeespec.microscope.master.custom;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.yeespec.microscope.utils.log.Logger;

public class MyRecyclerView extends RecyclerView {

    protected static final boolean DEBUG = false;


    private OnBottomCallback mOnBottomCallback;

    public interface OnBottomCallback {
        void onBottom();
    }

    public void setOnBottomCallback(OnBottomCallback onBottomCallback) {
        this.mOnBottomCallback = onBottomCallback;
    }
    public boolean isSlideToBottom(RecyclerView recyclerView) {
        if (recyclerView == null) return false;

        if (recyclerView.computeHorizontalScrollExtent() + recyclerView.computeHorizontalScrollOffset()>= recyclerView.computeHorizontalScrollRange())
            return true;
        return false;

    }

    /**
     * 记录当前第一个View
     */
    private int mCurrentPosition;

    public void setCurrentPosition(int position) {
        this.mCurrentPosition = position;
    }

    private OnItemScrollChangeListener mItemScrollChangeListener;

    public void setOnItemScrollChangeListener(
            OnItemScrollChangeListener mItemScrollChangeListener) {
        this.mItemScrollChangeListener = mItemScrollChangeListener;
    }

    public interface OnItemScrollChangeListener {
        void onChange(View view, int position);
    }

    public MyRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        this.setOnScrollListener(listener);
    }

    private OnScrollListener listener = new OnScrollListener() {

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }


        /**
         * 滚动时，判断当前第一个View是否发生变化，发生才回调
         */
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (isSlideToBottom(recyclerView)) {
                mOnBottomCallback.onBottom();
            }
            LayoutManager manager = recyclerView.getLayoutManager();
            int childCount = manager.getChildCount();
            View firstView = getChildAt(0);
            View lastView = getChildAt(childCount - 1);
            if (mItemScrollChangeListener != null) {
                if (mCurrentPosition == getChildAdapterPosition(firstView) || mCurrentPosition == getChildAdapterPosition(lastView))
                    return;
                mCurrentPosition = getChildAdapterPosition(firstView);
                mItemScrollChangeListener.onChange(firstView, getChildAdapterPosition(firstView));
            }

        }
    };


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

    }
}
