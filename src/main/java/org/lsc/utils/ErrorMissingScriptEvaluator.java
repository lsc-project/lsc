package org.lsc.utils;

import org.lsc.Task;
import org.lsc.exception.LscServiceException;

import java.util.List;
import java.util.Map;

public class ErrorMissingScriptEvaluator
implements ScriptableEvaluator {
    @Override
    public String evalToString(Task task, String expression, Map<String, Object> params) throws LscServiceException {
        throw new LscServiceException("Missing Script evaluator");
    }

    @Override
    public List<Object> evalToObjectList(Task task, String expression, Map<String, Object> params) throws LscServiceException {
        throw new LscServiceException("Missing Script evaluator");
    }

    @Override
    public List<byte[]> evalToByteArrayList(Task task, String expression, Map<String, Object> params) throws LscServiceException {
        throw new LscServiceException("Missing Script evaluator");
    }

    @Override
    public byte[] evalToByteArray(Task task, String expression, Map<String, Object> params) throws LscServiceException {
        throw new LscServiceException("Missing Script evaluator");
    }

    @Override
    public Boolean evalToBoolean(Task task, String expression, Map<String, Object> params) throws LscServiceException {
        throw new LscServiceException("Missing Script evaluator");
    }


}
