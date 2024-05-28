@echo off

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

call:log "Starting LSC Remote Agent"


set CLASSPATH=.
 for  %%a in ("%LIB_DIR%\slf4j-api-*.jar") do (
   set CLASSPATH=!CLASSPATH!;%%a
 )
 for  %%a in ("%LIB_DIR%\*.jar") do (
   set CLASSPATH=!CLASSPATH!;%%a
 )
set CLASSPATH=!CLASSPATH!


"%JAVA_COMMAND%" -cp "%CLASSPATH%" org.lsc.jmx.LscAgent %*

REM LSC finished running
call:log "LSC Remote Agent finished running"

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
REM Nothing seems approprite, warn and exit
call:fatal "No java executable found on PATH or in JAVA_HOME! Aborting."
call:fatal "Define JAVA_HOME"

EXIT /B 2


