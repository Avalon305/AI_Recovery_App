package com.bdl.airecovery.dialog;

import android.app.Dialog;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.bdl.airecovery.R;
import com.bdl.airecovery.util.WifiUtils;

/**
 * 密码输入Dialog
 */
public class WifiLinkDialog extends Dialog implements View.OnClickListener {

    private TextView wifiLinkTitle;
    private EditText passwordEdit;
    private TextView btnSure;
    private TextView btnCancel;

    private String wifiName = null; //WiFi名称
    private String capabilities; //加密类型


    public WifiLinkDialog(@NonNull Context context, String wifiName, String capabilities) {
        super(context, R.style.CustomDialog);
        this.wifiName = wifiName;
        this.capabilities = capabilities;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_dialog);
        wifiLinkTitle = findViewById(R.id.input_dialog_title);
        passwordEdit = findViewById(R.id.input_dialog_edit);
        btnSure = findViewById(R.id.input_dialog_sure);
        btnCancel = findViewById(R.id.input_dialog_cancel);
        wifiLinkTitle.setText(wifiName);
        passwordEdit.setHint("请输入WiFi密码");
        initListener();

    }

    private void initListener(){
        btnSure.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        passwordEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if((capabilities.contains("WPA") || capabilities.contains("WPA2") || capabilities.contains("WPS"))){
                    Log.e("wifi------------", capabilities);
                    Log.e("wifi------------", "----------");
                    if(passwordEdit.getText() == null && passwordEdit.getText().toString().length() < 8){
                        btnSure.setClickable(false);
                    }else{
                        btnSure.setClickable(true);
                    }
                }else if(capabilities.contains("WEP")){
                    Log.e("wifi------------", "----------");
                    if(passwordEdit.getText() == null && passwordEdit.getText().toString().length() < 8){
                        btnSure.setClickable(false);
                    }else{
                        btnSure.setClickable(true);
                    }
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.input_dialog_sure:
                Log.e("wifi------------", "----------==");
                WifiConfiguration tempConfig  = WifiUtils.isExsits(wifiName,getContext());
                if(tempConfig == null){
                    Log.e("wifi------------", "----------1");
                    WifiConfiguration wifiConfiguration =  WifiUtils.createWifiConfig(wifiName,passwordEdit.getText().toString(),WifiUtils.getWifiCipher(capabilities));
                    WifiUtils.addNetWork(wifiConfiguration,getContext());
                }else{
                    Log.e("wifi------------", "----------2");
                    WifiUtils.addNetWork(tempConfig,getContext());
                }
                dismiss();
                break;
            case R.id.input_dialog_cancel:
                dismiss();
                break;
        }
    }
}
