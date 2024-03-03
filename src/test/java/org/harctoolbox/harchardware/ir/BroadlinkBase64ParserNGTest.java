package org.harctoolbox.harchardware.ir;

import org.harctoolbox.ircore.IrSequence;
import org.harctoolbox.ircore.IrSignal;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BroadlinkBase64ParserNGTest {

    public BroadlinkBase64ParserNGTest() {
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
     * Test of toIrSignal method, of class BroadlinkParser.
     * @throws Exception
     */
    @Test
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void testToIrSignal() throws Exception {
        System.out.println("toIrSignal");
        String in = "JgBYAAABJ5MUNhMSExEUNhQRExITNhQRFBETNxM2FBEUNhM3ExEUNhM3EzYUERQ2EzYUERQRFBETEhMSEzYUERQREzYUNhQ2EwAFHQABJ0kTAAxOAAEnSRQADQU=";
        IrSequence irSequence = new IrSequence(new int[]{+9688,-4827,+618,-1848,+618,-618,+618,-618,+618,-1848,+618,-618,+618,-618,+618,-1848,+618,-618,+618,-618,+618,-1848,+618,-1848,+618,-618,+618,-1848,+618,-1848,+618,-618,+618,-1848,+618,-1848,+618,-1848,+618,-618,+618,-1848,+618,-1848,+618,-618,+618,-618,+618,-618,+618,-618,+618,-618,+618,-1848,+618,-618,+618,-618,+618,-1848,+618,-1848,+618,-1848,+618,-42988,+9688,-1848,+618,-106451,+9688,-1848,+618,-106451});
        IrSignal expResult = new IrSignal(irSequence, Broadlink.A_PRIOR_MODULATION_FREQUENCY, null);
        BroadlinkParser instance = new BroadlinkBase64Parser(in);
        IrSignal result = instance.toIrSignal();
        assertTrue(result.approximatelyEquals(expResult));
    }

    /**
     * Test of getName method, of class BroadlinkParser.
     */
    @Test
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void testGetName() {
        System.out.println("getName");
        BroadlinkParser instance = new BroadlinkBase64Parser("fgfgfg");
        String expResult = "BroadlinkBase64";
        String result = instance.getName();
        assertEquals(result, expResult);
    }
}
