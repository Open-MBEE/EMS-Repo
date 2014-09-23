#!/bin/bash
# kill alfresco server

#change test_mms to 1 to just see commands without running them
#change test_mms to 0 to run normally
if [ -z "$test_mms" ]; then
  export test_mms=1 # just test
  #export test_mms=0 # normal
fi

pid=`ps auxwww | grep java | grep -v grep | grep -v artifactory | grep alfresco | cut -d' ' -f 2`
if [ "$pid" == "" ]; then
  pid=`ps -elf | grep java | grep -v grep | grep -v artifactory | grep alfresco | cut -d' ' -f 4`
fi
if [ "$pid" == "" ]; then
  pidFile=`find /opt/local/ -name "*atalina.pid"`
  if [ -f $pidFile ]; then
    pid=$(cat $pidFile)
  fi
fi

if [ "$pid" == "" ]; then
  echo "Could not find alfresco server to kill!"
  exit 1
fi

echo "kill $pid"
if [[ "$test_mms" -eq "0" ]]; then
  kill $pid
fi

exit 0
