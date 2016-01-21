#!/bin/bash

mvn clean -P purge
echo psql -U $USER -f ./src/main/java/gov/nasa/jpl/view_repo/db/mms.sql
psql -U $USER -f ./src/main/java/gov/nasa/jpl/view_repo/db/mms.sql
mvn jrebel:generate
./runserver.sh 
