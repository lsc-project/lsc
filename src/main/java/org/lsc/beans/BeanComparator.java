/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 *
 * Copyright (c) 2008, LSC Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:

 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *    * Neither the name of the LSC Project nor the names of its
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
 *               (c) 2008 - 2009 LSC Project
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.beans;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.beans.syncoptions.ISyncOptions.STATUS_TYPE;
import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;
import org.lsc.utils.JScriptEvaluator;
import org.lsc.utils.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bean comparison to generate the JndiModification array
 *
 * This class is used to generate the modifications to be applied to the
 * directory according the differences between two beans.
 *
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 * @author Jonathan Clarke &lt;jon@lsc-project.org&gt;
 */
public final class BeanComparator {

	/**
	 * This class must not be called as an instance.
	 */
	private BeanComparator() {
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(BeanComparator.class);

	/**
	 * Static method to return the kind of operation that would happen
	 *
	 * @param syncOptions SyncOptions object from properties
	 * @param srcBean Bean from source
	 * @param dstBean JNDI bean
	 * @param customLibrary User-specified object to add to the JavaScript execution environment
	 * @return JndiModificationType the modification type that would happen
	 * @throws CloneNotSupportedException
	 */
	public static JndiModificationType calculateModificationType(ISyncOptions syncOptions,
					IBean srcBean, IBean dstBean, Object customLibrary) throws CloneNotSupportedException {
		// no beans, nothing to do
		if (srcBean == null && dstBean == null) {
			return null;
		}
		
		// if there is no source bean, we will delete the destination entry, if it exists
		if (srcBean == null && dstBean != null) {
			return JndiModificationType.DELETE_ENTRY;
		}
		
		// if there is no destination bean, we must create it
		if (dstBean == null) {
			return JndiModificationType.ADD_ENTRY;
		}

		// we have the object in the source and the destination
		// this must be either a MODIFY or MODRDN operation
		// clone the source bean to calculate modifications on the DN
		IBean itmBean = cloneSrcBean(srcBean, dstBean, syncOptions, customLibrary);
		if (!"".equals(itmBean.getDistinguishedName()) &&
				dstBean.getDistinguishedName().compareToIgnoreCase(itmBean.getDistinguishedName()) != 0) {
			return JndiModificationType.MODRDN_ENTRY;
		} else {
			return JndiModificationType.MODIFY_ENTRY;
		}
	}

	/**
	 * <p>Static comparison method.</p>
	 *
	 * <p>By default, source information override destination
	 * (i.e. Database => Directory) But if a piece of information is
	 * present only in the destination, it remains</p>
	 * 
	 * @param syncOptions Instance of {@link ISyncOptions} to use.
	 * @param srcBean Source bean from JDBC or JNDI
	 * @param destBean JNDI bean
	 * @param customLibrary 
	 * @return modifications to apply to the directory
	 * @throws NamingException an exception may be thrown if an LDAP data
	 * access error is encountered
	 * @throws CloneNotSupportedException 
	 * @deprecated Use {@link #calculateModifications(ISyncOptions, IBean, IBean, Object, boolean)}
	 */
	public static JndiModifications calculateModifications(ISyncOptions syncOptions, IBean srcBean, IBean destBean,
					Object customLibrary) throws NamingException, CloneNotSupportedException {

		// this method is deprecated so no need for optimizations
		// set condition to true, since using false is only useful for some optimizations after here
		boolean condition = true;
		return calculateModifications(syncOptions, srcBean, destBean, customLibrary, condition);
	}

	/**
	 * Static comparison method. By default, source information override
	 * destination (i.e. Database => Directory) But if a piece of information is
	 * present only in the destination, it remains
	 * 
	 * @param syncOptions Instance of {@link ISyncOptions} to use.
	 * @param srcBean
	 *            Source bean
	 * @param dstBean
	 *            JNDI bean
	 * @param customLibrary 
	 * @param condition
	 * @return modifications to apply to the directory
	 * @throws NamingException
	 *             an exception may be thrown if an LDAP data access error is
	 *             encountered
	 * @throws CloneNotSupportedException 
	 */
	public static JndiModifications calculateModifications(
					ISyncOptions syncOptions, IBean srcBean, IBean dstBean,
					Object customLibrary, boolean condition) throws NamingException,
					CloneNotSupportedException {

		JndiModifications jm = null;

		// clone the source bean to work on it
		IBean itmBean = cloneSrcBean(srcBean, dstBean, syncOptions, customLibrary);

		// get modification type to perform
		JndiModificationType modificationType = calculateModificationType(syncOptions, itmBean, dstBean, customLibrary);

		// if there's nothing to do, just return
		if (modificationType == null) {
			return null;
		}

		// prepare JndiModifications object
		jm = new JndiModifications(modificationType, syncOptions.getTaskName());
		jm.setDistinguishName(getDstDN(itmBean, dstBean, condition));

		switch (modificationType) {
			case ADD_ENTRY:
			case MODIFY_ENTRY:
				jm = getAddModifyEntry(jm, syncOptions, srcBean, itmBean, dstBean, customLibrary);
				break;

			case MODRDN_ENTRY:
				// WARNING: updating the RDN of the entry will cancel other
				// modifications! Relaunch synchronization to complete update
				jm.setNewDistinguishName(itmBean.getDistinguishedName());
				break;

			default:
				break;
		}

		return jm;
	}

	private static String getDstDN(IBean itmBean, IBean dstBean,
					boolean condition) throws NamingException {
		// If we already know which object we're aiming for in the destination,
		// we have the DN
		if (dstBean != null) {
			return dstBean.getDistinguishedName();
		}

		// If the itmBean has a DN set, use that (this is where JavaScript
		// generated DNs come from)
		if (itmBean != null && itmBean.getDistinguishedName() != null) {
			return itmBean.getDistinguishedName();
		}

		// At this stage, we don't have a real DN to use.

		// If we're not really going to create the entry, silently return a
		// pseudo value
		if (false == condition) {
			// condition is false, we're not really going to create the entry
			// set a pseudo DN to use for display purposes
			return "No DN set! Read it from the source or set lsc.tasks.NAME.dn";
		}

		throw new RuntimeException("No DN set! Read it from the source or set lsc.tasks.NAME.dn");
	}

	/**
	 * Compare attributes and values to build a list of modifications to apply
	 * to the destination for one object.
	 *
	 * @param modOperation
	 *            Operation to be done on the entry (should only be of type ADD or MODIFY)
	 * @param syncOptions
	 *            Instance of {@link ISyncOptions} to provide transformation configuration
	 * @param srcBean
	 *            The original bean read from the source
	 * @param itmBean
	 *            The source bean with local modifications (default and force values, DN renaming)
	 * @param dstBean
	 *            The original bean read from the destination
	 * @param customLibrary
	 *            An optional class to pass into the JavaScript interpreter
	 * @return {@link JndiModifications} List of modifications to apply to the destination
	 * @throws NamingException
	 * @throws CloneNotSupportedException
	 */
	private static JndiModifications getAddModifyEntry(
					JndiModifications modOperation, ISyncOptions syncOptions,
					IBean srcBean, IBean itmBean, IBean dstBean, Object customLibrary)
					throws NamingException, CloneNotSupportedException {

		String dn = modOperation.getDistinguishName();
		String logPrefix = "In entry \"" + dn + "\": ";

		// This method only handles ADD or MODIFY
		JndiModificationType modType = modOperation.getOperation();
		if (modType != JndiModificationType.ADD_ENTRY && modType != JndiModificationType.MODIFY_ENTRY) {
			return null;
		}

		// Set up JavaScript objects
		Map<String, Object> javaScriptObjects = new HashMap<String, Object>();
		if (srcBean != null) {
			javaScriptObjects.put("srcBean", srcBean);
		}
		if (dstBean != null) {
			javaScriptObjects.put("dstBean", dstBean);
		}
		if (customLibrary != null) {
			javaScriptObjects.put("custom", customLibrary);
		}

		// We're going to iterate over the list of attributes we may write
		Set<String> writeAttributes = getWriteAttributes(syncOptions, itmBean);
		LOGGER.debug("{} List of attributes considered for writing in destination: {}", logPrefix, writeAttributes);

		// Iterate over attributes we may write
		List<ModificationItem> modificationItems = new ArrayList<ModificationItem>();
		for (String attrName : writeAttributes) {
			// Get attribute status type
			STATUS_TYPE attrStatus = syncOptions.getStatus(dn, attrName);
			LOGGER.debug("{} Attribute \"{}\" is in {} status",
							new Object[]{logPrefix, attrName, attrStatus});

			// Get the current attribute values from source and destination
			Attribute srcAttr = (itmBean != null ? itmBean.getAttributeById(attrName) : null);
			Attribute dstAttr = (dstBean != null ? dstBean.getAttributeById(attrName) : null);

			// Add attributes to JavaScript objects
			if (srcAttr != null) {
				javaScriptObjects.put("srcAttr", srcAttr);
			}
			if (dstAttr != null) {
				javaScriptObjects.put("dstAttr", dstAttr);
			}

			// Use a list of values for easier handling
			Set<Object> srcAttrValues = SetUtils.attributeToSet(srcAttr);
			Set<Object> dstAttrValues = SetUtils.attributeToSet(dstAttr);

			// Get list of values that the attribute should be set to in the destination
			Set<Object> toSetAttrValues = getValuesToSet(attrName, srcAttrValues, syncOptions, javaScriptObjects, modType);

			// Convention: if values to set is returned null, ignore this attribute
			if (toSetAttrValues == null) {
				continue;
			}
			
			// What operation do we need to do on this attribute?
			int operationType = getRequiredOperationForAttribute(toSetAttrValues, dstAttrValues);

			// Build the modification
			ModificationItem mi = null;
			switch (operationType) {
				case DirContext.REMOVE_ATTRIBUTE:
					if (attrStatus == STATUS_TYPE.FORCE) {
						LOGGER.debug("{} Deleting attribute  \"{}\"", logPrefix, attrName);
						mi = new ModificationItem(operationType, new BasicAttribute(attrName));
					}

					break;

				case DirContext.ADD_ATTRIBUTE:
					LOGGER.debug("{} Adding attribute \"{}\" with values {}",
									new Object[]{logPrefix, attrName, toSetAttrValues});

					if (modType != JndiModificationType.ADD_ENTRY && attrStatus == STATUS_TYPE.FORCE) {
						// By default, if we try to modify an attribute in
						// the destination entry, we have to care to replace all
						// values in the following conditions:
						// - FORCE action is used;
						// - A value is specified by the create_value parameter.
						// So, instead of add the attribute, we replace it.
						operationType = DirContext.REPLACE_ATTRIBUTE;
					}

					mi = new ModificationItem(operationType, SetUtils.setToAttribute(attrName, toSetAttrValues));

					break;

				case DirContext.REPLACE_ATTRIBUTE:
					if (attrStatus == STATUS_TYPE.FORCE) {
						if (!SetUtils.doSetsMatch(toSetAttrValues, dstAttrValues)) {
							Attribute replaceAttr = SetUtils.setToAttribute(dstAttr.getID(), toSetAttrValues);

							LOGGER.debug("{} Replacing attribute \"{}\": source values are {}, old values were {}, new values are {}",
											new Object[]{logPrefix, attrName, srcAttrValues, dstAttrValues, toSetAttrValues});
							mi = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, replaceAttr);
						}
					} else if (attrStatus == STATUS_TYPE.MERGE) {
						// check if there are any extra values to be added
						Set<?> missingValues = SetUtils.findMissingNeedles(dstAttrValues, toSetAttrValues);

						if (missingValues.size() > 0) {
							Attribute addValuesAttr = SetUtils.setToAttribute(dstAttr.getID(), missingValues);

							LOGGER.debug("{} Adding values to attribute \"{}\": new values are {}",
											new Object[]{logPrefix, attrName, missingValues});
							mi = new ModificationItem(DirContext.ADD_ATTRIBUTE, addValuesAttr);
						}
					}

					break;

			}

			if (mi == null) {
				LOGGER.debug("{} Attribute \"{}\" will not be written to the destination", logPrefix, attrName);
			} else {
				modificationItems.add(mi);
			}

			// Remove attributes from JavaScript objects
			if (srcAttr != null) {
				javaScriptObjects.remove("srcAttr");
			}
			if (dstAttr != null) {
				javaScriptObjects.remove("dstAttr");
			}
		}

		JndiModifications result = null;
		if (modificationItems.size() != 0) {
			result = modOperation;
			result.setModificationItems(modificationItems);
		} else {
			LOGGER.debug("Entry \"{}\" will not be written to the destination", dn);
		}

		return result;
	}

	/**
	 * <P>
	 * Return the operation to perform on a set of current values, so that they
	 * match the set of values wanted.
	 * </P>
	 *
	 * <P>
	 * The result returned is an integer representing the operation type from
	 * DirContext: ADD_ATTRIBUTE, REPLACE_ATTRIBUTE or REMOVE_ATTRIBUTE. As a
	 * convention, a return value of 0 means "do nothing".
	 * </P>
	 *
	 * @param toSetAttrValues
	 *            Target set of values
	 * @param currentAttrValues
	 *            Current set of values
	 * @return Operation to perform: {@link DirContext} constants, or 0 for no operation.
	 */
	private static int getRequiredOperationForAttribute(
					Set<Object> toSetAttrValues, Set<Object> currentAttrValues) {
		if (toSetAttrValues.size() == 0 && currentAttrValues.size() != 0) {
			return DirContext.REMOVE_ATTRIBUTE;
		} else if (toSetAttrValues.size() > 0 && currentAttrValues.size() == 0) {
			return DirContext.ADD_ATTRIBUTE;
		} else if (toSetAttrValues.size() > 0 && currentAttrValues.size() > 0) {
			return DirContext.REPLACE_ATTRIBUTE;
		} else {
			return 0;
		}
	}

	/**
	 * <P>
	 * Return the set of attribute names that may be updated in the destination
	 * </P>
	 * <P>
	 * This list is read from the destination server configuration property
	 * "lsc.tasks.taskname.dstService.attrs", if it is defined. If it is not
	 * defined, then the list of attributes returned contains all source
	 * attributes, and all force valued/default valued/create valued attributes.
	 * </P>
	 *
	 * @param syncOptions
	 *            Instance of {@link ISyncOptions} to provide transformation
	 *            configuration
	 * @param srcBean
	 *            The original bean read from the source
	 * @return Set of attribute names to be updated
	 */
	private static Set<String> getWriteAttributes(ISyncOptions syncOptions, IBean srcBean) {
		Set<String> res = new HashSet<String>();

		// Check if an explicit list was configured
		List<String> syncOptionsWriteAttributes = syncOptions.getWriteAttributes();

		if (syncOptionsWriteAttributes != null) {
			for (String attrName : syncOptionsWriteAttributes) {
				res.add(attrName);
			}
		}

		// If no explicit list of attribute types to write is specified,
		// we build a list from all source attributes, all force and default values
		if (res.size() == 0) {
			Set<String> itmBeanAttrsList = srcBean.getAttributesNames();
			Set<String> forceAttrsList = syncOptions.getForceValuedAttributeNames();
			Set<String> defaultAttrsList = syncOptions.getDefaultValuedAttributeNames();
			Set<String> createAttrsList = syncOptions.getCreateAttributeNames();

			if (itmBeanAttrsList != null) res.addAll(itmBeanAttrsList);
			if (forceAttrsList != null) res.addAll(forceAttrsList);
			if (defaultAttrsList != null) res.addAll(defaultAttrsList);
			if (createAttrsList != null) res.addAll(createAttrsList);
		}

		return res;
	}

	/**
	 * Check modifications across other directory objects - Never used at this time : implementation may be buggy
	 *
	 * While adding, deleting or modifying an entry, specific treatments must be done like removing a member from all
	 * the remaining inscription, modifying an attribute in the person entry while the original modification has been
	 * done on a inscription.
	 *
	 * @param srcBean database object bean
	 * @param destBean directory object bean
	 * @param jm modification to apply on the main object
	 * @return Array of {@link JndiModifications}
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	public static JndiModifications[] checkOtherModifications(IBean srcBean, IBean destBean, JndiModifications jm)
					throws IllegalAccessException, InvocationTargetException {
		String methodName = "checkDependencies";
		Class<?>[] params = new Class[]{JndiModifications.class};
		try {
			Method checkDependencies = destBean.getClass().getMethod(methodName, params);
			if (checkDependencies != null) {
				return (JndiModifications[]) checkDependencies.invoke(destBean, new Object[]{jm});
			}
		} catch (SecurityException e) {
			LOGGER.warn("Unattended exception has been raised : {}", e.toString());
			LOGGER.debug(e.toString(), e);
		} catch (NoSuchMethodException e) {
			LOGGER.debug("No method \"{}\" to manage modification dependencies {} ({})",
							new Object[]{methodName, destBean.getClass().getName(), e});
		}
		return new JndiModifications[]{};
	}

	/**
	 * <p>
	 * Clone the source bean and return a new object that is a copy of the
	 * srcBean and includes any modifications on the DN.
	 * </p>
	 * <p>
	 * Always use this method for source/destination compares, and make sure to
	 * only change the result intermediary bean, never the original source bean
	 * </p>
	 *
	 * @param srcBean Original bean from source
	 * @param dstBean Destination bean
	 * @param syncOptions
	 * @param customLibrary
	 * @return New bean cloned from srcBean
	 * @throws CloneNotSupportedException
	 */
	private static IBean cloneSrcBean(IBean srcBean, IBean dstBean, ISyncOptions syncOptions,
					Object customLibrary) throws CloneNotSupportedException {
		//
		// We clone the source object, because syncoptions should not be used
		// on modified values of the source object :)
		//
		IBean itmBean = null;
		if (srcBean != null) {
			itmBean = srcBean.clone();

			// apply any new DN from properties to this intermediary bean
			String dn = syncOptions.getDn();
			if (dn != null) {
				Map<String, Object> table = new HashMap<String, Object>();
				table.put("srcBean", srcBean);
				table.put("dstBean", dstBean);
				if (customLibrary != null) {
					table.put("custom", customLibrary);
				}
				itmBean.setDistinguishedName(JScriptEvaluator.evalToString(dn, table));
			}
		}

		return itmBean;
	}

	/**
	 * <P>
	 * Build a set of values for a given attribute, that should be set in the
	 * destination repository.
	 * </P>
	 * <P>
	 * This method implements logic regarding precedence of:
	 * <ol>
	 * <li>Force values</li>
	 * <li>Original source values</li>
	 * <li>Default values and create values</li>
	 * </ol>
	 * </P>
	 * <P>
	 * It also handles the special case of MERGE attributes. In this case, the
	 * resulting list of values is the union of:
	 * <ul>
	 * <li>Original source values</li>
	 * <li>Original destination values</li>
	 * <li>Default values or create values</li>
	 * </ul>
	 * </P>
	 *
	 * <P>The special case where an attribute is considered only for setting
	 * values on creation is handled by returning null, if this is not a create
	 * operation. This indicates this attribute should not be considered for
	 * synchronization. In all other cases, a valid List will be returned (maybe
	 * empty, of course).</P>
	 *
	 * @param attrName
	 *            {@link String} Name of the attribute to be considered. Used to
	 *            read default/force values from syncoptions.
	 * @param srcAttrValues
	 *            {@link Set}<Object> All values of the considered attribute
	 *            read from the source.
	 * @param syncOptions
	 *            {@link ISyncOptions} Object to read syncoptions from
	 *            configuration.
	 * @param javaScriptObjects
	 *            {@link Map}<String, Object> Object map to pass objects into
	 *            JavaScript environment.
	 * @param modType
	 *            {@link JndiModificationType} Modification type, to determine
	 *            if the object is being newly created (causes
	 *            create values to be used instead of default values)
	 * @return List<Object> The list of values that should be set in the
	 *         destination, or null if this attribute should be ignored.
	 * @throws NamingException
	 */
	protected static Set<Object> getValuesToSet(String attrName,
					Set<Object> srcAttrValues, ISyncOptions syncOptions,
					Map<String, Object> javaScriptObjects, JndiModificationType modType)
					throws NamingException {
		// Result
		Set<Object> attrValues = new HashSet<Object>();

		// If we have force values, they take precedence over anything else, just use them
		List<String> forceValueDefs = syncOptions.getForceValues(null, attrName);
		if (forceValueDefs != null) {
			for (String forceValueDef : forceValueDefs) {
				List<String> forceValues = JScriptEvaluator.evalToStringList(forceValueDef, javaScriptObjects);
				if (forceValues != null) {
					attrValues.addAll(forceValues);
				}
			}

			return attrValues;
		}

		// No force values
		// If we have source values, use them
		if (srcAttrValues != null && srcAttrValues.size() > 0) {
			attrValues.addAll(srcAttrValues);
		}

		// Add default or create values if:
		// a) there are no values yet, or
		// b) attribute is in Merge status
		if (attrValues.size() == 0 || syncOptions.getStatus(null, attrName) == STATUS_TYPE.MERGE) {
			List<String> newValuesDefs;
			if (modType == JndiModificationType.ADD_ENTRY) {
				newValuesDefs = syncOptions.getCreateValues(null, attrName);
			} else {
				newValuesDefs = syncOptions.getDefaultValues(null, attrName);
			}
			if (newValuesDefs != null) {
				for (String defaultValueDef : newValuesDefs) {
					List<String> defaultValues = JScriptEvaluator.evalToStringList(defaultValueDef, javaScriptObjects);
					if (defaultValues != null) {
						attrValues.addAll(defaultValues);
					}
				}
			}
		}
		
		// special case : is this attribute configured for create_values only?
		// if so, and if there are no values, and this is not an add operation,
		// ignore this attribute
		// by convention, returning null ignores this attribute
		if (attrValues.size() == 0 
				&& modType != JndiModificationType.ADD_ENTRY 
				&& syncOptions.getCreateValues(null, attrName) != null) {
			return null;
		}

		return attrValues;
	}
}
