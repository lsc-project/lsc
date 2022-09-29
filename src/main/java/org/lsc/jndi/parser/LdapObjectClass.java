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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Define an ldap objectclass representation.
 * 
 * Successfully tested with OpenLDAP 2.3
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class LdapObjectClass {

	/** This is the maximum number of parser pass before considering failing. */
	private static final int MAX_PASS_BEFORE_FAILING = 100;

	/** The attribute object identifier. */
	private String oid;

	/** The main attribute name. */
	private String name;

	/** The attribute extensions. */
	private Map<String, String> x;

	/** The attribute parent type - may be null. */
	private String inheritFrom;

	/** The attribute description. */
	private String description;

	/** Object class type (structural, auxiliary, ...). */
	private String type;

	/** List of mono valued attributes. */
	private List<String> monoAttrs;

	/** List of multi valued attributes. */
	private List<String> multiAttrs;

	private static final Logger LOGGER = LoggerFactory.getLogger(LdapAttributeType.class);

	/**
	 * The default constructor.
	 */
	public LdapObjectClass() {
		x = new HashMap<String, String>();
		monoAttrs = new ArrayList<String>();
		multiAttrs = new ArrayList<String>();
	}

	/**
	 * 
	 * @param value
	 * @param pattern
	 * @return the matched values array
	 */
	public static String[] execRegex(String value, String pattern) {
		Pattern ocPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		Matcher ocMatcher = ocPattern.matcher(value);
		if (ocMatcher.matches()) {
			return new String[] { ocMatcher.group(1), ocMatcher.group(2) };
		}
		return null;
	}

	public static String[] execRegex3(String value, String pattern) {
		Pattern ocPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		Matcher ocMatcher = ocPattern.matcher(value);
		if (ocMatcher.matches()) {
			return new String[] { ocMatcher.group(1), ocMatcher.group(2),
					ocMatcher.group(3) };
		}
		return null;
	}

	/**
	 * Parse the object class description.
	 * @param ocStr object class description
	 * @param ats attribute types
	 * @return the completed object
	 */
	public static LdapObjectClass parse(final String ocStr,
			final Map<String, LdapAttributeType> ats) {
		LdapObjectClass loc = new LdapObjectClass();
		String rest = ocStr;
		List<String> attrs = new ArrayList<String>();

		String[] ret = execRegex(rest, "\\(\\s+([0-9\\.]+)(.*)\\s*\\)\\s*");
		if (ret != null) {
			loc.oid = ret[0];
			rest = ret[1];
		} else {
			LOGGER.error("Unable to match the oid in \"{}\"", ocStr);
			return null;
		}

		ret = execRegex(rest, "\\s*NAME\\s+(\\([^\\)]+\\)|[^ ]+)(.*)\\s*");
		if (ret != null) {
			if (ret[0].startsWith("(")) {
				ret[0] = ret[0].substring(1, ret[0].length() - 1);
			}
			StringTokenizer names = new StringTokenizer(ret[0], " ");
			loc.setName(names.nextToken());
			if (names.hasMoreElements()) {
				LOGGER.debug("Multiple names not supported. Using first one ({}) for \"{}\"",
								loc.name, ocStr);
			}
			rest = ret[1];
		} else {
			LOGGER.error("Unable to match the name in \"{}\"", ocStr);
			return null;
		}

		int maxPass = 0;
		for (; rest != null && rest.length() > 0
		&& maxPass < MAX_PASS_BEFORE_FAILING; maxPass++) {
			LOGGER.debug("Re/Starting analysis with rest=\"{}\"", rest);
			ret = execRegex(rest, "\\s*SUP\\s+(\\([^\\)]+\\)|[^\\s]+)?\\s*(.*)\\s*");
			if (ret != null) {
				if (ret[0].startsWith("(")) {
					ret[0] = ret[0].substring(1, ret[0].length() - 2);
				}
				StringTokenizer sups = new StringTokenizer(ret[0], "$");
				loc.inheritFrom = sups.nextToken();
				if (sups.hasMoreElements()) {
					LOGGER.warn("Multiple inheritence not supported. Using first one ({}) for \"{}\"", loc.inheritFrom, ocStr);
				}
				rest = ret[1];
			}

			ret = execRegex(rest, "\\s*DESC\\s+('[^']*')\\s*(.*)\\s*");
			if (ret != null) {
				loc.description = ret[0];
				rest = ret[1];
			}

			ret = execRegex(rest,
			"\\s*MUST\\s+(\\([^\\)]+\\)|[^\\s]+)?\\s*(.*)\\s*");
			if (ret != null) {
				if (ret[0].startsWith("(")) {
					ret[0] = ret[0].substring(1, ret[0].length() - 2);
				}
				StringTokenizer musts = new StringTokenizer(ret[0], "$");
				attrs.addAll(toList(musts));
				rest = ret[1];
			}

			ret = execRegex(rest,
			"\\s*MAY\\s+(\\([^\\)]+\\)|[^\\s]+)?\\s*(.*)\\s*");
			if (ret != null) {
				if (ret[0].startsWith("(")) {
					ret[0] = ret[0].substring(1, ret[0].length() - 2);
				}
				StringTokenizer mays = new StringTokenizer(ret[0], "$");
				attrs.addAll(toList(mays));
				rest = ret[1];
			}

			ret = execRegex3(rest, "\\s*X-([^\\s]+)\\s+('[^']+')?\\s*(.*)\\s*");
			if (ret != null) {
				loc.x.put(ret[0], ret[1]);
				rest = ret[2];
			}

			ret = execRegex(rest,
			"\\s*(STRUCTURAL|AUXILIARY|ABSTRACT)\\s*(.*)\\s*");
			if (ret != null) {
				loc.type = ret[0];
				rest = ret[1];
			}
		}

		if (maxPass >= MAX_PASS_BEFORE_FAILING) {
			LOGGER.error("The parser encountered an error while parsing the following string : {} while parsing {}",
							rest, ocStr);
			return null;
		}

		if (loc.inheritFrom == null) {
			LOGGER.debug("No inheritence found for \"{}\". Defaulting to top", ocStr);
			loc.inheritFrom = "top";
		}
		if (loc.description == null) {
			LOGGER.debug("No description found for \"{}\"", ocStr);
		}
		if (loc.type == null) {
			loc.type = "AUXILIARY";
			LOGGER.debug("No structural or abstract type found. Defaulting to auxiliary in \"{}\"", ocStr);
		}

		// Managing attributes
		for (String attributeName : attrs) {
			
			// ignore attributes with names containing a "-"
			// see http://tools.lsc-project.org/issues/show/31
			if (attributeName.indexOf('-') != -1) {
				badAttributeName(attributeName);
				continue;
			}
			
			if (ats.get(attributeName) != null
					&& ats.get(attributeName).isSingleValue()) {
				loc.monoAttrs.add(attributeName);
			} else {
				loc.multiAttrs.add(attributeName);
			}
		}
		LOGGER.debug("Successfully parsed objectclass {}", loc.name);
		return loc;
	}

	private static List<String> toList(final StringTokenizer names) {
		List<String> ret = new ArrayList<String>();
		while (names.hasMoreTokens()) {
			ret.add(names.nextToken().trim());
		}
		return ret;
	}

	public final String getInheritFrom() {
		return inheritFrom;
	}

	public final void setInheritFrom(String inheritFrom) {
		this.inheritFrom = inheritFrom;
	}

	public final List<String> getMultiAttrs() {
		return multiAttrs;
	}

	public final void setMultiAttrs(List<String> multiAttrs) {
		this.multiAttrs = multiAttrs;
	}

	public final String getName() {
		return name;
	}

	public final void setName(String name) {
		if (name.trim().startsWith("'")) {
			this.name = name.trim().substring(1, name.length() - 1);
		} else {
			this.name = name.trim();
		}
	}

	public final String getDescription() {
		return description;
	}

	public final void setDescription(String description) {
		this.description = description;
	}

	public final List<String> getMonoAttrs() {
		return monoAttrs;
	}

	public final void setMonoAttrs(List<String> optionalAttrs) {
		this.monoAttrs = optionalAttrs;
	}

	public final String getType() {
		return type;
	}

	public final void setType(String type) {
		this.type = type;
	}

	public final String getOid() {
		return oid;
	}

	public final void setOid(String oid) {
		this.oid = oid;
	}
	
	/**
	 * Handle bad attribute names found during generation.
	 *
	 * Currently, this applies to names containing "-", which causes
	 * invalid method names in Java. We just log a warning for now.
	 *
	 * @param name The attribute name that we rejected
	 */
	protected static final void badAttributeName(String name) {
		LOGGER.warn("Ignoring attribute {}. It contains currently unsupported characters. See http://tools.lsc-project.org/issues/show/31", name);
	}

}
