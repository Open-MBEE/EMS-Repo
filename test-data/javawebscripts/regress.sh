#!/bin/bash

echo "pushd ~/git/alfresco-view-repo; python test-data/javawebscripts/waitOnServer.py; python test-data/javawebscripts/regression_test_harness.py $@; popd"
pushd ~/git/alfresco-view-repo; python test-data/javawebscripts/waitOnServer.py; python test-data/javawebscripts/regression_test_harness.py $@; popd


