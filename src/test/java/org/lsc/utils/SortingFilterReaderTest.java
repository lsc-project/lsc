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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import junit.framework.TestCase;

public class SortingFilterReaderTest extends TestCase {

	private File file;

	private String FILE_CONTENT = "a\nc\nb\n";
	private String SORTED_FILE_CONTENT = "a\nb\nc\n";
	private String RSORTED_FILE_CONTENT = "c\nb\na\n";
	
	public SortingFilterReaderTest() {
		file = new File("test-" + this.getName());
	}
	
	public void setUp() throws FileNotFoundException, IOException {
		// Generate a simple file
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(FILE_CONTENT.getBytes());
		fos.close();		
	}

	public void tearDown() throws FileNotFoundException, IOException {
		file.delete();
	}

	public void testAlphabetic() throws IOException {
		SortingFilterReader sfr = new SortingFilterReader(new FileReader(file));
		sfr.setOrder(true);
		char[] arr = sortContent(sfr);
		assertEquals(new String(arr), SORTED_FILE_CONTENT);
	}

	public void testRAlphabetic() throws IOException {
		SortingFilterReader sfr = new SortingFilterReader(new FileReader(file));
		sfr.setOrder(false);
		char[] arr = sortContent(sfr);
		assertEquals(new String(arr), RSORTED_FILE_CONTENT);
	}

	private char[] sortContent(SortingFilterReader asf) throws IOException {
		char[] arr = new char[(int) file.length()];
		int pos = 0;
		int size = (int) file.length();
		int len = 0;
		while(len != -1 && size > 0) {
			len = asf.read(arr, pos, size);
			size -= len;
			pos += len;
		}
		return arr;
	}
}
