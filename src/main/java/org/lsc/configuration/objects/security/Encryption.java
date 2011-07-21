package org.lsc.configuration.objects.security;

import java.io.File;

import org.lsc.Configuration;
import org.lsc.utils.security.SymmetricEncryption;

public class Encryption {

	private String keyfile;
	
	private String algorithm;
	
	private int strength;
	
	public Encryption() {
		algorithm = SymmetricEncryption.DEFAULT_CIPHER_ALGORITHM;
		strength = SymmetricEncryption.DEFAULT_CIPHER_STRENGTH;
		keyfile = new File(Configuration.getConfigurationDirectory(), "lsc.key").getAbsolutePath();
	}
	
	public String getKeyfile() {
		return keyfile;
	}
	
	public String getAlgorithm() {
		return algorithm;
	}
	
	public int getStrength() {
		return strength;
	}

	public void setKeyfile(String absoluteFile) {
		keyfile = absoluteFile;
	}
}
