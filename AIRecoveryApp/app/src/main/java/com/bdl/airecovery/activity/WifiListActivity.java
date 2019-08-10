package com.bdl.airecovery.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bdl.airecovery.R;
import com.bdl.airecovery.adapter.WifiListAdapter;
import com.bdl.airecovery.base.BaseActivity;
import com.bdl.airecovery.dialog.WifiLinkDialog;
import com.bdl.airecovery.entity.WifiBean;
import com.bdl.airecovery.util.WifiUtils;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


@ContentView(R.layout.activity_wifi_list)
public class WifiListActivity extends BaseActivity {

    private static final String TAG = "WifiListActivity";

    @ViewInject(R.id.btn_return)
    private ImageView btnReturn;

    @ViewInject(R.id.pb_wifi_loading)
    private ProgressBar pbWifiLoading; //进度条

    public static final String WIFI_STATE_CONNECT = "已连接";
    public static final String WIFI_STATE_ON_CONNECTING = "正在连接";
    public static final String WIFI_STATE_UNCONNECT = "未连接";

    List<WifiBean> realWifiList = new ArrayList<>(); //WiFiBean集合

    private WifiListAdapter adapter; //适配器

    private RecyclerView recyWifiList;

    private WifiBroadcastReceiver wifiReceiver; //广播

    private int connectType = 0; //1：连接成功？ 2 正在连接（如果wifi热点列表发生变需要该字段）

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DisplayMetrics dm = new DisplayMetrics();
        getWindow().getWindowManager().getDefaultDisplay().getMetrics(dm);
        ViewGroup.LayoutParams p = getWindow().getAttributes();
        p.height = (int) (dm.heightPixels * 0.9);
        p.width = (int) (dm.widthPixels * 0.5);
        WifiListActivity.this.setFinishOnTouchOutside(false);

        initImmersiveMode();
        hidingProgressBar(); //隐藏进度条
        initRecycler(); //初始化WiFi列表

        btnReturn.setOnClickListener(new View.OnClickListener() { //为返回ImageView添加点击监听事件
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                String result[] = new String[2];
                if (WifiUtils.isWifiContected(WifiListActivity.this)) { //当前已经有WiFi连接
                    result[0] = WifiUtils.getConnectWifiSSID(WifiListActivity.this); //获取已连接WiFi的SSID
                    result[1] = WifiUtils.getIP(WifiListActivity.this); //更新IP地址
                } else {
                    result[0] = "--";
                    result[1] = "--";
                }
                intent.putExtra("results", result);
                setResult(1, intent);
                WifiListActivity.this.finish();
            }
        });

    }

    private void initRecycler() {
        //使用自定义适配器将WiFi列表绑定到前端UI
        recyWifiList = (RecyclerView) this.findViewById(R.id.recy_list_wifi);
        adapter = new WifiListAdapter(this, realWifiList);
        recyWifiList.setLayoutManager(new LinearLayoutManager(this));
        recyWifiList.setAdapter(adapter);
        sortScaResult();

        adapter.setOnItemClickListener(new WifiListAdapter.onItemClickListener() {
            @Override
            public void onItemClick(View view, int position, Object o) {
                WifiBean wifiBean = realWifiList.get(position);
                if (wifiBean.getState().equals(WIFI_STATE_UNCONNECT) || wifiBean.getState().equals(WIFI_STATE_CONNECT)) { //当前WiFi未连接或者已连接状态
                    String capabilities = realWifiList.get(position).getCapabilities();
                    if (WifiUtils.getWifiCipher(capabilities) == WifiUtils.WifiCipherType.WIFICIPHER_NOPASS) {//无需密码
                        WifiConfiguration exsits = WifiUtils.createWifiConfig(wifiBean.getWifiName(), null, WifiUtils.WifiCipherType.WIFICIPHER_NOPASS);
                        WifiUtils.addNetWork(exsits, WifiListActivity.this);
                    } else {   //需要密码，弹出输入密码dialog
                        new WifiLinkDialog(WifiListActivity.this,
                                realWifiList.get(position).getWifiName(),
                                realWifiList.get(position).getCapabilities())
                                .show();
                    }
                }
            }
        });
    }

    /**
     * 在onResume方法中注册广播
     */
    @Override
    protected void onResume() {
        super.onResume();
        //注册广播
        wifiReceiver = new WifiBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);//监听wifi是开关变化的状态
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);//监听wifi连接状态广播
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);//监听wifi列表变化（开启一个热点或者关闭一个热点）
        this.registerReceiver(wifiReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(wifiReceiver);
    }

    //监听wifi状态
    public class WifiBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                switch (state) {
                    /**
                     * WIFI_STATE_DISABLED    WLAN已经关闭
                     * WIFI_STATE_DISABLING   WLAN正在关闭
                     * WIFI_STATE_ENABLED     WLAN已经打开
                     * WIFI_STATE_ENABLING    WLAN正在打开
                     * WIFI_STATE_UNKNOWN     未知
                     */
                    case WifiManager.WIFI_STATE_DISABLED: {
                        Log.d(TAG, "已经关闭");
                        Toast.makeText(WifiListActivity.this, "WIFI处于关闭状态", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case WifiManager.WIFI_STATE_DISABLING: {
                        Log.d(TAG, "正在关闭");
                        break;
                    }
                    case WifiManager.WIFI_STATE_ENABLED: {
                        Log.d(TAG, "已经打开");
                        sortScaResult();
                        break;
                    }
                    case WifiManager.WIFI_STATE_ENABLING: {
                        Log.d(TAG, "正在打开");
                        break;
                    }
                    case WifiManager.WIFI_STATE_UNKNOWN: {
                        Log.d(TAG, "未知状态");
                        break;
                    }
                }
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                Log.d(TAG, "--NetworkInfo--" + info.toString());
                if (NetworkInfo.State.DISCONNECTED == info.getState()) {//wifi没连接上
                    Log.d(TAG, "WiFi未连接成功");
                    hidingProgressBar();
                    for (int i = 0; i < realWifiList.size(); i++) {//没连接上将 所有的连接状态都置为“未连接”
                        realWifiList.get(i).setState(WIFI_STATE_UNCONNECT);
                    }
                    adapter.notifyDataSetChanged();
                } else if (NetworkInfo.State.CONNECTED == info.getState()) {//wifi连接上了
                    Log.d(TAG, "WiFi已连接");
                    hidingProgressBar();
                    WifiInfo connectedWifiInfo = WifiUtils.getConnectedWifiInfo(WifiListActivity.this);
                    //连接成功 跳转界面 传递ip地址
                    connectType = 1;
                    wifiListSet(connectedWifiInfo.getSSID(), connectType);
                }
                else if (NetworkInfo.State.CONNECTING == info.getState()) {//正在连接
                    Log.d(TAG, "WiFi正在连接");
                    showProgressBar();
                    WifiInfo connectedWifiInfo = WifiUtils.getConnectedWifiInfo(WifiListActivity.this);
                    connectType = 2;
                    wifiListSet(connectedWifiInfo.getSSID(), connectType);
                }
            }
        }
    }

    /**
     * 将"已连接"或者"正在连接"的wifi热点放置在第一个位置
     *
     * @param wifiName
     * @param type
     */
    public void wifiListSet(String wifiName, int type) {
        int index = -1;
        WifiBean wifiInfo = new WifiBean();
        if (isNullOrEmpty(realWifiList)) {
            return;
        }
        for (int i = 0; i < realWifiList.size(); i++) {
            realWifiList.get(i).setState(WIFI_STATE_UNCONNECT);
        }
        Collections.sort(realWifiList);//根据信号强度排序
        for (int i = 0; i < realWifiList.size(); i++) {
            WifiBean wifiBean = realWifiList.get(i);
            if (index == -1 && ("\"" + wifiBean.getWifiName() + "\"").equals(wifiName)) {
                index = i;
                wifiInfo.setLevel(wifiBean.getLevel());
                wifiInfo.setWifiName(wifiBean.getWifiName());
                wifiInfo.setCapabilities(wifiBean.getCapabilities());
                if (type == 1) {
                    wifiInfo.setState(WIFI_STATE_CONNECT);
                } else {
                    wifiInfo.setState(WIFI_STATE_ON_CONNECTING);
                }
            }
        }
        if (index != -1) {
            realWifiList.remove(index);
            realWifiList.add(0, wifiInfo);
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * 判断集合是否为null或者0个元素
     *
     * @param c
     * @return
     */
    public static boolean isNullOrEmpty(Collection c) {
        if (null == c || c.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * 获取wifi列表然后将bean转成自己定义的WifiBean
     */
    public void sortScaResult() {
        List<ScanResult> scanResults = WifiUtils.noSameName(WifiUtils.getWifiScanResult(this));
        realWifiList.clear();
        if (!isNullOrEmpty(scanResults)) {
            for (int i = 0; i < scanResults.size(); i++) {
                WifiBean wifiBean = new WifiBean();
                wifiBean.setWifiName(scanResults.get(i).SSID);
                wifiBean.setState(WIFI_STATE_UNCONNECT);   //只要获取都假设设置成未连接，真正的状态都通过广播来确定
                wifiBean.setCapabilities(scanResults.get(i).capabilities);
                wifiBean.setLevel(WifiUtils.getLevel(scanResults.get(i).level) + "");
                realWifiList.add(wifiBean);

                //排序
                Collections.sort(realWifiList);
                adapter.notifyDataSetChanged();
            }
        }
    }


    public void showProgressBar() {
        pbWifiLoading.setVisibility(View.VISIBLE);
    }

    public void hidingProgressBar() {
        pbWifiLoading.setVisibility(View.GONE);
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
