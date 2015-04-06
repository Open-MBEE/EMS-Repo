#!/bin/bash
viewRepoDir=../../../alfresco-view-repo
echo "pushd ${viewRepoDir}; python test-data/javawebscripts/waitOnServer.py; python test-data/javawebscripts/regression_test_harness.py $@; popd"
pushd ${viewRepoDir}; python test-data/javawebscripts/waitOnServer.py; python test-data/javawebscripts/regression_test_harness.py $@; popd


