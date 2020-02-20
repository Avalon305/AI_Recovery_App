package com.bdl.airecovery.entity;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

@Table(name = "segment_calibration")
public class SegmentCalibration {
    /**
     * id,主键
     */
    @Column(name = "id", isId = true)
    int id;

    /**
     * 当前需要标定的力量值
     */
    @Column(name = "force")
    int force;
    /**
     * 分段位置
     */
    @Column(name = "segmentPosition")
    int segmentPosition;

    /**
     * 去程力矩
     */
    @Column(name = "goingTorque")
    int goingTorque;

    /**
     * 回程力矩
     */
    @Column(name = "returnTorque")
    int returnTorque;

    /**
     * 去程速度
     */
    @Column(name = "goingSpeed")
    int goingSpeed;

    /**
     * 返回速度
     */
    @Column(name = "returnSpeed")
    int returnSpeed;

    /**
     * 回弹力量
     */
    @Column(name = "bounce")
    int bounce;

    /**
     * 拉动力臂的阈值
     */
    @Column(name = "pullThresholdVal")
    int pullThresholdVal;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getForce() {
        return force;
    }

    public void setForce(int force) {
        this.force = force;
    }

    public int getSegmentPosition() {
        return segmentPosition;
    }

    public void setSegmentPosition(int segmentPosition) {
        this.segmentPosition = segmentPosition;
    }

    public int getGoingTorque() {
        return goingTorque;
    }

    public void setGoingTorque(int goingTorque) {
        this.goingTorque = goingTorque;
    }

    public int getReturnTorque() {
        return returnTorque;
    }

    public void setReturnTorque(int returnTorque) {
        this.returnTorque = returnTorque;
    }

    public int getGoingSpeed() {
        return goingSpeed;
    }

    public void setGoingSpeed(int goingSpeed) {
        this.goingSpeed = goingSpeed;
    }

    public int getReturnSpeed() {
        return returnSpeed;
    }

    public void setReturnSpeed(int returnSpeed) {
        this.returnSpeed = returnSpeed;
    }

    public int getBounce() {
        return bounce;
    }

    public void setBounce(int bounce) {
        this.bounce = bounce;
    }

    public int getPullThresholdVal() {
        return pullThresholdVal;
    }

    public void setPullThresholdVal(int pullThresholdVal) {
        this.pullThresholdVal = pullThresholdVal;
    }

    @Override
    public String toString() {
        return "SegmentCalibration{" +
                "id=" + id +
                ", force=" + force +
                ", segmentPosition=" + segmentPosition +
                ", goingTorque=" + goingTorque +
                ", returnTorque=" + returnTorque +
                ", goingSpeed=" + goingSpeed +
                ", returnSpeed=" + returnSpeed +
                ", bounce=" + bounce +
                ", pullThresholdVal=" + pullThresholdVal +
                '}';
    }

    public static String[] headerData() {
        return new String[]{
                "id",
                "force", "segmentPosition", "goingTorque", "returnTorque",
                "goingSpeed", "returnSpeed", "bounce", "pullThresholdVal"};
    }

}
