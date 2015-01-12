#!/bin/bash

# make sure checksum for patched jar is correct
f1=`/bin/ls -1 ./target/mms-repo-*war/WEB-INF/lib/alfresco-repository-*.jar`
f2=`/bin/ls -1 ./src/main/amp/web/WEB-INF/lib/alfresco-repository-*.jar`
if [ "$f1" = "" ]; then
  echo no target file
  exit 0
fi
if [ "$f2" = "" ]; then
  echo no src file
  exit 0
fi

cso1=`md5sum $f1`
cso2=`md5sum $f2`
cs1=`echo $cso1 | cut -d' ' -f 1`
cs2=`echo $cso2 | cut -d' ' -f 1`
#cs2=`md5sum ./src/main/amp/web/WEB-INF/lib/alfresco-repository-*.jar | cut -d' ' -f 1`
echo $f1
echo $f2
echo $cso1
echo $cso2
echo $cs1
echo $cs2
if [ "cs_$cs1" = "cs_$cs2" ]; then
  echo "checksum matched!"
else
  echo "alfresco-repository jar checksums on do not match!"
  exit 1
fi
exit 0
# 761054791893eee91a0871903139f5fb  /home/bclement/git/alfresco-view-repo/src/main/amp/web/WEB-INF/lib/alfresco-repository-4.2.e.jar

