package com.bdl.airecovery.activity;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.bdl.airecovery.entity.CurrentTime;
import com.bdl.airecovery.entity.Upload;
import com.bdl.airecovery.service.BluetoothService;
import com.bdl.airecovery.service.CardReaderService;
import com.bdl.airecovery.util.MessageUtils;
import com.bdl.airecovery.util.SendReqOfCntTimeUtil;
import com.google.gson.Gson;
import com.bdl.airecovery.widget.MyBallView;

import org.xutils.common.util.LogUtil;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

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

    @ViewInject(R.id.tv_mp_getnumber)
    private TextView getnumber;             //次数

    @ViewInject(R.id.tv_mp_speednumber)
    private TextView getSpeed;              //当前速度

    @ViewInject(R.id.iv_mp_speedminus)
    private ImageView speedMinus;           //减小速度

    @ViewInject(R.id.iv_mp_speedplus)
    private ImageView speedPlus;            //增加速度

    @ViewInject(R.id.tv_mp_gettime)
    private TextView gettime;               //倒计时

    @ViewInject(R.id.tv_mp_time)
    private TextView tv_ma_time;            //提示文本：训练/休息倒计时

    @ViewInject(R.id.iv_mp_help)
    private ImageView iv_ma_help;           //“帮助”图片按钮

    @ViewInject(R.id.iv_mp_state)
    private ImageView iv_ma_state;    //登录状态

    @ViewInject(R.id.btn_mp_coach)
    private Button btn_ma_coach;   //“教练协助/停止协助”按钮

    @ViewInject(R.id.btn_mp_pause)
    private Button btn_ma_pause;   //“暂停”按钮

    @ViewInject(R.id.bv_mp_ball)
    private MyBallView ball;                //小球

    private TextView medium_dialog_msg;     //最后5秒模态框 时间文本

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
    private SendReqOfCntTimeUtil sendReqOfCntTimeUtil; //发送同步时间请求的工具
    private float lastPowerX; //上一次PowerX【小球动画相关】
    private float curPowerX; //当前PowerX【小球动画相关】
    private Upload upload = new Upload();
    private BluetoothReceiver bluetoothReceiver;        //蓝牙广播接收器，监听用户的登录广播
    private double weight;

    locationReceiver LocationReceiver = new locationReceiver();
    IntentFilter filterHR = new IntentFilter();

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
        syncCurrentTime(); //同步当前时间
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
                                //TODO 痉挛弹窗
                                timer.cancel();
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
        if (localCountDownThread == null) {
            CreatelocalCountDownTheard(); //创建本机倒计时线程
            localCountDownThread.start(); //启动本机倒计时线程
        }
        if (drawCartoonThread == null) {
            CreateDrawCartoonThread(); //创建实时更新小球位置线程
            drawCartoonThread.start(); //启动实时更新小球位置线程
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
        //本机倒计时线程
        if (localCountDownThread != null) {
            localCountDownThread.interrupt(); //中断线程
            localCountDownThread = null;
        }
        //小球动画线程
        if (drawCartoonThread != null) {
            drawCartoonThread.interrupt(); //中断线程
            drawCartoonThread = null;
        }
        //停止Timer TimerTask
        if (sendReqOfCntTimeUtil != null && sendReqOfCntTimeUtil.timer != null && sendReqOfCntTimeUtil.timerTask != null) {
            sendReqOfCntTimeUtil.timerTask.cancel();
            sendReqOfCntTimeUtil.timer.cancel();
            //Log.d("同步倒计时","AdaptModeActivity：停止TimerTask");
        }
    }

    /**
     * 同步当前时间
     */
    private void syncCurrentTime() {
        //同步时间服务器业务
        sendReqOfCntTimeUtil = new SendReqOfCntTimeUtil();
        sendReqOfCntTimeUtil.SendRequestOfCurrentTime();
        //Log.d("LoginActivity","请求同步当前时间");
    }

    /**
     * 查询当前设备参数
     */
    private void queryDeviceParam() {
        getSpeed.setText(speed / 100 + "");
        //获取设备信息
        //判空
//        if (MyApplication.getInstance().getCurrentDevice() == null) {
//            return;
//        }
//        if (MyApplication.getInstance().getCurrentDevice().getReverseForce() != null && MyApplication.getInstance().getCurrentDevice().getConsequentForce() != null) {
//            positivenumber.setText(MyApplication.getInstance().getCurrentDevice().getConsequentForce());//顺向力数值
//            inversusnumber.setText(MyApplication.getInstance().getCurrentDevice().getReverseForce());   //反向力数值
//            positiveTorqueLimited = Integer.parseInt(String.valueOf(positivenumber.getText())) * 100;
//            negativeTorqueLimited = Integer.parseInt(String.valueOf(inversusnumber.getText())) * 100;
//        }
    }

    /**
     * 计算卡路里消耗
     */
    private double countEnergy(int count, int force){
        return count * (0.01 * weight + 0.02 * force);
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
                        btn_ma_coach.setText("教练协助"); //更新为“教练协助”按钮
                        iv_ma_state.setImageDrawable(getResources().getDrawable((R.drawable.yonghu1)));
                    } else { //否则，不为空串，说明有教练连接蓝牙
                        btn_ma_coach.setText("停止协助"); //更新为“停止协助”按钮
                        iv_ma_state.setImageDrawable(getResources().getDrawable((R.drawable.shou)));
                        //person.append("【调试模式】"); //追加“【调试模式】”文本
                    }
                } else if (MyApplication.getInstance().getUser().getRole().equals("coach")) {
                    btn_ma_coach.setText("医护设置"); //更新为“医护设置”按钮
                    iv_ma_state.setImageDrawable(getResources().getDrawable((R.drawable.guanliyuan1)));
                    //person.append("【教练用户】"); //追加“【教练用户】”文本
                } else {
                    //person.append("【测试模式】"); //追加“【测试模式】”文本
                    iv_ma_state.setImageDrawable(getResources().getDrawable((R.drawable.banshou1)));
                }
            }
            weight = MyApplication.getInstance().getUser().getWeight();

        }
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
        ball.frontLimit = frontLimitedPosition / 10000; //前方限制
        ball.backLimit = rearLimitedPosition / 10000; //后方限制
        ball.stateS = 1; //设置小球状态：可运动
    }
    //扫描教练的定时任务
    Timer timerLog = new Timer();
    TimerTask taskLog = new TimerTask() {
        @Override
        public void run() {
            Intent intent = new Intent(PassiveModeActivity.this, BluetoothService.class);
            intent.putExtra("command", CommonCommand.SECOND__LOGIN.value());
            startService(intent);

        }
    };
    /**
     * “教练协助/停止协助”按钮
     * 如果是调试模式，显示“停止协助”，点击会将HelpUser置为空串，然后跳转到主界面
     * 如果不是调制模式，显示“教练协助”，点击事件与主界面一致（连接教练蓝牙）
     */
    @Event(R.id.btn_ma_coach)
    private void btn_ma_coach_onClick(View v) {
        //如果是教练协助
        if (btn_ma_coach.getText().equals("教练协助")) {
            btn_ma_coach.setText("教练协助...");
            Intent intent2 = new Intent(PassiveModeActivity.this, CardReaderService.class);
            intent2.putExtra("command", CommonCommand.SECOND__LOGIN.value());
            startService(intent2);
            timerLog.schedule(taskLog,0,2000);
            Log.d("AdaptModeModeActivity", "request to login");
        }
        //如果是停止协助
        else if (btn_ma_coach.getText().equals("停止协助")) {
            Intent intent2 = new Intent(PassiveModeActivity.this, CardReaderService.class);
            intent2.putExtra("command", CommonCommand.SECOND__LOGOUT.value());
            startService(intent2);
            Intent intent = new Intent(PassiveModeActivity.this, BluetoothService.class);
            intent.putExtra("command", CommonCommand.SECOND__LOGOUT.value());
            startService(intent);
            Log.d("AdaptModeActivity", "request to logout");
        }
        //如果是医护设置
        else if (btn_ma_coach.getText().equals("医护设置")) {
            //跳转医护设置界面
            Intent intent = new Intent(PassiveModeActivity.this, PersonalSettingActivity.class); //新建一个跳转到医护设置界面Activity的显式意图
            startActivity(intent); //启动
            PassiveModeActivity.this.finish(); //结束当前Activity
        }

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
        //“结束”按钮 监听
        commonDialog.setOnNegativeClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //置空用户
                Intent intentLog = new Intent(PassiveModeActivity.this, BluetoothService.class);
                intentLog.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
                startService(intentLog);
                Intent intentLog2 = new Intent(PassiveModeActivity.this, CardReaderService.class);
                intentLog2.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
                startService(intentLog2);
                commonDialog.dismiss();
                //新建一个跳转到待机界面Activity的显式意图
                Intent intent = new Intent(PassiveModeActivity.this, LoginActivity.class);
                //启动
                startActivity(intent);
                //结束当前Activity
                PassiveModeActivity.this.finish();
            }
        });
        //“继续”按钮 监听，点击跳转到待机界面
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

    MediumDialog mediumDialog;
    /**
     * 最后5秒倒计时 模态框
     */
    private void Last5sAlertDialog() {
        mediumDialog = new MediumDialog((PassiveModeActivity.this));
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
     * 创建本机倒计时线程（如果时间显示器宕机，需要用到该倒计时）
     */
    private void CreatelocalCountDownTheard() {
        //创建Handler，用于在UI线程中获取倒计时线程创建的Message对象，得到倒计时秒数与时间类型
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                //获取倒计时秒数
                int arg1 = msg.arg1;
                if (msg.what == 0) {
                    //如果当前时间为训练时间
                    tv_ma_time.setText("训练倒计时：");
                    gettime.setTextColor(gettime.getResources().getColor(R.color.DeepSkyBlue)); //深天蓝色
                }
                if (msg.what == 1) {
                    //如果当前时间为休息时间
                    tv_ma_time.setText("休息倒计时：");
                    gettime.setTextColor(gettime.getResources().getColor(R.color.OrangeRed)); //橘红色
                }
                //如果倒计时秒数小于等于5秒，弹模态框
                if (!isAlert && arg1 <= 5 && msg.what == 0) {
                    Last5sAlertDialog();
                    isAlert = true;
                }
                if (isAlert) {
                    medium_dialog_msg.setText("0:0" + arg1);
                }
                //设置文本内容（有两种特殊情况，单独设置合适的文本格式）
                int minutes = arg1 / 60;
                int remainSeconds = arg1 % 60;
                if (remainSeconds < 10) {
                    gettime.setText(minutes + ":0" + remainSeconds);
                } else {
                    gettime.setText(minutes + ":" + remainSeconds);
                }
            }
        };
        localCountDownThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //训练时间60s倒计时
                localCountDown = 60;
                while (!Thread.currentThread().isInterrupted() && localCountDown > 0) {
                    if (MyApplication.getCurrentTime().getType() != -1) {
                        //校准本机倒计时秒数
                        localCountDown = MyApplication.getCurrentTime().getSeconds(); //获取秒数
                        localCountDownType = MyApplication.getCurrentTime().getType(); //获取时间类型
                        MyApplication.setCurrentTime(new CurrentTime(-1, -1)); //将全局变量currentTime恢复为(-1,-1)，即一旦有值，取后销毁，实现另一种方式的传递。

                    }
                    //获取第一次的倒计时作为当前训练时长，上传至训练结果
                    if(upload.getTrainTime_() == 0) {
                        upload.setTrainTime_(localCountDown);
                    }
                    //将当前倒计时数值存储在Message对象中，通过Handler将消息发送给UI线程，更新UI
                    Message message = handler.obtainMessage();
                    message.what = localCountDownType; //what属性指定为当前时间的类型（0为训练时间，1为休息时间）
                    message.arg1 = localCountDown; //arg1属性指定为当前时间的秒数
                    handler.sendMessage(message); //把一个包含消息数据的Message对象压入到消息队列中

                    //线程睡眠1s
                    try {
                        Thread.sleep(1000);
                        localCountDown--;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }

                //设置训练结果
                    //1.获取当前次数
                upload.setFinishCount_(Integer.parseInt(getnumber.getText().toString()));
                upload.setCalorie_(countEnergy(Integer.parseInt(getnumber.getText().toString()),10));

                //2.获取训练时长
                //已经在第一次校准时间处获取
                MyApplication.setUpload(upload);

                //倒计时结束，跳转再见界面
                //新建一个跳转到再见界面Activity的显式意图
                if(mediumDialog != null && mediumDialog.isShowing()) {
                    mediumDialog.dismiss();
                }
                if(commonDialog != null && commonDialog.isShowing()) {
                    commonDialog.dismiss();
                }
                if(helpDialog != null && helpDialog.isShowing()) {
                    helpDialog.dismiss();
                }
                Intent intent = new Intent(PassiveModeActivity.this, ByeActivity.class);
                //启动
                //startActivity(intent); //TODO 注释后不会跳转到再见界面
                //结束当前Activity
                //PassiveModeActivity.this.finish(); //TODO 注释后不会跳转到再见界面
            }
        });
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
                startActivity(new Intent(PassiveModeActivity.this, ScramActivity.class));
                PassiveModeActivity.this.finish();
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
        timerLog.cancel();
    }
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
                //第一用户登录成功
                case CommonMessage.FIRST__LOGIN_REGISTER_OFFLINE:
                case CommonMessage.FIRST__LOGIN_REGISTER_ONLINE:
                case CommonMessage.FIRST__LOGIN_SUCCESS_OFFLINE:
                case CommonMessage.FIRST__LOGIN_SUCCESS_ONLINE:
                    LogUtil.d("广播接收器收到："+ commonMessage.toString());
                    break;
                //第一用户下线成功
                case CommonMessage.FIRST__LOGOUT:
                case CommonMessage.FIRST__DISCONNECTED:
                    LogUtil.d("广播接收器收到："+ commonMessage.toString());
                    break;
                //第二用户登录成功
                case CommonMessage.SECOND__LOGIN_SUCCESS_OFFLINE:
                case CommonMessage.SECOND__LOGIN_SUCCESS_ONLINE:
                    LogUtil.d("广播接收器收到："+ commonMessage.toString());
                    //如果连接成功，跳转医护设置界面
                    Log.e("MainActivity","login successfully");
                    Intent activityintent = new Intent(PassiveModeActivity.this,PersonalSettingActivity.class); //新建一个跳转到医护设置界面Activity的显式意图
                    startActivity(activityintent); //启动
                    PassiveModeActivity.this.finish(); //结束当前Activity
                    break;
                //第二用户下线成功
                case CommonMessage.SECOND__DISCONNECTED:
                case CommonMessage.SECOND__LOGOUT:
                    LogUtil.d("广播接收器收到："+ commonMessage.toString());
                    //刷新主界面
                    needAfterMotion = false;
                    PassiveModeActivity.this.recreate();
                    break;
                //获得心率
                case CommonMessage.HEART_BEAT:
                    LogUtil.d("广播接收器收到："+ commonMessage.toString());
                    getrate.setText(commonMessage.getAttachment());
                    break;
                default:
                    LogUtil.e("未知广播，收到message：" + commonMessage.getMsgType());
            }
        }
    }


}