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

import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class is a Ant helper to sort lines during a copy task
 * 
 * the alphabeticOrder parameter is a simple configuration accessor to the way sorting is done
 * <pre>
 * 	<copy overwrite="false" tofile="dstfile" srcfile="lsc.properties" />
 * 		<filterchain>
 * 			<filterreader classname="org.lsc.utils.SortingFilterReader" classpathref="execution.path">
 * 		    	<param name="alphabeticOrder" value="true" />
 * 			</filterreader>
 * 		</filterchain>
 * 	</copy>
 * </pre>
 * Derived from http://www.exampledepot.com/egs/java.util.regex/LineFilter2.html
 * @author Sebastien Bahloul <seb@lsc-project.org>
 */
public class SortingFilterReader extends FilterReader {

	private boolean alphabeticOrder;

	private Iterator<String> lines;

	private BufferedReader in;

	// This variable holds the current line.
	// If null and emitNewline is false, a newline must be fetched.
	private String curLine;

	// This is the index of the first unread character in curLine.
	// If at any time curLineIx == curLine.length, curLine is set to null.
	private int curLineIx;

	// If true, the newline at the end of curLine has not been returned.
	// It would have been more convenient to append the newline
	// onto freshly fetched lines. However, that would incur another
	// allocation and copy.
	private boolean emitNewline;

	/**
	 * Default constructor
	 * @param in
	 */
	public SortingFilterReader(Reader in) {
		super(in);
		this.in = new BufferedReader(in);
	}

	/**
	 * Simple accessor to alphabeticOrder
	 * @param alphabeticOrder
	 */
	public void setOrder(boolean alphabeticOrder) {
		this.alphabeticOrder = alphabeticOrder;
	}

	/**
	 * Get next matching line
	 * @throws IOException
	 */
	private void initialize() throws IOException {
		Set<String> lines = new TreeSet<String>(new AlphabeticOrderComparator(alphabeticOrder));
		String currentLine = in.readLine();
		while (currentLine != null) {
			lines.add(currentLine);
			currentLine = in.readLine();
		}
		this.lines = lines.iterator();
	}

	@Override
	public int read(char cbuf[], int off, int len) throws IOException {
		if(lines == null) {
			initialize();
		}
		
        // Fetch new line if necessary
        if (curLine == null && !emitNewline && lines.hasNext()) {
        	getNextLine();
        }

		// Return characters from current line
		if (curLine != null) {
			int num = Math.min(len, Math.min(cbuf.length - off, curLine.length()
					- curLineIx));
			// Copy characters from curLine to cbuf
			for (int i = 0; i < num; i++) {
				cbuf[off++] = curLine.charAt(curLineIx++);
			}

			// No more characters in curLine
			if (curLineIx == curLine.length()) {
				curLine = null;

				// Is there room for the newline?
				if (num < len && off < cbuf.length) {
					cbuf[off++] = '\n';
					emitNewline = false;
					num++;
				}
			}

			// Return number of character read
			return num;
		} else if (emitNewline && len > 0) {
			// Emit just the newline
			cbuf[off] = '\n';
			emitNewline = false;
			return 1;
		} else if (len > 0) {
			// No more characters left in input reader
			return -1;
		} else {
			// Client did not ask for any characters
			return 0;
		}
	}

	/**
	 * Set the next line into the curLine variable
	 */
	private void getNextLine() {
		curLine = lines.next();
		emitNewline = true;
		curLineIx = 0;
	}

	@Override
	public boolean ready() throws IOException {
		return curLine != null || emitNewline || in.ready();
	}

	@Override
	public boolean markSupported() {
		return false;
	}
}
