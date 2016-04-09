# TODO -- need to parametrize any path that specifies /opt/local

# Tell MMS that this job has started
status=running
(curl -w "\n%{http_code}\n" -u ${MMS_USER}:${MMS_PASSWORD} -X POST -H Content-Type:application/json --data "{\"jobs\":[{\"sysmlid\":\"${JOB_ID}\", \"status\":\"${status}\"}]}" "${MMS_SERVER}/alfresco/service/workspaces/master/jobs") || echo "curl failed"

git submodule init

git submodule update

ant -buildfile jenkinsbuild.xml

export MAGICDRAW_HOME=/opt/local/MD

#complete classpath to launch magicdraw via java
export CLASSPATH=$WORKSPACE/bin/
export CLASSPATH=${CLASSPATH}:$WORKSPACE/lib/*
export CLASSPATH=${CLASSPATH}:$WORKSPACE/mdk_module/lib/*
export CLASSPATH=${CLASSPATH}:$WORKSPACE/mdk_module/lib/test/*
export CLASSPATH=${CLASSPATH}:$MAGICDRAW_HOME/plugins/gov.nasa.jpl.mbee.docgen/*
export CLASSPATH=${CLASSPATH}:$MAGICDRAW_HOME/plugins/com.nomagic.magicdraw.automaton/*
export CLASSPATH=${CLASSPATH}:$MAGICDRAW_HOME/lib/*
export CLASSPATH=${CLASSPATH}:$MAGICDRAW_HOME/lib/graphics/*
export CLASSPATH=${CLASSPATH}:$MAGICDRAW_HOME/lib/webservice/*

#echo $CLASSPATH
export DISPLAY=:1

#robot disabled pending determination of alternate command line argument pass in
java -Xmx4096M -XX:PermSize=64M -XX:MaxPermSize=512M gov.nasa.jpl.mbee.emsrci.mdk.pma.PMADrone -tstrt $WORKSPACE/ -twprj $TEAMWORK_PROJECT -crdlc $CREDENTIALS --doclist $DOCUMENTS

# Tell MMS that this job has completed.  If it&apos;s in the &quot;running&quot; state, then we assume everything executed properly
# and change status to &quot;completed.&quot;  Otherwise, we assume that $status has been set to an appropriate value elsewhere.

if [ "$status" == "running" ]; then status=completed; fi
(curl -w "\n%{http_code}\n" -u ${MMS_USER}:${MMS_PASSWORD} -X POST -H Content-Type:application/json --data "{\"jobs\":[{\"sysmlid\":\"${JOB_ID}\", \"status\":\"${status}\"}]}" "${MMS_SERVER}/alfresco/service/workspaces/master/jobs")  echo "curl failed" 
