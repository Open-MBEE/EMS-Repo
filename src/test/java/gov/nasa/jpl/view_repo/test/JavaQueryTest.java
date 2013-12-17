/**
 * 
 */
package gov.nasa.jpl.view_repo.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import gov.nasa.jpl.ae.util.JavaEvaluator;
import gov.nasa.jpl.ae.util.MoreToString;
import gov.nasa.jpl.ae.util.Utils;
import gov.nasa.jpl.view_repo.JavaQuery;

import org.alfresco.service.cmr.repository.NodeRef;
import org.junit.BeforeClass;
import org.junit.Ignore;
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

    @Test
    @Ignore
    public void testXPath() {
        System.out.println( "testXPath()" ); // _x0020_
        List<String> qList = new ArrayList<String>();
        List<String> aList = new ArrayList<String>();
        // the start
        qList.add( "" );
        qList.add( "Company Home" );
        qList.add( "Company Home/Data Dictionary" );
        qList.add( "Data Dictionary" );
        // cm:
        qList.add( "cm:Company Home" );
        qList.add( "cm:Company Home/cm:Data Dictionary" );
        qList.add( "Company Home/cm:Data Dictionary" );
        qList.add( "cm:Data Dictionary" );
        // app:
        qList.add( "app:Company Home" );
        qList.add( "app:Company Home/Data Dictionary" );
        qList.add( "app:Company Home/cm:Data Dictionary" );
        qList.add( "app:Data Dictionary" );

        // now make lower case versions
        aList.clear();
        for ( String q : qList ) {
            if ( q.matches( ".*[A-Z].*" ) ) {
                aList.add( q.toLowerCase() );
                if ( q.matches( ".*[C].*[D].*" ) ) {
                    aList.add( q.replace( 'C', 'c' ).replace( 'H', 'h' ) );
                }
            }
        }
        qList.addAll( aList );

        // now make ones that start with a "/"
        aList.clear();
        for ( String q : qList ) {
            aList.add( "/" + q );
        }
        qList.addAll( aList );
        
        // now make ones that end with a "/" and "/*"
        aList.clear();
        for ( String q : qList ) {
            if ( q.length() > 1 ) {
                aList.add( q + "/");
            }
            aList.add( q + "/*");
        }
        qList.addAll( aList );

        // now make ones with "//" instead of "/" for all and for just the first
        aList.clear();
        for ( String q : qList ) {
            if ( q.contains( "/" ) ) {
                aList.add( q.replaceAll( "/", "//" ) );
                if ( q.matches( "^/.+/.*" ) ) {
                    aList.add( q.replaceFirst( "/", "//" ) );
                    int pos = q.lastIndexOf( '/' );
                    aList.add( q.substring(0, pos) + q.substring( pos ).replaceAll( "/", "//" ) );
                }
            }
        }
        qList.addAll( aList );

        // replace spaces with "_" and "_x0020_"
        aList.clear();
        for ( String q : qList ) {
            if ( q.contains( " " ) ) {
                aList.add( q.replaceAll( " ", "_" ) );
                aList.add( q.replaceAll( " ", "_x0020_" ) );
                if ( q.matches( ".* [^ ]+ .*" ) ) {
                    aList.add( q.replaceFirst( " ", "_" ).replaceAll( " ", "_x0020_" ) );
                }
            } else {
                aList.add( q );
            }
        }
        qList.clear(); // Replacing entire list!  Don't want plain spaces!
        qList.addAll( aList );
  
//        // escape all ":"s and all but first ":"
//        aList.clear();
//        for ( String q : qList ) {
//            if ( q.contains( ":" ) ) {
//                aList.add( q.replaceAll( ":", "\\:" ) );
//                if ( q.matches( ".*:.*:.*" ) ) {
//                    aList.add( q.replaceFirst( ":", ";;;" ).replaceAll( ":", "\\:" ).replaceAll( ";;;", ":" ) );
//                }
//            }
//        }
//        qList.addAll( aList );
        
//        // precede with "archive://SpacesStore"
//        aList.clear();
//        for ( String q : qList ) {
//            if ( q.length() == 0 || q.startsWith( "/" ) ) {
//                aList.add( "archive://SpacesStore" + q );
//            }
//        }
//        qList.addAll( aList );
        
//        // put ":" in front of "/"
//        aList.clear();
//        for ( String q : qList ) {
//            if ( q.matches( ".*[^/:]/.*" ) ) {
//                aList.add( q.replaceAll( "([^/:])/", "$1:/") );
//                if ( q.matches( ".*[^/:]/.*[^/:]/.*" ) ) {
//                    aList.add( q.replaceFirst( "([^/:])/", "$1:/") );
//                }
//            }
//        }
//        qList.addAll( aList );
        
        // put "child::" after "/" 
        aList.clear();
        for ( String q : qList ) {
            if ( q.matches( ".*/[^\\/:*.@].*" ) ) {
                aList.add( q.replaceAll( "/([^\\/:*.@])", "/child::$1") );
                aList.add( q.replaceAll( "/([^\\/:*.@]+)", "/child::[contains('$1')]") );
                aList.add( q.replaceAll( "/([^\\/:*.@]+)", "/*[contains('$1')]") );
                aList.add( q.replaceAll( "/([^\\/:*.@]+)", "/@*[contains('$1')]") );
            }
        }
        qList.addAll( aList );
        
        qList.add( "@*" );
        qList.add( "*" );
        qList.add( "//*" );
        qList.add( "/*" );
        qList.add( "child::[contains('ompany')]" );
        qList.add( "//*child::[contains('ompany')]" );
        qList.add( "child::[contains('ata')]" );
        qList.add( "//*child::[contains('ata')]" );

        qList.add( "*[contains('ompany')]" );
        qList.add( "//@*[contains('ompany')]" );
        qList.add( "*::[contains('ata')]" );
        qList.add( "//@*[contains('ata')]" );

        // add PATH for lucene
        aList.clear();
        for ( String q : qList ) {
            aList.add( "PATH:\"" + q + "\"" );
        }
        qList.addAll( aList );
        
        qList.add( "@*child" );
        qList.add( "*child" );
        qList.add( "//*child" );
        qList.add( "/*child" );
        qList.add("workspace://SpacesStore/4c1f76e6-0fb0-4086-8217-f8bd48909c72");

        NodeRef lastGoodNode = null; 
        System.out.println( qList.size()
                            + " queries to check:\n"
                            + MoreToString.Helper.toString( qList, false,
                                                            false, null, null,
                                                            "  ", "\n  ", "\n",
                                                            false ) );
        for ( String qString : qList ) {
            List<NodeRef> xnodes = javaQueryComponent.xpathTest( qString );
            List<NodeRef> lnodes = javaQueryComponent.luceneTest( qString );
            if ( lastGoodNode == null && !Utils.isNullOrEmpty( xnodes ) ) {
                lastGoodNode = xnodes.get( 0 );
            }
            if ( lastGoodNode == null && !Utils.isNullOrEmpty( lnodes ) ) {
                lastGoodNode = lnodes.get( 0 );
            }
        }
        assertNotNull( lastGoodNode );
        System.out.println("testXPath() succeeded!");
    }
    
    @Test
    public void testLucene() {
        List<NodeRef> longList = new ArrayList<NodeRef>();
        String[] queries = new String[] {
            "PATH:\"/app:company_home/cm:Data_Dictionary//*\"",
            "PATH:\"/app:company_home/cm:Data_x0020_Dictionary//*\"",
            "PATH:\"/app:company_home/cm:data_dictionary//*\"",
            "PATH:\"/app:company_home/cm:DataDictionary//*\"",

            "PATH:\"/app:company_home/Data_Dictionary//*\"",
            "PATH:\"/app:company_home/Data_x0020_Dictionary//*\"",
            "PATH:\"/app:company_home/data_dictionary//*\"",
            "PATH:\"/app:company_home/DataDictionary//*\"",

            "PATH:\"/app:company_home//*\"",
            "PATH:\"/company_home//*\"",
            "PATH:\"/app:company_home//*/*\"",
            "PATH:\"/company_home//*/*\"",
            "PATH:\"/app:company_home/*//*\"",
            "PATH:\"/company_home/*//*\"",
            "workspace://SpacesStore/4c1f76e6-0fb0-4086-8217-f8bd48909c72",
            "//SpacesStore/4c1f76e6-0fb0-4086-8217-f8bd48909c72",
            "/4c1f76e6-0fb0-4086-8217-f8bd48909c72",
            "4c1f76e6-0fb0-4086-8217-f8bd48909c72",
            "app:company_home/4c1f76e6-0fb0-4086-8217-f8bd48909c72",
            "app:company_home/SpacesStore/4c1f76e6-0fb0-4086-8217-f8bd48909c72",
            "app:company_home//SpacesStore/4c1f76e6-0fb0-4086-8217-f8bd48909c72",
            "app:company_home//SpacesStore/4c1f76e6-0fb0-4086-8217-f8bd48909c72//*"
        };
        //String query = "PATH:\"/app:company_home/cm:Data_Dictionary//*\"";
        for ( String query : queries ) {
            List<NodeRef> nodeList = javaQueryComponent.luceneTest(query);
            System.out.println( "testLucene(" + query + ") got "
                                + MoreToString.Helper.toLongString( nodeList ) );
//            query = "PATH:\"/app:company_home//*\"";
//            nodeList = javaQueryComponent.luceneTest(query);
//            System.out.println( "testLucene(" + query + ") got "
//                                + MoreToString.Helper.toLongString( nodeList ) );
            if ( !Utils.isNullOrEmpty( nodeList ) ) {
                System.out.println("SUCCEEDED!!!");
                longList = nodeList;
            }
        }
        assertTrue( !Utils.isNullOrEmpty( longList ) );
    }
    
    
    @Test
    public void testCmis() {
        Collection<NodeRef> longList = new ArrayList<NodeRef>();
        String[] queries = new String[] {
             "SELECT cmis:objectId from cmis:folder where cmis:name = 'Data Dictionary'",
             // can't talk about cmis:path! //"SELECT cmis:objectId from cmis:folder where cmis:path = '/Data Dictionary/Scripts'",
          // can't talk about cmis:path! //"SELECT cmis:objectId from cmis:folder where cmis:path like '/Data Dictionary/Scripts%'"
             "SELECT cmis:objectId from cmis:folder where cmis:name like 'Scripts%'",
             "SELECT cmis:objectId from cmis:document where in_tree('workspace://SpacesStore/4c1f76e6-0fb0-4086-8217-f8bd48909c72')"
             };
        for ( String query : queries ) {
            //Collection<NodeRef> nodeList = javaQueryComponent.cmisNodeQuery( query );
            //ItemIterable< QueryResult > qResults = javaQueryComponent.cmisQuery( query );
            Collection<NodeRef> nodeList = javaQueryComponent.cmisTest( query );
            System.out.println( "testCmis(" + query + ") got "
                                + MoreToString.Helper.toLongString( nodeList ) );
            if ( !Utils.isNullOrEmpty( nodeList ) ) {
                System.out.println( "SUCCEEDED!!!" );
                longList = nodeList;
            }
        }
        assertTrue( !Utils.isNullOrEmpty( longList ) );
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
