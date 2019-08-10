package com.bdl.airecovery.util;

import android.util.Log;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.entity.Setting;
import com.bdl.airecovery.netty.CountDownSocketClient;
import com.bdl.airecovery.proto.BdlProto;
import com.bdl.airecovery.proto.CountDownProtoUtil;

import org.xutils.DbManager;
import org.xutils.ex.DbException;

import java.net.ConnectException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 发送校准当前时间的请求
 */
public class SendReqOfCntTimeUtil {
    private int deviceId;   //设备ID
    private BdlProto.DeviceType deviceType; //设备类型
    private String UUID;    //系统设置的UUID
    private Integer seq = 1;        //消息序列号
    public Timer timer = new Timer();
    public TimerTask timerTask; //发生校准当前时间请求的TimerTask


    public void SendRequestOfCurrentTime() {

        //获取需要的请求打包参数（打包参数需要当前设备类型与ID）
        if (MyApplication.getInstance().getCurrentDevice() == null || MyApplication.getInstance().getCurrentDevice().getDeviceInnerID() == null) {
            return;
        }
        deviceId = Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getDeviceInnerID());
        //如果是跑步机12，改为健身车16（兼容时间服务器）
        if(deviceId == 12) {
            deviceId = 16;
        }
        deviceType = BdlProto.DeviceType.forNumber(deviceId);

        try {
            DbManager db = MyApplication.getInstance().getDbManager();
            Setting setting = db.findFirst(Setting.class);
            UUID = setting.getUUID();
        } catch (DbException e) {
            e.printStackTrace();
        }

        //设置TimerTask
        timerTask = new TimerTask() {
            @Override
            public void run() {
                //BasicConfigurator.configure();
                //请求当前时间
                BdlProto.CurrentTimeRequest request = BdlProto.CurrentTimeRequest.newBuilder()
                        .setDeviceType(deviceType)
                        .setDeviceId(UUID)
                        .setClientTime(String.valueOf(new Date()))
                        .build();
                //控制流水号清0
                if(seq == Integer.MAX_VALUE) {
                    seq = 1;
                }
                //包装Message
                BdlProto.Message message = CountDownProtoUtil.packCurrentTimeRequest(seq++, request);
                try {
                    //发送更新个人设置信息，一旦收到响应会在CountDownSocketListener中触发相应的条件分支
                    //Log.d("同步倒计时","发送同步当前时间的请求,序列号："+(seq-1));
                    CountDownSocketClient.getInstance().sendMsg(message);
                } catch (ConnectException e) {
                    //网络连接失败的情况
                    Log.d("同步倒计时","出现异常"+(seq-1));
                    e.printStackTrace();
                }
            }
        };

        timer.schedule(timerTask, 500, 2000);

    }
}
