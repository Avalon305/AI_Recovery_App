package com.bdl.airecovery.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.entity.DTO.PersonalSettingDTO;
import com.bdl.airecovery.entity.DTO.StrengthTest;
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
    private int strengthTestSeq = 1;
    private int ErrorInfoSeq = 1;                //消息序列号

    //service对象启动即单例，后台返回也不会重复创建。
    DbManager dbManager = MyApplication.getInstance().getDbManager();
    //重传间隔时间常量，暂定5分钟
    private final static int duration = 1000 * 5;
    //创建定时器对象
    private final Timer timer = new Timer();
    //定时任务，查询数据库前10条数据然后发送。
    //内部不用起线程，timer的源代码里面就是线程实现，直接用即可
    private TimerTask task = new TimerTask() {
        public void run() {
            Log.d("重传service", "正在重传数据");
            queryTempStorage();
        }
    };

    /**
     * 查询暂存表
     */
    private void queryTempStorage() {
        try {
            //查询出10条数据，方法可用性需要测试。
            List<TempStorage> tempStorageList =
                    dbManager.selector(TempStorage.class).limit(10).findAll();
            if (tempStorageList != null) {
                Log.d("重传service", "成功读取暂存表，大小为" + tempStorageList.size());
                for (TempStorage tempStorage : tempStorageList) {
                    //代码简洁是毒药！此处注意switch与枚举配合使用的bug.必须使用简洁的写法。
                    //如果测试不通过，该用静态常量对方式定义类型
                    Log.d("重传service", "数据类型，" + tempStorage.getType());
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

                            case 3:
                                Log.d("重传service", "准备发送肌力测试结果");
                                sendStrengthTestResult(tempStorage);
                                Log.d("重传service", "数据库id校验：" + dbManager.findById(TempStorage.class, tempStorage.getId()).getId());
                                break;
                            default:
                                Log.e("重传service", "暂存表数据异常，已删除异常数据");
                                dbManager.deleteById(TempStorage.class, tempStorage.getId());
                                break;
                        }
                    } catch (ConnectException e) {
                        Log.e("重传service", "无法连接至教练机");
                        break;
                    }
                }
            }
        } catch (DbException e) {
            e.printStackTrace();
            Log.e("重传service", "查询暂存表失败");
        }
    }

    /**
     * 调用发送训练结果接口
     *
     * @param tempStorage
     */
    private void SendTrainResult(TempStorage tempStorage) throws ConnectException {
        Gson gson = new Gson();
        String sendStr = tempStorage.getData();
        Type listType = new TypeToken<TrainResultDTO>() {
        }.getType();
        TrainResultDTO sendMsg = gson.fromJson(sendStr, listType);

        Log.d("重传service", "数据库id：" + tempStorage.getId());

        BdlProto.UploadRequest uploadRequest = BdlProto.UploadRequest.newBuilder()
                .setUid(sendMsg.getUid_())
                .setDeviceTypeValue(sendMsg.getDeviceTypeValue_())
                .setTrainModeValue(sendMsg.getTrainModeValue_())
                .setConsequentForce(sendMsg.getForwardForce_())
                .setReverseForce(sendMsg.getReverseForce_())
                .setPower(sendMsg.getPower_())
                .setSpeedRank(sendMsg.getSpeedRank())
                .setFinishNum(sendMsg.getFinishNum_())
                .setDistance(sendMsg.getFinalDistance_())
                .setEnergy(sendMsg.getEnergy_())
                .setHeartRateList(sendMsg.getHeart_rate_list())
                .setBindId(sendMsg.getBindId_())
                .setDpId(sendMsg.getDpId_())
                .setDataId(String.valueOf(tempStorage.getId()))
                .build();
        //请求递增，seq达到 Integer.MAX_VALUE时重新计数
        if (trainResultSeq == Integer.MAX_VALUE) {
            trainResultSeq = 1;
        }
        BdlProto.Message message = DataProtoUtil.packUploadRequest(trainResultSeq++, uploadRequest);

        Log.d("重传service", "发送的请求：" + message.toString());
        //发送Message
        Log.d("重传service", "正在发送训练结果");
        DataSocketClient.getInstance().sendMsg(message);


    }

    /**
     * 调用重设医护设置接口
     *
     * @param tempStorage
     */
    private void reSetPersonalSettinglist(TempStorage tempStorage) throws ConnectException {
        //TODO
        Gson gson = new Gson();
        String sendStr = tempStorage.getData();
        Type listType = new TypeToken<PersonalSettingDTO>() {
        }.getType();
        PersonalSettingDTO sendMsg = gson.fromJson(sendStr, listType);

        Log.d("重传service", "前方限制：" + sendMsg.getForwardLimit());
        Log.d("重传service", "数据库id：" + tempStorage.getId());
        BdlProto.PersonalSetRequest request = BdlProto.PersonalSetRequest.newBuilder()
                .setUid(sendMsg.getUid()) //用户ID
                .setSeatHeight(sendMsg.getSeatHeight()) //座位高度
                .setBackDistance(sendMsg.getBackDistance()) //靠背距离
                .setFootboardDistance(sendMsg.getFootboardDistance())//踏板距离
                .setLeverAngle(sendMsg.getLeverAngle()) //杠杆角度
                .setForwardLimit(sendMsg.getForwardLimit()) //前方限制
                .setBackLimit(sendMsg.getBackLimit()) //后方限制
                .setConsequentForce(sendMsg.getConsequentForce())//顺向力
                .setReverseForce(sendMsg.getReverseForce())
                .setPower(sendMsg.getPower())
                .setDataId(String.valueOf(tempStorage.getId()))
                .build();

        //打包方法的第一个参数seq是消息序列号，每个请求递增，当达到 Integer.MAX_VALUE时重新计数，调用时自行处理
        if (personalSettingSeq == Integer.MAX_VALUE) {
            personalSettingSeq = 1;
        }

        BdlProto.Message message = DataProtoUtil.packPersonalSetRequest(personalSettingSeq++, request);
        Log.d("重传service", "发送的请求：" + message.toString());
        //发送Message
        Log.d("重传service", "正在发送医护设置");
        DataSocketClient.getInstance().sendMsg(message);
    }

    /**
     * 发送肌力测试结果
     * @param tempStorage
     */
    private void sendStrengthTestResult(TempStorage tempStorage) throws ConnectException {
        Gson gson = new Gson();
        String sendStr = tempStorage.getData();
        Type listType = new TypeToken<TrainResultDTO>() {
        }.getType();
        StrengthTest sendMsg = gson.fromJson(sendStr, listType);

        BdlProto.MuscleStrengthRequest request = BdlProto.MuscleStrengthRequest.newBuilder()
                .setUid(sendMsg.getUid())
                .setMuscleTestValue(sendMsg.getResult())
                .setMuscleCreatTime(sendMsg.getTime())
                .build();

        if (strengthTestSeq == Integer.MAX_VALUE) {
            strengthTestSeq = 1;
        }

        BdlProto.Message message = DataProtoUtil.packMuscleStrengthRequest(strengthTestSeq++, request);
        DataSocketClient.getInstance().sendMsg(message);
    }

//    private  void SendErrorInfo() throws ConnectException{
//
//        BdlProto.ErrorInfoRequest request = BdlProto.ErrorInfoRequest.newBuilder()
//                .setUid()
//                .setDeviceTypeValue()
//                .setTrainModeValue()
//                .setTrainModeValue()
//                .setError()
//                .setErrorStartTime()
//                .build();
//
//        if(ErrorInfoSeq == Integer.MAX_VALUE) {
//            ErrorInfoSeq = 1;
//        }
//        BdlProto.Message message =DataProtoUtil.packErrorInfoRequest(ErrorInfoSeq++,request);
//        Log.d("重传service","发送的请求："+message.toString());
//        //发送Message
//        Log.d("重传service","肌力测试");
//        DataSocketClient.getInstance().sendMsg(message);
//    }

    public ReSendService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        //启动定时器
        Log.d("重传service", "重传service已启动");
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
