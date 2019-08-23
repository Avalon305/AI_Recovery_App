package com.bdl.airecovery.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.biz.LoginBiz;
import com.bdl.airecovery.bluetooth.CommonCommand;
import com.bdl.airecovery.bluetooth.CommonMessage;
import com.google.gson.Gson;
import com.zero.serialport.framework.callback.ZeroCallback;
import com.zero.serialport.framework.data.ZeroData;
import com.zero.serialport.framework.decoder.DefaultDecoder;
import com.zero.serialport.framework.port.ZeroPort;

import org.xutils.common.util.LogUtil;
import org.xutils.x;

/**
 * @explain  使用自定义框架封装
 * @author zero.
 * @creat time 2019/4/15 10:40 AM.
 */

public class CardReaderService extends Service {


    private ZeroPort cardPortOne = null;
    private ZeroPort cardPortTwo = null;

    private volatile boolean isAccepted= false;

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d("service init...");
        initPort();
    }

    private void initPort() {
        try {
            cardPortOne = new ZeroPort(new ZeroCallback<ZeroData>() {
                @Override
                public void receive(ZeroData data) {
                    if(isAccepted){
                        LogUtil.d("service回调函数接收："+data.toString());
                        loginExecute((String) data.getDataBody());
                    }
                }
            },new DefaultDecoder(),80,"/dev/ttyO4",115200);
            cardPortTwo = new ZeroPort(new ZeroCallback<ZeroData>() {
                @Override
                public void receive(ZeroData data) {
                    if(isAccepted){
                        LogUtil.d(data.toString());
                        loginExecute((String) data.getDataBody());
                    }
                }
            },new DefaultDecoder(),80,"/dev/ttyO5",115200);
        }
        catch (SecurityException e) {
            //串口设置有安全问题
            e.printStackTrace();
        }
        catch (Exception e) {
            //其他问题
            e.printStackTrace();
        }
        //cardPortOne.start();
        //暂时未定义接口
        //cardPortTwo.start();
    }

    //第一用户还是第二用户登录指令的变量
    private volatile int whoLogin = 0;
    //第一用户还是第二用户登录指令的变量
    private volatile int whoLogout = 0;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.d("收到了command");
        String command = intent.getStringExtra("command");
        //初始化的时候，肯定是不带指令的，所以直接return；
        if (command == null) return super.onStartCommand(intent, flags, startId);
        LogUtil.d("command ！= null ");
        CommonCommand commonCommand = CommonCommand.getEnumByString(command);
        switch (commonCommand){
            case LOGIN:  whoLogin = 1;
                //设置为开始接受
                isAccepted = true;
                startRead();
                break;
            case LOGOUT: whoLogout = 1;
                isAccepted = false;
                logout();
                //下线所有设备
                break;
        }
        return super.onStartCommand(intent, flags, startId);
    }
    //下线
    private void logout() {
        if (whoLogout == 1){
            MyApplication.getInstance().setUser(null);
            sendBroadcastMsg(CommonMessage.LOGOUT);
        }
    }

    private void loginExecute(final  String name){
        x.task().run(new Runnable() {
            @Override
            public void run() {
                //执行业务
                LogUtil.d("发卡器运行LoginBiz！！！！！！！");
                int loginResult = LoginBiz.getInstance().loginBiz(name,CardReaderService.this.whoLogin,null);
                LogUtil.d("登陆方法回调的结果：" + loginResult);

                //如果其他人已经做过了，就不需要接收了
                if (loginResult == 0){
                    isAccepted = false;
                    return;
                }
                //针对第一用户的 ：有医护设置（在线，离线） 无医护设置（在线，离线） 的情况 设置为蓝牙登陆
                if (loginResult == CommonMessage.LOGIN_SUCCESS_ONLINE ||
                        loginResult == CommonMessage.LOGIN_REGISTER_ONLINE ||
                        loginResult == CommonMessage.LOGIN_SUCCESS_OFFLINE ||
                        loginResult == CommonMessage.LOGIN_REGISTER_OFFLINE){
                    //MyApplication.getInstance().getUser().setType("serialport");
                    isAccepted = false;
                    //先发出广播让界面登录，后台慢慢连接蓝牙
                    sendBroadcastMsg(loginResult);
                }
                else{
                    //对上面未考虑到的情况做广播。
                    sendBroadcastMsg(loginResult);
                }

            }
        });
    }
    private static Gson gsonUtil = new Gson();
    /**
     * 发送广播的快捷方法
     */
    private void sendBroadcastMsg(int msgType) {
        //蓝牙登录成功之后，发出登录结果广播的逻辑。
        Intent intentLogin = new Intent("com.bdl.bluetoothmessage");
        intentLogin.putExtra("message",gsonUtil.toJson(new CommonMessage(msgType,null)));
        getApplicationContext().sendBroadcast(intentLogin);
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cardPortOne.end();
    }
    /**
     * @explain 统一的启动接口
     * @author zero.
     * @creat time 2019/6/24 9:52 PM.
     */

    public void startRead(){
        if (cardPortOne != null) {
            cardPortOne.start();
        }
        if (cardPortTwo != null) {
            cardPortTwo.start();
        }
    }
    /**
     * @explain  统一的关闭接口
     * @author zero.
     * @creat time 2019/6/24 9:53 PM.
     */

    public void endRead(){
        if (cardPortOne != null) {
            cardPortOne.end();
        }
        if (cardPortTwo != null) {
            cardPortTwo.end();
        }
    }
}
