package com.bdl.airecovery.entity.login;

import com.bdl.airecovery.biz.LoginUtils;
import com.bdl.airecovery.proto.BdlProto;

public class User {
    //用户标识
    private String userId ;

    //用户姓名
    private String username;

    //待训练设备列表
    private String deviceTypearrList;

    //是否存在医护设置
    private boolean exisitSetting;

    //训练模式
    private String trainMode;

    //移乘方式
    private int moveWay;

    //医生指示建议
    private String memo;

    //目标组数
    private int groupCount;

    //每组运动个数
    private int groupNum;

    //每组间隔休息时间
    private int relaxTime;

    //运动速度等级
    private int speedRank;

    //系统版本
    private String sysVersion;

    //体重
    private double weight;

    //年龄
    private int age;

    //最大心率
    private int heartRatemMax;

    //手环id

    private String bindId;

    //处方id
    private  int dpId;

    //客户端时间戳
    private String clientTime ;


    public User() {

    }
    public User(BdlProto.Message message) {
        //用户标识
        this.userId = message.getLoginResponse().getUid();
        //带训练设备列表
        this.deviceTypearrList = String.valueOf(message.getLoginResponse().getDeviceTypeArrList());
        //是否存在医护设置
        this.exisitSetting = message.getLoginResponse().getExisitSetting();
        //训练模式
        switch (message.getLoginResponse().getTrainMode()) {
            case RehabilitationModel:
                this.trainMode = "康复模式";
                break;
            case ActiveModel:
                this.trainMode = "主动模式";
                break;
            case PassiveModel:
                this.trainMode = "被动模式";
                break;
            default:
                break;
        }
        //移乘方式
        this.moveWay = message.getLoginResponse().getDpMoveway();
        //医生指示建议
        this.memo = message.getLoginResponse().getDpMemo();
        //目标组数
        this.groupCount = message.getLoginResponse().getDpGroupcount();
        //每组运动个数
        this.groupNum = message.getLoginResponse().getDpGroupnum();
        //每组间隔休息时间
        this.relaxTime = message.getLoginResponse().getDpRelaxtime();
        //运动速度等级
        this.speedRank = message.getLoginResponse().getSpeedRank();
        //系统版本
        this.sysVersion = message.getLoginResponse().getSysVersion();
        //体重
        this.weight = message.getLoginResponse().getWeight();
        //年龄
        this.age = message.getLoginResponse().getAge();
        //最大心率
        this.heartRatemMax = 220 - this.age;
        //处方id
        this.dpId=message.getLoginResponse().getDpId();
        //用户姓名
        this.username=message.getLoginResponse().getUserName();
        //客户端时间
        this.clientTime=message.getLoginResponse().getClientTime();

    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDeviceTypearrList() {
        return deviceTypearrList;
    }

    public void setDeviceTypearrList(String deviceTypearrList) {
        this.deviceTypearrList = deviceTypearrList;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getBindId() {
        return bindId;
    }

    public void setBindId(String bindId) {
        this.bindId = bindId;
    }

    public int getDpId() {
        return dpId;
    }

    public void setDpId(int dpId) {
        this.dpId = dpId;
    }

    public boolean isExisitSetting() {
        return exisitSetting;
    }

    public void setExisitSetting(boolean exisitSetting) {
        this.exisitSetting = exisitSetting;
    }

    public String getTrainMode() {
        return trainMode;
    }

    public void setTrainMode(String trainMode) {
        this.trainMode = trainMode;
    }

    public int getMoveWay() {
        return moveWay;
    }

    public void setMoveWay(int moveWay) {
        this.moveWay = moveWay;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public int getGroupCount() {
        return groupCount;
    }

    public void setGroupCount(int groupCount) {
        this.groupCount = groupCount;
    }

    public int getGroupNum() {
        return groupNum;
    }

    public void setGroupNum(int groupNum) {
        this.groupNum = groupNum;
    }

    public int getRelaxTime() {
        return relaxTime;
    }

    public void setRelaxTime(int relaxTime) {
        this.relaxTime = relaxTime;
    }

    public int getSpeedRank() {
        return speedRank;
    }

    public void setSpeedRank(int speedRank) {
        this.speedRank = speedRank;
    }

    public String getSysVersion() {
        return sysVersion;
    }

    public void setSysVersion(String sysVersion) {
        this.sysVersion = sysVersion;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getHeartRatemMax() {
        return heartRatemMax;
    }

    public void setHeartRatemMax(int heartRatemMax) {
        this.heartRatemMax = heartRatemMax;
    }

    public String getClientTime() {
        return clientTime;
    }

    public void setClientTime(String clientTime) {
        this.clientTime = clientTime;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", deviceTypearrList='" + deviceTypearrList + '\'' +
                ", exisitSetting=" + exisitSetting +
                ", trainMode='" + trainMode + '\'' +
                ", moveWay=" + moveWay +
                ", memo='" + memo + '\'' +
                ", groupCount=" + groupCount +
                ", groupNum=" + groupNum +
                ", relaxTime=" + relaxTime +
                ", speedRank=" + speedRank +
                ", sysVersion='" + sysVersion + '\'' +
                ", weight=" + weight +
                ", age=" + age +
                ", heartRatemMax=" + heartRatemMax +
                ", bindId='" + bindId + '\'' +
                ", dpId=" + dpId +
                ", clientTime='" + clientTime + '\'' +
                '}';
    }
}
