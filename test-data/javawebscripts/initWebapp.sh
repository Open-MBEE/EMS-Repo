#!/bin/bash
#curl -w \n%{http_code}\n -u admin:admin -X POST -H Content-Type:application/json --data @JsonData/presentElemClassifiers.json http://localhost:8080/alfresco/service/workspaces/master/elements
echo "Executing: ./modelpost presentElemClassifiers.json"
./modelpost presentElemClassifiers.json 
