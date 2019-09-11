package com.bdl.airecovery.biz;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.entity.Device;
import com.bdl.airecovery.entity.Personal;
import com.bdl.airecovery.entity.login.User;
import com.bdl.airecovery.personalInfoDAO;
import com.bdl.airecovery.proto.BdlProto;
import com.bdl.airecovery.util.CommonUtils;
import com.google.gson.Gson;

import org.xutils.common.util.LogUtil;
import org.xutils.ex.DbException;

import java.util.ArrayList;
import java.util.List;

public class LoginUtils {
    /**
     * 解析训练模式
     * @param trainMode
     * @return
     */
    public static String getTrainMode(BdlProto.TrainMode trainMode){

         if (trainMode.equals(BdlProto.TrainMode.ActiveModel)){
            return "主被动模式";
        }else if (trainMode.equals(BdlProto.TrainMode.PassiveModel)){
            return "被动模式";
         }
         else if(trainMode.equals(BdlProto.TrainMode.RehabilitationModel)){
             return "康复模式";
         }
        else {
             return "未知模式";
         }

    }
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
     * 根据响应数据更新Device类
     */
    public static void parseLoginRespMsg() {
        User user = MyApplication.getInstance().getUser();
        if (user == null) {
            LogUtil.e("全局User为空");
            return;
        }
        Device device = MyApplication.getInstance().getCurrentDevice();
        if (device == null) {
            LogUtil.e("全局Device为空");
            return;
        }
        List<Personal> personalList  = device.getPersonalList();
        if (personalList == null) {
            LogUtil.e("设备参数为空");
            return;
        }

        //有医护设置
        if (user.isExisitSetting()) {
            int consequentForce = (int) user.getConsequentForce();
            int reverseForce = (int) user.getReverseForce();
            device.setConsequentForce(String.valueOf(consequentForce));
            device.setReverseForce(String.valueOf(reverseForce));

            //医护设置集合
            for (Personal personal : personalList) {
                LogUtil.e("开始同步医护设置集合");
                switch (personal.getName()) {
                    case "前方限制":
                        personal.setValue(String.valueOf(user.getForwardLimit()));
                        break;
                    case "后方限制":
                        personal.setValue(String.valueOf(user.getBackLimit()));
                        break;
                    case "靠背距离":
                        personal.setValue(String.valueOf(user.getBackDistance()));
                        break;
                    case "杠杆角度":
                        personal.setValue(String.valueOf((int) user.getLeverAngle()));
                        break;
                    case "座位高度":
                        personal.setValue(String.valueOf(user.getSeatHeight()));
                        break;
                    case "初始功率":
                        personal.setValue(String.valueOf((int) user.getPower()));
                        break;
                    case "踏板距离":
                        personal.setValue(String.valueOf(user.getFootboardDistance()));
                        break;
                    default:
                        break;
                }
            }
            //保存医护设置至全局Device类
            device.setPersonalList(personalList);
        }
    }

}
