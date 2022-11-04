/*
Copyright (C) 2013, 2014, 2020 Bengt Martensson.

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

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.RXTXPort;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.NonExistingPortException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.IrSequence;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.OddSequenceLengthException;
import org.harctoolbox.ircore.ThisCannotHappenException;

/**
 * This class implements support for Kevin Timmerman's Ir Widget.
 * It uses the NRJavaSerial library, which encapsulates all system dependencies.
 * Although it duplicates some functionality found in Kevin's program IrScope (file widget.cpp),
 * it is not derived.
 *
 * <a href="http://www.compendiumarcana.com/irwidget/">Original web page</a>.
 */

// Only the "irwidgetPulse" (case 0 in widget.cpp) is implemented.
public class IrWidget implements IHarcHardware, ICapture {

    private static final Logger logger = Logger.getLogger(IrWidget.class.getName());

    /** Number of micro seconds in a count. */
    public static final int MICROS_PER_TICK = 100;
    public static final String DEFAULT_PORTNAME = "/dev/ttyUSB0";
    public static final String IRWIDGET = "IrWidget";
    private static final int BAUDRATE = 115200;
    private static final int MASK = 0x3F;
    private static final int SHORT_DELAY = 20;
    private static final int LONG_DELAY = 100; // was 100, 200 by Kevin
    private static final int INVALID = -1;
    private static final int EMERGENCY_TIMEOUT = 10000;

    private final CommPortIdentifier portIdentifier;
    private RXTXPort serialPort;
    private int debug;
    private byte[] data;
    private int[] times;
    private int dataLength;
    private double frequency;
    private boolean stopRequested;
    private boolean verbose;
    private int beginTimeout;
    private int captureMaxSize;
    private int endingTimeout;
    private final boolean lowerDtrRts;

     /**
     * Constructs new IrWidget with default port name and timeouts.
     *
     * @throws java.io.IOException
     * @throws org.harctoolbox.harchardware.comm.NonExistingPortException
     */
    public IrWidget() throws IOException, NonExistingPortException {
        this(null);
    }

    public IrWidget(String portName) throws IOException, NonExistingPortException {
        this(portName, false, null, true);
    }

    /**
     * Constructs new IrWidget with default timeouts.
     *
     * @param portName Name of serial port to use. Typically something like COM7: (Windows) or /dev/ttyUSB0.
     * @param verbose
     * @param timeout
     * @throws java.io.IOException
     * @throws org.harctoolbox.harchardware.comm.NonExistingPortException
     */
    public IrWidget(String portName, boolean verbose, Integer timeout, boolean lowerDtrRts) throws IOException, NonExistingPortException {
        this(portName, verbose, timeout, null, null, lowerDtrRts);
    }

    /**
     * Constructs new IrWidget.
     * @param portName Name of serial port to use. Typically something like COM7: (Windows) or /dev/ttyUSB0.
     * @param beginTimeout
     * @param captureMaxSize
     * @param verbose
     * @param endingTimeout
     * @param lowerDtrRts
     * @throws java.io.IOException
     * @throws org.harctoolbox.harchardware.comm.NonExistingPortException
     */
    public IrWidget(String portName, boolean verbose, Integer beginTimeout, Integer captureMaxSize, Integer endingTimeout, boolean lowerDtrRts) throws IOException, NonExistingPortException {
        this.debug = 0;
        this.verbose = verbose;
        this.beginTimeout = beginTimeout != null ? beginTimeout : DEFAULT_BEGIN_TIMEOUT;
        this.captureMaxSize = captureMaxSize != null ? captureMaxSize : DEFAULT_CAPTURE_MAXSIZE;
        this.endingTimeout = endingTimeout != null ? endingTimeout : DEFAULT_ENDING_TIMEOUT;
        this.lowerDtrRts = lowerDtrRts;
        String realPortName = LocalSerialPort.canonicalizePortName(portName, DEFAULT_PORTNAME);
        try {
            portIdentifier = CommPortIdentifier.getPortIdentifier(realPortName);
        } catch (NoSuchPortException ex) {
            // Repack to prevent exporting the NRserial/RXTX-exceptions.
            throw new NonExistingPortException(realPortName);
        }
    }

    @Override
    public void setDebug(int debug) {
        this.debug = debug;
    }

    @Override
    public void open() throws HarcHardwareException, IOException {
        try {
           serialPort = portIdentifier.open(getClass().getName(), DEFAULT_BEGIN_TIMEOUT);
        } catch (PortInUseException ex) {
            throw new HarcHardwareException(ex);
        }

        try {
            serialPort.setSerialPortParams(BAUDRATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        } catch (UnsupportedCommOperationException ex) {
            throw new HarcHardwareException(ex);
        }
        serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);

        serialPort.disableReceiveThreshold();
        //serialPort.disableReceiveFraming();
        //serialPort.enableReceiveThreshold(0);
        serialPort.enableReceiveTimeout(beginTimeout);
    }

    @Override
    public void close() throws IOException {
        if (serialPort != null) {
            if (lowerDtrRts)
                disableIrWidgetMode();
            serialPort.close();
            serialPort = null;
        }
    }

    private void enableIrWidgetMode() {
        try {
            serialPort.setDTR(false);
            serialPort.setRTS(false);
            Thread.sleep(SHORT_DELAY); // ???
            serialPort.setDTR(true);
            Thread.sleep(LONG_DELAY);
            serialPort.setRTS(true);
        } catch (InterruptedException ex) {
            throw new ThisCannotHappenException(ex);
        }
    }

    private void disableIrWidgetMode() {
        serialPort.setDTR(false);
        serialPort.setRTS(false);
    }

    /**
     *
     * @param timeout
     */
    @Override
    public void setTimeout(int timeout) {
        setBeginTimeout(timeout);
    }

    @Override
    public void setBeginTimeout(int timeout) {
        this.beginTimeout = timeout;
    }

    @Override
    public void setCaptureMaxSize(int maxCaptureMaxSize) {
        this.captureMaxSize = maxCaptureMaxSize;
    }

    @Override
    public void setEndingTimeout(int endingTimeout) {
        this.endingTimeout = endingTimeout;
    }

    /**
     * Captures a signal using the given timeout values, and returns it as a ModulatedIrSequence.
     *
     * @return ModulatedIrSequence
     * @throws IOException
     */
    @Override
    public ModulatedIrSequence capture() throws IOException {
        if (lowerDtrRts)
            enableIrWidgetMode();
        InputStream inputStream = serialPort.getInputStream();
        try {
            serialPort.clearCommInput();
        } catch (UnsupportedCommOperationException ex) {
            throw new ThisCannotHappenException(ex);
        }
        //serialPort.enableReceiveTimeout(beginTimeout);

        try {
            while (true) {
                int maxToRead = (int) Math.round(IrCoreUtils.milliseconds2microseconds(captureMaxSize) / MICROS_PER_TICK);
                data = new byte[maxToRead];

                int readByte = inputStream.read(); // blocks. If IOException we really have a problem, so don't catch it
                if (readByte == INVALID) { // timeout
                    if (verbose)
                        System.err.println("TIMEOUT");
                    return null;
                }
                data[0] = (byte) readByte;

                byte last = (byte) INVALID;
                long startTime = System.currentTimeMillis();
                long lastEvent = startTime;
                stopRequested = false;
                int bytesRead = 1;

                // Runs in spin-wait, but only when really capturing
                while (bytesRead < maxToRead && !stopRequested) {
                    if (inputStream.available() == 0) {
                        if (EMERGENCY_TIMEOUT > 0 && System.currentTimeMillis() - lastEvent >= EMERGENCY_TIMEOUT)
                            break;

                        continue;
                    }
                    int noRead = inputStream.read(data, bytesRead, maxToRead - bytesRead);
                    bytesRead += noRead;
                    int i = 0;
                    while (i < noRead && data[bytesRead - noRead + i] == last) {
                        i++;
                    }

                    if (i == noRead) {
                        // no new information has arrived
                        if (System.currentTimeMillis() - lastEvent >= endingTimeout)
                            break;
                    } else {
                        // something happened
                        lastEvent = System.currentTimeMillis();
                    }

                    last = data[bytesRead - 1];
                }
                boolean success = compute(bytesRead);
                if (success) {
                    try {
                        ModulatedIrSequence modulatedIrSequence = new ModulatedIrSequence(new IrSequence(times), frequency, null);
                        if (verbose)
                            System.err.println("<Received: " + modulatedIrSequence.toString(true));
                        return modulatedIrSequence;
                    } catch (OddSequenceLengthException ex) {
                        throw new ThisCannotHappenException(ex);
                    }
                } else
                    // useless data received, make another attempt
                    ;
            }
        } finally {
            if (serialPort != null && lowerDtrRts)
                disableIrWidgetMode();
            inputStream.close();
        }
    }

    /**
     * The IrWidget does not support versions.
     * @return null
     */
    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public boolean isValid() {
        return serialPort != null;
    }

    /**
     * Stops ongoing capture.
     * @return true
     */
    @Override
    public boolean stopCapture() {
        if (debug > 0)
            System.err.println("captureStop called");
        stopRequested = true;
        return true;
    }

    private boolean compute(int noBytes) {
        if (noBytes == 0)
            return false;

        // replace the existing, incremental data by actual count in the intervals
        for (int i = 0; i < noBytes - 1; i++)
            data[i] = (byte) (MASK & (data[i + 1] - data[i]));
        dataLength = (noBytes - 1) & ~1;

        // Compute frequency
        int periods = 0;
        int bins = 0;
        int pulses = 1;
        int gaps = 0;

        for (int i = 1; i < dataLength; i++) {
            if (i < dataLength - 1 && data[i] > 0 && data[i - 1] > 0 && data[i + 1] > 0) {
                periods += data[i];
                bins++;
            }
            if (data[i] > 0 && data[i - 1] == 0)
                pulses++;
            if (data[i] == 0 && data[i - 1] > 0)
                gaps++;
        }

        if (bins == 0)
            return false;

        frequency = periods / IrCoreUtils.microseconds2seconds(bins * MICROS_PER_TICK);
        times = new int[2 * gaps];
        int index = 0;
        int currentCount = 0;
        int currentGap = 0;
        boolean previousState = false;
        boolean currentState = false;
        for (int i = 0; i < dataLength; i++) {
            currentState = data[i] > 0;
            if (currentState == previousState) {
                if (currentState)
                    currentCount += data[i];
                else
                    currentGap += MICROS_PER_TICK;
            } else {
                if (currentState) { // starting flash
                    currentGap += gapDuration(data[i]);
                    if (index > 0) {
                        times[index] = -currentGap;
                        index++;
                    }
                    currentCount = data[i];
                    currentGap = 0;
                } else { // starting gap
                    times[index] = pulseDuration(currentCount);
                    index++;
                    currentGap = gapDuration(data[i - 1]) + MICROS_PER_TICK;
                    currentCount = 0;
                }
            }
            previousState = currentState;
        }
        if (index < 2 * gaps)
            times[index] = currentState ? pulseDuration(currentCount) : -currentGap;

        return true;
    }

    private int pulseDuration(int pulses) {
        int x = (int) Math.round(IrCoreUtils.seconds2microseconds(pulses / frequency));
        return x;
    }

    private int gapDuration(int pulses) {
        return MICROS_PER_TICK - pulseDuration(pulses);
    }
}
