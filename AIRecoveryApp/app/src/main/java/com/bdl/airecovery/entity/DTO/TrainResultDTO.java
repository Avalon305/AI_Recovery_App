package com.bdl.airecovery.entity.DTO;

public class TrainResultDTO {
    String uid_ = "";                 //用户ID
    int deviceTypeValue_ = 0;              //设备名
    int trainModeValue_ = 0;          //训练模式
    double forwardForce_ = 0D;        //顺向力
    double reverseForce_ = 0D;        //反向力
    double  power_ = 0D;              //最终功率
    int speedRank=0;                   //运动速度
    int finishNum_ = 0;             //训练个数
    int finishTime_=0;              //完成训练时间
    double finalDistance_ = 0D;       //运动距离
    double energy_ = 0D;             //耗能（卡路里）
    String heart_rate_list ="";      ////心率集合：运动过程实时心率集合，数据之间*分割'
    String pr_userthoughts="";        //用户感想
    String dataId = "";              //安卓端记录的id
    String bindId_="";                //手环id
    int dpId_=0;                      //处方id

    public TrainResultDTO() {

    }

    public String getUid_() {
        return uid_;
    }

    public void setUid_(String uid_) {
        this.uid_ = uid_;
    }

    public int getDeviceTypeValue_() {
        return deviceTypeValue_;
    }

    public void setDeviceTypeValue_(int deviceTypeValue_) {
        this.deviceTypeValue_ = deviceTypeValue_;
    }

    public int getTrainModeValue_() {
        return trainModeValue_;
    }

    public void setTrainModeValue_(int trainModeValue_) {
        this.trainModeValue_ = trainModeValue_;
    }

    public double getForwardForce_() {
        return forwardForce_;
    }

    public void setForwardForce_(double forwardForce_) {
        this.forwardForce_ = forwardForce_;
    }

    public double getReverseForce_() {
        return reverseForce_;
    }

    public void setReverseForce_(double reverseForce_) {
        this.reverseForce_ = reverseForce_;
    }

    public double getPower_() {
        return power_;
    }

    public void setPower_(double power_) {
        this.power_ = power_;
    }

    public int getSpeedRank() {
        return speedRank;
    }

    public void setSpeedRank(int speedRank) {
        this.speedRank = speedRank;
    }

    public int getFinishNum_() {
        return finishNum_;
    }

    public void setFinishNum_(int finishNum_) {
        this.finishNum_ = finishNum_;
    }

    public int getFinishTime_() {
        return finishTime_;
    }

    public void setFinishTime_(int finishTime_) {
        this.finishTime_ = finishTime_;
    }

    public double getFinalDistance_() {
        return finalDistance_;
    }

    public void setFinalDistance_(double finalDistance_) {
        this.finalDistance_ = finalDistance_;
    }

    public double getEnergy_() {
        return energy_;
    }

    public void setEnergy_(double energy_) {
        this.energy_ = energy_;
    }

    public String getHeart_rate_list() {
        return heart_rate_list;
    }

    public void setHeart_rate_list(String heart_rate_list) {
        this.heart_rate_list = heart_rate_list;
    }

    public String getPr_userthoughts() {
        return pr_userthoughts;
    }

    public void setPr_userthoughts(String pr_userthoughts) {
        this.pr_userthoughts = pr_userthoughts;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getBindId_() {
        return bindId_;
    }

    public void setBindId_(String bindId_) {
        this.bindId_ = bindId_;
    }

    public int getDpId_() {
        return dpId_;
    }

    public void setDpId_(int dpId_) {
        this.dpId_ = dpId_;
    }

    @Override
    public String toString() {
        return "TrainResultDTO{" +
                "uid_='" + uid_ + '\'' +
                ", deviceTypeValue_=" + deviceTypeValue_ +
                ", trainModeValue_=" + trainModeValue_ +
                ", forwardForce_=" + forwardForce_ +
                ", reverseForce_=" + reverseForce_ +
                ", power_=" + power_ +
                ", speedRank=" + speedRank +
                ", finishNum_=" + finishNum_ +
                ", finishTime_=" + finishTime_ +
                ", finalDistance_=" + finalDistance_ +
                ", energy_=" + energy_ +
                ", heart_rate_list='" + heart_rate_list + '\'' +
                ", pr_userthoughts='" + pr_userthoughts + '\'' +
                ", dataId='" + dataId + '\'' +
                ", bindId_='" + bindId_ + '\'' +
                ", dpId_=" + dpId_ +
                '}';
    }
}
