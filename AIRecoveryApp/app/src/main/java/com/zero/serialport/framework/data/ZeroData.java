package com.zero.serialport.framework.data;

/**
  * @explain 读取到的数据封装类，可以被继承
  * @author zero
  * @creat time 2019/4/14 7:04 PM.
  */

public class ZeroData {
    //数据类型
    private int dataType;
    //数据内容
    private Object dataBody;

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public Object getDataBody() {
        return dataBody;
    }

    public void setDataBody(Object dataBody) {
        this.dataBody = dataBody;
    }

    public ZeroData() {
    }

    public ZeroData(int dataType, Object dataBody) {
        this.dataType = dataType;
        this.dataBody = dataBody;
    }

    @Override
    public String toString() {
        return "ZeroData{" +
                "dataType=" + dataType +
                ", dataBody=" + dataBody +
                '}';
    }
}
