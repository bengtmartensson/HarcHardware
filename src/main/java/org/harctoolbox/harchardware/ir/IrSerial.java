/*
Copyright (C) 2013, 2014 Bengt Martensson.

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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.comm.LocalSerialPort;

/**
 * This class models a serial device that takes text commands from a serial port, like the Arduino.
 * @param <T>
 */
public abstract class IrSerial<T extends LocalSerialPort> implements IHarcHardware {
    protected boolean verbose;
    private int timeout;
    protected T serialPort;
    private String portName;
    private int baudRate;
    private int dataSize;
    private LocalSerialPort.StopBits stopBits;
    private LocalSerialPort.Parity parity;
    private LocalSerialPort.FlowControl flowControl;
    private final Class<T> clazz;

    public IrSerial(Class<T> clazz, String portName, boolean verbose, Integer timeout, Integer baudRate, Integer dataSize,
            LocalSerialPort.StopBits stopBits, LocalSerialPort.Parity parity, LocalSerialPort.FlowControl flowControl)
            throws IOException {
        this.clazz = clazz;
        this.portName = portName != null ? portName : LocalSerialPort.DEFAULT_PORT;
        this.baudRate = baudRate != null ? baudRate : LocalSerialPort.DEFAULT_BAUD;
        this.dataSize = dataSize != null ? dataSize : LocalSerialPort.DEFAULT_DATABITS;
        this.stopBits = stopBits != null ? stopBits : LocalSerialPort.DEFAULT_STOPBITS;
        this.parity = parity != null ? parity : LocalSerialPort.DEFAULT_PARITY;
        this.flowControl = flowControl != null ? flowControl : LocalSerialPort.DEFAULT_FLOWCONTROL;
        this.timeout = timeout != null ? timeout : LocalSerialPort.DEFAULT_TIMEOUT;
        this.verbose = verbose;
        //open();
    }

    /**
     * @param baudRate the baudRate to set
     */
    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    /**
     * @param dataSize the dataSize to set
     */
    public void setDataSize(int dataSize) {
        this.dataSize = dataSize;
    }

    /**
     * @param stopBits the stopBits to set
     */
    public void setStopBits(LocalSerialPort.StopBits stopBits) {
        this.stopBits = stopBits;
    }

    /**
     * @param parity the parity to set
     */
    public void setParity(LocalSerialPort.Parity parity) {
        this.parity = parity;
    }

    /**
     * @param flowControl the flowControl to set
     */
    public void setFlowControl(LocalSerialPort.FlowControl flowControl) {
        this.flowControl = flowControl;
    }

    /**
     * @param portName the portName to set
     */
    public void setPortName(String portName) {
        this.portName = portName;
    }

    @Override
    // Default version for hardware that does not support a sensible version.
    // NOTE: just return null, not something "user friendly"
    // -- this is the task of the user interface.
    public String getVersion() throws IOException {
        return null;
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public void setTimeout(int timeout) throws IOException {
        if (!isValid())
            throw new IOException("Port not valid, cannot set timeout.");
        this.timeout = timeout;
        serialPort.setTimeout(timeout);
    }

    @Override
    public boolean isValid() {
        return serialPort != null && serialPort.isValid();
    }

    @Override
    public void close() throws IOException {
        if (!isValid())
            return;

        try {
            serialPort.flush();
        } finally {
            try {
                serialPort.close();
            } finally {
                serialPort = null;
            }
        }
    }

    public Transmitter getTransmitter() {
        return null;
    }


    @Override
    @SuppressWarnings("unchecked")
    public void open() throws HarcHardwareException, IOException {
        try {
            Constructor<T> constructor =  clazz.getConstructor(String.class, boolean.class, Integer.class, Integer.class, Integer.class,
                    LocalSerialPort.StopBits.class, LocalSerialPort.Parity.class, LocalSerialPort.FlowControl.class);
            serialPort = constructor.newInstance(portName, verbose, timeout, baudRate, dataSize, stopBits, parity, flowControl);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException("Programming error in IrSerial");
        }
        serialPort.open();
    }
}
