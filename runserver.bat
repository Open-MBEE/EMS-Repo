REM set MAVEN_OPTS=-Xms256m -Xmx1G -XX:PermSize=300m -Xdebug -Xrunjdwp:transport=dt_socket,address=10000,server=y,suspend=n -javaagent:c:\Users\bclement\Downloads\jrebel-5.4.0-nosetup\jrebel\jrebel.jar

mvn integration-test -X -U -Pmbee-dev -Pamp-to-war -DdeploymentName=mms -Dmaven.test.skip=true -Drebel.log=true
