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
package org.lsc.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.lsc.Configuration;

/**
 * Public class to test the DAO engine loader
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class DaoConfigTest extends TestCase {

    private Connection con;

    private Logger LOGGER = Logger.getLogger(DaoConfigTest.class);

    /**
     * Load the DB driver and initialize the connection.final 
     */
    public final void setUp() {
        PropertyConfigurator.configure(this.getClass().getClassLoader().getResource("log4j.properties"));
    }

    /**
     * Test the JDBC connection.
     */
    public final void testConnection() {
        try {
            Properties pc = Configuration.getPropertiesFromFileInConfigDir(Configuration.DATABASE_PROPERTIES_FILENAME);
            
            /* Test loading driver */
            LOGGER.info("=> loading driver:");
            Class.forName((String) pc.get("driver")).newInstance();
            LOGGER.info("OK");

            /* Test the connection */
            LOGGER.info("=> connecting:");
            con = DriverManager.getConnection((String) pc.get("url"));
            LOGGER.info("OK");
        } catch (ClassNotFoundException y) {
            LOGGER.error("ERR: driver not found. Please check your CLASSPATH !");
        } catch (Exception x) {
            LOGGER.error(x.toString(), x);
        }
    }

    public final void testRequest() {
        ResultSet rs = null;
        try {
            if(con == null) {
                testConnection();
            }
            Statement stm = con.createStatement();
            String sql = "DROP TABLE test IF EXISTS; CREATE TABLE test (id INTEGER PRIMARY KEY)";
            rs = stm.executeQuery(sql);
            //
            while (rs.next()) {
                System.out.println("Table has " + rs.getInt(1) + " rows.");
            }
        } catch (SQLException e) {
            LOGGER.error(e.toString(), e);
        }
        assertNotNull(rs);
    }
    
    public final void testGetSqlMapClient() {
    	// this is useless but breaks the test otherwise :)
        if(con == null) {
            testConnection();
        }
    	
    	assertNotNull(DaoConfig.getSqlMapClient());
    }

    /**
     * Close DB connection
     */
    public final void tearDown() {
        try {
            con.close();
        } catch (SQLException e) {
            LOGGER.error(e.toString(), e);
        }
    }
}
