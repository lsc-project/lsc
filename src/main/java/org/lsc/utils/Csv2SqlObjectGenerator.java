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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;

import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lsc.AbstractGenerator;


/**
 * To generate the right skeleton, this class dump the csv2sql
 * configuration file.
 *
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class Csv2SqlObjectGenerator extends AbstractGenerator {
    /** This is the maximum first line length read in the CSV file. */
    private static final int MAX_LINE_LENGTH = 3000;

    /** This is the local LOG4J logger. */
    private static final Logger LOGGER = 
	LoggerFactory.getLogger(Csv2SqlObjectGenerator.class);

    /** This is the CSV filename. */
    private String csvFilename;

    /** This is the CSV file separator. */
    private String csvSeparator;

    /**
     * Run the whole CSV2SQL generator.
     *
     * @param className the classname related generating operation
     * @param destination the generated file destination directory
     * @param csvFile the csv filename
     * @param csvSeparator the csv fields separator
     *
     * @throws NamingException thrown if an directory exception is encountered
     *         while generating the new bean
     */
    public static final void run(final String className,
                                 final String destination,
                                 final String csvFile,
                                 final String csvSeparator)
                          throws NamingException {
        Csv2SqlObjectGenerator csog = new Csv2SqlObjectGenerator();
        csog.init(csvFile, csvSeparator);
        csog.setDestination(destination);
        csog.generate(className);
    }

    /**
     * Initialize the generator fields.
     *
     * @param lcsvFile the csv filename
     * @param lcsvSeparator the csv fields separator
     */
    private void init(final String lcsvFile, final String lcsvSeparator) {
        csvFilename = lcsvFile;
        csvSeparator = lcsvSeparator;
        setPackageName(getGenericPackageName());
    }

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
        InputStream is;

        try {
            is = new FileInputStream(new File(csvFilename));
        } catch (FileNotFoundException e) {
            LOGGER.fatal(I18n.getMessage(null,
                             "org.lsc.messages.SYNC_ERROR",
                             new Object[] { e.getMessage() }), e);

            return false;
        }

        byte[] bytes = new byte[MAX_LINE_LENGTH];

        try {
            is.read(bytes);
        } catch (IOException e) {
            LOGGER.fatal(I18n.getMessage(null,
                             "org.lsc.messages.SYNC_ERROR",
                             new Object[] { e.getMessage() }), e);

            return false;
        }

        String[] lines = new String(bytes).split("\n");
        StringTokenizer sTok = new StringTokenizer(lines[0], csvSeparator);

        if (sTok.countTokens() <= 0) {
            return false;
        }

        String[] cols = new String[sTok.countTokens()];

        for (int i = 0; sTok.hasMoreTokens();) {
            cols[i++] = sTok.nextToken();
        }

        writeContent(generateXml(className, cols));

        return true;
    }

    /**
     * Generate the XML content.
     *
     * @param className the related class name
     * @param cols columns array
     *
     * @return the XML generated content
     */
    private String generateXml(final String className, final String[] cols) {
        StringBuffer sb = new StringBuffer();
        sb.append(getXmlStartStructure());

        for (int i = 0; i < cols.length; i++) {
            if (cols[i].startsWith("\"") && cols[i].endsWith("\"")) {
                cols[i] = cols[i].substring(1, cols[i].length() - 1);
            }

            sb.append("\t\t\t<field name=\"").append(cols[i])
              .append("\" type=\"VARCHAR\"/>\n");
        }

        return sb.append(getXmlEndStructure()).toString();
    }

    /**
     * Generate the XML start content.
     *
     * @return the XML start content
     */
    private String getXmlStartStructure() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
               + "<root>\n\t<!-- CONFIGURATOR DATA -->\n"
               + "\t<descriptor version=\"3\"/>\n"
               + "\t\t<!-- DEFINE HERE TABLE STRUCTURE -->\n"
               + "\t\t<structure tablename=\"" + getClassName() + "\">\n";
    }

    /**
     * Generate the XML start content.
     *
     * @return the XML start content
     */
    private String getXmlEndStructure() {
        String utf8CsvFilename = 
            csvFilename.substring(csvFilename.lastIndexOf(getSeparator()) + 1,
                                  csvFilename.lastIndexOf(".")) + "-utf8.csv";

        return "\t\t</structure>\n\n" + "\t\t<!-- WHAT GRAMMAR USE -->\n"
               + "\t\t<grammar "
               + "class=\"net.sf.csv2sql.grammars.standard.GrammarFactory\"/>"
               + "\n\n\t\t<!-- TEMPORARY STORAGE -->\n"
               + "\t\t<storage class=\"net.sf.csv2sql.storage.Memory\"/>\n\n"
               + "\t\t<!-- RENDERER CONFIGURATION -->\n"
               + "\t\t<render "
               + "class=\"net.sf.csv2sql.renders.SqlInsertRenderer\">\n\n"
               + "\t\t<param name=\"inputfile\" value=\"" + utf8CsvFilename
               + "\"/>\n" + "\t\t<param name=\"separator\" value=\""
               + csvSeparator + "\"/>\n" + "\t\t<!--optional-->\n"
               + "\t\t<param name=\"trimdata\"           value=\"true\"/>\n"
               + "\t\t<param name=\"suppressheader\"     value=\"true\"/>\n"
               + "\t\t<param name=\"removedoublequotes\" value=\"true\"/>\n"
               + "\t</render>\n\n" + "\t<!-- WRITER CONFIGURATION -->\n"
               + "\t<output>\n\n" + "\t\t<writerAppender active=\"true\" "
               + "class=\"net.sf.csv2sql.writers.JdbcWriter\">\n"
               + "\t\t<param name=\"driver\"   "
               + "value=\"org.hsqldb.jdbcDriver\"/>\n"
               + "\t\t<param name=\"url\"      "
               + "value=\"jdbc:hsqldb:file:target/hsqldb/lsc\"/>\n"
               + "\t\t<param name=\"username\" " + "value=\"sa\"/>\n"
               + "\t\t<param name=\"password\" " + "value=\"\"/>\n\n"
               + "\t\t<!--optional-->\n"
               + "\t\t<!--if false commit every statement.-->\n"
               + "\t\t<param name=\"commit\"   value=\"false\"/>\n"
               + "\t\t<param name=\"commitbatchcount\"   value=\"0\"/>\n"
               + "\t\t<!-- decomment to select path of "
               + "jdbcdriver (only if is out of classpath)-->\n"
               + "\t\t<!-- <param name=\"jdbcjar\"  "
               + "value=\"sample/myJdbcDriver.jar\"/> -->\n"
               + "\t</writerAppender>\n\n" + "\t</output>\n" + "</root>\n";
    }

    /**
     * Unused.
     * @return the content
     */
    @Override
    protected final String generateContent() {
        throw new RuntimeException("Must never be there !");
    }

    /**
     * Unused.
     * @return the generic package name
     */
    @Override
    public final String getGenericPackageName() {
        return "";
    }

    /**
     * Return the Csv2Sql XML filename.
     * @return the csv2sql "xml" filename
     */
    public final String getFileName() {
        File csvFileObj = new File(csvFilename);

        return csvFileObj.getParent() + getSeparator()
               + csvFileObj.getName()
                           .substring(0, csvFileObj.getName().lastIndexOf("."))
               + ".xml";
    }
}
