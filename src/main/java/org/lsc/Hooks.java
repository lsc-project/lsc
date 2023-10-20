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
 ****************************************************************************
 */
package org.lsc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.ProcessBuilder;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import com.fasterxml.jackson.databind.ObjectMapper; // For encoding object to JSON
import com.fasterxml.jackson.databind.ObjectWriter;



/**
 * This object is managing posthook scripts
 */
public class Hooks {

	static final Logger LOGGER = LoggerFactory.getLogger(AbstractSynchronize.class);
	/**
	 * Method calling a postSyncHook if necessary
	 *
	 * return nothing
	 */
	public final static void postSyncHook(final String hook, final LscModifications lm) {

		if( hook != null && ! hook.equals("") )
		{
			// Compute json modifications
			String jsonModifications = null;

			switch (lm.getOperation()) {
				case CREATE_OBJECT:
					jsonModifications = getJsonModifications(lm);
					callHook("create", hook, lm.getMainIdentifier(), jsonModifications);
					break;

				case UPDATE_OBJECT:
					jsonModifications = getJsonModifications(lm);
					callHook("update", hook, lm.getMainIdentifier(), jsonModifications);
					break;

				case CHANGE_ID:
					jsonModifications = getJsonModifications(lm);
					callHook("changeId", hook, lm.getMainIdentifier(), jsonModifications);
					break;

				case DELETE_OBJECT:
					callHook("delete", hook, lm.getMainIdentifier(), jsonModifications);
					break;

				default:
					LOGGER.info("Error: unknown operation for posthook {}", hook);
			}
		}
	}

	public final static void callHook(	String operationType,
						String hook,
						String identifier,
						String jsonModifications) {

		LOGGER.info("Calling {} posthook {} for {}", operationType, hook, identifier);
		try {
			if( jsonModifications != null ) {
				Process p = new ProcessBuilder(
					hook,
					identifier,
					operationType,
					jsonModifications)
				.start();
			}
			else {
				Process p = new ProcessBuilder(
					hook,
					identifier,
					operationType)
				.start();
			}
		}
		catch(IOException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			LOGGER.error("Error while calling {} posthook {} for {}: {}",
					operationType,hook, identifier, sw.toString());
		}
	}

	/**
	 * Method computing modifications as json
	 *
	 * @return modifications in a json String
	 */
	public final static String getJsonModifications(final LscModifications lm) {
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String json = "";
		try {
			json = ow.writeValueAsString(lm.getLscAttributeModifications());
		}
		catch(Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			LOGGER.error("Error while encoding LSC modifications to json", sw.toString());
		}
		return json;
	}

}
