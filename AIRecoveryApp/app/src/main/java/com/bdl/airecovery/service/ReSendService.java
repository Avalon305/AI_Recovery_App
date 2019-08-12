package com.bdl.airecovery.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.entity.DTO.PersonalSettingDTO;
import com.bdl.airecovery.entity.DTO.TrainResultDTO;
import com.bdl.airecovery.entity.TempStorage;
import com.bdl.airecovery.netty.DataSocketClient;
import com.bdl.airecovery.proto.BdlProto;
import com.bdl.airecovery.proto.DataProtoUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.xutils.DbManager;
import org.xutils.ex.DbException;

import java.lang.reflect.Type;
import java.net.ConnectException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * 重传service，每隔一段时间发送数据。
 * 伴随app启动而启动
 */
public class ReSendService extends Service {
    private int personalSettingSeq = 1;         //消息序列号
    private int trainResultSeq = 1;             //消息序列号

    //service对象启动即单例，后台返回也不会重复创建。
    DbManager dbManager = MyApplication.getInstance().getDbManager();
    //重传间隔时间常量，暂定5分钟
    private final static int duration = 1000 * 5;
    //创建定时器对象
    private final Timer timer = new Timer();
    //定时任务，查询数据库前10条数据然后发送。
    //内部不用起线程，timer的源代码里面就是线程实现，直接用即可
    private TimerTask task = new TimerTask(){
        public void run() {
            Log.d("重传service","正在重传数据");
            queryTempStorage();
        }
    };

    /**
     * 查询暂存表
     */
    private void queryTempStorage(){
        try {
            //查询出10条数据，方法可用性需要测试。
            List<TempStorage> tempStorageList =
                    dbManager.selector(TempStorage.class).limit(10).findAll();
            if (tempStorageList != null) {
                Log.d("重传service","成功读取暂存表，大小为"+tempStorageList.size());
                for (TempStorage tempStorage : tempStorageList) {
                    //代码简洁是毒药！此处注意switch与枚举配合使用的bug.必须使用简洁的写法。
                    //如果测试不通过，该用静态常量对方式定义类型
                    Log.d("重传service","数据类型，"+tempStorage.getType());
                    try {
                        switch (tempStorage.getType()) {
                            case 1:
                                Log.d("重传service", "准备发送医护设置");
                                reSetPersonalSettinglist(tempStorage);
                                Log.d("重传service", "数据库id校验：" + dbManager.findById(TempStorage.class, tempStorage.getId()).getId());

                                break;

                            case 2:
                                Log.d("重传service", "准备发送训练结果");
                                SendTrainResult(tempStorage);
                                Log.d("重传service", "数据库id校验：" + dbManager.findById(TempStorage.class, tempStorage.getId()).getId());
                                break;

                            default:
                                Log.e("重传service", "暂存表数据异常，已删除异常数据");
                                dbManager.deleteById(TempStorage.class, tempStorage.getId());
                                break;
                        }
                    }catch (ConnectException e){
                        Log.e("重传service","无法连接至教练机");
                        break;
                    }
                }
            }
        } catch (DbException e) {
            e.printStackTrace();
            Log.e("重传service","查询暂存表失败");
        }
    }

    /**
     * 调用发送训练结果接口
     * @param tempStorage
     */
    private void SendTrainResult(TempStorage tempStorage) throws ConnectException {
        //TODO
        Gson gson = new Gson();
        String sendStr = tempStorage.getData();
        Type listType = new TypeToken<TrainResultDTO>(){}.getType();
        TrainResultDTO sendMsg = gson.fromJson(sendStr,listType);

        Log.d("重传service","数据库id："+tempStorage.getId());


//        if (sendMsg.getActivityTypeValue_() == 0) {
//            for (int devicetype = 0; devicetype < 9; devicetype++) {
        BdlProto.UploadRequest uploadRequest = BdlProto.UploadRequest.newBuilder()
                .setUid(sendMsg.getUid_())
                .setTrainModeValue(sendMsg.getTrainModeValue_())
                .setCourseId(sendMsg.getCourseId_())
                .setActivityId(sendMsg.getActivityId_())
                .setActivityRecordId(sendMsg.getActivityRecordId_())
                .setDeviceTypeValue(sendMsg.getDeviceTypeValue_())
                .setActivityTypeValue(sendMsg.getActivityTypeValue_())
                .setDefatModeEnable(sendMsg.isDefatModeEnable_())
                .setReverseForce(sendMsg.getReverseForce_())
                .setForwardForce(sendMsg.getForwardForce_())
                .setPower(sendMsg.getPower_())
                .setFinishCount(sendMsg.getFinishCount_())
                .setFinalDistance(sendMsg.getFinalDistance_())
                .setCalorie(sendMsg.getCalorie_())
                .setTrainTime(sendMsg.getTrainTime_())
                .setHeartRateAvg(sendMsg.getHeartRateAvg_())
                .setHeartRateMax(sendMsg.getHeartRateMax_())
                .setHeartRateMin(sendMsg.getHeartRateMin_())
                .setDataId(String.valueOf(tempStorage.getId()))
                .build();//前两行从蓝牙service获取，后两行从activity获取
        //请求递增，seq达到 Integer.MAX_VALUE时重新计数
        if (trainResultSeq == Integer.MAX_VALUE) {
            trainResultSeq = 1;
        }
        BdlProto.Message message = DataProtoUtil.packUploadRequest(trainResultSeq++, uploadRequest);

//        if(MyApplication.getUpload() != null) {
//            Log.d("重传service", "设置测试数据：" + MyApplication.getUpload().getHeartRateMin_());
//        }
//        Log.d("重传service", "校验打包数据：" + sendMsg.getHeartRateMin_());
        Log.d("重传service", "发送的请求：" + message.toString());
        //发送Message
        Log.d("重传service", "正在发送训练结果");
        DataSocketClient.getInstance().sendMsg(message);
//            }
//        }else if (sendMsg.getActivityTypeValue_() == 1){
//            for (int devicetype = 9; devicetype < 17; devicetype++) {
//                BdlProto.UploadRequest uploadRequest = BdlProto.UploadRequest.newBuilder()
//                        .setUid(sendMsg.getUid_())
//                        .setTrainModeValue(sendMsg.getTrainModeValue_())
//                        .setCourseId(sendMsg.getCourseId_())
//                        .setActivityId(sendMsg.getActivityId_())
//                        .setActivityRecordId(sendMsg.getActivityRecordId_())
//                        .setDeviceTypeValue(devicetype)
//                        .setActivityTypeValue(sendMsg.getActivityTypeValue_())
//                        .setDefatModeEnable(sendMsg.isDefatModeEnable_())
//                        .setReverseForce(sendMsg.getReverseForce_())
//                        .setForwardForce(sendMsg.getForwardForce_())
//                        .setPower(sendMsg.getPower_())
//                        .setFinishCount(sendMsg.getFinishCount_())
//                        .setFinalDistance(sendMsg.getFinalDistance_())
//                        .setCalorie(sendMsg.getCalorie_())
//                        .setTrainTime(sendMsg.getTrainTime_())
//                        .setHeartRateAvg(sendMsg.getHeartRateAvg_())
//                        .setHeartRateMax(sendMsg.getHeartRateMax_())
//                        .setHeartRateMin(sendMsg.getHeartRateMin_())
//                        .setDataId(String.valueOf(tempStorage.getId()))
//                        .build();//前两行从蓝牙service获取，后两行从activity获取
//                //请求递增，seq达到 Integer.MAX_VALUE时重新计数
//                if (trainResultSeq == Integer.MAX_VALUE) {
//                    trainResultSeq = 1;
//                }
//                BdlProto.Message message = DataProtoUtil.packUploadRequest(trainResultSeq++, uploadRequest);
//
//                Log.d("重传service", "设置测试数据：" + MyApplication1.getUpload().getHeartRateMin_());
//                Log.d("重传service", "校验打包数据：" + sendMsg.getHeartRateMin_());
//                Log.d("重传service", "发送的请求：" + message.toString());
//                //发送Message
//                Log.d("重传service", "正在发送训练结果");
//                DataSocketClient.getInstance().sendMsg(message);
//            }
//        }

    }

    /**
     * 调用重设医护设置接口
     * @param tempStorage
     */
    private void reSetPersonalSettinglist(TempStorage tempStorage) throws ConnectException {
        //TODO
        Gson gson = new Gson();
        String sendStr = tempStorage.getData();
        Type listType = new TypeToken<PersonalSettingDTO>(){}.getType();
        PersonalSettingDTO sendMsg = gson.fromJson(sendStr,listType);

        Log.d("重传service","前方限制："+sendMsg.getFrontLimit());
        Log.d("重传service","数据库id："+tempStorage.getId());
        BdlProto.PersonalSetRequest request = BdlProto.PersonalSetRequest.newBuilder()
                .setUid(sendMsg.getUid()) //用户ID
                .setDeviceTypeValue(sendMsg.getDeviceTypeValue()) //设备类型
                .setActivityTypeValue(sendMsg.getActivityTypeValue()) //循环类型
                .setSeatHeight(sendMsg.getSeatHeight()) //座位高度
                .setBackDistance(sendMsg.getBackDistance()) //靠背距离
                .setLeverLength(sendMsg.getLeverLength()) //杠杆长度
                .setLeverAngle(sendMsg.getLeverAngle()) //杠杆角度
                .setForwardLimit(sendMsg.getFrontLimit()) //前方限制
                .setBackLimit(sendMsg.getBackLimit()) //后方限制
                .setTrainModeValue(sendMsg.getTrainModeValue()) //训练模式
                .setDefatModeEnable(sendMsg.isOpenFatLossMode()) //是否开启减脂模式
                .setDataId(String.valueOf(tempStorage.getId()))
                .build();

        //打包方法的第一个参数seq是消息序列号，每个请求递增，当达到 Integer.MAX_VALUE时重新计数，调用时自行处理
        if(personalSettingSeq == Integer.MAX_VALUE) {
            personalSettingSeq = 1;
        }

        BdlProto.Message message = DataProtoUtil.packPersonalSetRequest(personalSettingSeq++,request);
        Log.d("重传service","发送的请求："+message.toString());
        //发送Message
        Log.d("重传service","正在发送医护设置");
        DataSocketClient.getInstance().sendMsg(message);
    }

    public ReSendService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        //启动定时器
        Log.d("重传service","重传service已启动");
        timer.schedule(task, 0, duration);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        task.cancel();
        timer.cancel();
        super.onDestroy();
    }

}