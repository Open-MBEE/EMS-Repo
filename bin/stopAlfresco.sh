#!/bin/bash
# stop alfresco server

d=$(dirname $0)
killCommand=$d/killAlfresco.sh

if [ -e /etc/init.d/alfresco ]; then
  echo "/etc/init.d/alfresco stop &"
  if [[ "$test" -eq "0" ]]; then
    /etc/init.d/alfresco stop &
  fi
  echo sleep 5
  sleep 5
  echo $killCommand
  #if [[ "$test" -eq "0" ]]; then
    $killCommand
  #fi
else
  echo "/etc/init.d/tomcat stop"
  if [[ "$test" -eq "0" ]]; then
    /etc/init.d/tomcat stop
  fi
fi

exit 0
