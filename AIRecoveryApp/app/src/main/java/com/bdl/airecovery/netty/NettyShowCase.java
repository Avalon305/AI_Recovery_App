package com.bdl.airecovery.netty;

import com.bdl.airecovery.proto.BdlProto;
import com.bdl.airecovery.proto.CountDownProtoUtil;

import java.net.ConnectException;
import java.util.UUID;

/**
 * netty通信接口示例，此类无实际意义
 */
public class NettyShowCase {
    /*
    BdlProto这个类不要修改，是Protobuf自动生成的，所有的通信对象到会包装为BdlProto.Message对象
    通过  CountDownSocketClient.getInstance().sendMsg(message);或者  DataSocketClient.getInstance().sendMsg(message);发送
    CountDownSocketClient是与时间服务器通信的类，DataSocketClient是与教练机通信的
     */

    /*public static void main(String[] args) {
        //发送心跳请求示例
        //.setDeviceId(UUID.randomUUID().toString()) 所有的报文中，设备ID这一属性不是必须的，若要设置应保证每台设备唯一且不会变化，暂时保留这个字段没有用到。
        BdlProto.KeepaliveRequest request = BdlProto.KeepaliveRequest.newBuilder().setDeviceType(BdlProto.DeviceType.E13)
                .setDeviceId(UUID.randomUUID().toString()).setClientTime("2019-01-26 11:11:11").build();

        //所有的请求都要用这种XXProtoUtil打包，避免手动设置粗心缺少相关属性
        //目前有CountDownProtoUtil和DataProtoUtil两个打包工具类
        //打包方法的第一个参数seq是消息序列号，每个请求递增，当达到 Integer.MAX_VALUE时归零重新计数，调用时自行处理
        BdlProto.Message message = CountDownProtoUtil.packKeepaliveRequest(1,request);
        try {
            //发送心跳信息，一旦收到响应会在CountDownSocketListener中触发相应的条件分支
            CountDownSocketClient.getInstance().sendMsg(message);
        } catch (ConnectException e) {
            //TODO 处理网络连接失败的情况
            e.printStackTrace();
        }
    }*/
}
