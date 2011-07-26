package org.lsc.configuration.objects.security;

import org.lsc.exception.LscException;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("security")
public class Security {

	private Encryption encryption;
	
	public Encryption getEncryption() {
		return encryption;
	}

	public void setEncryption(Encryption encryption) {
		this.encryption = encryption;
	}

	public void validate() throws LscException {
		encryption.validate();
	}
}
