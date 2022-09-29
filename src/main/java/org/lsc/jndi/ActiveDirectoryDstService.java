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
package org.lsc.jndi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.naming.CommunicationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.lsc.LscDatasetModification;
import org.lsc.LscModifications;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.exception.LscServiceException;
import org.lsc.utils.CaseIgnoreStringHashMap;

/**
 * A custom JNDI service to translate modifications on the user's "memberOf"
 * attribute to modifications on the "member" attribute of each groups. This is
 * the way to apply changes on groups with ActiveDirectory.
 * @author St&eacute;phane Bond &lt;&gt;
 */
public class ActiveDirectoryDstService extends SimpleJndiDstService {

	final String MEMBER_OF_ATTR = "memberOf";

	final String GROUP_MEMBER_ATTR = "member";

	public ActiveDirectoryDstService(TaskType task)
			throws LscServiceConfigurationException {
		super(task);
	}

	@Override
	public boolean apply(LscModifications lm) throws LscServiceException {
		boolean success = true;

		// Convert operations on "memberOf" to operations on the "member"
		// attribute of the group
		LscDatasetModification memberOfDm = null;
		for (int i = 0; i < lm.getLscAttributeModifications().size(); i++) {
			LscDatasetModification dm = lm.getLscAttributeModifications()
					.get(i);
			if (dm.getAttributeName().equals(MEMBER_OF_ATTR)) {
				memberOfDm = dm;
				lm.getLscAttributeModifications().remove(i);
				break;
			}
		}

		// Apply regular changes
		if (lm.getLscAttributeModifications().size() > 0
				|| lm.getNewMainIdentifier() != null) {
			success = super.apply(lm);
		}

		// Apply changes on memberships
		if (memberOfDm != null) {
			List<String> oldValues = getAttribute(lm.getMainIdentifier(),
					MEMBER_OF_ATTR);
			List<JndiModifications> memberOfChanges = computeChanges(
					lm.getMainIdentifier(), memberOfDm.getValues(), oldValues);
			try {
				for (JndiModifications jm : memberOfChanges) {
					success &= jndiServices.apply(jm);
				}
			} catch (CommunicationException e) {
				throw new LscServiceException(e);
			}
		}
		return success;
	}

	/**
	 * Compute changes to apply on groups attributes
	 * 
	 * @param userDn
	 * @param newValues
	 * @param oldValues
	 * @return
	 */
	protected List<JndiModifications> computeChanges(String userDn,
			List<?> newValues, List<?> oldValues) {

		List<JndiModifications> results = new ArrayList<JndiModifications>();

		// Attribute to modify
		Attribute attr = new BasicAttribute(GROUP_MEMBER_ATTR);
		attr.add(userDn);

		// Add operation on newValues not in oldValues
		for (String groupDn : valuesDiff(newValues, oldValues)) {
			JndiModifications jm = new JndiModifications(
					JndiModificationType.MODIFY_ENTRY);
			jm.setDistinguishName(groupDn);
			ModificationItem modItem = new ModificationItem(
					DirContext.ADD_ATTRIBUTE, attr);
			jm.setModificationItems(Arrays
					.asList(new ModificationItem[] { modItem }));
			results.add(jm);
		}
		// Remove operation on oldValues not in newValues
		for (String groupDn : valuesDiff(oldValues, newValues)) {
			JndiModifications jm = new JndiModifications(
					JndiModificationType.MODIFY_ENTRY);
			jm.setDistinguishName(groupDn);
			ModificationItem modItem = new ModificationItem(
					DirContext.REMOVE_ATTRIBUTE, attr);
			jm.setModificationItems(Arrays
					.asList(new ModificationItem[] { modItem }));
			results.add(jm);
		}
		return results;
	}

	/**
	 * Returns values from vals1 which are not present in vals2 (case
	 * insensitive)
	 * 
	 * @param vals1
	 * @param vals2
	 * @return
	 */
	protected Set<String> valuesDiff(List<?> vals1, List<?> vals2) {
		CaseIgnoreStringHashMap<?> diff = new CaseIgnoreStringHashMap<Object>();
		if (vals1 != null) {
			for (Object o : vals1) {
				diff.put(o.toString(), null);
			}
		}
		if (vals2 != null) {
			for (Object o : vals2) {
				diff.remove(o.toString());
			}
		}
		return diff.keySet();
	}

	/**
	 * Retrieve a specific attribute from an object
	 * 
	 * @param objectDn
	 * @param attribute
	 * @return
	 * @throws LscServiceException
	 */
	protected List<String> getAttribute(String objectDn, String attribute)
			throws LscServiceException {
		List<String> values = null;
		try {
			// Setup search
			SearchControls sc = new SearchControls();
			sc.setDerefLinkFlag(false);
			sc.setReturningAttributes(new String[] { attribute });
			sc.setSearchScope(SearchControls.OBJECT_SCOPE);
			sc.setReturningObjFlag(true);

			// Retrieve attribute values
			SearchResult res = jndiServices.getEntry(objectDn, "cn=*", sc);
			Attribute attr = res.getAttributes().get(attribute);
			if (attr != null) {
				values = new ArrayList<String>();
				NamingEnumeration<?> enu = attr.getAll();
				while (enu.hasMoreElements()) {
					Object val = enu.next();
					values.add(val.toString());
				}
			}
		} catch (NamingException e) {
			throw new LscServiceException(e);
		}
		return values;
	}

}
