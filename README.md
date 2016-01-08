# This repo represents the view amp for alfresco repository (overlay on alfresco.war)

#MAC OSX Yosemite 10.10.5 Quick Install (assuming you've installed Maven and Eclipse)

1. Update your JAVA_HOME to use JDK 1.7.  Check the path using this command /usr/libexec/java_home.
	
		export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk1.7.0_71.jdk/Contents/Home"

2. Update your MAVEN_OPTS variable:

		export MAVEN_OPTS='-Xms256m -Xmx1G -XX:PermSize=300m -Xdebug  -Xrunjdwp:transport=dt_socket,address=10000,server=y,suspend=n -javaagent:/Applications/jrebel/jrebel.jar'
	
3. Clone alfresco-view-repo using the Eclipse git tool. If you need instructions on installing and using git in eclipse see this section: [typical local environment instructions](#typical)
4. Right click your project and run maven >> Update project
5. Install jrebel and scala from Eclipse using Help >> Eclipse Marketplace
6. From the command line navigate to git/alfresco-view-repo  and update the last line sudo vim /etc/hosts to read:
	 
		127.0.0.1  'your-machine-name'

7. Run this script from the command line:

		./cpr.sh
	
###The remaining instructions of the readme will guide you through specific set ups 

***

## Contents <a name="contents"></a>

###[AMI environment instructions](#ami)
###[typical local environment instructions](#typical)
###[building, setting up maven, jrebel](#building)
###[Managing Enterprise and Community Builds](#manage)
###[Debugging](#debug)
###[Testing](#test-this)
###[Other Debug](#other)
###[Documentation Links](#doc-links)
###[Miscellaneous](#misc)
###[Debugging Overview](#debug-overview)


***

#AMI environment instructions <a name="ami"></a>

#### [return to table of contents](#contents)

import view-repo with git (same instructions as below)

update maven project:
	right click maven project
	Maven -> Update Project...

*This Eclipse project can be built with or without dependencies to the bae/uti/sysml projects. By default,
this project depends on the BUS projects. To take them out:*
	* Remove the BUS projects from the build path (this will modify the .classpath file, which you shouldn't commit).
 	* Right click the project, select _Properties_, select _Java Build Path_, select _Projects_ tab and remove the projects.
	* Configure Eclipse to use the proper maven profile that uses the appropriate maven dependencies that pull from artifactory.
 	* Right click the project, select _Properties_, select _Maven_, set the Active Maven Profiles to _mbee-dev_. 

build with maventest in command line
	
	cd /home/username/git/alfresco-view-repo
	# for those that don't want to load bae/util/sysml projects
	# also need to remove the src dependencies on those packages in .classpath
	mvn integration-test -Pamp-to-war -Dmaven.test.skip=true -P mbee-dev
	
	# for those that do want load bae/util/sysml projects
    mvn integration-test -Pamp-to-war -Dmaven.test.skip=true -P mms-dev
    
if using the mdk or bae MagicDraw plugins, set up magicdraw path
    Window->Preferences->MagicDraw Installation
    Set the root directory to "/opt/local/magicdraw"
    There are separate mdk and bae setup instructions, which can be ignored for view-repo development and test.

restart eclipse

#typical local environment instructions <a name="typical"></a>

#### [return to table of contents](#contents)

This project contains the content model and webscripts for accessing and modifying the alfresco repository. 

Location of content model: src/main/amp/config/alfresco/module/view-repo/viewModel.xml

This is registered by spring config in module-context.xml (which imports another spring config xml)

Location of webscripts: src/main/amp/config/alfresco/extension/templates/webscripts

Eclipse/Maven
    Make sure your JAVA_HOME is set to a java 1.7 installation.

    Install maven if you don't have it (mac & linux may have it pre-installed)	
		To install maven (not included in OS X 10.9 (Mavericks)):
		1. Install homebrew by copy/pasting the last command in this link into the terminal: http://brew.sh/
		2. Install maven using the command: 'brew install maven'. 
		Note: This is maven 3.1.1. If this version causes problems, install maven 3.0 using command: 
                'brew install homebrew/versions/maven30'
		
    You might want to avoid yoxos.

    For a fresh copy of the latest Eclipse for JavaEE installation, install new software: egit and maven (no need to add update site)
    		- If Maven is not installed in Eclipse, go to Help -> Eclipse Marketplace -> type in "m2eclipse" in search box 
		      & install the first item (Maven Integration for Eclipse WTP (Juno))	

    Make sure you have a local checkout of alfresco from git. 
    	- Clone Alfresco-View-Repo from git. To do so: 
    	1. Go to Git Repo Perspective in Eclipse
    	2. Click clone a git repository icon in "Git Repositories" menu
    	3. Copy the Https link from Stash for Alfresco-View-Repo Refractor or Master branch (Use Refractor to set up a working Alfresco; you can switch to other branches later)
    	4. Paste into "URI" textbox in Clone Git Repository window in Eclipse
    	5. Change Protocol to HTTPS
    	6. Make sure to enter fields for "User" and "Password" with your username and password
    	7. Click Next, Select the branches you want to clone, and click next again
    	8. Ensure that "Import all existing projects after clone finishes" item is checked & click finish

    Import a maven project from the local ~/git/alfresco-view-repo
        1. Go to Java Perspective in Eclipse
        2. File -> Import ->In dialog window click: Git -> Projects from Git. Click next.
        3. Select Local, Click next.
        4. Select Git Repository you want to include in Java workspace. Click Next.
        5. Select "Import Existing Projects". Click  Next.
        6. Select projects to import and then click finish.
        7. If project is not a maven project, right click project in Java perspective->Maven->Convert to Maven Project
    
    To connect the project to git, right-click the project in the Package Explorer and select Team->Share Project->Git->"Use or create repository in parent folder of project".

    If there are errors and you can resolve them later, choose to resolve them later.  After importing, open the pom.xml, and use the second quick-fix choice for each error.

        - If errors inhibit the later steps, then delete the project from java perspective, and re-import it from the EGit Perspective as shown above.
        
    We're using a local libraries that need to be included in your local Maven repository using the following commands.
    
    	mvn install:install-file -Dfile=lib/AE.jar -DgroupId=gov.nasa.jpl -DartifactId=AE -Dversion=1.0 -Dpackaging=jar
        mvn install:install-file -Dfile=lib/mbee_util.jar -DgroupId=gov.nasa.jpl -DartifactId=mbee_util -Dversion=1.0 -Dpackaging=jar
        mvn install:install-file -Dfile=lib/sysml.jar -DgroupId=gov.nasa.jpl -DartifactId=sysml -Dversion=1.0 -Dpackaging=jar

    For the bae, sysml, and util projects you will need to update the build.properties file and rebel.xml file with your home directory and folder of where magic draw is installed.  For instance:
        home=/home/gcgandhi
        md=/opt/local/magicdraw

    If you are not using Eclispse yoxos, then you will need to install the IMCE Eclipse plug ins.  Do the following:
        1. Get the .jar files from Brad or Doris, and then place them in your eclipse installation plugins folder, i.e. "/Applications/eclipse/plugins".  
        2. Restart Eclipse.  
        3. For the mdk and bae projects right click on the project folder and check that the library (IMCE MagicDraw Classpath Container) was added:  Properties->Java BuildPath->Libraries.  
        4.Make sure that the library appears first in the list for the bae project (you can adjust this in the Order and Export tab).  

    Make sure that Java Buildpath for the view-repo project is using Java 1.7.

    Open up the Ant window by clicking on Window->Show View->Other->Ant.  For the bae, sysml, and util projects, 
    drag the build.xml files into the Ant window.  Then expand each one in the Ant window, and run the makejar.  You must do this everytime after you purge.
    There is a way to get Eclipse to automatically do this when saving a file in those projects (talk to Brad).
    
    In order to get Eclipse to find the source code for the sysml, util, and bae projects, and the source code
    to the build path by adding the projects to build path.  Make sure that in Order and Export tab, that they are
    above the Maven Dependencies.
    
    ----- If Eclipse closes when trying to commit with a response like:
    cairo-misc.c:380: _cairo_operator_bounded_by_source: Assertion `NOT_REACHED' failed."
    try adding:    adding "-Dorg.eclipse.swt.internal.gtk.cairoGraphics=false" to eclipse.ini.
    
# building, setting up maven, jrebel <a name="building"></a>

#### [return to table of contents](#contents)

Make sure to install both the JRebel plug-in for Eclipse and download the Jrebel .jar and store it somewhere like "/Applications/jrebel".  
The path to the jrebel.jar will be used by mvn via the MAVEN_OPTS environment variable.

To make sure the rebel.xml file is generated properly, do 

    mvn jrebel:generate
    
To build the amp file, do 

    mvn package
    
To run in embedded jetty container and H2 db, (with jrebel and remote debugging!), the javaagent should point to your licensed jrebel.jar, address should be a port you can attach a debugger to

	export MAVEN_OPTS='-Xms256m -Xmx1G -XX:PermSize=300m -Xdebug -Xrunjdwp:transport=dt_socket,address=10000,server=y,suspend=n -javaagent:/Applications/jrebel/jrebel.jar'
	
    mvn integration-test -Pamp-to-war -Dmaven.test.skip=false

**IMPORTANT:** Check that /etc/hosts has the appropriate IP address for your machine, e.g., the line that starts with 128.149.16.... should match your instance's IP address
    
**NOTE:** It's possible that Eclipse can get in the way of the maven execution. So, when running maven, temporarily turn off the Eclipse->Project->Build Automatically". Once the Jetty server is up and running, turn it back on so you can make changes hot swap your code updates.
    
To clean all data and artifacts.  After doing this make sure to update the Maven project (right click on project folder->Maven->Update Project) and re-create/copy the sysml, util, bae .jar files.

    mvn clean -Ppurge

To execute JUnit tests and attach a debugger

    mvn -Dmaven.surefire.debug -Dmaven.test.skip=false test

    Put JUnit test java files in src/test/java
    Set a breakpoint in a JUnit test.
    Run a Remote Java Application configuration with localhost for Host and 5005 for Port. There's a view-repo.launch config in the project directory.  You may need to change the view-repo configuration's argument from 10000 to 5005.  
    Follow DemoComponentTest.java and its entry in service-content.xml
    
For JUnit tests in a single Java class, for example, in MyJavaJUnitTestClass.java

    mvn -Dmaven.surefire.debug -Dmaven.test.skip=false -Dtest=MyJavaJUnitTestClass test

To update the target/mms-repo-war manually

	mvn package -Pamp-to-war

## Managing Enterprise and Community Builds <a name="manage"></a>

#### [return to table of contents](#contents)
	
The default pom.xml builds the community version. To build the enterprise version, use the pom.xml.enterprise, e.g.

    mvn -f pom.enterprise.xml [goal]
    
PLEASE keep pom.xml and pom.xml.enterprise in sync!!!

*NOTE:* Since Enterprise and Community need a different set of files, each pom has a
copy-resource plugin that copies the files in /resources/[community|enterprise] into the
appropriate /src directory as part of the validation. Make changes to the appropriate files
in the /resources/[community|enterprise] directory.

### Enterprise settings with Maven 

Need to update settings.xml to connect to the Alfresco private repository. Ask Ly or Cin-Young
for username and password.  On a Mac the path for this file is: /Users/[USER_NAME]/.m2/settings.xml.

	<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
	  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
	                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
	  <localRepository/>
	  <interactiveMode/>
	  <usePluginRegistry/>
	  <offline/>
	  <pluginGroups/>
	  <servers>
	        <server>
	                <id>alfresco-private-repository</id>
	                <username>username</username>
	                <password>password</password>
	        </server>
	  </servers>
	  <mirrors/>
	  <proxies/>
	  <profiles/>
	  <activeProfiles/>
	</settings>
	
# Debug <a name="debug"></a>

#### [return to table of contents](#contents)

To attach an eclipse debugger, there's a view-repo.launch, you can use it to debug at the 10000 port, or change the debug config port if you didn't use 10000 in the maven opts

Jrebel is monitoring the target/classes and src/main/amp/config dir for changes, and the src/main/amp/web for static file changes, make sure the eclipse build automatically is on, and usually any changes to java classes or spring configs will be reloaded automagically

# Testing <a name="test-this"></a>

#### [return to table of contents](#contents)

Note: URL has changed for Alfresco 4.2.e to use alfresco instead of view-repo.

Go to [http://localhost:8080/alfresco/](http://localhost:8080/alfresco/) for the alfresco explorer interface (it'll take a while to startup)

In the repository, create a "/ViewEditor/model" folder and "/ViewEditor/snapshots" folder, the view information will be created in these spaces.

Use the view import/export from the latest dev release of mdk, or you can use curl commands below

Post test view:

    curl -H "Content-Type: application/json" --data @test-data/postview.json -X POST -u admin:admin "http://localhost:8080/alfresco/service/rest/views/_17_0_1_2_407019f_1340300322136_11866_17343?force=true&recurse=true&doc=true&user=dlam"

Post view hierarchy:

    curl -H "Content-Type: application/json" --data @test-data/postviewhierarchy.json -X POST -u admin:admin "http://localhost:8080/alfresco/service/rest/views/_17_0_1_2_407019f_1340300322136_11866_17343/hierarchy"

Post DocGen Manual

    curl -H "Content-Type: application/json" --data @test-data/docgenManual.json -X POST -u admin:admin "http://localhost:8080/alfresco/service/rest/views/_17_0_3_244e03eb_1333856871739_813876_16836?force=true&recurse=true&doc=true&user=dlam"

Post comments

	curl -H "Content-Type: application/json" --data @test-data/postcomment.json -X POST -u admin:admin "http://localhost:8080/alfresco/service/rest/views/_17_0_1_2_407019f_1340300322136_11866_17343/comments?recurse=true"

Post Project organization

	curl -H "Content-Type: application/json" --data @test-data/postproject.json -X POST -u admin:admin "http://localhost:8080/alfresco/service/rest/projects/europa"
	
Get test view

	curl -u admin:admin "http://localhost:8080/alfresco/service/rest/views/_17_0_1_2_407019f_1340300322136_11866_17343?recurse=true"

Get DocGen Manual

	curl -u admin:admin "http://localhost:8080/alfresco/service/rest/views/_17_0_3_244e03eb_1333856871739_813876_16836?recurse=true"

Get comments

	curl -u admin:admin "http://localhost:8080/alfresco/service/rest/views/_17_0_1_2_407019f_1340300322136_11866_17343/comments?recurse=true"



# Other debug <a name="other"></a>

#### [return to table of contents](#contents)

To open the javascript debugger: [http://localhost:8080/alfresco/service/api/javascript/debugger](http://localhost:8080/alfresco/service/api/javascript/debugger) (you may have to close and reopen to get it to step through on consecutive script calls)

To refresh changes to scripts (they have to be updated in the "target/mms-repo-war/WEB-INF/classes/alfresco/extension/..."): [http://localhost:8080/alfresco/service/index](http://localhost:8080/alfresco/service/index) hit refresh at the botton

Maven archetype from [Alfresco Maven SDK](https://artifacts.alfresco.com/nexus/content/repositories/alfresco-docs/alfresco-lifecycle-aggregator/latest/index.html)

# Documentation links: <a name="doc-links"></a>

#### [return to table of contents](#contents)

* [Web Scripts](http://docs.alfresco.com/4.2/index.jsp?topic=%2Fcom.alfresco.enterprise.doc%2Fconcepts%2Fws-architecture.html)
* [AMP modules](http://docs.alfresco.com/4.2/index.jsp?topic=%2Fcom.alfresco.enterprise.doc%2Fconcepts%2Fdev-extensions-modules-intro.html)
* [Content modeling](http://docs.alfresco.com/4.2/index.jsp?topic=%2Fcom.alfresco.enterprise.doc%2Fconcepts%2Fcontent-modeling-about.html)
* [Forms?](http://wiki.alfresco.com/wiki/Forms)

Get a directory of available services including the REST API

    https://localhost:8080/alfresco/service/index
    https://sheldon.jpl.nasa.gov/alfresco/service/index
    https://europaems.jpl.nasa.gov/alfresco/service/index

# Miscellaneous <a name="misc"></a>

#### [return to table of contents](#contents)

Sometimes, eclipse will lock the GUI with a popup saying that the user operation is waiting on other tasks, typically a rebuild.
If this is annoying because the rebuild takes a long time, you can create a ram disk to store the target directory and create a soft link to it.
This should speed up the rebuild significantly.

     sudo mkdir /mnt/ramdisk
     sudo mount -t tmpfs -o size=1024m tmpfs /mnt/ramdisk
     cd ~/git/alfresco-view-repo
     mv target /mnt/ramdisk
     ln -s /mnt/ramdisk/target .

For an mvn purge on the ramdisk, running this command may be helpful:
     mvn clean -Ppurge; mkdir /mnt/ramdisk/target; mkdir /mnt/ramdisk/alf_data; ./runserver.sh

THE FOLLOWING DOESN'T WORK: To attempt to turn off indexing (maybe because it slows down junit test runs), change VALIDATE to NONE for index.recovery.mode in
 
    target/mms-repo-war/WEB-INF/classes/alfresco/repository.properties

To evaluate a Java expression from a webpage, go to 

    http://localhost:8080/alfresco/wcs/java_query

To evaluate a Java expression, in this example, Math.Min(1,2), from the command line

    curl -w "%{http_code}\n" -u admin:admin -X POST -H "Content-Type:text/plain" "http://localhost:8080/alfresco/service/java_query?verbose=false" --data 'Math.min(1,2)'


#Debugging Overview <a name="debug-overview"></a>

#### [return to table of contents](#contents)

**IMPORTANT:** Push code changes to your own branch. Merge them with workspaces branch at Cin-Young's and Brad's consent.

##Eclipse Environment

###Set-Up:
   	Add view-repo debug configuration in eclipse:
		Run > Debug Configurations > Remote Java Application > view-repo > change port to alfresco port (usually 8080) > Apply
		Click on bug icon (select view-repo under drop-down menu by clicking on the arrow key)
	
	To customize editor:
		Window > Preferences
		For line numbers: > General > Text Editors > click "Show line numbers"
		For java-specific customizations: > Java > Editor
	
	To add breakpoints: Double click on left margin (left of line numbers) 
	
	To skip breakpoints: Run > Skip All Breakpoints  OR  click on icon in quick-access toolbar with line across it
	
	To add redo shortcut: Window > Preferences > General > Keys > Search "redo" > Change binding to Ctrl+Y or your choice
	
	Note: For easier debugging, copy-paste stacktrace on console window to access lines referenced in error message

###Eclipse Shortcuts:

Note: Following shortcuts are mainly for Linux.
	
|       **ShortCut**      |       **Description**    |
|-------------------------|--------------------------|
| Ctrl+Shift+R            | Resource/File finder     |
| Ctrl+L                  | Go to line               |
| F3                      | Go to Definition         |
| F4                      | Type Hierarchy           |
| Ctrl+Alt+H              | Call Hierachy            |


##Code Summary

### Main Files

	[../alfresco-view-repo/src/main/amp/config/alfresco/module/view-repo/context] :
		- mms-service-context.xml : bean scripts used for linking Java or Javascript classes with CRUD requests for various URLs on alfresco
			* Changes made to mms-service-context.xml can be viewed in class2url.html
			* Make sure to run class2UrlMapping.py in the same directory to update class2url.html
	
	[../alfresco-view-repo/src/main/amp/config/alfresco/extension/templates/webscripts/gov/nasa/jpl/javawebscripts/view/] :
		- contains descriptor files for various URLs and bean scripts
			
	[../alfresco-view-repo/src/main/java/gov/nasa/jpl/view_repo/webscripts/] :
		- contains Java classes (webscripts) used by the webservice when CRUD requests are made 
	
	[../alfresco-view-repo/src/main/amp/config/alfresco/module/view-repo/] 	:
		- sysmlModel.xml - dictionary/specifications for all SysML types and aspects, and their respective properties; defines content model
		- emsModel.xml - also defines the content model
	
	[../alfresco-view-repo/src/main/java/gov/nasa/jpl/view_repo/util/] :
		- EmsScriptNode.java : class for model elements 
			* ingestJson() - for reading from Json object and updating node
			* toJsonObject () - for writing to Json objects from node
		- Acm.java : acm = "alfersco content model"; is a static class that has json property names and the alfresco content model types that correspond to them (like DictionaryService)
	
### Supporting Items:
	
	DictionaryService (interface, provided by Alfresco) 
		- tells you what is in the content model (which defines how we store things in Alfresco database)
		- provides information on content meta-data like types and aspects (which are equivalent to MagicDraw stereotypes) 
	
	http://localhost:8080/mms/raml.index
		- raml API (DESIGN), shows intended CRUD requests for web server/alfresco
		
	http://localhost:8080/alfresco/service/debug?on 
		- check if debugging is on.
	
	Debug.isOn() - check if debugging is on
	Debug.turnOn() or Debug.turnOff() - turn debugging on or off 
	
	executeImpl - gets called first when you invoke webserver with a HTTP request (crud request - e.g. during test cases)
	
### Alfresco-View-Share In Parallel With View-Repo:

	clone alfresco-view-share repository
	change JVM environment address variable to something else (ex: change 10000 to 10001)
		echo $MAVEN_OPTS
		copy output and change the address to some other number
		export MAVEN_OPTS="PASTE OUTPUT HERE WITH NEW ADDRESS VARIABLE"     (Note: Include quotation marks)
	change directory to alfresco-view-share
	mvn package                                   (only needed the first time around)
	mvn integration-test -Pamp-to-war             (needed every time view-share is to be displayed)
	open localhost:8081/alfresco/share/
	sign in as admin:admin
	
	run curl commands/test cases against view-repo database
	refresh view-share page
	the final, integrated content model is displayed in view-share in your dashboard (under sites) and in Repository
	
### Running Multiple Alfrescos with Tomcat on Different Ports via Maven

You may be able to run multiple alfresco servers on the same machine by changing ports used and permissions to a file.  

You may have problems opening a file resulting in a lot of exceptions and SEVERE errors when bringing up alfresco.  You may need write access to /tmp/Alfresco.  One way to do this:

    sudo chmod 777 /tmp/Alfresco

There's a way to specify a different file so that permissions are not an issue.  TODO: What is it?

There are port assignments that must be unique.  Remember that these are specified for the alfresco-view-share as well as the alfresco-view-repo.  You may be able to avoid changing ports if you can configure the server to use a different ip address: http://www.appnovation.com/blog/running-multiple-alfresco-server-instances-same-linux-machine.

To change ports to 9091 for Alfresco and 10002 for the debugger, run this from the alfresco-view-repo directory:

    . switchPorts.sh 9091 10002

This script automates the detailed instructions below so that you don't have to read them. 

### Detailed Instructions on Running Multiple Alfrescos

You shouldn't need to read these if the switchPorts.sh described above works.


To have the alfresco web server run on a different port add 

    -Dmaven.tomcat.port=9091
    
to the command line options (such as in runserver.sh).  To run the regression test on this server, you need to edit test-data/javawebscripts/regression_lib.py and set the host to the new port:

    HOST="localhost:9091"

The port for connecting a debugger must also be unique.  You probably have this specified as 10000 in your MAVEN_OPTS environment variable.  Change it to something like 10002:

    -Xrunjdwp:transport=dt_socket,address=10002

There are instructions elsewhere on this page on how to change MAVEN_OPTS.

You also need to set the RMI ports.  By default, these port numbers start with 50500.  If you don't set these to be different than a running server, you'll see "java.rmi.server.ExportException: Port already in use: 50501," and alfresco won't work.  You can assign the ports to 0 so that the they are chosen randomly from unused ports.  Edit view-repo/src/test/properties/local/alfresco-global.properties and add/edit to include the following assignments: 

    avm.rmi.service.port=0
    avmsync.rmi.service.port=0
    attribute.rmi.service.port=0
    authentication.rmi.service.port=0
    repo.rmi.service.port=0
    action.rmi.service.port=0
    wcm-deployment-receiver.rmi.service.port=0
    monitor.rmi.service.port=0

view-repo.launch is used by Eclipse to attach its debugger to a running Alfresco.  The debug port is specified in the file.  So, to make sure you attach to port used by Alfresco as specified in $MAVEN_OPTS, edit this line in view-repo.launch:

    <mapEntry key="port" value="10002"/>

The tomcat port (ex. 9091) and the debug port (ex. 10002) may or may not be opened in the firewall.  You may need to open these to access the server locally, and they must be open to access the server remotely.  The instructions vary for different versions of operating systems and are not included here.  For reference, here are some commands that may help you for a linux OS:


    # see what ports are open (LISTEN)
    netstat -anp

    # edit iptables to add ports to open
    sudo vi /etc/sysconfig/iptables 
    
    # restart iptables
    sudo /etc/init.d/iptables restart
    
    # if that didn't open ports, restart network
    sudo /etc/init.d/network restart
    
If on the amazon cloud, the ports may also need to be opened from the AWS console.

