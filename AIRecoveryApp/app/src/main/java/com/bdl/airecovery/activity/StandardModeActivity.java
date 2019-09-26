package com.bdl.airecovery.activity;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.bdl.airecovery.dialog.RatingDialog;
import com.bdl.airecovery.dialog.SmallPwdDialog;
import com.bdl.airecovery.entity.CalibrationParameter;
import com.bdl.airecovery.entity.DTO.ErrorMsg;
import com.bdl.airecovery.entity.Setting;
import com.bdl.airecovery.entity.TempStorage;
import com.bdl.airecovery.entity.Upload;
import com.bdl.airecovery.service.BluetoothService;
import com.bdl.airecovery.service.CardReaderService;
import com.bdl.airecovery.util.MessageUtils;
import com.google.gson.Gson;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.popup.QMUIPopup;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

import org.xutils.DbManager;
import org.xutils.common.util.LogUtil;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.bdl.airecovery.contoller.Writer.setInitialBounce;
import static com.bdl.airecovery.contoller.Writer.setKeepArmTorque;
import static com.bdl.airecovery.contoller.Writer.setParameter;
import static java.lang.Math.abs;

@ContentView(R.layout.activity_mode_standard)
public class StandardModeActivity extends BaseActivity {
    //TODO:发送训练结果,顺反向力存入设置表


    //TODO:电机相关
    private int num = 0; //次数
    private int positiveTorqueLimited; //顺向力
    private int negativeTorqueLimited; //反向力
    private int frontLimitedPosition; //前方限制
    private int rearLimitedPosition; //后方限制
    private int deviceType; //设备类型
    private boolean allowRecordNum = true; //允许计数
    private String errorID; //错误ID
    private CommonDialog errorDialog; //错误提示框
    //double rate = MyApplication.getCurrentRate();
    private eStopBroadcastReceiver eStopReceiver; //急停广播
    int motorDirection = MyApplication.getInstance().motorDirection;
    private Handler countHandler = new Handler() { //次数handler
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.arg1 == 0) {
                return;
            }
            if (msg.arg1 % MyApplication.getInstance().getUser().getGroupNum() == 0) {
                currGroupNum = MyApplication.getInstance().getUser().getGroupNum();
                currGroup = msg.arg1 / MyApplication.getInstance().getUser().getGroupNum();
            } else {
                currGroupNum = msg.arg1 % MyApplication.getInstance().getUser().getGroupNum();
                currGroup = 1 + msg.arg1 / MyApplication.getInstance().getUser().getGroupNum();
            }

            if (currGroup == MyApplication.getInstance().getUser().getGroupCount() &&
                    currGroupNum == MyApplication.getInstance().getUser().getGroupNum()) {
                tv_curr_groupnum.setText(String.valueOf(currGroupNum)); //当前组的次数
                allowRecordNum = false;
                //btn_ms_pause.setText("结束"); //暂停按钮修改为结束按钮
                if (MyApplication.getInstance().getUser().getUsername().equals("体验者")) {
					//打开体验者退出模态框
					openExperiencer();
				} else {
					//弹出评级模态框
					openRatingDialog();
				}
				
                return ;
            }

            tv_curr_groupnum.setText(String.valueOf(currGroupNum)); //当前组的次数
            tv_curr_groupcount.setText(String.valueOf(currGroup)); //当前组

            //如果当前组做完，进入组间休息
            if (currGroupNum == MyApplication.getInstance().getUser().getGroupNum()) {
                canOpenRestDialog = true;
                allowRecordNum = false; //期间不允许计数
                currGroupNum = 0;
                currGroup++;
            }
        }
    };

    private boolean isErrorDialogShow = false;
    private Handler errorDialogHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    if (!isErrorDialogShow) {
                        isErrorDialogShow = true;
                        showErrorDialog();
                    }
                    break;
                case 2:
                    errorDialog.dismiss();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    isErrorDialogShow = false;
                    break;

            }
        }
    };

    private boolean needAfterMotion = true;

    //标定参数
    DbManager db = MyApplication.getInstance().getDbManager(); //获取DbManager对象
    private CalibrationParameter calibrationParameter = null;
    /**
     * 类成员
     */
    private int flag_dialog;                //警告模态框弹出标志位
    private Boolean isAlert = false;        //标识是否弹5s倒计时模态框
    private locationReceiver LocationReceiver = new locationReceiver();       //广播监听类
    private IntentFilter filterHR = new IntentFilter();                       //广播过滤器
    private Thread seekBarThread;           //电机速度与位移的SeekBar线程
    private float lastPosition, curPosition; //上一次电机位置、当前电机位置
    private float lastSpeed = -1, curSpeed; //上一次电机速度，当前电机速度
    Timer timer = new Timer();
    private Upload upload = new Upload();
    private BluetoothReceiver bluetoothReceiver;        //蓝牙广播接收器，监听用户的登录广播
    private double weight;
    private long startTime; //开始时间

    /**
     * 获取控件
     */
    //TextView
    @ViewInject(R.id.tv_ms_person)
    private TextView person;        //用户名
    @ViewInject(R.id.tv_ms_getrate)
    private TextView getrate;        //心率
    @ViewInject(R.id.tv_ms_positivenumber)
    private TextView positivenumber; //顺向力值
    @ViewInject(R.id.tv_ms_inversusnumber)
    private TextView inversusnumber; //反向力值
    @ViewInject(R.id.tv_heart_analyze)
    private TextView tv_heart_analyze; //心率分析
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
    @ViewInject(R.id.iv_heartrate_help)
    private ImageView iv_heartrate_help; //心率区间 帮助按钮
    //Button
    //MySeekBar
    @ViewInject(R.id.sp_ms_speed)
    private com.bdl.airecovery.widget.MySeekBar sp_speed;//速度seekbar
    @ViewInject(R.id.sp_ms_scope)
    private com.bdl.airecovery.widget.MySeekBar sp_scope;//活动范围seekbar

    @ViewInject(R.id.tv_curr_groupcount)
    private TextView tv_curr_groupcount; //当前组数
    @ViewInject(R.id.tv_target_groupcount)
    private TextView tv_target_groupcount; //目标组数
    @ViewInject(R.id.tv_curr_groupnum)
    private TextView tv_curr_groupnum; //当前次数
    @ViewInject(R.id.tv_target_groupnum)
    private TextView tv_target_groupnum; //每组次数

    @ViewInject(R.id.btn_ms_pause)
    private Button btn_ms_pause; //暂停按钮

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        needAfterMotion = true;
        initImmersiveMode(); //隐藏状态栏，导航栏
        initCalibrationParam();
        initMotor(); //电机参数初始化
        queryDeviceParam();  //查询设备参数
        queryUserInfo();     //查询用户信息
        iv_ms_help_onClick();//帮助图片的点击事件（使用xUtils框架会崩溃）
        iv_heartrate_help_onClick(); //心率区间 帮助按钮点击事件

        chooseDeviceType(); //选择设备类型
        //注册蓝牙用监听器
        bluetoothReceiver = new BluetoothReceiver();
        IntentFilter intentFilter = new IntentFilter("com.bdl.bluetoothmessage");
        registerReceiver(bluetoothReceiver, intentFilter);

        startTime = System.currentTimeMillis(); //开始时间
    }

    int currGroup;
    int currGroupNum;
    boolean canOpenRestDialog = false;

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
                    return new Pair<>("极限心率", "#B22222");
                } else {
                    //80%~90%
                    return new Pair<>("无氧心率", "#FF4500");
                }
            } else {
                //70%~80%
                return new Pair<>("有氧心率", "#D2691E");
            }
        } else {
            if (currHeartRate >= (int) maxRate*0.6) {
                //60%~70%
                return new Pair<>("燃脂心率", "	#8B4513");
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
        LogUtil.d("个数："+count+"  力："+force);
        double res = count * (0.01 * weight + 0.02 * force);
        LogUtil.d("卡路里："+res);
        return (double) Math.round(res * 100) / 100; //四舍五入，小数点保留两位
    }

    private void initCalibrationParam() {
        try {
            calibrationParameter = db.findFirst(CalibrationParameter.class);
        } catch (DbException e) {
            e.printStackTrace();
        }
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
                            setParameter(calibrationParameter.getBackSpeed() * 100, MotorConstant.SET_BACK_SPEED);
                            setParameter(0, MotorConstant.SET_GOING_SPEED);
                            break;
                        case 2: //推设备
                            Writer.setParameter(frontLimitedPosition / 10000 * 4856, MotorConstant.SET_FRONTLIMIT);
                            Writer.setParameter(rearLimitedPosition  / 10000 * 4856, MotorConstant.SET_REARLIMIT);
                            //初始化速度
                            setParameter(calibrationParameter.getBackSpeed() * 100, MotorConstant.SET_BACK_SPEED);
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
     * seekBar handler.
     */
    Handler seekBarHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            sp_scope.setProgress(msg.arg1);
            sp_speed.setProgress(msg.arg2);

        }
    };

    /**
     * 发送更新UI的msg给seekBarHandler
     */
    void sendMsgToHandlerOfSeekBar(int position, int speed) {
        Message msg = seekBarHandler.obtainMessage();
        msg.arg1 = position;
        msg.arg2 = speed;
        seekBarHandler.sendMessage(msg);
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
                                    //sp_scope.setProgress((int) lastPosition);
                                    //sp_speed.setProgress((int) lastSpeed);
                                    sendMsgToHandlerOfSeekBar((int)lastPosition, (int)lastSpeed);
                                    for (int i = 1; i < frequency; ++i) {
                                        Thread.sleep(transInterval);
                                        lastPosition += diffPosition;
                                        lastSpeed += diffSpeed;
                                        //sp_scope.setProgress((int) lastPosition);
                                        //sp_speed.setProgress((int) lastSpeed);
                                        sendMsgToHandlerOfSeekBar((int)lastPosition, (int)lastSpeed);
                                    }
                                    //最后一帧校准
                                    Thread.sleep(transInterval);
                                    //sp_scope.setProgress((int) curPosition);
                                    //sp_speed.setProgress((int) curSpeed);
                                    sendMsgToHandlerOfSeekBar((int)curPosition, (int)curSpeed);
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
                    if (currentSpeed <= 10 && negativeTorqueLimited < calibrationParameter.getMinBackTorque() * 100) {
                        setParameter(calibrationParameter.getMinBackTorque() * 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
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
                            int leads = currentSpeed / 100 + calibrationParameter.getLead(); //提前量
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

    private QMUIPopup mNormalPopup;
    /**
     * 心率区间 帮助按钮
     */
    private void iv_heartrate_help_onClick() {
        iv_heartrate_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initNormalPopupIfNeed();
                mNormalPopup.setAnimStyle(QMUIPopup.ANIM_GROW_FROM_CENTER);
                mNormalPopup.setPreferredDirection(QMUIPopup.DIRECTION_BOTTOM);
                mNormalPopup.show(view);
            }
        });
    }

    private void initNormalPopupIfNeed() {
        if (mNormalPopup == null) {
            mNormalPopup = new QMUIPopup(this, QMUIPopup.DIRECTION_NONE);
            TextView textView = new TextView(this);
            textView.setLayoutParams(mNormalPopup.generateLayoutParam(
                    QMUIDisplayHelper.dp2px(this, 500),
                    WRAP_CONTENT
            ));
            if (tv_heart_analyze.getText().equals("热身心率")) {
                textView.setText("在此心率区间运动10至15分钟可实现完美热身效果，进而有利于进行后续强度更大的运动，此外也有助于在健身结束进行放松。");
            } else if (tv_heart_analyze.getText().equals("燃脂心率")) {
                textView.setText("进行厌氧运动可增强耐力并使身体能够应付更大强度的运动，这一强度的运动可燃烧脂肪，但需要持续较长时间。");
            } else if (tv_heart_analyze.getText().equals("有氧心率")) {
                textView.setText("在此心率区间，心血管耐力将得到增强，在较长一段时间内保持此心率水平可消耗热量，燃烧碳水化合物和脂肪。");
            } else if (tv_heart_analyze.getText().equals("无氧心率")) {
                textView.setText("在此心率区间，短暂的高强度运动可提升耐力水平并练出肌肉，在高强度运动的间隙进行适当的休息可帮助达到期望的效果。");
            } else if (tv_heart_analyze.getText().equals("极限心率")) {
                textView.setText("不建议康复患者达到此心率区间，此运动强度非常高。");
            } else {
                textView.setText("未检测到当前心率");
                textView.setLayoutParams(mNormalPopup.generateLayoutParam(
                        QMUIDisplayHelper.dp2px(this, 200),
                        WRAP_CONTENT
                ));
            }
            textView.setLineSpacing(QMUIDisplayHelper.dp2px(this, 4), 1.0f);
            int padding = QMUIDisplayHelper.dp2px(this, 20);
            textView.setPadding(padding, padding, padding, padding);
            textView.setTextColor(Color.parseColor("#000000"));
            textView.setTextSize(20);
            mNormalPopup.setContentView(textView);
            mNormalPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {

                }
            });
        }
    }

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
        if (countDownThread == null) {
            createCountDownTheard(); //创建休息倒计时线程
            countDownThread.start(); //启动休息倒计时线程
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
        //本机倒计时线程
        if (countDownThread != null) {
            countDownThread.interrupt(); //中断线程
            countDownThread = null;
        }
    }

    //卸载广播
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(LocationReceiver);
        unregisterReceiver(eStopReceiver);
        unregisterReceiver(bluetoothReceiver);
        //timerLog.cancel();
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
        if (MyApplication.getInstance().getUser() == null) {
            return;
        }
        person.setText(MyApplication.getInstance().getUser().getUsername()); //用户名
        weight = MyApplication.getInstance().getUser().getWeight(); //体重

        //设置目标组数与次数
        //目标组数
        tv_target_groupcount.setText(String.valueOf(MyApplication.getInstance().getUser().getGroupCount()));
        //每组个数
        tv_target_groupnum.setText(String.valueOf(MyApplication.getInstance().getUser().getGroupNum()));
    }

    //扫描教练的定时任务
    /*Timer timerLog = new Timer();
    TimerTask taskLog = new TimerTask() {
        @Override
        public void run() {
            Intent intent = new Intent(StandardModeActivity.this, BluetoothService.class);
            intent.putExtra("command", CommonCommand.SECOND__LOGIN.value());
            startService(intent);

        }
    };*/

    DbManager dbManager = MyApplication.getInstance().getDbManager();
    Setting setting;
    /**
     * 医护设置 进入按钮
     * 需要密码，在高级设置界面设置
     * 默认admin
     */
    @Event(R.id.btn_setting)
    private void btnSettingOnClick(View v) {
        try {
            setting = dbManager.selector(Setting.class).findFirst();
        } catch (DbException e) {
            e.printStackTrace();
        }
        //创建对话框对象的时候对对话框进行监听
        String info = "请输入密码";
        final int[] cnt = {0};
        final boolean[] flag = {false};
        final SmallPwdDialog dialog = new SmallPwdDialog(StandardModeActivity.this, info, R.style.CustomDialog,
                new SmallPwdDialog.DataBackListener() {
                    @Override
                    public void getData(String data) {
                        String result = data;
                        if (result.equals(setting.getMedicalSettingPassword())) {
                            flag[0] = true;
                        } else {
                            flag[0] = false;
                        }
                        if (flag[0]) {
                            startActivity(new Intent(StandardModeActivity.this, PersonalSettingActivity.class));
                            finish();
                        } else if (cnt[0] != 0) {
                            Toast.makeText(StandardModeActivity.this, "密码错误请重试!", Toast.LENGTH_SHORT).show();
                        }
                        cnt[0]++;
                    }
                });

        dialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.y = 100;
        dialog.getWindow().setGravity(Gravity.TOP);
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
        dialog.show();
        initImmersiveMode(); //隐藏虚拟按键和状态栏
    }

    CommonDialog commonDialog;

    /**
     * 暂停按钮 暂停后不计个数
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
                intentLog2.putExtra("command", CommonCommand.LOGOUT.value());
                startService(intentLog2);
                Intent intentLog = new Intent(StandardModeActivity.this, BluetoothService.class);
                intentLog.putExtra("command", CommonCommand.LOGOUT.value());
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

    private void uploadErrorInfo() {
        //获取当前时间
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = dateFormat.format(date);
        ErrorMsg errorMsg = new ErrorMsg();
        errorMsg.setUid("222");
        errorMsg.setDeviceType(Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getDeviceInnerID()));
        errorMsg.setTrainMode(2);
        errorMsg.setError(errorID);
        errorMsg.setErrorStartTime(currentTime);
        //存暂存表
        TempStorage tempStorage = new TempStorage();
        Gson gson = new Gson();
        tempStorage.setData(gson.toJson(errorMsg)); //重传数据（转换为JSON串）
        tempStorage.setType(4); //重传类型
        try {
            db.saveBindingId(tempStorage);
        } catch (DbException e) {
            e.printStackTrace();
        }

    }
    /**
     * 打开错误信息提示框
     */
    private void showErrorDialog() {
        uploadErrorInfo();
        errorDialog = new CommonDialog(StandardModeActivity.this);
        errorDialog.setTitle("警告");
        errorDialog.setMessage("变频器内部发生错误，错误码：" + errorID);
        errorDialog.setPositiveBtnText("RESET");
        errorDialog.setOnPositiveClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MotorProcess.restoration();

            }
        });
        errorDialog.show();
    }
    RatingDialog ratingDialog;
    //打开评级模态框
    private void openRatingDialog() {
        ratingDialog = new RatingDialog(StandardModeActivity.this);
        ratingDialog.setTitle("完成训练");
        ratingDialog.setMessage("本次训练感受？");
        ratingDialog.setMessage2(" ");
        ratingDialog.setPositiveBtnText("确定");
        ratingDialog.setCanceledOnTouchOutside(false);
        //评级 监听
        ratingDialog.setRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                int ratingInt = (int)rating;
                TextView ratingNote = ratingDialog.getTextMsg2();
                switch (ratingInt) {
                    case 1:
                    case 2:
                        ratingNote.setText("非常轻松");
                        ratingNote.setTextColor(Color.parseColor("#424088"));
                        upload.setUserThoughts("非常轻松");
                        break;
                    case 3:
                    case 4:
                        ratingNote.setText("很轻松");
                        ratingNote.setTextColor(Color.parseColor("#007cb9"));
                        upload.setUserThoughts("很轻松");
                        break;
                    case 5:
                    case 6:
                        ratingNote.setText("轻松");
                        ratingNote.setTextColor(Color.parseColor("#00a03e"));
                        upload.setUserThoughts("轻松");
                        break;
                    case 7:
                    case 8:
                        ratingNote.setText("有点儿困难");
                        ratingNote.setTextColor(Color.parseColor("#ff6c01"));
                        upload.setUserThoughts("有点儿困难");
                        break;
                    case 9:
                    case 10:
                        ratingNote.setText("困难");
                        ratingNote.setTextColor(Color.parseColor("#fa1f55"));
                        upload.setUserThoughts("困难");
                        break;
                    default:
                        break;
                }
            }
        });
        //“确定”按钮 监听
        ratingDialog.setOnPositiveClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //设置Upload类
                int currGroup = Integer.parseInt(tv_curr_groupcount.getText().toString());
                int currGroupNum = Integer.parseInt(tv_curr_groupnum.getText().toString());
                int targetGroupNum = MyApplication.getInstance().getUser().getGroupNum();
                int sumNum = (currGroup-1) * targetGroupNum + currGroupNum;
                upload.setFinishNum(sumNum); //计算训练个数
                long trainTime = (System.currentTimeMillis() - startTime) / 1000;
                upload.setFinishTime((int)trainTime);
                upload.setConsequentForce(Integer.parseInt(positivenumber.getText().toString())); //最终顺向力
                upload.setReverseForce(Integer.parseInt(inversusnumber.getText().toString())); //最终反向力
                LogUtil.d("卡路里参数："+sumNum + "+" +positiveTorqueLimited);
                upload.setEnergy(countEnergy(sumNum, positiveTorqueLimited/100));
                upload.setHeartRateList(heartRateList);
                MyApplication.getInstance().setUpload(upload);

                //关闭可能存在的模态框
                if (commonDialog != null && commonDialog.isShowing()) {
                    commonDialog.dismiss();
                }
                if (helpDialog != null && helpDialog.isShowing()) {
                    helpDialog.dismiss();
                }

                ratingDialog.dismiss();

                //跳转再见页面
                Intent intent = new Intent(StandardModeActivity.this, ByeActivity.class);
                //启动
                startActivity(intent);
                //结束当前Activity
                StandardModeActivity.this.finish();
            }
        });
        //模态框隐藏导航栏
        ratingDialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        ratingDialog.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
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
                ratingDialog.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
            }
        });
        ratingDialog.show();
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
                    setParameter(positiveTorqueLimited + (calibrationParameter.getMinTorque() * 100), MotorConstant.SET_POSITIVE_TORQUE_LIMITED );
                    setParameter(negativeTorqueLimited + (calibrationParameter.getMinTorque() * 100), MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                    setInitialBounce(negativeTorqueLimited + calibrationParameter.getBounce() * 100);
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
                if (! isErrorDialogShow) {
                    startActivity(new Intent(StandardModeActivity.this, ScramActivity.class));
                    StandardModeActivity.this.finish();
                }

            }
            errorID = intent.getStringExtra("error");
            if (errorID != null && !errorID.equals("0")) {
                Message message = errorDialogHandler.obtainMessage();
                message.what = 1;
                message.arg1 = 1;
                errorDialogHandler.sendMessage(message);

            }
            if (errorID != null && errorID.equals("0") && isErrorDialogShow) {
                Message message = errorDialogHandler.obtainMessage();
                message.what = 2;
                message.arg1 = 1;
                errorDialogHandler.sendMessage(message);
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
    private List<Integer> heartRateList = new ArrayList<>();
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
                //用户登录成功
                case CommonMessage.LOGIN_REGISTER_OFFLINE:
                case CommonMessage.LOGIN_REGISTER_ONLINE:
                case CommonMessage.LOGIN_SUCCESS_OFFLINE:
                case CommonMessage.LOGIN_SUCCESS_ONLINE:
                    LogUtil.d("广播接收器收到：" + commonMessage.toString());
                    break;
                //用户下线成功
                case CommonMessage.LOGOUT:
                case CommonMessage.DISCONNECTED:
                    LogUtil.d("广播接收器收到：" + commonMessage.toString());
                    break;
                //获得心率
                case CommonMessage.HEART_BEAT:
                    LogUtil.d("广播接收器收到：" + commonMessage.toString());
                    getrate.setText(commonMessage.getAttachment()); //心率数值
                    if (Integer.parseInt(commonMessage.getAttachment()) != 0) {
                        heartRateList.add(Integer.parseInt(commonMessage.getAttachment()));
                    }
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

    private MediumDialog restDialog;
    private TextView rest_dialog_msg; //休息倒计时模态框 时间文本
    /**
     * 休息指定时间的 倒计时模态框
     */
    private void openRestDialog() {
        restDialog = new MediumDialog(StandardModeActivity.this);
        restDialog.setTime(String.valueOf(MyApplication.getInstance().getUser().getRelaxTime()) + "秒");
        restDialog.setCanceledOnTouchOutside(false);
        //模态框隐藏导航栏
        restDialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        restDialog.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
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
                restDialog.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
            }
        });
        restDialog.show();

        rest_dialog_msg = restDialog.findViewById(R.id.medium_dialog_time);
    }

    private Thread countDownThread;    //休息倒计时线程
    private Handler restHandler;
    private int countDown = 0;
    /**
     * 休息倒计时线程
     */
    private void createCountDownTheard() {
        restHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (!canOpenRestDialog) return;
                if (!isAlert) {
                    openRestDialog();
                    tv_curr_groupcount.setText(String.valueOf(currGroup));
                    tv_curr_groupnum.setText("0");
                    isAlert = true;
                } else {
                    rest_dialog_msg.setText(msg.arg1 + "秒");
                }
            }
        };
        countDownThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    if (!canOpenRestDialog) continue;

                    //休息倒计时
                    countDown = MyApplication.getInstance().getUser().getRelaxTime();
                    while(countDown != 0) {
                        try {
                            Message message = restHandler.obtainMessage();
                            message.arg1 = countDown; //arg1属性指定为当前时间的秒数
                            restHandler.sendMessage(message); //把一个包含消息数据的Message对象压入到消息队列中
                            Thread.sleep(1000);
                            countDown--;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return;
                        }
                    }

                    //休息倒计时结束
                    canOpenRestDialog = false;
                    allowRecordNum = true;
                    isAlert = false;
                    restDialog.dismiss();
                }
            }
        });
    }

	//打开体验者模式模态框
    private  void openExperiencer(){
        commonDialog = new CommonDialog(StandardModeActivity.this);
        commonDialog.setTitle("温馨提示");
        commonDialog.setMessage("您已经完成本次体验，请返回");
        commonDialog.setPositiveBtnText("我知道了");
        commonDialog.setOnPositiveClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commonDialog.dismiss();
                if (MyApplication.getInstance().getUser() != null){
                    Intent intentLog = new Intent(StandardModeActivity.this, BluetoothService.class);
                    intentLog.putExtra("command", CommonCommand.LOGOUT.value());
                    startService(intentLog);
                }
                //置空用户
                MyApplication.getInstance().setUser(null);
                startActivity(new Intent(StandardModeActivity.this,LoginActivity.class));
                finish(); //关闭本activity
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

}
