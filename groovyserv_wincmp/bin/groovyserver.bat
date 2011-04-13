@echo off
rem -----------------------------------------------------------------------
rem Copyright 2009-2011 the original author or authors.
rem
rem Licensed under the Apache License, Version 2.0 (the "License");
rem you may not use this file except in compliance with the License.
rem You may obtain a copy of the License at
rem
rem     http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.
rem -----------------------------------------------------------------------

setlocal

set JAVA_HOME=c:\opt\jdk
::set GROOVY_HOME=C:\opt\groovypp-0.4.101
set GROOVY_HOME=C:\opt\groovy-1.7.10
::set JAVA_OPTS=-Dgroovy.source.encoding=UTF-8 -Dfile.encoding=UTF-8
::set GROOVYSERV_OPTS=-Dgroovy.source.encoding=UTF-8 -Dfile.encoding=UTF-8

rem -----------------------------------
rem Resolve base directory
rem -----------------------------------
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.\
set GROOVYSERV_HOME=%DIRNAME%..
rem echo DEBUG: GROOVYSERV_HOME: %GROOVYSERV_HOME%

rem -----------------
rem Parse parameters
rem -----------------
:loop
if "%1" == "" (
    goto break
) else if "%1" == "-v" (
    set GROOVYSERV_OPTS=%GROOVYSERV_OPTS% -Dgroovyserver.verbose=true
) else if "%1" == "-q" (
    set OPT_QUIET=1
) else if "%1" == "-p" (
    if "%2" == "" (
        echo ERROR: Port number must be specified.
        goto end
    )
    set GROOVYSERVER_PORT=%2
    shift
) else if "%1" == "-k" (
    echo ERROR: groovyserver.bat does not support %1.
    goto end
) else if "%1" == "-r" (
    echo ERROR: groovyserver.bat does not support %1.
    goto end
) else (
    echo usage: groovyserver.bat [options]
    echo options:
    echo   -v       verbose output to the log file
    echo   -q       suppress starting messages
    echo  ^(-k       unsupported in groovyserver.bat^)
    echo  ^(-r       unsupported in groovyserver.bat^)
    echo   -p port  specify the port to listen

    goto end
)
shift
goto loop
:break

rem --------------------
rem Specify port number
rem --------------------
if not defined GROOVYSERVER_PORT (
    set GROOVYSERVER_PORT=1961
)
set GROOVYSERV_OPTS=%GROOVYSERV_OPTS% -Dgroovyserver.port=%GROOVYSERVER_PORT%
rem echo DEBUG: GROOVYSERV_OPTS: %GROOVYSERV_OPTS%

rem ---------------
rem Set class path
rem ---------------
set CP=%GROOVYSERV_HOME%\lib\jna-3.2.2.jar;
set CP=%GROOVYSERV_HOME%\lib\groovyserv-0.6.jar;%CP%
set CLASSPATH=%CP%;%CLASSPATH%
rem echo DEBUG: CLASSPATH: %CLASSPATH%

rem ---------------------------------
rem Compatibility for cygwin (FIXME)
rem ---------------------------------
if "%JAVA_HOME:~0,9%" == "/cygdrive" (
    for /f %%z in ('cygpath.exe -d "%JAVA_HOME%"') do set JAVA_HOME=%%z
)
if "%GROOVY_HOME:~0,9%" == "/cygdrive" (
    for /f %%z in ('cygpath.exe -d "%GROOVY_HOME%"') do set GROOVY_HOME=%%z
)

rem echo DEBUG: JAVA_HOME: %JAVA_HOME%
rem echo DEBUG: GROOVY_HOME: %GROOVY_HOME%

rem --------------------------------
rem Replace long name to short name
rem --------------------------------
for %%A in ("%GROOVY_HOME%"\bin\groovy) do set GROOVY_CMD=%%~sA
rem echo DEBUG: GROOVY_CMD: %GROOVY_CMD%

rem -------------
rem Start server
rem -------------
start "groovyserver[port:%GROOVYSERVER_PORT%]" /MIN %GROOVY_CMD% %GROOVYSERV_OPTS% -e "println('Groovyserver^(port %GROOVYSERVER_PORT%^) is running');println('Close this window to stop');org.jggug.kobo.groovyserv.GroovyServer.main(args)"
if errorlevel 1 (
    echo ERROR: Failed to invoke groovy command.
    exit /B 1
)

rem -------------------------------------------
rem  Wait for available
rem -------------------------------------------

if NOT "%OPT_QUIET%" == "1" (
  SET /P X=Starting.< NUL 1>&2
)

goto check
:loop2
rem trickey way to echo without newline
if NOT "%OPT_QUIET%" == "1" (
  SET /P X=.< NUL 1>&2
)

rem trickey way to wait one second
ping -n 2 127.0.0.1 > NUL

:check
rem if connecting to server is succeed, return successfully
"%GROOVYSERV_HOME%"\bin\groovyclient -Cwithout-invoking-server -e "" > NUL 2>&1
if not errorlevel 1 goto end
goto loop2

:end

if NOT "%OPT_QUIET%" == "1" (
  echo. 2>&1
)
