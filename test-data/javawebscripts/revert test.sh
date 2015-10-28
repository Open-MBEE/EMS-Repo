# 
#  A1  ----  A2 
#    \
#     B1  ---- B2 ---- B3 
#       \
#        C1 ---- C2 --- C3
#

# tag A1
curl 'http://127.0.0.1:8080/alfresco/s/workspaces/master/configurations' -u admin:admin -H 'Content-Type:application/json' -d '{"configurations":[{"name":"A1","description":""}]}'

# branch B
curl 'http://127.0.0.1:9000/alfresco/s/workspaces/B?sourceWorkspace=master&copyTime=null' -u admin:admin

# tag B1
curl 'http://127.0.0.1:8080/alfresco/s/workspaces/B/configurations' -u admin:admin -H 'Content-Type:application/json' -d '{"configurations":[{"name":"B1","description":""}]}'

# branch C
curl 'http://127.0.0.1:9000/alfresco/s/workspaces/C?sourceWorkspace=master&copyTime=null' -u admin:admin

# tag C1
curl 'http://127.0.0.1:8080/alfresco/s/workspaces/C/configurations' -u admin:admin -H 'Content-Type:application/json' -d '{"configurations":[{"name":"C1","description":""}]}'

# create A2
curl 'http://127.0.0.1:8080/alfresco/s/workspaces/master/elements' -u admin:admin -H 'Content-Type:application/json' -d '{"elements":[{"sysmlid":"_24_0_5_1_8660276_1414446925841_771668_16706", "name": "A2"}]}'

# tag A2
curl 'http://127.0.0.1:8080/alfresco/s/workspaces/master/configurations' -u admin:admin -H 'Content-Type:application/json' -d '{"configurations":[{"name":"A2","description":""}]}'

# create B2
curl 'http://127.0.0.1:8080/alfresco/s/workspaces/B/elements' -u admin:admin -H 'Content-Type:application/json' -d '{"elements":[{"sysmlid":"_24_0_5_1_8660276_1414446925841_771668_16706", "name": "B2"}]}'

# tag B2
curl 'http://127.0.0.1:8080/alfresco/s/workspaces/B/configurations' -u admin:admin -H 'Content-Type:application/json' -d '{"configurations":[{"name":"B2","description":""}]}'

# create B3
curl 'http://127.0.0.1:8080/alfresco/s/workspaces/B/elements' -u admin:admin -H 'Content-Type:application/json' -d '{"elements":[{"sysmlid":"_24_0_5_1_8660276_1414446925841_771668_16706", "name": "B3"}]}'

# tag B3
curl 'http://127.0.0.1:8080/alfresco/s/workspaces/B/configurations' -u admin:admin -H 'Content-Type:application/json' -d '{"configurations":[{"name":"B3","description":""}]}'

# create C2
curl 'http://127.0.0.1:8080/alfresco/s/workspaces/C/elements' -u admin:admin -H 'Content-Type:application/json' -d '{"elements":[{"sysmlid":"_24_0_5_1_8660276_1414446925841_771668_16706", "name": "C2"}]}'

# tag C2
curl 'http://127.0.0.1:8080/alfresco/s/workspaces/C/configurations' -u admin:admin -H 'Content-Type:application/json' -d '{"configurations":[{"name":"C2","description":""}]}'

# create C3
curl 'http://127.0.0.1:8080/alfresco/s/workspaces/C/elements' -u admin:admin -H 'Content-Type:application/json' -d '{"elements":[{"sysmlid":"_24_0_5_1_8660276_1414446925841_771668_16706", "name": "C3"}]}'

# tag C3
curl 'http://127.0.0.1:8080/alfresco/s/workspaces/C/configurations' -u admin:admin -H 'Content-Type:application/json' -d '{"configurations":[{"name":"C3","description":""}]}'
