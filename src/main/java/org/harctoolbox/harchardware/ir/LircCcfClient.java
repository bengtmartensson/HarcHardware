/*
Copyright (C) 2009-2013 Bengt Martensson.

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
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.Pronto;

/**
 * A <a href="http://www.lirc.org">LIRC</a> client, talking to a remote LIRC
 * server through a TCP port.
 */
public class LircCcfClient extends LircClient implements IRawIrSender {

    public LircCcfClient(InetAddress hostname, Integer port, boolean verbose, Integer timeout) throws IOException {
        super(hostname, port, verbose, timeout);
    }

    public LircCcfClient(String hostname, Integer port, boolean verbose, Integer timeout) throws IOException {
        this(InetAddress.getByName(hostname), port, verbose, timeout);
    }

    public LircCcfClient(InetAddress hostname, boolean verbose, Integer timeout) throws IOException {
        this(hostname, null, verbose, timeout);
    }

    public LircCcfClient(InetAddress hostname, boolean verbose) throws IOException {
        this(hostname, verbose, null);
    }

    public LircCcfClient(InetAddress hostname) throws IOException {
        this(hostname, false);
    }

    public LircCcfClient(String hostname) throws UnknownHostException, IOException {
        this(InetAddress.getByName(hostname));
    }

    public boolean sendCcf(String ccf, int count, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        if (transmitter != null) {
            boolean success = setTransmitters(transmitter);
            if (!success)
                throw new NoSuchTransmitterException(transmitter);
        }
        return sendCommand("SEND_CCF_ONCE " + (count - 1) + " " + ccf, false) != null;
    }

    public boolean sendCcf(String ccf, int count, int port) throws IOException, NoSuchTransmitterException {
        return sendCcf(ccf, count, getTransmitter(port));
    }

    public boolean sendCcfRepeat(String ccf, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
            return setTransmitters(transmitter)
                    && sendCommand("SEND_CCF_START " + ccf, false) != null;
    }

    @Override
    public boolean sendIr(IrSignal irSignal, int count, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        return sendCcf(Pronto.toString(irSignal), count, transmitter);
    }

    @Override
    public boolean stopIr(Transmitter transmitter) throws IOException, NoSuchTransmitterException {
            return setTransmitters(transmitter)
                    && sendCommand("SEND_STOP", false) != null;
    }

    public boolean stopIr(int port) throws NoSuchTransmitterException, IOException {
        return stopIr(getTransmitter(port));
    }
}
