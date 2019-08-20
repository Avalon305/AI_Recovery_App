package com.bdl.airecovery.bluetooth;


/*
 此对象用于activity发的命令
 */
public enum CommonCommand {
//    FIRST__LOGIN("FIRST__LOGIN"), //第一用户登录
//    LOGOUT("LOGOUT"), //第一用户退出
//    SECOND__LOGIN("SECOND__LOGIN"), //第二用户登录
//    SECOND__LOGOUT("SECOND__LOGOUT"),//第二用户退出
//    ALL__LOGOUT("ALL__LOGOUT"),//待机页面大退
//    CARD_STOP_ACCEPT("CARD_STOP_ACCEPT");//蓝牙专用，扫描到合适设备之后，发指令给发卡器service，让其不再接受数据。
    LOGIN("LOGIN"),  //登录
    LOGOUT("LOGOUT"); //登出

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
