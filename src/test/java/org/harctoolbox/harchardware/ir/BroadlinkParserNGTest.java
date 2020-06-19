package org.harctoolbox.harchardware.ir;

import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.MultiParser;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BroadlinkParserNGTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    public BroadlinkParserNGTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

//    /**
//     * Test of newParser method, of class BroadlinkParser.
//     */
//    @Test
//    public void testNewParser() {
//        System.out.println("newParser");
//        String source = "";
//        MultiParser expResult = null;
//        MultiParser result = BroadlinkParser.newParser(source);
//        assertEquals(result, expResult);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of toIrSignal method, of class BroadlinkParser.
     */
    @Test
    public void testToIrSignal() throws Exception {
        System.out.println("toIrSignal");
        //# Command #1: power_toggle; Protocol: nec1, Parameters: S=255 D=128 F=10
        //   IR_TOKEN,
        IrSignal expected = new IrSignal(new int[]{9024, 4512, 564, 564, 564, 564, 564, 564, 564, 564, 564, 564, 564, 564, 564, 564, 564, 1692, 564, 1692, 564, 1692, 564, 1692, 564, 1692, 564, 1692, 564, 1692, 564, 1692, 564, 1692, 564, 564, 564, 1692, 564, 564, 564, 1692, 564, 564, 564, 564, 564, 564, 564, 564, 564, 1692, 564, 564, 564, 1692, 564, 564, 564, 1692, 564, 1692, 564, 1692, 564, 1692, 564, 38628,
            9024, 2256, 564, 96156}, 68, 4, 38400);
        String string = "260048000001289412121212121212121212121212121237123712371237123712371237123712371212123712121237121212121212121212371212123712121237123712371237120004F20001284A12000D05";
        // 'JgBIAAABKJQSEhISEhISEhISEhISEhI3EjcSNxI3EjcSNxI3EjcSNxISEjcSEhI3EhISEhISEhISNxISEjcSEhI3EjcSNxI3EgAE8gABKEoSAA0F'
        Double fallbackFrequency = 38400d;
        Double dummyGap = null;
        BroadlinkParser instance = new BroadlinkParser(string);
        //IrSignal expResult = null;
        IrSignal result = instance.toIrSignal(fallbackFrequency, dummyGap);
        //assertTrue(expected.approximatelyEquals(result));
    }
}
