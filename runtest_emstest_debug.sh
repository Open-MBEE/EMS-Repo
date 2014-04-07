#!/bin/bash

mvn -Dmaven.surefire.debug -Drebel.log=true -Dmaven.test.skip=false -Dtest=EmsSystemModelTest test | tee runtests.log

