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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.exception.LscConfigurationException;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * This class represent the generic synchronization options configuration
 * container. This object is used to return the settings required to identify
 * how and when construct the virtual destination object through source
 * and destination object datasets.
 * 
 * It must be subtyped to provide the real implementation.
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public abstract class SyncOptions {

	@XStreamOmitField
	private HashMap<String, String> otherSettings;
	
	/**
	 * These conditions will describe if the object has to be
	 * created, updated, delete or renamed if required
	 */
	private Conditions conditions;
	
	/** 
	 * Rule to build the object identifier
	 * 'cn=' + srcBean.getAttributeValueById('uid')
	 */
	private String mainIdentifier;
	
	public SyncOptions() {
		otherSettings = new HashMap<String, String>();
		conditions = new Conditions();
	}
	
	public void load(String taskname, Properties props) {
		for(String name: props.stringPropertyNames()) {
			otherSettings.put(name, props.getProperty(name));
		}	
	}
	
	public void setOtherSetting(String name, String value) {
		otherSettings.put(name, value);
	}

	public void setOtherSettings(HashMap<String, String> otherSettings) {
		this.otherSettings = otherSettings;
	}

	public Map<String, String> getOtherSettings() {
		return otherSettings;
	}

	public String getMainIdentifier() {
		return mainIdentifier;
	}
	
	public void setMainIdentifier(String value) {
		this.mainIdentifier = value;
	}

	public Conditions getConditions() {
		return conditions;
	}

	/**
	 * Return the implementation of this configuration object
	 * @return the syncoption implementation
	 */
	public abstract Class<? extends ISyncOptions> getImplementation();

	public abstract void validate() throws LscConfigurationException;
}
