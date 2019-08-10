package com.bdl.airecovery.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * 继承 SeekBar 实现自己的SeekBar
 *	该类主要是设置seekbar不可拖动
 *  MySeekBar部件主要用于6种模式的界面中
 */
public class MySeekBar extends android.support.v7.widget.AppCompatSeekBar {

    public MySeekBar(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public MySeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.seekBarStyle);
    }

    public MySeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    /**
     * onTouchEvent 是在 SeekBar 继承的抽象类 AbsSeekBar 里
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        //原来是要将TouchEvent传递下去的,现在不让它传递下去就行了
        //return super.onTouchEvent(event);

        return false ;
    }





}