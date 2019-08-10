package com.bdl.airecovery.entity;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

/**
 * 个人信息表 从教练机请求的个人信息设置
 */

@Table(name = "personalInfo")
public class PersonalInfo {

    @Column(name = "id", isId = true)
    private int id; //主键

    @Column(name = "userId")
    private String userId; //用户标识

    @Column(name = "deviceType")
    private String deviceType; //设备类型

    @Column(name = "devicePersonalList")
    private String devicePersonalList; //个人设置的设备参数（JSON串）

    @Column(name = "infoPersonalList")
    private String infoPersonalList; //个人信息的参数（JSON串）

    public PersonalInfo() {
    }

    public PersonalInfo(int id, String userId, String deviceType, String devicePersonalList, String infoPersonalList) {
        this.id = id;
        this.userId = userId;
        this.deviceType = deviceType;
        this.devicePersonalList = devicePersonalList;
        this.infoPersonalList = infoPersonalList;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDevicePersonalList() {
        return devicePersonalList;
    }

    public void setDevicePersonalList(String devicePersonalList) {
        this.devicePersonalList = devicePersonalList;
    }

    public String getInfoPersonalList() {
        return infoPersonalList;
    }

    public void setInfoPersonalList(String infoPersonalList) {
        this.infoPersonalList = infoPersonalList;
    }

    @Override
    public String toString() {
        return "PersonalInfo{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", devicePersonalList='" + devicePersonalList + '\'' +
                ", infoPersonalList='" + infoPersonalList + '\'' +
                '}';
    }
}
