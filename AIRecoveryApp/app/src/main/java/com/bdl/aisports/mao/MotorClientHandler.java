package com.bdl.aisports.mao;

import android.util.Log;

import com.bdl.aisports.constant.MotorConstant;
import com.bdl.aisports.util.CodecUtils;
import com.bdl.aisports.util.MessageUtils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MotorClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final String TAG = "MotorClientHandler";

    private CountDownLatch latch; //锁

    private byte[] responseMsg; //服务器返回的信息

    public MotorClientHandler( CountDownLatch latch) {
        this.latch = latch;
    }

    /**
     * 建立连接后此方法被调用一次
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Log.e(TAG, "channelActive......");
    }

    /**
     * 此方法会在接收到数据时被调用
     *
     * @param channelHandlerContext
     * @param byteBuf
     * @throws Exception
     */
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        byte[] receiveMsgBytes = new byte[byteBuf.readableBytes()]; //接收数据的byte数组
        byteBuf.readBytes(receiveMsgBytes); //将byte数组读到byteBuf中
        responseMsg = receiveMsgBytes;
        latch.countDown(); //打开锁
    }

    /**
     * 心跳检测机制
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            switch (event.state()){
                case READER_IDLE:
                    break;
                case WRITER_IDLE:
                    ByteBuf byteBuf = ctx.alloc().buffer();
                    byteBuf.writeBytes(MotorConstant.READ_STATE);
                    ctx.writeAndFlush(ctx.channel().writeAndFlush(byteBuf));
                    break;
                case ALL_IDLE:
                    break;
            }
        }
    }

    /**
     * 异常处理
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 当管道断开连接时，进行重新连接
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Log.e(TAG, "连接断开，开始重连...");
        final EventLoop eventLoop = ctx.channel().eventLoop();
        eventLoop.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    MotorSocketClient.getInstance().start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 2, TimeUnit.SECONDS);

    }

    public void resetLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    public byte[] getResponseMsg() {
        return responseMsg;
    }
}
