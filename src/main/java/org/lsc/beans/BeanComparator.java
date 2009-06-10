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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.log4j.Logger;
import org.lsc.Configuration;
import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.beans.syncoptions.ISyncOptions.STATUS_TYPE;
import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;
import org.lsc.utils.JScriptEvaluator;

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
    private BeanComparator() {}

    /** LOG4J local logger. */
    private static final Logger LOGGER = Logger.getLogger(BeanComparator.class);

    /**
     * Static method to return the kind of operation that would happen
     * 
     * @param syncOptions SyncOptions object from properties
     * @param itmBean Modified bean from source
     * @param destBean JNDI bean
     * @return JndiModificationType the modification type that would happen
     */
    public static JndiModificationType calculateModificationType(ISyncOptions syncOptions, IBean itmBean, IBean destBean) {
    	if (itmBean == null && destBean == null) {
    		return null;
    	} else if (itmBean == null && destBean != null) {
    		return JndiModificationType.DELETE_ENTRY;
    	} else if (itmBean != null && destBean == null) {
    		return JndiModificationType.ADD_ENTRY;
    	} else { /* srcBean != null && destBean != null */
    		if (itmBean.getDistinguishName() == null
    				|| destBean.getDistinguishName().compareToIgnoreCase(itmBean.getDistinguishName()) == 0) {
    			return JndiModificationType.MODIFY_ENTRY;
    		} else {
    			return JndiModificationType.MODRDN_ENTRY;
    		}
    	}
    }
    
    /**
     * Static comparison method.
     * 
     * By default, source information override destination 
     * (i.e. Database => Directory) But if a piece of information is
     * present only in the destination, it remains
     * 
     * @param srcBean Source bean from JDBC or JNDI
     * @param destBean JNDI bean
     * @return modifications to apply to the directory
     * @throws NamingException an exception may be thrown if an LDAP data
     * access error is encountered
     * @deprecated
     */
    public static JndiModifications calculateModifications(ISyncOptions syncOptions, IBean srcBean, IBean destBean, 
            Object customLibrary)	throws NamingException, CloneNotSupportedException {

    	// this method is deprecated so no need for optimizations
    	// set condition to true, since using false is only useful for some optimizations after here
    	boolean condition = true;
    	return calculateModifications(syncOptions, srcBean, destBean, customLibrary, condition);
    }
    
    /**
     * Static comparison method.
     * 
     * By default, source information override destination 
     * (i.e. Database => Directory) But if a piece of information is
     * present only in the destination, it remains
     * 
     * @param srcBean Source bean from JDBC or JNDI
     * @param destBean JNDI bean
     * @param condition 
     * @return modifications to apply to the directory
     * @throws NamingException an exception may be thrown if an LDAP data
     * access error is encountered
     */
    public static JndiModifications calculateModifications(ISyncOptions syncOptions, IBean srcBean, IBean destBean, 
            Object customLibrary, boolean condition)	throws NamingException, CloneNotSupportedException {

		//
		// We clone the source object, because syncoptions should not be used
		// on modified values of the source object :)
		//
		IBean itmBean = null;
		if (srcBean != null)
		{
			itmBean = srcBean.clone();
		}

        JndiModifications jm = null;

        String dn = syncOptions.getDn();
        if(srcBean != null && dn != null) {
            Map<String, Object> table = new HashMap<String, Object>();
            table.put("srcBean", srcBean);
            if(customLibrary != null) {
                table.put("custom", customLibrary);
            }
            itmBean.setDistinguishName(JScriptEvaluator.evalToString(dn, table));
        }
        
        // get modification type to perform
        JndiModificationType modificationType = calculateModificationType(syncOptions, itmBean, destBean);
        if (modificationType==JndiModificationType.DELETE_ENTRY)
        {
        	jm = new JndiModifications(modificationType, syncOptions.getTaskName());
        	jm.setDistinguishName(destBean.getDistinguishName());
        	LOGGER.debug("Deleting entry : \"" + destBean.getDistinguishName() + "\"");
        }
        else if (modificationType==JndiModificationType.ADD_ENTRY) 
        {
        	jm = getAddEntry(syncOptions, srcBean, itmBean, customLibrary, condition);
        }
        else if (modificationType==JndiModificationType.MODIFY_ENTRY)
        {
        	jm = getModifyEntry(syncOptions, srcBean, itmBean, destBean, customLibrary);
        }
        else if (modificationType==JndiModificationType.MODRDN_ENTRY)
        {
        	//WARNING: updating the RDN of the entry will cancel other modifications! Relaunch synchronization to complete update
        	jm = new JndiModifications(JndiModificationType.MODRDN_ENTRY, syncOptions.getTaskName());
        	jm.setDistinguishName(destBean.getDistinguishName());
        	jm.setNewDistinguishName(itmBean.getDistinguishName());
        }

        if (jm.getOperation() == JndiModificationType.MODRDN_ENTRY
                || (jm.getModificationItems() != null && jm.getModificationItems().size() != 0)) {            
            return jm;
        } else {
            return null;
        }
    }

    private static JndiModifications getModifyEntry(ISyncOptions syncOptions, IBean srcBean, IBean itmBean, IBean destBean, 
            Object customLibrary) throws NamingException, CloneNotSupportedException {

        JndiModifications jm = new JndiModifications(JndiModificationType.MODIFY_ENTRY, syncOptions.getTaskName());
        jm.setDistinguishName(destBean.getDistinguishName());

        /*
         * If attributes are in the request, we put them in the directory. But if they are not, we forget
         */
        Map<String, Object> table = new HashMap<String, Object>();
        table.put("srcBean", srcBean);
        table.put("dstBean", destBean);
        if(customLibrary != null) {
            table.put("custom", customLibrary);
        }

        // Force attribute values for forced attributes in syncoptions
        Set<String> forceAttrsNameSet = syncOptions.getForceValuedAttributeNames();
        List<String> writeAttributes = syncOptions.getWriteAttributes();
        if (forceAttrsNameSet != null) {

            Iterator<String> forceAttrsNameIt = forceAttrsNameSet.iterator();
            while (forceAttrsNameIt.hasNext()) {
                String attrName = forceAttrsNameIt.next();

                /* We do something only if we have to write */
                if(writeAttributes == null || writeAttributes.contains(attrName)) {
                    List<String> forceValues = syncOptions.getForceValues(itmBean.getDistinguishName(), attrName);
                    if ( forceValues != null ) {
                        Attribute forceAttribute = new BasicAttribute(attrName);
                        Iterator<String> forceValuesIt = forceValues.iterator();
                        while (forceValuesIt.hasNext()) {
                            String forceValue = forceValuesIt.next();
                            List<String> values = JScriptEvaluator.evalToStringList(forceValue, table);
                            Iterator<String> valuesIt = values.iterator();
                            while (valuesIt.hasNext()) {
                                forceAttribute.add(valuesIt.next());
                            }
                        }
                        itmBean.setAttribute(forceAttribute);
                    }
                }
            }
        }

        // Use default attributes values specified by syncOptions but not present in srcJdbcBean
        Set<String> defaultAttrsNameSet = syncOptions.getDefaultValuedAttributeNames();
        if (defaultAttrsNameSet != null) {

            Iterator<String> defaultAttrsNameIt = defaultAttrsNameSet.iterator();
            while (defaultAttrsNameIt.hasNext()) {
                String attrName = defaultAttrsNameIt.next();

                /* We do something only if we have to write */
                if(writeAttributes == null || writeAttributes.contains(attrName)) {
                    List<String> defaultValues = syncOptions.getDefaultValues(itmBean.getDistinguishName(), attrName);
                    if ( defaultValues != null && itmBean.getAttributeById(attrName) == null ) {
                        Attribute defaultAttribute = new BasicAttribute(attrName);
                        List<String> defaultValuesModified = new ArrayList<String>();
                        Iterator<String> defaultValuesIt = defaultValues.iterator();
                        while(defaultValuesIt.hasNext()) {
                            String defaultValue = defaultValuesIt.next();
                            defaultValuesModified.addAll(JScriptEvaluator.evalToStringList(defaultValue, table));
                        }
                        Iterator<String> defaultValuesModifiedIter = defaultValuesModified.iterator();
                        while(defaultValuesModifiedIter.hasNext()) {
                            defaultAttribute.add(defaultValuesModifiedIter.next());
                        }

                        itmBean.setAttribute(defaultAttribute);
                    }
                }
            }
        }

        Iterator<String> srcBeanAttrsNameIter = itmBean.getAttributesNames().iterator();
        List<ModificationItem> modificationItems = new ArrayList<ModificationItem>();
        while (srcBeanAttrsNameIter.hasNext()) {
            String srcAttrName = srcBeanAttrsNameIter.next();

            /* We do something only if we have to write */
            if(writeAttributes == null || writeAttributes.contains(srcAttrName)) {

                ModificationItem mi = null;
                Attribute srcAttr = itmBean.getAttributeById(srcAttrName);
                Attribute destAttr = destBean.getAttributeById(srcAttrName);
                table.put("srcAttr", srcAttr);
                table.put("dstAttr", destAttr);

                // Clean up srcAttr
                if (srcAttr == null) {
                    srcAttr = new BasicAttribute(srcAttrName);
                }
                while (srcAttr.size() >= 1 && ((String)srcAttr.get(0)).length() == 0) {
                    srcAttr.remove(0);
                }

                // Manage default values
                List<String> defaultValues = syncOptions.getDefaultValues(itmBean.getDistinguishName(),	srcAttrName);
                List<String> defaultValuesModified = new ArrayList<String>();
                if (defaultValues != null) {
                    Iterator<String> defaultValuesIt = defaultValues.iterator();
                    while(defaultValuesIt.hasNext()) {
                        String defaultValue = defaultValuesIt.next();
                        defaultValuesModified.addAll(JScriptEvaluator.evalToStringList(defaultValue, table));
                    }
                }

                if (defaultValuesModified.size() > 0 &&
                        (syncOptions.getStatus(itmBean.getDistinguishName(), srcAttrName) == STATUS_TYPE.MERGE ||
                                (srcAttr == null || srcAttr.size() == 0) )) {
                    Iterator<String> defaultValuesIter = defaultValuesModified.iterator();
                    while(defaultValuesIter.hasNext()) {
                        String value = defaultValuesIter.next();
                        if (value != null & value.length() > 0) {
                            srcAttr.add(value);
                        }
                    }
                }

                if (syncOptions.getStatus(itmBean.getDistinguishName(), srcAttrName) == STATUS_TYPE.FORCE) {
                    if ( ( srcAttr == null || srcAttr.size() == 0) && (destAttr != null && destAttr.size() > 0 ) ) {
                        LOGGER.debug("Deleting attribute  \"" + srcAttrName + "\" in entry \""
                                + destBean.getDistinguishName() + "\"");
                        // delete all values of the attribute - to do this we must create an empty Attribute
                        mi = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(srcAttrName));
                    } else if ( ( srcAttr != null && srcAttr.size() > 0) && (destAttr == null || destAttr.size() == 0 )) {
                        LOGGER.debug("Adding attribute \"" + srcAttrName + "\" in entry \""
                                + destBean.getDistinguishName() + "\"");
                        // By default, if we try to modify an attribute in
                        // the destination entry, we have to care to replace all
                        // values in the following conditions:
                        // - FORCE action is used;
                        // - A value is specified by the create_value parameter.
                        // So, instead of add the attribute, we replace it.
                        mi = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, itmBean.getAttributeById(srcAttrName));
                    } else if ( ( srcAttr == null || srcAttr.size() == 0) && (destAttr == null || destAttr.size() == 0 ) ) {
                        // Do nothing
                        LOGGER.debug("Do nothing (" + srcAttrName + ")");
                    } else {
                        mi = compareAttribute(srcAttr, destAttr);
                    }

                    if (mi != null) {
                        modificationItems.add(mi);
                    }
                } else if (syncOptions.getStatus(itmBean.getDistinguishName(), srcAttrName) == STATUS_TYPE.MERGE) {
                    if ( ( srcAttr == null || srcAttr.size() == 0) && (destAttr != null && destAttr.size() > 0 ) ) {
                        // Do nothing
                        LOGGER.debug("Do nothing (" + srcAttrName + ")");
                    } else if ( ( srcAttr != null && srcAttr.size() > 0) && (destAttr == null || destAttr.size() == 0 ) ) {
                        LOGGER.debug("Adding attribute \"" + srcAttrName + "\" in entry \""
                                + destBean.getDistinguishName() + "\"");
                        mi = new ModificationItem(DirContext.ADD_ATTRIBUTE, itmBean.getAttributeById(srcAttrName));
                    } else if ( ( srcAttr == null || srcAttr.size() == 0) && (destAttr == null || destAttr.size() == 0 ) ) {
                        // Do nothing
                        LOGGER.debug("Do nothing (" + srcAttrName + ")");
                    } else {
                        mi = mergeAttributes(srcAttr, destAttr);
                    }

                    if (mi != null) {
                        modificationItems.add(mi);
                    }
                } else {
                    LOGGER.debug("Forget any modifications because of the 'Keep' status (" + srcAttrName + ")");
                }
            }
        }

        if (modificationItems.size() != 0) {
            jm.setModificationItems(modificationItems);
            LOGGER.debug("Modifying entry \"" + destBean.getDistinguishName() + "\"");
        } else {
            LOGGER.debug("Entry \"" + destBean.getDistinguishName()
                    + "\" is the same in the source and in the destination");
        }
        return jm;
    }

    /**
     * 
     * @param syncOptions
     * @param srcJdbcBean
     * @param itmBean
     * @param customLibrary
     * @param condition The create condition to avoid recalculating it in the method.
     * @return
     * @throws NamingException
     * @throws CloneNotSupportedException
     */
    private static JndiModifications getAddEntry(ISyncOptions syncOptions, IBean srcJdbcBean, IBean itmBean, Object customLibrary, boolean condition) throws NamingException, CloneNotSupportedException {
        /* table used for JScript interpretation of creation values */
        Map<String, Object> table = new HashMap<String, Object>();
        table.put("srcBean", srcJdbcBean);
        if(customLibrary != null) {
            table.put("custom", customLibrary);
        }

        JndiModifications jm = new JndiModifications(JndiModificationType.ADD_ENTRY, syncOptions.getTaskName());
        
        if (itmBean.getDistinguishName() != null) {
            jm.setDistinguishName(itmBean.getDistinguishName());
        } else {            
            // only complain about a missing DN if we're really going to create the entry
            if (condition) {
            	LOGGER.warn("No DN set! Trying to generate an DN based on the uid attribute!");

            	if (itmBean.getAttributeById("uid") == null || itmBean.getAttributeById("uid").size() == 0) {
                    throw new RuntimeException("-- Development error: No RDN found (uid by default)!");
            	}
            	jm.setDistinguishName("uid=" + itmBean.getAttributeById("uid").get() + "," + Configuration.DN_PEOPLE);
            }
            else
            {
            	// condition is false, we're not really going to create the entry
            	// set a pseudo DN to use for display purposes
            	jm.setDistinguishName("No DN set! Read it from the source or set lsc.tasks.NAME.dn");
            }
            
        }

        // Force attribute values for forced attributes in syncoptions
        Set<String> forceAttrsNameSet = syncOptions.getForceValuedAttributeNames();
        List<String> writeAttributes = syncOptions.getWriteAttributes();
        if (forceAttrsNameSet != null) {

            Iterator<String> forceAttrsNameIt = forceAttrsNameSet.iterator();
            while (forceAttrsNameIt.hasNext()) {
                String attrName = forceAttrsNameIt.next();

                /* We do something only if we have to write */
                if(writeAttributes == null || writeAttributes.contains(attrName)) {
                    List<String>forceValues = syncOptions.getForceValues(itmBean.getDistinguishName(), attrName);
                    if ( forceValues != null ) {
                        Attribute forceAttribute = new BasicAttribute(attrName);
                        Iterator<String> forceValuesIt = forceValues.iterator();
                        while (forceValuesIt.hasNext()) {
                            String forceValue = forceValuesIt.next();
                            List<String> values = JScriptEvaluator.evalToStringList(forceValue, table);
                            Iterator<String> valuesIt = values.iterator();
                            while (valuesIt.hasNext()) {
                            	String value = valuesIt.next();
                            	if (value != null && value.length() > 0) {                                
                            		forceAttribute.add(value);
                            	}
                            }
                        }

                        itmBean.setAttribute(forceAttribute);
                    }
                }
            }
        }

        Iterator<String> jdbcAttrsName = itmBean.getAttributesNames().iterator();
        List<ModificationItem> modificationItems = new ArrayList<ModificationItem>();
        while (jdbcAttrsName.hasNext()) {
            String jdbcAttrName = jdbcAttrsName.next();

            /* We do something only if we have to write */
            if(writeAttributes == null || writeAttributes.contains(jdbcAttrName)) {
                Attribute srcJdbcAttribute = itmBean.getAttributeById(jdbcAttrName);
                List<String> createValues = syncOptions.getCreateValues(itmBean.getDistinguishName(), srcJdbcAttribute.getID());

                if ( ( createValues != null ) 
                        && (srcJdbcAttribute.getAll() == null || !srcJdbcAttribute.getAll().hasMore() ||
                                syncOptions.getStatus(itmBean.getDistinguishName(), jdbcAttrName)==STATUS_TYPE.MERGE)) {
                    // interpret JScript in createValue
                    table.put("srcAttr", srcJdbcAttribute);
                    List<String> createValuesModified = new ArrayList<String>();
                    Iterator<String> createValuesIt = createValues.iterator();
                    while (createValuesIt.hasNext()) {
                        String createValue = (String) createValuesIt.next();
                        createValuesModified.addAll(JScriptEvaluator.evalToStringList(createValue, table));
                    }

                    Iterator<String> createValuesModifiedIter = createValuesModified.iterator();
                    while(createValuesModifiedIter.hasNext()) {
                    	String value = createValuesModifiedIter.next();
                    	if (value != null && value.length() > 0) {
                    		srcJdbcAttribute.add(value);
                    	}
                    }
                }
                modificationItems.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, srcJdbcAttribute));
            }
        }

        // create extra attributes specified by syncOptions but not present in srcJdbcBean
        Set<String> createAttrsNameSet = syncOptions.getCreateAttributeNames();
        if (createAttrsNameSet != null) {
            Iterator<String> createAttrsNameIt = createAttrsNameSet.iterator();
            while (createAttrsNameIt.hasNext()) {
                String attrName = (String) createAttrsNameIt.next();
                List<String> createValues = syncOptions.getCreateValues(itmBean.getDistinguishName(), attrName);
                if ( createValues != null && itmBean.getAttributeById(attrName) == null ) {
                    Attribute createdAttribute = new BasicAttribute(attrName);
                    List<String> createValuesModified = new ArrayList<String>();
                    Iterator<String> createValuesIt = createValues.iterator();
                    while (createValuesIt.hasNext()) {
                        String createValue = (String) createValuesIt.next();
                        createValuesModified.addAll(JScriptEvaluator.evalToStringList(createValue, table));
                    }

                    Iterator<String> createValuesModifiedIter = createValuesModified.iterator();
                    while(createValuesModifiedIter.hasNext()) {
                        createdAttribute.add(createValuesModifiedIter.next());
                    }
                    modificationItems.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, createdAttribute));
                }
            }
        }

        jm.setModificationItems(modificationItems);
        LOGGER.debug("Adding new entry \"" + jm.getDistinguishName() + "\"");
        return jm;
    }

    /**
     * Compare two attributes and compute a modification item
     * 
     * @param srcAttr
     *            the source attribute
     * @param dstAttr
     *            the destination attribute
     * @return the modification item or null if not needed
     * @throws NamingException
     */
    private static ModificationItem compareAttribute(Attribute srcAttr, Attribute dstAttr) throws NamingException {

        boolean differs = false;

        // read in all values from dstAttr
        List<String> dstAttrValues = new ArrayList<String>(dstAttr.size());
        NamingEnumeration<?> dstNe = dstAttr.getAll();			
        while (dstNe.hasMore()) {
            Object o = dstNe.next();
            if (o.getClass() == String.class) {
                dstAttrValues.add((String) o);
            } else if (o.getClass().isAssignableFrom(byte[].class)) {
                dstAttrValues.add(new String((byte[]) o));
            } else if (o.getClass().isAssignableFrom(List.class)) {
                /* FIXME: can this really happen? */
                List<?> values = ((List<?>) o);
                if (compare(values, srcAttr) != 0) {
                    differs = true;
                }
            } 
        }

        // check if there are any values in srcAttr not in dstAttr
        // and build up replacement attribute, in case
        Attribute toReplaceAttr = new BasicAttribute(srcAttr.getID());
        NamingEnumeration<?> srcNe = srcAttr.getAll();
        String value = null;
        while (srcNe.hasMore()) {
            Object o = srcNe.next();
            if (o.getClass() == String.class) {
                value = (String) o;
            } else if (o.getClass().isAssignableFrom(byte[].class)) {
                value = new String((byte[]) o);
            } else if (o.getClass().isAssignableFrom(List.class)) {
                /* FIXME: can this really happen? */
                List<?> values = ((List<?>) o);
                if (compare(values, srcAttr) != 0) {
                    differs = true;
                }
            }

            if (value != null) toReplaceAttr.add(value);

            if (!dstAttrValues.contains(value)) {
                differs = true;
            }
        }

        // check if there are any values in dstAttr not in srcAttr
        Iterator<String> dstIt = dstAttrValues.iterator(); 
        while (dstIt.hasNext()) {
            value = dstIt.next();
            if (!toReplaceAttr.contains(value)) {
                differs = true;
                break;
            }
        }

        if (differs) {
            return new ModificationItem(DirContext.REPLACE_ATTRIBUTE, toReplaceAttr);
        } else {
            return null;
        }
    }

    /**
     * Merge two attributes and compute a modification item
     * 
     * @param srcAttr
     *            the source attribute
     * @param dstAttr
     *            the destination attribute
     * @return the modification item or null if not needed
     * @throws NamingException
     */
    private static ModificationItem mergeAttributes(Attribute srcAttr, Attribute dstAttr) throws NamingException {

        // read in all values from dstAttr
        List<String> dstAttrValues = new ArrayList<String>(dstAttr.size());
        NamingEnumeration<?> dstNe = dstAttr.getAll();			
        while (dstNe.hasMore()) {
            Object o = dstNe.next();
            if (o.getClass() == String.class) {
                dstAttrValues.add((String) o);
            } else if (o.getClass().isAssignableFrom(byte[].class)) {
                dstAttrValues.add(new String((byte[]) o));
            }
        }

        // check if there are any extra values to be added from the source attribute
        Attribute addValuesAttr = new BasicAttribute(srcAttr.getID());
        NamingEnumeration<?> srcNe = srcAttr.getAll();
        String value;
        while (srcNe.hasMore()) {
            value = null;
            Object o = srcNe.next();
            if (o.getClass() == String.class) {
                value = (String) o;
            } else if (o.getClass().isAssignableFrom(byte[].class)) {
                value = new String((byte[]) o);
            }

            if (value != null) {
                if (!dstAttrValues.contains(value)) {
                    addValuesAttr.add(value);
                }
            }
        }

        if (addValuesAttr.size() > 0) {
            return new ModificationItem(DirContext.ADD_ATTRIBUTE, addValuesAttr);
        } else {
            return null;
        }
    }

    /**
     * Compare the bean as requested by the comparator interface.
     * @param l the values
     * @param attr
     * @return
     */
    private static int compare(final List<?> l, final Attribute attr) {
        if (l.size() != attr.size()) {
            return l.size() - attr.size();
        }
        Iterator<?> lIter = l.iterator();
        while (lIter.hasNext()) {
            if (!attr.contains(lIter.next())) {
                return -1;
            }
        }
        return 0;
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
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public static JndiModifications[] checkOtherModifications(IBean srcBean, IBean destBean, JndiModifications jm)
    throws IllegalAccessException, InvocationTargetException {
        String methodName = "checkDependencies";
        Class<?>[] params = new Class[] { JndiModifications.class };
        try {
            Method checkDependencies = destBean.getClass().getMethod(methodName, params);
            if (checkDependencies != null) {
                return (JndiModifications[]) checkDependencies.invoke(destBean, new Object[] { jm });
            }
        } catch (SecurityException e) {
            LOGGER.warn("Unattended exception has been raised : " + e, e);
        } catch (NoSuchMethodException e) {
            LOGGER.debug("No method \"" + methodName + "\" to manage modification dependencies"
                    + destBean.getClass().getName() + " (" + e + ") on ", e);
        }
        return new JndiModifications[] {};
    }
}
