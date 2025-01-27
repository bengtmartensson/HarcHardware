package org.harctoolbox.harchardware.ir;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.ircore.IrSequence;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.OddSequenceLengthException;
import org.harctoolbox.ircore.Pronto;
import org.harctoolbox.irp.IrpDatabase;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BroadlinkNGTest {

    public BroadlinkNGTest() {
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

    /**
     * Test of scanDevices method, of class Broadlink.
     */
    @Test
    public void testScanDevices_0args() throws Exception {
        System.out.println("scanDevices");
        //Map expResult = null;
        Map<InetAddress,Broadlink> result = Broadlink.scanDevices();
        for (Map.Entry<InetAddress, Broadlink> e : result.entrySet())
            System.out.println(e.getKey().getHostName() + ": \t" + e.getValue());
    }
//
//    /**
//     * Test of broadlinkHexString method, of class Broadlink.
//     */
//    @Test
//    public void testBroadlinkHexString_byteArr() {
//        System.out.println("broadlinkHexString");
//        byte[] data = null;
//        String expResult = "";
//        String result = Broadlink.broadlinkHexString(data);
//        assertEquals(result, expResult);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of broadlinkHexString method, of class Broadlink.
//     */
//    @Test
//    public void testBroadlinkHexString_ModulatedIrSequence_int() {
//        System.out.println("broadlinkHexString");
//        ModulatedIrSequence irSequence = null;
//        int count = 0;
//        String expResult = "";
//        String result = Broadlink.broadlinkHexString(irSequence, count);
//        assertEquals(result, expResult);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of broadlinkBase64String method, of class Broadlink.
//     */
//    @Test
//    public void testBroadlinkBase64String() {
//        System.out.println("broadlinkBase64String");
//        ModulatedIrSequence irSequence = null;
//        int count = 0;
//        String expResult = "";
//        String result = Broadlink.broadlinkBase64String(irSequence, count);
//        assertEquals(result, expResult);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of newBroadlink method, of class Broadlink.
//     */
//    @Test
//    public void testNewBroadlink_String_Integer() throws Exception {
//        System.out.println("newBroadlink");
//        String hostname = "";
//        Integer timeout = null;
//        Broadlink expResult = null;
//        Broadlink result = Broadlink.newBroadlink(hostname, timeout);
//        assertEquals(result, expResult);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of newBroadlink method, of class Broadlink.
//     */
//    @Test
//    public void testNewBroadlink_String() throws Exception {
//        System.out.println("newBroadlink");
//        String hostname = "";
//        Broadlink expResult = null;
//        Broadlink result = Broadlink.newBroadlink(hostname);
//        assertEquals(result, expResult);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of newBroadlink method, of class Broadlink.
//     */
//    @Test
//    public void testNewBroadlink_0args() throws Exception {
//        System.out.println("newBroadlink");
//        Broadlink expResult = null;
//        Broadlink result = Broadlink.newBroadlink();
//        assertEquals(result, expResult);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
    /**
     * Test of toString method, of class Broadlink.
     */
    @Test
    public void testToString() throws IOException {
        System.out.println("toString");
        Broadlink instance = new Broadlink();
        String expResult = "Type: 272a Host: 192.168.1.41 Mac: 34:ea:34:f4:4c:29";
        String result = instance.toString();
        assertEquals(result, expResult);
    }

    /**
     * Test of sendIr method, of class Broadlink.
     */
    @Test(enabled = false)
    public void testSendIr_IrSignal_int() throws Exception {
        System.out.println("sendIr");
        IrSignal irSignal = Pronto.parse("0000 0068 0000 000D 0060 0018 0030 0018 0018 0018 0030 0018 0018 0018 0030 0018 0018 0018 0018 0018 0030 0018 0018 0018 0018 0018 0018 0018 0018 0408");
        int count = 3;
        boolean result;
        try (Broadlink instance = new Broadlink()) {
            instance.open();
            result = instance.sendIr(irSignal, count);
        }
        assertTrue(result);
    }

    /**
     * Test of receive method, of class Broadlink.
     */
    @Test(enabled = false)
    public void testReceive() throws Exception {
        System.out.println("receive");
        IrSequence result;
        try (Broadlink instance = new Broadlink()) {
            instance.open();
            result = instance.receive();
        }
        System.out.println(result);
    }

    /**
     * Test of getTemperature method, of class Broadlink.
     */
    @Test
    public void testGetTemperature() throws Exception {
        System.out.println("getTemperature");
        double expResult;
        double result;
        try (Broadlink instance = new Broadlink()) {
            instance.open();
            expResult = 23.0;
            result = instance.getTemperature();
        }
        assertEquals(result, expResult, 10.0);
    }
}
