/**
 * 
 */
package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsSystemModel;

import java.util.ArrayList;
import java.util.Collection;

import sysml.Viewable;

import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A View embeds {@link Viewable}s and itself is a {@link Viewable}. View
 * inherits from List so that it may contain Viewables in addition to having
 * View children.
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
        setElement( viewNode );
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

    @Override
    public EmsScriptNode getElement() {
        return viewNode;
    }

    /**
     * @param viewNode the viewNode to set
     */
    @Override
    public void setElement( EmsScriptNode viewNode ) {
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
        return Utils.getEmptyList();
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
        
        // Get all elements of Expose type:
        Collection<EmsScriptNode> exposeElements = model.getType(null, "Expose");
        
        // Get all elements of Conform type:
        Collection<EmsScriptNode> conformElements = model.getType(null, "Conform");
        
        Collection<EmsScriptNode> matchingExposeElements = new ArrayList<EmsScriptNode>();
        Collection<EmsScriptNode> exposed = new ArrayList<EmsScriptNode>();
        EmsScriptNode viewpoint = null;

		// Check if any of the nodes in the passed collection of Expose or Conform
		// elements have the View as a sysml:source:
        for ( EmsScriptNode node : exposeElements ) {
        	
        	// If the sysml:source of the Expose element is the View, then
        	// add it to our expose list (there can be multiple exposes for
        	// a view):
            if ( model.getSource( node ).equals( viewNode ) ) { 
                matchingExposeElements.add(node);
            }
        }
        
        for ( EmsScriptNode node : conformElements ) {
        	
        	// If the sysml:source of the Compose element is the View:
            if ( model.getSource( node ).equals( viewNode ) ) { 
            	
                // Get the target of the Conform relationship (the Viewpoint):
                Collection<EmsScriptNode> viewpointNodes = model.getTarget(node);
                
                if (!Utils.isNullOrEmpty(viewpointNodes)) {
                	viewpoint = viewpointNodes.iterator().next();
                }            	
                break;
            }
        }
                    
        // Get the Method Property from the ViewPoint element
        //      The Method Property owner is the Viewpoint
        
        // Get the value of the elementValue of the Method Property, which is an
        // Operation:
                    
        // Parse and convert the Operation:
        
        // Get the target(s) of the Expose relationship:
        for (EmsScriptNode exposeNode : matchingExposeElements) {
        	
            Collection<EmsScriptNode> nodes = model.getTarget(exposeNode);
            
            if (!Utils.isNullOrEmpty(nodes)) {
            	exposed.add(nodes.iterator().next());
            }

        }
        
        // Set the function call arguments with the exposed model elements:
        
        // Return the converted JSON from the expression evaluation:
            

        JSONObject json = new JSONObject();
        JSONObject viewProperties = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try {
            json.append( "views", jsonArray );
            jsonArray.put( viewProperties );
            viewProperties.put("id", viewNode.getId() );

            JSONArray elements = new JSONArray();
            viewProperties.put("displayedElements", elements );
            for ( EmsScriptNode elem : getDisplayedElements() ) {
                elements.put( elem.getId() );
            }

            elements = new JSONArray();
            viewProperties.put("allowedElements", elements );
            for ( EmsScriptNode elem : getDisplayedElements() ) {
                elements.put( elem.getId() );
            }

            elements = new JSONArray();
            viewProperties.put("childrenViews", elements );
            for ( sysml.View<EmsScriptNode> view : getChildViews() ) {
                if ( view instanceof View ) {
                    elements.put( view.getElement().getId() );
                }
            }

            JSONArray viewables = new JSONArray();
            viewProperties.put("contains", viewables );
            for ( Viewable viewable : this ) {
                if ( viewable != null ) {
                    viewables.put( viewable.toViewJson() );
                }
            }

        } catch ( JSONException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // TODO Auto-generated method stub
        return json;
    }

}
