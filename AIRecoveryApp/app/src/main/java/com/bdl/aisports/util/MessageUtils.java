package com.bdl.aisports.util;

import android.net.wifi.aware.PublishConfig;

import com.bdl.aisports.MyApplication;
import com.bdl.aisports.constant.MotorConstant;
import com.bdl.aisports.util.CodecUtils;

import java.math.BigInteger;
import java.text.DecimalFormat;

public class MessageUtils {
    public enum Parameter {
        /**
         * 读写参数
         */
        RORW(1),

        /**
         * 响应参数
         */
        RESPONSE(2),
        /**
         * 参数类型
         */
        TYPE(10);

        private int value;

        private Parameter(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * 获取报文各位上的参数
     *
     * @param bytes
     * @param parameter
     * @return
     */
    public static String getParameter(byte[] bytes, Parameter parameter) {
        return Integer.toHexString(bytes[parameter.getValue()] & 0xFF);
    }

    /**
     * 在报文中获取数据
     * 1byte :20 ~ 19
     * 2bytes:21 ~ 19
     * 4bytes:23 ~ 19
     *
     * @param bytes
     * @return
     */
    public static String getData(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 23; i > 19; i--) {
            String s = Integer.toHexString(bytes[i] & 0xFF);
            if (s.length() == 1) {
                stringBuffer.append("0" + s);
            } else {
                stringBuffer.append(s);
            }
        }
        BigInteger bi = new BigInteger(String.valueOf(stringBuffer), 16);
        int a = bi.intValue();
        return String.valueOf(a);
    }

    /**
     * 获取150号参数bitfield类型的数据
     *
     * @param bytes
     * @return
     */
    public static String getStatusData(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 21; i > 19; i--) {
            String s = Integer.toHexString(bytes[i] & 0xFF);
            if (s.length() == 1) {
                stringBuffer.append("0" + s);
            } else {
                stringBuffer.append(s);
            }
        }
        StringBuffer stringBuffer1 = new StringBuffer(CodecUtils.hexString2binaryString(String.valueOf(stringBuffer)));
        return String.valueOf(stringBuffer1.reverse());
    }

    /**
     * 映射0~199上的值
     *
     * @param position
     * @return
     */
    public static int getMappedValue(int position) {
        return (int) (((MyApplication.getInstance().getCurrentDevice().getMaxLimit() - MyApplication.getInstance().getCurrentDevice().getMinLimit()) / 200.0) * (position + 1));
    }

    /**
     * 翻转前后方限制
     */
    public static int[] recersalLimit(int frontLimitedPosition, int rearLimitedPosition, int motorDirection) {
        int midPoint = (MyApplication.getInstance().getCurrentDevice().getMaxLimit() + MyApplication.getInstance().getCurrentDevice().getMinLimit()) / 2 * 10000; //中点的值
        int newFrontLimitedPosition = midPoint + (midPoint - rearLimitedPosition);
        int newRearLimitedPosition = midPoint - (frontLimitedPosition - midPoint);
        switch (motorDirection) { //运动方向
            case 1: //向右
                if (midPoint - rearLimitedPosition > frontLimitedPosition - midPoint) {
                    return new int[]{frontLimitedPosition, rearLimitedPosition};
                } else {
                    return new int[]{newFrontLimitedPosition, newRearLimitedPosition};
                }
            case 2: //向左
                if (midPoint - rearLimitedPosition < frontLimitedPosition - midPoint) {
                    return new int[]{frontLimitedPosition, rearLimitedPosition};
                } else {
                    return new int[]{newFrontLimitedPosition, newRearLimitedPosition};
                }
        }
        return new int[]{frontLimitedPosition, rearLimitedPosition};
    }
}
