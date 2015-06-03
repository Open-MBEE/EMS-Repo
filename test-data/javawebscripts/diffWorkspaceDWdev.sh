#!/bin/bash
#Script for running all of the curl commands.  Put into one script so that you can
# easily change the server/user preferences.

mkdir -p outputWorkspaces

failedTest=0

export CURL_STATUS='-w \n%{http_code}\n'
export CURL_POST_FLAGS_NO_DATA="-X POST"
export CURL_POST_FLAGS='-X POST -H Content-Type:application/json --data'
export CURL_PUT_FLAGS="-X PUT"
export CURL_GET_FLAGS="-X GET"

#export CURL_SECURITY=" -k -3"

#if [true]; then
       export CURL_USER=" -u admin:admin"
       export CURL_FLAGS=$CURL_STATUS$CURL_USER
       export SERVICE_URL="http://localhost:8080/alfresco/service/"
       export BASE_URL="http://localhost:8080/alfresco/service/workspaces/master/"
#else
#        export CURL_USER=" -u shatkhin"
#        export CURL_FLAGS=$CURL_STATUS$CURL_USER$CURL_SECURITY
#        export SERVICE_URL="https://europaems-dev-staging-a/alfresco/service/" 
#       export BASE_URL="http://europaems-dev-staging-a:8443/alfresco/service/javawebscripts/"
#        export BASE_URL="https://europaems-dev-staging-a/alfresco/service/javawebscripts/"
#fi

###################################    POST CURL COMMANDS   ############################################

echo 'testPost 4'
# post comments (can only add these to a particular view - though view isn't really checked at the moment)
echo
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/comments.json $BASE_URL"elements"  
echo
curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/comments.json $BASE_URL"elements"  | grep -v '"read":'| grep -v '"lastModified"' | grep -v '"sysmlid"'| grep -v '"author"' > outputWorkspaces/post4.json
DIFF=$(diff -I 'author' baselineWorkspaces/post4.json outputWorkspaces/post4.json | grep -v '"author"')
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo


####################################     	GET CURL COMMANDS                ###########################################


echo 'testGET9'
# get search
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"element/search?keyword=some*\""
curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"element/search?keyword=some*" | grep -v '"read":'| grep -v '"lastModified"' | grep -v '"sysmlid"' > outputWorkspaces/get9.json
DIFF=$(diff baselineWorkspaces/get9.json outputWorkspaces/get9.json)
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo



####################################    	POST CHANGE CURL COMMANDS        ###########################################



####################################   		CONFIGURATIONS CURL COMMANDS     ###########################################


# post configuration
echo 'testCONFIG1'
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/configuration.json $BASE_URL"sites/europa/configurations\""
curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/configuration.json $BASE_URL"sites/europa/configurations" | grep -v '"read":' | grep -v '"lastModified"' | grep -v '"sysmlid"' > outputWorkspaces/config1.json
DIFF=$(diff baselineWorkspaces/config1.json outputWorkspaces/config1.json | egrep -v "[0-9]+[c|a|d][0-9]+" | grep -ve '---' | grep -v 'time')
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo


# get configurations
echo 'testCONFIG2'
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"sites/europa/configurations\""
curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"sites/europa/configurations" | grep -v '"read":' | grep -v '"modified"' | grep -v '"sysmlid"' > outputWorkspaces/config2.json

#as long as outputs/baselines match these regex - outputWorkspaces is conceptually correct
grep -vE '"id":*' outputWorkspaces/config2.json | grep -vE '"url": "/alfresco/service/snapshots/*' | grep -vE '"created":'  > baselineWorkspaces/tempConfig2_1.json
grep -vE '"read":' baselineWorkspaces/config2.json  | grep -v '"modified"' | grep -v '"sysmlid"' | grep -vE '"id":*'| grep -vE '"url": "/alfresco/service/snapshots/*' | grep -vE '"created":'  > baselineWorkspaces/tempConfig2_2.json
DIFF=$(diff baselineWorkspaces/tempConfig2_2.json baselineWorkspaces/tempConfig2_1.json)
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo

####################################   		ADDED CURL COMMANDS     ##########################################

exit $failedTest