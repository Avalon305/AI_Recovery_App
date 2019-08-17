package com.bdl.airecovery.entity;


public class CalibrationParameter {

    //最小力矩
    private int minTorque;

    //力矩返回速度
    private int backSpeed;

    //非运动状态下的速度
    private int normalSpeed;

    //最小返回力矩
    private int minBackTorque;

    //初始反弹力量
    private int bounce;

    //提前量
    private int lead;

    public int getMinTorque() {
        return minTorque;
    }

    public void setMinTorque(int minTorque) {
        this.minTorque = minTorque;
    }

    public int getBackSpeed() {
        return backSpeed;
    }

    public void setBackSpeed(int backSpeed) {
        this.backSpeed = backSpeed;
    }



    public int getNormalSpeed() {
        return normalSpeed;
    }

    public void setNormalSpeed(int normalSpeed) {
        this.normalSpeed = normalSpeed;
    }

    public int getMinBackTorque() {
        return minBackTorque;
    }

    public void setMinBackTorque(int minBackTorque) {
        this.minBackTorque = minBackTorque;
    }

    public int getBounce() {
        return bounce;
    }

    public void setBounce(int bounce) {
        this.bounce = bounce;
    }

    public int getLead() {
        return lead;
    }

    public void setLead(int lead) {
        this.lead = lead;
    }

    @Override
    public String toString() {
        return "CalibrationParameter{" +
                "minTorque=" + minTorque +
                ", backSpeed=" + backSpeed +
                ", normalSpeed=" + normalSpeed +
                ", minBackTorque=" + minBackTorque +
                ", bounce=" + bounce +
                ", lead=" + lead +
                '}';
    }
}
