ref 
	http://d.hatena.ne.jp/htz/20080806/1218009392

■enviroments

	http://www.gnustep.org/experience/Windows.html 


Download
	1.gnustep-system-0.28.1-setup.exe
	2.gnustep-core-0.28.1-setup.exe
Applications, Frameworks and other Libraries
	3.SystemPreferences-1.1.0-2-setup.exe
	4.gorm-1.2.13-1-setup.exe
//5.Calculator-1.0.0-2-setup.exe

install dist D:\ToolDev\GNUstep\

#create BAT image

@echo off
set HEADER=D:\ToolDev/GNUstep/GNUstep/System/Library/Headers
set LIB=c:/GNUstep/GNUstep/System/Library/Libraries
set OPT=-lobjc -lgnustep-base -fconstant-string-class=NSConstantString -enable-auto-import
echo Compiling Now %1
gcc -o %1 %1.m -I %HEADER% -L %LIB% %OPT% 
echo Compiled.
