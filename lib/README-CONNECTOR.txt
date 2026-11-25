The MySQL JDBC driver is already here:

    lib/mysql-connector-j-8.4.0.jar

run.bat / run.sh use it, so Maven is NOT needed.

NetBeans (Ant project): right-click Libraries -> Add JAR/Folder -> pick this jar.
Maven (pom.xml): optional; Maven downloads the same driver itself.

Newer version? Download "Platform Independent" from
  https://dev.mysql.com/downloads/connector/j/
and replace the jar here.

The connection settings (host, port, user, password) are in Java:
    src/main/java/tala/database/DBConnection.java
