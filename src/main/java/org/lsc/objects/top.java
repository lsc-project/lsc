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
package org.lsc.objects;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.lsc.LscObject;
import org.lsc.beans.AbstractBean;
import org.lsc.objects.flat.fTop;

/**
 * Bean representing LDAP top LDAP objectclass
 */
public class top extends LscObject {

	protected static Map<String, List<Method>> localMethods;

	static {
		localMethods = new HashMap<String, List<Method>>();
	}

	public top() {
		objectClass = new ArrayList<String>();
		objectClass.add("top");
	}
	
	public static String getDn(String uidValue) {
		throw new UnsupportedOperationException();
	}

	public final void setUpFromObject(fTop fo) throws IllegalAccessException, InvocationTargetException {
		AbstractBean.loadLocalMethods(this.getClass(), localMethods, AbstractBean.SET_ACCESSOR_PREFIX);
		
		Method[] methods = fo.getClass().getMethods();
		
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().startsWith(AbstractBean.GET_ACCESSOR_PREFIX)) {
			    
			    /* Get the name of the parameter */
				String paramName = methods[i].getName().substring(AbstractBean.GET_ACCESSOR_PREFIX.length());
				paramName = paramName.substring(0, 1).toLowerCase() + paramName.substring(1);
				
				/* Get the returnType for the get method */
				Class<?> returnType = methods[i].getReturnType();
				
				/* Get matching local methods for this parameter */
				Method localMethod = null;
				List<Method> meths = localMethods.get(paramName);
				if (meths == null) localMethod = null;
				else {
					if (meths.size() == 1) {
						localMethod = meths.get(0);
					} else {
						/* Find method matching returnType */
						Iterator<Method> methsIt = meths.iterator();
						Method currentMeth = null;
						while (methsIt.hasNext()) {
							currentMeth = methsIt.next();
							if (currentMeth.getParameterTypes()[0].isAssignableFrom(returnType)) {
								localMethod = currentMeth;
							}
						}
					
						/* If no method was found, use a random one and see what we can do... */
						if (localMethod == null) {
							localMethod = meths.get(0);
						}
					}
				}
				
				if (localMethod != null && returnType != null) {
					Object returnedObject = methods[i].invoke(fo, new Object[] {});
					List<Object> paramsToUse = null;
					Class<?>[] toReturnTypes = localMethod.getParameterTypes();
					if (returnedObject == null) {
						// TODO: check the following : no need to call a method
						// on a empty value ?
						// localMethod.invoke(this, new Object[] {});
						LOGGER.debug("No need to call a method with an empty value ... ({})", paramName);
					} else {
						if (toReturnTypes != null && toReturnTypes[0] == returnType) {
							LOGGER.debug("Method invocation: {}", localMethod.getName());
							try {
								localMethod.invoke(this, new Object[] { returnedObject });
							} catch (IllegalArgumentException e) {
								LOGGER.error("Bad argument invoking {} for attribute {}", localMethod.getName(), paramName);
								LOGGER.error(e.toString());
							}
						} else if (toReturnTypes != null && toReturnTypes[0] == List.class) {
							LOGGER.debug("Method invocation: {}", localMethod.getName());
							try {
								paramsToUse = new ArrayList<Object>();
								paramsToUse.add(returnedObject);
								localMethod.invoke(this, new Object[] { paramsToUse });
							} catch (IllegalArgumentException e) {
								LOGGER.error("Bad argument invoking {} for attribute {}", localMethod.getName(), paramName);
								LOGGER.error(e.toString(), e);
							}
						} else {
							LOGGER.error("Unable to manage translation from {} to {} for {} !", new Object[] { returnType, toReturnTypes[0], paramName });
						}
					}
				} else {
					if (paramName.compareToIgnoreCase("class") != 0) {
						LOGGER.warn("No corresponding method for original {} on {} object !", methods[i].getName(), fo.getClass());
					}
				}
			}
		}
	}
}
