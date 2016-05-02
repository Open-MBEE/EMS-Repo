package gov.nasa.jpl.view_repo.actions;

import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.EmsConfig;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.action.executer.MailActionExecuter;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.discussion.DiscussionService;
import org.alfresco.service.cmr.discussion.PostInfo;
import org.alfresco.service.cmr.discussion.PostWithReplies;
import org.alfresco.service.cmr.discussion.TopicInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.model.ContentModel;

public class DiscussionsEmailNotificationActionExecutor  extends ActionExecuterAbstractBase {

	public final static String NAME = "discussionsEmailNotification";

    protected NodeService nodeService;
    protected PersonService personService;
    protected ServiceRegistry services;
    protected DiscussionService discussionService;
    protected SiteService siteService;
    protected SiteInfo siteInfo;

    public void setNodeService(NodeService nodeService)
    {
       this.nodeService = nodeService;
    }
    
    public void setPersonService(PersonService personService)
    {
    	this.personService = personService;
    }
    
    public void setServices(ServiceRegistry sr) {
        this.services = sr;
    }
    
    public void setDiscussionService(DiscussionService service){
    	this.discussionService = service;
    }
    
    public void setSiteService(SiteService service){
    	this.siteService = service;
    }
    
    @Override
    protected void executeImpl(Action action, NodeRef nodeRef) {
        try{
            clearCache();
	        TopicInfo primaryTopic = getPrimaryTopic(nodeRef);
	        PostInfo primaryPost = discussionService.getPrimaryPost(primaryTopic);
	    	PostWithReplies postWithReplies = discussionService.listPostReplies(primaryPost, 1);
	    	
	    	List<String> emailAddresses = new ArrayList<String>();
	    	setSiteInfo(nodeRef);
	    	addSiteMgrToEmailAddresses(nodeRef, emailAddresses);
	    	gatherEmailAddresses(postWithReplies, emailAddresses);
	        sendEmail(primaryTopic, primaryPost, emailAddresses);
        }
        catch(Exception ex){
        	System.out.println("ERROR: Failed to process discussion forum rule action! " + ex.getMessage() + ex.getStackTrace());
        }
	}
    
    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
        // TODO Auto-generated method stub
    }
    
    private TopicInfo getPrimaryTopic(NodeRef nodeRef){
        org.alfresco.util.Pair<TopicInfo,PostInfo> pair = discussionService.getForNodeRef(nodeRef);
        return (TopicInfo)pair.getFirst();
    }
    
    private NodeRef getUserProfile(String userName){
    	return personService.getPerson(userName, false);
    }

    private void setSiteInfo(NodeRef nodeRef){
    	if(this.siteInfo == null){
    		this.siteInfo = siteService.getSite(nodeRef);
    	}
    }
    
    private Map<String, String> getSiteManagers(NodeRef nodeRef){
    	return siteService.listMembers(this.siteInfo.getShortName(), null, SiteModel.SITE_MANAGER, 0);
    }

    private void addSiteMgrToEmailAddresses(NodeRef nodeRef, List<String> emailAddresses){
    	try{
	    	Map<String, String> managers = getSiteManagers(nodeRef);
	    	if(managers==null) System.out.println("did not find any site manager!");
	    	Iterator itr = managers.entrySet().iterator();
	    	while(itr.hasNext()){
	    		Map.Entry pairs = (Map.Entry)itr.next();
	    		if(pairs == null || pairs.getKey()==null){ 
	    			System.out.println("site manager map entry is null.");
	    			continue;
	    		}
	    		String email = getEmail(pairs.getKey().toString());
	    		if(email!=null && !email.isEmpty()) emailAddresses.add(email);
	    		else System.out.println("did not find email address for " + pairs.getKey().toString());
	    	}
    	}
    	catch(Exception ex){
    		System.out.println("Failed to add site manager(s) to email list!");
    		ex.printStackTrace();
    	}
    }
    
    private String getEmail(String userName) {
    	try{
			NodeRef person = getUserProfile(userName);
			if(person == null) return "";
			Object o = NodeUtil.getNodeProperty( person, ContentModel.PROP_EMAIL,
			                                     services, true, true );
			if ( o instanceof String ) return (String)o;
			// TODO -- throw error here
			return "" + o;
    	}
    	catch(Exception ex){
    		System.out.println("Failed to get email address for " + userName);
    	}
    	return "";
	}

    private boolean isEmailExisted(List<String> emailList, String email){
		return emailList.contains(email.trim().toLowerCase());
	}
	
	private void addEmailToList(PostInfo post, List<String> emailList){
		String poster = post.getCreator();
		String posterEmail = getEmail(poster);
		if(!isEmailExisted(emailList, posterEmail)) {
			emailList.add(posterEmail.trim().toLowerCase());
		}
	}
	
	private void gatherEmailAddresses(PostWithReplies postWithReplies, List<String> emailList){
		for(PostWithReplies post:postWithReplies.getReplies()){
			PostInfo p = post.getPost();
			addEmailToList(p, emailList);
			gatherEmailAddresses(discussionService.listPostReplies(p, 1), emailList);
		}
	}
	
	private NodeRef getEmailTemplate(){
		String templatePATH = "PATH:\"/app:company_home/app:dictionary/app:email_templates/app:notify_email_templates/cm:discussion-email.html.ftl\"";
		ResultSet resultSet = services.getSearchService().query(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"), SearchService.LANGUAGE_LUCENE, templatePATH);
		if (resultSet.length()==0){
			System.out.println("Template "+ templatePATH+" not found.");
			return null;
		}
		return resultSet.getNodeRef(0);
	}
	
	
	private void sendEmail(TopicInfo primaryTopic, PostInfo post, List<String> emailAddresses ){
		NodeRef template = getEmailTemplate();
		String topicTitle = primaryTopic.getTitle();
		String subject = "New Post in Discussion: " + topicTitle;
        Action mailAction = services.getActionService().createAction(MailActionExecuter.NAME);
        mailAction.setParameterValue(MailActionExecuter.PARAM_SUBJECT, subject);
        mailAction.setParameterValue(MailActionExecuter.PARAM_FROM, EmsConfig.get("app.email.from"));
        mailAction.setParameterValue(MailActionExecuter.PARAM_TEMPLATE, template);
    	for (int i = 0; i < emailAddresses.size(); i++) {
    		String email = emailAddresses.get(i);
    		if(email == null || email.isEmpty()) continue;
    		mailAction.setParameterValue(MailActionExecuter.PARAM_TO, email);
    		Map<String, Serializable> templateArgs = new HashMap<String, Serializable>();
    		templateArgs.put("posterName", post.getCreator());
    		templateArgs.put("siteName", this.siteInfo.getShortName());
    		templateArgs.put("topicTitle", topicTitle);
    		templateArgs.put("topicID", primaryTopic.getSystemName());
    		Map<String, Serializable> templateModel = new HashMap<String, Serializable>();
    		templateModel.put("args",(Serializable)templateArgs);
    		mailAction.setParameterValue(MailActionExecuter.PARAM_TEMPLATE_MODEL,(Serializable)templateModel);
    		try{
    			services.getActionService().executeAction(mailAction, null);
    		}
    		catch(Exception ex){
    			System.out.println("ERROR: Failed to send discussion notification email to " + email);
    			ex.printStackTrace();
    		}
    	}
	}
	
    private void clearCache() {
        NodeUtil.setBeenInsideTransaction( false );
        NodeUtil.setBeenOutsideTransaction( false );
        NodeUtil.setInsideTransactionNow( false );
    }
}
