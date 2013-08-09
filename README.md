# view amp for alfresco repository (overlay on alfresco.war)
This project contains the content model and webscripts for accessing and modifying the alfresco repository. 

Location of content model: src/main/amp/config/alfresco/module/view-repo/viewModel.xml
This is registered by spring config in module-context.xml (which imports another spring config xml)

Location of webscripts: src/main/amp/config/alfresco/extension/templates/webscripts

To build the amp file, do 

    mvn package
    
To run in embedded jetty container and H2 db,

    mvn integration-test -Pamp-to-war
    
To clean all data and artifacts

    mvn clean -Ppurge

Go to [http://localhost:8080/view-repo/](http://localhost:8080/view-repo/) for the alfresco explorer interface (it'll take a while to startup)

Create a "europa" site under Sites, then a "/vieweditor/model" folder and "/vieweditor/presentation" folder, the view information will be created in these spaces.

Test post view (post json data taken from the Bootcamp Example document in DocGen project):

    curl -H "Content-Type: application/json" --data @test-data/postview.json -X POST -u admin:admin "http://localhost:8080/view-repo/service/views/_17_0_1_2_407019f_1340300322136_11866_17343?force=true&recurse=true&doc=true"

Test get view

	curl -u admin:admin "http://localhost:8080/view-repo/service/views/_17_0_1_2_407019f_1340300322136_11866_17343?recurse=true"

To open the javascript debugger: [http://localhost:8080/view-repo/service/api/javascript/debugger](http://localhost:8080/view-repo/service/api/javascript/debugger) (you may have to close and reopen to get it to step through on consecutive script calls)

To refresh changes to scripts (they have to be updated in the "target/view-repo-war/WEB-INF/classes/alfresco/extension/..."): [http://localhost:8080/view-repo/service/index](http://localhost:8080/view-repo/service/index) hit refresh at the botton

Maven archetype from [Alfresco Maven SDK](https://artifacts.alfresco.com/nexus/content/repositories/alfresco-docs/alfresco-lifecycle-aggregator/latest/index.html)

Documentation links:

* [Web Scripts](http://docs.alfresco.com/4.2/index.jsp?topic=%2Fcom.alfresco.enterprise.doc%2Fconcepts%2Fws-architecture.html)
* [AMP modules](http://docs.alfresco.com/4.2/index.jsp?topic=%2Fcom.alfresco.enterprise.doc%2Fconcepts%2Fdev-extensions-modules-intro.html)
* [Content modeling](http://docs.alfresco.com/4.2/index.jsp?topic=%2Fcom.alfresco.enterprise.doc%2Fconcepts%2Fcontent-modeling-about.html)
* [Forms?](http://wiki.alfresco.com/wiki/Forms)