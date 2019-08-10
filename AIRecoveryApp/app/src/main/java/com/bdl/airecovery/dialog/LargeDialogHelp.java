package com.bdl.airecovery.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bdl.airecovery.R;





public class LargeDialogHelp extends Dialog {

    private String machineName;//设备名称
    private String useNote;//设备使用说明
    private int machineView;//设备图片
    private View.OnClickListener onPositiveClickListener;
    private String positiveBtnText = "确定"; //Positive按钮文本
    public LargeDialogHelp( Context context) {
        super(context, R.style.CustomDialog);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.large_dialog_help);
        TextView textName = (TextView) findViewById(R.id.large_help_name);//显示机器设备名称
        TextView textUse = (TextView) findViewById(R.id.large_help_usenote);//显示使用说明
        TextView sure = (TextView) findViewById(R.id.large_help_instruction);//按钮名称
        ImageView img=(ImageView) findViewById(R.id.large_help_img);//设备图片



        if (!TextUtils.isEmpty(machineName)) { //判断是否设置了设备名称
            textName.setVisibility(View.VISIBLE);
            textName.setText(machineName);
        } else {
            textName.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(useNote)) { //判断是否设置了使用说明
            textUse.setVisibility(View.VISIBLE);
            textUse.setText(useNote);
        } else {
            textName.setVisibility(View.GONE);
        }

        if (onPositiveClickListener != null) { //判断是否设置了确认按钮
            sure.setVisibility(View.VISIBLE);
            sure.setOnClickListener(onPositiveClickListener);
            sure.setText(positiveBtnText);
        } else {
            sure.setVisibility(View.GONE);
        }

    }

    public void setPositiveBtnText(String positiveBtnText) {
        this.positiveBtnText = positiveBtnText;
    }


    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public void setOnPositiveClickListener(View.OnClickListener onPositiveClickListener) {
        this.onPositiveClickListener = onPositiveClickListener;
    }

    public void setMachineView(int machineView) {
        this.machineView = machineView;
    }

    public void setUseNote(String useNote) {
        this.useNote = useNote;
    }
}
