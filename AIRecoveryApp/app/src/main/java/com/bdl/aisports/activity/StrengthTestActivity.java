package com.bdl.aisports.activity;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.bdl.aisports.R;
import com.bdl.aisports.base.BaseActivity;
import com.bdl.aisports.constant.MotorConstant;
import com.bdl.aisports.contoller.MotorProcess;
import com.bdl.aisports.contoller.Reader;

import org.xutils.view.annotation.ContentView;

import java.util.Timer;
import java.util.TimerTask;

import static com.bdl.aisports.contoller.Reader.getRespData;
import static com.bdl.aisports.contoller.Writer.setParameter;

@ContentView(R.layout.activity_strength_test)
public class StrengthTestActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            strengthTest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        MotorProcess.motorInitialization();
        super.onDestroy();
    }

    /**
     * 肌力测试过程
     * @throws Exception
     */
    public void strengthTest() throws Exception {
        //测试之前的定位
        MotorProcess.motorDirection(110);
        //力矩设置
        setParameter(100 * 100, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    getRespData(MotorConstant.READ_TORQUE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(timerTask, 0, 10);
    }

}
