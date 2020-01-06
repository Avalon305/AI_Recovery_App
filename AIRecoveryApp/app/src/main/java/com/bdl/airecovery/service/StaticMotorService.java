package com.bdl.airecovery.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.bdl.airecovery.constant.StaticMotorConstant;
import com.bdl.airecovery.util.CodecUtils;
import com.bdl.airecovery.util.SerialPortUtils;

import java.util.Arrays;

/**
 * 应用设备：坐式背部伸展机(力量循环    坐式腿伸展训练机(力量耐力循环)   坐式屈腿训练机(力量耐力循环)  坐式背部伸展机(力量耐力循环)
 * 开机需要先执行标定流程：过程中不能坐人，先发送心跳，无回应则失败，有回应则继续：使电机下降到最低点--执行标定命令--再使电机下降到最低点
 * 电机direction：0为顺向，1为反向
 */

public class StaticMotorService extends Service{

    //全局变量
    private static String TAG = "静态电机service";
    private byte[] receive_Flag = {0x55, (byte) 0xAA};
    private Intent locateBroadcast = new Intent("locate");
    private InitLocateReceiver initLocateReceiver = new InitLocateReceiver();       //限位广播监听类
    //    private Intent initLocate = new Intent("locate");
    //静态电机工具包实例
    private class StaticMotorUtil{
        public byte[] receive_Head = {0,0};
        public byte[] receive_Till = {0,0};
        public byte[] receiveMsg;
        public byte[] responseMsg; //服务器返回的信息
        public int checkThisPosition;
        public boolean isPositionNeedCheck = false;
        public SerialPortUtils StaticMotor;
        public int direction = 0;//安装时是否装反了

        //连测定位
        public boolean onInitSet = false;//是否处于连测定位
        public boolean onMotorAlive = false;//是否返回心跳应答
        public boolean onInitGet = false;//是否返回了标定应答
        public boolean isSeat = false;//是否是座椅
        //训练前定位
        public boolean onTrainSet = false;//是否处于训练前定位
    }
    StaticMotorUtil StaticMotorUtil_1 = new StaticMotorUtil();
    StaticMotorUtil StaticMotorUtil_2 = new StaticMotorUtil();
    //定位外部专用接口
    public class Controler {
        //连测定位（返回值方式）
        public boolean initLocate(int MotorIndex, boolean isSeat) {
            Log.d(TAG, "initLocate: 联测了！！！！！");
            boolean result = false;
            if (MotorIndex == 1) {
                StaticMotorUtil_1.isSeat = isSeat;
                result = initSet(StaticMotorUtil_1);
            } else if (MotorIndex == 2) {
                StaticMotorUtil_2.isSeat = isSeat;
                result = initSet(StaticMotorUtil_2);
            } else {
                Log.e(TAG, "initLocate: 无此静态电机");
            }
            return result;
        }
        //连测定位（广播方式）
        public void initLocate2(int MotorIndex){
            if (MotorIndex == 1) {
                initSet2(StaticMotorUtil_1);
            } else if (MotorIndex == 2) {
                initSet2(StaticMotorUtil_2);
            } else {
                Log.e(TAG, "initLocate: 无此静态电机");
            }
        }
        //训练前定位（广播方式）
        public void trainLocate(int MotorIndex,int position,int MotorDirection) {
            Log.d(TAG, "trainLocate: 训练前定位了！！！！！！！！！！");
            Log.d(TAG, "电机为: "+MotorIndex);
            Log.d(TAG, "位置为: "+position);
            if (MotorIndex == 1) {
                StaticMotorUtil_1.direction = MotorDirection;
                trainSet(StaticMotorUtil_1,position);
            } else if (MotorIndex == 2) {
                StaticMotorUtil_2.direction = MotorDirection;
                trainSet(StaticMotorUtil_2,position);
            } else {
                Log.e(TAG, "initLocate: 无此静态电机");
            }
        }
    }
    public static Controler controler = null;
    public static Controler getControler() {
        return controler;
    }

    /**
     * 串口生命周期
     */
    //设置串口（服务启动时执行）
    private void SerialSet() {
        StaticMotorUtil_1.StaticMotor = new SerialPortUtils("/dev/ttyO2",9600);
        StaticMotorUtil_2.StaticMotor = new SerialPortUtils("/dev/ttyO3",9600);
        SerialOpen(StaticMotorUtil_1);
        SerialOpen2(StaticMotorUtil_2);
    }
    //打开电机1串口,获取串口读写方法
    private void SerialOpen(StaticMotorUtil util) {
        try {
            util.StaticMotor.openSerialPort();
            util.StaticMotor.setOnDataReceiveListener(new Recived());
            Log.d(TAG,"打开电机1串口成功");
        }catch (Exception e){
            Log.e(TAG,"openSerialPort: 打开电机1串口异常：" + e.toString());
        }
    }
    //打开电机2串口
    private void SerialOpen2(StaticMotorUtil util) {
        try {
            util.StaticMotor.openSerialPort();
            util.StaticMotor.setOnDataReceiveListener(new Recived_2());
            Log.d(TAG,"打开电机2串口成功");
        }catch (Exception e){
            Log.e(TAG,"openSerialPort: 打开电机2串口异常：" + e.toString());
        }
    }
    //关闭串口（服务销毁时执行）
    private void SerialClose(StaticMotorUtil util){
        util.StaticMotor.closeSerialPort();
    }

    /**
     * 被动方法
     */
    //分析应答
    private void analyzeMsg(final StaticMotorUtil util){
        Log.e(TAG,"收到报文："+Arrays.toString(util.responseMsg));
        byte[] analyzeHead = {0, 0, 0, 0, 0, 0, 0, 0};
        if (util.responseMsg.length >= 8) {
            for (int i = 0; i < 8; i++) {
                analyzeHead[i] = util.responseMsg[i];
            }
        }
        if (Arrays.equals(util.responseMsg,StaticMotorConstant.ANSWER_MOVE)){
            Log.d(TAG,"收到了位移应答");
        }else if (Arrays.equals(util.responseMsg,StaticMotorConstant.MOVEDONE)){
            Log.d(TAG,"收到了位移完毕通知");
            answerMovedone(util.StaticMotor);
            getPosition(util.StaticMotor);
            util.isPositionNeedCheck = true;
        }else if (Arrays.equals(util.responseMsg,StaticMotorConstant.ANSWER_START)){
            Log.d(TAG,"收到了开始移动应答");
        }else if (Arrays.equals(util.responseMsg,StaticMotorConstant.ANSWER_STOP)){
            Log.d(TAG,"收到了停止移动应答");
        }else if (Arrays.equals(util.responseMsg,StaticMotorConstant.OVERCURRENT)){
            Log.d(TAG,"收到了过流通知");
            answerOvercurrent(util.StaticMotor);
        }else if (Arrays.equals(util.responseMsg,StaticMotorConstant.FINISH_INIT)){
            Log.d(TAG,"收到了标定完成应答");
            moveDown(util.StaticMotor);
            //TODO
            locateBroadcast.putExtra("initlocate",true);
            sendBroadcast(locateBroadcast);
        }else if (Arrays.equals(analyzeHead,StaticMotorConstant.ANSWER_GETPOSITION_HEAD)){
            Log.d(TAG,"收到了获取位置应答");
            if (util.isPositionNeedCheck){
                Log.d(TAG,"正在校验位置...");
                checkPosition(util);
            }
        }else if (Arrays.equals(analyzeHead,StaticMotorConstant.REACHLIMIT_HEAD)){
            Log.d(TAG,"收到了到达限位通知");
            answerReachLimit(util.StaticMotor);
            if (util.onInitSet){
                if (util.isSeat){
                    Log.d(TAG,"取消标定，限位开关故障");
                } else {
                    util.onInitSet = true;
                    Log.d(TAG,"标定流程2：标定指令");
                    util.onInitSet = false;
                    initMotor(util.StaticMotor);
                    new Thread(){
                        @Override
                        public void run() {
                            super.run();
                            while (true) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                Log.d(TAG, "run: 是否开始标定："+util.onInitGet);
                                if (!util.onInitGet){
                                    initMotor(util.StaticMotor);
                                }else {
                                    Log.d(TAG, "run: 不发标定了！！！！！！！！");
                                    break;
                                }
                            }
                        }
                    }.start();
                }
            } else if (util.onInitGet) {
                if (util.isSeat) {
                    Log.d(TAG,"取消标定，限位开关故障");
                } else {
                    util.onInitGet = false;
                    Log.d(TAG, "run: 标定完成，电机复位");
                }
            }
        }else if (Arrays.equals(analyzeHead,StaticMotorConstant.ANSWER_HEARTBEAT_HEAD)) {
            Log.d(TAG, "收到了心跳应答");
            if (util.onInitSet){
                util.onMotorAlive = true;
            }
        }else if (Arrays.equals(util.responseMsg,StaticMotorConstant.ANSWER_INIT)) {
            Log.d(TAG, "收到了标定应答");
            util.onInitGet = true;
        } else if (Arrays.equals(util.responseMsg,StaticMotorConstant.ANSWER_LIMIT)) {
            Log.d(TAG, "收到了限位通知应答");
            if (util.onInitSet){
                Log.d(TAG,"标定流程2：标定指令");
                util.onInitSet = false;
                initMotor(util.StaticMotor);
                new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        while (true) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Log.d(TAG, "run: 是否开始标定："+util.onInitGet);
                            if (!util.onInitGet){
                                initMotor(util.StaticMotor);
                            }else {
                                Log.d(TAG, "run: 不发标定了！！！！！！！！");
                                break;
                            }
                        }
                    }
                }.start();
            } else if (util.onInitGet) {
                util.onInitGet = false;
                Log.d(TAG, "run: 标定完成，电机复位");
            }
        }else {
            Log.e(TAG,"收到未识别的报文："+Arrays.toString(util.responseMsg));
        }
    }
    //发送心跳
    private void sendHeartbeat(SerialPortUtils StaticMotor){
        byte[] sendMsg = StaticMotorConstant.HEARTBEAT;
        StaticMotor.sendByteArray(sendMsg);
        Log.d(TAG,"发送了心跳"+Arrays.toString(sendMsg));
    }
    //获取位置
    private void getPosition(SerialPortUtils StaticMotor){
        byte[] sendMsg = StaticMotorConstant.GETPOSITION;
        StaticMotor.sendByteArray(sendMsg);
        Log.d(TAG,"发送了获取位置"+Arrays.toString(sendMsg));
    }
    //校验位置
    private void checkPosition(StaticMotorUtil util){
        util.isPositionNeedCheck = false;
        byte dataByte = util.responseMsg[8];
//        if (dataByte != util.checkThisPosition){
        if (dataByte < util.checkThisPosition-1 || dataByte > util.checkThisPosition+1){
            Log.e(TAG,"位置有误，重新定位");
            setPosition(util,util.checkThisPosition);
        }else {
            Log.d(TAG,"位置正确");
            if (util.onTrainSet){
                util.onTrainSet = false;
                locateBroadcast.putExtra("success",true);
                sendBroadcast(locateBroadcast);
            }
        }
    }
    //位移完毕应答
    private void answerMovedone(SerialPortUtils StaticMotor){
        byte[] sendMsg = StaticMotorConstant.ANSWER_MOVEDONE;
        StaticMotor.sendByteArray(sendMsg);
        Log.d(TAG,"发送了位移完成应答"+Arrays.toString(sendMsg));
    }
    //过流通知应答
    private void answerOvercurrent(SerialPortUtils StaticMotor){
        byte[] sendMsg = StaticMotorConstant.ANSWER_OVERCURRENT;
        StaticMotor.sendByteArray(sendMsg);
        Log.d(TAG,"发送了过流通知应答"+Arrays.toString(sendMsg));
    }
    //到达限位通知应答
    private void answerReachLimit(SerialPortUtils StaticMotor){
        byte[] sendMsg = StaticMotorConstant.ANSWER_REACHLIMIT;
        StaticMotor.sendByteArray(sendMsg);
        Log.d(TAG,"发送了到达限位通知应答"+Arrays.toString(sendMsg));
    }

    /**
     * service生命周期
     */
    @Override
    public IBinder onBind(Intent intent){
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"静态电机服务已创建");
        controler = new Controler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                SerialSet();
            }
        }).start();

    }

    @Override
    public void onDestroy() {
        SerialClose(StaticMotorUtil_1);
        SerialClose(StaticMotorUtil_2);
        super.onDestroy();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getIntExtra("index",0) == 1){      //电机1
            switch (intent.getStringExtra("command")){
                case "SETPOSITION":
                    Log.d("静态电机1", "请求设置位置");
                    StaticMotorUtil_1.checkThisPosition = intent.getIntExtra("position",0);
                    StaticMotorUtil_1.direction = intent.getIntExtra("type",0);
                    setPosition(StaticMotorUtil_1,intent.getIntExtra("position",0));
                    break;
                case "TESTLOCATE":
                    Log.d("静态电机1", "请求连测定位");
                    StaticMotorUtil_1.checkThisPosition = 1;
                    testLocate(StaticMotorUtil_1.StaticMotor);
                    break;
                case "GETPOSITION":
                    Log.d("静态电机1", "请求获得位置");
                    StaticMotorUtil_2.isPositionNeedCheck = false;
                    getPosition(StaticMotorUtil_1.StaticMotor);
                    break;
                case "STOP":
                    Log.d("静态电机1", "请求停止");
                    stopMove(StaticMotorUtil_1.StaticMotor);
                    break;
                case "SENDHEARTBEAT":
                    Log.d("静态电机1", "请求标定");
                    initMotor(StaticMotorUtil_1.StaticMotor);
                    break;
                case "MOVEUP":
                    Log.d("静态电机1", "请求上升");
                    moveUp(StaticMotorUtil_1.StaticMotor);
                    break;
                case "MOVEDOWN":
                    Log.d("静态电机1", "请求下降");
                    moveDown(StaticMotorUtil_1.StaticMotor);
                    break;
                default:break;
            }
        }else if (intent.getIntExtra("index",0) == 2){//电机2
            switch (intent.getStringExtra("command")){
                case "SETPOSITION":
                    Log.d("静态电机2", "请求设置位置");
                    StaticMotorUtil_2.checkThisPosition = intent.getIntExtra("position",0);
                    StaticMotorUtil_2.direction = intent.getIntExtra("type",0);
                    setPosition(StaticMotorUtil_2,intent.getIntExtra("position",0));
                    break;
                case "TESTLOCATE":
                    Log.d("静态电机2", "请求连测定位");
                    StaticMotorUtil_2.checkThisPosition = 1;
                    testLocate(StaticMotorUtil_2.StaticMotor);
                    break;
                case "GETPOSITION":
                    Log.d("静态电机2", "请求获得位置");
                    StaticMotorUtil_2.isPositionNeedCheck = false;
                    getPosition(StaticMotorUtil_2.StaticMotor);
                    break;
                case "STOP":
                    Log.d("静态电机2", "请求停止");
                    stopMove(StaticMotorUtil_2.StaticMotor);
                    break;
                case "SENDHEARTBEAT":
                    Log.d("静态电机2", "请求发送心跳");
                    sendHeartbeat(StaticMotorUtil_2.StaticMotor);
                    break;
                case "MOVEUP":
                    Log.d("静态电机2", "请求上升");
                    moveUp(StaticMotorUtil_2.StaticMotor);
                    break;
                case "MOVEDOWN":
                    Log.d("静态电机2", "请求下降");
                    moveDown(StaticMotorUtil_2.StaticMotor);
                    break;
                default:break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 主动方法
     */
    //运动到指定位置
    private void setPosition(StaticMotorUtil util,int position) {
        if (util.direction == 1){                                                          //如果装反了则反向设置
            position = 100 - position;
            util.checkThisPosition = position;
        }
        if (position >= 10 && position <= 90) {
            byte[] sendMsg = {0,0,0,0,0,0,0,0,0,0,0,0};
            byte[] checkMsg = {0,0,0,0,0,0,0};
            byte dataByte = (byte) position;
            for (int i = 0; i < StaticMotorConstant.MOVE_HEAD.length; i++){
                sendMsg[i] = StaticMotorConstant.MOVE_HEAD[i];
                if (i >= 2){
                    checkMsg[i - 2] = sendMsg[i];
                }
            }
            checkMsg[6] = dataByte;
            byte checkByte = CodecUtils.getXor(checkMsg);
            sendMsg[8] = dataByte;
            sendMsg[9] = checkByte;
            for (int i = 0; i < 2; i++){
                sendMsg[i + 10] = StaticMotorConstant.TILL[i];
            }
            util.StaticMotor.sendByteArray(sendMsg);
            Log.d(TAG,"发送了设置位置:"+Arrays.toString(sendMsg));
        }
    }
    //标定流程(连测定位)
    private boolean initSet(StaticMotorUtil util){
        util.onInitSet = true;//设置当前处于标定流程状态
        util.onInitGet = false;
        //Step 1:发送心跳
        sendHeartbeat(util.StaticMotor);
        try {
            Thread.sleep(500);
            if (util.onMotorAlive) {
                util.onMotorAlive = false;
                //Step 2:发送下降
                moveDown(util.StaticMotor);
                try {
                    Thread.sleep(1000 * 10);//等待到达限位（最低点）
                    if (util.onInitSet){
                        util.onInitSet = false;
                        return false;
                    }else {
                        return true;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    util.onInitSet = false;
                    return false;
                }
            }else {
                util.onInitSet = false;
                return false;
            }
        } catch (InterruptedException e) {
            util.onInitSet = false;
            e.printStackTrace();
            return false;
        }
    }
    //标定流程2发送限位
    private void initSetLimit(StaticMotorUtil util, boolean isTop) {
        if (util.onInitSet) {
            sendLimit(util.StaticMotor, isTop);
        }
    }
    //连测定位
    private void initSet2(StaticMotorUtil util){
        util.onInitSet = true;//设置当前处于标定流程状态
        //Step 1:发送心跳
        sendHeartbeat(util.StaticMotor);
        try {
            Thread.sleep(500);
            if (util.onMotorAlive) {
                util.onMotorAlive = false;
                //Step 2:发送下降
                moveDown(util.StaticMotor);
            }else {
                locateBroadcast.putExtra("initlocate",false);
                sendBroadcast(locateBroadcast);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            locateBroadcast.putExtra("initlocate",false);
            sendBroadcast(locateBroadcast);
        }
    }
    //训练前定位
    private void trainSet(StaticMotorUtil util,int position){
        util.onTrainSet = true;
        Log.d(TAG, "设置位置了！！！！！！！！！！！！！！！！！！");
        util.checkThisPosition = position;
        setPosition(util,position);
    }
    //运动到位置1
    private void testLocate(SerialPortUtils StaticMotor) {
        byte[] sendMsg = StaticMotorConstant.MOVE;
        StaticMotor.sendByteArray(sendMsg);
        Log.d(TAG,"发送了连测定位:"+Arrays.toString(sendMsg));
    }
    //顶升
    private void moveUp(SerialPortUtils StaticMotor){
        byte[] sendMsg = StaticMotorConstant.STARTUP;
        StaticMotor.sendByteArray(sendMsg);
        Log.d(TAG,"发送了上升请求:"+Arrays.toString(sendMsg));
    }
    //下降
    private void moveDown(SerialPortUtils StaticMotor){
        byte[] sendMsg = StaticMotorConstant.STARTDOWN;
        StaticMotor.sendByteArray(sendMsg);
        Log.d(TAG,"发送了下降请求"+Arrays.toString(sendMsg));
    }
    //停止
    private void stopMove(SerialPortUtils StaticMotor){
        byte[] sendMsg = StaticMotorConstant.STOP;
        StaticMotor.sendByteArray(sendMsg);
        Log.d(TAG,"发送了停止请求"+Arrays.toString(sendMsg));
    }
    //标定
    private void initMotor(SerialPortUtils StaticMotor){
        //Step 3:发送标定
        byte[] sendMsg = StaticMotorConstant.INIT;
        StaticMotor.sendByteArray(sendMsg);
        Log.d(TAG,"发送了标定请求"+Arrays.toString(sendMsg));
    }
    //限位通知
    private void sendLimit(SerialPortUtils StaticMotor, boolean isTop){
        byte[] sendMsg;
        if (isTop) {
            sendMsg = StaticMotorConstant.TOP_LIMIT;
        } else {
            sendMsg = StaticMotorConstant.BOT_LIMIT;
        }
        StaticMotor.sendByteArray(sendMsg);
        Log.d(TAG,"发送了限位通知"+Arrays.toString(sendMsg));
    }

    /**
     * 监听类
     * 收到电机返回数据后会进入这个方法
     */
    //电机1
    class Recived implements SerialPortUtils.OnDataReceiveListener{

        @Override
        public void onDataReceive(byte[] buffer, int size) {
            Log.d(TAG,"返回了数据！！！！！！！！！！！！！！");
            Log.d(TAG,""+Arrays.toString(buffer));

            StaticMotorUtil_1.receiveMsg = new byte[size];
            for (int i = 0; i < 2; i++) {
                StaticMotorUtil_1.receiveMsg[i] = buffer[i];
                StaticMotorUtil_1.receive_Head[i] = buffer[i];
            }
            for (int i = 2; i < size - 2; i++) {
                StaticMotorUtil_1.receiveMsg[i] = buffer[i];
            }
            for (int i = size - 2; i < size; i++) {
                StaticMotorUtil_1.receiveMsg[i] = buffer[i];
                StaticMotorUtil_1.receive_Till[i + 2 - size] = buffer[i];
            }

            if (Arrays.equals(StaticMotorUtil_1.receive_Head, receive_Flag) && Arrays.equals(StaticMotorUtil_1.receive_Till, receive_Flag)) {
                StaticMotorUtil_1.responseMsg = StaticMotorUtil_1.receiveMsg;
                analyzeMsg(StaticMotorUtil_1);
                //初始化
                StaticMotorUtil_1.receive_Head[0] = 0;
                StaticMotorUtil_1.receive_Head[1] = 0;
                StaticMotorUtil_1.receive_Till[0] = 0;
                StaticMotorUtil_1.receive_Till[1] = 0;
                StaticMotorUtil_1.receiveMsg = null;
                StaticMotorUtil_1.responseMsg = null;
            } else {
                Log.e(TAG, "返回了错误格式报文：" + Arrays.toString(StaticMotorUtil_1.receiveMsg));
            }
        }
    }
    //电机2
    class Recived_2 implements SerialPortUtils.OnDataReceiveListener{

        @Override
        public void onDataReceive(byte[] buffer, int size) {
            Log.d(TAG,"返回了数据！！！！！！！！！！！！！！");

            StaticMotorUtil_2.receiveMsg = new byte[size];
            for (int i = 0; i < 2; i++) {
                StaticMotorUtil_2.receiveMsg[i] = buffer[i];
                StaticMotorUtil_2.receive_Head[i] = buffer[i];
            }
            for (int i = 2; i < size - 2; i++) {
                StaticMotorUtil_2.receiveMsg[i] = buffer[i];
            }
            for (int i = size - 2; i < size; i++) {
                StaticMotorUtil_2.receiveMsg[i] = buffer[i];
                StaticMotorUtil_2.receive_Till[i + 2 - size] = buffer[i];
            }

            if (Arrays.equals(StaticMotorUtil_2.receive_Head, receive_Flag) && Arrays.equals(StaticMotorUtil_2.receive_Till, receive_Flag)) {
                StaticMotorUtil_2.responseMsg = StaticMotorUtil_2.receiveMsg;
                analyzeMsg(StaticMotorUtil_2);
                //初始化
                StaticMotorUtil_2.receive_Head[0] = 0;
                StaticMotorUtil_2.receive_Head[1] = 0;
                StaticMotorUtil_2.receive_Till[0] = 0;
                StaticMotorUtil_2.receive_Till[1] = 0;
                StaticMotorUtil_2.receiveMsg = null;
                StaticMotorUtil_2.responseMsg = null;
            } else {
                Log.e(TAG, "返回了错误格式报文：" + Arrays.toString(StaticMotorUtil_2.receiveMsg));
            }
        }
    }
    /**
     * 限位广播接收类
     */
    public class InitLocateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            try{
                if (intentAction.equals("init_locate")){
                    if (intent.getStringExtra("seat_motor") != null) {
                        if (intent.getStringExtra("seat_motor").equals("top_limit")) {
                            sendLimit(StaticMotorUtil_1.StaticMotor, true);
//                            initSetLimit(StaticMotorUtil_1, true);
                        } else if (intent.getStringExtra("seat_motor").equals("bot_limit")) {
                            sendLimit(StaticMotorUtil_1.StaticMotor, false);
//                            initSetLimit(StaticMotorUtil_1, false);
                        }
                    }
//                    else if (intent.getStringExtra("static_motor2") != null) {
//                        if (intent.getStringExtra("static_motor2").equals("top_limit")) {
//                            sendLimit(StaticMotorUtil_2.StaticMotor, true);
////                            initSetLimit(StaticMotorUtil_2, true);
//                        } else if (intent.getStringExtra("static_motor2").equals("bot_limit")) {
//                            sendLimit(StaticMotorUtil_2.StaticMotor, false);
////                            initSetLimit(StaticMotorUtil_2, false);
//                        }
//                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}