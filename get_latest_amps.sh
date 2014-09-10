#!/bin/bash

if [ -a /opt/local/apache-tomcat ]; then
    export path="/opt/local/apache-tomcat/amps"
    export owner="tomcat:jpl"
else
    if [ -a /opt/local/alfresco-4.2.e ]; then
        export path="/opt/local/alfresco-4.2.e/amps"
        export owner="alfresco:jpl"
    else
        export path="."
    fi
fi

echo "path: $path"

echo "determining latest version for view-share"
export share_latest=`curl -s http://europambee-build:8082/artifactory/libs-release-local/gov/nasa/jpl/view-share/maven-metadata.xml | grep latest | sed 's/<latest>//g' | sed 's/<\/latest>//g' | sed 's/ //g'`
echo "downloading view-share-$share_latest.amp..."

if [ -a $path/view-share-$share_latest.amp ]; then
  echo "view-share-$share_latest.amp already exists"
else
  curl -s http://europambee-build:8082/artifactory/libs-release-local/gov/nasa/jpl/view-share/$share_latest/view-share-$share_latest.amp > $path/view-share-$share_latest.amp
  if [ -n $owner ]; then
    chown $owner $path/view-share-$share_latest.amp
  fi
  echo "completed download"
fi

echo "determining latest version for view-repo"
export repo_latest=`curl -s http://europambee-build:8082/artifactory/libs-release-local/gov/nasa/jpl/view-repo/maven-metadata.xml | grep latest | sed 's/<latest>//g' | sed 's/<\/latest>//g' | sed 's/ //g'`
echo "downloading view-repo-$repo_latest.amp..."

if [ -a $path/view-repo-$repo_latest.amp ]; then
  echo "view-repo-$repo_latest.amp already exists"
else
  curl -s http://europambee-build:8082/artifactory/libs-release-local/gov/nasa/jpl/view-repo/$repo_latest/view-repo-$repo_latest.amp > $path/view-repo-$repo_latest.amp
  if [ -n $owner ]; then
    chown $owner $path/view-repo-$repo_latest.amp
  fi
  echo "completed download"
fi
