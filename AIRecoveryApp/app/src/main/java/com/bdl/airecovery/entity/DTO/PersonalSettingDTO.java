package com.bdl.airecovery.entity.DTO;

public class PersonalSettingDTO {
    String Uid = "";                        //用户ID
    String bindId="";                       //手环id
    int deviceTypeValue = 0;               //设备类型
    int trainModeValue = 0;                //训练模式
    int seatHeight = 0;                    //座位高度
    int backDistance = 0;                  //靠背距离
    int footboardDistance=0;               //踏板距离
    double leverAngle = 0D;                 //杠杆角度
    int frontLimit = 0;                    //前方限制
    int backLimit = 0;                     //后方限制
    double consequentForce =0D;            //顺向力
    double reverseForce=0D;                //反向力
    double power=0D;                       //功率

    public PersonalSettingDTO() {
    }

    public String getUid() {
        return Uid;
    }

    public void setUid(String uid) {
        Uid = uid;
    }

    public String getBindId() {
        return bindId;
    }

    public void setBindId(String bindId) {
        this.bindId = bindId;
    }

    public int getDeviceTypeValue() {
        return deviceTypeValue;
    }

    public void setDeviceTypeValue(int deviceTypeValue) {
        this.deviceTypeValue = deviceTypeValue;
    }

    public int getTrainModeValue() {
        return trainModeValue;
    }

    public void setTrainModeValue(int trainModeValue) {
        this.trainModeValue = trainModeValue;
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

}
