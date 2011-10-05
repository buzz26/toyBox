set USER_HOME=%USERPROFILE%

set MVN_HOME=D:\Tooldev/apache-maven-3.0.3

set GROOVY_HOME=set GROOVY_HOME=C:\opt/Groovy-1.8.0
set JAVA_HOME=c:\opt/jdk
::set JAVA_HOME=C:\opt/jrockit-R28.0.1-jre1.6.0_20
set JAVA_OPTIONS=-Dfile.encoding=UTF-8

::doskey gcc-3=gcc

set GNUSTEP_HOME=D:\Tooldev/GNUstep
set GNUSTEP_PATH=%GNUSTEP_HOME%/bin;%GNUSTEP_HOME%/mingw32/bin;%GNUSTEP_HOME%/msys/1.0/bin;%GNUSTEP_HOME%/GNUstep/System/Tools
::set PATH=%PATH%;%GNUSTEP_PATH%
::set PATH=%PATH%;%GNUSTEP_PATH%;%MVN_HOME%/bin
set PATH=%GNUSTEP_PATH%;%MVN_HOME%/bin

::set HEADER=%GNUSTEP_HOME%/GNUstep/System/Library/Headers
::set LIB=%GNUSTEP_HOME%/GNUstep/System/Library/Libraries
::set OPT=-lobjc -lgnustep-base -fconstant-string-class=NSConstantString -enable-auto-import

::mvn.bat clean verify
::mvn clean package
::mvn -Dmaven.test.skip=true clean package

::mvn.bat -Dmaven.test.skip=true clean package %*

set PATH=%PATH%;%WINDIR%/system32
mvn.bat -f maven/pom.xml -Dmaven.test.skip=true clean package %*
