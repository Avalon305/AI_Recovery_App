package com.bdl.airecovery.activity;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.BounceInterpolator;
import android.widget.AdapterView;
import android.widget.Button;
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
import com.bdl.airecovery.util.WifiUtils;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

import org.xutils.DbManager;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@ContentView(R.layout.activity_advanced_setting)
public class AdvancedSettingActivity extends BaseActivity {

    //控件绑定
    @ViewInject(R.id.tv_adset_version) //软件版本
    private TextView tvVersion;
    @ViewInject(R.id.tv_adset_quick_login_switch) //是否开启快速登录
    private TextView tvQuickLogin;
    @ViewInject(R.id.tv_adset_strength_test) //是否开启肌力测试
    private TextView tvStrengthTest;
    @ViewInject(R.id.btn_adset_quick_login) //快速登录开关
    private TextView btnQuickLogin;
    @ViewInject(R.id.btn_adset_strength_test) //肌力测试开关
    private TextView btnStrengthTest;
    @ViewInject(R.id.img_update_sys_setting)
    private ImageView imgUpdate;
    @ViewInject(R.id.sys_btn_return) //返回按钮
    private QMUIRoundButton btnReturn;

    Setting setting = null; //接收数据库数据的Setting对象
    DbManager db = MyApplication.getInstance().getDbManager(); //获取DbManager对象
    Boolean isModify = false; //是否修改
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new FindAsyncTask().execute();//执行查询数据库的异步任务
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
                setting.setCoachDeviceAddress(null);
                setting.setVersion(null);
                setting.setCanQuickLogin(null);
                setting.setCanStrengthTest(null);
            }
            //处理数据库中查询到的数据
            //版本
            setTextView(tvVersion, setting.getVersion());
            //是否开启快速登录
            String canQuickLogin = setting.getCanQuickLogin() == true ? "开启":"关闭";
            setTextView(tvQuickLogin, "当前状态：" + canQuickLogin);
            btnQuickLogin.setText(canQuickLogin);
            //是否开启肌力测试
            String canStrengthTest = setting.getCanStrengthTest() == true ? "开启":"关闭";
            setTextView(tvStrengthTest, "当前状态：" + canStrengthTest);
            btnStrengthTest.setText(canStrengthTest);
        }
    }

    @Event(R.id.btn_adset_quick_login)
    private void setCanQuickLogin(View view) {
        isModify = true;
        Button btn = (Button) view;
        if (btn.getText().equals("开启")) {
            btn.setText("关闭");
            setTextView(tvQuickLogin, "当前状态：关闭");
            setting.setCanQuickLogin(false);
        } else {
            btn.setText("开启");
            setTextView(tvQuickLogin, "当前状态：开启");
            setting.setCanQuickLogin(true);
        }
    }

    @Event(R.id.btn_adset_strength_test)
    private void setCanStrengthTest(View view) {
        isModify = true;
        Button btn = (Button) view;
        if (btn.getText().equals("开启")) {
            btn.setText("关闭");
            setTextView(tvStrengthTest, "当前状态：关闭");
            setting.setCanStrengthTest(false);
        } else {
            btn.setText("开启");
            setTextView(tvStrengthTest, "当前状态：开启");
            setting.setCanStrengthTest(true);
        }
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
            final CommonDialog commonDialog = new CommonDialog(AdvancedSettingActivity.this);
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
                    startActivity(new Intent(AdvancedSettingActivity.this, LocationActivity.class));
                    AdvancedSettingActivity.this.finish();
                }
            });
            commonDialog.show();
        } else {
            startActivity(new Intent(AdvancedSettingActivity.this, LocationActivity.class));
            AdvancedSettingActivity.this.finish();
        }
    }

    /**
     * 获得当前页面的setting信息
     *
     * @param setting
     * @return
     */
    private Setting getCurrentSettings(Setting setting) {
        setting.setVersion(getRealData(tvVersion));
        setting.setCanQuickLogin(btnQuickLogin.getText().equals("开启"));
        setting.setCanStrengthTest(btnStrengthTest.getText().equals("开启"));
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
            Toast.makeText(AdvancedSettingActivity.this, "数据已更新,APP将在3后重启", Toast.LENGTH_SHORT).show();
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
