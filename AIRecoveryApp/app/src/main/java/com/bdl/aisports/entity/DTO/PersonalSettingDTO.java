package com.bdl.aisports.entity.DTO;

import com.bdl.aisports.proto.BdlProto;

public class PersonalSettingDTO {
    String Uid = "";                        //用户ID
    int deviceTypeValue = 0;               //设备类型
    int activityTypeValue = 0;             //循环类型
    int seatHeight = 0;                    //座位高度
    int backDistance = 0;                  //靠背距离
    int leverLength = 0;                   //杠杆长度
    double leverAngle = 0D;                 //杠杆角度
    int frontLimit = 0;                    //前方限制
    int backLimit = 0;                     //后方限制
    int trainModeValue = 0;                //训练模式
    boolean isOpenFatLossMode=false;         //是否开启减脂模式

    public PersonalSettingDTO() {
    }

    public String getUid() {
        return Uid;
    }

    public void setUid(String uid) {
        Uid = uid;
    }

    public int getDeviceTypeValue() {
        return deviceTypeValue;
    }

    public void setDeviceTypeValue(int deviceTypeValue) {
        this.deviceTypeValue = deviceTypeValue;
    }

    public int getActivityTypeValue() {
        return activityTypeValue;
    }

    public void setActivityTypeValue(int activityTypeValue) {
        this.activityTypeValue = activityTypeValue;
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

    public int getLeverLength() {
        return leverLength;
    }

    public void setLeverLength(int leverLength) {
        this.leverLength = leverLength;
    }

    public double getLeverAngle() {
        return leverAngle;
    }

    public void setLeverAngle(double leverAngle) {
        this.leverAngle = leverAngle;
    }

    public int getFrontLimit() {
        return frontLimit;
    }

    public void setFrontLimit(int frontLimit) {
        this.frontLimit = frontLimit;
    }

    public int getBackLimit() {
        return backLimit;
    }

    public void setBackLimit(int backLimit) {
        this.backLimit = backLimit;
    }

    public int getTrainModeValue() {
        return trainModeValue;
    }

    public void setTrainModeValue(int trainModeValue) {
        this.trainModeValue = trainModeValue;
    }

    public boolean isOpenFatLossMode() {
        return isOpenFatLossMode;
    }

    public void setOpenFatLossMode(boolean openFatLossMode) {
        isOpenFatLossMode = openFatLossMode;
    }

}
