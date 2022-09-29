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
package org.lsc.utils;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptEngine;

import org.apache.commons.io.FilenameUtils;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.lsc.Task;
import org.lsc.jndi.AbstractSimpleJndiService;
import org.lsc.jndi.ScriptableJndiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the Groovy Script evaluation context.
 * TODO: Reintroduce cache and imports
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public final class GroovyEvaluator implements ScriptableEvaluator {

	// Logger
	private static final Logger LOGGER = LoggerFactory.getLogger(GroovyEvaluator.class);

	private GroovyScriptEngineImpl engine;
	
	/**
	 * Default public constructor.
	 */
	public GroovyEvaluator(ScriptEngine se) {
		this.engine = (GroovyScriptEngineImpl) se;
	}

	/**
	 * Evaluate your groovy script expression (manage pre-compiled expressions
	 * cache).
	 *
	 * @param expression
	 *                the expression to eval
	 * @param params
	 *                the keys are the name used in the
	 * @return the evaluation result
	 */
	@Override
	public String evalToString(final Task task, final String expression,
					final Map<String, Object> params) {
		Object result = instanceEval(task, expression, params);

		if (result == null) {
			return null;
		}
		return (String) result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object> evalToObjectList(final Task task, final String expression,
					final Map<String, Object> params) {
		Object result = instanceEval(task, expression, params);
		if (result == null) {
			return null;
		}
		if(result instanceof Object[]) {
			return Arrays.asList(result);
		} else if(result instanceof List) {
			return (List<Object>) result;
		} else if (result.getClass().isArray() && result.getClass().getComponentType().equals(byte.class)) {
			return Arrays.asList(result);
		} else {
			List<Object> resultsArray = new ArrayList<Object>();
			String resultAsString = result.toString();
			if (resultAsString != null && resultAsString.length() > 0) {
				resultsArray.add(resultAsString);
			}
			return resultsArray;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<byte[]> evalToByteArrayList(final Task task, final String expression,
					final Map<String, Object> params) {
		Object result = instanceEval(task, expression, params);

		if(result instanceof byte[][]) {
			return Arrays.asList((byte[][])result);
		} else if (result instanceof byte[]) {
			return Collections.singletonList((byte[])result);
		} else if(result instanceof List) {
			return (List<byte[]>) result;
		} else if(result instanceof Set) {
			return new ArrayList<byte[]>((Set<byte[]>)result);
		} else {
			List<byte[]> resultsArray = new ArrayList<byte[]>();
			String resultAsString = result.toString();
			if (resultAsString != null && resultAsString.length() > 0) {
				resultsArray.add(resultAsString.getBytes());
			}
			return resultsArray;
		}
	}

	@Override
	public byte[] evalToByteArray(final Task task, final String expression,
					final Map<String, Object> params) {
		Object result = instanceEval(task, expression, params);

		if(result instanceof byte[]) {
			return (byte[])result;
		} else if (result instanceof String) {
			return ((String)result).getBytes();
		} else {
			return result.toString().getBytes();
		}
	}

	@Override
	public Boolean evalToBoolean(final Task task, final String expression, final Map<String, Object> params) {
		return (Boolean) instanceEval(task, expression, params);
	}

	/**
	 * Local instance evaluation.
	 *
	 * @param expression
	 *                the expression to eval
	 * @param params
	 *                the keys are the name used in the
	 * @return the evaluation result
	 */
	private Object instanceEval(final Task task, final String expression,
					final Map<String, Object> params) {
		Bindings bindings = engine.createBindings();


		/* Allow to have shorter names for function in the package org.lsc.utils.directory */
		String expressionImport =
//						"import static org.lsc.utils.directory.*\n" +
//						"import static org.lsc.utils.*\n" + 
						expression;

		// add LDAP interface for destination
		if (!bindings.containsKey("ldap") && task.getDestinationService() instanceof AbstractSimpleJndiService) {
			ScriptableJndiServices dstSjs = new ScriptableJndiServices();
			dstSjs.setJndiServices(((AbstractSimpleJndiService)task.getDestinationService()).getJndiServices());
			bindings.put("ldap", dstSjs);
		}

		// add LDAP interface for source
		if (!bindings.containsKey("srcLdap") && task.getSourceService() instanceof AbstractSimpleJndiService) {
			ScriptableJndiServices srcSjs = new ScriptableJndiServices();
			srcSjs.setJndiServices(((AbstractSimpleJndiService)task.getSourceService()).getJndiServices());
			bindings.put("srcLdap", srcSjs);
		}

		if(params != null) {
			for(String paramName: params.keySet()) {
				bindings.put(paramName, params.get(paramName));
			}
		}
		
		Object ret = null;
		try {
			if (task.getScriptIncludes() != null) {
				for (File scriptInclude: task.getScriptIncludes()) {
					String extension = FilenameUtils.getExtension(scriptInclude.getAbsolutePath());
					if ("groovy".equals(extension) || "gvy".equals(extension) || "gy".equals(extension) || "gsh".equals(extension)) {
						FileReader reader = new FileReader(scriptInclude);
						try {
							engine.eval(reader, bindings);
						} finally {
							reader.close();
						}					
					}
				}
			}
			ret = engine.eval(expressionImport, bindings);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(e.toString());
			LOGGER.debug(e.toString(), e);
			return null;
		}

		return ret;
	}
}
