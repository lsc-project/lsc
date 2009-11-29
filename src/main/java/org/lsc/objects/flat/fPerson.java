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
 * LDAP fPerson objectClass representation.
 * 
 * @deprecated
 * 		This class was used in LSC 1.1 projects, and is no longer
 * 		necessary, but kept for reverse compatibility. It will be
 * 		removed in LSC 1.3.
 */
public class fPerson extends fTop {

   /** Monovalued attribute : sn. */
   private String sn;

   /** Monovalued attribute : cn. */
   private String cn;

   /** Monovalued attribute : userPassword. */
   private String userPassword;

   /** Monovalued attribute : telephoneNumber. */
   private String telephoneNumber;

   /** Monovalued attribute : seeAlso. */
   private String seeAlso;

   /** Monovalued attribute : description. */
   private String description;

   /**
    * Default sn getter.
    * @return sn value
    */
   public final String getSn() {
       return sn;
   }

   /**
    * sn setter.
    * @param value sn value
    */
   public final void setSn(final String value) {
       this.sn = value;
   }

   /**
    * Default cn getter.
    * @return cn value
    */
   public final String getCn() {
       return cn;
   }

   /**
    * cn setter.
    * @param value cn value
    */
   public final void setCn(final String value) {
       this.cn = value;
   }

   /**
    * Default userPassword getter.
    * @return userPassword value
    */
   public final String getUserPassword() {
       return userPassword;
   }

   /**
    * userPassword setter.
    * @param value userPassword value
    */
   public final void setUserPassword(final String value) {
       this.userPassword = value;
   }

   /**
    * Default telephoneNumber getter.
    * @return telephoneNumber value
    */
   public final String getTelephoneNumber() {
       return telephoneNumber;
   }

   /**
    * telephoneNumber setter.
    * @param value telephoneNumber value
    */
   public final void setTelephoneNumber(final String value) {
       this.telephoneNumber = value;
   }

   /**
    * Default seeAlso getter.
    * @return seeAlso value
    */
   public final String getSeeAlso() {
       return seeAlso;
   }

   /**
    * seeAlso setter.
    * @param value seeAlso value
    */
   public final void setSeeAlso(final String value) {
       this.seeAlso = value;
   }

   /**
    * Default description getter.
    * @return description value
    */
   public final String getDescription() {
       return description;
   }

   /**
    * description setter.
    * @param value description value
    */
   public final void setDescription(final String value) {
       this.description = value;
   }

}