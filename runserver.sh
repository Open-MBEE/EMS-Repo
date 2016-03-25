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

echo "mvn integration-test -X -U -f $pom -Pmbee-dev -Pamp-to-war -Dmaven.test.skip=true 2>runserver.err 2>&1 | tee runserver.log | tee runserver.out"
mvn integration-test -X -U -f $pom -Pmbee-dev -Pamp-to-war -Dmaven.test.skip=true -Dmaven.tomcat.port=8080 2>runserver.err 2>&1 | tee runserver.log | tee runserver.out

