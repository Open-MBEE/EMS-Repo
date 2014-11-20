package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.admin.SysAdminParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class HostnameGet extends AbstractJavaWebScript {

	@Override
    protected Map< String, Object > executeImpl( WebScriptRequest req, Status status, Cache cache ) {
		printHeader( req );
	    clearCaches();

		Map< String, Object > model = new HashMap< String, Object >();
		JSONObject jsonObj = new JSONObject();
		SysAdminParams sysAdminParams = this.services.getSysAdminParams();
		
		JSONObject alfrescoJson = new JSONObject();
		try{
			alfrescoJson.put("protocol", sysAdminParams.getAlfrescoProtocol());
			alfrescoJson.put("host", sysAdminParams.getAlfrescoHost());
			alfrescoJson.put("port", sysAdminParams.getAlfrescoPort());
			
			JSONObject shareJson = new JSONObject();
			shareJson.put("protocol", sysAdminParams.getShareProtocol());
			shareJson.put("host", sysAdminParams.getShareHost());
			shareJson.put("port", sysAdminParams.getSharePort());
			
			jsonObj.put("alfresco", alfrescoJson);
			jsonObj.put("share", shareJson);
			
			model.put( "res", jsonObj.toString() );
		}
		catch(JSONException js)
		{
			status.setCode(Status.STATUS_NOT_FOUND);
			status.setMessage("Cannot get host name information.");
			status.setException(js);
			status.setRedirect(true);
		}
		return model;
	}

	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		// TODO Auto-generated method stub
		return false;
	}
}
