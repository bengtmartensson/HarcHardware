package org.harctoolbox.harchardware.ir;

import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.Pronto;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GlobalCacheNGTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    public GlobalCacheNGTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of parse method, of class GlobalCache.
     * @throws org.harctoolbox.ircore.Pronto.NonProntoFormatException
     * @throws org.harctoolbox.ircore.InvalidArgumentException
     */
    @Test
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void testParse() throws Pronto.NonProntoFormatException, InvalidArgumentException {
        System.out.println("parse");
        String gcString = "sendir,1:1,1,38381,1,69,347,173,22,22,22,22,22,65,22,65,22,22,22,22,22,22,22,22,22,22,22,65,22,22,22,22,22,22,22,65,22,22,22,22,22,22,22,22,22,22,22,65,22,65,22,65,22,22,22,22,22,65,22,65,22,65,22,22,22,22,22,22,22,65,22,65,22,1700,347,87,22,3692";
        IrSignal expResult = Pronto.parse("0000 006C 0022 0002 015B 00AD 0016 0016 0016 0016 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 06A4 015B 0057 0016 0E6C");
        IrSignal result = GlobalCache.parse(gcString);
        assertTrue(result.approximatelyEquals(expResult));
    }
}
