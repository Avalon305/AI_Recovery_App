package com.bdl.aisports.activity;


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
import com.bdl.aisports.constant.MotorConstant;
import com.bdl.aisports.contoller.Reader;
import com.bdl.aisports.contoller.Writer;
import com.bdl.aisports.dialog.CommonDialog;
import com.bdl.aisports.dialog.LargeDialogHelp;
import com.bdl.aisports.dialog.MediumDialog;
import com.bdl.aisports.entity.CurrentTime;
import com.bdl.aisports.entity.Personal;
import com.bdl.aisports.entity.Upload;
import com.bdl.aisports.service.BluetoothService;
import com.bdl.aisports.service.CardReaderService;
import com.bdl.aisports.util.SendReqOfCntTimeUtil;
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

import static java.lang.Math.abs;

@ContentView(R.layout.activity_mode_bike)
public class BikeModeActivity extends BaseActivity{
    //TODO:电机相关
    private int power = Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(0).getValue()) * 100; //初始功率
    private int deviceType = MyApplication.getInstance().getCurrentDevice().getDeviceType(); //获得设备信息
    private eStopBroadcastReceiver eStopReceiver; //急停广播

    /**
     * 类成员
     */
    private Thread localCountDownThread;    //本机倒计时线程
    private int localCountDown = 60;        //本机倒计时（单位：秒）
    private int localCountDownType = 0;     //本机倒计时类型（0运动，1休息）
    private Handler handler;                //用于在UI线程中获取倒计时线程创建的Message对象，得到倒计时秒数与时间类型
    private Handler handler_dialog;         //用于模态框ui线程中获取倒计时线程创建的Message对象
    private Boolean isAlert = false;        //标识是否弹5s倒计时模态框
    private float K;                        //指数K
    private float Energy;                   //千卡
    private float Distance;                 //路程
    private float speed = 0;                //速度
    private double weight;                  //体重
    private locationReceiver LocationReceiver = new locationReceiver();       //广播监听类
    private IntentFilter filterHR = new IntentFilter();                       //广播过滤器
    private boolean testMode = false;       //暂停或调试模式
    private Timer animTimer = new Timer();
    private SendReqOfCntTimeUtil sendReqOfCntTimeUtil; //发送同步时间请求的工具
    private Thread seekBarThread;           //电机速度与位移的SeekBar线程
    private float lastPosition, curPosition; //上一次电机位置、当前电机位置
    private float lastSpeed = -1, curSpeed; //上一次电机速度，当前电机速度
    private Upload upload = new Upload();

    ArrayList<PointValue> pointValueList;
    ArrayList<PointValue> points;
    ArrayList<Line> linesList;
    Axis axisX;// X轴属性，Y轴类同
    Axis axisY;
    LineChartData lineChartData;
    int position = 0;
    private BluetoothReceiver bluetoothReceiver;        //蓝牙广播接收器，监听用户的登录广播
    /**
     * 获取控件
     */
    //ImageView
    @ViewInject(R.id.iv_mb_help)
    private ImageView iv_mb_help;    //“帮助”图片按钮
    @ViewInject(R.id.iv_mb_state)
    private ImageView iv_mb_state;    //登录状态
    //TextView
    @ViewInject(R.id.tv_mb_person)
    private TextView person;         //用户名
    @ViewInject(R.id.tv_mb_getrate)
    private TextView getrate;        //心率
    @ViewInject(R.id.tv_mb_gettime)
    private TextView gettime;        //倒计时
    @ViewInject(R.id.tv_mb_getenergy1)
    private TextView getenergy1;     //千焦
    @ViewInject(R.id.tv_mb_getenergy2)
    private TextView getenergy2;     //千卡
    @ViewInject(R.id.tv_mb_getdistance)
    private TextView getdistance;    //距离
    @ViewInject(R.id.tv_mb_getspeed)
    private TextView getspeed;       //速度
    @ViewInject(R.id.tv_mb_watt)
    private TextView watt;           //瓦特值
    @ViewInject(R.id.tv_mb_time)
    private TextView tv_mb_time;     //提示文本 训练/休息倒计时
    private TextView medium_dialog_msg;     //最后5秒模态框 时间文本
    //Button
    @ViewInject(R.id.btn_mb_end)
    private Button btn_mb_end;     //“教练协助/停止协助”按钮
    @ViewInject(R.id.btn_mb_pause)
    private Button btn_mb_pause;   //“暂停”按钮
    //LineChartView
    @ViewInject(R.id.mb_chart)
    private LineChartView mb_chart;  //折线图
    //SeekBar
    @ViewInject(R.id.sb_speed)
    private com.bdl.aisports.widget.MySeekBar sb_speed; //单车速度SeekBar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initImmersiveMode();  //隐藏状态栏，导航栏
        queryDeviceParam();   //查询设备参数
        queryUserInfo();      //查询用户信息
        iv_mb_help_onClick(); //帮助图片的点击事件（使用xUtils框架会崩溃）
        syncCurrentTime(); //同步当前时间
        initPower();

        //注册蓝牙用监听器
        bluetoothReceiver = new BluetoothReceiver();
        IntentFilter intentFilter = new IntentFilter("com.bdl.bluetoothmessage");
        registerReceiver(bluetoothReceiver,intentFilter);

        Energy = 0;
        Distance = 0;

        initAxisView();
        showMovingLineChart();

    }

    /**
     * 速度的SeekBar设置
     * 请求电机线程在onResume开启，在onStop关闭
     * 需要电机的速度范围
     */
    private void SeekBarSetting() {
        sb_speed.setMax(50); //速度范围
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
                        if (lastSpeed == -1) {
                            //第一次获取，需要获取两次，才能动画过渡
                            lastSpeed = abs(Float.parseFloat(Reader.getRespData(MotorConstant.READ_ROTATIONAL_SPEED)));
                            Thread.sleep(interval);
                            curSpeed = abs(Float.parseFloat(Reader.getRespData(MotorConstant.READ_ROTATIONAL_SPEED)));
                        } else {
                            //从第二次开始，将上一次的保存，并再获取一次，进行动画过渡
                            lastSpeed = curSpeed;
                            curSpeed = abs(Float.parseFloat(Reader.getRespData(MotorConstant.READ_ROTATIONAL_SPEED)));
                        }

                        //进度条位置过渡，每5ms更新一次
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    float diffSpeed = (curSpeed - lastSpeed) / frequency; //过渡差值
                                    sb_speed.setProgress((int) lastSpeed);
                                    for (int i = 1; i < frequency; ++i) {
                                        Thread.sleep(transInterval);
                                        lastSpeed += diffSpeed;
                                        sb_speed.setProgress((int) lastSpeed);
                                    }
                                    //最后一帧校准
                                    Thread.sleep(transInterval);
                                    sb_speed.setProgress((int) curSpeed);
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

    private void initPower() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Writer.setParameter(power, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
                    Writer.setParameter(power, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    /**
     * 折线图动画效果
     */
    private void initAxisView() {
        pointValueList = new ArrayList<PointValue>();
        linesList = new ArrayList<Line>();

        /** 初始化Y轴 */
        axisY = new Axis();
        axisY.setName("速度(m/s)");//添加Y轴的名称
        axisY.setHasLines(false);//Y轴分割线
        axisY.setTextSize(25);//设置字体大小
        axisY.setTextColor(Color.parseColor("#66CDAA"));//设置Y轴颜色，默认浅灰色
        lineChartData = new LineChartData(linesList);
        lineChartData.setAxisYLeft(axisY);//设置Y轴在左边

        /** 初始化X轴 */
        axisX = new Axis();
        axisX.setHasTiltedLabels(false);//X坐标轴字体是斜的显示还是直的，true是斜的显示
        axisX.setTextColor(Color.parseColor("#66CDAA"));//设置X轴颜色
        axisX.setName("时间(s)");//X轴名称
        axisX.setHasLines(true);//X轴分割线
        axisX.setTextSize(15);//设置字体大小
        axisX.setMaxLabelChars(0);//设置0的话X轴坐标值就间隔为1
        List<AxisValue> mAxisXValues = new ArrayList<AxisValue>();
        for (int i = 0; i < 61; i++) {
            mAxisXValues.add(new AxisValue(i).setLabel(i+""));
        }
        axisX.setValues(mAxisXValues);//填充X轴的坐标名称
        lineChartData.setAxisXBottom(axisX);//X轴在底部

        mb_chart.setLineChartData(lineChartData);

        Viewport port = initViewPort(-10,0);//初始化X轴10个间隔坐标
        mb_chart.setCurrentViewportWithAnimation(port);
        mb_chart.setInteractive(false);//设置不可交互
        mb_chart.setScrollEnabled(true);
        mb_chart.setValueTouchEnabled(false);
        mb_chart.setFocusableInTouchMode(false);
        mb_chart.setViewportCalculationEnabled(false);
        mb_chart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        mb_chart.startDataAnimation();

        loadData();//加载待显示数据
    }
    private Viewport initViewPort(float left,float right) {
        Viewport port = new Viewport();
        port.top = 100;//Y轴上限，固定(不固定上下限的话，Y轴坐标值可自适应变化)
        port.bottom = 0;//Y轴下限，固定
        port.left = left;//X轴左边界，变化
        port.right = right;//X轴右边界，变化
        return port;
    }
    private void loadData() {
        points = new ArrayList<PointValue>();
        Log.d("单车模式","点集："+points.toString());
    }
    private void showMovingLineChart() {
        animTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                points.add(new PointValue(position,speed));
                if(true){
                    Log.d("单车模式","更新折线图："+points.get(position).toString());
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
                    linesList =  new ArrayList<Line>();
                    linesList.add(line);
                    lineChartData = new LineChartData(linesList);
                    lineChartData.setAxisYLeft(axisY);//设置Y轴在左
                    lineChartData.setAxisXBottom(axisX);//X轴在底部
                    mb_chart.setLineChartData(lineChartData);

                    float xAxisValue = points.get(position).getX();
                    //根据点的横坐标实时变换X坐标轴的视图范围
                    Viewport port;
                    if(xAxisValue > 0){
                        port = initViewPort(xAxisValue - 10,xAxisValue);
                    }
                    else {
                        port = initViewPort(-10,0);
                    }
                    mb_chart.setMaximumViewport(port);
                    mb_chart.setCurrentViewport(port);

                    position++;
                }
            }
        },1000,1000);
    }

    LargeDialogHelp helpDialog;
    /**
     * 帮助按钮点击事件
     */
    private void iv_mb_help_onClick() {
        iv_mb_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                helpDialog = new LargeDialogHelp(BikeModeActivity.this);
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
                final ImageView large_help_img= helpDialog.findViewById(R.id.large_help_img);
                large_help_img.setImageResource(getResources().getIdentifier(MyApplication.getInstance().getCurrentDevice().getGeneralImg(),"drawable",getPackageName()));
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
        //判空
        if(localCountDownThread == null) {
            CreatelocalCountDownTheard(); //创建本机倒计时线程
            localCountDownThread.start(); //启动本机倒计时线程
        }
        if (seekBarThread == null) {
            SeekBarSetting(); //速度、运动范围的SeekBar设置
            seekBarThread.start();
        }
        //注册广播
        filterHR.addAction("heartrate");
        filterHR.addAction("log");
        registerReceiver(LocationReceiver,filterHR);
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
        //判空
        if (localCountDownThread != null) {
            localCountDownThread.interrupt(); //中断线程
            localCountDownThread = null;
        }
        //停止Timer TimerTask
        if(sendReqOfCntTimeUtil.timer != null && sendReqOfCntTimeUtil.timerTask != null) {
            sendReqOfCntTimeUtil.timerTask.cancel();
            sendReqOfCntTimeUtil.timer.cancel();
            //Log.d("同步倒计时","BikeModeActivity：停止TimerTask");
        }
        if (seekBarThread != null) {
            seekBarThread.interrupt();
            seekBarThread = null;
        }
        if (animTimer != null){
            animTimer.cancel();
            animTimer.purge();
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
    }

    /**
     * 接收广播
     */
    private class eStopBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra("state");
            if (state != null && state.equals("1")) {
                startActivity(new Intent(BikeModeActivity.this, ScramActivity.class));
                BikeModeActivity.this.finish();
            }

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
        if(MyApplication.getInstance().getCurrentDevice() != null) {
            if (MyApplication.getInstance().getCurrentDevice().getPersonalList() != null) {
                List<Personal> personalInfo = MyApplication.getInstance().getCurrentDevice().getPersonalList();
                for (Personal p : personalInfo) {
                    if (p.getName().equals("初始功率")) {
                        watt.setText(p.getValue()); //读取瓦特值
                    }
                }
            }
        }
    }

    /**
     * 获取用户信息
     */
    private void queryUserInfo() {
        if(MyApplication.getInstance().getUser() != null) { //判空
            //用户名
            person.setText(MyApplication.getInstance().getUser().getUsername());
            //如果用户是学员，则执行如下逻辑
            if (MyApplication.getInstance().getUser().getRole()!=null) {
                if (MyApplication.getInstance().getUser().getRole().equals("trainee")) {
                    //如果为空，说明无教练连接蓝牙
                    if (MyApplication.getInstance().getUser().getHelperuser() == null || MyApplication.getInstance().getUser().getHelperuser().getUsername().equals("")) {
                        //btn_mb_end.setText("教练协助"); //更新为“教练协助”按钮
                        iv_mb_state.setImageDrawable(getResources().getDrawable((R.drawable.yonghu1)));
                    } else { //否则，不为空串，说明有教练连接蓝牙
                        btn_mb_end.setText("停止协助"); //更新为“停止协助”按钮
                        iv_mb_state.setImageDrawable(getResources().getDrawable((R.drawable.shou)));
                        //person.append("【调试模式】"); //追加“【调试模式】”文本
                        testMode = true;
                    }
                } else if (MyApplication.getInstance().getUser().getRole().equals("coach")){
                    btn_mb_end.setText("个人设置"); //更新为“个人设置”按钮
                    iv_mb_state.setImageDrawable(getResources().getDrawable((R.drawable.guanliyuan1)));
                    //person.append("【教练用户】"); //追加“【教练用户】”文本
                }else {
                    //person.append("【测试模式】"); //追加“【测试模式】”文本
                    iv_mb_state.setImageDrawable(getResources().getDrawable((R.drawable.banshou1)));
                }
            }
            weight = MyApplication.getInstance().getUser().getWeight();
        }
    }

    //更新显示数据
    private void UpdateDisplayData(float speed) {
        if (!testMode)/*是否为暂停或调试*/ {//TODO
            K = 30 / (5 / (12 * speed));  //指数K  speed(/*min/400m*/ km/h)
            Energy += weight * (1 / 3600) * K;  //体重kg为单位
            Distance += speed * (1 / 3600);  //speed(km/h)
            getdistance.setText(String.valueOf(Distance));
            getenergy2.setText(String.valueOf(Energy));       //千卡
            getenergy1.setText(String.valueOf(Energy * 4.184)); //千焦
        }
    }

    /*处理显示speed的handler*/
    private Handler handler_speed = new Handler() {
        public void handlerMessage(Message msg) {
            super.handleMessage(msg);

            float speed = (float)msg.obj;
            getspeed.setText(String.valueOf(speed));
            UpdateDisplayData(speed);
        }
    };
    //扫描教练的定时任务
    Timer timerLog = new Timer();
    TimerTask taskLog = new TimerTask() {
        @Override
        public void run() {
            Intent intent = new Intent(BikeModeActivity.this, BluetoothService.class);
            intent.putExtra("command", CommonCommand.SECOND__LOGIN.value());
            startService(intent);

        }
    };
    /**
     * “教练协助/停止协助”按钮
     *  如果是调试模式，显示“停止协助”，点击会将HelpUser置为空串，然后跳转到主界面
     *  如果不是调制模式，显示“教练协助”，点击事件与主界面一致（连接教练蓝牙）
     */
    @Event(R.id.btn_mb_end)
    private void endClick(View v){
        //如果是教练协助
        if(btn_mb_end.getText() == "教练协助") {
            testMode = true;

            btn_mb_end.setText("教练协助...");
            Intent intent2 = new Intent(BikeModeActivity.this, CardReaderService.class);
            intent2.putExtra("command", CommonCommand.SECOND__LOGIN.value());
            startService(intent2);
            timerLog.schedule(taskLog,0,2000);
            Log.d("MainActivity","request to login");
        }
        //如果是停止协助
        else if(btn_mb_end.getText() == "停止协助"){
            testMode = false;

            Intent intent2 = new Intent(BikeModeActivity.this, CardReaderService.class);
            intent2.putExtra("command", CommonCommand.SECOND__LOGOUT.value());
            startService(intent2);
            Intent intent = new Intent(BikeModeActivity.this, BluetoothService.class);
            intent.putExtra("command", CommonCommand.SECOND__LOGOUT.value());
            startService(intent);
            Log.d("BikeModeActivity","request to logout");
        }
        //如果是个人设置
        else if(btn_mb_end.getText() == "个人设置"){
            //跳转个人设置界面
            Intent intent = new Intent(BikeModeActivity.this,PersonalSettingActivity.class); //新建一个跳转到个人设置界面Activity的显式意图
            startActivity(intent); //启动
            BikeModeActivity.this.finish(); //结束当前Activity
        }

    }

    CommonDialog commonDialog;
    /**
     * 为暂停按钮添加点击监听
     */
    @Event(R.id.btn_mb_pause)
    private void pauseClick(View v){
        testMode = true;

        commonDialog = new CommonDialog(BikeModeActivity.this);
        commonDialog.setTitle("温馨提示");
        commonDialog.setMessage("您确定要放弃本次训练吗？");
        commonDialog.setNegativeBtnText("结束");
        commonDialog.setPositiveBtnText("继续");
        //“结束”按钮 监听，点击跳转到待机界面
        commonDialog.setOnNegativeClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //请求退出登录
                Intent intent2 = new Intent(BikeModeActivity.this, CardReaderService.class);
                intent2.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
                startService(intent2);
                LogUtil.d("card request to logout");
                Intent intentLog = new Intent(BikeModeActivity.this, BluetoothService.class);
                intentLog.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
                startService(intentLog);
                Log.d("BikeModeActivity","request to logout");

                commonDialog.dismiss();
                //新建一个跳转到待机界面Activity的显式意图
                Intent intent = new Intent(BikeModeActivity.this,LoginActivity.class);
                //启动
                startActivity(intent);
                //结束当前Activity
                BikeModeActivity.this.finish();
            }
        });
        //“继续”按钮 监听
        commonDialog.setOnPositiveClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commonDialog.dismiss();
                testMode = false;
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

        //TODO:电机通讯
    }

    MediumDialog mediumDialog;
    /**
     * 最后5秒倒计时 模态框
     */
    private void Last5sAlertDialog() {
        //final CommonDialog commonDialog = new CommonDialog(BikeModeActivity.this);
        /*commonDialog.setTitle("温馨提示");
        commonDialog.setMessage("距离训练结束还剩：\n0:05");*/
        mediumDialog = new MediumDialog((BikeModeActivity.this));
        mediumDialog.setTime("0:`05");
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
                if(msg.what == 0) {
                    //如果当前时间为训练时间
                    tv_mb_time.setText("训练倒计时：");
                    gettime.setTextColor(gettime.getResources().getColor(R.color.DeepSkyBlue)); //深天蓝色

                }
                if(msg.what == 1) {
                    //如果当前时间为休息时间
                    tv_mb_time.setText("休息倒计时：");
                    gettime.setTextColor(gettime.getResources().getColor(R.color.OrangeRed)); //橘红色

                }
                //如果倒计时秒数小于等于5秒，弹模态框
                if(!isAlert && arg1 <= 5 && msg.what == 0) {
                    Last5sAlertDialog();
                    isAlert = true;
                }
                if(isAlert) {
                    medium_dialog_msg.setText("0:0" + arg1);
                }
                //设置文本内容（有两种特殊情况，单独设置合适的文本格式）
                int minutes = arg1 / 60;
                int remainSeconds = arg1 % 60;
                if(remainSeconds < 10) {
                    gettime.setText(minutes + ":0" + remainSeconds);
                } else {
                    gettime.setText(minutes + ":" + remainSeconds);
                }
            }
        };
        localCountDownThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //训练时间240s倒计时（单车/跑步机独有，240s训练时间）
                localCountDown = 240;
                while(!Thread.currentThread().isInterrupted() && localCountDown > 0) {
                    if(MyApplication.getCurrentTime().getType() != -1) {
                        //校准本机倒计时秒数
                        localCountDown = MyApplication.getCurrentTime().getSeconds(); //获取秒数
                        localCountDownType = MyApplication.getCurrentTime().getType(); //获取时间类型
                        MyApplication.setCurrentTime(new CurrentTime(-1,-1)); //将全局变量currentTime恢复为(-1,-1)，即一旦有值，取后销毁，实现另一种方式的传递。

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

                        speed = (float)0.015 * abs(Float.parseFloat(Reader.getRespData(MotorConstant.READ_ROTATIONAL_SPEED)));

                        Message speedmsg = handler_speed.obtainMessage();
                        speedmsg.obj = speed;
                        handler_speed.sendMessage(speedmsg);    //刷新显示数据

                    } catch(Exception e) {
                        e.printStackTrace();
                        return;
                    }
                }

                //设置训练结果
                //1.获取消耗卡路里
                upload.setCalorie_(Integer.parseInt(getenergy2.getText().toString()));
                //2.获取训练时长
                //已经在第一次校准时间处获取
                //3.最终功率
                upload.setPower_(Integer.parseInt(watt.getText().toString()));
                //4.最终运动距离
                upload.setFinalDistance_(Integer.parseInt(getdistance.getText().toString()));
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
                Intent intent = new Intent(BikeModeActivity.this,ByeActivity.class);
                //启动
                startActivity(intent);
                //结束当前Activity
                BikeModeActivity.this.finish();
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
            try{
                if (intentAction.equals("heartrate")){
                    getrate.setText(intent.getStringExtra("heartrate"));
                }
                else if (intentAction.equals("log")) {
                    if (intent.getStringExtra("log").equals("twologicard")||intent.getStringExtra("log").equals("twologiblue")) {
                        //如果连接成功，跳转个人设置界面
                        Log.e("BikeModeActivity","login successfully");
                        Intent activityintent = new Intent(BikeModeActivity.this,PersonalSettingActivity.class); //新建一个跳转到个人设置界面Activity的显式意图
                        startActivity(activityintent); //启动
                        BikeModeActivity.this.finish(); //结束当前Activity
                    }
                    else if (intent.getStringExtra("log").equals("twologocard")||intent.getStringExtra("log").equals("twologoblue")){
                        Log.e("BikeModeActivity","logout successfully");
                        Intent activityintent = new Intent(BikeModeActivity.this,MainActivity.class); //新建一个跳转到主界面Activity的显式意图
                        startActivity(activityintent); //启动
                        BikeModeActivity.this.finish(); //结束当前Activity
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
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
                    Intent activityintent = new Intent(BikeModeActivity.this,PersonalSettingActivity.class); //新建一个跳转到个人设置界面Activity的显式意图
                    startActivity(activityintent); //启动
                    BikeModeActivity.this.finish(); //结束当前Activity
                    break;
                //第二用户下线成功
                case CommonMessage.SECOND__DISCONNECTED:
                case CommonMessage.SECOND__LOGOUT:
                    LogUtil.d("广播接收器收到："+ commonMessage.toString());
                    //刷新主界面
                    BikeModeActivity.this.recreate();
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
