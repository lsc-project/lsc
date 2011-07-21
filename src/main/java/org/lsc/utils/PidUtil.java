package org.lsc.utils;

/**
 * MySwing: Advanced Swing Utilites
 * Copyright (C) 2005  Santhosh Kumar T
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;

/**
 * @author Santhosh Kumar T &lt;santhosh.tekuri@gmail.com&gt;
 */
public class PidUtil {
	
	public static String getPID() throws IOException {
		String pid = System.getProperty("pid"); // NOI18N
		if (pid == null) {
			String cmd[];
			File tempFile = null;
			if (System.getProperty("os.name").toLowerCase().indexOf("windows") == -1)
				cmd = new String[] { "/bin/sh", "-c", "echo $$ $PPID" }; // NOI18N
			else {
				// getpids.exe is taken from
				// http://www.scheibli.com/projects/getpids/index.html (GPL)
				tempFile = File.createTempFile("getpids", "exe"); // NOI18N
				// extract the embedded getpids.exe file from the jar and save
				// it to above file
				pump(PidUtil.class.getResourceAsStream("getpids.exe"), new FileOutputStream(tempFile), true, true); // NOI18N
				cmd = new String[] { tempFile.getAbsolutePath() };
			}
			if (cmd != null) {
				Process p = Runtime.getRuntime().exec(cmd);
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				pump(p.getInputStream(), bout, false, true);
				if (tempFile != null)
					tempFile.delete();

				StringTokenizer stok = new StringTokenizer(bout.toString());
				stok.nextToken(); // this is pid of the process we spanned
				pid = stok.nextToken();
				if (pid != null)
					System.setProperty("pid", pid); // NOI18N
			}
		}
		return pid;
	}

	public static void pump(InputStream in, OutputStream out, boolean closeIn,
			boolean closeOut) throws IOException {
		byte[] bytes = new byte[1024];
		int read;
		try {
			while ((read = in.read(bytes)) != -1)
				out.write(bytes, 0, read);
		} finally {
			if (closeIn)
				in.close();
			if (closeOut)
				out.close();
		}
	}

	public static void main(String[] args) throws IOException {
		System.out.println(getPID());
	}
}