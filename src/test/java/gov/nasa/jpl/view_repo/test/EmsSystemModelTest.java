package gov.nasa.jpl.view_repo.test;

import static org.junit.Assert.assertNotNull;

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
        // This test assumes that the model in
        // alfresco-view-repo/test-data/javawebscripts/JsonData/expressionElements.json
        // has been loaded with the curl command:
        //   curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/elements.json $BASE_URL"sites/europa/projects/123456/elements\""
        // TODO -- load the data in initAppContext() above!

        //NodeRef node = NodeUtil.findNodeRefById( "expr_32165", model.getServices() );
                
        System.out.println( "testExpressionEvaluation()" );
        
        Collection< EmsScriptNode > nodes = model.getElementWithName( null, "*" );
        System.out.println( "testExpressionEvaluation() nodes: "
                            + MoreToString.Helper.toLongString( nodes ) );
        if ( Utils.isNullOrEmpty( nodes ) ) {
            nodes = model.getElementWithName( null, "*" );
            System.out.println( "testExpressionEvaluation() again, nodes : "
                                + MoreToString.Helper.toLongString( nodes ) );
        }
        
        assertNotNull( nodes );
        Assert.assertFalse( nodes.isEmpty() );
        EmsScriptNode node = nodes.iterator().next();
        assertNotNull( node );
        
        Object evalResult = sysmlToAe.evaluateExpression( node );
        System.out.println( "testExpressionEvaluation() evalResult: "
                            + MoreToString.Helper.toLongString( evalResult ) );
        assertNotNull( evalResult );
        
        Expression< Boolean > expression = sysmlToAe.toAeExpression( node );
        System.out.println( "testExpressionEvaluation() expression: "
                + MoreToString.Helper.toLongString( expression ) );
        assertNotNull( expression );
        Assert.assertTrue( Boolean.class.isAssignableFrom( expression.getType() ) );
        ConstraintExpression constraint = new ConstraintExpression( expression );
        System.out.println( "testExpressionEvaluation() constraint: "
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
