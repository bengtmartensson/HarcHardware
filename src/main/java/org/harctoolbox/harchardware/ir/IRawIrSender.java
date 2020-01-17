/*
Copyright (C) 2012 Bengt Martensson.

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
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;

/**
 * Commands for IR senders being able to send IR signals.
 */
public interface IRawIrSender {

    /** Max number of repeats to honor */
    static int repeatMax = 1000;

    /**
     *
     * @param irSignal
     * @param count
     * @param transmitter
     * @return if false, command failed.
     * @throws NoSuchTransmitterException
     * @throws IOException
     * @throws org.harctoolbox.ircore.InvalidArgumentException
     */
    public boolean sendIr(IrSignal irSignal, int count, Transmitter transmitter) throws HarcHardwareException, NoSuchTransmitterException, IOException, InvalidArgumentException;

    /**
     * Returns a default Transmitter for the device. May be null if the device ignores the Transmitter argument in sendIr.
     * @return
     */
    public Transmitter getTransmitter();
}
