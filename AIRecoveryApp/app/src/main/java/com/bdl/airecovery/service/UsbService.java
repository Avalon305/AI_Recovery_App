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

import java.text.DecimalFormat;
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

    private final int VendorID = 8746;
    private final int ProductID = 10;



    //搜索usb设备
    private void searchUsb() {
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if(mUsbManager==null){
            Log.d("usbservice","mUsbManager为空");
        }
        HashMap<String, UsbDevice> devices = mUsbManager.getDeviceList();
        if(!devices.isEmpty()) {

            Iterator<UsbDevice> iterator = devices.values().iterator();
            while (iterator.hasNext()) {
                UsbDevice device = iterator.next();

                Log.d("usbService", "DeviceInfo: v" + device.getVendorId() + " , p"
                        + device.getProductId());

                if(device.getVendorId() ==VendorID
                        && device.getProductId()==ProductID){

                    initDevice(device);
                    Log.d("usbService", "枚举设备成功");
                }
//                if (mUsbManager.hasPermission(device)) {
//
//                } else {
//                    mUsbManager.requestPermission(device, mPermissionIntent);
//                }
            }
        }
    }

    //初始化设备
    private void initDevice(UsbDevice device) {
        if(device!=null) {
            Log.d("usbService","usbInterface count"+device.getInterfaceCount());

            for(int i=0;i<device.getInterfaceCount();i++){

                UsbInterface usbInterface = device.getInterface(i);
                Log.d("usbservice","每个接口的属性"+usbInterface.getInterfaceClass()
                        +","+usbInterface.getInterfaceSubclass()
                        +","+usbInterface.getInterfaceProtocol()
                        +","
                        +"," );
                if(usbInterface.getInterfaceClass()==3
                        && usbInterface.getInterfaceSubclass()==0
                        &&  usbInterface.getInterfaceProtocol()==0
                        ){
                    mUsbInterface=usbInterface;

                    Log.d("usbService", "找到我的设备接口");
                    assignEndpoint(mUsbInterface);
                    openDevice(device);

                }
            }
        }
    }

    /**
     * 分配端点，IN | OUT，即输入输出；此处我直接用1为OUT端点，0为IN，当然你也可以通过判断
     */
    private void assignEndpoint(UsbInterface myInterface) {
        if (myInterface.getEndpoint(1) != null) {
            mUsbEndpointOut = myInterface.getEndpoint(1);
        }
        if (myInterface.getEndpoint(0) != null) {
            mUsbEndpointIn = myInterface.getEndpoint(0);
        }

        Log.d("usbService", "分配断点成功");
    }

    //打开设备
    public void openDevice(UsbDevice device){
           if(mUsbInterface!=null){
               UsbDeviceConnection conn = null;
               // 在open前判断是否有连接权限；对于连接权限可以静态分配，也可以动态分配权限，可以查阅相关资料
               if (mUsbManager.hasPermission(device)) {
                   conn = mUsbManager.openDevice(device);
               }else {
                   mUsbManager.requestPermission(device, mPermissionIntent);
                   Log.d("usbservice","没有权限");
               }

               if (conn == null) {

                   Log.d("usbservice","代码129");
               }
               if(conn.claimInterface(mUsbInterface,true)){
                   mUsbDeviceConnection =conn;
                   Log.d("usbservice","打开设备成功");
                   startReading();
               }
           }

    }

    //开线程读取数据
    private void startReading() {
        isReading = true;
        final StringBuffer qr = new StringBuffer();
Log.d("usbservice","即将进入线程");
        mReadingthread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isReading) {
                    synchronized (this) {
                        Log.d("usbService","已经进入线程");
                        byte[] bytes = new byte[mUsbEndpointIn.getMaxPacketSize()];
                        Log.d("usbService","字节数组"+bytes.length);
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
                            Log.d("usbService最终数据", stringbuilder.toString());

//                            bind_id=stringbuilder.toString();
//                            //发送广播
//                            Intent intent = new Intent();
//                            intent.putExtra("bind_id", bind_id);
//                            intent.setAction("com.bdl.airecovery.service.UsbService");
//                            sendBroadcast(intent);
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
        Log.i("usbService","on onCreate");

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
