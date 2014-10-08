#!/bin/bash
# get the alfresco process id

pid=`ps auxwww | grep java | grep -v grep | grep -v artifactory | grep alfresco | cut -d' ' -f 2`
if [ "$pid" == "" ]; then
  pid=`ps -elf | grep java | grep -v grep | grep -v artifactory | grep alfresco | cut -d' ' -f 4`
fi

if [ "$pid" == "" ]; then
  tmpFile=/tmp/errrrr
  pidFile=`find /opt/local/ -name "*atalina.pid" 2> $tmpFile`
  grep -v 'Permission denied' $tmpFile > /dev/stderr
  rm $tmpFile
  #echo "pidFile = $pidFle"
  if [ -f "$pidFile" ]; then
    pid=`cat $pidFile`
  fi
fi

if [ "$pid" == "" ]; then
  exit 1
fi

echo $pid

exit 0
