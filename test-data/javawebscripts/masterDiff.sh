#!/bin/bash

passTest=0
soapServer="128.149.16.xxx:8080"

#start up the server
pkill -fn 'integration-test'
echo 'KILLING SERVER IF ONE IS RUNNING'
sleep 3s

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
	sleep 60s

	#run the diff script
	echo 'RUNNING OLD API DIFF SCRIPT'
	#./diff2.sh
	#passTest=$?

	echo 'RUNNING WORKSPACES DIFF SCRIPT'
        #./diffWorkspaces.sh
        #passTest=$?


        #connect to soapUI -- WORK STILL NEEDED
        echo 'RUNNING SOAP UI TESTS'
        #ssh $soapServer 'cd /classPath/; ./soapScript;'
        #classPath=??
        TestSuite="WorkspacesTesting"
        #TestCase="??"
        #./testrunner.sh -f ./soapTestData -s $TestSuite -c $TestCase $classpath
        cd ./soapStuff
	for i in $(ls . | grep "soapui-project.xml"); do
	         echo RUNNING TEST $i
                ./Resources/app/bin/testrunner.sh -s $TestSuite ./$i
        done

        #shutdown the tomcat server process
        pkill -fn 'integration-test'
        echo 'KILLING SERVER'

	#echo 'PASSTEST?'
        #echo "$passTest"
	#exit $passTest
fi

if [ $server -eq 2 ]; then
	echo 'SERVER TIME-OUT'
fi

