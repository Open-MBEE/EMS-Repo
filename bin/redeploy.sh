#!/bin/bash

usage="usage: sudo $0 repoAmpFile [repoWarFile] [shareAmpFile] [shareWarFile] [mmsappDir] "

# variable initialization
d=$(dirname $0)
installWarCommand=$d/installWar.sh
startAlfrescoCmd=$d/stopAlfresco.sh
stopAlfrescoCmd=$d/stopAlfresco.sh
deloyMmsappCmd=$d/deployMmsapp.sh

tomcatDir=/opt/local/apache-tomcat
if [ ! -f $tomcatDir ]; then
  tomcatDir=/opt/local/alfresco-4.2e/tomcat
fi
webappDir=${tomcatDir}/webapps
alfrescoWebappDir=${webappDir}/alfresco
shareWebappDir=${webappDir}/share
existingWarFile=${alfrescoWebappDir}.war
existingShareWarFile=${webappDir}/share.war
mmtJar=${tomcatDir}/bin/alfresco-mmt.jar
if [ ! -f $mmtJar ]; then
  mmtJar=${tomcatDir}/../bin/alfresco-mmt.jar
fi

mmsappDeployDir=${alfrescoWebappDir}/mmsapp
tmpDir=/tmp/mmsappZip

echo tomcatDir=$tomcatDir
echo webappDir=$webappDir
echo alfrescoWebappDir=$alfrescoWebappDir
echo existingWarFile=$existingWarFile
echo existingShareWarFile=$existingShareWarFile
echo mmtJar=$mmtJar

# process arguments
if [ "$#" -eq 0 ]; then
  echo "$0 : Error! Need at least one argument!"
  echo $usage
  exit 1
fi;

ampFile=$1
shareAmpFile=""
warfile=$existingWarFile
shareWarFile=$existingShareWarFile
mmsappDir=""
mmsappZip=""

# Look at substrings in the input to determine which input file is which
for var in "$@"
do
  if [[ $var == *share* ]]; then
    echo $var " is share"
    if [[ $var == *amp ]]; then
      echo $var " is amp"
      shareAmpFile=$var
    else
      if [[ $var == *war ]]; then
        echo $var " is war"
        shareWarFile=$var
      fi
    fi
  else
    if [[ $var == *repo* ]]; then
      echo $var " is repo"
      if [[ $var == *amp ]]; then
        echo $var " is amp"
        ampFile=$var
      else
        if [[ $var == *war ]]; then
          echo $var " is war"
          warFile=$var
        fi
      fi
    else
      if [[ $var == *mms* ]]; then
        echo $var " is mmsapp"
        if [[ $var == *zip ]]; then
          echo $var " is zip"
          mmsappZip=$var
        else
	  mmsappDir=$var
        fi
      else
        if [[ $var == *zip ]]; then
          echo $var " is zip"
          mmsappZip=$var
	fi
      fi
    fi
  fi
done

echo "arguments processed with the following assignments:"
echo "  ampFile=" $ampFile
echo "  shareAmpFile=" $shareAmpFile
echo "  warFile=" $warFile
echo "  shareWarFile=" $shareWarFile
echo "  mmsappDir=" $mmsappDir
echo "  mmsappZip=" $mmsappZip

exit 0

# stop alfresco server
$stopAlfrescoCmd

# install war files
if [ -f "$ampFile" ]; then
  echo $installWarCommand $mmtjar $ampFile $warFile $existingWarFile $alfrescoWebappDir
  $installWarCommand $mmtjar $ampFile $warFile $existingWarFile $alfrescoWebappDir
fi
if [ -f "$shareAmpFile" ]; then
  echo $installWarCommand $mmtjar $shareAmpFile $shareWarFile $existingShareWarFile $shareWebappDir
  $installWarCommand $mmtjar $shareAmpFile $shareWarFile $existingShareWarFile $shareWebappDir
fi

# deploy mmsapp
if [ -f "$ampFile" ]; then
  $deployMmsappCmd $mmsappDeployDir $mmsappDir $backupDir $mmsappZip
fi

#start server
$startAlrfescoCmd

exit 0
