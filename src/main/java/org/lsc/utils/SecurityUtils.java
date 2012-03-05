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
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.utils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;
import org.lsc.configuration.LscConfiguration;
import org.lsc.utils.security.SymmetricEncryption;

/**
 * Provides some methods to encrypt, decrypt or hash data.
 * @see org.lsc.utils.security.SymmetricEncryption
 */
public class SecurityUtils {

	public static final String HASH_MD5 = "MD5";
	public static final String HASH_SHA1 = "SHA1";
	public static final String HASH_SHA256 = "SHA256";
	public static final String HASH_SHA512 = "SHA512";

	private static SymmetricEncryption encryptionInstance;
	
	// Utility class
	private SecurityUtils() {}
	
	private static SymmetricEncryption getEncryptionInstance() throws GeneralSecurityException {
		if(encryptionInstance == null) {
            if(LscConfiguration.getSecurity() == null) {
                throw new RuntimeException("lsc>security node of the LSC configuration cannot be null !");
            } else if(LscConfiguration.getSecurity().getEncryption() == null) {
                throw new RuntimeException("lsc>security>encryption node of the LSC configuration cannot be null !");
            }
            encryptionInstance = new SymmetricEncryption(LscConfiguration.getSecurity().getEncryption());
		}
		return encryptionInstance;
	}
	
	/**
	 * Decrypt a base64 value.
	 * @param value The value
	 * @return The decrypted String
	 * @throws java.security.GeneralSecurityException
	 * @throws java.io.IOException
	 */
	public static String decrypt(String value) throws GeneralSecurityException, IOException {
		SymmetricEncryption se = getEncryptionInstance();
		if (!se.initialize()) {
			throw new RuntimeException("SecurityUtils: Error initializing SymmetricEncryption!");
		}
		return new String(se.decrypt(new Base64().decode(value.getBytes())));
	}

	/**
	 * Encrypt a value.
	 * @param value The value
	 * @return The encrypted String, base64 encoded
	 * @throws java.security.GeneralSecurityException
	 * @throws java.io.IOException
	 */
	public static String encrypt(String value) throws GeneralSecurityException, IOException {
		SymmetricEncryption se = getEncryptionInstance();
		if (!se.initialize()) {
			throw new RuntimeException("SecurityUtils: Error initializing SymmetricEncryption!");
		}
		return new String(new Base64().encode(se.encrypt(value.getBytes())));
	}

	/**
	 * Hash a value within a supported hash type.
	 * @param type A valid hash type: SecurityUtils.HASH_MD5, SecurityUtils.HASH_SHA1, SecurityUtils.HASH_SHA256 or SecurityUtils.HASH_SHA512
	 * @param value A value to hash
	 * @return A valid base64 encoded hash
	 * @throws java.security.NoSuchAlgorithmException
	 */
	public static String hash(String type, String value) throws NoSuchAlgorithmException {
		byte data[] = value.getBytes();
		byte hash[] = MessageDigest.getInstance(type).digest(data);
		return new String(new Base64().encode(hash));
	}
}
