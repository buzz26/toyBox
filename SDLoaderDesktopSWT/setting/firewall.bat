set APPPORT=%1
if "%1" == "" set APPPORT=9090
echo "APPPORT=%APPPORT%"

for /f "usebackq tokens=*" %%i in (`ver`) do @set OSname=%%i
echo "OSname=%OSname%"

for /F "delims=" %%s in ('cd') do @set PWD=%%s

netsh advfirewall firewall add rule name="TCP %APPPORT%" dir=in action=allow protocol=TCP localport=%APPPORT%
netsh advfirewall firewall add rule name="SDLoaderDesktopSWT" dir=in action=allow program="%PWD%\SDLoaderDesktopSWT.exe" enable=yes

::winXP
netsh firewall set opmode mode = ENABLE
netsh firewall add portopening protocol = TCP port = %APPPORT% name = "TCP %APPPORT%"
netsh firewall add allowedprogram program = "%PWD%\SDLoaderDesktopSWT.exe" name ="SDLoaderDesktopSWT"
