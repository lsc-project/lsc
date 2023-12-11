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
package org.lsc.beans.syncoptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import org.lsc.LscModificationType;
import org.lsc.configuration.DatasetType;
import org.lsc.configuration.LscConfiguration;
import org.lsc.configuration.PolicyType;
import org.lsc.configuration.PropertiesBasedSyncOptionsType;
import org.lsc.configuration.TaskType;
import org.lsc.configuration.ValuesType;

/**
 * Synchronization options based on a properties file
 * 
 * This class interprets properties to get detailed options for
 * synchronization, including behavior and values for the general
 * case or attribute by attribute.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 * @author Jonathan Clarke &lt;jon@lsc-project.org&gt;
 */
public class PropertiesBasedSyncOptions implements ISyncOptions {

//	private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesBasedSyncOptions.class);

	private PropertiesBasedSyncOptionsType conf;

	public void initialize(TaskType task) {
		conf = task.getPropertiesBasedSyncOptions();
	}
	
	public final PolicyType getStatus(final String id, final String attributeName) {
		PolicyType statusType = LscConfiguration.getDataset(conf, attributeName).getPolicy();
		return (statusType != null ? statusType : conf.getDefaultPolicy());
	}

	public final List<String> getDefaultValues(final String id, final String attributeName) {
		ValuesType datasetValues = LscConfiguration.getDataset(conf, attributeName).getDefaultValues();
		ArrayList<String> copy = null;
		if (datasetValues != null && datasetValues.getString().size() > 0) {
			copy = new ArrayList<String>(datasetValues.getString());
		}
		return copy;
	}

	public final List<String> getCreateValues(final String id, final String attributeName) {
		ValuesType datasetValues = LscConfiguration.getDataset(conf, attributeName).getCreateValues();
		ArrayList<String> copy = null;
		if (datasetValues != null && datasetValues.getString().size() > 0) {
			copy = new ArrayList<String>(datasetValues.getString());
		}
		return copy;
	}

	public final List<String> getForceValues(final String id, final String attributeName) {
		ValuesType datasetValues = LscConfiguration.getDataset(conf, attributeName).getForceValues();
		ArrayList<String> copy = null;
		if (datasetValues != null && datasetValues.getString().size() > 0) {
			copy = new ArrayList<String>(datasetValues.getString());
		}
		return copy;
	}

	@Override
	public Set<String> getCreateAttributeNames() {
		Set<String> createAttrs = new HashSet<String>();
		for(DatasetType attr : conf.getDataset()) {
			if(!attr.getCreateValues().getString().isEmpty()) {
				createAttrs.add(attr.getName());
			}
		}
		return createAttrs;
	}


	@Override
	public Set<String> getDefaultValuedAttributeNames() {
		Set<String> createAttrs = new HashSet<String>();
		for(DatasetType attr : conf.getDataset()) {
			if(!attr.getDefaultValues().getString().isEmpty()) {
				createAttrs.add(attr.getName());
			}
		}
		return createAttrs;
	}


	@Override
	public Set<String> getForceValuedAttributeNames() {
		Set<String> createAttrs = new HashSet<String>();
		for(DatasetType attr : conf.getDataset()) {
			if(!attr.getForceValues().getString().isEmpty()) {
				createAttrs.add(attr.getName());
			}
		}
		return createAttrs;
	}
	
	public String getDn() {
		return conf.getMainIdentifier();
	}


	public String getCreateCondition() {
		if (conf.getConditions() == null || conf.getConditions().getCreate() == null) {
			return DEFAULT_CONDITION;
		}
		return conf.getConditions().getCreate();
	}

	public String getDeleteCondition() {
		if (conf.getConditions() == null || conf.getConditions().getDelete() == null) {
			return DEFAULT_CONDITION;
		}
		return conf.getConditions().getDelete();
	}

	public String getUpdateCondition() {
		if (conf.getConditions() == null || conf.getConditions().getUpdate() == null) {
			return DEFAULT_CONDITION;
		}
		return conf.getConditions().getUpdate();
	}

	public String getChangeIdCondition() {
		if (conf.getConditions() == null || conf.getConditions().getChangeId() == null) {
			return DEFAULT_CONDITION;
		}
		return conf.getConditions().getChangeId();
	}

	public String getCondition(LscModificationType operation) {
		String result = DEFAULT_CONDITION;
		switch (operation) {
			case CREATE_OBJECT:
				result = this.getCreateCondition();
				break;
			case UPDATE_OBJECT:
				result = this.getUpdateCondition();
				break;
			case DELETE_OBJECT:
				result = this.getDeleteCondition();
				break;
			case CHANGE_ID:
				result = this.getChangeIdCondition();
				break;
		}
		return result;
	}

	public OutputFormat getPostHookOutputFormat() {
		// default: returns LDIF format
		if (conf.getHooks() == null || conf.getHooks().getOutputFormat() == null) {
			return OutputFormat.LDIF;
		}
		switch(conf.getHooks().getOutputFormat()){
			case "json":
				return OutputFormat.JSON;
			default:
				return OutputFormat.LDIF;
		}
	}

	public Optional<String> getCreatePostHook() {
		Optional<String> hook = Optional.ofNullable(conf.getHooks()).map(o -> o.getCreatePostHook()).filter(s -> !s.isEmpty());
		return hook;
	}

	public Optional<String> getDeletePostHook() {
		Optional<String> hook = Optional.ofNullable(conf.getHooks()).map(o -> o.getDeletePostHook()).filter(s -> !s.isEmpty());
		return hook;
	}

	public Optional<String> getUpdatePostHook() {
		Optional<String> hook = Optional.ofNullable(conf.getHooks()).map(o -> o.getUpdatePostHook()).filter(s -> !s.isEmpty());
		return hook;
	}

	public Optional<String> getChangeIdPostHook() {
		Optional<String> hook = Optional.ofNullable(conf.getHooks()).map(o -> o.getChangeIdPostHook()).filter(s -> !s.isEmpty());
		return hook;
	}

	public Optional<String> getPostHook(LscModificationType operation) {
		switch (operation) {
			case CREATE_OBJECT:
				return this.getCreatePostHook();
			case UPDATE_OBJECT:
				return this.getUpdatePostHook();
			case DELETE_OBJECT:
				return this.getDeletePostHook();
			case CHANGE_ID:
				return this.getChangeIdPostHook();
		}
		return Optional.empty();
	}
	
	public String getDelimiter(String name) {
        DatasetType dataset = LscConfiguration.getDataset(conf, name);
        if(dataset != null && dataset.getDelimiter() != null) {
            return dataset.getDelimiter();
        } else {
            return conf.getDefaultDelimiter();
        }
	}
}
