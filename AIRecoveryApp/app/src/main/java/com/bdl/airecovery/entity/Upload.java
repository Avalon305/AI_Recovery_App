package com.bdl.airecovery.entity;

/**
 * 上传结果
 */

public class Upload {
    private String uid_ = ""; //用户ID
    private int trainMode_ = 0; //训练模式名称
    private long courseId_ = 0L; //训练课程ID
    private long activityId_ = 0L; //训练活动ID
    private long  activityRecordId_ = 0L; //训练活动记录ID
    private int deviceType_ = 0; //设备名
    private int activityType_ = 0; //循环名称
    private boolean defatModeEnable_ =false; //是否开启减脂模式状态
    private double reverseForce_ = 0D; //最终反向力
    private double forwardForce_ = 0D; //最终顺向力
    private double  power_ = 0D; //最终功率
    private int finishCount_ = 0; //训练个数
    private double finalDistance_ = 0D; //运动距离
    private double calorie_ = 0D; //耗能（卡路里）
    private int trainTime_ = 0; //训练时间
    private int heartRateAvg_ = 0; //运动过程中的平均心率
    private int heartRateMax_ = 0; //运动过程中的最大心率
    private int heartRateMin_ = 0; //运动过程中的最小心率

    public String getUid_() {
        return uid_;
    }

    public void setUid_(String uid_) {
        this.uid_ = uid_;
    }

    public int getTrainMode_() {
        return trainMode_;
    }

    public void setTrainMode_(int trainMode_) {
        this.trainMode_ = trainMode_;
    }

    public long getCourseId_() {
        return courseId_;
    }

    public void setCourseId_(long courseId_) {
        this.courseId_ = courseId_;
    }

    public long getActivityId_() {
        return activityId_;
    }

    public void setActivityId_(long activityId_) {
        this.activityId_ = activityId_;
    }

    public long getActivityRecordId_() {
        return activityRecordId_;
    }

    public void setActivityRecordId_(long activityRecordId_) {
        this.activityRecordId_ = activityRecordId_;
    }

    public int getDeviceType_() {
        return deviceType_;
    }

    public void setDeviceType_(int deviceType_) {
        this.deviceType_ = deviceType_;
    }

    public int getActivityType_() {
        return activityType_;
    }

    public void setActivityType_(int activityType_) {
        this.activityType_ = activityType_;
    }

    public boolean isDefatModeEnable_() {
        return defatModeEnable_;
    }

    public void setDefatModeEnable_(boolean defatModeEnable_) {
        this.defatModeEnable_ = defatModeEnable_;
    }

    public double getReverseForce_() {
        return reverseForce_;
    }

    public void setReverseForce_(double reverseForce_) {
        this.reverseForce_ = reverseForce_;
    }

    public double getForwardForce_() {
        return forwardForce_;
    }

    public void setForwardForce_(double forwardForce_) {
        this.forwardForce_ = forwardForce_;
    }

    public double getPower_() {
        return power_;
    }

    public void setPower_(double power_) {
        this.power_ = power_;
    }

    public int getFinishCount_() {
        return finishCount_;
    }

    public void setFinishCount_(int finishCount_) {
        this.finishCount_ = finishCount_;
    }

    public double getFinalDistance_() {
        return finalDistance_;
    }

    public void setFinalDistance_(double finalDistance_) {
        this.finalDistance_ = finalDistance_;
    }

    public double getCalorie_() {
        return calorie_;
    }

    public void setCalorie_(double calorie_) {
        this.calorie_ = calorie_;
    }

    public int getTrainTime_() {
        return trainTime_;
    }

    public void setTrainTime_(int trainTime_) {
        this.trainTime_ = trainTime_;
    }

    public int getHeartRateAvg_() {
        return heartRateAvg_;
    }

    public void setHeartRateAvg_(int heartRateAvg_) {
        this.heartRateAvg_ = heartRateAvg_;
    }

    public int getHeartRateMax_() {
        return heartRateMax_;
    }

    public void setHeartRateMax_(int heartRateMax_) {
        this.heartRateMax_ = heartRateMax_;
    }

    public int getHeartRateMin_() {
        return heartRateMin_;
    }

    public void setHeartRateMin_(int heartRateMin_) {
        this.heartRateMin_ = heartRateMin_;
    }

}
