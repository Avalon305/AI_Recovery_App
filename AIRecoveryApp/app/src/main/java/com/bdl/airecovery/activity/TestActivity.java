package com.bdl.airecovery.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.R;
import com.bdl.airecovery.base.BaseActivity;
import com.bdl.airecovery.bluetooth.CommonCommand;
import com.bdl.airecovery.service.BluetoothService;

import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

@ContentView(R.layout.activity_test)
public class TestActivity extends BaseActivity implements View.OnClickListener {


    @ViewInject(R.id.test_ShutDown)
    private Button test_ShutDown;

    @ViewInject(R.id.test_AdaptModeActivity)
    private Button test_AdaptModeActivity;

    @ViewInject(R.id.test_BikeModeActivity)
    private Button test_BikeModeActivity;

    @ViewInject(R.id.test_ByeActivity)
    private Button test_ByeActivity;

    @ViewInject(R.id.test_EqualSpeedModeActivity)
    private Button test_EqualSpeedModeActivity;

    @ViewInject(R.id.test_HeartRateModeActivity)
    private Button test_HeartRateModeActivity;

    @ViewInject(R.id.test_LocatinActivity)
    private Button test_LocatinActivity;

    @ViewInject(R.id.test_LoginActivity)
    private Button test_LoginActivity;

    @ViewInject(R.id.test_MuscleModeActivity)
    private Button test_MuscleModeActivity;

    @ViewInject(R.id.test_MainActivity)
    private Button test_MainActivity;

    @ViewInject(R.id.test_PersonalSettingActivity)
    private Button test_PersonalSettingActivity;

    @ViewInject(R.id.test_SelfUpdatingActivity)
    private Button test_SelfUpdatingActivity;

    @ViewInject(R.id.test_StandardModeActivity)
    private Button test_StandardModeActivity;

    @ViewInject(R.id.test_SystemsetActivity)
    private Button test_SystemsetActivity;

    @ViewInject(R.id.test_WifiListActivity)
    private Button test_WifiListActivity;

    @ViewInject(R.id.test_drop_db)
    private Button test_DropDb;

    @ViewInject(R.id.test_active)
    private Button test_Active;

    @ViewInject(R.id.test_PassiveModeActivity)
    private Button test_Passive;

    @ViewInject(R.id.test_static_moveup)
    private Button test_static_moveup;

    @ViewInject(R.id.test_static_movedown)
    private Button test_static_movedown;

    @ViewInject(R.id.test_static_stop)
    private Button test_static_stop;

    @ViewInject(R.id.test_static_heartbeat)
    private Button test_static_heartbeat;

    @ViewInject(R.id.test_static_getposition)
    private Button test_static_getposition;

    @ViewInject(R.id.test_static_testlocate)
    private Button test_static_testlocate;

    @ViewInject(R.id.test_static_position)
    private EditText test_static_position;

    @ViewInject(R.id.test_static_setposition)
    private Button test_static_setposition;

    @ViewInject(R.id.strength_test)
    private Button strengthTest;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        test_AdaptModeActivity.setOnClickListener(this);
        test_BikeModeActivity.setOnClickListener(this);
        test_ByeActivity.setOnClickListener(this);
        test_EqualSpeedModeActivity.setOnClickListener(this);
        test_HeartRateModeActivity.setOnClickListener(this);
        test_LocatinActivity.setOnClickListener(this);
        test_LoginActivity.setOnClickListener(this);
        test_MuscleModeActivity.setOnClickListener(this);
        test_PersonalSettingActivity.setOnClickListener(this);
        test_SelfUpdatingActivity.setOnClickListener(this);
        test_StandardModeActivity.setOnClickListener(this);
        test_SystemsetActivity.setOnClickListener(this);
        test_WifiListActivity.setOnClickListener(this);
        test_MainActivity.setOnClickListener(this);
        test_ShutDown.setOnClickListener(this);
        test_DropDb.setOnClickListener(this);
        test_Active.setOnClickListener(this);
        test_Passive.setOnClickListener(this);
        test_static_moveup.setOnClickListener(this);
        test_static_movedown.setOnClickListener(this);
        test_static_stop.setOnClickListener(this);
        test_static_heartbeat.setOnClickListener(this);
        test_static_getposition.setOnClickListener(this);
        test_static_testlocate.setOnClickListener(this);
        test_static_setposition.setOnClickListener(this);
        strengthTest.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

        if (view == test_AdaptModeActivity) {
            startActivity(new Intent(TestActivity.this, AdaptModeActivity.class));
        } else if (view == test_BikeModeActivity) {
            startActivity(new Intent(TestActivity.this, BikeModeActivity.class));
        } else if (view == test_ByeActivity) {
            startActivity(new Intent(TestActivity.this, ByeActivity.class));
        } else if (view == test_EqualSpeedModeActivity) {
            startActivity(new Intent(TestActivity.this, EqualSpeedModeActivity.class));
        } else if (view == test_HeartRateModeActivity) {
            startActivity(new Intent(TestActivity.this, HeartRateModeActivity.class));
        } else if (view == test_LocatinActivity) {
            startActivity(new Intent(TestActivity.this, LocationActivity.class));
        } else if (view == test_LoginActivity) {
            startActivity(new Intent(TestActivity.this, LoginActivity.class));
        } else if (view == test_MuscleModeActivity) {
            startActivity(new Intent(TestActivity.this, MuscleModeActivity.class));
        } else if (view == test_PersonalSettingActivity) {
            startActivity(new Intent(TestActivity.this, PersonalSettingActivity.class));
        } else if (view == test_SelfUpdatingActivity) {
            startActivity(new Intent(TestActivity.this, SelfUpdatingActivity.class));
        } else if (view == test_StandardModeActivity) {
            startActivity(new Intent(TestActivity.this, StandardModeActivity.class));
        } else if (view == test_SystemsetActivity) {
            startActivity(new Intent(TestActivity.this, SystemSettingActivity.class));
        } else if (view == test_Active) {
            startActivity(new Intent(TestActivity.this, ActivePassiveModeActivity.class));
        } else if (view == test_Passive) {
            startActivity(new Intent(TestActivity.this, PassiveModeActivity.class));
        } else if (view == test_WifiListActivity) {
            startActivity(new Intent(TestActivity.this, WifiListActivity.class));
        } else if (view == test_MainActivity) {
            startActivity(new Intent(TestActivity.this, MainActivity.class));
        } else if (view == test_ShutDown) {
            Intent intent = new Intent(TestActivity.this, BluetoothService.class);
            intent.putExtra("command", CommonCommand.FIRST__LOGOUT.value());
            startService(intent);
            stopService(new Intent(this, BluetoothService.class));
            System.exit(0);
        } else if (view == test_DropDb) {
            try {
                MyApplication.getInstance().getDbManager().dropDb();
                Toast.makeText(TestActivity.this, "删库成功！", Toast.LENGTH_SHORT).show();

            } catch (DbException e) {
                e.printStackTrace();
            }
        } else if (view == strengthTest) {
            startActivity(new Intent(TestActivity.this, StrengthTestActivity.class));
        }
    }
}
