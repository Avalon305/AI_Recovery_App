package com.bdl.aisports.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.bdl.aisports.R;

/**
 * 自定义圆角dialog
 * 实例化CommonDialog对象后即可使用
 * setTitle():设置标题，如果不设置就不会显示
 * setMessage():设置要显示的主要信息，如果不设置就不会显示
 * setOnNegativeClickListener():设置取消键的监听事件，如果不设置就不会显示
 * setOnPositiveClickListener():设置确认键的监听事件，如果不设置就不会显示
 * setNegativeBtnText():更改取消键的文本（默认为“取消”）
 * setPositiveBtnText():更改确认键的文本（默认为“确认”）
 * 可以参考SystemSetActivity中的252行代码
 */
public class CommonDialog extends Dialog {

    private String title; //标题
    private String message; //主要信息
    private View.OnClickListener onNegativeClickListener;
    private View.OnClickListener onPositiveClickListener;
    private String negativeBtnText = "取消"; //Negative按钮文本
    private String positiveBtnText = "确定"; //Positive按钮文本


    public CommonDialog(Context context) { super(context, R.style.CustomDialog);}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.dialog_common);
        TextView textTitle = (TextView) findViewById(R.id.common_dialog_title);
        TextView textMsg = (TextView) findViewById(R.id.common_dialog_msg);
        TextView sure = (TextView) findViewById(R.id.common_dialog_sure);
        TextView cancel = (TextView) findViewById(R.id.common_dialog_cancel);

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
}