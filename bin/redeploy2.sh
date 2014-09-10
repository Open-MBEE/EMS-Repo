#!/bin/bash

usage="usage: sudo $0 ampFile [warFile [mmsappDir]] "

# variable initialization
tomcatDir=/opt/local/apache-tomcat
#tomcatDir=/opt/local/alfresco-4.2.e/tomcat
webappDir=${tomcatDir}/webapps
alfrescoWebappDir=${webappDir}/alfresco
existingWarFile=${alfrescoWebappDir}.war
mmtJar=${tomcatDir}/bin/alfresco-mmt.jar
#mmtJar=${tomcatDir}/../bin/alfresco-mmt.jar

echo 0=$0
echo 1=$1
echo 2=$2
echo 3=$3
echo tomcatDir=$tomcatDir
echo webappDir=$webappDir
echo alfrescoWebappDir=$alfrescoWebappDir
echo existingWarFile=$existingWarFile
echo mmtJar=$mmtJar

# process arguments
if [ "$#" -eq 0 ]; then
  echo "$0 : Error! Need at least one argument!"
  echo $usage
  exit 1
fi;
ampFile=$1
if [ "$#" -eq 2 ]; then
  warFile=$2
else
  warfile=$existingWarFile
fi;

if [ "$#" -eq 3 ]; then
  mmsappDir=$3
else
  mmsappDir=/home/cinyoung/mmsapp
fi;

echo ampFile=$ampFile
echo warFile=$warFile
echo mmsappDir=$mmsappDir

# backup war file
echo cp $existingWarFile ${existingWarFile}.`date '+%Y%m%d'`
cp $existingWarFile ${existingWarFile}.`date '+%Y%m%d'`
# use specified warFile
echo cp -f $warFile $existingWarFile
cp -f $warFile $existingWarFile

# install amp to war
echo java -jar $mmtJar install $ampFile $existingWarFile -force
java -jar $mmtJar install $ampFile $existingWarFile -force

# owner must be tomcat
echo chown tomcat:tomcat $existingWarFile
chown tomcat:tomcat $existingWarFile

# blast alfresco directory
echo rm -rf $alfrescoWebappDir
rm -rf $alfrescoWebappDir

# owner must be tomcat
echo "amp installed!"
echo "restart:\n\t\tsudo /etc/init.d/tomcat restart"
echo "after alfresco dir is recreated, copy mmsapp:\n\t\tcp -pRf $mmsappDir $alfrescoWebAppDir"
echo "then, make sure the mmsapp's owner is tomcat:\n\t\tchown -Rh tomcat:tomcat ${alfrescoWebappDir}/$mmsappDir"

exit 0

~                                                                                                  
~                                                                                                  
~                     
