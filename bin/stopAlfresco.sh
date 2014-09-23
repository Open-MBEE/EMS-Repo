#!/bin/bash
# stop alfresco server

#change test_mms to 1 to just see commands without running them
#change test_mms to 0 to run normally
if [ -z "$test_mms" ]; then
  #export test_mms=1 # just test
  export test_mms=0 # normal
fi

d=$(dirname $0)
killCommand=$d/killAlfresco.sh

if [ -e /etc/init.d/alfresco ]; then
  echo "/etc/init.d/alfresco stop &"
  if [[ "$test_mms" -eq "0" ]]; then
    /etc/init.d/alfresco stop &
  fi
  echo sleep 5
  sleep 5
  echo $killCommand
  #if [[ "$test_mms" -eq "0" ]]; then
    $killCommand
  #fi
else
  echo "/etc/init.d/tomcat stop"
  if [[ "$test_mms" -eq "0" ]]; then
    /etc/init.d/tomcat stop
  fi
fi

exit 0
