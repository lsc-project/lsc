package org.lsc.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.script.*;

import org.lsc.Task;
import org.lsc.exception.LscServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class ScriptingEvaluator {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptingEvaluator.class);
	
	/**
	 * The instances, one per thread to protect non thread safe engines like
	 * Rhino.
	 */
	private static Cache<Object, Object> instancesCache;

	private static Map<String, Class<? extends ScriptableEvaluator>> implementetionsCache;

	public static ScriptEngineManager mgr;

	private Map<String, ScriptableEvaluator> instancesTypeCache;

	private ScriptableEvaluator defaultImplementation;

	static {
		implementetionsCache = new HashMap<String, Class<? extends ScriptableEvaluator>>();
        instancesCache = CacheBuilder.newBuilder().maximumSize(15).build();
        mgr = new ScriptEngineManager();
	}

	private ScriptingEvaluator() {
		instancesTypeCache = new HashMap<String, ScriptableEvaluator>();
		List<ScriptEngineFactory> factories = mgr.getEngineFactories();
		for (ScriptEngineFactory sef : factories) {
			boolean loaded = false;
			for (String name : sef.getNames()) {
				if ("js".equals(name)) {
					instancesTypeCache.put(name,
							new JScriptEvaluator(sef.getScriptEngine()));
					loaded = true;
					break;
				} else if ("groovy".equals(name)) {
					instancesTypeCache.put("gr",
							new GroovyEvaluator(sef.getScriptEngine()));
					loaded = true;
					break;
				}
				else if ("graal.js".equals(name)) {
					/** for some reason graal.js is not enumerated in factories this code is not hit
					 * so leave it out by getting it explicitly with
					 * ScriptEngineManager().getEngineByName("graal.js");
					 * later.
					 */
					loaded = true;
					break;

				}
			}
			if(!loaded) {
				LOGGER.debug("Unsupported scripting engine: " + sef.getEngineName());
			}
		}

        // Add the rhino engine without debugging capabilities
		instancesTypeCache.put("rjs",
                new RhinoJScriptEvaluator(false));
        // Add the rhino engine with debugging capabilities
        instancesTypeCache.put("rdjs",
                new RhinoJScriptEvaluator(true));

		ScriptEngine graaljsEngine = new ScriptEngineManager().getEngineByName("graal.js");
		if ( graaljsEngine != null ) {
			Bindings bindings = graaljsEngine.getBindings(ScriptContext.ENGINE_SCOPE);
			bindings.put("polyglot.js.allowHostAccess", true);
			bindings.put("polyglot.js.allowHostClassLookup", (Predicate<String>) s -> true);
			bindings.put( "polyglot.js.nashorn-compat", true);
			JScriptEvaluator graaljsevaluator = new JScriptEvaluator(graaljsEngine);
			instancesTypeCache.put("gj", graaljsevaluator);
			defaultImplementation = graaljsevaluator;
		}
		else {
			defaultImplementation = instancesTypeCache.get("js");
		}
	}

	public static ScriptingEvaluator getInstance() {
        String threadName = Thread.currentThread().getName();
        ScriptingEvaluator scriptingEvaluator = null;

        scriptingEvaluator = (ScriptingEvaluator) instancesCache.getIfPresent(threadName);
        if (scriptingEvaluator == null) {
            scriptingEvaluator = new ScriptingEvaluator();
            instancesCache.put(threadName, scriptingEvaluator);
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
				&& instancesTypeCache.containsKey(parts[0])) {
			return instancesTypeCache.get(parts[0]);
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
                && instancesTypeCache.containsKey(parts[0])) {
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

	public static List<Object> evalToObjectList(final Task task,
			final String expression, final Map<String, Object> params) throws LscServiceException {
		ScriptableEvaluator se = getInstance().identifyScriptingEngine(
				expression);
		return se.evalToObjectList(task, getInstance().removePrefix(expression), params);
	}

	public static List<byte[]> evalToByteArrayList(final Task task,
			final String expression, final Map<String, Object> params) throws LscServiceException {
		ScriptableEvaluator se = getInstance().identifyScriptingEngine(
				expression);
		return se.evalToByteArrayList(task, getInstance().removePrefix(expression), params);
	}

	public static byte[] evalToByteArray(final Task task,
			final String expression, final Map<String, Object> params) throws LscServiceException {
		ScriptableEvaluator se = getInstance().identifyScriptingEngine(
				expression);
		return se.evalToByteArray(task, getInstance().removePrefix(expression), params);
	}

	public static Boolean evalToBoolean(final Task task,
			final String expression, final Map<String, Object> params) throws LscServiceException {
		ScriptableEvaluator se = getInstance().identifyScriptingEngine(
				expression);
		return se.evalToBoolean(task, getInstance().removePrefix(expression), params);
	}

}
