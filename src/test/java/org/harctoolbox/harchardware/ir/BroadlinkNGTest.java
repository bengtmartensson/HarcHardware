package org.harctoolbox.harchardware.ir;

import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.OddSequenceLengthException;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BroadlinkNGTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    public BroadlinkNGTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of broadlinkHexString method, of class BroadlinkHexParser.
     */
    @Test
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void testBroadlinkHexString() throws OddSequenceLengthException {
        System.out.println("broadlinkHexString");
        ModulatedIrSequence irSequence = new ModulatedIrSequence(new int[] {9721,4827,624,1773,657,558,657,558,624,1806,624,558,657,558,657,1773,624,591,624,591,624,1773,657,1773,624,591,624,1773,657,1773,657,558,624,1773,657,1773,657,1773,624,591,624,1773,657,1773,657,558,624,591,624,558,657,558,657,558,657,1773,624,591,624,558,657,1773,657,1773,624,1773,657,42955,9721,2364,657,100000},
                ModulatedIrSequence.DEFAULT_FREQUENCY);
        int count = 42;
        String expResult = "2629540000012893133614111411133713111411143613121312133614361312133614361411133614361436131213361436141113121311141114111436131213111436143613361400051C0001284814000D05";
        String result = Broadlink.broadlinkHexString(irSequence, count);
        assertEquals(result, expResult);
    }

//    /**
//     * Test of isValid method, of class Broadlink.
//     */
//    @Test
//    public void testIsValid() {
//        System.out.println("isValid");
//        Broadlink instance = new Broadlink();
//        boolean expResult = false;
//        boolean result = instance.isValid();
//        assertEquals(result, expResult);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

//    /**
//     * Test of open method, of class Broadlink.
//     */
//    @Test
//    public void testOpen() throws Exception {
//        System.out.println("open");
//        Broadlink instance = new Broadlink();
//        instance.open();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

//    /**
//     * Test of sendIr method, of class Broadlink.
//     */
//    @Test
//    public void testSendIr_IrSequence_int() throws Exception {
//        System.out.println("sendIr");
//        IrSequence irSequence = null;
//        int count = 0;
//        Broadlink instance = new Broadlink();
//        boolean expResult = false;
//        boolean result = instance.sendIr(irSequence, count);
//        assertEquals(result, expResult);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

//    /**
//     * Test of sendIr method, of class Broadlink.
//     */
//    @Test
//    public void testSendIr_IrSignal_int() throws Exception {
//        System.out.println("sendIr");
//        IrSignal irSignal = null;
//        int count = 0;
//        Broadlink instance = new Broadlink();
//        boolean expResult = false;
//        boolean result = instance.sendIr(irSignal, count);
//        assertEquals(result, expResult);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

//    /**
//     * Test of sendIr method, of class Broadlink.
//     */
//    @Test
//    public void testSendIr_3args() throws Exception {
//        System.out.println("sendIr");
//        IrSignal irSignal = null;
//        int count = 0;
//        Transmitter transmitter = null;
//        Broadlink instance = new Broadlink();
//        boolean expResult = false;
//        boolean result = instance.sendIr(irSignal, count, transmitter);
//        assertEquals(result, expResult);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

//    /**
//     * Test of receive method, of class Broadlink.
//     */
//    @Test
//    public void testReceive() throws Exception {
//        System.out.println("receive");
//        Broadlink instance = new Broadlink();
//        IrSequence expResult = null;
//        IrSequence result = instance.receive();
//        assertEquals(result, expResult);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
}
