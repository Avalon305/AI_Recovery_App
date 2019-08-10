package com.bdl.airecovery.entity.login;

import com.bdl.airecovery.biz.LoginUtils;
import com.bdl.airecovery.proto.BdlProto;

public class User {
    //第二用户
    private Helperuser helperuser;
    //用户类型（教练/学员  coach/trainee）
    private String role ;
    //登录类型（串口/蓝牙  serialport/bluetooth）
    private String type ;
    //用户手机号后四位
    private String phone ;
    //用户姓名
    private String username ;
    //用户标识
    private String userId ;
    //系统版本
    private int sysVersion;
    //体重
    private double weight;
    //年龄
    private int age;
    //是否开启减脂模式
    private boolean defatModeEnable;
    //训练模式
    private String trainMode;
    //最大心率
    private int heartRatemMax;
    //待训练设备列表
    private String deviceTypearrList;
    //训练活动记录ID
    private int activityRecordId;
    //训练活动ID
    private int activityId;
    //课程ID
    private int courseId;
    //是否存在个人设置
    private boolean exisitSetting;

    public User() {

    }
    public User(BdlProto.Message message) {
        //用户类型
        this.role = message.getLoginResponse().getRoleId() == 1 ? "trainee" : "coach";
        //手机号后4位
        this.phone = message.getLoginResponse().getUid().substring(message.getLoginResponse().getUid().length()-4);
        //用户标识
        this.userId = message.getLoginResponse().getUid();
        //用户名
        this.username = message.getLoginResponse().getUid().substring(0,message.getLoginResponse().getUid().length()-4);
        //系统版本
        this.sysVersion = message.getLoginResponse().getSysVersion();
        //体重
        this.weight = message.getLoginResponse().getWeight();
        //年龄
        this.age = message.getLoginResponse().getAge();
        //是否开启减脂模式
        this.defatModeEnable = message.getLoginResponse().getDefatModeEnable();
        //训练模式
        this.trainMode = LoginUtils.getTrainMode(message.getLoginResponse().getTrainMode());
        //最大心率
        this.heartRatemMax = message.getLoginResponse().getHeartRateMax();
        //带训练设备列表
        this.deviceTypearrList = String.valueOf(message.getLoginResponse().getDeviceTypeArrList());
        //训练活动ID
        this.activityId = (int)message.getLoginResponse().getActivityId();
        //训练活动记录ID
        this.activityRecordId = (int)message.getLoginResponse().getActivityRecordId();
        //课程ID
        this.courseId = (int)message.getLoginResponse().getCourseId();
        //是否存在个人设置
        this.exisitSetting = message.getLoginResponse().getExisitSetting();
    }

    public User(Helperuser helperuser, String role, String type, String phone, String username, String userId, int sysVersion, double weight, int age, boolean defatModeEnable, String trainMode, int heartRatemMax, String deviceTypearrList, int activityRecordId, int activityId, int courseId, boolean exisitSetting) {
        this.helperuser = helperuser;
        this.role = role;
        this.type = type;
        this.phone = phone;
        this.username = username;
        this.userId = userId;
        this.sysVersion = sysVersion;
        this.weight = weight;
        this.age = age;
        this.defatModeEnable = defatModeEnable;
        this.trainMode = trainMode;
        this.heartRatemMax = heartRatemMax;
        this.deviceTypearrList = deviceTypearrList;
        this.activityRecordId = activityRecordId;
        this.activityId = activityId;
        this.courseId = courseId;
        this.exisitSetting = exisitSetting;
    }

    public Helperuser getHelperuser() {
        return helperuser;
    }

    public void setHelperuser(Helperuser helperuser) {
        this.helperuser = helperuser;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getSysVersion() {
        return sysVersion;
    }

    public void setSysVersion(int sysVersion) {
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

    public boolean isDefatModeEnable() {
        return defatModeEnable;
    }

    public void setDefatModeEnable(boolean defatModeEnable) {
        this.defatModeEnable = defatModeEnable;
    }

    public String getTrainMode() {
        return trainMode;
    }

    public void setTrainMode(String trainMode) {
        this.trainMode = trainMode;
    }

    public int getHeartRatemMax() {
        return heartRatemMax;
    }

    public void setHeartRatemMax(int heartRatemMax) {
        this.heartRatemMax = heartRatemMax;
    }

    public String getDeviceTypearrList() {
        return deviceTypearrList;
    }

    public void setDeviceTypearrList(String deviceTypearrList) {
        this.deviceTypearrList = deviceTypearrList;
    }

    public int getActivityRecordId() {
        return activityRecordId;
    }

    public void setActivityRecordId(int activityRecordId) {
        this.activityRecordId = activityRecordId;
    }

    public int getActivityId() {
        return activityId;
    }

    public void setActivityId(int activityId) {
        this.activityId = activityId;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public boolean isExisitSetting() {
        return exisitSetting;
    }

    public void setExisitSetting(boolean exisitSetting) {
        this.exisitSetting = exisitSetting;
    }

    @Override
    public String toString() {
        return "User{" +
                "helperuser=" + helperuser +
                ", role='" + role + '\'' +
                ", type='" + type + '\'' +
                ", phone='" + phone + '\'' +
                ", username='" + username + '\'' +
                ", userId='" + userId + '\'' +
                ", sysVersion=" + sysVersion +
                ", weight=" + weight +
                ", age=" + age +
                ", defatModeEnable=" + defatModeEnable +
                ", trainMode='" + trainMode + '\'' +
                ", heartRatemMax=" + heartRatemMax +
                ", deviceTypearrList='" + deviceTypearrList + '\'' +
                ", activityRecordId=" + activityRecordId +
                ", activityId=" + activityId +
                ", courseId=" + courseId +
                ", exisitSetting=" + exisitSetting +
                '}';
    }

}
