package com.bdl.aisports.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.bdl.aisports.R;

/**
 * Created by NayelyA on 2019/3/10.
 */

public class LargeDialog extends Dialog {

    private String title; //标题
    private String message; //主要信息
    private View.OnClickListener onPositiveClickListener;
    private String positiveBtnText = "确定"; //Positive按钮文本


    public LargeDialog(Context context) { super(context, R.style.CustomDialog);}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.large_dialog);
        TextView textTitle = (TextView) findViewById(R.id.large_dialog_title);
        TextView textMsg = (TextView) findViewById(R.id.large_dialog_msg);
        TextView sure = (TextView) findViewById(R.id.large_dialog_instruction);

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


    public void setOnPositiveClickListener(View.OnClickListener onPositiveClickListener) {
        this.onPositiveClickListener = onPositiveClickListener;
    }

    public void setPositiveBtnText(String positiveBtnText) {
        this.positiveBtnText = positiveBtnText;
    }
}
