call ../setEnvGG.bat


::call gradle clean jar
call gradle clean jar %*

echo "====jar running start =="
java -jar build/libs/sdloader-jsp21.jar
echo "====jar running end =="
