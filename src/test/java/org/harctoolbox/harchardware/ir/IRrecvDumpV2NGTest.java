/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.harctoolbox.harchardware.ir;

import org.harctoolbox.ircore.IrSequence;
import org.harctoolbox.ircore.OddSequenceLengthException;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author bengt
 */
public class IRrecvDumpV2NGTest {

    private static final String testStr
            = "unsigned int  rawData[67] = {8950,4450, 600,500, 600,500, 600,550, 600,500, 600,500, 600,550, 550,550, 600,1650, 550,1650, 600,1650, 600,1650, 600,1600, 600,1650, 600,1650, 550,1650, 600,1650, 600,1650, 550,1650, 600,1650, 600,1650, 550,1650, 600,550, 550,550, 600,500, 600,500, 600,550, 600,500, 600,500, 600,550, 600,1600, 600,1650, 600,1650, 550};  // NEC 1FFF807";
    private static final String expected = "[8950,4450,600,500,600,500,600,550,600,500,600,500,600,550,550,550,600,1650,550,1650,600,1650,600,1650,600,1600,600,1650,600,1650,550,1650,600,1650,600,1650,550,1650,600,1650,600,1650,550,1650,600,550,550,550,600,500,600,500,600,550,600,500,600,500,600,550,600,1600,600,1650,600,1650,550,20000]";


    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    public IRrecvDumpV2NGTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of parse method, of class IRrecvDumpV2.
     * @throws org.harctoolbox.ircore.OddSequenceLengthException
     */
    @Test
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void testParse() throws OddSequenceLengthException {
        System.out.println("parse");
        IrSequence result = IRrecvDumpV2.parse(expected);
        assertNull(result);

        result = IRrecvDumpV2.parse(testStr);
        assertEquals(result.toString(), expected);
        // TODO review the generated test code and remove the default call to fail.

    }

}
