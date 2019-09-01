package com.bdl.airecovery.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.transition.CircularPropagation;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.R;
import com.bdl.airecovery.activity.MainActivity;
import com.bdl.airecovery.base.BaseActivity;
import com.bdl.airecovery.constant.MotorConstant;
import com.bdl.airecovery.contoller.MotorProcess;
import com.bdl.airecovery.contoller.Reader;
import com.bdl.airecovery.dialog.CommonDialog;
import com.bdl.airecovery.dialog.LargeDialog;
import com.bdl.airecovery.entity.DTO.StrengthTest;
import com.bdl.airecovery.entity.TempStorage;
import com.bdl.airecovery.entity.Upload;
import com.bdl.airecovery.proto.BdlProto;
import com.bdl.airecovery.service.StaticMotorService;
import com.bdl.airecovery.widget.CircularRingPercentageView;
import com.google.gson.Gson;

import org.apache.log4j.lf5.viewer.categoryexplorer.CategoryImmediateEditor;
import org.xutils.DbManager;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

import io.netty.handler.codec.spdy.SpdyHttpResponseStreamIdHandler;

import static com.bdl.airecovery.contoller.Reader.getRespData;
import static com.bdl.airecovery.contoller.Writer.setParameter;

@ContentView(R.layout.activity_strength_test)
public class StrengthTestActivity extends BaseActivity {

    @ViewInject(R.id.btn_st_start)
    private Button btnStartTest;

    @ViewInject(R.id.btn_st_end)
    private Button btnEndTest;

    @ViewInject(R.id.tv_st_tip)
    private TextView tvTip;

    private LargeDialog dialog_locating;
    private int count = 0;
    private int strength = 0;
    private int maxStrength = 0;
    private int resultGrade = 0;
    private CircularRingPercentageView circularRingPercentageView;
    private DbManager db = MyApplication.getInstance().getDbManager();
    Timer timer = new Timer();
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int arg1 = msg.arg1;
            switch (msg.what) {
                case 1:
                    showCommonDialog();
                    uploadResult();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        circularRingPercentageView = (CircularRingPercentageView) findViewById(R.id.process_circle);
//        try {
//            LaunchDialogLocating();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }

    @Override
    protected void onDestroy() {
        MotorProcess.motorInitialization();
        super.onDestroy();
    }


    /**
     * 将测试结果存暂存表
     */
    private void uploadResult() {
        //获取当前时间
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = dateFormat.format(date);
        StrengthTest strengthTest = new StrengthTest();
        strengthTest.setUid(MyApplication.getInstance().getUser().getUserId());
        strengthTest.setResult(ratingByResult(maxStrength));
        strengthTest.setTime(currentTime);

        //存暂存表
        TempStorage tempStorage = new TempStorage();
        Gson gson = new Gson();
        tempStorage.setData(gson.toJson(strengthTest)); //重传数据（转换为JSON串）
        tempStorage.setType(3); //重传类型
        try {
            db.saveBindingId(tempStorage);
        } catch (DbException e) {
            e.printStackTrace();
        }

    }
    @Event(R.id.btn_st_end)
    private void setEndTestOnClick(View v) {
        Message message = mHandler.obtainMessage();
        message.what = 1;
        message.arg1 = 1;
        mHandler.sendMessage(message);
        timer.cancel();
    }
    //点击事件
    @Event(R.id.btn_st_start)
    private void setStartTestOnClick(View v) {
        btnStartTest.setEnabled(false);
        btnEndTest.setEnabled(true);
        tvTip.setText("\n    请用力拉动力臂");
        tvTip.setTextColor(0xffe67e22);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    strength = Integer.parseInt(getRespData(MotorConstant.READ_TORQUE));
                    if (strength > maxStrength) { //获取最大力矩
                        maxStrength = strength;
                        resultGrade = resultGrade(maxStrength);
                    }
                    circularRingPercentageView.setProgress(strength / 5);
                    count++;
                    if (count >= 1000) {
                        Message message = mHandler.obtainMessage();
                        message.what = 1;
                        message.arg1 = 1;
                        mHandler.sendMessage(message);
                        timer.cancel();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(timerTask, 0, 50);
    }

    private String ratingByResult(int result) {
        double k = 5.0 / 60.0;
        double ratedResult = k * result + 3.33;
        ratedResult = (double) Math.round(ratedResult * 100) / 100;
        return String.valueOf(ratedResult);
    }

    private int resultGrade(int result) {
        if (result > 0 && result <= 20) {
            return 1;
        } else if (result > 20 && result <= 80) {
            return 2;
        } else if (result > 80 && result <= 140) {
            return 3;
        } else if (result > 140 && result <= 200) {
            return 4;
        } else if (result > 200) {
            return 5;
        }
        return 0;
    }

    private void showCommonDialog() {
        final CommonDialog commonDialog = new CommonDialog(StrengthTestActivity.this);
        commonDialog.setTitle("测试结果");
        commonDialog.setMessage("您在肌力测试中使用的最大力量为" + ratingByResult(maxStrength) + "N，肌力测试评级为" + resultGrade + "级");
        commonDialog.setCanceledOnTouchOutside(true);
        commonDialog.setOnPositiveClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commonDialog.dismiss();
                StrengthTestActivity.this.finish();
            }
        });
        commonDialog.show();
    }

    //打开定位模态框
    private void LaunchDialogLocating() throws Exception {
        dialog_locating = new LargeDialog(StrengthTestActivity.this);
        //显示定位模态框
        dialog_locating.setTitle("注意");
        dialog_locating.setMessage("杠杆正在定位到开始的位置...\n" +
                "请勿阻碍杠杆运动！");
        dialog_locating.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        dialog_locating.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        //布局位于状态栏下方
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        //全屏
                        //View.SYSTEM_UI_FLAG_FULLSCREEN |
                        //隐藏导航栏
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                if (Build.VERSION.SDK_INT >= 19) {
                    uiOptions |= 0x00001000;
                } else {
                    uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                }
                dialog_locating.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
            }
        });
        dialog_locating.setCanceledOnTouchOutside(false);
        dialog_locating.show();

        //定位
        setParameter(0, MotorConstant.SET_BACK_SPEED);
        //开启来程速度
        setParameter(-500, MotorConstant.SET_GOING_SPEED);
        setParameter(-500, MotorConstant.SET_COMPARE_SPEED);
        final Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    //读取当前位置
                    String currentPosition = Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION);
                    if (currentPosition == null) { //读取失败
                        return;
                    }
                    if (Integer.parseInt(currentPosition) <= 110 * 10000) { //比较当前位置和初始位置
                        setParameter((Integer.valueOf(currentPosition) / 10000 - 10) * 4856, MotorConstant.SET_FRONTLIMIT);
                        //设置去程速度为0
                        setParameter(0, MotorConstant.SET_GOING_SPEED);
                        setParameter(0, MotorConstant.SET_COMPARE_SPEED);
                        setParameter(100 * 100, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
                        //关闭返回速度
                        timer.cancel(); //关闭当前定时轮询任务
                        dialog_locating.dismiss();//隐藏定位模态框
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(timerTask, 0, 50);

    }

}
