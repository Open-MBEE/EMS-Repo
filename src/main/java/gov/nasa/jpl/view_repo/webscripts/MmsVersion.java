package gov.nasa.jpl.view_repo.webscripts;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.List;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript;

public class MmsVersion extends AbstractJavaWebScript {

	private static Logger logger = Logger.getLogger(MmsVersion.class);
	protected boolean prettyPrint = true;
    public MmsVersion() {
        super();
    }

    public MmsVersion(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Webscript entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
    	MmsVersion instance = new MmsVersion(repository, getServices());
        return instance.executeImplImpl(req,  status, cache, runWithoutTransactions);
    }
	@Override
	protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
		// TODO Auto-generated method stub
		Map<String, Object> model = new HashMap<String, Object>();
//		if (logger.isDebugEnabled()) {
//			String user = AuthenticationUtil.getFullyAuthenticatedUser();
//			logger.debug(user + " " + req.getURL());
//		}
//		Timer timer = new Timer();
//		printHeader(req);
		JSONObject mmsVersion = new JSONObject();

		
		System.out.println("Checking MMS Versions");
//		try {
			mmsVersion = getMMSversion();
			if (prettyPrint) {
				model.put("res", NodeUtil.jsonToString(mmsVersion, 4));
			} else {
				model.put("res", NodeUtil.jsonToString(mmsVersion));
			}
//		} catch (JSONException e) {
//			log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create JSONObject");
//			model.put("res", createResponseJson());
//			e.printStackTrace();
//		}

//		status.setCode(responseStatus.getCode());

//		printFooter();

//		if (logger.isInfoEnabled()) {
//			log(Level.INFO, "MmsVersion: %s", timer);
			// logger.info( "ModelGet: " + timer );
//		}

		return model;
	}

	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		// TODO Auto-generated method stub
		getBooleanArg(req, "mmsVersion", false);
		return false;
	}

}
