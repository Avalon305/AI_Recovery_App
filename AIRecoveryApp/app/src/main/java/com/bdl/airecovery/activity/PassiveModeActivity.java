package com.bdl.airecovery.activity;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
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
import com.bdl.airecovery.entity.DTO.ErrorMsg;
import com.bdl.airecovery.entity.Setting;
import com.bdl.airecovery.entity.TempStorage;
import com.bdl.airecovery.entity.Upload;
import com.bdl.airecovery.service.BluetoothService;
import com.bdl.airecovery.service.CardReaderService;
import com.bdl.airecovery.util.MessageUtils;
import com.bdl.airecovery.widget.MyBallView;
import com.google.gson.Gson;

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

import static com.bdl.airecovery.contoller.Writer.setParameter;

@ContentView(R.layout.activity_mode_passive)
public class PassiveModeActivity extends BaseActivity {


    /**
     * 电机相关
     */
    private int num = 0;
    private static final int positiveTorqueLimited = 10 * 100;
    private static final int negativeTorqueLimited = 10 * 100;
    private int     frontLimitedPosition;
    private int rearLimitedPosition;
    private int deviceType = MyApplication.getInstance().getCurrentDevice().getDeviceType(); //获得设备信息
    private int speed = 500;
    private boolean allowRecordNum = true; //允许计数
    private boolean setSpeed = false;
    int motorDirection = MyApplication.getInstance().motorDirection;
    private eStopBroadcastReceiver eStopReceiver; //急停广播
    private String errorID; //错误ID
    private CommonDialog errorDialog; //错误提示框
    int currGroup;
    int currGroupNum;
    boolean canOpenRestDialog = false;
    DbManager db = MyApplication.getInstance().getDbManager(); //获取DbManager对象
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
                //btn_pause.setText("结束"); //暂停按钮修改为结束按钮
                //弹出评级模态框
                openRatingDialog();
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
    private CommonDialog spasmDialog;
    private Handler spasmDialogHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    showSpasmDialog();
                    break;
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
                        Log.e("-----flag", String.valueOf(isErrorDialogShow));
                        isErrorDialogShow = true;
                        showErrorDialog();
                        Log.e("-----flag", String.valueOf(isErrorDialogShow));
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


    //基本运动定时器
    Timer timer = new Timer();
    private boolean needAfterMotion = true;

    Timer countTimer = new Timer();

    /**
     * 控件绑定
     */
    @ViewInject(R.id.tv_mp_person)
    private TextView person;                //用户名

    @ViewInject(R.id.tv_mp_getrate)
    private TextView getrate;               //心率


    @ViewInject(R.id.tv_mp_speednumber)
    private TextView getSpeed;              //当前速度

    @ViewInject(R.id.iv_mp_speedminus)
    private ImageView speedMinus;           //减小速度

    @ViewInject(R.id.iv_mp_speedplus)
    private ImageView speedPlus;            //增加速度

    @ViewInject(R.id.iv_mp_help)
    private ImageView iv_ma_help;           //“帮助”图片按钮

    @ViewInject(R.id.iv_mp_state)
    private ImageView iv_ma_state;    //登录状态

    @ViewInject(R.id.bv_mp_ball)
    private MyBallView ball;                //小球

    @ViewInject(R.id.tv_curr_groupcount)
    private TextView tv_curr_groupcount; //当前组数
    @ViewInject(R.id.tv_target_groupcount)
    private TextView tv_target_groupcount; //目标组数
    @ViewInject(R.id.tv_curr_groupnum)
    private TextView tv_curr_groupnum; //当前次数
    @ViewInject(R.id.tv_target_groupnum)
    private TextView tv_target_groupnum; //每组次数

    @ViewInject(R.id.btn_mp_pause)
    private Button btn_pause; //暂停按钮

    /**
     * 类成员
     */
    private Thread localCountDownThread;    //本机倒计时线程
    private Thread delayThread;             //模态框倒计时线程
    private int localCountDown = 60;        //本机倒计时（单位：秒）
    private int localCountDownType = 0;     //本机倒计时类型（0运动，1休息）
    private Handler handler;                //用于在UI线程中获取倒计时线程创建的Message对象，得到倒计时秒数与时间类型
    private Handler handler_dialog;         //用于模态框ui线程中获取倒计时线程创建的Message对象
    private Boolean isAlert = false;        //标识是否弹5s倒计时模态框
    private Thread drawCartoonThread;       //绘制动画线程
    private float lastPowerX; //上一次PowerX【小球动画相关】
    private float curPowerX; //当前PowerX【小球动画相关】
    private Upload upload = new Upload();
    private BluetoothReceiver bluetoothReceiver;        //蓝牙广播接收器，监听用户的登录广播
    private double weight;
    private long startTime;

    locationReceiver LocationReceiver = new locationReceiver();
    IntentFilter filterHR = new IntentFilter();

    private void uploadErrorInfo() {
        //获取当前时间
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd :hh:mm:ss");
        String currentTime = dateFormat.format(date);
        ErrorMsg errorMsg = new ErrorMsg();
        errorMsg.setUid(MyApplication.getInstance().getUser().getUserId());
        errorMsg.setDeviceType(2);
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
     * 打开痉挛信息提示框
     */
    private void showSpasmDialog() {
        spasmDialog = new CommonDialog(PassiveModeActivity.this);
        spasmDialog.setTitle("警告");
        spasmDialog.setMessage("系统检测到用户有痉挛现象，请医护人员确认用户是否需要帮助");
        spasmDialog.setPositiveBtnText("结束训练");
        spasmDialog.setCancelable(false);
        spasmDialog.setOnPositiveClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent intentLog = new Intent(PassiveModeActivity.this, BluetoothService.class);
                    intentLog.putExtra("command", CommonCommand.LOGOUT.value());
                    startService(intentLog);
                    Log.d("StandardModeActivity", "request to logout");

                    spasmDialog.dismiss();
                    //新建一个跳转到待机界面Activity的显式意图
                    Intent intent = new Intent(PassiveModeActivity.this, LoginActivity.class);
                    //启动
                    startActivity(intent);
                    //结束当前Activity
                    PassiveModeActivity.this.finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        spasmDialog.show();
    }
    
    
    /**
     * 打开错误信息提示框
     */
    private void showErrorDialog() {
        errorDialog = new CommonDialog(PassiveModeActivity.this);
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        needAfterMotion = true;
        initImmersiveMode(); //隐藏状态栏，导航栏
        initMotor();
        filterHR.addAction("heartrate");
        registerReceiver(LocationReceiver, filterHR);
        queryDeviceParam();//查询设备参数
        queryUserInfo(); //查询用户信息
        setCartoon(); //动画设置
        iv_ma_help_onClick(); //帮助图片的点击事件（使用xUtils框架会崩溃）

        startTime = System.currentTimeMillis();

        //注册蓝牙用监听器
        bluetoothReceiver = new BluetoothReceiver();
        IntentFilter intentFilter = new IntentFilter("com.bdl.bluetoothmessage");
        registerReceiver(bluetoothReceiver,intentFilter);

        if(deviceType == 3) {
            if (motorDirection == 1) {
                MyApplication.getInstance().motorDirection = 2;
                int[] newLimitedPosition = MessageUtils.recersalLimit(frontLimitedPosition, rearLimitedPosition, 1);
                frontLimitedPosition = newLimitedPosition[0];
                rearLimitedPosition = newLimitedPosition[1];
            } else if (motorDirection == 2) {
                MyApplication.getInstance().motorDirection = 1;
                int[] newLimitedPosition = MessageUtils.recersalLimit(frontLimitedPosition, rearLimitedPosition, 2);
                frontLimitedPosition = newLimitedPosition[0];
                rearLimitedPosition = newLimitedPosition[1];
            }
        }
        try {
            passiveModeProcess();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化前方限制、后方限制、设备类型
     */
    private void initMotor() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Writer.setParameter(positiveTorqueLimited, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
                    Writer.setParameter(negativeTorqueLimited, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                    Writer.setParameter(negativeTorqueLimited, MotorConstant.SET_PUSH_TORQUE);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();
        String deviceName = MyApplication.getInstance().getCurrentDevice().getDisplayName();
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
                    Writer.setParameter(frontLimitedPosition / 10000 * 4856, MotorConstant.SET_FRONTLIMIT);
                    Writer.setParameter(rearLimitedPosition / 10000 * 4856, MotorConstant.SET_REARLIMIT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 判断是否异号，异号返回true
     */
    private boolean oppositeSigns(int a, int b) { return (a ^ b) < 0; }

    /**
     * 被动模式运动过程
     */
    private void passiveModeProcess() throws Exception {
        Log.e("----", "被动模式1");

        final int[] lastLocation = {frontLimitedPosition}; //上一次的位置，初始值为前方限制
        final int[] lastDifference = {0}; //上一次的位置差初始值为0

        final boolean[] isStop = {false};
        final boolean[] countFlag = {false};//计数标志位
        final boolean[] isCompareSpeedEnable = {true};
        final int[] spasmCount = {0};
        final int[] count = {0};
        final boolean[] isCountEnable = {false};
        //开速度
        setParameter(-speed, MotorConstant.SET_GOING_SPEED);
        setParameter(-speed, MotorConstant.SET_COMPARE_SPEED);
        setParameter(speed, MotorConstant.SET_BACK_SPEED);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    //发送消息到handler
                    Message message = countHandler.obtainMessage();
                    message.what = 1;
                    //读取当前位置
                    String currentLocation = Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION);
                    String currentSpeed = Reader.getRespData(MotorConstant.READ_ROTATIONAL_SPEED);
                    int difference = Integer.parseInt(currentLocation) - lastLocation[0]; //本次位置和上次读到的位置差
                    if (Integer.parseInt(currentLocation) >= rearLimitedPosition + 80000
                    &&  Integer.parseInt(currentLocation) <= frontLimitedPosition - 80000) {
                        isCountEnable[0] = true;
                    } else {
                        isCountEnable[0] = false;
                    }
                    if (Math.abs(Integer.valueOf(currentSpeed)) <= 10 && !isStop[0] && isCountEnable[0]) { //逼停
                        Log.e("----", "逼停");
                        setParameter(0, MotorConstant.SET_GOING_SPEED);
                        setParameter(0, MotorConstant.SET_COMPARE_SPEED);
                        setParameter(0, MotorConstant.SET_BACK_SPEED);
                        isStop[0] = true;
                    }
                    if (isStop[0]) {
                        //痉挛判断
                        if (oppositeSigns(lastDifference[0], difference)
                                && lastDifference[0] != 0
                                && difference != 0) { //如果和上次的位置差异号
                            count[0] = 0;
                            spasmCount[0]++;
                            Log.e("----", String.valueOf(spasmCount[0]));
                            if (spasmCount[0] >= 1) { //如果和上次的位置差异号
                                //痉挛
                                timer.cancel();

                                Message message2 = spasmDialogHandler.obtainMessage();
                                message2.what = 1;
                                message2.arg1 = 1;
                                spasmDialogHandler.sendMessage(message);
                            }
                        } else {
                            Log.e("----", "计数" + String.valueOf(count[0]));
                            count[0]++;
                            if (count[0] >= 30) {
                                Log.e("----", "重新进入被动");
                                count[0] = 0;
                                setParameter(-speed, MotorConstant.SET_GOING_SPEED);
                                setParameter(-speed, MotorConstant.SET_COMPARE_SPEED);
                                setParameter(speed, MotorConstant.SET_BACK_SPEED);
                                Thread.sleep(200);
                                Log.e("----", "开速度");
                                isStop[0] = false;
                            }
                        }
                    }


                    if (setSpeed && isCompareSpeedEnable[0]) {
                        setParameter(speed, MotorConstant.SET_BACK_SPEED);
                        setParameter(-speed, MotorConstant.SET_GOING_SPEED);
                        setParameter(-speed, MotorConstant.SET_COMPARE_SPEED);
                        setSpeed = false;
                    } else if (setSpeed && !isCompareSpeedEnable[0]) {
                        setParameter(speed, MotorConstant.SET_BACK_SPEED);
                        setParameter(-speed, MotorConstant.SET_GOING_SPEED);
                        setSpeed = false;
                    }

                    //更新上次的位置差
                    lastDifference[0] = difference;
                    if (difference > 20000) { //回程
//                        if (Integer.parseInt(currentLocation) <= rearLimitedPosition + 100000) {
//                            setParameter(-speed, MotorConstant.SET_COMPARE_SPEED);
//                            isCompareSpeedEnable[0] = true;
//                        }
                        //超过前方限制
                        if (Integer.parseInt(currentLocation) >= frontLimitedPosition - 50000) {
//                            Thread.sleep(3000);
//                            setParameter(0, MotorConstant.SET_BACK_SPEED);
                            setParameter(-speed, MotorConstant.SET_GOING_SPEED);
                            setParameter(-speed, MotorConstant.SET_COMPARE_SPEED);
                            isCompareSpeedEnable[0] = true;
                            if (countFlag[0] && allowRecordNum) {
                                num++;
                                message.arg1 = num;
                                countHandler.sendMessage(message);
                            }
                            countFlag[0] = false;
                            spasmCount[0] = 0;
                        }
                        //更新lastLocation
                        lastLocation[0] = Integer.parseInt(currentLocation);
                    } else if (difference < -20000) { //去程
                        if (Integer.parseInt(currentLocation) <= rearLimitedPosition + 150000) {
                            setParameter(0, MotorConstant.SET_COMPARE_SPEED);
                            isCompareSpeedEnable[0] = false;
                        }
                        //超过后方限制
                        if (Integer.parseInt(currentLocation) <= rearLimitedPosition + 50000) {
//                            setParameter(speed, MotorConstant.SET_BACK_SPEED);
                            setParameter(0, MotorConstant.SET_GOING_SPEED);
                            setParameter(0, MotorConstant.SET_COMPARE_SPEED);
                            countFlag[0] = true;
                            spasmCount[0] = 0;
                        }
                        //更新lastLocation
                        lastLocation[0] = Integer.parseInt(currentLocation);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(timerTask, 0, 100);
    }
    /**
     * 当Activity准备好和用户进行交互时，调用onResume()
     * 此时Activity处于【运行状态】
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (drawCartoonThread == null) {
            CreateDrawCartoonThread(); //创建实时更新小球位置线程
            drawCartoonThread.start(); //启动实时更新小球位置线程
        }
        if (countDownThread == null) {
            createCountDownTheard(); //创建休息倒计时线程


            countDownThread.start(); //启动休息倒计时线程
        }
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
        //小球动画线程
        if (drawCartoonThread != null) {
            drawCartoonThread.interrupt(); //中断线程
            drawCartoonThread = null;
        }
        //本机倒计时线程
        if (countDownThread != null) {
            countDownThread.interrupt(); //中断线程
            countDownThread = null;
        }
    }

    /**
     * 查询当前设备参数
     */
    private void queryDeviceParam() {
        //getSpeed.setText(speed / 100 + "");
        getSpeed.setText(String.valueOf(MyApplication.getInstance().getUser().getSpeedRank()));
        speed = 50 * Integer.valueOf(getSpeed.getText().toString()) + 250;
    }

    /**
     * 计算卡路里消耗
     */
    private double countEnergy(int count, int force){
        double res = count * (0.01 * weight + 0.02 * force);
        return (double) Math.round(res * 100) / 100; //四舍五入，小数点保留两位
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

        ///设置目标组数与次数
        //目标组数
        tv_target_groupcount.setText(String.valueOf(MyApplication.getInstance().getUser().getGroupCount()));
        //每组个数
        tv_target_groupnum.setText(String.valueOf(MyApplication.getInstance().getUser().getGroupNum()));
    }

    /**
     * 更新小球位置线程
     */
    private void CreateDrawCartoonThread() {
        //绘制动画线程（每100ms绘制一次，100ms期间的过渡动画每5ms绘制一次）
        //通过更改小球实例的成员变量值，即可重新绘制
        final int interval = 100; //绘制间隔：100ms
        final int frequency = 20; //过渡动画中100ms内的绘制频率
        final int transInterval = interval/frequency;   //过渡动画的绘制间隔：5ms
        drawCartoonThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        //获取推拉速度
                        ball.speedY = Float.parseFloat(Reader.getRespData(MotorConstant.READ_ROTATIONAL_SPEED));
                        //获取推拉位移
                        if(lastPowerX == 0) {
                            //第一次获取，需要获取两次，才能动画过渡
                            lastPowerX = Float.parseFloat(Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION)) / 10000;
                            Thread.sleep(interval);
                            curPowerX = Float.parseFloat(Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION)) / 10000;
                        } else {
                            //从第二次开始，将上一次的保存，并再获取一次，进行动画过渡
                            lastPowerX = ball.powerX;
                            curPowerX = Float.parseFloat(Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION)) / 10000;
                        }

                        //动画过渡，每5ms绘制一次
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    float diff = (curPowerX - lastPowerX) / frequency; //过渡差值
                                    ball.powerX = lastPowerX; //绘制小球开始位置
                                    for (int i = 1; i < frequency; ++i) {
                                        Thread.sleep(transInterval);
                                        ball.powerX += diff;
                                    }
                                    //最后一帧校准
                                    Thread.sleep(transInterval);
                                    ball.powerX = curPowerX;
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();

                        Thread.sleep(interval+5);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

//                    Log.d("ball1","电机传值:powerX=" + ball.powerX + "\tspeedY=" + ball.speedY);
//                    Log.d("ball2","画布坐标系:currentX=" + ball.currentX + "\tcurrentY=" + ball.currentY);
//                    Log.d("ball3","计算坐标系:calculateX=" + ball.calculateX + "\tcalculateY=" + ball.calculateY);
                }
            }
        });
    }

    /**
     * 右侧动画设置（与电机联动）TODO：需要电机传值
     */
    private void setCartoon() {
        ball.powerX = -320; //电机传进来的物理位移
        ball.speedY = 0; //电机传进来的推拉速度
        ball.MaxR = 999; //合理的速度范围的最大边界值
        ball.MinR = 0; //合理的速度范围的最小边界值
        ball.frontLimit = frontLimitedPosition / 10000 - 2; //前方限制
        ball.backLimit = rearLimitedPosition / 10000 + 4; //后方限制
        ball.stateS = 1; //设置小球状态：可运动
    }
    /*//扫描教练的定时任务
    Timer timerLog = new Timer();
    TimerTask taskLog = new TimerTask() {
        @Override
        public void run() {
            Intent intent = new Intent(PassiveModeActivity.this, BluetoothService.class);
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
        final SmallPwdDialog dialog = new SmallPwdDialog(PassiveModeActivity.this, info, R.style.CustomDialog,
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
                            startActivity(new Intent(PassiveModeActivity.this, PersonalSettingActivity.class));
                            finish();
                        } else if (cnt[0] != 0) {
                            Toast.makeText(PassiveModeActivity.this, "密码错误请重试!", Toast.LENGTH_SHORT).show();
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

    //速度的“+”
    @Event(R.id.iv_mp_speedplus)
    private void speedPlusOnClick(View v) {
        //显示
        if (Integer.valueOf(getSpeed.getText().toString()) < 10) {
            getSpeed.setText(Integer.valueOf(getSpeed.getText().toString()) + 1 + "");
            speed = 50 * Integer.valueOf(getSpeed.getText().toString()) + 250;
            setSpeed = true;
        }
    }

    //速度的“-”
    @Event(R.id.iv_mp_speedminus)
    private void speedMinusOnClick(View v) {
        //显示
        if (Integer.valueOf(getSpeed.getText().toString()) > 1) {
            getSpeed.setText(Integer.valueOf(getSpeed.getText().toString()) - 1 + "");
            speed = 50 * Integer.valueOf(getSpeed.getText().toString()) + 250;
            setSpeed = true;
        }
    }

    CommonDialog commonDialog;
    /**
     * “暂停”按钮 监听
     * 点击弹出模态框，同时电机暂停 TODO：电机
     */
    @Event(R.id.btn_mp_pause)
    private void pauseClick(View v) {
        allowRecordNum = false;
        commonDialog = new CommonDialog(PassiveModeActivity.this);
        commonDialog.setTitle("温馨提示");
        commonDialog.setMessage("您确定要放弃本次训练吗？");
        commonDialog.setNegativeBtnText("结束");
        commonDialog.setPositiveBtnText("继续");
        //“结束”按钮 监听，点击跳转到待机界面
        commonDialog.setOnNegativeClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //请求退出登录
                Intent intentLog2 = new Intent(PassiveModeActivity.this, CardReaderService.class);
                intentLog2.putExtra("command", CommonCommand.LOGOUT.value());
                startService(intentLog2);
                Intent intentLog = new Intent(PassiveModeActivity.this, BluetoothService.class);
                intentLog.putExtra("command", CommonCommand.LOGOUT.value());
                startService(intentLog);
                Log.d("StandardModeActivity", "request to logout");

                commonDialog.dismiss();
                //新建一个跳转到待机界面Activity的显式意图
                Intent intent = new Intent(PassiveModeActivity.this, LoginActivity.class);
                //启动
                startActivity(intent);
                //结束当前Activity
                PassiveModeActivity.this.finish();
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

    LargeDialogHelp helpDialog;
    /**
     * “帮助”图片的监听（使用xUtils框架会崩溃）
     * 点击跳转到帮助页面
     */
    private void iv_ma_help_onClick() {
        iv_ma_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                helpDialog = new LargeDialogHelp(PassiveModeActivity.this);
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
                //设置点击Dialog外部任意区域关闭Dialog
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
                if (!isErrorDialogShow) {
                    startActivity(new Intent(PassiveModeActivity.this, ScramActivity.class));
                    PassiveModeActivity.this.finish();
                }

            }
            errorID = intent.getStringExtra("error");
            if (errorID != null && !errorID.equals("0")) {
                Message message = errorDialogHandler.obtainMessage();
                message.what = 1;
                message.arg1 = 1;
                errorDialogHandler.sendMessage(message);
                uploadErrorInfo();
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
                //接收心率广播
                if (intentAction.equals("heartrate")) {
                    getrate.setText(intent.getStringExtra("heartrate")); //更新心率
                } else if (intentAction.equals("log")) {
                    if (intent.getStringExtra("log").equals(LoginResp.LOCALTWOLOGICARD.getStr())||intent.getStringExtra("log").equals(LoginResp.LOCALTWOLOGIBLUE.getStr())
                            ||intent.getStringExtra("log").equals(LoginResp.REMOTETWOLOGIBLUE.getStr())
                            ||intent.getStringExtra("log").equals(LoginResp.REMOTETWOLOGICARD.getStr())) {
                        //如果连接成功，跳转医护设置界面
                        Log.e("StandardModeActivity", "login successfully");
                        Intent activityintent = new Intent(PassiveModeActivity.this, PersonalSettingActivity.class); //新建一个跳转到医护设置界面Activity的显式意图
                        startActivity(activityintent); //启动
                        PassiveModeActivity.this.finish(); //结束当前Activity
                    } else if (intent.getStringExtra("log").equals(LoginResp.LOCALTWOLOGOCARD.getStr())||intent.getStringExtra("log").equals(LoginResp.LOCALTWOLOGOBLUE.getStr())
                            ||intent.getStringExtra("log").equals(LoginResp.REMOTETWOLOGOBLUE.getStr())
                            ||intent.getStringExtra("log").equals(LoginResp.REMOTETWOLOGOCARD.getStr())) {
                        //刷新界面
                        PassiveModeActivity.this.recreate();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //解注册广播
        if (LocationReceiver != null) {
            try {
                unregisterReceiver(LocationReceiver);
                Log.e("LoginActivity", "解注册广播");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        unregisterReceiver(eStopReceiver);

        timer.cancel(); //结束定时任务
        if (needAfterMotion) {
            MotorProcess.motorInitialization();
        }
        unregisterReceiver(bluetoothReceiver);
        //timerLog.cancel();
    }

    private List<Integer> heartRateList = new ArrayList<>();
    /**
     * 蓝牙广播专用接收器
     */
    private class BluetoothReceiver extends BroadcastReceiver {

        private Gson gson = new Gson();

        private CommonMessage transfer(String json){
            return gson.fromJson(json,CommonMessage.class);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String messageJson = intent.getStringExtra("message");
            CommonMessage commonMessage = transfer(messageJson);
            switch (commonMessage.getMsgType()){
                //获得心率
                case CommonMessage.HEART_BEAT:
                    LogUtil.d("广播接收器收到："+ commonMessage.toString());
                    getrate.setText(commonMessage.getAttachment());
                    if (Integer.parseInt(commonMessage.getAttachment()) != 0) {
                        heartRateList.add(Integer.parseInt(commonMessage.getAttachment()));
                    }
                    break;
                default:
                    LogUtil.e("未知广播，收到message：" + commonMessage.getMsgType());
            }
        }
    }

    RatingDialog ratingDialog;
    //打开评级模态框
    private void openRatingDialog() {
        timer.cancel(); //结束定时任务
        MotorProcess.motorInitialization();

        ratingDialog = new RatingDialog(PassiveModeActivity.this);
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
                upload.setSpeedRank(Integer.parseInt(getSpeed.getText().toString()));
                upload.setEnergy(countEnergy(sumNum, positiveTorqueLimited / 100));
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
                Intent intent = new Intent(PassiveModeActivity.this, ByeActivity.class);
                //启动
                startActivity(intent);
                //结束当前Activity
                PassiveModeActivity.this.finish();
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

    private MediumDialog restDialog;
    private TextView rest_dialog_msg; //休息倒计时模态框 时间文本
    /**
     * 休息指定时间的 倒计时模态框
     */
    private void openRestDialog() {
        restDialog = new MediumDialog(PassiveModeActivity.this);
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
                    try {
                        setParameter(0, MotorConstant.SET_GOING_SPEED);
                        setParameter(0, MotorConstant.SET_COMPARE_SPEED);
                        setParameter(0, MotorConstant.SET_BACK_SPEED);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }



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
                    try {
                        setParameter(-speed, MotorConstant.SET_GOING_SPEED);
                        setParameter(-speed, MotorConstant.SET_COMPARE_SPEED);
                        setParameter(speed, MotorConstant.SET_BACK_SPEED);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    restDialog.dismiss();
                }
            }
        });
    }

}
