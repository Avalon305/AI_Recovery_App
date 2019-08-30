package com.bdl.airecovery.service;

import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.activity.LoginActivity;
import com.bdl.airecovery.bluetooth.CommonCommand;
import com.bdl.airecovery.bluetooth.CommonMessage;
import com.bdl.airecovery.bluetooth.MyBluetoothGattCharacteristic;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleScanState;
import com.clj.fastble.exception.BleException;
import com.google.gson.Gson;
import org.xutils.common.util.LogUtil;
import org.xutils.x;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

public class BluetoothService extends Service {

    //蓝牙是否打开并且可用的标志。默认不可用登陆方法回调的结果
    private volatile boolean isBluetoothUseful = false;
    //标记蓝牙当前的状态
    private volatile Status status = Status.NORMAL;

    //蓝牙状态
    private enum Status{
        NORMAL(1), //正常(未扫描、未连接的状态)
        SCANNING(2), //扫描(扫描附近可连接的、最近的设备的状态)
        TRY_CONNECTING(3),// 尝试连接(已扫描到当前设备，不再扫描其他设备，尝试连接当前设备的状态)
        CONNECTING(4); //连接中(已连接的状态)

        private int value = 0;

        private Status(int value) {     //必须是private的，否则编译错误
            this.value = value;
        }

        public static Status valueOf(int value) {    //手写的从int到enum的转换函数
            switch (value) {
                case 1:
                    return NORMAL;
                case 2:
                    return SCANNING;
                case 3:
                    return CONNECTING;
                case 4:
                    return TRY_CONNECTING;
                default:
                    return null;
            }
        }

        public int value() {
            return this.value;
        }
    }

    /**
     * 当第一次调用startService时会执行
     */
    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d("BluetoothService初始化...");
        initBluetooth();
    }

    /**
     * 初始化蓝牙模块
     * 检查并启动蓝牙模块，达到可以接受请求的状态。
     */
    private void initBluetooth() {
        if (BleManager.getInstance().isSupportBle()){
            LogUtil.d("设备支持低功耗蓝牙");
            isBluetoothUseful = true;
        }else{
            isBluetoothUseful = false;
            LogUtil.d("设备不支持蓝牙");
        }
        //查看本机器蓝牙的状态
        boolean result = BleManager.getInstance().isBlueEnable();
        if (result){
            LogUtil.d("bluetooth has started");
        }else{
            BleManager.getInstance().enableBluetooth();
            LogUtil.d("bluetooth is started");
        }
    }

    /**
     * 请求接收的处理
     * 每次调用startService时都会执行
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.d("收到了command");

        //不支持BLE蓝牙的设备，拒绝执行任何命令！
        if (!isBluetoothUseful){
            return super.onStartCommand(intent, flags, startId);
        }

        String command = intent.getStringExtra("command");

        //初始化的时候，肯定是不带指令的，所以直接return
        if (command == null) {
            return super.onStartCommand(intent, flags, startId);
        }

        CommonCommand commonCommand = CommonCommand.getEnumByString(command);
        switch (commonCommand){
            case LOGIN: //开启蓝牙扫描，自动连接附近设备。若成功会发送提示广播，并连续广播心率。
                String nfcMessage = intent.getStringExtra("message");
                //scanDevice();
                this.status = Status.TRY_CONNECTING;
                connectMAC(nfcMessage);
                break;
            case LOGOUT: //断开蓝牙连接
                disConnect();
                break;
            default:
                LogUtil.d("接收到未知command");
                break;
        }
        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    private static Gson gsonUtil = new Gson();

    private final Handler BTHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1: //扫描到合法设备，尝试连接
                    BleDevice bleDevice = (BleDevice)msg.obj;
                    if(bleDevice == null) break;
                    BluetoothService.this.status = Status.TRY_CONNECTING;
                    tryConnectMAC(bleDevice.getMac());
            }
        }
    };

    /**
     * 根据mac地址尝试连接
     * @param mac
     */
    private void tryConnectMAC(String mac) {
        //如果不是尝试连接状态一律return，包括scanning，nomal，connecting，
        if (status != Status.TRY_CONNECTING) {
            return;
        }
        //直接连接mac地址
        connectMAC(mac);
    }

    /**
     * 根据mac地址直接精准连接
     * @param mac
     */
    public void connectMAC(final String mac){

        BleManager.getInstance().connect(mac, new BleGattCallback() {

            private AtomicInteger tryConnect = new AtomicInteger(0);
            private AtomicInteger reConnect = new AtomicInteger(0);
            @Override
            public void onStartConnect() {
                LogUtil.d("mac开始连接！");
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                LogUtil.d("mac连接失败!");
                disConnect();
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                //当不是尝试连接状态的时候，就不用连接了，这种情况是连接中途发来了指令
                if (BluetoothService.this.status != Status.TRY_CONNECTING){
                    //把尝试次数归零
                    reConnect.set(0);
                    tryConnect.set(0);
                    //断开应该断开的连接
                    BleManager.getInstance().disconnectAllDevice();
                    return;
                } else {
                    BluetoothService.this.status = Status.CONNECTING;
                    LogUtil.d("mac连接成功了！");
                    //把尝试次数归零
                    reConnect.set(0);
                    tryConnect.set(0);

                    //发送蓝牙连接成功的广播
                    int msgType = CommonMessage.CONNECT_SUCCESS;
                    sendBroadcastMsg(msgType);

                    //下面的顺序千万不能调换，改状态，监听心率
                    heartBeatNotify(bleDevice);
                }
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                LogUtil.d("mac断开了连接！");

                //不在连接状态了，就不应该重连，应该彻底断开
                if(BluetoothService.this.status != Status.CONNECTING){
                    //把尝试次数归零
                    reConnect.set(0);
                    tryConnect.set(0);
                    //断开应该断开的连接
                    BleManager.getInstance().disconnectAllDevice();
                    return;
                }
                //不正常下线，就是掉线，掉线之后，尝试重新连接2次，还连不上则断开连接
                if(reConnect.get() >= 2){
                    disConnect();
                    reConnect.set(0);
                } else {
                    //尝试次数加一。
                    reConnect.incrementAndGet();
                    connectMAC(mac);
                }
            }

        });
    }

    /**
     * 扫描附近的设备列表，x秒一个扫描周期，最后会放在list里面。选出其中名称合法的，并且强度最大，
     * 并且该强度满足距离要求的设备，调用连接方法mac开始连接！
     */
    private synchronized void scanDevice(){
        //如果蓝牙模块在扫描状态，则返回，否则设置为Normal
        if(BleManager.getInstance().getScanSate()== BleScanState.STATE_SCANNING){
            return;
        } else {
            //如果代码逻辑在扫描状态，但是蓝牙模块在空闲状态，则纠正蓝牙service为nomal状态。
            if(status == Status.SCANNING){
                status = Status.NORMAL;
            }
        }

        //1. 针对跳到待机页面，蓝牙还连接着的情况的处理办法。
        //2. 正常情况下，在已经连接了蓝牙的情形中，在发出扫描请求，拒绝是对的。
        //3. 有一种情况，登陆界面扫描到之后，尝试连接，然后又来了一次断开连接，
        if (status == Status.CONNECTING) {
            return;
        }
        //尝试连接状态，当然也不执行了
        if (status == Status.TRY_CONNECTING) {
            return;
        }

        //开启蓝牙模块扫描功能
        BleManager.getInstance().scan(new BleScanCallback() {
            /**
             * 会回到主线程，参数表示本次扫描动作是否开启成功。由于蓝牙没有打开，上一次扫描没有结束等原因，
             * 会造成扫描开启失败。
             */
            @Override
            public void onScanStarted(boolean success) {
                //设置为扫描状态
                status = Status.SCANNING;
                LogUtil.d("开始扫描...");
            }
            /**
             * 扫描过程中所有被扫描到的结果回调。由于扫描及过滤的过程是在工作线程中的，此方法也处于工作线程中。
             * 同一个设备会在不同的时间，携带自身不同的状态（比如信号强度等），出现在这个回调方法中，
             * 出现次数取决于周围的设备量及外围设备的广播间隔。
             */
            @Override
            public void onLeScan(BleDevice bleDevice) {
                //设置为扫描状态
                status = Status.SCANNING;
                LogUtil.d("onLeScan:"+bleDevice.getName());
            }

            /**
             * 扫描过程中的所有过滤后的结果回调。与onLeScan区别之处在于：它会回到主线程；同一个设备只会出现一次；
             * 出现的设备是经过扫描过滤规则过滤后的设备。
             */
            @Override
            public void onScanning(BleDevice bleDevice) {
                //设置为扫描状态
                status = Status.SCANNING;
                LogUtil.d("onScanning:"+bleDevice.getName());
            }

            /**
             * 迁移到项目中去之后，扫描3S为一个扫描周期，在这个方法中选择距离最近的设置为currentbleDevice
             * @param scanResultList
             */
            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                LogUtil.d("扫描结束，取出符合规则的设备，如果有的话");

                //判断附近的所有的合法设备列表
                if (scanResultList==null || scanResultList.isEmpty()) {
                    LogUtil.d("scanResultList==null || scanResultList.isEmpty()");
                    BluetoothService.this.status = Status.NORMAL;
                    return;
                }

                List<BleDevice> ResultList = new ArrayList<>();
                //给出判断之前要先进行一波筛选
                for (BleDevice bleDevice:scanResultList){
                    if (bleDevice.getName() != null && isCorrectDevice(bleDevice)) {
                        ResultList.add(bleDevice);
                    }
                }

                if (ResultList != null && !ResultList.isEmpty()) {
                    BleDevice device = getCorrectDevice(ResultList);
                    if (device != null) {
                        //设置为扫描结束的nomal状态
                        status = Status.TRY_CONNECTING;
                    } else {
                        //为null的情况说明没有合适的，设置状态为nomal
                        status = Status.NORMAL;
                    }
                    Message msg = Message.obtain();
                    msg.what = 1;
                    msg.obj = device;
                    BTHandler.sendMessage(msg);
                } else {
                    LogUtil.d("扫描结束，scanResultList == null");
                    BluetoothService.this.status = Status.NORMAL;
                }
            }

            private double DETERMINE_THE_RADIUS_CONN = 0.4;
            //先处理一下名字，再选择距离！
            private BleDevice getCorrectDevice(List<BleDevice> scanResultList){

                int tempRSSi = -9999;
                int deviceNum  = -1;
                BleDevice resDeivce ;

                for (int i = 0;i<scanResultList.size();i++){
                    LogUtil.d("遍历设备名字-距离"+scanResultList.get(i).getName()
                            +"--"+scanResultList.get(i).getRssi());
                    if (scanResultList.get(i).getRssi() > tempRSSi){
                        tempRSSi = scanResultList.get(i).getRssi();
                        deviceNum = i;
                    }
                }

                if (deviceNum == -1){
                    return null;
                }

                resDeivce = scanResultList.get(deviceNum);
                LogUtil.d("选出的设备名字-距离"+resDeivce.getName()
                        +"--"+resDeivce.getRssi());
                //名字在输入的列表中就已经是合法的了
                if (isTooFar(resDeivce.getRssi()) /*&& isCorrectDevice(resDeivce)*/){
                    return resDeivce;
                }else{
                    return null;
                }

            }

            private boolean isTooFar(int newRSSi){
//                    int A = 82;
//                    double n = 2.0;
//                    int tRssi = abs(newRSSi);
//                    double power = (tRssi - A)/(10 * n);
//                    double res = pow(10,power);
//                    if (res < DETERMINE_THE_RADIUS_CONN){
//                        LogUtil.d("isTooFar-合理的距离");
//                        return true;
//                    }else{
//                        LogUtil.d("isTooFar-距离太远");
//                        return false;
//                    }
                //改为直接判断信号强度，65大约是距离工控机15厘米
                int rssi = Math.abs(newRSSi);
                boolean result = false;
                if (rssi <= 65) {
                    LogUtil.d("isTooFar-距离合适--》"+rssi);
                    result =true;
                }else{
                    LogUtil.d("isTooFar-距离太远--》"+rssi);
                }
                return result;
            }

            private boolean isCorrectDevice(BleDevice device){
                String deviceName = device.getName();
                //需要首先处理名字为null的情况
                if(deviceName == null){
                    LogUtil.d("设备非目标设备：name=null");
                    return false;
                }
                //设备名称为：名字 + 电话号码后四位
                String regex = "[\u4e00-\u9fa5]{2,3}+\\d{4}";
                if (!deviceName.matches(regex)){
                    LogUtil.d("设备非目标设备："+deviceName);
                    return false;
                }
                LogUtil.d("设备为目标设备："+deviceName);
                return true;
            }

        });
    }

    /**
     * 断开连接
     */
    private void disConnect(){
        LogUtil.d("执行断开连接命令");
        BleManager.getInstance().disconnectAllDevice();
        this.status = Status.NORMAL;
        int msgType = CommonMessage.DISCONNECTED;
        sendBroadcastMsg(msgType);
    }

    /**
     * 发送广播的快捷方法
     */
    private void sendBroadcastMsg(int msgType) {
        //蓝牙登录成功之后，发出登录结果广播的逻辑。
        Intent intentLogin = new Intent("com.bdl.bluetoothmessage");
        intentLogin.putExtra("message",gsonUtil.toJson(new CommonMessage(msgType,null)));
        getApplicationContext().sendBroadcast(intentLogin);
    }

    /**
     * 发出心率的通知
     */
    private void heartBeatNotify(BleDevice currentbleDevice){
        //要实现这个方法必须在连接状态
        if (this.status != Status.CONNECTING) {
            return;
        }

        BleManager.getInstance().notify(
                currentbleDevice,
                "0000180d-0000-1000-8000-00805f9b34fb",
                "00002a37-0000-1000-8000-00805f9b34fb",
                new BleNotifyCallback() {
                    @Override
                    public void onNotifySuccess() {
                        // 打开通知操作成功
                        LogUtil.d("可以打开通知！");
                    }

                    @Override
                    public void onNotifyFailure(BleException exception) {
                        // 打开通知操作失败
                        LogUtil.d("打开通知失败！");
                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {
                        // 打开通知后，设备发过来的数据将在这里出现
                        int index = ((data[0] & 0x01) == 1) ? 2 : 1;
                        int format = (index == 1) ? FORMAT_UINT8 : FORMAT_UINT16;
                        int intValue = new MyBluetoothGattCharacteristic(data).getIntValue(format, index);
                        String result = String.valueOf(intValue);

                        //发心率广播，在第一用户是蓝牙手环的情况下
                        if(MyApplication.getInstance().getUser()!=null){
                            LogUtil.d("打印当前用户："+MyApplication.getInstance().getUser().getUserId());
                            LogUtil.d("打印用户心率："+result);
                            Intent intent = new Intent("com.bdl.bluetoothmessage");
                            CommonMessage message = CommonMessage.heartBeatMsg(result);
                            intent.putExtra("message",gsonUtil.toJson(message));
                            getApplicationContext().sendBroadcast(intent);
                        }

                    }
                });
    }

}
