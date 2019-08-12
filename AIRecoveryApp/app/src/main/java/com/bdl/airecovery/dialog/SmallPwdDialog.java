package com.bdl.airecovery.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.R;


public class SmallPwdDialog extends Dialog {
    //定义接口
    public interface DataBackListener{
        public void getData(String data);
    }
    protected static final String ACTIVITY_TAG="MyAndroid";

    private String tip;
    private View.OnClickListener onNegativeClickListener;
    private View.OnClickListener onPositiveClickListener;
    private EditText editText;
    private TextView btnSure;
    private TextView btnCancel;
    private TextView tipText;
    private String positiveBtnText = "确定"; //Positive按钮文本

    SmallPwdDialog.DataBackListener listener;   //创建监听对象
    public SmallPwdDialog(Context context, String info, int theme, final SmallPwdDialog.DataBackListener listener) {
        super(context, theme);
        //用传递过来的监听器来初始化
        this.listener = listener;
        setContentView(R.layout.small_dialog_password);
        //tipText=(TextView)findViewById(R.id.small_pwd_tip);
        editText = (EditText) findViewById(R.id.small_pwd_edit);
        btnSure = (TextView) findViewById(R.id.small_pwd_sure);
        btnCancel = (TextView) findViewById(R.id.small_pwd_cancel);
        //title.setVisibility(View.GONE);
        editText.setHint(info);
        final String str=editText.getText().toString();
        listener.getData(str);
        btnSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = editText.getText().toString();
                //这里调用接口，将数据传递出去。
                listener.getData(str);
                if(str.equals(MyApplication.ADMIN_PASSWORD)) {
                    dismiss();
                }else {
                    editText.setText("");
                }
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
       /* btnSure.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });*/
       /* if (onPositiveClickListener == null) { //判断是否设置了确认按钮
            btnSure.setOnClickListener(onPositiveClickListener);
            btnSure.setText(positiveBtnText);
        }*/


       /* if (!TextUtils.isEmpty(tip)) { //判断是否设置了title
            tipText.setVisibility(View.VISIBLE);
            tipText.setText(tip);
        } else {
            tipText.setVisibility(View.GONE);
        }*/


    }


    public void setOnPositiveClickListener(View.OnClickListener onPositiveClickListener) {
        this.onPositiveClickListener = onPositiveClickListener;
    }

    public void setTip(String tip) {
        this.tip = tip;
    }
    public void setPositiveBtnText(String positiveBtnText) {
        this.positiveBtnText = positiveBtnText;
    }

    public void setOnNegativeClickListener(View.OnClickListener onNegativeClickListener) {
        this.onNegativeClickListener = onNegativeClickListener;
    }



}
