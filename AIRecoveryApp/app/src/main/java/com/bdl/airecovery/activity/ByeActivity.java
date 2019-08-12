package com.bdl.airecovery.activity;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.R;
import com.bdl.airecovery.base.BaseActivity;
import com.bdl.airecovery.bluetooth.CommonCommand;
import com.bdl.airecovery.entity.DTO.TrainResultDTO;
import com.bdl.airecovery.entity.Device;
import com.bdl.airecovery.entity.TempStorage;
import com.bdl.airecovery.entity.TestItem;
import com.bdl.airecovery.entity.Upload;
import com.bdl.airecovery.entity.login.User;
import com.bdl.airecovery.service.BluetoothService;
import com.bdl.airecovery.widget.PieChartView;
import com.google.gson.Gson;

import org.xutils.DbManager;
import org.xutils.common.util.LogUtil;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


@ContentView(R.layout.activity_bye)
public class ByeActivity extends BaseActivity{

    /**
     * 获取控件
     */
    //ListView
    @ViewInject(R.id.lv_bye_not)       //未完成的设备列表
    private ListView listView;
    //TextView
    @ViewInject(R.id.tv_bye_complete)  //已完成的设备
    private TextView complete;
    @ViewInject(R.id.tv_bye_removeFat) //减脂
    private TextView removeFat;
    @ViewInject(R.id.tv_bye_removeFat1) //减脂
    private TextView removeFat1;
    @ViewInject(R.id.tv_bye_removeFat2) //减脂
    private TextView removeFat2;
    @ViewInject(R.id.pie_chart)        //训练结果
    private PieChartView pieChartView;
    List<TestItem> testItemList;       //连测参数列表
    private Thread locateThread;       //电机连测线程


    /**
     * 程序执行入口
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐藏状态栏，导航栏
        initImmersiveMode();
        //获取设备名称、操作电机复位、页面跳转、待训练设备列表
        mainEvent();
        //将训练结果显示在页面并存到暂存表，由重传service上传
        trainResult();
    }

    /**
     * 将训练结果显示在页面并存到暂存表，由重传service上传
     */
    private void trainResult(){
        //获取DbManager
        DbManager dbManager = MyApplication.getInstance().getDbManager();
        //取得上传结果类
        Upload upload = MyApplication.getUpload();
        MyApplication.setUpload(null);
        //获取训练结果，显示在页面
        List<PieChartView.PieceDataHolder> pieceDataHolders = new ArrayList<>();
        //在页面显示训练结果
        //测试用可注掉
        //
//        pieceDataHolders.add(new PieChartView.PieceDataHolder(1200,0xFF00CD00, "训练时间:" + upload.getTrainTime_()));
//        pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFF00FF00, "训练个数:" + upload.getFinishCount_()));
//        pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFFFF901E, "耗能:" + upload.getCalorie_()));
//        pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFF00BFFF, "最终顺向力:" + (int)upload.getForwardForce_()));
//        pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFF00B2EE, "运动距离:" + upload.getFinalDistance_()));
//        pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFF009ACD, "最终反向力:" + (int)upload.getReverseForce_()));
//        pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFF00688B, "最终功率:" + upload.getPower_()));
//        pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFFCD2626, "最大心率:" + upload.getHeartRateMax_()));
//        pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFFFF4040, "最小心率" + upload.getHeartRateMin_()));
//        pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFFFF3030, "平均心率:" + upload.getHeartRateAvg_()));
        //实际用下方的
        if(upload.getTrainTime_() != 0){
            pieceDataHolders.add(new PieChartView.PieceDataHolder(1200,0xFF00CD00, "训练时间:" + upload.getTrainTime_()));
        }
        if (upload.getFinishCount_() != 0){
            pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFF00FF00, "训练个数:" + upload.getFinishCount_()));
        }
        if (upload.getCalorie_() != 0D){
            pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFFFF901E, "耗能:" + (int)upload.getCalorie_()));
        }
        if (upload.getForwardForce_() != 0D){
            pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFF00BFFF, "最终顺向力:" + (int)upload.getForwardForce_()));
        }
        if (upload.getFinalDistance_() != 0D){
            pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFF00B2EE, "运动距离:" + (int)upload.getFinalDistance_()));
        }
        if (upload.getReverseForce_() != 0D){
            pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFF009ACD, "最终反向力:" + (int)upload.getReverseForce_()));
        }
        if (upload.getPower_() != 0D){
            pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFF00688B, "最终功率:" + (int)upload.getPower_()));
        }
        if (upload.getHeartRateMax_() != 0){
            pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFFCD2626, "最大心率:" + upload.getHeartRateMax_()));
        }
        if (upload.getHeartRateMin_() != 0){
            pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFFFF4040, "最小心率:" + upload.getHeartRateMin_()));
        }
        if (upload.getHeartRateAvg_() != 0){
            pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFFFF3030, "平均心率:" + upload.getHeartRateAvg_()));
        }
        pieChartView.setData(pieceDataHolders);
        //从user获取上传的信息
        if (MyApplication.getInstance().getUser() != null){
            upload.setUid_(MyApplication.getInstance().getUser().getUserId());
           // upload.setTrainMode_(MyApplication.getInstance().getUser().getTrainMode());
            //解析训练模式
            if (MyApplication.getInstance().getUser().getTrainMode().equals("标准模式")){
                upload.setTrainMode_(0);
            }
            if (MyApplication.getInstance().getUser().getTrainMode().equals("适应性模式")){
                upload.setTrainMode_(1);
            }
            if (MyApplication.getInstance().getUser().getTrainMode().equals("等速模式")){
                upload.setTrainMode_(2);
            }
            if (MyApplication.getInstance().getUser().getTrainMode().equals("心率模式")){
                upload.setTrainMode_(3);
            }
            if (MyApplication.getInstance().getUser().getTrainMode().equals("增肌模式")){
                upload.setTrainMode_(4);
            }
            if (MyApplication.getInstance().getUser().getTrainMode().equals("主动模式")){
                upload.setTrainMode_(5);
            }
            if (MyApplication.getInstance().getUser().getTrainMode().equals("被动模式")){
                upload.setTrainMode_(6);
            }
            upload.setCourseId_(MyApplication.getInstance().getUser().getCourseId());
            upload.setActivityId_(MyApplication.getInstance().getUser().getActivityId());
            upload.setActivityRecordId_(MyApplication.getInstance().getUser().getActivityRecordId());
            upload.setDefatModeEnable_(MyApplication.getInstance().getUser().isDefatModeEnable());
            //减脂提示信息
            if (upload.isDefatModeEnable_() == true){
                removeFat.setVisibility(View.VISIBLE);
                removeFat1.setVisibility(View.VISIBLE);
                removeFat2.setVisibility(View.VISIBLE);
            }
        }
        //从Device获取上传结果信息
        if (MyApplication.getInstance().getCurrentDevice() != null){
            //解析设备类型
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("腿部推蹬机(力量循环)")){
               upload.setDeviceType_(0);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("坐式背阔肌高拉机(力量循环)")){
                upload.setDeviceType_(1);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("三头肌训练机(力量循环)")){
                upload.setDeviceType_(2);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("腿部内弯机(力量循环)")){
                upload.setDeviceType_(3);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("腿部外弯机(力量循环)")){
                upload.setDeviceType_(4);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("蝴蝶机(力量循环)")){
                upload.setDeviceType_(5);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("反向蝴蝶机(力量循环)")){
                upload.setDeviceType_(6);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("坐式背部伸展机(力量循环)")){
                upload.setDeviceType_(7);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("躯干扭转组合(力量循环)")){
                upload.setDeviceType_(8);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("坐式腿伸展训练机(力量耐力循环)")){
                upload.setDeviceType_(9);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("坐式推胸机(力量耐力循环)")){
                upload.setDeviceType_(10);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("坐式划船机(力量耐力循环)")){
                upload.setDeviceType_(11);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("椭圆跑步机(力量耐力循环)")){
                upload.setDeviceType_(12);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("坐式屈腿训练机(力量耐力循环)")){
                upload.setDeviceType_(13);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("腹肌训练机(力量耐力循环)")){
                upload.setDeviceType_(14);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("坐式背部伸展机(力量耐力循环)")){
                upload.setDeviceType_(15);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("健身车(力量耐力循环)")){
                upload.setDeviceType_(16);
            }
            upload.setActivityType_(MyApplication.getInstance().getCurrentDevice().getActivityType());
        }
        //打包训练结果
        TrainResultDTO trainResultDTO = new TrainResultDTO();
        //从user及device获取
        trainResultDTO.setUid_(upload.getUid_());
        trainResultDTO.setTrainModeValue_(upload.getTrainMode_());
        trainResultDTO.setCourseId_(upload.getCourseId_());
        trainResultDTO.setActivityId_(upload.getActivityId_());
        trainResultDTO.setActivityRecordId_(upload.getActivityRecordId_());
        trainResultDTO.setDeviceTypeValue_(upload.getDeviceType_());
        trainResultDTO.setActivityTypeValue_(upload.getActivityType_());
        trainResultDTO.setDefatModeEnable_(upload.isDefatModeEnable_());
        //从activity获取
        trainResultDTO.setReverseForce_(upload.getReverseForce_());
        trainResultDTO.setForwardForce_(upload.getForwardForce_());
        trainResultDTO.setPower_(upload.getPower_());
        trainResultDTO.setFinishCount_(upload.getFinishCount_());
        trainResultDTO.setFinalDistance_(upload.getFinalDistance_());
        trainResultDTO.setCalorie_(upload.getCalorie_());
        trainResultDTO.setTrainTime_(upload.getTrainTime_());
        trainResultDTO.setHeartRateAvg_(upload.getHeartRateAvg_());
        trainResultDTO.setHeartRateMax_(upload.getHeartRateMax_());
        trainResultDTO.setHeartRateMin_(upload.getHeartRateMin_());
        //存暂存表
        TempStorage tempStorage = new TempStorage();
        Gson gson = new Gson();
        //转为json数据
        tempStorage.setData(gson.toJson(trainResultDTO));
        //重传类型
        tempStorage.setType(2);
        //判断训练结果是否保存数据库
        if(MyApplication.getInstance().getUser() != null && MyApplication.getInstance().getUser().getUserId() != null) {
            try {
                //存到数据库
                dbManager.saveBindingId(tempStorage);
            } catch (DbException dbe) {
                dbe.printStackTrace();
                Log.d("TrainResultDTO", "训练结果暂存失败");
            }
        }

    }

    /**
     * 电机复位
     */
    public void init(){
        //电机复位线程
        locateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Device device = MyApplication.getInstance().getCurrentDevice();
                if (device != null){
                    for (int i = 0;i < device.getTestItemList().size();++i){
                        if (device.getTestItemList().get(i).getMachine() != "" && device.getTestItemList().get(i).getMachine().equals("动态电机")){
                            //处理动态电机
//                            try {
//                                boolean[] flag ={};
//                                flag = Location.motorInit();
//
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
                        }else {
//                            //处理静态电机
//                            StaticMotorService.Controler controler = StaticMotorService.getControler();
//                            if (controler != null){
//                               boolean inir = controler.initLocate(Integer.valueOf(device.getTestItemList().get(i).getMachine()));
//                            }

                        }
                    }
                }
            }
        });
        locateThread.start();
    }

    /**
     * 获取设备名称、操作电机复位、页面跳转、待训练设备
     */
    private void  mainEvent(){

        //待训练设备列表
        //取得用户对象
        User user = MyApplication.getInstance().getUser();

        if (user != null){
            //String str1 ="[ P00,   P01,P02,P03,P04,P05,P06,P07,P08,P09,E10,E11,E12,E13,E14,E15,E16,E28,E10]";
           // String str1 ="[E11,E12,E13,E14,P05,P06,P07,P08,P09,E15,E16,E28,E10]";
            String str1 =user.getDeviceTypearrList();
            //去掉首尾[]
            String str0 = str1.replaceAll("\\s","");
            String str2 = str0.substring(1, str0.length());
            String str3 = str2.substring(0,str2.length()-1);
            //解析训练结果
            String str4 = str3.replace("P00", "腿部推蹬机");
            String str5 = str4.replace("P01", "坐式背阔肌高拉机");
            String str6 = str5.replace("P02", "三头肌训练机");
            String str7 = str6.replace("P03", "腿部内弯机");
            String str8 = str7.replace("P04", "腿部外弯机");
            String str9 = str8.replace("P05", "蝴蝶机");
            String str10 = str9.replace("P06", "反向蝴蝶机");
            String str11 = str10.replace("P07", "坐式背部伸展机");
            String str12 = str11.replace("P08", "躯干扭转组合");
            String str13 = str12.replace("P09", "坐式腿伸展训练机");
            String str14 = str13.replace("E10", "坐式推胸机");
            String str15 = str14.replace("E11", "坐式划船机");
            String str16 = str15.replace("E12", "椭圆跑步机");
            String str17 = str16.replace("E13", "坐式屈腿训练机");
            String str18 = str17.replace("E14", "腹肌训练机");
            String str19 = str18.replace("E15", "坐式背部伸展机");
            String str20 = str19.replace("E16", "健身车");
            //将字符串转为集合
            List<String> result = Arrays.asList(str20.split(","));
            //将待训练设备信息设置在页面上
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,R.layout.list_equipment_bye,result);
            listView.setAdapter(adapter);
        }
        //获取设备名称
        Device device = MyApplication.getInstance().getCurrentDevice(); //获取设备信息
        if (device == null || device.getDisplayName() == null || device.getDisplayName().equals("")){
            complete.setText("");
        }else {
            complete.setText(device.getDisplayName());         //获得设备名称并设置到文本中

        }
        //TODO：给电机发送复位指令
        init();
        //十五秒之后跳转到待机页面
        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                //断开蓝牙连接
                if (MyApplication.getInstance().getUser() != null
                        && MyApplication.getInstance().getUser().getType().equals("bluetooth")){
                    Intent intentLog = new Intent(ByeActivity.this, BluetoothService.class);
                    intentLog.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
                    startService(intentLog);
                    LogUtil.e("再见页面结束时，蓝牙第一用户退出");
                }else if (MyApplication.getInstance().getUser() != null
                        && MyApplication.getInstance().getUser().getHelperuser() != null
                        && MyApplication.getInstance().getUser().getHelperuser().getType().equals("bluetooth")){
                    Intent intentLog = new Intent(ByeActivity.this, BluetoothService.class);
                    intentLog.putExtra("command", CommonCommand.SECOND__LOGOUT.value());
                    startService(intentLog);
                    LogUtil.e("再见页面结束时，蓝牙第二用户退出");
                }
                //置空用户
                MyApplication.getInstance().setUser(null);
                startActivity(new Intent(ByeActivity.this,LoginActivity.class));
                t.cancel();
                finish(); //关闭本activity
            }
        } , 15000);
    }


    /**
     * 隐藏状态栏，导航栏
     */
    @SuppressLint("NewApi")
    private void initImmersiveMode() {
        if (Build.VERSION.SDK_INT >= 19) {
            View.OnSystemUiVisibilityChangeListener listener = new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        enterImmersiveMode();
                    }
                }
            };
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(listener);
            enterImmersiveMode();
        }
    }
    @SuppressLint("NewApi")
    private void enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

}
