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

import java.io.IOException;
import java.util.List;
import org.harctoolbox.harchardware.Utils;
import org.harctoolbox.harchardware.misc.SonySerialCommand; // just for main().

public final class LocalSerialPortRaw extends LocalSerialPort implements IBytesCommand {

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) {
        List<String> names;
        try {
            names = getSerialPortNames(false);
            names.forEach((name) -> {
                System.out.println(name);
            });

            LocalSerialPortRaw port = new LocalSerialPortRaw(DEFAULT_PORT, true, 10000, 38400, 8, StopBits.ONE, Parity.EVEN, FlowControl.NONE);

            if (args.length == 0) {
                int upper = 0x1;
                int lower = 0x13;
                SonySerialCommand.Type type = SonySerialCommand.Type.get;
                //byte[] cmd = SonySerialCommand.bytes(0x17, 0x15); // power toggle
                //byte[] cmd = SonySerialCommand.bytes(0x17, 0x2f); // power off
                byte[] cmd = SonySerialCommand.bytes(upper, lower, type); // get lamp time
                port.sendBytes(cmd);
                if (upper <= 1) {
                    byte[] answer = port.readBytes(SonySerialCommand.size);
                    for (int i = 0; i < SonySerialCommand.size; i++) {
                        System.out.println(i + "\t" + answer[i]);
                    }
                    SonySerialCommand.Command response = SonySerialCommand.interpret(answer);
                    System.out.println(response);
                }
                port.close();
            }
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public LocalSerialPortRaw(String portName, boolean verbose, Integer timeout, Integer baud, Integer dataLength, StopBits stopBits, Parity parity, FlowControl flowControl) throws IOException {
        super(portName, verbose, timeout, baud, dataLength, stopBits, parity, flowControl);
    }

    @Override
    public byte[] readBytes(int size) throws IOException {
        return Utils.readBytes(inStream, size);
    }

    public int readBytes(byte[] buf) throws IOException {
        return inStream.read(buf);
    }

    public int readByte() throws IOException {
        return inStream.read();
    }

    @Override
    public void sendBytes(byte[] data) throws IOException {
        outStream.write(data);
    }

    public void sendBytes(byte[] data, int offset, int length) throws IOException {
        outStream.write(data, offset, length);
    }

    public void sendByte(byte b) throws IOException {
        outStream.write(b);
    }
}
