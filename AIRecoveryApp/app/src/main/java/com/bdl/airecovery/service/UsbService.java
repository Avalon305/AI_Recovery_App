package com.bdl.airecovery.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
/**
 * author : zfc
 * date   : 2019/8/20
 * desc   :
 */
public class UsbService extends Service {
    private  String bind_id; //USB传输的数据解码后的字符串
    private UsbManager mUsbManager = null; //负责管理USB设备
    private static final String ACTION_DEVICE_PERMISSION = "com.linc.USB_PERMISSION";
    private PendingIntent mPermissionIntent; //权限intent
    private UsbEndpoint mUsbEndpointIn; //一个接口的输入节点
    private UsbEndpoint mUsbEndpointOut; //一个接口的输出节点
    private UsbInterface mUsbInterface;   //USB设备的一个接口（物理接口）
    private UsbDeviceConnection mUsbDeviceConnection; //USB连接,可以想USB设备发送和接收数据
    private Thread mReadingthread = null; //持续读数据的线程
    private boolean isReading = false; //锁





    //获取设备权限广播
    private final BroadcastReceiver mUsbPermissionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_DEVICE_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            initDevice(device);
                        }
                    }
                }
            }
        }
    };

    //获取usb插拔广播
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {   // 插入
                searchUsb();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {  // 拔出
                closeUsbService();
            }
        }
    };

    //搜索usb设备
    private void searchUsb() {
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> devices = mUsbManager.getDeviceList();
        Iterator<UsbDevice> iterator = devices.values().iterator();
        while (iterator.hasNext()) {
            UsbDevice device = iterator.next();

            if (mUsbManager.hasPermission(device)) {
                initDevice(device);
            } else {
                mUsbManager.requestPermission(device, mPermissionIntent);
            }
        }
    }


    //初始化设备
    private void initDevice(UsbDevice device) {
        UsbInterface usbInterface = device.getInterface(0);
        UsbEndpoint ep = usbInterface.getEndpoint(0);
        if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {

            if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                mUsbEndpointIn = ep;
            } else {
                mUsbEndpointOut = ep;
            }
            if ((null == mUsbEndpointIn)) {
                mUsbEndpointIn = null;
                mUsbInterface = null;
            } else {
                mUsbInterface = usbInterface;
                mUsbDeviceConnection = mUsbManager.openDevice(device);

                startReading();
            }
        }
    }


    //开线程读取数据
    private void startReading() {
        mUsbDeviceConnection.claimInterface(mUsbInterface, true);

        isReading = true;

        final StringBuffer qr = new StringBuffer();

        mReadingthread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isReading) {
                    synchronized (this) {
                        byte[] bytes = new byte[mUsbEndpointIn.getMaxPacketSize()];
                        int ret = mUsbDeviceConnection.bulkTransfer(
                                mUsbEndpointIn,
                                bytes,
                                bytes.length,
                                100);


                        if (ret > 0) {
                            StringBuilder stringbuilder = new StringBuilder(bytes.length);
                            for (byte b : bytes) {
                                if (b != 0) {
                                    if (b == 2) {
                                        stringbuilder.append("da");
                                    }
                                    stringbuilder.append(Integer.toHexString(b));

                                }
                            }


                            //最终处理数据
                            Log.d("usbService", stringbuilder.toString());

                            bind_id=stringbuilder.toString();
                            //发送广播
                            Intent intent = new Intent();
                            intent.putExtra("bind_id", bind_id);
                            intent.setAction("com.bdl.airecovery.service.UsbService");
                            sendBroadcast(intent);
                        }
                    }

                }
                mUsbDeviceConnection.close();
            }


        });


        mReadingthread.start();
    }


    //关闭usb服务
    private void closeUsbService(){
        if(isReading == true){
            isReading = false;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("usbService","on onCreate");

        //注册插拔广播
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, usbFilter);

        //注册usb权限广播
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_DEVICE_PERMISSION), 0);
        IntentFilter permissionFilter = new IntentFilter(ACTION_DEVICE_PERMISSION);
        registerReceiver(mUsbPermissionReceiver, permissionFilter);

        searchUsb();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bind_id="";
        Log.d("usbService","on destroy");
    }


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
