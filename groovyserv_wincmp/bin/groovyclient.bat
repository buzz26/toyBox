::cd /d %0\..
set CLPATH=%0\..
::set HOME=%USERPROFILE%
set JAVA_HOME=c:\opt\jdk
set GROOVY_HOME=C:\opt\groovy-1.8.0
::set JAVA_OPTS=-Dgroovy.source.encoding=UTF-8 -Dfile.encoding=UTF-8
::set GROOVYSERV_OPTS=-Dgroovy.source.encoding=UTF-8 -Dfile.encoding=UTF-8

::arg check
set argc=0 
for %%a in ( %* ) do set /a argc+=1 
echo %argc% 

if "%argc%" == "0" goto CMD_STAY 

::%CLPATH%\groovyclient.exe %* 

:CMD_STAY 

::Windows
%windir%/system32/doskey groovy=%CLPATH%\groovyclient $* 
%windir%/system32/doskey groovyc=%CLPATH%\groovyclient -e "org.codehaus.groovy.tools.FileSystemCompiler.main(args)" $*
%windir%/system32/doskey groovysh=%CLPATH%\groovyclient -e "groovy.ui.InteractiveShell.main(args)" $* 
%windir%/system32/doskey groovyConsole=%CLPATH%\groovyclient -e "groovy.ui.Console.main(args)" $* 
%windir%/system32/doskey grape=%CLPATH%\groovyclient -e "org.codehaus.groovy.tools.GrapeMain.main(args)" $* 

::Linux
::alias groovy=groovyclient 
::alias groovyc="groovyclient -e 'org.codehaus.groovy.tools.FileSystemCompiler.main(args)'" 
::alias groovysh="groovyclient -e 'groovy.ui.InteractiveShell.main(args)'" 
::alias groovyConsole="groovyclient -e 'groovy.ui.Console.main(args)'" 
::alias grape="groovyclient -e 'org.codehaus.groovy.tools.GrapeMain.main(args)'" 