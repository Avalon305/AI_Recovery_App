package com.bdl.airecovery.entity.DTO;

public class TrainResultDTO {
    String uid_ = "";                 //用户ID
    int trainModeValue_ = 0;          //训练模式名称
    long courseId_ = 0L;              //训练课程ID
    long activityId_ = 0L;            //训练活动ID
    long  activityRecordId_ = 0L;     //训练活动记录ID
    int deviceTypeValue_ = 0;              //设备名
    int activityTypeValue_ = 0;            //循环名称
    boolean defatModeEnable_ =false;  //是否开启减脂模式状态
    double reverseForce_ = 0D;        //最终反向力
    double forwardForce_ = 0D;        //最终顺向力
    double  power_ = 0D;              //最终功率
    int finishCount_ = 0;             //训练个数
    double finalDistance_ = 0D;       //运动距离
    double calorie_ = 0D;             //耗能（卡路里）
    int trainTime_ = 0;               //训练时间
    int heartRateAvg_ = 0;            //运动过程中的平均心率
    int heartRateMax_ = 0;            //运动过程中的最大心率
    int heartRateMin_ = 0;            //运动过程中的最小心率

    public TrainResultDTO() {
    }

    public String getUid_() {
        return uid_;
    }

    public void setUid_(String uid_) {
        this.uid_ = uid_;
    }

    public int getTrainModeValue_() {
        return trainModeValue_;
    }

    public void setTrainModeValue_(int trainModeValue_) {
        this.trainModeValue_ = trainModeValue_;
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

    public int getDeviceTypeValue_() {
        return deviceTypeValue_;
    }

    public void setDeviceTypeValue_(int deviceTypeValue_) {
        this.deviceTypeValue_ = deviceTypeValue_;
    }

    public int getActivityTypeValue_() {
        return activityTypeValue_;
    }

    public void setActivityTypeValue_(int activityTypeValue_) {
        this.activityTypeValue_ = activityTypeValue_;
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
