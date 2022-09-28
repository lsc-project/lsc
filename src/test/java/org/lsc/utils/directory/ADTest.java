package org.lsc.utils.directory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.apache.commons.io.HexDump;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

/**
 * Test class to check org.lsc.utils.directory.AD implementation
 * 
 *
 */
public class ADTest {


    	@Before
	public void setup() {
            // nothing to setup
	}

    	@Test
	public void testGetUnicodePwd() throws Exception {
            String inputPassword = "1aDhgtryjhddç";
            String quotedPassword =  "\"" + inputPassword + "\"";

            System.out.println("Computed right UTF16-LE");
            byte[] correct=quotedPassword.getBytes("UTF-16LE");
            HexDump.dump(correct,0,System.out,0);
           

            // for reference
            byte[] right = new byte[] {34,0,49,0,97,0,68,0,104,0,103,0,116,0,114,0,121,0,106,0,104,0,100,0,100,0,(byte) 0xe7,0,34,0};
            // byte[] wrong = new byte[] {34,0,49,0,97,0,68,0,104,0,103,0,116,0,114,0,121,0,106,0,104,0,100,0,100,0,-17,-65,-67,0,34,0};

            Assert.assertArrayEquals("INTERNAL failure wrong encoding", correct, right);

            String outputPassword = AD.getUnicodePwd(inputPassword);
            
            System.out.println("Hardcoded right UTF16-LE");
            HexDump.dump(right,0,System.out,0);

            byte[] pwdArray=outputPassword.getBytes("ISO-8859-1");
            System.out.println("current / via ISO-8859-1");
            HexDump.dump(pwdArray,0,System.out,0);

            // use default encoding
            pwdArray=outputPassword.getBytes();
            System.out.println("current / via default encoding");
            HexDump.dump(pwdArray,0,System.out,0);
            
            Assert.assertArrayEquals("wrong encoding", pwdArray, right);
            System.out.println("");
        }	

}
