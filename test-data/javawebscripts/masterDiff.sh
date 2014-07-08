#!/bin/bash


passTest=0

#start up the server
#cd ./../..
./runserver.sh > serverLog.txt &
echo 'STARTING UP SERVER'
sleep 60s

#poll to see if the server is up
cd ./test-data/javawebscripts
server=0
serverCount=0
echo 'POLLING SERVER'
while [ $server -eq 0 ]; do
	> tempMasterDiff
	netstat -ln | grep '8080' > tempMasterDiff
	count=`sed -n '$=' tempMasterDiff`
	if [ $count -gt 0 ]; then
		server=1
	fi
	
	#time-out condition
	serverCount=$(($serverCount+1))
	if [ $serverCount -gt 50000 ];then
		server=2
	fi
done

if [ $server -eq 1 ]; then
	echo 'SERVER CONNECTED'
	sleep 10s

	#run the diff script
	echo 'RUNNING DIFF SCRIPT'
	./diff2.sh
	passTest=$?
	echo 'PASSTEST?'
	echo "$passTest"
	exit $passTest

fi
if [ $server -eq 2 ]; then
	echo 'SERVER TIME-OUT'
fi

#shutdown the tomcat server process
pkill -fn 'integration-test'
echo 'KILLING SERVER'
