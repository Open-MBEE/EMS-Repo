#!/bin/bash

mvn integration-test -Pamp-to-war -Dmaven.test.skip=true -Drebel.log=true | tee runserver.log