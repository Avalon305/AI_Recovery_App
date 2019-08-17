package com.bdl.airecovery.proto;

public class DataProtoUtil {


    /**
     * Ò
     * 打包登陆请求
     *
     * @param seq
     * @param request
     * @return
     */
    public static BdlProto.Message packLoginRequest(int seq, BdlProto.LoginRequest request) {
        return BdlProto.Message.newBuilder()
                .setSequence(seq)
                .setLoginRequest(request)
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
        return BdlProto.Message.newBuilder()
                .setSequence(seq)
                .setUploadRequest(request)
                .setType(BdlProto.HeadType.Upload_Request)
                .build();
    }
    /**
     * 打包个人设置请求
     *
     * @param seq
     * @param request
     * @return
     */
    public static BdlProto.Message packPersonalSetRequest(int seq, BdlProto.PersonalSetRequest request) {
        return BdlProto.Message.newBuilder()
                .setSequence(seq)
                .setPersonalSetRequest(request)
                .setType(BdlProto.HeadType.PersonalSet_Request)
                .build();
    }

    /**
     *  @author zfc
     *  @time 2019/8/14  17:03
     *  @describe 肌力测试上传请求
     */
    public static BdlProto.Message packMuscleStrengthRequest(int seq,BdlProto.MuscleStrengthRequest request){
        return  BdlProto.Message.newBuilder()
                .setSequence(seq)
                .setMuscleStrengthRequest(request)
                .setType(BdlProto.HeadType.MuscleStrength_Request)
                .build();
    }

    /**
     *  @author zfc
     *  @time 2019/8/14  17:11
     *  @describe 错误信息上传请求
     */
    public  static  BdlProto.Message packErrorInfoRequest(int seq,BdlProto.ErrorInfoRequest request){
        return  BdlProto.Message.newBuilder().setSequence(seq)
                 .setErrorInfoRequest(request)
                 .setType(BdlProto.HeadType.ErrorInfo_Request)
                 .build();
    }
}
