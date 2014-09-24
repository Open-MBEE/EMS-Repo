#!/bin/bash
# kill alfresco server

pid=`ps auxwww | grep java | grep -v grep | grep -v artifactory | grep alfresco | cut -d' ' -f 2`
if [ ! "$pid" == "" ]; then
  pid=`ps -elf | grep java | grep -v grep | grep -v artifactory | grep alfresco | cut -d' ' -f 4`
fi

if [ "$pid" == "" ]; then
  echo "Could not find alfresco server to kill!"
  exit 1
fi

echo "kill $pid"
if [[ "$test" -eq "0" ]]; then
  kill $pid
fi

exit 0
