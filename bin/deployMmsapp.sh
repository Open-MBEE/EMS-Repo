#!/bin/bash
# deploy mmsapp

myUsage() {
  echo
  echo "usage: sudo $(basename $0) mmsappDeployDir mmsappZip|mmsappDir [backupDir]"
  echo
}

# Change test_mms to 1 to just see commands without running them.
# Change test_mms to 0 to run normally.
# An existing test_mms environment variable overrides setting the value here.
if [ -z "$test_mms" ]; then
  export test_mms=1 # just test
  #export test_mms=0 # normal
fi

if [ "$#" -lt 2 ]; then
  echo "$0 : ERROR! Need at least two arguments!"
  myUsage
  exit 1
fi
if [ "$#" -gt 3 ]; then
  echo "$0 : ERROR! No more than three arguments!"
  myUsage
  exit 1
fi

tmpDir=/tmp/mmsappZip

mmsappDeployDir=$1
if [[ $2 == *zip ]]; then
  mmsappZip=$2
else
  mmsappDir=$2
fi

if [ "$#" -gt 2 ]; then
  backupDir=$3
fi

if [[ ( ! -n "$backupDir" ) || ( ! -d "$backupDir" ) ]]; then
  if [ -n "$backupDir" ]; then
    echo "$0: WARNING! Specified backup directory \"${backupDir}\" does not exist!"
  fi
  backupDir=${mmsappDeployDir}/../..
fi

deployParentDir=$(dirname $mmsappDeployDir)

# Use the owner of the webapp directory as the owner of the deployed webapp
owner=`ls -ld $deployParentDir | cut -d' ' -f 3`

echo "##### arguments for $0 processed with the following assignments and inferred values:"
echo "  mmsappDeployDir =" $mmsappDeployDir
echo "  mmsappDir =" $mmsappDir
echo "  mmsappZip =" $mmsappZip
echo "  backupDir =" $backupDir
echo "  owner = " $owner

# unzip zip file if provided
if [ -f "$mmsappZip" ]; then
  echo
  echo "##### unzip zip file"
  echo mkdir $tmpDir
  mkdir $tmpDir
  echo pushd $tmpDir
  pushd $tmpDir
  echo
  echo unzip -q $mmsappZip
  unzip -q $mmsappZip
  unzippedDir=`/bin/ls -1`
  #unzippedDir=`readlink -e $unzippedDir`
  mmsappDir=`readlink -e $unzippedDir`
  echo popd
  popd
fi

# backup existing mmsapp dir
if [ -e "$mmsappDeployDir" ]; then
  echo
  echo "##### backup existing mmsapp dir"
  echo "tar -zcf ${backupDir}/mmsapp.`date '+%Y%m%d-%H%M%S'`.tgz $mmsappDeployDir"
  #echo "/bin/mv $mmsappDeployDir ${backupDir}/mmsapp.`date '+%Y%m%d-%H%M%S'`"
  if [[ "$test_mms" -eq "0" ]]; then
    #/bin/mv $mmsappDeployDir ${backupDir}/mmsapp.`date '+%Y%m%d-%H%M%S'`
    tar -zcf ${backupDir}/mmsapp.`date '+%Y%m%d-%H%M%S'`.tgz $mmsappDeployDir
    if [ "$?" -ne "0" ]; then
      echo "$0: ERROR! backup of existing mmsapp failed!"
      exit 1
    fi
    echo "/bin/rm -rf $mmsappDeployDir"
    /bin/rm -rf $mmsappDeployDir
    if [ "$?" -ne "0" ]; then
      echo "$0: ERROR! command failed! \"!!\""
      exit 1
    fi
  fi
fi

# copy the mmsapp directory to the deployed location
if [ -d $mmsappDir ]; then
  echo
  echo "##### copy the mmsapp directory to the deployed location"
  echo cp -pRf $mmsappDir $mmsappDeployDir
  if [[ "$test_mms" -eq "0" ]]; then
    cp -pRf $mmsappDir $mmsappDeployDir
    if [ "$?" -ne "0" ]; then
      echo "$0: ERROR! command failed! \"!!\""
      exit 1
    fi
  fi
fi

# delete the temporary unzip directory if it exists
if [ -e "$tmpDir" ]; then
  echo
  echo "##### delete the temporary unzip directory if it exists"
  echo /bin/rm -rf $tmpDir
  #if [[ "$test_mms" -eq "0" ]]; then
    /bin/rm -rf $tmpDir
    if [ "$?" -ne "0" ]; then
      echo "$0: Warning! command failed! \"!!\""
    fi
  #fi
fi

# change permissions for the deployed mmsapp
if [[ -d "$mmsappDeployDir" || "$test_mms" -ne "0" ]]; then
  echo
  echo "##### change permissions for the deployed mmsapp"
  echo chown -Rh ${owner}:${owner} $mmsappDeployDir
  if [[ "$test_mms" -eq "0" ]]; then
    chown -Rh ${owner}:${owner} $mmsappDeployDir
    if [ "$?" -ne "0" ]; then
      echo "$0: ERROR! command failed! \"!!\""
      exit 1
    fi
  fi
else
  echo
  echo "$0: ERROR! Could not find directory \"${mmsappDeployDir}\"to deploy mmsapp"
  exit 1
fi

exit 0
