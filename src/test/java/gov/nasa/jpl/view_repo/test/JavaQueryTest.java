/**
 * 
 */
package gov.nasa.jpl.view_repo.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import gov.nasa.jpl.view_repo.JavaQuery;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.util.ApplicationContextHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

/**
 *
 */
public class JavaQueryTest {

    protected static ApplicationContext applicationContext;
    private static JavaQuery javaQueryComponent = null;
    protected static final String ADMIN_USER_NAME = "admin";
    public static Log log = LogFactory.getLog( JavaQuery.class );

    @BeforeClass
    public static void initAppContext() {
        // TODO: Make testing properly working without need for helpers
        // TODO: Provide this in an SDK base class
        javaQueryComponent = null;
        ApplicationContextHelper.setUseLazyLoading( false );
        ApplicationContextHelper.setNoAutoStart( true );
        String[] contextPath = new String[] { "classpath:alfresco/application-context.xml" };
        applicationContext =
                ApplicationContextHelper.getApplicationContext( contextPath  );
        javaQueryComponent =
                (JavaQuery)applicationContext.getBean( "java_query" );
        javaQueryComponent.nodeService =
                (NodeService)applicationContext.getBean( "NodeService" );
        AuthenticationUtil.setFullyAuthenticatedUser( ADMIN_USER_NAME );
        log.debug( "Sample test logging: Application Context properly loaded for JavaQuery" );
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
    
    private static String theNodePath = "/Data Dictionary/Space Templates/Software Engineering Project/Documentation/Samples";
    private static String theNodeName = "system-overview.html";
    
    @Test
    public void testGetName() {
        System.out.println( "testGetName()" );
        NodeRef node = javaQueryComponent.getNode( theNodePath + "/" + theNodeName );
        assertNotNull( node );
        String nodeName =
                (String)javaQueryComponent.nodeService.getProperty( node,
                                                                    ContentModel.PROP_NAME );
        assertNotNull( nodeName );
        assertEquals( nodeName, nodeName );
    }

}
