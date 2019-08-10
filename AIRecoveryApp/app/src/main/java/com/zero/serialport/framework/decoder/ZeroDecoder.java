package com.zero.serialport.framework.decoder;

import com.zero.serialport.framework.callback.ZeroCallback;

import java.io.InputStream;

/**
  * @explain  读取串口数据的解码器。解析成功以后，触发回调.为用户根据自己的业务自定义。
  *             范型为回调的输入方法，为用户自己定义的业务对象
  * @author zero.
  * @creat time 2019/4/14 7:08 PM.
  */

public interface ZeroDecoder<T> {
    /**
      * @explain  解析方法，在此处要处理读半包。如果是希望得到的报文，则调用回调。
      * @author zero.
      * @creat time 2019/4/14 7:16 PM.
      */

    public void resolveInputstream(InputStream inputStream, ZeroCallback<T> callback);
}
