package com.bdl.airecovery.biz;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.entity.Device;
import com.bdl.airecovery.entity.Personal;
import com.bdl.airecovery.entity.login.Helperuser;
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
        if (trainMode.equals(BdlProto.TrainMode.STANDARD_MODE)){
            return "标准模式";
        }else if (trainMode.equals(BdlProto.TrainMode.ADAPTER_MODE)){
            return "适应性模式";
        }else if (trainMode.equals(BdlProto.TrainMode.CONST_SPEED_MODE)){
            return "等速模式";
        }else if (trainMode.equals(BdlProto.TrainMode.HEART_RATE_MODE)){
            return "心率模式";
        }else if (trainMode.equals(BdlProto.TrainMode.ADD_MUSCLES_MODE)){
            return "增肌模式";
        }else if (trainMode.equals(BdlProto.TrainMode.ACTIVE_MODE)){
            return "主被动模式";
        }else if (trainMode.equals(BdlProto.TrainMode.PASSIVE_MODE)){
            return "被动模式";
        }else {
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
            case "10":
                return BdlProto.DeviceType.E10;
            case "11":
                return BdlProto.DeviceType.E11;
            case "12":
                return BdlProto.DeviceType.E12;
            case "13":
                return BdlProto.DeviceType.E13;
            case "14":
                return BdlProto.DeviceType.E14;
            case "15":
                return BdlProto.DeviceType.E15;
            case "16":
                return BdlProto.DeviceType.E16;
            default:
                return null;
        }
    }

    /**
     * 1.设置当前对象，区分好是第一用户还是第二用户
     * 2.针对此人与其医护设置执行插入/修改操作
     * 3.需要注意的是第二用户，一定要是教练角色，如果不是教练角色嘛，要把第二用户置空。
     * @param message
     */
    public static void parseLoginRespMsg(BdlProto.Message message) {

        Gson gson = new Gson();

        LogUtil.e("Message : " + gson.toJson(message));
        //个人信息
        User user = null;
        if (message.getLoginResponse().getExisitSetting() == true){
            LogUtil.e("有医护设置");
            user = new User(message);
        }else {
            LogUtil.e("无医护设置");
            user = new User();
        }
        Device device = new Device();//医护设置
        List<Personal> personalList  = new ArrayList<>();
        for (Personal personal : MyApplication.getInstance().getCurrentDevice().getPersonalList()){
            Personal temp = new Personal();
            temp.setName(personal.getName());
            temp.setValue(personal.getValue());
            temp.setMachine(personal.getMachine());
            temp.setCmd(personal.getCmd());
            temp.setMin(personal.getMin());
            temp.setMax(personal.getMax());
            temp.setType(personal.getType());
            personalList.add(temp);
        }
//        LogUtil.d("开始同步个人信息");
        //输出角色
        LogUtil.d(" 角色ID：" + user.getRole());
        LogUtil.d(" 用户标识：" + user.getUserId());
        /*
        处理步骤：
        1.如果教练机查有此人，直接存个人信息存库
        2.分情况讨论
            2.1 第一用户
                2.1.1 查有此人，设置为返回值
                2.1.2 查无此人，设置为默认值
            2.2 第二用户
                2.2.1 查有此人，且必须为教练角色，设置为返回值，不是教练什么都不做。
                2.1.2 查无此人，不能设置默认值。
         3.设置当前用户和当前设备的有限信息（顺反+医护设置）
         */
        //如果此人的医护设置在教练机中存在，则存入本机。
        if (message.getLoginResponse().getExisitSetting() == true){
            //循环类型
            device.setActivityType(message.getLoginResponse().getActivityTypeValue());
            //顺向力
            int forward = (int)message.getLoginResponse().getForwardForce();
            device.setConsequentForce(String.valueOf(forward));
            LogUtil.e("同步完成顺向力" + String.valueOf(forward));
            //反向力
            int reverse = (int)message.getLoginResponse().getReverseForce();
            device.setReverseForce(String.valueOf(reverse));
            LogUtil.e("同步完成反向力" + String.valueOf(reverse));
            //医护设置集合
            for (Personal personal:personalList){
                LogUtil.e("开始同步医护设置集合");
                if(personal.getName().equals("前方限制")){
                    personal.setValue(String.valueOf(message.getLoginResponse().getForwardLimit()));
                    LogUtil.e("前方限制设置成功" + String.valueOf(message.getLoginResponse().getForwardLimit()));
                }else if(personal.getName().equals("后方限制")){
                    personal.setValue(String.valueOf(message.getLoginResponse().getBackLimit()));
                    LogUtil.e("后方限制设置成功" + String.valueOf(message.getLoginResponse().getBackLimit()));
                }else if(personal.getName().equals("靠背距离")){
                    personal.setValue(String.valueOf(message.getLoginResponse().getBackDistance()));
                    LogUtil.e("靠背距离" + String.valueOf(message.getLoginResponse().getBackDistance()));
                }else if(personal.getName().equals("杠杆角度")){
                    personal.setValue(String.valueOf((int)message.getLoginResponse().getLeverAngle()));
                    LogUtil.e("杠杆角度" + String.valueOf(message.getLoginResponse().getLeverAngle()));
                }else if(personal.getName().equals("座位高度")){
                    personal.setValue(String.valueOf(message.getLoginResponse().getSeatHeight()));
                    LogUtil.e("座位高度");
                }else if (personal.getName().equals("初始功率")){
                    int power = Integer.parseInt(String.valueOf((int)message.getLoginResponse().getPower()));
                    LogUtil.e("初始功率");
                    personal.setValue(String.valueOf(power));
                }else if (personal.getName().equals("杠杆长度")){
                    personal.setValue(String.valueOf(message.getLoginResponse().getLeverLength()));
                    LogUtil.e("杠杆长度");
                }
            }
        }
        //如果远程不存在使用默认信息
        else {
            LogUtil.e("使用默认信息");
            List<BdlProto.DeviceType> temp = new ArrayList<>();
            if(CommonUtils.getDeviceType().getNumber() == 1){
                for(int i = 0;i < 10;i++){
                    temp.add(BdlProto.DeviceType.forNumber(i));
                }
            }else{
                for(int i = 10;i < 17;i++){
                    temp.add(BdlProto.DeviceType.forNumber(i));
                }
            }
            user.setHelperuser(null);
            user.setPhone(message.getLoginResponse().getUid().substring(message.getLoginResponse().getUid().length()-4,message.getLoginResponse().getUid().length()));
            user.setRole("trainee");
            user.setUsername(message.getLoginResponse().getUid().substring(0,message.getLoginResponse().getUid().length()-4));
            user.setUserId(message.getLoginResponse().getUid());
            user.setSysVersion(0);
            user.setWeight(80);
            user.setAge(25);
            user.setTrainMode("标准模式");
            user.setHeartRatemMax(140);
            user.setDeviceTypearrList(String.valueOf(temp));
            user.setActivityId(1);
            user.setCourseId(1);
            user.setActivityRecordId(1);
        }
        //TODO 查无此人被吃了
        //第一用户为空，初始化第一用户
        if (MyApplication.getInstance().getUser() == null){
            //初始化全局User对象
            MyApplication.getInstance().setUser(user);
            LogUtil.e("初始化的全局User对象：" + MyApplication.getInstance().getUser().getUserId());
            //存入暂存表
            personalInfoDAO personalInfoDAO = com.bdl.airecovery.personalInfoDAO.getInstance();
            try {
                device.setPersonalList(personalList);
                MyApplication.getInstance().UserUpdateDevice(device);
                LogUtil.e("更新本地设备设置");
                personalInfoDAO.SavrOrUpdata(message.getLoginResponse().getUid(),BdlProto.DeviceType.getDescriptor().getName(),user,device);
                for (Personal personal:MyApplication.getInstance().getCurrentDevice().getPersonalList()) {
                    if (personal.getName().equals("前方限制")) {
                        LogUtil.e("前方限制" + String.valueOf(personal));
                    } else if (personal.getName().equals("后方限制")) {
                        LogUtil.e("后方限制" + String.valueOf(personal));
                    }
                }
                LogUtil.e("当前设备名称：" + MyApplication.getInstance().getCurrentDevice().getDisplayName());
            } catch (DbException e) {
                e.printStackTrace();
                LogUtil.e("数据库更新失败");
                return;
            }
        }

        //第一用户非空，初始化第二用户
        else if(MyApplication.getInstance().getUser() != null &&
                MyApplication.getInstance().getUser().getHelperuser() == null
                && user.getRole().equals("coach")){
            //初始化第二用户
            Helperuser helperuser = new Helperuser();
            //设置登录类型
            helperuser.setType("bluetooth");
            //设置手机号
            helperuser.setPhone(message.getLoginResponse().getUid().substring(message.getLoginResponse().getUid().length()-4));
            //设置用户标识
            helperuser.setUserid(message.getLoginResponse().getUid());
            //设置用户名
            helperuser.setUsername(message.getLoginResponse().getUid().substring(0,message.getLoginResponse().getUid().length()-4));
            //初始化全局Helperuser
            MyApplication.getInstance().getUser().setHelperuser(helperuser);
            LogUtil.e("初始化的全局Helperuser对象：" + MyApplication.getInstance().getUser().getHelperuser().getUserid());
            //存入暂存表
            personalInfoDAO personalInfoDAO = com.bdl.airecovery.personalInfoDAO.getInstance();
            try {
                device.setPersonalList(personalList);
                personalInfoDAO.SavrOrUpdata(message.getLoginResponse().getUid(),BdlProto.DeviceType.getDescriptor().getName(),user,device);
                for (Personal personal:MyApplication.getInstance().getCurrentDevice().getPersonalList()) {
                    if (personal.getName().equals("前方限制")) {
                        LogUtil.e("前方限制" + String.valueOf(personal));
                    } else if (personal.getName().equals("后方限制")) {
                        LogUtil.e("后方限制" + String.valueOf(personal));
                    }
                }
            } catch (DbException e) {
                e.printStackTrace();
                LogUtil.e("数据库更新失败");
                return;
            }
        }
    }
}