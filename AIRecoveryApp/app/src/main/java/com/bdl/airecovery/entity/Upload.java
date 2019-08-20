package com.bdl.airecovery.entity;

import java.util.List;

/**
 * 上传结果
 */

public class Upload {
    private String uid; //用户ID
    private int deviceType; //设备类型
    private int trainMode; //训练模式
    private double consequentForce; //最终顺向力
    private double reverseForce; //最终反向力
    private double power; //最终功率
    private int speedRank; //运动速度
    private int finishNum; //训练个数
//    private double distance; //运动距离 千米，两位小数
    private double energy;//训练总耗能 单位卡路里
    private List<Integer> heartRateList;//心率集合：运动过程实时心率集合
    private String userThoughts; //病人感想

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public int getTrainMode() {
        return trainMode;
    }

    public void setTrainMode(int trainMode) {
        this.trainMode = trainMode;
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

    public int getSpeedRank() {
        return speedRank;
    }

    public void setSpeedRank(int speedRank) {
        this.speedRank = speedRank;
    }

    public int getFinishNum() {
        return finishNum;
    }

    public void setFinishNum(int finishNum) {
        this.finishNum = finishNum;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }

    public List<Integer> getHeartRateList() {
        return heartRateList;
    }

    public void setHeartRateList(List<Integer> heartRateList) {
        this.heartRateList = heartRateList;
    }

    public String getUserThoughts() {
        return userThoughts;
    }

    public void setUserThoughts(String userThoughts) {
        this.userThoughts = userThoughts;
    }

    @Override
    public String toString() {
        return "Upload{" +
                "uid='" + uid + '\'' +
                ", deviceType=" + deviceType +
                ", trainMode=" + trainMode +
                ", consequentForce=" + consequentForce +
                ", reverseForce=" + reverseForce +
                ", power=" + power +
                ", speedRank=" + speedRank +
                ", finishNum=" + finishNum +
                ", energy=" + energy +
                ", heartRateList=" + heartRateList +
                ", userThoughts='" + userThoughts + '\'' +
                '}';
    }
}
