/**
 * 
 */
package gov.nasa.jpl.view_repo;

//import AbstractContentTransformer2;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.nodelocator.NodeLocatorService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.ApplicationContextHelper;
//import org.apache.log4j.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

/**
 * 
 */
public class JavaQuery 
	extends AbstractModuleComponent 
	implements ModelInterface<NodeRef, String, Serializable, String, AssociationRef> {

	protected static final String ADMIN_USER_NAME = "admin";

    protected static Log log = LogFactory.getLog(JavaQuery.class);
    
    protected NodeService nodeService;
    
    protected NodeLocatorService nodeLocatorService;

	protected ContentService contentService;

	protected SearchService searchService;

    protected static ApplicationContext applicationContext;

    private static JavaQuery javaQueryComponent = null;
    
	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}
	
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
    
    public void setNodeLocatorService(NodeLocatorService nodeLocatorService) {
        this.nodeLocatorService = nodeLocatorService;
    }

	public NodeRef getNode(String string) {
        return nodeLocatorService.getNode(string, null, null);
	}
	
	// JUnit

	@Override
	public String getName(NodeRef object) {
		return (String) nodeService.getProperty(object, ContentModel.PROP_NAME);
	}


	@Override
	public String getType(NodeRef object) {
		QName type = nodeService.getType(object);
		if (type == null) return null;
		return type.toPrefixString();
	}

	
	@Override
	public Collection<Serializable> getProperties(NodeRef object) {
		Map<QName, Serializable> propMap = nodeService.getProperties(object);
		return propMap.values();
//		QNamePattern typeQNamePattern = new RegexQNamePattern(".*");
//		QNamePattern qnamePattern = new RegexQNamePattern(".*");
//		List<ChildAssociationRef> propRels = nodeService.getChildAssocs(object, typeQNamePattern, qnamePattern, false);
//		List<NodeRef> props = new ArrayList<NodeRef>();
//		for ( ChildAssociationRef ref : propRels ) {
//			props.add(ref.getChildRef());
//		}
//		return props;
	}

	@Override
	public Serializable getProperty(NodeRef object, String propertyName) {
		Serializable prop = nodeService.getProperty(object, QName.createQName(propertyName));
		return prop;
//		QNamePattern typeQNamePattern = new RegexQNamePattern(".*");
//		QNamePattern qnamePattern = new RegexQNamePattern(propertyName);
//		List<ChildAssociationRef> propRels = nodeService.getChildAssocs(object, typeQNamePattern, qnamePattern, false);
//		for ( ChildAssociationRef ref : propRels ) {
//			return ref.getChildRef();
//		}
//		return null;
	}

	/* (non-Javadoc)
	 * @see gov.nasa.jpl.view_repo.ModelInterface#getRelationships(java.lang.Object)
	 */
	@Override
	public Collection<AssociationRef> getRelationships(NodeRef object) {
		//List<ChildAssociationRef> passocs = nodeService.getParentAssocs(object);
		//List<ChildAssociationRef> cassocs = nodeService.getChildAssocs(object);
		List<AssociationRef> sassocs = nodeService.getSourceAssocs(object, new RegexQNamePattern(".*"));
		List<AssociationRef> tassocs = nodeService.getTargetAssocs(object, new RegexQNamePattern(".*"));
		List<AssociationRef> assocs = new ArrayList<AssociationRef>();
		assocs.addAll(sassocs);
		assocs.addAll(tassocs);
		return assocs;
	}

	@Override
	public AssociationRef getRelationship(NodeRef object, String relationshipName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<NodeRef> getRelated(NodeRef object, String relationshipName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void executeInternal() throws Throwable {
        System.out.println("DemoComponent has been executed");
        log.debug("Test debug logging. Congratulation your AMP is working");
        log.info("This is only for information purposed. Better remove me from the log in Production");
	}
	
    @BeforeClass
    public static void initAppContext()
    {
        // TODO: Make testing properly working without need for helpers
        // TODO: Provide this in an SDK base class
    	javaQueryComponent = null;
    	ApplicationContextHelper.setUseLazyLoading(false);
        ApplicationContextHelper.setNoAutoStart(true);
        applicationContext = ApplicationContextHelper.getApplicationContext(new String[] { "classpath:alfresco/application-context.xml" });
        JavaQuery javaQueryComponent = (JavaQuery) applicationContext.getBean("java_query");
        javaQueryComponent.nodeService = (NodeService) applicationContext.getBean("NodeService");
        AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);
        log.debug("Sample test logging: Application Context properly loaded for JavaQuery");
        System.out.println("damn! 1");
    }

    @Test
    public void testWiring() {
        System.out.println("damn! 2");
        assertNotNull(javaQueryComponent);
    }
    
    @Test
    public void testGetCompanyHome() {
        System.out.println("damn! 3");
        NodeRef node = javaQueryComponent.getNode("someNode");
        assertNotNull(node);
        String nodeName = (String) nodeService.getProperty(node, ContentModel.PROP_NAME);
        assertNotNull(nodeName);
        assertEquals("node name", nodeName);
    }

}
