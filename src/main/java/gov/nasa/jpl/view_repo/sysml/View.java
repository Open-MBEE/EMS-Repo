/**
 * 
 */
package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsSystemModel;

import java.util.Collection;

import sysml.Viewable;

import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A View embeds {@link Viewable}s and itself is a {@link Viewable}.
 *
 */
public class View extends List implements sysml.View< EmsScriptNode > {

    private static final long serialVersionUID = -7618965504816221446L;
    
    protected EmsScriptNode viewNode = null;
    
    protected EmsSystemModel model = null;

    /**
     * @see List#List() 
     */
    public View() {
        super();
    }

    /**
     * Create a View object from the view element node. 
     * @see List#List() 
     */
    public View( EmsScriptNode viewNode ) {
        this();
        setViewNode( viewNode );
    }

    /**
     * Create a View and add the {@link Viewable}s in the input {@link Collection}.
     * @param c
     * @see List#List(Collection)
     */
    public View( Collection< ? extends Viewable< EmsScriptNode >> c ) {
        super( c );
    }

    /**
     * @param initialCapacity
     */
    public View( int initialCapacity ) {
        super( initialCapacity );
    }

    /**
     * @return the viewNode
     */
    public EmsScriptNode getViewNode() {
        return viewNode;
    }

    /**
     * @param viewNode the viewNode to set
     */
    public void setViewNode( EmsScriptNode viewNode ) {
        this.viewNode = viewNode;
    }

    /**
     * @return the model
     */
    public EmsSystemModel getModel() {
        if ( model == null ) setModel( new EmsSystemModel() );
        return model;
    }

    /**
     * @param model the model to set
     */
    public void setModel( EmsSystemModel model ) {
        this.model = model;
    }

    /* (non-Javadoc)
     * @see sysml.View#getChildViews()
     */
    @Override
    public Collection< sysml.View< EmsScriptNode > > getChildViews() {
        // TODO Auto-generated method stub
        return null;
    }
    
    /** 
     * Create a JSON String for the View with the following format:
     * <P>
     * <code>
     *  {
     *       "views": 
     *           [ {
     *              "id": elementId,
     *              "displayedElements": [elementId, ...],
     *              "allowedElements": [elementId, ..],
     *              "childrenViews": [viewId, ..],
     *              "contains": [
     *                  {
     *                      "type": Paragraph", 
     *                      "sourceType": "reference"|"text",
     *                      
     *                      // if sourceType is reference
     *                      "source": elementId, 
     *                      "sourceProperty": "documentation"|"name"|"value", 
     *                      
     *                      // if sourceType is text
     *                      "text": text
     *                  },
     *                  {
     *                      "type": "Table", 
     *                      "title": title, 
     *                      "body": [[{
     *                          "content": [ //this allows multiple things in a cell
     *                              {
     *                                  "type": "Paragraph"|"List", 
     *                                  (see above...)
     *                              }, ...
     *                          ],
     *                          // optional
     *                          "colspan": colspan, 
     *                          "rowspan": rowspan
     *                      }, ...], ...], 
     *                      "header": same as body, 
     *                      // optional, probably translate to table class if user wants to customize css?
     *                      "style": style
     *                  },
     *                  {
     *                      "type": "List",
     *                      "list": [
     *                          [{ //each list item can have multiple things, Table in a list may not be supported
     *                              "type": "Paragraph"/"List"/"Table",
     *                              (see above...)
     *                          }, ...], ...
     *                      ],
     *                      "ordered": true/false
     *                  }, ...  
     *              ],
     *          }
     *      
     *      ]
     *  }
     * </code>
     *
     * @see gov.nasa.jpl.view_repo.sysml.List#toViewJson()
     */
    @Override
    public JSONObject toViewJson() {
        if ( viewNode == null ) return null;
//        EmsSystemModel model = new EmsSystemModel(services);
//        NodeRef viewNodeRef = view.getNodeRef();
        
        // Get all elements of Expose type:
        Collection<EmsScriptNode> exposeElements = model.getType(null, "Expose");
        
        // Get all elements of Conform type:
        Collection<EmsScriptNode> conformElements = model.getType(null, "Conform");
        
		// Check if any of the nodes in the passed collection of Expose or Conform
		// elements have the View as a sysml:source:
        for ( EmsScriptNode node : exposeElements ) {
        	
        	// If the sysml:source of the Expose element is the View:
            if ( model.getSource( node ).equals( viewNode ) ) { 
                
            }
        }
        
        // Get the unique Expose and Conform element with the View as a sysml:source:
//        EmsScriptNode myExposeElement = getMatchingExposeConformElements(viewNode, exposeElements);
//        EmsScriptNode myConformElement = getMatchingExposeConformElements(viewNode, conformElements);
        
//        if (myExposeElement != null && myConformElement != null) {
            
            // Get the target of the Conform relationship (the Viewpoint):
            
            // Get the Method Property from the ViewPoint element
            //      The Method Property owner is the Viewpoint
            
            // Get the value of the elementValue of the Method Property, which is an
            // Operation:
                        
            // Parse and convert the Operation:
            
            // Get the target of the Expose relationship:
            
            // Set the function call arguments with the exposed model elements:
            
            // Return the converted JSON from the expression evaluation:
            
//        }
//        else {
//            // TODO: error!
//        }

        JSONObject json = new JSONObject();
        JSONObject viewProperties = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try {
            json.append( "views", jsonArray );
            jsonArray.put( viewProperties );
            viewProperties.put("id", viewNode.getName() );
        } catch ( JSONException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // TODO Auto-generated method stub
        return json;
    }

}
