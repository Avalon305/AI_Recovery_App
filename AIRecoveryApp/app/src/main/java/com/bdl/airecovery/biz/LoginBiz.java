package com.bdl.airecovery.biz;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.bluetooth.CommonMessage;
import com.bdl.airecovery.entity.Device;
import com.bdl.airecovery.entity.Personal;
import com.bdl.airecovery.entity.PersonalInfo;
import com.bdl.airecovery.entity.login.Helperuser;
import com.bdl.airecovery.entity.login.User;
import com.bdl.airecovery.netty.DataSocketClient;
import com.bdl.airecovery.proto.BdlProto;
import com.bdl.airecovery.proto.DataProtoUtil;
import com.bdl.airecovery.util.CommonUtils;
import com.google.gson.Gson;

import org.xutils.DbManager;
import org.xutils.common.util.LogUtil;
import org.xutils.ex.DbException;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 蓝牙与发卡器的公有方法
 */
public class LoginBiz {

    private volatile static LoginBiz instance = new LoginBiz();

    private LoginBiz(){

    }

    public static LoginBiz getInstance() {
        return instance;
    }
    //单例闭锁
    public CountDownLatch COUNT_DOWN_LATCH = null;
    //重入锁
    private ReentrantLock lock = new ReentrantLock();

    /**
     * 解决多线程回调的策略
     * 1.等待netty回调的时候本线程睡眠5秒，再去检测LoginUser的值、
     * 2.闭锁，目前静态闭锁出现了失效，经过测试后修正
     * 3.handler,子线程中使用handler，要解决looper问题
     *
     * 注意：
     * 1.这里面的方法不用异步，是在service中启动线程去执行的
     * 2.蓝牙与发卡器使用本类单例，异步执行才有互斥的效果
     * 3.调用这个方法的前提是手环名字或者卡的名字合法，才能调用。
     *   service要区分第一第二用户登录端情况，如果没有第一用户，不允许发第二用户登录端请求，即不能调用该方法
     * @param name 需要的名字
     * @param who 第一用户：1 第二用户：2
     * @return 返回的是，此次登录的结果：在线有，在线没有，本地有，本地没有。使用我们的常量（1-6 根据第二用户）。
     *         教练第二用户只能远程登陆成功，本地登录成功，远程/本地 查无此人不予处理
     *         如果返回0，说明本次不需要继续执行了，另一种登录方式已经登录成功了
     */
    public int loginBiz(String name,int who){
        try {
            //发卡器与此方法共用，防止并发，先上锁。
            lock.lock();
            //每次执行都要更新一下锁，方便netty进行countdown
            COUNT_DOWN_LATCH = new CountDownLatch(1);
            //0.进来之后先检查要进行的操作（设置user值有没有被对方执行了，区分第一用户，第二用户，如果有，则return true）
            boolean hasExecuted = checkHasExecuted(name,who);
            if(hasExecuted){
                return 0;
            }
            //TODO 要求在待机页面创建的时候置空一下当前的登录对象，在oncreate方法里面

            //向教练机发送请求,然后等待，500ms，教练机没响应说明凉凉了。需要注意传入的参数，是request中需要的参数，确认是手环名字还是啥奇葩东西。
            sendLoginRequest(name);
            LogUtil.e("上锁。。。。。");
            COUNT_DOWN_LATCH.await(500,TimeUnit.MILLISECONDS);//收到响应：17:53:32.370 - 阻塞结束：17:53:31.840
            LogUtil.e("解锁。。。。。");

            boolean loginResult = checkLoginResp(who);

            LogUtil.e("闭锁阻塞结束");
            //TODO 看看给user加一个字段，注明登录类型，最后return的就是该登录类型
            if (loginResult){
                LogUtil.e("在线登录");
                //1.连接上教练机，处理有此人，无此人。 区分第一第二用户
                List<PersonalInfo> resPerson = new ArrayList<>();
                try {
                    resPerson = MyApplication.getInstance().getDbManager().selector(PersonalInfo.class)
                            .where("userId","=",name)
                            .findAll();
                } catch (DbException e) {
                    e.printStackTrace();
                    LogUtil.e("查询数据库失败");
                }
                //剔除非当前设备类型的信息
                if (resPerson != null){
                    for (int i = 0;i<resPerson.size();i++){
                        if (!resPerson.get(i).getDeviceType()
                                .equals(BdlProto.DeviceType.getDescriptor().getName())){
                            resPerson.remove(i);
                        }
                    }
                }
                //第一用户
                if (who == 1){
                    //如果远程有此人
                    if (resPerson != null && resPerson.size() != 0){
                        LogUtil.e("当前用户是否有教练？");
                        if (MyApplication.getInstance().getUser() != null){
                            LogUtil.e(MyApplication.getInstance().getUser().getHelperuser() == null ? "没有" : "有");
                        }
                        for (Personal personal:MyApplication.getInstance().getCurrentDevice().getPersonalList()) {
                            if (personal.getName().equals("前方限制")) {
                                LogUtil.e("前方限制" + String.valueOf(personal));
                            } else if (personal.getName().equals("后方限制")) {
                                LogUtil.e("后方限制" + String.valueOf(personal));
                            }
                        }
                        return CommonMessage.FIRST__LOGIN_SUCCESS_ONLINE;
                    }
                    //如果远程无此人
                    else {
                        return CommonMessage.FIRST__LOGIN_REGISTER_ONLINE;
                    }
                }
                //第二用户
                else if (who == 2){
                    //如果远程有此人
                    if (resPerson != null && resPerson.size() != 0){

                        LogUtil.e("第二用户登录");
                        for (Personal personal:MyApplication.getInstance().getCurrentDevice().getPersonalList()) {
                            if (personal.getName().equals("前方限制")) {
                                LogUtil.e("前方限制" + String.valueOf(personal));
                            } else if (personal.getName().equals("后方限制")) {
                                LogUtil.e("后方限制" + String.valueOf(personal));
                            }
                        }

                        return CommonMessage.SECOND__LOGIN_SUCCESS_ONLINE;
                    }
                    //如果远程无此人
                    else {
                        return CommonMessage.SECOND__LOGIN_FAILE;
                    }
                }
            }else{
                LogUtil.e("离线登录");
                //2.连不上教练机，处理有此人，无此人。 区分第一第二用户
                 Object obj = getByName(name);
                if (obj != null){
                    //2.1 执行本地登录的逻辑  设置当前对象（包括登录类型-本地有此人）
                    //如果第一用户为空，则初始化第一用户
                    if (MyApplication.getInstance().getUser() == null){
                        MyApplication.getInstance().setUser((User)obj);
                        //更新医护设置
                        Gson gson = new Gson();
                        List<PersonalInfo> resPerson = new ArrayList<>();
                        try {
                            resPerson = MyApplication.getInstance().getDbManager().selector(PersonalInfo.class)
                                    .where("userId","=",name)
                                    .findAll();
                        } catch (DbException e) {
                            e.printStackTrace();
                            LogUtil.e("查询数据库失败");
                        }
                        //剔除非当前设备类型的信息
                        if (resPerson != null){
                            for (int i = 0;i<resPerson.size();i++){
                                if (!resPerson.get(i).getDeviceType()
                                        .equals(BdlProto.DeviceType.getDescriptor().getName())){
                                    resPerson.remove(i);
                                }
                            }
                        }
                        //如果查有此人，则转换成User之后返回该User对象
                        if (resPerson != null && resPerson.size() != 0){
                            String deviceJSON = resPerson.get(0).getDevicePersonalList();
                            MyApplication.getInstance().UserUpdateDevice(gson.fromJson(deviceJSON,Device.class));
                        }
                        return CommonMessage.FIRST__LOGIN_SUCCESS_OFFLINE;
                    }
                    //如果第一用户非空，则初始化第二用户（判断是否为教练）
                    else {
                        //如果是教练初始化全局变量
                        if (((User) obj).getRole().equals("coach")){
                            MyApplication.getInstance().getUser().setHelperuser(new Helperuser(
                                    ((User) obj).getType(),
                                    ((User) obj).getPhone(),
                                    ((User) obj).getUsername(),
                                    ((User) obj).getUserId()
                            ));
                            return CommonMessage.SECOND__LOGIN_SUCCESS_OFFLINE;
                        }
                        //如果不是教练
                        else {
                            return CommonMessage.SECOND__LOGIN_FAILE;
                        }
                    }
                }else{
                    //2.2 执行本地登录的逻辑  设置默认对象（包括登录类型-本地无此人）
                    List<BdlProto.DeviceType> temp = new ArrayList<>();
                    //0为力量循环，1为耐力循环
                    if(CommonUtils.getDeviceType().getNumber() == 1){
                        for(int i = 0;i < 10;i++){
                            temp.add(BdlProto.DeviceType.forNumber(i));
                        }
                    }else{
                        for(int i = 10;i < 17;i++){
                            temp.add(BdlProto.DeviceType.forNumber(i));
                        }
                    }
                    MyApplication.getInstance().setUser(new User(
                            null,
                            "trainee",
                            "bluetooth",
                            name.substring(name.length()-4,name.length()),
                            name.substring(0,name.length()-4),
                            name,
                            0,
                            80,
                            25,
                            false,
                            "标准模式",
                            140,
                            String.valueOf(temp),
                            1,
                            1,
                            1,
                            false

                    ));
                    return CommonMessage.FIRST__LOGIN_REGISTER_OFFLINE;
                }
            }

//            LogUtil.d("before CountDownLatch...");
//            //此方法用于发出去LoginRequest之后，在netty的listener中唤醒，防止意外的话，要有超时判定,超时之后自动往下执行
//            COUNT_DOWN_LATCH.await(500,TimeUnit.MILLISECONDS);
//
//            LogUtil.d("after CountDownLatch...");
            //最后return的是本地

            //return 0;
        }catch (Exception e){
            LogUtil.e("loginBiz"+e.toString());
            return 0;
        }
        finally {
            lock.unlock();
        }
        return 0;
    }

    /**
     * 从数据库中查询,查到了返回对象，查不到返回null
     * @return
     */
    private User getByName(String name) {
        Gson gson = new Gson();
        DbManager dbManager = MyApplication.getInstance().getDbManager();
        List<PersonalInfo> resPerson = new ArrayList<>();
        try {
            resPerson = dbManager.selector(PersonalInfo.class)
                    .where("userId","=",name)
                    .findAll();
        } catch (Exception e) {
            e.printStackTrace();
            //查询出错也返回null
            return null;
        }
        //剔除非当前设备类型的信息
        if (resPerson != null){
            for (int i = 0;i<resPerson.size();i++){
                if (!resPerson.get(i).getDeviceType()
                        .equals(BdlProto.DeviceType.getDescriptor().getName())){
                    resPerson.remove(i);
                }
            }
        }
        //如果查有此人，则转换成User之后返回该User对象
        if (resPerson != null && resPerson.size() != 0){
            String infoJSON = resPerson.get(0).getInfoPersonalList();
            return gson.fromJson(infoJSON,User.class);
        }
        //如果查无此人，则返回null
        else {
            return null;
        }
    }

    /**
     * 该方法用于检查登录的结果
     * 有,此人信息的反馈netty监听器执行的操作：设置登录对象+插入/更新本地对象,返回true
     * 无，此人信息的反馈netty监听器执行的操作：设置默认用户信息到当前对象，此人信息不插入本地库，返回true
     * 连接不上的反馈netty监听器执行的操作：连接不上就设置要登录的用户是null，
     * 需要考虑测试时候直接跳转主页面，没有第一用户,连接第二用户的情况，返回false
     * @return
     */
    private boolean checkLoginResp(int who) {

        boolean result = false;
        if (who == 1){
            if (MyApplication.getInstance().getUser() == null){
                LogUtil.e("一、第一用户未初始化");
                result = false;
            }else {
                LogUtil.e("一、第一用户初始化");
                result = true;
            }
        }
        if (who == 2){
            if (MyApplication.getInstance().getUser() != null && MyApplication.getInstance().getUser().getHelperuser() == null){
                LogUtil.e("二、第二用户初始化");
                result = false;
            }else {
                LogUtil.e("二、第二用户未初始化");
                result = true;
            }
        }
        return result;
    }

    /**
     * 在这个方法中执行发送登录请求的指令，如果教练机有反馈需要做的是
     * 1.解析登录的结果设置到loginUser
     * 2.发出解锁请求（有响应+出异常都需要考虑进去）
     * @param name
     */
    private AtomicInteger Seq = new AtomicInteger(1);
    private void sendLoginRequest(String name) {

        //生成请求
        BdlProto.LoginRequest request =
                BdlProto.LoginRequest.newBuilder().setDeviceType(CommonUtils.getDeviceType())
                        .setUid(name).build();
        //请求递增，seq达到 Integer.MAX_VALUE时重新计数
        final BdlProto.Message message = DataProtoUtil.packLoginRequest(Seq.get(),request);
        LogUtil.i("Message : " + message);
        Seq.getAndIncrement();
        if (Seq.get() == Integer.MAX_VALUE){
            Seq.set(1);
        }
        try{
            LogUtil.i("发送数据！");
            DataSocketClient.getInstance().sendMsg(message);
        }catch (ConnectException e){
            LogUtil.i("异常解锁！");
            //是异常，也要解锁
            LoginBiz.getInstance().COUNT_DOWN_LATCH.countDown();
        }
    }

    /**
     * 在这个方法里面检查当前用户是否登录了，如果登录了，返回true，否则返回false。
     * 需要注意的是，要比对传入的name和已经登录的name，如果能匹配上才返回true
     * 如果手环和卡不是一个人，然后再次登录可能会出错
     * @return
     */
    private boolean checkHasExecuted(String name,int who) {
        //默认没有执行
        boolean result = false;
        //如果是第一用户
        if (who == 1 && MyApplication.getInstance().getUser() != null){
               result = true;
        }
        //如果是第二用户
        if(who == 2 && MyApplication.getInstance().getUser() != null
                && MyApplication.getInstance().getUser().getHelperuser() != null){
                result = true;
        }
        return result;
    }
}
