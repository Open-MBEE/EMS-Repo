# Basic Build Config XML File

This is the base template for Jenkins Schedule Build Configuration

-------

<?xml version='1.0' encoding='UTF-8'?>
<project>
  <actions/>
  <description></description>
  <logRotator class="hudson.tasks.LogRotator">
    <daysToKeep>-1</daysToKeep>
    <numToKeep>-1</numToKeep>
    <artifactDaysToKeep>-1</artifactDaysToKeep>
    <artifactNumToKeep>-1</artifactNumToKeep>
  </logRotator>
  <keepDependencies>false</keepDependencies>
  <properties/>
  <scm class="hudson.plugins.git.GitSCM" plugin="git@2.3.5">
    <configVersion>2</configVersion>
    <userRemoteConfigs>
      <hudson.plugins.git.UserRemoteConfig>
        <url>git@github.jpl.nasa.gov:mbee-dev/ems-rci.git</url>
        <credentialsId>075d11db-d909-4e1b-bee9-c89eec0a4a13</credentialsId>
      </hudson.plugins.git.UserRemoteConfig>
    </userRemoteConfigs>
    <branches>
      <hudson.plugins.git.BranchSpec>
        <name>*/develop</name>
      </hudson.plugins.git.BranchSpec>
    </branches>
    <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
    <submoduleCfg class="list"/>
    <extensions/>
  </scm>
  <canRoam>true</canRoam>
  <disabled>false</disabled>
  <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>
  <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>
  <jdk>jdk1.7.0_76</jdk>
  <triggers/>
  <concurrentBuild>false</concurrentBuild>
  <builders>
    <hudson.tasks.Shell>
      <command>JOB_ID=job0000
DOCUMENTS=&quot;_18_1111_111_111&quot;
MMS_SERVER=cae-jenkins
MMS_USER=mmsadmin
MMS_PASSWORD=letmein
TEAMWORK_PROJECT=&quot;MD Forever&quot;
TEAMWORK_SERVER=&quot;secae-fn.jpl.nasa.gov&quot;
TEAMWORK_PORT=18001
TEAMWORK_USER=mmsadmin
TEAMWORK_PASSWORD=letmein
WORKSPACE=master


# Tell MMS that this job has started
status=running
curl -w &quot;\n%{http_code}\n&quot; -u ${MMS_USER}:${MMS_PASSWORD} -X POST -H Content-Type:application/json --data &quot;{\&quot;jobs\&quot;:[{\&quot;id\&quot;:\&quot;${JOB_ID}\&quot;, \&quot;\&quot;status\&quot;:\&quot;${status}\&quot;]}&quot; &quot;${MMS_SERVER}/alfresco/service/workspaces/master/jobs&quot;


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
if [ &quot;$status&quot; == &quot;running&quot; ]; then status=completed; fi
curl -w &quot;\n%{http_code}\n&quot; -u ${MMS_USER}:${MMS_PASSWORD} -X POST -H Content-Type:application/json --data &quot;{\&quot;elements\&quot;:[{\&quot;id\&quot;:\&quot;${JOB_ID}\&quot;, \&quot;status\&quot;:\&quot;${status}\&quot;]}&quot; &quot;${MMS_SERVER}/alfresco/service/workspaces/master/elements&quot;
</command>
    </hudson.tasks.Shell>
  </builders>
  <publishers/>
  <buildWrappers>
    <hudson.plugins.ws__cleanup.PreBuildCleanup plugin="ws-cleanup@0.28">
      <deleteDirs>false</deleteDirs>
      <cleanupParameter></cleanupParameter>
      <externalDelete></externalDelete>
    </hudson.plugins.ws__cleanup.PreBuildCleanup>
  </buildWrappers>
</project>


# This is an example of the View Repo config.xml

<?xml version='1.0' encoding='UTF-8'?>
<maven2-moduleset plugin="maven-plugin@2.10">
  <actions/>
  <description>Packages the enterprise amp from the develop branch for a nightly build. Uses robot framework to create build and rest results.</description>
  <logRotator class="hudson.tasks.LogRotator">
    <daysToKeep>-1</daysToKeep>
    <numToKeep>-1</numToKeep>
    <artifactDaysToKeep>-1</artifactDaysToKeep>
    <artifactNumToKeep>-1</artifactNumToKeep>
  </logRotator>
  <keepDependencies>false</keepDependencies>
  <properties/>
  <scm class="hudson.plugins.git.GitSCM" plugin="git@2.3.5">
    <configVersion>2</configVersion>
    <userRemoteConfigs>
      <hudson.plugins.git.UserRemoteConfig>
        <url>git@github.jpl.nasa.gov:mbee-dev/alfresco-view-repo.git</url>
        <credentialsId>075d11db-d909-4e1b-bee9-c89eec0a4a13</credentialsId>
      </hudson.plugins.git.UserRemoteConfig>
    </userRemoteConfigs>
    <branches>
      <hudson.plugins.git.BranchSpec>
        <name>*/robot-testing</name>
      </hudson.plugins.git.BranchSpec>
    </branches>
    <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
    <submoduleCfg class="list"/>
    <extensions/>
  </scm>
  <canRoam>true</canRoam>
  <disabled>false</disabled>
  <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>
  <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>
  <jdk>(Default)</jdk>
  <triggers>
    <hudson.triggers.TimerTrigger>
      <spec>H 22 * * *</spec>
    </hudson.triggers.TimerTrigger>
  </triggers>
  <concurrentBuild>false</concurrentBuild>
  <rootModule>
    <groupId>gov.nasa.jpl</groupId>
    <artifactId>mms-repo</artifactId>
  </rootModule>
  <goals>package -Dmaven.test.skip=true -Pmbee-dev -U -X</goals>
  <aggregatorStyleBuild>true</aggregatorStyleBuild>
  <incrementalBuild>false</incrementalBuild>
  <ignoreUpstremChanges>true</ignoreUpstremChanges>
  <archivingDisabled>false</archivingDisabled>
  <siteArchivingDisabled>false</siteArchivingDisabled>
  <fingerprintingDisabled>false</fingerprintingDisabled>
  <resolveDependencies>false</resolveDependencies>
  <processPlugins>false</processPlugins>
  <mavenValidationLevel>-1</mavenValidationLevel>
  <runHeadless>false</runHeadless>
  <disableTriggerDownstreamProjects>true</disableTriggerDownstreamProjects>
  <settings class="jenkins.mvn.DefaultSettingsProvider"/>
  <globalSettings class="jenkins.mvn.DefaultGlobalSettingsProvider"/>
  <reporters/>
  <publishers>
    <hudson.plugins.robot.RobotPublisher plugin="robot@1.6.2">
      <outputPath></outputPath>
      <reportFileName>report.html</reportFileName>
      <logFileName>log.html</logFileName>
      <outputFileName>output.xml</outputFileName>
      <disableArchiveOutput>false</disableArchiveOutput>
      <passThreshold>90.0</passThreshold>
      <unstableThreshold>60.0</unstableThreshold>
      <otherFiles>
        <string></string>
      </otherFiles>
      <onlyCritical>true</onlyCritical>
    </hudson.plugins.robot.RobotPublisher>
  </publishers>
  <buildWrappers/>
  <prebuilders>
    <hudson.tasks.Maven>
      <targets>clean -Ppurge</targets>
      <mavenName>Maven 3.3.3</mavenName>
      <usePrivateRepository>false</usePrivateRepository>
      <settings class="jenkins.mvn.DefaultSettingsProvider"/>
      <globalSettings class="jenkins.mvn.FilePathGlobalSettingsProvider">
        <path>/opt/local/maven/conf/settings.xml</path>
      </globalSettings>
    </hudson.tasks.Maven>
    <hudson.tasks.Shell>
      <command># clean the database before use each time
psql -U mmsuser -f src/main/java/gov/nasa/jpl/view_repo/db/mms.sql mydb</command>
    </hudson.tasks.Shell>
  </prebuilders>
  <postbuilders>
    <hudson.tasks.Shell>
      <command># Run Regression Test
# robot test-data/javawebscripts/regression_suite.robot
./test-data/javawebscripts/executeJenkinsRobotTestSuite.sh</command>
    </hudson.tasks.Shell>
  </postbuilders>
  <runPostStepsIfResult>
    <name>FAILURE</name>
    <ordinal>2</ordinal>
    <color>RED</color>
    <completeBuild>true</completeBuild>
  </runPostStepsIfResult>
</maven2-moduleset>


## Basic Configuration Template with Inject Environment Variables

<?xml version='1.0' encoding='UTF-8'?>
<maven2-moduleset plugin="maven-plugin@2.10">
  <actions/>
  <description></description>
  <keepDependencies>false</keepDependencies>
  <properties/>
  <scm class="hudson.scm.NullSCM"/>
  <canRoam>true</canRoam>
  <disabled>false</disabled>
  <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>
  <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>
  <jdk>(Default)</jdk>
  <triggers/>
  <concurrentBuild>false</concurrentBuild>
  <aggregatorStyleBuild>true</aggregatorStyleBuild>
  <incrementalBuild>false</incrementalBuild>
  <ignoreUpstremChanges>false</ignoreUpstremChanges>
  <archivingDisabled>false</archivingDisabled>
  <siteArchivingDisabled>false</siteArchivingDisabled>
  <fingerprintingDisabled>false</fingerprintingDisabled>
  <resolveDependencies>false</resolveDependencies>
  <processPlugins>false</processPlugins>
  <mavenValidationLevel>-1</mavenValidationLevel>
  <runHeadless>false</runHeadless>
  <disableTriggerDownstreamProjects>false</disableTriggerDownstreamProjects>
  <blockTriggerWhenBuilding>true</blockTriggerWhenBuilding>
  <settings class="jenkins.mvn.DefaultSettingsProvider"/>
  <globalSettings class="jenkins.mvn.FilePathGlobalSettingsProvider">
    <path>/opt/local/maven/conf/settings.xml</path>
  </globalSettings>
  <reporters/>
  <publishers/>
  <buildWrappers>
    <EnvInjectBuildWrapper plugin="envinject@1.91.3">
      <info>
        <propertiesContent>
            ENV1=this
            ENV2=this
            ENV3=this
            ENV4=this
        </propertiesContent>
        <loadFilesFromMaster>false</loadFilesFromMaster>
      </info>
    </EnvInjectBuildWrapper>
  </buildWrappers>
  <prebuilders/>
  <postbuilders/>
  <runPostStepsIfResult>
    <name>FAILURE</name>
    <ordinal>2</ordinal>
    <color>RED</color>
    <completeBuild>true</completeBuild>
  </runPostStepsIfResult>
</maven2-moduleset>