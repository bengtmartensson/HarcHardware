/*
Copyright (C) 2023 Bengt Martensson.

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
import static org.harctoolbox.harchardware.ir.Broadlink.A_PRIOR_MODULATION_FREQUENCY;
import static org.harctoolbox.harchardware.ir.Broadlink.DURATIONS_OFFSET;
import static org.harctoolbox.harchardware.ir.Broadlink.IR_ENDING_TOKEN;
import static org.harctoolbox.harchardware.ir.Broadlink.IR_TOKEN;
import static org.harctoolbox.harchardware.ir.Broadlink.LENGTH_LSB_POS;
import static org.harctoolbox.harchardware.ir.Broadlink.LENGTH_MSB_POS;
import static org.harctoolbox.harchardware.ir.Broadlink.REPEAT_POS;
import static org.harctoolbox.harchardware.ir.Broadlink.RF_433_ENDING_TOKEN;
import static org.harctoolbox.harchardware.ir.Broadlink.RF_433_TOKEN;
import static org.harctoolbox.harchardware.ir.Broadlink.TICK;
import static org.harctoolbox.harchardware.ir.Broadlink.TOKEN_POS;
import org.harctoolbox.ircore.AbstractIrParser;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSequence;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.IrSignalParser;
import org.harctoolbox.ircore.OddSequenceLengthException;

/**
 *
 */
public class BroadlinkParser extends AbstractIrParser implements IrSignalParser {

    private final static Logger logger = Logger.getLogger(BroadlinkParser.class.getName());

    public static BroadlinkParser newParser(String str) {
        BroadlinkParser parser = str.startsWith(Integer.toHexString(IR_TOKEN)) ? new BroadlinkHexParser(str) : new BroadlinkBase64Parser(str);
        return parser;
    }

    public static IrSequence parse(String str) throws InvalidArgumentException {
        BroadlinkParser parser = str.startsWith(Integer.toHexString(IR_TOKEN)) ? new BroadlinkHexParser(str) : new BroadlinkBase64Parser(str);
        return parser.toIrSequence();
    }

    public static IrSequence parse(byte[] data) throws InvalidArgumentException {
        BroadlinkParser parser = new BroadlinkParser(data);
        return parser.toIrSequence();
    }

    @SuppressWarnings({"UseOfSystemOutOrSystemErr", "CallToPrintStackTrace"})
    public static void main(String[] args) {
        try {
            IrSequence irSequence = parse(args[0]);
            System.out.println(irSequence);
        } catch (InvalidArgumentException ex) {
            ex.printStackTrace();
        }
    }

    protected byte[] data;

    protected BroadlinkParser(byte[] data) {
        super("");
        this.data = data;
    }

    @Override
    public IrSequence toIrSequence(Double dummyGap) throws OddSequenceLengthException {
        if (this.data == null || this.data.length == 0)
            return null;
        if (!isIr() && ! isRf433()) {
            logger.log(Level.FINER, "IR signal did not start with 0x{0} or 0x{1}", new Object[]{Integer.toHexString(IR_TOKEN), Integer.toHexString(RF_433_TOKEN)});
            return null;
        }
        int repeats = readdata(REPEAT_POS);
        int length = 256 * readdata(LENGTH_MSB_POS) + readdata(LENGTH_LSB_POS);
        List<Integer> durations = new ArrayList<>(length);

        try {
            int readIndex = DURATIONS_OFFSET;
            while (true) {
                int chunk = readdata(readIndex);
                readIndex++;
                if (chunk == 0) {
                    chunk = 256 * readdata(readIndex) + readdata(readIndex + 1);
                    readIndex += 2;
                    durations.add((int) Math.round(chunk * TICK));
                    if (chunk == IR_ENDING_TOKEN || chunk == RF_433_ENDING_TOKEN)
                        break;
                } else
                    durations.add((int) Math.round(chunk * TICK));
                if (readIndex >= length + DURATIONS_OFFSET)
                    break;
            }
        } catch (IndexOutOfBoundsException ex) {
            logger.log(Level.FINER, "IR data inconsistent");
            return null;
        }

        IrSequence irSequence = new IrSequence(durations);
        if (repeats > 0)
            irSequence.append(irSequence, repeats);
        return irSequence;
    }

    @Override
    public IrSignal toIrSignal(Double fallbackFrequency, Double dummyGap) throws OddSequenceLengthException {
        IrSequence irSequence = toIrSequence(dummyGap);
        return irSequence != null ? new IrSignal(irSequence, isIr() ? A_PRIOR_MODULATION_FREQUENCY : 0.0, null) : null;
    }

    private int readdata(int i) throws IndexOutOfBoundsException {
        byte d = data[i];
        return Byte.toUnsignedInt(d);
    }

    private boolean isIr() {
        return readdata(TOKEN_POS) == IR_TOKEN;
    }

    private boolean isRf433() {
        return readdata(TOKEN_POS) == RF_433_TOKEN;
    }

    @Override
    public String getName() {
        return "Broadlink";
    }
}
