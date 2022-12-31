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

import java.util.Base64;

/**
 *
 */
final public class BroadlinkBase64Parser extends BroadlinkParser {

    private static String nukeTrailingEquals(String string) {
        int idx = string.indexOf('=');
        return idx == -1 ? string : string.substring(0, idx);
    }

    public BroadlinkBase64Parser(String str) {
        super(nukeTrailingEquals(str));
    }

    @Override
    void setup() throws IllegalArgumentException {
        data = Base64.getDecoder().decode(getSource());
    }

    @Override
    public String getName() {
        return "BroadlinkBase64";
    }
}
