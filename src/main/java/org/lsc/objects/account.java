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

import org.lsc.objects.top;

/**
 * LDAP account objectClass representation
 * 
 * @deprecated
 * 		This class was used in LSC 1.1 projects, and is no longer
 * 		necessary, but kept for reverse compatibility. It will be
 * 		removed in LSC 1.3.
 */
@SuppressWarnings("unchecked")
public class account extends top {

	public account() {
		super();
		objectClass.add("account");
	}

   /** Multivalued attribute : uid */
   protected List uid;

   /** Multivalued attribute : description */
   protected List description;

   /** Multivalued attribute : seeAlso */
   protected List seeAlso;

   /** Multivalued attribute : l */
   protected List l;

   /** Multivalued attribute : o */
   protected List o;

   /** Multivalued attribute : ou */
   protected List ou;

   /** Multivalued attribute : host */
   protected List host;

   /**
    * uid getter
    * @return uid values
    */
   public final List getUid() {
       return uid;
   }

   /**
    * uid setter
    * @param values uid values
    */
   public final void setUid(List values) {
       this.uid = values;
   }

   /**
    * description getter
    * @return description values
    */
   public final List getDescription() {
       return description;
   }

   /**
    * description setter
    * @param values description values
    */
   public final void setDescription(List values) {
       this.description = values;
   }

   /**
    * seeAlso getter
    * @return seeAlso values
    */
   public final List getSeeAlso() {
       return seeAlso;
   }

   /**
    * seeAlso setter
    * @param values seeAlso values
    */
   public final void setSeeAlso(List values) {
       this.seeAlso = values;
   }

   /**
    * l getter
    * @return l values
    */
   public final List getL() {
       return l;
   }

   /**
    * l setter
    * @param values l values
    */
   public final void setL(List values) {
       this.l = values;
   }

   /**
    * o getter
    * @return o values
    */
   public final List getO() {
       return o;
   }

   /**
    * o setter
    * @param values o values
    */
   public final void setO(List values) {
       this.o = values;
   }

   /**
    * ou getter
    * @return ou values
    */
   public final List getOu() {
       return ou;
   }

   /**
    * ou setter
    * @param values ou values
    */
   public final void setOu(List values) {
       this.ou = values;
   }

   /**
    * host getter
    * @return host values
    */
   public final List getHost() {
       return host;
   }

   /**
    * host setter
    * @param values host values
    */
   public final void setHost(List values) {
       this.host = values;
   }

}