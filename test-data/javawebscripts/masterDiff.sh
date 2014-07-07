#!/bin/bash

#start up the server
cd ./../..
./runserver.sh &
echo 'STARTING UP SERVER'
sleep 10s

#poll to see if the server is up
cd ./test-data/javawebscripts
server=0;
echo 'POLLING SERVER'
while [ $server -eq 0 ]; do
	netstat -ln | grep '8080' > tempMasterDiff
	count=`sed -n '$=' tempMasterDiff`
	if [ $count -gt 0 ]; then
		server=1;
	fi
done
echo 'SERVER CONNECTED'
sleep 10s

#run the diff script
echo 'RUNNING DIFF SCRIPT'
./diff2.sh
