call ../setEnvGG.bat


call gradle clean 
pause

:: clean jar runnning strange action.
:: step by step running need 
::gradle jar > hogehoge.txt 2>&1
gradle jar
