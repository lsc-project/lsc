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
package org.lsc.jndi.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Define an ldap attribute type representation. Successfully tested with
 * OpenLDAP 2.3 and 2.4
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class LdapAttributeType {

	/** This is the maximum number of parser pass before considering failing. */
	private static final int MAX_PASS_BEFORE_FAILING = 100;

	/** The attribute is single valued. */
	private boolean singleValue;

	/** The directory does not allowed any user attribute modification. */
	private boolean noUserModification;

	/** @TODO */
	private boolean directoryOperation;

	/** The directory reversed this attribute for its internals. */
	private boolean dSAOperation;

	/** Attribute that should no longer be used. */
	private boolean obsolete;
	
	/** @TODO */
	private boolean distributedOperation;

	/** The attribute object identifier. */
	private String oid;

	/** The main attribute name. */
	private String name;

	/** The attribute usage. */
	private String usage;

	/** The attribute syntax. */
	private String syntax;

	/** The attribute extensions. */
	private Map<String, String> x;

	/** The attribute parent type - may be null. */
	private String inheritFrom;

	/** The attribute description. */
	private String description;

	/** The equality rule applying to this attribute - or null. */
	private String equalityRule;

	/** The content matching rule applying to this attribute - or null. */
	private String substringRule;

	/** The ordering rule applying to this attribute - or null. */
	private String orderingRule;

	private static final Logger LOGGER = LoggerFactory.getLogger(LdapAttributeType.class);

	/**
	 * The default constructor.
	 */
	public LdapAttributeType() {
		singleValue = false;
		noUserModification = false;
		directoryOperation = false;
		dSAOperation = false;
		distributedOperation = false;
		x = new HashMap<String, String>();
	}

	/**
	 * The main object builder.
	 * 
	 * @param atStr
	 *                the string extracted from the directory describing the
	 *                attribute type
	 * @return the new completed object
	 */
	public static LdapAttributeType parse(final String atStr) {
		LdapAttributeType lat = new LdapAttributeType();
		String rest = atStr;

		String[] ret = LdapObjectClass.execRegex(rest, "\\(\\s+([0-9\\.]+)(.*)\\s*\\)\\s*");
		if (ret != null) {
			lat.oid = ret[0];
			rest = ret[1];
		} else {
			LOGGER.error("Unable to match the oid in \"{}\"", atStr);
			return null;
		}

		ret = LdapObjectClass.execRegex(rest, "\\s*NAME\\s+(\\([^\\)]+\\)|[^ ]+)(.*)\\s*");
		if (ret != null) {
			if (ret[0].startsWith("(")) {
				ret[0] = ret[0].substring(1, ret[0].length() - 1);
			}
			StringTokenizer names = new StringTokenizer(ret[0], " ");
			lat.setName(names.nextToken());
			if (names.hasMoreElements()) {
				LOGGER.debug("Multiple names not supported. Using first one ({}) for \"{}\"",
								lat.name, atStr);
			}
			rest = ret[1];
		} else {
			LOGGER.error("Unable to match the name in \"{}\"", atStr);
			return null;
		}
		
		int maxPass = 0;
		for (; rest != null && rest.length() > 0
				&& maxPass < MAX_PASS_BEFORE_FAILING; maxPass++) {
			LOGGER.debug("Re/Starting analysis with rest=\"{}\"", rest);

			ret = LdapObjectClass.execRegex(rest, "\\s*SYNTAX\\s+([^\\s]+)\\s*(.*)\\s*");
			if (ret != null) {
				lat.syntax = ret[0];
				rest = ret[1];
			}

			ret = LdapObjectClass.execRegex(rest, "\\s*SUP\\s+([^\\s]+)\\s*(.*)\\s*");
			if (ret != null) {
				lat.description = ret[0];
				rest = ret[1];
			}

			ret = LdapObjectClass.execRegex(rest, "\\s*DESC\\s+('[^']*')\\s*(.*)\\s*");
			if (ret != null) {
				lat.description = ret[0];
				rest = ret[1];
			}

			ret = LdapObjectClass.execRegex(rest, "\\s*EQUALITY\\s+([^\\s]+)\\s*(.*)\\s*");
			if (ret != null) {
				lat.equalityRule = ret[0];
				rest = ret[1];
			}

			ret = LdapObjectClass.execRegex(rest, "\\s*SUBSTR\\s+([^\\s]+)\\s*(.*)\\s*");
			if (ret != null) {
				lat.substringRule = ret[0];
				rest = ret[1];
			}

			ret = LdapObjectClass.execRegex(rest, "\\s*SUBSTRINGS\\s+([^\\s]+)\\s*(.*)\\s*");
			if (ret != null) {
				lat.substringRule = ret[0];
				rest = ret[1];
			}

			ret = LdapObjectClass.execRegex(rest, "\\s*ORDERING\\s+([^\\s]+)\\s*(.*)\\s*");
			if (ret != null) {
				lat.orderingRule = ret[0];
				rest = ret[1];
			}

			ret = LdapObjectClass.execRegex(rest, "\\s*USAGE\\s+([^\\s]+)\\s*(.*)\\s*");
			if (ret != null) {
				lat.usage = ret[0];
				rest = ret[1];
			}

			ret = LdapObjectClass.execRegex3(rest, "\\s*X-([^\\s]+)\\s+('[^']+')?\\s*(.*)\\s*");
			if (ret != null) {
				lat.x.put(ret[0], ret[1]);
				rest = ret[2];
			}

			ret = LdapObjectClass.execRegex(rest, "\\s*(SINGLE-VALUE)\\s*(.*)\\s*");
			if (ret != null) {
				lat.singleValue = true;
				rest = ret[1];
			}

			ret = LdapObjectClass.execRegex(rest, "\\s*(NO-USER-MODIFICATION)\\s*(.*)\\s*");
			if (ret != null) {
				lat.noUserModification = true;
				rest = ret[1];
			}

			ret = LdapObjectClass.execRegex(rest, "\\s*(directoryOperation)\\s*(.*)\\s*");
			if (ret != null) {
				lat.directoryOperation = true;
				rest = ret[1];
			}

			ret = LdapObjectClass.execRegex(rest, "\\s*(dSAOperation)\\s*(.*)\\s*");
			if (ret != null) {
				lat.dSAOperation = true;
				rest = ret[1];
			}

			ret = LdapObjectClass.execRegex(rest, "\\s*(distributedOperation)\\s*(.*)\\s*");
			if (ret != null) {
				lat.distributedOperation = true;
				rest = ret[1];
			}
			
			ret = LdapObjectClass.execRegex(rest, "\\s*(OBSOLETE)\\s*(.*)\\s*");
			if (ret != null) {
				lat.obsolete = true;
				rest = ret[1];
			}
		}

		if (maxPass >= MAX_PASS_BEFORE_FAILING) {
			LOGGER.error("The parser encountered an error while parsing the following string : {} while parsing {}",
							rest, atStr);
			return null;
		}

		if (lat.inheritFrom == null) {
			LOGGER.debug("No inheritence found for \"{}\"", atStr);
		}
		if (lat.description == null) {
			LOGGER.debug("No description found for \"{}\"", atStr);
		}
		if (lat.equalityRule == null) {
			LOGGER.debug("No equality rule found for \"{}\"", atStr);
		}
		if (lat.substringRule == null) {
			LOGGER.debug("No substring rule found for \"{}\"", atStr);
		}
		if (lat.orderingRule == null) {
			LOGGER.debug("No ordering rule found for \"{}\"", atStr);
		}
		if (lat.x.size() == 0) {
			LOGGER.debug("No x rule found for \"{}\"", atStr);
		}

		return lat;
	}

	/**
	 * The slightly modify name setter.
	 * 
	 * @param lname
	 *                the name to set
	 */
	public final void setName(final String lname) {
		name = lname.trim();
		if (name.startsWith("'")) {
			name = name.substring(1, name.length() - 1);
		}
	}

	/**
	 * Default getter for singleValue.
	 * 
	 * @return the singleValue
	 */
	public final boolean isSingleValue() {
		return singleValue;
	}

	/**
	 * Default getter for name.
	 * 
	 * @return the name
	 */
	public final String getName() {
		return name;
	}

	public static int getMAX_PASS_BEFORE_FAILING() {
		return MAX_PASS_BEFORE_FAILING;
	}

	public boolean isNoUserModification() {
		return noUserModification;
	}

	public boolean isDirectoryOperation() {
		return directoryOperation;
	}

	public boolean isDSAOperation() {
		return dSAOperation;
	}

	public boolean isObsolete() {
		return obsolete;
	}

	public boolean isDistributedOperation() {
		return distributedOperation;
	}

	public String getOid() {
		return oid;
	}

	public String getUsage() {
		return usage;
	}

	public String getSyntax() {
		return syntax;
	}

	public Map<String, String> getX() {
		return x;
	}

	public String getInheritFrom() {
		return inheritFrom;
	}

	public String getDescription() {
		return description;
	}

	public String getEqualityRule() {
		return equalityRule;
	}

	public String getSubstringRule() {
		return substringRule;
	}

	public String getOrderingRule() {
		return orderingRule;
	}
}
