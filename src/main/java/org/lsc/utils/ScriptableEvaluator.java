package org.lsc.utils;

import java.util.List;
import java.util.Map;

import org.lsc.Task;
import org.lsc.exception.LscServiceException;

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
	 * @throws LscServiceException
	 */
	public String evalToString(final Task task, final String expression,
					final Map<String, Object> params) throws LscServiceException;

	public List<String> evalToStringList(final Task task, final String expression,
					final Map<String, Object> params) throws LscServiceException;

	public Boolean evalToBoolean(final Task task, final String expression, final Map<String, Object> params) throws LscServiceException;
}
