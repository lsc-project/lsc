/*
 ****************************************************************************
 * Ldap Synchronization Connector provid es tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 * 
 * Copyright (c) 2009-2010, LSC Project 
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
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.utils;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.clapper.util.classutil.AndClassFilter;
import org.clapper.util.classutil.ClassFilter;
import org.clapper.util.classutil.ClassFinder;
import org.clapper.util.classutil.ClassInfo;
import org.clapper.util.classutil.RegexClassFilter;
import org.clapper.util.classutil.SubclassClassFilter;

import com.thoughtworks.xstream.InitializationException;

/**
 * Helper to found classes type according to class primary type
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class ClasstypeFinder {

	private static ClasstypeFinder instance;
	
	private ClassFinder finder;

	private Map<String, List<String>> cacheExtensions;
	
	private Map<String, String> cacheEquivalence;

	static {
		instance = new ClasstypeFinder();
	}

	public static ClasstypeFinder getInstance() {
		return instance;
	}
	
	private ClasstypeFinder() {
		cacheExtensions = new HashMap<String, List<String>>(); 
		cacheEquivalence = new HashMap<String, String>();
	}

	/**
	 * Feed the class path with the required jars/dirs/base directory
	 * @param args
	 */
	public void loadClasspath(File[] args) {
		finder = new ClassFinder();
		finder.addClassPath();
		for (File arg : args) {
			finder.add(arg);
		}
	}
	
	public void setupClasspath(File libSubDirectory) {
		if(!isInitialized()) {
			if(!libSubDirectory.isDirectory() || libSubDirectory.list() == null) {
				throw new ExceptionInInitializerError("Unable to find LSC libraries in lib directory ! Please check your installation ...");
			}
			String[] jars = libSubDirectory.list(new SuffixFileFilter(".jar"));
			File[] libs = new File[1+jars.length];
			libs[0] = libSubDirectory;
			for(int i = 0; i < jars.length; i++) {
				libs[i+1] = new File(libSubDirectory, jars[i]);
			}
			loadClasspath(libs);
		}
	}

	/**
	 * Find the class extending (or implementing) the baseClass
	 * @param baseClass the parent class (or interface)
	 * @return the classes found in the loaded class path
	 */
	public synchronized Collection<String> findExtensions(Class<?> baseClass) {
		if(finder == null) {
			throw new InitializationException("Classpath not initialized !");
		}
		if(cacheExtensions.containsKey(baseClass.getName())) {
			return cacheExtensions.get(baseClass.getName());
		} else {
			List<String> classes = new ArrayList<String>();
			ClassFilter filter = new SubclassClassFilter(baseClass);
			Collection<ClassInfo> foundClasses = new ArrayList<ClassInfo>();
			
			finder.findClasses(foundClasses, filter);

			for (ClassInfo classInfo : foundClasses) {
				if(!Modifier.isAbstract(classInfo.getModifier())) {
					classes.add(classInfo.getClassName());
				}
			}
			cacheExtensions.put(baseClass.getName(), classes);
			return classes;
		}
	}

	public boolean isInitialized() {
		return finder != null;
	}

	public synchronized String findEquivalence(String simpleName, Class<?> baseClass) {
		if(finder == null) {
			throw new InitializationException("Classpath not initialized !");
		}
		String key = baseClass.getName() + "/" + simpleName; 
		if(cacheEquivalence.containsKey(key)) {
			return cacheEquivalence.get(key);
		} else {
			ClassFilter filter = new AndClassFilter(
					new RegexClassFilter(".*\\."+simpleName),
					new SubclassClassFilter(baseClass));
			
			Collection<ClassInfo> foundClasses = new ArrayList<ClassInfo>();
			
			finder.findClasses(foundClasses, filter);
	
			if(foundClasses.size() != 1) {
				return null;
			} else {
				String className = foundClasses.iterator().next().getClassName();
				cacheEquivalence.put(key, className);
				return className;
			}
		}
	}
}
