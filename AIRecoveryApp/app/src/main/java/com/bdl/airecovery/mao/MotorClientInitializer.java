package com.bdl.airecovery.mao;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;


public class MotorClientInitializer extends ChannelInitializer<SocketChannel> {

    public MotorClientInitializer(CountDownLatch latch) {
        this.latch = latch;
    }

    private CountDownLatch latch; //同步锁

    private MotorClientHandler handler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        handler = new MotorClientHandler(latch);
        ch.pipeline().addLast(new FixedLengthFrameDecoder(28));
        ch.pipeline().addLast(new IdleStateHandler(0, 500, 0, TimeUnit.MILLISECONDS));
        ch.pipeline().addLast(handler);
    }

    /**
     * 获取服务端的响应
     *
     * @return
     */
    public byte[] getServerResponse() {
        return handler.getResponseMsg();
    }

    /**
     * 重新上锁
     *
     * @param latch
     */
    public void resetLatch(CountDownLatch latch) {
        handler.resetLatch(latch);
    }
}
