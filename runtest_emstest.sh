#!/bin/bash

mvn -Drebel.log=true -Dmaven.test.skip=false -Dtest=EmsSystemModelTest test | tee runtests.log

