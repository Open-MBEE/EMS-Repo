package gov.nasa.jpl.pma;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.xml.fastinfoset.stax.events.Util;

import gov.nasa.jpl.mbee.util.FileUtils;
import gov.nasa.jpl.mbee.util.Utils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;

public class JenkinsBuildConfig {
/*  parameters from PMADrone on 2/16/2016
            JOB_ID=job0000
            DOCUMENTS="Basic Document, Rapid Table Document"
            #DOCUMENTS="_18_1111_111_111"
            #MMS_SERVER=cae-jenkins
            MMS_USER=mmsadmin
            MMS_PASSWORD=letmein
            TEAMWORK_PROJECT="Intern Testing Project"
            TEAMWORK_SERVER="cae-tw.jpl.nasa.gov"
            TEAMWORK_PORT=18001
            TEAMWORK_USER=tester
            TEAMWORK_PASSWORD=AuToTeStEr
            WORKSPACE=master
*/
    
    
    static final         String  outputEncoding     = "UTF-8";
    private static final boolean DEBUG              = true;
    private              String  configTemplatePath = "./BuildConfigTemplate.xml";
    private              String  jobID              = "job0000";
    private              String  documentID         = "_18_1111_111_111";
    private              String  mmsServer          = "cae-ems-uat.jpl.nasa.gov";
    private              String  mmsUser            = "mmsadmin";
    private              String  mmsPassword        = "letmein";
    //private              String  teamworkProject    = "MD Forever";
    private              String  teamworkProject    = "Intern Testing Project";
    //private              String  teamworkServer     = "secae-fn.jpl.nasa.gov";
    private              String  teamworkServer     = "cae-tw-uat.jpl.nasa.gov";
    private              String  teamworkPort       = "18051";
    //private              String  teamworkUser       = "mmsadmin";
    //private              String  teamworkPassword   = "letmein";
    private              String  teamworkUser       = "docgen";
    private              String  teamworkPassword   = "docgen";
    private              String  workspace          = "master";
    // jdk might have to be (Default)
    private              String  jdkVersion         = "jdk1.8.0_45";
    private              String  gitURL             = "git@github.jpl.nasa.gov:mbee-dev/ems-rci.git";
    private              String  gitCredentials     = "075d11db-d909-4e1b-bee9-c89eec0a4a13";
    private              String  gitBranch          = "*/develop";
    private              String  schedule           = null;

    private String magicdrawSchedulingCommand = null;
    public JenkinsBuildConfig() {
        // TODO Auto-generated constructor stub
    }

    public void parseConfigDOM(String fileNamePath) {
        // Create the DocumentBuilder
        DocumentBuilderFactory factory        = DocumentBuilderFactory.newInstance();
        DocumentBuilder        builder        = null;
        Document               configDocument = null;
        File                   xmlFile        = null;

        // Create a Document from a file or stream
        try {
            builder = factory.newDocumentBuilder();

            if (fileNamePath.isEmpty()) {
                fileNamePath = configTemplatePath;
            }

            xmlFile = new File(fileNamePath);

            if (builder != null) {
                configDocument = builder.parse(xmlFile);
                if (configDocument != null) {
                    configDocument.getDocumentElement().normalize();
                }
            }

            XPath xPath = XPathFactory.newInstance().newXPath();

            String   buildersExpression = "/project/builders";
            NodeList nodeList           = (NodeList) xPath.compile(buildersExpression)
                    .evaluate(configDocument, XPathConstants.NODESET);
            for (int index = 0; index < nodeList.getLength(); index++) {
                Node nNode = nodeList.item(index);
                if (DEBUG) {

                    System.out.println("\nCurrent builder : " + nNode.getNodeName());
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element currentElement = (Element) nNode;
                        System.out.println("Current command is " +
                                currentElement.getElementsByTagName("hudson.tasks.Shell").item(0).getTextContent());
                    }
                }
            }
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
    }

    public String generateBaseConfigXML() {
   // public String generateBaseConfigXML( JenkinsBuildConfig config ) {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document        doc        = docBuilder.newDocument();
            Element         tempElement;

            // Root Element
            Element rootElement = doc.createElement("project");
            // this would specify a maven plugin... 
            //rootElement.setAttribute("plugin", "maven-plugin@2.10");
            doc.appendChild(rootElement);

            tempElement = doc.createElement("actions");
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("description");
            tempElement.appendChild(doc.createTextNode(" "));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("keepDependencies");
            tempElement.appendChild(doc.createTextNode("false"));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("properties");
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("scm");
            tempElement.setAttribute("class", "hudson.plugins.git.GitSCM");
            tempElement.setAttribute("plugin", "hudson.plugins.git.GitSCM");

            Element scmTempElement1 = doc.createElement("configVersion");
            scmTempElement1.appendChild(doc.createTextNode("2"));
            tempElement.appendChild(scmTempElement1);

            scmTempElement1 = doc.createElement("userRemoteConfigs");
            Element scmTempElement2 = doc.createElement("hudson.plugins.git.UserRemoteConfig");
            Element scmTempElement3 = doc.createElement("url");
            scmTempElement3.appendChild(doc.createTextNode(this.gitURL));
            scmTempElement2.appendChild(scmTempElement3);
            scmTempElement3 = doc.createElement("credentialsId");
            scmTempElement3.appendChild(doc.createTextNode(this.gitCredentials));
            scmTempElement2.appendChild(scmTempElement3);
            scmTempElement1.appendChild(scmTempElement2);
            tempElement.appendChild(scmTempElement1);

            scmTempElement1 = doc.createElement("branches");
            scmTempElement2 = doc.createElement("hudson.plugins.git.BranchSpec");
            scmTempElement3 = doc.createElement("name");
            scmTempElement3.appendChild(doc.createTextNode(this.gitBranch));
            scmTempElement2.appendChild(scmTempElement3);
            scmTempElement1.appendChild(scmTempElement2);
            tempElement.appendChild(scmTempElement1);

            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("canRoam");
            tempElement.appendChild(doc.createTextNode("true"));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("disabled");
            tempElement.appendChild(doc.createTextNode("false"));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("blockBuildWhenDownstreamBuilding");
            tempElement.appendChild(doc.createTextNode("false"));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("blockBuildWhenUpstreamBuilding");
            tempElement.appendChild(doc.createTextNode("false"));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("jdk");
            tempElement.appendChild(doc.createTextNode(this.jdkVersion));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("triggers");
            if ( !Utils.isNullOrEmpty( getSchedule() ) ) {
                Element hudTrig = doc.createElement("hudson.triggers.TimerTrigger");
                Element spec = doc.createElement("spec");
                spec.appendChild( doc.createTextNode( this.schedule ) );
                hudTrig.appendChild( spec );
                tempElement.appendChild( hudTrig );
            }
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("concurrentBuild");
            tempElement.appendChild(doc.createTextNode("false"));
            rootElement.appendChild(tempElement);

            /**
             *
             * Builder Section
             * This is where the console / shell commands will be placed
             */
            tempElement = doc.createElement("builders");
            Element hudson  = doc.createElement("hudson.tasks.Shell");
            Element command = doc.createElement("command");
            
            // Get the script file and put the string contents in
            // magicdrawSchedulingCommand.
            String s = null;

            if ( Utils.isNullOrEmpty( s ) ) {
                String sysEnvStr = System.getenv("TOMCAT_HOME");

                if( sysEnvStr == null )
                    sysEnvStr = "";
                
                File tomcat = new File( sysEnvStr ); 
                String curDir = null;
                
                // try to get the tomcat environment var to find the file
                if( Util.isEmptyString( sysEnvStr ) || tomcat == null) {
                    curDir = System.getProperty( "user.dir" ); 
                }
                else {
                    curDir = tomcat.toString() + "/webapps/alfresco";
                    System.setProperty( "user.dir", curDir );
                }
              
                // what are we using this for?
                File alfresco = new File( curDir );
                
                File file = FileUtils.findFile( "docwebJenkinsScript.sh" );
                  
                System.out.println( "The current working directory is: " + System.getProperty( "user.dir" )  );

                try {
                    s = FileUtils.fileToString( file );
                    this.magicdrawSchedulingCommand = s;
                    System.out.println("\n\n\nGot file for command in the following path: " + file.getPath() + "\n\n\n");
                } catch ( FileNotFoundException e ) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            command.appendChild(doc.createTextNode(this.magicdrawSchedulingCommand));
            hudson.appendChild(command);
            tempElement.appendChild(hudson);
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("aggregatorStyleBuild");
            tempElement.appendChild(doc.createTextNode("false"));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("incrementalBuild");
            tempElement.appendChild(doc.createTextNode("false"));
            rootElement.appendChild(tempElement);

            // This has a purposeful spelling error, Jenkins for some reason uses
            //  ignoreUpstrem instead of ignoreUpstream
            tempElement = doc.createElement("ignoreUpstremChanges");
            tempElement.appendChild(doc.createTextNode("false"));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("archivingDisabled");
            tempElement.appendChild(doc.createTextNode("false"));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("siteArchivingDisabled");
            tempElement.appendChild(doc.createTextNode("false"));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("fingerprintingDisabled");
            tempElement.appendChild(doc.createTextNode("false"));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("resolveDependencies");
            tempElement.appendChild(doc.createTextNode("false"));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("processPlugins");
            tempElement.appendChild(doc.createTextNode("false"));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("mavenValidationLevel");
            tempElement.appendChild(doc.createTextNode("-1"));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("runHeadless");
            tempElement.appendChild(doc.createTextNode("false"));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("aggregatorStyleBuild");
            tempElement.appendChild(doc.createTextNode("true"));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("disableTriggerDownstreamProjects");
            tempElement.appendChild(doc.createTextNode("false"));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("blockTriggerWhenBuilding");
            tempElement.appendChild(doc.createTextNode("true"));
            rootElement.appendChild(tempElement);

            tempElement = doc.createElement("aggregatorStyleBuild");
            tempElement.appendChild(doc.createTextNode("true"));
            rootElement.appendChild(tempElement);

            // Define the build configuration settings
            Element settingsElement = doc.createElement("settings");
            settingsElement.setAttribute("class", "jenkins.mvn.DefaultSettingsProvider");
            rootElement.appendChild(settingsElement);

            Element globalSettingsElement = doc.createElement("globalSettings");
            globalSettingsElement.setAttribute("class", "jenkins.mvn.FilePathGlobalSettingsProvider");
            tempElement = doc.createElement("path");
            tempElement.appendChild(doc.createTextNode("/opt/local/maven/conf/settings.xml"));
            globalSettingsElement.appendChild(tempElement);
            rootElement.appendChild(globalSettingsElement);

            Element infoElement          = doc.createElement("info");
            Element buildWrappers        = doc.createElement("buildWrappers");
            Element propertiesContent    = doc.createElement("propertiesContent");
            Element injectEnvironmentVar = doc.createElement("EnvInjectBuildWrapper");
            propertiesContent.appendChild(doc.createTextNode("\n"));
            propertiesContent.appendChild(doc.createTextNode("JOB_ID=" + this.jobID + "\n"));
            propertiesContent.appendChild(doc.createTextNode("DOCUMENTS=" + this.documentID + "\n"));
            propertiesContent.appendChild(doc.createTextNode("MMS_SERVER=" + this.mmsServer + "\n"));
            propertiesContent.appendChild(doc.createTextNode("MMS_USER=" + this.mmsUser + "\n"));
            propertiesContent.appendChild(doc.createTextNode("MMS_PASSWORD=" + this.mmsPassword + "\n"));
            propertiesContent.appendChild(doc.createTextNode("TEAMWORK_PROJECT=" + this.teamworkProject + "\n"));
            propertiesContent.appendChild(doc.createTextNode("TEAMWORK_SERVER=" + this.teamworkServer + "\n"));
            propertiesContent.appendChild(doc.createTextNode("TEAMWORK_PORT=" + this.teamworkPort + "\n"));
            propertiesContent.appendChild(doc.createTextNode("TEAMWORK_USER=" + this.teamworkUser + "\n"));
            propertiesContent.appendChild(doc.createTextNode("TEAMWORK_PASSWORD=" + this.teamworkPassword + "\n"));
            propertiesContent.appendChild(doc.createTextNode("WORKSPACE=" + this.workspace + "\n"));
            injectEnvironmentVar.setAttribute("plugin", "envinject@1.91.3");
            infoElement.appendChild(propertiesContent);
            injectEnvironmentVar.appendChild(infoElement);
            buildWrappers.appendChild(injectEnvironmentVar);
            rootElement.appendChild(buildWrappers);

            tempElement = doc.createElement("reporters");
            rootElement.appendChild(tempElement);
            tempElement = doc.createElement("publishers");
            Element artifactArchiver = doc.createElement("hudson.tasks.ArtifactArchiver");
            Element secondTempElem = doc.createElement( "artifacts" );

            secondTempElem.appendChild( doc.createTextNode( "mdNotificationWindowText.html" ));
            artifactArchiver.appendChild( secondTempElem );

            secondTempElem = doc.createElement("allowEmptyArchive");
            secondTempElem.appendChild( doc.createTextNode( "false" ) );
            artifactArchiver.appendChild( secondTempElem );

            secondTempElem = doc.createElement("onlyIfSuccessful");
            secondTempElem.appendChild( doc.createTextNode( "false" ) );
            artifactArchiver.appendChild( secondTempElem );

            secondTempElem = doc.createElement("fingerprint");
            secondTempElem.appendChild( doc.createTextNode( "false" ) );
            artifactArchiver.appendChild( secondTempElem );

            secondTempElem = doc.createElement("defaultExcludes");
            secondTempElem.appendChild( doc.createTextNode( "true" ) );
            artifactArchiver.appendChild( secondTempElem );

            tempElement.appendChild( artifactArchiver );

            rootElement.appendChild(tempElement);

            rootElement.appendChild(doc.createElement("prebuilders"));
            rootElement.appendChild(doc.createElement("postbuilders"));

            Element runPostStepsIfResultsElement = doc.createElement("runPostStepsIfResult");

            tempElement = doc.createElement("name");
            tempElement.appendChild(doc.createTextNode("FAILURE"));
            runPostStepsIfResultsElement.appendChild(tempElement);

            tempElement = doc.createElement("ordinal");
            tempElement.appendChild(doc.createTextNode("2"));
            runPostStepsIfResultsElement.appendChild(tempElement);

            runPostStepsIfResultsElement.appendChild(doc.createElement("color").appendChild(doc.createTextNode("RED")));
            runPostStepsIfResultsElement.appendChild(doc.createElement("completeBuild").appendChild(doc.createTextNode("true")));

            rootElement.appendChild(runPostStepsIfResultsElement);
            //  Save this in case the above code does not work and an element needs to be made for each individual build tag
            //            Element Element                     = doc.createElement("");

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer        transformer        = transformerFactory.newTransformer();
            DOMSource          source             = new DOMSource(doc);
            StringWriter       stringWriter       = new StringWriter();
            //StreamResult       result             = new StreamResult(new File("./test-output.xml"));
            StreamResult       result             = new StreamResult(stringWriter);//new File("./test-output.xml"));
            transformer.transform(source, result);
            StreamResult consoleResult = new StreamResult(System.out);
            transformer.transform(source, consoleResult);
            
            return stringWriter.toString();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return configTemplatePath;
    }

    /**
     * @return the documentID
     */
    public String getDocumentID() {
        return documentID;
    }

    /**
     * @param documentID
     *         the documentID to set
     */
    public void setDocumentID(String documentID) {
        this.documentID = documentID;
    }

    /**
     * @return the mmsServer
     */
    public String getMmsServer() {
        return mmsServer;
    }

    /**
     * @param mmsServer
     *         the mmsServer to set
     */
    public void setMmsServer(String mmsServer) {
        this.mmsServer = mmsServer;
    }

    /**
     * @return the mmsUser
     */
    public String getMmsUser() {
        return mmsUser;
    }

    /**
     * @param mmsUser
     *         the mmsUser to set
     */
    public void setMmsUser(String mmsUser) {
        this.mmsUser = mmsUser;
    }

    /**
     * @return the mmsPassword
     */
    public String getMmsPassword() {
        return mmsPassword;
    }

    /**
     * @param mmsPassword
     *         the mmsPassword to set
     */
    public void setMmsPassword(String mmsPassword) {
        this.mmsPassword = mmsPassword;
    }

    /**
     * @return the teamworkProject
     */
    public String getTeamworkProject() {
        return teamworkProject;
    }

    /**
     * @param teamworkProject
     *         the teamworkProject to set
     */
    public void setTeamworkProject(String teamworkProject) {
        this.teamworkProject = teamworkProject;
    }

    /**
     * @return the teamworkServer
     */
    public String getTeamworkServer() {
        return teamworkServer;
    }

    /**
     * @param teamworkServer
     *         the teamworkServer to set
     */
    public void setTeamworkServer(String teamworkServer) {
        this.teamworkServer = teamworkServer;
    }

    /**
     * @return the teamworkPort
     */
    public String getTeamworkPort() {
        return teamworkPort;
    }

    /**
     * @param teamworkPort
     *         the teamworkPort to set
     */
    public void setTeamworkPort(String teamworkPort) {
        this.teamworkPort = teamworkPort;
    }

    /**
     * @return the teamworkUser
     */
    public String getTeamworkUser() {
        return teamworkUser;
    }

    /**
     * @param teamworkUser
     *         the teamworkUser to set
     */
    public void setTeamworkUser(String teamworkUser) {
        this.teamworkUser = teamworkUser;
    }

    /**
     * @return the teamworkPassword
     */
    public String getTeamworkPassword() {
        return teamworkPassword;
    }

    /**
     * @param teamworkPassword
     *         the teamworkPassword to set
     */
    public void setTeamworkPassword(String teamworkPassword) {
        this.teamworkPassword = teamworkPassword;
    }

    /**
     * @return the workspace
     */
    public String getWorkspace() {
        return workspace;
    }

    /**
     * @param workspace
     *         the workspace to set
     */
    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    /**
     * @return the configTemplatePath
     */
    public String getConfigTemplatePath() {
        return configTemplatePath;
    }

    /**
     * @param configTemplatePath
     *         the configTemplatePath to set
     */
    public void setConfigTemplatePath(String configTemplatePath) {
        this.configTemplatePath = configTemplatePath;
    }

    public void setJDK(String version) {
        this.jdkVersion = version;
    }

    public String getJDKVersion() {
        return this.jdkVersion;
    }

    public String getJobID() {
        return this.jobID;
    }

    public void setJobID(String jobID) {
        this.jobID = jobID;
    }

    /**
     * @return the schedule
     */
    public String getSchedule() {
        return schedule;
    }

    /**
     * @param schedule the schedule to set
     */
    public void setSchedule( String schedule ) {
        this.schedule = schedule;
    }
}
