#!/bin/bash
cp runserver.log runserver.last.log
mvn integration-test -Pmms-dev -Pamp-to-war -Dmaven.test.skip=true -Drebel.log=true 2>runserver.err 2>&1 | tee runserver.log

