package com.bdl.airecovery.util;

import android.media.audiofx.AudioEffect;

import com.bdl.airecovery.entity.SegmentCalibration;
import org.xutils.DbManager;
import org.xutils.common.util.LogUtil;
import org.xutils.ex.DbException;

import java.util.List;

public class CalibrationUtil {
    private DbManager db;
    public CalibrationUtil(DbManager db) {
        this.db = db;
    }
    //-------------------------标定设置-------------------------//
    /**
     * 最小的分段开始值
     */
    public static final int SEGMENT_START = 80;

    /**
     * 最大的分段开始值
     */
    public static final int SEGMENT_END = 180;

    /**
     * 分段个数
     */
    public static final int SEGMENT_NUM = 10;

    //---------------------标定参数的默认值---------------------//
    /**
     * 去程速度，助力速度
     * 默认-5
     */
    public static final int GOING_SPEED = 0;

    /**
     * 返回速度
     */
    public static final int RETURN_SPEED = 35;

    /**
     * 回弹力量
     */
    public static final int BOUNCE = 15;

    /**
     * 使力臂拉动的阈值
     */
    public static final int PULL_THRESHOLD_VAL = -2;


    /**
     * 此方法根据各种标定设置的默认值初始化数据库
     */
    public void initSegmentCalibration() throws DbException {
        SegmentCalibration calibration = new SegmentCalibration();
        for (int i = 5; i <= 99; i++) {
            for (int j = SEGMENT_START; j <= SEGMENT_END; j += SEGMENT_NUM) {
                calibration.setForce(i);
                calibration.setSegmentPosition(j);
                calibration.setGoingTorque(i);
                calibration.setReturnTorque(i);
                calibration.setGoingSpeed(GOING_SPEED);
                calibration.setReturnSpeed(RETURN_SPEED);
                calibration.setBounce(BOUNCE);
                calibration.setPullThresholdVal(PULL_THRESHOLD_VAL);
                this.db.save(calibration);
            }
        }
    }

    /**
     * 重置所有参数
     */
    public void resetSegmentCalibration() throws DbException {
        SegmentCalibration calibration = new SegmentCalibration();
        int count = 0;
        for (int i = 5; i <= 99; i++) {
            for (int j = SEGMENT_START; j <= SEGMENT_END; j += SEGMENT_NUM) {
                count++;
                calibration.setId(count);
                calibration.setForce(i);
                calibration.setSegmentPosition(j);
                calibration.setGoingTorque(i);
                calibration.setReturnTorque(i);
                calibration.setGoingSpeed(GOING_SPEED);
                calibration.setReturnSpeed(RETURN_SPEED);
                calibration.setBounce(BOUNCE);
                calibration.setPullThresholdVal(PULL_THRESHOLD_VAL);
                this.db.update(calibration);
            }
        }
    }



    /**
     * 获得某个力矩下的所有分段值
     * @param force 力
     * @return 该力矩下的分段List
     */
    public List<SegmentCalibration> getCalibrationsByForce(int force) throws DbException {
        LogUtil.e("当前力===========" + force);
        return db.selector(SegmentCalibration.class)
                .where("force","=",force)
                .findAll();
    }

    /**
     * 通过当前位置获取相应的参数值
     * @param currentPosition 当前位置此值为0~200的值，可能需要外部转换
     * @param calibrations 当前力矩下的所有标定参数集合，应配合{@link CalibrationUtil#getCalibrationsByForce}方法
     * @return 某条标定参数值
     */
    public SegmentCalibration getCalibrationsByCurrentPosition(List<SegmentCalibration> calibrations, int currentPosition) throws DbException {
        if (currentPosition > SEGMENT_END) {  //当前位置大于标定的最大位置，返回最大位置标定的值
            return getCalibrationByPosition(calibrations, SEGMENT_END);
        }
        if (currentPosition <= SEGMENT_START) { //当前位置小于标定的最大位置，返回最小位置标定的值
            return getCalibrationByPosition(calibrations, SEGMENT_START);
        }
        //计算当前位置应属的区间端点值
        int position = 180 - SEGMENT_NUM * ((SEGMENT_END - currentPosition) / SEGMENT_NUM);
        LogUtil.e("当前段===========" + position);
        return getCalibrationByPosition(calibrations, position);
    }

    /**
     * 在某个参数List中获取某个位置的参数对象
     * @param calibrations
     * @param position
     * @return
     */
    private SegmentCalibration getCalibrationByPosition(List<SegmentCalibration> calibrations, int position) {
        for (SegmentCalibration s : calibrations) {
            if (position == s.getSegmentPosition()) {
                LogUtil.e("当前参数====" + s.toString());
                return s;
            }
        }
        return null;
    }

}
