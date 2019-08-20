package com.bdl.airecovery.activity;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;


@ContentView(R.layout.activity_bye)
public class ByeActivity extends BaseActivity{

    /**
     * 获取控件
     */
    //ListView
    @ViewInject(R.id.lv_bye_not)       //未完成的设备列表
    private ListView listView;
    //TextView
    @ViewInject(R.id.pie_chart)        //训练结果
    private PieChartView pieChartView;
    List<TestItem> testItemList;       //连测参数列表
    private Thread locateThread;       //电机连测线程

    //LineChartView
    @ViewInject(R.id.mh_chart)
    private LineChartView mh_chart;         //折线图

    //Button
    @ViewInject(R.id.btn_return)
    private Button btn_return;

    /*折线图相关变量*/
    List<PointValue> mPointValues = new ArrayList<PointValue>();
    List<AxisValue> mAxisXValues = new ArrayList<AxisValue>();


    /**
     * 程序执行入口
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //本次结束心率折线图
        getAxisXLables();
        getAxisPoints();
        initAxisView();
        //隐藏状态栏，导航栏
        initImmersiveMode();
        //获取设备名称、操作电机复位、页面跳转、待训练设备列表
        mainEvent();
        //将训练结果显示在页面并存到暂存表，由重传service上传
        trainResult();
    }
    /**
     * 折线图动画效果
     */
    private void initAxisView() {
        //获取upload对象
        Upload upload = MyApplication.getUpload();
        List<Integer> list=upload.getHeartRateList();
        Line line = new Line(mPointValues).setColor(Color.parseColor("#1E90FF"));//折线的颜色
        List<Line> lines = new ArrayList<Line>();
        line.setShape(ValueShape.CIRCLE);//折线图上每个数据点的形状  这里是圆形 （有三种 ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.SQUARE）
        line.setCubic(false);//曲线是否平滑
//	    line.setStrokeWidth(3);//线条的粗细，默认是3
        line.setFilled(false);//是否填充曲线的面积
        line.setHasLabels(true);//曲线的数据坐标是否加上备注
        //line.setHasLabelsOnlyForSelected(true);//点击数据坐标提示数据（设置了这个line.setHasLabels(true);就无效）
        line.setHasLines(true);//是否用直线显示。如果为false 则没有曲线只有点显示
        line.setHasPoints(true);//是否显示圆点 如果为false 则没有原点只有点显示
        lines.add(line);
        LineChartData data = new LineChartData();
        data.setLines(lines);

        //X坐标轴
        Axis axisX = new Axis(); //X轴
        axisX.setHasTiltedLabels(false);//X坐标轴字体是斜的显示还是直的，true是斜的显示
        axisX.setHasLines(true);//X轴分割线
        axisX.setTextSize(15);//设置字体大小
        axisX.setTextColor(Color.parseColor("#1E90FF"));//黑色
        axisX.setMaxLabelChars(0); //最多几个X轴坐标，意思就是你的缩放让X轴上数据的个数7<=x<=mAxisValues.length
        axisX.setValues(mAxisXValues);  //填充X轴的坐标值
        data.setAxisXBottom(axisX); //x 轴在底部

        //Y坐标轴
        Axis axisY = new Axis();  //Y轴
        axisY.setName("心率(次数)");//添加Y轴的名称
        axisY.setTextSize(30);//设置字体大小
        data.setAxisYLeft(axisY);  //Y轴设置在左边
        axisY.setTextColor(Color.parseColor("#1E90FF"));//设置Y轴颜色，默认浅灰色
        axisY.setHasLines(false);//Y轴分割线

        //设置行为属性，支持缩放、滑动以及平移
        mh_chart.setInteractive(true);//设置不可交互
        mh_chart.setZoomType(ZoomType.HORIZONTAL);  //缩放类型，水平
        mh_chart.setMaxZoom((float) 20);//缩放比例
        mh_chart.setLineChartData(data);
        mh_chart.setVisibility(View.VISIBLE);
        /**注d：下面的7，10只是代表一个数字去类比而已
         * 尼玛搞的老子好辛苦！！！见（http://forum.xda-evelopers.com/tools/programming/library-hellocharts-charting-library-t2904456/page2）;
         * 下面几句可以设置X轴数据的显示个数（x轴0-7个数据），当数据点个数小于（29）的时候，缩小到极致hellochart默认的是所有显示。当数据点个数大于（29）的时候，
         * 若不设置axisX.setMaxLabelChars(int count)这句话,则会自动适配X轴所能显示的尽量合适的数据个数。
         * 若设置axisX.setMaxLabelChars(int count)这句话,
         * 33个数据点测试，若 axisX.setMaxLabelChars(10);里面的10大于v.right= 7; 里面的7，则
         刚开始X轴显示7条数据，然后缩放的时候X轴的个数会保证大于7小于10
         若小于v.right= 7;中的7,反正我感觉是这两句都好像失效了的样子 - -!
         * 并且Y轴是根据数据的大小自动设置Y轴上限
         * 若这儿不设置 v.right= 7; 这句话，则图表刚开始就会尽可能的显示所有数据，交互性太差
         */
        Viewport v = new Viewport(mh_chart.getMaximumViewport());
        v.left = 0;
        v.right= list.size();
        mh_chart.setCurrentViewport(v);
    }

    private  void getAxisXLables() {
        Upload upload = MyApplication.getUpload();
        List<Integer> list = upload.getHeartRateList();
        for (int i = 0; i < list.size(); i++) {
            mAxisXValues.add(new AxisValue(i).setLabel(i + ""));
        }
    }
    private void  getAxisPoints() {
        Upload upload = MyApplication.getUpload();
        List<Integer> list = upload.getHeartRateList();
        for (int i = 0; i < list.size(); i++) {
            mPointValues.add(new PointValue(i, list.get(i)));
        }
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
        if (upload.getFinishNum()!= 0){
            pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFF00FF00, "训练个数:" + upload.getFinishNum()));
        }
        if (upload.getEnergy() != 0D){
            pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFFFF901E, "总耗能:" + (int)upload.getEnergy()));
        }
        if (upload.getConsequentForce() != 0D){
            pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFF00BFFF, "最终顺向力:" + (int)upload.getConsequentForce()));
        }
        if (upload.getReverseForce() != 0D){
            pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFF009ACD, "最终反向力:" + (int)upload.getReverseForce()));
        }
        if (upload.getPower() != 0D){
            pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFF00688B, "最终功率:" + (int)upload.getPower()));
        }
        if (upload.getSpeedRank() != 0){
            pieceDataHolders.add(new PieChartView.PieceDataHolder(1200, 0xFF00688B, "运动速度:" + (int)upload.getSpeedRank()));
        }
        pieChartView.setData(pieceDataHolders);
        //从user获取上传的信息
        if (MyApplication.getInstance().getUser() != null){
            upload.setUid(MyApplication.getInstance().getUser().getUserId());;
            //解析训练模式
            if (MyApplication.getInstance().getUser().getTrainMode().equals("康复模式")){
                upload.setTrainMode(0);
            }
            if (MyApplication.getInstance().getUser().getTrainMode().equals("主动模式")){
                upload.setTrainMode(1);
            }
            if (MyApplication.getInstance().getUser().getTrainMode().equals("被动模式")){
                upload.setTrainMode(2);
            }
        }
        //从Device获取上传结果信息
        if (MyApplication.getInstance().getCurrentDevice() != null){
            //解析设备类型
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("坐式划船机")){
                upload.setDeviceType(0);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("坐式推胸机")){
                upload.setDeviceType(1);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("腿部推蹬机")){
                upload.setDeviceType(2);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("腹肌训练机")){
                upload.setDeviceType(3);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("三头肌训练机")){
                upload.setDeviceType(4);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("腿部外弯机")){
                upload.setDeviceType(5);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("腿部内弯机")){
                upload.setDeviceType(6);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("蝴蝶机")){
                upload.setDeviceType(7);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("反向蝴蝶机")){
                upload.setDeviceType(8);
            }
            if (MyApplication.getInstance().getCurrentDevice().getDeviceName().equals("坐式背部伸展机")){
                upload.setDeviceType(9);
            }
        }
        //打包训练结果
        TrainResultDTO trainResultDTO = new TrainResultDTO();
        trainResultDTO.setUid_(upload.getUid());
        trainResultDTO.setTrainModeValue_(upload.getTrainMode());
        trainResultDTO.setDeviceTypeValue_(upload.getDeviceType());
        trainResultDTO.setReverseForce_(upload.getReverseForce());
        trainResultDTO.setForwardForce_(upload.getConsequentForce());
        trainResultDTO.setPower_(upload.getPower());
        trainResultDTO.setSpeedRank(upload.getSpeedRank());
        trainResultDTO.setFinishNum_(upload.getFinishNum());
        trainResultDTO.setEnergy_(upload.getEnergy());
        //转换心率类型
        List<Integer> list = upload.getHeartRateList();
        String HeartRate = "";
        if (list != null && list.size() > 0) {
            for (Object item : list) {
                // 把列表中的每条数据用逗号分割开来，然后拼接成字符串
                HeartRate += item + "*";
            }
            // 去掉最后一个逗号
            HeartRate = HeartRate.substring(0, HeartRate.length() - 1);
        }
        //保存心率
        trainResultDTO.setHeart_rate_list(HeartRate);

        //保存手环id
        trainResultDTO.setBindId_(MyApplication.getInstance().getUser().getBindId());
        //保存处方id
        trainResultDTO.setDpId_(MyApplication.getInstance().getUser().getDpId());
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
            String str4 = str3.replace("P00", "坐式划船机");
            String str5 = str4.replace("P01", "坐式推胸机");
            String str6 = str5.replace("P02", "腿部推蹬机");
            String str7 = str6.replace("P03", "腹肌训练机");
            String str8 = str7.replace("P04", "三头肌训练机");
            String str9 = str8.replace("P05", "腿部外弯机");
            String str10 = str9.replace("P06", "腿部内弯机");
            String str11 = str10.replace("P07", "蝴蝶机");
            String str12 = str11.replace("P08", "反向蝴蝶机");
            String str13 = str12.replace("P09", "坐式背部伸展机");
            //将字符串转为集合
            List<String> result = Arrays.asList(str11.split(","));
            //将待训练设备信息设置在页面上
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,R.layout.list_equipment_bye,result);
            listView.setAdapter(adapter);
        }

        //TODO：给电机发送复位指令
        init();
        //十五秒之后跳转到待机页面
        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                //断开蓝牙连接
                if (MyApplication.getInstance().getUser() != null){
                    Intent intentLog = new Intent(ByeActivity.this, BluetoothService.class);
                    intentLog.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
                    startService(intentLog);
                    LogUtil.e("再见页面结束时，蓝牙第一用户退出");
                }else if (MyApplication.getInstance().getUser() != null){
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

    //按钮监听事件，返回待机页面
    @Event(R.id.btn_return)
    private void setBtn_return(View v) {
        //TODO：给电机发送复位指令
        init();
        //十五秒之后跳转到待机页面
        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                //断开蓝牙连接
                if (MyApplication.getInstance().getUser() != null){
                    Intent intentLog = new Intent(ByeActivity.this, BluetoothService.class);
                    intentLog.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
                    startService(intentLog);
                    LogUtil.e("再见页面结束时，蓝牙第一用户退出");
                }else if (MyApplication.getInstance().getUser() != null){
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
        } , 10);
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
