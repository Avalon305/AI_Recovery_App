package com.bdl.airecovery.util;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import android_serialport_api.SerialPort;


public class SerialPortUtils {
    private final String TAG = "SerialPortUtils";
    private String path = "/dev/ttymxc2";
    private int baudrate = 9600;
    public boolean serialPortStatus = false; //是否打开串口标志
    public String data_;
    public boolean threadStatus; //线程状态，为了安全终止线程

    public SerialPort serialPort = null;
    public InputStream inputStream = null;
    public OutputStream outputStream = null;


    public SerialPortUtils(String port,int baudrate){
        this.path = port;
        this.baudrate = baudrate;
    }
    /**
     * 打开串口
     * @return serialPort串口对象
     */
    public SerialPort openSerialPort(){
        try {
            serialPort = new SerialPort(new File(path),baudrate,0);
            this.serialPortStatus = true;
            threadStatus = false; //线程状态

            //获取打开的串口中的输入输出流，以便于串口数据的收发
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();

            new ReadThread().start(); //开始线程监控是否有数据要接收
        } catch (IOException e) {
            Log.e(TAG, "openSerialPort: 打开串口异常：" + e.toString());
            return serialPort;
        }
        Log.d(TAG, "openSerialPort: 打开串口");
        return serialPort;
    }

    /**
     * 关闭串口
     */
    public void closeSerialPort(){
        try {
            inputStream.close();
            outputStream.close();

            this.serialPortStatus = false;
            this.threadStatus = true; //线程状态
            serialPort.close();
        } catch (IOException e) {
            Log.e(TAG, "closeSerialPort: 关闭串口异常："+e.toString());
            return;
        }
        Log.d(TAG, "closeSerialPort: 关闭串口成功");
    }

    /**
     * 发送串口指令（字符串）
     * @param data String数据指令
     */
    public void sendSerialPort(String data){
        Log.d(TAG, "sendSerialPort: 发送数据（字符串）");

        try {
            byte[] sendData = data.getBytes(); //string转byte[]
            this.data_ = new String(sendData); //byte[]转string
            if (sendData.length > 0) {
                outputStream.write(sendData);
                outputStream.write('\n');
                //outputStream.write('\r'+'\n');
                outputStream.flush();
                Log.d(TAG, "sendSerialPort: 串口数据发送成功");
            }
        } catch (IOException e) {
            Log.e(TAG, "sendSerialPort: 串口数据发送失败："+e.toString());
        }

    }

    /**
     * 发送串口指令（字节数组）
     * @param data byte[]数据指令
     */
    public void sendByteArray(byte[] data){
        Log.d(TAG, "sendSerialPort: 发送数据（字节数组）");

        try {
            byte[] sendData = data; //string转byte[]
//            this.data_ = new String(sendData); //byte[]转string
            if (sendData.length > 0 && outputStream != null) {
                outputStream.write(sendData);
                outputStream.flush();
                Log.d(TAG, "sendSerialPort: 串口数据发送成功");
            }
        } catch (IOException e) {
            Log.e(TAG, "sendSerialPort: 串口数据发送失败："+e.toString());
        }

    }

    /**
     * 发送串口指令（字节）
     * @param data byte数据指令
     */
    public void sendByte(byte data){
        Log.d(TAG, "sendSerialPort: 发送数据（字节）");

        try {
            byte sendData = data; //string转byte[]
            outputStream.write(sendData);
            outputStream.flush();
            Log.d(TAG, "sendSerialPort: 串口数据发送成功");
        } catch (IOException e) {
            Log.e(TAG, "sendSerialPort: 串口数据发送失败："+e.toString());
        }

    }

//    /**
//     * 单开一线程，来读数据
//     */
//    private class ReadThread2 extends Thread{
//        @Override
//        public void run() {
//            super.run();
//            //判断进程是否在运行，更安全的结束进程
//            while (!threadStatus){
////                Log.d(TAG, "进入线程run");
//                //64   1024
//                try{
//                    Thread.sleep(600);
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//                byte[] buffer = new byte[64];
//                int size; //读取数据的大小
//                try {
//                    size = inputStream.read(buffer);
//                    if (size > 0){
//                        Log.d(TAG, "run: 接收到了数据大小：" + String.valueOf(size));
//                        onDataReceiveListener.onDataReceive(buffer,size);
//                    }
//                } catch (IOException e) {
//                    Log.e(TAG, "run: 数据读取异常：" +e.toString());
//                }
//            }
//        }
//    }
//
//    /**
//     * 单开一线程，来读数据
//     */
//    private class ReadThread extends Thread{
//        @Override
//        public void run() {
//            super.run();
//            //判断进程是否在运行，更安全的结束进程
//            while (!threadStatus){
////                Log.d(TAG, "进入线程run");
//                //64   1024
////                try{
////                    Thread.sleep(600);
////                }catch (Exception e){
////                    e.printStackTrace();
////                }
//                byte[] firstBuffer = new byte[64];
//                int firstSize;
//                try{
//                    firstSize = inputStream.read(firstBuffer);
//                    Log.d("静态电机", "firstsize: " + firstSize + "   firstbuffer:" + Arrays.toString(firstBuffer));
//                    if (firstSize > 0) {
//                        try {
//                            Thread.sleep(600);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        byte[] secondBuffer = new byte[64];
//                        int secondSize;
//                        try {
//                            secondSize = inputStream.read(secondBuffer);
//                            Log.d("静态电机", "secondsize: "+secondSize+"   secondbuffer:"+Arrays.toString(secondBuffer));
//                            if (secondSize > 0){
//                                byte[] buffer = new byte[64];
//                                int size = firstSize + secondSize;
//                                for (int i = 0; i < firstSize; i++) {
//                                    buffer[i] = firstBuffer[i];
//                                }
//                                for (int i = 0; i < secondSize; i++) {
//                                    buffer[i + firstSize] = secondBuffer[i];
//                                }
//                                Log.d("静态电机", "size: "+size+"   buffer:"+Arrays.toString(buffer));
//                                Log.d(TAG, "run: 接收到了数据大小：" + String.valueOf(size));
//                                onDataReceiveListener.onDataReceive(buffer,size);
//                            }
//                        } catch (IOException e) {
//                            Log.e(TAG, "run: 数据读取异常：" +e.toString());
//                        }
//
//                    }
//                }catch (IOException e){
//                    e.printStackTrace();
//                }
//
//
//
////                byte[] buffer = new byte[64];
////                int size; //读取数据的大小
////                try {
////                    size = inputStream.read(buffer);
////                    if (size > 0){
////                        Log.d(TAG, "run: 接收到了数据大小：" + String.valueOf(size));
////                        onDataReceiveListener.onDataReceive(buffer,size);
////                    }
////                } catch (IOException e) {
////                    Log.e(TAG, "run: 数据读取异常：" +e.toString());
////                }
//            }
//
//        }
//    }

    /**
     * 单开一线程，来读数据
     */
    private class ReadThread extends Thread{
        @Override
        public void run() {
            super.run();
            //判断进程是否在运行，更安全的结束进程
            while (!threadStatus) {
                Log.d(TAG, "进入线程run");
                byte[] buffer = new byte[64];
                int size;
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    Log.d(TAG, "ready to read");
                    size = inputStream.read(buffer);
                    Log.d(TAG, "size: "+size);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    int moreSize = inputStream.read(buffer, size, 64 - size);
                    if (moreSize > 0) {
                        size += moreSize;
                    }
                    if (size > 0) {
                        Log.d(TAG, "size: " + size + "   buffer:" + Arrays.toString(buffer));
                        Log.d(TAG, "run: 接收到了数据大小：" + String.valueOf(size));
                        onDataReceiveListener.onDataReceive(buffer, size);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

//    /**
//     * 单开一线程，来读数据 静态电机用
//     */
//    private class ReadThread extends Thread{
//        @Override
//        public void run() {
//            super.run();
//            //判断进程是否在运行，更安全的结束进程
//            byte[] buffer = new byte[64];
//            byte[] FLAG = {0x55, (byte) 0xAA};
//            byte[] HEAD = {0,0};
//            int LEN = 0;
//            int size; //读取数据的大小
//            int index = 0;
//            int contentIndex = 0;
//            while (!threadStatus){
////                Log.d(TAG, "进入线程run");
//                //64   1024
////                int newByte = 0;
//                try {
//                    int newByte = inputStream.read();
//                    if (newByte != -1){
//                        switch (index){
//                            case 0:
//                            case 1:
//                                HEAD[index] = (byte) newByte;
//                                index++;
//                                break;
//                            case 2:
//                                if (HEAD == FLAG) {
//                                    buffer[0] = HEAD[0];
//                                    buffer[1] = HEAD[1];
//                                    buffer[2] = 0x00;
//                                    index++;
//                                }
//                                break;
//                            case 3:
//                                LEN = newByte + 3;
//                                buffer[3] = (byte) newByte;
//                                index++;
//                                break;
//                            case 4:
//                                if (contentIndex < LEN) {
//                                    buffer[contentIndex + 4] = (byte) newByte;
//                                    contentIndex++;
//                                } else {
//                                    size = LEN+4;
//                                    Log.d(TAG, "run: 接收到了数据大小：" + String.valueOf(size));
//                                    onDataReceiveListener.onDataReceive(buffer,size);
//                                    index = 0;
//                                    contentIndex = 0;
//                                    LEN = 0;
//                                }
//                                break;
//                            default:
//                                break;
//                        }
//                    }
////                    if (size > 0){
////                        Log.d(TAG, "run: 接收到了数据大小：" + String.valueOf(size));
////                        onDataReceiveListener.onDataReceive(buffer,size);
////                    }
//                } catch (IOException e) {
//                    Log.e(TAG, "run: 数据读取异常：" +e.toString());
//                }
//            }
//        }
//    }

    //这是写了一监听器来监听接收数据
    public OnDataReceiveListener onDataReceiveListener = null;
    public static interface OnDataReceiveListener {
        public void onDataReceive(byte[] buffer, int size);
    }
    public void setOnDataReceiveListener(OnDataReceiveListener dataReceiveListener) {
        onDataReceiveListener = dataReceiveListener;
    }

}
