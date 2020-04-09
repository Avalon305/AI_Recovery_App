package com.bdl.airecovery.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bdl.airecovery.constant.StaticMotorConstant;
import com.bdl.airecovery.util.CodecUtils;
import com.bdl.airecovery.util.SerialPortUtils;

import java.util.Arrays;

/**
 * 应用设备：坐式背部伸展机(力量循环)   坐式腿伸展训练机(力量耐力循环)   坐式屈腿训练机(力量耐力循环)  坐式背部伸展机(力量耐力循环)
 * 开机需要先执行标定流程：过程中不能坐人，先发送3次心跳，三次无回应则失败，有回应则继续：使电机下降到最低点--执行标定命令--再使电机下降到最低点（归零）
 * 电机direction：0为正向，1为反向（座椅电机必须正向安装，否则影响标定）
 * 电机type：1为座椅电机，0为普通电机
 * 多电机情况座椅电机machine须设为1
 *
 * 目前电机串口：电机1--machine:1--"/dev/ttyO2"
 *              电机2--machine:2--"/dev/ttyO3"
 *
 * device.json文件包含以下电机信息（以下电机信息从device.json获取）：
 *      direction：电机安装方向，0为正向，1为反向（座椅电机必须正向安装，否则影响标定）
 *      type：电机是否为座椅电机，1为座椅电机，0为普通电机
 *      machine：电机序号，1为电机1，2为电机2（序号决定对哪个串口操作）
 *
 * 代码学习建议先全部折叠
 * 任务1：与电机进行串口通讯，可实时接收电机报文和向电机发送报文--实现：内部-串口生命周期、内部-串口监听类、外部-SerialPortUtils串口类
 * 任务2：收到限位广播后，接收一次有效广播，过滤（忽略）重复广播，根据广播种类向电机发送限位通知（顶部/底部）+更改电机状态量--实现：限位广播接收类、方法sendLimit()
 * 任务3：联测定位，流程：Ⅰ 方法getControler()获得联测定位接口类
 *                      Ⅱ 调用接口类返回值方式联测定位方法initLocate()，传入电机序号和电机类型
 *                      Ⅲ 方法initSet()启动联测定位流程
 *                      Ⅳ 第一阶段：先与电机进行三次心跳测试（发送心跳报文并检查应答报文，主动方法sendHeartbeat()，被动方法analyzeMsg()监听心跳应答）（只设一次会有串口读取残留问题），成功则下一步
 *                      Ⅴ          命令电机下降至底端（主动方法moveDown()），普通电机碰到物理限位进入下一步（被动方法analyzeMsg()监听到达限位通知） / 座椅电机收到底端限位通知进入下一步（被动方法analyzeMsg()监听限位通知应答）
 *                      Ⅵ 第二阶段：命令电机执行标定命令（从底端移动至顶端），收到标定完成通知报文进入下一步（被动方法analyzeMsg()监听标定完成通知）
 *                      Ⅶ 第三阶段：复位（命令电机下降至底端），普通电机碰到物理限位进入下一步（被动方法analyzeMsg()监听到达限位通知） / 座椅电机收到底端限位通知进入下一步（被动方法analyzeMsg()监听限位通知应答）
 *                      Ⅷ          若以上流程成功，则置电机标志位isRePositionSuccess为true，标定成功，否则失败
 *                      Ⅸ 联测定位结果判定：心跳测试不通过-------------------------失败（标志位onMotorAlive）
 *                                         心跳测试成功，30s后电机仍未进入第二阶段--失败（标志位onInitSet）
 *                                         心跳测试成功，30s后电机未完成复位-------失败（标志位isRePositionSuccess）
 *                                         （座椅电机限定）心跳测试成功，30s不足三次接收限位广播----失败（标志位isRePositionSuccess）
 *                                         （座椅电机限定）前三次有效接收限位广播顺序不为【下上下】--失败（标志位isRePositionSuccess）
 *                                         否则成功
 * 任务4：训练前定位，流程：Ⅰ 方法getControler()获得训练前定位接口类
 *                       Ⅱ 调用接口类广播方式训练前定位方法trainLocate()，传入电机序号、电机安装方向和需定位到的位置
 *                       Ⅲ 方法trainSet()启动训练前定位流程
 *                       Ⅳ          命令电机移动到指定位置（主动方法setPosition()），被动方法analyzeMsg()监听位移完毕通知
 *                       Ⅴ          收到位移完毕通知后获取当前位置（被动方法getPosition()），被动方法analyzeMsg()监听获取位置应答
 *                       Ⅵ          收到获取位置应答后从报文取出当前位置int型数值并与需定位到的位置对比校验
 *                       Ⅶ          若实际位置在需定位位置±1范围内则发送成功定位广播，否则返回Ⅳ重复执行
 * 任务5：设置位置，流程：Ⅰ 方法onStartCommand()接收指令
 *                      Ⅱ intent.getIntExtra("index",0)获取需操作的电机序号，1为电机1，2为电机2
 *                      Ⅲ intent.getStringExtra("command")获取对应操作的指令（该流程中应为"SETPOSITION"）
 *                      Ⅳ intent.getIntExtra("position",0)获取需移动到的位置
 *                      Ⅴ intent.getIntExtra("type",0)获取电机安装方向
 *                      Ⅵ 命令电机移动到指定位置（主动方法setPosition()），被动方法analyzeMsg()监听位移完毕通知
 *                      Ⅶ 收到位移完毕通知后获取当前位置（被动方法getPosition()），被动方法analyzeMsg()监听获取位置应答
 *                      Ⅷ 收到获取位置应答后从报文取出当前位置int型数值并与需定位到的位置对比校验
 *                      Ⅸ 若实际位置在需定位位置±1范围内则设置位置成功，否则返回Ⅳ重复执行
 * 注：任务4、5流程基本一致，共用大部分代码
 */

public class StaticMotorService extends Service{

    //全局变量
    private static String TAG = "静态电机service";  // 日志TAG（过滤日志用）
    private byte[] receive_Flag = {0x55, (byte) 0xAA};  // 电机报文分隔符，分隔符内为报文具体内容（用于对比接收到的实际报文分隔符是否与此分隔符一致）
    private Intent locateBroadcast = new Intent("locate");  // 定位广播，目前只在训练前定位、设置界面使用
    private InitLocateReceiver initLocateReceiver = new InitLocateReceiver();  // 限位广播监听类，收到该广播则向电机发送限位通知（通知电机到达临界点）
    private volatile boolean allowLimitBroad = false;  // 控制对限位广播的接收开关，true为处理此次广播，false为忽略此次广播
    //静态电机工具包实例（每个电机的状态量等）
    private class StaticMotorUtil{
        public byte[] receive_Head = {0,0};  // 报文首部分隔符
        public byte[] receive_Till = {0,0};  // 报文尾部分隔符
        public byte[] receiveMsg;  // 从电机串口收到的报文（未校验）
        public byte[] responseMsg; // 符合校验的报文
        public int checkThisPosition;  // 设置此电机需要移动的位置，并检查是否移动到此位置
        public boolean isPositionNeedCheck = false;  // 电机是否需要检查位置准确性
        public SerialPortUtils StaticMotor;  // 串口类，对串口开闭读写等操作
        public int direction = 0;  //安装时是否装反了

        //连测定位
        public boolean onInitSet = false;  //是否处于连测定位（联测定位第一阶段：下降至底端）
        public boolean onMotorAlive = false;  //是否返回心跳应答
        public boolean onInitGet = false;  //是否返回了标定应答（联测定位第二阶段：标定过程从底端上升至顶端）
        public boolean isSeat = false;  //是否是座椅电机
        public boolean onRePosition = false;  //是否标定完成正在复位（联测定位第三阶段：复位至底端）（座椅电机限定）
        public boolean isRePositionSuccess = false;  //是否复位成功（座椅电机限定）
        public int limitType = 0;  //此次接收限位广播类型：0-默认，1-顶部，2-底部（座椅电机限定）
        //训练前定位
        public boolean onTrainSet = false;//是否处于训练前定位
    }
    StaticMotorUtil StaticMotorUtil_1 = new StaticMotorUtil();  // 电机1
    StaticMotorUtil StaticMotorUtil_2 = new StaticMotorUtil();  // 电机2（至多2个静态电机接入）
    //联测定位外部专用接口（单词拼错懒得改了）
    public class Controler {
        //连测定位（返回值方式）----目前联测定位界面正在使用，【参数：电机序号（电机1/电机2），是否为座椅电机】，会线程阻塞，需要单独线程执行
        public boolean initLocate(int MotorIndex, boolean isSeat) {
            Log.d(TAG, "initLocate: 联测了！！！！！");
            boolean result = false;
            if (MotorIndex == 1) {
                StaticMotorUtil_1.isSeat = isSeat;
                result = initSet(StaticMotorUtil_1);  // 进入联测流程，线程阻塞等待返回联测结果
            } else if (MotorIndex == 2) {
                StaticMotorUtil_2.isSeat = isSeat;
                result = initSet(StaticMotorUtil_2);  // 进入联测流程，线程阻塞等待返回联测结果
            } else {
                Log.e(TAG, "initLocate: 无此静态电机");
            }
            return result;
        }
        //连测定位（广播方式）----暂未使用
        @Deprecated
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
     * 被动方法（收到电机报文后执行）
     */
    //分析报文
    private void analyzeMsg(final StaticMotorUtil util){
        Log.e(TAG,"收到报文："+Arrays.toString(util.responseMsg));
        byte[] analyzeHead = {0, 0, 0, 0, 0, 0, 0, 0};
        if (util.responseMsg.length >= 8) {
            System.arraycopy(util.responseMsg, 0, analyzeHead, 0, 8);
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
            Log.d(TAG,"收到了标定完成通知");
            if (util.isSeat && util.limitType == 1) {
                moveDown(util.StaticMotor);
                //TODO
                util.onRePosition = true;
                new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        try {
                            Thread.sleep(1000 * 2);
                            allowLimitBroad = true;
                            Thread.sleep(1000 * 5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            } else if (!util.isSeat) {
                moveDown(util.StaticMotor);
                //TODO
                util.onRePosition = true;
                locateBroadcast.putExtra("initlocate", true);
                sendBroadcast(locateBroadcast);
            } else {
                Log.e(TAG, "run: 限位开关方向异常，中断标定");
            }
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
                util.onInitGet = false;
                Log.d(TAG, "run: 标定完成，电机复位");
            } else if (util.onRePosition) {
                util.isRePositionSuccess = true;
                util.onRePosition = false;
                Log.d(TAG, "run: 电机复位完成，流程结束");
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
            if (util.onInitSet && util.limitType == 2){
                Log.d(TAG,"标定流程2：标定指令");
                util.onInitSet = false;
//                allowLimitBroad = false; // 防止触底后电机被困于限位开关范围内，暂停对限位开关的响应
                initMotor(util.StaticMotor);
                new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        while (true) {
                            try {
                                Thread.sleep(1000);

                                Log.d(TAG, "run: 是否开始标定："+util.onInitGet);
                                if (!util.onInitGet){
                                    initMotor(util.StaticMotor);
                                } else {
                                    Log.d(TAG, "run: 不发标定了！！！！！！！！");
                                    Thread.sleep(1000);
                                    allowLimitBroad = true;
                                    break;
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }.start();
            } else if (util.onInitGet && util.limitType == 1) {
                util.onInitGet = false;
                Log.d(TAG, "run: 标定完成，电机复位");
            } else if (util.onRePosition && util.limitType == 2) {
                util.isRePositionSuccess = true;
                util.onRePosition = false;
                Log.d(TAG, "run: 电机复位完成，流程结束");
            } else {
                Log.e(TAG, "run: 限位开关方向异常，中断标定");
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
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("init_locate");
        registerReceiver(initLocateReceiver, intentFilter);
        Log.d(TAG,"静态电机服务已创建");
        controler = new Controler();
        new Thread(this::SerialSet).start();
    }
    @Override
    public void onDestroy() {
        SerialClose(StaticMotorUtil_1);
        SerialClose(StaticMotorUtil_2);
        unregisterReceiver(initLocateReceiver);
        super.onDestroy();
    }
    // 指令式外部接口----------设置界面
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
     * 主动方法（主动向电机发送）
     */
    //运动到指定位置（训练前定位、设置界面）
    private void setPosition(StaticMotorUtil util,int position) {
        if (util.isSeat) {
            allowLimitBroad = true;
        }
        if (util.direction == 1){             //如果装反了则反向设置
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
            System.arraycopy(StaticMotorConstant.TILL, 0, sendMsg, 10, 2);
            util.StaticMotor.sendByteArray(sendMsg);
            Log.d(TAG,"发送了设置位置:"+Arrays.toString(sendMsg));
        }
    }
    //标定流程(连测定位)--------正在使用
    private boolean initSet(StaticMotorUtil util){
        if (util.isSeat) {
            allowLimitBroad = true;
            util.limitType = 0;
            util.onRePosition = false;//是否标定完成正在复位
            util.isRePositionSuccess = false;//是否复位成功
        }
        util.onInitSet = true;//设置当前处于标定流程状态
        util.onInitGet = false;
        //Step 1:发送心跳
//        sendHeartbeat(util.StaticMotor);
        try {
            for (int i = 0; i < 3; i++) {
                sendHeartbeat(util.StaticMotor);
                Thread.sleep(500);
            }
            if (util.onMotorAlive) {
                util.onMotorAlive = false;
                //Step 2:发送下降
                moveDown(util.StaticMotor);
                try {
                    Thread.sleep(1000 * 30);//等待复位完成
                    if (util.onInitSet){
                        util.onInitSet = false;
                        Log.e(TAG, "Error1");
                        return false;
                    } else {
                        if(!util.isRePositionSuccess){
                            Log.e(TAG, "Error5");
                        }
                        return util.isRePositionSuccess;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    util.onInitSet = false;
                    Log.e(TAG, "Error2");
                    return false;
                }
            }else {
                util.onInitSet = false;
                Log.e(TAG, "Error3");
                return false;
            }
        } catch (InterruptedException e) {
            util.onInitSet = false;
            e.printStackTrace();
            Log.e(TAG, "Error4");
            return false;
        }
    }
    //训练前定位
    private void trainSet(StaticMotorUtil util,int position){
        util.onTrainSet = true;
        Log.d(TAG, "设置位置了！！！！！！！！！！！！！！！！！！");
        util.checkThisPosition = position;
        setPosition(util,position);
    }
    //运动到位置1--------------测试用
    private void testLocate(SerialPortUtils StaticMotor) {
        byte[] sendMsg = StaticMotorConstant.MOVE;
        StaticMotor.sendByteArray(sendMsg);
        Log.d(TAG,"发送了测试移动:"+Arrays.toString(sendMsg));
    }
    //顶升（忽略位置）
    private void moveUp(SerialPortUtils StaticMotor){
        byte[] sendMsg = StaticMotorConstant.STARTUP;
        StaticMotor.sendByteArray(sendMsg);
        Log.d(TAG,"发送了上升请求:"+Arrays.toString(sendMsg));
    }
    //下降(连测定位)（忽略位置）
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
    //标定(连测定位)（向“上次移动的相反方向”移动至限位后，完成标定，须手动回到标定前位置）
    private void initMotor(SerialPortUtils StaticMotor){
        //Step 3:发送标定
        byte[] sendMsg = StaticMotorConstant.INIT;
        StaticMotor.sendByteArray(sendMsg);
        Log.d(TAG,"发送了标定请求"+Arrays.toString(sendMsg));
    }
    //限位通知
    private void sendLimit(SerialPortUtils StaticMotor, boolean isTop) {
        byte[] sendMsg;
        if (isTop) {
            sendMsg = StaticMotorConstant.TOP_LIMIT;
        } else {
            sendMsg = StaticMotorConstant.BOT_LIMIT;
        }
        StaticMotor.sendByteArray(sendMsg);
        Log.d(TAG,"发送了限位通知"+Arrays.toString(sendMsg));
    }
    //标定流程(连测定位)-----------------暂时废弃
    @Deprecated
    private void initSet2(StaticMotorUtil util){
        util.onInitSet = true;//设置当前处于标定流程状态
        //Step 1:发送心跳
        sendHeartbeat(util.StaticMotor);
        try {
            for (int i = 0; i < 3; i++) {
                Thread.sleep(500);
                sendHeartbeat(util.StaticMotor);
            }
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

    /**
     * 串口监听类
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
            if (size - 2 - 2 >= 0)
                System.arraycopy(buffer, 2, StaticMotorUtil_1.receiveMsg, 2, size - 2 - 2);
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
            if (size - 2 - 2 >= 0)
                System.arraycopy(buffer, 2, StaticMotorUtil_2.receiveMsg, 2, size - 2 - 2);
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
        public void onReceive(Context context, @NonNull Intent intent) {
            if (allowLimitBroad) {
                allowLimitBroad = false;
                String intentAction = intent.getAction();
                try {
                    if (intentAction != null && intentAction.equals("init_locate")) {
                        if (intent.getStringExtra("seat_motor") != null) {
                            if (intent.getStringExtra("seat_motor").equals("top_limit")) {
                                sendLimit(StaticMotorUtil_1.StaticMotor, true);
                                StaticMotorUtil_1.limitType = 1;
                                Log.e(TAG,"=====收到广播======top_limit");

//                            initSetLimit(StaticMotorUtil_1, true);
                            } else if (intent.getStringExtra("seat_motor").equals("bot_limit")) {
                                sendLimit(StaticMotorUtil_1.StaticMotor, false);
                                StaticMotorUtil_1.limitType = 2;
                                Log.e(TAG,"=====收到广播======bot_limit");
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}