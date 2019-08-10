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
import com.bdl.airecovery.entity.Personal;
import com.bdl.airecovery.entity.Upload;
import com.bdl.airecovery.service.BluetoothService;
import com.bdl.airecovery.service.CardReaderService;
import com.bdl.airecovery.util.MessageUtils;
import com.bdl.airecovery.util.SendReqOfCntTimeUtil;
import com.google.gson.Gson;

import org.xutils.common.util.LogUtil;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

import static com.bdl.airecovery.contoller.Writer.setParameter;

@ContentView(R.layout.activity_mode_heartrate)
public class HeartRateModeActivity extends BaseActivity {
    //TODO:电机相关
    private int num = 0;
    private int positiveTorqueLimited;
    private int negativeTorqueLimited;
    private int frontLimitedPosition;
    private int rearLimitedPosition;
    double rate = MyApplication.getCurrentRate();
    private int deviceType;
    int motorDirection = MyApplication.getInstance().motorDirection;
    private boolean allowRecordNum = true; //允许计数
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
    //TODO:电机相关
    private boolean needAfterMotion = true;

    /**
     * 定义变量
     */
    private Timer animTimer = new Timer();

    private int timeRate = 126;             //时时心率
    private int maxRate = 190;              //最大心率
    private int suitRate = 147;             //最宜心率
    private int lowRate = 128;              //低于预期的心率（生活心率）
    private int readyRate = 141;            //热身心率
    private int fatRate = 153;             //燃脂心率
    private int heartRate = 166;            //心肺心率
    private int muscleRate = 179;           //肌力心率
    private int extremeRate = 192;          //极限心率
    private int uploadMax = 0;              //上传的最大心率
    private int uploadMin = 220;            //上传的最小心率
    private int uploadAvg = 0;              //上传的平均心率
    private int heartSum = 0;               //心率之和
    private int pastRate = 0;               //用于对比时时心率
    private int i = 1;                      //用于统计时时心率的个数
    private int c = 0;                      //心率折线
    private int h = 1;                      //用于标识心率计算
    private int localCountDownType = 0;     //本机倒计时类型（0运动，1休息）
    private Thread localCountDownThread;    //本机倒计时线程
    private int localCountDown;             //本机倒计时（单位：秒）
    private Handler handler;                //用于在UI线程中获取倒计时线程创建的Message对象，得到倒计时秒数与时间类型
    private Boolean isAlert = false;        //标识是否弹5s倒计时模态框
    private List<Personal> personalList;    //个人设置表
    private SendReqOfCntTimeUtil sendReqOfCntTimeUtil; //发送同步时间请求的工具
    private Upload upload = new Upload();
    private double weight;


    /*折线图相关变量*/
    Timer timer = new Timer();
    ArrayList<PointValue> pointValueList;
    ArrayList<PointValue> points;
    ArrayList<Line> linesList;
    Axis axisX;// X轴属性，Y轴类同
    Axis axisY;
    LineChartData lineChartData;
    boolean isFinish;
    int position = 0;

    /**
     * 获取控件
     */
    //TextView
    @ViewInject(R.id.tv_mh_person)
    private TextView person;                //用户名
    @ViewInject(R.id.tv_mh_getrate)
    private TextView getrate;               //心率
    @ViewInject(R.id.tv_mh_getnumber)
    private TextView getnumber;             //次数
    @ViewInject(R.id.tv_mh_gettime)
    private TextView gettime;               //倒计时
    @ViewInject(R.id.tv_mh_positivenumber)
    private TextView positivenumber;        //顺向力值
    @ViewInject(R.id.tv_mh_inversusnumber)
    private TextView inversusnumber;        //反向力值
    @ViewInject(R.id.tv_mh_ratespace)
    private TextView ratespace;             //心率区间
    @ViewInject(R.id.tv_mh_tips)
    private TextView tips;                  //心率区间提示信息
    @ViewInject(R.id.tv_mh_tips1)
    private TextView tips1;                 //心率区间提示信息
    @ViewInject(R.id.tv_mh_getrate)
    private TextView heartrate;             //心率显示
    @ViewInject(R.id.tv_mh_time)
    private TextView tv_mh_time;            //提示文本 训练/休息倒计时
    private TextView medium_dialog_msg;     //最后5秒模态框 时间文本
    //ImageView
    @ViewInject(R.id.iv_mh_help)
    private ImageView iv_mh_help;           //帮助图标
    @ViewInject(R.id.iv_mh_state)
    private ImageView iv_mh_state;          //登录状态
    //Button
    @ViewInject(R.id.btn_mh_coach)
    private Button btn_mh_coach;            //教练协助按钮
    @ViewInject(R.id.btn_mh_pause)
    private Button btn_mh_pause;            //暂停按钮
    //LineChartView
    @ViewInject(R.id.mh_chart)
    private LineChartView mh_chart;         //折线图

    //广播对象
    locationReceiver LocationReceiver = new locationReceiver();
    IntentFilter filterHR = new IntentFilter();
    private BluetoothReceiver bluetoothReceiver;        //蓝牙广播接收器，监听用户的登录广播

    /*  设置 ball.stateS=1; 小球将会开始运动，stateS在widget/MyBallViewHeart路径下，public属性，可以通过外界访问获得或改变他的值
     * 在MyBallViewHeart中，我暂时直接设置为1了，所以打开这个界面小球将会自己运动 */

    /**
     * 程序执行入口
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        needAfterMotion = true;
        //隐藏状态栏，导航栏
        initImmersiveMode();
        //接收心率广播注册
        filterHR.addAction("heartrate");
        registerReceiver(LocationReceiver, filterHR);
        initMotor();
        //心率,次数
        mainEvent();
        //调整顺反向力值
        strengthEvent();
        //查询用户信息,获取用户名
        queryUserInfo();
        syncCurrentTime(); //同步当前时间

        //注册蓝牙用监听器
        bluetoothReceiver = new BluetoothReceiver();
        IntentFilter intentFilter = new IntentFilter("com.bdl.bluetoothmessage");
        registerReceiver(bluetoothReceiver, intentFilter);

        initAxisView();
        showMovingLineChart();

        chooseDeviceType();
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

    private void chooseDeviceType() {
        //运动过程选择
        switch (deviceType) {
            case 1: //拉设备
                heartRateModeProcessByPulling();
                break;
            case 2: //推设备
                heartRateModeProcessByPushing();
                break;
            case 3: //躯干扭转组合
                if (motorDirection == 1) {
                    MyApplication.getInstance().motorDirection = 2;
                    int[] newLimitedPosition = MessageUtils.recersalLimit(frontLimitedPosition, rearLimitedPosition, 1);
                    frontLimitedPosition = newLimitedPosition[0];
                    rearLimitedPosition = newLimitedPosition[1];
                    heartRateModeProcessByPushing();
                } else if (motorDirection == 2) {
                    MyApplication.getInstance().motorDirection = 1;
                    int[] newLimitedPosition = MessageUtils.recersalLimit(frontLimitedPosition, rearLimitedPosition, 2);
                    frontLimitedPosition = newLimitedPosition[0];
                    rearLimitedPosition = newLimitedPosition[1];
                    heartRateModeProcessByPulling();
                }
                break;
        }
    }

    /**
     * 拉设备运动过程
     */
    private void heartRateModeProcessByPulling() {
        //打开运动过程
        final int[] lastLocation = {frontLimitedPosition}; //上一次的位置，初始值为前方限制
        final boolean[] haveStopped = {false};
        final boolean[] countFlag = {false};//计数标志位
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    //发送消息到handler
                    Message message = countHandler.obtainMessage();
                    message.what = 1;
                    //读取当前位置
                    String currentLocation = Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION);
                    int currentSpeed = Math.abs(Integer.valueOf(Reader.getRespData(MotorConstant.READ_ROTATIONAL_SPEED)));
                    int difference = Integer.parseInt(currentLocation) - lastLocation[0]; //本次位置和上次读到的位置差
                    if (currentSpeed <= 10 && negativeTorqueLimited < MotorConstant.MIN_BACK_TORQUE) {
                        setParameter(MotorConstant.MIN_BACK_TORQUE, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                        haveStopped[0] = true;
                    }
                    if (difference > 20000) { //回程
                        //转速超过500，且与最新的限位比较，如果距离大于20000，则可以继续更改，考虑一些延时的因素
                        if (currentSpeed >= 450 && currentSpeed <= 1000 && Integer.valueOf(currentLocation) > rearLimitedPosition + 50000) {
                            int leads = currentSpeed / 100 - 2; //提前量
                            setParameter(leads * 10000, MotorConstant.SET_LEADS);
                        } else if (currentSpeed > 1000 && Integer.valueOf(currentLocation) > rearLimitedPosition + 50000) {
                            int leads = currentSpeed / 100 + 3; //提前量
                            setParameter(leads * 10000, MotorConstant.SET_LEADS);
                        }
                        //超过前方限制
                        if (Integer.valueOf(currentLocation) >= frontLimitedPosition - 50000) {
                            if (haveStopped[0]) { //是否需要恢复反向力量
                                setParameter(negativeTorqueLimited, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                                haveStopped[0] = false;
                            }
                            //取消返回速度
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

                        //超过后方限制
                        if (Integer.valueOf(currentLocation) < rearLimitedPosition + 50000) {
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
    private void heartRateModeProcessByPushing() {
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
                        if (Integer.parseInt(currentLocation) >= frontLimitedPosition - 5) {
                            flag[0] = true;
                        }
                        //更新lastLocation
                        lastLocation[0] = Integer.parseInt(currentLocation);
                    } else if (difference < -20000) { //回程
                        if (Integer.parseInt(currentLocation) <= rearLimitedPosition + 5) {
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
     * 折线图动画效果
     */
    private void initAxisView() {
        pointValueList = new ArrayList<PointValue>();
        linesList = new ArrayList<Line>();

        /** 初始化Y轴 */
        axisY = new Axis();
        axisY.setName("心率(次数)");//添加Y轴的名称
        axisY.setHasLines(false);//Y轴分割线
        axisY.setTextSize(30);//设置字体大小
        axisY.setTextColor(Color.parseColor("#1E90FF"));//设置Y轴颜色，默认浅灰色
        lineChartData = new LineChartData(linesList);
        lineChartData.setAxisYLeft(axisY);//设置Y轴在左边

        /** 初始化X轴 */
        axisX = new Axis();
        axisX.setHasTiltedLabels(false);//X坐标轴字体是斜的显示还是直的，true是斜的显示
        axisX.setTextColor(Color.parseColor("#1E90FF"));//设置X轴颜色
        axisX.setName("时间(s)");//X轴名称
        axisX.setHasLines(true);//X轴分割线
        axisX.setTextSize(15);//设置字体大小
        axisX.setMaxLabelChars(0);//设置0的话X轴坐标值就间隔为1
        List<AxisValue> mAxisXValues = new ArrayList<AxisValue>();
        for (int i = 0; i < 61; i++) {
            mAxisXValues.add(new AxisValue(i).setLabel(i + ""));
        }
        axisX.setValues(mAxisXValues);//填充X轴的坐标名称
        lineChartData.setAxisXBottom(axisX);//X轴在底部

        mh_chart.setLineChartData(lineChartData);

        Viewport port = initViewPort(-10, 0);//初始化X轴10个间隔坐标
        mh_chart.setCurrentViewportWithAnimation(port);
        mh_chart.setInteractive(false);//设置不可交互
        mh_chart.setScrollEnabled(true);
        mh_chart.setValueTouchEnabled(false);
        mh_chart.setFocusableInTouchMode(false);
        mh_chart.setViewportCalculationEnabled(false);
        mh_chart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        mh_chart.startDataAnimation();

        loadData();//加载待显示数据
    }

    private Viewport initViewPort(float left, float right) {
        Viewport port = new Viewport();
        port.top = suitRate * 2;//Y轴上限，固定(不固定上下限的话，Y轴坐标值可自适应变化)
        port.bottom = 0;//Y轴下限，固定
        port.left = left;//X轴左边界，变化
        port.right = right;//X轴右边界，变化
        return port;
    }

    private void loadData() {
        points = new ArrayList<PointValue>();
    }

    private void showMovingLineChart() {
        animTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                int chart_heartrate;
                if (getrate.getText().toString().equals("--")) {
                    chart_heartrate = 0;
                } else {
                    chart_heartrate = Integer.valueOf(getrate.getText().toString());
                }
                points.add(new PointValue(position, chart_heartrate));
                if (true) {
                    Log.d("心率模式", "更新折线图：" + points.get(position).toString());
                    pointValueList.add(points.get(position));//实时添加新的点

                    //根据新的点的集合画出新的线
                    Line line = new Line(pointValueList);
                    line.setShape(ValueShape.CIRCLE);//设置折线图上数据点形状为 圆形 （共有三种 ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.DIAMOND）
                    line.setCubic(true);//曲线是否平滑，true是平滑曲线，false是折线
                    line.setHasLabels(true);//数据是否有标注
                    //        line.setHasLabelsOnlyForSelected(true);//点击数据坐标提示数据,设置了line.setHasLabels(true);之后点击无效
                    line.setHasLines(true);//是否用线显示，如果为false则没有曲线只有点显示
                    line.setHasPoints(true);//是否显示圆点 ，如果为false则没有原点只有点显示（每个数据点都是个大圆点）
                    line.setFilled(true);
                    line.setColor(Color.parseColor("#1E90FF"));
                    linesList = new ArrayList<Line>();
                    linesList.add(line);
                    lineChartData = new LineChartData(linesList);
                    lineChartData.setAxisYLeft(axisY);//设置Y轴在左
                    lineChartData.setAxisXBottom(axisX);//X轴在底部
                    mh_chart.setLineChartData(lineChartData);

                    float xAxisValue = points.get(position).getX();
                    //根据点的横坐标实时变换X坐标轴的视图范围
                    Viewport port;
                    if (xAxisValue > 0) {
                        port = initViewPort(xAxisValue - 10, xAxisValue);
                    } else {
                        port = initViewPort(-10, 0);
                    }
                    mh_chart.setMaximumViewport(port);
                    mh_chart.setCurrentViewport(port);

                    position++;

                }
            }
        }, 1000, 1000);
    }


    /**
     * 当Activity准备好和用户进行交互时，调用onResume()
     * 此时Activity处于【运行状态】
     */
    @Override
    protected void onResume() {
        super.onResume();
        //注册急停广播
        IntentFilter filter = new IntentFilter();
        filter.addAction("E-STOP");
        eStopReceiver = new eStopBroadcastReceiver();
        registerReceiver(eStopReceiver, filter);

        if (localCountDownThread == null) {
            CreatelocalCountDownTheard(); //创建本机倒计时线程
            localCountDownThread.start(); //启动本机倒计时线程
        }
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
        //停止Timer TimerTask
        if (sendReqOfCntTimeUtil.timer != null && sendReqOfCntTimeUtil.timerTask != null) {
            sendReqOfCntTimeUtil.timerTask.cancel();
            sendReqOfCntTimeUtil.timer.cancel();
            //Log.d("同步倒计时","HeartRateModeActivity：停止TimerTask");
        }
        if (animTimer != null) {
            animTimer.cancel();
            animTimer.purge();
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
                        btn_mh_coach.setText("教练协助"); //更新为“教练协助”按钮
                        iv_mh_state.setImageDrawable(getResources().getDrawable((R.drawable.yonghu1)));
                    } else { //否则，不为空串，说明有教练连接蓝牙
                        btn_mh_coach.setText("停止协助"); //更新为“停止协助”按钮
                        iv_mh_state.setImageDrawable(getResources().getDrawable((R.drawable.shou)));
                        //person.append("【调试模式】"); //追加“【调试模式】”文本
                    }
                } else if (MyApplication.getInstance().getUser().getRole().equals("coach")) {
                    btn_mh_coach.setText("个人设置"); //更新为“个人设置”按钮
                    iv_mh_state.setImageDrawable(getResources().getDrawable((R.drawable.guanliyuan1)));
                    //person.append("【教练用户】"); //追加“【教练用户】”文本
                } else {
                    //person.append("【测试模式】"); //追加“【测试模式】”文本
                    iv_mh_state.setImageDrawable(getResources().getDrawable((R.drawable.banshou1)));
                }
            }
            weight = MyApplication.getInstance().getUser().getWeight();

        }
    }

    /**
     * 调整顺反向力值
     */
    private void strengthEvent() {

        //获取设备信息
        if (MyApplication.getInstance().getCurrentDevice() != null) {
            if (MyApplication.getInstance().getCurrentDevice().getReverseForce() != null && MyApplication.getInstance().getCurrentDevice().getConsequentForce() != null) {
                positivenumber.setText(MyApplication.getInstance().getCurrentDevice().getConsequentForce());//顺向力数值
                //将最终顺向力值设置到上传结果
//                MyApplication.getUpload().setForwardForce_(Double.valueOf(positivenumber.getText().toString()));
                inversusnumber.setText(MyApplication.getInstance().getCurrentDevice().getReverseForce());   //反向力数值
                //将最终反向力值设置到上传结果
//                MyApplication.getUpload().setReverseForce_(Double.valueOf(inversusnumber.getText().toString()));
            }
        }
        switch (deviceType) {
            case 1: //拉设备
                positiveTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(positivenumber.getText())) * 100 * rate);
                negativeTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(inversusnumber.getText())) * 100 * rate);
                Log.e("顺向力+反向力", String.valueOf(positiveTorqueLimited )+ String.valueOf(negativeTorqueLimited));
                break;
            case 2: //推设备
                positiveTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(inversusnumber.getText())) * 100 * rate);
                negativeTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(positivenumber.getText())) * 100 * rate);
                Log.e("顺向力+反向力", String.valueOf(positiveTorqueLimited )+ String.valueOf(negativeTorqueLimited));
                break;
            case 3:
                switch (motorDirection) {
                    case 1:
                        positiveTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(inversusnumber.getText())) * 100 * rate);
                        negativeTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(positivenumber.getText())) * 100 * rate);
                        break;
                    case 2:
                        positiveTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(positivenumber.getText())) * 100 * rate);
                        negativeTorqueLimited = (int) ((double)Integer.parseInt(String.valueOf(inversusnumber.getText())) * 100 * rate);
                        break;
                }
                break;
        }
    }


    /**
     * 接收急停广播
     */
    private class eStopBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra("state");
            if (state != null && state.equals("1")) {
                startActivity(new Intent(HeartRateModeActivity.this, ScramActivity.class));
                HeartRateModeActivity.this.finish();
            }
        }
    }

    /**
     * 广播接收类，获取心率
     */
    public class locationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            try {
                if (intentAction.equals("heartrate")) {
                    //设置时时心率的值
                    heartrate.setText(intent.getStringExtra("heartrate"));
                    //获得时时心率
                    timeRate = Integer.valueOf(intent.getStringExtra("heartrate"));
                    //设置对比心率的值
                    if (h == 1) {
                        h = 0;
                        pastRate = timeRate;
                        heartSum = pastRate;
                    }
                    //时时心率等于最宜心率
                    if (timeRate == suitRate) {
                        //设置心率区间值
                        ratespace.setText("最宜心率");
                        ratespace.setTextColor(getResources().getColor(R.color.green));
                        tips.setText("燃脂区间");
                        tips.setTextColor(getResources().getColor(R.color.green));
                        tips1.setText("很棒多多保持！");

                    }
                    //时时心率等于最大心率
                    if (timeRate == maxRate) {

                        //设置心率区间值
                        ratespace.setText("最大心率");
                        ratespace.setTextColor(getResources().getColor(R.color.qmui_config_color_red));
                        tips.setText("预警区间");
                        tips.setTextColor(getResources().getColor(R.color.qmui_config_color_red));
                        //TODO:停止继续增加力，并且自动减少顺向力/反向力
                        //设置顺向力值
//            if (1 == 1 && Integer.valueOf(positivenumber.getText().toString()) > 0){
//                positivenumber.setText(Integer.valueOf(positivenumber.getText().toString()) - 1 + "");
//            }
                        //设置反向力值
//            if (1 == 1 && Integer.valueOf(inversusnumber.getText().toString()) > 0){
//                inversusnumber.setText(Integer.valueOf(inversusnumber.getText().toString()) - 1 + "");
//            }
                    }
                    //时时心率小于等于最低心率(生活区间)
                    if (timeRate <= lowRate) {
                        //设置心率值
                        //getrate.setText();
                        //设置心率区间值
                        ratespace.setText("低于运动心率");
                        ratespace.setTextColor(getResources().getColor(R.color.btn_filled_blue_bg_normal));
                        tips.setText("生活区间");
                        tips.setTextColor(getResources().getColor(R.color.btn_filled_blue_bg_normal));
                        tips1.setText("希望多多加油！");
                    }
                    //时时心率在热身区间范围内
                    if (lowRate < timeRate && timeRate <= readyRate) {
                        //设置心率值
                        //getrate.setText();
                        //设置心率区间值
                        tips.setText("热身区间");
                        tips.setTextColor(getResources().getColor(R.color.yellow));
                    }
                    //时时心率在燃脂区间范围内
                    if (readyRate < timeRate && timeRate <= fatRate) {
                        //设置心率值
                        //getrate.setText();
                        //设置心率区间值
                        tips.setText("燃脂区间");
                        tips.setTextColor(getResources().getColor(R.color.green));
                    }
                    //时时心率在心肺区间范围内
                    if (fatRate < timeRate && timeRate <= heartRate) {
                        //设置心率值
                        //getrate.setText();
                        //设置心率区间值
                        tips.setText("心肺区间");
                        tips.setTextColor(getResources().getColor(R.color.DeepSkyBlue));
                    }
                    //时时心率在肌力区间范围内
                    if (heartRate < timeRate && timeRate <= muscleRate) {
                        //设置心率值
                        //getrate.setText();
                        //设置心率区间值
                        tips.setText("肌力区间");
                        tips.setTextColor(getResources().getColor(R.color.tintRed));
                    }
                    //时时心率在极限区间范围内
                    if (muscleRate < timeRate && timeRate <= extremeRate) {
                        //设置心率值
                        //getrate.setText();
                        //设置心率区间值
                        tips.setText("极限区间");
                        tips.setTextColor(getResources().getColor(R.color.qmui_config_color_red));
                    }
                    //时时心率在预警区间范围内
                    if (extremeRate < timeRate) {
                        //设置心率值
                        //getrate.setText();
                        //设置心率区间值
                        tips.setText("预警区间");
                        tips.setTextColor(getResources().getColor(R.color.purple));
                    }
                    //上传运动中的最大心率，最小心率，平均心率
                    //取得上传结果类
//                    Upload upload = MyApplication.getUpload();
                    //设置上传的最大心率
                    if (timeRate > uploadMax) {
                        uploadMax = timeRate;
                        upload.setHeartRateMax_(uploadMax);
                    }
                    //设置上传的最小心率
                    if (timeRate < uploadMin) {
                        uploadMin = timeRate;
                        upload.setHeartRateMin_(uploadMin);
                    }
                    //设置上传的平均心率
                    if (pastRate != timeRate) {
                        ++i;
                        heartSum += timeRate;
                        pastRate = timeRate;
                        uploadAvg = heartSum / i;
                        upload.setHeartRateAvg_(uploadAvg);
                    }
                } else if (intentAction.equals("log")) {
                    if (intent.getStringExtra("log").equals("twologicard") || intent.getStringExtra("log").equals("twologiblue")) {
                        Log.e("HeartRateModeActivity", "login successfully");
                        //如果连接成功，跳转个人设置界面
                        Intent activityintent = new Intent(HeartRateModeActivity.this, PersonalSettingActivity.class); //新建一个跳转到个人设置界面Activity的显式意图
                        startActivity(activityintent); //启动
                        finish(); //结束当前Activity
                    } else if (intent.getStringExtra("log").equals("twologocard") || intent.getStringExtra("log").equals("twologoblue")) {
                        Log.e("HeartRateModeActivity", "logout successfully");
                        //跳转回主界面
                        Intent activityintent = new Intent(HeartRateModeActivity.this, MainActivity.class); //新建一个跳转到主界面Activity的显式意图
                        startActivity(activityintent); //启动
                        finish(); //结束当前Activity
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //卸载广播
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(LocationReceiver);
        unregisterReceiver(eStopReceiver);
        timer.cancel(); //结束定时任务
        try {
            MotorProcess.motorInitialization();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (needAfterMotion) {
            MotorProcess.motorInitialization();
        }
        unregisterReceiver(bluetoothReceiver);
        timerLog.cancel();
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
     * 获取用户名,心率,次数
     */
    private void mainEvent() {

        //获取最大心率
        if (MyApplication.getInstance().getUser() != null) {
            maxRate = MyApplication.getInstance().getUser().getHeartRatemMax();
        }
        //设置最宜心率
        suitRate = (int) (maxRate * 0.765);
        //日常生活心率
        lowRate = (int) (maxRate * 0.67);
        //热身心率
        readyRate = (int) (maxRate * 0.73);
        //燃脂心率
        fatRate = (int) (maxRate * 0.8);
        //心肺心率
        heartRate = (int) (maxRate * 0.86);
        //肌力心率
        muscleRate = (int) (maxRate * 0.93);
        //极限心率
        extremeRate = maxRate;

    }

    LargeDialogHelp helpDialog;

    /**
     * 帮助图标点击事件
     */
    @Event(R.id.iv_mh_help)
    private void helpClick(View v) {
        helpDialog = new LargeDialogHelp(HeartRateModeActivity.this);
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
        ImageView large_help_img = helpDialog.findViewById(R.id.large_help_img);
        Log.e("test", large_help_img == null ? "large_help_img==null" : "large_help_img!=null");
        Log.e("test", MyApplication.getInstance().getCurrentDevice().getGeneralImg() == null ? "MyApplication.getInstance().getCurrentDevice().getGeneralImg()==null" : "MyApplication.getInstance().getCurrentDevice().getGeneralImg()!=null");
        Log.e("test", getResources() == null ? "getResources()==null" : "getResources()!=null");
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

    //扫描教练的定时任务
    Timer timerLog = new Timer();
    TimerTask taskLog = new TimerTask() {
        @Override
        public void run() {
            Intent intent = new Intent(HeartRateModeActivity.this, BluetoothService.class);
            intent.putExtra("command", CommonCommand.SECOND__LOGIN.value());
            startService(intent);

        }
    };

    /**
     * 为教练协助按钮添加点击监听
     */
    @Event(R.id.btn_mh_coach)
    private void endClick(View v) {

        //如果是教练协助
        if (btn_mh_coach.getText() == "教练协助") {
            //扫描第二用户
            btn_mh_coach.setText("教练协助...");
            Intent intent2 = new Intent(HeartRateModeActivity.this, CardReaderService.class);
            intent2.putExtra("command", CommonCommand.SECOND__LOGIN.value());
            startService(intent2);
            timerLog.schedule(taskLog, 0, 2000);
        }
        //如果是停止协助
        if (btn_mh_coach.getText() == "停止协助") {
            //HelpUser置为空串

            //断开第二用户
            Intent intent2 = new Intent(HeartRateModeActivity.this, CardReaderService.class);
            intent2.putExtra("command", CommonCommand.SECOND__LOGOUT.value());
            startService(intent2);
            Intent intent = new Intent(HeartRateModeActivity.this, BluetoothService.class);
            intent.putExtra("command", CommonCommand.SECOND__LOGOUT.value());
            startService(intent);
        }
        //如果是个人设置
        if (btn_mh_coach.getText() == "个人设置") {
            //跳转个人设置界面
            Intent intent = new Intent(HeartRateModeActivity.this, PersonalSettingActivity.class); //新建一个跳转到个人设置界面Activity的显式意图
            startActivity(intent); //启动
            finish(); //结束当前Activity
        }

    }

    CommonDialog commonDialog;

    /**
     * 为暂停按钮添加点击监听
     */
    @Event(R.id.btn_mh_pause)
    private void pauseClick(View v) {
        allowRecordNum = false;
        commonDialog = new CommonDialog(HeartRateModeActivity.this);
        final TextView common_dialog_cancel = commonDialog.findViewById(R.id.common_dialog_cancel);
        commonDialog.setTitle("温馨提示");
//        commonDialog.setMessage("点击继续按钮继续运动\n\n点击结束按钮结束运动");
        commonDialog.setMessage("训练已暂停");
        commonDialog.setNegativeBtnText("结束");
        // common_dialog_cancel.setTextColor(getResources().getColor(R.color.qmui_config_color_red));
        commonDialog.setPositiveBtnText("继续");
        //“结束”按钮 监听
        commonDialog.setOnNegativeClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //TODO:上传训练结果，用户置空
                //置空用户
                Intent intentLog2 = new Intent(HeartRateModeActivity.this, CardReaderService.class);
                intentLog2.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
                startService(intentLog2);
                Intent intentLog = new Intent(HeartRateModeActivity.this, BluetoothService.class);
                intentLog.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
                startService(intentLog);
                commonDialog.dismiss();
                //新建一个跳转到待机界面Activity的显式意图
                Intent intent = new Intent(HeartRateModeActivity.this, LoginActivity.class);
                //启动
                startActivity(intent);
                //结束当前Activity
                finish();
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
        mediumDialog = new MediumDialog((HeartRateModeActivity.this));
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
                    tv_mh_time.setText("训练倒计时：");
                    gettime.setTextColor(gettime.getResources().getColor(R.color.DeepSkyBlue)); //深天蓝色
                }
                if (msg.what == 1) {
                    //如果当前时间为休息时间
                    tv_mh_time.setText("休息倒计时：");
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
                    if (upload.getTrainTime_() == 0) {
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
                //如果处于教练调试模式则一分钟的跳转逻辑失效
//                if(MyApplication.getInstance().getUser() != null) { //判空
//                    //如果用户是学员，则执行如下逻辑
//                    if (MyApplication.getInstance().getUser().getType().equals("trainee")){
//                        //如果为空，说明无教练连接蓝牙
//                        if(MyApplication.getInstance().getUser().getHelperuser() == null || MyApplication.getInstance().getUser().getHelperuser().getUsername().equals("")) {

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
                //5.平均心率
                //6.最小心率
                //7.最大心率
                //已经在实时获取心率处获取
                MyApplication.setUpload(upload);

                //倒计时结束，跳转再见界面
                //新建一个跳转到再见界面Activity的显式意图
                if (mediumDialog != null && mediumDialog.isShowing()) {
                    mediumDialog.dismiss();
                }
                if (commonDialog != null && commonDialog.isShowing()) {
                    commonDialog.dismiss();
                }
                if (helpDialog != null && helpDialog.isShowing()) {
                    helpDialog.dismiss();
                }
                Intent intent = new Intent(HeartRateModeActivity.this, ByeActivity.class);
                //启动
                startActivity(intent);
                //结束当前Activity
                HeartRateModeActivity.this.finish();
//                        } else { //否则，不为空串，说明有教练连接蓝牙
//
//                        }
//                    }
//                }

            }
        });
    }

    /**
     * 计算卡路里消耗
     */
    private double countEnergy(int count, int force){
        return count * (0.01 * weight + 0.02 * force);
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
            Log.e("BtTest", "收到message：" + commonMessage.toString());
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
                    //如果连接成功，跳转个人设置界面
                    Log.e("MainActivity", "login successfully");
                    Intent activityintent = new Intent(HeartRateModeActivity.this, PersonalSettingActivity.class); //新建一个跳转到个人设置界面Activity的显式意图
                    startActivity(activityintent); //启动
                    HeartRateModeActivity.this.finish(); //结束当前Activity
                    break;
                //第二用户下线成功
                case CommonMessage.SECOND__DISCONNECTED:
                case CommonMessage.SECOND__LOGOUT:
                    LogUtil.d("广播接收器收到：" + commonMessage.toString());
                    //刷新主界面
                    needAfterMotion = false;
                    HeartRateModeActivity.this.recreate();
                    break;
                //获得心率
                case CommonMessage.HEART_BEAT:
                    LogUtil.d("广播接收器收到：" + commonMessage.toString());
                    getrate.setText(commonMessage.getAttachment());
                    //设置时时心率的值
                    heartrate.setText(commonMessage.getAttachment());
                    //获得时时心率
                    timeRate = Integer.valueOf(commonMessage.getAttachment());
                    //设置对比心率的值
                    if (h == 1) {
                        h = 0;
                        pastRate = timeRate;
                        heartSum = pastRate;
                    }
                    //时时心率等于最宜心率
                    if (timeRate == suitRate) {
                        //设置心率区间值
                        ratespace.setText("最宜心率");
                        ratespace.setTextColor(getResources().getColor(R.color.green));
                        tips.setText("燃脂区间");
                        tips.setTextColor(getResources().getColor(R.color.green));
                        tips1.setText("很棒多多保持！");

                    }
                    //时时心率等于最大心率
                    if (timeRate == maxRate) {

                        //设置心率区间值
                        ratespace.setText("最大心率");
                        ratespace.setTextColor(getResources().getColor(R.color.qmui_config_color_red));
                        tips.setText("预警区间");
                        tips.setTextColor(getResources().getColor(R.color.qmui_config_color_red));
                        //TODO:停止继续增加力，并且自动减少顺向力/反向力
                        //设置顺向力值
//            if (1 == 1 && Integer.valueOf(positivenumber.getText().toString()) > 0){
//                positivenumber.setText(Integer.valueOf(positivenumber.getText().toString()) - 1 + "");
//            }
                        //设置反向力值
//            if (1 == 1 && Integer.valueOf(inversusnumber.getText().toString()) > 0){
//                inversusnumber.setText(Integer.valueOf(inversusnumber.getText().toString()) - 1 + "");
//            }
                    }
                    //时时心率小于等于最低心率(生活区间)
                    if (timeRate <= lowRate) {
                        //设置心率值
                        //getrate.setText();
                        //设置心率区间值
                        ratespace.setText("低于运动心率");
                        ratespace.setTextColor(getResources().getColor(R.color.btn_filled_blue_bg_normal));
                        tips.setText("生活区间");
                        tips.setTextColor(getResources().getColor(R.color.btn_filled_blue_bg_normal));
                        tips1.setText("希望多多加油！");
                    }
                    //时时心率在热身区间范围内
                    if (lowRate < timeRate && timeRate <= readyRate) {
                        //设置心率值
                        //getrate.setText();
                        //设置心率区间值
                        tips.setText("热身区间");
                        tips.setTextColor(getResources().getColor(R.color.yellow));
                    }
                    //时时心率在燃脂区间范围内
                    if (readyRate < timeRate && timeRate <= fatRate) {
                        //设置心率值
                        //getrate.setText();
                        //设置心率区间值
                        tips.setText("燃脂区间");
                        tips.setTextColor(getResources().getColor(R.color.green));
                    }
                    //时时心率在心肺区间范围内
                    if (fatRate < timeRate && timeRate <= heartRate) {
                        //设置心率值
                        //getrate.setText();
                        //设置心率区间值
                        tips.setText("心肺区间");
                        tips.setTextColor(getResources().getColor(R.color.DeepSkyBlue));
                    }
                    //时时心率在肌力区间范围内
                    if (heartRate < timeRate && timeRate <= muscleRate) {
                        //设置心率值
                        //getrate.setText();
                        //设置心率区间值
                        tips.setText("肌力区间");
                        tips.setTextColor(getResources().getColor(R.color.tintRed));
                    }
                    //时时心率在极限区间范围内
                    if (muscleRate < timeRate && timeRate <= extremeRate) {
                        //设置心率值
                        //getrate.setText();
                        //设置心率区间值
                        tips.setText("极限区间");
                        tips.setTextColor(getResources().getColor(R.color.qmui_config_color_red));
                    }
                    //时时心率在预警区间范围内
                    if (extremeRate < timeRate) {
                        //设置心率值
                        //getrate.setText();
                        //设置心率区间值
                        tips.setText("预警区间");
                        tips.setTextColor(getResources().getColor(R.color.purple));
                    }
                    //上传运动中的最大心率，最小心率，平均心率
                    //取得上传结果类
//                    Upload upload = MyApplication.getUpload();
                    //设置上传的最大心率
                    if (timeRate > uploadMax) {
                        uploadMax = timeRate;
                        upload.setHeartRateMax_(uploadMax);
                    }
                    //设置上传的最小心率
                    if (timeRate < uploadMin) {
                        uploadMin = timeRate;
                        upload.setHeartRateMin_(uploadMin);
                    }
                    //设置上传的平均心率
                    if (pastRate != timeRate) {
                        ++i;
                        heartSum += timeRate;
                        pastRate = timeRate;
                        uploadAvg = heartSum / i;
                        upload.setHeartRateAvg_(uploadAvg);
                    }
                    break;
                default:
                    LogUtil.e("未知广播，收到message：" + commonMessage.getMsgType());
            }
        }
    }
}
