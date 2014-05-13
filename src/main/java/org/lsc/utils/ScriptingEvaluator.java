package org.lsc.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.commons.collections.map.LRUMap;
import org.lsc.Task;
import org.lsc.exception.LscServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptingEvaluator {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptingEvaluator.class);
	
	/**
	 * The instances, one per thread to protect non thread safe engines like
	 * Rhino.!
	 */
	private static LRUMap instances = new LRUMap(15, 0.75f);

	private static Map<String, Class<? extends ScriptableEvaluator>> implementetionsCache;

	private Map<String, ScriptableEvaluator> instancesCache;

	private ScriptableEvaluator defaultImplementation;

	static {
		implementetionsCache = new HashMap<String, Class<? extends ScriptableEvaluator>>();
	}

	private ScriptingEvaluator() {
		instancesCache = new HashMap<String, ScriptableEvaluator>();
		ScriptEngineManager mgr = new ScriptEngineManager();
		List<ScriptEngineFactory> factories = mgr.getEngineFactories();
		for (ScriptEngineFactory sef : factories) {
			boolean loaded = true;
			for (String name : sef.getNames()) {
				if ("js".equals(name)) {
					instancesCache.put(name,
							new JScriptEvaluator(sef.getScriptEngine()));
					break;
				} else if ("groovy".equals(name)) {
					instancesCache.put("gr",
							new GroovyEvaluator(sef.getScriptEngine()));
					break;
				}
				loaded = false;
			}
			if(!loaded) {
				LOGGER.debug("Unsupported scripting engine: " + sef.getEngineName());
			}
		}
        // Add the rhino engine without debugging capabilities
		instancesCache.put("rjs",
                new RhinoJScriptEvaluator(false));
        // Add the rhino engine with debugging capabilities
        instancesCache.put("rdjs",
                new RhinoJScriptEvaluator(true));

        // Default to Javascript
		defaultImplementation = instancesCache.get("js");
	}

	public static ScriptingEvaluator getInstance() {
		String threadName = Thread.currentThread().getName();
		ScriptingEvaluator scriptingEvaluator = (ScriptingEvaluator) instances
				.get(threadName);
		if (scriptingEvaluator == null) {
			scriptingEvaluator = new ScriptingEvaluator();
			instances.put(threadName, scriptingEvaluator);
		}
		return scriptingEvaluator;
	}

	public static void contribute(String implementationName,
			Class<? extends ScriptableEvaluator> implementationClass) {
		implementetionsCache.put(implementationName, implementationClass);
	}

	private ScriptableEvaluator identifyScriptingEngine(String expression) {
		String[] parts = expression.split(":");
		if (parts != null && parts.length > 0 && parts[0].length() < 10
				&& instancesCache.containsKey(parts[0])) {
			return instancesCache.get(parts[0]);
		}
		return defaultImplementation;
	}

	/**
	 * Remove scripting engine prefix if required
	 * @param expression the expression
	 * @return the expression without the "prefix:" prefix
	 */
	private String removePrefix(String expression) {
        String[] parts = expression.split(":");
        if (parts != null && parts.length > 0 && parts[0].length() < 10
                && instancesCache.containsKey(parts[0])) {
            return expression.substring(expression.indexOf(":") + 1);
        }
        return expression;
	}
	
	/**
	 * Evaluate your Ecma script expression (manage pre-compiled expressions
	 * cache).
	 * 
	 * @param expression
	 *            the expression to eval
	 * @param params
	 *            the keys are the name used in the
	 * @return the evaluation result
	 * @throws LscServiceException 
	 */
	public static String evalToString(final Task task, final String expression,
			final Map<String, Object> params) throws LscServiceException {
		ScriptableEvaluator se = getInstance().identifyScriptingEngine(
				expression);
		return se.evalToString(task, getInstance().removePrefix(expression), params);
	}

	public static List<String> evalToStringList(final Task task,
			final String expression, final Map<String, Object> params) throws LscServiceException {
		ScriptableEvaluator se = getInstance().identifyScriptingEngine(
				expression);
		return se.evalToStringList(task, getInstance().removePrefix(expression), params);
	}

	public static Boolean evalToBoolean(final Task task,
			final String expression, final Map<String, Object> params) throws LscServiceException {
		ScriptableEvaluator se = getInstance().identifyScriptingEngine(
				expression);
		return se.evalToBoolean(task, getInstance().removePrefix(expression), params);
	}

}
