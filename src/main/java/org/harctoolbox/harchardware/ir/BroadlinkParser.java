/*
Copyright (C) 2022 Bengt Martensson.

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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.ircore.AbstractIrParser;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.IrSignalParser;
import org.harctoolbox.ircore.OddSequenceLengthException;

/**
 *
 */
public abstract class BroadlinkParser extends AbstractIrParser implements IrSignalParser {
    private final static Logger logger = Logger.getLogger(BroadlinkParser.class.getName());

    private final static double TICK = 32.84d;
    private final static int IR_TOKEN = 0x26;
    private final static int ENDING_TOKEN = 0x0d05;

    protected byte[] data;

    protected BroadlinkParser(String string) {
        super(string);
    }

    @Override
    public IrSignal toIrSignal(Double fallbackFrequency, Double dummyGap) throws OddSequenceLengthException, InvalidArgumentException, NumberFormatException {
        try {
            setup();
        } catch (IllegalArgumentException ex) {
            logger.log(Level.FINER, "Invalid Base64 data: {0}", getSource());
            return null;
        }
        int length = data.length;
        List<Integer> durations = new ArrayList<>(length);
        int prefix = readdata(0);
        if (prefix != IR_TOKEN) {
            logger.log(Level.FINER, "IR signal did not start with 0x{0}", Integer.toHexString(IR_TOKEN));
            return null;
        }

        int index = 4;
        while (index < data.length) {
            int chunk = readdata(index);
            index++;
            if (chunk == 0) {
                chunk = readdata(index);
                chunk = 256 * chunk + readdata(index + 1);
                index += 2;
            }
            durations.add((int) Math.round(chunk * TICK));
            if (chunk == ENDING_TOKEN)
                break;
        }
        int[] array = durations.stream().mapToInt(Integer::intValue).toArray();
        return new IrSignal(array, durations.size(), 0, fallbackFrequency);
    }

    private int readdata(int i) {
        byte d = data[i];
        return d >= 0 ? d : d + 256;
    }

    abstract void setup();
}
