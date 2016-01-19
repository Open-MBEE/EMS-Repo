#!/bin/bash

mvn clean -P purge
psql -U mmsuser -f ./src/main/java/gov/nasa/jpl/view_repo/db/mms.sql
mvn jrebel:generate
./runserver.sh 