#!/bin/bash
cp runserver.log runserver.last.log
mvn integration-test -Pamp-to-war -Pmbee-dev -Dmaven.test.skip=true -Drebel.log=true 2>runserver.err 2>&1 | tee runserver.log


# -Dexec.workingdir="/test-data/javawebscripts" -Dexec.args="+x diff2.sh"
