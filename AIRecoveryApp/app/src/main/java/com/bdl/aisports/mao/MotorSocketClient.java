package com.bdl.aisports.mao;

import android.util.Log;

import com.bdl.aisports.mao.MotorClientInitializer;
import com.bdl.aisports.util.CodecUtils;
import com.google.gson.internal.bind.TimeTypeAdapter;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.ConnectException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class MotorSocketClient {

    private static MotorSocketClient nettyClient = new MotorSocketClient(); //EchoClient单例

    private static final String HOST = "192.168.5.10";          //服务端IP
    private static final int PORT = 9410;                       //服务端端口号
    private static final String TAG = "MotorSocketClient";      //日志输出
    private int maxRecNum = 5;

    //类成员变量
    private EventLoopGroup group;
    private CountDownLatch latch;
    private MotorClientInitializer initializer;
    private ChannelFuture cf;
    private Bootstrap bootstrap;
    private boolean waiting = true;

    private MotorSocketClient() {

    }

    /**
     * 初始化线程池并尝试连接
     *
     * @throws Exception
     */
    public void start() throws InterruptedException, ConnectException {
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        initializer = new MotorClientInitializer(latch);
        bootstrap.group(group)
                //指定Nio
                .channel(NioSocketChannel.class)
                //保持长连接状态
                .option(ChannelOption.SO_KEEPALIVE, true)
                //不延迟，直接发送
                .option(ChannelOption.TCP_NODELAY, true)
                //指定handler
                .handler(initializer);
        doConnect();
    }

    /**
     * 尝试与服务端进行连接
     * 连接失败重连
     */
    private void doConnect() throws ConnectException {
        try {
            this.cf = bootstrap.connect(HOST, PORT).sync();
            Log.e(TAG,"服务端连接成功，可以进行电机控制");
        } catch (Exception e) {
            throw new ConnectException("远程服务器连接失败啊啊啊");
        }


//        if (cf.channel() != null && cf.channel().isActive()) {
//            return;
//        }
//        cf.addListener(new ChannelFutureListener() {
//            @Override
//            public void operationComplete(ChannelFuture futureListener) throws Exception {
//                if (futureListener.isSuccess()) {
//                    Log.e(TAG, "与服务端连接成功");
//                } else {
//                    Log.e(TAG, "连接失败，开始重连...");
//                    Log.e(TAG, String.valueOf(maxRecNum));
//                    maxRecNum--;
//                    futureListener.channel().eventLoop().schedule(new Runnable() {
//                        @Override
//                        public void run() {
//                                doConnect();
//                        }
//                    }, 3, TimeUnit.SECONDS);
//                    if (maxRecNum <= 0) {
//                        throw new StopMsgException();
//                    }
//                }
//            }
//        });
    }
    /**
     * 从客户端发送消息，并接受服务端返回的数据
     *
     * @param message
     * @return
     * @throws Exception
     */
    public byte[] sendMessage(byte[] message) throws Exception {
        synchronized (this) {
            if (cf.channel().isActive() && cf.channel() != null) {
                ByteBuf byteBuf = cf.channel().alloc().buffer(); //初始化byteBuf
                byteBuf.writeBytes(message); //将byte[]数组转换为ByteBuf类型
                cf.channel().writeAndFlush(byteBuf); //写入并刷新
                latch = new CountDownLatch(1);
                initializer.resetLatch(latch);
                latch.await(200, TimeUnit.MILLISECONDS);
            } else {
                start();
            }
            return initializer.getServerResponse();
        }
    }

    /**
     * 断开连接的方法
     */
    private void disconnect() {
        //优雅地关闭
        group.shutdownGracefully();
        cf.channel().close();
    }


    /**
     * 获得单例
     *
     * @return nettyClient
     */
    public static MotorSocketClient getInstance() throws Exception {
        return nettyClient;
    }

}
