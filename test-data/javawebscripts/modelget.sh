#!/bin/bash

curl -w "%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/view-repo/service/javawebscripts/elements/$1$2"
