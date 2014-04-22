package gov.nasa.jpl.view_repo.test;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import gov.nasa.jpl.ae.event.ConstraintExpression;
import gov.nasa.jpl.ae.event.DurativeEvent;
import gov.nasa.jpl.ae.event.Expression;
import gov.nasa.jpl.ae.event.Parameter;
import gov.nasa.jpl.ae.event.ParameterListener;
import gov.nasa.jpl.ae.event.ParameterListenerImpl;
import gov.nasa.jpl.ae.solver.ConstraintLoopSolver;
import gov.nasa.jpl.ae.sysml.SystemModelSolver;
import gov.nasa.jpl.ae.sysml.SystemModelToAeExpression;
import gov.nasa.jpl.ae.util.ClassData;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.MoreToString;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsSystemModel;
import gov.nasa.jpl.view_repo.util.NodeUtil;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class EmsSystemModelTest {
    
    public static EmsSystemModel model = null;
    public static SystemModelToAeExpression< EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel > sysmlToAe = null;
    public static ServiceRegistry services = NodeUtil.getServiceRegistry();
    protected static final String ADMIN_USER_NAME = "admin";
    protected static boolean wasOn;
    
    @AfterClass
    public static void cleanup() {
        if ( !wasOn ) Debug.turnOff();        
    }
    @BeforeClass
    public static void initAppContext() {
        wasOn = Debug.isOn();
        if ( !wasOn ) Debug.turnOn();
  
        model = new EmsSystemModel( services );
        sysmlToAe = new SystemModelToAeExpression< EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel >( model );
        //AuthenticationUtil.setRunAsUserSystem();
        AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);
        
        // TODO these curl commands dont seem to be working from the junit test, but work went sent manually
        // Load model for testing:
        //
        // Assuming git directory is in home directory, so model is in:
        // $HOME/git/alfresco-view-repo/test-data/javawebscripts/JsonData/expressionElements.json
        // See $HOME/git/alfresco-view-repo/test-data/javawebscripts/curl.tests.sh for environment variables and sample curl commands
        String userHome = System.getenv("HOME");
        // curl -w "%{http_code}" -u admin:admin -X POST -H "Content-Type:application/json" --data '{"name":"CY Test"}' "http://localhost:8080/view-repo/service/javawebscripts/sites/europa/projects/123456?fix=true&createSite=true"
        String curlCmd1 = "curl -w \"%{http_code}\" -u admin:admin -X POST -H \"Content-Type:application/json\" --data '{\"name\":\"CY Test\"}' \"http://localhost:8080/alfresco/service/javawebscripts/sites/europa/projects/123456?fix=true&createSite=true\"";
        //   curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/expressionElements.json $BASE_URL"sites/europa/projects/123456/elements\""
        String curlCmdTemp = String.format("%s/git/alfresco-view-repo/test-data/javawebscripts/JsonData/expressionElements.json \"http://localhost:8080/alfresco/service/javawebscripts/sites/europa/projects/123456/elements\"",userHome);
        String curlCmd2 = "curl -w \"%{http_code}\" -u admin:admin -X POST -H \"Content-Type:application/json\" --data @"+curlCmdTemp;
        
    	System.out.println("You must execute this command before running this test: "+curlCmd1);
    	System.out.println("You must execute this command before running this test: "+curlCmd2);

//        try {
//        	System.out.println("Executing command: "+curlCmd1);
//        	Runtime.getRuntime().exec(curlCmd1);
//        	System.out.println("Executing command: "+curlCmd2);
//        	Runtime.getRuntime().exec(curlCmd2);
//        }
//        catch (IOException e) {
//        	e.printStackTrace();	
//        }
      
//        System.out.println("LAUNCH DEBUGGER START!!!");
//        try {
//            Thread.sleep( 12000 );
//        } catch ( InterruptedException e ) {
//            e.printStackTrace();
//        }
//        System.out.println("LAUNCH DEBUGGER END!!!");

    }

    @Test
    public void testDidInit() {
        System.out.println( "testDidInit() model: " + model );
        assertNotNull( model );
        System.out.println( "testDidInit() sysmlToAe: " + sysmlToAe );
    }
    
    @Test
    public void testExpressionEvaluation() {
    	
        //NodeRef node = NodeUtil.findNodeRefById( "expr_32165", model.getServices() );
                
        System.out.println( "testExpressionEvaluation()" );
        
        Collection< EmsScriptNode > nodes = model.getElementWithName( null, "expr_32165" );
        
        System.out.println("\n*testExpressionEvaluation() findNodeRefByType: "+
        				  NodeUtil.findNodeRefByType("LiteralInteger", "@sysml\\:type:\"", services));
        
        System.out.println( "\n*testExpressionEvaluation() nodes: "
                            + MoreToString.Helper.toLongString( nodes ) );
        System.out.println( "\n*testExpressionEvaluation() got " + nodes.size() + " nodes." );

//        if ( Utils.isNullOrEmpty( nodes ) ) {
//            nodes = model.getElementWithName( null, "*" );
//            if ( !Utils.isNullOrEmpty( nodes ) ) {
//                System.out.println( "testExpressionEvaluation() got " + nodes.size() + " nodes." );
//            }
//            System.out.println( "testExpressionEvaluation() again, nodes : "
//                                + MoreToString.Helper.toLongString( nodes ) );
//        }
        
        assertNotNull( nodes );
        Assert.assertFalse( nodes.isEmpty() );
        EmsScriptNode node = nodes.iterator().next();
        assertNotNull( node );
        
//        System.out.println("\n*testExpressionEvaluation() node.getProperty(sysml:operand): "
//        					+ MoreToString.Helper.toLongString(node.getProperty(Acm.ACM_OPERAND)));
//        // this is an ArrayList but in EmsSystemModel it expects it to be a EmsScriptNode
//        System.out.println("\n*testExpressionEvaluation() node.getProperty(sysml:operand).getClass(): "
//				+ MoreToString.Helper.toLongString(node.getProperty(Acm.ACM_OPERAND).getClass()));
//        ArrayList<NodeRef> props = (ArrayList<NodeRef>)node.getProperty(Acm.ACM_OPERAND);
//        
//        for (NodeRef prop : props) {
//        	
//        	EmsScriptNode propNode = new EmsScriptNode(prop,services);
//        	System.out.println("\n*testExpressionEvaluation() propNode: "+propNode);
//        	System.out.println("\n*testExpressionEvaluation() propNode.name: "+propNode.getName() + " id: "+propNode.getId() + " type: "+propNode.getType());
//
//        	NodeRef valueOfElem = (NodeRef)propNode.getProperty(Acm.ACM_ELEMENT_VALUE_ELEMENT);
//        	System.out.println("\n*testExpressionEvaluation() propNode.elementValueOfElement: "+valueOfElem);
//
//        	if ( valueOfElem == null ) continue;
//        
//        	// This will get the name of the operator via ACM_NAME:
//        	EmsScriptNode valueOfElemNode = new EmsScriptNode(valueOfElem,services);
//        	System.out.println("\n*testExpressionEvaluation() valueOfElemNode.name: "+valueOfElemNode.getName() + " id: "+valueOfElemNode.getId() + " type: "+valueOfElemNode.getType() + " sysmlName: "+valueOfElemNode.getProperty(Acm.ACM_NAME));
//        	if ( valueOfElemNode.getType() == null ) continue;
//        	// If its not a Operation type, ie. Property type (the command arguments):
//        	if (!valueOfElemNode.getType().contains(Acm.JSON_OPERATION)) {
//        		
//        		ArrayList<NodeRef> argValues = (ArrayList<NodeRef>)valueOfElemNode.getProperty(Acm.ACM_VALUE);
//            	System.out.println("\n*testExpressionEvaluation() argValues: " +argValues);
//            	
//            	// The argument integer property:
//            	EmsScriptNode argValueNode = new EmsScriptNode(argValues.get(0),services);
//            	System.out.println("\n*testExpressionEvaluation() argValueNode.type: " +argValueNode.getType());
//            	System.out.println("\n*testExpressionEvaluation() argValueNode.integer: " +argValueNode.getProperty(Acm.ACM_INTEGER));
//
//        	}
//        }
        
//        // playing:
//        Collection< EmsScriptNode > nodesTest = model.getElementWithName( null, "duration" );
//        EmsScriptNode nodeTest = nodesTest.iterator().next();
//        // This also returns an ArrayList<NodeRef> of size 1
//        System.out.println("\n*testExpressionEvaluation() nodeTest.getProperty(sysml:value).getClass(): "
//				+ nodeTest.getProperty(Acm.ACM_VALUE).getClass());

        
        Object evalResult = sysmlToAe.evaluateExpression( node, Boolean.class );  
        System.out.println( "\n*testExpressionEvaluation() evalResult: "
                            + evalResult );
        assertNotNull( evalResult );
        
        Expression< Boolean > expression = sysmlToAe.toAeExpression( node );
        System.out.println( "\n*testExpressionEvaluation() expression: "
                + expression );
        assertNotNull( expression ); 
        Class< ? > type = expression.getType();
        System.out.println( "\n*testExpressionEvaluation() expression type: "
                            + type.getSimpleName() );
        //Assert.assertTrue( Boolean.class.isAssignableFrom( evalResult.getClass() ) ); 
        
        ConstraintExpression constraint = new ConstraintExpression( expression );
        System.out.println( "\n*testExpressionEvaluation() constraint: "
                + MoreToString.Helper.toLongString( constraint ) );
        assertNotNull( constraint );
        
        //SystemModelSolver< E, C, T, P, N, I, U, R, V, W, CT > solver = 
        //SystemModelSolver< ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? > solver = new SystemModelSolver( model, new ConstraintLoopSolver() ); 
        SystemModelSolver solver = new SystemModelSolver( model, new ConstraintLoopSolver() );
        System.out.println( "\n*testExpressionEvaluation() solver: "
                            + MoreToString.Helper.toLongString( solver ) );
        
        // Add the constraint to the auto-generated parameter listener:
        ClassData cd = sysmlToAe.getClassData();
        ParameterListenerImpl listener = cd.getAeClasses().values().iterator().next();
        listener.getConstraintExpressions().add(constraint);
        
        // Solve the constraint:
        boolean r = solver.solve(listener.getConstraints(true, null) );
        // TODO -- dig solution out of solver (really out of constraint)!
        
        System.out.println("\n*testExpressionEvaluation() creating a DurativeEvent and execute()....\n");
        DurativeEvent durEvent = new DurativeEvent("testDuration", listener);
        durEvent.execute();
        
        System.out.println("\n*testExpressionEvaluation() durEvent.execution(): "+durEvent.executionToString());
    }

    @Test
    public void testExpressionSolving() {
        
    }

    public static void setServiceRegistry( ServiceRegistry services ) {
        EmsSystemModelTest.services  = services;
        
    }
    
}
