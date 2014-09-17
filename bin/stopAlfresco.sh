#!/bin/bash
# stop alfresco server

d=$(dirname $0)
killCommand=$d/killAlfresco.sh

if [ -e /etc/init.d/alfresco ]; then
  echo "/etc/init.d/alfresco stop &"
  /etc/init.d/alfresco stop &
  echo sleep 5
  sleep 5
  echo $killCommand
  $killCommand
else
  echo "/etc/init.d/tomcat stop"
  /etc/init.d/tomcat stop
fi

exit 0
