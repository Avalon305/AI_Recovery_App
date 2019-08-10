package com.bdl.aisports.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import com.bdl.aisports.MyApplication;

/**
 * @Author: 闫科宇
 * @Data: 2019/4/7 13:49
 * @Description:
 */
public class CharTextView extends AppCompatTextView {
    public CharTextView(Context context) {
        super(context);
    }

    public CharTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CharTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setTypeface(@Nullable Typeface tf) {
        tf = MyApplication.typefaceChar;
        super.setTypeface(tf);
    }
}
