#!/bin/bash

usage="usage: sudo $0 mmtjar ampFile warFile existingWarFile explodedWebappDir"

if [[ ( ! "$#" -eq 4 ) &&  ( ! "$#" -eq 5 ) ]]; then
#if [ ! "$#" -eq 3 ]; then
  echo "$0 : Error! Need at three arguments! number of passed args = $#"
  echo $usage
  exit 1
fi

mmtjar=$1
ampFile=$2
warFile=$3
existingWarFile=$4
explodedWebappDir=$5

owner=`ls -ld $existingWarFile | cut -d' ' -f 3`

echo "arguments processed with the following assignments:"
echo "  ampFile=" $ampFile
echo "  warFile=" $warFile
echo "  existingWarFile=" $existingWarFile
echo "  explodedWebappDir=" $explodedWebappDir

# backup war file
if [ ! $existingWarFile -ef $warFile ]; then
  echo cp $existingWarFile ${existingWarFile}.`date '+%Y%m%d-%H%M%S'`
  if [[ "$test" -eq "0" ]]; then
    cp $existingWarFile ${existingWarFile}.`date '+%Y%m%d-%H%M%S'`
  fi
  # use specified warFile
  echo cp -f $warFile $existingWarFile
  if [[ "$test" -eq "0" ]]; then
    cp -f $warFile $existingWarFile
  fi
fi

# install amp to war
echo java -jar $mmtJar install $ampFile $existingWarFile -force
temp=`mktemp`
if [[ "$test" -eq "0" ]]; then
  java -jar $mmtJar install $ampFile $existingWarFile -force | tee $temp | head -n 5
  echo . . .
  tail -n 5 $temp
  /bin/rm -rf $temp
fi

# change owner to tomcat if specified
#if [ "tomcat" == "$owner" ]; then
  echo chown ${owner}:${owner} $existingWarFile
  if [[ "$test" -eq "0" ]]; then
    chown ${owner}:${owner} $existingWarFile
  fi
  #chown tomcat:tomcat $existingWarFile
#fi

if [ ! "$explodedWebappDir" -eq "" ]; then

  # blast alfresco directory
  if [ -d $explodedWebappDir ]; then
    echo rm -rf $explodedWebappDir
    if [[ "$test" -eq "0" ]]; then
      rm -rf $explodedWebappDir
    fi
  fi

  # explode war
  echo mkdir $explodedWebappDir
  if [[ "$test" -eq "0" ]]; then
    mkdir $explodedWebappDir
  fi

  echo pushd $explodedWebappDir
  pushd $explodedWebappDir

  echo jar xvf $existingWarFile
  if [[ "$test" -eq "0" ]]; then
    jar xvf $existingWarFile
  fi

  # change owner
  echo chown -Rh ${owner}:${owner} $explodedWebappDir
  if [[ "$test" -eq "0" ]]; then
    chown -Rh ${owner}:${owner} $explodedWebappDir
  fi

  # get back to where we were
  echo popd
  popd

fi

exit 0
