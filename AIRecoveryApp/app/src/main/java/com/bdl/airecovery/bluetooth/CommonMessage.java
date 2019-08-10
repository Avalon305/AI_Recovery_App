package com.bdl.airecovery.bluetooth;

/*
  此消息用于蓝牙发广播，在这里定义msgtype，比如用户登录成功，
 */
public class CommonMessage {
    //心率类型
    public static final int HEART_BEAT = 1;
    //第一用户登录的反馈：在线登录，离线登录，远程无此人登录，本地无此人登录
    public static final int FIRST__LOGIN_SUCCESS_ONLINE = 2;
    public static final int FIRST__LOGIN_SUCCESS_OFFLINE = 3;
    public static final int FIRST__LOGIN_REGISTER_ONLINE = 4;
    public static final int FIRST__LOGIN_REGISTER_OFFLINE = 5;
    //第二用户登录的反馈，因为必须是教练，所以身份必须存在，登录成功一定是远程或者本地有，都没有就是失败登录
    public static final int SECOND__LOGIN_SUCCESS_ONLINE = 6; //远程登录成功
    public static final int SECOND__LOGIN_SUCCESS_OFFLINE = 7;//本地登录成功
    public static final int SECOND__LOGIN_FAILE = 8;          //教练登录失败
    //第一用户掉线与退出的广播,掉线是蓝牙专用，目前掉线仅监听，并无发送
    public static final int FIRST__DISCONNECTED = 9;
    public static final int FIRST__LOGOUT = 10;
    //第二用户掉线与退出广播,掉线是蓝牙专用，目前掉线仅监听，并无发送
    public static final int SECOND__DISCONNECTED = 11;
    public static final int SECOND__LOGOUT = 12;


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
