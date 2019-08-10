package com.zero.serialport.framework.port;

import android.util.Log;

import com.zero.serialport.framework.callback.ZeroCallback;
import com.zero.serialport.framework.data.ZeroData;
import com.zero.serialport.framework.decoder.ZeroDecoder;

import org.xutils.common.util.LogUtil;
import org.xutils.x;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android_serialport_api.SerialPort;

/**
  * @explain  强化串口类，封装了google-api。
  * @author zero.
  * @creat time 2019/4/14 7:21 PM.
  */

public class ZeroPort {

    public volatile SerialPort serialPort = null;
    public volatile InputStream inputStream = null;
    public volatile OutputStream outputStream = null;

    //回调方法由外界传入
    private volatile ZeroCallback callback;
    //解码器
    private volatile ZeroDecoder decoder;
    //线程是否启动标识，用于open方法
    private volatile boolean isRunning = false;


    //线程池,只有一个线程
    private ExecutorService threadPool = null;
    //串口读取停顿时间，默认100毫秒
    private int intervalTime = 100;
    //串口路径
    private String path;
    //波特率
    private int baudrate;

    /**
     * @explain  构造函数，回调与解析方法。回调在使用场景里面写，编码器可以业务不相关，读取间隔时间
     * @author zero.
     * @creat time 2019/4/14 8:54 PM.
     */
    public ZeroPort(ZeroCallback callback, ZeroDecoder decoder, int intervalTime,String path,
                    int baudrate) throws IOException {
        this.callback = callback;
        this.decoder = decoder;
        this.intervalTime = intervalTime;
        //串口名字，波特率
        this.baudrate = baudrate;
        this.path = path;
        //this.start();
        //serialPort = new SerialPort(new File(path),baudrate,0);
    }

    //private ReentrantLock lock = new ReentrantLock();

    //start方法默认打开串口，实现读取
    public void start(){
        //如果线程已经在运行了
        if (isRunning) {
            return;
        }
        LogUtil.d("before run");

            //如果进来了，设置为已经启动线程了
            isRunning = true;

            threadPool = Executors.newSingleThreadExecutor();
            //打开串口，获得输入输出流
            if (serialPort == null){
                try {
                    serialPort = new SerialPort(new File(path),baudrate,0);
                } catch (IOException e) {
                    LogUtil.e("打开串口异常！");
                }
            }
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
            try {
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        while(isRunning){
                            //读串口的逻辑，返回inputstream。
                            LogUtil.d("running 运行发生在："+Thread.currentThread().getName());
                            //读串口的逻辑结束
                            decoder.resolveInputstream(inputStream,callback);
                            //LogUtil.d("running end");
                        }
                    } catch (Exception e) {
                        //e.printStackTrace();
                        LogUtil.d("run方法执行异常。");
                    } finally {
                        //如果报错出意外，要考虑初始化，等待下次重启。
                        end();
                    }
                }
            });
        } catch (Exception e) {
            LogUtil.d("线程池执行异常。");
            //e.printStackTrace();
            end();
        }
    }


    /**
     * @explain  结束方法，初始化所有的状态
     * @author zero.
     * @creat time 2019/4/14 8:39 PM.
     */
    public void end(){
        threadPool.shutdownNow();

//        结束关闭数据的状态
//        isAccepted = false;
        //结束死循环，把线程归还给线程池，也就结束读取了。
        //这种写法的前提是，不会卡在解析方法
        isRunning = false;
        //isAccepted = false;
        LogUtil.d("end...");
        try {
            if (inputStream != null) {

                inputStream.close();
                //inputStream = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (outputStream != null) {
                outputStream.close();
                //outputStream = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (serialPort != null) {
                serialPort.close();
                //serialPort = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.gc();

    }

    /**
      * @explain  通过串口发送数据
      * @author zero.
      * @creat time 2019/4/14 8:41 PM.
      */
    public void send(byte[] bytes){
        byte[] sendData = bytes;
        try {
            outputStream.write(sendData);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
