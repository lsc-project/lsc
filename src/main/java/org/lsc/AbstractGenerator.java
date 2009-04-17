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
package org.lsc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.lsc.jndi.JndiServices;


/**
 * Contains meta generator methods.
 *
 * @author Thomas Chemineau &lt;tchemineau@linagora.com&gt;
 */
public abstract class AbstractGenerator {
    /** This is the local logger. */
    public static final Logger LOGGER = 
        Logger.getLogger(AbstractGenerator.class);

    /** Where to store generated classes. */
    private String destination;

    /** This is the LDAP objectclass name on which the generator is working. */
    private String className;

    /**
     * This is the package name to use to identify the local path
     * where to store the generated file.
     */
    private String packageName;

    /**
     * This is the file path separator taken from
     * System.getProperty("file.separator").
     */
    private String separator;

    /** This is the list of object classes available in the directory. */
    private List<String> objectClasses;

    /** This is the list of attribute types available in the directory. */
    private List<String> attributeTypes;

    /** This generator apply to source (or destination) directory ? */
	private boolean fromSource;

    /**
     * Create a new AbstractGenerator for a new object from a basic class
     * name.
     */
    public AbstractGenerator() {
        this.separator = System.getProperty("file.separator");
    }

    /**
     * Generate file.
     *
     * @param objectClassName the target object class name
     *
     * @return the generation status
     *
     * @throws NamingException thrown if an exception is encountered during
     *         generation
     */
    protected abstract boolean generate(String objectClassName) throws NamingException;

    /**
     * Return class name for latest generated file.
     *
     * @return Class name for latest generated file.
     */
    public final String getClassName() {
        return className;
    }

    /**
     * Return a generic file name for latest generated file.
     *
     * @return A java generic file name.
     */
    public abstract String getFileName();

    /**
     * Is this generator applying to source directory (false = target) ?
     * @return applied to source ?
     */
    public boolean isFromSource() {
    	return fromSource;
    }
    
    /**
     * Get the default standard filename.
     *
     * @return the filename.
     */
    public final String getStandardFileName() {
        return getPackagePath() + separator + getClassName() + ".java";
    }

    /**
     * Return a generic package name.
     *
     * @return A generic package name.
     */
    protected abstract String getGenericPackageName();

    /**
     * Return package name for latest generated bean.
     *
     * @return A bean package name.
     */
    public final String getPackageName() {
        return packageName;
    }

    /**
     * Return package path for generated file.
     *
     * @return A package source path.
     */
    public final String getPackagePath() {
        String mainLocation = null;

        if (destination != null) {
            mainLocation = destination;
        } else {
            mainLocation = System.getProperty("user.dir") + separator + "src"
            + separator + "main" + separator + "java";
        }

        return mainLocation + separator
                + packageName.replaceAll("\\.", "\\" + separator);
    }

    /**
     * Prepare data, so we just get informations from LDAP directory.
     * This method could be called one first time before any generations.
     *
     * @return the initialization status
     *
     * @throws NamingException thrown when an directory related exception is
     *         encountered while initializing the generator
     *
     * @deprecated
     */
    public final boolean init() throws NamingException {
        return init(false);
    }

    /**
     * Initialize the generator fields.
     *
     * @param fromSource the generated object is based on the source directory
     *        (otherwise on the destination)
     *
     * @return the initialization status
     *
     * @throws NamingException thrown when a directory related exception is
     *         encountered while initializing this generator
     */
    public final boolean init(final boolean fromSource)
    throws NamingException {
        JndiServices js = null;

        this.fromSource = fromSource;
        
        if (fromSource) {
            js = JndiServices.getSrcInstance();
        } else {
            js = JndiServices.getDstInstance();
        }

        Map<String, List<String>> ocsTemp = js.getSchema(new String[] {
                "objectclasses"
        });
        Map<String, List<String>> atsTemp = js.getSchema(new String[] {
                "attributetypes"
        });

        if ((ocsTemp == null) || (ocsTemp.keySet().size() == 0)
                || (atsTemp == null) || (atsTemp.keySet().size() == 0)) {
            LOGGER.error("Unable to read objectclasses or attributetypes in "
                    + "ldap schema! Exiting...");
            return false;
        }

        objectClasses = filterNames(ocsTemp.values().iterator().next());
        attributeTypes = filterNames(atsTemp.values().iterator().next());

        return true;
    }

    /**
     * Filter the attribute and object classes names
     * @param names List of names
     * @return list of filtered names
     */
    private List<String> filterNames(List<String> names) {
    	List<String> filteredNames = new ArrayList<String>();
    	Iterator<String> namesIter = names.iterator();
    	while(namesIter.hasNext()) {
    		String name = namesIter.next();
    		String filteredName = filterName(name);
    		if(filteredName != null) {
    			filteredNames.add(filteredName);
    		} else {
    			LOGGER.error("Name invalid: " + name + ". Attributes or object class not generated !!!");
    		}
    	}
    	return filteredNames;
    	
	}

    /**
     * Filter name according to attribute or object class 
     * @param name the originale name
     * @return the filtered name or null if not matching
     */
	public String filterName(String name) {
		String REGEX = "^\\p{Alpha}[\\w]*$";
		Pattern p = Pattern.compile(REGEX);
		Matcher m = p.matcher(name);
		if(m.matches()) {
			return null;
		} else {
			return name;
		}
	}

	/**
     * Default setter for the package name.
     *
     * @param lpackageName the package name to use in this generator
     */
    public final void setPackageName(final String lpackageName) {
        packageName = lpackageName;
    }

    // Protected methods
    /**
     * Generate java content.
     *
     * @return Content within string representation.
     */
    protected abstract String generateContent();

    /**
     * Write java content in a particular file.
     *
     * @param content the content to write
     *
     * @return True if the operation succeeded.
     */
    protected final boolean writeContent(final String content) {
        return writeContent(content, false);
    }
    /**
     * Write java content in a particular file.
     *
     * @param content the content to write
     * @param overwrite overwrite the file if exists
     * @return true if the operation succeeded.
     */
    protected final boolean writeContent(final String content, 
            final boolean overwrite) {
        String fileName = getFileName();
        File file = new File(fileName);

        LOGGER.info("Creating file (" + fileName + ") ...");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("\n---------------\n" + content + "---------------\n");
        }

        try {
            if (file.exists() && !overwrite) {
                LOGGER.warn("File generation failed: file (" + fileName
                        + ") already exists.");
            } else if (file.createNewFile() || overwrite) {
                FileOutputStream os = new FileOutputStream(file);
                os.write(content.getBytes());
                return true;
            } else {
                LOGGER.error("File generation failed: file (" + fileName
                        + ") could not be created (probably a rights issue).");
            }
        } catch (FileNotFoundException fnfe) {
            LOGGER.error(fnfe + " (" + fileName + ")", fnfe);
        } catch (IOException e) {
            LOGGER.error(e + " (" + fileName + ")", e);
        }

        return false;
    }

    /**
     * Default setter for destination.
     *
     * @param ldestination destination name to set
     */
    public final void setDestination(final String ldestination) {
        this.destination = ldestination;
    }

    /**
     * Default getter for attribute types string data.
     *
     * @return the attribute types
     */
    public final List<String> getAttrs() {
        return attributeTypes;
    }

    /**
     * Default object classes getter.
     *
     * @return the object classes list
     */
    public final List<String> getOcs() {
        return objectClasses;
    }

    /**
     * Default separator getter.
     *
     * @return the separator
     */
    public final String getSeparator() {
        return separator;
    }

    /**
     * Default destination getter.
     *
     * @return the destination
     */
    public final String getDestination() {
        return destination;
    }

    /**
     * Default class name setter.
     *
     * @param lclassName the class name
     */
    public final void setClassName(final String lclassName) {
        className = lclassName;
    }

    static Map<Integer, String> wtCache = new HashMap<Integer, String>();
    
    /**
     * Generate white space with a length of the specified number of
     * tabulation number.
     *
     * @param tabNumber tabulation number
     *
     * @return the string composed of the required white spaces
     */
    protected static final String wt(final int tabNumber) {
        if (!wtCache.containsKey(tabNumber)) {
            StringBuffer sb = new StringBuffer(tabNumber);

            for (int i = 0; i < tabNumber; i++) {
                sb.append("\t");
            }

            wtCache.put(tabNumber, sb.toString());
        }

        return wtCache.get(tabNumber);
    }
}
