#!/bin/bash

mvn clean
mvn jrebel:generate
./runserver.sh 

