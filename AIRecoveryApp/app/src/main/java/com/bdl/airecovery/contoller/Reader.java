package com.bdl.airecovery.contoller;

import android.util.Log;

import com.bdl.airecovery.constant.MotorConstant;
import com.bdl.airecovery.mao.MotorSocketClient;
import com.bdl.airecovery.util.MessageUtils;
import com.bdl.airecovery.util.MessageUtils.*;

public class Reader {
    private static final String TAG = "Reader";

    //各状态位信息
    public enum StatusBit {
        /**
         * 急停
         */
        EStop(1),

        /**
         * 回零相关
         */
        READY(5),
        /**
         * 回零相关
         */
        INHIBIT(7),
        /**
         * 回零相关
         */
        HOMING_POSIAVAILABLE(14),
        /**
         * 失败
         */
        FAIL(15);
        private int value;

        private StatusBit(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * 发送报文到服务端，并返回服务端回传的报文
     *
     * @param msg
     * @return
     * @throws Exception
     */
    public static String getRespData(byte[] msg) throws Exception {
        //发送报文到服务端，并且返回服务端回传的报文
        byte[] respMsg = MotorSocketClient.getInstance().sendMessage(msg);
        if (respMsg == null) {
            return null;
        }
        String readOrWrite = MessageUtils.getParameter(respMsg, Parameter.RORW);
        String response = MessageUtils.getParameter(respMsg, Parameter.RESPONSE);
        String type = MessageUtils.getParameter(respMsg, Parameter.TYPE);
        if (readOrWrite.equals("82") && response.equals("80")) {
            return MessageUtils.getData(respMsg);
        }
        Log.e(TAG, "收到服务端错误响应");
        return null;
    }

    /**
     * 发送读取状态位的请求，可选择相应的状态位
     * @param statusBit
     * @return
     * @throws Exception
     */
    public static String getStatus(StatusBit statusBit) throws Exception {
        byte[] respMsg = MotorSocketClient.getInstance().sendMessage(MotorConstant.READ_STATE);
        if (respMsg == null) {
            return null;
        }
        String bitField = MessageUtils.getStatusData(respMsg);
        return String.valueOf(bitField.charAt(statusBit.getValue()));
    }

}
