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
import java.util.Base64;
import java.util.List;
import org.harctoolbox.ircore.IrSequence;

/**
 *
 */
public final class Broadlink {
    public final static String HEX_STRING_FORMAT = "%02X";

    public final static double TICK = 32.84d;
    public final static int IR_TOKEN = 0x26;
    public final static int RF_433_TOKEN = 0xB2; // Not yet used
    public final static int RF_315_TOKEN = 0xD7; // Not yet used
    public final static int IR_ENDING_TOKEN = 0x0d05;
    public final static int RF_433_ENDING_TOKEN = 0x0181; // Not yet used
    public final static int RF_315_ENDING_TOKEN = 0xFFFF; // FIXME
    public final static int TOKEN_POS = 0;
    public final static int REPEAT_POS = 1;
    public final static int LENGTH_LSB_POS = 2;
    public final static int LENGTH_MSB_POS = 3;
    public final static int DURATIONS_OFFSET = 4;
    public final static double A_PRIOR_MODULATION_FREQUENCY = 38000d;

    public static String broadlinkHexString(IrSequence irSequence, int count) {
        List<Integer> irData = broadlinkList(irSequence, count);
        StringBuilder sb = new StringBuilder(4 * irData.size());
        irData.forEach(chunk -> {
            sb.append(String.format(HEX_STRING_FORMAT, chunk));
        });
        return sb.toString();
    }

    public static String broadlinkBase64String(IrSequence irSequence, int count) {
        byte[] bytearray = broadlinkData(irSequence, count);
        return Base64.getEncoder().encodeToString(bytearray);
    }

    private static List<Integer> broadlinkList(IrSequence irSequence, int count) {
        List<Integer> list = new ArrayList<>(2 * irSequence.getLength());
        list.add(IR_TOKEN);
        list.add(count - 1);
        list.add(0);
        list.add(0);
        for (int i = 0; i < irSequence.getLength() - 1; i++) { // ignoring final gap ...
            double duration = irSequence.get(i);
            int noTicks = (int) Math.round(duration / TICK);
            addEntry(list, noTicks);
        }
        addEntry(list, IR_ENDING_TOKEN); // ... and replacing it with the Broadlink ending token
        list.set(LENGTH_MSB_POS, list.size() / 256);
        list.set(LENGTH_LSB_POS, list.size() % 256);
        return list;
    }

    private static byte[] broadlinkData(IrSequence irSequence, int count) {
        List<Integer> list = broadlinkList(irSequence, count);
        byte[] data = new byte[list.size()];
        for (int i = 0; i < data.length; i++)
            data[i] = list.get(i).byteValue();
        return data;
    }

    private static void addEntry(List<Integer> list, int noTicks) {
        if (noTicks > 255) {
            list.add(0);
            list.add(noTicks / 256);
        }
        list.add(noTicks % 256);
    }

    private Broadlink() {
    }
}
