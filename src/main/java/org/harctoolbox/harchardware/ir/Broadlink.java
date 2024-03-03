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

/**
 * Just a skeleton for the parsing classes. To be fixed later...
 */
public final class Broadlink /*implements IHarcHardware, IRawIrSender, IReceive /* NOT ICapture */ {

    public static final String DEFAULT_HOST = "broadlink";
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
    public final static int MIN_ARCTECH_REPEATS = 6;
    public final static double A_PRIOR_MODULATION_FREQUENCY = 38000d;

    private Broadlink() {
    }
}
