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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.api.util.Strings;
import org.lsc.Task;
import org.lsc.exception.LscServiceException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;

/**
 * An abstract class that gather all the common methods for the JScript and RhinoJScript
 * evaluator classes.
 */
public abstract class AbstractJSEvaluator implements ScriptableEvaluator {
    /** {@inheritDoc} */
    @Override
    public String evalToString(final Task task, final String expression, final Map<String, Object> params)
            throws LscServiceException {
        Object result = instanceEval(task, expression, params);

        if (result == null) {
            return null;
        } else if (result instanceof String) {
            return (String) result;
        } else {
            return result.toString();
        }
    }


    /** {@inheritDoc} */
    @Override
    public Boolean evalToBoolean(Task task, String expression, Map<String, Object> params) throws LscServiceException {
        try {
            return (Boolean) Context.jsToJava(instanceEval(task, expression, params), Boolean.class);
        } catch (EvaluatorException e) {
            throw new LscServiceException(e);
        }
    }


    /** {@inheritDoc} */
    @Override
    public byte[] evalToByteArray(final Task task, final String expression, final Map<String, Object> params)
            throws LscServiceException {
        Object result = null;
        try {
            result = convertJsToJava(instanceEval(task, expression, params));
        } catch (EvaluatorException e) {
            throw new LscServiceException(e);
        }

        if (result instanceof byte[]) {
            return (byte[]) result;
        } else if (result instanceof String) {
            return ((String) result).getBytes();
        } else {
            return result.toString().getBytes();
        }
    }


    /** {@inheritDoc} */
    @Override
    public List<byte[]> evalToByteArrayList(Task task, String expression, Map<String, Object> params)
            throws LscServiceException {
        Object result = null;

        try {
            result = convertJsToJava(instanceEval(task, expression, params));
        } catch (EvaluatorException e) {
            throw new LscServiceException(e);
        }

        if (result instanceof byte[][]) {
            return Arrays.asList((byte[][])result);
        } else if (result instanceof byte[]) {
            return Arrays.asList((byte[])result);
        } else if (result instanceof String) {
            List<byte[]> resultsArray = new ArrayList<>();
            String resultAsString = (String) result;

            if (Strings.isNotEmpty(resultAsString)) {
                resultsArray.add(resultAsString.getBytes());
            }

            return resultsArray;
        } else if (result instanceof List) {
            List<byte[]> resultsArray = new ArrayList<>();

            for (Object resultValue : (List<?>) result) {
                if (resultValue instanceof byte[]) {
                    resultsArray.add((byte[]) resultValue);
                } else {
                    resultsArray.add(resultValue.toString().getBytes());
                }
            }

            return resultsArray;
        } else if (result instanceof Set) {
            List<byte[]> resultsArray = new ArrayList<>();

            for (Object resultValue : (Set<?>) result) {
                if (resultValue instanceof byte[]) {
                    resultsArray.add((byte[]) resultValue);
                } else {
                    resultsArray.add(resultValue.toString().getBytes());
                }
            }

            return resultsArray;
        } else if (result == null) {
            return null;
        } else {
            List<byte[]> resultsArray = new ArrayList<>();

            if (result != null) {
                resultsArray.add(result.toString().getBytes());
            }
            return resultsArray;
        }
    }


    /** {@inheritDoc} */
    @Override
    public List<Object> evalToObjectList(Task task, String expression, Map<String, Object> params)
            throws LscServiceException {
        Object result = null;

        try {
            result = convertJsToJava(instanceEval(task, expression, params));
        } catch (EvaluatorException e) {
            throw new LscServiceException(e);
        }

        if (result == null) {
            return null;
        }

        if (result instanceof String) {
            if (Strings.isNotEmpty((String)result)) {
                return Arrays.asList(result);
            } else {
                return new ArrayList<Object>();
            }
        }

        if (result instanceof Object[]) {
            return Arrays.asList((Object[])result);
        }

        List<Object> resultsArray = new ArrayList<Object>();

        if (result instanceof String[]) {
            for (String resultValue : (String[]) result) {
                resultsArray.add(resultValue);
            }
        }

        if (result instanceof List) {
            for (Object resultValue : (List<?>) result) {
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

    @Override
    public String evalToFilter(Task task, String expression, Map<String, Object> params) throws LscServiceException {
        Object result = instanceEval(task, expression, params);

        if (result == null) {
            return expression;
        } else if (result instanceof String) {
            return (String) result;
        } else {
            return result.toString();
        }
    }

    /**
     * Local instance evaluation.
     *
     * @param task The associated task
     * @param expression the expression to evaluate
     * @param params the keys are the name used in the
     * @return the evaluation result
     * @throws LscServiceException If the evaluation throws an exception
     */
    abstract protected Object instanceEval(Task task, String expression, Map<String, Object> params)
        throws LscServiceException;

    /**
     * A method to convert the JavaScript result value to Java
     *
     * @param src The Javascript result to convert
     * @return A converted Java Object instance
     */
    abstract protected Object convertJsToJava(Object src);

}

