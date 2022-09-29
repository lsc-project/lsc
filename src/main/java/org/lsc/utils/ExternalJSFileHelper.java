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
 */package org.lsc.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.lsc.exception.LscServiceException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;

/**
 * First try to load and execute an function specified inside another js file.
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class ExternalJSFileHelper extends ScriptableObject {

    private static final long serialVersionUID = 8803488253722834372L;

    /**
     * 
     * @param jsFile relative or full path to the Javascript file
     * @param fctName the function name to call
     * @param args the array containing the various function parameters
     * @return the function result
     * @throws LscServiceException thrown if an error occurs while reading the file or parsing it
     */
    public static Object invoke(String jsFile, String fctName, Object[] args) throws LscServiceException {
        Context ctx = ContextFactory.getGlobal().enterContext();
        try {
            InputStream fis = new FileInputStream(jsFile);
            byte[] arr = new byte[30000];
            fis.read(arr);
            fis.close();
            ScriptableObject scope = ctx.initStandardObjects();
            ctx.evaluateString(scope, new String(arr).trim(), jsFile, 1, null);
            Function fct = (Function)scope.get(fctName, scope);
            return fct.call(ctx, scope, scope, args);
        } catch(EvaluatorException e) {
            throw new LscServiceException(e);
        } catch (IOException e) {
            throw new LscServiceException(e);
        } finally {
            Context.exit();
        }
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }
}
