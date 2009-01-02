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
package org.lsc.beans;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import org.lsc.AbstractGenerator;
import org.lsc.jndi.parser.LdapAttributeType;
import org.lsc.jndi.parser.LdapObjectClass;

/**
 * In order to get the right stuff for synchronization from the enhanced schema
 * this class is fine to generate the corresponding bean. Then this bean have to
 * be complete.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class BeanGenerator extends AbstractGenerator {

    /** This is the bean related class name. */
    private String initialName;

    /** This it the mono valued attributes in list bean related class name. */
    private List<String> monoAttrs;

    /** This is the multi valued attributes list in bean related class name. */
    private List<String> multiAttrs;

    /**
     * Generate bean file.
     * 
     * @param className
     *                the classname related generating operation
     * @return the generation status
     * @throws NamingException
     *                 thrown if an directory exception is encountered while
     *                 generating the new bean
     */
    protected final boolean generate(final String className)
    throws NamingException {

        if (!(getOcs() != null && getOcs().size() > 0 
                && getAttrs() != null && getAttrs()
                .size() > 0)) {
            LOGGER.error("Generator have to be initialized");
            return false;
        }

        // Could loop on this method to do batch generation

        initialName = className;
        setClassName(className + "Bean");
        setPackageName(getGenericPackageName());

        // Get ldap object class of corresponding new bean

        Map<String, LdapAttributeType> ats = 
            new HashMap<String, LdapAttributeType>();
        Iterator<String> atIter = getAttrs().iterator();
        while (atIter.hasNext()) {
            String atStr = atIter.next();
            LdapAttributeType lat = LdapAttributeType.parse(atStr);
            if (lat != null) {
                ats.put(lat.getName(), lat);
            }
        }

        LdapObjectClass myLoc = null;
        Iterator<String> ocIter = getOcs().iterator();
        while (ocIter.hasNext() && myLoc == null) {
            String ocStr = ocIter.next();
            LdapObjectClass loc = LdapObjectClass.parse(ocStr, ats);
            if (loc != null
                    && loc.getName().compareToIgnoreCase(initialName) == 0) {
                myLoc = loc;
            }
        }

        // Try to generate bean class file

        if (myLoc != null) {
            this.monoAttrs = myLoc.getMonoAttrs();
            this.multiAttrs = myLoc.getMultiAttrs();
            if (this.writeContent(generateContent())) {
                LOGGER.info("Bean generation successed for "
                        + this.getFileName());
                return true;
            }
        } else {
            LOGGER.error("Bean generation failed : LDAP objectClass ("
                    + this.initialName
                    + ") could not be found in LDAP directory.");
        }

        return false;
    }

    /**
     * Return a generic package name.
     * 
     * @return A generic package name
     */
    protected final String getGenericPackageName() {
        return this.getClass().getPackage().getName();
    }

    /**
     * Generate java content.
     * 
     * @return Content within string representation.
     */
    protected final String generateContent() {

        String beanClassName = super.getClassName();
        String initialClassName = this.initialName;
        String content = "";

        content += "/*\n * Generated - please do not edit manually\n */\n";
        content += "package " + this.getPackageName() + ";\n\n";
        content += "import java.lang.reflect.InvocationTargetException;\n"
            + "import java.lang.reflect.Method;\n"
            + "import java.util.HashMap;\n";

        // if (multiAttrs.size() > 0) {
        // content += "import java.util.List;\n";
        // }

        // + "import org.lsc.Configuration;\n"
        content += "import javax.naming.NamingException;\n\n"
            + "import org.lsc.jndi.JndiModifications;\n"
            + "import org.lsc.objects." + initialClassName + ";\n"
            + "import org.lsc.objects.top;\n\n";

        content += "public class " + beanClassName
        + " extends AbstractBean implements IBean {\n\n";

        content += "	public static " + beanClassName + " getInstance("
        + "top myclass) throws IllegalArgumentException, "
        + "IllegalAccessException, InvocationTargetException, "
        + "NamingException {\n" + wt(2) + beanClassName
        + " bean = new " + beanClassName + "() ;\n" + wt(2)
        + "AbstractBean.mapper(" + beanClassName
        + ".class, bean, myclass);\n" + wt(2)
        + "bean.generateDn();\n" + wt(2) + "return bean;\n" + wt(1)
        + "}\n\n";

        content += wt(1) + "public " + beanClassName + "() {\n" + wt(2)
        + "super();\n" + wt(1) + "}\n\n";

        Iterator<String> monoAttrsIter = monoAttrs.iterator();
        while (monoAttrsIter.hasNext()) {
            String attribute = monoAttrsIter.next();
            String attributeName = attribute.substring(0, 1).toUpperCase()
            + attribute.substring(1);

            content += "    //public static void map"
                + attributeName
                + "("
                + initialClassName
                + " soc, IBean doc, String value) throws NamingException {\n"
                + wt(2)
                + "// Do nothing because it is generated through other map "
                + "methods !\n"
                + wt(2)
                + "//if (value != null && value.trim().length() > 0) {\n"
                + wt(2) + "//	mapString(doc, \"" + attribute
                + "\", Filters.filterString(value));\n" + "		//	generate"
                + attributeName + "(soc, doc);\n" + "		//}\n" + "	//}\n\n";

            content += "	public static void generate" + attributeName + "("
            + initialClassName
            + " soc, IBean doc) throws NamingException {\n"
            + "		// to be completed\n" + "		//String value = \"\";\n"
            + "		//Attribute attr = new BasicAttribute(\"" + attribute
            + "\");\n" + "		//attr.add(value);\n"
            + "		//doc.setAttribute(attr);\n" + "	}\n\n";
        }

        Iterator<String> multiAttrsIter = multiAttrs.iterator();
        while (multiAttrsIter.hasNext()) {
            String attribute = multiAttrsIter.next();
            String attributeName = attribute.substring(0, 1).toUpperCase()
            + attribute.substring(1);

            content += wt(1)
            + "//public static void map"
            + attributeName
            + "("
            + initialClassName
            + " soc, IBean doc, List values) throws NamingException {\n"
            + wt(2)
            + "// Do nothing because it is generated through other map "
            + "methods !\n"
            + "		//if (values != null && values.size() > 0) {\n"
            + "		//	Vector<String> v = new Vector<String>();\n"
            + "		//	Iterator valuesIter = values.iterator();\n"
            + "		//	while (valuesIter.hasNext()) {\n"
            + "		//		String value = (String) valuesIter.next();\n"
            + "		//		if (value != null && value.trim().length() > 0) {\n"
            + "		//			mapString(doc, \"" + attribute
            + "\", Filters.filterString(value));\n"
            + "		//			generate"
            + attributeName + "(soc, doc);\n" + "		//		}\n"
            + "		//	}\n" + "		//}\n" + "	//}\n\n";

            content += "	//public static void generate" + attributeName + "("
            + initialClassName
            + " soc, IBean doc) throws NamingException {\n"
            + "		// to be completed\n" + "		//String value = \"\";\n"
            + "		//Attribute attr = new BasicAttribute(\"" + attribute
            + "\");\n" + "		//attr.add(value);\n"
            + "		//doc.setAttribute(attr);\n" + "	//}\n\n";
        }

        content += "	public JndiModifications[] checkDependenciesWith"
            + initialClassName + "(" + initialClassName
            + " soc, JndiModifications jm) {\n" + "		// to be completed\n"
            + "		return new JndiModifications[] {};\n" + "	}\n\n";

        content += wt(1)
        + "//public void generateDn() throws NamingException {\n"
        + wt(2)
        + "//setDistinguishName(\"uid=\" + getAttributeById(\"uid\")."
        + "get() + \",\" + Configuration.DN_PEOPLE);\n"
        + wt(2)
        + "//throw new UnsupportedOperationException(\"Complete this to make"
        + " it available !\");\n"
        + wt(1) + "//}\n\n";
        
        // close class definition
        content += "}\n";

        return content;
    }

    /**
     * Launch the whole bean generation process.
     * @param className
     *                the bean related class name
     * @param destination
     *                the destination directory
     * @param fromSource
     *                the generated bean is related to source directory (or
     *                destination)
     * @return the bean name
     * @throws NamingException
     *                 thrown if an directory exception is encountered while
     *                 generating the new bean
     */
    public static final String run(final String className,
            final String destination, final boolean fromSource)
    throws NamingException {
        BeanGenerator beanGenerator = new BeanGenerator();
        beanGenerator.init(fromSource);
        beanGenerator.setDestination(destination);
        beanGenerator.generate(className);
        return beanGenerator.getPackageName() + "."
        + beanGenerator.getClassName();
    }

    /**
     * Return a generic file name for latest generated file.
     * @return A java generic file name.
     */
    public final String getFileName() {
        return getStandardFileName();
    }
}
