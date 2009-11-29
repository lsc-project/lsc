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
package org.lsc.objects;

import java.util.List;

/**
 * LDAP person objectClass representation.
 */
public class person extends top {

    /**
     * Default constructor.
     */
    public person() {
        super();
		objectClass.add("person");
	}

   /** Multivalued attribute : sn. */
   private List sn;

   /** Multivalued attribute : cn. */
   private List cn;

   /** Multivalued attribute : userPassword. */
   private List userPassword;

   /** Multivalued attribute : telephoneNumber. */
   private List telephoneNumber;

   /** Multivalued attribute : seeAlso. */
   private List seeAlso;

   /** Multivalued attribute : description. */
   private List description;

   /**
    * sn getter.
    * @return sn values
    */
   public final List getSn() {
       return sn;
   }

   /**
    * sn setter.
    * @param values sn values
    */
   public final void setSn(final List values) {
       this.sn = values;
   }

   /**
    * cn getter.
    * @return cn values
    */
   public final List getCn() {
       return cn;
   }

   /**
    * cn setter.
    * @param values cn values
    */
   public final void setCn(final List values) {
       this.cn = values;
   }

   /**
    * userPassword getter.
    * @return userPassword values
    */
   public final List getUserPassword() {
       return userPassword;
   }

   /**
    * userPassword setter.
    * @param values userPassword values
    */
   public final void setUserPassword(final List values) {
       this.userPassword = values;
   }

   /**
    * telephoneNumber getter.
    * @return telephoneNumber values
    */
   public final List getTelephoneNumber() {
       return telephoneNumber;
   }

   /**
    * telephoneNumber setter.
    * @param values telephoneNumber values
    */
   public final void setTelephoneNumber(final List values) {
       this.telephoneNumber = values;
   }

   /**
    * seeAlso getter.
    * @return seeAlso values
    */
   public final List getSeeAlso() {
       return seeAlso;
   }

   /**
    * seeAlso setter.
    * @param values seeAlso values
    */
   public final void setSeeAlso(final List values) {
       this.seeAlso = values;
   }

   /**
    * description getter.
    * @return description values
    */
   public final List getDescription() {
       return description;
   }

   /**
    * description setter.
    * @param values description values
    */
   public final void setDescription(final List values) {
       this.description = values;
   }

}