package org.lsc.utils;

import java.util.List;
import java.util.Map;

import org.lsc.Task;
import org.lsc.exception.LscServiceException;

public interface ScriptableEvaluator {

    /**
     * Evaluate your script expression to a single string
     * @param task the task concerned by this evaluation
     * @param expression the expression to evaluate
     * @param params the keys are the name used in the
     * @return the evaluation result, null if nothing
     * @throws LscServiceException thrown when a technical error is encountere
     */
    public String evalToString(final Task task, final String expression, final Map<String, Object> params)
            throws LscServiceException;

    /**
     * Evaluate your script expression to a list of string
     * @param task the task concerned by this evaluation
     * @param expression the expression to evaluate
     * @param params the keys are the name used in the
     * @return the evaluation result, null if nothing
     * @throws LscServiceException thrown when a technical error is encountere
     */
    public List<Object> evalToObjectList(final Task task, final String expression, final Map<String, Object> params)
            throws LscServiceException;

    /**
     * Evaluate your script expression to a list of byte array
     * @param task the task concerned by this evaluation
     * @param expression the expression to evaluate
     * @param params the keys are the name used in the
     * @return the evaluation result, null if nothing
     * @throws LscServiceException thrown when a technical error is encountere
     */
    public List<byte[]> evalToByteArrayList(final Task task, final String expression, final Map<String, Object> params)
            throws LscServiceException;

    /**
     * Evaluate your script expression to a byte array
     * @param task the task concerned by this evaluation
     * @param expression the expression to evaluate
     * @param params the keys are the name used in the
     * @return the evaluation result, null if nothing
     * @throws LscServiceException thrown when a technical error is encountere
     */
    public byte[] evalToByteArray(final Task task, final String expression, final Map<String, Object> params)
            throws LscServiceException;

    /**
     * Evaluate your script expression to a boolean value
     * @param task the task concerned by this evaluation
     * @param expression the expression to evaluate
     * @param params the keys are the name used in the
     * @return the evaluation result, null if nothing
     * @throws LscServiceException thrown when a technical error is encountere
     */
    public Boolean evalToBoolean(final Task task, final String expression, final Map<String, Object> params)
            throws LscServiceException;
}
