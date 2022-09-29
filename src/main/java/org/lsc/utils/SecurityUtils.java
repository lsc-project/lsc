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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
	
	/**
	 * Encrypt a password for samba, LMPassword version.
	 * @param password the password to encrypt
	 * @return the LMPassword
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	public static String computeSambaLMPassword(String password) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		byte[] oemPwd = password.toUpperCase().getBytes("US-ASCII");
		int pwdLength = Math.min(oemPwd.length, 14);

		byte[] p14 = new byte[14];
		System.arraycopy(oemPwd, 0, p14, 0, pwdLength);
		byte[] constantKGS = "KGS!@#$%".getBytes("US-ASCII");
		byte[] lmHash = new byte[16];

		Key lowKey = createDESKey(p14, 0);
		Key highKey = createDESKey(p14, 7);
		Cipher des = Cipher.getInstance("DES/ECB/NoPadding");

		des.init(Cipher.ENCRYPT_MODE, lowKey);
		byte[] lowHash = des.doFinal(constantKGS);
		
		des.init(Cipher.ENCRYPT_MODE, highKey);
		byte[] highHash = des.doFinal(constantKGS);
		
		System.arraycopy(lowHash, 0, lmHash, 0, 8);
		System.arraycopy(highHash, 0, lmHash, 8, 8);

		return bytesToHexString(lmHash);
	}
	
	/**
	 * Encrypt a password for samba, NTPassword version.
	 * @param password the password to encrypt
	 * @return the NTPassword
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchProviderException 
	 * @throws NoSuchAlgorithmException 
	 */
	public static String computeSambaNTPassword(String password) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchProviderException {
		byte[] unicodePassword = password.getBytes("UnicodeLittleUnmarked");
		
		MessageDigest md4 = MessageDigest.getInstance("MD4", new BouncyCastleProvider());
		byte[] ntHash = md4.digest(unicodePassword);
		
		return bytesToHexString(ntHash);
	}
	
	/**
	 * Convert a byte array to an hexadecimal string.
	 * @param bytes to convert
	 * @return hexadecimal string
	 */
	private static final String bytesToHexString(final byte [] bytes) {
		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) {
			int hex = (0xff & bytes[i]);
			String tmp = Integer.toHexString(hex);
			tmp = (tmp.length() < 2) ? "0" + tmp : tmp; // if tmp=="9" => tmp=="09"
			hexString.append(tmp);
		}
 
		return hexString.toString().toUpperCase();
	}

    /**
     *  Copyright Â© 2003, 2006 Eric Glass
     *  Permission to use, copy, modify, and distribute this document 
     *  for any purpose and without any fee is hereby granted, provided 
     *  that the above copyright notice and this list of conditions appear 
     *  in all copies.
     *  
     *  The most current version of this document may be obtained 
     *  from http://davenport.sourceforge.net/ntlm.html. The author may 
     *  be contacted vie e-mail at eric.glass at gmail.com.
     */
    private static Key createDESKey(byte[] bytes, int offset) {
		byte[] keyBytes = new byte[7];
		System.arraycopy(bytes, offset, keyBytes, 0, 7);
		byte[] material = new byte[8];
		material[0] = keyBytes[0];
		material[1] = (byte) (keyBytes[0] << 7 | (keyBytes[1] & 0xff) >>> 1);
		material[2] = (byte) (keyBytes[1] << 6 | (keyBytes[2] & 0xff) >>> 2);
		material[3] = (byte) (keyBytes[2] << 5 | (keyBytes[3] & 0xff) >>> 3);
		material[4] = (byte) (keyBytes[3] << 4 | (keyBytes[4] & 0xff) >>> 4);
		material[5] = (byte) (keyBytes[4] << 3 | (keyBytes[5] & 0xff) >>> 5);
		material[6] = (byte) (keyBytes[5] << 2 | (keyBytes[6] & 0xff) >>> 6);
		material[7] = (byte) (keyBytes[6] << 1);
		oddParity(material);
		return new SecretKeySpec(material, "DES");
    }
    
    /**
     *  Copyright Â© 2003, 2006 Eric Glass
     *  Permission to use, copy, modify, and distribute this document 
     *  for any purpose and without any fee is hereby granted, provided 
     *  that the above copyright notice and this list of conditions appear 
     *  in all copies.
     *  
     *  The most current version of this document may be obtained 
     *  from http://davenport.sourceforge.net/ntlm.html. The author may 
     *  be contacted vie e-mail at eric.glass at gmail.com.
     */
    private static void oddParity(byte[] bytes) {
		for (int i = 0; i < bytes.length; i++) {
		    byte b = bytes[i];
		    boolean needsParity = (((b >>> 7) ^ (b >>> 6) ^ (b >>> 5) ^
								    (b >>> 4) ^ (b >>> 3) ^ (b >>> 2) ^
								    (b >>> 1)) & 0x01) == 0;
		    if (needsParity) {
				bytes[i] |= (byte) 0x01;
		    } else {
				bytes[i] &= (byte) 0xfe;
		    }
		}
    }
}
