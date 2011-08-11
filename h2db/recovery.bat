pushd bin

call createdb.bat testDb
pause 
:: creating 
call toolsScript.bat testDb ../script.sql

popd
