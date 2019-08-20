package com.bdl.airecovery.activity;


import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.BounceInterpolator;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.R;
import com.bdl.airecovery.base.BaseActivity;
import com.bdl.airecovery.dialog.CommonDialog;
import com.bdl.airecovery.dialog.InputDialog;
import com.bdl.airecovery.dialog.MenuDialog;
import com.bdl.airecovery.dialog.SmallPwdDialog;
import com.bdl.airecovery.entity.Device;
import com.bdl.airecovery.entity.Setting;
import com.bdl.airecovery.netty.DataSocketClient;
import com.bdl.airecovery.util.WifiUtils;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

import org.xutils.ex.DbException;
import org.xutils.DbManager;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


@ContentView(R.layout.activity_sysset)
public class SystemSettingActivity extends BaseActivity {
    /**
     * 依赖注入
     */
    @ViewInject(R.id.sys_update_address) //远程升级地址TextView
    private TextView tvUpdateAddress;

    @ViewInject(R.id.sys_coach_device_address) //教练机地址TextView
    private TextView tvCoachDeviceAddress;

    @ViewInject(R.id.sys_wifi) //WiFi名称TextView
    private TextView tvWifi;

    @ViewInject(R.id.sys_ip) //ip地址
    private TextView tvIp;

    @ViewInject(R.id.tv_connect_status)
    private TextView tvConnectStatus; //C#端连接状态

    @ViewInject(R.id.btn_set_update_address) //修改远程升级地址
    private QMUIRoundButton setUpdateAddress;

    @ViewInject(R.id.btn_set_coach_device_address) //修改教练机ip地址
    private QMUIRoundButton setCoachDeviceAddress;

    @ViewInject(R.id.btn_set_wifi) //修改WiFi连接按钮
    private QMUIRoundButton setWifi;

    @ViewInject(R.id.update_sys_setting) //确认修改按钮
    private QMUIRoundButton btnConfirm;

    @ViewInject(R.id.img_update_sys_setting)
    private ImageView imgUpdate;

    @ViewInject(R.id.sys_btn_return) //返回按钮
    private QMUIRoundButton btnReturn;

    @ViewInject(R.id.btn_adset) //进入高级设置页面
    private QMUIRoundButton btnAdset;

    Setting setting = null; //接收数据库数据的Setting对象
    //String deviceNameArray[] = getDeviceNameArray(); //获取设备名称数组
    DbManager db = MyApplication.getInstance().getDbManager(); //获取DbManager对象
    Timer updateStatusTimer = new Timer(); //更新C#端连接状态
    Boolean isModify = false; //是否修改

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new FindAsyncTask().execute();//执行查询数据库的异步任务
        getInfoDynamically(); //动态获取部分
    }

    @Override
    protected void onResume() {
        super.onResume();
        initImmersiveMode(); //隐藏虚拟按键和状态栏
    }

    //更新与C#端连接状态的定时任务
    TimerTask updateStatusTask = new TimerTask(){
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //动态获取当前与C#端连接状态
                    String status;
                    if (DataSocketClient.getInstance() == null) {
                        status = "未联通";
                    } else {
                        status = DataSocketClient.getInstance().status ? "联通" : "未联通";
                    }
                    setTextView(tvConnectStatus, status);
                    tvConnectStatus.setTextColor(Color.parseColor("#00EE00"));
                }
            });
        }
    };

    /**
     * 查询数据库的异步任务
     */
    class FindAsyncTask extends AsyncTask<String, Void, Void> {

        //onPreExecute用于异步处理前的操作
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        //在doInBackground方法中进行异步任务的处理.
        @Override
        protected Void doInBackground(String... params) {
            //查询数据库的信息，并取出
            try {
                setting = db.selector(Setting.class).findFirst(); //从数据库中查询
            } catch (DbException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (setting == null){ //数据库为空时的处理
                setting = new Setting();
                setting.setDeviceName(null);
                setting.setUpdateAddress(null);
                setting.setCoachDeviceAddress(null);
                setting.setVersion(null);
                setting.setCanQuickLogin(null);
                setting.setCanStrengthTest(null);
                setting.setMedicalSettingPassword("admin"); //默认密码
            }
            //处理数据库中查询到的数据
            String updateAddress = setting.getUpdateAddress();
            String setCoachDeviceAddressDeviceAddress = setting.getCoachDeviceAddress();
            //设置TextView的Text
            setTextView(tvUpdateAddress, updateAddress);
            setTextView(tvCoachDeviceAddress, setCoachDeviceAddressDeviceAddress);
        }
    }

    /**
     * 动态获取的信息部分
     */
    private void getInfoDynamically() {
        //动态获取当前连接WiFi的SSID
        String SSID = WifiUtils.getConnectWifiSSID(SystemSettingActivity.this);
        setTextView(tvWifi, SSID);

        //动态获取ip地址
        String ipAddress = WifiUtils.getIP(SystemSettingActivity.this);
        setTextView(tvIp, ipAddress);

        //动态更新当前与C#端连接状态
        updateStatusTimer.schedule(updateStatusTask, 0, 2);
    }

    /**
     * 修改远程升级地址
     *
     * @param view
     */
    @Event(R.id.btn_set_update_address)
    private void setUpdateAddressClick(View view) {
        isModify = true;
        //创建对话框对象的时候对对话框进行监听
        String info = "请输入远程升级地址";
        final InputDialog dialog = new InputDialog(SystemSettingActivity.this, info, R.style.CustomDialog,
                new InputDialog.DataBackListener() {
                    @Override
                    public void getData(String data) {
                        String result = data;
                        tvUpdateAddress.setText(result);
                    }
                });
        dialog.setTitle("设置远程升级地址");
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

    /**
     * 修改教练机IP地址
     *
     * @param view
     */
    @Event(R.id.btn_set_coach_device_address)
    private void setCoachDeviceAddressClick(View view) {
        isModify = true;
        String info = "请输入教练机IP地址";
        //创建对话框对象的时候对对话框进行监听
        final InputDialog dialog = new InputDialog(SystemSettingActivity.this, info, R.style.CustomDialog,
                new InputDialog.DataBackListener() {
                    @Override
                    public void getData(String data) {
                        String result = data;
                        tvCoachDeviceAddress.setText(result);
                    }
                });
        dialog.setTitle("设置教练机IP地址");
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
    }

    /**
     * APP内手动连接WiFi
     *
     * @param view
     */
    @Event(R.id.btn_set_wifi)
    private void setWifiClick(View view) {
        startWifiListActivity(); //跳转到WifiListActivity
    }

    /**
     * 按下确认按钮更新数据库并重启
     *
     * @param view
     */
    @Event(R.id.update_sys_setting)
    private void setConfirmClick(View view) {
        ObjectAnimator rotate = ObjectAnimator.ofFloat(imgUpdate, "rotation", 0f, 360f).setDuration(800);
        rotate.setInterpolator(new BounceInterpolator());
        rotate.start();

        new UpdateAsyncTask().execute(); //执行更新数据库的异步任务
    }

    /**
     * 返回
     *
     * @param view
     */
    @Event(R.id.sys_btn_return)
    private void setReturnClick(View view) {
        if (isModify) { //比较当前页面数据和数据库中数据
            final CommonDialog commonDialog = new CommonDialog(SystemSettingActivity.this);
            commonDialog.setTitle("温馨提示");
            commonDialog.setMessage("当前数据未保存，确定返回？");
            commonDialog.setOnNegativeClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    commonDialog.dismiss();
                }
            });
            commonDialog.setOnPositiveClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    commonDialog.dismiss();
                    startActivity(new Intent(SystemSettingActivity.this, LocationActivity.class));
                    SystemSettingActivity.this.finish();
                }
            });
            commonDialog.show();
        } else {
            startActivity(new Intent(SystemSettingActivity.this, LocationActivity.class));
            SystemSettingActivity.this.finish();
        }
    }

    /**
     * 进入高级设置按钮
     */
    @Event(R.id.btn_adset)
    private void gotoAdvancedSetting(View view) {
        //创建对话框对象的时候对对话框进行监听
        String info = "请输入密码";
        final int[] cnt = {0};
        final boolean[] flag = {false};
        final SmallPwdDialog dialog = new SmallPwdDialog(SystemSettingActivity.this, info, R.style.CustomDialog,
                new SmallPwdDialog.DataBackListener() {
                    @Override
                    public void getData(String data) {
                        String result = data;
                        if (result.equals(MyApplication.ADMIN_PASSWORD)) {
                            flag[0] = true;
                        } else {
                            flag[0] = false;
                        }
                        if (flag[0]) {
                            startActivity(new Intent(SystemSettingActivity.this, AdvancedSettingActivity.class));
                        } else if (cnt[0] != 0) {
                            Toast.makeText(SystemSettingActivity.this, "密码错误请重试!", Toast.LENGTH_SHORT).show();
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

    /**
     * 获得当前页面的setting信息
     *
     * @param setting
     * @return
     */
    private Setting getCurrentSettings(Setting setting) {
        //setting.setDeviceName(getRealData(tvDeviceName));
        setting.setUpdateAddress(getRealData(tvUpdateAddress));
        setting.setCoachDeviceAddress(getRealData(tvCoachDeviceAddress));
        return setting;
    }

    /**
     * 判断当前TextView的数据
     * 获得真正应该写入数据库的数据
     * 防止数据库被写入"--"字符串
     *
     * @param tv
     * @return
     */
    private String getRealData(TextView tv) {
        if (!tv.getText().equals("--")) {
            return (String) tv.getText();
        } else {
            return null;
        }
    }

    /**
     * 设置TextView的Text
     *
     * @param tv
     * @param settingInfo
     */
    private void setTextView(TextView tv, String settingInfo) {
        if (settingInfo == null || "".equals(settingInfo)) { //判断是否为空或者等于空字符串
            tv.setText("--");
        } else {
            tv.setText(settingInfo);
        }
    }

    /**
     * 启动WifiListActivity
     */
    private void startWifiListActivity() {
        Intent intent = new Intent(SystemSettingActivity.this, WifiListActivity.class);
        startActivityForResult(intent, 1);
    }

    /**
     * 接收其他Activity返回的数据
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                if (data != null) { //这里必须要判空否则Activity不带数据返回时会报错
                    String results[] = data.getStringArrayExtra("results");
                    tvWifi.setText(results[0]);
                    tvIp.setText(results[1]);
                }
                break;
        }
    }

    /**
     * 执行更新数据库的异步任务
     */
    class UpdateAsyncTask extends AsyncTask<String, Void, Void> {

        //onPreExecute用于异步处理前的操作
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        //在doInBackground方法中进行异步任务的处理.
        @Override
        protected Void doInBackground(String... params) {
            setting = getCurrentSettings(setting);
            try {
                db.update(setting); //更新数据库
            } catch (DbException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(SystemSettingActivity.this, "数据已更新,APP将在3后重启", Toast.LENGTH_SHORT).show();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    restartApp();
                }
            }, 3*1000);
        }
    }

    /**
     * 重启APP
     *
     */
    private void restartApp() {
        Intent intent = new Intent(getBaseContext(), SelfUpdatingActivity.class);
        @SuppressLint("WrongConstant") PendingIntent restartIntent = PendingIntent.getActivity(getBaseContext(), 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
        AlarmManager mgr = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, restartIntent);
        android.os.Process.killProcess(android.os.Process.myPid());
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
