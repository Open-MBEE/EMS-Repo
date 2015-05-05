#!/bin/bash
cp runserver.log runserver.last.log
# DO NOT CHANGE TO -P OPTION AS JENKINS NEEDS IT THIS WAY!
mvn integration-test -Pmbee-dev -Pamp-to-war -Dmaven.test.skip=true -Drebel.log=true 2>runserver.err 1>runserver.log &

