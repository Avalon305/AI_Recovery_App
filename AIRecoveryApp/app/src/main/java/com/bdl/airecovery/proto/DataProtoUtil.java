package com.bdl.airecovery.proto;

public class DataProtoUtil {
    /**
     * 打包个人设置请求
     *
     * @param seq
     * @param request
     * @return
     */
    public static BdlProto.Message packPersonalSetRequest(int seq, BdlProto.PersonalSetRequest request) {
        return BdlProto.Message.newBuilder().setSequence(seq).setPersonalSetRequest(request)
                .setType(BdlProto.HeadType.PersonalSet_Request)
                .build();
    }

    /**
     * Ò
     * 打包登陆请求
     *
     * @param seq
     * @param request
     * @return
     */
    public static BdlProto.Message packLoginRequest(int seq, BdlProto.LoginRequest request) {
        return BdlProto.Message.newBuilder().setSequence(seq).setLoginRequest(request)
                .setType(BdlProto.HeadType.Login_Request)
                .build();
    }

    /**
     * 打包上传训练信息请求
     * @param seq
     * @param request
     * @return
     */
    public static BdlProto.Message packUploadRequest(int seq, BdlProto.UploadRequest request) {
        return BdlProto.Message.newBuilder().setSequence(seq).setUploadRequest(request)
                .setType(BdlProto.HeadType.Upload_Request).build();
    }
}
