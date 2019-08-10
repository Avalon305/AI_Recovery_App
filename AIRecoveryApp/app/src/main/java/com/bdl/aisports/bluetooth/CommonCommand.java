package com.bdl.aisports.bluetooth;


/*
 此对象用于activity发的命令
 */
public enum CommonCommand {
    FIRST__LOGIN("FIRST__LOGIN"), //第一用户登录
    FIRST__LOGOUT("FIRST__LOGOUT"), //第一用户退出
    SECOND__LOGIN("SECOND__LOGIN"), //第二用户登录
    SECOND__LOGOUT("SECOND__LOGOUT"),//第二用户退出
    ALL__LOGOUT("ALL__LOGOUT"),//待机页面大退
    CARD_STOP_ACCEPT("CARD_STOP_ACCEPT");//蓝牙专用，扫描到合适设备之后，发指令给发卡器service，让其不再接受数据。


    private String value = "";

    private CommonCommand(String value) {     //必须是private的，否则编译错误
        this.value = value;
    }

    public static CommonCommand getEnumByString(String value) {    //手写的从string到enum的转换函数
        switch (value) {
            case "FIRST__LOGIN":
                return FIRST__LOGIN;
            case "FIRST__LOGOUT":
                return FIRST__LOGOUT;
            case "SECOND__LOGIN":
                return SECOND__LOGIN;
            case "SECOND__LOGOUT":
                return SECOND__LOGOUT;
            case "ALL__LOGOUT":
                return ALL__LOGOUT;
            case "CARD_STOP_ACCEPT":
                return CARD_STOP_ACCEPT;
            default:
                return null;
        }
    }

    public String value() {
        return this.value;
    }
}
