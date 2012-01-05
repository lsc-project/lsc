#!/bin/bash

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#
# Ldap Synchronization Connector provides tools to synchronize electronic
# identities from a list of data sources including any database with a JDBC
# connector, another LDAP directory, flat files...
#
# LSC automatically embeds HSQLDB library. So, this script can:
# - Create a table from a CSV file header in a HSQLDB database;
# - Inject CSV data into a HSQLDB database;
# - Read data from a HSQLDB database table.
#
# ---------------------------------------------------------------------------------
#
# Copyright (c) 2008 - 2011 LSC Project 
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without modification,
# are permitted provided that the following conditions are met:
#
#   * Redistributions of source code must retain the above copyright notice, this
#     list of conditions and the following disclaimer.
#   * Redistributions in binary form must reproduce the above copyright notice,
#     this list of conditions and the following disclaimer in the documentation
#     and/or other materials provided with the distribution.
#   * Neither the name of the LSC Project nor the names of its contributors may be
#     used to endorse or promote products derived from this software without
#     specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
# ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
# (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
# OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
# NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
# IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#
#       (c) 2008 - 2011 LSC Project
#   Sebastien Bahloul <seb@lsc-project.org>
#   Thomas Chemineau <thomas@lsc-project.org>
#   Jonathan Clarke <jon@lsc-project.org>
#   Remy-Christophe Schermesser <rcs@lsc-project.org>
#
# ---------------------------------------------------------------------------------
#
# Version 0.2 (2010):
# - Adapted the script as a demo of LSC (lsc sample)
# - Removed all server start/stop operations, to just use a file database
# - Update population: add SHUTDOWN after table creation and don't delete CSV
#   file to allow re-opening of the DB.
# Author: Jonathan Clarke
#
# Version 0.1 (2009):
# - First version
# Author: Thomas CHEMINEAU
#
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

# ---------------------------------------------------------------------------------
# CONFIGURATION
# ---------------------------------------------------------------------------------

for /f %%i in ("%0") do set LSC_HSQLDBSAMPLE=%%~dpi..

set CLASSPATH=
 for  %%a in ("%LSC_HSQLDBSAMPLE%\lib\hsqldb-*.jar") do (
   set CLASSPATH=!CLASSPATH!;%%a
 )
set CLASSPATH=!CLASSPATH!

"%LSC_HSQLDBSAMPLE%\..\..\bin\lsc.bat" -f "%LSC_HSQLDBSAMPLE%\etc" -s all 