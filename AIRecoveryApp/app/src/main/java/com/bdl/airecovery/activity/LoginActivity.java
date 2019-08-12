package com.bdl.airecovery.activity;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bdl.airecovery.R;
import com.bdl.airecovery.base.BaseActivity;

import org.xutils.common.util.LogUtil;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.bluetooth.CommonCommand;
import com.bdl.airecovery.bluetooth.CommonMessage;
import com.bdl.airecovery.dialog.CommonDialog;
import com.bdl.airecovery.entity.CurrentTime;
import com.bdl.airecovery.service.BluetoothService;
import com.bdl.airecovery.service.CardReaderService;
import com.bdl.airecovery.util.SendReqOfCntTimeUtil;
import com.google.gson.Gson;

import java.util.Timer;
import java.util.TimerTask;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
@ContentView(R.layout.activity_login)
public class LoginActivity extends BaseActivity {

    /**
     * 待机登录界面
     * 主要业务：
     *      1. 查询设备信息
     *      2. 本地倒计时
     *      3. 同步时间服务器倒计时
     *      4. 监听广播（蓝牙/发卡器）
     */

    /**
     * 类成员
     */
    private Thread localCountDownThread;                //本机倒计时线程
    private int localCountDown = 60;                    //本机倒计时（单位：秒）
    private int localCountDownType = 0;                 //本机倒计时类型（0运动，1休息）
    private Handler handler;                            //用于在UI线程中获取倒计时线程创建的Message对象，得到倒计时秒数与时间类型
    private int clickCount = 0;                         //ID 点击计数器
    private Thread clearClickCountThread;               //清空点击计数器线程
    private CommonDialog commonDialog;                  //ShowTips弹模态框
    private SendReqOfCntTimeUtil sendReqOfCntTimeUtil;  //发送同步时间请求的工具
    private BluetoothReceiver bluetoothReceiver;        //蓝牙广播接收器，监听用户的登录广播

    private eStopBroadcastReceiver eStopReceiver; //急停广播

    /**
     * 控件绑定
     */
    //TextView
    @ViewInject(R.id.tv_dev_id)
    private TextView tv_dev_id;         //设备ID（body strong <ID>)
    @ViewInject(R.id.tv_dev_name)
    private TextView tv_dev_name;       //设备名称
    @ViewInject(R.id.tv_time)
    private TextView tv_time;           //倒计时
    //ImageView
    @ViewInject(R.id.iv_muscle_image)
    private ImageView iv_muscle_image;  //锻炼肌肉图


    Timer timer = new Timer();
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            startBlueAndCardScan();
        }
    };

    /**
     * 接收广播
     */
    private class eStopBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra("state");
            if (state != null && state.equals("1")) {
                startActivity(new Intent(LoginActivity.this, ScramActivity.class));
                LoginActivity.this.finish();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initImmersiveMode(); //隐藏状态栏与导航栏
        queryDevInfo(); //查询设备信息
        syncCurrentTime(); //同步当前时间


        //重回页面重新登录
        //resetBt();
        //resetCR();

        timer.schedule(task,200,500);

        registerLoginReceiver(); //登录监听广播接收器的注册

        createClearClickCountThread(); //清空计数器线程
        clearClickCountThread.start();

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
        IntentFilter filter = new IntentFilter();
        filter.addAction("E-STOP");
        eStopReceiver = new eStopBroadcastReceiver();
        registerReceiver(eStopReceiver, filter);

        //执行待机页面的大退逻辑-发卡器大退
        Intent intent = new Intent(this, CardReaderService.class);
        intent.putExtra("command", CommonCommand.ALL__LOGOUT.value());
        startService(intent);
        //执行待机页面的大退逻辑-蓝牙大退
        Intent intent2 = new Intent(this, BluetoothService.class);
        intent2.putExtra("command", CommonCommand.ALL__LOGOUT.value());
        startService(intent2);
        MyApplication.getInstance().setUser(null);

        //启动发卡器扫描
        Intent intent3 = new Intent(this, CardReaderService.class);
        intent3.putExtra("command", CommonCommand.FIRST__LOGIN.value());
        startService(intent3);
        LogUtil.d("发出了启动卡扫描的命令");
    }

    /**
     * 当Activity已经完全不可见时，调用onStop()
     * 此时Activity处于【停止状态】
     */
    @Override
    protected void onStop() {
        super.onStop();
        //判空
        if(localCountDownThread != null) {
            try {
                localCountDownThread.interrupt(); //中断线程
                localCountDownThread = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //停止发送同步时间请求的Timer TimerTask
        if(sendReqOfCntTimeUtil.timer != null && sendReqOfCntTimeUtil.timerTask != null) {
            sendReqOfCntTimeUtil.timerTask.cancel();
            sendReqOfCntTimeUtil.timer.cancel();

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        task.cancel();
        //解注册广播
        if(bluetoothReceiver != null) {
            try {
                unregisterReceiver(bluetoothReceiver);
                Log.e("LoginActivity", "解注册广播");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        unregisterReceiver(eStopReceiver);
    }

    /**
     * 同步当前时间
     */
    private void syncCurrentTime() {
        //同步时间服务器业务
        sendReqOfCntTimeUtil = new SendReqOfCntTimeUtil();
        sendReqOfCntTimeUtil.SendRequestOfCurrentTime();
    }

    /**
     * 重启APP
     *
     */
    private void restartApp() {
        Intent intent = new Intent(getBaseContext(), SelfUpdatingActivity.class);
        @SuppressLint("WrongConstant") PendingIntent restartIntent = PendingIntent.getActivity(getBaseContext(), 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
        AlarmManager mgr = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, restartIntent);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * 中上方body strong ID 点击事件
     * 一定时间内点击5次，重启程序
     * 计数器clickCount
     */
    @Event(R.id.tv_dev_id)
    private void tv_dev_id_onClick(View v) {
        if(clickCount < 5) {
            ++clickCount;
        } else {
            //点击5次，重启程序
            restartApp();
        }
    }

    /**
     * 清空计数器线程
     */
    private void createClearClickCountThread() {
        clearClickCountThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //如果计数器不为空
                if(clickCount != 0) {
                    //3秒后清空计数器
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                    clickCount = 0;
                }
            }
        });
    }


    /**
     * 启动蓝牙与发卡器扫描
     */
    private void startBlueAndCardScan() {

//        //如果登录成功了，则不会继续发消息了，这里必须被注释掉，否则等不到发广播就直接跳转了
//        if (MyApplication.getInstance().getUser()!=null) {
//
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            loginSuccessed();
//            return;
//        }


        //启动蓝牙扫描
        Intent intent = new Intent(this, BluetoothService.class);
        intent.putExtra("command", CommonCommand.FIRST__LOGIN.value());
        startService(intent);
        LogUtil.d("发出了启动蓝牙扫描的命令");

    }

    /**
     * 登录监听广播接收器的注册
     */
    private void registerLoginReceiver() {
        //注册登录广播监听器
        bluetoothReceiver = new BluetoothReceiver();
        IntentFilter intentFilter = new IntentFilter("com.bdl.bluetoothmessage");
        registerReceiver(bluetoothReceiver,intentFilter);
    }

    /**
     * 查询设备信息，包括设备ID与设备名称，传给前端
     */
    private void queryDevInfo() {
        if(MyApplication.getCurrentTime() == null) {
            return;
        }
        //判断是否获取到设备信息
        if(MyApplication.getInstance().getCurrentDevice().getDeviceInnerID() != null && !MyApplication.getInstance().getCurrentDevice().getDeviceInnerID().equals("")) {
            int deviceId = Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getDeviceInnerID());
            deviceId++;
            tv_dev_id.setText("BodyStrong " + String.valueOf(deviceId));
        }
        if(MyApplication.getInstance().getCurrentDevice().getDisplayName() != null && !MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("")) {
            //获取设备名称
            if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("躯干扭转组合"))
            {
                switch (MyApplication.getInstance().motorDirection) {
                    case 1:
                        tv_dev_name.setText(MyApplication.getInstance().getCurrentDevice().getDisplayName() + "向右");
                        break;
                    case 2:
                        tv_dev_name.setText(MyApplication.getInstance().getCurrentDevice().getDisplayName() + "向左");
                        break;
                }
            } else {
                tv_dev_name.setText(MyApplication.getInstance().getCurrentDevice().getDisplayName());
            }
        }
        if(MyApplication.getInstance().getCurrentDevice().getMuscleImg() != null && !MyApplication.getInstance().getCurrentDevice().getMuscleImg().equals("")) {
            //获取肌肉图（需要根据String找到资源文件中对应的ID）
            iv_muscle_image.setImageResource(getResources().getIdentifier(MyApplication.getInstance().getCurrentDevice().getMuscleImg(),"drawable",getPackageName()));
        }
    }

    /**
     * 用于界面提示，主要是连接不上教练机与查询不到登录用户的信息
     * @param value
     */
    private void showTips(String value) {
        //弹出对话框之前，先检查当前界面是否存在对话框，如果存在，先关闭，在执行下方的逻辑
        if (commonDialog != null && commonDialog.isShowing()) {
            return;
        }
        commonDialog = new CommonDialog(LoginActivity.this);
        commonDialog.setTitle("温馨提示");
        commonDialog.setMessage(value);
        commonDialog.setPositiveBtnText("我知道了");
        commonDialog.setOnPositiveClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commonDialog.dismiss();
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

    private void loginSuccessed() {
        //用户已登录，执行跳转主界面逻辑
        LogUtil.d("loginactivity广播接收器收到---跳转");
        Intent skipIntent = new Intent(LoginActivity.this,MainActivity.class); //新建一个跳转到主界面Activity的显式意图
        startActivity(skipIntent); //启动
        LoginActivity.this.finish(); //结束当前Activity
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
                //设置文本颜色（区分训练时间与休息时间）
                if(msg.what == 0) {
                    //如果当前时间为训练时间
                    tv_time.setTextColor(tv_time.getResources().getColor(R.color.DeepSkyBlue)); //深天蓝色
                }
                if(msg.what == 1) {
                    //如果当前时间为休息时间
                    tv_time.setTextColor(tv_time.getResources().getColor(R.color.OrangeRed)); //橘红色
                }

                //设置文本内容（有两种特殊情况，单独设置合适的文本格式）
                int minutes = arg1 / 60;
                int remainSeconds = arg1 % 60;
                if(remainSeconds < 10) {
                    tv_time.setText(minutes + ":0" + remainSeconds);
                } else {
                    tv_time.setText(minutes + ":" + remainSeconds);
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
                            MyApplication.setCurrentTime(new CurrentTime(-1,-1)); //将全局变量currentTime恢复为(-1,-1)，即一旦有值，取后销毁，实现另一种方式的传递。
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
     * 广播接收器
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
            LogUtil.d("收到广播："+messageJson);
            switch (commonMessage.getMsgType()){
                //第一用户登录成功
                case CommonMessage.FIRST__LOGIN_REGISTER_OFFLINE:
                case CommonMessage.FIRST__LOGIN_REGISTER_ONLINE:
                case CommonMessage.FIRST__LOGIN_SUCCESS_OFFLINE:
                case CommonMessage.FIRST__LOGIN_SUCCESS_ONLINE:
                    LogUtil.d("loginactivity广播接收器收到："+ commonMessage.toString());
                    //登录成功时，执行跳转的逻辑
                    loginSuccessed();
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
                    break;
                //第二用户下线成功
                case CommonMessage.SECOND__DISCONNECTED:
                case CommonMessage.SECOND__LOGOUT:
                    LogUtil.d("广播接收器收到："+ commonMessage.toString());
                    break;
                //获得心率
                case CommonMessage.HEART_BEAT:
                    LogUtil.d("广播接收器收到："+ commonMessage.toString());
                    break;
                default:
                    LogUtil.e("未知广播，收到message：" + commonMessage.getMsgType());
            }
        }
    }

}
