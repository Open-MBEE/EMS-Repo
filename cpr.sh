#!/bin/bash

mvn clean -P purge
mvn jrebel:generate
./runserver.sh 

