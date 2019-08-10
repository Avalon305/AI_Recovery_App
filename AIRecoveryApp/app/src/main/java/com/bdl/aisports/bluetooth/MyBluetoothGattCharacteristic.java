package com.bdl.aisports.bluetooth;



public class MyBluetoothGattCharacteristic {

    private byte[] mValue;

    public MyBluetoothGattCharacteristic(byte[] mValue) {
        this.mValue = mValue;
    }

    public static final int FORMAT_UINT8 = 0x11;

    /**
     * Characteristic value format type uint16
     */
    public static final int FORMAT_UINT16 = 0x12;

    /**
     * Characteristic value format type uint32
     */
    public static final int FORMAT_UINT32 = 0x14;

    /**
     * Characteristic value format type sint8
     */
    public static final int FORMAT_SINT8 = 0x21;

    /**
     * Characteristic value format type sint16
     */
    public static final int FORMAT_SINT16 = 0x22;

    /**
     * Characteristic value format type sint32
     */
    public static final int FORMAT_SINT32 = 0x24;


    private int getTypeLen(int formatType) {
        return formatType & 0xF;
    }
    private int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }
    /**
     * Convert signed bytes to a 16-bit unsigned int.
     */
    private int unsignedBytesToInt(byte b0, byte b1) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8));
    }
    private int unsignedToSigned(int unsigned, int size) {
        if ((unsigned & (1 << size - 1)) != 0) {
            unsigned = -1 * ((1 << size - 1) - (unsigned & ((1 << size - 1) - 1)));
        }
        return unsigned;
    }
    private int unsignedBytesToInt(byte b0, byte b1, byte b2, byte b3) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8))
                + (unsignedByteToInt(b2) << 16) + (unsignedByteToInt(b3) << 24);
    }
    public Integer getIntValue(int formatType, int offset) {
        if ((offset + getTypeLen(formatType)) > mValue.length) return null;

        switch (formatType) {
            case FORMAT_UINT8
                    :
                return unsignedByteToInt(mValue[offset]);

            case FORMAT_UINT16:
                return unsignedBytesToInt(mValue[offset], mValue[offset + 1]);

            case FORMAT_UINT32:
                return unsignedBytesToInt(mValue[offset], mValue[offset + 1],
                        mValue[offset + 2], mValue[offset + 3]);
            case FORMAT_SINT8:
                return unsignedToSigned(unsignedByteToInt(mValue[offset]), 8);

            case FORMAT_SINT16:
                return unsignedToSigned(unsignedBytesToInt(mValue[offset],
                        mValue[offset + 1]), 16);

            case FORMAT_SINT32:
                return unsignedToSigned(unsignedBytesToInt(mValue[offset],
                        mValue[offset + 1], mValue[offset + 2], mValue[offset + 3]), 32);
        }

        return null;
    }
}
