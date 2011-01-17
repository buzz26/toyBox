#!/bin/sh

JAVA_HOME=/opt/jdk
H2_HOME=/opt/h2db
#H2_HOME=../
H2_DIR=${H2_HOME}/db
CLASSPATH=${H2_HOME}/classes:${H2_HOME}/lib/*

DBNAME=${1}
OPTS="-file ${H2_DIR}/backup/${DBNAME}.zip -dir ${H2_DIR} -db ${DBNAME}"

if [ -z ${DBNAME} ]; then
  echo "USAGE: ${0} dbname"
  exit 1
fi

#cd
mkdir -p ${H2_DIR}/backup
${JAVA_HOME}/bin/java -cp ${CLASSPATH} org.h2.tools.Backup ${OPTS}

