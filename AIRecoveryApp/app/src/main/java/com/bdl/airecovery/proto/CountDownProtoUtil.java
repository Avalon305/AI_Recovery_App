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
        return BdlProto.Message.newBuilder()
                .setSequence(seq)
                .setKeepaliveRequest(request)
                .setType(BdlProto.HeadType.Keepalive_Request)
                .build();

    }
}
