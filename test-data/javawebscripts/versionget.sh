#!/bin/bash

#echo curl -w "%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/workspaces/master/elements/$1?mmsVersion=true"
#curl -w "%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/workspaces/master/elements/$1?mmsVersion=true"
echo curl -w "%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/mmsversion"
curl -w "%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/mmsversion"
