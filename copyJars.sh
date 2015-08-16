#!/bin/sh
# run this from the alfresco-view-repo directory
pushd
cd ../util
ant
cd ../sysml
ant
cd ../bae
ant
cd ../KLang
ant
#cd ../emsscala
#ant
popd

