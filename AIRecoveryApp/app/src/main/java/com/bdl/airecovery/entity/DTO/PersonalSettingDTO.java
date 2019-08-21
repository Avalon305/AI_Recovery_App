package com.bdl.airecovery.entity.DTO;

public class PersonalSettingDTO {
    String uid = ""; //用户ID
    boolean exisitSetting = true; //是否存在个人设置
    int trainMode = 0; //训练模式:主被动，康复模式
    int seatHeight = 0; //座椅高度
    int backDistance = 0; //靠背距离
    int footboardDistance = 0;//踏板距离
    double leverAngle = 0D;//杠杆角度
    int forwardLimit = 0; //前方限制
    int backLimit = 0; //后方限制
    double consequentForce = 0D; //顺向力
    double reverseForce = 0D; //反向力
    double power = 0D; //功率

    public PersonalSettingDTO() {
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public boolean isExisitSetting() {
        return exisitSetting;
    }

    public void setExisitSetting(boolean exisitSetting) {
        this.exisitSetting = exisitSetting;
    }

    public int getTrainMode() {
        return trainMode;
    }

    public void setTrainMode(int trainMode) {
        this.trainMode = trainMode;
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

    @Override
    public String toString() {
        return "PersonalSettingDTO{" +
                "uid='" + uid + '\'' +
                ", exisitSetting=" + exisitSetting +
                ", trainMode=" + trainMode +
                ", seatHeight=" + seatHeight +
                ", backDistance=" + backDistance +
                ", footboardDistance=" + footboardDistance +
                ", leverAngle=" + leverAngle +
                ", forwardLimit=" + forwardLimit +
                ", backLimit=" + backLimit +
                ", consequentForce=" + consequentForce +
                ", reverseForce=" + reverseForce +
                ", power=" + power +
                '}';
    }
}
