package com.bdl.aisports;

import com.bdl.aisports.entity.Device;
import com.bdl.aisports.entity.Personal;
import com.bdl.aisports.entity.PersonalInfo;
import com.bdl.aisports.entity.login.Helperuser;
import com.bdl.aisports.entity.login.User;
import com.bdl.aisports.proto.BdlProto;
import com.bdl.aisports.util.CommonUtils;
import com.google.gson.Gson;

import org.xutils.DbManager;
import org.xutils.ex.DbException;

import java.util.List;


public class personalInfoDAO {

    //数据库管理对象
    private DbManager dbManager = MyApplication.getInstance().getDbManager();
    private static personalInfoDAO personalInfoDAO = new personalInfoDAO();
    private static Helperuser nullhelperuser = null;

    /**
     * 私有化构造函数，防止其他人瞎几把new
     */
    private personalInfoDAO() {
    }

    /**
     * getInstance，对外开放获得的方法
     * @return
     */
    public static personalInfoDAO getInstance() {
        return personalInfoDAO;
    }

    /**
     * 根据用户id插入或者更新
     * @param userId
     * @param loginUser
     * @param currentDevice
     * @throws DbException
     */
    public void SavrOrUpdata(String userId,String deviceType, User loginUser, Device currentDevice) throws DbException {
         //1.清除教练用户
         if (loginUser.getHelperuser() != null){
             loginUser.setHelperuser(null);
         }

         //转换JOSN
         Gson gson = new Gson();
         String infoJson = gson.toJson(loginUser);
         String deviceJson = gson.toJson(currentDevice);

         //2、查询是否存在该user
        PersonalInfo res = FindOne(userId);
         if (res != null){
             // 3.如果存在则更新，不存在就插入，在执行之前，转化为json串
             res.setInfoPersonalList(infoJson);
             dbManager.update(res,"infoPersonalList");
             res.setDevicePersonalList(deviceJson);
             dbManager.update(res,"devicePersonalList");
         }else {
             res = new PersonalInfo();
             res.setUserId(userId);
             res.setDevicePersonalList(deviceJson);
             res.setInfoPersonalList(infoJson);
             res.setDeviceType(deviceType);
             dbManager.save(res);
         }
    }

    /**
     * 根据用户id（手环名称）获得的个人信息对象
     * @param userId
     * @return
     * @throws DbException
     */
    public PersonalInfo FindOne(String userId)throws DbException {
        List<PersonalInfo> resPersons = dbManager.selector(PersonalInfo.class)
                .where("userId","=",userId)
                .findAll();
        //剔除非当前设备类型的信息
        if (resPersons != null){
            for (int i = 0;i<resPersons.size();i++){
                if (!resPersons.get(i).getDeviceType()
                        .equals(BdlProto.DeviceType.getDescriptor().getName())){
                    resPersons.remove(i);
                }
            }
        }
        if (resPersons!=null && resPersons.size() != 0){
            return resPersons.get(0);
        }else {
            return null;
        }
    }

}

