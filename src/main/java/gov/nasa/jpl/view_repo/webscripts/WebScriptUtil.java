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

import java.util.HashSet;
import java.util.Set;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;

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
    
    public static Set<EmsScriptNode> getAllNodesInPath(String qnamePath, String luceneContext, String acmType, ServiceRegistry services, StringBuffer response) {
        String pattern = luceneContext + ":\"" + acmType + "\"";
        Set<EmsScriptNode> set = new HashSet<EmsScriptNode>();
        
        ResultSet resultSet = null;
        try {
            resultSet = services.getSearchService().query(SEARCH_STORE, SearchService.LANGUAGE_LUCENE, pattern);
            for (ResultSetRow row: resultSet) {
                EmsScriptNode node = new EmsScriptNode(row.getNodeRef(), services, response);
                // filter by project
                if (node != null && node.exists()) {
                    if (node.getQnamePath().startsWith(qnamePath)) {
                        set.add(node);
                    }
                }
            }
        } catch (Exception e) {
            // catch result set exception - if its invalid
            e.printStackTrace();  
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        
        return set;
    }
}
