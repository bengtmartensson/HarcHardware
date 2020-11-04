/*
Copyright (C) 2013, 2014, 2015, 2020 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox.harchardware.ir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.TimeoutException;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.LocalSerialPortRaw;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.IrSequence;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.OddSequenceLengthException;
import org.harctoolbox.ircore.Pronto;

/**
 * This class contains a driver for Dangerous Prototype's IrToy.
 * @see <a href="http://www.dangerousprototypes.com/docs/USB_IR_Toy:_Sampling_mode">http://www.dangerousprototypes.com/docs/USB_IR_Toy:_Sampling_mode</a>
 *
 */
public final class IrToy extends IrSerial<LocalSerialPortRaw> implements IRawIrSender, ICapture, IReceive {

    static final Logger logger = Logger.getLogger(IrToy.class.getName());

    public static final boolean useDebuggingLeds = false; // For best performance, do not use debugging LEDs.
    public static final String defaultPortName = "/dev/ttyACM0";
    public static final int defaultBaudRate = 115200;
    public static final LocalSerialPort.FlowControl defaultFlowControl = LocalSerialPort.FlowControl.RTSCTS;
    public static final int defaultTimeout = DEFAULT_BEGIN_TIMEOUT;

    private static final int dataSize = 8;
    private static final LocalSerialPort.StopBits stopBits = LocalSerialPort.StopBits.ONE;
    private static final LocalSerialPort.Parity parity = LocalSerialPort.Parity.NONE;

    private static final double oscillatorFrequency = 48000000;
    private static final double period = 21.3333; // microseconds
    private static final double PICClockFrequency = 12000000;
    private static final byte dutyCycle = 0; // semantically: don't care

    private static final boolean transmitNotifyEnabled = true;
    private static final boolean transmitByteCountReportEnabled = true;
    private static final boolean transmitHandshakeEnabled = true;

    private final static byte cmdReset = 0x00; // Reset (returns to remote decoder mode)
    // 0x01 RESERVED for SUMP RUN
    // 0x02 RESERVED for SUMP ID
    private final static byte cmdTransmit = 0x03; // Transmit (FW v07+)
    private final static byte cmdFrequencyReport = 0x04; // Frequency report (reserved for future hardware)
    // 0x05 Setup sample timer (FW v07+)
    private final static byte cmdSetFrequency = 0x06; // Setup frequency modulation timer (FW v07+)
    private final static byte cmdLedMuteOn = 0x10; // LED mute on (FW v07+)
    private final static byte cmdLedMuteOff = 0x11; // LED mute off (FW v07+)
    private final static byte cmdLedOn = 0x12; // LED on (FW v07+)
    private final static byte cmdLedOff = 0x13; // LED off (FW v07+)
    // 0x23 Settings descriptor report (FW v20+)
    private final static byte cmdTransmitByteCountReport = 0x24; // Enable transmit byte count report (FW v20+)
    private final static byte cmdTransmitNotify = 0x25; // Enable transmit notify on complete (FW v20+)
    private final static byte cmdTransmitHandshake = 0x26; // Enable transmit handshake (FW v20+)
    private final static byte cmdIOwrite = 0x30; // Sets the IO pins to ground (0) or +5volt (1).
    private final static byte cmdIOdirection = 0x31; // Sets the IO pins to input (1) or output (0).
    private final static byte cmdIOread = 0x32; // Read the IO pins, returns 1 byte.
    private final static byte cmdUARTsetup = 0x40; // Setup the UART to send serial data. Uses the current virtual serial port settings.
    private final static byte cmdUARTclose = 0x41; // Close the UART.
    private final static byte cmdUARTwrite = 0x42; // Send a byte to the serial UART.

    // Source: http://dangerousprototypes.com/docs/USB_IR_Toy:_IRman_decoder_mode
    private final static byte cmdSamplingMode = (byte) 's';
    private final static byte cmdSelfTest = (byte) 't';
    private final static byte cmdVersion = (byte) 'v';
    private final static byte cmdBootloaderMode = (byte) '$';

    private final static byte endOfData = (byte) 0xff;
    private final static int transmitByteCountToken = 't';
    private final static int transmitCompleteSuccess = 'C';
    private final static int transmitCompleteFailure = 'F';

    // Versions strings are exactly 4 chars in length, see http://dangerousprototypes.com/docs/USB_IR_Toy:_IRman_decoder_mode
    private final static int lengthVersionString = 4;
    private final static int lengthSelftestVersionString = 4;
    private final static int lengthProtocolVersionString = 3;
    private final static String expectedProtocolVersion = "S01";
    private final static int emptyBufferSize = 62;

    private final static int powerPin = 5;
    private final static int receivePin = 3;
    private final static int sendingPin = 4;

    private boolean stopCaptureRequest = true;
    private String protocolVersion;
    private String version;
    private int captureMaxSize = DEFAULT_CAPTURE_MAXSIZE;
    private int IOdirections = -1;
    private int IOdata = 0;
    private boolean useSignalingLed;

    public IrToy() throws IOException {
        this(defaultPortName);
    }

    public IrToy(String portName) throws IOException {
        this(portName, false);
    }

    public IrToy(String portName, boolean verbose) throws IOException {
        this(portName, verbose, null);
    }

    public IrToy(String portName, boolean verbose, Integer timeout) throws IOException {
        this(portName, verbose, timeout, defaultBaudRate);
    }

    public IrToy(String portName, boolean verbose, Integer timeout, Integer baudRate) throws IOException {
        this(portName, verbose, timeout, baudRate, DEFAULT_CAPTURE_MAXSIZE, defaultFlowControl);
    }

    public IrToy(String portName, boolean verbose, Integer timeout, Integer baudRate, Integer maxLearnLength, LocalSerialPort.FlowControl flowControl)
            throws IOException {
        super(LocalSerialPortRaw.class, LocalSerialPort.canonicalizePortName(portName, defaultPortName), verbose, timeout != null ? timeout : defaultTimeout, baudRate, dataSize, stopBits, parity, flowControl);
    }

    private void goSamplingMode() throws IOException, HarcHardwareException {
        send(cmdSamplingMode);
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
        }
        protocolVersion = readString(lengthProtocolVersionString);
        if (!protocolVersion.equals(expectedProtocolVersion))
            throw new HarcHardwareException("Unsupported IrToy protocol version: " + protocolVersion);
    }

    private void setupSendingModes() throws IOException {
        if (transmitNotifyEnabled)
            send(cmdTransmitNotify);
        if (transmitHandshakeEnabled)
            send(cmdTransmitHandshake);
        if (transmitByteCountReportEnabled)
            send(cmdTransmitByteCountReport);
    }

    private byte[] prepare3(byte cmd, int data) {
        byte[] array = new byte[3];
        array[0] = cmd;
        array[1] = (byte) ((data >> 8) & 0xff);
        array[2] = (byte) (data & 0xff);
        return array;
    }

    private void setIOData() throws IOException {
        send(prepare3(cmdIOdirection, IOdirections));
        send(prepare3(cmdIOwrite, IOdata));
    }

    /**
     * pin 2: RA2
     * pin 3: RA3
     * pin 4: RA4
     * pin 5: RA5
     * pin 11: RB3
     * pin 13: RB5
     *
     * @param pin
     * @param state
     * @throws IOException
     */
    public void setPin(int pin, boolean state) throws IOException {
        if (useSignalingLed) {
            int mask = 1 << pin;
            IOdirections &= ~mask;
            if (state)
                IOdata |= mask;
            else
                IOdata &= ~mask;
            setIOData();
        }
    }

    @Override
    public void open() throws IOException, HarcHardwareException {
        super.open();
        reset(5);
        send(cmdVersion);
        version = readString(lengthVersionString);
        checkVersion();
        goSamplingMode();
        setupSendingModes();
        setPin(powerPin, true);
    }

    public void checkVersion() throws HarcHardwareException, IOException {
        int numerical;
        try {
            numerical = Integer.parseInt(version.substring(1));
        } catch (NumberFormatException ex) {
            throw new HarcHardwareException("Unsupported firmware: " + version);
        }
        int hwVersion = numerical / 100;
        int swMainVersion = (numerical / 10) % 10;
        int swMinorVersion = numerical % 10;
        if (!(hwVersion == 2 && swMainVersion == 2 && swMinorVersion != 3))
            // Just does not work, see http://dangerousprototypes.com/forum/viewtopic.php?f=29&t=4024&start=23
            throw new HarcHardwareException("Unsupported firmware: " + version);

        useSignalingLed = useDebuggingLeds && (swMinorVersion >= 2);
    }

    @Override
    public void close() throws IOException {
        if (isValid()) {
            IOdirections = -1;
            setIOData();
            reset(1);
        }
        super.close();
    }

    public void reset(int times) throws IOException {
        for (int i = 0; i < times; i++)
            send(cmdReset);
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
        }
        serialPort.flushInput();
    }

    private void send(byte[] buf) throws IOException {
        serialPort.sendBytes(buf);
        //serialPort.flush();
    }

    private void send(byte[] buf, int offset, int length) throws IOException {
        serialPort.sendBytes(buf, offset, length);
        //serialPort.flush();
    }

    private void send(byte b) throws IOException {
        serialPort.sendByte(b);
        //serialPort.flush();
    }

    private byte[] toByteArray(int[] data) {
        byte[] buf = new byte[2*data.length];
        for (int i = 0; i < data.length; i++) {
            int periods = (int)Math.round(data[i]/period);
            buf[2*i] = (byte)(periods / 256);
            buf[2*i+1] = (byte) (periods % 256);
        }
        // REPLACE last gap by 0xFFFF
        buf[2*data.length-2] = endOfData;
        buf[2*data.length-1] = endOfData;
        return buf;
    }

    private int[] recv() throws IOException  {
        try {
            int[] array = new int[captureMaxSize];
            int size = 0;
            stopCaptureRequest = false;
            setPin(receivePin, true);
            for (int i = 0; i < captureMaxSize; i++) { // if leaving here, reset is needed.
                if (stopCaptureRequest)
                    return null;
                int val = read2Bytes(); // throws TimeoutException
                int ms = (int) Math.round(val * period);
                array[i] = ms;
                size++;
                if (val == 0xffff)
                    // Only way for timeout, 1.4 seconds. Too long for most use cases ... :-\
                    break;
            }

            int[] result = new int[size];
            System.arraycopy(array, 0, result, 0, size);
            return result;
        } finally {
            setPin(receivePin, false);
        }
    }

    private double getFrequency(int onTimes) throws IOException {
        send(cmdFrequencyReport);
        /*int t1 =*/ read2Bytes();
        /*int t2 =*/ read2Bytes();
        /*int t3 =*/ read2Bytes();
        int count = read2Bytes();
        return count/IrCoreUtils.microseconds2seconds(onTimes) ;
    }

    @Override
    public ModulatedIrSequence capture() throws HarcHardwareException, IOException {
        // reset, while I do not want any already recorder signals.
        reset(5);
        goSamplingMode();
        int[] data;
        try {
            data = recv(); // throws TimeoutException as per beginTimeout
        } catch (TimeoutException ex) {
            return null;
        }
        if (stopCaptureRequest || data == null)
            return null;

        int sum = 0;
        for (int i = 0; i < data.length / 2; i++) {
            sum += data[2 * i];
        }
        double frequency = getFrequency(sum);
        ModulatedIrSequence seq = null;
        try {
            seq = new ModulatedIrSequence(data, frequency);
        } catch (OddSequenceLengthException ex) {
            for (int i = 0; i < data.length; i++)
                logger.log(Level.FINEST, "data[{0}] = {1}", new Object[]{i, data[i]});
            throw new HarcHardwareException("IrToy: Erroneous data received.");
        }
        return seq;
    }

    @Override
    public boolean stopCapture() {
        stopCaptureRequest = true;
        return true;
    }

    @Override
    public IrSequence receive() throws HarcHardwareException, IOException {
        return capture();
    }

    @Override
    public boolean stopReceive() {
        return stopCapture();
    }

    private boolean transmit(int[] data, double frequency) throws IOException, HarcHardwareException {
        if (frequency > 0)
            setFrequency(frequency);
        return transmit(data);
    }

    private boolean transmit(int[] data) throws IOException, HarcHardwareException {
        reset(1);
        goSamplingMode();
        setupSendingModes();
        setPin(sendingPin, true);
        byte[] buf = toByteArray(data);
        send(cmdTransmit);
        boolean succcess = true;

        try {
            if (transmitHandshakeEnabled) {
                int bytesSent = 0;
                while (bytesSent < buf.length) {
                    int noBytes = readByte(); // number of bytes free in buffer, the number we should send
                    if (noBytes != emptyBufferSize)
                        continue;
                    int toSend = Math.min(noBytes, buf.length - bytesSent);
                    send(buf, bytesSent, toSend);
                    bytesSent += toSend;
                }
            }
            int noBytes = readByte();
            if (noBytes != emptyBufferSize) {
                logger.log(Level.FINE, "got {0} expected {1}", new Object[]{noBytes, emptyBufferSize});
                succcess = false;
            }

            if (succcess && transmitByteCountReportEnabled) {
                int token = readByte();
                if (token == transmitByteCountToken) { // 't'
                    int bytesSent = read2Bytes();
                    if (bytesSent != data.length * 2) {
                        logger.log(Level.FINE, "sent {0} should: {1}", new Object[]{bytesSent, data.length * 2});
                        succcess = false;
                    }
                } else {
                    logger.log(Level.FINE, "did not get t but {0}", token);
                    succcess = false;
                }
            }

            if (succcess && transmitNotifyEnabled) {
                int token = readByte();
                if (token != transmitCompleteSuccess) {
                    logger.log(Level.FINE, "Status: {0}", token);
                    succcess = false;
                }
            }
        } finally {
            setPin(sendingPin, false);
        }
        return succcess;
    }

    private int byte2unsignedInt(byte b) {
        return b >= 0 ? b : b + 256;
    }

    private String readString(int length) throws IOException {
        byte[] buf = serialPort.readBytes(length);
        return new String(buf, 0, length, Charset.forName("US-ASCII"));
    }

    private int readByte() throws IOException {
        byte[] a = serialPort.readBytes(1);
        return a.length > 0 ? byte2unsignedInt(a[0]) : -1;
    }

    private int read2Bytes() throws IOException {
        byte[] a = serialPort.readBytes(2);
        return a.length < 2 ? -1 : 256*byte2unsignedInt(a[0]) + byte2unsignedInt(a[1]);
    }

    public String selftest() throws IOException {
        reset(5);
        send(cmdSelfTest);
        String ver = readString(lengthSelftestVersionString);
        return ver;
    }

    public void bootloaderMode() throws IOException {
        reset(5);
        send(cmdBootloaderMode);
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setLedMute(boolean status) throws IOException {
        if (useSignalingLed)
            send(status ? cmdLedMuteOn : cmdLedMuteOff);
    }

    public void setLed(boolean status) throws IOException {
        if (useSignalingLed)
            send(status ? cmdLedOn : cmdLedOff);
    }

    private void setFrequency(double frequency) throws IOException {
        byte pr2 = (byte) Math.round(oscillatorFrequency/(16*frequency) - 1);
        byte[] buf = new byte[3];
        buf[0] = cmdSetFrequency;
        buf[1] = pr2;
        buf[2] = dutyCycle;
        send(buf);
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setBeginTimeout(int beginTimeout) throws IOException {
        super.setTimeout(beginTimeout);
    }

    @Override
    public void setCaptureMaxSize(int captureMaxSize) {
        this.captureMaxSize = captureMaxSize;
    }

    @Override
    public void setEndingTimeout(int timeout) {
    }

    @Override
    public boolean sendIr(IrSignal code, int count, Transmitter transmitter) throws IOException, HarcHardwareException {
        return transmit(code.toIntArray(count), code.getFrequency());
    }

    public boolean sendCcf(String ccf, int count, Transmitter transmitter) throws IOException, HarcHardwareException, Pronto.NonProntoFormatException, InvalidArgumentException {
        return sendIr(Pronto.parse(ccf), count, transmitter);
    }

    /**
     * Not supported due to hardware restrictions.
     *
     * @param ccf
     * @param transmitter
     * @return
     */
    public boolean sendCcfRepeat(String ccf, Transmitter transmitter) {
        throw new UnsupportedOperationException("Not supported due to hardware restrictions.");
    }


    @Override
    public void setDebug(int debug) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
