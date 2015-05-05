#!/bin/sh
# run this from the alfresco-view-repo directory
pushd
cd ../util
ant
cd ../sysml
ant
cd ../bae
ant
cd ../K
ant
#cd ../emsscala
#ant
popd

