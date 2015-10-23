#!/bin/bash

echo curl -w "%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/history/$1"
curl -w "%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/history/$1"


