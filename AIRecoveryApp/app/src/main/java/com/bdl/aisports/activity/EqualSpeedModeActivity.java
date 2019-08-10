package com.bdl.aisports.activity;


import android.animation.IntArrayEvaluator;
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
import android.widget.TextView;
import android.widget.ImageView;

import com.bdl.aisports.MyApplication;
import com.bdl.aisports.R;
import com.bdl.aisports.base.BaseActivity;
import com.bdl.aisports.bluetooth.CommonCommand;
import com.bdl.aisports.bluetooth.CommonMessage;
import com.bdl.aisports.charts.LineChart;
import com.bdl.aisports.constant.MotorConstant;
import com.bdl.aisports.contoller.MotorProcess;
import com.bdl.aisports.contoller.Reader;
import com.bdl.aisports.contoller.Writer;
import com.bdl.aisports.dialog.LargeDialogHelp;
import com.bdl.aisports.dialog.MediumDialog;
import com.bdl.aisports.entity.CurrentTime;
import com.bdl.aisports.entity.Upload;
import com.bdl.aisports.service.BluetoothService;
import com.bdl.aisports.service.CardReaderService;
import com.bdl.aisports.util.MessageUtils;
import com.bdl.aisports.util.SendReqOfCntTimeUtil;
import com.bdl.aisports.dialog.CommonDialog;
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

import static com.bdl.aisports.contoller.Writer.setParameter;
import static java.lang.Math.abs;


@ContentView(R.layout.activity_mode_equalspeed)
public class EqualSpeedModeActivity extends BaseActivity {
    //TODO:电机相关
    private int num = 0;
    private int positiveTorqueLimited = 0;
    private int negativeTorqueLimited = 0;
    private int frontLimitedPosition;
    private int rearLimitedPosition;
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
    private Handler torqueHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) { //顺向力反向力handler
            super.handleMessage(msg);
            int arg1 = msg.arg1;
            int arg2 = msg.arg2;
            switch (msg.what) {
                case 1:
                    positivenumber.setText(String.valueOf(arg1));
                    inversusnumber.setText(String.valueOf(arg2));
                    break;
            }
        }
    };
    //TODO:电机相关
    private BluetoothReceiver bluetoothReceiver;        //蓝牙广播接收器，监听用户的登录广播
    private boolean needAfterMotion = true;

    /**
     * 控件绑定
     */
    //ImageView
    @ViewInject(R.id.iv_me_help)
    private ImageView iv_me_help;           //“帮助”图片按钮
    @ViewInject(R.id.iv_me_state)
    private ImageView iv_me_state;    //登录状态
    //TextView
    @ViewInject(R.id.tv_me_person)
    private TextView person;                //用户名
    @ViewInject(R.id.tv_me_getrate)
    private TextView getrate;               //心率
    @ViewInject(R.id.tv_me_getnumber)
    private TextView getnumber;             //次数
    @ViewInject(R.id.tv_me_gettime)
    private TextView gettime;               //倒计时
    @ViewInject(R.id.tv_me_positivenumber)
    private TextView positivenumber;        //顺向力值
    @ViewInject(R.id.tv_me_inversusnumber)
    private TextView inversusnumber;        //反向力值
    @ViewInject(R.id.tv_me_time)
    private TextView tv_me_time;            //提示文本 训练/休息倒计时
    private TextView medium_dialog_msg;     //最后5秒模态框 时间文本
    //Button
    @ViewInject(R.id.btn_me_coach)
    private Button btn_me_coach;   //“教练协助/停止协助”按钮
    //LineChartView
    @ViewInject(R.id.chart_equal)
    private LineChartView chart;            //动画折线图
    //SeekBar
    @ViewInject(R.id.sb_scope)
    private com.bdl.aisports.widget.MySeekBar sb_scope; //活动范围SeekBar

    /**
     * 类成员
     */
    private Thread animThread;
    private Timer animTimer = new Timer();

    private Thread localCountDownThread;    //本机倒计时线程
    private Thread delayThread;             //模态框倒计时线程
    private int localCountDown = 60;        //本机倒计时（单位：秒）
    private int localCountDownType = 0;     //本机倒计时类型（0运动，1休息）
    private Handler handler;                //用于在UI线程中获取倒计时线程创建的Message对象，得到倒计时秒数与时间类型
    private Handler handler_dialog;         //用于模态框ui线程中获取倒计时线程创建的Message对象
    private Boolean isAlert = false;        //标识是否弹5s倒计时模态框
    EqualSpeedModeActivity.locationReceiver LocationReceiver = new locationReceiver();
    IntentFilter filterHR = new IntentFilter();
    private SendReqOfCntTimeUtil sendReqOfCntTimeUtil; //发送同步时间请求的工具
    Timer timer = new Timer(); //运动时的定时任务
    private Thread seekBarThread;           //电机速度与位移的SeekBar线程
    private float lastPosition, curPosition; //上一次电机位置、当前电机位置
    private float lastSpeed = -1, curSpeed; //上一次电机速度，当前电机速度
    //动画部分
    private Thread cartoonThreadEntad;
    private Thread cartoonThreadOffCenter;
    private Handler handler1;
    private Handler handler2;
    private boolean flag = true;                          //true 向心运动开始
    private boolean flag_1 = true;                        //true  离心运动开始
    private String[] Xasis = new String[10];
    private List<int[]> ListYasis = new ArrayList<>();
    private LineChart lineChart = new LineChart();
    private int[] Yasis1s = new int[20];                //向心动画开始时数据
    private int[] Yasis2s = new int[20];                //离心动画开始时数据
    private int[] Yasis1e = new int[20];                //向心数据更新结果
    private int[] Yasis2e = new int[20];                //离心数据更新结果
    private int[] Yasis1gap = new int[20];              //向心图每秒步进的数据
    private int[] Yasis2gap = new int[20];              //离心图每秒步进的数据
    private final int TIMEDATACHANGE = 2000;            //2秒数据率刷新一次
    private final int FPS = 20;                         //帧数设置
    private Upload upload = new Upload();


    ArrayList<PointValue> pointValueList;
    ArrayList<PointValue> points;
    ArrayList<Line> linesList;
    Axis axisX;// X轴属性，Y轴类同
    Axis axisY;
    LineChartData lineChartData;
    int position = 0;
    int currentForce = 0;
    byte[] motorCommand = MotorConstant.READ_TORQUE;

    private double weight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        needAfterMotion = true;
        initImmersiveMode(); //隐藏状态栏，导航栏
//        test();
        initMotor();
        queryDeviceParam();//查询设备参数
        queryUserInfo(); //查询用户信息
//        initCartoon(); //初始化动画
        iv_me_help_onClick(); //帮助图片点击事件（使用xUtils框架会崩溃）
        filterHR.addAction("heartrate");
        registerReceiver(LocationReceiver, filterHR);

        syncCurrentTime(); //同步当前时间
        //运动过程选择
        try {
            chooseDeviceType();
        } catch (Exception e) {
            e.printStackTrace();
        }

        initAxisView();
        showMovingLineChart();

        //注册蓝牙用监听器
        bluetoothReceiver = new BluetoothReceiver();
        IntentFilter intentFilter = new IntentFilter("com.bdl.bluetoothmessage");
        registerReceiver(bluetoothReceiver, intentFilter);

        animThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });
//        animThread.start();
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
                    setParameter(1, MotorConstant.EQUAL_SPEED_MODE);
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

    private void chooseDeviceType() throws Exception {
        //运动过程选择
        switch (deviceType) {
            case 1: //拉设备
                equalSpeedModeProcessByPulling();
                break;
            case 2: //推设备
                equalSpeedModeProcessByPushing();
                break;
            case 3: //躯干扭转组合
                if (motorDirection == 1) {
                    MyApplication.getInstance().motorDirection = 2;
                    int[] newLimitedPosition = MessageUtils.recersalLimit(frontLimitedPosition, rearLimitedPosition, 1);
                    frontLimitedPosition = newLimitedPosition[0];
                    rearLimitedPosition = newLimitedPosition[1];
                    equalSpeedModeProcessByPushing();
                } else if (motorDirection == 2) {
                    MyApplication.getInstance().motorDirection = 1;
                    int[] newLimitedPosition = MessageUtils.recersalLimit(frontLimitedPosition, rearLimitedPosition, 2);
                    frontLimitedPosition = newLimitedPosition[0];
                    rearLimitedPosition = newLimitedPosition[1];
                    equalSpeedModeProcessByPulling();
                }
                break;
        }
    }

    /**
     * 运动范围的SeekBar设置
     * 请求电机线程在onResume开启，在onStop关闭
     * 需要电机的位移范围（前后方限制）
     */
    private void SeekBarSetting() {
        final int tenThousand = 10000; //一万
        final int frontLimit = frontLimitedPosition / tenThousand; //前方限制
        final int backLimit = rearLimitedPosition / tenThousand; //后方限制
        sb_scope.setMax(frontLimit - backLimit); //位移范围
        final int interval = 100; //绘制间隔：100ms
        final int frequency = 20; //过渡动画中100ms内的绘制频率
        final int transInterval = interval / frequency;   //过渡动画的绘制间隔：5ms
        //每200ms获取一次当前电机速度与电机位移
        seekBarThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        //获取推拉位移
                        if (lastPosition == 0) {
                            //第一次获取，需要获取两次，才能动画过渡
                            lastPosition = Float.parseFloat(Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION)) / tenThousand - frontLimit;
                            Thread.sleep(interval);
                            curPosition = Float.parseFloat(Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION)) / tenThousand - backLimit;
                        } else {
                            //从第二次开始，将上一次的保存，并再获取一次，进行动画过渡
                            lastPosition = curPosition;
                            curPosition = Float.parseFloat(Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION)) / tenThousand - backLimit;
                        }

                        //进度条位置过渡，每5ms更新一次
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    float diffPosition = (curPosition - lastPosition) / frequency; //过渡差值
                                    sb_scope.setProgress((int) lastPosition);
                                    for (int i = 1; i < frequency; ++i) {
                                        Thread.sleep(transInterval);
                                        lastPosition += diffPosition;
                                        sb_scope.setProgress((int) lastPosition);
                                    }
                                    //最后一帧校准
                                    Thread.sleep(transInterval);
                                    sb_scope.setProgress((int) curPosition);
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
     * 折线图动画效果
     */
    private void initAxisView() {
        pointValueList = new ArrayList<PointValue>();
        linesList = new ArrayList<Line>();

        /** 初始化Y轴 */
        axisY = new Axis();
        axisY.setName("扭力");//添加Y轴的名称
        axisY.setHasLines(false);//Y轴分割线
        axisY.setTextSize(25);//设置字体大小
//        axisY.setTextColor(Color.parseColor("#AFEEEE"));//设置Y轴颜色，默认浅灰色
        lineChartData = new LineChartData(linesList);
        lineChartData.setAxisYLeft(axisY);//设置Y轴在左边

        /** 初始化X轴 */
        axisX = new Axis();
        axisX.setHasTiltedLabels(false);//X坐标轴字体是斜的显示还是直的，true是斜的显示
        axisX.setTextColor(Color.parseColor("#ececec"));//设置X轴颜色
        axisX.setName("时间(s)");//X轴名称
        axisX.setHasLines(false);//X轴分割线
        axisX.setTextSize(15);//设置字体大小
        axisX.setMaxLabelChars(0);//设置0的话X轴坐标值就间隔为1
        List<AxisValue> mAxisXValues = new ArrayList<AxisValue>();
        for (int i = 0; i < 61; i++) {
            mAxisXValues.add(new AxisValue(i).setLabel(i + ""));
        }
        axisX.setValues(mAxisXValues);//填充X轴的坐标名称
        lineChartData.setAxisXBottom(axisX);//X轴在底部

        chart.setLineChartData(lineChartData);

        Viewport port = initViewPort(-MotorConstant.speed, 0);//初始化X轴10个间隔坐标
        chart.setCurrentViewportWithAnimation(port);
        chart.setInteractive(false);//设置不可交互
        chart.setScrollEnabled(true);
        chart.setValueTouchEnabled(false);
        chart.setFocusableInTouchMode(false);
        chart.setViewportCalculationEnabled(false);
        chart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        chart.startDataAnimation();

        loadData();//加载待显示数据
    }

    private Viewport initViewPort(float left, float right) {
        Viewport port = new Viewport();
        port.top = 200;//Y轴上限，固定(不固定上下限的话，Y轴坐标值可自适应变化)
        port.bottom = -200;//Y轴下限，固定
        port.left = left;//X轴左边界，变化
        port.right = right;//X轴右边界，变化
        return port;
    }

    private void loadData() {
        points = new ArrayList<PointValue>();
        Log.d("单车模式", "点集：" + points.toString());
    }

    private void showMovingLineChart() {
//        Timer timer = new Timer();
        animTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    switch (deviceType) {
                        case 1:
                            currentForce = Integer.parseInt(Reader.getRespData(motorCommand));
                            break;
                        case 2:
                            currentForce = -Integer.parseInt(Reader.getRespData(motorCommand));
                            break;
                        case 3:
                            if (motorDirection == 1) {
                                currentForce = -Integer.parseInt(Reader.getRespData(motorCommand));
                            } else {
                                currentForce = Integer.parseInt(Reader.getRespData(motorCommand));
                            }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d("等速模式", "当前转速: " + currentForce);
                points.add(new PointValue(position, currentForce));
                if (true) {
                    Log.d("单车模式", "更新折线图：" + points.get(position).toString());
                    pointValueList.add(points.get(position));//实时添加新的点

                    //根据新的点的集合画出新的线
                    Line line = new Line(pointValueList);
                    line.setShape(ValueShape.CIRCLE);//设置折线图上数据点形状为 圆形 （共有三种 ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.DIAMOND）
                    line.setCubic(false);//曲线是否平滑，true是平滑曲线，false是折线
                    line.setHasLabels(false);//数据是否有标注
                    //        line.setHasLabelsOnlyForSelected(true);//点击数据坐标提示数据,设置了line.setHasLabels(true);之后点击无效
                    line.setHasLines(true);//是否用线显示，如果为false则没有曲线只有点显示
                    line.setHasPoints(false);//是否显示圆点 ，如果为false则没有原点只有点显示（每个数据点都是个大圆点）
                    line.setFilled(true);
                    line.setColor(Color.parseColor("#1E90FF"));
                    linesList = new ArrayList<Line>();
                    linesList.add(line);
                    lineChartData = new LineChartData(linesList);
                    lineChartData.setAxisYLeft(axisY);//设置Y轴在左
                    lineChartData.setAxisXBottom(axisX);//X轴在底部
                    chart.setLineChartData(lineChartData);

                    float xAxisValue = points.get(position).getX();
                    //根据点的横坐标实时变换X坐标轴的视图范围
                    Viewport port;
                    if (xAxisValue > 0) {
                        port = initViewPort(xAxisValue - 50, xAxisValue);
                    } else {
                        port = initViewPort(-50, 0);
                    }
                    chart.setMaximumViewport(port);
                    chart.setCurrentViewport(port);

                    position++;
                }
            }
        }, 300, 200);
    }

//    /**
//     * 循环读取当前力矩限制并且设置初始正向力和反向力
//     */
//    private void monitorCurrentTorque(final Handler handler) throws Exception {
//
//        try {
//            setParameter(40 * 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
//            setParameter(40 * 100, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        final int[] lastTorque = {Integer.parseInt(Reader.getRespData(MotorConstant.READ_TORQUE))}; //上一次的位置，初始值为前方限制
//        final Timer readTorqueTimer = new Timer();
//        TimerTask readTorqueTimerTask = new TimerTask() {
//            @Override
//            public void run() {
//                try {
//                    //读取当前力矩
//                    int currentTorque = Integer.parseInt(Reader.getRespData(MotorConstant.READ_TORQUE));
//                    int torqueDif = Math.abs(currentTorque - lastTorque[0]);
//                    Log.e("Equal", "当前力矩::::::"+String.valueOf(currentTorque));
//                    Log.e("Equal", "力矩差::::" + String.valueOf(torqueDif));
//                    if (torqueDif >= 20) {
//                        readTorqueTimer.cancel();
//                        int torque = 12 * torqueDif + 250; //函数关系
//                        if (torque >= 5000) {
//                            positiveTorqueLimited = 5000;
//                            negativeTorqueLimited = 5000;
//                            //设置顺反向力
//                            setParameter(positiveTorqueLimited, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
//                            setParameter(negativeTorqueLimited, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
//                            //发送数据到handler
//                            Message message = handler.obtainMessage();
//                            message.what = 1;
//                            message.arg1 = positiveTorqueLimited / 100;
//                            message.arg2 = negativeTorqueLimited / 100;
//                            handler.sendMessage(message);
//                        } else {
//                            positiveTorqueLimited = torque;
//                            negativeTorqueLimited = torque;
//                            //设置顺反向力
//                            setParameter(positiveTorqueLimited, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
//                            setParameter(negativeTorqueLimited, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
//                            //发送数据到handler
//                            Message message = handler.obtainMessage();
//                            message.what = 1;
//                            message.arg1 = positiveTorqueLimited / 100;
//                            message.arg2 = negativeTorqueLimited / 100;
//                            handler.sendMessage(message);
//                        }
//                    }
//                    lastTorque[0] = currentTorque;
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        };
//        readTorqueTimer.schedule(readTorqueTimerTask, 0, 100);
//    }
    /**
     * 循环读取当前力矩限制并且设置初始正向力和反向力
     */
//    private void monitorCurrentTorque(final Handler handler) {
//
//        try {
//            setParameter(100 * 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
//            setParameter(100 * 100, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        final Timer readTorqueTimer = new Timer();
//        TimerTask readTorqueTimerTask = new TimerTask() {
//            @Override
//            public void run() {
//                try {
//                    //读取当前力矩
//                    String currentTorque = Reader.getRespData(MotorConstant.READ_TORQUE);
//                    Log.e("当前力矩啊啊啊啊", currentTorque);
//                    if (currentTorque == null) {
//                        return;
//                    }
//                    if (Math.abs(Integer.parseInt(currentTorque)) >= 64) { //检测到力矩变化
//                        int torque = Math.abs(Integer.parseInt(currentTorque)) * 10000 / 323;
//                        Log.e("当前力矩百分比啊啊啊啊", String.valueOf(torque));
//                        readTorqueTimer.cancel();
//                        if (torque >= 40 * 100) {
//                            positiveTorqueLimited = 40 * 100;
//                            negativeTorqueLimited = 40 * 100;
//                            //设置顺反向力
//                            setParameter(positiveTorqueLimited, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
//                            setParameter(negativeTorqueLimited, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
//                            //发送数据到handler
//                            Message message = handler.obtainMessage();
//                            message.what = 1;
//                            message.arg1 = positiveTorqueLimited / 100;
//                            message.arg2 = negativeTorqueLimited / 100;
//                            handler.sendMessage(message);
//                        } else {
//                            positiveTorqueLimited = torque;
//                            negativeTorqueLimited = torque;
//                            //设置顺反向力
//                            setParameter(positiveTorqueLimited, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
//                            setParameter(negativeTorqueLimited, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
//                            //发送数据到handler
//                            Message message = handler.obtainMessage();
//                            message.what = 1;
//                            message.arg1 = positiveTorqueLimited / 100;
//                            message.arg2 = negativeTorqueLimited / 100;
//                            handler.sendMessage(message);
//                        }
//
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        };
//        readTorqueTimer.schedule(readTorqueTimerTask, 0, 200);
//    }

    /**
     * 拉设备运动过程
     */
    private void equalSpeedModeProcessByPulling() throws Exception {
        //打开运动过程
        final int[] lastLocation = {frontLimitedPosition}; //上一次的位置，初始值为前方限制
        final boolean[] countFlag = {false};//计数标志位
        final boolean[] haveStopped = {false};
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    //发送消息到handler
                    Message message = countHandler.obtainMessage();
                    message.what = 1;
                    //读取当前位置
                    String actualTorqueLimit = Reader.getRespData(MotorConstant.READ_TORQUE_LIMITED);
                    String currentLocation = Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION);
                    int currentSpeed = Math.abs(Integer.valueOf(Reader.getRespData(MotorConstant.READ_ROTATIONAL_SPEED)));
                    if (currentSpeed <= 10 && negativeTorqueLimited < MotorConstant.MIN_BACK_TORQUE) {
                        setParameter(MotorConstant.MIN_BACK_TORQUE, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                        haveStopped[0] = true;
                    }

                    if (Integer.valueOf(actualTorqueLimit) < 6000) {
                        //发送数据到handler
                        Message torqueMessage = torqueHandler.obtainMessage();
                        torqueMessage.what = 1;
                        torqueMessage.arg1 = Integer.valueOf(actualTorqueLimit) / 100 + 5;
                        torqueMessage.arg2 = Integer.valueOf(actualTorqueLimit) / 100 + 5;
                        torqueHandler.sendMessage(torqueMessage);
                    }
                    int difference = Integer.parseInt(currentLocation) - lastLocation[0]; //本次位置和上次读到的位置差
                    if (difference > 20000) { //回程
                        //超过前方限制
                        if (Integer.valueOf(currentLocation) >= frontLimitedPosition - 5000) {
                            if (haveStopped[0]) { //是否需要恢复反向力量
                                setParameter(negativeTorqueLimited, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                                haveStopped[0] = false;
                            }
                            if (countFlag[0] && allowRecordNum) {
                                num++;
                                message.arg1 = num;
                                countHandler.sendMessage(message);
                            }
                            //无法继续计数
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
    private void equalSpeedModeProcessByPushing() throws Exception {
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
                        if (Integer.parseInt(currentLocation) <= rearLimitedPosition + 50000) { //超过后方限制
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
     * 当Activity准备好和用户进行交互时，调用onResume()
     * 此时Activity处于【运行状态】
     */
    @Override
    protected void onResume() {
        super.onResume();
        //判空
        if (localCountDownThread == null) {
            CreatelocalCountDownTheard(); //创建本机倒计时线程
            localCountDownThread.start(); //启动本机倒计时线程
        }
        if (seekBarThread == null) {
            SeekBarSetting(); //速度、运动范围的SeekBar设置
            seekBarThread.start();
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
        //模态框倒计时5s线程
        if (delayThread != null) {
            delayThread.interrupt(); //中断线程
            delayThread = null;
        }
        //向心动画线程
        if (cartoonThreadEntad != null) {
            cartoonThreadEntad.interrupt(); //中断线程
            cartoonThreadEntad = null;
        }
        //离心动画线程
        if (cartoonThreadOffCenter != null) {
            cartoonThreadOffCenter.interrupt(); //中断线程
            cartoonThreadOffCenter = null;
        }
        //停止Timer TimerTask
        if (sendReqOfCntTimeUtil.timer != null && sendReqOfCntTimeUtil.timerTask != null) {
            sendReqOfCntTimeUtil.timerTask.cancel();
            sendReqOfCntTimeUtil.timer.cancel();
            //Log.d("同步倒计时","EqualSpeedActivity：停止TimerTask");
        }
        if (seekBarThread != null) {
            seekBarThread.interrupt();
            seekBarThread = null;
        }
        //
        if (animThread != null) {
            animThread.interrupt();
            animThread = null;
        }
        if (animTimer != null) {
            animTimer.cancel();
            animTimer.purge();
        }
    }

    @Override
    protected void onDestroy() {
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
            try {
                setParameter(0, MotorConstant.EQUAL_SPEED_MODE);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
//            //传入电机的值
//            positiveTorqueLimited = Integer.parseInt(String.valueOf(positivenumber.getText())) * 100;
//            negativeTorqueLimited = Integer.parseInt(String.valueOf(inversusnumber.getText())) * 100;
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
                        btn_me_coach.setText("教练协助"); //更新为“教练协助”按钮
                        iv_me_state.setImageDrawable(getResources().getDrawable((R.drawable.yonghu1)));
                    } else { //否则，不为空串，说明有教练连接蓝牙
                        btn_me_coach.setText("停止协助"); //更新为“停止协助”按钮
                        iv_me_state.setImageDrawable(getResources().getDrawable((R.drawable.shou)));
                        //person.append("【调试模式】"); //追加“【调试模式】”文本
                    }
                } else if (MyApplication.getInstance().getUser().getRole().equals("coach")) {
                    btn_me_coach.setText("个人设置"); //更新为“个人设置”按钮
                    iv_me_state.setImageDrawable(getResources().getDrawable((R.drawable.guanliyuan1)));
                    //person.append("【教练用户】"); //追加“【教练用户】”文本
                } else {
                    //person.append("【测试模式】"); //追加“【测试模式】”文本
                    iv_me_state.setImageDrawable(getResources().getDrawable((R.drawable.banshou1)));
                }
            }
            weight = MyApplication.getInstance().getUser().getWeight();

        }
    }

    LargeDialogHelp helpDialog;

    /**
     * “帮助”按钮 监听（使用xUtils框架会崩溃）
     * 点击跳转到帮助页面
     */
    private void iv_me_help_onClick() {
        iv_me_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                helpDialog = new LargeDialogHelp(EqualSpeedModeActivity.this);
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

    //扫描教练的定时任务
    Timer timerLog = new Timer();
    TimerTask taskLog = new TimerTask() {
        @Override
        public void run() {
            Intent intent = new Intent(EqualSpeedModeActivity.this, BluetoothService.class);
            intent.putExtra("command", CommonCommand.SECOND__LOGIN.value());
            startService(intent);

        }
    };

    /**
     * “教练协助/停止协助”按钮
     * 如果是调试模式，显示“停止协助”，点击会将HelpUser置为空串，然后跳转到主界面
     * 如果不是调制模式，显示“教练协助”，点击事件与主界面一致（连接教练蓝牙）
     */
    @Event(R.id.btn_me_coach)
    private void endClick(View v) {
        //如果是教练协助
        if (btn_me_coach.getText().equals("教练协助")) {
            btn_me_coach.setText("教练协助...");
            Intent intent2 = new Intent(EqualSpeedModeActivity.this, CardReaderService.class);
            intent2.putExtra("command", CommonCommand.SECOND__LOGIN.value());
            startService(intent2);
            timerLog.schedule(taskLog, 0, 2000);
            Log.d("MainActivity", "request to login");
        }
        //如果是停止协助
        else if (btn_me_coach.getText().equals("停止协助")) {
            Intent intent2 = new Intent(EqualSpeedModeActivity.this, CardReaderService.class);
            intent2.putExtra("command", CommonCommand.SECOND__LOGOUT.value());
            startService(intent2);
            Intent intent = new Intent(EqualSpeedModeActivity.this, BluetoothService.class);
            intent.putExtra("command", CommonCommand.SECOND__LOGOUT.value());
            startService(intent);
            Log.d("AdaptModeActivity", "request to logout");
        }
        //如果是个人设置
        else if (btn_me_coach.getText().equals("个人设置")) {
            //跳转个人设置界面
            Intent intent = new Intent(EqualSpeedModeActivity.this, PersonalSettingActivity.class); //新建一个跳转到个人设置界面Activity的显式意图
            startActivity(intent); //启动
            EqualSpeedModeActivity.this.finish(); //结束当前Activity
        }
    }

    CommonDialog commonDialog;

    /**
     * “暂停”按钮 监听
     * 点击弹出模态框，可选择“继续”或“结束”
     * 暂停期间，不再统计用户训练的次数
     */
    @Event(R.id.btn_me_pause)
    private void pauseClick(View v) {
        allowRecordNum = false;
        commonDialog = new CommonDialog(EqualSpeedModeActivity.this);
        commonDialog.setTitle("温馨提示");
        commonDialog.setMessage("训练已暂停");
        commonDialog.setNegativeBtnText("结束");
        commonDialog.setPositiveBtnText("继续");
        //“结束”按钮 监听
        commonDialog.setOnNegativeClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commonDialog.dismiss();
                Intent intentLog = new Intent(EqualSpeedModeActivity.this, BluetoothService.class);
                intentLog.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
                startService(intentLog);
                Intent intentLog2 = new Intent(EqualSpeedModeActivity.this, CardReaderService.class);
                intentLog2.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
                startService(intentLog2);
                //新建一个跳转到待机界面Activity的显式意图
                Intent intent = new Intent(EqualSpeedModeActivity.this, LoginActivity.class);
                //启动
                startActivity(intent);
                //结束当前Activity
                EqualSpeedModeActivity.this.finish();
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
        mediumDialog = new MediumDialog((EqualSpeedModeActivity.this));
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
        medium_dialog_msg = (TextView) mediumDialog.findViewById(R.id.medium_dialog_time);
    }

    /**
     * 计算卡路里消耗
     */
    private double countEnergy(int count, int force){
        return count * (0.01 * weight + 0.02 * force);
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
                    tv_me_time.setText("训练倒计时：");
                    gettime.setTextColor(gettime.getResources().getColor(R.color.DeepSkyBlue)); //深天蓝色
                }
                if (msg.what == 1) {
                    //如果当前时间为休息时间
                    tv_me_time.setText("休息倒计时：");
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
                if (mediumDialog != null && mediumDialog.isShowing()) {
                    mediumDialog.dismiss();
                }
                if (commonDialog != null && commonDialog.isShowing()) {
                    commonDialog.dismiss();
                }
                if (helpDialog != null && helpDialog.isShowing()) {
                    helpDialog.dismiss();
                }
                Intent intent = new Intent(EqualSpeedModeActivity.this, ByeActivity.class);
                //启动
                startActivity(intent);
                //结束当前Activity
                EqualSpeedModeActivity.this.finish();
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
                startActivity(new Intent(EqualSpeedModeActivity.this, ScramActivity.class));
                EqualSpeedModeActivity.this.finish();
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
                    if (intent.getStringExtra("log").equals("twologicard") || intent.getStringExtra("log").equals("twologiblue")) {
                        //如果连接成功，跳转个人设置界面
                        Log.e("StandardModeActivity", "login successfully");
                        Intent activityintent = new Intent(EqualSpeedModeActivity.this, PersonalSettingActivity.class); //新建一个跳转到个人设置界面Activity的显式意图
                        startActivity(activityintent); //启动
                        EqualSpeedModeActivity.this.finish(); //结束当前Activity
                    } else if (intent.getStringExtra("log").equals("twologocard") || intent.getStringExtra("log").equals("twologoblue")) {
                        //刷新界面
                        EqualSpeedModeActivity.this.recreate();
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
                    Intent activityintent = new Intent(EqualSpeedModeActivity.this, PersonalSettingActivity.class); //新建一个跳转到个人设置界面Activity的显式意图
                    startActivity(activityintent); //启动
                    EqualSpeedModeActivity.this.finish(); //结束当前Activity
                    break;
                //第二用户下线成功
                case CommonMessage.SECOND__DISCONNECTED:
                case CommonMessage.SECOND__LOGOUT:
                    LogUtil.d("广播接收器收到：" + commonMessage.toString());
                    //刷新主界面
                    needAfterMotion = false;
                    EqualSpeedModeActivity.this.recreate();
                    break;
                //获得心率
                case CommonMessage.HEART_BEAT:
                    LogUtil.d("广播接收器收到：" + commonMessage.toString());
                    getrate.setText(commonMessage.getAttachment());
                    break;
                default:
                    LogUtil.e("未知广播，收到message：" + commonMessage.getMsgType());
            }
        }
    }
}
