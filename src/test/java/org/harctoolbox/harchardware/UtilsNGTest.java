/*
Copyright (C) 2017 Bengt Martensson.

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

package org.harctoolbox.harchardware;

import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.testng.Assert.assertFalse;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class UtilsNGTest {

    public UtilsNGTest() {
    }

    @BeforeClass
    public void setUpClass() throws Exception {
    }

    @AfterClass
    public void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of getHostname method, of class Utils.
     */
    @Test(enabled = true)
    public void testGetHostname() {
        System.out.println("getHostname");
        String result = Utils.getHostname();
        System.out.println(result);
        assertFalse(result.isEmpty());
    }

    /**
     * Test of getMacAddress method, of class Utils.
     * @throws java.net.UnknownHostException
     */
    @Test
    public void testGetMacAddress() throws UnknownHostException {
        System.out.println("getMacAddress");
        InetAddress address = InetAddress.getByName(Utils.getHostname());
        String result = Utils.getMacAddress(address);
        System.out.println(result);
        assertFalse(result.isEmpty());
    }
}
