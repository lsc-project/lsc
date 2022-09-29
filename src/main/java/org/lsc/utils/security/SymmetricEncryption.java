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
 *         Sebastien Bahloul&lt;seb@lsc-project.org&gt;
 *         Thomas Chemineau&lt;thomas@lsc-project.org&gt;
 *         Jonathan Clarke&lt;jon@lsc-project.org&gt;
 *         Remy-Christophe Schermesser&lt;rcs@lsc-project.org&gt;
 ****************************************************************************
 */
package org.lsc.utils.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.lsc.Configuration;
import org.lsc.configuration.EncryptionType;
import org.lsc.configuration.LscConfiguration;
import org.lsc.exception.LscException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This new class allows symmetric encryption. You should have BouncyCastle
 * installed. Three new configuration parameters could be added to the
 * configuration:</p>
 * <ul>
 *   <li>lsc &gt; security &gt; encryption &gt; keyfile: the path to the file used to
 *   encrypt/decrypt data</li>
 *   <li>lsc &gt; security &gt; encryption &gt; algorithm: the algorithm to use</li>
 *   <li>lsc &gt; security &gt; encryption &gt; strength: the strength in bits</li>
 * </ul>
 */
public class SymmetricEncryption {

	private static final Logger LOGGER = LoggerFactory.getLogger(SymmetricEncryption.class);

	public static final int DEFAULT_CIPHER_STRENGTH = 128;
	public static final String DEFAULT_CIPHER_ALGORITHM = "AES";
	private int strength;
	private String algorithm;
	private String keyPath;
	private Cipher cipherDecrypt;
	private Cipher cipherEncrypt;
	private Provider securityProvider;

	/**
	 * New SymmetricEncryption object with default values.
	 * @throws java.security.GeneralSecurityException
	 */
	public SymmetricEncryption() throws GeneralSecurityException {
		this.securityProvider = new BouncyCastleProvider();
		Security.addProvider(this.securityProvider);
	}

	/**
	 * New SymmetricEncryption object.
	 * @param encryption the encryption required structure
	 * @throws java.security.GeneralSecurityException
	 */
	public SymmetricEncryption(EncryptionType encryption) throws GeneralSecurityException {
	    if(encryption == null) {
            throw new RuntimeException("lsc>security>encryption node of the LSC configuration cannot be null !");
        } else if(encryption.getKeyfile() == null) {
            throw new RuntimeException("lsc>security>encryption>keyfile node of the LSC configuration cannot be null !");
        } else if(encryption.getAlgorithm() == null) {
            throw new RuntimeException("lsc>security>encryption>algorithm node of the LSC configuration cannot be null !");
        }

        this.securityProvider = new BouncyCastleProvider();
		this.algorithm = encryption.getAlgorithm();
		this.strength = encryption.getStrength();
		this.keyPath = encryption.getKeyfile();

		Security.addProvider(this.securityProvider);
	}

	/**
	 * Encrypt bytes.
	 * @param toEncrypt
	 * @return Encrypted bytes.
	 * @throws java.security.GeneralSecurityException
	 */
	public byte[] encrypt(byte[] toEncrypt) throws GeneralSecurityException {
		return cipherEncrypt.doFinal(toEncrypt);
	}

	/**
	 * Decrypt bytes.
	 * @param toDecrypt
	 * @return Decrypted bytes.
	 * @throws java.security.GeneralSecurityException
	 */
	public byte[] decrypt(byte[] toDecrypt) throws GeneralSecurityException {
		return cipherDecrypt.doFinal(toDecrypt);
	}

	/**
	 * Generate a random key file with default value
	 * @return boolean false if an error occurred
	 * @throws NoSuchAlgorithmException 
	 * @throws NoSuchProviderException 
	 */
	public boolean generateDefaultRandomKeyFile() throws NoSuchAlgorithmException, NoSuchProviderException {
		File keypath = new File(Configuration.getConfigurationDirectory(), "lsc.key");
		if(keypath.exists()) {
			LOGGER.error("Existing key file in {}. Please move it away before generating a new key !", keypath.getAbsolutePath());
			return false;
		}
		boolean status = this.generateRandomKeyFile(keypath.getAbsolutePath(), this.algorithm, this.strength);
		this.keyPath = keypath.getAbsolutePath();
		return status;
	}

	/**
	 * Generate a random key file.
	 * @param keyPath The filename where to write the key
	 * @param algo The supported algorithm to use
	 * @param strength The encryption strength
	 * @return boolean false if an error occurred
	 * @throws NoSuchAlgorithmException 
	 * @throws NoSuchProviderException 
	 */
	public boolean generateRandomKeyFile(String keyPath, String algo, int strength) throws NoSuchAlgorithmException, NoSuchProviderException {
		OutputStream os = null;
		try {
			KeyGenerator kg = KeyGenerator.getInstance(algo, securityProvider.getName());
			SecretKey cipherKey = kg.generateKey();
			SecureRandom sr = new SecureRandom();
			kg.init(strength, sr);
			os = new FileOutputStream(keyPath);
			os.write(cipherKey.getEncoded());
		} catch (IOException e) {
			LOGGER.error("Unable to write new generated key in " + keyPath + ". Encountered exception is : " + e.getLocalizedMessage(), e);
			return false;
		} finally {
			try {
				if(os != null) {
					os.close();
				}
			}
			catch (IOException e1) {
			}
		}
		return true;
	}

	/**
	 * Return the default filename of the key to use.
	 * The filename could be specified in the configuration file through the
	 * lsc.security.encryption.keyfile property.
	 * @return A filename
	 */
	public static String getKeyPath() {
		return LscConfiguration.getSecurity().getEncryption().getKeyfile();
	}

	/**
	 * Return the default supported algorithm to use.
	 * The algorithm could be specified in the configuration file through the
	 * lsc.security.encryption.algorithm property. See constant values defined
	 * in this class which specified supported algorithms.
	 * @return A supported algorithm
	 */
	public static String getAlgorithm() {
		return LscConfiguration.getSecurity().getEncryption().getAlgorithm();
	}

	/**
	 * Return the default encryption strength.
	 * The encryption strength could be specified in the configuration file
	 * through the lsc.security.encryption.strength property.
	 * @return int
	 */
	public static int getStrength() {
		return LscConfiguration.getSecurity().getEncryption().getStrength();
	}

	/**
	 * Initialize encryption object from the configuration file.
	 * @return boolean (always true if no exception)
	 * @throws GeneralSecurityException 
	 */
	public boolean initialize() throws GeneralSecurityException {
		InputStream input = null;
		boolean fail = false;
		try {
			input = new FileInputStream(new File(this.keyPath));
			byte[] data = new byte[strength / Byte.SIZE];
			input.read(data);

			Key key = new SecretKeySpec(data, this.algorithm);
			this.cipherEncrypt = Cipher.getInstance(this.algorithm);
			this.cipherEncrypt.init(Cipher.ENCRYPT_MODE, key);
			this.cipherDecrypt = Cipher.getInstance(this.algorithm);
			this.cipherDecrypt.init(Cipher.DECRYPT_MODE, key);

		} catch (IOException e) {
			fail = true;
		} finally {
			try {
				if(input != null) {
					input.close();
				}
			} catch (IOException e) {
			}
		}
		
		if (fail) {
			LOGGER.error("Error reading the key for SymmetricEncryption! ({})", this.keyPath);
			return false;
		}
		
		return true;
	}

	/**
	 * This main allow user to generate random key file.
	 * @param argv
	 */
	public static void main(String argv[]) {
		try {
			Options options = new Options();
			options.addOption("f", "cfg", true, "Specify configuration directory");
			CommandLine cmdLine = new GnuParser().parse(options, argv);

			if (cmdLine.getOptions().length > 0 && cmdLine.hasOption("f")) {
				// if a configuration directory was set on command line, use it to set up Configuration
				Configuration.setUp(cmdLine.getOptionValue("f"), false);
			} else {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("lsc", options);
				System.exit(1);
			}
		} catch (ParseException e) {
			StringBuilder sbf = new StringBuilder();
			for(String arg : argv) {
				sbf.append(arg).append(" ");
			}
			
			LOGGER.error("Unable to parse options : {}({})", sbf.toString(), e);
			System.exit(1);
		} catch (LscException e) {
			LOGGER.error("Something goes wrong while loading configuration: " + e.toString(), e);
			System.exit(2);
		}

		try {
            if(LscConfiguration.getSecurity() == null) {
                throw new RuntimeException("lsc>security node of the LSC configuration cannot be null !");
            } else if(LscConfiguration.getSecurity().getEncryption() == null) {
                throw new RuntimeException("lsc>security>encryption node of the LSC configuration cannot be null !");
            }
			SymmetricEncryption se = new SymmetricEncryption(LscConfiguration.getSecurity().getEncryption());
			if (se.generateDefaultRandomKeyFile()) {
				LOGGER.info("Key generated: {}. Do not forget to check the lsc>security>encryption>keyfile node value in your configuration file !", se.keyPath );
			}
		} catch (GeneralSecurityException ex) {
			LOGGER.debug(ex.toString(), ex);
		}
	}
}
