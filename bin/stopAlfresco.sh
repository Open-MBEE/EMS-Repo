#!/bin/bash
# stop alfresco server
if [ -e /etc/init.d/alfresco ]; then
  echo "/etc/init.d/alfresco stop &"
  /etc/init.d/alfresco stop &
  sleep 5
  killCommand=${0/stop/kill}
  echo $killCommand
  $killCommand
else
  echo "/etc/init.d/tomcat stop"
  /etc/init.d/tomcat stop
fi

exit 0
