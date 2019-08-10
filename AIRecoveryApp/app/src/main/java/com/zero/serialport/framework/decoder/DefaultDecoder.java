package com.zero.serialport.framework.decoder;

import android.util.Log;

import com.zero.serialport.framework.callback.ZeroCallback;
import com.zero.serialport.framework.data.ZeroData;

import org.xutils.common.util.LogUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Random;

/**
 * @explain  默认解码器，贡献人赵曰艺,使用默认的数据对象,专门用于发卡器
 * @author zero.
 * @creat time 2019/4/14 7:52 PM.
 */

public class DefaultDecoder implements ZeroDecoder<ZeroData>
{

    private static final byte HEAD = (byte) 0xAA;  //发卡器协议头（用于校验）
    private static final byte READ = 0x02;         //发卡器协议读卡命令字（用于校验
    private static final byte LEN = (byte) 0x0E;  //body长度
    private static final byte TILL = (byte) 0xCC;  //发卡器协议尾（用于校验）

    @Override
    public void resolveInputstream(InputStream inputStream, ZeroCallback<ZeroData> callback)  {

        //头部首字节
        try {
            byte[] First = new byte[1];
            try {
                inputStream.read(First);
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] buffer = new byte[19];
            //针对读到到第一个字节到情况进行补齐
            if (First[0] == HEAD){
                LogUtil.d("First[0] == HEAD");
                buffer[0] = HEAD;
                try {
                    int size = inputStream.read(buffer,1,18);
                    if(size!=18){
                        LogUtil.d("此一次读 size= " + size);
                        //没读全，睡100s继续读
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        int moreSize;
                        //LogUtil.d("size2 = inputStream.read(buffer, size, 64 - size);");
                        moreSize = inputStream.read(buffer, size+1, 18 - size);
                        //继续读
                        if (moreSize > 0) {
                            size += moreSize;
                            LogUtil.d("第二次读 size= " + moreSize);
                        }
                    }
                    LogUtil.d("总共 size= " + size);
                    transferToObj(buffer,size,callback);
                    LogUtil.d("content = "+bytesToHexFun(buffer));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else if (First[0] == READ){
                LogUtil.d("First[0] == READ");
                buffer[0] = HEAD;
                buffer[1] = READ;
                try {
                    int size = inputStream.read(buffer,2,18);
                    if(size!=17){
                        LogUtil.d("此一次读 size= " + size);
                        //没读全，睡100s继续读
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        int moreSize;
                        LogUtil.d("size2 = inputStream.read(buffer, size, 64 - size);");
                        moreSize = inputStream.read(buffer, size+1, 18 - size);
                        //继续读
                        if (moreSize > 0) {
                            size += moreSize;
                            LogUtil.d("此二次读 size= " + moreSize);
                        }
                    }
                    LogUtil.d("总共 size= " + size);
                    transferToObj(buffer,size,callback);
                    LogUtil.d("transferToObj(buffer, size,callback);");
                    LogUtil.d("content = "+bytesToHexFun(buffer));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else if(First[0] == LEN){
                LogUtil.d("First[0] == LEN");
                buffer[0] = HEAD;
                buffer[1] = READ;
                buffer[1] = LEN;
                try {
                    int size = inputStream.read(buffer,3,18);
                    if(size!=16){
                        LogUtil.d("此一次读 size= " + size);
                        //没读全，睡100s继续读
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        int moreSize;
                        LogUtil.d("size2 = inputStream.read(buffer, size, 64 - size);");
                        moreSize = inputStream.read(buffer, size+1, 18 - size);
                        //继续读
                        if (moreSize > 0) {
                            LogUtil.d("此二次读 size= " + moreSize);
                            size += moreSize;
                        }
                    }
                    LogUtil.d("总共 size= " + size);
                    transferToObj(buffer,size,callback);
                    LogUtil.d("transferToObj(buffer, size,callback);");
                    LogUtil.d("content = "+bytesToHexFun(buffer));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
      * @explain  将合法的数据，转化为对象
      * @author zero.
      * @creat time 2019/6/18 6:08 PM.
      */

    private void transferToObj(byte[] buffer, int size, ZeroCallback<ZeroData> callback) {
        boolean valid = checkVaild(buffer,size);
        if (valid){
            //校验通过，进行解析,去空格。
            String result = AnalyzeData(buffer).trim();
            LogUtil.d("result"+result);
            if (result != null){
                callback.receive(new ZeroData(1,result));
            }else {
                LogUtil.d("不符合解码规则的数据！");
            }
        }else{
            LogUtil.d("异或校验不通过的数据！");
        }
    }

    /**
     * 输入一个值，异或校验最后一位
     * @param datas
     * @return
     */
    public boolean getXor(byte[] datas){

        byte temp=datas[1];

        for (int i = 2; i <datas.length-2; i++) {
            temp ^=datas[i];
        }

        return temp == datas[datas.length-2]?true:false;
    }
    /**
      * @explain  异或校验的合法性
      * @author zero.
      * @creat time 2019/6/18 6:07 PM.
      */

    private boolean checkVaild(byte[] buffer, int size) {
        boolean checkResult = getXor(buffer);
        return checkResult;
    }
    /**
      * @explain  分析返回数据
      * @author zero.
      * @creat time 2019/6/18 6:07 PM.
      */

    private String AnalyzeData(byte[] resp){
        String result = null;

        byte respCmd = resp[1];                //返回命令字
        int respLen = resp[2];                 //返回包长度
        byte[] respName = new byte[10];        //返回姓名
        byte[] respPhone = new byte[4];        //返回电话
        LogUtil.d("respLen:" + respLen + "   respCmd:" + respCmd);
        if (respLen == 14 && respCmd == READ) {
            for (int i = 3; i < respLen + 3; i++) {
                if (i < 13) {
                    respName[i - 3] = resp[i];
                } else if (i < 17) {
                    respPhone[i - 13] = resp[i];
                }
            }
            LogUtil.d("name: "+ Arrays.toString(respName)+"  phone:"+Arrays.toString(respPhone));
            String loginName = "";
            try {
                String name = new String(respName, "GBK").replace("\000","").trim(); //GBK解码&删除空字节
                String phone = new String(respPhone, "ASCII").trim();                                   //ASCII解码
                loginName = name + phone;//UID                                                              //拼接uid
                LogUtil.d("strname: "+name+"  strphone:"+phone);
                LogUtil.d("read service data: "+loginName);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String regex = "\\s*+[\u4e00-\u9fa5]{2,3}+\\d{4}";
            if (loginName.matches(regex)) {//正则表达式校验通过时 进行登陆
                result = loginName;
            } else {
                //不符合规则，置为空返回
                result = null;
            }
        } else {
            //不符合解析规则，置为空返回
            result = null;
        }
        LogUtil.d("读到的数据："+result);
        return result;
    }

    private static final char[] HEX_CHAR = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String bytesToHexFun(byte[] bytes) {
        StringBuilder buf = null;
        try {
            buf = new StringBuilder(bytes.length * 2);
            for(byte b : bytes) { // 使用String的format方法进行转换
                buf.append(String.format("%02x", new Integer(b & 0xff)));
            }
            //加空格
            String regex = "(.{2})";
            return buf.toString().replaceAll(regex, "$1 ");
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

}
