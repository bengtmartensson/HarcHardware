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
 *
 */
final public class BroadlinkHexParser extends BroadlinkParser {

    private static byte[] digest(String str) {
        try {
            int length = str.length() / 2;
            byte[] array = new byte[length];
            for (int i = 0; i < length; i++) {
                String s = str.substring(2 * i, 2 * i + 2);
                int x = Integer.parseInt(s, 16);
                array[i] = (byte) x;
            }
            return array;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public BroadlinkHexParser(String str) {
        super(digest(str));
    }

    @Override
    public String getName() {
        return "BroadlinkHex";
    }
}
