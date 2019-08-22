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
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bdl.airecovery.R;
import com.bdl.airecovery.base.BaseActivity;

import org.xutils.common.util.LogUtil;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.biz.LoginBiz;
import com.bdl.airecovery.bluetooth.CommonCommand;
import com.bdl.airecovery.bluetooth.CommonMessage;
import com.bdl.airecovery.dialog.CommonDialog;
import com.bdl.airecovery.dialog.LoginDialog;
import com.bdl.airecovery.entity.login.User;
import com.bdl.airecovery.service.BluetoothService;
import com.bdl.airecovery.service.CardReaderService;
import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
@ContentView(R.layout.activity_login)
public class LoginActivity extends BaseActivity {

    /**
     * 待机登录界面
     * 主要业务：
     *      1. 查询设备信息
     *      2. 监听广播（蓝牙/发卡器）
     */

    /**
     * 类成员
     */
    private NfcReceiver nfcReceiver;//接收NFC标签信息的广播
    private BluetoothReceiver bluetoothReceiver;//蓝牙广播接收器，监听用户的登录广播
    private String nfcMessage;//NFC标签
    private LoginDialog loginDialog;                  //ShowLogin弹模态框
    private CommonDialog commonDialog;                  //ShowTips弹模态框
    //第一用户登录指令的变量
    private volatile int whoLogin = 0;
    //设置全局当前时间变量
    private String nowDate;

    /**
     * 控件绑定
     */
    //TextView
    @ViewInject(R.id.tv_dev_id)
    private TextView tv_dev_id;         //设备ID（body strong <ID>)
    @ViewInject(R.id.tv_dev_name)
    private TextView tv_dev_name;       //设备名称
    //ImageView
    @ViewInject(R.id.iv_muscle_image)
    private ImageView iv_muscle_image;  //锻炼肌肉图

    //Button
    @ViewInject(R.id.btn_quick_login)
    private Button btn_quick_login;



    //按钮监听事件，快速登录点击跳转训练模块
    @Event(R.id.btn_quick_login)
    private void setBtn_quick_login(View v) {
        User user = new User();
        //初始化待训练设备
        String str1 ="[ P00,P01,P02,P03,P04,P05,P06,P07,P08,P09,E10,E11,E12,E13,E14,E15,E16,E28,E10]";
        user.setDeviceTypearrList(str1);
        user.setUsername("体验者");
        user.setExisitSetting(false);
        user.setMoveWay(0);
        user.setGroupCount(5);
        user.setGroupNum(10);
        user.setRelaxTime(30);
        user.setSpeedRank(1);
        user.setAge(30);
        user.setWeight(60);
        user.setHeartRatemMax(190);
        user.setTrainMode("康复模式");
        MyApplication.getInstance().setUser(user);
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);//新建一个跳转到主界面Activity的显式意图
        startActivity(intent); //启动
        LoginActivity.this.finish(); //结束当前Activity
    }


    /**
     * NFC标签广播接受的注册
     */
    private void registerNfcReceiver() {
        //注册登录广播监听器
        nfcReceiver = new NfcReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.bdl.airecovery.service.UsbService");
        registerReceiver(nfcReceiver,intentFilter);
    }
    /**
     * 蓝牙返回信息的注册
     */
    private void registerBluetoothReceiver(){
        //注册蓝牙广播监听器
        bluetoothReceiver=new BluetoothReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.bdl.bluetoothmessage");
        registerReceiver(bluetoothReceiver,intentFilter);
    }

    /**
     * 接收NFC标签广播
     */
    private class NfcReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            nfcMessage=intent.getStringExtra("bind_id");
            showLogin();//弹出登录模态框
            loginExecute(nfcMessage);//请求登录
            startBluetooth();//开启蓝牙扫描
        }
    }
    /**
     * 启动蓝牙扫描
     */
    private void startBluetooth(){
        //启动蓝牙扫描
        Intent intent = new Intent(this, BluetoothService.class);
        intent.putExtra("command", nfcMessage);
        startService(intent);
        LogUtil.d("发出了启动蓝牙扫描的命令");
    }

    /**
     * 用于界面提示，在用户连接蓝牙的时间段里，弹出正在登录模态框
     */
    private void showLogin() {
        //弹出对话框之前，先检查当前界面是否存在对话框，如果存在，先关闭，在执行下方的逻辑
        if (loginDialog != null && loginDialog.isShowing()) {
            return;
        }
        loginDialog = new LoginDialog(LoginActivity.this);
        loginDialog.setTitle("温馨提示");
        loginDialog.setMessage("正在登录");
        loginDialog.setCancelable(false);//设置点击空白处模态框不消失
        loginDialog.show();
    }
    /**
     * 接收蓝牙连接成功返回广播
     */
    private class BluetoothReceiver extends BroadcastReceiver{

        private long interval;
        private long second;
        private Gson gson = new Gson();
        private CommonMessage transfer(String json){
            return gson.fromJson(json,CommonMessage.class);
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            String messageJson = intent.getStringExtra("message");
            CommonMessage commonMessage = transfer(messageJson);
            LogUtil.d("收到广播："+messageJson);
            //计算蓝牙登录时间差
            SimpleDateFormat myFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            TimeZone timeZoneChina = TimeZone.getTimeZone("Asia/Shanghai");//获取中国的时区
            myFmt.setTimeZone(timeZoneChina);//设置系统时区
            try {
                Date startDate =myFmt.parse(nowDate);
                Date endDate = myFmt.parse(MyApplication.getInstance().getUser().getClientTime());
                interval=(endDate.getTime()-startDate.getTime())/1000;//秒
                second=interval%60;//秒
                System.out.println(second);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if(commonMessage.getMsgType()==CommonMessage.LOGIN_SUCCESS_ONLINE &&
                    MyApplication.getInstance().getUser()!=null && second<=6){
                //关闭模态框
                loginDialog.dismiss();
                //登录成功时，执行跳转的逻辑
                loginSuccess();
            }
            if(second>6){
                //提示登录失败
                if (commonDialog != null && commonDialog.isShowing()) {
                    return;
                }
                commonDialog = new CommonDialog(LoginActivity.this);
                commonDialog.setMessage("登录失败");
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() { commonDialog.dismiss();
                    }
                },2000); // 延时2秒
            }else {
                //此处为离线登录，虽然代码很无聊，但是还是要写的
                //离线登录时，执行跳转的逻辑
                loginSuccess();
            }
        }
    }
    //跳转主页面
    private void loginSuccess(){
        //用户已登录，执行跳转主界面逻辑
        LogUtil.d("loginActivity广播接收器收到---跳转");
        Intent skipIntent = new Intent(LoginActivity.this,MainActivity.class); //新建一个跳转到主界面Activity的显式意图
        startActivity(skipIntent); //启动
        LoginActivity.this.finish(); //结束当前Activity
    }

    /**
     * 查询设备信息，包括设备ID与设备名称，传给前端
     */
    private void queryDevInfo() {
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
     * 登录请求
     * @param name
     */
    private void loginExecute(final  String name){
                //获取当前时间
                SimpleDateFormat myFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                TimeZone timeZoneChina = TimeZone.getTimeZone("Asia/Shanghai");//获取中国的时区
                myFmt.setTimeZone(timeZoneChina);//设置系统时区
                nowDate=myFmt.format(new Date());
                //执行业务
                LogUtil.d("蓝牙执行LoginBiz！！！！！！！！");
                int loginResult = LoginBiz.getInstance().loginBiz(name,LoginActivity.this.whoLogin,nowDate);
                LogUtil.d("登陆方法回调的结果：" + loginResult);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        queryDevInfo(); //查询设备信息
        registerBluetoothReceiver();//蓝牙监听广播接收器的注册
        registerNfcReceiver();//nfc标签广播接收器的注册

    }

    /**
     * 当Activity准备好和用户进行交互时，调用onResume()
     * 此时Activity处于【运行状态】
     */
    @Override
    protected void onResume() {
        super.onResume();
    }
    /**
     * 当Activity已经完全不可见时，调用onStop()
     * 此时Activity处于【停止状态】
     */
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(nfcReceiver);
    }
}
