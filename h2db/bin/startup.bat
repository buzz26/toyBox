::#!/bin/sh

::#
::#[-help] or [-?]         Print the list of options
::#[-web]                  Start the web server with the H2 Console
::#[-webAllowOthers]       Allow other computers to connect - see below
::#[-webPort <port>]       The port (default: 8082)
::#[-webSSL]               Use encrypted (HTTPS) connections
::#[-browser]              Start a browser and open a page to connect to the web server
::#[-tcp]                  Start the TCP server
::#[-tcpAllowOthers]       Allow other computers to connect - see below
::#[-tcpPort <port>]       The port (default: 9092)
::#[-tcpSSL]               Use encrypted (SSL) connections
::#[-tcpPassword <pwd>]    The password for shutting down a TCP server
::#[-tcpShutdown "<url>"]  Stop the TCP server; example: tcp://localhost:9094
::#[-tcpShutdownForce]     Do not wait until all connections are closed
::#[-pg]                   Start the PG server(ODBC)
::#[-pgAllowOthers]        Allow other computers to connect - see below
::#[-pgPort <port>]        The port (default: 5435)
::#[-baseDir <dir>]        The base directory for H2 databases; for all servers
::#[-ifExists]             Only existing databases may be opened; for all servers
::#[-trace]                Print additional trace information; for all servers
::#
::#
::# %java -cp /pathto/h2.jar org.h2.tools.Server
::# TCP server running on tcp://localhost:9092 (only local connections)
::# ODBC server running on tcp://localhost:9083 (only local connections)
::# Web server running on http://localhost:8082 (only local connections)
::#	% java -cp /pathto/h2.jar org.h2.tools.Server -tcp
::#	TCP server running on tcp://localhost:9092 (only local connections)
::#
::#

set JAVA_HOME=/opt/jdk
set H2_HOME=../
::set H2_HOME=/opt/h2db
set H2_DIR=%H2_HOME%/db
set CLASSPATH=%H2_HOME%/classes;%H2_HOME%/lib/*

::#H2 DB Secction
set TCP_OPTS=-tcp -tcpAllowOthers -tcpPort 9092
set WEB_OPTS=-web -webAllowOthers -webPort 8082

set OPTS=%TCP_OPTS% %WEB_OPTS% -baseDir %H2_DIR% -ifExists

::umask 002
%JAVA_HOME%/bin/java -cp %CLASSPATH% org.h2.tools.Server %OPTS%

