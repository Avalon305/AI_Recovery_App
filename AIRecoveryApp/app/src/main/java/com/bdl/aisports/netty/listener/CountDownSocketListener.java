package com.bdl.aisports.netty.listener;


import com.bdl.aisports.MyApplication;
import com.bdl.aisports.entity.CurrentTime;
import com.bdl.aisports.netty.CountDownSocketClient;
import com.bdl.aisports.proto.BdlProto;

import org.apache.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class CountDownSocketListener extends ChannelInboundHandlerAdapter {
    private static final Logger logger = Logger.getLogger(CountDownSocketClient.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        BdlProto.Message message = (BdlProto.Message) msg;
        //收到心跳响应
        if (message.hasKeepaliveResponse()) {
            logger.info("CountDownSocket客户端收到心跳响应:"+message.toString());
            BdlProto.KeepaliveResponse keepResp =message.getKeepaliveResponse();
        }
        //收到倒计时广播请求
        if (message.hasCountDownBroadCast()) {
            logger.info("CountDownSocket客户端收到倒计时广播请求:" + message);
            //这就是给安卓留的接口，在这里处理倒计时逻辑，倒计时信息都在下面这个broadCast对象里
            BdlProto.CountDownBroadCast broadCast = message.getCountDownBroadCast();
            //目前用不到
        }
        //收到当前时间响应
        if (message.hasCurrentTimeResponse()){
            logger.info("CountDownSocket客户端收到当前时间响应："+message);
            //这就是留给安卓的接口，若要查询当前倒计时到第几秒了，在下面这个对象中
            BdlProto.CurrentTimeResponse currentTimeResp = message.getCurrentTimeResponse();
            //获取当前秒数与时间类型，传给全局变量currentTime
            if (MyApplication.getCurrentTime() != null && MyApplication.getCurrentTime().getType() == -1) {
                if (currentTimeResp.getCountDownType() == BdlProto.CountDownType.STOP) {
                    MyApplication.setCurrentTime(new CurrentTime(currentTimeResp.getCurrentSeconds(), 1));
                } else {
                    MyApplication.setCurrentTime(new CurrentTime(currentTimeResp.getCurrentSeconds(), 0));
                }
            }
        }


        //ctx.close(); 不要关闭通道，保持长连接

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("SocketClient捕获到全局异常");
        logger.error(cause.getMessage(), cause);
        cause.printStackTrace();
        ctx.close();
    }

}
