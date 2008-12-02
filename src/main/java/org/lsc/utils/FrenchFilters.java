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


import java.text.ParsePosition;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Random;


/**
 * Manage all common string manipulation.
 *
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public final class FrenchFilters {
    /** La regExp sur les caract&egrave;res autoris&eacute;s. */
    private static final String REGEXP_CHARACTERS = 
	"[\\p{Alpha}\\s'\"áÁ&agrave;&agrave;âÂäÄ&eacute;&eacute;&egrave;"
	 + "&egrave;êÊëËÌìÍíîÎïÏÒòÓóôÔöÖùÙÚúûÛüÜÝýç-]+";

    /** Tableau de RegExp pour les accesnts et c&eacute;dilles !. */
    private static final String[] REGEXP_ACCENTS_CEDILLES = {
                                                                "À", "á", "Á",
                                                                "&agrave;",
                                                                "&agrave;", "â",
                                                                "Â", "ä", "Ä",
                                                                "&eacute;",
                                                                "&eacute;",
                                                                "&egrave;",
                                                                "&egrave;", "ê",
                                                                "Ê", "ë", "Ë",
                                                                "È", "É", "é",
                                                                "è", "Ì", "ì",
                                                                "Í", "í", "î",
                                                                "Î", "ï", "Ï",
                                                                "Ò", "ò", "Ó",
                                                                "ó", "ô", "Ô",
                                                                "ö", "Ö", "ù",
                                                                "Ù", "Ú", "ú",
                                                                "û", "Û", "ü",
                                                                "Ü", "Ý", "ý",
                                                                "ç"
                                                            };

    /**
     * Tableau des caract&egrave;res de correspondance pour remplacer
     * le tableau pr&eacute;c&eacute;dent.
     */
    private static final String[] REGEXP_STRING_ACCENTS_CEDILLES = {
                                                                       "A", "a", "A",
                                                                       "a", "A",
                                                                       "a", "A",
                                                                       "a", "A",
                                                                       "e", "E",
                                                                       "e", "E",
                                                                       "e", "E",
                                                                       "e", "E",
                                                                       "E", "E",
                                                                       "e", "e",
                                                                       "I", "i",
                                                                       "I", "i",
                                                                       "i", "I",
                                                                       "i", "I",
                                                                       "O", "o",
                                                                       "O", "o",
                                                                       "o", "O",
                                                                       "o", "O",
                                                                       "u", "U",
                                                                       "U", "u",
                                                                       "u", "U",
                                                                       "u", "U",
                                                                       "Y", "y",
                                                                       "c"
                                                                   };

    /** Caract&egrave;res autoris&eacute;s pour l'espace de mots. */
    private static final String[] SEPARATORS_FOR_UPPER_BEGINNING_NAME = {
                                                                            " ",
                                                                            "'",
                                                                            "\"",
                                                                            "-",
                                                                            "_"
                                                                        };

    /**
     * Caract&egrave;res non autoris&eacute;s pour l'espace de mots
     * dans une adresse Email.
     */
    public static final String[] BAD_SEPARATOR_FOR_EMAIL = { " ", "'", "\"" };

    /**
     * Caracteres autoris&eacute;s pour l'espace des mots ds une
     * adresse Email.
     */
    public static final String[] GOOD_SEPARATOR_FOR_EMAIL = { "_", "_", "_" };

    /**
     * Caracteres &agrave; remplacer pour les num&eacute;ros de
     * t&eacute;l&eacute;phone.
     */
    public static final String[] BAD_SEPARATOR_FOR_PHONE = {
                                                               "-", " ", "\\.",
                                                               "/", "\\+",
                                                               "\\(", "\\)",
                                                               ";", ":", "_",
                                                               ","
                                                           };

    /**
     * Caracteres de remplacement des num&eacute;ros de
     * t&eacute;l&eacute;phone.
     */
    public static final String[] GOOD_SEPARATOR_FOR_PHONE = {
                                                                "", "", "", "",
                                                                "", "", "", "",
                                                                "", "", ""
                                                            };

    /** Expression r&eacute;guli&egrave; pour le formatage du pr&eacute;nom */
    private static final String REGEXP_FOR_FISRTNAME = 
	"[\\p{Alpha}áÁ&agrave;&agrave;âÂäÄ&eacute;&eacute;&egrave;&egrave;" +
	"êÊëËÌìÍíîÎïÏÒòÓóôÔöÖùÙÚúûÛüÜÝýç' -]+";

    /** Expression r&eacute;guli&egrave; pour le formatage du nom ! */
    private static final String REGEXP_FOR_LASTNAME = 
	"[\\p{Alpha}áÁ&agrave;&agrave;âÂäÄ&eacute;&eacute;&egrave;&egrave;" +
	"êÊëËÌìÍíîÎïÏÒòÓóôÔöÖùÙÚúûÛüÜÝýç'\"\\s -_]+";

    /**
     * Caract&egrave;res non autoris&eacute;s pour l'espace de mots
     * dans les identifiants.
     */
    private static final String[] BAD_SEPARATOR_FOR_ID = 
    	{ " ", "'", "\"", "-" };

    /**
     * Caract&egrave;res autoris&eacute;s pour l'espace des mots dans
     * les identifiants
     */

    // private static final String[] GOOD_SEPARATOR_FOR_ID = {"","","",""};
    /**
     * Liste des caract&egrave;res autoris&eacute;s pour les mots de
     * passe (pas de O,0 et I,1,l et .).
     */
    private static final String GOOD_PASSWORD = 
	"abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789/";

    /** Authorized characters for numerical identifier. */
    private static final String REGEXP_FOR_NUMERICAL_ID = "-?[0123456789]+";

    /** Authorized characters for numerical identifier. */
    private static final String REGEXP_FOR_ALPHA_NUMERICAL_ID = 
	"[\\p{Alpha}0123456789]+";

/** Tool class. */
    private FrenchFilters() {
    }

    /**
     * M&eacute;thode permettant de savoir si une chaine est contenue
     * dans un tableau de chaines.
     *
     * @param tab le tablean
     * @param name la chaine
     *
     * @return oui ou non
     */
    public static boolean containsInTab(final String[] tab, final String name) {
        boolean trouve = false;
        int i = 0;

        while ((i < tab.length) && (!trouve)) {
            if (tab[i].equals(name)) {
                trouve = true;
            }

            i++;
        }

        return trouve;
    }

    /**
     * Normalise les caract&egrave;res accentu&eacute;s divers et
     * autres c&eacute;dilles.
     *
     * @param src la chaine d'origine
     *
     * @return la chaine filtr&eacute;e
     */
    public static String removeBadChars(final String src) {
        // Suppression de tous les accents-c&eacute;dilles!
        return filterRegexp(src, REGEXP_ACCENTS_CEDILLES,
                            REGEXP_STRING_ACCENTS_CEDILLES);
    }

    /**
     * Filtre la chaîne en enlevant les caract&egrave;res de srcRegexp
     * pour les remplacer par ceux de destRegexp.
     *
     * @param src
     * @param srcRegexp
     * @param destRegexp
     *
     * @return the filered string
     */
    public static String filterRegexp(final String src,
                                      final String[] srcRegexp,
                                      final String[] destRegexp) {
        String dest = src;

        for (int i = 0; i < srcRegexp.length; i++) {
            dest = dest.replaceAll(srcRegexp[i], destRegexp[i]);
        }

        return dest;
    }

    /**
     * Methode permettant de supprimer toutes les occurrences d'une
     * chaine de caract&egrave;res dans une chaine.
     *
     * @param charactere
     * @param chaine
     *
     * @return
     */
    private static String filterDelStringIntoString(final String charactere,
                                                    final String chaine) {
        String returned = "";
        String tmp = chaine;
        int i = tmp.indexOf(charactere);

        while ((i != -1) && (i < chaine.length())) {
            returned += tmp.substring(0, i);
            tmp = tmp.substring(i + 1, tmp.length());
            i = tmp.indexOf(charactere);
        }

        if (tmp.length() > 0) {
            returned += tmp;
        }

        return returned;
    }

    /**
     * Filtre pour transformer les t&eacute;l&eacute;phones en
     * num&eacute;ro internationnal!
     *
     * @param phone2parse
     *
     * @return the filtered phone number
     */
    public static String filterPhones(final String phone2parse) {
        // On supprime les espaces, les points et les tirets
        String phoneResult = filterRegexp(phone2parse,
                                          BAD_SEPARATOR_FOR_PHONE,
                                          GOOD_SEPARATOR_FOR_PHONE);

        switch (phoneResult.length()) {
        case 8:
            return "331" + phoneResult;

        case 10:
            return "33" + phoneResult.substring(1, phoneResult.length());

        default:
            return phoneResult;
        }
    }

    /**
     * M&eacute;thode permettant de mettre tous les mots d'une chaine
     * s&eacute;par&eacute;s par un charact&egrave;re tel que espace ou - en
     * commençant par une majuscule et le reste en minuscule.
     *
     * @param chaine
     *
     * @return
     */
    private static String toUpperCaseAllBeginingNames(final String chaine) {
        String returned = "";
        String tmp = chaine;
        // La chaine commence forc&eacute;ment par une majuscule!
        tmp = tmp.substring(0, 1).toUpperCase()
              + tmp.substring(1, tmp.length());

        for (int j = 0; j < SEPARATORS_FOR_UPPER_BEGINNING_NAME.length; j++) {
            int i = tmp.indexOf(SEPARATORS_FOR_UPPER_BEGINNING_NAME[j]);

            while ((i != -1) && (i < (tmp.length() - 1))) {
                returned += tmp.substring(0, i + 1);

                try {
                    tmp = tmp.substring(i + 1, i + 2).toUpperCase()
                          + tmp.substring(i + 2, tmp.length());
                } catch (StringIndexOutOfBoundsException e) {
                    System.err.println(e + " caused by '" + chaine + "'");
                    throw e;
                }

                i = tmp.indexOf(SEPARATORS_FOR_UPPER_BEGINNING_NAME[j]);
            }
        }

        if (tmp.length() > 0) {
            returned += tmp;
        }

        return returned;
    }

    /**
     * M&eacute;thode permettant de formatter le nouveau sn!
     * @param sn
     * @return the filtered surname
     * @throws CharacterUnacceptedException thrown if an rejected character 
     * is encountered during analysis
     */
    public static String filterSn(final String sn)
                           throws CharacterUnacceptedException {
        String tmp = toUpperCaseAllBeginingNames(filterName(sn));

        if (!tmp.matches(REGEXP_FOR_LASTNAME)) {
            throw new CharacterUnacceptedException();
        }

        return tmp;
    }

    /**
     * Supprime de la chaîne pass&eacute;e en argument tous les
     * mauvais caract&egrave;res
     * @param startString
     * @return
     */
    private static String filterBadChars(final String startString) {
        String tmp = filterName(startString);

        for (int i = 0; i < BAD_SEPARATOR_FOR_ID.length; i++) {
            tmp = filterDelStringIntoString(BAD_SEPARATOR_FOR_ID[i], tmp);
        }

        // Suppression de tous les accents-c&eacute;dilles!
        tmp = removeBadChars(tmp);

        return tmp;
    }

    /**
     * Filtre pour r&eacute;cup&eacute;rer l'uid sur 14 caracteres max
     * bien format&eacute;.
     * @param sn the last name to filter
     * @return the filtered uid
     */
    public static String filterUid(final String sn) {
        String tmp = filterBadChars(sn);

        if (tmp.length() > 14) {
            return tmp.substring(0, 14);
        } else {
            return tmp;
        }
    }

    /**
     * Filtre pour r&eacute;cup&eacute;rer l'uid court sur 8.
     * caract&egrave;res max bien format&eacute;
     * @param sn the last name to filter
     * @return the filtered short uid
     */
    public static String filterShortUid(final String sn) {
        String tmp = filterBadChars(sn);

        if (tmp.length() > 8) {
            return tmp.substring(0, 8);
        } else {
            return tmp;
        }
    }

    /**
     * M&eacute;thode rassemblant les diff&eacute;rents filtres
     * &agrave; appeler sur l'attribut nomPatronymique.
     * @param name the last name to filter
     * @return the filtered patronimic name
     * @throws CharacterUnacceptedException thrown if an rejected character 
     * is encountered during analysis
     */
    public static String filterNomPatronymique(final String name)
                                        throws CharacterUnacceptedException {
        String tmp = toUpperCaseAllBeginingNames(filterName(name));

        if (!tmp.matches(REGEXP_CHARACTERS)) {
            throw new CharacterUnacceptedException(tmp);
        }

        return tmp;
    }

    /**
     * M&eacute;thode permettant de filtrer le prenom de l'etat civil.
     * @param name the first name to filter
     * @return the filtered public given name
     * @throws CharacterUnacceptedException thrown if an rejected character 
     * is encountered during analysis
     */
    public static String filterPrenomEtatCivil(final String name)
                                        throws CharacterUnacceptedException {
        String tmp = toUpperCaseAllBeginingNames(filterName(name));

        if (!tmp.matches(REGEXP_FOR_FISRTNAME)) {
            throw new CharacterUnacceptedException(tmp);
        }

        return tmp;
    }

    /**
     * M&eacute;thode permettant de filtrer le GivenName.
     * @param oldValue the value to filter
     * @return the filtered givenname
     * @throws CharacterUnacceptedException thrown if an rejected character 
     * is encountered during analysis
     */
    public static String filterGivenName(final String oldValue)
                                  throws CharacterUnacceptedException {
        String tmp = toUpperCaseAllBeginingNames(filterName(oldValue));

        if (!tmp.matches(REGEXP_FOR_FISRTNAME)) {
            throw new CharacterUnacceptedException(tmp);
        }

        return tmp;
    }

    /**
     * M&eacute;thode de g&eacute;n&eacute;ration d'un mot de passe: 8
     * caract&egrave;res.
     *
     * @return Le mot de passe
     */
    public static String generatePwd() {
        StringBuffer passwd = new StringBuffer("");
        Random r = new Random();

        for (int i = 0; i < 8; i++) {
            passwd.append(GOOD_PASSWORD.charAt(r.nextInt(64)));
        }

        return passwd.toString();
    }

    /**
     * M&eacute;thode permettant de virer les espaces au d&eacute;but
     * et en fin de chaîne et de remplacer les espaces et points restant par
     * des tirets.
     *
     * @param aString la chaîne &agrave; filtrer
     *
     * @return la chaîne filtr&eacute;e
     */
    public static String filterName(final String aString) {
        String tmp = aString.trim().replace('.', '-').toLowerCase();

        while (tmp.lastIndexOf('-') == (tmp.length() - 1)) {
            if (tmp.length() != 1) {
                tmp = tmp.substring(0, tmp.length() - 1);
            } else {
                tmp = "UNKNOWN";
            }
        }

        return tmp;
    }

    /**
     * M&eacute;thode permettant de virer les espaces au d&eacute;but
     * et en fin de chaîne et de remplacer les espaces et points restant par
     * des tirets.
     * @param aString la chaîne &agrave; filtrer
     * @return la chaîne filtr&eacute;e
     */
    public static String filterString(final String aString) {
        return aString.trim();
    }

    /**
     * Filters numerical identifier.
     *
     * @param value the string
     * @return the normalized String
     * @throws CharacterUnacceptedException launch if and only if the argument
     *         is not a numerical identifier
     */
    public static String filterNumber(final String value)
                               throws CharacterUnacceptedException {
        int n = Integer.parseInt(value);
        String tmp = String.valueOf(n);

        if (!tmp.matches(REGEXP_FOR_NUMERICAL_ID)) {
            throw new CharacterUnacceptedException(tmp);
        }

        return tmp;
    }

    /**
     * Filter all alphanumeric characters.
     * @param value the original value
     * @return the filtered string
     * @throws CharacterUnacceptedException thrown if an rejected character 
     * is encountered during analysis
     */
    public static String filterAlpha(final String value)
                              throws CharacterUnacceptedException {
        String tmp = value.trim();

        if (!tmp.matches(REGEXP_FOR_ALPHA_NUMERICAL_ID)) {
            throw new CharacterUnacceptedException(tmp);
        }

        return tmp;
    }

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
    public static String filterDate(final String value, final String format)
                             throws CharacterUnacceptedException {
        String tmp = value.trim();

        if (tmp.length() > 0) {
            SimpleDateFormat myFormat = new SimpleDateFormat(format);
            Date myDate = myFormat.parse(value, new ParsePosition(0));

            if (myDate != null) {
                return DateUtils.format(myDate);
            }
        }

        throw new CharacterUnacceptedException(tmp);
    }
}
