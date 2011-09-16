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
package org.lsc.beans.syncoptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lsc.LscModificationType;
import org.lsc.configuration.objects.Task;
import org.lsc.configuration.objects.services.DstDatabase;
import org.lsc.configuration.objects.services.DstLdap;
import org.lsc.configuration.objects.syncoptions.PBSODataset;

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

	private Task task;

	private org.lsc.configuration.objects.syncoptions.PropertiesBasedSyncOptions conf;

	public void initialize(Task task) {
		this.task = task;
		conf = (org.lsc.configuration.objects.syncoptions.PropertiesBasedSyncOptions) task.getSyncOptions();
	}
	
	public final STATUS_TYPE getStatus(final String id, final String attributeName) {
		STATUS_TYPE  statusType = conf.getDataset(attributeName).getPolicy();
		return (statusType == STATUS_TYPE.UNKNOWN ? conf.getDefaultPolicy() : statusType);
	}

	public final List<String> getDefaultValues(final String id, final String attributeName) {
		List<String> values = conf.getDataset(attributeName).getDefaultValues();
		ArrayList<String> copy = null;
		if (values != null && values.size() > 0) {
			copy = new ArrayList<String>(values);
		}
		return copy;
	}

	public final List<String> getCreateValues(final String id, final String attributeName) {
		List<String> values = conf.getDataset(attributeName).getCreateValues();
		ArrayList<String> copy = null;
		if (values != null && values.size() > 0) {
			copy = new ArrayList<String>(values);
		}
		return copy;
	}

	public final List<String> getForceValues(final String id, final String attributeName) {
		List<String> values = conf.getDataset(attributeName).getForceValues();
		ArrayList<String> copy = null;
		if (values != null && values.size() > 0) {
			copy = new ArrayList<String>(values);
		}
		return copy;
	}

	public List<String> getWriteAttributes() {
		if(task.getDestinationService() instanceof DstLdap) {
			return Arrays.asList(((DstLdap)task.getDestinationService()).getFetchedAttributes());
		} else if (task.getDestinationService() instanceof DstDatabase) {
			return ((DstDatabase)task.getDestinationService()).getFetchedAttributes();
		}
		return new ArrayList<String>();
	}

	@Override
	public Set<String> getCreateAttributeNames() {
		Set<String> createAttrs = new HashSet<String>();
		for(PBSODataset attr : conf.getDatasets()) {
			if(!attr.getCreateValues().isEmpty()) {
				createAttrs.add(attr.getName());
			}
		}
		return createAttrs;
	}


	@Override
	public Set<String> getDefaultValuedAttributeNames() {
		Set<String> createAttrs = new HashSet<String>();
		for(PBSODataset attr : conf.getDatasets()) {
			if(!attr.getDefaultValues().isEmpty()) {
				createAttrs.add(attr.getName());
			}
		}
		return createAttrs;
	}


	@Override
	public Set<String> getForceValuedAttributeNames() {
		Set<String> createAttrs = new HashSet<String>();
		for(PBSODataset attr : conf.getDatasets()) {
			if(!attr.getForceValues().isEmpty()) {
				createAttrs.add(attr.getName());
			}
		}
		return createAttrs;
	}
	
	public String getDn() {
		return conf.getMainIdentifier();
	}


	public String getCreateCondition() {
		String condition = conf.getConditions().getCreate();
		if (condition == null) {
			return DEFAULT_CONDITION;
		}
		return condition;
	}

	public String getDeleteCondition() {
		String condition = conf.getConditions().getDelete();
		if (condition == null) {
			return DEFAULT_CONDITION;
		}
		return condition;
	}

	public String getUpdateCondition() {
		String condition = conf.getConditions().getUpdate();
		if (condition == null) {
			return DEFAULT_CONDITION;
		}
		return condition;
	}

	public String getChangeIdCondition() {
		String condition = conf.getConditions().getChangeId();
		if (condition == null) {
			return DEFAULT_CONDITION;
		}
		return condition;
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
}
