#!/bin/sh

JAVA_HOME=/opt/jdk
H2_HOME=/opt/h2db
#H2_HOME=../
H2_DIR=${H2_HOME}/db
CLASSPATH=${H2_HOME}/classes:${H2_HOME}/lib/*


DBNAME=${1}
DUMMY_SCRIPT=/tmp/dummy_$(whoami).sql
OPTS="-url jdbc:h2:file:${H2_DIR}/${DBNAME} -user sa -script ${DUMMY_SCRIPT}"

if [ -z ${DBNAME} ]; then
  echo "USAGE: ${0} dbname"
  exit 1
fi

cd
touch ${DUMMY_SCRIPT}
mkdir -p ${H2_DIR}
#chown $(whoami):h2database ${H2_DIR}
chown $(whoami):$(whoami) ${H2_DIR}
chmod g+s ${H2_DIR}
${JAVA_HOME}/bin/java -cp ${CLASSPATH} org.h2.tools.RunScript ${OPTS}
echo "${H2_DIR}/${DBNAME} was created."
