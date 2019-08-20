package com.bdl.airecovery.dialog;

import android.app.Dialog;
import android.content.Context;
import android.media.Rating;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bdl.airecovery.R;

/**
 * @Description: 评级Dialog
 * @Author: Keyon
 * @Date: 2019/8/20 15:10
 */
public class RatingDialog extends Dialog {

    private String title; //标题
    private String message; //主要信息
    private View.OnClickListener onNegativeClickListener;
    private View.OnClickListener onPositiveClickListener;
    private String negativeBtnText = "取消"; //Negative按钮文本
    private String positiveBtnText = "确定"; //Positive按钮文本
    private RatingBar.OnRatingBarChangeListener ratingBarChangeListener;


    public RatingDialog(Context context) { super(context, R.style.CustomDialog);}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.dialog_rating);
        TextView textTitle = (TextView) findViewById(R.id.common_dialog_title);
        TextView textMsg = (TextView) findViewById(R.id.common_dialog_msg);
        TextView sure = (TextView) findViewById(R.id.common_dialog_sure);
        TextView cancel = (TextView) findViewById(R.id.common_dialog_cancel);
        RatingBar ratingBar = (RatingBar) findViewById(R.id.rb_rating);

        if (!TextUtils.isEmpty(title)) { //判断是否设置了title
            textTitle.setVisibility(View.VISIBLE);
            textTitle.setText(title);
        } else {
            textTitle.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(message)) { //判断是否设置了message
            textMsg.setVisibility(View.VISIBLE);
            textMsg.setText(message);
        } else {
            textMsg.setVisibility(View.GONE);
        }

        if (onNegativeClickListener != null) { //判断是否设置了返回按钮
            cancel.setVisibility(View.VISIBLE);
            cancel.setOnClickListener(onNegativeClickListener);
            cancel.setText(negativeBtnText);
        } else {
            cancel.setVisibility(View.GONE);
        }

        if (onPositiveClickListener != null) { //判断是否设置了确认按钮
            sure.setVisibility(View.VISIBLE);
            sure.setOnClickListener(onPositiveClickListener);
            sure.setText(positiveBtnText);
        } else {
            sure.setVisibility(View.GONE);
        }

    }
    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setOnNegativeClickListener(View.OnClickListener onNegativeClickListener) {
        this.onNegativeClickListener = onNegativeClickListener;
    }

    public void setOnPositiveClickListener(View.OnClickListener onPositiveClickListener) {
        this.onPositiveClickListener = onPositiveClickListener;
    }

    public void setNegativeBtnText(String negativeBtnText) {
        this.negativeBtnText = negativeBtnText;
    }

    public void setPositiveBtnText(String positiveBtnText) {
        this.positiveBtnText = positiveBtnText;
    }

    public RatingBar.OnRatingBarChangeListener getRatingBarChangeListener() {
        return ratingBarChangeListener;
    }

    public void setRatingBarChangeListener(RatingBar.OnRatingBarChangeListener ratingBarChangeListener) {
        this.ratingBarChangeListener = ratingBarChangeListener;
    }
}
