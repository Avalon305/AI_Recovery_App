package com.bdl.airecovery.activity;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Intent;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.R;
import com.bdl.airecovery.appEnum.LoginResp;
import com.bdl.airecovery.base.BaseActivity;
import com.bdl.airecovery.bluetooth.CommonCommand;
import com.bdl.airecovery.bluetooth.CommonMessage;
import com.bdl.airecovery.constant.MotorConstant;
import com.bdl.airecovery.contoller.MotorProcess;
import com.bdl.airecovery.contoller.Reader;
import com.bdl.airecovery.contoller.Writer;
import com.bdl.airecovery.dialog.CommonDialog;
import com.bdl.airecovery.dialog.LargeDialogHelp;
import com.bdl.airecovery.dialog.MediumDialog;
import com.bdl.airecovery.entity.Upload;
import com.bdl.airecovery.service.BluetoothService;
import com.bdl.airecovery.service.CardReaderService;
import com.bdl.airecovery.util.MessageUtils;
import com.google.gson.Gson;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

import org.xutils.common.util.LogUtil;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.Timer;
import java.util.TimerTask;

import static com.bdl.airecovery.contoller.Writer.setInitialBounce;
import static com.bdl.airecovery.contoller.Writer.setKeepArmTorque;
import static com.bdl.airecovery.contoller.Writer.setParameter;
import static java.lang.Math.abs;

@ContentView(R.layout.activity_mode_standard)
public class StandardModeActivity extends BaseActivity {
    //TODO:发送训练结果,顺反向力存入设置表


    //TODO:电机相关
    private int num = 0;
    private int positiveTorqueLimited; //顺向力
    private int negativeTorqueLimited; //反向力
    private int frontLimitedPosition; //前方限制
    private int rearLimitedPosition; //后方限制
    private int deviceType; //设备类型
    private boolean allowRecordNum = true; //允许计数
    //double rate = MyApplication.getCurrentRate();
    private eStopBroadcastReceiver eStopReceiver; //急停广播
    int motorDirection = MyApplication.getInstance().motorDirection;
    private Handler countHandler = new Handler() { //次数handler
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int arg1 = msg.arg1;
            switch (msg.what) {
                case 1:
                    getnumber.setText(String.valueOf(arg1));
                    break;
            }
        }
    };
    //TODO:电机相关
    private boolean needAfterMotion = true;


    /**
     * 类成员
     */
    private int flag_dialog;                //警告模态框弹出标志位
    private locationReceiver LocationReceiver = new locationReceiver();       //广播监听类
    private IntentFilter filterHR = new IntentFilter();                       //广播过滤器
    private Thread seekBarThread;           //电机速度与位移的SeekBar线程
    private float lastPosition, curPosition; //上一次电机位置、当前电机位置
    private float lastSpeed = -1, curSpeed; //上一次电机速度，当前电机速度
    Timer timer = new Timer();
    private Upload upload = new Upload();
    private BluetoothReceiver bluetoothReceiver;        //蓝牙广播接收器，监听用户的登录广播
    private double weight;

    /**
     * 获取控件
     */
    //TextView
    @ViewInject(R.id.tv_ms_person)
    private TextView person;        //用户名
    @ViewInject(R.id.tv_ms_getrate)
    private TextView getrate;        //心率
    @ViewInject(R.id.tv_ms_getnumber)
    private TextView getnumber;     //次数
    @ViewInject(R.id.tv_ms_gettime)
    private TextView gettime;        //倒计时
    @ViewInject(R.id.tv_ms_positivenumber)
    private TextView positivenumber; //顺向力值
    @ViewInject(R.id.tv_ms_inversusnumber)
    private TextView inversusnumber; //反向力值
    @ViewInject(R.id.tv_ms_time)
    private TextView tv_ms_time;
    @ViewInject(R.id.tv_heart_analyze)
    private TextView tv_heart_analyze; //心率分析
    private TextView medium_dialog_msg;     //最后5秒模态框 时间文本
    //ImageView
    @ViewInject(R.id.iv_ms_positiveplus)
    private ImageView positiveplus;  //顺向力“+”
    @ViewInject(R.id.iv_ms_positiveminus)
    private ImageView positiveminus;  //顺向力“-”
    @ViewInject(R.id.iv_ms_inversusplus)
    private ImageView inversusplus;  //反向力“+”
    @ViewInject(R.id.iv_ms_inversusminus)
    private ImageView inversusminus;  //反向力“-”
    @ViewInject(R.id.iv_ms_help)
    private ImageView iv_ms_help;     //“帮助”图片按钮
    @ViewInject(R.id.iv_ms_state)
    private ImageView iv_ms_state;//登陆头像
    //Button
    @ViewInject(R.id.btn_ms_end)
    private Button btn_ms_end;     //“教练协助/停止协助”按钮
    @ViewInject(R.id.btn_ms_pause)
    private Button btn_ms_pause;   //“暂停”按钮
    //MySeekBar
    @ViewInject(R.id.sp_ms_speed)
    private com.bdl.airecovery.widget.MySeekBar sp_speed;//速度seekbar
    @ViewInject(R.id.sp_ms_scope)
    private com.bdl.airecovery.widget.MySeekBar sp_scope;//活动范围seekbar


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        needAfterMotion = true;
        initImmersiveMode(); //隐藏状态栏，导航栏
        initMotor(); //电机参数初始化
        queryDeviceParam();  //查询设备参数
        queryUserInfo();     //查询用户信息
        iv_ms_help_onClick();//帮助图片的点击事件（使用xUtils框架会崩溃）

        chooseDeviceType(); //选择设备类型
        //注册蓝牙用监听器
        bluetoothReceiver = new BluetoothReceiver();
        IntentFilter intentFilter = new IntentFilter("com.bdl.bluetoothmessage");
        registerReceiver(bluetoothReceiver, intentFilter);

    }

    /**
     * 心率分析
     * @param currHeartRate
     * @return 分析结果
     */
    private Pair heartRateAnalysis(int currHeartRate) {
        if (MyApplication.getInstance().getUser() == null) {
            return null;
        }
        //最大心率
        int maxRate = MyApplication.getInstance().getUser().getHeartRatemMax();
        //心率分析
        if (currHeartRate >= maxRate*0.7) {
            if (currHeartRate >= (int) maxRate*0.8) {
                if (currHeartRate >= (int) maxRate*0.9) {
                    //90%~100%
                    return new Pair<>("极限心率", "#df6d80");
                } else {
                    //80%~90%
                    return new Pair<>("无氧心率", "#f1c369");
                }
            } else {
                //70%~80%
                return new Pair<>("有氧心率", "#faf97c");
            }
        } else {
            if (currHeartRate >= (int) maxRate*0.6) {
                //60%~70%
                return new Pair<>("燃脂心率", "#bde293");
            } else {
                //50%~60%
                return new Pair<>("热身心率", "#7fd3f8");
            }
        }
    }

    /**
     * 计算卡路里消耗
     */
    private double countEnergy(int count, int force){
        return count * (0.01 * weight + 0.02 * force);
    }

    /**
     * 初始化前方限制、后方限制、设备类型
     */
    private void initMotor() {
        String deviceName = MyApplication.getInstance().getCurrentDevice().getDisplayName();
        deviceType = MyApplication.getInstance().getCurrentDevice().getDeviceType(); //获得设备信息
        if ("腿部内弯机".equals(deviceName) ||
                "蝴蝶机".equals(deviceName) ||
                "反向蝴蝶机".equals(deviceName)) { //只有后方限制的机器
            frontLimitedPosition = MyApplication.getInstance().getCurrentDevice().getMaxLimit() * 10000;
            rearLimitedPosition = MessageUtils.getMappedValue(Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(0).getValue())) * 10000;
        } else {
            frontLimitedPosition = MessageUtils.getMappedValue(Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(0).getValue())) * 10000;
            rearLimitedPosition = MessageUtils.getMappedValue(Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(1).getValue())) * 10000;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //将前后方限制写入变频器
                    switch (deviceType) {
                        case 1: //拉设备
                            Writer.setParameter(frontLimitedPosition / 10000 * 4856, MotorConstant.SET_FRONTLIMIT);
                            Writer.setParameter(rearLimitedPosition / 10000 * 4856, MotorConstant.SET_REARLIMIT);
                            //初始化速度
                            setParameter(MotorConstant.speed, MotorConstant.SET_BACK_SPEED);
                            setParameter(0, MotorConstant.SET_GOING_SPEED);
                            break;
                        case 2: //推设备
                            Writer.setParameter(frontLimitedPosition / 10000 * 4856, MotorConstant.SET_FRONTLIMIT);
                            Writer.setParameter(rearLimitedPosition  / 10000 * 4856, MotorConstant.SET_REARLIMIT);
                            //初始化速度
                            setParameter(MotorConstant.speed, MotorConstant.SET_BACK_SPEED);
                            setParameter(0, MotorConstant.SET_GOING_SPEED);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 选择运动过程
     */
    private void chooseDeviceType() {
        switch (deviceType) {
            case 1: //拉设备
                standardModeProcessByPulling();
                break;
            case 2: //推设备
                standardModeProcessByPushing();
                break;
            case 3: //躯干扭转组合
                if (motorDirection == 1) {
                    MyApplication.getInstance().motorDirection = 2;
                    int[] newLimitedPosition = MessageUtils.recersalLimit(frontLimitedPosition, rearLimitedPosition, 1);
                    frontLimitedPosition = newLimitedPosition[0];
                    rearLimitedPosition = newLimitedPosition[1];
                    standardModeProcessByPushing();
                } else if (motorDirection == 2) {
                    MyApplication.getInstance().motorDirection = 1;
                    int[] newLimitedPosition = MessageUtils.recersalLimit(frontLimitedPosition, rearLimitedPosition, 2);
                    frontLimitedPosition = newLimitedPosition[0];
                    rearLimitedPosition = newLimitedPosition[1];
                    standardModeProcessByPulling();
                }
                break;
        }
    }

    /**
     * 速度、运动范围的SeekBar设置
     * 请求电机线程在onResume开启，在onStop关闭
     * 需要电机的速度范围与电机的位移范围（前后方限制）
     */
    private void SeekBarSetting() {
        final int tenThousand = 10000; //一万
        final int frontLimit = frontLimitedPosition / tenThousand; //前方限制
        final int backLimit = rearLimitedPosition / tenThousand; //后方限制
        sp_scope.setMax(frontLimit - backLimit); //位移范围
        sp_speed.setMax(700); //速度范围
        final int interval = 100; //绘制间隔：100ms
        final int frequency = 20; //过渡动画中100ms内的绘制频率ms
        final int transInterval = interval / frequency;   //过渡动画的绘制间隔：5
        //每200ms获取一次当前电机速度与电机位移
        seekBarThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        //获取推拉位移
                        if (lastPosition == 0 && lastSpeed == -1) {
                            //第一次获取，需要获取两次，才能动画过渡
                            lastPosition = Float.parseFloat(Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION)) / tenThousand - frontLimit;
                            lastSpeed = abs(Float.parseFloat(Reader.getRespData(MotorConstant.READ_ROTATIONAL_SPEED)));
                            Thread.sleep(interval);
                            curPosition = Float.parseFloat(Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION)) / tenThousand - backLimit;
                            curSpeed = abs(Float.parseFloat(Reader.getRespData(MotorConstant.READ_ROTATIONAL_SPEED)));
                        } else {
                            //从第二次开始，将上一次的保存，并再获取一次，进行动画过渡
                            lastPosition = curPosition;
                            lastSpeed = curSpeed;
                            curPosition = Float.parseFloat(Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION)) / tenThousand - backLimit;
                            curSpeed = abs(Float.parseFloat(Reader.getRespData(MotorConstant.READ_ROTATIONAL_SPEED)));
                        }

                        //进度条位置过渡，每5ms更新一次
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    float diffPosition = (curPosition - lastPosition) / frequency; //过渡差值
                                    float diffSpeed = (curSpeed - lastSpeed) / frequency; //过渡差值
                                    sp_scope.setProgress((int) lastPosition);
                                    sp_speed.setProgress((int) lastSpeed);
                                    for (int i = 1; i < frequency; ++i) {
                                        Thread.sleep(transInterval);
                                        lastPosition += diffPosition;
                                        lastSpeed += diffSpeed;
                                        sp_scope.setProgress((int) lastPosition);
                                        sp_speed.setProgress((int) lastSpeed);
                                    }
                                    //最后一帧校准
                                    Thread.sleep(transInterval);
                                    sp_scope.setProgress((int) curPosition);
                                    sp_speed.setProgress((int) curSpeed);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();

                        Thread.sleep(interval + 5);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * 推设备运动过程
     */
    private void standardModeProcessByPushing() {
        final int[] lastLocation = {rearLimitedPosition}; //上一次的位置，初始值为后方限制
        final boolean[] flag = {false}; //计数标志位
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                Message message = countHandler.obtainMessage();
                message.what = 1;
                try {
                    //读取当前位置
                    String currentLocation = Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION);
                    int difference = Integer.parseInt(currentLocation) - lastLocation[0]; //本次位置和上次读到的位置差
                    if (difference > 20000) { //去程
                        setParameter(negativeTorqueLimited, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                        if (Integer.parseInt(currentLocation) >= frontLimitedPosition - 50000) {
                            flag[0] = true;
                        }
                        //更新lastLocation
                        lastLocation[0] = Integer.parseInt(currentLocation);
                    } else if (difference < -20000) { //回程
                        if (Integer.parseInt(currentLocation) <= rearLimitedPosition + 50000) {
                            //次数增加
                            if (flag[0] && allowRecordNum) {
                                num++;
                                message.arg1 = num;
                                countHandler.sendMessage(message);
                            }
                            flag[0] = false;
                        }
                        //更新lastLocation
                        lastLocation[0] = Integer.parseInt(currentLocation);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(timerTask, 0, 50);
    }

    /**
     * 拉设备运动过程
     */
    private void standardModeProcessByPulling() {
        //打开运动过程
        final int[] lastLocation = {frontLimitedPosition}; //上一次的位置，初始值为前方限制
        final boolean[] countFlag = {false};//计数标志位
        //如果出现修改，该位置就改变
        final boolean[] haveStopped = {false};
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    //发送消息到handler
                    Message message = countHandler.obtainMessage();
                    message.what = 1;
                    //读取当前位置
                    int currentSpeed = Math.abs(Integer.valueOf(Reader.getRespData(MotorConstant.READ_ROTATIONAL_SPEED)));
                    String currentLocation = Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION);
                    int difference = Integer.parseInt(currentLocation) - lastLocation[0]; //本次位置和上次读到的位置差
                    if (currentSpeed <= 10 && negativeTorqueLimited < MotorConstant.MIN_BACK_TORQUE) {
                        setParameter(MotorConstant.MIN_BACK_TORQUE, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                        haveStopped[0] = true;
                    }
                    if (difference > 20000) { //回程
                        //超过前方限制
                        if (Integer.valueOf(currentLocation) >= frontLimitedPosition - 50000) {

                            if (haveStopped[0]) { //是否需要恢复反向力量
                                setParameter(negativeTorqueLimited, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                                haveStopped[0] = false;
                            }
                            if (countFlag[0] && allowRecordNum) {
                                num++;
                                message.arg1 = num;
                                countHandler.sendMessage(message);
                            }
                            //无法继续计数和计时
                            countFlag[0] = false;
                        }
                        //更新lastLocation
                        lastLocation[0] = Integer.parseInt(currentLocation);
                    } else if (difference < -20000) {//去程

                        //转速超过500，且与最新的限位比较，如果距离大于20000，则可以继续更改，考虑一些延时的因素
                        if (currentSpeed >= 450 && currentSpeed <= 1000 && Integer.valueOf(currentLocation) > rearLimitedPosition + 50000) {
                            int leads = currentSpeed / 100 - 2; //提前量
                            setParameter(leads * 10000, MotorConstant.SET_LEADS);
                        } else if (currentSpeed > 1000 && Integer.valueOf(currentLocation) > rearLimitedPosition + 50000) {
                            int leads = currentSpeed / 100 + 3; //提前量
                            setParameter(leads * 10000, MotorConstant.SET_LEADS);
                        }
                        //超过后方限制
                        if (Integer.valueOf(currentLocation) < rearLimitedPosition + 50000) {
//                            setParameter(MotorConstant.speed, MotorConstant.SET_BACK_SPEED);
                            setParameter(90 * 100, MotorConstant.SET_POSITIVE_TORQUE_LIMITED1);
                            countFlag[0] = true; //允许计数
                        }
                        //更新lastLocation
                        lastLocation[0] = Integer.parseInt(currentLocation);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(timerTask, 0, 50);
    }


//    /**
//     * 拉设备运动过程
//     */
//    private void standardModeProcessByPulling() {
//        Toast.makeText(StandardModeActivity.this, String.valueOf(rearLimitedPosition) + "  " + String.valueOf(frontLimitedPosition), Toast.LENGTH_LONG).show();
//        final int[] lastLocation = {frontLimitedPosition}; //上一次的位置，初始值为前方限制
//        final int[] count = {0};                           //位置无变化的计时
//        final boolean[] countFlag = {false};//计数标志位
//        final boolean[] allowMax = {true}; //允许力矩Max
//        final boolean[] isOpenSpeedEnable = {false};
//
//        TimerTask timerTask = new TimerTask() {
//            @Override
//            public void run() {
//                try {
//                    //发送消息到handler
//                    Message message = countHandler.obtainMessage();
//                    message.what = 1;
//                    //读取当前位置
//                    String currentLocation = Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION);
//                    Log.e("motor", "当前位置"  + currentLocation);
//                    if (Integer.valueOf(currentLocation) >= rearLimitedPosition + 10 * 10000 &&
//                            Integer.valueOf(currentLocation) <= frontLimitedPosition - 10 * 10000) {
//                        isOpenSpeedEnable[0] = true;
//                    }
//                    if (Integer.valueOf(currentLocation) >= rearLimitedPosition + 20){
//                        allowMax[0] = true;
//                    }
//                    int difference = Integer.parseInt(currentLocation) - lastLocation[0]; //本次位置和上次读到的位置差
//                    if (difference > 20000) { //去程
//
//                        count[0] = 0; //重新计数
//                        //重新设置顺向力
//                        setParameter(positiveTorqueLimited, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
//                        //超过前方限制
//                        if (Integer.parseInt(currentLocation) >= frontLimitedPosition) {
//                            Log.e("----------------------","超过前方限制");
//                            //反向力矩限幅Max
//                            setParameter(negativeTorqueLimited + 30 * 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
//                            //取消返回速度
//                            setParameter(0, MotorConstant.ZERO_SPEED_AND_BACK_SPEED_CHANGE);
//                            Log.e("----------------------", "顺向力矩max");
//                            if (countFlag[0] && allowRecordNum) {
//                                num++;
//                                message.arg1 = num;
//                                countHandler.sendMessage(message);
//                            }
//                            //无法继续计数和计时
//                            isOpenSpeedEnable[0] = false;
//                            countFlag[0] = false;
//                        }
//                        //更新lastLocation
//                        lastLocation[0] = Integer.parseInt(currentLocation);
//                    } else if (difference < -20000) {//回程
//                        count[0] = 0; //重新计数
//                        //重新设置反向力
//                        setParameter(negativeTorqueLimited, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
//                        //超过后方限制
//                        if (Integer.parseInt(currentLocation) <= rearLimitedPosition && allowMax[0])  {
//                            Log.e("----------------------","超过后方限制");
//                            //正向力矩限幅Max
//                            setParameter(positiveTorqueLimited + 40 * 100, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
//                            //打开返回速度
//                            setParameter(1, MotorConstant.ZERO_SPEED_AND_BACK_SPEED_CHANGE);
//                            Log.e("----------------------", "顺向力矩max");
//                            allowMax[0] = false;
//                            countFlag[0] = true; //允许计数
//                        }
//                        //更新lastLocation
//                        lastLocation[0] = Integer.parseInt(currentLocation);
//                    } else if (difference >= -20000 && difference <= 20000 && isOpenSpeedEnable[0]) { //位置无变化
//                        if (count[0] >= 2) { //如果1.5s以上位置无变化
//                            setParameter(1, MotorConstant.ZERO_SPEED_AND_BACK_SPEED_CHANGE);
//                        }
//                        count[0]++;
//                        //更新lastLocation
//                        lastLocation[0] = Integer.parseInt(currentLocation);
//
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        };
//        timer.schedule(timerTask, 0, 50);
//    }

    LargeDialogHelp helpDialog;

    /**
     * 帮助图片的点击事件
     */
    private void iv_ms_help_onClick() {
        iv_ms_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //新建一个跳转到帮助界面Activity的显式意图
                helpDialog = new LargeDialogHelp(StandardModeActivity.this);
                helpDialog.setMachineName(MyApplication.getInstance().getCurrentDevice().getDisplayName());
                helpDialog.setUseNote(MyApplication.getInstance().getCurrentDevice().getHelpWord());
                helpDialog.setPositiveBtnText("关闭");
                //helpDialog.setMachineView(getResources().getIdentifier(MyApplication.getInstance().getCurrentDevice().getGeneralImg(),"drawable",getPackageName()));
                //“继续”按钮 监听，点击跳转到界面
                helpDialog.setOnPositiveClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        helpDialog.dismiss();
                    }
                });
                helpDialog.show();
                helpDialog.setCanceledOnTouchOutside(true);
                final ImageView large_help_img = helpDialog.findViewById(R.id.large_help_img);
                large_help_img.setImageResource(getResources().getIdentifier(MyApplication.getInstance().getCurrentDevice().getGeneralImg(), "drawable", getPackageName()));
                //模态框隐藏导航栏
                helpDialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                helpDialog.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                //布局位于状态栏下方
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                //全屏
//                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                                //隐藏导航栏
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                        if (Build.VERSION.SDK_INT >= 19) {
                            uiOptions |= 0x00001000;
                        } else {
                            uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                        }
                        helpDialog.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
                    }
                });
            }
        });
    }

    /**
     * 当Activity准备好和用户进行交互时，调用onResume()
     * 此时Activity处于【运行状态】
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (seekBarThread == null) {
            SeekBarSetting(); //速度、运动范围的SeekBar设置
            seekBarThread.start();
        }

        //注册广播
        filterHR.addAction("heartrate");
        filterHR.addAction("log");
        registerReceiver(LocationReceiver, filterHR);

        //注册急停广播
        IntentFilter filter = new IntentFilter();
        filter.addAction("E-STOP");
        eStopReceiver = new eStopBroadcastReceiver();
        registerReceiver(eStopReceiver, filter);
    }

    /**
     * 当Activity已经完全不可见时，调用onStop()
     * 此时Activity处于【停止状态】
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (seekBarThread != null) {
            seekBarThread.interrupt();
            seekBarThread = null;
        }
    }

    //卸载广播
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(LocationReceiver);
        unregisterReceiver(eStopReceiver);
        unregisterReceiver(bluetoothReceiver);
        timerLog.cancel();
        timer.cancel(); //结束定时任务
        if (needAfterMotion) {
            MotorProcess.motorInitialization();
        }
    }

    /**
     * 查询当前设备参数
     */
    private void queryDeviceParam() {
        //获取设备信息
        //判空
        if (MyApplication.getInstance().getCurrentDevice() != null) {
            if (MyApplication.getInstance().getCurrentDevice().getReverseForce() != null && MyApplication.getInstance().getCurrentDevice().getConsequentForce() != null) {
                positivenumber.setText(MyApplication.getInstance().getCurrentDevice().getConsequentForce());//顺向力数值
                inversusnumber.setText(MyApplication.getInstance().getCurrentDevice().getReverseForce());   //反向力数值
                //传入电机的值
                switch (deviceType) {
                    case 1: //拉设备
                        positiveTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(positivenumber.getText())) * 100);
                        negativeTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(inversusnumber.getText())) * 100);
                        Log.e("顺向力+反向力", String.valueOf(positiveTorqueLimited )+ String.valueOf(negativeTorqueLimited));
                        break;
                    case 2: //推设备
                        positiveTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(inversusnumber.getText())) * 100);
                        negativeTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(positivenumber.getText())) * 100);
                        Log.e("顺向力+反向力", String.valueOf(positiveTorqueLimited )+ String.valueOf(negativeTorqueLimited));
                        break;
                    case 3:
                        switch (motorDirection) {
                            case 1:
                                positiveTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(inversusnumber.getText())) * 100);
                                negativeTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(positivenumber.getText())) * 100);
                                break;
                            case 2:
                                positiveTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(positivenumber.getText())) * 100);
                                negativeTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(inversusnumber.getText())) * 100);
                                break;
                        }
                        break;
                }

            }
        }
    }

    /**
     * 获取用户信息
     */
    private void queryUserInfo() {
        if (MyApplication.getInstance().getUser() != null) { //判空
            //用户名
            person.setText(MyApplication.getInstance().getUser().getUsername());
            //如果用户是学员，则执行如下逻辑
            if (MyApplication.getInstance().getUser().getRole() != null) {
                if (MyApplication.getInstance().getUser().getRole().equals("trainee")) {
                    //如果为空，说明无教练连接蓝牙
                    if (MyApplication.getInstance().getUser().getHelperuser() == null || MyApplication.getInstance().getUser().getHelperuser().getUsername().equals("")) {
                        btn_ms_end.setText("教练协助"); //更新为“教练协助”按钮
                        iv_ms_state.setImageDrawable(getResources().getDrawable((R.drawable.yonghu1)));

                    } else { //否则，不为空串，说明有教练连接蓝牙
                        btn_ms_end.setText("停止协助"); //更新为“停止协助”按钮
                        iv_ms_state.setImageDrawable(getResources().getDrawable((R.drawable.shou)));

                    }
                } else if (MyApplication.getInstance().getUser().getRole().equals("coach")) {
                    btn_ms_end.setText("医护设置"); //更新为“医护设置”按钮
                    iv_ms_state.setImageDrawable(getResources().getDrawable((R.drawable.guanliyuan1)));
                } else {
                    iv_ms_state.setImageDrawable(getResources().getDrawable((R.drawable.banshou1)));

                }
            }
            weight = MyApplication.getInstance().getUser().getWeight();
        }
    }

    //扫描教练的定时任务
    Timer timerLog = new Timer();
    TimerTask taskLog = new TimerTask() {
        @Override
        public void run() {
            Intent intent = new Intent(StandardModeActivity.this, BluetoothService.class);
            intent.putExtra("command", CommonCommand.SECOND__LOGIN.value());
            startService(intent);

        }
    };

    /**
     * “教练协助/停止协助”按钮
     * 如果是调试模式，显示“停止协助”，点击会将HelpUser置为空串，然后跳转到主界面
     * 如果不是调制模式，显示“教练协助”，点击事件与主界面一致（连接教练蓝牙）
     */
    @Event(R.id.btn_ms_end)
    private void endClick(View v) {
        //如果是教练协助
        if (btn_ms_end.getText() == "教练协助") {
            btn_ms_end.setText("教练协助...");
            Intent intent2 = new Intent(StandardModeActivity.this, CardReaderService.class);
            intent2.putExtra("command", CommonCommand.SECOND__LOGIN.value());
            startService(intent2);
            timerLog.schedule(taskLog, 0, 2000);
            Log.d("StandardModeActivity", "request to login");
        }
        //如果是停止协助
        else if (btn_ms_end.getText() == "停止协助") {
            Intent intent2 = new Intent(StandardModeActivity.this, CardReaderService.class);
            intent2.putExtra("command", CommonCommand.SECOND__LOGOUT.value());
            startService(intent2);
            Intent intent = new Intent(StandardModeActivity.this, BluetoothService.class);
            intent.putExtra("command", CommonCommand.SECOND__LOGOUT.value());
            startService(intent);
            Log.d("StandardModeActivity", "request to logout");
        }
        //如果是医护设置
        else if (btn_ms_end.getText() == "医护设置") {
            //跳转医护设置界面
            Intent intent = new Intent(StandardModeActivity.this, PersonalSettingActivity.class); //新建一个跳转到医护设置界面Activity的显式意图
            startActivity(intent); //启动
            StandardModeActivity.this.finish(); //结束当前Activity
        }
    }

    CommonDialog commonDialog;

    /**
     * 暂停按钮    暂停后不计个数 TODO：发送训练结果（要发送的内容）
     */
    @Event(R.id.btn_ms_pause)
    private void pauseClick(View v) {
        allowRecordNum = false;
        commonDialog = new CommonDialog(StandardModeActivity.this);
        commonDialog.setTitle("温馨提示");
        commonDialog.setMessage("您确定要放弃本次训练吗？");
        commonDialog.setNegativeBtnText("结束");
        commonDialog.setPositiveBtnText("继续");
        //“结束”按钮 监听，点击跳转到待机界面
        commonDialog.setOnNegativeClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //请求退出登录
                Intent intentLog2 = new Intent(StandardModeActivity.this, CardReaderService.class);
                intentLog2.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
                startService(intentLog2);
                Intent intentLog = new Intent(StandardModeActivity.this, BluetoothService.class);
                intentLog.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
                startService(intentLog);
                Log.d("StandardModeActivity", "request to logout");

                commonDialog.dismiss();
                //新建一个跳转到待机界面Activity的显式意图
                Intent intent = new Intent(StandardModeActivity.this, LoginActivity.class);
                //启动
                startActivity(intent);
                //结束当前Activity
                StandardModeActivity.this.finish();
            }
        });
        //“继续”按钮 监听
        commonDialog.setOnPositiveClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commonDialog.dismiss();
                allowRecordNum = true;
            }
        });
        //模态框隐藏导航栏
        commonDialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        commonDialog.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        //布局位于状态栏下方
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        //全屏
//                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        //隐藏导航栏
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                if (Build.VERSION.SDK_INT >= 19) {
                    uiOptions |= 0x00001000;
                } else {
                    uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                }
                commonDialog.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
            }
        });
        commonDialog.show();

    }

    //运动过程中改变顺反向力
    public void changeTorque() {
        switch (deviceType) {
            case 1: //拉设备
                positiveTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(positivenumber.getText())) * 100);
                negativeTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(inversusnumber.getText())) * 100);
                break;
            case 2: //推设备
                positiveTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(positivenumber.getText())) * 100);
                negativeTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(inversusnumber.getText())) * 100);
                break;
            case 3:
                switch (motorDirection) {
                    case 1:
                        positiveTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(positivenumber.getText())) * 100);
                        negativeTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(inversusnumber.getText())) * 100);
                        break;
                    case 2:
                        positiveTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(positivenumber.getText())) * 100);
                        negativeTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(inversusnumber.getText())) * 100);
                        break;
                }
        }

        //改变变频器中的值
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    setParameter(positiveTorqueLimited, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
                    setParameter(negativeTorqueLimited, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                    setInitialBounce(negativeTorqueLimited + MotorConstant.DIF_BETWEEN_NEG_TORQUE_AND_BOUNCE);
                    setKeepArmTorque(positiveTorqueLimited);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //顺向力的“+”
    @Event(R.id.iv_ms_positiveplus)
    private void setIv_ms_positiveplus_onClick(View v) {
        //显示
        if (Integer.valueOf(positivenumber.getText().toString()) < 99 && Integer.valueOf(inversusnumber.getText().toString()) < 99) {
            positivenumber.setText(Integer.valueOf(positivenumber.getText().toString()) + 1 + "");
            inversusnumber.setText(Integer.valueOf(inversusnumber.getText().toString()) + 1 + "");
            MyApplication.getInstance().getCurrentDevice().setReverseForce(inversusnumber.getText().toString());
            MyApplication.getInstance().getCurrentDevice().setConsequentForce(positivenumber.getText().toString());
        }
        changeTorque();
        //TODO:存入PERSONALINFO
    }

    //顺向力的“-”
    @Event(R.id.iv_ms_positiveminus)
    private void setIv_ms_positiveminus_onClick(View v) {
        //显示
        if (Integer.valueOf(positivenumber.getText().toString()) > 5) {
            positivenumber.setText(Integer.valueOf(positivenumber.getText().toString()) - 1 + "");
            inversusnumber.setText(Integer.valueOf(inversusnumber.getText().toString()) - 1 + "");
            MyApplication.getInstance().getCurrentDevice().setReverseForce(inversusnumber.getText().toString());
            MyApplication.getInstance().getCurrentDevice().setConsequentForce(positivenumber.getText().toString());
        }
        changeTorque();
        //TODO:存入PERSONALINFO
    }

    //反向力的“+”
    @Event(R.id.iv_ms_inversusplus)
    private void setIv_ms_inversusplus_onClick(View v) {
        //显示
        if (Integer.valueOf(inversusnumber.getText().toString()) < 99) {
            inversusnumber.setText(Integer.valueOf(inversusnumber.getText().toString()) + 1 + "");
            MyApplication.getInstance().getCurrentDevice().setReverseForce(inversusnumber.getText().toString());
        }
        //警告模态框
        if (Integer.valueOf(inversusnumber.getText().toString()) > Integer.valueOf(positivenumber.getText().toString()) * 1.5) {
            //弹出50%
            if (flag_dialog != 2) {
                flag_dialog = 2;
                LaunchDialogAlert();
            }
        } else if (Integer.valueOf(inversusnumber.getText().toString()) > Integer.valueOf(positivenumber.getText().toString()) * 1.3) {
            //弹出30%
            if (flag_dialog != 1 && flag_dialog != 2) {
                flag_dialog = 1;
                LaunchDialogAlert();
            }
        }
        changeTorque();
        //TODO:存入PERSONALINFO
    }

    //反向力的“-”
    @Event(R.id.iv_ms_inversusminus)
    private void setIv_ms_inversusminus_onClick(View v) {
        //显示
        if (Integer.valueOf(inversusnumber.getText().toString()) > Integer.valueOf(positivenumber.getText().toString())) {
            inversusnumber.setText(Integer.valueOf(inversusnumber.getText().toString()) - 1 + "");
            MyApplication.getInstance().getCurrentDevice().setReverseForce(inversusnumber.getText().toString());
        }
        //模态框标志位
        if (Integer.valueOf(inversusnumber.getText().toString()) <= Integer.valueOf(positivenumber.getText().toString()) * 1.3)
            flag_dialog = 0;
        else if (Integer.valueOf(inversusnumber.getText().toString()) <= Integer.valueOf(positivenumber.getText().toString()) * 1.5)
            flag_dialog = 1;
        changeTorque();
        //TODO:存入PERSONALINFO
    }

    /**
     * 启动超负荷警告模态框
     */
    public void LaunchDialogAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(StandardModeActivity.this);
        //加载警示视图
        View viewalert = View.inflate(StandardModeActivity.this, R.layout.dialog_mode_alert, null);
        builder
                .setView(viewalert);
        final AlertDialog dialog = builder.create();
        //模态框隐藏导航栏
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialogshape);   //设置dialog的形状
        dialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        dialog.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        //布局位于状态栏下方
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        //全屏
//                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        //隐藏导航栏
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                if (Build.VERSION.SDK_INT >= 19) {
                    uiOptions |= 0x00001000;
                } else {
                    uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                }
                dialog.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
            }
        });
        //设置点击Dialog外部任意区域关闭Dialog
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        //设置模态框内参数
        TextView text_positive = (TextView) dialog.findViewById(R.id.tv_alert_getpositive);
        TextView text_inversus = (TextView) dialog.findViewById(R.id.tv_alert_getinversus);
        TextView text_high = (TextView) dialog.findViewById(R.id.tv_alert_highvalue);
        String value_positive = positivenumber.getText().toString();
        String value_inversus = inversusnumber.getText().toString();
        String value_high = String.valueOf((Integer.valueOf(value_inversus) - Integer.valueOf(value_positive)) * 100 / Integer.valueOf(value_positive));
        text_positive.setText(value_positive);
        text_inversus.setText(value_inversus);
        text_high.setText(value_high);
        //获取模态框中的确定按钮
        QMUIRoundButton btn_password_confirm = dialog.findViewById(R.id.btn_password_confirm);
        //“确定”按钮监听事件
        btn_password_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        //取得屏幕尺寸、设置模态框宽高
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = (int) (size.x * 0.7);
        lp.height = (int) (size.y * 0.6);
        window.setAttributes(lp);
    }

    MediumDialog mediumDialog;

    /**
     * 最后5秒倒计时 模态框
     */
    private void Last5sAlertDialog() {
        mediumDialog = new MediumDialog((StandardModeActivity.this));
        mediumDialog.setTime("0:05");
        //模态框隐藏导航栏
        mediumDialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        mediumDialog.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        //布局位于状态栏下方
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        //全屏
//                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        //隐藏导航栏
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                if (Build.VERSION.SDK_INT >= 19) {
                    uiOptions |= 0x00001000;
                } else {
                    uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                }
                mediumDialog.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
            }
        });
        mediumDialog.show();

        //找到CommonDialog内容控件
        medium_dialog_msg = mediumDialog.findViewById(R.id.medium_dialog_time);
    }

    /**
     * 隐藏状态栏，导航栏
     */
    @SuppressLint("NewApi")
    private void initImmersiveMode() {
        if (Build.VERSION.SDK_INT >= 19) {
            View.OnSystemUiVisibilityChangeListener listener = new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        enterImmersiveMode();
                    }
                }
            };
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(listener);
            enterImmersiveMode();
        }
    }

    @SuppressLint("NewApi")
    private void enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    /**
     * 接收广播
     */
    private class eStopBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra("state");
            if (state != null && state.equals("1")) {
                startActivity(new Intent(StandardModeActivity.this, ScramActivity.class));
                StandardModeActivity.this.finish();
            }
        }
    }

    /**
     * 广播接收类
     */
    public class locationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            try {
                if (intentAction.equals("heartrate")) {
                    getrate.setText(intent.getStringExtra("heartrate"));
                } else if (intentAction.equals("log")) {
                    if (intent.getStringExtra("log").equals(LoginResp.LOCALTWOLOGICARD.getStr()) || intent.getStringExtra("log").equals(LoginResp.LOCALTWOLOGIBLUE.getStr())
                            || intent.getStringExtra("log").equals(LoginResp.REMOTETWOLOGIBLUE.getStr())
                            || intent.getStringExtra("log").equals(LoginResp.REMOTETWOLOGICARD.getStr())) {
                        //如果连接成功，跳转医护设置界面
                        Log.e("StandardModeActivity", "login successfully");
                        Intent activityintent = new Intent(StandardModeActivity.this, PersonalSettingActivity.class); //新建一个跳转到医护设置界面Activity的显式意图
                        startActivity(activityintent); //启动
                        StandardModeActivity.this.finish(); //结束当前Activity
                    } else if (intent.getStringExtra("log").equals(LoginResp.LOCALTWOLOGOCARD.getStr()) || intent.getStringExtra("log").equals(LoginResp.LOCALTWOLOGOBLUE.getStr())
                            || intent.getStringExtra("log").equals(LoginResp.REMOTETWOLOGOBLUE.getStr())
                            || intent.getStringExtra("log").equals(LoginResp.REMOTETWOLOGOCARD.getStr())) {
                        Log.e("StandardModeActivity", "logout successfully");
                        Intent activityintent = new Intent(StandardModeActivity.this, MainActivity.class); //新建一个跳转到主界面Activity的显式意图
                        startActivity(activityintent); //启动
                        StandardModeActivity.this.finish(); //结束当前Activity
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 蓝牙广播专用接收器
     */
    private class BluetoothReceiver extends BroadcastReceiver {

        private Gson gson = new Gson();

        private CommonMessage transfer(String json) {
            return gson.fromJson(json, CommonMessage.class);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String messageJson = intent.getStringExtra("message");
            CommonMessage commonMessage = transfer(messageJson);
            switch (commonMessage.getMsgType()) {
                //第一用户登录成功
                case CommonMessage.FIRST__LOGIN_REGISTER_OFFLINE:
                case CommonMessage.FIRST__LOGIN_REGISTER_ONLINE:
                case CommonMessage.FIRST__LOGIN_SUCCESS_OFFLINE:
                case CommonMessage.FIRST__LOGIN_SUCCESS_ONLINE:
                    LogUtil.d("广播接收器收到：" + commonMessage.toString());
                    break;
                //第一用户下线成功
                case CommonMessage.FIRST__LOGOUT:
                case CommonMessage.FIRST__DISCONNECTED:
                    LogUtil.d("广播接收器收到：" + commonMessage.toString());
                    break;
                //第二用户登录成功
                case CommonMessage.SECOND__LOGIN_SUCCESS_OFFLINE:
                case CommonMessage.SECOND__LOGIN_SUCCESS_ONLINE:
                    LogUtil.d("广播接收器收到：" + commonMessage.toString());
                    //如果连接成功，跳转医护设置界面
                    Log.e("MainActivity", "login successfully");
                    Intent activityintent = new Intent(StandardModeActivity.this, PersonalSettingActivity.class); //新建一个跳转到医护设置界面Activity的显式意图
                    startActivity(activityintent); //启动
                    StandardModeActivity.this.finish(); //结束当前Activity
                    break;
                //第二用户下线成功
                case CommonMessage.SECOND__DISCONNECTED:
                case CommonMessage.SECOND__LOGOUT:
                    LogUtil.d("广播接收器收到：" + commonMessage.toString());
                    //FIXME 刷新主界面
                    needAfterMotion = false;
                    StandardModeActivity.this.recreate();
                    break;
                //获得心率
                case CommonMessage.HEART_BEAT:
                    LogUtil.d("广播接收器收到：" + commonMessage.toString());
                    getrate.setText(commonMessage.getAttachment()); //心率数值
                    //心率分析
                    Pair<String, String> res = heartRateAnalysis(Integer.parseInt(commonMessage.getAttachment()));
                    if (res != null) {
                        tv_heart_analyze.setText(res.first);
                        tv_heart_analyze.setTextColor(Color.parseColor(res.second));
                    }
                    break;
                default:
                    LogUtil.e("未知广播，收到message：" + commonMessage.getMsgType());
            }
        }
    }
}
