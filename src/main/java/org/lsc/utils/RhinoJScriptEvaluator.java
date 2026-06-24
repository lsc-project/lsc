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
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of the LSC Project nor the names of its
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;
import org.lsc.Task;
import org.lsc.beans.IBean;
import org.lsc.exception.LscServiceException;
import org.lsc.jndi.AbstractSimpleJndiService;
import org.lsc.jndi.ScriptableJndiServices;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.UniqueTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the Rhino Java Script evaluation context.
 *
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public final class RhinoJScriptEvaluator extends AbstractJSEvaluator {

	// Logger
	private static final Logger LOGGER = LoggerFactory.getLogger(RhinoJScriptEvaluator.class);

	/** The local Rhino context. */
	private Context cx;

	/** debug flag */
	private boolean debug;

	/**
	 * Default public constructor.
	 */
	public RhinoJScriptEvaluator(boolean debug) {
		this.debug = debug;
	}

    /** {@inheritDoc} */
	protected Object instanceEval(Task task, String expression, Map<String, Object> params)
	        throws LscServiceException {
		RhinoDebugger rhinoDebugger = null;
		Map<String, Object> localParams = new HashMap<String, Object>();
		if (params != null) {
			localParams.putAll(params);
		}

		String mainIdentifier = "unknown id";
		if( params != null && params.containsKey("srcBean") &&
			((IBean) params.get("srcBean")).getMainIdentifier() != null )
		{
			mainIdentifier = "id=" + ((IBean) params.get("srcBean")).getMainIdentifier();
		}
		else if( params != null && params.containsKey("dstBean") &&
			((IBean) params.get("dstBean")).getMainIdentifier() != null )
		{
			mainIdentifier = "id=" + ((IBean) params.get("dstBean")).getMainIdentifier();
		}

		/*
		 * Allow to have shorter names for function in the package
		 * org.lsc.utils.directory
		 */
		String expressionImport = "with (new JavaImporter(Packages.org.lsc.utils.directory)) {"
				+ "with (new JavaImporter(Packages.org.lsc.utils)) {\n" + expression + "\n}}";

		ContextFactory factory = new ContextFactory();

		if (debug) {
			rhinoDebugger = new RhinoDebugger(expressionImport, factory);
		}

		cx = factory.enterContext();

		Scriptable scope = cx.initStandardObjects();

		// add LDAP interface for destination
		if (!localParams.containsKey("ldap") && task.getDestinationService() instanceof AbstractSimpleJndiService) {
			ScriptableJndiServices dstSjs = new ScriptableJndiServices();
			dstSjs.setJndiServices(((AbstractSimpleJndiService) task.getDestinationService()).getJndiServices());
			localParams.put("ldap", dstSjs);
		}

		// add LDAP interface for source
		if (!localParams.containsKey("srcLdap") && task.getSourceService() instanceof AbstractSimpleJndiService) {
			ScriptableJndiServices srcSjs = new ScriptableJndiServices();
			srcSjs.setJndiServices(((AbstractSimpleJndiService) task.getSourceService()).getJndiServices());
			localParams.put("srcLdap", srcSjs);
		}

		for (Entry<String, Object> entry : localParams.entrySet()) {
			Object jsObj = Context.javaToJS(entry.getValue(), scope);
			ScriptableObject.putProperty(scope, entry.getKey(), jsObj);
		}

		Object ret = null;
		try {
			List<Script> includes = new ArrayList<Script>();
			if (task.getScriptIncludes() != null) {
				for (File scriptInclude : task.getScriptIncludes()) {
					if ("js".equals(FilenameUtils.getExtension(scriptInclude.getAbsolutePath()))) {
						FileReader reader = new FileReader(scriptInclude);
						try {
							Script include = cx.compileReader(reader, scriptInclude.getAbsolutePath(), 1, null);
							includes.add(include);
						} finally {
							reader.close();
						}
					}
				}
			}
			Script script = cx.compileString(expressionImport, "<cmd>", 1, null);
			if (debug) {
				rhinoDebugger.initContext(cx, scope, script);
				Object jsObj = Context.javaToJS(rhinoDebugger, scope);
				ScriptableObject.putProperty(scope, "rhinoDebugger", jsObj);
				for (Script include : includes) {
					rhinoDebugger.execInclude(include);
				}
				ret = rhinoDebugger.exec();
			} else {
				for (Script include : includes) {
					include.exec(cx, scope);
				}
				ret = script.exec(cx, scope);
			}
		} catch (EcmaError e) {
			LOGGER.error("Fail to compute RhinoJS expression: " + expression + " on " +
					mainIdentifier + "\nReason: " + e.toString());
			LOGGER.debug(e.toString(), e);
			throw new LscServiceException(e);
		} catch (RuntimeException e) {
			LOGGER.error("Fail to compute RhinoJS expression: " + expression + " on " +
					mainIdentifier + "\nReason: " + e.toString());
			throw e;
		} catch (Exception e) {
			LOGGER.error(e.toString());
			LOGGER.debug(e.toString(), e);
			throw new LscServiceException(e);
		} finally {
			if (debug) {
				rhinoDebugger.run();
			}

			Context.exit();
		}

		return ret;
	}

    /** {@inheritDoc} */
	protected Object convertJsToJava(Object src) {
		if (src == null) {
			return null;
		} else if (src.getClass().getName().equals("sun.org.mozilla.javascript.internal.NativeJavaObject")) {
			return Context.jsToJava(src, Object.class);
		} else if (src.getClass().getName().equals("sun.org.mozilla.javascript.internal.NativeArray")) {
			try {
				Method getMethod = src.getClass().getMethod("get", int.class,
						Class.forName("sun.org.mozilla.javascript.internal.Scriptable"));
				Object length = src.getClass().getMethod("getLength").invoke(src);
				Object[] retarr = new Object[Integer.parseInt(length.toString())];
				for (int index = 0; index < retarr.length; index++) {
					retarr[index] = getMethod.invoke(src, index, null);
				}
				return retarr;
			} catch (Exception e) {
				LOGGER.error(e.toString());
				LOGGER.debug(e.toString(), e);
			}
		} else if (src == UniqueTag.NOT_FOUND || src == UniqueTag.NULL_VALUE) {
			return null;
		}
		return src;
	}
}
