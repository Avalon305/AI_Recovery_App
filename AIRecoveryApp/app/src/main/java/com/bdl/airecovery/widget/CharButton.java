package com.bdl.airecovery.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;

import com.bdl.airecovery.MyApplication;

/**
 * @Author: 闫科宇
 * @Data: 2019/4/7 15:33
 * @Description:
 */
public class CharButton extends AppCompatButton {
    public CharButton(Context context) {
        super(context);
    }

    public CharButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CharButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setTypeface(@Nullable Typeface tf) {
        tf = MyApplication.typefaceChar;
        super.setTypeface(tf);
    }
}
