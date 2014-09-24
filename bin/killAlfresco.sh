#!/bin/bash
# kill alfresco server

#change test_mms to 1 to just see commands without running them
#change test_mms to 0 to run normally
if [ -z "$test_mms" ]; then
  export test_mms=1 # just test
  #export test_mms=0 # normal
fi

d=$(dirname "$0")
getPidCommand=$d/getAlfrescoPid.sh

pid=`${d}/getAlfrescoPid.sh`

if [ "$pid" == "" ]; then
  echo "Could not find alfresco server to kill!"
  exit 0
fi

echo "kill $pid"
if [[ "$test_mms" -eq "0" ]]; then
  kill $pid
fi

exit 0
