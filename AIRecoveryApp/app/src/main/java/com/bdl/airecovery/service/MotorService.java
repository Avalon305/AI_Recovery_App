package com.bdl.airecovery.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.constant.MotorConstant;
import com.bdl.airecovery.contoller.MotorProcess;
import com.bdl.airecovery.contoller.Reader;
import com.bdl.airecovery.contoller.Writer;
import com.bdl.airecovery.mao.MotorSocketClient;

import java.util.Timer;
import java.util.TimerTask;


import static com.bdl.airecovery.contoller.Writer.setInitialBounce;
import static com.bdl.airecovery.contoller.Writer.setKeepArmTorque;
import static com.bdl.airecovery.contoller.Writer.setParameter;

public class MotorService extends Service {

    private static final String TAG = "MotorClientService";
    private static Intent initIntent = new Intent("locate"); //运动前初始化的广播
    private static Intent locationIntent = new Intent("location"); //联测定位的广播

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static MotorService instance = null;

    public static MotorService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MotorSocketClient.getInstance().start(); //连接服务端
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        eStopBroadcast(); //急停广播

    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * 动态电机的联测定位
     *
     * @return
     * @throws Exception
     */
    public void motorLocation() throws Exception {
        final Timer timer = new Timer(); //监测状态位
        TimerTask timerTask = new TimerTask() { //轮询当前状态位2
            @Override
            public void run() {
                try {
                    String homingAvailable = Reader.getStatus(Reader.StatusBit.HOMING_POSIAVAILABLE);
                    Log.e(TAG, "homingAvailable" + homingAvailable);
                    if (homingAvailable.equals("1")) { //触发回零开关
                        Thread.sleep(3000);
                        setParameter(0, MotorConstant.HOMING_START_STOP);
                        setParameter(0, MotorConstant.OPERATION_MODE);
                        MotorProcess.motorInitialization();
//                        setParameter(0, MotorConstant.SET_GOING_SPEED);
//                        setParameter(MotorConstant.initSpeed, MotorConstant.SET_BACK_SPEED);
//                        setParameter(1, MotorConstant.ZERO_SPEED_AND_BACK_SPEED_CHANGE); //开启返回速度
//                        rotateClockwise(MyApplication.getInstance().getCurrentDevice().getMaxLimit() * 10000);
//                        setParameter(MotorConstant.initSpeed, MotorConstant.SET_GOING_SPEED);
//                        rotateClockwise(MyApplication.getInstance().getCurrentDevice().getMaxLimit() * 10000);
                        Thread.sleep(8000);
                        //发送联测定位成功的广播
                        locationIntent.putExtra("state", true);
                        sendBroadcast(locationIntent);
                        timer.cancel();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };

        //step 1:读取状态位
        String homingAvailable = Reader.getStatus(Reader.StatusBit.HOMING_POSIAVAILABLE);
        String ready = Reader.getStatus(Reader.StatusBit.READY);
        String inhibit = Reader.getStatus(Reader.StatusBit.INHIBIT);
        //防止定位的时候力矩过小，带不起来
        try {
            setParameter(40 * 100, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
            setParameter(40 * 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e(TAG, "联测定位初始状态位->" +
                "HomingAvailable:" + homingAvailable + " " +
                "Ready:" + ready + " " +
                "inhibit" + inhibit + " ");
        if (homingAvailable.equals("0")
                && ready.equals("0")
                && inhibit.equals("1")) { //第一次上电
            //开机以后上使能
            Writer.setParameter(1, MotorConstant.MOTOR_ENABLE);
            Thread.sleep(1000);
            Writer.setParameter(0, MotorConstant.MOTOR_ENABLE);
            //step 2:进入寻零模式
            setParameter(1, MotorConstant.OPERATION_MODE);
            //step 3:发送HomingStartStop命令
            setParameter(1, MotorConstant.HOMING_START_STOP);
            timer.schedule(timerTask, 0, 200); //开启定时任务
        } else if (homingAvailable.equals("1")
                && ready.equals("1")
                && inhibit.equals("0")) { //上电一次以后再次进入联测定位页面
            MotorProcess.motorInitialization();
            Thread.sleep(5000);
            locationIntent.putExtra("state", true);
            sendBroadcast(locationIntent);
        } else if (homingAvailable.equals("0")
                && ready.equals("1")
                && inhibit.equals("0")) { //上电一次以后再次进入联测定位页面
            //step 2:进入寻零模式
            setParameter(1, MotorConstant.OPERATION_MODE);
            //step 3:发送HomingStartStop命令
            setParameter(1, MotorConstant.HOMING_START_STOP);
            timer.schedule(timerTask, 0, 200); //开启定时任务
        } else {
            //发送联测定位失败的广播
            locationIntent.putExtra("state", false);
            sendBroadcast(locationIntent);
        }
    }

    /**
     * 运动前的初始化
     *
     * @param position              前方限制或者后方限制
     * @param deviceType
     * @param positiveTorqueLimited
     * @param negativeTorqueLimited
     */
    public void initializationBeforeStart(final int position,
                                    final int deviceType,
                                    final int positiveTorqueLimited,
                                    final int negativeTorqueLimited) {
        //防止定位的时候力矩过小，带不起来
        try {
            setParameter(40 * 100, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
            setParameter(40 * 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
            setParameter(0, MotorConstant.SET_BACK_SPEED);
            //开速度
            setParameter(-MotorConstant.initSpeed, MotorConstant.SET_GOING_SPEED);
            setParameter(-MotorConstant.initSpeed, MotorConstant.SET_COMPARE_SPEED);

        } catch (Exception e) {
            e.printStackTrace();
        }

        //每200ms读取一次当前位置
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
                    if (Integer.parseInt(currentPosition) <= position + 50000) { //比较当前位置和初始位置
                        //关速度
                        setParameter(0, MotorConstant.SET_GOING_SPEED);
                        setParameter(0, MotorConstant.SET_COMPARE_SPEED);

                        timer.cancel(); //关闭当前定时轮询任务
                        switch (deviceType) {
                            case 1: //拉设备
                                //初始化顺反向力
                                setParameter((int) ((double)positiveTorqueLimited ), MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
                                setParameter((int) ((double)negativeTorqueLimited ), MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                                setInitialBounce(negativeTorqueLimited + MotorConstant.DIF_BETWEEN_NEG_TORQUE_AND_BOUNCE);
                                setKeepArmTorque(positiveTorqueLimited);
                                break;
                            case 2: //推设备
                                setParameter((int) ((double)positiveTorqueLimited ), MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
                                setParameter((int) ((double)negativeTorqueLimited ), MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                                break;
                        }
                        //发送初始化成功的广播
                        initIntent.putExtra("success", true);
                        sendBroadcast(initIntent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(timerTask, 0, 100);
    }
    /**
     * 急停广播
     */
    private synchronized void eStopBroadcast() {
        final Intent intent = new Intent();
        intent.setAction("E-STOP");
        final Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    String status = Reader.getStatus(Reader.StatusBit.EStop);
                    Log.e("-----", status);
                    if (status != null) {
                        intent.putExtra("state", status);
                        sendBroadcast(intent); //发送广播
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(timerTask, 0, 500);
    }
}
