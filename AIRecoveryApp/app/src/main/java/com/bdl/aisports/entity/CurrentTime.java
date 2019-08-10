package com.bdl.aisports.entity;

public class CurrentTime {
    private int seconds; //秒数
    private int type; //类型（0运动，1休息）

    public CurrentTime() {
    }

    public CurrentTime(int seconds, int type) {
        this.seconds = seconds;
        this.type = type;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "CurrentTime{" +
                "seconds=" + seconds +
                ", type=" + type +
                '}';
    }
}
