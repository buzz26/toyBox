set JAVA_HOME=c:\opt/jdk
set H2_HOME=..\
set H2_DIR=%H2_HOME%\db
set CLASSPATH=%H2_HOME%/classes;%H2_HOME%/lib/*


set DBNAME=%1
set DUMMY_SCRIPT=%2
set OPTS=-url jdbc:h2:file:%H2_DIR%/%DBNAME% -user sa -script %DUMMY_SCRIPT%

mkdir %H2_DIR%
%JAVA_HOME%/bin/java -cp %CLASSPATH% org.h2.tools.RunScript %OPTS%
echo "%H2_DIR%/%DBNAME% was created."
