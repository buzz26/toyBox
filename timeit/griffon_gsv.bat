cd /d %0\..

call ../setEnvG9.bat

set GROOVYSRV_HOME=D:\Tooldev/groovyserv-0.9
set CLASSPATH=%GRIFFON_HOME%/lib/groovy-all-1.8.1.jar
set CLASSPATH=%CLASSPATH%;%GRIFFON_HOME%/dist/griffon-cli-0.9.3.jar
set CLASSPATH=%CLASSPATH%;%GRIFFON_HOME%/dist/griffon-rt-0.9.3.jar
::set CLASSPATH=%CLASSPATH%;%GRIFFON_HOME%/lib/ant-1.8.1.jar
::set CLASSPATH=%CLASSPATH%;%GRIFFON_HOME%/lib/ant-luncher-1.8.1.jar
set CLASSPATH=%CLASSPATH%;%GRIFFON_HOME%/lib/slf4j-api-1.6.1.jar
set CLASSPATH=%CLASSPATH%;%GRIFFON_HOME%/lib/slf4j-log4j12-1.6.1.jar
set CLASSPATH=%CLASSPATH%;%GRIFFON_HOME%/lib/log4j-1.2.16.jar

%GROOVYSRV_HOME%\bin\groovyclient.exe ^
-cp "%CLASSPATH%" ^
-Dprogram.name="" ^
-Dgriffon.home="%GRIFFON_HOME%" ^
-Dbase.dir='.' ^
-Dtools.jar="%JAVA_HOME%/lib/tools.jar" ^
-Dgroovy.starter.conf="%GRIFFON_HOME%/conf/groovy-starter.conf" ^
-e 'org.codehaus.griffon.cli.support.GriffonStarter.main(args)' ^
--main org.codehaus.griffon.GriffonMain ^
--conf "%GRIFFON_HOME%/conf/groovy-starter.conf" ^
stats
