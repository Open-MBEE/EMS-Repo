#!/bin/sh

export CURL_STATUS="-w \"%{http_code}\""
export CURL_USER=" -u admin:admin"

export CURL_SECURITY="-k -3" 

export CURL_FLAGS=$CURL_STATUS$CURL_USER
export CURL_POST_FLAGS="-X POST -H \"Content-Type:application/json\" --data"
export CURL_PUT_FLAGS="-X PUT"
export CURL_GET_FLAGS="-X GET"
export BASE_URL="\"http://localhost:8080/view-repo/service/javawebscripts/"

# TODO: CURL commands aren't executed from bash using environment variables
echo POSTS
# create project and site
echo curl $CURL_FLAGS $CURL_POST_FLAGS \'{\"name\":\"View Repo Test\"}\' $BASE_URL"sites/europa/projects/123456?fix=true&createSite=true\""

# post elements to project
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/elements.json $BASE_URL"sites/europa/projects/123456/elements\""

# post views
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/views.json $BASE_URL"views\""

# post comments (can only add these to a particular view - though view isn't really checked at the moment)
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/comments.json $BASE_URL"view/301/elements\""

# post products
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/products.json $BASE_URL"products\""

echo ""
echo GET
# get project - should just return 200
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"sites/europa/projects/123456\""

# get elements
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"elements/123456?recurse=true\""

# get views
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"views/301\""

# get view elements
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"views/301/elements\""

# get comments for element
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"elements/303/comments\""

# get product
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"products/301\""

# get moaproducts
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"moaproducts/301?format=json\""

# get product list
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"productlist/europa?format=json\""

# TODO - fix these URLs? change search to just search
# get search
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"element/search?keyword=some*\""

echo ""
echo POST changes

# post changes to directed relationships only (without owners)
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/directedrelationships.json $BASE_URL"sites/europa/projects/123456/elements\""

# get changed element to see if source/target changed
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"elements/400\""
