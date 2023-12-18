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
 *               (c) 2008 - 2011 LSC Project
 *         Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 *         Thomas Chemineau &lt;thomas@lsc-project.org&gt;
 *         Jonathan Clarke &lt;jon@lsc-project.org&gt;
 *         Remy-Christophe Schermesser &lt;rcs@lsc-project.org&gt;
 ****************************************************************************
 */
package org.lsc.beans;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;

import org.lsc.Configuration;
import org.lsc.LscDatasetModification;
import org.lsc.LscDatasetModification.LscDatasetModificationType;
import org.lsc.LscModificationType;
import org.lsc.LscModifications;
import org.lsc.Task;
import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.configuration.LscConfiguration;
import org.lsc.configuration.PolicyType;
import org.lsc.exception.LscServiceException;
import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;
import org.lsc.utils.ScriptingEvaluator;
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
	 * @param task task object (used for syncoptions, custom library and source/destination service)
	 * @param srcBean Bean from source
	 * @param dstBean JNDI bean
	 * @return JndiModificationType the modification type that would happen
	 * @throws LscServiceException
	 */
	public static LscModificationType calculateModificationType(Task task,
					IBean srcBean, IBean dstBean) throws LscServiceException {
		// no beans, nothing to do
		if (srcBean == null && dstBean == null) {
			return null;
		}
		
		// if there is no source bean, we will delete the destination entry, if it exists
		if (srcBean == null && dstBean != null) {
			return LscModificationType.DELETE_OBJECT;
		}
		
		// if there is no destination bean, we must create it
		if (dstBean == null) {
			return LscModificationType.CREATE_OBJECT;
		}

		// we have the object in the source and the destination
		// this must be either a MODIFY or MODRDN operation
		// clone the source bean to calculate modifications on the DN
		IBean itmBean = cloneSrcBean(task, srcBean, dstBean);
		if (!"".equals(itmBean.getMainIdentifier()) &&
				dstBean.getMainIdentifier().compareToIgnoreCase(itmBean.getMainIdentifier()) != 0) {
			return LscModificationType.CHANGE_ID;
		} else {
			return LscModificationType.UPDATE_OBJECT;
		}
	}

	/**
	 * Static comparison method. By default, source information override
	 * destination (i.e. Database =&gt; Directory) But if a piece of information is
	 * present only in the destination, it remains
	 * 
	 * @param task the corresponding task parameter
	 * @param srcBean the source bean
	 * @param dstBean the destination bean
         * @throws org.lsc.exception.LscServiceException lsc service
	 * @return modifications to apply to the directory
	 */
	public static LscModifications calculateModifications(
					Task task, IBean srcBean, IBean dstBean) 
					throws LscServiceException {

		LscModifications lm = null;

		// clone the source bean to work on it
		IBean itmBean = cloneSrcBean(task, srcBean, dstBean);

		// get modification type to perform
		LscModificationType modificationType = calculateModificationType(task, srcBean, dstBean);

		// if there's nothing to do, just return
		if (modificationType == null) {
			return null;
		}

		// prepare JndiModifications object
		lm = new LscModifications(modificationType, task.getName());
		lm.setSourceBean(srcBean);
		lm.setDestinationBean(dstBean);
		lm.setMainIdentifer(getDstDN(itmBean, dstBean));

		switch (modificationType) {
			case CREATE_OBJECT:
			case UPDATE_OBJECT:
				lm = getUpdatedObject(task, lm, srcBean, itmBean, dstBean);
				break;

			case CHANGE_ID:
				// WARNING: updating the RDN of the entry will cancel other
				// modifications! Relaunch synchronization to complete update
				lm.setNewMainIdentifier(itmBean.getMainIdentifier());
				break;

			default:
				break;
		}

		return lm;
	}

	private static String getDstDN(IBean itmBean, IBean dstBean) {
		// If we already know which object we're aiming for in the destination,
		// we have the DN
		if (dstBean != null) {
			return dstBean.getMainIdentifier();
		}

		// If the itmBean has a DN set, use that (this is where JavaScript
		// generated DNs come from)
		if (itmBean != null && itmBean.getMainIdentifier() != null) {
			return itmBean.getMainIdentifier();
		}

		throw new RuntimeException("No DN set! Read it from the source or set " + Configuration.LSC_TASKS_PREFIX + ".NAME.dn");
	}

	/**
	 * Compare attributes and values to build a list of modifications to apply
	 * to the destination for one object.
	 *
	 * @param modOperation
	 *            Operation to be done on the entry (should only be of type ADD or MODIFY)
	 * @param srcBean
	 *            The original bean read from the source
	 * @param itmBean
	 *            The source bean with local modifications (default and force values, DN renaming)
	 * @param dstBean
	 *            The original bean read from the destination
	 * @return {@link JndiModifications} List of modifications to apply to the destination
	 * @throws NamingException
	 * @throws CloneNotSupportedException
	 */
	private static LscModifications getUpdatedObject(
					Task task, LscModifications modOperation,
					IBean srcBean, IBean itmBean, IBean dstBean)
					throws LscServiceException {

		String id = modOperation.getMainIdentifier();
		String logPrefix = "In object \"" + id + "\": ";

		// This method only handles ADD or MODIFY
		LscModificationType modType = modOperation.getOperation();
		if (modType != LscModificationType.CREATE_OBJECT && modType != LscModificationType.UPDATE_OBJECT) {
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
		if (task.getCustomLibraries() != null) {
			javaScriptObjects.put("custom", task.getCustomLibraries());
		}
		javaScriptObjects.putAll(task.getScriptingVars());

		// We're going to iterate over the list of attributes we may write
		Set<String> writeAttributes = getWriteAttributes(task, itmBean);
		LOGGER.debug("{} List of attributes considered for writing in destination: {}", logPrefix, writeAttributes);

		// Iterate over attributes we may write
		List<LscDatasetModification> modificationItems = new ArrayList<LscDatasetModification>();
		for (String attrName : writeAttributes) {
			// Get attribute status type
			PolicyType attrStatus = task.getSyncOptions().getStatus(id, attrName);
			LOGGER.debug("{} Attribute \"{}\" is in {} status",
							new Object[]{logPrefix, attrName, attrStatus});

			// Get the current attribute values from source and destination
			Set<Object> srcAttrValues = (itmBean != null && itmBean.getDatasetById(attrName)!= null ? itmBean.getDatasetById(attrName) : new HashSet<Object>());
			Set<Object> dstAttrValues = (dstBean != null && dstBean.getDatasetById(attrName)!= null ? dstBean.getDatasetById(attrName) : new HashSet<Object>());
			Attribute srcAttr = (itmBean != null ? new BasicAttribute(attrName, srcAttrValues) : null);
			Attribute dstAttr = (dstBean != null ? new BasicAttribute(attrName, dstAttrValues) : null);

			// Add attributes to JavaScript objects
			if (srcAttrValues != null) {
				javaScriptObjects.put("srcValues", srcAttrValues);
				javaScriptObjects.put("srcAttr", srcAttr);
			}
			if (dstAttrValues != null) {
				javaScriptObjects.put("dstValues", dstAttrValues);
				javaScriptObjects.put("dstAttr", dstAttr);
			}

//			// Use a list of values for easier handling
//			Set<Object> srcAttrValues = SetUtils.attributeToSet(srcAttr);
//			Set<Object> dstAttrValues = SetUtils.attributeToSet(dstAttr);

			// Get list of values that the attribute should be set to in the destination
			Set<Object> toSetAttrValues = getValuesToSet(task, attrName, srcAttrValues, dstAttrValues, javaScriptObjects, modType);

			// Convention: if values to set is returned null, ignore this attribute
			if (toSetAttrValues == null) {
				continue;
			}
			
			// What operation do we need to do on this attribute?
			LscDatasetModificationType operationType = getRequiredOperationForAttribute(toSetAttrValues, dstAttrValues);

			// Build the modification
			List<LscDatasetModification> multiMi = null;
			LscDatasetModification mi = null;
			switch (operationType) {
				case DELETE_VALUES:
					if (attrStatus == PolicyType.FORCE) {
						LOGGER.debug("{} Deleting attribute  \"{}\"", logPrefix, attrName);
						mi = new LscDatasetModification(operationType, attrName, new HashSet<Object>());
					}

					break;

				case ADD_VALUES:
					LOGGER.debug("{} Adding attribute \"{}\" with values {}",
									new Object[]{logPrefix, attrName, toSetAttrValues});

					if (modType != LscModificationType.CREATE_OBJECT && attrStatus == PolicyType.FORCE) {
						// By default, if we try to modify an attribute in
						// the destination entry, we have to care to replace all
						// values in the following conditions:
						// - FORCE action is used;
						// - A value is specified by the create_value parameter.
						// So, instead of add the attribute, we replace it.
						operationType = LscDatasetModificationType.REPLACE_VALUES;
					}

					mi = new LscDatasetModification(operationType, attrName, toSetAttrValues);

					break;

				case REPLACE_VALUES:
					if (attrStatus == PolicyType.FORCE) {
						// check if there are any extra values to be added
						Set<Object> missingValues = SetUtils.findMissingNeedles(dstAttrValues, toSetAttrValues);
						// check if there are any extra values to be removed
						Set<Object> extraValues = SetUtils.findMissingNeedles(toSetAttrValues, dstAttrValues);

						if((missingValues.size() + extraValues.size()) >= toSetAttrValues.size()) {
							// More things to add and delete than remaining in the final set
							// so, replace with the final set directly.
							LOGGER.debug("{} Replacing attribute \"{}\": source values are {}, old values were {}, new values are {}",
											new Object[]{logPrefix, attrName, srcAttrValues, dstAttrValues, toSetAttrValues});
							mi = new LscDatasetModification(operationType, dstAttr.getID(), toSetAttrValues);
						} else {
							// Adding and deleting the values is less expensive than replacing everything
							multiMi = new ArrayList<LscDatasetModification>(2);

							if (missingValues.size() > 0) {
								LOGGER.debug("{} Adding values to attribute \"{}\": new values are {}",
								new Object[]{logPrefix, attrName, missingValues});
								multiMi.add(new LscDatasetModification(LscDatasetModificationType.ADD_VALUES, dstAttr.getID(), missingValues));
							}

							if (extraValues.size() > 0) {
								LOGGER.debug("{} Removing values from attribute \"{}\": old values are {}",
								new Object[]{logPrefix, attrName, extraValues});
								multiMi.add(new LscDatasetModification(LscDatasetModificationType.DELETE_VALUES, dstAttr.getID(), extraValues));
							}
						}
					} else if (attrStatus == PolicyType.MERGE) {
						// check if there are any extra values to be added
						Set<Object> missingValues = SetUtils.findMissingNeedles(dstAttrValues, toSetAttrValues);

						if (missingValues.size() > 0) {
							LOGGER.debug("{} Adding values to attribute \"{}\": new values are {}",
											new Object[]{logPrefix, attrName, missingValues});
							mi = new LscDatasetModification(LscDatasetModificationType.ADD_VALUES, dstAttr.getID(), missingValues);
						}
					}

					break;
			}

			if (mi == null && multiMi == null) {
				LOGGER.debug("{} Attribute \"{}\" will not be written to the destination", logPrefix, attrName);
			} else if (multiMi != null) {
				modificationItems.addAll(multiMi);
			} else if (mi != null) {
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

		LscModifications result = null;
		if (modificationItems.size() != 0) {
			result = modOperation;
			result.setLscAttributeModifications(modificationItems);
		} else {
			LOGGER.debug("Entry \"{}\" will not be written to the destination", id);
		}

		return result;
	}

	private static boolean isModified(IBean dstBean, Set<Object> dstAttrValues,
			Set<Object> toSetAttrValues) {
		if (dstBean instanceof OrderedValuesBean) {
			return !SetUtils.doSetsMatchWithOrder(toSetAttrValues, dstAttrValues);
		} else {
			return !SetUtils.doSetsMatch(toSetAttrValues, dstAttrValues);
		}
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
	 * @return Operation to perform: {@link LscDatasetModificationType} constants, or 0 for no operation.
	 */
	private static LscDatasetModificationType getRequiredOperationForAttribute (
					Set<Object> toSetAttrValues, Set<Object> currentAttrValues) {
		if (toSetAttrValues.size() == 0 && currentAttrValues.size() != 0) {
			return LscDatasetModificationType.DELETE_VALUES;
		} else if (toSetAttrValues.size() > 0 && currentAttrValues.size() == 0) {
			return LscDatasetModificationType.ADD_VALUES;
		} else if (toSetAttrValues.size() > 0 && currentAttrValues.size() > 0) {
			return LscDatasetModificationType.REPLACE_VALUES;
		} else {
//			LOGGER.warn("Check your default / create / force values because the expression has returned a null value !");
			return LscDatasetModificationType.UNKNOWN;
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
	private static Set<String> getWriteAttributes(Task task, IBean srcBean) {
		Set<String> res = new HashSet<String>();

		// Check if an explicit list was configured
		List<String> syncOptionsWriteAttributes = task.getDestinationService().getWriteDatasetIds();

		if (syncOptionsWriteAttributes != null) {
			for (String attrName : syncOptionsWriteAttributes) {
				res.add(attrName);
			}
		}

		// If no explicit list of attribute types to write is specified,
		// we build a list from all source attributes, all force and default values
		if (res.size() == 0) {
			List<String> itmBeanAttrsList = srcBean.datasets().getAttributesNames();
			Set<String> forceAttrsList = task.getSyncOptions().getForceValuedAttributeNames();
			Set<String> defaultAttrsList = task.getSyncOptions().getDefaultValuedAttributeNames();
			Set<String> createAttrsList = task.getSyncOptions().getCreateAttributeNames();

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
	 * @throws InvocationTargetException invocation target
	 * @throws IllegalAccessException illegal access
	 * @throws IllegalArgumentException illegal argument
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
	 * @param task
	 * @param srcBean Original bean from source
	 * @param dstBean Destination bean
	 * @return New bean cloned from srcBean
	 * @throws LscServiceException
	 */
	private static IBean cloneSrcBean(Task task, IBean srcBean, IBean dstBean) throws LscServiceException {
		//
		// We clone the source object, because syncoptions should not be used
		// on modified values of the source object :)
		//
		IBean itmBean = null;
		if (srcBean != null) {
		    try {
	            itmBean = srcBean.clone();
		    } catch(CloneNotSupportedException e) {
		        throw new LscServiceException(e);
		    }

			// apply any new DN from properties to this intermediary bean
			String dn = task.getSyncOptions().getDn();
			if (dn != null) {
				Map<String, Object> table = new HashMap<String, Object>();
				table.put("srcBean", srcBean);
				table.put("dstBean", dstBean);
				if (task.getCustomLibraries() != null) {
					table.put("custom", task.getCustomLibraries());
				}
				itmBean.setMainIdentifier(ScriptingEvaluator.evalToString(task, dn, table));
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
	 */
	protected static Set<Object> getValuesToSet(Task task, String attrName,
					Set<Object> srcAttrValues, Set<Object> dstAttrValues, Map<String, Object> javaScriptObjects, LscModificationType modType)
					throws LscServiceException {
		// Result
		Set<Object> attrValues = new LinkedHashSet<Object>();

		PolicyType attrPolicy = task.getSyncOptions().getStatus(null, attrName);
		
		// Ignore the attribute if the policy is to keep values and that the destination already contains at least a single value
		if(attrPolicy == PolicyType.KEEP && dstAttrValues.size() > 0) {
		   return null; 
		}
		
		// If we have force values, they take precedence over anything else, just use them
		List<String> forceValueDefs = task.getSyncOptions().getForceValues(null, attrName);
		if (forceValueDefs != null) {
			for (String forceValueDef : forceValueDefs) {
				List<? extends Object> forceValues = evaluateExpression(task, attrName, forceValueDef, javaScriptObjects);
				if (forceValues != null) {
					attrValues.addAll(forceValues);
				}
			}

			return splitValues(task, attrName, attrValues);
		}

		// No force values
		// If we have source values, use them
		if (srcAttrValues != null && srcAttrValues.size() > 0) {
			attrValues.addAll(srcAttrValues);
		}

		// Add default or create values if:
		// a) there are no values yet, or
		// b) attribute is in Merge status
		if (attrValues.size() == 0 || attrPolicy == PolicyType.MERGE) {
			List<String> newValuesDefs;
			if (modType == LscModificationType.CREATE_OBJECT) {
				newValuesDefs = task.getSyncOptions().getCreateValues(null, attrName);
			} else {
				newValuesDefs = task.getSyncOptions().getDefaultValues(null, attrName);
			}
			if (newValuesDefs != null) {
				for (String defaultValueDef : newValuesDefs) {
					List<? extends Object> defaultValues = evaluateExpression(task, attrName, defaultValueDef, javaScriptObjects);
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
				&& modType != LscModificationType.CREATE_OBJECT 
				&& task.getSyncOptions().getCreateValues(null, attrName) != null) {
			return null;
		}

		return splitValues(task, attrName, attrValues);
	}

	private static List<? extends Object> evaluateExpression(Task task, String attributeName, String expression, Map<String, Object> scriptingObjects) throws LscServiceException {
		if (LscConfiguration.isLdapBinaryAttribute(attributeName)) {
			return ScriptingEvaluator.evalToByteArrayList(task, expression, scriptingObjects);
		} else {
			return ScriptingEvaluator.evalToObjectList(task, expression, scriptingObjects);
		}
	}

    private static Set<Object> splitValues(Task task, String attrName, Set<Object> attrValues) {
        Set<Object> ret = new LinkedHashSet<Object>();
        for(Object value : attrValues) {
            if(value instanceof String) {
                String delimiter = task.getSyncOptions().getDelimiter(attrName);
                if(delimiter != null) {
                    StringTokenizer sTok = new StringTokenizer((String) value, delimiter);
                    while( sTok.hasMoreTokens() ) {
                        ret.add(sTok.nextToken());
                    }
                } else {
                    ret.add(value);
                }
            } else {
                ret.add(value);
            }
        }
        return ret;
    }
}
