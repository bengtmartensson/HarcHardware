package org.harctoolbox.harchardware.ir;

import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.MultiParser;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class GlobalCacheParserNGTest {

    private static final String sendir = "sendir,1:1,1,38400,1,69,347,173,22,22,22,22,22,65,22,65,22,22,22,22,22,22,22,22,22,22,22,65,22,22,22,22,22,22,22,65,22,22,22,22,22,22,22,22,22,22,22,65,22,65,22,65,22,22,22,22,22,65,22,65,22,65,22,22,22,22,22,22,22,65,22,65,22,1700,347,87,22,3692";
    private static final String sendirSilly = "sendir,1:1,1,38400,1,999,347,173,22,22,22,22,22,65,22,65,22,22,22,22,22,22,22,22,22,22,22,65,22,22,22,22,22,22,22,65,22,22,22,22,22,22,22,22,22,22,22,65,22,65,22,65,22,22,22,22,22,65,22,65,22,65,22,22,22,22,22,22,22,65,22,65,22,1700,347,87,22,3692";
    private static final String pronto = "0000 006C 0022 0002 015B 00AD 0016 0016 0016 0016 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 06A4 015B 0057 0016 0E6C";

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    public GlobalCacheParserNGTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of toIrSignal method, of class GlobalCacheParser.
     * @throws org.harctoolbox.ircore.InvalidArgumentException
     */
    @Test
    @SuppressWarnings("UnusedAssignment")
    public void testToIrSignal() throws InvalidArgumentException {
        System.out.println("toIrSignal");
        Double fallbackFrequency = null;
        Double dummyGap = null;
        MultiParser instance = GlobalCacheParser.newParser(sendir);
        IrSignal result = instance.toIrSignal(fallbackFrequency, dummyGap);
        assertEquals(result.getRepeatLength(), 4);
        instance = GlobalCacheParser.newParser(sendirSilly);
        try {
            result = instance.toIrSignal(fallbackFrequency, dummyGap);
            fail();
        } catch (InvalidArgumentException | NumberFormatException ex) {
            System.out.println(ex.getLocalizedMessage());
        }
        instance = GlobalCacheParser.newParser(pronto);
        result = instance.toIrSignal(fallbackFrequency, dummyGap);
        assertEquals(result.getRepeatLength(), 4);
    }

    /**
     * Test of toIrSignal method, of class GlobalCacheParser.
     * @throws org.harctoolbox.ircore.InvalidArgumentException
     */
    @Test
    public void testToModulatedIrSequence() throws InvalidArgumentException {
        System.out.println("toIrSignal");
        Double fallbackFrequency = null;
        Double dummyGap = null;
        MultiParser instance = GlobalCacheParser.newParser(sendir);
        ModulatedIrSequence result = instance.toModulatedIrSequence(fallbackFrequency, dummyGap);
        assertEquals(result.getLength(), 72);
     }
}
