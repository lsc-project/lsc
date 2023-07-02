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
package org.lsc.persistence;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lsc.configuration.DatabaseConnectionType;
import org.lsc.configuration.LscConfiguration;
import org.lsc.exception.LscConfigurationException;
import org.lsc.exception.LscServiceConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Public class to test the DAO engine loader
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class DaoConfigTest {

	private Connection con;
	private static final Logger LOGGER = LoggerFactory.getLogger(DaoConfigTest.class);

	@Before
	public void setUp() throws IOException, InstantiationException, SQLException, ClassNotFoundException, IllegalAccessException, LscConfigurationException {
		LscConfiguration.reset();
		DatabaseConnectionType pc = (DatabaseConnectionType) LscConfiguration.getConnection("src-jdbc");
		pc.setUrl("jdbc:hsqldb:file:target/hsqldb/lsc");

		Class.forName(pc.getDriver()).newInstance();
		con = DriverManager.getConnection(pc.getUrl());
	}

	@Test
	public final void testRequest() throws SQLException {
		ResultSet rs = null;

		Statement stm = con.createStatement();
		String sql = "DROP TABLE test IF EXISTS; CREATE TABLE test (id INTEGER PRIMARY KEY)";
		rs = stm.executeQuery(sql);
		while (rs.next()) {
			LOGGER.debug("Table has {} rows.", rs.getInt(1));
		}
		assertNotNull(rs);
	}

	@Test
	public final void testGetSqlMapClient() throws LscServiceConfigurationException {
		assertNotNull(DaoConfig.getSqlMapClient((DatabaseConnectionType)LscConfiguration.getConnection("src-jdbc")));
	}

	/**
	 * Close DB connection
	 */
	@After
	public final void tearDown() throws SQLException {
		con.close();
	}
}
