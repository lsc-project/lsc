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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.io.FilenameUtils;
import org.lsc.Task;
import org.lsc.beans.IBean;
import org.lsc.exception.LscServiceException;
import org.lsc.jndi.AbstractSimpleJndiService;
import org.lsc.jndi.ScriptableJndiServices;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.UniqueTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the Rhino Java Script evaluation context.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public final class JScriptEvaluator implements ScriptableEvaluator {

	// Logger
	private static final Logger LOGGER = LoggerFactory.getLogger(JScriptEvaluator.class);

//	/** The precompiled Javascript cache. */
//	private Map<String, Script> cache;

	private ScriptEngine engine;
	
	/** The local Rhino context. */
//	private ScriptContext cx;

	/**
	 * Default public constructor.
	 */
	public JScriptEvaluator(ScriptEngine se) {
//		cache = new HashMap<String, Script>();
		this.engine = se;
//		cx = se.getContext();//new ContextFactory().enterContext();
	}

    /** {@inheritDoc} */
	@Override
	public String evalToString(final Task task, final String expression,
					final Map<String, Object> params) throws LscServiceException {
		Object result = instanceEval(task, expression, params);

		if (result == null) {
			return null;
		} else if (result instanceof String) {
			return (String)result;
		} else {
			return result.toString();
		}

//		return (String) Context.jsToJava(result, String.class);
	}

    /** {@inheritDoc} */
	@Override
	public List<Object> evalToObjectList(final Task task, final String expression,
					final Map<String, Object> params) throws LscServiceException {
        Object result = null;
	    try {
	        result = convertJsToJava(instanceEval(task, expression, params));
	    } catch(EvaluatorException e) {
	        throw new LscServiceException(e);
	    }
	    if(result == null){
			return null;
	    }
	    List<Object> resultsArray = new ArrayList<Object>();
		if(result instanceof String[] || result instanceof Object[] ) {
			for (Object resultValue : (Object[])result) {
				resultsArray.add(resultValue.toString());
			}
		} else if (result instanceof String) {
			String resultAsString = (String)result;
			if (resultAsString != null && resultAsString.length() > 0) {
				resultsArray.add(resultAsString);
			}
		} else if (result instanceof List) {
			for (Object resultValue : (List<?>)result) {
				resultsArray.add(resultValue.toString());
			}
		} else if (result.getClass().isArray() && result.getClass().getComponentType().equals(byte.class)) {
			resultsArray.add(result);
		} else {
			if (result != null) {
				resultsArray.add(result.toString());
			}
		}
		return resultsArray;
	}

    /** {@inheritDoc} */
	@Override
	public List<byte[]> evalToByteArrayList(final Task task, final String expression,
					final Map<String, Object> params) throws LscServiceException {
        Object result = null;
	    try {
	        result = convertJsToJava(instanceEval(task, expression, params));
	    } catch(EvaluatorException e) {
	        throw new LscServiceException(e);
	    }
	    
		if(result instanceof byte[][]) {
			List<byte[]> resultsArray = new ArrayList<byte[]>();
			for (byte[] resultValue : (byte[][])result) {
				resultsArray.add(resultValue);
			}
			return resultsArray;
		} else if (result instanceof byte[]) {
			List<byte[]> resultsArray = new ArrayList<byte[]>();
			byte[] resultAsByteArray = (byte[])result;
			if (resultAsByteArray != null && resultAsByteArray.length > 0) {
				resultsArray.add(resultAsByteArray);
			}
			return resultsArray;
		} else if (result instanceof String) {
			List<byte[]> resultsArray = new ArrayList<byte[]>();
			String resultAsString = (String)result;
			if (resultAsString != null && resultAsString.length() > 0) {
				resultsArray.add(resultAsString.getBytes());
			}
			return resultsArray;
		} else if (result instanceof List) {
			List<byte[]> resultsArray = new ArrayList<byte[]>();
			for (Object resultValue : (List<?>)result) {
				if (resultValue instanceof byte[]) {
					resultsArray.add((byte[])resultValue);
				} else {
					resultsArray.add(resultValue.toString().getBytes());
				}
			}
			return resultsArray;
		} else if (result instanceof Set) {
			List<byte[]> resultsArray = new ArrayList<byte[]>();
			for (Object resultValue : (Set<?>)result) {
				if (resultValue instanceof byte[]) {
					resultsArray.add((byte[])resultValue);
				} else {
					resultsArray.add(resultValue.toString().getBytes());
				}
			}
			return resultsArray;
		} else if(result == null){
			return null;
		} else {
			List<byte[]> resultsArray = new ArrayList<byte[]>();
			if (result != null) {
				resultsArray.add(result.toString().getBytes());
			}
			return resultsArray;
		}
	}

    /** {@inheritDoc} */
	@Override
	public byte[] evalToByteArray(final Task task, final String expression,
					final Map<String, Object> params) throws LscServiceException {
        Object result = null;
	    try {
	        result = convertJsToJava(instanceEval(task, expression, params));
	    } catch(EvaluatorException e) {
	        throw new LscServiceException(e);
	    }
	    
		if (result instanceof byte[]) {
			return (byte[])result;
		} else if (result instanceof String) {
			return ((String)result).getBytes();
		} else {
			return result.toString().getBytes();
		}
	}

    /** {@inheritDoc} */
	@Override
	public Boolean evalToBoolean(final Task task, final String expression, final Map<String, Object> params) throws LscServiceException {
	    try {
	        return (Boolean) Context.jsToJava(instanceEval(task, expression, params), Boolean.class);
	    } catch(EvaluatorException e) {
	        throw new LscServiceException(e);
	    }
	}

	/**
	 * Local instance evaluation.
	 *
	 * @param expression
	 *                the expression to eval
	 * @param params
	 *                the keys are the name used in the
	 * @return the evaluation result
	 * @throws LscServiceException 
	 */
	private Object instanceEval(final Task task, final String expression,
					final Map<String, Object> params) throws LscServiceException {
//		Script script = null;
		Bindings bindings = engine.createBindings();


		/* Allow to have shorter names for function in the package org.lsc.utils.directory */
		String expressionImport =
                        "var version = java.lang.System.getProperty(\"java.version\");\n" +
                        "load(\"nashorn:mozilla_compat.js\");\n" +
                        "importPackage(org.lsc.utils.directory);\n" +
                        "importPackage(org.lsc.utils);\n" + expression;

//		if (cache.containsKey(expressionImport)) {
//			script = cache.get(expressionImport);
//		} else {
//			script = cx.compileString(expressionImport, "<cmd>", 1, null);
//			cache.put(expressionImport, script);
//		}

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
					if ("js".equals(FilenameUtils.getExtension(scriptInclude.getAbsolutePath()))) {
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
		} catch (ScriptException e) {
            LOGGER.error("Fail to compute expression: " + expression + " on " + 
                    (params.containsKey("srcBean") && ((IBean)params.get("srcBean")).getMainIdentifier() != null ? "id=" + ((IBean)params.get("srcBean")).getMainIdentifier() : 
                        ( params.containsKey("dstBean") && ((IBean)params.get("dstBean")).getMainIdentifier() != null ? "id=" + ((IBean)params.get("dstBean")).getMainIdentifier() :
                            "unknown id !"))
                    + "\nReason: " + e.toString());
            LOGGER.debug(e.toString(), e);
            throw new LscServiceException (e);
		} catch (RuntimeException e) {
            throw new LscServiceException (e);
		} catch (Exception e) {
			LOGGER.error(e.toString());
			LOGGER.debug(e.toString(), e);
			return null;
		}

		return ret;
	}
	
	private static Object convertJsToJava(Object src) {
		if (src == null) {
			return null;
		} else if(src.getClass().getName().equals("sun.org.mozilla.javascript.internal.NativeJavaObject")) {
			return Context.jsToJava(src, Object.class);
		} else if (src.getClass().getName().equals("sun.org.mozilla.javascript.internal.NativeArray")) {
			try {
				Method getMethod = src.getClass().getMethod("get", int.class, Class.forName("sun.org.mozilla.javascript.internal.Scriptable"));
				Object length = src.getClass().getMethod("getLength").invoke(src);
				Object[] retarr = new Object[Integer.parseInt(length.toString())];
				for (int index = 0; index < retarr.length; index++) {
					retarr[index] = getMethod.invoke(src, index, null);
				}
				return retarr;
			} catch(Exception e) {
				LOGGER.error(e.toString());
				LOGGER.debug(e.toString(), e);
			}
		} else if (src instanceof Bindings) {
			try {
				final Class<?> cls = Class.forName("jdk.nashorn.api.scripting.ScriptObjectMirror");
				if (cls.isAssignableFrom(src.getClass())) {
					final Method isArray = cls.getMethod("isArray");
					final Object result = isArray.invoke(src);
					if (result != null && result.equals(true)) {
						final Method values = cls.getMethod("values");
						final Object vals = values.invoke(src);
						if (vals instanceof Collection<?>) {
							final Collection<?> coll = (Collection<?>) vals;
							return coll.toArray(new Object[0]);
						}
					}
				}
			} catch(Exception e) {
				LOGGER.error(e.toString());
				LOGGER.debug(e.toString(), e);
			}
		} else if (src instanceof List<?>) {
			final List<?> list = (List<?>) src;
			return list.toArray(new Object[0]);
		} else if (src == UniqueTag.NOT_FOUND
				|| src == UniqueTag.NULL_VALUE) {
			return null;
		}
		return src;
	}
}
