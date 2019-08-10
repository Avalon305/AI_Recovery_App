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

import static com.bdl.airecovery.contoller.Writer.setInitialBounce;
import static com.bdl.airecovery.contoller.Writer.setKeepArmTorque;
import static com.bdl.airecovery.contoller.Writer.setParameter;

@ContentView(R.layout.activity_mode_adapt)
public class AdaptModeActivity extends BaseActivity {

    private int num = 0;
    private int positiveTorqueLimited;
    private int negativeTorqueLimited;
    private int frontLimitedPosition;
    private int rearLimitedPosition;
    double rate = MyApplication.getCurrentRate();
    private int deviceType;
    private boolean allowRecordNum = true; //允许计数
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

    private boolean needAfterMotion = true;

    //顺反向力handler
    private Handler torqueHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int arg1 = msg.arg1;
            int arg2 = msg.arg2;
            switch (msg.what) {
                case 1:
                    positivenumber.setText(String.valueOf(arg1));
                    break;
                case 2:
                    inversusnumber.setText(String.valueOf(arg2));
                    break;
            }
        }
    };
    private BluetoothReceiver bluetoothReceiver;        //蓝牙广播接收器，监听用户的登录广播
    //TODO:电机相关


    /**
     * 控件绑定
     */
    //TextView
    @ViewInject(R.id.tv_ma_person)
    private TextView person;                //用户名
    @ViewInject(R.id.tv_ma_getrate)
    private TextView getrate;               //心率
    @ViewInject(R.id.tv_ma_getnumber)
    private TextView getnumber;             //次数
    @ViewInject(R.id.tv_ma_gettime)
    private TextView gettime;               //倒计时
    @ViewInject(R.id.tv_ma_positivenumber)
    private TextView positivenumber;        //顺向力值
    @ViewInject(R.id.tv_ma_inversusnumber)
    private TextView inversusnumber;        //反向力值
    @ViewInject(R.id.tv_ma_time)
    private TextView tv_ma_time;            //提示文本：训练/休息倒计时
    private TextView medium_dialog_msg;     //最后5秒模态框 时间文本
    //ImageView
    @ViewInject(R.id.iv_ma_help)
    private ImageView iv_ma_help;           //“帮助”图片按钮
    @ViewInject(R.id.iv_ma_state)
    private ImageView iv_ma_state;    //登录状态
    //Button
    @ViewInject(R.id.btn_ma_coach)
    private Button btn_ma_coach;   //“教练协助/停止协助”按钮
    @ViewInject(R.id.btn_ma_pause)
    private Button btn_ma_pause;   //“暂停”按钮
    //MyBallView
    @ViewInject(R.id.bv_ma_ball)
    private MyBallView ball;                //小球
//    @ViewInject(R.id.test_seekbar)
//    private SeekBar test_seekbar;           //模拟电机推拉值

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
    Timer timer = new Timer();
    locationReceiver LocationReceiver = new locationReceiver();
    IntentFilter filterHR = new IntentFilter();
    private Upload upload = new Upload();
    private MediumDialog mediumDialog;
    private double weight;


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

        //运动过程选择
        chooseDeviceType();

        //模拟电机推拉SeekBar初始化参数
//        test_seekbar.setMax(frontLimitedPosition / 10000 - rearLimitedPosition / 10000);
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

                            setParameter(50000, MotorConstant.SET_LEADS);
                            break;
                        case 2: //推设备
                            Writer.setParameter(frontLimitedPosition / 10000 * 4856, MotorConstant.SET_FRONTLIMIT);
                            Writer.setParameter(rearLimitedPosition  / 10000 * 4856, MotorConstant.SET_REARLIMIT);
                            //初始化速度
                            setParameter(MotorConstant.speed, MotorConstant.SET_BACK_SPEED);
                            setParameter(0, MotorConstant.SET_GOING_SPEED);

                            setParameter(50000, MotorConstant.SET_LEADS);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void chooseDeviceType() {
        //运动过程选择
        switch (deviceType) {
            case 1: //拉设备
                adaptModeProcessByPulling();
                break;
            case 2: //推设备
                adaptModeProcessByPushing();
                break;
            case 3: //躯干扭转组合
                if (motorDirection == 1) {
                    MyApplication.getInstance().motorDirection = 2;
                    int[] newLimitedPosition = MessageUtils.recersalLimit(frontLimitedPosition, rearLimitedPosition, 1);
                    frontLimitedPosition = newLimitedPosition[0];
                    rearLimitedPosition = newLimitedPosition[1];
                    adaptModeProcessByPushing();
                } else if (motorDirection == 2) {
                    MyApplication.getInstance().motorDirection = 1;
                    int[] newLimitedPosition = MessageUtils.recersalLimit(frontLimitedPosition, rearLimitedPosition, 2);
                    frontLimitedPosition = newLimitedPosition[0];
                    rearLimitedPosition = newLimitedPosition[1];
                    adaptModeProcessByPulling();
                }
                break;
        }
    }

    /**
     * 拉设备运动过程
     */
    private void adaptModeProcessByPulling() {
        //打开运动过程
        final int[] lastLocation = {frontLimitedPosition}; //上一次的位置，初始值为前方限制
        final boolean[] countFlag = {false};//计数标志位
        final boolean[] enableGoingSpeedDown = {true};
        final boolean[] enableBackSpeedDown = {true};
        final boolean[] isOpenSpeedEnable = {false};
        final boolean[] haveStopped = {false};
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    //发送消息到handler
                    Message message = countHandler.obtainMessage();
                    message.what = 1;
                    Message torqueMsg = torqueHandler.obtainMessage(); //力矩handler
                    //读取当前位置
                    String currentLocation = Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION);
                    int currentSpeed = Math.abs(Integer.valueOf(Reader.getRespData(MotorConstant.READ_ROTATIONAL_SPEED)));
                    //读取当前力矩限制防止力矩过小
                    String positiveTorque = Reader.getRespData(MotorConstant.READ_POSITIVE_TORQUE_LIMITED);
                    String negativeTorque = Reader.getRespData(MotorConstant.READ_NEGATIVE_TORQUE_LIMITED);
                    int difference = Integer.parseInt(currentLocation) - lastLocation[0]; //本次位置和上次读到的位置差
                    //两端不做速度过快或者过慢的判断
                    if (Integer.valueOf(currentLocation) >= rearLimitedPosition + 5 * 10000 &&
                            Integer.valueOf(currentLocation) <= frontLimitedPosition - 5 * 10000) {
                        isOpenSpeedEnable[0] = true;
                    } else {
                        isOpenSpeedEnable[0] = false;
                    }

                    if (currentSpeed <= 10 && negativeTorqueLimited < MotorConstant.MIN_BACK_TORQUE) {
                        setParameter(MotorConstant.MIN_BACK_TORQUE, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                        haveStopped[0] = true;
                    }
                    if (difference > 0) { //回程
                        //转速过快减小反向力
                        if (currentSpeed > 500
                                && Integer.parseInt(negativeTorque) > 1000
                                && enableBackSpeedDown[0]
                                && isOpenSpeedEnable[0]) {
                            negativeTorqueLimited -= 2 * 100 * rate; //减少反向力
                            torqueMsg.what = 2;
                            torqueMsg.arg2 = Integer.valueOf(inversusnumber.getText().toString()) - 2;
                            torqueHandler.sendMessage(torqueMsg);
                            setParameter(negativeTorqueLimited, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                            setInitialBounce(negativeTorqueLimited + MotorConstant.DIF_BETWEEN_NEG_TORQUE_AND_BOUNCE);
                            enableBackSpeedDown[0] = false;
                        }
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
                            enableBackSpeedDown[0] = true;
                            //无法继续计数和计时
                            countFlag[0] = false;
                        }
                        //更新lastLocation
                        lastLocation[0] = Integer.parseInt(currentLocation);
                    } else if (difference < 0) {//去程
                        //转速超过500，且与最新的限位比较
                        if (currentSpeed >= 450 && currentSpeed <= 1000 && Integer.valueOf(currentLocation) > rearLimitedPosition + 50000) {
                            int leads = currentSpeed / 100 - 2; //提前量
                            setParameter(leads * 10000, MotorConstant.SET_LEADS);
                        } else if (currentSpeed > 1000 && Integer.valueOf(currentLocation) > rearLimitedPosition + 50000) {
                            int leads = currentSpeed / 100 + 3; //提前量
                            setParameter(leads * 10000, MotorConstant.SET_LEADS);
                        }

                        setParameter(positiveTorqueLimited, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
                        //转速过慢减小反向力
                        if (currentSpeed < 200
                                && Integer.parseInt(positiveTorque) > 1000
                                && enableGoingSpeedDown[0]
                                && isOpenSpeedEnable[0]) {
                            Log.e("当前电机反向力", negativeTorque);
                            positiveTorqueLimited -= 2 * 100 * rate; //减少正向力
                            //向handler中发送信息
                            torqueMsg.what = 1;
                            torqueMsg.arg1 = Integer.valueOf(positivenumber.getText().toString()) - 2;
                            torqueHandler.sendMessage(torqueMsg);
                            //设置力矩
                            setParameter(positiveTorqueLimited, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
                            setKeepArmTorque(positiveTorqueLimited);
                            enableGoingSpeedDown[0] = false;
                        }
                        //超过后方限制
                        if (Integer.valueOf(currentLocation) < rearLimitedPosition + 50000) {
                            enableGoingSpeedDown[0] = true;
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

    /**
     * 推设备运动过程
     */
    private void adaptModeProcessByPushing() {
        final int[] lastLocation = {frontLimitedPosition}; //上一次的位置，初始值为前方限制
        final boolean[] countFlag = {false};//计数标志位
        final boolean[] enableGoingSpeedDown = {true};
        final boolean[] enableBackSpeedDown = {true};
        final boolean[] isOpenSpeedEnable = {false};
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    Message message = countHandler.obtainMessage(); //次数handler
                    message.what = 1;
                    Message torqueMsg = torqueHandler.obtainMessage(); //力矩handler
                    //读取当前位置
                    String currentLocation = Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION);
                    //读取当前力矩限制防止力矩过小
                    String positiveTorque = Reader.getRespData(MotorConstant.READ_POSITIVE_TORQUE_LIMITED);
                    String negativeTorque = Reader.getRespData(MotorConstant.READ_NEGATIVE_TORQUE_LIMITED);
                    String speed = Reader.getRespData(MotorConstant.READ_ROTATIONAL_SPEED);
                    int difference = Integer.parseInt(currentLocation) - lastLocation[0]; //本次位置和上次读到的位置差
                    //两端不做速度过快或者过慢的判断
                    if (Integer.valueOf(currentLocation) >= rearLimitedPosition + 5 * 10000 &&
                            Integer.valueOf(currentLocation) <= frontLimitedPosition - 5 * 10000) {
                        isOpenSpeedEnable[0] = true;
                    } else {
                        isOpenSpeedEnable[0] = false;
                    }
                    if (difference > 20000) { //去程
                        //复位反向力
                        setParameter(negativeTorqueLimited, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                        //转速过慢减小反向力
                        if ( Math.abs(Integer.valueOf(speed)) < 200
                                && Integer.parseInt(positiveTorque) > 1000
                                && enableGoingSpeedDown[0]
                                && isOpenSpeedEnable[0]) {
                            negativeTorqueLimited -= 2 * 100 * rate; //减少正向力
                            //向handler中发送信息
                            torqueMsg.what = 1;
                            torqueMsg.arg1 = Integer.valueOf(inversusnumber.getText().toString()) - 2;
                            torqueHandler.sendMessage(torqueMsg);
                            //设置力矩
                            setParameter(negativeTorqueLimited, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                            enableGoingSpeedDown[0] = false;
                        }
                        if (Integer.parseInt(currentLocation) >= frontLimitedPosition - 50000) {
                            enableGoingSpeedDown[0] = true;
                            countFlag[0] = true;
                        }
                        //更新lastLocation
                        lastLocation[0] = Integer.parseInt(currentLocation);
                    } else if (difference < -20000) { //回程
                        //转速过快减小顺向力
                        if (Math.abs(Integer.parseInt(speed)) > 500
                                && Integer.parseInt(negativeTorque) > 1000
                                && enableBackSpeedDown[0]
                                && isOpenSpeedEnable[0]) {
                            Log.e("当前电机转速", speed);
                            positiveTorqueLimited -= 2 * 100 * rate; //减少反向力
                            torqueMsg.what = 2;
                            torqueMsg.arg2 = Integer.valueOf(positivenumber.getText().toString()) - 2;
                            torqueHandler.sendMessage(torqueMsg);
                            setParameter(positiveTorqueLimited, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
                            enableBackSpeedDown[0] = false;
                        }
                        if (Integer.parseInt(currentLocation) <= rearLimitedPosition + 50000) {
                            //次数增加
                            if (countFlag[0] && allowRecordNum) {
                                num++;
                                message.arg1 = num;
                                countHandler.sendMessage(message);
                            }
                            //无法计数和计时
                            enableBackSpeedDown[0] = true;
                            countFlag[0] = false;
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
     * 接收急停广播
     */
    private class eStopBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra("state");
            if (state != null && state.equals("1")) {
                startActivity(new Intent(AdaptModeActivity.this, ScramActivity.class));
                AdaptModeActivity.this.finish();
            }
        }
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
        //获取设备信息
        //判空
        if (MyApplication.getInstance().getCurrentDevice() == null) {
            return;
        }
        if (MyApplication.getInstance().getCurrentDevice().getReverseForce() != null && MyApplication.getInstance().getCurrentDevice().getConsequentForce() != null) {
            positivenumber.setText(MyApplication.getInstance().getCurrentDevice().getConsequentForce());//顺向力数值
            inversusnumber.setText(MyApplication.getInstance().getCurrentDevice().getReverseForce());   //反向力数值
            positiveTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(positivenumber.getText())) * 100 * rate);
            negativeTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(inversusnumber.getText())) * 100 * rate);
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
                        btn_ma_coach.setText("教练协助"); //更新为“教练协助”按钮
                        iv_ma_state.setImageDrawable(getResources().getDrawable((R.drawable.yonghu1)));
                    } else { //否则，不为空串，说明有教练连接蓝牙
                        btn_ma_coach.setText("停止协助"); //更新为“停止协助”按钮
                        iv_ma_state.setImageDrawable(getResources().getDrawable((R.drawable.shou)));
                        //person.append("【调试模式】"); //追加“【调试模式】”文本
                    }
                } else if (MyApplication.getInstance().getUser().getRole().equals("coach")) {
                    btn_ma_coach.setText("个人设置"); //更新为“个人设置”按钮
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
        final int frequency = 50; //过渡动画中100ms内的绘制频率
        final int transInterval = interval/frequency;   //过渡动画的绘制间隔
        drawCartoonThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        //获取推拉速度
                        ball.speedY = Float.parseFloat(Reader.getRespData(MotorConstant.READ_ROTATIONAL_SPEED));
//                        ball.speedY = 30;
                        //获取推拉位移
                        if(lastPowerX == 0) {
                            //第一次获取，需要获取两次，才能动画过渡
                            lastPowerX = Float.parseFloat(Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION)) / 10000;
//                            lastPowerX = test_seekbar.getProgress() + rearLimitedPosition / 10000;
                            Thread.sleep(interval);
                            curPowerX = Float.parseFloat(Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION)) / 10000;
//                            curPowerX = test_seekbar.getProgress() + rearLimitedPosition / 10000;
                        } else {
                            //从第二次开始，将上一次的保存，并再获取一次，进行动画过渡
                            lastPowerX = ball.powerX;
                            curPowerX = Float.parseFloat(Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION)) / 10000;
//                            curPowerX = test_seekbar.getProgress() + rearLimitedPosition / 10000;
                        }

                        //动画过渡，每 transInterval ms绘制一次
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

                        Thread.sleep(interval+2);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

//                    Log.d("seekbar","seekbarProgress=" + test_seekbar.getProgress());
//                    Log.d("ball0","frontLimit=" + ball.frontLimit + "\tbackLimit=" + ball.backLimit);
//                    Log.d("ball1","powerX=" + ball.powerX + "\tspeedY=" + ball.speedY);
//                    Log.d("ball2","画布坐标系:currentX=" + ball.currentX + "\tcurrentY=" + ball.currentY);
//                    Log.d("ball3","计算坐标系:calculateX=" + ball.calculateX + "\tcalculateY=" + ball.calculateY);
                }
            }
        });
    }

    /**
     * 右侧动画设置（与电机联动）
     */
    private void setCartoon() {
        ball.powerX = 0; //电机传进来的物理位移
        ball.speedY = 0; //电机传进来的推拉速度
        ball.MaxR = 600; //合理的速度范围的最大边界值
        ball.MinR = 300; //合理的速度范围的最小边界值
        ball.frontLimit = frontLimitedPosition / 10000 - 10; //前方限制
        ball.backLimit = rearLimitedPosition / 10000 + 10; //后方限制
        ball.stateS = 1; //设置小球状态：可运动
    }
    //扫描教练的定时任务
    Timer timerLog = new Timer();
    TimerTask taskLog = new TimerTask() {
        @Override
        public void run() {
            Intent intent = new Intent(AdaptModeActivity.this, BluetoothService.class);
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
            Intent intent2 = new Intent(AdaptModeActivity.this, CardReaderService.class);
            intent2.putExtra("command", CommonCommand.SECOND__LOGIN.value());
            startService(intent2);
            timerLog.schedule(taskLog,0,2000);
        }
        //如果是停止协助
        else if (btn_ma_coach.getText().equals("停止协助")) {
            Intent intent2 = new Intent(AdaptModeActivity.this, CardReaderService.class);
            intent2.putExtra("command", CommonCommand.SECOND__LOGOUT.value());
            startService(intent2);
            Intent intent = new Intent(AdaptModeActivity.this, BluetoothService.class);
            intent.putExtra("command", CommonCommand.SECOND__LOGOUT.value());
            startService(intent);
            Log.d("AdaptModeActivity", "request to logout");
        }
        //如果是个人设置
        else if (btn_ma_coach.getText().equals("个人设置")) {
            //跳转个人设置界面
            Intent intent = new Intent(AdaptModeActivity.this, PersonalSettingActivity.class); //新建一个跳转到个人设置界面Activity的显式意图
            startActivity(intent); //启动
            AdaptModeActivity.this.finish(); //结束当前Activity
        }

    }

    CommonDialog commonDialog;
    /**
     * “暂停”按钮 监听
     * 点击弹出模态框，同时电机暂停
     */
    @Event(R.id.btn_ma_pause)
    private void pauseClick(View v) {
        allowRecordNum = false;
        commonDialog = new CommonDialog(AdaptModeActivity.this);
        commonDialog.setTitle("温馨提示");
        commonDialog.setMessage("您确定要放弃本次训练吗？");
        commonDialog.setNegativeBtnText("结束");
        commonDialog.setPositiveBtnText("继续");
        //“结束”按钮 监听
        commonDialog.setOnNegativeClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commonDialog.dismiss();
                Intent intentLog2 = new Intent(AdaptModeActivity.this, CardReaderService.class);
                intentLog2.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
                startService(intentLog2);
                Intent intentLog = new Intent(AdaptModeActivity.this, BluetoothService.class);
                intentLog.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
                startService(intentLog);
                //新建一个跳转到待机界面Activity的显式意图
                Intent intent = new Intent(AdaptModeActivity.this, LoginActivity.class);
                //启动
                startActivity(intent);
                //结束当前Activity
                AdaptModeActivity.this.finish();
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

    /**
     * 最后5秒倒计时 模态框
     */
    private void Last5sAlertDialog() {
        mediumDialog = new MediumDialog((AdaptModeActivity.this));
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
                upload.setCalorie_(countEnergy(Integer.parseInt(getnumber.getText().toString()),Integer.parseInt(positivenumber.getText().toString())));

                //2.获取训练时长
                //已经在第一次校准时间处获取
                    //3.最终顺向力
                upload.setForwardForce_(Integer.parseInt(positivenumber.getText().toString()));
                    //4.最终反向力
                upload.setReverseForce_(Integer.parseInt(inversusnumber.getText().toString()));
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
                Intent intent = new Intent(AdaptModeActivity.this, ByeActivity.class);
                //启动
                startActivity(intent);
                //结束当前Activity
                AdaptModeActivity.this.finish();
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
                helpDialog = new LargeDialogHelp(AdaptModeActivity.this);
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
                    if (intent.getStringExtra("log").equals("twologicard") || intent.getStringExtra("log").equals("twologiblue")) {
                        //如果连接成功，跳转个人设置界面
                        Log.e("StandardModeActivity", "login successfully");
                        Intent activityintent = new Intent(AdaptModeActivity.this, PersonalSettingActivity.class); //新建一个跳转到个人设置界面Activity的显式意图
                        startActivity(activityintent); //启动
                        AdaptModeActivity.this.finish(); //结束当前Activity
                    } else if (intent.getStringExtra("log").equals("twologocard") || intent.getStringExtra("log").equals("twologoblue")) {
                        //刷新界面
                        AdaptModeActivity.this.recreate();
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
        unregisterReceiver(bluetoothReceiver);
        timer.cancel(); //结束定时任务
        if (needAfterMotion) {
            MotorProcess.motorInitialization();
        }
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
            Log.e("BtTest","收到message：" + commonMessage.toString());
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
                    //如果连接成功，跳转个人设置界面
                    Log.e("MainActivity","login successfully");
                    Intent activityintent = new Intent(AdaptModeActivity.this,PersonalSettingActivity.class); //新建一个跳转到个人设置界面Activity的显式意图
                    startActivity(activityintent); //启动
                    AdaptModeActivity.this.finish(); //结束当前Activity
                    break;
                //第二用户下线成功
                case CommonMessage.SECOND__DISCONNECTED:
                case CommonMessage.SECOND__LOGOUT:
                    LogUtil.d("广播接收器收到："+ commonMessage.toString());
                    //刷新主界面
                    needAfterMotion = false;
                    AdaptModeActivity.this.recreate();
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
