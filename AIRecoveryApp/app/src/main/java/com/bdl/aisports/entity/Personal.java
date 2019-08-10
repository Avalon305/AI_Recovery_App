package com.bdl.aisports.entity;

/**
 * 个人设置
 */
public class Personal {
    //个人设置的名称，比如座椅高度
    private String name;
    //个人设置的值
    private String value;
    //某条个人设置关联的电机
    private String machine;
    //调节个人设置发送的指令
    private String cmd;
    //最小值
    private String min;
    //最大值
    private String max;
    //静态电机类型
    private String type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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

    public String getMin() {
        return min;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Personal{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", machine='" + machine + '\'' +
                ", cmd='" + cmd + '\'' +
                ", min='" + min + '\'' +
                ", max='" + max + '\'' +
                '}';
    }
}
