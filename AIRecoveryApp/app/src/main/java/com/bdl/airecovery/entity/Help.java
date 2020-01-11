package com.bdl.airecovery.entity;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

/**
 * 助力表
 * 该表共有10*5条数据，也就是说需要标定50次
 */
@Table(name = "help")
public class Help {
    @Column(name = "id", isId = true)
    int id;
    /**
     * help_divided_by_torque表id
     */
    @Column(name = "torque")
    int torque;

    /**
     * 位置区间：
     * 将0~199分为10个区间,取值为0,20,40......
     * 如果该值为0，即代表当前区间为0~19;
     * 如果该值为20，即代表当前区间为20~39;
     */
    @Column(name = "frontLimit")
    int position;

    /**
     * 助力速度
     */
    @Column(name = "helpSpeed")
    int helpSpeed;

    /**
     * 三个标定参数
     */
    @Column(name = "paramA")
    int paramA;
    @Column(name = "paramB")
    int paramB;
    @Column(name = "paramC")
    int paramC;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTorque() {
        return torque;
    }

    public void setTorque(int torque) {
        this.torque = torque;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getHelpSpeed() {
        return helpSpeed;
    }

    public void setHelpSpeed(int helpSpeed) {
        this.helpSpeed = helpSpeed;
    }

    public int getParamA() {
        return paramA;
    }

    public void setParamA(int paramA) {
        this.paramA = paramA;
    }

    public int getParamB() {
        return paramB;
    }

    public void setParamB(int paramB) {
        this.paramB = paramB;
    }

    public int getParamC() {
        return paramC;
    }

    public void setParamC(int paramC) {
        this.paramC = paramC;
    }

    @Override
    public String toString() {
        return "Help{" +
                "id=" + id +
                ", torque=" + torque +
                ", position=" + position +
                ", helpSpeed=" + helpSpeed +
                ", paramA=" + paramA +
                ", paramB=" + paramB +
                ", paramC=" + paramC +
                '}';
    }
}
