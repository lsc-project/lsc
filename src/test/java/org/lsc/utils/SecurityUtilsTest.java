/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 *
 * Copyright (c) 2008 - 2011 LSC Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:

 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of the LSC Project nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *                  ==LICENSE NOTICE==
 *
 *               (c) 2008 - 2011 LSC Project
 *         Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 *         Thomas Chemineau &lt;thomas@lsc-project.org&gt;
 *         Jonathan Clarke &lt;jon@lsc-project.org&gt;
 *         Remy-Christophe Schermesser &lt;rcs@lsc-project.org&gt;
 ****************************************************************************
 */
package org.lsc.utils;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lsc.configuration.LscConfiguration;
import org.lsc.utils.security.SymmetricEncryption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test security tools.
 */
public class SecurityUtilsTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityUtilsTest.class);
	
	@Before
	public void setUp() {
		LscConfiguration.reset();
	}
    @Test
    public final void testSymmetricEncryption() throws GeneralSecurityException, IOException {
    	try {
	        //
	        // First generate a random symmetric key. We could use it then to
	        // do all encryption operations.
	        //
	        String tmpKeyPath = new File(this.getClass().getClassLoader().getResource("").getFile(), "lsc-key.tmp").getAbsolutePath();
	        SymmetricEncryption se = new SymmetricEncryption();
	        assertTrue(se.generateRandomKeyFile(tmpKeyPath, "AES", 128));
	
	        LscConfiguration.getSecurity().getEncryption().setKeyfile(tmpKeyPath);
	
	        //
	        // Now, the test consist to encrypt a random value. Then, we compare the
	        // decrypted value with the initial one, they should be equal.
	        //
	        String chars = "abcdefghijklmonpqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	        Random r = new Random();
	        char[] buf = new char[20];
	        for (int i = 0; i < 20; i++) {
	            buf[i] = chars.charAt(r.nextInt(chars.length()));
	        }
	        String randomValue = new String(buf);
	        String encryptedValue = SecurityUtils.encrypt(randomValue);
	        String decryptedValue = SecurityUtils.decrypt(encryptedValue);
	        assertTrue(randomValue.equals(decryptedValue));
    	} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			throw new RuntimeException(e);
		}
    }

    @Test
    public final void testHash() throws NoSuchAlgorithmException {
        String simpleValue = "lsc-project.org";
        String hashedValueMD5 = "9xGo7EH8D2X+OOqXw1eIxQ==";
        String hashedValueSHA1 = "YVTOIPfeXwxFluZBGrS+V5lARgc=";

        // MD5
        String result = SecurityUtils.hash(SecurityUtils.HASH_MD5, simpleValue);
        assertTrue(result.equals(hashedValueMD5));

        // SHA-1
        result = SecurityUtils.hash(SecurityUtils.HASH_SHA1, simpleValue);
        assertTrue(result.equals(hashedValueSHA1));
    }

    @Test
    public final void testcomputeSambaPasswords() {
        String password = "lsc-project";
        String passwordSambaLM = "421C32AAE6A89FEF0DCD0BFE45023337";
        String passwordSambaNT = "433EFC29BCD88C3888E797704BEF3AE1";
        //
        // LM
        //
        try {
            String result = SecurityUtils.computeSambaLMPassword(password);
            Assert.assertEquals(result, passwordSambaLM);
        } catch (Exception ex) {
            assertTrue(false);
        }
        //
        // NT
        //
        try {
            String result = SecurityUtils.computeSambaNTPassword(password);
            Assert.assertEquals(result, passwordSambaNT);
        } catch (Exception ex) {
            assertTrue(false);
        }
    }
}
