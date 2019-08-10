package com.zero.serialport.framework.callback;

/**
  * @explain  用于读取到串口的回调类
  * @author zero.
  * @creat time 2019/4/14 7:05 PM.
  */

public interface ZeroCallback<T> {
    /**
      * @explain  接收到方法的回调，把数据块返回过来。
      * @author zero.
      * @creat time 2019/4/14 7:07 PM.
      */

    public void receive(T data);
}
