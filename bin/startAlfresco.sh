#!/bin/bash
#start server
if [ -e /etc/init.d/alfresco ]; then
  echo /etc/init.d/alfresco start
  /etc/init.d/alfresco start
else
  echo /etc/init.d/tomcat start
  /etc/init.d/tomcat start
fi

exit 0
