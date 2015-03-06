#!/bin/bash
#
# Source this file since it changes the MAVEN_OPTS environment variable.
# If you don't source it, it will not change MAVEN_OPTS.
#
# Here are some examples:
#
# > source ./switchPorts.sh 9091 10002
# > . ./switchPorts.sh 8080


if [ "$#" -lt 1 ]; then
  echo "usage> bash $0 alfresco_port [debug_port]"
  exit 1
fi
#oldPort=$ALFRESCO_PORT
newPort=$1

sed -i "s/\(localhost:\)[0-9]\+/\1$newPort/" test-data/javawebscripts/regression_lib.py
sed -i "s/\(tomcat.port=\)[0-9]\+/\1$newPort/" runserver.sh

# Assign RMI ports

if [ "$newPort" == "8080" ]; then
    # comment out the assigment of rmi ports to 0
    sed -i "s/^\([a-z].*[.]rmi[.].*=0\)/#\1/" src/test/properties/local/alfresco-global.properties
else
    # uncomment the assigment of rmi ports to 0
    sed -i "s/^#\([a-z].*[.]rmi[.].*=0\)/\1/" src/test/properties/local/alfresco-global.properties
fi


if [ "$#" -lt 2 ]; then
  exit 0
fi


# Set debug port

newDebugPort=$2
#usedDebugPorts=`netstat -anp | grep 1000 | cut -d':' -f 2 | cut -d' ' -f 1`

# Set debug port in $MAVEN_OPTS 
newMavenOpts=`echo $MAVEN_OPTS | sed "s@\(address=\)[0-9]\+@\1$newDebugPort@"`
export MAVEN_OPTS=$newMavenOpts

sed -i "s/\(key=\"port\" value=\"\)[0-9]\+/\1$newDebugPort/" view-repo.launch
