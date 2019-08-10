package com.bdl.airecovery.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.bdl.airecovery.R;

public class MediumDialog extends Dialog{


    private String time;


    public MediumDialog(Context context) { super(context, R.style.CustomDialog);}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.medium_dialog);
        TextView textTime=(TextView)findViewById(R.id.medium_dialog_time);

        if (!TextUtils.isEmpty(time)) { //判断是否设置了时间
            textTime.setVisibility(View.VISIBLE);
            textTime.setText(time);
        } else {
            textTime.setVisibility(View.GONE);
        }
    }

    public void setTime(String time){this.time=time;}

}
