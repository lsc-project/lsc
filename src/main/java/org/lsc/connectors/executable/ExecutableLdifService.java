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
package org.lsc.connectors.executable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.naming.CommunicationException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;

import org.apache.commons.codec.binary.Base64;
import org.lsc.LscAttributes;
import org.lsc.beans.IBean;
import org.lsc.jndi.IJndiWritableService;
import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;
import org.lsc.utils.output.LdifLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a generic but configurable implementation to provision data to
 * any referential which can be scripted
 * 
 * It just requires 6 scripts to :
 * <ul>
 * <li>list data</li>
 * <li>get a piece of data</li>
 * <li>add a new</li>  
 * <li>update a existing data</li>  
 * <li>rename - or change the identifier</li>  
 * <li>delete or archive an unused data</li>  
 * </ul>
 * 
 * The 4 scripts which change data are responsible for consistency. No explicit 
 * check neither rollback is achived by the LSC engine, so a successful result 
 * for any of these 4 operations must be fully checked.
 * 
 * At this time, no time out is managed. So please consider handling provisioned
 * referential availibility and/or time limit handling directly in the executable.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class ExecutableLdifService implements IJndiWritableService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutableLdifService.class);

	private static final String DEBUG_PREFIX = "DEBUG: ";
	private static final String INFO_PREFIX = "INFO: ";
	private static final String WARN_PREFIX = "WARN: ";
	private static final String ERROR_PREFIX = "ERROR: ";
	private static final String VARS_PREFIX = "vars";

	private String listScript;
	private String getScript;
	private Class<IBean> beanClass;
	private Runtime rt;
	private Properties globalEnvironmentVariables;

	/** Map a JndiModificationType to the associated Script **/
	private Map<JndiModificationType, String> modificationToScript = new HashMap<JndiModificationType, String>();

	@SuppressWarnings("unchecked")
	public ExecutableLdifService(Properties props, String beanClassName) {
		rt = Runtime.getRuntime();
		try {
			globalEnvironmentVariables = org.lsc.Configuration.getPropertiesSubset(props, VARS_PREFIX);
			listScript = (String) props.get("listScript");
			getScript = (String) props.get("getScript");

			beanClass = (Class<IBean>) Class.forName(beanClassName);

			modificationToScript.put(JndiModificationType.ADD_ENTRY, (String) props.get("addScript"));
			modificationToScript.put(JndiModificationType.DELETE_ENTRY, (String) props.get("deleteScript"));
			modificationToScript.put(JndiModificationType.MODIFY_ENTRY, (String) props.get("updateScript"));
			modificationToScript.put(JndiModificationType.MODRDN_ENTRY, (String) props.get("renameScript"));
		} catch (ClassNotFoundException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	
	/**
	 * Apply directory modifications.
	 *
	 * @param jm Modifications to apply in a {@link JndiModifications} object.
	 * @return Operation status
	 * @throws CommunicationException If the connection to the service is lost,
	 * and all other attempts to use this service should fail.
	 */
	public boolean apply(final JndiModifications jm) throws CommunicationException {
		int exitCode = 0;
		String ldif = LdifLayout.format(jm);
		exitCode = execute(getParameters(modificationToScript.get(jm.getOperation()), 
						jm.getDistinguishName()), getEnv(), ldif);
		if (exitCode != 0) {
			LOGGER.error("Exit code != 0: {}", exitCode);
		}
		return exitCode == 0;
	}

	/**
	 * The simple object getter according to its identifier.
	 * 
	 * @param pivotName Name of the entry to be returned, which is the name returned by {@link #getListPivots()}
	 *            (used for display only)
	 * @param pivotAttributes Map of attribute names and values, which is the data identifier in the
	 *            source such as returned by {@link #getListPivots()}. It must identify a unique entry in the
	 *            source.
	 * @return The bean, or null if not found
	 * @throws NamingException May throw a {@link NamingException} if the object is not found in the
	 *             directory, or if more than one object would be returned.
	 */
	public IBean getBean(String pivotName, LscAttributes pivotAttributes)
					throws NamingException {
		String output = executeWithReturn(getParameters(getScript, pivotName), getEnv(), toLdif(pivotAttributes));
		Collection<IBean> entries = fromLdif(output);
		if (entries.size() != 1) {
			LOGGER.error("Entries count: {}", entries.size());
			return null;
		}
		return entries.iterator().next();
	}

	/**
	 * Returns a list of all the objects' identifiers.
	 *
	 * @return Map of all entries names that are returned by the directory with an associated map of
	 *         attribute names and values (never null)
	 * @throws NamingException
	 */
	public Map<String, LscAttributes> getListPivots() throws NamingException {
		Map<String, LscAttributes> map = null;
		String output = executeWithReturn(getParameters(listScript), getEnv(), "");
		Collection<IBean> beans = fromLdif(output);
		if (beans != null) {
			map = new HashMap<String, LscAttributes>();
			for (IBean bean : beans) {
				LscAttributes attributes = new LscAttributes();
				for (String id : bean.getAttributesNames()) {
					Attribute attribute = bean.getAttributeById(id);
					//TODO: handle multi value attributes pivot
					attributes.getAttributes().put(id, attribute.get());
				}
				map.put(bean.getDistinguishedName(), attributes);
			}
		}
		return map;
	}

	public int execute(String[] runtime, String[] env, String input) {
		StringBuffer datas = new StringBuffer();
		return execute(runtime, env, input, datas);
	}

	public String executeWithReturn(String[] runtime, String[] env, String input) {
		StringBuffer datas = new StringBuffer();
		execute(runtime, env, input, datas);
		return datas.toString();
	}

	private int execute(String[] runtime, String[] env, String input, StringBuffer datas) {
		StringBuffer messages = new StringBuffer();
		Process p = null;
		try {
			if (LOGGER.isDebugEnabled()) {
				StringBuilder parametersStr = new StringBuilder();
				for (String parameter : runtime) {
					parametersStr.append(parameter).append(" ");
				}
				LOGGER.debug("Lauching '{}'", parametersStr.toString());
			}
			p = rt.exec(runtime, env);

			LOGGER.debug("Writing to STDIN {}", input);

			OutputStream outputStream = p.getOutputStream();
			outputStream.write(input.getBytes());
			outputStream.flush();
			outputStream.close();

			//TODO: need to check for max time
			LOGGER.debug("Waiting for command to stop ... ");

			p.waitFor();
		} catch (IOException e) {
			// Encountered an error while reading data from output
			LOGGER.error("Encountered an I/O exception while writing data to script {}", runtime);
			LOGGER.debug(e.toString(), e);
		} catch (InterruptedException e) {
			// Encountered an interruption
			LOGGER.error("Script {} interrupted", runtime);
			LOGGER.debug(e.toString(), e);
		}

		byte[] data = new byte[65535];
		try {
			while (p.getInputStream() != null && p.getInputStream().read(data) > 0) {
				datas.append(new String(data));
			}
		} catch (IOException e) {
			// Failing to read the complete string causes null return
			LOGGER.error("Fail to read complete data from script output stream: {}", runtime);
			LOGGER.debug(e.toString(), e);
		}

		byte[] message = new byte[65535];
		try {
			while (p.getErrorStream().read(message) > 0) {
				messages.append(new String(message));
			}
		} catch (IOException e) {
			// Failing to read the complete string causes null return
			LOGGER.error("Fail to read complete messages from script stderr stream: {}", runtime);
			LOGGER.debug(e.toString(), e);
		}
		
		if (p.exitValue() != 0) {
			// A non zero value causes null return
			LOGGER.error("Non zero exit code for runtime: {}, exit code={}", runtime[0], p.exitValue());
			displayByLevel(messages.toString());
		} else {
			LOGGER.debug("Messages dump on stderr by script: ");
			displayByLevel(messages.toString());
		}
		return p.exitValue();
	}

	/**
	 * Parse returned messages to send them to the correct log level
	 * Messages must be prefixed by "DEBUG: ", "INFO: ", "WARN: ", "ERROR: "
	 * Default level is WARN. 
	 * @param messages the returned messages
	 */
	private void displayByLevel(String messages) {
		StringTokenizer lines = new StringTokenizer(messages, "\n");
		while (lines.hasMoreTokens()) {
			String line = lines.nextToken();
			String message = (line.contains(": ") ? line.substring(line.indexOf(": ") + 2) : line);
			if (line.startsWith(DEBUG_PREFIX)) {
				LOGGER.debug(message);
			} else if (line.startsWith(INFO_PREFIX)) {
				LOGGER.info(message);
			} else if (line.startsWith(WARN_PREFIX)) {
				LOGGER.warn(message);
			} else if (line.startsWith(ERROR_PREFIX)) {
				LOGGER.error(message);
			} else {
				// Default to WARN level
				LOGGER.warn(line);
			}
		}
	}

	private String[] getEnv(String... args) {
		String[] envVars = new String[args.length + globalEnvironmentVariables.size()];
		int i = 0;
		for (String parameter : args) {
			envVars[i++] = parameter;
		}
		for (Object parameterName : globalEnvironmentVariables.keySet()) {
			envVars[i++] = (String) parameterName + "=" + (String) globalEnvironmentVariables.get(parameterName);
		}
		return envVars;
	}

	private String[] getParameters(String... args) {
		String[] parameters = new String[args.length];
		int i = 0;
		for (String parameter : args) {
			parameters[i++] = parameter;
		}
		return parameters;
	}

	private Collection<IBean> fromLdif(String output) {
		ArrayList<IBean> beans = new ArrayList<IBean>();
		try {
			IBean bean = null;
			StringTokenizer sTok = new StringTokenizer(output, "\n", true);
			while (sTok.hasMoreTokens()) {
				bean = beanClass.newInstance();
				String entryStr = "";
				while (sTok.hasMoreTokens()) {
					String line = sTok.nextToken();
					entryStr += line;
					if (entryStr.endsWith("\n\n")) {
						break;
					}
				}
				if (entryStr.trim().length() > 0) {
					updateBean(bean, entryStr);
					beans.add(bean);
				}
			}
		} catch (InstantiationException e) {
			LOGGER.error("Bean class name: {}", beanClass.getName());
			LOGGER.debug(e.toString(), e);
		} catch (IllegalAccessException e) {
			LOGGER.error("Bean class name: {}", beanClass.getName());
			LOGGER.debug(e.toString(), e);
		}

		return beans;
	}

	private void updateBean(IBean bean, String entryStr) {
		StringTokenizer sTok = new StringTokenizer(entryStr, "\n");
		String multiLineValue = null;
		String multiLineAttribute = null;
		boolean base64 = false;
		while (sTok.hasMoreTokens()) {
			String line = sTok.nextToken();
			if (multiLineValue != null && !line.startsWith(" ")) {
				// End of multi line value
				if (base64) {
					String attributeValue = new String(Base64.decodeBase64(multiLineValue.getBytes()));
					updateBeanAttributeValue(bean, multiLineAttribute, attributeValue);
				} else {
					updateBeanAttributeValue(bean, multiLineAttribute, multiLineValue);
				}
				multiLineValue = null;
			}
			if (multiLineValue != null && line.startsWith(" ")) {
				multiLineValue += line.substring(1);
			} else if (line.contains(":: ")) {
				multiLineAttribute = line.substring(0, line.indexOf(":"));
				multiLineValue = line.substring(line.indexOf(":: ") + 3);
				base64 = true;
			} else if (line.contains(": ")) {
				multiLineAttribute = line.substring(0, line.indexOf(":"));
				multiLineValue = line.substring(line.indexOf(": ") + 2);
				base64 = false;
			} else if (line.trim().length() == 0) {
				break;
			} else {
				// TODO
				LOGGER.error("Got something strange : '{}'. Please consider checking as this data may be either an incorrect format or an error !", line);
			}
		}
		if (multiLineValue != null) {
			if (base64) {
				String attributeValue = new String(Base64.decodeBase64(multiLineValue.getBytes()));
				updateBeanAttributeValue(bean, multiLineAttribute, attributeValue);
			} else {
				updateBeanAttributeValue(bean, multiLineAttribute, multiLineValue);
			}
		}
	}

	private void updateBeanAttributeValue(IBean bean,
					String attributeName, String attributeValue) {
		if (attributeName.equals("dn")) {
			bean.setDistinguishedName(attributeValue);
		} else {
			if (bean.getAttributeById(attributeName) != null) {
				Attribute attr = bean.getAttributeById(attributeName);
				attr.add(attributeValue);
				bean.setAttribute(attr);
			} else {
				bean.setAttribute(new BasicAttribute(attributeName, attributeValue));
			}
			if (bean.getDistinguishedName() == null) {
				bean.setDistinguishedName(attributeValue);
			}
		}
	}

	private String toLdif(LscAttributes attributes) throws NamingException {
		StringBuilder sb = new StringBuilder();
		for (String attributeName : attributes.getAttributes().keySet()) {
			LdifLayout.printAttributeToStringBuffer(sb, new BasicAttribute(attributeName, attributes.getAttributes().get(attributeName)));
		}
		return sb.toString();
	}
}
