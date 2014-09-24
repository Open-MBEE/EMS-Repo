#!/bin/bash
#start server
echo "##### start alfresco server"

#change test_mms to 1 to just see commands without running them
#change test_mms to 0 to run normally

if [ -z "$test_mms" ]; then
  export test_mms=1 # just test
  #export test_mms=0 # normal
fi

d=$(dirname "$0")
getPidCommand=$d/getAlfrescoPid.sh

pid=`${d}/getAlfrescoPid.sh`

if [ -n "$pid" ]; then
  echo "Alfreco server is still running as pid=$pid . . . not restarting!"
  exit 1
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
