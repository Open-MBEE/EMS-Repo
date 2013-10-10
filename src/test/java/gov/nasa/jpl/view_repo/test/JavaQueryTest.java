/**
 * 
 */
package gov.nasa.jpl.view_repo.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import gov.nasa.jpl.ae.util.JavaEvaluator;
import gov.nasa.jpl.ae.util.MoreToString;
import gov.nasa.jpl.ae.util.Utils;
import gov.nasa.jpl.view_repo.JavaQuery;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class JavaQueryTest {

    public static JavaQuery javaQueryComponent = null;

    @BeforeClass
    public static void initAppContext() {
        // TODO: Make testing properly working without need for helpers
        // TODO: Provide this in an SDK base class
        javaQueryComponent = JavaQuery.getInstance();
    }

    @Test
    public void testDidInit() {
        System.out.println( "testDidInit()" );
        assertNotNull( javaQueryComponent );
    }

    /*
     * "Name","Id","Type","Value"
     * "Allowed Child Object Types Ids","cmis:allowedChildObjectTypeIds","id",[]
     * "Object Type Id","cmis:objectTypeId","id",["cmis:folder"]
     * "Path","cmis:path","string",[
     * "/Data Dictionary/Space Templates/Software Engineering Project/Documentation/Samples"
     * ] "Name","cmis:name","string",["Samples"]
     * "Creation Date","cmis:creationDate","datetime",[2013-09-30 11:46:16
     * -0700] "Change token","cmis:changeToken","string",[]
     * "Last Modified By","cmis:lastModifiedBy","string",["System"]
     * "Created by","cmis:createdBy","string",["System"]
     * "Object Id","cmis:objectId"
     * ,"id",["workspace://SpacesStore/bfbfe02b-0c36-4cca-b984-77d96fa57ac4"]
     * "Base Type Id","cmis:baseTypeId","id",["cmis:folder"]
     * "Alfresco Node Ref","alfcmis:nodeRef","id",[
     * "workspace://SpacesStore/bfbfe02b-0c36-4cca-b984-77d96fa57ac4"]
     * "Parent Id","cmis:parentId","id",[
     * "workspace://SpacesStore/9778128c-ddc4-47f4-8d20-2aa476f9c166"]
     * "Last Modified Date","cmis:lastModificationDate","datetime",[2013-09-30
     * 11:46:16 -0700]
     */
    
    //private static String theNodePath = "cm:Data Dictionary/cm:Space Templates/cm:Software Engineering Project/cm:Documentation/cm:Samples";
    public static String theNodePath = "Data Dictionary";
    public static String theNodeName = "system-overview.html";
    
    @Test
    public void testGetName() {
        System.out.println( "testGetName()" );
        NodeRef node = javaQueryComponent.getNode( theNodePath );//+ "/" + theNodeName );
        assertNotNull( node );
        String nodeName =
                (String)javaQueryComponent.nodeService.getProperty( node,
                                                                    ContentModel.PROP_NAME );
        System.out.println("testGetName() got nodeName " + nodeName);
        assertNotNull( nodeName );
        assertEquals( nodeName, nodeName );
        System.out.println("testGetName() succeeded!");
    }
    
    @Test
    public void testXPath() {
        System.out.println( "testXPath()" );
        String[] queries = new String[] {
          "//*",
          "/*",
          "*",
          "Company Home",
          "Company Home/Data Dictionary",
          "/Company Home",
          "/Company Home/Data Dictionary",
          "Data Dictionary",
          "/Data Dictionary",
          "cm:Company Home",
          "Company Home/cm:Data Dictionary",
          "/cm:Company Home",
          "/Company Home/cm:Data Dictionary",
          "cm:Data Dictionary",
          "/cm:Data Dictionary",
          "cm:Company Home/cm:Data Dictionary",
          "/cm:Company Home/cm:Data Dictionary"
        };
        NodeRef lastGoodNode = null; 
        for ( String qString : queries ) {
            NodeRef node = JavaQuery.get( qString );
            System.out.println( "testXPath: get(" + qString + ") got node: "
                                + node );
            // NodeRef node = javaQueryComponent.getNode( theNodePath );//+ "/"
            // + theNodeName );
            if ( node == null ) continue;
            String nodeName =
                    (String)javaQueryComponent.nodeService.getProperty( node,
                                                                        ContentModel.PROP_NAME );
            System.out.println( "testGetName() got nodeName " + nodeName );
            if ( lastGoodNode == null || !Utils.isNullOrEmpty( nodeName ) ) {
                lastGoodNode = node;
            }
        }
        assertNotNull( lastGoodNode );
        System.out.println("testGetName() succeeded!");
    }
    
    @Test
    public void testJavaEvaluatorSqrt() {
        String java = "Math.sqrt( 49 )";
        Object actualObj = JavaEvaluator.evaluate(java);
        Double actual = null;
        if ( actualObj instanceof Double ) actual = (Double)actualObj;
        System.out.println( "testJavaEvaluatorSqrt() evaluate(" + java + ") = " + MoreToString.Helper.toString( actualObj ) );
        Double expected = Math.sqrt( 49 );
        assertEquals( expected, actual, 0.0001 );
    }
    
    @Test
    public void testJavaEvaluatorGetFields() {
        String java = "org.alfresco.model.ContentModel.class.getFields()";
        Object expected = org.alfresco.model.ContentModel.class.getFields();
        Object actualObj = JavaEvaluator.evaluate(java);
        System.out.println( "testJavaEvaluatorGetFields() evaluate(" + java + ") = " + MoreToString.Helper.toString( actualObj ) );
        assertNotNull( actualObj );
        assertTrue( actualObj.equals( expected ) );
    }
    
    @Test
    public void testJavaEvaluatorConstant() {
        String java = "org.alfresco.model.ContentModel.PROP_NAME";
        Object expected = org.alfresco.model.ContentModel.PROP_NAME; 
        Object actualObj = JavaEvaluator.evaluate(java);
        System.out.println( "testJavaEvaluatorConstant() evaluate(" + java + ") = " + MoreToString.Helper.toString( actualObj ) );
        assertNotNull( actualObj );
        assertTrue( actualObj.equals( expected ) );
    }
    
    @Test
    public void testJavaEvaluatorClassRef() {
        String clsName = "gov.nasa.jpl.view_repo.JavaQuery";
        Object actualObj = JavaEvaluator.evaluate(clsName);
        System.out.println( "testJavaEvaluatorClassRef() evaluate(" + clsName + ") = " + MoreToString.Helper.toString( actualObj ) );
        assertNotNull( actualObj );
        assertTrue( actualObj.toString().equals( clsName ) );
    }
    
    @Test
    public void testJavaEvaluatorGet() {
        String clsName = "gov.nasa.jpl.view_repo.JavaQuery.get(\"*\")";
        Object actualObj = JavaEvaluator.evaluate(clsName);
        System.out.println( "testJavaEvaluatorGet() evaluate(" + clsName + ") = " + MoreToString.Helper.toString( actualObj ) );
        assertNotNull( actualObj );
        assertTrue( actualObj.toString().equals( clsName ) );
    }
    
    @Test
    public void testJavaEvaluatorTestClassRef() {
        String clsName = "gov.nasa.jpl.view_repo.test.JavaQueryTest";
        Object actualObj = JavaEvaluator.evaluate(clsName);
        System.out.println( "testJavaEvaluatorTestClassRef() evaluate(" + clsName + ") = " + MoreToString.Helper.toString( actualObj ) );
        assertNotNull( actualObj );
        assertTrue( actualObj.toString().equals( clsName ) );
    }
}
