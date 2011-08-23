drop table IF EXISTS HOGE_K6DATA2;

//case file links
create linked table  HOGE_DATA2('org.h2.Driver','jdbc:h2:file:../db2/testDb','sa','','HOGE_DATA');

//existing riser instance (ex> using startup3.bat and ../db2)
create linked table  HOGE_DATA2('org.h2.Driver','jdbc:h2:tcp://localhost:29092/testDb','sa','','HOGE_DATA');

