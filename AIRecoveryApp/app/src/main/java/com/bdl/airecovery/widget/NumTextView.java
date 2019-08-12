package com.bdl.airecovery.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import com.bdl.airecovery.MyApplication;

/**
 * @Author: 闫科宇
 * @Data: 2019/4/7 13:52
 * @Description:
 */
public class NumTextView extends AppCompatTextView {
    public NumTextView(Context context) {
        super(context);
    }

    public NumTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NumTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setTypeface(@Nullable Typeface tf) {
        tf = MyApplication.typefaceNum;
        super.setTypeface(tf);
    }
}
