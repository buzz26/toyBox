set MODDATE=%date:~0,4%%date:~5,2%%date:~8,2%
set MODTIME=%time:~0,2%%time:~3,2%%time:~6,2%

move db db_%MODDATE%_%MODTIME%

pushd bin

call createdb.bat testDb
pause 

:: creating hotbackup
call toolsScript.bat testDb ../script.sql

popd
