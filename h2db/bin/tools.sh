#!/bin/sh

JAVA_HOME=/opt/jdk
H2_HOME=/opt/h2db
#H2_HOME=../
H2_DIR=${H2_HOME}/db
CLASSPATH=${H2_HOME}/classes:${H2_HOME}/lib/*

COMMAND=${1}
COMMANDS[0]="ChangeFileEncryption"
COMMANDS[1]="Console"
COMMANDS[2]="ConvertTraceFile"
COMMANDS[3]="CreateCluster"
COMMANDS[4]="DeleteDbFiles"
COMMANDS[5]="Script"
COMMANDS[6]="Recover"
COMMANDS[7]="Restore"
COMMANDS[8]="RunScript"
COMMANDS[9]="Server"
COMMANDS[10]="Shell"

if [ -z ${COMMAND} ]; then
  echo "Command is not set."
  echo "Please set classname into first argument, or select from below."
  select s in ${COMMANDS[@]}; do
    if [ ${s} ]; then
      COMMAND=${s}
      break
    else
      echo "Invalid selection."
    fi
  done
fi

I=-1
for arg in ${@}; do
  if [ ${I} -ge 0 ]; then
    ARGS[${I}]=${arg}
  fi
  I=$(expr ${I} + 1)
done

#umask 002
${JAVA_HOME}/bin/java -cp ${CLASSPATH} org.h2.tools.${COMMAND} ${ARGS[@]}

