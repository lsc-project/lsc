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
 *         Jonathan Clarke &lt;jon@lsc-project.org&gt;
 ****************************************************************************
 */
package org.lsc.utils;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;

/**
 * Utility class offering various methods to manipulate and search in Lists.
 * 
 * @author Jonathan Clarke &lt;jonathan@phillipoux.net&gt;
 */
public class SetUtils {

	// Utility class, no public constructor
	private SetUtils() {
	}

	/**
	 * Return a new HashSet containing all the Objects that are an Attribute's
	 * values.
	 * 
	 * @param attr
	 *            An Attribute containing values to extract.
	 * @return {@link HashSet}<Object> Values as a set. Never null.
	 * @throws NamingException
	 */
	public static Set<Object> attributeToSet(Attribute attr)
					throws NamingException {
		if (attr == null || attr.size() == 0) {
			return new HashSet<Object>();
		}

		Set<Object> attrValues = new LinkedHashSet<Object>(attr.size());
		NamingEnumeration<?> ne = attr.getAll();
		while (ne.hasMore()) {
			Object value = ne.next();

			// ignore empty string values
			if ((value instanceof String) && ((String) value).length() == 0) {
				continue;
			}

			attrValues.add(value);
		}
		return attrValues;
	}

	/**
	 * Return an Attribute containing all the Objects that are in a Set.
	 * 
	 * @param attrName
	 *            The name of the attribute to return
	 * @param values Values as a set
	 * @return Attribute An Attribute containing values from the set. Never null.
	 */
	public static Attribute setToAttribute(String attrName, Set<?> values) {
		Attribute ret = new BasicAttribute(attrName);

		if (values == null || values.size() == 0) {
			return ret;
		}

		for (Object value : values) {
			ret.add(value);
		}

		return ret;
	}

	/**
	 * Find missing needles from a haystack. In other words, identify values in
	 * the set of needles that are not in the haystack, and return them in a
	 * new Set. This method is type-aware and will intelligently compare
	 * byte[], String, etc.
	 * 
	 * @param haystack
	 *            Set of Objects to find the needles in.
	 * @param needles
	 *            Set of Objects to search for in the haystack.
	 * @return {@link Set} of needles that are not in the haystack.
	 */
	public static Set<Object> findMissingNeedles(Set<?> haystack, Set<Object> needles) {
		Set<Object> missingNeedles = new HashSet<Object>();

		// no needles? they can't be missing then.
		if (needles == null) {
			return missingNeedles;
		}

		// no haystack? all needles must be missing then.
		if (haystack == null) {
			return needles;
		}

		for (Object needle : needles) {
			ByteBuffer needleBuff = null;

			// use a byte buffer is needle is binary
			if (needle.getClass().isAssignableFrom(byte[].class)) {
				needleBuff = ByteBuffer.wrap((byte[]) needle);
			}

			boolean foundInHaystack = false;
			for (Object haystackValue : haystack) {
				ByteBuffer haystackValueBuff = null;

				// use a byte buffer if haystack value is binary
				if (haystackValue.getClass().isAssignableFrom(byte[].class)) {
					haystackValueBuff = ByteBuffer.wrap((byte[]) haystackValue);

					// make sure we have a byte buffer for the needle too
					if (needleBuff == null) {
						// if needle is binary, make this haystack value binary
						if (needle.getClass().isAssignableFrom(String.class)) {
							needleBuff = ByteBuffer.wrap(((String) needle).getBytes());
						} else {
							continue;
						}
					}
				}

				// needleBuff is set if either needle or haystack value are
				// binary
				// do a binary comparison
				if (needleBuff != null) {
					// make sure we have a byte buffer for haystack value too
					if (haystackValueBuff == null) {
						if (haystackValue.getClass().isAssignableFrom(String.class)) {
							haystackValueBuff = ByteBuffer.wrap(((String) haystackValue).getBytes());
						} else {
							continue;
						}
					}

					// binary comparison
					if (haystackValueBuff.compareTo(needleBuff) == 0) {
						foundInHaystack = true;
					}
				} else {
					// fall back to standard compare (works well for String,
					// int, boolean, etc)
					if (haystackValue.equals(needle)) {
						foundInHaystack = true;
					}
				}
			}

			if (!foundInHaystack) {
				missingNeedles.add(needle);
			}
		}

		return missingNeedles;
	}

	/**
	 * Check that two supposed identical sets are stored
	 * in the same order. This method is type-aware and will intelligently compare
	 * byte[], String, etc.
	 * 
	 * @param first
	 *            First set of Objects to compare.
	 * @param second
	 *            Second set of Objects.
	 * @return {@link boolean} if the order is the same.
	 */
	public static boolean checkOrder(Set<?> first, Set<Object> second) {
		if (first.size() != second.size()) {
			return false;
		}

		Iterator<Object> iterator = second.iterator();
		for (Object element1: first) {
			Object element2 = iterator.next(); 
			
			ByteBuffer element1Buff = null;
			
			// use a byte buffer is element1 is binary
			if (element1.getClass().isAssignableFrom(byte[].class)) {
				element1Buff = ByteBuffer.wrap((byte[]) element1);
			}
			
			ByteBuffer element2Buff = null;
			
			// use a byte buffer if element2 value is binary
			if (element2.getClass().isAssignableFrom(byte[].class)) {
				element2Buff = ByteBuffer.wrap((byte[]) element2);
				
				// make sure we have a byte buffer for element1 too
				if (element1Buff == null) {
					// if element1 is binary, make this element2 value binary
					if (element1.getClass().isAssignableFrom(String.class)) {
						element1Buff = ByteBuffer.wrap(((String) element1).getBytes());
					} else {
						return false;
					}
				}
			}
			
			// element1Buff is set if either element1 or element2 value are
			// binary
			// do a binary comparison
			if (element1Buff != null) {
				// make sure we have a byte buffer for element2 value too
				if (element2Buff == null) {
					if (element2.getClass().isAssignableFrom(String.class)) {
						element2Buff = ByteBuffer.wrap(((String) element2).getBytes());
					} else {
						return false;
					}
				}
				
				// binary comparison
				if (element2Buff.compareTo(element1Buff) != 0) {
					return false;
				}
			} else {
				// fall back to standard compare (works well for String,
				// int, boolean, etc)
				if (!element2.equals(element1)) {
					return false;
				}
			}
		}
		
		return true;
	}

	/**
	 * Check to make sure all needles are in the haystack. In other words each
	 * value from the set needles must be in the set haystack. This method is
	 * type-aware and will intelligently compare byte[], String, etc.
	 * 
	 * @param haystack
	 *            Set of Objects to find the needles in.
	 * @param needles
	 *            Set of Objects to search for in the haystack.
	 * @return true if all the members of needles are found in haystack
	 */
	public static boolean setContainsAll(Set<Object> haystack, Set<Object> needles) {
		return (findMissingNeedles(haystack, needles).size() == 0);
	}

	/**
	 * Compare two lists of values to see if they contain the same values. This
	 * method is type-aware and will intelligently compare byte[], String, etc.
	 * 
	 * @param srcAttrValues
	 * @param dstAttrValues
	 * @return true if all values of each set are present in the other set, false otherwise
	 */
	public static boolean doSetsMatch(Set<Object> srcAttrValues, Set<Object> dstAttrValues) {
		// make sure value counts are the same
		if (srcAttrValues.size() != dstAttrValues.size()) {
			return false;
		}

		// check if there are any values in srcAttr not in dstAttr
		if (!SetUtils.setContainsAll(dstAttrValues, srcAttrValues)) {
			return false;
		}

		// check if there are any values in dstAttr not in srcAttr
		if (!SetUtils.setContainsAll(srcAttrValues, dstAttrValues)) {
			return false;
		}

		// looks ok!
		return true;
	}

	/**
	 * Compare two lists of values to see if they contain the same values and in same order.
	 * This method is type-aware and will intelligently compare byte[], String, etc.
	 * 
	 * @param srcAttrValues
	 * @param dstAttrValues
	 * @return true if all values of each set are present in the other set and in the same order, false otherwise
	 */
	public static boolean doSetsMatchWithOrder(Set<Object> srcAttrValues, Set<Object> dstAttrValues) {
		if (!doSetsMatch(srcAttrValues, dstAttrValues)) {
			return false;
		}
		return checkOrder(srcAttrValues, dstAttrValues);
	}
	
	public static void addAllIfNotPresent(Set<Object> set, Set<Object> values) {
		Set<Object> valuesToAdd = findMissingNeedles(set, values);
		set.addAll(valuesToAdd);
	}
}
