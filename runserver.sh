#!/bin/bash

./checksum.sh

if [ -f runserver.log ]; then
  cp runserver.log runserver.last.log
fi
if [ -f runserver.out ]; then
  cp runserver.out runserver.last.out
fi


# DO NOT CHANGE TO -P OPTION AS JENKINS NEEDS IT THIS WAY!

mvn integration-test -U -Pmbee-dev -Pamp-to-war -Dmaven.test.skip=true -Drebel.log=true 2>runserver.err 2>&1 | tee runserver.log | tee runserver.out

