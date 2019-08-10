package com.bdl.airecovery.entity.login;

/**
 * helpuser 一定是教练，可能存在不同的登录类型
 */

public class Helperuser {

    //登录类型（串口/蓝牙  serialport/bluetooth）
    private String type ;
    //用户手机号后四位
    private String phone ;
    //用户姓名
    private String username ;
    //用户标识
    private String userid;

    public Helperuser() {
    }

    public Helperuser(String type, String phone, String username, String userid) {
        this.type = type;
        this.phone = phone;
        this.username = username;
        this.userid = userid;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "Helperuser{" +
                "type='" + type + '\'' +
                ", phone='" + phone + '\'' +
                ", username='" + username + '\'' +
                ", userid='" + userid + '\'' +
                '}';
    }
}
