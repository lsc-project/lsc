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
import java.io.OutputStream;
import java.io.IOException;
import java.util.Optional;
import org.lsc.utils.output.LdifLayout;
import org.lsc.beans.syncoptions.ISyncOptions.OutputFormat;
import com.fasterxml.jackson.databind.ObjectMapper; // For encoding object to JSON
import com.fasterxml.jackson.databind.ObjectWriter;


/**
 * This object is managing posthook scripts
 */
public class Hooks {

	static final Logger LOGGER = LoggerFactory.getLogger(Hooks.class);

	private static final ObjectMapper Mapper = new ObjectMapper();
	/**
	 * Method calling a postSyncHook if necessary
	 *
	 * return nothing
	 */
	public final void postSyncHook(final Optional<String> hook, final OutputFormat outputFormat, final LscModifications lm) {

		hook.ifPresent( h -> callHook(lm.getOperation(), h, lm.getMainIdentifier(), outputFormat, lm));
	}

	public final static void callHook(	LscModificationType operationType,
						String hook,
						String identifier,
						OutputFormat outputFormat,
						LscModifications lm) {

		LOGGER.info("Calling {} posthook {} with format {} for {}",
				operationType.getDescription(),
				hook,
				outputFormat.toString(),
				identifier);

		String modifications = null;
		// Compute modifications only in a create / update / changeid operation
		if( operationType != LscModificationType.DELETE_OBJECT)
			{
			if( outputFormat == OutputFormat.JSON ) {
				modifications = getJsonModifications(lm);
			}
			else {
				modifications = LdifLayout.format(lm);
			}
		}

		try {
			if( modifications != null ) {
				Process p = new ProcessBuilder(
					hook,
					identifier,
					operationType.getDescription())
				.start();

				// sends ldif modifications to stdin of hook script
				OutputStream stdin = p.getOutputStream();
				stdin.write(modifications.getBytes());
				stdin.write("\n".getBytes());
				stdin.flush();
				stdin.close();
			}
			else {
				Process p = new ProcessBuilder(
					hook,
					identifier,
					operationType.getDescription())
				.start();
			}
		}
		catch(IOException e) {
			LOGGER.error("Error while calling {} posthook {} with format {} for {}",
					operationType.getDescription(),
					hook,
					outputFormat.toString(),
					identifier,
					e);
		}
	}

	/**
	* Method computing modifications as json
	*
	* @return modifications in a json String
	*/
	public final static String getJsonModifications(final LscModifications lm) {
		ObjectWriter ow = Mapper.writer().withDefaultPrettyPrinter();
		String json = "";
		try {
			json = ow.writeValueAsString(lm.getLscAttributeModifications());
		}
		catch(Exception e) {
			LOGGER.error("Error while encoding LSC modifications to json", e);
		}
		return json;
	}

}
