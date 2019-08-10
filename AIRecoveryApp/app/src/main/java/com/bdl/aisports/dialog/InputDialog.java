package com.bdl.aisports.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.bdl.aisports.R;

import org.w3c.dom.Text;

public class InputDialog extends Dialog {
    //定义接口
    public interface DataBackListener{
        public void getData(String data);
    }
    private TextView title;
    private EditText editText;
    private TextView btnSure;
    private TextView btnCancel;
    DataBackListener listener;   //创建监听对象
    public InputDialog(Context context, String info, int theme, final DataBackListener listener) {
        super(context, theme);
        //用传递过来的监听器来初始化
        this.listener = listener;
        setContentView(R.layout.input_dialog);
        title = findViewById(R.id.input_dialog_title);
        editText = (EditText) findViewById(R.id.input_dialog_edit);
        btnSure = (TextView) findViewById(R.id.input_dialog_sure);
        btnCancel = (TextView) findViewById(R.id.input_dialog_cancel);
        title.setVisibility(View.GONE);
        editText.setHint(info);
        btnSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = editText.getText().toString();
                //这里调用接口，将数据传递出去。
                listener.getData(str);
                dismiss();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }
}