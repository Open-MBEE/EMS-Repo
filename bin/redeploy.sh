#!/bin/bash

usage() {
  echo "usage: sudo $(basename $0) repoAmpFile|shareAmpFile [shareAmpFile|repoAmpFile] [repoWarFile] [shareWarFile] [mmsappDir] [mmsappZip]"
  echo
  echo "The order of arguments is not important. The $(basename $0) script may be called from anywhere, but the scripts on which it depends (installWar.sh, startAlfresco.sh, stopAlfresco.h, and deployMmsapp.sh) must be in the same directory as $(basename $0)."
  echo
}

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

echo
echo initialized directories and files:
echo "  tomcatDir =" $tomcatDir
echo "  webappDir =" $webappDir
echo "  alfrescoWebappDir =" $alfrescoWebappDir
echo "  existingWarFile =" $existingWarFile
echo "  existingShareWarFile =" $existingShareWarFile
echo "  mmtJar =" $mmtJar

# process arguments
if [ "$#" -eq 0 ]; then
  echo "$0 : Error! Need at least one argument!"
  usage
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
  if [[ $var == *zip ]]; then
    mmsappZip=$var
  else
    if [[ $var == *share* ]]; then
      if [[ $var == *amp ]]; then
        shareAmpFile=$var
      else
        if [[ $var == *war ]]; then
          shareWarFile=$var
        else
          if [[ -d "$var" ]]; then
            mmsappDir=$var
          else
            echo "Unknown argument! " $var 
          fi
        fi
      fi
    else
#      if [[ ( $var == *repo* ) || ( $var == alfresco* ) ]]; then
        if [[ $var == *amp ]]; then
          ampFile=$var
        else
          if [[ $var == *war ]]; then
            warFile=$var
#          fi
#        fi
          else
            if [[ -d "$var" ]]; then
              mmsappDir=$var
            else
              echo "Unknown argument! " + $var 
            fi
          fi
        fi
    fi
  fi
done

echo
echo "arguments processed with the following assignments:"
echo "  ampFile =" $ampFile
echo "  shareAmpFile =" $shareAmpFile
echo "  warFile =" $warFile
echo "  shareWarFile =" $shareWarFile
echo "  mmsappDir =" $mmsappDir
echo "  mmsappZip =" $mmsappZip

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
