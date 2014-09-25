#!/bin/bash

usage() {
  echo
  echo "usage: sudo $(basename $0) repoAmpFile|shareAmpFile [shareAmpFile|repoAmpFile] [repoWarFile] [shareWarFile] [mmsappDir] [mmsappZip]"
  echo
  echo "The order of arguments is not important. The $(basename $0) script may be called from anywhere, but the scripts on which it depends (installWar.sh, startAlfresco.sh, stopAlfresco.h, and deployMmsapp.sh) must be in the same directory as $(basename $0)."
  echo
  echo
}

# Change test_mms to 1 to just see commands without running them.
# Change test_mms to 0 to run normally.
# An existing test_mms environment variable overrides setting the value here.
if [ -z "$test_mms" ]; then
  #export test_mms=1 # just test
  export test_mms=0 # normal
fi

echo
if [[ "$test_mms" -eq "0" ]]; then
  echo "running $0 normally"
else
  echo "running $0 in test mode; will not affect server"
fi
echo

#This below doesn't work.
# BUT it probably would if using !-2 instead of !!
#checkLastCommand() {
#  if [ "$?" -ne "0" ]; then
#    echo "$0: ERROR! command failed! \"!!\""
#    exit 1
#  fi
#}


# variable initialization
d=$(dirname "$0")
installWarCommand=$d/installWar.sh
startAlfrescoCmd=$d/startAlfresco.sh
stopAlfrescoCmd=$d/stopAlfresco.sh
deployMmsappCmd=$d/deployMmsapp.sh

tomcatDir=/opt/local/apache-tomcat
if [ ! -f $tomcatDir ]; then
  tomcatDir=/opt/local/alfresco-4.2.e/tomcat
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

ampFile=""
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
echo "### arguments for $0 processed with the following assignments:"
echo "  ampFile =" $ampFile
echo "  shareAmpFile =" $shareAmpFile
echo "  warFile =" $warFile
echo "  shareWarFile =" $shareWarFile
echo "  mmsappDir =" $mmsappDir
echo "  mmsappZip =" $mmsappZip

# stop alfresco server
if [[ -n "$ampFile" || -n "$shareAmpFile" ]]; then
  echo
  echo "### stop alfresco server"
  echo $stopAlfrescoCmd
  #if [[ "$test_mms" -eq "0" ]]; then
    $stopAlfrescoCmd
    if [ "$?" -ne "0" ]; then
      echo "$0: ERROR! command failed! \"!!\""
      exit 1
    fi
  #fi
else
  echo "Not stopping alfresco server since no amps are being installed."
fi

# install view-repo war files
if [[ -n "$ampFile" ]]; then
  echo
  echo "### install view-repo amp and war files"
  if [[ ( -f "$mmtJar" ) &&  ( -f "$ampFile" ) &&  ( -f "$warFile" ) &&  ( -f "$existingWarFile" ) &&  ( -d $(dirname $alfrescoWebappDir) ) ]]; then
    echo
    echo $installWarCommand $mmtJar $ampFile $warFile $existingWarFile $alfrescoWebappDir
    $installWarCommand $mmtJar $ampFile $warFile $existingWarFile $alfrescoWebappDir
    if [ "$?" -ne "0" ]; then
      echo "$0: ERROR! command failed! \"!!\""
      exit 1
    fi
  else
    echo "ERROR! Not all inputs to $installWarCommand exist for view-repo!"
    exit 1
  fi
else
  echo
  echo "### skipping installation of view-repo amp/war files"
fi

if [[ -n "$shareAmpFile" ]]; then
  echo
  echo "### install view-share amp and war files"
  if [[ ( -f "$mmtJar" ) &&  ( -f "$shareAmpFile" ) &&  ( -f "$shareWarFile" ) &&  ( -f "$existingShareWarFile" ) &&  ( -d $(dirname $shareWebappDir) ) ]]; then
    echo
    echo $installWarCommand $mmtJar $shareAmpFile $shareWarFile $existingShareWarFile $shareWebappDir
      $installWarCommand $mmtJar $shareAmpFile $shareWarFile $existingShareWarFile $shareWebappDir
    if [ "$?" -ne "0" ]; then
      echo "$0: ERROR! command failed! \"!!\""
      exit 1
    fi
  else
    echo "ERROR! Not all inputs to $installWarCommand exist for share!"
    exit 1
  fi
fi

# deploy mmsapp
echo
if [[ ( -f "$mmsappZip" ) || ( -d "$mmsappDir" ) ]]; then
  if [[ -d $(dirname $mmsappDeployDir) ]]; then
    echo $deployMmsappCmd $mmsappDeployDir "$mmsappDir" "$backupDir" "$mmsappZip"
    $deployMmsappCmd $mmsappDeployDir $mmsappDir $backupDir $mmsappZip
    if [ "$?" -ne "0" ]; then
      echo "$0: ERROR! command failed! \"!!\""
      exit 1
    fi
  else
    echo "ERROR! Directory $(dirname $mmsappDeployDir) does not exist! Cannot run $deployMmsappCmd."
    exit 1
  fi
else
  echo
  echo "### skipping installation of view-share amp/war files"
fi

#start server
if [[ -n "$ampFile" || -n "$shareAmpFile" ]]; then
  echo
  echo "### start alfresco server"
  echo $startAlfrescoCmd
  $startAlfrescoCmd
else
  echo "Not restarting alfresco server since no amps were being installed."
fi

echo
echo "Success!"

exit 0
