#!/bin/bash

mvn clean -P purge
psql -U mmsuser -f ./src/main/java/gov/nasa/jpl/view_repo/db/mms.sql
psql -U mmsuser -f ./src/main/java/gov/nasa/jpl/view_repo/db/mms.sql mydb
mvn jrebel:generate
./runserver.sh 