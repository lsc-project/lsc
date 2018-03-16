/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
		String test = "Ç'ést lä lùttè fînâÀàlÉ";
		assertEquals("C'est la lutte finaAalE", FrenchFilters.removeBadChars(test));	
	}

	@Test
	public void testRemoveBadCharsInNames() {
		String test = "Clément Niña Aÿla Stãphane";
		assertEquals("Clement Nina Ayla Staphane", FrenchFilters.removeBadChars(test));
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
		assertEquals("Jean-Francois Test", FrenchFilters.toUpperCaseAllBeginningNames("Jean-Francois Test"));
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
