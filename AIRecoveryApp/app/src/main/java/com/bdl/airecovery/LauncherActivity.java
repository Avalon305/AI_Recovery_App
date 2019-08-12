package com.bdl.airecovery;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.bdl.airecovery.base.BaseActivity;
import com.bdl.airecovery.util.SerialPortUtils;

import org.apache.log4j.Logger;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

@ContentView(R.layout.activity_launcher)
public class LauncherActivity extends BaseActivity {

    Logger logger = Logger.getLogger(LauncherActivity.class);
    @ViewInject(R.id.btn_serial1)
    private Button serialportBtn;
    @ViewInject(R.id.btn_serial2)
    private Button closePortBtn;

    //每一个串口就重新new一个这种对象
    private SerialPortUtils ttymxc2 = new SerialPortUtils("/dev/ttymxc2",9600);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //打开串口按钮，打开串口并发送信息
        serialportBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ttymxc2.openSerialPort();
                //设置监听的类
                ttymxc2.setOnDataReceiveListener(new Recived());
                ttymxc2.sendSerialPort("hahahah");
                Log.d("HAHA", "点击了啊");

            }
        });
        //关闭串口，注意串口不能重复打开，要先关闭再开
        closePortBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ttymxc2.closeSerialPort();
            }
        });
    }

    //监听类，收到串口数据后会进入这个方法
    class Recived implements SerialPortUtils.OnDataReceiveListener{

        @Override
        public void onDataReceive(byte[] buffer, int size) {
            try {
                //休眠一段时间再读，防止拆包，时间自己掌控，一般几百毫秒就够了。
                Thread.sleep(600);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("串口收到消息");
            logger.info(new String(buffer));
        }
    }
}
