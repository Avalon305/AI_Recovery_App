package com.bdl.airecovery;

import android.content.Intent;
import android.graphics.Typeface;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.bdl.airecovery.constant.MotorConstant;
import com.bdl.airecovery.contoller.Writer;
import com.bdl.airecovery.entity.CurrentTime;
import com.bdl.airecovery.entity.Device;
import com.bdl.airecovery.entity.Setting;
import com.bdl.airecovery.entity.Upload;
import com.bdl.airecovery.entity.login.User;
import com.bdl.airecovery.service.BluetoothService;
import com.bdl.airecovery.service.CardReaderService;
import com.bdl.airecovery.service.MotorService;
import com.bdl.airecovery.service.ReSendService;
import com.bdl.airecovery.service.StaticMotorService;
import com.bdl.airecovery.util.JsonFileUtil;
import com.bdl.airecovery.util.WifiUtils;
import com.clj.fastble.BleManager;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.xutils.DbManager;
import org.xutils.db.table.TableEntity;
import org.xutils.ex.DbException;
import org.xutils.x;

import java.io.File;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;

/**
 * 本app的application对象
 */
public class MyApplication extends MultiDexApplication {

    public int motorDirection = 2;



    /**
     * 管理密码
     */
    public static final String ADMIN_PASSWORD = "admin";

    /**
     * 初始化DaoConfig配置
     */
    DbManager.DaoConfig daoConfig = new DbManager.DaoConfig()
            //设置数据库名，默认xutils.db
            .setDbName("myapp.db")
            //设置数据库路径，默认存储在app的私有目录
            .setDbDir(new File("/mnt/sdcard/"))
            //设置数据库的版本号
            .setDbVersion(2)
            //设置数据库打开的监听
            .setDbOpenListener(new DbManager.DbOpenListener() {
                @Override
                public void onDbOpened(DbManager db) {
                    //开启数据库支持多线程操作，提升性能，对写入加速提升巨大
                    db.getDatabase().enableWriteAheadLogging();
                }
            })
            //设置数据库更新的监听
            .setDbUpgradeListener(new DbManager.DbUpgradeListener() {
                @Override
                public void onUpgrade(DbManager db, int oldVersion, int newVersion) {
                }
            })
            //设置表创建的监听
            .setTableCreateListener(new DbManager.TableCreateListener() {
                @Override
                public void onTableCreated(DbManager db, TableEntity<?> table){
                    Log.i("JAVA", "onTableCreated：" + table.getName());
                }
            });
    //设置是否允许事务，默认true
    //.setAllowTransaction(true)

    DbManager db = x.getDb(daoConfig);
    //使用单例
    private static MyApplication instance;

    //读取json文件生成的list
    private static List<Device> deviceList;

    //app启动以后根据存储的设备名字，查询而出的本机类型。
    //闫科宇用的时候判断空，这是可能为空的。在最初安装的时候可能为空
    private volatile Device currentDevice;
    // 在application的onCreate中初始化
    @Override
    public void onCreate() {
        super.onCreate();
        WifiUtils.openWifi(MyApplication.this); //APP启动时打开WiFi
        x.Ext.init(this);
        x.Ext.setDebug(BuildConfig.DEBUG); // 是否输出debug日志, 开启debug会影响性能.

       // Log4JConfiger.configure();//配置日志框架
        readJson();//读取json文件到list

        try {
            initSetting(); //初始化数据库数据
        } catch (DbException e) {
            e.printStackTrace();
        }
        setCurrentDevice();//根据数据库中存储的当前设备名称，查询出当前设备的一些固定信息
        //启动重传service。
        startReSendService();
        //启动蓝牙Service
        initBLEManager();
        startBluetoothService();

        //启动发卡器Service
        startCardReaderService();
        //启动电机Service
        startMotorService();
        //启动静态电机Service
        startStaticMotorService();

        //初始化需要的字体样式
        //typefaceChar = Typeface.createFromAsset(getAssets(),"fonts/NotoSansHans_Light.otf");
        typefaceNum = Typeface.createFromAsset(getAssets(), "fonts/Arvo-Bold.ttf");
        //创建时初始化。
        instance = this;
    }

    /**
     * 获得单例
     */
    public static MyApplication getInstance(){
        return instance;
    }

    /**
     * 启动重传service,try一下，防止抛异常启动失败，影响程序的启动。
     */
    private void startStaticMotorService(){
        try {
            Intent intent = new Intent(this,StaticMotorService.class);
            startService(intent);
        } catch (Exception e) {
            //e.printStackTrace();
            Log.e("App.startSMService",e.getMessage());
        }
    }

    /**
     * 启动重传service,try一下，防止抛异常启动失败，影响程序的启动。
     */
    private void startReSendService(){
        try {
            Intent intent = new Intent(this,ReSendService.class);
            startService(intent);
        } catch (Exception e) {
            //e.printStackTrace();
            Log.e("App.startReSendService",e.getMessage());
        }
    }

    /**
     * 蓝牙service,try一下，防止抛异常启动失败，影响程序的启动。
     */
    private void startBluetoothService(){
        try {
            Intent intent = new Intent(this,BluetoothService.class);
            startService(intent);
        } catch (Exception e) {
            //e.printStackTrace();
            Log.e("App.startBtService",e.getMessage());
        }
    }

    /**
     * 发卡器service,try一下，防止抛异常启动失败，影响程序的启动。
     */
    private void startCardReaderService(){
        try {
            Intent intent = new Intent(getApplicationContext(),CardReaderService.class);
            startService(intent);
        } catch (Exception e) {
            //e.printStackTrace();
            Log.e("App.startCRService",e.getMessage());
        }
    }

    /**
     * 电机service,try一下，防止抛异常启动失败，影响程序的启动。
     */
    private void startMotorService() {
        try {
            Writer.setParameter(1, MotorConstant.MOTOR_ENABLE);
            Thread.sleep(1000);
            Writer.setParameter(0, MotorConstant.MOTOR_ENABLE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Intent intent = new Intent(this, MotorService.class);
            startService(intent);
        } catch (Exception e) {
            //e.printStackTrace();
            Log.e("App.startMotorService",e.getMessage());
        }
    }

    /**
     * 设置当前设备类型。
     */
    public void setCurrentDevice() {
        //若为空，则读取的过程出现了异常，返回即可。
        if (deviceList == null){
            return;
        }
        //查询当前设备是否被设置了设备类型，如果没有设置，也返回，不做操作。
        Setting setting = null;
        try {
            setting = db.selector(Setting.class).findFirst();
        } catch (DbException e) {
            e.printStackTrace();
        }
        //查出来为空，返回，说明系统初始化错了。
        if (setting == null ){
            return;
        }
        //查出来属性为null或者空串说明还没设置设备类型。
        if (setting.getDeviceName() == null || "".equals(setting.getDeviceName())){
            return;
        }
        //到此就可以执行遍历查询的逻辑，因为数据量不多，不需要设计算法，直接遍历匹配即可。
        //Iterator<Device> iter = deviceList.iterator();
        for (int i = 0; i < deviceList.size(); i++) {
            if (deviceList.get(i).getDeviceName().equals(setting.getDeviceName())){
                currentDevice = deviceList.get(i);
                return;
            }
        }
    }

    /**
     * 初始化数据库数据
     * @throws DbException
     */
    private void initSetting() throws DbException {
        Setting setting = new Setting();
        if (db.findAll(Setting.class) == null){
            setting.setDeviceName("坐式划船机");
            setting.setVersion("1.0");
            setting.setUpdateAddress("192.168.1.102");
            setting.setCoachDeviceAddress("192.168.1.102");
            setting.setUUID(UUID.randomUUID().toString());
            setting.setCanQuickLogin(true);
            setting.setCanStrengthTest(true);
            setting.setMedicalSettingPassword("admin");
            db.save(setting);
        }
    }

    /**
     * 读取json文件到list的具体方法。
     */
    private void readJson() {
        String result = JsonFileUtil.getJson("device.json",this);
        Type listType = new TypeToken<List<Device>>(){}.getType();
        this.deviceList = new Gson().fromJson(result,listType);
    }

    /**
     * 因为app是单例的，所以使用时获得即可保证不会新建多个。
     * @return DbManager
     */
    public DbManager getDbManager(){
        return db!=null?db:x.getDb(daoConfig);
    }

    /**
     * 获取全部设备信息
     */
    public List<Device> getDeviceList(){
        //每次读取重新载入
        String result = JsonFileUtil.getJson("device.json",this);
        Type listType = new TypeToken<List<Device>>(){}.getType();
        this.deviceList = new Gson().fromJson(result,listType);
        return deviceList;
    }




    /**
     * 获得当前设备类型
     */
    public Device getCurrentDevice(){
        return currentDevice;
    }

    //登录用户对象
    private volatile  User user ;

    /**
     * 设置User对象
     */
    public void setUser(User newUser){
        user = newUser;
    }

    /**
     * 获得User对象
     */
    public synchronized User getUser(){
        return user;
    }
    /**
     * 设置传结果对象
     */
    private static Upload upload = new Upload();

    /**
     * 获得上传结果对象
     * @return
     */
    public static Upload getUpload(){return upload;}

    /**
     * 设置上传结果对象
     */
    public static void setUpload(Upload upload_) {
        upload = upload_;
    }

    /**
     * 倒计时对象
     */
    private static CurrentTime currentTime = new CurrentTime(-1,-1);

    /**
     * 获得倒计时对象
     */
    public synchronized static CurrentTime getCurrentTime() {
        return currentTime;
    }

    /**
     * 设置倒计时对象
     */
    public synchronized static void setCurrentTime(CurrentTime currentTime_) {
        currentTime = currentTime_;
    }

    /**
     * BtService 更新 Device，只允许更新顺向力，反向力
     */
    public void UserUpdateDevice(Device newDevice){
        currentDevice.setConsequentForce(newDevice.getConsequentForce());
        currentDevice.setReverseForce(newDevice.getReverseForce());
        currentDevice.setPersonalList(newDevice.getPersonalList());
    }

    /**
     * 获得电机比率
     */
    /*public static double getCurrentRate() {
        Setting setting = null;
        try {
            setting = MyApplication.getInstance().getDbManager().selector(Setting.class).findFirst();
        } catch (DbException e) {
            e.printStackTrace();
        }
        return Double.parseDouble(setting.getRate());
    }*/

    /**
     * 自定义字体
     * 设置成静态变量，节省每个界面加载时重复创建资源
     */
    //中文字体样式
    public static Typeface typefaceChar;
    //数字字体样式
    public static Typeface typefaceNum;

    /**
     * 初始化BLE管理器
     */
    private void initBLEManager() {
        BleManager.getInstance().init(this);
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setOperateTimeout(3000);
        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                //.setServiceUuids(serviceUuids)      // 只扫描指定的服务的设备，可选
                //.setDeviceName(true, names)         // 只扫描指定广播名的设备，可选
                //.setDeviceMac(mac)                  // 只扫描指定mac的设备，可选
                //.setAutoConnect(isAutoConnect)      // 连接时的autoConnect参数，可选，默认false
                .setScanTimeOut(1500)              // 扫描超时时间，可选，默认10秒
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
    }

}
