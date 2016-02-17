# Tell MMS that this job has started
status=running
curl -w "\n%{http_code}\n" -u ${MMS_USER}:${MMS_PASSWORD} -X POST -H Content-Type:application/json --data "{\"jobs\":[{\"id\":\"${JOB_ID}\", \"\"status\":\"${status}\"]}" "${MMS_SERVER}/alfresco/service/workspaces/master/jobs"


#curl -o temp http://cae-artifactory.jpl.nasa.gov/artifactory/simple/ext-release-local/gov/nasa/jpl/cae/magicdraw/packages/cae_md18_0_sp5_mdk/2.3-RC3/cae_md18_0_sp5_mdk-2.3-RC3.zip

pwd

git submodule init

git submodule update

ant -buildfile jenkinsbuild.xml

cd /opt/local/MD

#complete classpath to launch magicdraw via java
export CLASSPATH=${CLASSPATH}:/opt/local/jenkins/working_dir/workspace/MDKTest/bin/
export CLASSPATH=${CLASSPATH}:/opt/local/jenkins/working_dir/workspace/MDKTest/lib/*
export CLASSPATH=${CLASSPATH}:/opt/local/jenkins/working_dir/workspace/MDKTest/mdk_module/lib/*
export CLASSPATH=${CLASSPATH}:/opt/local/jenkins/working_dir/workspace/MDKTest/mdk_module/lib/test/*
export CLASSPATH=${CLASSPATH}:/opt/local/MD/lib/*
export CLASSPATH=${CLASSPATH}:/opt/local/MD/lib/graphics/*
export CLASSPATH=${CLASSPATH}:/opt/local/MD/lib/webservice/*

#export display of magicdraw to vnc with gui installed (required to launch)
export DISPLAY=:1

#robot disabled pending determination of alternate command line argument pass in
#java org.robotframework.RobotFramework -d /opt/local/jenkins/working_dir/workspace/MDKTest/mdk/robot/report/TeamworkTest /opt/local/jenkins/working_dir/workspace/MDKTest/mdk/robot/test/TeamworkTest.robot

# test case argument list:
# -d &lt;String&gt; : uses supplied string as date. not implemented elsewhere atm
# -tstnm &lt;string&gt; : uses supplied string as testName, either to find a local test file or to store reference output. probably not something you&apos;ll use.
# -tstrt &lt;string&gt; : uses supplied string as testRoot location. again, probably not something you&apos;ll use
# -twprj &lt;string&gt; : specifies teamwork project name
# -twsrv &lt;string&gt; : specifies teamwork server
# -twprt &lt;string&gt; : specifies teamwork port
# -twusr &lt;string&gt; : specifies teamwork user
# -twpsd &lt;string&gt; : specifies teamwork password
# -wkspc : specifies workspace. not currently implemented.
# -mmsusr &lt;string&gt; : specifies mms username
# -mmspsd &lt;string&gt; : specifies mms password


#secondary java 7 command line test calls
java -Xmx1200m -XX:PermSize=1200m -XX:MaxPermSize=1200m gov.nasa.jpl.mbee.emsrci.mdk.test.TableElementCopyWithVEChange -tstrt /opt/local/jenkins/working_dir/workspace/MDKTest/ -ptrlc /opt/local/node.js/bin/
#java -Xmx1200m -XX:PermSize=1200m -XX:MaxPermSize=1200m gov.nasa.jpl.mbee.emsrci.mdk.test.TeamworkTest -tstrt &quot;/opt/local/jenkins/working_dir/workspace/MDKTest/&quot;# -ptrlc &quot;/opt/local/node.js/bin/&quot;

#java -Xmx1200m -XX:PermSize=1200m -XX:MaxPermSize=1200m gov.nasa.jpl.mbee.emsrci.mdk.test.SyncTest -tstrt &quot;/opt/local/jenkins/working_dir/workspace/MDKTest/&quot;



#secondary java 8 command line test calls
#java -Xmx1200m gov.nasa.jpl.mbee.emsrci.mdk.test.TeamworkTest -tstrt &quot;/opt/local/jenkins/working_dir/workspace/MDKTest/&quot;# -ptrlc &quot;/opt/local/node.js/bin/&quot;

#protractor tests
#/bin/bash /opt/local/jenkins/working_dir/workspace/MDKTest/test.sh tablecopytest /opt/local/jenkins/working_dir/workspace/MDKTest/

# Tell MMS that this job has completed.  If it&apos;s in the &quot;running&quot; state, then we assume everything executed properly
# and change status to &quot;completed.&quot;  Otherwise, we assume that $status has been set to an appropriate value elsewhere.

if [ "$status" == "running" ]; then status=completed; fi
curl -w "\n%{http_code}\n" -u ${MMS_USER}:${MMS_PASSWORD} -X POST -H Content-Type:application/json --data "{\"elements\":[{\"id\":\"${JOB_ID}\", \"status\":\"${status}\"]}" "${MMS_SERVER}/alfresco/service/workspaces/master/elements" 

