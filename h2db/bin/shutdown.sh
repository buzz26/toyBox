#!/bin/sh

JAVA_HOME=/opt/jdk
H2_HOME=/opt/h2db
#H2_HOME=../
H2_DIR=${H2_HOME}/db
CLASSPATH=${H2_HOME}/classes:${H2_HOME}/lib/*

#H2 DB Secction
OPTS="-tcpShutdown tcp://localhost:9092 -tcpShutdownForce"

${JAVA_HOME}/bin/java -cp ${CLASSPATH} org.h2.tools.Server ${OPTS}

