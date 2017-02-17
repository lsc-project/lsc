@echo off

REM ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
REM
REM Ldap Synchronization Connector provides tools to synchronize electronic
REM identities from a list of data sources including any database with a JDBC
REM connector, another LDAP directory, flat files...
REM
REM LSC automatically embeds HSQLDB library. So, this script can:
REM - Create a table from a CSV file header in a HSQLDB database;
REM - Inject CSV data into a HSQLDB database;
REM - Read data from a HSQLDB database table.
REM
REM ---------------------------------------------------------------------------------
REM
REM Copyright (c) 2008 - 2011 LSC Project 
REM All rights reserved.
REM
REM Redistribution and use in source and binary forms, with or without modification,
REM are permitted provided that the following conditions are met:
REM
REM   * Redistributions of source code must retain the above copyright notice, this
REM     list of conditions and the following disclaimer.
REM   * Redistributions in binary form must reproduce the above copyright notice,
REM     this list of conditions and the following disclaimer in the documentation
REM     and/or other materials provided with the distribution.
REM   * Neither the name of the LSC Project nor the names of its contributors may be
REM     used to endorse or promote products derived from this software without
REM     specific prior written permission.
REM
REM THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
REM ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
REM WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
REM DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
REM ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
REM (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
REM OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
REM THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
REM NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
REM IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
REM
REM       (c) 2008 - 2011 LSC Project
REM   Sebastien Bahloul <seb@lsc-project.org>
REM   Thomas Chemineau <thomas@lsc-project.org>
REM   Jonathan Clarke <jon@lsc-project.org>
REM   Remy-Christophe Schermesser <rcs@lsc-project.org>
REM
REM ---------------------------------------------------------------------------------
REM
REM Version 0.2 (2010):
REM - Adapted the script as a demo of LSC (lsc sample)
REM - Removed all server start/stop operations, to just use a file database
REM - Update population: add SHUTDOWN after table creation and don't delete CSV
REM   file to allow re-opening of the DB.
REM Author: Jonathan Clarke
REM
REM Version 0.1 (2009):
REM - First version
REM Author: Thomas CHEMINEAU
REM
REM ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

REM ---------------------------------------------------------------------------------
REM CONFIGURATION
REM ---------------------------------------------------------------------------------

for /f %%i in ("%0") do set LSC_HSQLDBSAMPLE=%%~dpi..

set CLASSPATH=
 for  %%a in ("%LSC_HSQLDBSAMPLE%\lib\hsqldb-*.jar") do (
   set CLASSPATH=!CLASSPATH!;%%a
 )
set CLASSPATH=!CLASSPATH!

"%LSC_HSQLDBSAMPLE%\..\..\bin\lsc.bat" -f "%LSC_HSQLDBSAMPLE%\etc" -s all 
