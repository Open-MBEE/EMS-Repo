#!/bin/bash
# 
# This script changes the localhost port and the debug port of the MMS alfrescoi
# view repo in other scripts for launching and testing the MMS locally.
#
# Source this file since it changes the MAVEN_OPTS environment variable.
# If you don't source it, it will not change MAVEN_OPTS.
#
# Here are some examples:
#
# > source ./switchPorts.sh 9091 10002
# > . ./switchPorts.sh 8080


if [ "$#" -lt 1 ]; then
  echo "usage> source $0 alfresco_port [debug_port]"
else

#oldPort=$ALFRESCO_PORT
newPort=$1
#echo "newPort = $newPort"
scripts="configdelete configget configpost curl.tests2.sh curl.tests.sh diff2.sh diffWorkspaceDWdev.sh diffWorkspaceDW.sh diffWorkspaceLIMBOdev.sh diffWorkspaceLIMBO.sh diffWorkspace.sh diffWorkspaceWORKSdev.sh diffWorkspaceWORKS.sh expeval exppost fixconstraint localCurlParams mms.curl.tests.sh modeldelete modelget modelmerge modelpost productget productpost productpost.sh projectadd projectget projectpost projectpost.sh regression_lib.py secureCurlParams snapshotdelete snapshotget snapshotpost viewget viewpost viewpost.sh wsdelete wsdiff wsdiffull wsdiffmerge wsget wspost move generateView"

#echo "scripts = ${scripts}"

sed -i "s/\(tomcat.port=\)[0-9]\+/\1$newPort/" runserver.sh
#echo runserver
#sed -i "s/\(localhost:\)[0-9]\+/\1$newPort/" test-data/javawebscripts/regression_lib.py
pushd test-data/javawebscripts
#echo "sed -i \"s/\(localhost:\)[0-9]\+/\1$newPort/\" $scripts"
sed -i "s/\(localhost:\)[0-9]\+/\1$newPort/" $scripts
popd
#echo "done with scripts"

# Assign RMI ports

if [ "$newPort" == "8080" ]; then
    # comment out the assigment of rmi ports to 0
    sed -i "s/^\([a-z].*[.]rmi[.].*=0\)/#\1/" src/test/properties/local/alfresco-global.properties
else
    # uncomment the assigment of rmi ports to 0
    sed -i "s/^#\([a-z].*[.]rmi[.].*=0\)/\1/" src/test/properties/local/alfresco-global.properties
fi


if [ "$#" -gt 1 ]; then

  # Set debug port

  newDebugPort=$2
  #usedDebugPorts=`netstat -anp | grep 1000 | cut -d':' -f 2 | cut -d' ' -f 1`

  # Set debug port in $MAVEN_OPTS 
  newMavenOpts=`echo $MAVEN_OPTS | sed "s@\(address=\)[0-9]\+@\1$newDebugPort@"`
  export MAVEN_OPTS=$newMavenOpts

  sed -i "s/\(key=\"port\" value=\"\)[0-9]\+/\1$newDebugPort/" view-repo.launch

fi

fi

