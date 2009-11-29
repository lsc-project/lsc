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
package org.lsc.objects.flat;

/**
 * LDAP fOrganizationalPerson objectClass representation.
 * 
 * @deprecated
 * 		This class was used in LSC 1.1 projects, and is no longer
 * 		necessary, but kept for reverse compatibility. It will be
 * 		removed in LSC 1.3.
 */
public class fOrganizationalPerson extends fPerson {

    /** Monovalued attribute : preferredDeliveryMethod. */
    private String preferredDeliveryMethod;

    /** Monovalued attribute : title. */
    private String title;

    /** Monovalued attribute : x121Address. */
    private String x121Address;

    /** Monovalued attribute : registeredAddress. */
    private String registeredAddress;

    /** Monovalued attribute : destinationIndicator. */
    private String destinationIndicator;

    /** Monovalued attribute : telexNumber. */
    private String telexNumber;

    /** Monovalued attribute : teletexTerminalIdentifier. */
    private String teletexTerminalIdentifier;

    /** Monovalued attribute : internationaliSDNNumber. */
    private String internationaliSDNNumber;

    /** Monovalued attribute : facsimileTelephoneNumber. */
    private String facsimileTelephoneNumber;

    /** Monovalued attribute : street. */
    private String street;

    /** Monovalued attribute : postOfficeBox. */
    private String postOfficeBox;

    /** Monovalued attribute : postalCode. */
    private String postalCode;

    /** Monovalued attribute : postalAddress. */
    private String postalAddress;

    /** Monovalued attribute : physicalDeliveryOfficeName. */
    private String physicalDeliveryOfficeName;

    /** Monovalued attribute : ou. */
    private String ou;

    /** Monovalued attribute : st. */
    private String st;

    /** Monovalued attribute : l. */
    private String l;

    /**
     * Default preferredDeliveryMethod getter.
     * @return preferredDeliveryMethod value
     */
    public final String getPreferredDeliveryMethod() {
        return preferredDeliveryMethod;
    }

    /**
     * preferredDeliveryMethod setter.
     * @param value preferredDeliveryMethod value
     */
    public final void setPreferredDeliveryMethod(final String value) {
        this.preferredDeliveryMethod = value;
    }

    /**
     * Default title getter.
     * @return title value
     */
    public final String getTitle() {
        return title;
    }

    /**
     * title setter.
     * @param value title value
     */
    public final void setTitle(final String value) {
        this.title = value;
    }

    /**
     * Default x121Address getter.
     * @return x121Address value
     */
    public final String getX121Address() {
        return x121Address;
    }

    /**
     * x121Address setter.
     * @param value x121Address value
     */
    public final void setX121Address(final String value) {
        this.x121Address = value;
    }

    /**
     * Default registeredAddress getter.
     * @return registeredAddress value
     */
    public final String getRegisteredAddress() {
        return registeredAddress;
    }

    /**
     * registeredAddress setter.
     * @param value registeredAddress value
     */
    public final void setRegisteredAddress(final String value) {
        this.registeredAddress = value;
    }

    /**
     * Default destinationIndicator getter.
     * @return destinationIndicator value
     */
    public final String getDestinationIndicator() {
        return destinationIndicator;
    }

    /**
     * destinationIndicator setter.
     * @param value destinationIndicator value
     */
    public final void setDestinationIndicator(final String value) {
        this.destinationIndicator = value;
    }

    /**
     * Default telexNumber getter.
     * @return telexNumber value
     */
    public final String getTelexNumber() {
        return telexNumber;
    }

    /**
     * telexNumber setter.
     * @param value telexNumber value
     */
    public final void setTelexNumber(final String value) {
        this.telexNumber = value;
    }

    /**
     * Default teletexTerminalIdentifier getter.
     * @return teletexTerminalIdentifier value
     */
    public final String getTeletexTerminalIdentifier() {
        return teletexTerminalIdentifier;
    }

    /**
     * teletexTerminalIdentifier setter.
     * @param value teletexTerminalIdentifier value
     */
    public final void setTeletexTerminalIdentifier(final String value) {
        this.teletexTerminalIdentifier = value;
    }

    /**
     * Default internationaliSDNNumber getter.
     * @return internationaliSDNNumber value
     */
    public final String getInternationaliSDNNumber() {
        return internationaliSDNNumber;
    }

    /**
     * internationaliSDNNumber setter.
     * @param value internationaliSDNNumber value
     */
    public final void setInternationaliSDNNumber(final String value) {
        this.internationaliSDNNumber = value;
    }

    /**
     * Default facsimileTelephoneNumber getter.
     * @return facsimileTelephoneNumber value
     */
    public final String getFacsimileTelephoneNumber() {
        return facsimileTelephoneNumber;
    }

    /**
     * facsimileTelephoneNumber setter.
     * @param value facsimileTelephoneNumber value
     */
    public final void setFacsimileTelephoneNumber(final String value) {
        this.facsimileTelephoneNumber = value;
    }

    /**
     * Default street getter.
     * @return street value
     */
    public final String getStreet() {
        return street;
    }

    /**
     * street setter.
     * @param value street value
     */
    public final void setStreet(final String value) {
        this.street = value;
    }

    /**
     * Default postOfficeBox getter.
     * @return postOfficeBox value
     */
    public final String getPostOfficeBox() {
        return postOfficeBox;
    }

    /**
     * postOfficeBox setter.
     * @param value postOfficeBox value
     */
    public final void setPostOfficeBox(final String value) {
        this.postOfficeBox = value;
    }

    /**
     * Default postalCode getter.
     * @return postalCode value
     */
    public final String getPostalCode() {
        return postalCode;
    }

    /**
     * postalCode setter.
     * @param value postalCode value
     */
    public final void setPostalCode(final String value) {
        this.postalCode = value;
    }

    /**
     * Default postalAddress getter.
     * @return postalAddress value
     */
    public final String getPostalAddress() {
        return postalAddress;
    }

    /**
     * postalAddress setter.
     * @param value postalAddress value
     */
    public final void setPostalAddress(final String value) {
        this.postalAddress = value;
    }

    /**
     * Default physicalDeliveryOfficeName getter.
     * @return physicalDeliveryOfficeName value
     */
    public final String getPhysicalDeliveryOfficeName() {
        return physicalDeliveryOfficeName;
    }

    /**
     * physicalDeliveryOfficeName setter.
     * @param value physicalDeliveryOfficeName value
     */
    public final void setPhysicalDeliveryOfficeName(final String value) {
        this.physicalDeliveryOfficeName = value;
    }

    /**
     * Default ou getter.
     * @return ou value
     */
    public final String getOu() {
        return ou;
    }

    /**
     * ou setter.
     * @param value ou value
     */
    public final void setOu(final String value) {
        this.ou = value;
    }

    /**
     * Default st getter.
     * @return st value
     */
    public final String getSt() {
        return st;
    }

    /**
     * st setter.
     * @param value st value
     */
    public final void setSt(final String value) {
        this.st = value;
    }

    /**
     * Default l getter.
     * @return l value
     */
    public final String getL() {
        return l;
    }

    /**
     * l setter.
     * @param value l value
     */
    public final void setL(final String value) {
        this.l = value;
    }

}