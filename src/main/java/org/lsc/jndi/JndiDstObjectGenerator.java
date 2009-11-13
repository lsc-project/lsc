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
package org.lsc.jndi;

import org.lsc.AbstractGenerator;
import org.lsc.jndi.parser.LdapAttributeType;
import org.lsc.jndi.parser.LdapObjectClass;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingException;


/**
 * In order to get the right stuff for synchronization from the enhanced
 * schema this class is fine to generate the corresponding JNDI source
 * object. Then this object have to be complete.
 *
 * @author Thomas Chemineau &lt;tchemineau@linagora.com&gt;
 */
public class JndiDstObjectGenerator extends AbstractGenerator {
    /** This is the bean related class name. */
    private String initialName;

    /**
     * Generate bean file.
     *
     * @param className the classname related generating operation
     *
     * @return the generation status
     *
     * @throws NamingException thrown if an directory exception is encountered
     *         while generating the new bean
     */
    public final boolean generate(final String className)
                           throws NamingException {
        if (!((getOcs() != null) && (getOcs().size() > 0)
                && (getAttrs() != null) && (getAttrs().size() > 0))) {
            LOGGER.error("Generator have to be initialized");

            return false;
        }

        // Could loop on this method to do batch generation
        this.initialName = className;
        setClassName(className.substring(0, 1).toUpperCase()
                     + className.substring(1) + "JndiService");
        setPackageName(getGenericPackageName());

        // Get ldap object class of corresponding new bean
        Map<String, LdapAttributeType> ats = 
            new HashMap<String, LdapAttributeType>();
        Iterator<String> atIter = getAttrs().iterator();

        while (atIter.hasNext()) {
            String atStr = (String) atIter.next();
            LdapAttributeType lat = LdapAttributeType.parse(atStr);

            if (lat != null) {
                ats.put(lat.getName(), lat);
            }
        }

        LdapObjectClass myLoc = null;
        Iterator<String> ocIter = getOcs().iterator();

        while (ocIter.hasNext() && (myLoc == null)) {
            String ocStr = ocIter.next();
            LdapObjectClass loc = LdapObjectClass.parse(ocStr, ats);

            if ((loc != null) && (loc.getName().
        	    compareToIgnoreCase(this.initialName) == 0)) {
                myLoc = loc;
            }
        }

        // Try to generate bean class file
        if (myLoc != null) {
            String content = generateContent();

            if (writeContent(content)) {
                LOGGER.info("JndiObject generation successed for {}", this.getFileName());
                return true;
            }
        } else {
            LOGGER.error("JndiObject generation failed : LDAP objectClass ({}) could not be found in LDAP directory.",
										this.initialName);
        }

        return false;
    }

    /**
     * Return a generic package name.
     *
     * @return A generic package name
     */
    public final String getGenericPackageName() {
        return this.getClass().getPackage().getName();
    }

    /**
     * Generate java content.
     *
     * @return Content within string representation.
     */
    protected final String generateContent() {
        String beanClassName = this.initialName + "Bean";
        String jndiClassName = super.getClassName();
        String content = "";

        content += "/*\n * Generated - please do not edit manually\n */\n";
        content += ("package " + this.getPackageName() + ";\n\n");
        content += ("import java.util.Iterator;\n\n"
                   + "import javax.naming.NamingException;\n\n"
                   + "import javax.naming.directory.SearchControls;\n"
                   + "import org.lsc.Configuration;\n"
                   + "import org.lsc.beans.AbstractBean;\n"
                   + "import org.lsc.beans." + beanClassName
                   + ";\n\n");
        // + "import org.lsc.objects.top;\n\n";
        content += ("public class " + jndiClassName
                   + " implements IJndiDstService {\n\n");
        content += ("	public AbstractBean getBean(String id) throws "
                   + "NamingException {\n"
                   + "		//@TODO: Please refactor to find the correct "
                   + "entry\n\t\t//return (" + beanClassName + ") "
                   + beanClassName
                   + ".getInstance(JndiServices.getInstance().getEntry("
                   + "Configuration.DN_PEOPLE, \"uid=\" + id), "
                   + "Configuration.DN_PEOPLE, "
                   + beanClassName + ".class);\n" + wt(2) 
                   + "return null;\n" + wt(1)
                   + "}\n\n");

        content += ("	public Iterator<String> getIdsList() throws "
        	   + "NamingException {\n"
                   + "		//@TODO: Please refactor to find the "
                   + "correct entry\n\t\t//return JndiServices.getInstance()"
                   + ".getAttrList(Configuration.DN_PEOPLE, \"objectClass="
                   + initialName
                   + "\", SearchControls.SUBTREE_SCOPE, \"uid\").values()."
                   + "iterator();\n"
                   + "		return null;\n" + "	}\n\n");

        content += "}";

        return content;
    }

    // Static methods
    /**
     * Launch the whole bean generation process.
     *
     * @param className the bean related class name
     * @param destination the destination directory
     *
     * @return the generated object name or null if failed
     *
     * @throws NamingException thrown if an directory exception is encountered
     *         while generating the new bean
     */
    public static String run(final String className, final String destination)
                      throws NamingException {
        JndiDstObjectGenerator jndiGenerator = new JndiDstObjectGenerator();
        jndiGenerator.init(false);
        jndiGenerator.setDestination(destination);

        if (jndiGenerator.generate(className)) {
            return jndiGenerator.getClassName();
        } else {
            return null;
        }
    }

    /**
     * Return a generic file name for latest generated file.
     * @return A java generic file name.
     */
    public final String getFileName() {
	return getStandardFileName();
    }
}
