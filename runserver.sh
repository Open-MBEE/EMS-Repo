#!/bin/bash

./checksum.sh

if [ -f runserver.log ]; then
  cp runserver.log runserver.last.log
fi
if [ -f runserver.out ]; then
  cp runserver.out runserver.last.out
fi

echo "arg 1 = $1"

if [ "$1" == "" ]; then
  pom=pom.xml
else
  pom=$1
fi

echo "mvn integration-test -X -U -f $pom -Pmbee-dev -Pamp-to-war -DdeploymentName -Dmaven.test.skip=true 2>runserver.err 2>&1 | tee runserver.log | tee runserver.out"
user=`whoami`
if [ $user == 'jenkins' ]; then
  echo 'running with specified MAVEN_OPTS'
  MAVEN_OPTS='-Xms256m -Xmx4G -XX:PermSize=512m -Xdebug -Xrunjdwp:transport=dt_socket,address=10000,server=y,suspend=n' mvn integration-test -X -U -f $pom -Pmbee-dev -Pamp-to-war -DdeploymentName=mms -Dmaven.test.skip=true -Dmaven.tomcat.port=8080 2>runserver.err 2>&1 | tee runserver.log | tee runserver.out 
else
  echo 'running with user MAVEN_OPTS'
  mvn integration-test -X -U -f $pom -Pmbee-dev -Pamp-to-war -DdeploymentName=mms -Dmaven.test.skip=true -Dmaven.tomcat.port=8080 2>runserver.err 2>&1 | tee runserver.log | tee runserver.out
fi


#omitPIDs=$(<omitPIDs.txt)

#mapfile omitPIDs < omitPIDs.txt


#echo PIDs to omit are $omitPIDs

#cmd="$(/sbin/pidof sh -c -x"

#for pid in $omitPIDs 
#do
#        echo ommiting PID $pid
#        cmd+=" -o $pid"
#done

#cmd+=" | awk '{print $1}')"

#echo cmd is $cmd
#PID= eval $cmd 

# TODO - uncomment following for Axel's DNG tests to run in cae-ems
#sleep 20

#PID=$(/sbin/pidof sh -c -x | awk '{print $1}')
#PID=$(ps ax | grep 'java -Xms256m -Xmx4G -XX:PermSize=512m -Xdebug -Xrunjdwp:transport=dt_socket,address=10000,' | awk 'NR==1' | awk '{print $1}')

### Trim leading whitespaces ###
#PID="${PID##*( )}"

### trim trailing whitespaces  ##
#PID="${PID%%*( )}"

#echo The PID is $PID 
#echo $PID > myPID.txt



