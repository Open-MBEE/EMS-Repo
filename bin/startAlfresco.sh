#!/bin/bash
#start server
if [ -e /etc/init.d/alfresco ]; then
  echo /etc/init.d/alfresco start
  if [[ "$test" -eq "0" ]]; then
    /etc/init.d/alfresco start
  fi
else
  echo /etc/init.d/tomcat start
  if [[ "$test" -eq "0" ]]; then
    /etc/init.d/tomcat start
  fi
fi

exit 0
