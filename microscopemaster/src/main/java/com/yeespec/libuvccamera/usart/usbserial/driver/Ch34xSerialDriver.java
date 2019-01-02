/* Copyright 2014 Andreas Butti
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.yeespec.libuvccamera.usart.usbserial.driver;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Driver for CH340, maybe also working with CH341, but not tested
 * See http://wch-ic.com/product/usb/ch340.asp
 *
 * @author Andreas Butti
 */
public class Ch34xSerialDriver implements UsbSerialDriver {

    private static final String TAG = Ch34xSerialDriver.class.getSimpleName();

    private final UsbDevice mDevice;
    private final UsbSerialPort mPort;

    public Ch34xSerialDriver(UsbDevice device) {
        mDevice = device;
        mPort = new Ch340SerialPort(mDevice, 0);
    }

    @Override
    public UsbDevice getDevice() {
        return mDevice;
    }

    @Override
    public List<UsbSerialPort> getPorts() {
        return Collections.singletonList(mPort);
    }

    public class Ch340SerialPort extends CommonUsbSerialPort {

        private static final int USB_TIMEOUT_MILLIS = 500;

        private final int DEFAULT_BAUD_RATE = 9600;

        private boolean dtr = false;
        private boolean rts = false;

        private UsbEndpoint mReadEndpoint;
        private UsbEndpoint mWriteEndpoint;

        public Ch340SerialPort(UsbDevice device, int portNumber) {
            super(device, portNumber);
        }

        @Override
        public UsbSerialDriver getDriver() {
            return Ch34xSerialDriver.this;
        }

        @Override
        public void open(UsbDeviceConnection connection) throws IOException {
            if (mConnection != null) {
                throw new IOException("Already opened.");
            }

            mConnection = connection;
            boolean opened = false;
            try {
                for (int i = 0; i < mDevice.getInterfaceCount(); i++) {
                    UsbInterface usbIface = mDevice.getInterface(i);
                    if (mConnection.claimInterface(usbIface, true)) {
                        Log.d(TAG, "claimInterface " + i + " SUCCESS");
                    } else {
                        Log.d(TAG, "claimInterface " + i + " FAIL");
                    }
                }

                UsbInterface dataIface = mDevice.getInterface(mDevice.getInterfaceCount() - 1);
                for (int i = 0; i < dataIface.getEndpointCount(); i++) {
                    UsbEndpoint ep = dataIface.getEndpoint(i);
                    if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                            mReadEndpoint = ep;
                        } else {
                            mWriteEndpoint = ep;
                        }
                    }
                }

                initialize();
                //                setBaudRate(DEFAULT_BAUD_RATE);
                //                setParameters(9600, 8, 1, 0);

                opened = true;

                //                Toast.makeText(mContext, "Device Has Attached to Android", Toast.LENGTH_LONG).show();
                if (READ_ENABLE == false) {
                    READ_ENABLE = true;
                    readThread = new read_thread(mReadEndpoint, mConnection);
                    readThread.start();
                }

            } finally {
                if (!opened) {
                    try {
                        close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        // Ignore IOExceptions during close()
                    }
                }
            }
        }

        @Override
        public void close() throws IOException {

            if (READ_ENABLE == true) {
                READ_ENABLE = false;
            }

            if (mConnection == null) {
                throw new IOException("Already closed");
            }

            // TODO: nothing sended on close, maybe needed?

            try {
                mConnection.close();
            } finally {
                mConnection = null;
            }
        }


        @Override
        public int read(byte[] dest, int timeoutMillis) throws IOException {
            final int numBytesRead;
            ReadTimeOutMillis = timeoutMillis;
            int length = 64;
            /*should be at least one byte to read*/
            if ((length < 1) || (totalBytes == 0)) {
                numBytesRead = 0;
                //                Log.w("CH34x", "read() 1 numBytesRead  " + numBytesRead + " === ");
                return numBytesRead;
            }
                    /*check for max limit*/
            if (length > totalBytes)
                length = totalBytes;

            /*update the number of bytes available*/
            totalBytes -= length;

            numBytesRead = length;

            /*copy to the user buffer*/
            for (int count = 0; count < length; count++) {
                dest[count] = mReadBuffer[readIndex];
                readIndex++;
            /*shouldnt read more than what is there in the buffer,
             * 	so no need to check the overflow
             */
                readIndex %= mReadBuffer.length;
            }

           /* synchronized (mReadBufferLock) {
                int readAmt = Math.min(dest.length, mReadBuffer.length);
                numBytesRead = mConnection.bulkTransfer(mReadEndpoint, mReadBuffer, readAmt,
                        timeoutMillis);
                if (numBytesRead < 0) {
                    // This sucks: we get -1 on timeout, not 0 as preferred.
                    // We *should* use UsbRequest, except it has a bug/api oversight
                    // where there is no way to determine the number of bytes read
                    // in response :\ -- http://b.android.com/28023
                    return 0;
                }
                System.arraycopy(mReadBuffer, 0, dest, 0, numBytesRead);
            }*/

//            Log.w("CH34x", "read() 2 numBytesRead " + numBytesRead + " === ");
            return numBytesRead;
        }

        //====================================================
        //        private byte[] readBuffer; /*circular buffer //循环缓冲区 */
        private byte[] usbdata = new byte[1024];
        private int writeIndex = 0;
        private int readIndex = 0;
        private int readcount;
        private int totalBytes;
        //        final int maxnumbytes = 65536;
        public boolean READ_ENABLE = false;
        public int ReadTimeOutMillis = 10000;
        public read_thread readThread;


        /*usb input data handler //USB输入数据处理 */
        private class read_thread extends Thread {
            UsbEndpoint endpoint;
            UsbDeviceConnection mConn;

            read_thread(UsbEndpoint point, UsbDeviceConnection con) {
                endpoint = point;
                mConn = con;
                this.setPriority(Thread.MAX_PRIORITY);
            }

            public void run() {
                while (READ_ENABLE == true) {
                    while (totalBytes > (mReadBuffer.length - 63)) {    //65536 - 63
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    synchronized (mReadBufferLock) {
                        if (endpoint != null) {
                            //  readcount = mConn.bulkTransfer(endpoint, mReadBuffer, 4096, ReadTimeOutMillis);
                            readcount = mConn.bulkTransfer(endpoint, usbdata, 64, ReadTimeOutMillis);
                            //           Log.w("CH34x", "read_thread#run()  readcount " + readcount + " === ");
                            if (readcount > 0) {
                                for (int count = 0; count < readcount; count++) {
                                    mReadBuffer[writeIndex] = usbdata[count];
                                    writeIndex++;
                                    writeIndex %= mReadBuffer.length;
                                }

                                if (writeIndex >= readIndex)
                                    totalBytes = writeIndex - readIndex;
                                else
                                    totalBytes = (mReadBuffer.length - readIndex) + writeIndex;
                            }
                        }
                    }
                }
            }
        }

        @Override
        public int write(byte[] src, int timeoutMillis) throws IOException {

            int offset = 0;
            int HasWritten = 0;
            int length = src.length;
            int odd_len = length;
            if (mWriteEndpoint == null)
                return -1;
            while (offset < length) {
                synchronized (mWriteBufferLock) {
                    int mLen = Math.min(odd_len, mWriteEndpoint.getMaxPacketSize());
                    byte[] arrayOfByte = new byte[mLen];
                    if (offset == 0) {
                        System.arraycopy(src, 0, arrayOfByte, 0, mLen);
                    } else {
                        System.arraycopy(src, offset, arrayOfByte, 0, mLen);
                    }
                    HasWritten = mConnection.bulkTransfer(mWriteEndpoint, arrayOfByte, mLen, timeoutMillis);
                    if (HasWritten < 0) {
                        return -2;
                    } else {
                        offset += HasWritten;
                        odd_len -= HasWritten;
                        Log.d(TAG, "offset " + offset + " odd_len " + odd_len);
                    }
                }
            }
            return offset;

          /*  int offset = 0;
            while (offset < src.length) {
                final int writeLength;
                final int amtWritten;
                synchronized (mWriteBufferLock) {
                    final byte[] writeBuffer;
                    writeLength = Math.min(src.length - offset, mWriteBuffer.length);
                    if (offset == 0) {
                        writeBuffer = src;
                    } else {
                        // bulkTransfer does not support offsets, make a copy.
                        System.arraycopy(src, offset, mWriteBuffer, 0, writeLength);
                        writeBuffer = mWriteBuffer;
                    }
                    amtWritten = mConnection.bulkTransfer(mWriteEndpoint, writeBuffer, writeLength,
                            timeoutMillis);
                }
                if (amtWritten <= 0) {
                    throw new IOException("Error writing " + writeLength
                            + " bytes at offset " + offset + " length=" + src.length);
                }
                Log.d(TAG, "Wrote amt=" + amtWritten + " attempted=" + writeLength);
                offset += amtWritten;
            }
            return offset;*/
        }

        private int controlOut(int request, int value, int index) {
            final int REQTYPE_HOST_TO_DEVICE = UsbConstants.USB_TYPE_VENDOR | 0 | UsbConstants.USB_DIR_OUT;
            //            final int REQTYPE_HOST_TO_DEVICE = 0x41;
            return mConnection.controlTransfer(REQTYPE_HOST_TO_DEVICE, request,
                    value, index, null, 0, USB_TIMEOUT_MILLIS);
        }


        private int controlIn(int request, int value, int index, byte[] buffer) {
            //            final int REQTYPE_HOST_TO_DEVICE = UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_DIR_IN;
            final int REQTYPE_HOST_TO_DEVICE = UsbConstants.USB_TYPE_VENDOR | 0 | UsbConstants.USB_DIR_IN;
            return mConnection.controlTransfer(REQTYPE_HOST_TO_DEVICE, request,
                    value, index, buffer, buffer.length, USB_TIMEOUT_MILLIS);
        }


        private void checkState(String msg, int request, int value, int[] expected) throws IOException {
            byte[] buffer = new byte[expected.length];
            int ret = controlIn(request, value, 0, buffer);

            if (ret < 0) {
                throw new IOException("Faild send cmd [" + msg + "]");
            }

            if (ret != expected.length) {
                throw new IOException("Expected " + expected.length + " bytes, but get " + ret + " [" + msg + "]");
            }

            for (int i = 0; i < expected.length; i++) {
                if (expected[i] == -1) {
                    continue;
                }

                int current = buffer[i] & 0xff;
                if (expected[i] != current) {
                    Log.e("CH34x", "====================================================\n" +
                            "Expected 0x" + Integer.toHexString(expected[i]) + " bytes, but get 0x" + Integer.toHexString(current) + " [" + msg + "]");
                    //					throw new IOException("Expected 0x" + Integer.toHexString(expected[i]) + " bytes, but get 0x" + Integer.toHexString(current) + " [" + msg + "]");
                }
            }
        }

        private void writeHandshakeByte() throws IOException {
            if (controlOut(UartCmd.VENDOR_MODEM_OUT, ~((dtr ? 1 << 5 : 0) | (rts ? 1 << 6 : 0)), 0) < 0) {
                throw new IOException("Faild to set handshake byte");
            }
        }

        private void initialize() throws IOException {

            int ret;
            //            int size = 8;
            int size = 2;
            byte[] buffer = new byte[size];
            controlOut(UartCmd.VENDOR_SERIAL_INIT, 0, 0);
            ret = controlIn(UartCmd.VENDOR_VERSION, 0, 0, buffer);
            if (ret < 0) {
                Log.w(TAG, "UartInit 1 ret = " + ret);
                throw new IOException("UartInit 1 ret = " + ret);
            }
            controlOut(UartCmd.VENDOR_WRITE, 4882, 55682);
            controlOut(UartCmd.VENDOR_WRITE, 3884, 4);
            ret = controlIn(UartCmd.VENDOR_READ, 9496, 0, buffer);
            if (ret < 0) {
                Log.w(TAG, "UartInit 2 ret = " + ret);
                throw new IOException("UartInit 2 ret = " + ret);
            }
            controlOut(UartCmd.VENDOR_WRITE, 10023, 0);
            controlOut(UartCmd.VENDOR_MODEM_OUT, 255, 0);

            /*
            checkState("init #1", UartCmd.VENDOR_VERSION, 0, new int[]{-1  0x27, 0x30 , 0x00});
            if (controlOut(UartCmd.VENDOR_SERIAL_INIT, 0, 0) < 0)
                throw new IOException("init failed! #2");
            //            setBaudRate(DEFAULT_BAUD_RATE);
            checkState("init #4", UartCmd.VENDOR_READ, 9496, new int[]{-1  0x56, c3, 0x00});
            if (controlOut(UartCmd.VENDOR_WRITE, 9496, 80) < 0)
                throw new IOException("init failed! #5");
            checkState("init #6", UartCmd.VENDOR_READ, 1798, new int[]{0xff, 0xee});
            if (controlOut(UartCmd.VENDOR_SERIAL_INIT, 20511, 55562) < 0)
                throw new IOException("init failed! #7");
            //            setBaudRate(DEFAULT_BAUD_RATE);
            writeHandshakeByte();
            checkState("init #10", UartCmd.VENDOR_READ, 1798, new int[]{-1 0x9f, 0xff, 0xee});
*/
        }


/*
        private void setBaudRate(int baudRate) throws IOException {
            int[] baud = new int[]{2400, 0xd901, 0x0038, 4800, 0x6402,
                    0x001f, 9600, 0xb202, 0x0013, 19200, 0xd902, 0x000d, 38400,
                    0x6403, 0x000a, 115200, 0xcc03, 0x0008};

            for (int i = 0; i < baud.length / 3; i++) {
                if (baud[i * 3] == baudRate) {
                    int ret = controlOut(UartCmd.VENDOR_WRITE, 0x1312, baud[i * 3 + 1]);
                    if (ret < 0) {
                        throw new IOException("Error setting baud rate. #1");
                    }
                    ret = controlOut(UartCmd.VENDOR_WRITE, 0x0f2c, baud[i * 3 + 2]);
                    if (ret < 0) {
                        throw new IOException("Error setting baud rate. #1");
                    }

                    return;
                }
            }


            throw new IOException("Baud rate " + baudRate + " currently not supported");
        }
*/

        private int mBaudRate = -1, mDataBits = -1, mStopBits = -1, mParity = -1;

        @Override
        public void setParameters(int baudRate, int dataBits, int stopBits, int parity, int flowControl) throws IOException {

            //            setBaudRate(baudRate);
            // TODO databit, stopbit and paraty set not implemented

            if ((mBaudRate == baudRate) && (mDataBits == dataBits)
                    && (mStopBits == stopBits) && (mParity == parity)) {
                // Make sure no action is performed if there is nothing to change
                return;
            }

            int value = 0;
            int index = 0;
            char valueHigh = 0, valueLow = 0, indexHigh = 0, indexLow = 0;
            switch (parity) {
                case 0:	/*NONE*/
                    valueHigh = 0x00;
                    break;
                case 1:	/*ODD*/
                    valueHigh |= 0x08;
                    break;
                case 2:	/*Even*/
                    valueHigh |= 0x18;
                    break;
                case 3:	/*Mark*/
                    valueHigh |= 0x28;
                    break;
                case 4:	/*Space*/
                    valueHigh |= 0x38;
                    break;
                default:	/*None*/
                    valueHigh = 0x00;
                    break;
            }

            if (stopBits == 2) {
                valueHigh |= 0x04;
            }

            switch (dataBits) {
                case 5:
                    valueHigh |= 0x00;
                    break;
                case 6:
                    valueHigh |= 0x01;
                    break;
                case 7:
                    valueHigh |= 0x02;
                    break;
                case 8:
                    valueHigh |= 0x03;
                    break;
                default:
                    valueHigh |= 0x03;
                    break;
            }

            valueHigh |= 0xc0;
            valueLow = 0x9c;

            value |= valueLow;
            value |= (int) (valueHigh << 8);

            switch (baudRate) {
                case 50:
                    indexLow = 0;
                    indexHigh = 0x16;
                    break;
                case 75:
                    indexLow = 0;
                    indexHigh = 0x64;
                    break;
                case 110:
                    indexLow = 0;
                    indexHigh = 0x96;
                    break;
                case 135:
                    indexLow = 0;
                    indexHigh = 0xa9;
                    break;
                case 150:
                    indexLow = 0;
                    indexHigh = 0xb2;
                    break;
                case 300:
                    indexLow = 0;
                    indexHigh = 0xd9;
                    break;
                case 600:
                    indexLow = 1;
                    indexHigh = 0x64;
                    break;
                case 1200:
                    indexLow = 1;
                    indexHigh = 0xb2;
                    break;
                case 1800:
                    indexLow = 1;
                    indexHigh = 0xcc;
                    break;
                case 2400:
                    indexLow = 1;
                    indexHigh = 0xd9;
                    break;
                case 4800:
                    indexLow = 2;
                    indexHigh = 0x64;
                    break;
                case 9600:
                    indexLow = 2;
                    indexHigh = 0xb2;
                    break;
                case 19200:
                    indexLow = 2;
                    indexHigh = 0xd9;
                    break;
                case 38400:
                    indexLow = 3;
                    indexHigh = 0x64;
                    break;
                case 57600:
                    indexLow = 3;
                    indexHigh = 0x98;
                    break;
                case 115200:
                    indexLow = 3;
                    indexHigh = 0xcc;
                    break;
                case 230400:
                    indexLow = 3;
                    indexHigh = 0xe6;
                    break;
                case 460800:
                    indexLow = 3;
                    indexHigh = 0xf3;
                    break;
                case 500000:
                    indexLow = 3;
                    indexHigh = 0xf4;
                    break;
                case 921600:
                    indexLow = 7;
                    indexHigh = 0xf3;
                    break;
                case 1000000:
                    indexLow = 3;
                    indexHigh = 0xfa;
                    break;
                case 2000000:
                    indexLow = 3;
                    indexHigh = 0xfd;
                    break;
                case 3000000:
                    indexLow = 3;
                    indexHigh = 0xfe;
                    break;
                default:    // default baudRate "9600"
                    indexLow = 2;
                    indexHigh = 0xb2;
                    break;
            }

            index |= 0x88 | indexLow;
            index |= (int) (indexHigh << 8);

            controlOut(UartCmd.VENDOR_SERIAL_INIT, value, index);
            if (flowControl == 1) {
                Uart_Tiocmset(UartModem.TIOCM_DTR | UartModem.TIOCM_RTS, 0x00);
            }

            mBaudRate = baudRate;
            mDataBits = dataBits;
            mStopBits = stopBits;
            mParity = parity;
        }


        private int Uart_Set_Handshake(int control) {
            return controlOut(UartCmd.VENDOR_MODEM_OUT, ~control, 0);
        }

        public int Uart_Tiocmset(int set, int clear) {
            int control = 0;
            if ((set & UartModem.TIOCM_RTS) == UartModem.TIOCM_RTS)
                control |= UartIoBits.UART_BIT_RTS;
            if ((set & UartModem.TIOCM_DTR) == UartModem.TIOCM_DTR)
                control |= UartIoBits.UART_BIT_DTR;
            if ((clear & UartModem.TIOCM_RTS) == UartModem.TIOCM_RTS)
                control &= ~UartIoBits.UART_BIT_RTS;
            if ((clear & UartModem.TIOCM_DTR) == UartModem.TIOCM_DTR)
                control &= ~UartIoBits.UART_BIT_DTR;

            return Uart_Set_Handshake(control);
        }

        @Override
        public boolean getCD() throws IOException {
            return false;
        }

        @Override
        public boolean getCTS() throws IOException {
            return false;
        }

        @Override
        public boolean getDSR() throws IOException {
            return false;
        }

        @Override
        public boolean getDTR() throws IOException {
            return dtr;
        }

        @Override
        public void setDTR(boolean value) throws IOException {
            dtr = value;
            writeHandshakeByte();
        }

        @Override
        public boolean getRI() throws IOException {
            return false;
        }

        @Override
        public boolean getRTS() throws IOException {
            return rts;
        }

        @Override
        public void setRTS(boolean value) throws IOException {
            rts = value;
            writeHandshakeByte();
        }

        @Override
        public boolean purgeHwBuffers(boolean purgeReadBuffers, boolean purgeWriteBuffers) throws IOException {
            return true;
        }

    }

    public static Map<Integer, int[]> getSupportedDevices() {
        final Map<Integer, int[]> supportedDevices = new LinkedHashMap<Integer, int[]>();
        supportedDevices.put(UsbId.VENDOR_QINHENG, new int[]{
                UsbId.QINHENG_HL340
        });
        return supportedDevices;
    }

    public final class UartCmd {
        public static final int VENDOR_WRITE_TYPE = 64;
        public static final int VENDOR_READ_TYPE = 192;
        public static final int VENDOR_READ = 149;
        public static final int VENDOR_WRITE = 154;
        public static final int VENDOR_SERIAL_INIT = 161;
        public static final int VENDOR_MODEM_OUT = 164;
        public static final int VENDOR_VERSION = 95;
    }

    public final class UartModem {
        public static final int TIOCM_LE = 1;
        public static final int TIOCM_DTR = 2;
        public static final int TIOCM_RTS = 4;
        public static final int TIOCM_ST = 8;
        public static final int TIOCM_SR = 16;
        public static final int TIOCM_CTS = 32;
        public static final int TIOCM_CAR = 64;
        public static final int TIOCM_RNG = 128;
        public static final int TIOCM_DSR = 256;
        public static final int TIOCM_CD = TIOCM_CAR;
        public static final int TIOCM_RI = TIOCM_RNG;
        public static final int TIOCM_OUT1 = 8192;
        public static final int TIOCM_OUT2 = 16384;
        public static final int TIOCM_LOOP = 32768;
    }

    public final class UartIoBits {
        public static final int UART_BIT_RTS = (1 << 6);
        public static final int UART_BIT_DTR = (1 << 5);
    }

}