#!/bin/bash
# Bash file containing CURL commands generated from MMSCurlTestsGenerator.py. Based on curl.tests.sh and diff2.sh.

mkdir -p TestsOutput
passedTest=0

export CURL_STATUS='-w \n%{http_code}\n'
export CURL_POST_FLAGS_NO_DATA="-X POST"
export CURL_POST_FLAGS='-X POST -H Content-Type:application/json --data'
export CURL_PUT_FLAGS="-X PUT"
export CURL_GET_FLAGS="-X GET"

export CURL_USER=" -u admin:admin"
export CURL_FLAGS=$CURL_STATUS$CURL_USER
export SERVICE_URL="http://localhost:8080/alfresco/service/"



######################################## GET REQUESTS ########################################
echo
echo GET REQUESTS:

# get {path}?cs={cs?}&amp;extension={extension?}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/artifacts/Test_path?cs={cs?}&amp;extension={extension?}\"

# get checklogin
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/checklogin\"

# get debug?on={on?}&amp;off={off?}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/debug?on={on?}&amp;off={off?}\"

# get demo
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/demo\"

# get java_query
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/java_query\"

# get {siteName}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/javawebscripts/configurations/Europa_Regression_Test\"

# get search?keyword={keyword}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/javawebscripts/element/search?keyword=Test_keyword\"

# get {elementid}?recurse={recurse?}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/javawebscripts/elements/Test_elementid?recurse={recurse?}\"

# get comments
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/javawebscripts/elements/Test_id/comments\"

# get {id}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/javawebscripts/products/Test_id\"

# get {projectId}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/javawebscripts/projects/Regression_Test_Project\"

# get commits
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/javawebscripts/sites/Europa_Regression_Test/commits\"

# get {projectId}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/javawebscripts/sites/Europa_Regression_Test/projects/Regression_Test_Project\"

# get {id}?recurse={recurse?}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/javawebscripts/views/Test_id?recurse={recurse?}\"

# get elements?recurse={recurse?}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/javawebscripts/views/Test_modelid/elements?recurse={recurse?}\"

# get logout
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/logout\"

# get info
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/logout/info\"

# get sites
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/rest/sites\"

# get comments?recurse={recurse?}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/rest/views/Test_viewid/comments?recurse={recurse?}\"

# get {viewid}?recurse={recurse?}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/rest/views/Test_viewid?recurse={recurse?}\"

# get {site}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/ve/configurations/Test_site\"

# get {id}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/ve/documents/Test_id\"

# get {id}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/ve/index/Test_id\"

# get {id}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/ve/products/Test_id\"

# get {artifactId}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/workspaces/master/artifacts/Test_artifactId\"

# get {elementId}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/workspaces/master/elements/Test_elementId\"

# get configurations
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/workspaces/master/sites/Regression_Test_Site/configurations\"

# get {configurationId}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/workspaces/master/sites/Regression_Test_Site/configurations/Test_configurationId\"

# get products
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/workspaces/master/sites/Regression_Test_Site/configurations/Test_configurationId/products\"

# get snapshots
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/workspaces/master/sites/Regression_Test_Site/configurations/Test_configurationId/snapshots\"

# get products
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/workspaces/master/sites/Regression_Test_Site/products\"

# get snapshots
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/workspaces/master/sites/Regression_Test_Site/products/Test_productId/snapshots\"

# get {projectId}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"/workspaces/master/sites/Regression_Test_Site/projects/Regression_Test_Project\"
echo

######################################## POST REQUESTS ########################################
echo
echo POST REQUESTS:

# Post {path}?cs={cs?}&amp;extension={extension?}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/artifacts/Test_path?cs={cs?}&amp;extension={extension?}\"

# Post demo
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/demo\"

# Post {siteName}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/javawebscripts/configurations/Europa_Regression_Test\"

# Post {elementid}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/javawebscripts/elements/Test_elementid\"

# Post products
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/javawebscripts/products\"

# Post {projectId}?fix={fix?}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/javawebscripts/projects/Regression_Test_Project?fix={fix?}\"

# Post {commitId}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/javawebscripts/sites/Europa_Regression_Test/commits/Test_commitId\"

# Post elements?background={background?}?fix={fix?}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/javawebscripts/sites/Europa_Regression_Test/projects/Regression_Test_Project/elements?background={background?}?fix={fix?}\"

# Post {projectId}?delete={delete?}&amp;fix={fix?}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/javawebscripts/sites/Europa_Regression_Test/projects/Regression_Test_Project?delete={delete?}&amp;fix={fix?}\"

# Post views
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/javawebscripts/views\"

# Post elements
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/javawebscripts/views/Test_modelid/elements\"

# Post committed
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/rest/comments/committed\"

# Post {docid}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/rest/projects/document/Test_docid\"

# Post delete
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/rest/projects/document/Test_docid/delete\"

# Post delete
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/rest/projects/volume/Test_volid/delete\"

# Post {projectid}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/rest/projects/Test_projectid\"

# Post comments?recurse={recurse?}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/rest/views/Test_viewid/comments?recurse={recurse?}\"

# Post hierarchy
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/rest/views/Test_viewid/hierarchy\"

# Post {viewid}?force={force?}&amp;recurse={recurse?}&amp;doc={doc?}&amp;product={product?}&amp;user={user}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/rest/views/Test_viewid?force={force?}&amp;recurse={recurse?}&amp;doc={doc?}&amp;product={product?}&amp;user=Test_user\"

# Post elements
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/workspaces/master/elements\"

# Post {artifactId}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/workspaces/master/sites/Regression_Test_Site/artifacts/Test_artifactId\"

# Post configurations
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/workspaces/master/sites/Regression_Test_Site/configurations\"

# Post products
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/workspaces/master/sites/Regression_Test_Site/configurations/Test_configurationId/products\"

# Post snapshots
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/workspaces/master/sites/Regression_Test_Site/configurations/Test_configurationId/snapshots\"

# Post products
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/workspaces/master/sites/Regression_Test_Site/products\"

# Post snapshots
echo curl $CURL_FLAGS $CURL_POST_FLAGS $BASE_URL @JsonData/[TBD :D ]"/workspaces/master/sites/Regression_Test_Site/products/Test_productId/snapshots\"
echo

######################################## DELETE REQUESTS ########################################
echo
echo DELETE REQUESTS:

# get {id}
echo curl $CURL_FLAGS $CURL_DELETE_FLAGS $BASE_URL"/javawebscripts/elements/Test_id\"

# get {id}?project={projectid?}
echo curl $CURL_FLAGS $CURL_DELETE_FLAGS $BASE_URL"/javawebscripts/vieweditor/Test_id?project={projectid?}\"
echo