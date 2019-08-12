package com.bdl.airecovery.contoller;


import com.bdl.airecovery.constant.MotorConstant;
import com.bdl.airecovery.mao.MotorSocketClient;
import com.bdl.airecovery.util.CodecUtils;
import com.bdl.airecovery.util.MessageUtils;
import com.bdl.airecovery.util.MessageUtils.*;


public class Writer {
    private static final String TAG = "Writer";
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
        if (value < MotorConstant.MIN_BACK_TORQUE) {
            value = MotorConstant.MIN_BACK_TORQUE;
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
        if (value < MotorConstant.MIN_KEEP_ARM_TORQUE) {
            value = MotorConstant.MIN_KEEP_ARM_TORQUE;
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