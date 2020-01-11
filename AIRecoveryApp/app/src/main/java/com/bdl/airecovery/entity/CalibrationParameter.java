package com.bdl.airecovery.entity;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

@Table(name = "calibration_param")
public class CalibrationParameter {

    @Column(name = "id", isId = true)
    private int id; //主键

    //最小力矩
    @Column(name = "minTorque")
    private int minTorque;

    //力矩返回速度
    @Column(name = "backSpeed")
    private int backSpeed;

    //非运动状态下的速度
    @Column(name = "normalSpeed")
    private int normalSpeed;

    //最小返回力矩
    @Column(name = "minBackTorque")
    private int minBackTorque;

    //初始反弹力量
    @Column(name = "bounce")
    private int bounce;

    //提前量
    @Column(name = "lead")
    private int lead;

    public int getMinTorque() {
        return minTorque;
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
