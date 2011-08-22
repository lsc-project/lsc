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
package org.lsc.configuration.objects.connection.directory;

import org.apache.tapestry5.beaneditor.Validate;
import org.lsc.configuration.objects.AuthenticatedConnection;
import org.lsc.configuration.objects.services.DstLdap;
import org.lsc.configuration.objects.services.SrcLdap;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 *
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
@XStreamAlias("ldapConnection")
public class Ldap extends AuthenticatedConnection {

	/** Method to use to bind to the directory */
	@Validate("required")
	private AuthenticationType authentication;

	/** Request mutual authentication in case of a GSSAPI through SASL authentication */
	@Validate("required")
	private boolean saslMutualAuthentication;
	
	/**
	 * A simple directory URL with protocol, host and port, ie
	 * ldap://ldap.openldap.org:389/
	 */
	@Validate("required,regexp=^ldap(s)?://[_a-zA-A0-9][_a-zA-Z0-9\\-\\.]+(:\\d+)?(/\\S*)?$")
	private String url;

	/** */
	@Validate("required")
	private ReferralHandling referral;

	/** */
	@Validate("required")
	private AliasesHandling derefAliases;
	
	/** LDAP Version */
	@Validate("required")
	private LdapVersion version;
	
	/** page size */
	private int pageSize;
	
	/** Comma separated attribute names to sort on */
	private String sortedBy;
	
	/** Implicit value default to "com.sun.jndi.ldap.LdapCtxFactory" */
	private String factory;
	
	/** List of attributes to handle as binary attributes (i.e.: objectSID Active Directory unique entry identifier) */
	private String[] binaryAttributes;
	
	/** Is SSL/TLS activated for connecting to LDAP */
	private boolean tlsActivated;
	
	public Ldap() {
		factory = "com.sun.jndi.ldap.LdapCtxFactory";
		version = LdapVersion.VERSION_3;
		derefAliases = AliasesHandling.NEVER;
		referral = ReferralHandling.IGNORE;
		authentication = AuthenticationType.ANONYMOUS;
		pageSize = -1;
	}

	public AuthenticationType getAuthenticationType() {
		return authentication;
	}

	public LdapVersion getVersion() {
		return version;
	}

	public AliasesHandling getAliasesHandling() {
		return derefAliases;
	}

	public ReferralHandling getReferralHandling() {
		return referral;
	}

	public void setAuthenticationType(AuthenticationType authenticationType) {
		this.authentication = authenticationType;
	}

	public void setAuthenticationType(String authenticationType) {
		if(authenticationType != null) {
			this.authentication = AuthenticationType.valueOf(authenticationType.toUpperCase());
		} else {
			this.authentication = AuthenticationType.ANONYMOUS;
		}
	}

	public void setReferralHandling(ReferralHandling referralHandling) {
		this.referral = referralHandling;
	}
	
	public void setReferralHandling(String referralHandling) {
		if(referralHandling != null) {
			this.referral = ReferralHandling.valueOf(referralHandling.toUpperCase());
		} else {
			this.referral = ReferralHandling.IGNORE;
		}
	}

	public void setAliasesHandling(AliasesHandling aliasesHandling) {
		this.derefAliases = aliasesHandling;
	}

	public void setAliasesHandling(String aliasesHandling) {
		if(aliasesHandling != null) {
			this.derefAliases = AliasesHandling.valueOf(aliasesHandling.toUpperCase());
		} else {
			this.derefAliases = AliasesHandling.NEVER;
		}
	}

	public void setVersion(LdapVersion version) {
		this.version = version;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public String getSortedBy() {
		return sortedBy;
	}

	public void setSortedBy(String sortedBy) {
		this.sortedBy = sortedBy;
	}

	public String getFactory() {
		return factory;
	}

	public String[] getBinaryAttributes() {
		return binaryAttributes;
	}

	public void setBinaryAttributes(String[] binaryAttributes) {
		this.binaryAttributes = binaryAttributes;
	}
	
	public boolean isTlsActivated() {
		return tlsActivated;
	}
	
	public void setTlsActivated(boolean tlsActivated) {
		this.tlsActivated = tlsActivated;
	}

	public Class<?> getService(boolean isSource) {
		if(isSource) {
			return SrcLdap.class;
		} else {
			return DstLdap.class;
		}
	}

	@Override
	public String getConnectionTypeName() {
		return "LDAP connection";
	}

	@Override
	public String getUrl() {
		return url;
	}

	@Override
	public void setUrl(String url) {
		this.url = url;
	}
	
	public void setSaslMutualAuthentication(boolean status) {
		this.saslMutualAuthentication = status;
	}
	
	public boolean isSaslMutualAuthentication() {
		return saslMutualAuthentication;
	}
}
