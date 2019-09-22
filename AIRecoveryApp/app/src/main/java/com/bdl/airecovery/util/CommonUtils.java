package com.bdl.airecovery.util;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.proto.BdlProto;

/**
 * 根据myapplication对象执行prototype转化
 */
public class CommonUtils {
    /**
     * 根据当前设备类型转化为proto设备类型枚举对象
     * @return
     */
    public static BdlProto.DeviceType getDeviceType(){
        switch (MyApplication.getInstance().getCurrentDevice().getDeviceInnerID()){
            case "0":
                return BdlProto.DeviceType.P00;
            case "1":
                return BdlProto.DeviceType.P01;
            case "2":
                return BdlProto.DeviceType.P02;
            case "3":
                return BdlProto.DeviceType.P03;
            case "4":
                return BdlProto.DeviceType.P04;
            case "5":
                return BdlProto.DeviceType.P05;
            case "6":
                return BdlProto.DeviceType.P06;
            case "7":
                return BdlProto.DeviceType.P07;
            case "8":
                return BdlProto.DeviceType.P08;
            case "9":
                return BdlProto.DeviceType.P09;
            default:
                return null;
        }
    }
    /**
     * 根据当前设备类型转化为proto活动类型枚举对象
     */
//    public static BdlProto.ActivityType getActivityType(){
//        switch (MyApplication.getInstance().getCurrentDevice().getActivityType()){
//            case 0:
//                return BdlProto.ActivityType.forNumber(0);
//            case 1:
//                return BdlProto.ActivityType.forNumber(1);
//            default:
//                return BdlProto.ActivityType.forNumber(-1);
//        }
//    }
    /**
     *  @author zfc
     *  @time 2019/8/14  21:07
     *  @describe 根据当前设备类型转化为proto用户运动模式枚举对象
     */
    public static  BdlProto.TrainMode getTrainMode(int trainMode){
        switch (trainMode){
            case 0:
                return BdlProto.TrainMode.RehabilitationModel;
            case 1:
                return BdlProto.TrainMode.ActiveModel;
            case 2:
                return BdlProto.TrainMode.PassiveModel;
            default:
                return BdlProto.TrainMode.UNRECOGNIZED;
        }
 }




}
