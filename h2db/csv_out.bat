set time2=%time: =0%
set MODDATE=%date:~0,4%%date:~5,2%%date:~8,2%
set MODTIME=%time2:~0,2%%time2:~3,2%%time2:~6,2%

mkdir result

pushd bin
call toolsScript.bat prodDb ../sql/csvout.sql
move csvout ..\result\csvout_%MODDATE%_%MODTIME%
popd

