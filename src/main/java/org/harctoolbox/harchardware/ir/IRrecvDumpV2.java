/*
Copyright (C) 2020 Bengt Martensson.

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

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Logger;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.LocalSerialPortBuffered;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.IrSequence;
import org.harctoolbox.ircore.OddSequenceLengthException;

public class IRrecvDumpV2 implements IHarcHardware, IReceive, Closeable {

    private static final Logger logger = Logger.getLogger(IRrecvDumpV2.class.getName());

    private static final int DEFAULT_BAUD = 115200;
    private static final String DEFAULT_PORTNAME = "arduino";
    private static final String RAWDATADECL = "unsigned int  rawData";
    private static final int DUMMY_ENDING_GAP = (int) IrCoreUtils.DEFAULT_MINIMUM_LEADOUT;

    static IrSequence parse(String str) throws OddSequenceLengthException {
        if (!str.startsWith(RAWDATADECL))
            return null;

        String[] arr = str.split("\\{");
        if (arr.length != 2)
            return null;
        String[] arr1 = arr[1].split("\\}");
        if (arr1.length != 2)
            return null;

        String[] data = arr1[0].split(",\\s*");
        int[] micros = new int[data.length + 1];
        for (int i = 0; i < data.length; i++)
            micros[i] = Integer.parseInt(data[i]);

        micros[data.length] = DUMMY_ENDING_GAP;
        return new IrSequence(micros);
    }

    private final LocalSerialPortBuffered serialPort;
    private boolean stopRequested;
    private int beginTimeout = DEFAULT_BEGIN_TIMEOUT;

    public IRrecvDumpV2(LocalSerialPortBuffered hardware) throws HarcHardwareException, IOException {
        this.serialPort = hardware;
    }

    public IRrecvDumpV2(String portName, boolean verbose, Integer timeout, Integer baud) throws IOException, HarcHardwareException {
        this(new LocalSerialPortBuffered(portName, verbose, timeout, baud));
    }

    public IRrecvDumpV2(String portName, boolean verbose, Integer timeout) throws IOException, HarcHardwareException {
        this(new LocalSerialPortBuffered(LocalSerialPort.canonicalizePortName(portName, DEFAULT_PORTNAME), verbose, timeout, DEFAULT_BAUD));
    }

    @Override
    public void close() throws IOException {
        serialPort.close();
    }

    @Override
    public String getVersion() throws IOException {
        return null;
    }

    @Override
    public void setVerbose(boolean verbose) {
        serialPort.setVerbose(verbose);
    }

    @Override
    public void setBeginTimeout(int beginTimeout) throws IOException {
        serialPort.setTimeout(beginTimeout);
        this.beginTimeout = beginTimeout;
    }

    @Override
    public boolean isValid() {
        return serialPort.isValid();
    }

    @Override
    public void open() throws IOException, HarcHardwareException {
        serialPort.open();
    }

    @Override
    public IrSequence receive() throws HarcHardwareException, IOException, OddSequenceLengthException {
        if (stopRequested)
            return null;

        if (!isValid())
            throw new HarcHardwareException("Port not initialized");

        serialPort.setTimeout(beginTimeout);
        serialPort.flushInput();
        IrSequence irSequence = null;
        do {
            String str = serialPort.readString(true);
            if (str == null)
                return null; // timeout

            irSequence = parse(str);
        } while (irSequence == null);
        return irSequence;
    }


    @Override
    public boolean stopReceive() {
        stopRequested = true;
        return true;
    }

    public void reset() throws IOException, HarcHardwareException {
        serialPort.dropDTR(100);
    }

    @Override
    public void setDebug(int debug) {
    }

    @Override
    public void setTimeout(int timeout) throws IOException {
        serialPort.setTimeout(timeout);
    }

    @Override
    public void setCaptureMaxSize(int integer) {
    }

    @Override
    public void setEndingTimeout(int endingTimeout) {
    }
}
