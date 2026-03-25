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
*/
package org.lsc.utils.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;


/**
 * An helper class used to find annotations in methods and classes
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class AnnotationUtils
{
    private AnnotationUtils() {
    }

    /**
     * Get an instance of a class extracted from the annotation found in the method
     * or the class. We iterate on the stack trace until we find the desired annotation.
     *
     * @param clazz The Annotation we want to get an instance for
     * @return The instance or null if no annotation is found
     * @throws ClassNotFoundException If we can't find a class
     */
    public static Object getInstance( Class<? extends Annotation> clazz ) throws ClassNotFoundException {
        Object instance = null;

        // Get the caller by inspecting the stackTrace
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // Iterate on the stack trace.
        for ( int i = stackTrace.length - 1; i >= 0; i-- ) {
            Class<?> classCaller = null;

            // Get the current class
            try {
                classCaller = Class.forName( stackTrace[i].getClassName() );
            }
            catch ( ClassNotFoundException cnfe ) {
                // Corner case : we just have to go higher in the stack in this case.
                continue;
            }

            // Get the current method
            String methodCaller = stackTrace[i].getMethodName();

            // Check if we have any annotation associated with the method
            Method[] methods = classCaller.getMethods();

            for ( Method method : methods ) {
                if ( methodCaller.equals( method.getName() ) ) {
                    instance = method.getAnnotation( clazz );

                    if ( instance != null ) {
                        break;
                    }
                }
            }

            if ( instance == null ) {
                instance = classCaller.getAnnotation( clazz );
            }

            if ( instance != null ) {
                break;
            }
        }

        return instance;
    }
}
