#!/bin/bash

#if [ "$#" -eq 2 ] && [ $2 = 'recursive' ]; then
  echo curl -w "%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/workspaces/master/history/$1"
  curl -w "%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/workspaces/master/history/$1"
#elif [ "$#" -eq 2 ]; then
#  echo curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/$1/history/$2"
#  curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/$1/history/$2"
#else
#  echo "wrong number of args"
#  echo "usage: $0 [<workspace>] <elementId>"
#fi;


