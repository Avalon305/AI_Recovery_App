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

    @Column(name = "timeServerAddress")
    private String timeServerAddress;//时间服务器地址

    @Column(name = "coachDeviceAddress")
    private String coachDeviceAddress;//教练机设备地址

    @Column(name = "UUID")
    private String UUID; //与时间服务器通讯使用

    @Column(name = "rate")
    private String rate; //电机的比率

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

    public String getTimeServerAddress() {
        return timeServerAddress;
    }

    public void setTimeServerAddress(String timeServerAddress) {
        this.timeServerAddress = timeServerAddress;
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

    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }


    @Override
    public String toString() {
        return "Setting{" +
                "id=" + id +
                ", deviceName='" + deviceName + '\'' +
                ", version='" + version + '\'' +
                ", updateAddress='" + updateAddress + '\'' +
                ", timeServerAddress='" + timeServerAddress + '\'' +
                ", coachDeviceAddress='" + coachDeviceAddress + '\'' +
                ", UUID='" + UUID + '\'' +
                ", rate=" + rate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Setting setting = (Setting) o;

        if (deviceName != null ? !deviceName.equals(setting.deviceName) : setting.deviceName != null)
            return false;
        if (version != null ? !version.equals(setting.version) : setting.version != null)
            return false;
        if (updateAddress != null ? !updateAddress.equals(setting.updateAddress) : setting.updateAddress != null)
            return false;
        if (timeServerAddress != null ? !timeServerAddress.equals(setting.timeServerAddress) : setting.timeServerAddress != null)
            return false;
        if (coachDeviceAddress != null ? !coachDeviceAddress.equals(setting.coachDeviceAddress) : setting.coachDeviceAddress != null)
            return false;
        return rate != null ? rate.equals(setting.rate) : setting.rate == null;
    }

    @Override
    public int hashCode() {
        int result = deviceName != null ? deviceName.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (updateAddress != null ? updateAddress.hashCode() : 0);
        result = 31 * result + (timeServerAddress != null ? timeServerAddress.hashCode() : 0);
        result = 31 * result + (coachDeviceAddress != null ? coachDeviceAddress.hashCode() : 0);
        result = 31 * result + (rate != null ? rate.hashCode() : 0);
        return result;
    }
}


