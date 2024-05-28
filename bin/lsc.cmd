@echo off

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


REM ====================================================================
REM  work out where LSC lives

setLocal EnableDelayedExpansion

for /f %%i in ("%0") do set LSC_HOME=%%~dpi..

REM ====================================================================
REM  Configuration
REM ====================================================================

SET CFG_DIR=%LSC_HOME%\etc
SET LIB_DIR=%LSC_HOME%\lib
SET LOG_DIR=%TEMP%
SET LOG_FILE=%LOG_DIR%\lsc.log

call:get_java

call:log "Starting LSC"

set CLASSPATH=%CLASSPATH%;.
 for  %%a in ("%LIB_DIR%\slf4j-api-*.jar") do (
   set CLASSPATH=!CLASSPATH!;%%a
 )
 for  %%a in ("%LIB_DIR%\*.jar") do (
   set CLASSPATH=!CLASSPATH!;%%a
 )
set CLASSPATH=!CLASSPATH!

REM if LSC options include the "-a" flag, set the required JMX options
FOR %%i IN ( %* ) DO IF "%%i"=="-a" (
  IF DEFINED LSC_JMXPORT (
	SET JAVA_OPTS=-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=%LSC_JMXPORT% -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false
  ) ELSE (
    call:log "LSC: to control your asynchronous task(s), consider setting the LSC_JMXPORT environment variable to a positive value to bind the JMX interface to that TCP port."
  )
)

"%JAVA_COMMAND%" -cp "%CLASSPATH%" %JAVA_OPTS% org.lsc.Launcher %*

REM LSC finished running
call:log "LSC finished running"

if ERRORLEVEL 1 (
	echo Please look up the log file %LOG_FILE% to see the error
	exit /B ERRORLEVEL

)

REM ====================================================================
REM  Functions
REM ====================================================================

:log
	echo %DATE%  [lsc] %~1 >> %LOG_FILE%
goto:eof


:fatal
	call:log %~1
	echo %DATE%  [lsc] %~1
goto:eof


REM Find the java.exe executable
:get_java
	IF DEFINED JAVA_HOME ( SET JAVA_COMMAND=%JAVA_HOME%\bin\java.exe) ELSE ( SET JAVA_COMMAND=)
	IF NOT EXIST "%JAVA_COMMAND%" ( SET PATHQ="%PATH%"
 GOTO findJava  )
goto:eof

REM Explore the path to find Java
:findJava
    if "%PATHQ%"=="" goto WEND
    for /F "delims=;" %%i in ("%PATHQ%") do set JAVA_COMMAND=%%i\java.exe
    for /F "delims=; tokens=1,*" %%i in ("%PATHQ%") do set PATHQ=%%j
    if exist %JAVA_COMMAND% goto:eof
    goto findJava 

:WEND
REM Nothing seems appropriate, warn and exit
call:fatal "No java executable found on PATH or in JAVA_HOME! Aborting."
call:fatal "Define JAVA_HOME"

EXIT /B 2


