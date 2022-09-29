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
package org.lsc.opendj;

import static org.opends.server.util.ServerConstants.EOL;
import static org.opends.server.util.StaticUtils.createEntry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lsc.Configuration;
import org.opends.messages.Message;
import org.opends.server.api.Backend;
import org.opends.server.backends.MemoryBackend;
import org.opends.server.core.AddOperation;
import org.opends.server.core.DirectoryServer;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.types.DN;
import org.opends.server.types.DirectoryEnvironmentConfig;
import org.opends.server.types.Entry;
import org.opends.server.types.InitializationException;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.types.ResultCode;
import org.opends.server.util.LDIFException;
import org.opends.server.util.LDIFReader;

/**
 * Embedded OpenDJ directory
 */
public final class EmbeddedOpenDJ {

    /** Tool class */
    private EmbeddedOpenDJ() {}

    /**
     * A logger for the class
     */
    private static Log logger = LogFactory.getLog(EmbeddedOpenDJ.class);

    /**
     * The of the system property that specifies the target working directory, where OpenDJ do its business (db datas,
     * locks, logs, etc)
     */
    public static final String PROPERTY_WORKING_DIR = "org.opends.server.workingDir";

    /**
     * A default value for the working dir
     */
    public static final String DEFAULT_WORKING_DIR_NAME = "opendj-test";

    public static boolean SERVER_STARTED = false;

    private static final String CONFIG_DIR = "config";

    /**
     * The memory-based backend configured for use in the server.
     */
    private static MemoryBackend memoryBackend = null;

    /**
     * Initialize the server. We completely override the super class server set up.
     * @throws IOException 
     * @throws InitializationException 
     * @throws URISyntaxException 
     */
    public static void startServer() throws IOException, InitializationException, URISyntaxException {
        if (SERVER_STARTED) {
            return;
        }

        String configClass = "org.opends.server.extensions.ConfigFileHandler";

        String conf = getPathToConfigFile(CONFIG_DIR);
        if (conf == null || "".equals(conf)) {
        	throw new RuntimeException("The config directory template " + conf + "(from " + CONFIG_DIR + ") has not been found");
        }

        String workingDirectory = System.getProperty(PROPERTY_WORKING_DIR);
        if (null == workingDirectory) {
        	String tempDir = System.getProperty("java.io.tmpdir");
        	if ( !(tempDir.endsWith("/") || tempDir.endsWith("\\")) ) {
        	   tempDir = tempDir + System.getProperty("file.separator");
        	}
        	
        	workingDirectory = tempDir + DEFAULT_WORKING_DIR_NAME;
        }
        // copy to r/w location and init directory structure
        initOpenDJDirectory(conf, workingDirectory);

        DirectoryServer directoryServer = DirectoryServer.getInstance();
        directoryServer.bootstrapServer();
        try {
            directoryServer.initializeConfiguration(configClass, new File(new File(workingDirectory, CONFIG_DIR),
            "config.ldif").getAbsolutePath());
            DirectoryEnvironmentConfig dec = new DirectoryEnvironmentConfig();
            dec.setLockDirectory(new File(workingDirectory, "locks"));
            dec.setSchemaDirectory(new File(new File(workingDirectory, CONFIG_DIR), "schema"));
            directoryServer.setEnvironmentConfig(dec);
        } catch (Exception e) {
            throw new RuntimeException("Error when intializing the server: " + e, e);
        }

        try {
            directoryServer.startServer();
        } catch (Exception e) {
            throw new RuntimeException("Error when starting the server: " + e, e);
        }
        SERVER_STARTED = true;

        if (logger.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer("");
            for (Backend b : DirectoryServer.getBackends().values()) {
                sb.append("[ ").append(b.getBackendID()).append(" => ");
                DN[] dns = b.getBaseDNs();
                for (int i = 0; i < dns.length; i++) {
                    sb.append("(").append(dns[i].toNormalizedString()).append(")");
                }
                sb.append(" ]");
            }
            logger.debug(sb.toString());
        }
        SERVER_STARTED = true;
    }

    /**
     * Sets the system context root to null.
     * @param reason Message to log when shutting down
     */
    public static void shutdownServer(String reason) {
        if (SERVER_STARTED) {
            DirectoryServer.shutDown("org.lsc.opends.EmbeddedOpenDS", Message.fromObject(null, reason));
            SERVER_STARTED = false;
        }
    }

    /**
     * Test main class
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            startServer();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Server started");
        // ok, stop
        try {
            shutdownServer("Stop required");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Server stoped");
    }

    /**
     * Create the directory layer mandatory to OpenDJ
     * 
     * All directory and files has to be read/write, because the server
     * save, update, locks and do other stuff here. so, we need to have an initial config directory, wich contains at
     * least : 
     * - config/config.ldif : root config file for OpenDJ
     * - config/schemas/ : standard schema for the directory 
     * - config/upgrade/config.ldif.1834 and config/upgrade/schema.ldif.1834 : heu... mandatory :) 
     * These files are provided if you download a standard distrib of OpenDJ.
     * 
     * Moreover, we have to create these directories : 
     * - db/ : Directory for Berkeley BD JE data storage 
     * - logs/ : here go logs
     * - locks/ : here go locks
     * 
     * @param copyFromConfigDirectory : the source directory which contains config.ldif, schemas, upgrade
     * @param targetRootDirectory the target directory which will be used as openDJ root directory (create if non
     *            existing)
     * @throws IOException
     */
    private static void initOpenDJDirectory(String copyFromConfigDirectory, String targetRootDirectory)
    throws IOException {

        File workingDirectory = new File(targetRootDirectory);
        // delete recursively workingDirectory
        if (workingDirectory.exists()) {
            FileUtils.deleteDirectory(workingDirectory);
        }
        if (workingDirectory.exists()) {
            throw new IOException("Failed to delete: " + workingDirectory);
        }

        if (!workingDirectory.mkdirs()) {
            throw new IOException("Failed to create:" + workingDirectory);
        }

        // copy config schema
        FileUtils.copyDirectory(new File(copyFromConfigDirectory), new File(targetRootDirectory, CONFIG_DIR));

        // create missing directories
        // db backend, logs, locks
        String[] subDirectories = { "changelogDb", "classes", "db", "ldif", "locks", "logs" };
        for (String s : subDirectories) {
            new File(workingDirectory, s).mkdir();
        }
    }

    /**
     * Returns a modifiable List of entries parsed from the provided LDIF. It's best to call this after the server has
     * been initialized so that schema checking happens.
     * <p>
     * Also take a look at the makeLdif method below since this makes expressing LDIF a little bit cleaner.
     * 
     * @param ldif of the entries to parse.
     * @return a List of EntryS parsed from the ldif string.
     * @throws IOException 
     * @throws LDIFException 
     * @see #makeLdif
     */
    public static List<Entry> entriesFromLdifString(String ldif) throws IOException, LDIFException {
        LDIFImportConfig ldifImportConfig = new LDIFImportConfig(new StringReader(ldif));
        LDIFReader reader = new LDIFReader(ldifImportConfig);

        List<Entry> entries = new ArrayList<Entry>();
        Entry entry;
        while ((entry = reader.readEntry()) != null) {
            entries.add(entry);
        }

        return entries;
    }

    /**
     * This is used as a convenience when and LDIF string only includes a single entry. It's best to call this after the
     * server has been initialized so that schema checking happens.
     * <p>
     * Also take a look at the makeLdif method below since this makes expressing LDIF a little bit cleaner.
     * 
     * @param ldif Line of LDIF
     * @return the first Entry parsed from the ldif String
     * @see #makeLdif
     * @throws Exception If an unexpected problem occurs.
     */
    public static Entry entryFromLdifString(String ldif) throws Exception {
        return entriesFromLdifString(ldif).get(0);
    }

    /**
     * This method provides the minor convenience of not having to specify the newline character at the end of every
     * line of LDIF in test code. This is an admittedly small advantage, but it does make things a little easier and
     * less error prone. For example, this
     * 
     * <pre>
     * private static final String JOHN_SMITH_LDIF = TestCaseUtils.makeLdif(&quot;dn: cn=John Smith,dc=example,dc=com&quot;,
     * 		&quot;objectclass: inetorgperson&quot;, &quot;cn: John Smith&quot;, &quot;sn: Smith&quot;, &quot;givenname: John&quot;);
     * 
     * </pre>
     * 
     * is a <bold>little</bold> easier to work with than
     * 
     * <pre>
     * private static final String JOHN_SMITH_LDIF = &quot;dn: cn=John Smith,dc=example,dc=com\n&quot; + &quot;objectclass: inetorgperson\n&quot;
     * 		+ &quot;cn: John Smith\n&quot; + &quot;sn: Smith\n&quot; + &quot;givenname: John\n&quot;;
     * 
     * </pre>
     * @param lines Lines of LDIF without newline characters
     * 
     * @return the concatenation of each line followed by a newline character
     */
    public static String makeLdif(String... lines) {
        StringBuilder buffer = new StringBuilder();
        for (String line : lines) {
            buffer.append(line).append(EOL);
        }
        // Append an extra line so we can append LDIF Strings.
        buffer.append(EOL);
        return buffer.toString();
    }

    /**
     * This is a convience method that constructs an Entry from the specified lines of LDIF. Here's a sample usage
     * 
     * <pre>
     * Entry john = TestCaseUtils.makeEntry(&quot;dn: cn=John Smith,dc=example,dc=com&quot;, &quot;objectclass: inetorgperson&quot;,
     * 		&quot;cn: John Smith&quot;, &quot;sn: Smith&quot;, &quot;givenname: John&quot;);
     * </pre>
     * @param lines Lines of LDIF
     * @return Entry constructed from the specified lines of LDIF
     * @throws Exception 
     * 
     * @see #makeLdif
     */
    public static Entry makeEntry(String... lines) throws Exception {
        return entryFromLdifString(makeLdif(lines));
    }

    /**
     * This is a convience method that constructs an List of EntryS from the specified lines of LDIF. Here's a sample
     * usage
     * 
     * <pre>
     * List&lt;Entry&gt; smiths = TestCaseUtils.makeEntries(&quot;dn: cn=John Smith,dc=example,dc=com&quot;, &quot;objectclass: inetorgperson&quot;,
     * 		&quot;cn: John Smith&quot;, &quot;sn: Smith&quot;, &quot;givenname: John&quot;, &quot;&quot;, &quot;dn: cn=Jane Smith,dc=example,dc=com&quot;,
     * 		&quot;objectclass: inetorgperson&quot;, &quot;cn: Jane Smith&quot;, &quot;sn: Smith&quot;, &quot;givenname: Jane&quot;);
     * </pre>
     * @param lines Lines of LDIF
     * @return List of EntryS constructed from the specified lines of LDIF
     * @throws IOException 
     * @throws LDIFException 
     * 
     * @see #makeLdif
     */
    public static List<Entry> makeEntries(String... lines) throws LDIFException, IOException {
        return entriesFromLdifString(makeLdif(lines));
    }

    /**
     * Adds the provided entry to the Directory Server using an internal operation.
     * 
     * @param entry The entry to be added.
     * @return the error code
     */
    public static ResultCode addEntry(Entry entry) {
        InternalClientConnection conn = InternalClientConnection.getRootConnection();

        AddOperation addOperation = conn.processAdd(entry.getDN(), entry.getObjectClasses(), entry.getUserAttributes(),
                entry.getOperationalAttributes());
        return addOperation.getResultCode();
    }

    /**
     * Adds the provided set of entries to the Directory Server using internal operations.
     * 
     * @param entries The entries to be added.
     */
    public static void addEntries(List<Entry> entries) {
        for (Entry entry : entries) {
            addEntry(entry);
        }
    }

    /**
     * Adds the provided set of entries to the Directory Server using internal operations.
     * 
     * @param lines The lines defining the entries to add. If there are multiple entries, then they should be separated
     *            by blank lines.
     * @throws IOException 
     * @throws LDIFException 
     */
    public static void addEntries(String... lines) throws LDIFException, IOException {
        for (Entry entry : makeEntries(lines)) {
            addEntry(entry);
        }
    }

    // /**
    // * Retrieve the back-end used for testing pupose
    // * @return
    // */
    // public static Backend getTestBackend() {
    // return DirectoryServer.getBackend(TEST_BACKEND);
    // }
    //
    public static void initializeTestBackend(boolean createBaseEntry, String dn) throws Exception {

        DN baseDN = DN.decode(dn);
        if (memoryBackend == null) {
            memoryBackend = new MemoryBackend();
            memoryBackend.setBackendID("test");
            memoryBackend.setBaseDNs(new DN[] { baseDN });
            memoryBackend.initializeBackend();
            DirectoryServer.registerBackend(memoryBackend);
        }

        memoryBackend.clearMemoryBackend();

        if (createBaseEntry) {
            Entry e = createEntry(baseDN);
            memoryBackend.addEntry(e, null);
        }
    }

    // public static void clearBackend(boolean createBaseEntry) {
    // MemoryBackend memoryBackend = (MemoryBackend)DirectoryServer.getBackend(TEST_BACKEND);
    // memoryBackend.clearMemoryBackend();
    // }

    public static void importLdif(String ldif) throws IOException, LDIFException {
        BufferedReader br = new BufferedReader(new FileReader(ldif));
        StringBuffer sb = new StringBuffer("");
        String line = null;
        while (null != (line = br.readLine())) {
            sb.append(line).append("\n");
        }
        addEntries(sb.toString());
        br.close();
    }
    
    /**
     * <p>Search for a file in the possible configuration locations:
     * <ul><li>As a resource on the classpath (historical method)</li>
     * <li>As a file in the configuration directory</li></ul></P>
     * 
     * <P>This method first looks on the classpath, then if that fails,
     * looks in the configuration directory. Finally, it returns the
     * absolute path name to the file.</P>.
     * 
     * @param fileName Name of the file or directory to locate.
     * @return Absolute path name to the file, or null.
     */
    public static String getPathToConfigFile(String fileName) {
    	String res = null;
        
        try {
            URL configUrl = EmbeddedOpenDJ.class.getResource(fileName);
	        if (configUrl != null) {
				res = configUrl.toURI().getPath();
			}
        } catch (URISyntaxException e) {
        	// ignore, errors are handled below, this was just a first try
		}
        
        if (res == null || "".equals(res)) {
            // try getting the OpenDJ config directory from the main configuration directory
        	res = new File(Configuration.getConfigurationDirectory() + fileName).getAbsolutePath();
        }
        
        // return whatever we have, even if it's just null
        return res;

    }

}
