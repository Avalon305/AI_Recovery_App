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
//            case "10":
//                return BdlProto.DeviceType.E10;
//            case "11":
//                return BdlProto.DeviceType.E11;
//            case "12":
//                return BdlProto.DeviceType.E12;
//            case "13":
//                return BdlProto.DeviceType.E13;
//            case "14":
//                return BdlProto.DeviceType.E14;
//            case "15":
//                return BdlProto.DeviceType.E15;
//            case "16":
//                return BdlProto.DeviceType.E16;
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




}
