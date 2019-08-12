package com.bdl.airecovery.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bdl.airecovery.R;
import com.bdl.airecovery.base.BaseActivity;
import com.bdl.airecovery.entity.Setting;
import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.entity.update.Resp;
import com.bdl.airecovery.util.WifiUtils;
import com.google.gson.Gson;

import org.xutils.DbManager;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;

@ContentView(R.layout.activity_self_updating)
public class SelfUpdatingActivity extends BaseActivity {
    private File apkfile;
    private int progress=0;
    private Message message;
    private Context context = getBaseContext();
    private URL url = null;
    private HttpURLConnection connection = null;

    private String nowVersion = null;
    private DbManager dbManager = MyApplication.getInstance().getDbManager();
    //-1 代表 连接失败 0 代表无需更新 1 代表可以更新
    private int apkState = -1;
    //IP提示框
    @ViewInject(R.id.tv_ip_text)
    private TextView ip_tip;
    //Mac提示框
    @ViewInject(R.id.tv_mac_text)
    private TextView mac_tip;
    //跟新进度条progressbar
    @ViewInject(R.id.progressBar_update)
    private ProgressBar progressBar_update;
    //连接反馈文本
    @ViewInject(R.id.tv_link_result)
    private TextView link_result;
    //云平台版本
    @ViewInject(R.id.tv_newestver_text)
    private TextView newestver_text;
    //本机版本
    @ViewInject(R.id.tv_nversion_text)
    private TextView nversion_text;


    /**
     * 接收广播
     */
    private class eStopBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra("state");
            if (state != null && state.equals("1")) {
                startActivity(new Intent(SelfUpdatingActivity.this, ScramActivity.class));
                SelfUpdatingActivity.this.finish();
            }
        }
    }

    private Handler handler=new Handler(){

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            int p=msg.what;
            progressBar_update.setProgress(p);
        }

    };

    private Handler downloadingHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            link_result.setText("正在下载更新...");
        }
    };

    private Handler versionHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1){
                newestver_text.setText("V"+msg.obj);
            }else {
                nversion_text.setText("V"+msg.obj);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }

        initImmersiveMode(); //隐藏状态栏与导航栏
        //设置本机Mac地址和IP地址
        mac_tip.setText(WifiUtils.getLocalMacAddress(SelfUpdatingActivity.this));
        ip_tip.setText(WifiUtils.getIP(SelfUpdatingActivity.this));
        //开始下载apk的异步任务
        downloadAPK();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @SuppressLint("StaticFieldLeak")
    public void downloadAPK() {
        //启动异步任务处理
        new AsyncTask<Void, Integer, Void>() {

            //首先在主线程中弹出提示
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                //0.准备用于保存APK文件的FILE对象：/storage/sdcard/Android/package_name/files/xxx.apk
                apkfile = new File(getExternalFilesDir(null), "update.apk");
            }

            //onPreExecute() 执行完成之后 在woeker线程中执行 doInBackground
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    Log.d("update11","doInBackground");
                    //2.1 发送请求，通过url得到连接对象
                    //192.168.1.109是本机ip地址，不要填localhost!!!
                    //8080为所在web项目的端口号
                    //本次选的测试项目名为javaweb
                    //DataStorage.apk在下面这个路径中
                    Setting setting = dbManager.findById(Setting.class, 1);

                    Message nowversionMsg = versionHandler.obtainMessage();
                    nowversionMsg.what = 0;
                    nowversionMsg.obj = setting.getVersion();
                    versionHandler.sendMessage(nowversionMsg);

                    String path = "http://"+setting.getUpdateAddress();
                    nowVersion = setting.getVersion();
                    path = path+":8080/SoftwareManager/update/aisport_android";
                    URL url = new URL(path);
                    HttpURLConnection connection=(HttpURLConnection)url.openConnection();
                    //2.2设置连接超时和读取超时
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);//5s
                    connection.setReadTimeout(10000);//10s
                    //2.3连接
                    connection.connect();
                    //2.4 请求并得到响应码200
                    int responseCode = connection.getResponseCode();
                    Log.d("update11",connection.toString());
                    String TAG = "update11";
                    Log.d(TAG, "responseCode: "+responseCode);
                    if (responseCode == 200) {

                        InputStream inputStream=connection.getInputStream();
                        byte[] data=new byte[1];
                        StringBuffer sb=new StringBuffer();
                        int length=0;
                        while ((length=inputStream.read(data))!=-1){
                            String s=new String(data, Charset.forName("utf-8"));
                            sb.append(s);
                        }
                        String message=sb.toString();
                        inputStream.close();
                        connection.disconnect();
                        //赋值
                        Gson gson = new Gson();
                        Log.e("update11",message);
                        Resp resp = gson.fromJson(message, Resp.class);
                        Log.e("update11",resp.toString());
                        Log.d(TAG, "nowversion: "+nowVersion);
                        //TODO 向云平台请求最新版本号
                        if (nowVersion.equals(resp.getVersion()))
                        {
                            //当前版本已经是最新版本
                            connection.disconnect();
                            //apkState置为0，表示无需更新
                            apkState = 0;
                        }
                        else
                        {
                            Message newversionMsg = versionHandler.obtainMessage();
                            newversionMsg.what = 1;
                            newversionMsg.obj = resp.getVersion();
                            versionHandler.sendMessage(newversionMsg);

                            setting.setVersion(resp.getVersion());
                            Log.e("update11","downloading");
//                            link_result.setText("正在下载更新...");
                            Message downloadingmessage = downloadingHandler.obtainMessage();
                            downloadingHandler.sendMessage(downloadingmessage);
                            path = "http://"+setting.getUpdateAddress()+":8080/SoftwareManager/download/aisport_android/"+resp.getVersion()+"/start";
                            url = new URL(path);
                            connection = (HttpURLConnection)url.openConnection();
                            //2.2设置连接超时和读取超时
                            connection.setRequestMethod("GET");
                            connection.setConnectTimeout(5000);//5s
                            connection.setReadTimeout(10000);//10s
                            //2.3连接
                            connection.connect();
                            //2.4 请求并得到响应码200
                            responseCode = connection.getResponseCode();
                            Log.d(TAG, "downloadingurl: "+path);
                            Log.d(TAG, "downloadingresponsecode: "+responseCode);
                            if (responseCode == 200) {
                                progressBar_update.setMax(connection.getContentLength());



                                //2.5得到包含apk文件数据的InputStream
                                InputStream is = connection.getInputStream();
                                //2.6创建指向apkFile的FileOutputStream
                                FileOutputStream fos = new FileOutputStream(apkfile);
                                //2.7边读边写
                                byte[] buffer = new byte[1024];
                                int offset = 0;
                                int len = -1;
                                while ((len = is.read(buffer)) != -1) {
                                    //2.8显示下载进度
                                    fos.write(buffer, 0, len);
                                    offset += len;
                                    Log.d(TAG, "len: "+len);
                                    Log.d(TAG, "now: "+offset);
                                    //在分线程中更新UI
                                    publishProgress(len);
                                    //休息一会（模拟网速慢）
//                                    SystemClock.sleep(50);
                                }
                                fos.close();
                                is.close();
                                //2.9下载完成，关闭，切换到主线程
                                connection.disconnect();
                                //apkState置为1，表示可以进行安装
                                apkState = 1;
                            }else {
                                //更新失败
                                connection.disconnect();
                                //apkState置为-1，表示无法更新
                                apkState = -1;
                            }
                        }
                        //return null;
                    }
                    else
                    {
                        //更新失败
                        connection.disconnect();
                        //apkState置为-1，表示无法更新
                        apkState = -1;
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            //全部执行完成后主线程执行，即最后执行的
            @Override
            protected void onPostExecute(Void result){
                if (apkState == 1)
                {
                    installAPK();
                }else if (apkState == 0){
                    skinUpdate();
                }else {
                    connectFail();
                }
            }
            //在主线程中更新进度
            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                progressBar_update.incrementProgressBy(values[0]);
            }
        }.execute();
    }

    /* 与宝德龙云平台连接失败跳过本次更新*/
    public void connectFail()
    {
        link_result.setText("请检查网络后重试");
        new Thread(runnable).start();
        //3s后自动跳转下一界面
    }

    /*skinUpdate
    * 当前版本已经是最高版本跳过更新!*/
    public void skinUpdate()
    {
        link_result.setText("当前版本已经是最新版本");
        new Thread(runnable).start();
        //3s后自动跳转下一界面
        new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i=1;i<=3;i++) {
                    SystemClock.sleep(1000);
                }
                SelfUpdatingActivity.this.startActivity(new Intent(SelfUpdatingActivity.this,LocationActivity.class));
                SelfUpdatingActivity.this.finish();
            }
        }).start();
    }

    /*启动安装APK*/
    private void installAPK()
    {
        //以下参数勿动
        Intent intent = new Intent("android.intent.action.INSTALL_PACKAGE");
        intent.setDataAndType(Uri.fromFile(apkfile), "application/vnd.android.package-archive");
        SelfUpdatingActivity.this.startActivity(intent);
    }

    Runnable runnable=new Runnable() {

        @Override
        public void run() {
            message=handler.obtainMessage();
            // TODO Auto-generated method stub
            try {
                for (int i = 0; i <=100; i++) {
                    int x=progress++;
                    message.what=x;
                    handler.sendEmptyMessage(message.what);
                    Thread.sleep(40);
                }
                if(message.what==100)
                {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for(int i=1;i<=3;i++) {
                                SystemClock.sleep(1000);
                            }
                            SelfUpdatingActivity.this.startActivity(new Intent(SelfUpdatingActivity.this,LocationActivity.class));
                            SelfUpdatingActivity.this.finish();
                        }
                    }).start();
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };


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