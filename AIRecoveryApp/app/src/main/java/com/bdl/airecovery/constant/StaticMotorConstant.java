package com.bdl.airecovery.constant;

public class StaticMotorConstant {
    //移动到指定位置,默认为1（1~99%  0x01~0x63）----------------------主机发送
    public static final byte[] MOVE = {
            0x55, (byte) 0xAA,   //起始位
            0x00, 0x05,          //包长度（从设备厂商标识到数据区字节数）
            0x00, 0x00,          //设备厂商标识（固定）
            0x00, 0x05,          //命令字
            0x01,//0%            //数据区***
            0x01,                //校验位***
            0x55, (byte) 0xAA    //结束位
    };
    public static final byte[] MOVE_HEAD = {
            0x55, (byte) 0xAA,   //起始位
            0x00, 0x05,          //包长度（从设备厂商标识到数据区字节数）
            0x00, 0x00,          //设备厂商标识（固定）
            0x00, 0x05           //命令字
    };
    public static final byte[] TILL = {
            0x55, (byte) 0xAA    //结束位
    };

    //心跳----------------------------------------------------------主机发送
    public static final byte[] HEARTBEAT = {
            0x55, (byte) 0xAA,   //起始位
            0x00, 0x04,          //包长度（从设备厂商标识到数据区字节数）
            0x00, 0x00,          //设备厂商标识（固定）
            0x00, 0x00,          //命令字
            0x04,                //校验位
            0x55, (byte) 0xAA    //结束位
    };
    //过流通知应答---------------------------------------------------主机应答
    public static final byte[] ANSWER_OVERCURRENT = {
            0x55, (byte) 0xAA,   //起始位
            0x00, 0x05,          //包长度（从设备厂商标识到数据区字节数）
            0x00, 0x00,          //设备厂商标识（固定）
            0x00, 0x03,          //命令字
            0x06,                //校验位
            0x55, (byte) 0xAA    //结束位
    };

    //移动完成通知应答-----------------------------------------------主机发送
    public static final byte[] ANSWER_MOVEDONE = {
            0x55, (byte) 0xAA,
            0x00, 0x04,
            0x00, 0x00,
            0x00, 0x06,
            0x02,                //校验位
            0x55, (byte) 0xAA
    };
    //获取当前位置---------------------------------------------------主机发送
    public static final byte[] GETPOSITION = {
            0x55, (byte) 0xAA,
            0x00, 0x04,
            0x00, 0x00,
            0x00, 0x07,
            0x03,                //校验位
            0x55, (byte) 0xAA
    };
    //到达限位通知应答-----------------------------------------------主机发送
    public static final byte[] ANSWER_REACHLIMIT = {
            0x55, (byte) 0xAA,   //起始位
            0x00, 0x05,          //包长度（从设备厂商标识到数据区字节数）
            0x00, 0x00,          //设备厂商标识（固定）
            0x00, 0x04,          //命令字
            0x01,                //校验位
            0x55, (byte) 0xAA    //结束位
    };

    //启动电机正转----------------------------------------------------主机发送
    public static final byte[] STARTUP = {
            0x55, (byte) 0xAA,   //起始位
            0x00, 0x05,          //包长度（从设备厂商标识到数据区字节数）
            0x00, 0x00,          //设备厂商标识（固定）
            0x00, 0x01,          //命令字
            0x00,//0为正转1为反转 //数据区
            0x04,                //校验位
            0x55, (byte) 0xAA    //结束位
    };
    //启动电机反转----------------------------------------------------主机发送
    public static final byte[] STARTDOWN = {
            0x55, (byte) 0xAA,   //起始位
            0x00, 0x05,          //包长度（从设备厂商标识到数据区字节数）
            0x00, 0x00,          //设备厂商标识（固定）
            0x00, 0x01,          //命令字
            0x01,//0为正转1为反转 //数据区
            0x05,                //校验位
            0x55, (byte) 0xAA    //结束位
    };

    //停止电机-------------------------------------------------------主机发送
    public static final byte[] STOP = {
            0x55, (byte) 0xAA,   //起始位
            0x00, 0x04,          //包长度（从设备厂商标识到数据区字节数）
            0x00, 0x00,          //设备厂商标识（固定）
            0x00, 0x02,          //命令字
            0x06,                //校验位
            0x55, (byte) 0xAA    //结束位
    };
    //标定
    public static final byte[] INIT = {
            0x55, (byte) 0xAA,   //起始位
            0x00, 0x04,          //包长度（从设备厂商标识到数据区字节数）
            0x00, 0x00,          //设备厂商标识（固定）
            0x00, 0x08,          //命令字
            0x0C,                //校验位
            0x55, (byte) 0xAA    //结束位
    };





    //////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////
    //收到标定应答----------------------------------------------------设备发送
    public static final byte[] ANSWER_INIT = {
            0x55, (byte) 0xAA,   //起始位
            0x00, 0x04,          //包长度（从设备厂商标识到数据区字节数）
            0x00, 0x00,          //设备厂商标识（固定）
            0x00, 0x08,          //命令字
            0x0C,                //校验位
            0x55, (byte) 0xAA    //结束位
    };
    //标定完成应答----------------------------------------------------设备发送
    public static final byte[] FINISH_INIT = {
            0x55, (byte) 0xAA,   //起始位
            0x00, 0x04,          //包长度（从设备厂商标识到数据区字节数）
            0x00, 0x00,          //设备厂商标识（固定）
            0x00, 0x09,          //命令字
            0x0D,                //校验位
            0x55, (byte) 0xAA    //结束位
    };
    //停止电机应答----------------------------------------------------设备发送
    public static final byte[] ANSWER_STOP = {
            0x55, (byte) 0xAA,   //起始位
            0x00, 0x05,          //包长度（从设备厂商标识到数据区字节数）
            0x00, 0x00,          //设备厂商标识（固定）
            0x00, 0x02,          //命令字
            0x07,                //校验位
            0x55, (byte) 0xAA    //结束位
    };
    //过流通知-------------------------------------------------------设备发送
    public static final byte[] OVERCURRENT = {
            0x55, (byte) 0xAA,   //起始位
            0x00, 0x05,          //包长度（从设备厂商标识到数据区字节数）
            0x00, 0x00,          //设备厂商标识（固定）
            0x00, 0x03,          //命令字
            0x06,                //校验位
            0x55, (byte) 0xAA    //结束位
    };

    //到达限位通知---------------------------------------------------设备发送
    public static final byte[] REACHLIMIT = {
            0x55, (byte) 0xAA,   //起始位
            0x00, 0x05,          //包长度（从设备厂商标识到数据区字节数）
            0x00, 0x00,          //设备厂商标识（固定）
            0x00, 0x04,          //命令字
            0x00,//0为正转限位1为反转限位            //数据区
            0x06,                //校验位
            0x55, (byte) 0xAA    //结束位
    };
    public static final byte[] REACHLIMIT_HEAD = {
            0x55, (byte) 0xAA,   //起始位
            0x00, 0x05,          //包长度（从设备厂商标识到数据区字节数）
            0x00, 0x00,          //设备厂商标识（固定）
            0x00, 0x04           //命令字
    };


    //心跳应答-------------------------------------------------------设备发送
    public static final byte[] ANSWER_HEARTBEAT = {
            0x55, (byte) 0xAA,   //起始位
            0x00, 0x05,          //包长度（从设备厂商标识到数据区字节数）
            0x00, 0x00,          //设备厂商标识（固定）
            0x00, 0x00,          //命令字
            0x00,//设备软件版本号 //数据区
            0x06,                //校验位
            0x55, (byte) 0xAA    //结束位
    };
    public static final byte[] ANSWER_HEARTBEAT_HEAD = {
            0x55, (byte) 0xAA,   //起始位
            0x00, 0x0D,          //包长度（从设备厂商标识到数据区字节数）
            0x00, 0x00,          //设备厂商标识（固定）
            0x00, 0x00,          //命令字
    };
    //移动到指定位置应答----------------------------------------------设备发送
    public static final byte[] ANSWER_MOVE = {
            0x55, (byte) 0xAA,
            0x00, 0x04,
            0x00, 0x00,
            0x00, 0x05,
            0x01,                //校验位
            0x55, (byte) 0xAA
    };
    //移动完成通知---------------------------------------------------设备发送
    public static final byte[] MOVEDONE = {
            0x55, (byte) 0xAA,
            0x00, 0x04,
            0x00, 0x00,
            0x00, 0x06,
            0x02,                //校验位
            0x55, (byte) 0xAA
    };
    //获取当前位置应答（0~100%  0x00~0x64，0xFF为未知）----------------设备发送
    public static final byte[] ANSWER_GETPOSITION = {
            0x55, (byte) 0xAA,
            0x00, 0x05,
            0x00, 0x00,
            0x00, 0x07,
            0x64,
            0x06,                //校验位
            0x55, (byte) 0xAA
    };
    public static final byte[] ANSWER_GETPOSITION_HEAD = {
            0x55, (byte) 0xAA,
            0x00, 0x05,
            0x00, 0x00,
            0x00, 0x07
    };

    //启动电机应答----------------------------------------------------设备发送
    public static final byte[] ANSWER_START = {
            0x55, (byte) 0xAA,   //起始位
            0x00, 0x04,          //包长度（从设备厂商标识到数据区字节数）
            0x00, 0x00,          //设备厂商标识（固定）
            0x00, 0x01,          //命令字
            0x05,                //校验位
            0x55, (byte) 0xAA    //结束位
    };
}