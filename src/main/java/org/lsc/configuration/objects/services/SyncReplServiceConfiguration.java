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
package org.lsc.configuration.objects.services;

import org.apache.directory.ldap.client.api.exception.LdapException;
import org.apache.directory.ldap.client.api.message.SearchResponse;
import org.apache.directory.ldap.client.api.message.SearchResultEntry;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.control.Control;
import org.apache.tapestry5.beaneditor.Validate;
import org.lsc.configuration.objects.connection.directory.Ldap;
import org.lsc.exception.LscServiceCommunicationException;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.jndi.JndiServices;
import org.lsc.service.SyncReplSourceService;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * SyncRepl service configuration: configuration identical
 * to a standard Source LDAP service.
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
@XStreamAlias("syncreplSourceService")
public class SyncReplServiceConfiguration extends SrcLdap {

	public static final String SUPPORTED_CONTROLS_ATTRIBUTE = "supportedControl";

	/**
	 * Specify if the asynchronous synchronization must start
	 * with a full sync (true), or just catch updates (false)  
	 */
	@Validate("required")
	private boolean synchronizingAllWhenStarting;
	
	private ServerType serverType;
	
	@Override
	public Class<?> getImplementation() {
		return SyncReplSourceService.class;
	}
	
	public boolean isSynchronizingAllWhenStarting() {
		return synchronizingAllWhenStarting;
	}
	
	public ServerType getServerType() {
		return serverType;
	}

	@Override
	public void validate() throws LscServiceConfigurationException,
			LscServiceCommunicationException {
		Control continuationControl = SyncReplSourceService.getSearchContinuationControl(this.getServerType());
		try {
			// Need to check LDAP settings
			JndiServices.getInstance((org.lsc.configuration.objects.connection.directory.Ldap)this.getConnection());
			Cursor<SearchResponse> sres = SyncReplSourceService.getConnection((Ldap) this.getConnection()).search("", "(objectClass=*)", SearchScope.OBJECT, SUPPORTED_CONTROLS_ATTRIBUTE);
			if(!sres.next()
					|| ((SearchResultEntry)sres.get()).getEntry().get(SUPPORTED_CONTROLS_ATTRIBUTE) == null 
					|| !((SearchResultEntry)sres.get()).getEntry().get(SUPPORTED_CONTROLS_ATTRIBUTE).contains(continuationControl.getOid())) {
				throw new LscServiceConfigurationException("No continuation control (" + continuationControl.toString() + "). Aborting !");
			}
			sres.close();
		} catch (RuntimeException re) {
			if(re.getCause() instanceof LscServiceConfigurationException) {
				throw (LscServiceConfigurationException)re.getCause();
			} else if(re.getCause() instanceof LscServiceCommunicationException) {
				throw (LscServiceCommunicationException)re.getCause();
			} else {
				throw re;
			}
		} catch (LdapException e) {
			throw new LscServiceConfigurationException("Unable to find the right continuation control: " + continuationControl.toString(), e);
		} catch (Exception e) {
			throw new LscServiceConfigurationException("Unable to find the right continuation control: " + continuationControl.toString(), e);
		}
	}
}
