# view amp for alfresco repository (overlay on alfresco.war)
This project contains the content model and webscripts for accessing and modifying the alfresco repository. 

Location of content model: src/main/amp/config/alfresco/module/view-repo/viewModel.xml

This is registered by spring config in module-context.xml (which imports another spring config xml)

Location of webscripts: src/main/amp/config/alfresco/extension/templates/webscripts

Make sure your JAVA_HOME is set to a java 1.7 installation.

To build the amp file, do 

    mvn package
    
To run in embedded jetty container and H2 db, 

	export MAVEN_OPTS='-Xms256m -Xmx1G -XX:PermSize=300m'
	
    mvn integration-test -Pamp-to-war
    
To clean all data and artifacts

    mvn clean -Ppurge

To execute JUnit tests and attach a debugger

    mvn -Dmaven.surefire.debug -Dmaven.test.skip=false test
    
    Set a breakpoint in a test.
    Run a Remote Java Application configuration with localhost for Host and 5005 for Port.
    Follow DemoComponentTest.java and its entry in service-content.xml


Go to [http://localhost:8080/view-repo/](http://localhost:8080/view-repo/) for the alfresco explorer interface (it'll take a while to startup)

In the repository, create a "/ViewEditor/model" folder and "/ViewEditor/snapshots" folder, the view information will be created in these spaces.

Post test view:

    curl -H "Content-Type: application/json" --data @test-data/postview.json -X POST -u admin:admin "http://localhost:8080/view-repo/service/rest/views/_17_0_1_2_407019f_1340300322136_11866_17343?force=true&recurse=true&doc=true&user=dlam"

Post view hierarchy:

    curl -H "Content-Type: application/json" --data @test-data/postviewhierarchy.json -X POST -u admin:admin "http://localhost:8080/view-repo/service/rest/views/_17_0_1_2_407019f_1340300322136_11866_17343/hierarchy"

Post DocGen Manual

    curl -H "Content-Type: application/json" --data @test-data/docgenManual.json -X POST -u admin:admin "http://localhost:8080/view-repo/service/rest/views/_17_0_3_244e03eb_1333856871739_813876_16836?force=true&recurse=true&doc=true&user=dlam"

Post comments

	curl -H "Content-Type: application/json" --data @test-data/postcomment.json -X POST -u admin:admin "http://localhost:8080/view-repo/service/rest/views/_17_0_1_2_407019f_1340300322136_11866_17343/comments?recurse=true"

Post Project organization

	curl -H "Content-Type: application/json" --data @test-data/postproject.json -X POST -u admin:admin "http://localhost:8080/view-repo/service/rest/projects/europa"
	
Get test view

	curl -u admin:admin "http://localhost:8080/view-repo/service/rest/views/_17_0_1_2_407019f_1340300322136_11866_17343?recurse=true"

Get DocGen Manual

	curl -u admin:admin "http://localhost:8080/view-repo/service/rest/views/_17_0_3_244e03eb_1333856871739_813876_16836?recurse=true"

Get comments

	curl -u admin:admin "http://localhost:8080/view-repo/service/rest/views/_17_0_1_2_407019f_1340300322136_11866_17343/comments?recurse=true"
	
To open the javascript debugger: [http://localhost:8080/view-repo/service/api/javascript/debugger](http://localhost:8080/view-repo/service/api/javascript/debugger) (you may have to close and reopen to get it to step through on consecutive script calls)

To refresh changes to scripts (they have to be updated in the "target/view-repo-war/WEB-INF/classes/alfresco/extension/..."): [http://localhost:8080/view-repo/service/index](http://localhost:8080/view-repo/service/index) hit refresh at the botton

Maven archetype from [Alfresco Maven SDK](https://artifacts.alfresco.com/nexus/content/repositories/alfresco-docs/alfresco-lifecycle-aggregator/latest/index.html)

Documentation links:

* [Web Scripts](http://docs.alfresco.com/4.2/index.jsp?topic=%2Fcom.alfresco.enterprise.doc%2Fconcepts%2Fws-architecture.html)
* [AMP modules](http://docs.alfresco.com/4.2/index.jsp?topic=%2Fcom.alfresco.enterprise.doc%2Fconcepts%2Fdev-extensions-modules-intro.html)
* [Content modeling](http://docs.alfresco.com/4.2/index.jsp?topic=%2Fcom.alfresco.enterprise.doc%2Fconcepts%2Fcontent-modeling-about.html)
* [Forms?](http://wiki.alfresco.com/wiki/Forms)
