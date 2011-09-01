::#!/bin/sh

set JAVA_HOME=/opt/jdk
::set H2_HOME=/opt/h2db
set H2_HOME=../
set H2_DIR=%H2_HOME%/db
set CLASSPATH=%H2_HOME%/classes;%H2_HOME%/lib/*

::#H2 DB Secction

set OPTS=-tcpShutdown tcp://localhost:9092 -tcpShutdownForce

%JAVA_HOME%/bin/java -cp %CLASSPATH% org.h2.tools.Server %OPTS%

pause