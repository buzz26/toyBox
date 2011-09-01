::
:: http://d.hatena.ne.jp/uehaj/20100817/1282000473
::

cd /d %0\..

call ../setEnvG9.bat

set GROOVYSRV_HOME=D:\Tooldev/groovyserv-0.9
set CLASSPATH=%GRAILS_HOME%/lib/groovy-all-1.7.8.jar
set CLASSPATH=%CLASSPATH%;%GRAILS%/dist/grails-bootstrap-1.3.7.jar

%GROOVYSRV_HOME%\bin\groovyclient.exe ^
-cp "%CLASSPATH%" ^
-Dprogram.name='' ^
-Dgrails.home="%GRAILS_HOME%" ^
-Dbase.dir='.' ^
-e 'org.codehaus.grails.cli.support.GrailsStarter.main(args)' ^
--main org.codehaus.grails.cli.GrailsScriptRunner ^
--conf "%GRAILS_HOME%/conf/groovy-starter.conf" ^
stats
