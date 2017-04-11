package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;

/**
 * Copyright (C) 2005-2009 Alfresco Software Limited.
 *
 * This file is part of the Spring Surf Extension project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import gov.nasa.jpl.view_repo.util.NodeUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.*;


/**
 * Modified from Alfresco's DeclarativeWebSCript so we can have one place to modify the cache
 * control headers in the response.
 * 
 * Script/template driven based implementation of an Web Script
 *
 * @author davidc
 */
public class DeclarativeJavaWebScript extends AbstractWebScript 
{
    private static final Logger logger = Logger.getLogger(DeclarativeJavaWebScript.class);  
    
    private JSONObject      privateRequestJSON = null;
    private String          mmsVersionResponse = null;
    public static boolean   checkMmsVersions = false;
    public static boolean   cacheSnapshotsFlag = false;
    public static boolean   cacheDynamicFlag = false;

    public static Long maxage = new Long(5 * 60 * 1000); // default to 5 minutes
    
    // Map of uri + body string hash to the current time
    private static Map<Integer, String> timestampRequests = new HashMap<Integer, String>();
    
    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.WebScript#execute(org.alfresco.web.scripts.WebScriptRequest, org.alfresco.web.scripts.WebScriptResponse)
     */
    final public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException
    {
        // retrieve requested format
        String format = req.getFormat();
        Map<String, Object> model;
        
        try
        {
            // establish mimetype from format
            String mimetype = getContainer().getFormatRegistry().getMimeType(req.getAgent(), format);
            if (mimetype == null)
            {
                throw new WebScriptException("Web Script format '" + format + "' is not registered");
            }
            
            // construct model for script / template
            Status status = new Status();
            Cache cache = new Cache(getDescription().getRequiredCache());

            // MMS-681: Return 201 if timestamped request is already being serviced
            String requestStartTime = checkRequestActive(req);
            if (requestStartTime != null) {
                status.setCode( HttpStatus.SC_ACCEPTED );
                model = new HashMap<String, Object>(8, 1.0f);
                model.put( "res", String.format( "{\"startTime\":\"%s\"}", requestStartTime ) );
            } else {
                setRequestActive(req);
                setCacheHeaders(req, cache); // add in custom headers for nginx caching
    
                // if the flag for checking for MMS Version has been enabled it will then -
                if(checkMmsVersions)
                {
                    // Compare the version given by the request to the mms version. If the request versions match or if the request is a GET call
                    //  then function will executeImpl, else it will put a responseJSON into the model to be returned.
                    if(compareMmsVersions(req))
                    {
                        model = new HashMap<String, Object>(8, 1.0f);
                        model.put("res", createMmsVersionResponseJson());
                        // TODO: Set the cache and status to be put into the model
                    }
                    else
                    {
                        // If version checking turned passed the executeImpl.
                        model = executeImpl(req, status, cache);
                    }
                }
                else
                {
                    // executeImpl if checking of MMS versions has been turned off.
                    model = executeImpl(req, status, cache);
                }
                
                setRequestInactive(req);
            }

            if (model == null)
            {
                model = new HashMap<String, Object>(8, 1.0f);
            }

            model.put("status", status);
            model.put("cache", cache);

            if (!NodeUtil.skipQualified) {
                NodeUtil.ppAddQualifiedNameId2Json(req, model); // TODO: weave in as aspect
            }
            
            try
            {
                // execute script if it exists
                ScriptDetails script = getExecuteScript(req.getContentType());
                if (script != null)
                {
                    if (logger.isDebugEnabled())
                        logger.debug("Executing script " + script.getContent().getPathDescription());
                    
                    Map<String, Object> scriptModel = createScriptParameters(req, res, script, model);
                    
                    // add return model allowing script to add items to template model
                    Map<String, Object> returnModel = new HashMap<String, Object>(8, 1.0f);
                    scriptModel.put("model", returnModel);
                    executeScript(script.getContent(), scriptModel);
                    mergeScriptModelIntoTemplateModel(script.getContent(), returnModel, model);
                }
        
                // create model for template rendering
                Map<String, Object> templateModel = createTemplateParameters(req, res, model);
                
                // is a redirect to a status specific template required?
                if (status.getRedirect())
                {
                    sendStatus(req, res, status, cache, format, templateModel);
                }
                else
                {
                    // render output
                    int statusCode = status.getCode();
                    if (statusCode != HttpServletResponse.SC_OK && !req.forceSuccessStatus())
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Force success status header in response: " + req.forceSuccessStatus());
                            logger.debug("Setting status " + statusCode);
                        }
                        res.setStatus(statusCode);
                    }
                    
                    // apply location
                    String location = status.getLocation();
                    if (location != null && location.length() > 0)
                    {
                        if (logger.isDebugEnabled())
                            logger.debug("Setting location to " + location);
                        res.setHeader(WebScriptResponse.HEADER_LOCATION, location);
                    }
    
                    // apply cache
                    res.setCache(cache);
                    
                    String callback = null;
                    if (getContainer().allowCallbacks())
                    {
                        callback = req.getJSONCallback();
                    }
                    if (format.equals(WebScriptResponse.JSON_FORMAT) && callback != null)
                    {
                        if (logger.isDebugEnabled())
                            logger.debug("Rendering JSON callback response: content type=" + Format.JAVASCRIPT.mimetype() + ", status=" + statusCode + ", callback=" + callback);
                        
                        // NOTE: special case for wrapping JSON results in a javascript function callback
                        res.setContentType(Format.JAVASCRIPT.mimetype() + ";charset=UTF-8");
                        res.getWriter().write((callback + "("));
                    }
                    else
                    {
                        if (logger.isDebugEnabled())
                            logger.debug("Rendering response: content type=" + mimetype + ", status=" + statusCode);
    
                        res.setContentType(mimetype + ";charset=UTF-8");
                    }
                
                    // render response according to requested format
                    renderFormatTemplate(format, templateModel, res.getWriter());
                    
                    if (format.equals(WebScriptResponse.JSON_FORMAT) && callback != null)
                    {
                        // NOTE: special case for wrapping JSON results in a javascript function callback
                        res.getWriter().write(")");
                    }
                }
            }
            finally
            {
                // perform any necessary cleanup
                executeFinallyImpl(req, status, cache, model);
            }
        }
        catch(Throwable e)
        {
            if (logger.isDebugEnabled())
            {
                StringWriter stack = new StringWriter();
                e.printStackTrace(new PrintWriter(stack));
                logger.debug("Caught exception; decorating with appropriate status template : " + stack.toString());
            }

            throw createStatusException(e, req, res);
        }
    }
    
    private void setRequestInactive( WebScriptRequest req ) {
        int key = getRequestHash( req );

        synchronized(timestampRequests) {
            timestampRequests.remove( key );
        }
    }

    private void setRequestActive( WebScriptRequest req ) {
        int key = getRequestHash( req );

        synchronized(timestampRequests) {
            timestampRequests.put( key, TimeUtils.toTimestamp( new Date() ));
        }        
    }

    private String checkRequestActive( WebScriptRequest req ) {
        String timestamp = req.getParameter( "timestamp" );
        if (timestamp == null) {
            return null;
        }
        
        int key = getRequestHash( req );
        
        synchronized(timestampRequests) {
            if (timestampRequests.containsKey( key )) {
                return timestampRequests.get( key );
            }
        }
        
        return null;
    }
    
    private int getRequestHash( WebScriptRequest req ) {
        String uri = req.getURL();
        setRequestJSON(req);
        
        if (privateRequestJSON != null) {
            uri = uri + privateRequestJSON.toString();
        }
        
        return uri.hashCode();      
    }

    /**
     * Set the cache headers for caching server based on the request. This is single place
     * that we need to modify to update cache-control headers across all webscripts.
     * @param req
     * @param cache
     */
    private boolean setCacheHeaders( WebScriptRequest req, Cache cache ) {
        String[] names = req.getParameterNames();
        boolean cacheUpdated = false;

        // check if timestamp
        for (String name: names) {
            if (name.equals( "timestamp" )) {
                cacheUpdated = updateCache(cache);
                break;
            } 
        }
        
        if (cacheDynamicFlag && !cacheUpdated) {
            String webscript = req.getServiceMatch().getWebScript().toString();
            if ( webscript.contains( "post" ) || webscript.contains( "delete" )) {
                // do nothing with posts and deletes
            } else {
                cacheUpdated = updateCache(cache, maxage);
            }
        }


        // get url without the parameters
        String url = req.getServicePath();

        // check if configuration snapshots and products
        if (!cacheUpdated) {
            if (cacheSnapshotsFlag) {
                if (url.contains( "configurations" )) {
                    if (url.contains( "snapshots") || url.contains( "products" )) {
                        cacheUpdated = updateCache(cache);
                    }
                }
            }
        }
        
        return cacheUpdated;
    }
    
    private boolean updateCache(Cache cache) {
        return updateCache(cache, new Long(31536000));
    }
    
    private boolean updateCache(Cache cache, Long maxAge) {
        if (!cache.getIsPublic()) {
            cache.setIsPublic( true );
            // set to one year
            cache.setMaxAge( maxAge );
            // following are true by default, so need to set them to false
            cache.setNeverCache( false ); 
            cache.setMustRevalidate( false );
        }
        return true;
    }

    /**
     * Merge script generated model into template-ready model
     * 
     * @param scriptContent    script content
     * @param scriptModel      script model
     * @param templateModel    template model
     */
    final private void mergeScriptModelIntoTemplateModel(ScriptContent scriptContent, Map<String, Object> scriptModel, Map<String, Object> templateModel)
    {
        // determine script processor
        ScriptProcessor scriptProcessor = getContainer().getScriptProcessorRegistry().getScriptProcessor(scriptContent);        
        if (scriptProcessor != null)
        {
            for (Map.Entry<String, Object> entry : scriptModel.entrySet())
            {
                // retrieve script model value
                Object value = entry.getValue();
                Object templateValue = scriptProcessor.unwrapValue(value);
                templateModel.put(entry.getKey(), templateValue);
            }
        }
    }

    /**
     * Execute custom Java logic
     * 
     * @param req  Web Script request
     * @param status Web Script status
     * @return  custom service model
     * @deprecated
     */
    protected Map<String, Object> executeImpl(WebScriptRequest req, WebScriptStatus status)
    {
        return null;
    }

    /**
     * Execute custom Java logic
     * 
     * @param req  Web Script request
     * @param status Web Script status
     * @return  custom service model
     * @deprecated
     */
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status)
    {
        return executeImpl(req, new WebScriptStatus(status));
    }

    /**
     * Execute custom Java logic
     * 
     * @param  req  Web Script request
     * @param  status Web Script status
     * @param  cache  Web Script cache
     * @return  custom service model
     */
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        // NOTE: Redirect to those web scripts implemented before cache support and v2.9
        return executeImpl(req, status);
    }

    /**
     * Execute custom Java logic to clean up any resources
     *  
     * @param req  Web Script request
     * @param status  Web Script status
     * @param cache  Web Script cache
     * @param model  model
     */
    protected void executeFinallyImpl(WebScriptRequest req, Status status, Cache cache, Map<String, Object> model)
    {
    }
    
    
    /**
     * Render a template (of given format) to the Web Script Response
     * 
     * @param format  template format (null, default format)  
     * @param model  data model to render
     * @param writer  where to output
     */
    final protected void renderFormatTemplate(String format, Map<String, Object> model, Writer writer)
    {
        format = (format == null) ? "" : format;

        String templatePath = getDescription().getId() + "." + format;

        if (logger.isDebugEnabled())
            logger.debug("Rendering template '" + templatePath + "'");

        renderTemplate(templatePath, model, writer);
    }
    
    /**
     * Get map of template parameters that are available with given request.
     * This method is for FreeMarker Editor Extension plugin of Surf Dev Tools.
     * 
     * @param req webscript request
     * @param res webscript response
     * @return
     * @throws IOException
     */
    public  Map<String, Object> getTemplateModel(WebScriptRequest req, WebScriptResponse res) throws IOException
    {
     // construct model for script / template
        Status status = new Status();
        Cache cache = new Cache(getDescription().getRequiredCache());
        Map<String, Object> model = new HashMap<String, Object>(8, 1.0f);
        
        model.put("status", status);
        model.put("cache", cache);
        
        // execute script if it exists
        ScriptDetails script = getExecuteScript(req.getContentType());
        if (script != null)
        {                    
            Map<String, Object> scriptModel = createScriptParameters(req, res, script, model);                    
            // add return model allowing script to add items to template model
            Map<String, Object> returnModel = new HashMap<String, Object>(8, 1.0f);
            scriptModel.put("model", returnModel);
            executeScript(script.getContent(), scriptModel);
            mergeScriptModelIntoTemplateModel(script.getContent(), returnModel, model);
        }
        // create model for template rendering
        return createTemplateParameters(req, res, model);
    }

    /**
     * compareMmsVersions
     * <br>
     * <h3>Note: Returns true if this compare fails for either incorrect versions or if there is an error with the request.<br/>
     * Returns false if the check is successful and the versions match.</h3>
     * <pre>
     * Takes a request created when a service is called and will retrieve the mmsVersion that is sent with it.
     *  <b>The flag checkMmsVersions needs to be set to true for this service to work.</b>
     *  <br/><b>1. </b>Check if there the request comes with the parameter mmsVersion=2.#. If the global flag
     *  is set to check for mmsVersion it will then return either none if either invalid input or if none has been
     *  specified, or the value of the version the service is being called with.
     *  <br/><b>2. </b>If the value that is received after checking for mmsVersion in the request, is 'none' then
     *  it will call parseContent of the request to create a JSONObject. If that fails, an exception is thrown
     *  and the boolean value 'true' is returned to the calling method to signify failure of the check. Else it
     *  will try to grab the mmsVersion from where ever it may lie within the JSONObject.
     *  <br/><b>3. </b>
     * </pre>
     * @param req WebScriptRequest
     * @param response StringBuffer response
     * @param status Status of the request
     * @author EDK
     * @return boolean false if versions match, true if they do not match or if is an incorrect request.
     */
    public boolean compareMmsVersions(WebScriptRequest req ) {
        // Calls getBooleanArg to check if they have request for mms version
        // TODO: Possibly remove this and implement as an aspect?
        boolean incorrectVersion = true;
        JSONObject jsonRequest = null;
        String responseString = null;
        char logCase = '0';
        JSONObject jsonVersion = null;
        String mmsVersion = null;

        // Checks if the argument is mmsVersion and returns the value specified
        // by the request
        // if there is no request it will return 'none'
        String paramVal = getStringArg(req, "mmsVersion", "none");
        String paramArg = paramVal;
        // Checks data member requestJSON to see if it is not null and if
        // paramVal is none

       // Check if input is K or JSON
        String contentType = req.getContentType() == null ? ""
                : req.getContentType().toLowerCase();

        boolean jsonNotK = !contentType.contains("application/k");

        String descriptionPath = getDescription().getDescPath();
        if(descriptionPath.contains( ".get" )){
            if (logger.isDebugEnabled()){
                logger.info( "Get was found within the description path");
            }
            return false;
        }

        if (!jsonNotK && paramVal.equals("none")) {
                jsonRequest = getRequestJSON(req);

            if (jsonRequest != null) {
                paramVal = jsonRequest.optString("mmsVersion");
                paramVal = paramVal.substring(0, 2);
            }
        }

        if (paramVal != null && !paramVal.equals("none") && paramVal.length() > 0) {
            // Calls NodeUtil's getMMSversion
            jsonVersion = getMMSversion();
            mmsVersion = jsonVersion.get("mmsVersion").toString().substring(0,3);
             
            if (logger.isDebugEnabled()){
                logger.info("Comparing Versions....");
            }
            // version match only needs to be on the first two digits
            if (mmsVersion.startsWith(paramVal)) {
                // Compared versions matches
                logCase = '1';
                incorrectVersion = false;
            } else {
                // Versions do not match
                logCase = '2';
            }
        } else if (Utils.isNullOrEmpty(paramVal) || paramVal.equals("none")) {
            // Missing MMS Version parameter
            logCase = '3';
        } else {
            // Wrong MMS Version or Invalid input
            logCase = '4';
        }
        switch (logCase) {
            case '1' :
                responseString = "Correct Versions";
                if (logger.isDebugEnabled()){
                    logger.info(responseString);
                }
                break;
            case '2' :
                responseString = "Versions do not match! Expected Version " + mmsVersion + ". Instead received " + paramVal;
                if (logger.isDebugEnabled())
                    logger.warn(responseString);
                break;
            case '3' :
                    responseString ="Missing MMS Version or invalid parameter. Received parameter:" 
                       + paramArg + " and argument:" + mmsVersion + ". Request was: " + jsonRequest;
                if (logger.isDebugEnabled()){
                    logger.error(responseString);
                }
                break;
            // TODO: This should be removed but for the moment I am leaving this
            // in as a contingency if anything else may break this.
            case '4' :
                responseString = "Wrong MMS Version or invalid input. Expected mmsVersion="
                                + mmsVersion + ". Instead received "
                                + paramVal;
                if (logger.isDebugEnabled()){
                    logger.error(responseString);
                }
                break;
        }
        mmsVersionResponse = responseString;
        // Returns true if it is either the wrong version or if it failed to
        // compare it
        // Returns false if it was successful in retrieving the mmsVersions from
        // both the MMS and the request and
        return incorrectVersion;
    }

    /**
     * getMMSversion<br>
     * Returns a JSONObject representing the mms version being used. It's format
     * will be
     *
     * <pre>
     * {
     *     "mmsVersion":"2.3"
     * }
     * </pre>
     *
     * @return JSONObject mmsVersion
     */
    public static JSONObject getMMSversion() {
        JSONObject version = new JSONObject();
        version.put("mmsVersion", NodeUtil.getMMSversion());
        return version;
    }
    /**
     * getMMSversion <br/>
     * getMMSversion wraps
     *
     * @param req
     * @return
     */
    public static JSONObject getMMSversion(WebScriptRequest req) {
        // Calls getBooleanArg to check if they have request for mms version
        // TODO: Possibly remove this and implement as an aspect?
        JSONObject jsonVersion = null;
        boolean paramVal = getBooleanArg(req, "mmsVersion", false);
        if (paramVal) {
            jsonVersion = new JSONObject();
            jsonVersion = getMMSversion();
        }

        return jsonVersion;
    }

    /**
     * Helper utility to get the String value of a request parameter, calls on
     * getParameterNames from the WebScriptRequest object to compare the
     * parameter name passed in that is desired from the header.
     *
     * @param req
     *            WebScriptRequest with parameter to be checked
     * @param name
     *            String of the request parameter name to check
     * @param defaultValue
     *            default value if there is no parameter with the given name
     * @author dank
     * @return 'empty' if the parameter is assigned no value, if it is assigned
     *         "parameter value" (ignoring case), or if it's default is default
     *         value and it is not assigned "empty" (ignoring case).
     */
    public static String getStringArg(WebScriptRequest req, String name,
            String defaultValue) {
        if (!Utils.toSet(req.getParameterNames()).contains(name)) {
            return defaultValue;
        }
        String paramVal = req.getParameter(name);
        return paramVal;
    }

    /**
     * setRequestJSON <br>
     * This will set the AbstractJavaWebScript data member requestJSON. It will
     * make the parsedContent JSONObject remain within the scope of the
     * AbstractJavaWebScript. {@link #requestJSON}
     *
     * @param req
     *            WebScriptRequest
     */
    public void setRequestJSON(WebScriptRequest req) {

        try {
            Object content = req.parseContent();
            if (content instanceof JSONObject) {
                privateRequestJSON = (JSONObject) content;
            } else if (content instanceof JSONArray) {
                privateRequestJSON = new JSONObject();
                privateRequestJSON.put( "array", (JSONArray) content );
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (logger.isDebugEnabled()){
                logger.error("Could not retrieve JSON");
            }
        }
    }

    public JSONObject getRequestJSON(WebScriptRequest req) {
        // Returns immediately if requestJSON has already been set before checking MMS Versions
        if(privateRequestJSON == null) return privateRequestJSON;
        // Sets privateRequestJSON
        setRequestJSON(req);
        return privateRequestJSON;
    }
    
    /**
     * Helper utility to get the value of a Boolean request parameter
     *
     * @param req
     *            WebScriptRequest with parameter to be checked
     * @param name
     *            String of the request parameter name to check
     * @param defaultValue
     *            default value if there is no parameter with the given name
     * @return true if the parameter is assigned no value, if it is assigned
     *         "true" (ignoring case), or if it's default is true and it is not
     *         assigned "false" (ignoring case).
     */
    public static boolean getBooleanArg(WebScriptRequest req, String name,
                                        boolean defaultValue) {
        if ( !Utils.toSet( req.getParameterNames() ).contains( name ) ) {
            return defaultValue;
        }
        String paramVal = req.getParameter(name);
        if ( Utils.isNullOrEmpty( paramVal ) ) return true;
        Boolean b = Utils.isTrue( paramVal, false );
        if ( b != null ) return b;
        return defaultValue;
    }
    
    /**
     * Creates a json like object in a string and puts the response in the message key. Made this private so it is
     * not used by any inherited class and thus AbstractJavaWebscript will remain relatively unaffected by this. 
     *
     * @return The resulting string, ie "{'message':response}" or "{}"
     */
    private String createMmsVersionResponseJson() {
//        String resToString = response.toString();
//        String resStr = !Utils.isNullOrEmpty( resToString ) ? resToString.replaceAll( "\n", "" ) : "";
        return !Utils.isNullOrEmpty( mmsVersionResponse ) ? String.format("{\"message\":\"%s\"}", mmsVersionResponse) : "{}";
    }
}
