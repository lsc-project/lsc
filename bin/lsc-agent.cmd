@REM   ========================================================================
@REM    Description
@REM   ========================================================================

@REM   ------------------------------------------------------------------------

@REM   Windows batch script for remote interaction with LDAP Synchronization
@REM   Connector launching synchronization tasks with correct class path.

@REM   Last updated on 08/04/2020.

@REM   ------------------------------------------------------------------------



@REM   ========================================================================
@REM    License
@REM   ========================================================================

@REM   ------------------------------------------------------------------------

@REM   Copyright (c) 2008 - 2020 LSC Project
@REM   All rights reserved.

@REM   Redistribution and use in source and binary forms, with or without
@REM   modification, are permitted provided that the following conditions are
@REM   met:

@REM   * Redistributions of source code must retain the above copyright notice,
@REM     this list of conditions and the following disclaimer.
@REM   * Redistributions in binary form must reproduce the above copyright
@REM     notice, this list of conditions and the following disclaimer in the
@REM     documentation and/or other materials provided with the distribution.
@REM   * Neither the name of the LSC Project nor the names of its contributors
@REM     may be used to endorse or promote products derived from this software
@REM     without specific prior written permission.

@REM   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
@REM   IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
@REM   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
@REM   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
@REM   OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
@REM   EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
@REM   PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
@REM   PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
@REM   LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
@REM   NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
@REM   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

@REM   (c) 2008 - 2020 LSC Project
@REM   Sebastien Bahloul <seb@lsc-project.org>
@REM   Thomas Chemineau <thomas@lsc-project.org>
@REM   Jonathan Clarke <jon@lsc-project.org>
@REM   Remy-Christophe Schermesser <rcs@lsc-project.org>

@REM   ------------------------------------------------------------------------



@REM   ========================================================================
@REM    Configuration of CMD
@REM   ========================================================================

@REM   Configure command enhancements; also, variable expansion at runtime:
@SETLOCAL ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION

@REM   Disable echo of executed commands:
@ECHO OFF



REM   =========================================================================
REM    Configuration of LSC
REM   =========================================================================

REM   Change to batch script's directory.
CHDIR /D "%~dp0.."

REM   Make logs directory if not exist:
MKDIR logs>NUL 2>&1

REM   Define config variables:
SET LIB_DIR=!CD!\lib
SET LOG_FILE=!CD!\logs\lsc-agent.log

REM   Add spacing entry between runs to log file for easy reading.
CALL :Log "--------------------------------------------------"

REM   Add LSC' start of run entry to log file:
CALL :Log "INFO  - Starting LSC Remote Agent."

REM   Call function to get Java executable file path:
SET Java_Command=
CALL :Get

REM   Configure and run instance of LSC if Java executable was found:
IF DEFINED Java_Command (

	REM   Define "CLASSPATH" environment variable.

	REM      Reference:

	REM      - Setting the Class Path
	REM        https://docs.oracle.com/javase/8/docs/technotes/tools/windows/classpath.html
	IF DEFINED CLASSPATH (

		SET CLASSPATH_Backup=!CLASSPATH!
		SET CLASSPATH=!CLASSPATH!;.;!LIB_DIR!\slf4j-api-*;!LIB_DIR!\*
		CALL :Log "INFO  - A previously defined "CLASSPATH" variable was found."

		) ELSE (

			SET CLASSPATH=.;!LIB_DIR!\slf4j-api-*;!LIB_DIR!\*
			CALL :Log "INFO  - A previously defined "CLASSPATH" variable was not found." )

	REM   Detect "JAVA_OPTS" environment variable.
	IF DEFINED JAVA_OPTS (

		CALL :Log "INFO  - A previously defined "JAVA_OPTS" variable was found."

		) ELSE (

			CALL :Log "INFO  - A previously defined "JAVA_OPTS" variable was not found." )

	REM   Run instance of LSC with defined options:
	SET Full_Java_Command="!Java_Command!" -classpath "!CLASSPATH!" !JAVA_OPTS! org.lsc.jmx.LscAgent
	CALL :Log "INFO  - Executing command '!Full_Java_Command!'"
	!Full_Java_Command!

	REM   Check if there was some error and, if so, warn user:
	IF !ERRORLEVEL!==0 (

		CALL :Log "INFO  - The command was executed successfully."
		ECHO LSC Remote Agent was executed successfully.

		) ELSE (

			CALL :Log "ERROR - There was a problem while executing the command. The reported exit code was "!ERRORLEVEL!"."
			ECHO Please look up the log file "!LOG_FILE!" to see detailed information.)

	REM   Clear variables:
	SET CLASSPATH=!CLASSPATH_Backup!
	SET CLASSPATH_Backup=
	SET Full_Java_Command=
	SET Java_Command=)

REM   Add LSC's end of run entry to log file:
CALL :Log "INFO  - LSC Remote Agent finished running."

REM   Clear config variables:
SET LIB_DIR=
SET LOG_FILE=

REM   End batch script:
EXIT /B



REM   =========================================================================
REM    Functions
REM   =========================================================================

REM   -----------------------------------------
REM    Write execution messages into log file:
REM   -----------------------------------------

:Log

REM   Write given message through function parameter to log file:
SET Message=%*
ECHO !DATE! - !TIME!  [LSC -  CMD] !Message:~1,-1!>>!LOG_FILE!
SET Message=

REM   Exit function:
EXIT /B


REM   ---------------------------------
REM    Find the "java.exe" executable:
REM   ---------------------------------

:Get

REM   Check "JAVA_HOME" environment variabe:
IF EXIST "!JAVA_HOME!\bin\java.exe" (

	SET Java_Command=!JAVA_HOME!\bin\java.exe
	CALL :Log "INFO  - Found Java executable at "!Java_Command!", through the "JAVA_HOME" environment variable."

	) ELSE (

		REM   Check "PATH" environment variable:
		SET PATH_Query=!PATH!
		CALL :Find
		SET PATH_Query=

		IF NOT DEFINED Java_Command (

			REM   Check inside "Program Files" directory:
			SET Java_Install=
			FOR /F "tokens=*" %%A IN ( 'DIR "!ProgramFiles!\Java" /A:D /B /O:D 2^>NUL' ) DO (
				SET Java_Install=%%A)

			IF EXIST "!ProgramFiles!\Java\!Java_Install!\bin\java.exe" (

				SET Java_Command=!ProgramFiles!\Java\!Java_Install!\bin\java.exe
				SET Java_Install=
				CALL :Log "INFO  - Found Java executable at "!Java_Command!", through the "ProgramFiles" environment variable."

				) ELSE (

					REM   Check inside "Program Files (x86)" directory:
					SET Java_Install=
					FOR /F "tokens=*" %%A IN ( 'DIR "!ProgramFiles(x86)!\Java" /A:D /B /O:D 2^>NUL' ) DO (
						SET Java_Install=%%A)

					IF EXIST "!ProgramFiles(x86)!\Java\!Java_Install!\bin\java.exe" (

						SET Java_Command=!ProgramFiles^(x86^)!\Java\!Java_Install!\bin\java.exe
						SET Java_Install=
						CALL :Log "INFO  - Found Java executable at "!Java_Command!", through the "ProgramFiles^(x86^)" environment variable."

						) ELSE (

							REM   Write error finding "java.exe" into log file and warn user:
							CALL :Log "FATAL - Java executable was not found in the "JAVA_HOME" or "PATH" environment variables, nor in "Program Files" or "Program Files ^(x86^)" installation directories. Aborting..."
							CALL :Log "INFO  - Please define "JAVA_HOME" or "PATH" environment variables, or install Java."
							ECHO Error finding the Java executable. Aborted execution.))))

REM   Exit function:
EXIT /B


REM   --------------------------------------------------------------------
REM    Recursively explore the "PATH" environment variable to find Java's
REM    directory:
REM   --------------------------------------------------------------------

:Find

IF DEFINED PATH_Query (

	FOR /F "tokens=1* delims=;" %%A IN ( "!PATH_Query!" ) DO (

		IF EXIST "%%A\java.exe" (

			SET Java_Command=%%A\java.exe
			CALL :Log "INFO  - Found Java executable at "!Java_Command!", through the "PATH" environment variable."

			) ELSE (

				SET PATH_Query=%%B
				CALL :Find )))

REM   Exit function:
EXIT /B