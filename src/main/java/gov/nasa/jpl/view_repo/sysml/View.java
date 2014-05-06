/**
 * 
 */
package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.ae.event.Expression;
import gov.nasa.jpl.ae.event.Expression.Form;
import gov.nasa.jpl.ae.event.FunctionCall;
import gov.nasa.jpl.ae.sysml.SystemModelToAeExpression;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.MoreToString;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsSystemModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import sysml.SystemModel.ModelItem;
import sysml.SystemModel.Operation;
import sysml.Viewable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import sysml.SystemModel.Item;
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
     * Helper method to get the source property for the passed node
     */
    private EmsScriptNode getSource(EmsScriptNode node) {
    	
    	Collection<EmsScriptNode> sources = getModel().getSource( node );

    	if (!Utils.isNullOrEmpty(sources)) {
    		return sources.iterator().next();
    	}
    	else {
    		return null;
    	}
    }
    
    /**
     * Get the viewpoint to which this view conforms.
     */
    public EmsScriptNode getViewpoint() {
        if ( viewNode == null ) return null;
        EmsScriptNode viewpoint = null;

        // Get all elements of Conform type:
        Collection<EmsScriptNode> conformElements = getModel().getType(null, Acm.JSON_CONFORM);
        System.out.println( "Got "
                            + ( conformElements == null ? 0
                                                        : conformElements.size() )
                            + " elements of type " + Acm.JSON_CONFORM );
        for ( EmsScriptNode node : conformElements ) {
            
            // If the sysml:source of the Compose element is the View:
            if (getSource(node).equals( viewNode ) ) { 
                
                // Get the target of the Conform relationship (the Viewpoint):
                Collection<EmsScriptNode> viewpointNodes = getModel().getTarget(node);
                
                if (!Utils.isNullOrEmpty(viewpointNodes)) {
                    viewpoint = viewpointNodes.iterator().next();
                }               
                break;
            }
        }
        
        return viewpoint;
    }
    
    public Collection< EmsScriptNode > getExposedElements() {
        if ( viewNode == null ) return null;

        Collection<EmsScriptNode> exposed = new ArrayList<EmsScriptNode>();

        // Get all relationship elements of Expose type:
        Collection<EmsScriptNode> exposeElements = getModel().getType(null, Acm.JSON_EXPOSE);
        //Collection<EmsScriptNode> exposeElements = getModel().getRelationship(null, "Expose");  // Can we call this?

        Debug.outln( "Expose relationships of " + viewNode + ": "
                     + exposeElements );
        
        // Check if any of the nodes in the passed collection of Expose or Conform
        // elements have the View as a sysml:source:
        for ( EmsScriptNode node : exposeElements ) {
            
            // If the sysml:source of the Expose element is the View, then
            // add it to our expose list (there can be multiple exposes for
            // a view):
            if (getSource(node).equals( viewNode ) ) { 

                // Get the target(s) of the Expose relationship:

                Collection<EmsScriptNode> nodes = getModel().getTarget(node);
                
                if (!Utils.isNullOrEmpty(nodes)) {
                    exposed.addAll(nodes);
                }
            }
        }
        
        return exposed;
    }
    
    
    public EmsScriptNode getViewpointOperation() {
        EmsScriptNode viewpoint = getViewpoint();
        if ( viewpoint == null ) return null;
        
        // Get the Method property from the ViewPoint element:        
        Collection< EmsScriptNode > viewpointMethods =
                getModel().getProperty( viewpoint, "method" );
        
        EmsScriptNode viewpointMethod = null;
        
        if ( viewpointMethods.size() > 0 ) {
            viewpointMethod = viewpointMethods.iterator().next();
        }
        
        if ( viewpointMethod == null ) viewpointMethod = viewpoint;  // HACK -- TODO

        return viewpointMethod;
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
    	
        if ( viewNode == null ) {
            System.out.println("*** called View.toViewJson() without a view node! View = " + toBoringString() );
            return null;
        }
        
        // Get the related elements that define the the view.
        
        Collection<EmsScriptNode> exposed = getExposedElements();
        // TODO -- need to handle case where viewpoint operation does not exist
        //         and an external function (e.g., Java) is somehow specified.
        EmsScriptNode viewpointOp = getViewpointOperation(); 
        if ( viewpointOp == null ) {
            System.out.println("*** View.toViewJson(): no viewpoint operation! View = " + toBoringString() );
            return null;
        }
        
        // Translate the viewpoint Operation/Expression element into an AE Expression:
        ArrayList<Object> paramValList = new ArrayList<Object>();
        // This is a List of a collection of nodes, where the value of exposed 
        // parameter is a collection of nodes:
        paramValList.add( exposed );  
        SystemModelToAeExpression< EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel > sysmlToAeExpr =
                new SystemModelToAeExpression< EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel >( getModel() );
        Expression< Object > aeExpr = sysmlToAeExpr.operationToAeExpression(viewpointOp, paramValList);

        if ( aeExpr == null ) return null;
        
        // Evaluate the expression to get the Viewables and add them to this View.

        clear(); // make sure we clear out any old information
        Object evalResult = aeExpr.evaluate( true );
        if ( evalResult instanceof Viewable ) {
            Viewable< EmsScriptNode > v = (Viewable< EmsScriptNode >)evalResult;
            add( v );
        } else if ( evalResult instanceof Collection ) {
            Collection< ? > c = (Collection< ? >)evalResult;
            for ( Object o : c ) {
                try {
                    Viewable< EmsScriptNode > viewable =
                            (Viewable< EmsScriptNode >)o;
                    add( viewable );
                } catch ( ClassCastException e ) {
                    e.printStackTrace();
                }
            }
//            java.util.List< Viewable<EmsScriptNode> > viewables =
//                    (java.util.List< Viewable<EmsScriptNode> >)Utils.asList( c );
//            addAll( viewables );
        }

        // Generate the JSON for the view now that the View is populated with
        // Viewables.

        JSONObject json = new JSONObject();
        JSONObject viewProperties = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try {
            json.put("views", jsonArray);
            jsonArray.put( viewProperties );
            viewProperties.put("id", viewNode.getName() );

            JSONArray elements = new JSONArray();
            viewProperties.put("displayedElements", elements );
            for ( EmsScriptNode elem : getDisplayedElements() ) {
                elements.put( elem.getName() );
            }

            elements = new JSONArray();
            viewProperties.put("allowedElements", elements );
            for ( EmsScriptNode elem : getDisplayedElements() ) {
                elements.put( elem.getName() );
            }

            elements = new JSONArray();
            viewProperties.put("childrenViews", elements );
            for ( sysml.View<EmsScriptNode> view : getChildViews() ) {
                if ( view instanceof View ) {
                    elements.put( view.getElement().getName() );
                }
            }

            JSONArray viewables = new JSONArray();
            viewProperties.put("contains", viewables );
            for ( Viewable< EmsScriptNode > viewable : this ) {
                if ( viewable != null ) {
                    viewables.put( viewable.toViewJson() );
                }
            }

        } catch ( JSONException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return json;
    }
    
    private String toBoringString() {
        return "view node = "
               + this.viewNode
               + "; java.util.List = "
               + MoreToString.Helper.toString( this, false, false, null, null,
                                               MoreToString.PARENTHESES, true );
    }

    @Override
    public String toString() {
//        JSONObject jo = toViewJson();
//        if ( jo != null ) return jo.toString();
        return "toViewJson() FAILED: " + toBoringString();
    }

}
