package com.bdl.aisports.entity;

/**
 * 连测定位项目
 */
public class TestItem {
    //连测定位的项目名字
    private String name;
    //连测定位项目关联的电机
    private String machine;
    //连测定位项目发送的指令
    private String cmd;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    @Override
    public String toString() {
        return "TestItem{" +
                "name='" + name + '\'' +
                ", machine='" + machine + '\'' +
                ", cmd='" + cmd + '\'' +
                '}';
    }
}
