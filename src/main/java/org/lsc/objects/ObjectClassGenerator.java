/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 * 
 * Copyright (c) 2008, LSC Project 
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
 *               (c) 2008 - 2009 LSC Project
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.objects;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.lsc.AbstractGenerator;
import org.lsc.jndi.parser.LdapAttributeType;
import org.lsc.jndi.parser.LdapObjectClass;

/**
 * In order to get the right stuff for synchronization from the enhanced schema
 * this class is fine to generate the corresponding class.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class ObjectClassGenerator extends AbstractGenerator {

	/** This is the bean related class name. */
	private String initialName;

	/** This it the mono valued attributes in list bean related class name. */
	private List<String> monoAttrs;

	/** This is the multi valued attributes list in bean related class name. */
	private List<String> multiAttrs;

	/** This is the object on which the generation loop is done. */
	private LdapObjectClass objectClass;

	/** This is the local logger. */
	private static final Logger LOGGER = Logger
			.getLogger(ObjectClassGenerator.class);

	/**
	 * Must not be call.
	 * 
	 * @return nothing
	 * @see org.lsc.AbstractGenerator#generateContent()
	 */
	protected final String generateContent() {
		throw new RuntimeException("Must never be there !");
	}

	/**
	 * Generate java content.
	 * 
	 * @param inheritFrom
	 *            the parent class
	 * @return Content within string representation.
	 * @throws NamingException 
	 */
	protected final String generateContent(final String inheritFrom) throws NamingException {

		String classContent = "";
		classContent += "/*\n * Generated - please do not edit manually\n */\n";
		classContent += "package " + getPackageName() + ";\n\n";
		if (multiAttrs.size() > 0) {
			classContent += "import java.util.List;\n\n";
		}
		classContent += "/**\n * LDAP " + getClassName()
				+ " objectClass representation.\n */\n";
		if (inheritFrom != null) {
			classContent += "public class " + getClassName() + " extends "
					+ inheritFrom + " {\n\n";
			try {
				Class.forName(getPackageName() + "." + inheritFrom);
			} catch (ClassNotFoundException e) {
				ObjectClassGenerator.run(inheritFrom, getDestination(), isFromSource());
			}
		} else {
			classContent += "public class " + getClassName() + " {\n\n";
		}

		/**
		 * If not generating flat object, add the constructor and the
		 * objectclass name
		 */
		if (!getClassName().startsWith("f")) {
			// Adding the main constructor
			classContent += wt(1) + "/**\n" + wt(1)
					+ " * Default constructor.\n" + wt(1) + " */\n";
			classContent += wt(1) + "public " + getClassName() + "() {\n";
			classContent += wt(2) + "super();\n\t\tobjectClass.add(\""
					+ getClassName() + "\");\n	}\n\n";
		}

		Iterator<String> multiAttrsIter = multiAttrs.iterator();
		while (multiAttrsIter.hasNext()) {
			String attr = multiAttrsIter.next();
			classContent += wt(1) + "/** Multivalued attribute : " + attr
					+ ". */\n";
			classContent += wt(1) + "private List " + attr + ";\n\n";
		}
		Iterator<String> monoAttrsIter = monoAttrs.iterator();
		while (monoAttrsIter.hasNext()) {
			String attr = monoAttrsIter.next();
			classContent += wt(1) + "/** Monovalued attribute : " + attr
					+ ". */\n";
			classContent += wt(1) + "private String " + attr + ";\n\n";
		}

		multiAttrsIter = multiAttrs.iterator();
		while (multiAttrsIter.hasNext()) {
			String attr = (String) multiAttrsIter.next();
			// Generate the multivalued getter
			classContent += wt(1) + "/**\n";
			classContent += wt(1) + " * " + attr + " getter.\n";
			classContent += wt(1) + " * @return " + attr + " values\n";
			classContent += wt(1) + " */\n";
			classContent += wt(1) + "public final List get"
					+ attr.substring(0, 1).toUpperCase() + attr.substring(1)
					+ "() {\n";
			classContent += wt(2) + "return " + attr + ";\n";
			classContent += wt(1) + "}\n\n";
			// Generate the multivalued setter
			classContent += wt(1) + "/**\n";
			classContent += wt(1) + " * " + attr + " setter.\n";
			classContent += wt(1) + " * @param values " + attr + " values\n";
			classContent += wt(1) + " */\n";
			classContent += wt(1) + "public final void set"
					+ attr.substring(0, 1).toUpperCase() + attr.substring(1)
					+ "(final List values) {\n";
			classContent += wt(2) + attr + " = values;\n";
			classContent += wt(1) + "}\n\n";
		}
		monoAttrsIter = monoAttrs.iterator();
		while (monoAttrsIter.hasNext()) {
			// Generate the monovalued setter
			String attr = (String) monoAttrsIter.next();
			classContent += wt(1) + "/**\n";
			classContent += wt(1) + " * Default " + attr + " getter.\n";
			classContent += wt(1) + " * @return " + attr + " value\n";
			classContent += wt(1) + " */\n";
			classContent += wt(1) + "public final String get"
					+ attr.substring(0, 1).toUpperCase() + attr.substring(1)
					+ "() {\n";
			classContent += wt(2) + "return " + attr + ";\n";
			classContent += wt(1) + "}\n\n";
			// Generate the monovalued setter
			classContent += wt(1) + "/**\n";
			classContent += wt(1) + " * " + attr + " setter.\n";
			classContent += wt(1) + " * @param value " + attr + " value\n";
			classContent += wt(1) + " */\n";
			classContent += wt(1) + "public final void set"
					+ attr.substring(0, 1).toUpperCase() + attr.substring(1)
					+ "(final String value) {\n";
			classContent += wt(2) + "this." + attr + " = value;\n";
			classContent += wt(1) + "}\n\n";
		}
		classContent += "}";
		return classContent;
	}

	/**
	 * Generate the class file according to the definition given in the ldap
	 * schema.
	 * 
	 * @param className
	 *            the class name to look for into the directory in the
	 *            Configuration.DN_LDAP_SCHEMA tree
	 * @return true if the creation has succeded
	 * @throws NamingException
	 *             thrown while using the enhanced schema tree
	 */
	public final boolean generate(final String className)
			throws NamingException {
		if (!(getOcs() != null && getOcs().size() > 0 && getAttrs() != null && getAttrs()
				.size() > 0)) {
			LOGGER.error("Generator have to be initialized");
			return false;
		}

		// Could loop on this method to do batch generation

		this.initialName = className;

		setClassName(className);
		setPackageName(getGenericPackageName());

		Map<String, LdapAttributeType> ats = new HashMap<String, LdapAttributeType>();
		Iterator<String> atIter = getAttrs().iterator();
		while (atIter.hasNext()) {
			String atStr = atIter.next();
			LdapAttributeType lat = LdapAttributeType.parse(atStr);
			if (lat != null) {
				ats.put(lat.getName(), lat);
			}
		}

		Iterator<String> ocIter = getOcs().iterator();
		while (ocIter.hasNext() && objectClass == null) {
			String ocStr = ocIter.next();
			LdapObjectClass loc = LdapObjectClass.parse(ocStr, ats);
			if (loc != null
					&& loc.getName().compareToIgnoreCase(this.initialName) == 0) {
				objectClass = loc;
			}
		}

		// Try to generate files
		boolean ret = true;
		if (objectClass != null) {
			monoAttrs = objectClass.getMonoAttrs();
			multiAttrs = objectClass.getMultiAttrs();

			// Generate Class content
			if (writeContent(generateContent(objectClass.getInheritFrom()))) {
				objectClass.getMonoAttrs().addAll(objectClass.getMultiAttrs());
				objectClass.getMultiAttrs().clear();
				setClassName("f" + className.substring(0, 1).toUpperCase()
						+ className.substring(1));
				setPackageName(getClass().getPackage().getName() + ".flat");
				if (writeContent(generateContent("f"
						+ objectClass.getInheritFrom().substring(0, 1)
								.toUpperCase()
						+ objectClass.getInheritFrom().substring(1)))) {
					LOGGER.info("POJOs generation successed for "
							+ getFileName());
					ret &= true;
				} else {
					LOGGER.info("Flat POJO generation failed for "
							+ getFileName());
					ret &= false;
				}
				// TODO: Refactor this resetting
				setClassName(className);
				setPackageName(getGenericPackageName());
			} else {
				LOGGER.info("Non flat POJO generation failed for "
						+ getFileName());
				ret &= false;
			}
		} else {
			LOGGER.error("POJOs generation failed : LDAP objectClass ("
					+ initialName + ") could not be found in LDAP directory.");
			ret &= false;
		}

		return ret;
	}

	/**
	 * Return a generic package name.
	 * 
	 * @return A generic package name.
	 */
	public final String getGenericPackageName() {
		return this.getClass().getPackage().getName();
	}

	/**
	 * Run the POJO generator.
	 * 
	 * @param className
	 *            the bean related class name
	 * @param destination
	 *            the destination directory
	 * @param fromSource
	 *            the generated bean is related to source directory (or
	 *            destination)
	 * @return the POJO name or null
	 * @throws NamingException
	 *             thrown if an directory exception is encountered while
	 *             generating the new bean
	 */
	public static final String run(final String className,
			final String destination, final boolean fromSource)
			throws NamingException {
		ObjectClassGenerator ocg = new ObjectClassGenerator();
		ocg.init(fromSource);
		ocg.setDestination(destination);
		ocg.generate(className);
		return ocg.getPackageName() + "." + ocg.getClassName();
	}

	/**
	 * Return a generic file name for latest generated file.
	 * 
	 * @return A java generic file name.
	 */
	public final String getFileName() {
		return getStandardFileName();
	}
}
