:: netbean7 moe patch
:: org/netbeans/core/startup というフォルダを作ったディレクトリ
set IMG_PATH=./

set JAVA_HOME=c:/opt/jdk
set NETBEANS_HOME=D:/NetBeans7.0
set CORE_JAR_PATH=%NETBEANS_HOME%/nb/core/locale/core_nb.jar

::473*300
%JAVA_HOME%/bin/jar -uvf %CORE_JAR_PATH% -C %IMG_PATH% org/netbeans/core/startup/splash_nb.gif
::529*252
%JAVA_HOME%/bin/jar -uvf %CORE_JAR_PATH% -C %IMG_PATH% org/netbeans/core/startup/about_nb.png


set UPDATE_CORE_JAR_PATH=%NETBEANS_HOME%/nb/modules/ext/locale/updater_nb.jar
::473*300
%JAVA_HOME%/bin/jar -uvf %UPDATE_CORE_JAR_PATH% -C %IMG_PATH% org/netbeans/core/startup/splash_nb.gif
%JAVA_HOME%/bin/jar -uvf %UPDATE_CORE_JAR_PATH% -C %IMG_PATH% org/netbeans/updater/resources/updatersplash_nb.gif

pause
