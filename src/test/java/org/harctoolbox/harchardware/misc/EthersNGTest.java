package org.harctoolbox.harchardware.misc;

import java.io.File;
import java.io.IOException;
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
public class EthersNGTest {
    
    private static File ethers = new File("src/test/config/ethers"); 
    public EthersNGTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of getEtherAddress method, of class Ethers.
     */
    @Test
    public void testGetEtherAddress_String_File() throws Exception {
        System.out.println("getEtherAddress");
        scrutinize("rpi5", "aa:bb:cc:dd:ee:ff");
        scrutinize("rpi5", "aa:bb:cc:dd:ee:ff");
        try {
            scrutinize("nonexisting", null);
            fail();
        } catch (Ethers.MacAddressNotFound e) {
        }
        try {
            scrutinize("nonexisting", null);
            fail();
        } catch (Ethers.MacAddressNotFound e) {
        }
    }

    private void scrutinize(String hostname, String expResult) throws Ethers.MacAddressNotFound, IOException {
        String result = Ethers.getEtherAddress(hostname, ethers);
        assertEquals(result, expResult);
    }
}
