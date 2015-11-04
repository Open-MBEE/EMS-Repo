#!/bin/bash

echo curl -w "%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/modules?mmsVersion=true"
curl -w "%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/modules?mmsVersion=true"
