/**
 * 
 */
package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.ae.event.Expression;
import gov.nasa.jpl.ae.event.Expression.Form;
import gov.nasa.jpl.ae.event.FunctionCall;
import gov.nasa.jpl.ae.sysml.SystemModelToAeExpression;
import gov.nasa.jpl.mbee.util.Debug;
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
     * Get the viewpoint to which this view conforms.
     */
    public EmsScriptNode getViewpoint() {
        if ( viewNode == null ) return null;
        EmsScriptNode viewpoint = null;

        // Get all elements of Conform type:
        Collection<EmsScriptNode> conformElements = model.getType(null, Acm.JSON_CONFORM);

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
        
        return viewpoint;
    }
    
    public Collection< EmsScriptNode > getExposedElements() {
        if ( viewNode == null ) return null;

        Collection<EmsScriptNode> exposed = new ArrayList<EmsScriptNode>();

        // Get all relationship elements of Expose type:
        Collection<EmsScriptNode> exposeElements = model.getType(null, Acm.JSON_EXPOSE);
        //Collection<EmsScriptNode> exposeElements = model.getRelationship(null, "Expose");  // Can we call this?

        Debug.outln( "Expose relationships of " + viewNode + ": "
                     + exposeElements );
        
        // Check if any of the nodes in the passed collection of Expose or Conform
        // elements have the View as a sysml:source:
        for ( EmsScriptNode node : exposeElements ) {
            
            // If the sysml:source of the Expose element is the View, then
            // add it to our expose list (there can be multiple exposes for
            // a view):
            if ( model.getSource( node ).equals( viewNode ) ) { 

                // Get the target(s) of the Expose relationship:

                Collection<EmsScriptNode> nodes = model.getTarget(node);
                
                if (!Utils.isNullOrEmpty(nodes)) {
                    exposed.addAll(nodes);
                }
            }
        }
        
        return exposed;
    }
    
    
    public EmsScriptNode getViewpointExpression() {
        EmsScriptNode viewpoint = getViewpoint();
        if ( viewpoint == null ) return null;
        
        // Get the Method Property from the ViewPoint element
        //      The Method Property owner is the Viewpoint
        
        Collection< EmsScriptNode > viewpointMethods =
                model.getProperty( viewpoint, "method" );
        
        EmsScriptNode viewpointMethod = null;
        
        if ( viewpointMethods.size() > 0 ) {
            viewpointMethod = viewpointMethods.iterator().next();
        }
        
        if ( viewpointMethod == null ) viewpointMethod = viewpoint;  // HACK -- TODO

        // Get the value of the elementValue of the Method Property, which is an
        // Operation:

        // Collection< EmsScriptNode > viewpointExpr =
        //   model.getPropertyWithType( viewpointMethod,
        //                              getTypeWithName(null, "Expression" ) );
        Collection< EmsScriptNode > viewpointExprs =
                model.getProperty( viewpointMethod, Acm.JSON_EXPRESSION );
        EmsScriptNode viewpointExpr = null;
        if ( !Utils.isNullOrEmpty( viewpointExprs ) ) {
            viewpointExpr = viewpointExprs.iterator().next();
        }
        if ( viewpointExpr == null ) {
            if ( viewpointMethod != null ) {
                Collection< Object > exprs =
                        model.op( Operation.GET,
                                  Utils.newList( ModelItem.PROPERTY ),
                                  Utils.newList( new Item( viewpointMethod,
                                                           ModelItem.PROPERTY ) ),
                                  Utils.newList( new Item( "Expression",
                                                           ModelItem.TYPE ) ),
                                  null, false );
                if ( !Utils.isNullOrEmpty( exprs ) ) {
                    for ( Object o : exprs ) {
                        if ( o instanceof EmsScriptNode ) {
                            viewpointExpr = (EmsScriptNode)o;
                        }
                    }
                }
//                for ( EmsScriptNode node : model.getProperty( viewpointMethod, null ) ) {
//                    for ( EmsScriptNode tNode : model.getType( node, null ) ) {
//                        if ( )
//                    }
//                }
            }
        }
        if ( viewpointExpr == null ) viewpointExpr = viewpointMethod;  // HACK -- TODO

        if ( viewpointExpr == null ) {
            Debug.error(true, "Could not find viewpoint expression in " + viewpoint);
        }
        return viewpointExpr;
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
        
        // Get the related elements that define the the view.
        
        Collection<EmsScriptNode> exposed = getExposedElements();
        //EmsScriptNode viewpoint = getViewpoint();
        EmsScriptNode viewpointExpr = getViewpointExpression();
        if ( viewpointExpr == null ) return null;
        
        // Translate the viewpoint Operation/Expression element into an AE Expression:
        
        SystemModelToAeExpression< EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel > sysmlToAeExpr =
                new SystemModelToAeExpression< EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel >( getModel() );
        Expression< Object > aeExpr = sysmlToAeExpr.toAeExpression( viewpointExpr );

        // Set the function call arguments with the exposed model elements:
        
        if ( aeExpr.getForm() == Form.Function ) {
            Vector< Object > args = new Vector< Object >( exposed );
            ( (FunctionCall)aeExpr.expression ).setArguments( args  );
        }

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

}
