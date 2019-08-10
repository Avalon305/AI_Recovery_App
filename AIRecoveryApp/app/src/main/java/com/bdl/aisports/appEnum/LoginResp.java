package com.bdl.aisports.appEnum;

/**
 * 由赵曰艺和郑畅道一起维护。该枚举类用于穷举登录广播的所有情况
 * 枚举类会作为广播对象传播。
 */
public enum LoginResp {
    REMOTEONELOGIBLUE("蓝牙第一用户教练机登录"),
    REMOTEONELOGOBLUE("蓝牙第一用户教练机离线"),
    REMOTETWOLOGIBLUE("蓝牙第二用户教练机登录"),
    REMOTETWOLOGOBLUE("蓝牙第二用户教练机离线"),
    REMOTEONELOGICARD("发卡器第一用户教练机登录"),
    REMOTEONELOGOCARD("发卡器第一用户教练机离线"),
    REMOTETWOLOGICARD("发卡器第二用户教练机登录"),
    REMOTETWOLOGOCARD("发卡器第二用户教练机离线"),
    LOCALONELOGIBLUE("蓝牙第一用户本地登录"),
    LOCALONELOGOBLUE("蓝牙第一用户本地离线"),
    LOCALTWOLOGIBLUE("蓝牙第二用户本地登录"),
    LOCALTWOLOGOBLUE("蓝牙第二用户本地离线"),
    LOCALONELOGICARD("发卡器第一用户本地登录"),
    LOCALONELOGOCARD("发卡器第一用户本地离线"),
    LOCALTWOLOGICARD("发卡器第二用户本地登录"),
    LOCALTWOLOGOCARD("发卡器第二用户本地离线"),
    NOSUCHPERSON("查无此人"),
    LOSTCOACH("连接不上教练机"),
    TRYRECONNECT("尝试重连"),
    RECONNECTED("用户重新连接"),
    LOCALNOPERSON("本地查无此人"),
    REMOTENOPERSON("教练机查无此人");

    private final String value;

    private LoginResp(String value)
    {
        this.value = value;
    }

    public String getStr() {
        return value;
    }
    public static LoginResp getEnumByStr(String value) {
        switch (value){
            case "蓝牙第一用户教练机登录" :
                return LoginResp.REMOTEONELOGIBLUE;
            case "蓝牙第一用户教练机离线" :
                return LoginResp.REMOTEONELOGOBLUE;
            case "蓝牙第二用户教练机登录" :
                return LoginResp.REMOTETWOLOGIBLUE;
            case "蓝牙第二用户教练机离线" :
                return LoginResp.REMOTETWOLOGOBLUE;
            case "发卡器第一用户教练机登录" :
                return LoginResp.REMOTEONELOGICARD;
            case "发卡器第一用户教练机离线" :
                return LoginResp.REMOTEONELOGOCARD;
            case "发卡器第二用户教练机登录" :
                return LoginResp.REMOTETWOLOGICARD;
            case "发卡器第二用户教练机离线" :
                return LoginResp.REMOTETWOLOGOCARD;
            case "蓝牙第一用户本地登录" :
                return LoginResp.LOCALONELOGIBLUE;
            case "蓝牙第一用户本地离线" :
                return LoginResp.LOCALONELOGOBLUE;
            case "蓝牙第二用户本地登录" :
                return LoginResp.LOCALTWOLOGIBLUE;
            case "蓝牙第二用户本地离线" :
                return LoginResp.LOCALTWOLOGOBLUE;
            case "发卡器第一用户本地登录" :
                return LoginResp.LOCALONELOGICARD;
            case "发卡器第一用户本地离线" :
                return LoginResp.LOCALONELOGOCARD;
            case "发卡器第二用户本地登录" :
                return LoginResp.LOCALTWOLOGICARD;
            case "发卡器第二用户本地离线" :
                return LoginResp.REMOTENOPERSON;
            case "查无此人" :
                return LoginResp.NOSUCHPERSON;
            case "连接不上教练机" :
                return LoginResp.LOSTCOACH;
            case "尝试重连" :
                return LoginResp.TRYRECONNECT;
            case "用户重新连接" :
                return LoginResp.RECONNECTED;
            case "本地查无此人" :
                return LoginResp.LOCALNOPERSON;
            case "教练机查无此人" :
                return LoginResp.REMOTENOPERSON;
            default:
                return null;
        }

    }
}