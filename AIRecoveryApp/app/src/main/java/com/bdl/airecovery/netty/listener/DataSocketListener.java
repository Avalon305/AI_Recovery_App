package com.bdl.airecovery.netty.listener;


import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.biz.LoginBiz;
import com.bdl.airecovery.biz.LoginUtils;
import com.bdl.airecovery.entity.TempStorage;
import com.bdl.airecovery.entity.login.User;
import com.bdl.airecovery.proto.BdlProto;
import com.google.gson.Gson;

import org.apache.log4j.Logger;
import org.xutils.DbManager;
import org.xutils.common.util.LogUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class DataSocketListener extends ChannelInboundHandlerAdapter {
	private DbManager dbManager = MyApplication.getInstance() == null ? null : MyApplication.getInstance().getDbManager();
	private Gson gsonUtil = new Gson();
	private static final Logger logger = Logger.getLogger(DataSocketListener.class);

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {

	}

	//注意，不允许在此类上用Android.utils.log里的Log，使用logger即可。
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		BdlProto.Message message = (BdlProto.Message) msg;
		//Log.e("BtService","channelRead");
		if (message.hasLoginResponse()) {

			BdlProto.LoginResponse resp = message.getLoginResponse();
			logger.info("DataSocket客户端收到登陆结果响应:"+gsonUtil.toJson(resp).toString());
           //为全局User赋值
			User loginUser = new User(message);
			MyApplication.getInstance().setUser(loginUser);
				//TODO 在这里处理获取到的登陆信息
			LogUtil.e("收到教练机反馈-开始");
			//根据反馈情况进行设置
			LoginUtils.parseLoginRespMsg(message);
			//有了登录请求的反馈了，最后解锁
			LoginBiz.getInstance().COUNT_DOWN_LATCH.countDown();
			LogUtil.e("收到教练机反馈-结束");
		}
		if(message.hasKeepaliveResponse()){
			BdlProto.KeepaliveResponse resp = message.getKeepaliveResponse();
			logger.info("DataSocket客户端收到上传训练结果的响应:"+gsonUtil.toJson(resp).toString());
		}
		if (message.hasUploadResponse()){
			//logger.info("DataSocket客户端收到上传训练结果的响应:"+message);
			//TODO 在这里处理上传结果的业务
			BdlProto.UploadResponse resp = message.getUploadResponse();
			if (message.getUploadResponse().getSuccess() == true){
				logger.info("DataSocket客户端收到上传训练结果的响应:"+gsonUtil.toJson(resp).toString());
				//Log.d("重传service-upload",gsonUtil.toJson(resp).toString());
				if (dbManager!=null) {
					dbManager.deleteById(TempStorage.class,Integer.parseInt(message.getUploadResponse().getUid()));
				}
			}
		}
		if (message.hasPersonalSetResponse()){
			//logger.info("DataSocket客户端收到更新医护设置的响应："+message);
			//TODO 在这里处理更新医护设置后的业务
			BdlProto.PersonalSetResponse resp = message.getPersonalSetResponse();
			if (resp.getSuccess() == true){
				//Log.d("重传service-set",gsonUtil.toJson(resp).toString());
				logger.info("DataSocket客户端收到更新医护设置的响应："+gsonUtil.toJson(resp).toString());
				if (dbManager!=null) {
					dbManager.deleteById(TempStorage.class,Integer.parseInt(resp.getUid()));
				}
			}
		}
		if(message.hasMuscleStrengthResponse()){
			BdlProto.MuscleStrengthResponse resp = message.getMuscleStrengthResponse();

			if(resp.getSuccess()==true){
				logger.info("DataSocket客户端收到上传肌力测试的响应："+gsonUtil.toJson(resp).toString());

				if (dbManager!=null) {
					dbManager.deleteById(TempStorage.class,Integer.parseInt(resp.getUid()));
				}
			}

		}
		if(message.hasErrorInfoResponse()){
			BdlProto.ErrorInfoResponse resp = message.getErrorInfoResponse();

			if(resp.getSuccess()==true){
				logger.info("DataSocket客户端收到错误信息上传的响应："+gsonUtil.toJson(resp).toString());

				if (dbManager!=null) {
					dbManager.deleteById(TempStorage.class,Integer.parseInt(resp.getUid()));
				}
			}
		}
		//ctx.close(); 不要关闭通道，保持长连接

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		//有了登录请求的反馈,虽然是异常，也要解锁
		LoginBiz.getInstance().COUNT_DOWN_LATCH.countDown();

		logger.error("SocketClient捕获到全局异常");
		logger.error(cause.getMessage(), cause);
		cause.printStackTrace();
		ctx.close();
	}

}
