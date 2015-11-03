#!/bin/bash

echo curl -w "%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/modules"
curl -w "%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/modules"
