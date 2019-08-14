package com.bdl.airecovery.proto;

public class CountDownProtoUtil {
    /**
     * 打包心跳请求
     *
     * @param seq
     * @param request
     * @return
     */
    public static BdlProto.Message packKeepaliveRequest(int seq, BdlProto.KeepaliveRequest request) {
        return BdlProto.Message.newBuilder().setSequence(seq).setKeepaliveRequest(request)
                .setType(BdlProto.HeadType.Keepalive_Request).build();

    }

    /**
     * 打包获取当前时间请求
     *
     * @param seq
     * @param request
     * @return
     */
//    public static BdlProto.Message packCurrentTimeRequest(int seq, BdlProto.CurrentTimeRequest request) {
//        return BdlProto.Message.newBuilder().setSequence(seq).setCurrentTimeRequest(request)
//                .setType(BdlProto.HeadType.CurrentTime_Request).build();
//    }
}
