# TODO -- need to parametrize any path that specifies /opt/local

# Tell MMS that this job has started
status=running
curl -w "\n%{http_code}\n" -u ${MMS_USER}:${MMS_PASSWORD} -X POST -H Content-Type:application/json --data "{\"jobs\":[{\"sysmlid\":\"${JOB_ID}\", \"status\":\"${status}\"}]}" "https://${MMS_SERVER}/alfresco/service/workspaces/master/jobs"

git submodule init

git submodule update

ant -buildfile jenkinsbuild.xml

cd /opt/local/MD

#complete classpath to launch magicdraw via java
export CLASSPATH=${CLASSPATH}:/opt/local/jenkins/working_dir/workspace/${JOB_ID}/bin/
export CLASSPATH=${CLASSPATH}:/opt/local/jenkins/working_dir/workspace/${JOB_ID}/lib/*
export CLASSPATH=${CLASSPATH}:/opt/local/jenkins/working_dir/workspace/${JOB_ID}/mdk_module/lib/*
export CLASSPATH=${CLASSPATH}:/opt/local/jenkins/working_dir/workspace/${JOB_ID}/mdk_module/lib/test/*
export CLASSPATH=${CLASSPATH}:/opt/local/MD/lib/*
export CLASSPATH=${CLASSPATH}:/opt/local/MD/lib/graphics/*
export CLASSPATH=${CLASSPATH}:/opt/local/MD/lib/webservice/*

#export display of magicdraw to vnc with gui installed (required to launch)
export DISPLAY=:1

#secondary java 7 command line test calls
java -Xmx1200m -XX:PermSize=1200m -XX:MaxPermSize=1200m gov.nasa.jpl.mbee.emsrci.mdk.pma.PMADrone -tstrt /opt/local/jenkins/working_dir/workspace/${JOB_ID}/ -twsrv $TEAMWORK_SERVER -twprt $TEAMWORK_PORT -twusr $TEAMWORK_USER -twpsd $TEAMWORK_PASSWORD -twprj $TEAMWORK_PROJECT -mmsusr $MMS_USER -mmspsd $MMS_PASSWORD --doclist $DOCUMENTS

# Tell MMS that this job has completed.  If it&apos;s in the &quot;running&quot; state, then we assume everything executed properly
# and change status to &quot;completed.&quot;  Otherwise, we assume that $status has been set to an appropriate value elsewhere.

if [ "$status" == "running" ]; then status=completed; fi
curl -w "\n%{http_code}\n" -u ${MMS_USER}:${MMS_PASSWORD} -X POST -H Content-Type:application/json --data "{\"jobs\":[{\"sysmlid\":\"${JOB_ID}\", \"status\":\"${status}\"}]}" "https://${MMS_SERVER}/alfresco/service/workspaces/master/jobs" 

