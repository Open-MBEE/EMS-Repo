package gov.nasa.jpl.view_repo.test;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Collection;

import gov.nasa.jpl.ae.event.ConstraintExpression;
import gov.nasa.jpl.ae.event.Expression;
import gov.nasa.jpl.ae.solver.ConstraintLoopSolver;
import gov.nasa.jpl.ae.sysml.SystemModelSolver;
import gov.nasa.jpl.ae.sysml.SystemModelToAeExpression;
import gov.nasa.jpl.mbee.util.MoreToString;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsSystemModel;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class EmsSystemModelTest {
    
    public static EmsSystemModel model = null;
    public static SystemModelToAeExpression< EmsScriptNode, EmsScriptNode, String, EmsSystemModel > sysmlToAe = null;
    public static ServiceRegistry services = null;
    protected static final String ADMIN_USER_NAME = "admin";

    @BeforeClass
    public static void initAppContext() {
        model = new EmsSystemModel( services );
        sysmlToAe = new SystemModelToAeExpression< EmsScriptNode, EmsScriptNode, String, EmsSystemModel >( model );
        //AuthenticationUtil.setRunAsUserSystem();
        AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);
        
        // Load model for testing:
        //
        // Assuming git directory is in home directory, so model is in:
        // $HOME/git/alfresco-view-repo/test-data/javawebscripts/JsonData/expressionElements.json
        // See $HOME/git/alfresco-view-repo/test-data/javawebscripts/curl.tests.sh for environment variables and sample curl commands
        String userHome = System.getenv("HOME");
        // curl -w "%{http_code}" -u admin:admin -X POST -H "Content-Type:application/json" --data '{"name":"CY Test"}' "http://localhost:8080/view-repo/service/javawebscripts/sites/europa/projects/123456?fix=true&createSite=true"
        String curlCmd1 = "curl -w \"%{http_code}\" -u admin:admin -X POST -H \"Content-Type:application/json\" --data '{\"name\":\"CY Test\"}' \"http://localhost:8080/view-repo/service/javawebscripts/sites/europa/projects/123456?fix=true&createSite=true\"";
        //   curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/expressionElements.json $BASE_URL"sites/europa/projects/123456/elements\""
        String curlCmdTemp = String.format("%s/git/alfresco-view-repo/test-data/javawebscripts/JsonData/expressionElements.json \"http://localhost:8080/view-repo/service/javawebscripts/sites/europa/projects/123456/elements\"",userHome);
        String curlCmd2 = "curl -w \"%{http_code}\" -u admin:admin -X POST -H \"Content-Type:application/json\" --data @"+curlCmdTemp;
        
        try {
        	System.out.println("Executing command: "+curlCmd1);
        	Runtime.getRuntime().exec(curlCmd1);
        	System.out.println("Executing command: "+curlCmd2);
        	Runtime.getRuntime().exec(curlCmd2);
        }
        catch (IOException e) {
        	e.printStackTrace();	
        }
      
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
        
        //Collection< EmsScriptNode > nodes = model.getElementWithName( null, "expr_32165" );
        Collection< EmsScriptNode > nodes = model.getElementWithName( null, "arg_ev_33001" ); 

        System.out.println( "*testExpressionEvaluation() nodes: "
                            + MoreToString.Helper.toLongString( nodes ) );
        if ( Utils.isNullOrEmpty( nodes ) ) {
            nodes = model.getElementWithName( null, "*" );
            if ( !Utils.isNullOrEmpty( nodes ) ) {
                System.out.println( "testExpressionEvaluation() got " + nodes.size() + " nodes." );
            }
            System.out.println( "testExpressionEvaluation() again, nodes : "
                                + MoreToString.Helper.toLongString( nodes ) );
        }
        
        assertNotNull( nodes );
        Assert.assertFalse( nodes.isEmpty() );
        EmsScriptNode node = nodes.iterator().next();
        assertNotNull( node );
        
        Object evalResult = sysmlToAe.evaluateExpression( node );  
        System.out.println( "*testExpressionEvaluation() evalResult: "
                            + MoreToString.Helper.toLongString( evalResult ) );
        assertNotNull( evalResult );
        
        Expression< Boolean > expression = sysmlToAe.toAeExpression( node );
        System.out.println( "*testExpressionEvaluation() expression: "
                + MoreToString.Helper.toLongString( expression ) );
        assertNotNull( expression ); 
        Assert.assertTrue( Boolean.class.isAssignableFrom( expression.getType() ) );  // GG: this fails
        ConstraintExpression constraint = new ConstraintExpression( expression );
        System.out.println( "*testExpressionEvaluation() constraint: "
                + MoreToString.Helper.toLongString( constraint ) );
        assertNotNull( constraint );
        
        //SystemModelSolver< E, C, T, P, N, I, U, R, V, W, CT > solver = 
        //SystemModelSolver< ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? > solver = new SystemModelSolver( model, new ConstraintLoopSolver() ); 
        SystemModelSolver solver = new SystemModelSolver( model, new ConstraintLoopSolver() );
        System.out.println( "testExpressionEvaluation() solver: "
                            + MoreToString.Helper.toLongString( solver ) );
        boolean r = solver.solve( Utils.newList( constraint ) );
        // TODO -- dig solution out of solver (really out of constraint)!
    }

    @Test
    public void testExpressionSolving() {
        
    }

    public static void setServiceRegistry( ServiceRegistry services ) {
        EmsSystemModelTest.services  = services;
        
    }
    
}
