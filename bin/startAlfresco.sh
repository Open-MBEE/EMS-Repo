#!/bin/bash
#start server

#change test_mms to 1 to just see commands without running them
#change test_mms to 0 to run normally
if [ -z "$test_mms" ]; then
  export test_mms=1 # just test
  #export test_mms=0 # normal
fi


if [ -e /etc/init.d/alfresco ]; then
  echo /etc/init.d/alfresco start
  if [[ "$test_mms" -eq "0" ]]; then
    /etc/init.d/alfresco start
  fi
else
  echo /etc/init.d/tomcat start
  if [[ "$test_mms" -eq "0" ]]; then
    /etc/init.d/tomcat start
  fi
fi

exit 0
