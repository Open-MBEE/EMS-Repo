# TODO -- need to parametrize any path that specifies /opt/local

# Tell MMS that this job has started
status=running
(curl -w "\n%{http_code}\n" -n -X POST -H Content-Type:application/json --data "{\"jobs\":[{\"sysmlid\":\"${JOB_ID}\", \"status\":\"${status}\"}]}" "${MMS_SERVER}/alfresco/service/workspaces/master/jobs") || echo "curl failed"

# Run docweb job
export MD_HOME=/opt/local/MD
bash /opt/local/MD/automations/docweb.sh "${TEAMWORK_PROJECT}" "${DOCUMENTS}" "${CREDENTIALS}"

# Tell MMS that this job has completed.  If it&apos;s in the &quot;running&quot; state, then we assume everything executed properly
# and change status to &quot;completed.&quot;  Otherwise, we assume that $status has been set to an appropriate value elsewhere.

if [ "$status" == "running" ]; then status=completed; fi
(curl -w "\n%{http_code}\n" -n -X POST -H Content-Type:application/json --data "{\"jobs\":[{\"sysmlid\":\"${JOB_ID}\", \"status\":\"${status}\"}]}" "${MMS_SERVER}/alfresco/service/workspaces/master/jobs")  echo "curl failed" 
