package com.bdl.airecovery.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.bdl.airecovery.R;

/**
 * 实例化LoginDialog对象后即可使用
 * setTitle():设置标题，如果不设置就不会显示
 * setMessage():设置要显示的主要信息，如果不设置就不会显示
 * 可以参考SystemSetActivity中的252行代码
 */
public class LoginDialog extends Dialog {

    private String title; //标题
    private String message; //主要信息


    public LoginDialog(Context context) { super(context, R.style.CustomDialog);}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.dialog_login);
        TextView textTitle = (TextView) findViewById(R.id.common_dialog_title);
        TextView textMsg = (TextView) findViewById(R.id.common_dialog_msg);

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
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}