package com.bdl.aisports.entity;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

import java.util.List;

/**
 * 设备表，从json文件中读取设备list，然后根据设置中的设备名字去灵活的匹配。
 */

public class Device {

    private String deviceName;//设备名称（带循环类型），官方名称，如 蝴蝶机(力量循环)

    private String displayName;//设备名称，待机界面显示，如 蝴蝶机

    private int deviceType; //设备类型，1为拉设备，2为推设备，3为特殊设备，4为单车或者跑步机

    private int activityType;//循环类型，0为力量循环，1为力量耐力循环

    private String generalImg;//图片名称，显示在主页面

    private String muscleImg;//图片名称，肌肉图

    private String helpImg;//图片名称，帮助图

    private String helpWord;//使用说明文字。

    private String DeviceInnerID;//内部代号，如body strong01

    private String consequentForce;//顺向力

    private String reverseForce;//反向力

    private int maxLimit; //设备的最大限位位置

    private int minLimit; //设备的最小限位位置

    private List<Personal> personalList; //个人设置集合

    private List<TestItem> testItemList; //连测定位项目集合

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public int getActivityType() {
        return activityType;
    }

    public void setActivityType(int activityType) {
        this.activityType = activityType;
    }

    public String getGeneralImg() {
        return generalImg;
    }

    public void setGeneralImg(String generalImg) {
        this.generalImg = generalImg;
    }

    public String getMuscleImg() {
        return muscleImg;
    }

    public void setMuscleImg(String muscleImg) {
        this.muscleImg = muscleImg;
    }

    public String getHelpImg() {
        return helpImg;
    }

    public void setHelpImg(String helpImg) {
        this.helpImg = helpImg;
    }

    public String getHelpWord() {
        return helpWord;
    }

    public void setHelpWord(String helpWord) {
        this.helpWord = helpWord;
    }

    public String getDeviceInnerID() {
        return DeviceInnerID;
    }

    public void setDeviceInnerID(String deviceInnerID) {
        DeviceInnerID = deviceInnerID;
    }

    public String getConsequentForce() {
        return consequentForce;
    }

    public void setConsequentForce(String consequentForce) {
        this.consequentForce = consequentForce;
    }

    public int getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(int maxLimit) {
        this.maxLimit = maxLimit;
    }

    public int getMinLimit() {
        return minLimit;
    }

    public void setMinLimit(int minLimit) {
        this.minLimit = minLimit;
    }

    public String getReverseForce() {
        return reverseForce;
    }

    public void setReverseForce(String reverseForce) {
        this.reverseForce = reverseForce;
    }

    public List<Personal> getPersonalList() {
        return personalList;
    }

    public void setPersonalList(List<Personal> personalList) {
        this.personalList = personalList;
    }

    public List<TestItem> getTestItemList() {
        return testItemList;
    }

    public void setTestItemList(List<TestItem> testItemList) {
        this.testItemList = testItemList;
    }

    @Override
    public String toString() {
        return "Device{" +
                "deviceName='" + deviceName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", deviceType=" + deviceType +
                ", activityType=" + activityType +
                ", generalImg='" + generalImg + '\'' +
                ", muscleImg='" + muscleImg + '\'' +
                ", helpImg='" + helpImg + '\'' +
                ", helpWord='" + helpWord + '\'' +
                ", DeviceInnerID='" + DeviceInnerID + '\'' +
                ", consequentForce='" + consequentForce + '\'' +
                ", reverseForce='" + reverseForce + '\'' +
                ", maxLimit=" + maxLimit +
                ", minLimit=" + minLimit +
                ", personalList=" + personalList +
                ", testItemList=" + testItemList +
                '}';
    }
}
