package org.harctoolbox.harchardware.ir;

import org.harctoolbox.ircore.IrSignal;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BroadlinkHexParserNGTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    public BroadlinkHexParserNGTest() {
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
        BroadlinkHexParser instance = new BroadlinkHexParser("2600580000012793133614111411143613121311143614111312133614361411133713361411143613361436141113371336141114111312131213111436131213121336143613371300051c0001284913000c4d0001284913000d05");
        IrSignal expResult = new IrSignal(new int[]{+9688, -4827, +624, -1773, +657, -558, +657, -558, +657, -1773, +624, -591, +624, -558, +657, -1773, +657, -558, +624, -591, +624, -1773, +657, -1773, +657, -558, +624, -1806, +624, -1773, +657, -558, +657, -1773, +624, -1773, +657, -1773, +657, -558, +624, -1806, +624, -1773, +657, -558, +657, -558, +624, -591, +624, -591, +624, -558, +657, -1773, +624, -591, +624, -591, +624, -1773, +657, -1773, +624, -1806, +624, -42955, +9721, -2397, +624, -103413, +9721, -2397, +624, -109456},
                76, 0, fallbackFrequency);
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
        BroadlinkParser instance = new BroadlinkHexParser("2600580000012793133614111411143613121311143614111312133614361411133713361411143613361436141113371336141114111312131213111436131213121336143613371300051c0001284913000c4d0001284913000d05");
        String expResult = "BroadlinkHex";
        String result = instance.getName();
        assertEquals(result, expResult);
    }
}
