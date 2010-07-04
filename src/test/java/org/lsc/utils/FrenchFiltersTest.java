/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 * 
 * Copyright (c) 2010, LSC Project 
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
 *               (c) 2008 - 2010 LSC Project
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author rschermesser
 */
public class FrenchFiltersTest {

	@Test
	public void testRemoveBadChars() {
		String test = "Ç'ést lä lùttè fînâlÉ";
		assertEquals("C'est la lutte finalE", FrenchFilters.removeBadChars(test));	
	}

	@Test
	public void testFilterPhone() {
		String phone = "01 02 03 04 05";
		assertEquals("33102030405", FrenchFilters.filterPhone(phone));

		phone = "12 3 4 56 78 9-0";
		assertEquals("33234567890", FrenchFilters.filterPhone(phone));
	}

	@Test
	public void testToUpperCaseAllBeginningNames() {
		String name = "toto titi-tata.tutu_tyty";
		assertEquals("Toto Titi-Tata.tutu_Tyty", FrenchFilters.toUpperCaseAllBeginningNames(name));
	}

	@Test
	public void testFilterSn() throws CharacterUnacceptedException {
		String sn = "Me MySelf And I";
		assertEquals("Me Myself And I", FrenchFilters.filterSn(sn));
	}

	@Test(expected=CharacterUnacceptedException.class)
	public void testFilterSnException() throws Exception {
		String sn = "Me MySelf §!°^¨$*€`£ù%+=:/;,?# I";
		FrenchFilters.filterSn(sn);
	}

	@Test
	public void testFilterUid() {
		String uid = "ABCDEFGHIJKLMNOPQ";
		assertEquals("abcdefghijklmn", FrenchFilters.filterUid(uid));
	}

	@Test
	public void testFilterShortUid() {
		String uid = "ABCDEFGHIJKLMNOPQ";
		assertEquals("abcdefgh", FrenchFilters.filterShortUid(uid));
	}

	@Test
	public void testFilterLengthString() {
		assertEquals("identifier", FrenchFilters.filterLengthString("identifier", 12));
		assertEquals("ident", FrenchFilters.filterLengthString("identifier", 5));
		assertEquals("ident", FrenchFilters.filterLengthString("id-entifier", 5));
	}

//	public static String filterStringRegExp(final String value, final String regexp)
//					throws CharacterUnacceptedException {

	/**
	 * Filter a string to match a last name
	 *
	 * @param name the last name to filter
	 * @return the filtered patronimic name
	 * @throws CharacterUnacceptedException thrown if an rejected character
	 * is encountered during analysis
	 */
//	public static String filterLastName(final String name)
//					throws CharacterUnacceptedException {

	/**
	 * Filter a string to match a first name
	 *
	 * @param name the first name to filter
	 * @return the filtered public given name
	 * @throws CharacterUnacceptedException thrown if an rejected character
	 * is encountered during analysis
	 */
//	public static String filterFirstName(final String name)
//					throws CharacterUnacceptedException {

	/**
	 * Filter a string to match a givenName
	 *
	 * @param oldValue the value to filter
	 * @return the filtered givenname
	 * @throws CharacterUnacceptedException thrown if an rejected character
	 * is encountered during analysis
	 */
//	public static String filterGivenName(final String oldValue)
//					throws CharacterUnacceptedException {
	
	/**
	 * Filter all alphanumeric characters.
	 * @param value the original value
	 * @return the filtered string
	 * @throws CharacterUnacceptedException thrown if an rejected character
	 * is encountered during analysis
	 */
//	public static String filterAlpha(final String value)
//					throws CharacterUnacceptedException {

	/**
	 * Generate a 8 chars long password
	 *
	 * @return Le mot de passe
	 */
//	public static String generatePwd() {

	/**
	 * Remove trailing and starting spaces
	 * and replace remaining spaces and dots by dashes
	 *
	 * @param aString the string to filter
	 *
	 * @return the filtered string
	 */
//	public static String filterName(final String aString) {
	
	/**
	 * Filters numerical identifier.
	 *
	 * @param value the string
	 * @return the normalized String
	 * @throws CharacterUnacceptedException launch if and only if the argument
	 *         is not a numerical identifier
	 */
//	public static String filterNumber(final String value)
//					throws CharacterUnacceptedException {
		

	/**
	 * Converts Date into timestamp string.
	 *
	 * @param value A string representation fo a date
	 * @param format The format of Date with representation used by
	 *        SimpleDateFormat
	 *
	 * @return String A string containing correspondant timestamp
	 *
	 * @throws CharacterUnacceptedException thrown if an rejected character
	 * is encountered during analysis
	 */
//	public static String filterDate(final String value, final String format)
//					throws CharacterUnacceptedException {
}
