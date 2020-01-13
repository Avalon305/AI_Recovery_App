package com.bdl.airecovery.activity;

import android.content.Intent;
import android.os.Bundle;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.R;
import com.bdl.airecovery.base.BaseActivity;
import com.bdl.airecovery.bluetooth.CommonCommand;
import com.bdl.airecovery.service.BluetoothService;

import org.xutils.common.util.LogUtil;
import org.xutils.view.annotation.ContentView;

@ContentView(R.layout.activity_urgent_stopping)
public class ScramActivity extends BaseActivity {

    //TODO 急停复位
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //若为体验者模式登录，只显示急停界面
        if(MyApplication.getInstance().getUser().getUsername().equals("体验者")){
            return;
        }else {
            //若为其他模式，急停界面显示过程中要关闭蓝牙连接
            MyApplication.getInstance().setUser(null);
            //关闭蓝牙连接
            Intent intentLog = new Intent(ScramActivity.this, BluetoothService.class);
            intentLog.putExtra("command", CommonCommand.LOGOUT.value());
            startService(intentLog);
            LogUtil.e("蓝牙第一用户退出");
        }
        /*Intent intent = new Intent(ScramActivity.this,scramService.class);
        startService(intent);*/
    }

}
