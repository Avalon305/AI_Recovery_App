package com.bdl.airecovery.bluetooth;

/*
  此消息用于蓝牙发广播，在这里定义msgtype，比如用户登录成功，
 */
public class CommonMessage {
    //心率类型
    public static final int HEART_BEAT = 1;
    //用户登录的反馈：在线登录，离线登录，远程无此人登录，本地无此人登录
    public static final int LOGIN_SUCCESS_ONLINE = 2;
    public static final int LOGIN_SUCCESS_OFFLINE = 3;
    public static final int LOGIN_REGISTER_ONLINE = 4;
    public static final int LOGIN_REGISTER_OFFLINE = 5;
    //用户掉线与退出的广播,掉线是蓝牙专用，目前掉线仅监听，并无发送
    public static final int CONNECT_SUCCESS = 8; //连接成功
    public static final int DISCONNECTED = 9; //连接断开
    public static final int LOGOUT = 10;


    public CommonMessage(int msgType, String attachment) {
        this.msgType = msgType;
        this.attachment = attachment;
    }

    public CommonMessage() {

    }

    //消息类型
    private int msgType;
    //字符串附加物
    private String attachment;

    public int getMsgType() {
        return msgType;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    //快速生成心率对象
    public static CommonMessage heartBeatMsg(String attachment){
        CommonMessage commonMessage = new CommonMessage();
        commonMessage.setMsgType(HEART_BEAT);
        commonMessage.setAttachment(attachment);
        return commonMessage;
    }
    @Override
    public String toString() {
        return "CommonMessage{" +
                "msgType=" + msgType +
                ", attachment='" + attachment + '\'' +
                '}';
    }
}
