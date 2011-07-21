package org.lsc.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.lsc.Task;

public class ScriptingEvaluator {

	/**
	 * The instances, one per thread to protect non thread safe engines like
	 * Rhino.!
	 */
	private static Map<String, ScriptingEvaluator> instances = new HashMap<String, ScriptingEvaluator>();

	private static Map<String, Class<? extends ScriptableEvaluator>> implementetionsCache;

	private Map<String, ScriptableEvaluator> instancesCache;

//	// Logger
//	private static final Logger LOGGER = LoggerFactory
//			.getLogger(ScriptingEvaluator.class);

	private ScriptableEvaluator defaultImplementation;

	static {
		implementetionsCache = new HashMap<String, Class<? extends ScriptableEvaluator>>();
	}

	private ScriptingEvaluator() {
		instancesCache = new HashMap<String, ScriptableEvaluator>();
		ScriptEngineManager mgr = new ScriptEngineManager();
		List<ScriptEngineFactory> factories = mgr.getEngineFactories();
		for (ScriptEngineFactory sef : factories) {
			for (String name : sef.getNames()) {
				if("js".equals(name)) {
					instancesCache.put(name,
							new JScriptEvaluator(sef.getScriptEngine()));
				}
			}
		}
		defaultImplementation = new JScriptEvaluator(
				mgr.getEngineByExtension("js"));
	}

	public static ScriptingEvaluator getInstance() {
		String threadName = Thread.currentThread().getName();
		if (instances.get(threadName) == null) {
			instances.put(threadName, new ScriptingEvaluator());
		}
		return instances.get(threadName);
	}

	public static void contribute(String implementationName, Class<? extends ScriptableEvaluator> implementationClass) {
		implementetionsCache.put(implementationName, implementationClass);
	}
	
	private ScriptableEvaluator identifyScriptingEngine(
			String expression) {
		String[] parts = expression.split(":");
		if (parts != null && parts.length > 0
				&& instancesCache.containsKey(parts[0])) {
			return instancesCache.get(parts[0]);
		}
		return defaultImplementation;
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
	 */
	public static String evalToString(final Task task, final String expression,
			final Map<String, Object> params) {
		ScriptableEvaluator se = getInstance().identifyScriptingEngine(
				expression);
		return se.evalToString(task, expression, params);
	}

	public static List<String> evalToStringList(final Task task,
			final String expression, final Map<String, Object> params) {
		ScriptableEvaluator se = getInstance().identifyScriptingEngine(
				expression);
		return se.evalToStringList(task, expression, params);
	}

	public static Boolean evalToBoolean(final Task task,
			final String expression, final Map<String, Object> params) {
		ScriptableEvaluator se = getInstance().identifyScriptingEngine(
				expression);
		return se.evalToBoolean(task, expression, params);
	}

}
