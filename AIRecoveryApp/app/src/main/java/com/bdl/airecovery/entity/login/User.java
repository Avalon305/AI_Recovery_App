package com.bdl.airecovery.entity.login;

import com.bdl.airecovery.biz.LoginUtils;
import com.bdl.airecovery.proto.BdlProto;

public class User {
    //用户标识
    private String userId;

    //用户姓名
    private String username;

    //待训练设备列表
    private String deviceTypearrList;

    //是否存在医护设置
    private boolean exisitSetting;

    //训练模式
    private String trainMode;
    //---------个人设置信息↓
    private int seatHeight ; //座椅高度

    private int backDistance ; //靠背距离

    private int footboardDistance;//踏板距离

    private double leverAngle ;//杠杆角度

    private int forwardLimit ; //前方限制

    private int backLimit ; //后方限制

    private double consequentForce ; //顺向力

    private double reverseForce ; //反向力

    private double power ; //功率

    //--------处方信息 ↓
    int dpStatus ;//'1做了 0没做'
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
    private int dpId;

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", deviceTypearrList='" + deviceTypearrList + '\'' +
                ", exisitSetting=" + exisitSetting +
                ", trainMode='" + trainMode + '\'' +
                ", seatHeight=" + seatHeight +
                ", backDistance=" + backDistance +
                ", footboardDistance=" + footboardDistance +
                ", leverAngle=" + leverAngle +
                ", forwardLimit=" + forwardLimit +
                ", backLimit=" + backLimit +
                ", consequentForce=" + consequentForce +
                ", reverseForce=" + reverseForce +
                ", power=" + power +
                ", dpStatus=" + dpStatus +
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
                ", serverTime='" + serverTime + '\'' +
                ", infoResponse=" + infoResponse +
                '}';
    }

    //客户端时间戳
    private String clientTime;

    //服务端时间
    private String serverTime;

    private int infoResponse; // 0:用户不存在，1:无大处方，2：有大处方没做完 ，3：大处方已经做完 ，4：大处方以废弃，5：有可用大处方，没有该台设备训练计划，6：有可用大处方，有该台设备训练计划

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

        //---------个人设置信息↓
        this.seatHeight = message.getLoginResponse().getSeatHeight();
        this.backDistance = message.getLoginResponse().getBackDistance();
        this.footboardDistance = message.getLoginResponse().getFootboardDistance();
        this.leverAngle = message.getLoginResponse().getLeverAngle();
        this.forwardLimit = message.getLoginResponse().getForwardLimit();
        this.backLimit = message.getLoginResponse().getBackLimit();
        this.consequentForce = message.getLoginResponse().getConsequentForce();
        this.reverseForce = message.getLoginResponse().getReverseForce();
        this.power = message.getLoginResponse().getPower();

        //--------处方信息 ↓
        this.dpStatus = message.getLoginResponse().getDpStatus();
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
        this.dpId = message.getLoginResponse().getDpId();
        //用户姓名
        this.username = message.getLoginResponse().getUserName();
        //客户端时间
        this.clientTime = message.getLoginResponse().getClientTime();
        //服务端时间
        this.serverTime = message.getLoginResponse().getServerTime();

        //登录状态i
        this.infoResponse = message.getLoginResponse().getInfoResponse();

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

    public int getInfoResponse() {
        return infoResponse;
    }

    public void setInfoResponse(int infoResponse) {
        this.infoResponse = infoResponse;
    }

    public String getServerTime() {
        return serverTime;
    }

    public void setServerTime(String serverTime) {
        this.serverTime = serverTime;
    }

    public int getSeatHeight() {
        return seatHeight;
    }

    public void setSeatHeight(int seatHeight) {
        this.seatHeight = seatHeight;
    }

    public int getBackDistance() {
        return backDistance;
    }

    public void setBackDistance(int backDistance) {
        this.backDistance = backDistance;
    }

    public int getFootboardDistance() {
        return footboardDistance;
    }

    public void setFootboardDistance(int footboardDistance) {
        this.footboardDistance = footboardDistance;
    }

    public double getLeverAngle() {
        return leverAngle;
    }

    public void setLeverAngle(double leverAngle) {
        this.leverAngle = leverAngle;
    }

    public int getForwardLimit() {
        return forwardLimit;
    }

    public void setForwardLimit(int forwardLimit) {
        this.forwardLimit = forwardLimit;
    }

    public int getBackLimit() {
        return backLimit;
    }

    public void setBackLimit(int backLimit) {
        this.backLimit = backLimit;
    }

    public double getConsequentForce() {
        return consequentForce;
    }

    public void setConsequentForce(double consequentForce) {
        this.consequentForce = consequentForce;
    }

    public double getReverseForce() {
        return reverseForce;
    }

    public void setReverseForce(double reverseForce) {
        this.reverseForce = reverseForce;
    }

    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }

    public int getDpStatus() {
        return dpStatus;
    }

    public void setDpStatus(int dpStatus) {
        this.dpStatus = dpStatus;
    }
}
