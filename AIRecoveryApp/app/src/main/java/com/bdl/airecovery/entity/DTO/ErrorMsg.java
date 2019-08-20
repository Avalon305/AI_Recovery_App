package com.bdl.airecovery.entity.DTO;

public class ErrorMsg {
    String uid = ""; //用户ID
    int deviceType = 0;//设备类型
    int trainMode = 0; //训练模式
    String error = "" ;//错误信息,主要是电机发生的错误
    String errorStartTime = "";//错误发生时间

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public int getTrainMode() {
        return trainMode;
    }

    public void setTrainMode(int trainMode) {
        this.trainMode = trainMode;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorStartTime() {
        return errorStartTime;
    }

    public void setErrorStartTime(String errorStartTime) {
        this.errorStartTime = errorStartTime;
    }
}
