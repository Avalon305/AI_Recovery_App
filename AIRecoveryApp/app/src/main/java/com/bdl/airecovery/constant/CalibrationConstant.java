package com.bdl.airecovery.constant;

/**
 * 标定常数,包含：
 * 1. 标定设置
 * 2. 标定参数的初始化默认值
 * 3. 写入时都需要✖100
 */
public class CalibrationConstant {
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
    public static final int GOING_SPEED = -5;

    /**
     * 返回速度
     */
    public static final int RETURN_SPEED = 35;

    /**
     * 回弹力量
     */
    public static final int BOUNCE = 10;

    /**
     * 使力臂拉动的阈值
     */
    public static final int PULL_THRESHOLD_VAL = -2;
}
