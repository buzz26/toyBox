windows using h2database setting

■Environmental setting

  # cd h2db/bin 
	# startup.bat

■database creation
  # cd h2db/bin
  # createdb.bat testdb

-----------------------------------------

linux using h2database setting

■Environmental setting

/opt to h2db copy
  # chmod a+x opt/h2db/etc/init.d/h2db
  # cp /opt/h2db/etc/init.d/h2db /etc/init.d

  service start
  # /sbin/service h2db start

connection 
  http://serverip:8082/
using web console


■database creation
  # cd /opt/h2db
  # ./createdb.sh testdb



■H2DB in SQL
  WEB Console

  ALTER USER sa SET PASSWORD 'sa';
  CREATE USER user01 PASSWORD 'user01';
  ALTER USER user01 ADMIN TRUE;

 Because there is not the way of thinking of the DB owner, 
administrator rights is necessary for user01

=======================================================
A Japanese commentary page

■Connection string of the JDBC
	http://wiki.paulownia.jp/java/jdbc

■An operation commentary site
	http://homepage2.nifty.com/yoks/TechNote/H2/H2_MnFrm.htm
