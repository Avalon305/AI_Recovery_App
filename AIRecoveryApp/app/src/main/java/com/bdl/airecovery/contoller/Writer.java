package com.bdl.airecovery.contoller;


import android.util.Log;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.constant.MotorConstant;
import com.bdl.airecovery.mao.MotorSocketClient;
import com.bdl.airecovery.util.CodecUtils;
import com.bdl.airecovery.util.MessageUtils;
import com.bdl.airecovery.util.MessageUtils.*;

import java.util.Calendar;


public class Writer {
    private static final String TAG = "Writer";
    private static int minBackTorque  = MyApplication.getInstance()
            .getCalibrationParam()
            .getMinBackTorque()
            * 100;

    /**
     * 设置成功返回true
     *
     * @param value
     * @param originalMsg
     * @return
     * @throws Exception
     */
    public static boolean setParameter(int value, byte[] originalMsg) throws Exception {
        byte[] message = originalMsg;
        byte[] bytesValue = CodecUtils.decimalToBytes(value);
        for (int i = 20, j = 0; j < 4; i++, j++) {
            message[i] = bytesValue[j];
        }
        byte[] respMsg = MotorSocketClient.getInstance().sendMessage(message);
        return MessageUtils.getParameter(respMsg, Parameter.RORW).equals("83") &&
                MessageUtils.getParameter(respMsg, Parameter.RESPONSE).equals("80");
    }

    /**
     * 设置初始回弹力
     */
    public static boolean setInitialBounce(int value) throws Exception {
        Log.e("--------minBackTorque", String.valueOf(minBackTorque));
        if (value < minBackTorque) {
            value = minBackTorque;
        }
        byte[] message = MotorConstant.SET_INITIAL_BOUNCE;
        byte[] bytesValue = CodecUtils.decimalToBytes(value);
        for (int i = 20, j = 0; j < 4; i++, j++) {
            message[i] = bytesValue[j];
        }
        byte[] respMsg = MotorSocketClient.getInstance().sendMessage(message);
        return MessageUtils.getParameter(respMsg, Parameter.RORW).equals("83") &&
                MessageUtils.getParameter(respMsg, Parameter.RESPONSE).equals("80");
    }

    /**
     * 设置保持力臂所需力矩
     */
    public static boolean setKeepArmTorque(int value) throws Exception {
        if (value < minBackTorque) {
            value = minBackTorque;
        }
        byte[] message = MotorConstant.SET_KEEP_ARM_TORQUE;
        byte[] bytesValue = CodecUtils.decimalToBytes(value);
        for (int i = 20, j = 0; j < 4; i++, j++) {
            message[i] = bytesValue[j];
        }
        byte[] respMsg = MotorSocketClient.getInstance().sendMessage(message);
        return MessageUtils.getParameter(respMsg, Parameter.RORW).equals("83") &&
                MessageUtils.getParameter(respMsg, Parameter.RESPONSE).equals("80");
    }

}