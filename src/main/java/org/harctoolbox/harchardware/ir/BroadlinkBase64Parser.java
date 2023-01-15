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

import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
final public class BroadlinkBase64Parser extends BroadlinkParser {

    private final static Logger logger = Logger.getLogger(BroadlinkBase64Parser.class.getName());

    private static String nukeTrailingEquals(String string) {
        int idx = string.indexOf('=');
        return idx == -1 ? string : string.substring(0, idx);
    }

    private static byte[] digest(String str) {
        try {
            return Base64.getDecoder().decode(str);
        } catch (IllegalArgumentException ex) {
            logger.log(Level.FINER, "{0}", ex.getLocalizedMessage());
            return null;
        }
    }

    public BroadlinkBase64Parser(String str) {
        super(digest(nukeTrailingEquals(str)));
    }

    @Override
    public String getName() {
        return "BroadlinkBase64";
    }
}
