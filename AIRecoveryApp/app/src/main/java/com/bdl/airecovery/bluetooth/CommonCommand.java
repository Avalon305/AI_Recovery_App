package com.bdl.airecovery.bluetooth;


/*
 此对象用于activity发的命令
 */
public enum CommonCommand {
    LOGIN("LOGIN"),  //开启蓝牙扫描，自动连接附近设备。若成功会发送提示广播，并连续广播心率。
    LOGOUT("LOGOUT"); //断开蓝牙连接

    private String value = "";

    private CommonCommand(String value) {     //必须是private的，否则编译错误
        this.value = value;
    }

    public static CommonCommand getEnumByString(String value) {    //手写的从string到enum的转换函数
        switch (value) {
            case "LOGIN":
                return LOGIN;
            case "LOGOUT":
                return LOGOUT;
            default:
                return null;
        }
    }

    public String value() {
        return this.value;
    }
}
