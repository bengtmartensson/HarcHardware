/*
Copyright (C) 2013 Bengt Martensson.

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
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.LocalSerialPortBuffered;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.Pronto;

/**
 * This class models a serial device that takes text commands from a serial port, like the Arduino.
 */
public class IrGenericSerial extends IrSerial<LocalSerialPortBuffered> implements IRawIrSender {

    private String command;
    private boolean useSigns;
    private String separator;
    private String lineEnding;
    private boolean raw;

    public IrGenericSerial(String portName, boolean verbose, Integer timeout, Integer baudRate, Integer dataSize,
            LocalSerialPort.StopBits stopBits, LocalSerialPort.Parity parity, LocalSerialPort.FlowControl flowControl)
            throws IOException {
        super(LocalSerialPortBuffered.class, portName, verbose, timeout, baudRate, dataSize, stopBits, parity, flowControl);
    }

    /**
     * @param command the command to set
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * @param useSigns the useSigns to set
     */
    public void setUseSigns(boolean useSigns) {
        this.useSigns = useSigns;
    }

    /**
     * @param separator the separator to set
     */
    public void setSeparator(String separator) {
        this.separator = separator;
    }

    /**
     * @param lineEnding the lineEnding to set
     */
    public void setLineEnding(String lineEnding) {
        this.lineEnding = lineEnding;
    }

    /**
     * @param raw the raw to set
     */
    public void setRaw(boolean raw) {
        this.raw = raw;
    }

    @Override
    public synchronized boolean sendIr(IrSignal irSignal, int count, Transmitter transmitter) throws NoSuchTransmitterException, IOException {
        String payload = formatString(irSignal, count);
        serialPort.sendString(payload);
        if (verbose)
            System.err.print(payload);
        return true;
    }


    private String formatString(IrSignal irSignal, int count) {
        if (irSignal == null)
            throw new IllegalArgumentException("irSignal cannot be null");
        StringBuilder str = new StringBuilder(command);
        str.append(" ");
        ModulatedIrSequence seq = irSignal.toModulatedIrSequence(count);
        if (raw) {
            str.append(seq.toString(useSigns));
        } else {
            IrSignal signal = new IrSignal(seq, seq.getFrequency(), seq.getDutyCycle());
            str.append(Pronto.toString(signal));
        }
        str.append(lineEnding);
        return str.toString();
    }

    @Override
    public void setDebug(int debug) {
    }
}
