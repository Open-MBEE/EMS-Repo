#!/bin/bash
cp runserver.log runserver.last.log
mvn -e exec:exec -Pamp-to-war -Pmbee-dev -Dexec.executable="./diff2.sh" -Dexec.workingdir="./test-data/javawebscripts" -Dmaven.test.skip=true -Drebel.log=true 2>runserver.err 2>&1 | tee runserver.log


# -Dexec.workingdir="/test-data/javawebscripts" -Dexec.args="+x diff2.sh"
