#!/bin/bash

usage="usage: sudo $0 mmtjar ampFile warFile existingWarFile explodedWebappDir"

if [ ( ! "$#" -eq 4 ) &&  ( ! "$#" -eq 5 ) ]; then
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
  cp $existingWarFile ${existingWarFile}.`date '+%Y%m%d-%H%M%S'`
  # use specified warFile
  echo cp -f $warFile $existingWarFile
  cp -f $warFile $existingWarFile
fi

# install amp to war
echo java -jar $mmtJar install $ampFile $existingWarFile -force
temp=`mktemp`
java -jar $mmtJar install $ampFile $existingWarFile -force | tee $temp | head -n 5
echo . . .
tail -n 5 $temp
/bin/rm -rf $temp

# change owner to tomcat if specified
#if [ "tomcat" == "$owner" ]; then
  echo chown ${owner}:${owner} $existingWarFile
  chown ${owner}:${owner} $existingWarFile
  #chown tomcat:tomcat $existingWarFile
#fi

if [ ! "$explodedWebappDir" -eq "" ]; then

  # blast alfresco directory
  if [ -d $explodedWebappDir ]; then
    echo rm -rf $explodedWebappDir
    rm -rf $explodedWebappDir
  fi

  # explode war
  echo mkdir $explodedWebappDir
  mkdir $explodedWebappDir

  echo pushd $explodedWebappDir
  pushd $explodedWebappDir

  echo jar xvf $existingWarFile
  jar xvf $existingWarFile

  # change owner
  echo chown -Rh ${owner}:${owner} $explodedWebappDir
  chown -Rh ${owner}:${owner} $explodedWebappDir

  # get back to where we were
  echo popd
  popd

fi

exit 0



