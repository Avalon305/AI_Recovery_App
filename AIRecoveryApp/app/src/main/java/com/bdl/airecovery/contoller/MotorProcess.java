package com.bdl.airecovery.contoller;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.constant.MotorConstant;
import com.bdl.airecovery.entity.CalibrationParameter;

import org.xutils.DbManager;
import org.xutils.ex.DbException;

import java.util.Timer;
import java.util.TimerTask;

import static com.bdl.airecovery.contoller.Writer.setParameter;

public class MotorProcess {

    private static int speed = MyApplication.getInstance()
            .getCalibrationParam()
            .getNormalSpeed()
            * 100;
    /**
     * 复位
     * @throws Exception
     */
    public static void restoration(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    setParameter(0, MotorConstant.FAIL_RESET);
                    Thread.sleep(500);
                    setParameter(1, MotorConstant.FAIL_RESET);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 电机各种系数的初始化
     */
    public static void motorInitialization() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //复位顺反向力
                    setParameter(40 * 100, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
                    setParameter(40 * 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);

                    //复位来程去程速度
                    setParameter(0, MotorConstant.SET_GOING_SPEED);
                    setParameter(0, MotorConstant.SET_COMPARE_SPEED);
                    setParameter(speed, MotorConstant.SET_BACK_SPEED);

                    //复位前后方限制
                    Writer.setParameter( 190 * 4856, MotorConstant.SET_FRONTLIMIT);
                    Writer.setParameter(0, MotorConstant.SET_REARLIMIT);

                    Writer.setInitialBounce(3000);
                    Writer.setKeepArmTorque(1500);

                    Writer.setParameter(9000, MotorConstant.SET_PUSH_TORQUE);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    public static void motorDirection(int location) throws Exception {
        location *=  10000;
        int currentPosition = Integer.parseInt(Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION));
        if (currentPosition < location) {
            rotateClockwise(location);//顺时针
        } else if (currentPosition > location) {
            rotateAnticlockwise(location);//逆时针
        }
    }


    /**
     * 顺时针旋转
     * @param position
     */
    public static void rotateClockwise(final int position) throws Exception {
        //复位来程去程速度
        setParameter(0, MotorConstant.SET_GOING_SPEED);
        setParameter(0, MotorConstant.SET_COMPARE_SPEED);
        setParameter(speed, MotorConstant.SET_BACK_SPEED);
        setParameter( position / 10000 * 4856, MotorConstant.SET_FRONTLIMIT);
        /*final Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    //读取当前位置
                    String currentPosition = Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION);
                    if (currentPosition == null) { //读取失败
                        return;
                    }
                    if (Integer.parseInt(currentPosition) >= position - 20000) { //比较当前位置和初始位置
                        //设置去程速度为0
                        setParameter(0, MotorConstant.SET_BACK_SPEED);
                        //关闭返回速度
                        timer.cancel(); //关闭当前定时轮询任务
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(timerTask, 0, 50);*/
    }

    /**
     * 逆时针旋转
     *
     * @param position
     */
    public static void rotateAnticlockwise(final int position) throws Exception {
        setParameter(0, MotorConstant.SET_BACK_SPEED);
        //开启来程速度
        setParameter(-speed, MotorConstant.SET_GOING_SPEED);
        setParameter(-speed, MotorConstant.SET_COMPARE_SPEED);
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
                    if (Integer.parseInt(currentPosition) <= position) { //比较当前位置和初始位置
                        setParameter((Integer.valueOf(currentPosition ) / 10000 - 10)* 4856, MotorConstant.SET_FRONTLIMIT);
                        //设置去程速度为0
                        setParameter(0, MotorConstant.SET_GOING_SPEED);
                        setParameter(0, MotorConstant.SET_COMPARE_SPEED);
                        //关闭返回速度
                        timer.cancel(); //关闭当前定时轮询任务

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(timerTask, 0, 50);
    }

}
