package com.bdl.airecovery.activity;


import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.R;
import com.bdl.airecovery.base.BaseActivity;
import com.bdl.airecovery.constant.MotorConstant;
import com.bdl.airecovery.contoller.MotorProcess;
import com.bdl.airecovery.contoller.Writer;
import com.bdl.airecovery.dialog.SmallPwdDialog;
import com.bdl.airecovery.entity.Device;
import com.bdl.airecovery.entity.TestItem;
import com.bdl.airecovery.service.MotorService;
import com.bdl.airecovery.service.StaticMotorService;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@ContentView(R.layout.activity_location)
public class LocationActivity extends BaseActivity {

    /**
     * 定义成员变量
     */
    List<TestItem> testItemList;         //连测参数列表
    List<ImageView> testItemImg;
    List<TextView> testItemShow;         //需要显示的连测参数列表
    List<TextView> testItemRes;          //需要显示的连测结果列表
//    List<Boolean> locateRes;             //连测结果

    int[] locateRes = {0,0,0};           //连测结果，0为未知，1为成功，2为失败
    int a = 0;                           //统计连测成功的个数
    int b = 0;
    protected static final String ACTIVITY_TAG = "MyAndroid";
    private Thread locateThread;         //电机连测线程
    private Handler handler;             //处理message对象


    /**
     * 获取控件
     */
    //TextView
    @ViewInject(R.id.tv_location_tips)  //提示信息文本
    private TextView tips;

    @ViewInject(R.id.tv_location_item1)
    private TextView item1;              //要显示的连测参数1
    @ViewInject(R.id.tv_location_item2)
    private TextView item2;              //要显示的连测参数2
    @ViewInject(R.id.tv_location_item3)
    private TextView item3;              //要显示的连测参数3
    @ViewInject(R.id.tv_location_item1res)
    private TextView item1Res;           //连测参数1的连测结果
    @ViewInject(R.id.tv_location_item2res)
    private TextView item2Res;           //连测参数2的连测结果
    @ViewInject(R.id.tv_location_item3res)
    private TextView item3Res;           //连测参数3的连测结果
    //ImageView
    @ViewInject(R.id.iv_location_1)
    private ImageView imageView1;        //第一个箭头图标
    @ViewInject(R.id.iv_location_2)
    private ImageView imageView2;        //第二个箭头图标
    @ViewInject(R.id.iv_location_3)
    private ImageView imageView3;        //第二个箭头图标
//    @ViewInject(R.id.iv_location_wheel) //齿轮动画
//    private ImageView location_wheel;
    //Button
    @ViewInject(R.id.btn_location_again)
    private Button againButton; //再次定位按钮
    @ViewInject(R.id.btn_test)
    private Button btn_test;  //跳转待机界面测试按钮
    @ViewInject(R.id.gear)
    private ImageView gear;

    @ViewInject(R.id.gear_medium)
    private ImageView gearMedium;

    @ViewInject(R.id.gear_small)
    private ImageView gearSmall;
    //广播对象
    locationReceiver LocationReceiver = new locationReceiver();
    IntentFilter filterHR = new IntentFilter();
    /**
     * 程序执行入口
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐藏状态栏，导航栏
        initImmersiveMode();
        //查询连测参数，进行定位显示
        try {
            Writer.setParameter(1, MotorConstant.MOTOR_ENABLE);
            Thread.sleep(1000);
            Writer.setParameter(0, MotorConstant.MOTOR_ENABLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        queryTestItem();
        //广播注册
        filterHR.addAction("location");
        registerReceiver(LocationReceiver, filterHR);
    }

    @Override
    protected void onResume() {
        ObjectAnimator objectAnimatorSmallGear;
        objectAnimatorSmallGear = ObjectAnimator.ofFloat(gearSmall, "rotation", 360f);
        objectAnimatorSmallGear.setRepeatCount(Integer.MAX_VALUE);
        objectAnimatorSmallGear.setDuration(3000);
        objectAnimatorSmallGear.setInterpolator(new LinearInterpolator());
        objectAnimatorSmallGear.start();

        ObjectAnimator objectAnimatorMediumGear;
        objectAnimatorMediumGear = ObjectAnimator.ofFloat(gearMedium, "rotation", 360f, 0f);
        objectAnimatorMediumGear.setRepeatCount(Integer.MAX_VALUE);
        objectAnimatorMediumGear.setDuration(3000);
        objectAnimatorMediumGear.setInterpolator(new LinearInterpolator());
        objectAnimatorMediumGear.start();
        ObjectAnimator objectAnimatorGear;

        objectAnimatorGear = ObjectAnimator.ofFloat(gear, "rotation", 360f);
        objectAnimatorGear.setRepeatCount(Integer.MAX_VALUE);
        objectAnimatorGear.setDuration(3000);
        objectAnimatorGear.setInterpolator(new LinearInterpolator());
        objectAnimatorGear.start();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(LocationReceiver);
        super.onDestroy();
    }

    /**
     * 动态电机广播接收类
     */
    public class locationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            try {
                if (intentAction.equals("location")) {
                    //将连测结果添加到集合中
                    if (intent.getBooleanExtra("state", false)) {
//                        locateRes.add(true);
                        locateRes[0] = 1;
                        Message message = handler.obtainMessage();
                        message.what = 0;
                        handler.sendMessage(message); //把一个包含消息数据的Message对象压入到消息队列中
                    }
                    if (!intent.getBooleanExtra("state", false)) {
//                        locateRes.add(false);
                        locateRes[0] = 2;
                        Message message = handler.obtainMessage();
                        message.what = 0;
                        handler.sendMessage(message); //把一个包含消息数据的Message对象压入到消息队列中
                    }
                }
                Log.e("23333333", String.valueOf(intent.getBooleanExtra("state", false)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理电机的指令
     */
    public void command() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Device device = MyApplication.getInstance().getCurrentDevice();
                int successNum = 0;
                //String s = device.getTestItemList().get(0).getMachine();
                //Toast.makeText(LocationActivity.this,"" + s,Toast.LENGTH_SHORT).show();
                if (msg.what == 0) {
                    Log.e("--------------", String.valueOf(device.getTestItemList().size()));
                    Log.d("静态", "成功联测的个数: "+a);
                    Log.d("静态", "需要联测的个数: "+device.getTestItemList().size());
//                    Log.e("--------------", String.valueOf(locateRes.size()));
                    Log.e("--------------", String.valueOf(testItemRes.size()));
                    for (int i = 0; i < device.getTestItemList().size(); ++i) {

                        if (device.getTestItemList().get(i).getMachine() != "") {
                            if (locateRes.length >= i + 1) {
                                if (locateRes[i] == 2) {
                                    testItemRes.get(i).setText("连测定位失败");
                                    testItemRes.get(i).setTextColor(getResources().getColor(R.color.qmui_config_color_red));
                                    //设置显示再次定位按钮
                                    againButton.setVisibility(View.VISIBLE);
                                    //设置只有当连测失败才能做点击响应
                                    againButton.setEnabled(true);
                                } else if (locateRes[i] == 1) {
                                    testItemRes.get(i).setText("连测定位完成");
                                    testItemRes.get(i).setTextColor(getResources().getColor(R.color.green));
                                    //统计连测成功的个数
                                    ++successNum;
                                }
                            } else {
                                Log.e("Locationtttttt", "handleMessage: indexoutofbounds");
                            }


//                            if (locateRes.size() >= i + 1) {
//                                if (locateRes.get(i) == false) {
//                                    testItemRes.get(i).setText("连测定位失败");
//                                    testItemRes.get(i).setTextColor(getResources().getColor(R.color.qmui_config_color_red));
//                                    //设置显示再次定位按钮
//                                    againButton.setVisibility(View.VISIBLE);
//                                    //设置只有当连测失败才能做点击响应
//                                    againButton.setEnabled(true);
//                                } else if (locateRes.get(i) == true) {
//                                    testItemRes.get(i).setText("连测定位完成");
//                                    testItemRes.get(i).setTextColor(getResources().getColor(R.color.green));
//                                    //统计连测成功的个数
//                                    ++a;
//                                }
//                            } else {
//                                Log.e("Locationtttttt", "handleMessage: indexoutofbounds");
//                            }
                        }
                    }
                    a = successNum;
                    if (a == device.getTestItemList().size()) {
                        //两秒之后跳转到待机页面
                        final Timer t = new Timer();
                        t.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                //处理跳转到系统设置时，不再跳转到待机页面
                                if (b == 1) {

                                } else {
                                    startActivity(new Intent(LocationActivity.this, LoginActivity.class));
                                    t.cancel();
                                    finish(); //关闭本activity
                                }

                            }
                        }, 2000);
                    }
                }
            }
        };
        //电机连测线程
        locateThread = new Thread(new Runnable() {
            @Override
            public void run() {

                Device device = MyApplication.getInstance().getCurrentDevice();
                if (device != null) {
//                    locateRes = new ArrayList<Boolean>();
                    locateRes = new int[]{0,0,0};
                    a = 0;
                    //获取连测结果
                    for (int i = 0; i < device.getTestItemList().size(); ++i) {
                        Log.d("静态", "device.getTestItemList().get(i).getMachine():"+device.getTestItemList().get(i).getMachine());
                        if (device.getTestItemList().get(i).getMachine() != "" && device.getTestItemList().get(i).getMachine().equals("动态电机")) {
                            Log.e("=========", String.valueOf(device.getTestItemList().size()));
                            //处理动态电机
                            try {
                                MotorService.getInstance().motorLocation();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

//                                try {
//                                    boolean[] flag ={};
//                                    //将连测结果添加到集合中
//                                    for (int r = 0;r <flag.length;++r){
//                                        locateRes.add(flag[r]);
//                                    }
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
                        } else if (device.getTestItemList().get(i).getMachine() != ""){

                            //处理静态电机
                            Log.d("静态", "run: 准备了！！！！！！！！！");
                            StaticMotorService.Controler controler = StaticMotorService.getControler();
                            if (controler != null) {
                                //将连测结果添加到集合中
                                Log.d("静态", "run: 联测了！！！！！！");
                                boolean result = controler.initLocate(Integer.valueOf(device.getTestItemList().get(i).getMachine()), (device.getTestItemList().get(i).getType().compareTo("1") == 0)?true:false);
                                Log.d("静态", "run: 结果："+result);
                                if (result){
                                    locateRes[i] = 1;
                                }else if (!result){
                                    locateRes[i] = 2;
                                }
//                                locateRes.add(controler.initLocate(Integer.valueOf(device.getTestItemList().get(i).getMachine())));
                            }else {
                                Log.d("静态", "run: null了！！！！！！！！！！");
                            }

                        }
                    }
                    if (("" + locateRes).equals("[]")) {

                    } else {
                        Log.e("898989898", "8989898");
                        Message message = handler.obtainMessage();
                        message.what = 0;
                        handler.sendMessage(message); //把一个包含消息数据的Message对象压入到消息队列中
                    }
                }
            }
        });
        locateThread.start();
    }

    /**
     * 连测参数定位显示逻辑
     */
    private void queryTestItem() {

        //将要显示的连测参数列表添加到集合中
        testItemShow = new ArrayList<TextView>();
        testItemShow.add(item1);
        testItemShow.add(item2);
        testItemShow.add(item3);
        //将要显示的连测参数结果列表添加到集合中
        testItemRes = new ArrayList<TextView>();
        testItemRes.add(item1Res);
        testItemRes.add(item2Res);
        testItemRes.add(item3Res);

        testItemImg = new ArrayList<>();
        testItemImg.add(imageView1);
        testItemImg.add(imageView2);
        testItemImg.add(imageView3);

        //获取设备信息
        Device device = MyApplication.getInstance().getCurrentDevice();
        //判断设备信息是否为空
        if (device == null || device.getDisplayName() == null || device.getDisplayName().equals("")) {
            //如果设备信息为空则显示如下文本
            tips.setText("请点击动画图标来选择设备");
        } else {
            //获取电机的连测参数列表
            testItemList = device.getTestItemList();
            //连测参数遍历显示
            for (int i = 0; i < testItemList.size(); ++i) {
                if (testItemList.get(i).getName() != "") {
                    //加载连测参数的文本信息
                    testItemImg.get(i).setImageDrawable(getResources().getDrawable(R.drawable.ic_arrow_right));
                    testItemShow.get(i).setText(testItemList.get(i).getName());
                    //发指令时，显示正在初始化定位
                    testItemRes.get(i).setText("初始化定位中");
                    testItemRes.get(i).setTextColor(getResources().getColor(R.color.qmui_config_color_blue));
                }
            }
            //处理指令
            command();
        }
    }

    /**
     * 再次定位按钮的点击事件
     *
     * @param v
     */
    // @SuppressLint("WrongConstant")
    @Event(R.id.btn_location_again)
    private void againClick(View v) throws Exception {
        MotorProcess.restoration(); //复位

        for (int i = 0; i < testItemList.size(); ++i) {
            if (testItemList.get(i).getName() != "") {
                //发指令时，显示正在初始化定位
                testItemRes.get(i).setText("初始化定位中");
                testItemRes.get(i).setTextColor(getResources().getColor(R.color.qmui_config_color_blue));
            }
        }
        //处理指令
        command();
    }

    /**
     * 接收广播
     */
    private class eStopBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra("state");
            if (state != null && state.equals("1")) {
                startActivity(new Intent(LocationActivity.this, ScramActivity.class));
                LocationActivity.this.finish();
            }
        }
    }

    /**
     * 系统设置的点击事件
     *
     * @param v
     */
    @Event(R.id.gear)
    private void sysClick(View v) {
        b = 1;
        //创建对话框对象的时候对对话框进行监听
        String info = "请输入密码";
        final int[] cnt = {0};
        final boolean[] flag = {false};
        final SmallPwdDialog dialog = new SmallPwdDialog(LocationActivity.this, info, R.style.CustomDialog,
                new SmallPwdDialog.DataBackListener() {
                    @Override
                    public void getData(String data) {
                        String result = data;
                        if (result.equals(MyApplication.ADMIN_PASSWORD)) {
                            flag[0] = true;
                        } else {
                            flag[0] = false;
                        }
                        //Log.d(LocationActivity.ACTIVITY_TAG, "result:"+result+"  ADMIN_PWD:"+MyApplication.ADMIN_PASSWORD);
                        //Log.d(LocationActivity.ACTIVITY_TAG, "result:"+flag[0]);
                        if (flag[0]) {
                            startActivity(new Intent(LocationActivity.this, SystemSettingActivity.class));
                        } else if (cnt[0] != 0) {
                            Toast.makeText(LocationActivity.this, "密码错误请重试!", Toast.LENGTH_SHORT).show();
                        }
                        cnt[0]++;
                    }
                });

        dialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.y = 100;
        dialog.getWindow().setGravity(Gravity.TOP);
        dialog.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        //布局位于状态栏下方
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        //全屏
//                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        //隐藏导航栏
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                if (Build.VERSION.SDK_INT >= 19) {
                    uiOptions |= 0x00001000;
                } else {
                    uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                }
                dialog.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
            }
        });
        dialog.show();
        initImmersiveMode(); //隐藏虚拟按键和状态栏
    }

    @Event(R.id.btn_test)
    private void testClick(View v) {
        startActivity(new Intent(LocationActivity.this, LoginActivity.class));
        finish();
    }

    /**
     * 当Activity已经完全不可见时，调用onStop()
     * 此时Activity处于【停止状态】
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (locateThread != null) {
            locateThread.interrupt(); //中断线程
        }

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
