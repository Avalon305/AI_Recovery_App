package com.bdl.airecovery.entity;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

/**
 * 此为系统设置表，每台设备仅有一条记录
 * 安装程序时，就自带一条数据。
 */
//表创建时会默认插入一个设备名称、一个版本号、一个更新地址
@Table(name = "setting")
public class Setting {

    @Column(name = "id", isId = true)
    private int id;//主键

    @Column(name = "deviceName")
    private String deviceName;//设备名称，也就是设备类型。

    @Column(name = "version")
    private String version;//软件版本 1.0

    @Column(name = "updateAddress")
    private String updateAddress;//android升级地址，对应bdl云平台，装机自带。 128.0.0.1

    @Column(name = "coachDeviceAddress")
    private String coachDeviceAddress;//教练机设备地址

    @Column(name = "UUID")
    private String UUID; //与时间服务器通讯使用

    @Column(name = "canQuickLogin")
    private Boolean canQuickLogin; //是否可以快速登录

    @Column(name = "canStrengthTest")
    private Boolean canStrengthTest; //是否可以肌力测试

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUpdateAddress() {
        return updateAddress;
    }

    public void setUpdateAddress(String updateAddress) {
        this.updateAddress = updateAddress;
    }

    public String getCoachDeviceAddress() {
        return coachDeviceAddress;
    }

    public void setCoachDeviceAddress(String coachDeviceAddress) {
        this.coachDeviceAddress = coachDeviceAddress;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public Boolean getCanQuickLogin() {
        return canQuickLogin;
    }

    public void setCanQuickLogin(Boolean canQuickLogin) {
        this.canQuickLogin = canQuickLogin;
    }

    public Boolean getCanStrengthTest() {
        return canStrengthTest;
    }

    public void setCanStrengthTest(Boolean canStrengthTest) {
        this.canStrengthTest = canStrengthTest;
    }

    @Override
    public String toString() {
        return "Setting{" +
                "id=" + id +
                ", deviceName='" + deviceName + '\'' +
                ", version='" + version + '\'' +
                ", updateAddress='" + updateAddress + '\'' +
                ", coachDeviceAddress='" + coachDeviceAddress + '\'' +
                ", UUID='" + UUID + '\'' +
                ", canQuickLogin=" + canQuickLogin +
                ", canStrengthTest=" + canStrengthTest +
                '}';
    }

}


