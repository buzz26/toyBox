call ../setEnvGG.bat


call gradle clean 
pause

:: clean jarで動かすと変な動きをするので一個一個動かす
:: たぶんタスクが並列で動いていると思う
::gradle jar > hogehoge.txt 2>&1
gradle jar
