package com.bdl.airecovery.entity.DTO;

public class PersonalSettingDTO {
    String uid = ""; //用户ID
    boolean exisitSetting = true; //是否存在个人设置
    int trainMode = 0; //训练模式:主被动，康复模式
    int seatHeight = 0; //座椅高度
    int backDistance = 0; //靠背距离
    int footboardDistance = 0;//踏板距离
    double leverAngle = 0D;//杠杆角度
    int forwardLimit = 0; //前方限制
    int backLimit = 0; //后方限制
    double consequentForce = 0D; //顺向力
    double reverseForce = 0D; //反向力
    double power = 0D; //功率
    int dpStatus = 0;//'1做了 0没做'
    int dpMoveway = 0;//'移乘方式'
    String dpMemo = "";//注意点、指示
    int dp_groupcount = 0;//目标组数
    int dp_groupnum = 0;                  //每组运动个数
    int dp_relaxtime = 0;                  //每组间隔休息时间
    int speed_rank = 0;//运动速度等级
    String sysVersion = "";//系统版本
    int dpId = 0;//设备处方id

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }


    public boolean isExisitSetting() {
        return exisitSetting;
    }

    public void setExisitSetting(boolean exisitSetting) {
        this.exisitSetting = exisitSetting;
    }

    public int getTrainMode() {
        return trainMode;
    }

    public void setTrainMode(int trainMode) {
        this.trainMode = trainMode;
    }

    public int getSeatHeight() {
        return seatHeight;
    }

    public void setSeatHeight(int seatHeight) {
        this.seatHeight = seatHeight;
    }

    public int getBackDistance() {
        return backDistance;
    }

    public void setBackDistance(int backDistance) {
        this.backDistance = backDistance;
    }

    public int getFootboardDistance() {
        return footboardDistance;
    }

    public void setFootboardDistance(int footboardDistance) {
        this.footboardDistance = footboardDistance;
    }

    public double getLeverAngle() {
        return leverAngle;
    }

    public void setLeverAngle(double leverAngle) {
        this.leverAngle = leverAngle;
    }

    public int getForwardLimit() {
        return forwardLimit;
    }

    public void setForwardLimit(int forwardLimit) {
        this.forwardLimit = forwardLimit;
    }

    public int getBackLimit() {
        return backLimit;
    }

    public void setBackLimit(int backLimit) {
        this.backLimit = backLimit;
    }

    public double getConsequentForce() {
        return consequentForce;
    }

    public void setConsequentForce(double consequentForce) {
        this.consequentForce = consequentForce;
    }

    public double getReverseForce() {
        return reverseForce;
    }

    public void setReverseForce(double reverseForce) {
        this.reverseForce = reverseForce;
    }

    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }

    public int getDpStatus() {
        return dpStatus;
    }

    public void setDpStatus(int dpStatus) {
        this.dpStatus = dpStatus;
    }

    public int getDpMoveway() {
        return dpMoveway;
    }

    public void setDpMoveway(int dpMoveway) {
        this.dpMoveway = dpMoveway;
    }

    public String getDpMemo() {
        return dpMemo;
    }

    public void setDpMemo(String dpMemo) {
        this.dpMemo = dpMemo;
    }

    public int getDp_groupcount() {
        return dp_groupcount;
    }

    public void setDp_groupcount(int dp_groupcount) {
        this.dp_groupcount = dp_groupcount;
    }

    public int getDp_groupnum() {
        return dp_groupnum;
    }

    public void setDp_groupnum(int dp_groupnum) {
        this.dp_groupnum = dp_groupnum;
    }

    public int getDp_relaxtime() {
        return dp_relaxtime;
    }

    public void setDp_relaxtime(int dp_relaxtime) {
        this.dp_relaxtime = dp_relaxtime;
    }

    public int getSpeed_rank() {
        return speed_rank;
    }

    public void setSpeed_rank(int speed_rank) {
        this.speed_rank = speed_rank;
    }

    public String getSysVersion() {
        return sysVersion;
    }

    public void setSysVersion(String sysVersion) {
        this.sysVersion = sysVersion;
    }

    public int getDpId() {
        return dpId;
    }

    public void setDpId(int dpId) {
        this.dpId = dpId;
    }



//    String Uid = "";                        //用户ID
//    String bindId="";                       //手环id
//    int deviceTypeValue = 0;               //设备类型
//    int trainModeValue = 0;                //训练模式
//    int seatHeight = 0;                    //座位高度
//    int backDistance = 0;                  //靠背距离
//    int footboardDistance=0;               //踏板距离
//    double leverAngle = 0D;                 //杠杆角度
//    int frontLimit = 0;                    //前方限制
//    int backLimit = 0;                     //后方限制
//    double consequentForce =0D;            //顺向力
//    double reverseForce=0D;                //反向力
//    double power=0D;                       //功率

    public PersonalSettingDTO() {
    }


}
