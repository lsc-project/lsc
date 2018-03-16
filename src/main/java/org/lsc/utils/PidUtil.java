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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Santhosh Kumar T &lt;santhosh.tekuri@gmail.com&gt;
 */
public class PidUtil {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PidUtil.class);

    public static String getPID(final String fallback) {
        // Note: may fail in some JVM implementations
        // therefore fallback has to be provided

        // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
        final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        final int index = jvmName.indexOf('@');

        if (index < 1) {
            // part before '@' empty (index = 0) / '@' not found (index = -1)
            return fallback;
        }

        try {
            return Long.toString(Long.parseLong(jvmName.substring(0, index)));
        } catch (NumberFormatException e) {
            // ignore
        }
        return fallback;
    }
    
    public static String getPID() {
        String pid = System.getProperty("pid"); // NOI18N
        try {
            if (pid == null) {
                if (System.getProperty("os.name").toLowerCase().indexOf("windows") == -1) {
                    Process p = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", "echo $$ $PPID" });
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    pump(p.getInputStream(), bout, false, true);
                    StringTokenizer stok = new StringTokenizer(bout.toString());
                    stok.nextToken(); // this is pid of the process we spanned
                    pid = stok.nextToken();
                } else {
                    pid = getPID(null);
                }
                if (pid != null) {
                    System.setProperty("pid", pid); // NOI18N
                }
            }
        } catch(IOException e) {
            LOGGER.debug("Exception: " + e.toString());
        }
        return pid;
    }

    public static void pump(InputStream in, OutputStream out, boolean closeIn, boolean closeOut) throws IOException {
        byte[] bytes = new byte[1024];
        int read;
        try {
            while ((read = in.read(bytes)) != -1)
                out.write(bytes, 0, read);
        } finally {
            if (closeIn && in != null) {
                in.close();
            }
            if (closeOut && out != null) {
                out.close();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println(getPID());
    }
}
