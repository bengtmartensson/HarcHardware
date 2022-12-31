package org.harctoolbox.harchardware.ir;

import org.harctoolbox.ircore.IrSignal;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BroadlinkBase64ParserNGTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    public BroadlinkBase64ParserNGTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of toIrSignal method, of class BroadlinkParser.
     */
    @Test
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void testToIrSignal() throws Exception {
        System.out.println("toIrSignal");
        Double fallbackFrequency = 38123d;
        Double dummyGap = null;
        String command_on = "JgCQAAABJJQSExE4EhMTNhI4ETgROBITETgSExE4EhMTEhETERQROBE4EhMTNhI4ETgTEhMRExIRFBE4ERQRExMSETgSOBE4EQAFGwABJJURExI4ERMSOBE4ETgSOBETETkRExE5ERMRFBEUERMROBI4ERMSOBE4ETgSExEUERMSExE4EhMRFBETEjcSOBE4EQANBQAAAAAAAAAA=============";
        IrSignal expResult = new IrSignal(new int[]{+9589,-4860,+591,-624,+558,-1839,+591,-624,+624,-1773,+591,-1839,+558,-1839,+558,-1839,+591,-624,+558,-1839,+591,-624,+558,-1839,+591,-624,+624,-591,+558,-624,+558,-657,+558,-1839,+558,-1839,+591,-624,+624,-1773,+591,-1839,+558,-1839,+624,-591,+624,-558,+624,-591,+558,-657,+558,-1839,+558,-657,+558,-624,+624,-591,+558,-1839,+591,-1839,+558,-1839,+558,-42922,+9589,-4893,+558,-624,+591,-1839,+558,-624,+591,-1839,+558,-1839,+558,-1839,+591,-1839,+558,-624,+558,-1872,+558,-624,+558,-1872,+558,-624,+558,-657,+558,-657,+558,-624,+558,-1839,+591,-1839,+558,-624,+591,-1839,+558,-1839,+558,-1839,+591,-624,+558,-657,+558,-624,+591,-624,+558,-1839,+591,-624,+558,-657,+558,-624,+591,-1806,+591,-1839,+558,-1839,+558,-109456},
                136, 0, fallbackFrequency);
        BroadlinkParser instance = new BroadlinkBase64Parser(command_on);
        IrSignal result = instance.toIrSignal(fallbackFrequency, dummyGap);
        assertTrue(result.approximatelyEquals(expResult));
    }

    /**
     * Test of getName method, of class BroadlinkParser.
     */
    @Test
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void testGetName() {
        System.out.println("getName");
        BroadlinkParser instance = new BroadlinkHexParser("fgfgfg");
        String expResult = "BroadlinkHex";
        String result = instance.getName();
        assertEquals(result, expResult);
    }
}
