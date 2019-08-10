package com.bdl.aisports.contoller;

import android.util.Log;

import com.bdl.aisports.constant.MotorConstant;
import com.bdl.aisports.util.MessageUtils;

import java.util.Timer;
import java.util.TimerTask;

import static com.bdl.aisports.contoller.Writer.setParameter;

public class Location {
    private static final String TAG = "Location";

//    /**
//     * 复位
//     * @throws Exception
//     */
//    public static void restoration(){
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    setParameter(0, MotorConstant.FAIL_RESET);
//                    Thread.sleep(500);
//                    setParameter(1, MotorConstant.FAIL_RESET);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//
//    }


//    /**
//     * 判断当前位置和设置的位置的关系来决定电机如何运动
//     * @param position
//     * @throws Exception
//     */
//    public static void positionDetermine(int position) throws Exception {
//        position = MessageUtils.getMappedValue(position) * 10000;
//        int currentPosition = Integer.parseInt(Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION));
//        if (currentPosition < position) {
//            setParameter(MotorConstant.initSpeed, MotorConstant.SET_GOING_SPEED);
//            rotateClockwise(position);
//        } else if (currentPosition > position) {
//            setParameter(-MotorConstant.initSpeed, MotorConstant.SET_GOING_SPEED);
//            rotateAnticlockwise(position);
//        }
//    }
//    /**
//     * 顺时针旋转
//     *
//     * @param position
//     */
//    public static void rotateClockwise(final int position) {
//        //每200ms读取一次当前位置
//        final Timer timer = new Timer();
//        TimerTask timerTask = new TimerTask() {
//            @Override
//            public void run() {
//                try {
//                    //读取当前位置
//                    String currentPosition = Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION);
//                    if (currentPosition == null) { //读取失败
//                        return;
//                    }
//                    if (Integer.parseInt(currentPosition) >= position) { //比较当前位置和初始位置
//                        //设置去程速度为0
//                        setParameter(0, MotorConstant.SET_GOING_SPEED);
//                        setParameter(0, MotorConstant.SET_BACK_SPEED);
//                        //关闭返回速度
//                        timer.cancel(); //关闭当前定时轮询任务
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        };
//        timer.schedule(timerTask, 0, 200);
//    }
//
//    /**
//     * 逆时针旋转
//     *
//     * @param position
//     */
//    public static void rotateAnticlockwise(final int position) {
//        //每200ms读取一次当前位置
//        final Timer timer = new Timer();
//        TimerTask timerTask = new TimerTask() {
//            @Override
//            public void run() {
//                try {
//                    //读取当前位置
//                    String currentPosition = Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION);
//                    if (currentPosition == null) { //读取失败
//                        return;
//                    }
//                    if (Integer.parseInt(currentPosition) <= position) { //比较当前位置和初始位置
//                        //设置去程速度为0
//                        setParameter(0, MotorConstant.SET_GOING_SPEED);
//                        setParameter(0, MotorConstant.SET_BACK_SPEED);
//                        //关闭返回速度
//                        timer.cancel(); //关闭当前定时轮询任务
//
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        };
//        timer.schedule(timerTask, 0, 200);
//    }
}
