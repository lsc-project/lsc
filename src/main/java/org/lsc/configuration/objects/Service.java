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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.tapestry5.beaneditor.Validate;
import org.lsc.exception.LscServiceCommunicationException;
import org.lsc.exception.LscServiceConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This object represents the subset of settings shared by all services.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 * @author rschermesser
 */
public abstract class Service {

	private static final Logger LOGGER = LoggerFactory.getLogger(Service.class);

	/**
	 * <service type="SimpleJndiSrcService">
			<name>myADAccount</name>
			<connection>myAdServer</connection>
			<baseDn>ou=people</baseDn>
			<pivotAttributes>dn, cn</pivotAttributes>
			<fetchAttributes>uid, mail, cn</fetchAttributes>
			<getAllFilter>(&(objectClass=inetOrgPerson)(uid=*))</getAllFilter>
			<getOneFilter>(&(objectClass=inetOrgPerson)(uid={uid}))</getOneFilter>
		</service>
		<service type="SimpleJndiDstService">
			<name>myDestination</name>
			<connection>myAdServer</connection>
		</service>
	 */
	@Validate("required")
	private String name;
	
	private Connection connection;
	
	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAlias(String name) {
		return name;
	}
	
	public void setOtherSetting(String name, String value) {
		try {
			String methodName = getMethodName(getAlias(name), "set");
			Method method = this.getClass().getMethod(methodName, value.getClass());
			if(method!= null) {
				method.invoke(this, value);
			}
		} catch (SecurityException e) {
			LOGGER.error("Technical issue : " + e.toString(), e);
		} catch (IllegalArgumentException e) {
			LOGGER.error("Technical issue : " + e.toString(), e);
		} catch (IllegalAccessException e) {
			LOGGER.error("Technical issue : " + e.toString(), e);
		} catch (InvocationTargetException e) {
			LOGGER.error("Technical issue : " + e.toString(), e);
		} catch (NoSuchMethodException e) {
			LOGGER.error("No service settings named '" + name + "'. Unable to set value: '"+ value +"' (" + e.toString() + ")");
			if(LOGGER.isDebugEnabled()) {
				LOGGER.debug(e.toString(), e);
			}
		}
	}

	private String getMethodName(String name, String prefix) {
		return prefix + name.substring(0,1).toUpperCase() + name.substring(1); 
	}
	
	public abstract Class<?> getImplementation();
	
	public abstract void validate() throws LscServiceConfigurationException, LscServiceCommunicationException;
}
