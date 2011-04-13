set MVN_HOME=D:\Tooldev\apache-maven-3.0.1
set HOME=%USERPROFILE%
set JAVA_HOME=c:\opt\jdk
::set JAVA_HOME=C:\opt\jrmc
set GROOVY_HOME=set GROOVY_HOME=C:\opt\Groovy-1.7.10
set JAVA_OPTIONS=-Dfile.encoding=UTF-8
set gcc-3=gcc
set GNUSTEP_HOME=D:\Tooldev\GNUstep

set PATH=%PATH%;%GNUSTEP_HOME%\bin;%GNUSTEP_HOME%\mingw\bin;%GNUSTEP_HOME%\GNUstep\System\Tools
::set HEADER=%GNUSTEP_HOME%/GNUstep/System/Library/Headers
::set LIB=%GNUSTEP_HOME%/GNUstep/System/Library/Libraries
::set OPT=-lobjc -lgnustep-base -fconstant-string-class=NSConstantString -enable-auto-import

::%MVN_HOME%/bin/mvn.bat clean verify
::mvn clean package
::mvn -Dmaven.test.skip=true clean package
%MVN_HOME%/bin/mvn.bat -Dmaven.test.skip=true clean package
