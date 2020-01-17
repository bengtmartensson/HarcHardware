/*
Copyright (C) 2019 Bengt Martensson.

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

import gnu.io.CommDriver;
import java.io.File;

/**
 * Silly test class for loading a librxtxSerial.so-version. Produces noise on
 * stderr if locking is broken.
 *
 * Better would be a C program that tests the locking.
 * Even better would be to finally retire RXTX...
 */
public class TestRxtx {

    @SuppressWarnings("CallToPrintStackTrace")
    public static void main(String[] args) {
        try {
            String filename = new File(args[0]).getAbsolutePath();
            System.load(filename);
            CommDriver RXTXDriver = (CommDriver) Class.forName("gnu.io.RXTXCommDriver").newInstance();
            RXTXDriver.initialize();
            System.exit(0);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
