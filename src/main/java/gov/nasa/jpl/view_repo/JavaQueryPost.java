package gov.nasa.jpl.view_repo;

import gov.nasa.jpl.ae.event.Timepoint;
import gov.nasa.jpl.ae.magicdrawPlugin.modelQuery.EmfUtils;
import gov.nasa.jpl.ae.util.ClassUtils;
import gov.nasa.jpl.ae.util.Debug;
import gov.nasa.jpl.ae.util.JavaEvaluator;
import gov.nasa.jpl.ae.util.JavaToConstraintExpression;
import gov.nasa.jpl.ae.util.MoreToString;
import gov.nasa.jpl.ae.util.Utils;
import gov.nasa.jpl.ae.xml.EventXmlToJava;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Repository;

import org.alfresco.repo.nodelocator.NodeLocatorService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.springframework.extensions.surf.util.Content;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class JavaQueryPost extends DeclarativeWebScript {

	private NodeService nodeService;
    
    private NodeLocatorService nodeLocatorService;

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
    
    public void setNodeLocatorService(NodeLocatorService nodeLocatorService) {
        this.nodeLocatorService = nodeLocatorService;
    }
    
    private String n() {
        return "foo";
    }
    
    private String getReply() {
    	return "120";
    }
    
    protected static Class< ? >[] getJunitClasses() {
        String packageName = "gov.nasa.jpl.view_repo.test";
        File path = new File( "src" + File.separator + "test" + File.separator + "java" );
        File[] testClassFiles = EventXmlToJava.getJavaFileList( path  ); // could alternatively get all classes in the package
        //Package pkg = Package.getPackage( packageName );
        if ( testClassFiles == null ) {
            Debug.error("Path doesn't exist: " + path + ", " + path.getAbsolutePath() + ", " + path.getPath() );
            return new Class<?>[]{};
        }
        Class< ? >[] testClasses  = new Class<?>[testClassFiles.length];
        int i=0;
        for ( File f : testClassFiles ) {
            String clsName = f.getName().replaceFirst( "[.](java|class)$", "" );
            Class< ? > cls = ClassUtils.getClassForName( clsName, packageName, false );
            testClasses[i++] = cls;
        }
        return testClasses;
    }
    
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();
//		NodeRef root = nodeLocatorService.getNode("companyhome", null, null);
//		int count = nodeService.countChildAssocs(root, true);
//		//model.put("reply", n());//Integer.toString(count));

        Content query = req.getContent();

//        boolean runningJunitTests = req.getParameterNames() != null && req.getParameterNames().length > 1; 
//		if ( runningJunitTests ) {
//		    // put back what was already there
//		    model.put( "query", req.getParameter("query") );
//            model.put( "reply", req.getParameter("reply") );
//            
//            System.out.println( "\n\n\nDOING JUNIT TESTS!\n\n" );
//            JUnitCore junit = new JUnitCore();
//            Class< ? >[] testClasses = getJunitClasses();
//            Result result = junit.run( testClasses );
//            return model;
//		}
		
		String qString = null;
        try {
            qString = query.getContent();
        } catch ( IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if ( Utils.isNullOrEmpty( qString ) ) {
            Debug.error("Empty query string!");
            return model;
        }
        if (qString.equals( "dojunittest" ) ) {
            System.out.println( "\n\n\nDOING JUNIT TESTS!\n\n" );
            JUnitCore junit = new JUnitCore();
            Class< ? >[] testClasses = getJunitClasses();
            Result result = junit.run( testClasses );
            model.put( "query", "Run JUnit tests" );
            model.put( "reply", (String)EmfUtils.spewObject( result, "--" ) );
            return model;
        }
        
        String packageName = JavaQueryPost.class.getPackage().toString().replace( "package ", "" );
		model.put( "query", qString );
		System.out.println("\n\n\n" + Timepoint.now().toTimestamp() + "\nEvaluating: \"" + qString + "\"\n\n");
		Object reply = null;
		try {
		    reply = JavaEvaluator.evaluate( qString,  packageName );
		} catch ( Throwable e ) {
		    System.err.println( e.getClass().getSimpleName() + ": " + e.getLocalizedMessage() );
		    e.printStackTrace();
		}
		String replyString = "null";
		if ( reply != null ) replyString = MoreToString.Helper.toString( reply ); 
        System.out.println("\n\n\n" + Timepoint.now().toTimestamp() + "\nResult = : \"" + replyString + "\"\n\n");
		model.put("reply", replyString );
		return model;
	}
}
