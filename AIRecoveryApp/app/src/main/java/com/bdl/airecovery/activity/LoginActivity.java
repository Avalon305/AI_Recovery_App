package com.bdl.airecovery.activity;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bdl.airecovery.R;
import com.bdl.airecovery.base.BaseActivity;

import org.xutils.DbManager;
import org.xutils.common.util.LogUtil;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.biz.LoginBiz;
import com.bdl.airecovery.bluetooth.CommonCommand;
import com.bdl.airecovery.bluetooth.CommonMessage;
import com.bdl.airecovery.button.NbButton;
import com.bdl.airecovery.contoller.MotorProcess;
import com.bdl.airecovery.dialog.CommonDialog;
import com.bdl.airecovery.dialog.LoginDialog;
import com.bdl.airecovery.dialog.SmallPwdDialog;
import com.bdl.airecovery.entity.Device;
import com.bdl.airecovery.entity.Personal;
import com.bdl.airecovery.entity.Setting;
import com.bdl.airecovery.entity.login.User;
import com.bdl.airecovery.service.BluetoothService;
import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    //private NfcReceiver nfcReceiver;//接收NFC标签信息的广播
    private BluetoothReceiver bluetoothReceiver;//蓝牙广播接收器，监听用户的登录广播
    private String nfcMessage;//NFC标签
    private LoginDialog loginDialog;                  //ShowLogin弹模态框
    //ShowTips弹模态框
    private CommonDialog commonDialog1;
    private CommonDialog commonDialog2;
    private CommonDialog commonDialog3;
    private CommonDialog commonDialog4;
    private CommonDialog commonDialog5;
    private CommonDialog commonDialog6;
    private CommonDialog commonDialog7;
    private CommonDialog commonDialog8;
    private DbManager db = MyApplication.getInstance().getDbManager();
    //第一用户登录指令的变量
    private volatile int whoLogin = 1;
    //设置全局当前时间变量
    private String nowDate;
    //手环mac地址
    private String bind_id;
    //手环id
    private String usbData;
    //设备类型id
    private String deviceIds;
    private String str1;
    //判断该用户是否已经完成该设备（在有该台设备处方的情况下）
    boolean state = false;
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

    @ViewInject(R.id.usb_edittext)
    private EditText usb_edittext;   //usb输入框内容
    //Button
    @ViewInject(R.id.btn_quick_login)
    private NbButton btn_quick_login;


    @ViewInject(R.id.flash)
    private ImageView flash;

    @ViewInject(R.id.update)
    private ImageView update;

    @Event(R.id.start_system_setting)
    private void sysClick(View v) {
        //创建对话框对象的时候对对话框进行监听
        String info = "请输入密码";
        final int[] cnt = {0};
        final boolean[] flag = {false};
        final SmallPwdDialog dialog = new SmallPwdDialog(LoginActivity.this, info, R.style.CustomDialog,
                new SmallPwdDialog.DataBackListener() {
                    @Override
                    public void getData(String data) {
                        String result = data;
                        if (result.equals(MyApplication.ADMIN_PASSWORD)) {
                            flag[0] = true;
                        } else {
                            flag[0] = false;
                        }
                        //Log.d(LocationActivity.ACTIVITY_TAG, "result:"+result+"  ADMIN_PWD:"+MyApplication.ADMIN_PASSWORD);
                        //Log.d(LocationActivity.ACTIVITY_TAG, "result:"+flag[0]);
                        if (flag[0]) {
                            startActivity(new Intent(LoginActivity.this, SystemSettingActivity.class));
                        } else if (cnt[0] != 0) {
                            Toast.makeText(LoginActivity.this, "密码错误请重试!", Toast.LENGTH_SHORT).show();
                        }
                        cnt[0]++;
                    }
                });

        dialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.y = 100;
        dialog.getWindow().setGravity(Gravity.TOP);
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
        dialog.show();
    }
    private void setBtnQuickLoginOnClick() {
        btn_quick_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_quick_login.startAnim(flash, update);
                ObjectAnimator objectAnimator2;
                objectAnimator2 = ObjectAnimator.ofFloat(update, "rotation", 360f, 0f);
                objectAnimator2.setDuration(2000);
                objectAnimator2.setInterpolator(new OvershootInterpolator());
                objectAnimator2.start();
                final Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        User user = new User();
                        //初始化待训练设备
                        String str1 = "[P00,P01,P02,P03,P04,P05,P06,P07,P08,P09]";
                        user.setUserId("体验者");
                        user.setDeviceTypearrList(str1);
                        user.setUsername("体验者");
                        user.setExisitSetting(false);
                        user.setMoveWay(0);
                        user.setGroupCount(5);
                        user.setGroupNum(2);
                        user.setRelaxTime(10);
                        user.setSpeedRank(1);
                        user.setAge(30);
                        user.setWeight(60);
                        user.setHeartRatemMax(190);
                        user.setTrainMode("康复模式");
                        if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("坐式划船机")) {
                            user.setForwardLimit(130);
                            user.setBackLimit(50);
                            user.setSeatHeight(0);
                            user.setConsequentForce(25);
                            user.setReverseForce(25);
                        } else if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("坐式推胸机")) {
                            user.setForwardLimit(130);
                            user.setBackLimit(50);
                            user.setSeatHeight(0);
                            user.setConsequentForce(25);
                            user.setReverseForce(25);
                        } else if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("腿部推蹬机")) {
                            user.setForwardLimit(130);
                            user.setBackLimit(50);
                            user.setConsequentForce(25);
                            user.setReverseForce(25);
                        } else if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("腹肌训练机")) {
                            user.setForwardLimit(130);
                            user.setBackLimit(50);
                            user.setConsequentForce(25);
                            user.setReverseForce(25);
                            user.setLeverAngle(0);
                        } else if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("三头肌训练机")) {
                            user.setForwardLimit(130);
                            user.setBackLimit(50);
                            user.setConsequentForce(25);
                            user.setReverseForce(25);
                        } else if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("腿部外弯机")) {
                            user.setForwardLimit(130);
                            user.setBackLimit(50);
                            user.setConsequentForce(25);
                            user.setReverseForce(25);
                        } else if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("腿部内弯机")) {
                            user.setBackLimit(50);
                            user.setConsequentForce(25);
                            user.setReverseForce(25);
                        } else if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("蝴蝶机")) {
                            user.setBackLimit(50);
                            user.setConsequentForce(25);
                            user.setReverseForce(25);
                        } else if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("反向蝴蝶机")) {
                            user.setBackLimit(50);
                            user.setConsequentForce(25);
                            user.setReverseForce(25);
                        } else if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("坐式背部伸展机")) {
                            user.setForwardLimit(130);
                            user.setBackLimit(50);
                            user.setConsequentForce(25);
                            user.setReverseForce(25);
                            user.setLeverAngle(0);
                        }
                        MyApplication.getInstance().setUser(user);
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);//新建一个跳转到主界面Activity的显式意图
                        startActivity(intent); //启动
                        LoginActivity.this.finish(); //结束当前Activity
                    }


                }, 2100);

            }
        });


    }
    /**
     * NFC数据获取
     */
    public void nfcService() {
        usb_edittext = (EditText) findViewById(R.id.usb_edittext);
        usb_edittext.setInputType(InputType.TYPE_NULL);//禁止软键盘弹出
        usb_edittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (usb_edittext.length() == 16) {
                    showLogin();//弹出登录模态框
                    loginExecute(readerConvertIntoBindId(usb_edittext.getText().toString()));//请求登录
                    startBluetooth(readerConvertIntoMAC(usb_edittext.getText().toString()));//开启蓝牙扫描
                    usb_edittext.setText(null);
                }
            }
        });
    }

    /**
     * 将NFC标签转换成Mac地址
     *
     * @param readContent
     */
    private String readerConvertIntoMAC(String readContent) {
        Log.d("readerINfo", readContent);
        char[] chars = readContent.toUpperCase().toCharArray();
        StringBuilder BindId = new StringBuilder();
        BindId.append("D1:");
        BindId.append(chars[12]);
        BindId.append(chars[13] + ":");
        BindId.append(chars[10]);
        BindId.append(chars[11] + ":");
        BindId.append(chars[8]);
        BindId.append(chars[9] + ":");
        BindId.append(chars[6]);
        BindId.append(chars[7] + ":");
        BindId.append(chars[4]);
        BindId.append(chars[5]);
        bind_id = BindId.toString();
        Log.d("Mac", bind_id);
        return bind_id;

    }

    /**
     * 将NFC标签转换成BindId
     * @param readContent
     * @return
     */
    private String readerConvertIntoBindId(String readContent) {
        Log.d("readerINfo", readContent);
        char[] chars = readContent.toUpperCase().toCharArray();
        StringBuilder BindId = new StringBuilder();
        BindId.append(chars[12]);
        BindId.append(chars[13]);
        BindId.append(chars[10]);
        BindId.append(chars[11]);
        BindId.append(chars[8]);
        BindId.append(chars[9]);
        BindId.append(chars[6]);
        BindId.append(chars[7]);
        BindId.append(chars[4]);
        BindId.append(chars[5]);
        usbData = BindId.toString();
        Log.d("bind_id", usbData);
        return usbData;
    }

    /**
     * 启动蓝牙扫描
     */
    private void startBluetooth(String bind_id) {
        //启动蓝牙扫描
        Intent intent = new Intent(this, BluetoothService.class);
        intent.putExtra("command", CommonCommand.LOGIN.value());
        intent.putExtra("message", bind_id);
        startService(intent);
        LogUtil.d("发出了启动蓝牙扫描的命令");
    }

    /**
     * 蓝牙返回信息的注册
     */
    private void registerBluetoothReceiver() {
        //注册蓝牙广播监听器
        bluetoothReceiver = new BluetoothReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.bdl.bluetoothmessage");
        registerReceiver(bluetoothReceiver, intentFilter);
    }

    /**
     * 用于界面提示，在用户连接蓝牙的时间段里，弹出正在登录模态框
     */
    private void showLogin() {
        //弹出对话框之前，先检查当前界面是否存在对话框，如果存在，先关闭，在执行下方的逻辑
        loginDialog = new LoginDialog(LoginActivity.this);
        loginDialog.setTitle("温馨提示");
        loginDialog.setMessage("正在登录");
        //loginDialog.setCancelable(false);//设置点击空白处模态框不消失
        loginDialog.show();
    }

    //关闭蓝牙连接
    private void closeBluetooth() {
        Intent intentLog = new Intent(LoginActivity.this, BluetoothService.class);
        intentLog.putExtra("command", CommonCommand.LOGOUT.value());
        startService(intentLog);
        LogUtil.e("蓝牙用户退出");
    }
    /**
     * 截取并拼接设备id
     * 截取在线登录的设备号
     */
//    private void equipment(){
//        int deviceId = Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getDeviceInnerID());
//        deviceIds = "P0" + String.valueOf(deviceId);
//        if(MyApplication.getInstance().getUser().getDeviceTypearrList() == "[]"){
//            state = false;
//        }else {
//            String str = MyApplication.getInstance().getUser().getDeviceTypearrList();
//            LogUtil.d("设备类型："+str);
//            //将字符串数组转换成数组
//            String sTemp = str.substring(1, str.length()-1);
//            String[] sArray = sTemp.split(",");
//            for(int i=0;i<sArray.length;i++){
//                if(deviceIds.equals(sArray[i])){
//                    state = true;
//                }else{
//                    state = false;
//                }
//            }
//        }
//        if(MyApplication.getInstance().getUser().getDeviceTypearrList() == "[]") {
//            str1 = "";
//        }else{
//            String str = MyApplication.getInstance().getUser().getDeviceTypearrList();
//            LogUtil.d("设备类型：" + str);
//            str1 = str.substring(1, 4);
//        }
//    }


    /**
     * 接收蓝牙连接成功返回广播
     */
    private class BluetoothReceiver extends BroadcastReceiver {
        private Gson gson = new Gson();
        private CommonMessage transfer(String json) {
            return gson.fromJson(json, CommonMessage.class);
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            if(MyApplication.getInstance().getUser() == null){
                LogUtil.d("用户值为空");
            }else {
                //equipment();
                int test = MyApplication.getInstance().getUser().getInfoResponse();
                LogUtil.d("登录状态："+test);
//                LogUtil.d("当前设备类型："+deviceIds);
//                LogUtil.d("登录之后设备类型："+str1);
            }
            String messageJson = intent.getStringExtra("message");
            CommonMessage commonMessage = transfer(messageJson);
            LogUtil.d("接收到登录信息：" + commonMessage);
            //1. 蓝牙登陆 2. 联通教练机 3. 教练机有该用户 4. 该用户有处方 5. 处方有该设备 6. 该设备未完成
            if (commonMessage.getMsgType() == CommonMessage.CONNECT_SUCCESS &&
                    MyApplication.getInstance().getUser() != null &&
                    MyApplication.getInstance().getUser().getDpStatus() == 2) {
                //关闭loginDialog.dismiss();模态框
                try {
                    loginDialog.dismiss();
                }catch(Exception e){
                    e.printStackTrace();
                }
                unregisterReceiver(bluetoothReceiver);//同上
                MyApplication.getInstance().getUser().setBindId(usbData);
                //登录成功时，执行跳转的逻辑
                loginSuccess();
            }
            //1. 蓝牙登陆 2. 联通教练机 3. 教练机有该用户 4. 该用户有处方 5. 处方有该设备 6.该设备还没做
            else if (commonMessage.getMsgType() == CommonMessage.CONNECT_SUCCESS &&
                    MyApplication.getInstance().getUser() != null &&
                    MyApplication.getInstance().getUser().getInfoResponse() == 7) {
                //关闭模态框
                try {
                    loginDialog.dismiss();
                }catch(Exception e){
                    e.printStackTrace();
                }
                unregisterReceiver(bluetoothReceiver);//同上
                MyApplication.getInstance().getUser().setBindId(usbData);
                //登录成功时，执行跳转的逻辑
                loginSuccess();
            }
            //蓝牙连接成功，教练机连接失败
            else if (commonMessage.getMsgType() == CommonMessage.CONNECT_SUCCESS &&
                    MyApplication.getInstance().getUser() == null) {
                //关闭模态框
                try {
                    loginDialog.dismiss();
                }catch(Exception e){
                    e.printStackTrace();
                }
                unregisterReceiver(bluetoothReceiver);//同上
                //初始化设备
                OfflineLogin();
                //此时为离线登录
                loginSuccess();
            }
            //1. 蓝牙登陆 2. 联通教练机 3. 教练机有该用户 4. 该用户有处方 5. 处方有该设备 6. 该设备已完成
            else if (commonMessage.getMsgType() == CommonMessage.CONNECT_SUCCESS &&
                    MyApplication.getInstance().getUser() != null &&
                    MyApplication.getInstance().getUser().getInfoResponse() == 6) {
                //提示训练已经完成
                //关闭模态框
                try {
                    loginDialog.dismiss();
                }catch(Exception e){
                    e.printStackTrace();
                }
                commonDialog1();
            }
            //1. 蓝牙登陆 2. 联通教练机 3. 教练机有该用户 4. 该用户有处方 5. 处方无该设备
            else if (commonMessage.getMsgType() == CommonMessage.CONNECT_SUCCESS &&
                    MyApplication.getInstance().getUser() != null &&
                    MyApplication.getInstance().getUser().getInfoResponse() == 5) {
                //关闭登录框
                try {
                    loginDialog.dismiss();
                }catch(Exception e){
                    e.printStackTrace();
                }
                commonDialog2();
            }
            //处方已经做完
            else if(commonMessage.getMsgType() == CommonMessage.CONNECT_SUCCESS &&
                    MyApplication.getInstance().getUser() != null &&
                    MyApplication.getInstance().getUser().getInfoResponse() == 3){
                //关闭模态框
                try {
                    loginDialog.dismiss();
                }catch(Exception e){
                    e.printStackTrace();
                }
                commonDialog7();
            }
            //处方已经废弃
            else if(commonMessage.getMsgType() == CommonMessage.CONNECT_SUCCESS &&
                    MyApplication.getInstance().getUser() != null &&
                    MyApplication.getInstance().getUser().getInfoResponse() == 4 ){
                //关闭模态框
                try {
                    loginDialog.dismiss();
                }catch(Exception e){
                    e.printStackTrace();
                }
                commonDialog8();
            }
            //1. 蓝牙登陆 2. 联通教练机 3. 教练机有该用户 4. 该用户无处方
            else if (commonMessage.getMsgType() == CommonMessage.CONNECT_SUCCESS &&
                    MyApplication.getInstance().getUser() != null &&
                    MyApplication.getInstance().getUser().getInfoResponse() == 1) {
                //关闭模态框
                try {
                    loginDialog.dismiss();
                }catch(Exception e){
                    e.printStackTrace();
                }
                commonDialog3();
            } else if (commonMessage.getMsgType() == CommonMessage.DISCONNECTED) {
                //关闭模态框
                try {
                    loginDialog.dismiss();
                }catch(Exception e){
                    e.printStackTrace();
                }
                //提示蓝牙连接失败
                commonDialog4();
            } else if (MyApplication.getInstance().getUser().getInfoResponse() == 0) {
                //关闭模态框
                try {
                    loginDialog.dismiss();
                }catch(Exception e){
                    e.printStackTrace();
                }
                //提示无该用户
                commonDialog5();
            }else {
                //关闭模态框
                try {
                    loginDialog.dismiss();
                }catch(Exception e){
                    e.printStackTrace();
                }
                //提示登录失败
                commonDialog6();
            }
        }
    }

    private void commonDialog1() {
        unregisterReceiver(bluetoothReceiver);//同上
        //关闭蓝牙
        closeBluetooth();
        commonDialog1 = new CommonDialog(LoginActivity.this);
        commonDialog1.setTitle("温馨提示");
        commonDialog1.setMessage("您以完成本设备训练，请到下一设备训练！");
        commonDialog1.setPositiveBtnText("我知道了");
        commonDialog1.setOnPositiveClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commonDialog1.dismiss();
                //置空用户
                MyApplication.getInstance().setUser(null);
                //刷新页面
                refresh();
            }
        });
        commonDialog1.show();
    }

    private void commonDialog2() {
        unregisterReceiver(bluetoothReceiver);//同上
        //关闭蓝牙
        closeBluetooth();
        commonDialog2 = new CommonDialog(LoginActivity.this);
        commonDialog2.setTitle("温馨提示");
        commonDialog2.setMessage("您没有本设备处方，建议您去教练机设置处方");
        commonDialog2.setPositiveBtnText("我知道了");
        commonDialog2.setOnPositiveClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commonDialog2.dismiss();
                //置空用户
                MyApplication.getInstance().setUser(null);
                //刷新页面
                refresh();
            }
        });
        commonDialog2.show();
    }

    private void commonDialog3() {
        unregisterReceiver(bluetoothReceiver);//同上
        //关闭蓝牙
        closeBluetooth();
        commonDialog3 = new CommonDialog(LoginActivity.this);
        commonDialog3.setTitle("温馨提示");
        commonDialog3.setMessage("您没有处方，建议您去教练机设置处方");
        commonDialog3.setPositiveBtnText("我知道了");
        commonDialog3.setOnPositiveClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commonDialog3.dismiss();
                //置空用户
                MyApplication.getInstance().setUser(null);
                //刷新页面
                refresh();
            }
        });
        commonDialog3.show();
    }

    private void commonDialog4() {
        unregisterReceiver(bluetoothReceiver);//同上
        //关闭蓝牙
        closeBluetooth();
        commonDialog4 = new CommonDialog(LoginActivity.this);
        commonDialog4.setMessage("蓝牙连接失败");
        commonDialog4.setPositiveBtnText("我知道了");
        commonDialog4.setOnPositiveClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commonDialog4.dismiss();
                //置空用户
                MyApplication.getInstance().setUser(null);
                //刷新页面
                refresh();
            }
        });
        commonDialog4.show();
    }
    private void commonDialog5() {
        unregisterReceiver(bluetoothReceiver);//同上
        //关闭蓝牙
        closeBluetooth();
        commonDialog5 = new CommonDialog(LoginActivity.this);
        commonDialog5.setMessage("蓝牙手环未绑定");
        commonDialog5.setPositiveBtnText("我知道了");
        commonDialog5.setOnPositiveClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commonDialog5.dismiss();
                //置空用户
                MyApplication.getInstance().setUser(null);
                //刷新页面
                refresh();
            }
        });
        commonDialog5.show();
    }
    private void commonDialog6() {
        unregisterReceiver(bluetoothReceiver);//同上
        //关闭蓝牙
        closeBluetooth();
        commonDialog6 = new CommonDialog(LoginActivity.this);
        commonDialog6.setMessage("登录失败");
        commonDialog6.setPositiveBtnText("我知道了");
        commonDialog6.setOnPositiveClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commonDialog6.dismiss();
                //置空用户
                MyApplication.getInstance().setUser(null);
                //刷新页面
                refresh();
            }
        });
        commonDialog6.show();
    }
    private void commonDialog7() {
        unregisterReceiver(bluetoothReceiver);//同上
        //关闭蓝牙
        closeBluetooth();
        commonDialog7 = new CommonDialog(LoginActivity.this);
        commonDialog7.setMessage("该大处方已经做完，请重新下处方");
        commonDialog7.setPositiveBtnText("我知道了");
        commonDialog7.setOnPositiveClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commonDialog7.dismiss();
                //置空用户
                MyApplication.getInstance().setUser(null);
                //刷新页面
                refresh();
            }
        });
        commonDialog7.show();
    }
    private void commonDialog8() {
        unregisterReceiver(bluetoothReceiver);//同上
        //关闭蓝牙
        closeBluetooth();
        commonDialog8 = new CommonDialog(LoginActivity.this);
        commonDialog8.setMessage("该处方已经废弃");
        commonDialog8.setPositiveBtnText("我知道了");
        commonDialog8.setOnPositiveClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commonDialog6.dismiss();
                //置空用户
                MyApplication.getInstance().setUser(null);
                //刷新页面
                refresh();
            }
        });
        commonDialog8.show();
    }

    //刷新页面
    public void refresh() {
        onCreate(null);
    }

    /**
     * 离线用户全局
     */
    private void OfflineLogin() {
        User user = new User();
        //初始化待训练设备
        String str1 = "[P00,P01,P02,P03,P04,P05,P06,P07,P08,P09]";
        user.setUserId("离线用户");
        user.setDeviceTypearrList(str1);
        user.setUsername("离线用户");
        user.setExisitSetting(false);
        user.setMoveWay(0);
        user.setGroupCount(3);
        user.setGroupNum(3);
        user.setRelaxTime(10);
        user.setSpeedRank(1);
        user.setAge(30);
        user.setWeight(60);
        user.setHeartRatemMax(190);
        user.setTrainMode("康复模式");
        user.setBindId(usbData);
        user.setDpId(0);
        if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("坐式划船机")) {
            user.setForwardLimit(130);
            user.setBackLimit(50);
            user.setSeatHeight(0);
            user.setConsequentForce(25);
            user.setReverseForce(25);
        } else if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("坐式推胸机")) {
            user.setForwardLimit(130);
            user.setBackLimit(50);
            user.setSeatHeight(0);
            user.setConsequentForce(25);
            user.setReverseForce(25);
        } else if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("腿部推蹬机")) {
            user.setForwardLimit(130);
            user.setBackLimit(50);
            user.setConsequentForce(25);
            user.setReverseForce(25);
        } else if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("腹肌训练机")) {
            user.setForwardLimit(130);
            user.setBackLimit(50);
            user.setConsequentForce(25);
            user.setReverseForce(25);
            user.setLeverAngle(0);
        } else if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("三头肌训练机")) {
            user.setForwardLimit(130);
            user.setBackLimit(50);
            user.setConsequentForce(25);
            user.setReverseForce(25);
        } else if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("腿部外弯机")) {
            user.setForwardLimit(130);
            user.setBackLimit(50);
            user.setConsequentForce(25);
            user.setReverseForce(25);
        } else if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("腿部内弯机")) {
            user.setBackLimit(50);
            user.setConsequentForce(25);
            user.setReverseForce(25);
        } else if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("蝴蝶机")) {
            user.setBackLimit(50);
            user.setConsequentForce(25);
            user.setReverseForce(25);
        } else if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("反向蝴蝶机")) {
            user.setBackLimit(50);
            user.setConsequentForce(25);
            user.setReverseForce(25);
        } else if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("坐式背部伸展机")) {
            user.setForwardLimit(130);
            user.setBackLimit(50);
            user.setConsequentForce(25);
            user.setReverseForce(25);
            user.setLeverAngle(0);
        }
        MyApplication.getInstance().setUser(user);
    }

    //跳转主页面
    private void loginSuccess() {
        //用户已登录，执行跳转主界面逻辑
        LogUtil.d("loginActivity广播接收器收到---跳转");
        Intent skipIntent = new Intent(LoginActivity.this, MainActivity.class); //新建一个跳转到主界面Activity的显式意图
        startActivity(skipIntent); //启动
        LoginActivity.this.finish(); //结束当前Activity
    }
    /**
     * 转换设备id
     */
//    private void transformation(){
//        switch (MyApplication.getInstance().getCurrentDevice().getDeviceInnerID()){
//
//        }
//    }
    /**
     * 查询设备信息，包括设备ID与设备名称，传给前端
     */
    private void queryDevInfo() {
        //判断是否获取到设备信息
        if (MyApplication.getInstance().getCurrentDevice().getDeviceInnerID() != null && !MyApplication.getInstance().getCurrentDevice().getDeviceInnerID().equals("")) {
            int deviceId = Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getDeviceInnerID());
            deviceId++;
            tv_dev_id.setText("BodyStrong " + String.valueOf(deviceId));
        }
        if (MyApplication.getInstance().getCurrentDevice().getDisplayName() != null && !MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("")) {
            //获取设备名称
            if (MyApplication.getInstance().getCurrentDevice().getDisplayName().equals("躯干扭转组合")) {
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
        if (MyApplication.getInstance().getCurrentDevice().getMuscleImg() != null && !MyApplication.getInstance().getCurrentDevice().getMuscleImg().equals("")) {
            //获取肌肉图（需要根据String找到资源文件中对应的ID）
            iv_muscle_image.setImageResource(getResources().getIdentifier(MyApplication.getInstance().getCurrentDevice().getMuscleImg(), "drawable", getPackageName()));
        }
    }

    /**
     * 登录请求
     *
     * @param name
     */
    private void loginExecute(final String name) {
        //获取当前时间
        SimpleDateFormat myFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        TimeZone timeZoneChina = TimeZone.getTimeZone("Asia/Shanghai");//获取中国的时区
        myFmt.setTimeZone(timeZoneChina);//设置系统时区
        nowDate = myFmt.format(new Date());
        //执行业务

        LogUtil.d("蓝牙执行LoginBiz！！！！！！！！");
        int loginResult = LoginBiz.getInstance().loginBiz(name, LoginActivity.this.whoLogin, nowDate);
        LogUtil.d("登陆方法回调的结果：" + loginResult);
    }

    /**
     * 设置快速登录按钮是否可见
     */
    private void isBtnVisible() {
        Setting setting = new Setting();
        try {
            setting = db.findFirst(Setting.class);
        } catch (DbException e) {
            e.printStackTrace();
        }
        if (setting.getCanQuickLogin()) {
            btn_quick_login.setVisibility(View.VISIBLE);
        } else {
            btn_quick_login.setVisibility(View.INVISIBLE);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MotorProcess.motorInitialization();
        setBtnQuickLoginOnClick();
        queryDevInfo(); //查询设备信息
        registerBluetoothReceiver();//蓝牙监听广播接收器的注册
        //registerNfcReceiver();//nfc标签广播接收器的注册
        isBtnVisible();     //是否显示快速登录按钮
        nfcService();//nfc数据获取
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
        //unregisterReceiver(nfcReceiver);//注册广播解除
        //unregisterReceiver(bluetoothReceiver);//同上
    }



    /**
     * NFC标签广播接受的注册
     */
//    private void registerNfcReceiver() {
//        //注册登录广播监听器
//        nfcReceiver = new NfcReceiver();
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction("com.bdl.airecovery.service.UsbService");
//        registerReceiver(nfcReceiver,intentFilter);
//    }
//    /**
//     * 接收NFC标签广播
//     */
//    private class NfcReceiver extends BroadcastReceiver{
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            nfcMessage=intent.getStringExtra("bind_id");
//            showLogin();//弹出登录模态框
//            loginExecute(nfcMessage);//请求登录
//            startBluetooth();//开启蓝牙扫描
//        }
//    }
}
