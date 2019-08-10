package com.bdl.aisports.util;

public class CodecUtils {
    /**
     * byte数组转以逗号分割的十六进制字符串
     *
     * @param bytes
     * @return
     */
    public static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() < 2) {
                sb.append(0);
            }
            if (i == bytes.length - 1) {
                sb.append(hex);
            } else {
                sb.append(hex).append(",");
            }
        }
        return sb.toString();
    }

    /**
     * 以逗号分割的十六进制字符串转byte数组
     *
     * @param hexString
     * @return
     */
    public static byte[] hexStringToBytes(String hexString) {
        hexString = hexString.toLowerCase();
        String[] hexStrings = hexString.split(",");
        byte[] bytes = new byte[hexStrings.length];
        for (int i = 0; i < hexStrings.length; i++) {
            char[] hexChars = hexStrings[i].toCharArray();
            bytes[i] = (byte) (charToByte(hexChars[0]) << 4 | charToByte(hexChars[1]));
        }
        return bytes;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789abcdef".indexOf(c);
    }

    /**
     * 十六进制字符串转十进制数
     *
     * @param hexString
     * @return
     */
    public static int stringHexToDecimal(String hexString) {
        return Integer.parseInt(hexString.replaceAll(",", ""), 16);
    }

    /**
     * 十进制数转倒序十六进制字符串
     *
     * @param decimalNumber
     * @return
     */
    public static String decimalToHexString(int decimalNumber) {
        String a = String.format("%08x", decimalNumber); //转换为16进制并补零
        StringBuilder sb = new StringBuilder(); //字符串用逗号分割
        for (int i = a.length() / 2 - 1; i >= 0; i--) {
            if (i != 0) {
                sb.append(a.substring(2 * i, 2 * i + 2)).append(",");
            } else {
                sb.append(a.substring(2 * i, 2 * i + 2));
            }
        }
        return String.valueOf(sb);
    }

    /**
     * 十进制数转以逗号分割的倒序byte数组
     *
     * @param decimalNumber
     * @return
     */
    public static byte[] decimalToBytes(int decimalNumber) {
        return hexStringToBytes(decimalToHexString(decimalNumber));
    }

    /**
     * 十六进制字符串转16位二进制字符串
     * @param hexString
     * @return
     */
    public static String hexString2binaryString(String hexString) {
        if (hexString == null || hexString.length() % 2 != 0)
            return null;
        String bString = "", tmp;
        for (int i = 0; i < hexString.length(); i++) {
            tmp = "0000" + Integer.toBinaryString(Integer.parseInt(hexString.substring(i, i + 1), 16));
            bString += tmp.substring(tmp.length() - 4);
        }
        return bString;
    }

    /**
     * 计算异或校验位
     *
     * @param datas
     * @return byte
     */
    public static byte getXor(byte[] datas){

        byte temp=datas[0];

        for (int i = 1; i <datas.length; i++) {
            temp ^=datas[i];
        }

        return temp;
    }


    /**
     * 传入校验的字符串
     * 返回原字符串转数组+2位校验码的byte[]数组
     * 获取源数据和验证码的组合byte数组
     *
     * @param strings 可变长度的十六进制字符串
     * @return
     */
    public static byte[] appendCrc16(String... strings) {
        byte[] data = new byte[]{};
        for (int i = 0; i < strings.length; i++) {
            int x = Integer.parseInt(strings[i], 16);
            byte n = (byte) x;
            byte[] buffer = new byte[data.length + 1];
            byte[] aa = {n};
            System.arraycopy(data, 0, buffer, 0, data.length);
            System.arraycopy(aa, 0, buffer, data.length, aa.length);
            data = buffer;
        }
        return appendCrc16(data);
    }

    /**
     * 传入校验的字节数组
     * 返回原数组+2位校验码的byte[]数组
     * 获取源数据和验证码的组合byte数组
     *
     * @param aa 字节数组
     * @return
     */
    public static byte[] appendCrc16(byte[] aa) {
        byte[] bb = getCrc16(aa);
        byte[] cc = new byte[aa.length + bb.length];
        System.arraycopy(aa, 0, cc, 0, aa.length);
        System.arraycopy(bb, 0, cc, aa.length, bb.length);
        return cc;
    }

    /**
     * 传入校验的字节数组
     * 返回2位byte[]校验码
     * 获取验证码byte数组，基于Modbus CRC16的校验算法
     */
    public static byte[] getCrc16(byte[] arr_buff) {
        int len = arr_buff.length;

        // 预置 1 个 16 位的寄存器为十六进制FFFF, 称此寄存器为 CRC寄存器。
        int crc = 0xFFFF;
        int i, j;
        for (i = 0; i < len; i++) {
            // 把第一个 8 位二进制数据 与 16 位的 CRC寄存器的低 8 位相异或, 把结果放于 CRC寄存器
            crc = ((crc & 0xFF00) | (crc & 0x00FF) ^ (arr_buff[i] & 0xFF));
            for (j = 0; j < 8; j++) {
                // 把 CRC 寄存器的内容右移一位( 朝低位)用 0 填补最高位, 并检查右移后的移出位
                if ((crc & 0x0001) > 0) {
                    // 如果移出位为 1, CRC寄存器与多项式A001进行异或
                    crc = crc >> 1;
                    crc = crc ^ 0xA001;
                } else
                    // 如果移出位为 0,再次右移一位
                    crc = crc >> 1;
            }
        }
        return intToBytes(crc);
    }

    /**
     * 将int转换成byte数组，低位在前，高位在后
     * 改变高低位顺序只需调换数组序号
     */
    private static byte[] intToBytes(int value) {
        byte[] src = new byte[2];
        src[0] = (byte) ((value >> 8) & 0xFF);
        src[1] = (byte) (value & 0xFF);
        return src;
    }

}
