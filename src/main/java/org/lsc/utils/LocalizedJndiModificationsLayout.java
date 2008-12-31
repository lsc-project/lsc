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
package org.lsc.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapName;

import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.lsc.Configuration;
import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;

/**
 * Provides a localized Log4J layout for LDAP entries.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class LocalizedJndiModificationsLayout extends PatternLayout {

    /* The logger */
    private static final Logger LOGGER = Logger.getLogger(LocalizedJndiModificationsLayout.class);
	
    /* The separator of the log operations */
    protected static String LOG_OPERATIONS_SEPARATOR = ",";
    
    /* Configurations from the log4j.properties */
    private String logOperation;
    
    /* The operations to log */
    protected Set<JndiModificationType> operations;
	
    /**
     * Default constructor.
     */
    public LocalizedJndiModificationsLayout() {
        super();
        activateOptions();
    }

    /**
     * Format the logging event. This formatter will use the default formatter
     * or a LDIF pretty printer
     * 
     * @param le
     *            the logging event to format
     * @return the formatted string
     */
    public final String format(final LoggingEvent le) {
        Object message = le.getMessage();
        String msg = "";

        if (message == null || !(JndiModifications.class.isAssignableFrom(message.getClass()))) {
            if(!onlyLdif) {
                msg = super.format(le);
            }
        } else {
            JndiModifications jm = (JndiModifications) message;

            if(operations.contains(jm.getOperation())) {
	            String baseUrl = (String) Configuration.getDstProperties().get(
	            "java.naming.provider.url");
	            baseUrl = baseUrl.substring(baseUrl.lastIndexOf("/") + 1);
	            String dn = "";
	            if (jm.getDistinguishName() != null
	                    && jm.getDistinguishName().length() > 0) {
	                dn = jm.getDistinguishName();
	                if (!dn.endsWith(baseUrl)) {
	                    dn += "," + baseUrl;
	                }
	            } else {
	                dn = baseUrl;
	            }
	            
	            switch (jm.getOperation()) {
	            case ADD_ENTRY:
	                msg = I18n.getMessage(this, "ADD_ENTRY", new Object[] { dn, listToLdif(jm.getModificationItems(), true) });
	                break;
	            case MODRDN_ENTRY:
	                LdapName ln;
	                try {
	                    ln = new LdapName(jm.getNewDistinguishName());
	                    msg = I18n.getMessage(this, "MODRDN_ENTRY", new Object[] { dn, ln.get(0), ln.getSuffix(1) });
	                } catch (InvalidNameException e) {
	                    msg = I18n.getMessage(this, "MODRDN_ENTRY", new Object[] {
	                            dn, jm.getNewDistinguishName(),
	                            jm.getNewDistinguishName() + "," + baseUrl });
	                }
	                break;
	            case MODIFY_ENTRY:
	                msg = I18n.getMessage(this, "MODIFY_ENTRY", new Object[] { dn, listToLdif(jm.getModificationItems(), false) });
	                break;
	            case DELETE_ENTRY:
	                msg = I18n.getMessage(this, "REMOVE_ENTRY", new Object[] { dn });
	                break;
	            default:
	            }
            }
        }
        return msg;
    }

    /**
     * Pretty print the modification items.
     * 
     * @param modificationItems
     *            the modification items to pretty print
     * @param addEntry
     *            is this a new entry
     * @return the string to log
     */
    private String listToLdif(final List<?> modificationItems,
            final boolean addEntry) {
        StringBuffer sb = new StringBuffer();
        Iterator<?> miIter = modificationItems.iterator();
        boolean first = true;
        
        while (miIter.hasNext()) {
            ModificationItem mi = (ModificationItem) miIter.next();
            Attribute attr = mi.getAttribute();
            try {
                if (!addEntry) {
                    if (!first) {
                        sb.append("-\n");
                    }
                    switch (mi.getModificationOp()) {
                    case DirContext.REMOVE_ATTRIBUTE:
                        sb.append("delete: ").append(attr.getID()).append("\n");
                        break;
                    case DirContext.REPLACE_ATTRIBUTE:
                        sb.append("replace: ").append(attr.getID())
                        .append("\n");
                        break;
                    case DirContext.ADD_ATTRIBUTE:
                    default:
                        sb.append("add: ").append(attr.getID()).append("\n");
                    }
                }
                NamingEnumeration<?> ne = attr.getAll();
                while (ne.hasMore()) {
                    Object value = ne.next();
                    sb.append(attr.getID()).append(": ").append(value).append("\n");
                }
            } catch (NamingException e) {
                sb.append(attr.getID()).append(": ").append("!!! Unable to print value !!!\n");
            }
            first = false;
        }
        return sb.toString();
    }

    /**
     * Parse options
     * 
     * @see org.apache.log4j.Layout#activateOptions()
     */
    @Override
    public void activateOptions() {
        /* Parse logOperations */
        operations = new HashSet<JndiModificationType>();
        if(logOperation != null) {
            /* We only add valid options */
            StringTokenizer st = new StringTokenizer(logOperation, LOG_OPERATIONS_SEPARATOR);
            String token = null;
            for (int i = 0 ; st.hasMoreTokens() ; i++) {
                token = st.nextToken().toLowerCase();
                JndiModificationType op = JndiModificationType.getFromDescription(token);
                if(op != null) {
                    operations.add(op);
                } else {
                    LOGGER.error("Invalid operation in the LDIF logger (" + token + ")");                    
                }
            }
        } else { 
            /* Add all the operations */
            JndiModificationType[] values = JndiModificationType.values();
            for (int i = 0; i < values.length; i++) {
                operations.add(values[i]);
            }
        }
    }
    
    private boolean onlyLdif = false;

    /**
     * @return the onlyLdif
     */
    public boolean isOnlyLdif() {
        return onlyLdif;
    }

    /**
     * @param onlyLdif the onlyLdif to set
     */
    public void setOnlyLdif(boolean onlyLdif) {
        this.onlyLdif = onlyLdif;
    }
    
    /**
     * @return the logOperation
     */
    public String getLogOperation() {
        return logOperation;
    }

    /**
     * @param logOperation the logOperation to set
     */
    public void setLogOperation(String logOperation) {
        this.logOperation = logOperation;
    }

}
