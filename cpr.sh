#!/bin/bash

mvn clean -P purge
echo psql -U $USER -f ./src/main/java/gov/nasa/jpl/view_repo/db/mms.sql
psql -U mmsuser -f ./src/main/java/gov/nasa/jpl/view_repo/db/mms.sql
#Configurations that need a suffix with a database name to run psql command
psql -U mmsuser -f ./src/main/java/gov/nasa/jpl/view_repo/db/mms.sql mms
psql -U mmsuser -f ./src/main/java/gov/nasa/jpl/view_repo/db/doors-mms.sql mms
mvn jrebel:generate
./runserver.sh 
