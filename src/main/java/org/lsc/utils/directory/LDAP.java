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
package org.lsc.utils.directory;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.api.ldap.model.exception.LdapURLEncodingException;
import org.apache.directory.api.ldap.model.url.LdapUrl;
import org.lsc.jndi.JndiServices;

/**
 * <P>
 * Utility class that offers useful functions for standard LDAP directories.
 * </P>
 * <P>
 * Intended for use in the lsc.properties configuration file via the JavaScript
 * Rhino interpreter.
 * </P>
 * <P>
 * Rationale: the methods in this class have been thought out for repeated calls
 * during a synchronization task, when each method may be called as many times
 * as objects are synchronized. Thus, we attempt to optimize resource
 * utilization.
 * </P>
 * 
 * @author Jonathan Clarke &lt;jonathan@phillipoux.net&gt;
 */
public class LDAP {

	// Utility class
	private LDAP() {}
	
	/**
	 * Connects to a LDAP server anonymously and tries to rebind with the
	 * provided DN and password to check.
	 * 
	 * @param url
	 *            URL of the LDAP server to connect to, e.g.
	 *            "ldap://ldap.example.com/". If this URL starts with "ldaps" a
	 *            secure connection will be used.
	 * @param dnToCheck
	 *            Distinguished Name (DN) to check the bind with.
	 * @param passwordToCheck
	 *            Password to check the bind with.
	 * @return true if the bind succeeds, false if the bind fails
	 * @throws NamingException
	 *             any exceptions that occur during connection, other than bind
	 *             failures
	 */
	public static boolean canBind(String url, String dnToCheck,
					String passwordToCheck) throws NamingException {
		return canBind(url, null, null, dnToCheck, passwordToCheck);
	}

	/**
	 * Connects to a LDAP server using a specific DN and password, then tries to
	 * rebind with the provided DN and password to check.
	 * 
	 * @param url
	 *            URL of the LDAP server to connect to, e.g.
	 *            "ldap://ldap.example.com/". If this URL starts with "ldaps" a
	 *            secure connection will be used.
	 * @param bindDn
	 *            DN to bind to the server with. If null, binds anonymously.
	 * @param bindPassword
	 *            Password to bind to the server with.
	 * @param dnToCheck
	 *            Distinguished Name (DN) to check the bind with.
	 * @param passwordToCheck
	 *            Password to check the bind with.
	 * @return true if the bind succeeds, false if the bind fails
	 * @throws NamingException
	 *             any exceptions that occur during connection, other than bind
	 *             failures
	 */
	public static boolean canBind(String url, String bindDn,
					String bindPassword, String dnToCheck, String passwordToCheck)
					throws NamingException {

		// get JndiServices for this bindDn/password
		JndiServices bindJndiServices;
		try {
			bindJndiServices = getJndiServices(url, bindDn, bindPassword);
		} catch (NamingException e) {
			// any LDAP related error is thrown, since we haven't started
			// testing the actual bind yet, so this is an unrelated error
			throw e;
		}

		// check bind DN and password
		LdapContext bindContext = bindJndiServices.getContext();
		try {
			bindContext.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");
			bindContext.addToEnvironment(Context.SECURITY_PRINCIPAL, dnToCheck);
			bindContext.addToEnvironment(Context.SECURITY_CREDENTIALS, passwordToCheck);
			bindContext.reconnect(bindContext.getConnectControls());
		} catch (AuthenticationException e) {
			// the bind failed
			return false;
		} catch (NamingException e) {
			// some other LDAP related error occurred
			// we throw it, since it may be connection related,
			// and we don't want to return fake results
			throw e;
		} finally {
			// clean up and replace authentication on the context with original
			// identity
			Properties authProps = getJndiAuthenticationProperties(bindDn, bindPassword);
			for (Entry<Object, Object> propertyEntry : authProps.entrySet()) {
				bindContext.addToEnvironment(propertyEntry.getKey().toString(), propertyEntry.getValue());
			}
			bindContext.reconnect(bindContext.getConnectControls());
		}

		// if we got here, the bind succeeded
		return true;
	}

	/**
	 * Connects to a LDAP server anonymously, then performs a search to find a
	 * DN, then tries to rebind with the provided DN and password to check
	 * authentication.
	 * 
	 * @param url
	 *            URL of the LDAP server to connect to and search parameters,
	 *            e.g.
	 *            "ldap://ldap.example.com/dc=example,dc=com??sub?(uid=nportman)"
	 *            . If this URL starts with "ldaps" a secure connection will be
	 *            used. This URL must include the search filter, and may include
	 *            the search scope, which defaults to "sub".
	 * @param passwordToCheck
	 *            Password to check the bind with.
	 * @return true if the search finds exactly one user and the bind succeeds,
	 *         false if the bind fails
	 * @throws NamingException
	 *             any exceptions that occur during connection, other than bind
	 *             failures and no search results
	 * @throws LdapURLEncodingException 
	 */
	public static boolean canBindSearchRebind(String url, String passwordToCheck)
					throws NamingException, LdapURLEncodingException {
		return canBindSearchRebind(url, null, null, passwordToCheck);
	}

	/**
	 * Connects to a LDAP server using a specific DN and password, then performs
	 * a search to find a DN, then tries to rebind with the provided DN and
	 * password to check authentication.
	 * 
	 * @param url
	 *            URL of the LDAP server to connect to and search parameters,
	 *            e.g.
	 *            "ldap://ldap.example.com/dc=example,dc=com??sub?(uid=nportman)"
	 *            . If this URL starts with "ldaps" a secure connection will be
	 *            used. This URL must include the search filter, and may include
	 *            the search scope, which defaults to "sub".
	 * @param bindDn
	 *            DN to bind to the server with. If null, binds anonymously.
	 * @param bindPassword
	 *            Password to bind to the server with.
	 * @param passwordToCheck
	 *            Password to check the bind with.
	 * @return true if the search finds exactly one user and the bind succeeds,
	 *         false if the bind fails
	 * @throws NamingException
	 *             any exceptions that occur during connection, other than bind
	 *             failures and no search results
	 * @throws LdapURLEncodingException 
	 */
	public static boolean canBindSearchRebind(String url, String bindDn,
					String bindPassword, String passwordToCheck)
					throws NamingException, LdapURLEncodingException {

		// interpret the search URL to feed to JndiServices
		// this is done first to throw NamingException ASAP, not after
		// connecting...
		LdapUrl urlInstance = new LdapUrl(url);

		// get JndiServices for this bindDn and bindPassword
		JndiServices bindJndiServices;
		try {
			bindJndiServices = getJndiServices(url, bindDn, bindPassword);
		} catch (NamingException e) {
			// any LDAP related error is thrown, since we haven't started
			// testing
			// the actual bind yet, so this is an unrelated error
			throw e;
		}

		// transform to a relative DN for our JndiServices...
		String baseDn = urlInstance.getDn().toString();
		String contextDn = bindJndiServices.getContextDn();
		if (contextDn != null && baseDn.endsWith(contextDn)) {
			baseDn = baseDn.substring(0, baseDn.length() - contextDn.length());
		}

		// perform the search and get back matching DNS
		SearchResult matchingDns;
		try {
			matchingDns = bindJndiServices.getEntry(baseDn, urlInstance.getFilter().toString(), new SearchControls(), urlInstance.getScope().getScope());
		} catch (SizeLimitExceededException e) {
			// more than one result was returned!
			// only one user account may match, anything else is an error
			return false;
		}

		// no entry returned
		// only one user account may match, anything else is an error
		if (matchingDns == null) {
			return false;
		}

		// check password by using the DN found
		String dnToCheck = matchingDns.getNameInNamespace();
		return canBind(url, bindDn, bindPassword, dnToCheck, passwordToCheck);
	}

	/**
	 * Internal method to standardize getting a JndiServices instance, and thus
	 * encourage using the JndiServices cache.
	 * 
	 * @param url
	 *            URL of the LDAP server to connect to. Any parameters other
	 *            than the context DN will be ignored.
	 * @param bindDn
	 *            DN to bind to the server with. If null, binds anonymously.
	 * @param bindPassword
	 *            Password to bind to the server with.
	 * @return an instance of JndiServices to use
	 * @throws NamingException
	 *             In case of any error connecting.
	 */
	private static JndiServices getJndiServices(String url, String bindDn,
					String bindPassword) throws NamingException {
		try {
			// load properties to create LDAP connection
			Properties props = new Properties();

			// add some defaults
			props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			props.put(Context.REFERRAL, "ignore");

			// use clean URL
			String bindUrl = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
			props.put(Context.PROVIDER_URL, bindUrl);

			// authentication properties
			props.putAll(getJndiAuthenticationProperties(bindDn, bindPassword));

			// get JndiServices for these properties
			// this takes advantage of the JndiServices cache and will re-use
			// connections
			return JndiServices.getInstance(props);
		} catch (NamingException e) {
			// some other LDAP related error occurred
			// we throw it, since it may be connection related, and we don't
			// want to return fake results
			throw e;
		} catch (IOException e) {
			// error opening the connection
			throw new CommunicationException(e.toString());
		}

	}

	/**
	 * Internal method to standardize getting authentication properties for
	 * JNDI.
	 * 
	 * @param bindDn
	 *            DN to bind to the server with. If null, binds anonymously.
	 * @param bindPassword
	 *            Password to bind to the server with.
	 * @return Properties containing authentication information
	 */
	private static Properties getJndiAuthenticationProperties(String bindDn,
					String bindPassword) {
		Properties props = new Properties();

		if (bindDn == null) {
			props.put(Context.SECURITY_AUTHENTICATION, "none");
		} else {
			props.put(Context.SECURITY_AUTHENTICATION, "simple");
			props.put(Context.SECURITY_PRINCIPAL, bindDn);
			props.put(Context.SECURITY_CREDENTIALS, bindPassword);
		}

		return props;
	}
}
