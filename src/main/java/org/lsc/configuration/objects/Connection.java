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
package org.lsc.configuration.objects;

import java.util.Properties;

import org.apache.tapestry5.beaneditor.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author rschermesser
 */
@XStreamAlias("connection")
public abstract class Connection {

	/**
	 * <connection type="ldap">
			<name>myAdServer</name>
			<url>ldap://localhost:1390/dc=AD,dc=net</url>
			<username>cn=manager,dc=AD,dc=net</username>
			<password>secret</password>
			<referral>ignore</referral>
			<derefAliases>never</derefAliases>
			<factory>com.sun.jndi.ldap.LdapCtxFactory</factory>
			<version>3</version>
			<authentification>simple</authentification>
			<pageSize>10</pageSize>
			<tls>true</tls>
		</connection>
		<connection type="database">
			<name>myHSQLDBServer</name>
			<url>jdbc:hsqldb:file:hsqldb/lsc</url>
			<username>elilly</username>
			<password></password>
			<driver>org.hsqldb.blabla</driver>
		</connection>
		<connection type="nis">
			<name>myisSServer</name>
			<url>nis://server:port/domain</url>
		</connection>
	 */

	private static final Logger LOGGER = LoggerFactory.getLogger(Connection.class);

	@Validate("required")
	@XStreamAlias("id")
	protected String name;
	
	@Validate("required")
	protected String url;
	
	protected String username;
	
	protected String password;
	
	public void load(String name, Properties props) {
		this.name = name;
		for(String key : props.stringPropertyNames()) {
			if(key.endsWith("url")) {
				url  = props.getProperty(key);
			} else {
				LOGGER.error("Unknown \"" + name + "\" parameter !");
			}
		}
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	/**
	 * This method provides the service class
	 * @param isSource give service status : source or destination
	 * @return the corresponding service class
	 */
	public abstract Class<?> getService(boolean isSource);

	public abstract String getConnectionTypeName();
}
