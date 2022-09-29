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
package org.lsc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lsc.beans.IBean;
import org.lsc.utils.CaseIgnoreStringHashMap;

/**
 * Single object used to store all modifications on one object.
 *
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class LscModifications {

	/** The object main identifier. */
	private String mainId;
	/** If the entry is renamed, the new value. */
	private String newMainId;
	/** Operation must be DirContext. */
	private LscModificationType operation;
	/** This list contains attributes modifications. */
	private List<LscDatasetModification> attributesModifications;
	/** The task that these modifications concern */
	private String taskName;
	/** The source object */
	private IBean sourceBean;
	/** The destination object */
	private IBean destinationBean;

	/**
	 * Standard constructor.
	 * @param operation the main modification type defining this object
	 */
	public LscModifications(final LscModificationType operation) {
		this.operation = operation;
		attributesModifications = new ArrayList<LscDatasetModification>();	
	}

	/**
	 * Constructor.
	 *
	 * @param operation the main modification type defining this object
	 * @param taskName name of the task we're building modifications for
	 */
	public LscModifications(final LscModificationType operation, String taskName) {
		this.operation = operation;
		this.taskName = taskName;
		attributesModifications = new ArrayList<LscDatasetModification>();	
	}

	/**
	 * Default modifications items getter.
	 * @return the modifications items list
	 */
	public final List<LscDatasetModification> getLscAttributeModifications() {
		return attributesModifications;
	}

	/**
	 * Attribute modifications list setter.
	 * @param attrsMod a list of LscAttributeModification objects
	 */
	public final void setLscAttributeModifications(final List<LscDatasetModification> attrsMod) {
		attributesModifications = attrsMod;
	}

	/**
	 * Default operation getter.
	 * @return the operation type
	 */
	public final LscModificationType getOperation() {
		return operation;
	}

	/**
	 * Default operation setter.
	 * @param operation the operation type
	 */
	public final void setOperation(final LscModificationType operation) {
		this.operation = operation;
	}

	/**
	 * Default distinguish name getter.
	 * @return the primary distinguish name
	 */
	public final String getMainIdentifier() {
		return mainId;
	}

	/**
	 * Default main identifier setter.
	 * @param mainId Name the primary identifier
	 */
	public final void setMainIdentifer(final String mainId) {
		this.mainId = mainId;
	}

	/**
	 * Default new distinguish name getter.
	 * @return the new distinguish name
	 */
	public final String getNewMainIdentifier() {
		return newMainId;
	}

	/**
	 * Default distinguish name setter.
	 * @param newMainId the primary distinguish name
	 */
	public final void setNewMainIdentifier(final String newMainId) {
		this.newMainId = newMainId;
	}

	/**
	 * Return all the modification in a hash indexed by the name of the attribute
	 *
	 * @return the hash
	 */
	public Map<String, List<Object>> getModificationsItemsByHash() {
		HashMap<String, List<Object>> result = new CaseIgnoreStringHashMap<List<Object>>();
		List<LscDatasetModification> mi = this.getLscAttributeModifications();

		if (mi != null) {
			for (LscDatasetModification attributeModification : mi) {
				result.put(attributeModification.getAttributeName().toLowerCase(), attributeModification.getValues());
			}
		}
		return result;
	}

	/**
	 * @return the taskName
	 */
	public String getTaskName() {
		return taskName;
	}

	/**
	 * @param taskName the taskName to set
	 */
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("mainid: ").append(getMainIdentifier()).append("\n");
		if(getNewMainIdentifier() != null) { sb.append("newmainid: ").append(getNewMainIdentifier()).append("\n"); }
		sb.append("operation: ").append(getOperation().toString()).append("\n");
		for(LscDatasetModification lam: getLscAttributeModifications()) {
			sb.append(lam.getAttributeName()).append(": ").append(lam.getOperation().toString()).append("\n");
			for(Object value: lam.getValues()) {
				sb.append(" - ").append(value).append("\n");
			}
		}
		return sb.append("\n").toString();
	}

	public IBean getSourceBean() {
		return sourceBean;
	}

	public void setSourceBean(IBean sourceBean) {
		this.sourceBean = sourceBean;
	}

	public IBean getDestinationBean() {
		return destinationBean;
	}

	public void setDestinationBean(IBean destinationBean) {
		this.destinationBean = destinationBean;
	}
}
