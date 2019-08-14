package com.bdl.airecovery.netty;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.entity.Setting;
import com.bdl.airecovery.netty.listener.DataSocketListener;
import com.bdl.airecovery.proto.BdlProto;
import com.bdl.airecovery.proto.DataProtoUtil;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.xutils.ex.DbException;

import java.net.ConnectException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

/**
 * 数据交互客户端
 */
public class DataSocketClient {
	private static final Logger logger = Logger.getLogger(DataSocketClient.class);
	private static DataSocketClient instance = new DataSocketClient();

	private Bootstrap b;
	private String host;
	private final int port;
	private ChannelFuture cf;
	private EventLoopGroup group;

	{
		try {
			//获取数据库中储存的教练机的IP地址
			host = MyApplication.getInstance().getDbManager().selector(Setting.class).findFirst().getCoachDeviceAddress();
		} catch (DbException e) {
			e.printStackTrace();
		}
		//host = "192.168.1.105";//TODO 替换为教练机的IP和端口
		port = 6920;
		initLoop();
	}

	public static DataSocketClient getInstance() {
		return instance;
	}

	// 私有构造器
	private DataSocketClient() {

	}

	/**
	 * 初始化线程池
	 * 
	 * @return void
	 * @author fanmingyong
	 * @date 2018年11月30日 下午4:40:47
	 * @Modify
	 */
	private void initLoop() {
		group = new NioEventLoopGroup();
		b = new Bootstrap();
		b.group(group) // 注册线程池
				.channel(NioSocketChannel.class) // 使用NioSocketChannel来作为连接用的channel类
				.option(ChannelOption.SO_BACKLOG, 1024)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,500)
				.handler(new ChannelInitializer<SocketChannel>() { // 绑定连接初始化器
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast("frameDecoder", new ProtobufVarint32FrameDecoder())
								.addLast("decoder", new ProtobufDecoder(BdlProto.Message.getDefaultInstance()))
								.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender())
								.addLast("encoder", new ProtobufEncoder()).addLast(new DataSocketListener());
					}
				});
	}

	private void connect() throws ConnectException {
		try {
			this.cf = b.connect(host, port).sync();
			//logger.debug("远程服务器已经连接, 可以进行数据交换..");
		} catch (Exception e) {
			//logger.error("远程服务器连接失败", e);
			throw new ConnectException("远程服务器连接失败");
		}

	}

	public ChannelFuture getChannelFuture() throws ConnectException {
		// 如果管道没有被开启或者被关闭了，那么重连
		if (this.cf == null) {
			this.connect();
		}
		if (!this.cf.channel().isActive()) {
			this.connect();
		}
		return this.cf;
	}

	/**
	 * 发送消息
	 * 
	 * @param msg
	 *            未加密的
	 * @return void
	 * @author fanmingyong
	 * @date 2018年11月30日 下午4:57:16
	 * @Modify
	 */
	public void sendMsg(BdlProto.Message msg) throws ConnectException {
		try{
			if (msg != null) {
				System.out.println("发送信息");
				this.getChannelFuture().channel().writeAndFlush(msg);
//			try {
//				cf.channel().closeFuture().sync();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
			}
		}catch (Exception e){
			e.printStackTrace();
			throw e;
		}




	}


	public static void main(String[] args){
		//示例同CountDownSocketClient的main函数
		//获取请求
//		BasicConfigurator.configure();
//		BdlProto.LoginRequest request =
//				BdlProto.LoginRequest.newBuilder().setDeviceType(BdlProto.DeviceType.E13)
//						.setUid("YK-488A").setActivityType(BdlProto.ActivityType.forNumber(1)).build();
//
//		System.out.println("request : " + request.toString());
//
//		//包装Message
//		BdlProto.Message message = DataProtoUtil.packLoginRequest(1,request);
//
//		System.out.println("message : " + message.toString());
//
//		try {
//			DataSocketClient.getInstance().sendMsg(message);
//		} catch (ConnectException e) {
//			e.printStackTrace();
//			System.out.println("连不上教练机");
//		}
	}
}
