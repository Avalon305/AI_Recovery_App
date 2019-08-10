package com.bdl.aisports.activity;


import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.BounceInterpolator;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bdl.aisports.MyApplication;
import com.bdl.aisports.R;
import com.bdl.aisports.base.BaseActivity;
import com.bdl.aisports.dialog.CommonDialog;
import com.bdl.aisports.dialog.InputDialog;
import com.bdl.aisports.dialog.MenuDialog;
import com.bdl.aisports.entity.Device;
import com.bdl.aisports.entity.Setting;
import com.bdl.aisports.util.WifiUtils;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
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
    @ViewInject(R.id.sys_device_name)     //设备类型TextView
    private TextView tvDeviceName;

    @ViewInject(R.id.sys_update_address) //远程升级地址TextView
    private TextView tvUpdateAddress;

    @ViewInject(R.id.sys_timeserver_address) //时间服务器TextView
    private TextView tvTimeserver;

    @ViewInject(R.id.sys_coach_device_address) //教练机地址TextView
    private TextView tvCoachDeviceAddress;

    @ViewInject(R.id.sys_wifi) //WiFi名称TextView
    private TextView tvWifi;

    @ViewInject(R.id.sys_ip) //ip地址
    private TextView tvIp;


    @ViewInject(R.id.sys_version) //软件版本号
    private TextView tvVersion;

    @ViewInject(R.id.sys_rate)
    private TextView tvRate;

    @ViewInject(R.id.btn_set_device_name)  //修改设备类型按钮
    private QMUIRoundButton setDeviceName;

    @ViewInject(R.id.btn_set_update_address) //修改远程升级地址
    private QMUIRoundButton setUpdateAddress;

    @ViewInject(R.id.btn_set_timeserver_address) //修改时间服务器地址
    private QMUIRoundButton setTimeserver;

    @ViewInject(R.id.btn_set_coach_device_address) //修改教练机ip地址
    private QMUIRoundButton setCoachDeviceAddress;

    @ViewInject(R.id.btn_set_wifi) //修改WiFi连接按钮
    private QMUIRoundButton setWifi;

    @ViewInject(R.id.btn_set_rate)
    private QMUIRoundButton setRate;

    @ViewInject(R.id.update_sys_setting) //确认修改按钮
    private QMUIRoundButton btnConfirm;

    @ViewInject(R.id.img_update_sys_setting)
    private ImageView imgUpdate;

    @ViewInject(R.id.sys_btn_return) //返回按钮
    private QMUIRoundButton btnReturn;

    Setting setting = null; //接收数据库数据的Setting对象
    String deviceNameArray[] = getDeviceNameArray(); //获取设备名称数组
    DbManager db = MyApplication.getInstance().getDbManager(); //获取DbManager对象

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
                setting.setTimeServerAddress(null);
                setting.setCoachDeviceAddress(null);
                setting.setVersion(null);
                setting.setRate(null);
            }
            //处理数据库中查询到的数据
            String deviceName = setting.getDeviceName();
            String updateAddress = setting.getUpdateAddress();
            String timeServerAddress = setting.getTimeServerAddress();
            String setCoachDeviceAddressDeviceAddress = setting.getCoachDeviceAddress();
            String version = setting.getVersion();
            String rate = setting.getRate();
            //设置TextView的Text
            setTextView(tvDeviceName, deviceName);
            setTextView(tvUpdateAddress, updateAddress);
            setTextView(tvTimeserver, timeServerAddress);
            setTextView(tvCoachDeviceAddress, setCoachDeviceAddressDeviceAddress);
            setTextView(tvVersion, version);
            setTextView(tvRate, rate);
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
    }

    /**
     * 获取设备名称数组
     *
     * @return
     */
    private String[] getDeviceNameArray() {
        //获取设备名称数组
        List<Device> deviceList = MyApplication.getInstance().getDeviceList(); //获得json文件中所有Device的List
        int length = deviceList.size();
        String[] deviceName = new String[length]; //使用一个数组接收名称
        for (int i = 0; i < length; i++) { //将List中的设备名称赋值给String数组
            deviceName[i] = deviceList.get(i).getDeviceName();
        }
        return deviceName;
    }

    /**
     * 获取当前设备名称的下标值
     *
     * @return
     */
    private int getCurrentDeviceIndex() {
        String currentDeviceName = (String) tvDeviceName.getText();
        for (int i = 0; i < deviceNameArray.length; i++) {
            if (currentDeviceName.equals(deviceNameArray[i])) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 修改设备类型
     *
     * @param view
     */
    @Event(R.id.btn_set_device_name)
    private void setDeviceNameClick(View view) {
        List<String> menuItemsList = new ArrayList<String>();
        for(int i = 0; i < deviceNameArray.length; i++) {
            menuItemsList.add(deviceNameArray[i]);
        }

        final MenuDialog menuDialog = new MenuDialog(SystemSettingActivity.this);
        menuDialog.setTitle("选择训练模式");
        menuDialog.setMenuItems(menuItemsList);
        menuDialog.setSelectedIndex(getCurrentDeviceIndex());
        //ListView子项点击事件监听
        menuDialog.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //更新当前选择项的索引
                tvDeviceName.setText(deviceNameArray[i]);
                menuDialog.dismiss();
            }
        });
        //模态框隐藏导航栏
        menuDialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        menuDialog.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        //布局位于状态栏下方
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        //全屏
                        //View.SYSTEM_UI_FLAG_FULLSCREEN |
                        //隐藏导航栏
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                if (Build.VERSION.SDK_INT >= 19) {
                    uiOptions |= 0x00001000;
                } else {
                    uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                }
                menuDialog.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
            }
        });
        menuDialog.show();
    }

    /**
     * 修改远程升级地址
     *
     * @param view
     */
    @Event(R.id.btn_set_update_address)
    private void setUpdateAddressClick(View view) {
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
     * 修改时间服务器地址
     *
     * @param view
     */
    @Event(R.id.btn_set_timeserver_address)
    private void setTimeserverClick(View view) {
        String info = "请输入时间服务器地址";
        //创建对话框对象的时候对对话框进行监听
        final InputDialog dialog = new InputDialog(SystemSettingActivity.this, info, R.style.CustomDialog,
                new InputDialog.DataBackListener() {
                    @Override
                    public void getData(String data) {
                        String result = data;
                        tvTimeserver.setText(result);
                    }
                });
        dialog.setTitle("设置时间服务器地址");
        dialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
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
     * 修改教练机IP地址
     *
     * @param view
     */
    @Event(R.id.btn_set_coach_device_address)
    private void setCoachDeviceAddressClick(View view) {
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
     * 修改电机比率
     * @param view
     */
    @Event(R.id.btn_set_rate)
    private void setRate(View view) {
        final String[] rate = {"0.8", "0.9", "1.0", "1.1", "1.2", "1.5"};
        List<String> menuItemsList = new ArrayList<String>();
        for(int i = 0; i < rate.length; i++) {
            menuItemsList.add(rate[i]);
        }

        final MenuDialog menuDialog = new MenuDialog(SystemSettingActivity.this);
        menuDialog.setTitle("选择电机比率");
        menuDialog.setMenuItems(menuItemsList);
        menuDialog.setSelectedIndex(getCurrentDeviceIndex());
        //ListView子项点击事件监听
        menuDialog.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //更新当前选择项的索引
                tvRate.setText(rate[i]);
                menuDialog.dismiss();
            }
        });
        
        //模态框隐藏导航栏
        menuDialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        menuDialog.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        //布局位于状态栏下方
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        //全屏
                        //View.SYSTEM_UI_FLAG_FULLSCREEN |
                        //隐藏导航栏
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                if (Build.VERSION.SDK_INT >= 19) {
                    uiOptions |= 0x00001000;
                } else {
                    uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                }
                menuDialog.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
            }
        });
        menuDialog.show();
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
        Setting newSetting = new Setting();
        newSetting = getCurrentSettings(newSetting);
        if (!newSetting.equals(setting)) { //比较当前页面数据和数据库中数据
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
     * 获得当前页面的setting信息
     *
     * @param setting
     * @return
     */
    private Setting getCurrentSettings(Setting setting) {
        setting.setDeviceName(getRealData(tvDeviceName));
        setting.setUpdateAddress(getRealData(tvUpdateAddress));
        setting.setTimeServerAddress(getRealData(tvTimeserver));
        setting.setCoachDeviceAddress(getRealData(tvCoachDeviceAddress));
        setting.setVersion(getRealData(tvVersion));
        setting.setRate(getRealData(tvRate));
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
            case 0: //接收来自SetDeviceNameActivity回传的数据
                if (data != null) { //这里必须要判空否则Activity不带数据返回时会报错
                    String result = data.getStringExtra("value");
                    tvDeviceName.setText(result);
                }
                break;
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
