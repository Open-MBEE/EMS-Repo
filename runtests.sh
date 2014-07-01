#!/bin/bash

mvn -Drebel.log=true -Dmaven.test.skip=false test | tee runtests.log

