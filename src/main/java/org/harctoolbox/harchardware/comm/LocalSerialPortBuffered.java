/*
Copyright (C) 2012,2013,2014 Bengt Martensson.

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

package org.harctoolbox.harchardware.comm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.ICommandLineDevice;
import org.harctoolbox.ircore.IrCoreUtils;

public final class LocalSerialPortBuffered extends LocalSerialPort implements ICommandLineDevice {

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) {
        List<String> names;
        try (LocalSerialPortBuffered port = new LocalSerialPortBuffered("/dev/ttyS0", true, 10000, 9600, 8, StopBits.ONE, Parity.NONE, FlowControl.NONE)) {
            names = getSerialPortNames(false);
            names.forEach((name) -> {
                System.out.println(name);
            });

            String cmd = "#POW\r";
            port.open();
            port.sendString(cmd);
            System.out.println(port.readString());

        } catch (IOException | HarcHardwareException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private static String escapeCommandLine(String cmd) {
        return cmd.replace("\r", "\\r").replace("\n", "\\n");
    }

    private BufferedReader bufferedInStream;

    public LocalSerialPortBuffered(String portName, boolean verbose, Integer timeout, Integer baud, Integer dataLength, StopBits stopBits, Parity parity, FlowControl flowControl) throws IOException {
         super(portName, verbose, timeout, baud, dataLength, stopBits, parity, flowControl);
    }

    public LocalSerialPortBuffered(String portName, boolean verbose, Integer timeout, Integer baud) throws IOException {
        this(portName, verbose, timeout, baud, null, null, null, null);
    }

    public LocalSerialPortBuffered(String portName, boolean verbose, Integer timeout) throws IOException {
        this(portName, verbose, timeout, null);
    }

    public LocalSerialPortBuffered(String portName, boolean verbose) throws IOException {
        this(portName, verbose, null);
    }

    public LocalSerialPortBuffered(String portName) throws IOException {
        this(portName, false);
    }

    public LocalSerialPortBuffered(Integer portNumber) throws NonExistingPortException, IOException {
        this(getSerialPortName(portNumber));
    }

    @Override
    public void open() throws HarcHardwareException, IOException {
        super.open();
        bufferedInStream = new BufferedReader(new InputStreamReader(inStream, IrCoreUtils.DUMB_CHARSET));
    }

    @Override
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void sendString(String cmd) throws IOException {
        if (verbose)
            System.err.println("LocalSerialPortBuffered.sendString: Sent '" + escapeCommandLine(cmd) + "'.");
        sendBytes(cmd.getBytes(IrCoreUtils.DUMB_CHARSET));
    }

    //*@Override
    public void sendBytes(byte[] data) throws IOException {
        outStream.write(data);
    }

    public void sendBytes(byte[] data, int offset, int length) throws IOException {
        outStream.write(data, offset, length);
    }

    public void sendByte(byte b) throws IOException {
        outStream.write(b);
    }

    @Override
    public String readString() throws IOException {
        return readString(false);
    }

    @Override
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public String readString(boolean wait) throws IOException {
        if (!(wait || bufferedInStream.ready()))
            return null;

        try {
            String result = bufferedInStream.readLine();
            if (verbose)
                System.err.println("LocalSerialPortBuffered.readString: received "
                        + (result != null ? ("\"" + result + "\"") : "<null>"));
            return result;
        } catch (IOException ex) {
            if (ex.getMessage().equals("Underlying input stream returned zero bytes")) { // RXTX....
                if (verbose)
                    System.err.println("LocalSerialPortBuffered.readString: TIMEOUT");
                return null;
            } else
                throw ex;
        }
    }

    @Override
    public boolean ready() throws IOException {
        return bufferedInStream.ready();
    }
}
