package com.bdl.airecovery.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Intent;

import com.bdl.airecovery.R;
import com.bdl.airecovery.appEnum.LoginResp;
import com.bdl.airecovery.base.BaseActivity;
import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.bluetooth.CommonCommand;
import com.bdl.airecovery.bluetooth.CommonMessage;
import com.bdl.airecovery.dialog.LargeDialog;
import com.bdl.airecovery.entity.CurrentTime;
import com.bdl.airecovery.service.BluetoothService;
import com.bdl.airecovery.service.MotorService;
import com.bdl.airecovery.service.StaticMotorService;
import com.bdl.airecovery.util.MessageUtils;
import com.google.gson.Gson;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;
import com.bdl.airecovery.service.CardReaderService;

import org.xutils.common.util.LogUtil;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.TimerTask;
import java.util.Timer;

@ContentView(R.layout.activity_main)
public class MainActivity extends BaseActivity {

    /*
     * 主界面
     * 主要业务：
     *      查询当前用户的资料（姓名）、当前设备的个人参数、当前设备的顺反力
     *      查询当前设备的图片
     *      根据蓝牙手环接收心率
     *      校准时间（训练时间/休息时间）
     *      根据当前用户的权限（会员/教练）：
     *          如果是教练刷卡/手环登录，显示“医护设置”按钮，不显示“连接教练手环”按钮；
     *          如果是会员刷卡登录，不显示“医护设置”按钮，显示“连接教练手环”按钮，教练可刷卡/手环登录；
     *          如果是会员刷手环登录，不显示“医护设置”按钮，不显示“连接教练手环”按钮，教练可刷卡登录。
     *      监听“开始训练”、“退出训练”、“医护设置”、“连接教练手环”按钮
     *      监听顺反向力的“+”和“-”按钮
     */

    //TODO:电机数据
    private int positiveTorqueLimited;
    private int negativeTorqueLimited;
    private int frontLimitedPosition;
    private int rearLimitedPosition;
    int motorDirection = MyApplication.getInstance().motorDirection;
    private eStopBroadcastReceiver eStopReceiver; //急停广播
    private int deviceType;
    //TODO:电机数据
    /**
     * 类成员
     */
    private Thread localCountDownThread;    //本机倒计时线程
    private Thread DelayThread;             //延迟执行线程
    private int localCountDown = 60;        //本机倒计时（单位：秒）
    private int localCountDownType = 0;     //本机倒计时类型（1运动，0休息）
    private String ready_time_text;         //模态框倒计时（单位：秒）
    private Handler handler;                //用于在UI线程中获取倒计时线程创建的Message对象，得到倒计时秒数与时间类型
    private Handler handler_dialog;         //用于模态框ui线程中获取倒计时线程创建的Message对象
    private Handler handler_dialoglocating; //用于电机定位模态框ui线程中获取倒计时线程创建的Message对象
    private TextView text_dialog;           //用于从条件模态框ui中向模态框倒计时方法传入控件
    private int flag_dialog;                //警告模态框弹出标志位
    private locationReceiver LocationReceiver = new locationReceiver();       //广播监听类
    private IntentFilter filterHR = new IntentFilter();                       //广播过滤器
    private Thread getCurrentTimeThread;    //获取同步/校准倒计时
    private LargeDialog dialog_ready;
    private LargeDialog dialog_locating;
    private boolean isDialogReadyDisplay = false;
    private int locateDone;                 //定位完成的项目数量
    private int locateTodo = 1;                 //需要定位的项目数量
    private BluetoothReceiver bluetoothReceiver;        //蓝牙广播接收器，监听用户的登录广播


    //控件绑定
    //TextView
    @ViewInject(R.id.tv_title)
    private TextView  tv_title;                 //界面左上角模式标题（文本格式：设置模式 - Xxx训练）
    @ViewInject(R.id.tv_user_name)
    private TextView tv_user_name;              //用户名（文本格式：x先生）
    @ViewInject(R.id.tv_training_duration_text)
    private TextView tv_training_duration_text; //训练时间/休息时间 文本（文本格式：X X 时 间）
    @ViewInject(R.id.tv_training_duration_num)
    private TextView tv_training_duration_num;  //训练时间/休息时间 数值（数值格式：X:XX）
    @ViewInject(R.id.tv_ms_positivenumber)
    private TextView tv_ms_positivenumber;      //顺向力数值
    @ViewInject(R.id.tv_ms_inversusnumber)
    private TextView tv_ms_inversusnumber;      //反向力数值
    @ViewInject(R.id.tv_ms_positive)
    private TextView tv_ms_positive;            //顺向力标题
    @ViewInject(R.id.tv_ms_inversus)
    private TextView tv_ms_inversus;            //反向力标题
    @ViewInject(R.id.tv_pulse_num)
    private TextView tv_pulse_num;              //心率显示（无法获取时，显示“—”）
    //ImageView
    @ViewInject(R.id.iv_dev)
    private ImageView iv_dev;                   //设备图片
    @ViewInject(R.id.iv_ms_positiveplus)
    private ImageView iv_ms_positiveplus;       //顺向力的“+”按钮
    @ViewInject(R.id.iv_ms_positiveminus)
    private ImageView iv_ms_positiveminus;      //顺向力的“-”按钮
    @ViewInject(R.id.iv_ms_inversusplus)
    private ImageView iv_ms_inversusplus;       //反向力的“+”按钮
    @ViewInject(R.id.iv_ms_inversusminus)
    private ImageView iv_ms_inversusminus;      //反向力的“-”按钮
    @ViewInject(R.id.iv_main_state)
    private ImageView iv_main_state;        //登陆状态
    //Button
    @ViewInject(R.id.btn_start)
    private Button btn_start;                   //“开始训练”按钮
    @ViewInject(R.id.btn_quit)
    private Button btn_quit;                    //“退出训练”按钮
    @ViewInject(R.id.connect_coach_bluetooth)
    private Button connect_coach_bluetooth;     //“教练协助”按钮

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initImmersiveMode();//隐藏状态栏，导航栏
        initMotor();
        queryDevInfo();     //查询设备信息
        queryUserInfo();    //查询用户信息,设置用户名
        //注册广播
        filterHR.addAction("heartrate");
        filterHR.addAction("log");
        filterHR.addAction("locate");
        registerReceiver(LocationReceiver,filterHR);
        //注册蓝牙用监听器
        bluetoothReceiver = new BluetoothReceiver();
        IntentFilter intentFilter = new IntentFilter("com.bdl.bluetoothmessage");
        registerReceiver(bluetoothReceiver,intentFilter);
//        testupload();
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
    }

    /**
     * 接收广播
     */
    private class eStopBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra("state");
            if (state != null && state.equals("1")) {
                startActivity(new Intent(MainActivity.this, ScramActivity.class));
                MainActivity.this.finish();
            }
        }
    }

    /**
     * 当Activity从后台重新回到前台时（由不可见变为可见），调用onStart()
     * 此时Activity处于【运行状态】
     */
    @Override
    protected void onResume() {
        super.onResume();
        //判空，确保线程不会重复创建
        if(localCountDownThread == null) {
            CreatelocalCountDownTheard(); //创建本机倒计时线程
            localCountDownThread.start(); //启动本机倒计时线程
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
        if(DelayThread != null) {
            DelayThread.interrupt(); //中断线程
            DelayThread = null;
        }
        if(localCountDownThread != null) {
            localCountDownThread.interrupt(); //中断线程
            localCountDownThread = null;
        }
        //同步/校准倒计时线程
        if(getCurrentTimeThread != null) {
            getCurrentTimeThread.interrupt(); //中断线程
            getCurrentTimeThread = null;
        }
    }

    /**
     * 查询设备信息，包括设备ID与设备名称，传给前端
     */
    private void queryDevInfo() {
        //判断是否获取到设备信息
        if(MyApplication.getInstance().getCurrentDevice() != null) {
            //判空
            if (MyApplication.getInstance().getCurrentDevice().getGeneralImg() != null && !MyApplication.getInstance().getCurrentDevice().getGeneralImg().equals("")) {
                //获取设备图（需要根据String找到资源文件中对应的ID）
                iv_dev.setImageResource(getResources().getIdentifier(MyApplication.getInstance().getCurrentDevice().getGeneralImg(),"drawable",getPackageName()));
            }
            if (MyApplication.getInstance().getCurrentDevice().getReverseForce() != null && MyApplication.getInstance().getCurrentDevice().getConsequentForce() != null){
                tv_ms_positivenumber.setText(MyApplication.getInstance().getCurrentDevice().getConsequentForce());//顺向力数值
                tv_ms_inversusnumber.setText(MyApplication.getInstance().getCurrentDevice().getReverseForce());   //反向力数值
            }
            //单车跑步机无顺反向力
            if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("椭圆跑步机") || MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("健身车"))
            {
                iv_ms_positiveplus.setVisibility(View.GONE);
                iv_ms_positiveminus.setVisibility(View.GONE);
                iv_ms_inversusplus.setVisibility(View.GONE);
                iv_ms_inversusminus.setVisibility(View.GONE);
                tv_ms_positivenumber.setVisibility(View.GONE);
                tv_ms_inversusnumber.setVisibility(View.GONE);
                tv_ms_positive.setVisibility(View.GONE);
                tv_ms_inversus.setVisibility(View.GONE);
            }
            //待定位项目
            if (MyApplication.getInstance().getCurrentDevice().getPersonalList()!=null){
                for (int i = 0; i < MyApplication.getInstance().getCurrentDevice().getPersonalList().size(); i++) {
                    if (MyApplication.getInstance().getCurrentDevice().getPersonalList().get(i).getMachine().equals("1")){
                        locateTodo++;
                    }else if (MyApplication.getInstance().getCurrentDevice().getPersonalList().get(i).getMachine().equals("2")){
                        locateTodo++;
                    }
                }
            }
        }
    }

    /**
     * 获取用户信息,设置用户名
     */
    private void queryUserInfo() {
        //判空，如果非空是正常登陆
        if(MyApplication.getInstance().getUser() != null) {
            //非空的情况下，把第一用户的名字进行设置到指定位置
            tv_user_name.setText(MyApplication.getInstance().getUser().getUsername());
            //界面左上角模式标题
            tv_title.setText("      " + MyApplication.getInstance().getUser().getTrainMode());
            //判断角色是否是教练，如果是教练设置按钮和状态图
            if (MyApplication.getInstance().getUser().getRole().equals("coach")){
                //第一用户是教练自己训练的场景
                connect_coach_bluetooth.setText("医护设置"); //更新为“医护设置”按钮
                iv_main_state.setImageDrawable(getResources().getDrawable(R.drawable.guanliyuan1));
            }else{
                //不是教练，一定是用户
                connect_coach_bluetooth.setText("教练协助");
                iv_main_state.setImageDrawable(getResources().getDrawable(R.drawable.yonghu1));
                //对于用户登陆的场景，特殊考虑，是否存在第二用户，如果存在，一定是教练在协助，则显示协助的信息
                if(MyApplication.getInstance().getUser().getHelperuser() != null){
                    connect_coach_bluetooth.setText("停止协助"); //更新为“停止协助”按钮
                    iv_main_state.setImageDrawable(getResources().getDrawable(R.drawable.shou));
                }
            }
        }else{
            //否则就是测试页面直接跳转到主页面，应该显示扳手图标，开发者名字，测试状态按钮
            tv_user_name.setText("开发者");
            iv_main_state.setImageDrawable(getResources().getDrawable(R.drawable.banshou1));
            connect_coach_bluetooth.setText("调试状态");
        }
    }

    //按钮监听事件
    //“开始训练”
    @Event(R.id.btn_start)
    private void setBtn_start_onClick(View v) {
        dialog_locating = new LargeDialog(MainActivity.this);
        LaunchHandlerLocating();
        if (MyApplication.getInstance().getUser()!=null && MyApplication.getInstance().getUser().getHelperuser()!=null) {
            Message message1 = handler_dialoglocating.obtainMessage();//当教练协助时，直接进入训练
            handler_dialoglocating.sendMessage(message1);
        } else {
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            if (localCountDownType == 1)//休息时间
            {
                //显示倒计时模态框
                dialog_ready = new LargeDialog(MainActivity.this);
                dialog_ready.setTitle("提示");
                //获取主界面当前倒计时所剩时间并显示于模态框内
                String ready_time_text = String.valueOf(localCountDown);
//                String ready_time = tv_training_duration_num.getText().toString();
//                ready_time_text = ready_time.substring(ready_time.length() - 2, ready_time.length());
                dialog_ready.setMessage("准备开始训练！\n" +
                        "训练将在" + ready_time_text + "秒后自动启动...");
                dialog_ready.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                dialog_ready.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                //布局位于状态栏下方
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                //全屏
                                //View.SYSTEM_UI_FLAG_FULLSCREEN |
                                //隐藏导航栏
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                        if (Build.VERSION.SDK_INT >= 19) {
                            uiOptions |= 0x00001000;
                        } else {
                            uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                        }
                        dialog_ready.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
                    }
                });
                dialog_ready.setCanceledOnTouchOutside(false);
                dialog_ready.show();
                //获取主界面当前倒计时所剩时间并显示于模态框内
                final TextView text = (TextView) dialog_ready.findViewById(R.id.large_dialog_msg);
                //dialog内倒计时进入训练界面
                text_dialog = null;//初始化
                CreateDialogCountDownTheard(text);
                isDialogReadyDisplay = true;
            } else if (localCountDown < 12)//训练时间小于12秒
            {
                //退出登录请求
                Intent intent2 = new Intent(MainActivity.this, CardReaderService.class);
                intent2.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
                startService(intent2);
                Intent intent = new Intent(MainActivity.this, BluetoothService.class);
                intent.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
                startService(intent);
                Log.e("MainActivity", "request to logout");
                //显示退出登录模态框
                final LargeDialog dialog_cannot = new LargeDialog(MainActivity.this);
                dialog_cannot.setTitle("提示");
                dialog_cannot.setMessage("低于合理训练时间的最低限（12秒），训练无法启动\n" +
                        "您将在3秒后退出系统...");
                //模态框隐藏导航栏
                dialog_cannot.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                dialog_cannot.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
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
                        dialog_cannot.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
                    }
                });
                dialog_cannot.setCanceledOnTouchOutside(false);
                dialog_cannot.show();
                //创建延迟线程（延迟3秒退出系统）
                DelayThread = new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        try {
                            Thread.sleep(3000);//休眠3秒
                            dialog_cannot.dismiss();
                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                            startActivity(intent);
                            MainActivity.this.finish();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                //启动线程
                DelayThread.start();
            } else //训练时间大于12秒，直接进入相应训练界面
            {
                LaunchDialogLocating(0);
            }
        }
    }

    //“退出训练”
    @Event(R.id.btn_quit)
    private void setBtn_quit_onClick(View v) {

        //退出登录请求
        Intent intentLog2 = new Intent(this, CardReaderService.class);
        intentLog2.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
        startService(intentLog2);
        Intent intentLog = new Intent(this, BluetoothService.class);
        intentLog.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
        startService(intentLog);
        Log.d("MainActivity","request to logout");


        Intent intent =new Intent(MainActivity.this,LoginActivity.class);
        startActivity(intent);
        MainActivity.this.finish();//销毁当前activity
    }
    //扫描教练的定时任务
    Timer timer = new Timer();
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            Intent intent = new Intent(MainActivity.this, BluetoothService.class);
            intent.putExtra("command", CommonCommand.SECOND__LOGIN.value());
            startService(intent);

        }
    };

    //“教练协助”
    @Event(R.id.connect_coach_bluetooth)
    private void setConnect_coach_bluetooth_onClick(View v) {
        //新建定时任务对象
        Log.e("coach_bluetooth_onClick","触发点击事件");
        //如果是教练协助
        if(connect_coach_bluetooth.getText().equals("教练协助")) {
            connect_coach_bluetooth.setText("教练协助...");
            Intent intent2 = new Intent(MainActivity.this, CardReaderService.class);
            intent2.putExtra("command", CommonCommand.SECOND__LOGIN.value());
            startService(intent2);
            timer.schedule(task,0,2000);

            Log.d("MainActivity","request to login");
        }
        //如果是停止协助
        else if(connect_coach_bluetooth.getText().equals("停止协助")){
            Intent intent2 = new Intent(MainActivity.this, CardReaderService.class);
            intent2.putExtra("command", CommonCommand.SECOND__LOGOUT.value());
            startService(intent2);
            Intent intent = new Intent(MainActivity.this, BluetoothService.class);
            intent.putExtra("command", CommonCommand.SECOND__LOGOUT.value());
            startService(intent);
            Log.d("MainActivity","request to logout");
        }
        //如果是医护设置
        else if(connect_coach_bluetooth.getText().equals("医护设置")){
            //跳转医护设置界面
            Intent intent = new Intent(MainActivity.this,PersonalSettingActivity.class); //新建一个跳转到医护设置界面Activity的显式意图
            startActivity(intent); //启动
            MainActivity.this.finish(); //结束当前Activity
        }
    }

    //顺向力的“+”
    @Event(R.id.iv_ms_positiveplus)
    private void setIv_ms_positiveplus_onClick(View v) {
        //显示
        if (Integer.valueOf(tv_ms_positivenumber.getText().toString()) < 99 && Integer.valueOf(tv_ms_inversusnumber.getText().toString()) < 99) {
            tv_ms_positivenumber.setText(Integer.valueOf(tv_ms_positivenumber.getText().toString()) + 1 + "");
            tv_ms_inversusnumber.setText(Integer.valueOf(tv_ms_inversusnumber.getText().toString()) + 1 + "");
            MyApplication.getInstance().getCurrentDevice().setReverseForce(tv_ms_inversusnumber.getText().toString());
            MyApplication.getInstance().getCurrentDevice().setConsequentForce(tv_ms_positivenumber.getText().toString());
        }
    }
    //顺向力的“-”
    @Event(R.id.iv_ms_positiveminus)
    private void setIv_ms_positiveminus_onClick(View v) {
        //显示
        if (Integer.valueOf(tv_ms_positivenumber.getText().toString()) > 5) {
            tv_ms_positivenumber.setText(Integer.valueOf(tv_ms_positivenumber.getText().toString()) - 1 + "");
            tv_ms_inversusnumber.setText(Integer.valueOf(tv_ms_inversusnumber.getText().toString()) - 1 + "");
            MyApplication.getInstance().getCurrentDevice().setReverseForce(tv_ms_inversusnumber.getText().toString());
            MyApplication.getInstance().getCurrentDevice().setConsequentForce(tv_ms_positivenumber.getText().toString());
        }
    }
    //反向力的“+”
    @Event(R.id.iv_ms_inversusplus)
    private void setIv_ms_inversusplus_onClick(View v) {
        //显示
        if (Integer.valueOf(tv_ms_inversusnumber.getText().toString()) < 99) {
            tv_ms_inversusnumber.setText(Integer.valueOf(tv_ms_inversusnumber.getText().toString()) + 1 + "");
            MyApplication.getInstance().getCurrentDevice().setReverseForce(tv_ms_inversusnumber.getText().toString());
        }
        //警告模态框
        if (Integer.valueOf(tv_ms_inversusnumber.getText().toString()) > Integer.valueOf(tv_ms_positivenumber.getText().toString())*1.5){
            //弹出50%
            if (flag_dialog != 2){
                flag_dialog = 2;
                LaunchDialogAlert();
            }
        }
        else if (Integer.valueOf(tv_ms_inversusnumber.getText().toString()) > Integer.valueOf(tv_ms_positivenumber.getText().toString())*1.3){
            //弹出30%
            if (flag_dialog != 1 && flag_dialog != 2){
                flag_dialog = 1;
                LaunchDialogAlert();
            }
        }
    }
    //反向力的“-”
    @Event(R.id.iv_ms_inversusminus)
    private void setIv_ms_inversusminus_onClick(View v) {
        //显示
        if (Integer.valueOf(tv_ms_inversusnumber.getText().toString()) > Integer.valueOf(tv_ms_positivenumber.getText().toString())) {
            tv_ms_inversusnumber.setText(Integer.valueOf(tv_ms_inversusnumber.getText().toString()) - 1 + "");
            MyApplication.getInstance().getCurrentDevice().setReverseForce(tv_ms_inversusnumber.getText().toString());
        }
        //模态框标志位
        if (Integer.valueOf(tv_ms_inversusnumber.getText().toString()) <= Integer.valueOf(tv_ms_positivenumber.getText().toString())*1.3)
            flag_dialog = 0;
        else if (Integer.valueOf(tv_ms_inversusnumber.getText().toString()) <= Integer.valueOf(tv_ms_positivenumber.getText().toString())*1.5)
            flag_dialog = 1;
    }

    /**
     * 显示电机定位模态框
     */
    private void LaunchDialogLocating(int option) {
        //显示定位模态框
        if (option == 0) {
            dialog_locating.setTitle("注意");
            dialog_locating.setMessage("杠杆正在定位到训练开始的位置...\n" +
                    "请勿阻碍杠杆运动！");
            dialog_locating.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            dialog_locating.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            //布局位于状态栏下方
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            //全屏
                            //View.SYSTEM_UI_FLAG_FULLSCREEN |
                            //隐藏导航栏
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                    if (Build.VERSION.SDK_INT >= 19) {
                        uiOptions |= 0x00001000;
                    } else {
                        uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                    }
                    dialog_locating.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
                }
            });
            dialog_locating.setCanceledOnTouchOutside(false);
            dialog_locating.show();

            //动态电机初始化部分
            positiveTorqueLimited = Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getConsequentForce()) * 100;
            negativeTorqueLimited = Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getReverseForce()) * 100;
            switch (deviceType) {
                case 1: //拉设备
                    MotorService.getInstance().initializationBeforeStart(frontLimitedPosition, deviceType, positiveTorqueLimited, negativeTorqueLimited);
                    break;
                case 2: //推设备
                    MotorService.getInstance().initializationBeforeStart(rearLimitedPosition, deviceType, negativeTorqueLimited, positiveTorqueLimited);
                    break;
                case 3: //躯干扭转组合
                    if (motorDirection == 1) {
                        int[] newLimitedPosition = MessageUtils.recersalLimit(frontLimitedPosition, rearLimitedPosition, 1);
                        frontLimitedPosition = newLimitedPosition[0];
                        rearLimitedPosition = newLimitedPosition[1];
                        MotorService.getInstance().initializationBeforeStart(rearLimitedPosition, 2, negativeTorqueLimited, positiveTorqueLimited);
                    } else if (motorDirection == 2) {
                        int[] newLimitedPosition = MessageUtils.recersalLimit(frontLimitedPosition, rearLimitedPosition, 2);
                        frontLimitedPosition = newLimitedPosition[0];
                        rearLimitedPosition = newLimitedPosition[1];
                        MotorService.getInstance().initializationBeforeStart(frontLimitedPosition, 1, positiveTorqueLimited, negativeTorqueLimited);
                    }
                    break;
            }
            //处理静态电机
            StaticMotorService.Controler controler = StaticMotorService.getControler();
            for (int i = 0; i < MyApplication.getInstance().getCurrentDevice().getPersonalList().size(); i++) {
                if (MyApplication.getInstance().getCurrentDevice().getPersonalList().get(i).getMachine().equals("1")) {
                    int MotorType = Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(i).getType());
                    controler.trainLocate(1, Integer.valueOf(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(i).getValue()) * 10 + 10,MotorType);
                } else if (MyApplication.getInstance().getCurrentDevice().getPersonalList().get(i).getMachine().equals("2")) {
                    int MotorType = Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(i).getType());
                    controler.trainLocate(2, Integer.valueOf(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(i).getValue()) * 10 + 10,MotorType);
                }
            }

        } else if (option == 1){                               //定位完成
            if (!isDialogReadyDisplay) {
                dialog_locating.dismiss();//隐藏定位模态框
                LaunchModeActivity();
            }
        } else if (option == 2){                               //定位失败
            dialog_locating.dismiss();//隐藏定位模态框
            //显示定位失败模态框
            final LargeDialog dialog_locaterror = new LargeDialog(MainActivity.this);
            dialog_locaterror.setTitle("错误");
            dialog_locaterror.setMessage("定位出现错误！\n" +
                    "请重新定位");
            dialog_locaterror.setPositiveBtnText("重新定位");
            dialog_locaterror.setOnPositiveClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Message message1 = handler_dialoglocating.obtainMessage();
                    message1.what = 0;
                    handler_dialoglocating.sendMessage(message1);
                    dialog_locaterror.dismiss();
                    Log.d("11111111", "run: 22222222222222222222222222");
                }
            });
            dialog_locaterror.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            dialog_locaterror.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            //布局位于状态栏下方
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            //全屏
                            //View.SYSTEM_UI_FLAG_FULLSCREEN |
                            //隐藏导航栏
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                    if (Build.VERSION.SDK_INT >= 19) {
                        uiOptions |= 0x00001000;
                    } else {
                        uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                    }
                    dialog_locaterror.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
                }
            });
            dialog_locaterror.setCanceledOnTouchOutside(false);
            dialog_locaterror.show();
        }
    }

    /**
     * 启动电机定位模态框handler
     */
    private void LaunchHandlerLocating() {
        //创建Handler，用于显示定位模态框
        handler_dialoglocating = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                LaunchDialogLocating(msg.what);
            }
        };
    }

    /**
     * 启动超负荷警告模态框
     */
    private void LaunchDialogAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        //加载警示视图
        View viewalert = View.inflate(MainActivity.this, R.layout.dialog_mode_alert, null);
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
        TextView text_positive = (TextView)dialog.findViewById(R.id.tv_alert_getpositive);
        TextView text_inversus = (TextView)dialog.findViewById(R.id.tv_alert_getinversus);
        TextView text_high = (TextView)dialog.findViewById(R.id.tv_alert_highvalue);
        String value_positive = tv_ms_positivenumber.getText().toString();
        String value_inversus = tv_ms_inversusnumber.getText().toString();
        String value_high = String.valueOf((Integer.valueOf(value_inversus)-Integer.valueOf(value_positive))*100/Integer.valueOf(value_positive));
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
        lp.height = (int)(size.y * 0.6);
        window.setAttributes(lp);
    }

    /**
     * 启动相应训练模式界面
     */
    private void LaunchModeActivity() {//TODO:处理顺反向力
        Intent intent = null;
        if (MyApplication.getInstance().getUser() != null) {
            switch (MyApplication.getInstance().getUser().getTrainMode()) {
                case "康复模式":
                    //康复模式Activity
                    intent = new Intent(MainActivity.this,StandardModeActivity.class);
                    break;
                case "被动模式":
                    //被动模式Activity
                    intent = new Intent(MainActivity.this,PassiveModeActivity.class);
                    break;
                case "主被动模式":
                    //主被动模式Activity
                    intent = new Intent(MainActivity.this,ActivePassiveModeActivity.class);
                    break;
                default: break;
            }
        }
        //启动
        if (intent != null) {
            startActivity(intent);
            MainActivity.this.finish();
        }
    }

    /**
     * 创建模态框倒计时线程
     */
    public void CreateDialogCountDownTheard(final TextView text) {
        //创建Handler，用于模态框倒计时
        handler_dialog = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                int arg1 = msg.arg1;
                text_dialog = text;

                //设置文本内容
                text_dialog.setText("准备开始训练！\n" +
                        "训练将在" + arg1 + "秒后自动启动...");
            }
        };
    }

    /**
     * 创建本机倒计时线程（如果时间显示器宕机，需要用到该倒计时）
     */
    public void CreatelocalCountDownTheard() {
        //创建Handler，用于在UI线程中获取倒计时线程创建的Message对象，得到倒计时秒数与时间类型
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                //获取倒计时秒数
                int arg1 = msg.arg1;
                //设置文本颜色（区分训练时间与休息时间）
                if(msg.what == 0) {
                    //如果当前时间为训练时间
                    tv_training_duration_text.setText("训 练 时 间");
                    tv_training_duration_num.setTextColor(tv_training_duration_num.getResources().getColor(R.color.DeepSkyBlue)); //深天蓝色
                }
                if(msg.what == 1) {
                    //如果当前时间为休息时间
                    tv_training_duration_text.setText("休 息 时 间");
                    tv_training_duration_num.setTextColor(tv_training_duration_num.getResources().getColor(R.color.OrangeRed)); //橘红色
                }

                //设置文本内容（有两种特殊情况，单独设置合适的文本格式）
                int minutes = arg1 / 60;
                int remainSeconds = arg1 % 60;
                if(remainSeconds < 10) {
                    tv_training_duration_num.setText(minutes + ":0" + remainSeconds);
                } else {
                    tv_training_duration_num.setText(minutes + ":" + remainSeconds);
                }
            }
        };
        //如果是健身车/跑步机，特殊处理，训练时间240秒
        if(MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("健身车") || MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("椭圆跑步机")) {
            localCountDown = 240;
        } else {
            localCountDown = 60;
        }
        localCountDownThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(localCountDownThread != null) {
                    while(!localCountDownThread.isInterrupted() && localCountDown > 0) {
                        if(MyApplication.getCurrentTime().getType() != -1) {
                            //校准本机倒计时秒数
                            localCountDown = MyApplication.getCurrentTime().getSeconds(); //获取秒数
                            localCountDownType = MyApplication.getCurrentTime().getType(); //获取时间类型
                            //Log.d("同步倒计时","MainActivity：校准当前时间成功，当前时间："+MyApplication.getCurrentTime().toString());
                            //Log.d("同步倒计时","MainActivity：localCountDown " + String.valueOf(localCountDown));
                            MyApplication.setCurrentTime(new CurrentTime(-1,-1)); //将全局变量currentTime恢复为(-1,-1)，即一旦有值，取后销毁，实现另一种方式的传递。
                            //Log.d("同步倒计时","MainActivity：校准当前时间成功，恢复currentTime，当前currentTime："+MyApplication.getCurrentTime().toString());
                        }
                        //将当前倒计时数值存储在Message对象中，通过Handler将消息发送给UI线程，更新UI
                        Message message = handler.obtainMessage();
                        message.what = localCountDownType; //what属性指定为当前时间的类型（0为训练时间，1为休息时间）
                        message.arg1 = localCountDown; //arg1属性指定为当前时间的秒数
                        handler.sendMessage(message); //把一个包含消息数据的Message对象压入到消息队列中
                        if (isDialogReadyDisplay) {
                            //将当前倒计时数值存储在Message对象中，通过Handler将消息发送给UI线程，更新UI
                            Message message2 = handler_dialog.obtainMessage();
                            message2.arg1 = localCountDown; //arg1属性指定为当前时间的秒数
                            handler_dialog.sendMessage(message2); //把一个包含消息数据的Message对象压入到消息队列中
                            if (localCountDownType == 0) {
                                dialog_ready.dismiss();
                                isDialogReadyDisplay = false;
                                Message message1 = handler_dialoglocating.obtainMessage();
                                message1.what = 0;
                                handler_dialoglocating.sendMessage(message1);

                            }
                        }
                        if (handler_dialoglocating != null && !isDialogReadyDisplay) {
                            Log.d("静态", "locateDone:"+locateDone+"   locateTodo:"+locateTodo);//TODO

                            if (locateDone == locateTodo){
                                Message message1 = handler_dialoglocating.obtainMessage();
                                message1.what = 1;
                                handler_dialoglocating.sendMessage(message1);
                            }
                        }
                        //线程睡眠1s
                        try {
                            Thread.sleep(1000);
                            localCountDown--;
                        } catch(InterruptedException e) {
                            e.printStackTrace();
                            return;
                        }

                    }
                    //如果之前是运动时间
                    if(localCountDownType == 0) {
                        localCountDownType = 1; //更换为休息时间
                        localCountDown = 30;
                    } else {
                        localCountDownType = 0; //更换为运动时间
                        //如果是健身车/跑步机，特殊处理，训练时间240秒
                        if(MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("健身车") || MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("椭圆跑步机")) {
                            localCountDown = 240;
                        } else {
                            localCountDown = 60;
                        }
                    }
                }
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
                    tv_pulse_num.setText(intent.getStringExtra("heartrate"));
                }
                else if (intentAction.equals("log")) {
                    if (intent.getStringExtra("log").equals(LoginResp.LOCALTWOLOGICARD.getStr())||intent.getStringExtra("log").equals(LoginResp.LOCALTWOLOGIBLUE.getStr())
                            ||intent.getStringExtra("log").equals(LoginResp.REMOTETWOLOGIBLUE.getStr())
                            ||intent.getStringExtra("log").equals(LoginResp.REMOTETWOLOGICARD.getStr())) {
                        //如果连接成功，跳转医护设置界面
                        Log.e("MainActivity","login successfully");
                        Intent activityintent = new Intent(MainActivity.this,PersonalSettingActivity.class); //新建一个跳转到医护设置界面Activity的显式意图
                        startActivity(activityintent); //启动
                        MainActivity.this.finish(); //结束当前Activity
                    }
                    else if (intent.getStringExtra("log").equals(LoginResp.LOCALTWOLOGOCARD.getStr())||intent.getStringExtra("log").equals(LoginResp.LOCALTWOLOGOBLUE.getStr())
                            ||intent.getStringExtra("log").equals(LoginResp.REMOTETWOLOGOBLUE.getStr())
                            ||intent.getStringExtra("log").equals(LoginResp.REMOTETWOLOGOCARD.getStr())){
                        //刷新主界面
                        MainActivity.this.recreate();
                    }
                }
                else if (intentAction.equals("locate")){
                    //动态电机定位成功的广播
                    if (intent.getBooleanExtra("success", false)){
                        locateDone++;
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
                    Intent activityintent = new Intent(MainActivity.this,PersonalSettingActivity.class); //新建一个跳转到医护设置界面Activity的显式意图
                    startActivity(activityintent); //启动
                    MainActivity.this.finish(); //结束当前Activity
                    break;
                //第二用户下线成功
                case CommonMessage.SECOND__DISCONNECTED:
                case CommonMessage.SECOND__LOGOUT:
                    LogUtil.d("广播接收器收到："+ commonMessage.toString());
                    //刷新主界面
                    MainActivity.this.recreate();
                    break;
                //获得心率
                case CommonMessage.HEART_BEAT:
                    LogUtil.d("广播接收器收到："+ commonMessage.toString());
                    tv_pulse_num.setText(commonMessage.getAttachment());
                    break;
                default:
                    LogUtil.e("未知广播，收到message：" + commonMessage.getMsgType());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(LocationReceiver);
        if(bluetoothReceiver != null) {
            try {
                unregisterReceiver(bluetoothReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        unregisterReceiver(eStopReceiver);
        //结束扫描蓝牙的定时任务
        timer.cancel();
    }

}
