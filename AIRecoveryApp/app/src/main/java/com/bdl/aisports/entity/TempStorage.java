package com.bdl.aisports.entity;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

/**
 * 暂存表
 */
@Table(name = "tempStorage")
public class TempStorage {
    //暂定两种：重设个人设置，发送运动结果
    final static int ReSetPersonalSettinglist = 1;
    final static int SendTrainResult = 2;

    @Column(name = "id", isId = true)
    private int id;//主键

    @Column(name = "deviceName")
    private int type;//哪种数据发送失败了。也就是判断重传类型。

    @Column(name = "version")
    private String data;//数据串，一旦发送失败则打成json存数据库。

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "TempStorage{" +
                "id=" + id +
                ", type=" + type +
                ", data='" + data + '\'' +
                '}';
    }
}
