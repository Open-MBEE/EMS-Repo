/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").  
 * U.S. Government sponsorship acknowledged.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met:
 * 
 *  - Redistributions of source code must retain the above copyright notice, this list of 
 *    conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list 
 *    of conditions and the following disclaimer in the documentation and/or other materials 
 *    provided with the distribution.
 *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, 
 *    nor the names of its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER  
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;

/**
 * static class for webscript utilities
 * @author cinyoung
 *
 */
public class WebScriptUtil {
    // needed for Lucene search
    protected static final StoreRef SEARCH_STORE = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");

    // defeat instantiation
    private WebScriptUtil() {
        // do nothing
    }
    
    public static Set<EmsScriptNode> getAllNodesInPath(String qnamePath, String luceneContext, String acmType, Date dateTime, ServiceRegistry services, StringBuffer response) {
        String pattern = luceneContext + ":\"" + acmType + "\"";
        Set<EmsScriptNode> set = new HashSet<EmsScriptNode>();
        
        ResultSet resultSet = null;
        try {
            // TODO -- REVIEW -- Will lucene return deleted nodes that may have
            // existed at the specified date/time?
            resultSet = NodeUtil.luceneSearch( pattern, services ); 
            for (ResultSetRow row: resultSet) {
                NodeRef nr = row.getNodeRef();
                if ( nr == null ) continue;
                if ( dateTime != null ) {
                    nr = NodeUtil.getNodeRefAtTime( nr, dateTime );
                    if ( nr == null ) continue;
                }
                EmsScriptNode node = new EmsScriptNode(nr, services, response);
                // filter by project
                if (node.getQnamePath().startsWith(qnamePath)) {
                    set.add(node);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();  
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        
        return set;
    }
}
