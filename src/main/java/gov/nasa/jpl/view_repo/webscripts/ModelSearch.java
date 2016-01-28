/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech"). U.S.
 * Government sponsorship acknowledged.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. - Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. - Neither the name of Caltech nor its operating
 * division, the Jet Propulsion Laboratory, nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Model search service that returns a JSONArray of elements
 * 
 * @author cinyoung
 * 
 */
public class ModelSearch extends ModelGet {
    public class UnsupportedSearchException extends Exception {
        private static final long serialVersionUID = 1L;
        String msg = "";
        
        public UnsupportedSearchException(String message) {
            this.msg = message;
        }
    }

    static Logger logger = Logger.getLogger( ModelSearch.class );

    public ModelSearch() {
        super();
    }

    public ModelSearch( Repository repositoryHelper, ServiceRegistry registry ) {
        super( repositoryHelper, registry );
    }

    protected final Map< String, String > searchTypesMap =
            new HashMap< String, String >() {
                private static final long serialVersionUID =
                        -7336887332666278453L;
                {
                    put( "documentation", "@sysml\\:documentation:\"" );
                    put( "name", "@sysml\\:name:\"" );
                    put( "id", "@sysml\\:id:\"" );
                    put( "aspect",
                         "ASPECT:\"{http://jpl.nasa.gov/model/sysml-lite/1.0}" );
                    // should ASPECT still be in this Map???
                    put( "appliedMetatypes", "@sysml\\:appliedMetatypes:\"" );
                    put( "metatypes", "@sysml\\:metatypes:\"" );
                    // value has special handling
                }
            };

    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        ModelSearch instance = new ModelSearch( repository, getServices() );
        // bad call shouldn't happen -  so prevent it here
        return instance.executeImplImpl( req, status, cache,
                                         runWithoutTransactions );
    }

    @Override
    protected Map< String, Object >
            executeImplImpl( WebScriptRequest req, Status status, Cache cache ) {
        printHeader( req );

        clearCaches();

        Map< String, Object > model = new HashMap< String, Object >();

        try {
            JSONObject top = NodeUtil.newJsonObject();
            JSONArray elementsJson = executeSearchRequest( req, top );

            top.put( "elements", elementsJson );
            if ( !Utils.isNullOrEmpty( response.toString() ) ) top.put( "message",
                                                                        response.toString() );
            model.put( "res", NodeUtil.jsonToString( top, 4 ) );
        } catch ( UnsupportedSearchException use ) {
            log( Level.WARN, HttpServletResponse.SC_BAD_REQUEST, use.msg );
            model.put( "res", createResponseJson() );
        } catch ( JSONException e ) {
            log( Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,
                 "Could not create the JSON response" );
            model.put( "res", createResponseJson() );
            e.printStackTrace();
        }

        status.setCode( responseStatus.getCode() );

        printFooter();

        return model;
    }

    private
            JSONArray
            executeSearchRequest( WebScriptRequest req, JSONObject top )
                                                                        throws JSONException, UnsupportedSearchException {
        String keyword = req.getParameter( "keyword" );
        String propertyName = req.getParameter( "propertyName" );
        Integer maxItems = Utils.parseInt( req.getParameter( "maxItems" ) );
        Integer skipCount = Utils.parseInt( req.getParameter( "skipCount" ) );
        String[] filters =
                req.getParameter( "filters" ) == null
                                                     ? new String[] { "documentation" }
                                                     : req.getParameter( "filters" )
                                                          .split( "," );
        
        // add all if necessary                                             
        for (int ii = 0; ii < filters.length; ii++) {
            if (filters[ii].equalsIgnoreCase( "all" ) || filters[ii].equals( "*" )) {
                HashSet<String> allFilterKeys = new HashSet<String>();
                allFilterKeys.addAll( searchTypesMap.keySet() );
                allFilterKeys.add( "value" );
                filters = allFilterKeys.toArray( new String[allFilterKeys.size()] );
                break;
            }
        }
        
        boolean evaluate = getBooleanArg( req, "evaluate", false );

        if ( !Utils.isNullOrEmpty( keyword ) ) {

            // get timestamp if specified
            String timestamp = req.getParameter( "timestamp" );
            Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

            if (dateTime != null) throw new UnsupportedSearchException("Cannot search against timestamp.");
            
            Map< String, EmsScriptNode > rawResults =
                    new HashMap< String, EmsScriptNode >();

            WorkspaceNode workspace = getWorkspace( req );
            
            Map<String, Set<String>> id2SearchTypes = new HashMap<String, Set<String>>();
            ArrayList<String> types = new ArrayList<String>();
            for ( String searchType : filters ) {
                if ( !searchType.equals( "value" ) ) {
                    String lucenePrefix = searchTypesMap.get( searchType );
                    if ( lucenePrefix != null ) {
                        types.add(searchTypesMap.get( searchType ));
                    } else {
                        log( Level.INFO, HttpServletResponse.SC_BAD_REQUEST,
                             "Unexpected filter type: " + searchType );
                        return null;
                    }
                } else {
                    try {
                        Integer.parseInt( keyword );
                        types.add("@sysml\\:integer:\"");
                        types.add("@sysml\\:naturalValue:\"");
                    } catch ( NumberFormatException nfe ) {
                        // do nothing
                    }

                    try {
                        // Need to do Double.toString() in case they left out
                        // the decimal in something like 5.0
                        double d = Double.parseDouble( keyword );
                        types.add("@sysml\\:double:\"");
                    } catch ( NumberFormatException nfe ) {
                        // do nothing
                    }

                    if ( keyword.equalsIgnoreCase( "true" )
                         || keyword.equalsIgnoreCase( "false" ) ) {
                    	types.add("@sysml\\:boolean:\"");
                    }
                    types.add("@sysml\\:string:\"");
                }
            }
          if (types == null || types.size() < 1) throw new UnsupportedSearchException("types is not specificed correctly");
          Map<String, EmsScriptNode> results = searchForElements( types,
          keyword, false,
          workspace, dateTime, maxItems, skipCount );
		  rawResults.putAll( results );

            if (NodeUtil.doGraphDb) {
                // filter out based on postgres graph db
                List<String> noderefs = new ArrayList<String>(rawResults.keySet());
                
                PostgresHelper pgh = null;
                String workspaceId = ""; // can't use workspace in constructor, since filtering doesn't expect "master" as id
                if ( workspace != null ) workspaceId = workspace.getId();
                pgh = new PostgresHelper( workspaceId );
    
                List< String > filteredNoderefs = null;
                try {
                    // make sure workspace exists
                    pgh.connect();
                    if (!pgh.checkWorkspaceExists()) throw new UnsupportedSearchException("Workspace is not search compatible");
                    pgh.close();
                    
                    if (noderefs.size() > 0) {
                        pgh.connect();
                        
                        filteredNoderefs =
                                pgh.filterNodeRefsByWorkspace( noderefs, workspaceId  );
        
                        pgh.close();
                    }
                } catch ( SQLException e ) {
                    e.printStackTrace();
                } catch ( ClassNotFoundException e ) {
                    e.printStackTrace();
                }
            
                if (filteredNoderefs != null && filteredNoderefs.size() > 0) {
                    Map<String, EmsScriptNode> filteredResults = new HashMap<String, EmsScriptNode>();
                    for (String filteredNoderef: filteredNoderefs) {
                        EmsScriptNode filteredNode = rawResults.get(filteredNoderef);
                        if (filteredNode != null) {
                            filteredResults.put( filteredNode.getSysmlId(), filteredNode );
                        }
                    }
                    rawResults = filteredResults;
                    
                    // TODO: Add getting document results
                }
            }

            // filter out _pkgs:
            for ( String sysmlid : rawResults.keySet() ) {
                if ( !sysmlid.endsWith( "_pkg" ) ) {
                    elementsFound.put( sysmlid, rawResults.get( sysmlid ) );
                }
            }

            filterValueSpecs( propertyName, workspace, dateTime );

            addElementProperties( workspace, dateTime );            

            boolean checkReadPermission = true; // TODO -- REVIEW -- Should this
                                                // be false?
            boolean includeQualified = true;
            if (NodeUtil.doPostProcessQualified) includeQualified = false;
            handleElements( workspace, dateTime, includeQualified, true, evaluate, top,
                            checkReadPermission );
            
            for (int ii = 0; ii < elements.length(); ii++) {
                JSONObject elementJson = elements.getJSONObject( ii );
                String sysmlid = elementJson.getString( "sysmlid" );
                if (!id2SearchTypes.containsKey( sysmlid )) {
                    logger.warn( "could not find " + sysmlid + " in id2SearchTypes map" );
                } else {
                    String searchTypes = null;
                    for (String stype: id2SearchTypes.get( sysmlid )) {
                        if (searchTypes == null) { 
                            searchTypes = stype;
                        } else {
                            searchTypes += "," + stype;
                        }
                    }
                    elementJson.put( "searchTypes", searchTypes );
                }
            }
        }

        return elements;
    }
}
