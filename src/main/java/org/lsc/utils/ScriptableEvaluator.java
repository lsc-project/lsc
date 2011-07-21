package org.lsc.utils;

import java.util.List;
import java.util.Map;

import org.lsc.Task;

public interface ScriptableEvaluator {

	/**
	 * Evaluate your Ecma script expression (manage pre-compiled expressions
	 * cache).
	 *
	 * @param expression
	 *                the expression to eval
	 * @param params
	 *                the keys are the name used in the
	 * @return the evaluation result
	 */
	public String evalToString(final Task task, final String expression,
					final Map<String, Object> params);

	public List<String> evalToStringList(final Task task, final String expression,
					final Map<String, Object> params);

	public Boolean evalToBoolean(final Task task, final String expression, final Map<String, Object> params);
}
