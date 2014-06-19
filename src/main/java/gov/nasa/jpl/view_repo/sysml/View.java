/**
 * 
 */
package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.ae.event.Expression;
import gov.nasa.jpl.ae.sysml.SystemModelToAeExpression;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.MoreToString;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsSystemModel;
import gov.nasa.jpl.view_repo.util.NodeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import sysml.view.Viewable;
/**
 * A View embeds {@link Viewable}s and itself is a {@link Viewable}. View
 * inherits from List so that it may contain Viewables in addition to having
 * View children.
 * 
 */
public class View extends List implements sysml.view.View< EmsScriptNode >, Comparator<View>, Comparable<View> {

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

    /**
     * Override equals for EmsScriptNodes
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( obj instanceof View ) {
            if ( ((View)obj).getElement().equals( getElement() ) ) return true; 
        }
        return false;
    }

    
    /*
     * (non-Javadoc)
     * 
     * @see sysml.View#getChildViews()
     */
    @Override
    // TODO -- need to support a flag for recursion
    public Collection< sysml.view.View< EmsScriptNode > > getChildViews() {
        ArrayList< sysml.view.View< EmsScriptNode > > childViews =
                new ArrayList< sysml.view.View< EmsScriptNode > >();
        for ( EmsScriptNode node : getChildViewElements( null ) ) {
            childViews.add( new View( node ) );
        }
        return childViews;
    }

    // TODO -- need to support a flag for recursion
    public Collection< EmsScriptNode > getChildViewElements(Date dateTime) {
//        ArrayList< EmsScriptNode > childViews =
//                new ArrayList< EmsScriptNode >();
        Object o = viewNode.getProperty( Acm.ACM_CHILDREN_VIEWS );
        Collection< EmsScriptNode > childViews = getElementsForJson( o, dateTime );
      
//        JSONArray jarr = null;
//        if ( o instanceof String ) {
//            String s = (String)o;
//            if ( s.length() > 0 && s.charAt( 0 ) == '[' ) {
//                try {
//                    jarr = new JSONArray( s );
//                } catch ( JSONException e ) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        } else if ( o instanceof JSONArray ) {
//            jarr = (JSONArray)o;
//        }
//        if ( jarr != null ) {
//            for ( int i = 0; i < jarr.length(); ++i ) {
//                Object o1 = null;
//                try {
//                    o1 = jarr.get( i );
//                } catch ( JSONException e ) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//                if ( o1 instanceof String ) {
//                    NodeRef nodeRef =
//                            NodeUtil.findNodeRefById( (String)o1,
//                                                      getModel().getServices() );
//                    if ( nodeRef != null ) {
//                        EmsScriptNode node =
//                                new EmsScriptNode( nodeRef,
//                                                   getModel().getServices() );
//                        childViews.add( node );
//                    }
//                }
//            }
//        }

        return childViews;
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
        if (Debug.isOn()) System.out.println( "Got "
                            + ( conformElements == null ? 0
                                                        : conformElements.size() )
                            + " elements of type " + Acm.JSON_CONFORM );
        for ( EmsScriptNode node : conformElements ) {
            
            // If the sysml:source of the Compose element is the View:
            EmsScriptNode source = getSource(node);
            if (source != null && source.equals( viewNode ) ) { 
                
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

        if (Debug.isOn()) Debug.outln( "Expose relationships of " + viewNode + ": "
                     + exposeElements );
        
        // Check if any of the nodes in the passed collection of Expose or Conform
        // elements have the View as a sysml:source:
        for ( EmsScriptNode node : exposeElements ) {
            
            // If the sysml:source of the Expose element is the View, then
            // add it to our expose list (there can be multiple exposes for
            // a view):
            EmsScriptNode source = getSource(node);
            if (source != null && source.equals( viewNode ) ) { 

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
        //if ( viewpoint == null || !viewpoint.exists() ) return null;
        
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

    public void generateViewables() {
        // Get the related elements that define the the view.
        
        Collection<EmsScriptNode> exposed = getExposedElements();
        // TODO -- need to handle case where viewpoint operation does not exist
        //         and an external function (e.g., Java) is somehow specified.
        EmsScriptNode viewpointOp = getViewpointOperation(); 
        if ( viewpointOp == null ) {
            if (Debug.isOn()) System.out.println("*** View.toViewJson(): no viewpoint operation! View = " + toBoringString() );
            return;
        }
        
        // Translate the viewpoint Operation/Expression element into an AE Expression:
        ArrayList<Object> paramValList = new ArrayList<Object>();
        // This is a List of a collection of nodes, where the value of exposed 
        // parameter is a collection of nodes:
        paramValList.add( exposed );  
        SystemModelToAeExpression< EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel > sysmlToAeExpr =
                new SystemModelToAeExpression< EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel >( getModel() );
        Expression< Object > aeExpr = sysmlToAeExpr.operationToAeExpression(viewpointOp, paramValList);

        if ( aeExpr == null ) return;
        
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
            if (Debug.isOn()) System.out.println("*** called View.toViewJson() without a view node! View = " + toBoringString() );
            return null;
        }
        
        // Get the related elements that define the the view.
        generateViewables();

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
            for ( EmsScriptNode elem : getAllowedElements() ) {
                elements.put( elem.getName() );
            }

            elements = new JSONArray();
            viewProperties.put("childrenViews", elements );
            for ( sysml.view.View<EmsScriptNode> view : getChildViews() ) {
                if ( view instanceof View ) {
                    elements.put( view.getElement().getName() );
                }
            }

            JSONArray viewables = getContainsJson();
            viewProperties.put("contains", viewables );

        } catch ( JSONException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return json;
    }
    
    public JSONArray getContainsJson() {
        JSONArray viewablesJson = new JSONArray();

        if ( isEmpty() ) generateViewables();

        for ( Viewable< EmsScriptNode > viewable : this ) {
            if ( viewable != null ) {
                viewablesJson.put( viewable.toViewJson() );
            }
        }
        if ( viewablesJson.length() == 0 && getElement() != null ) {
            Object contains = getElement().getProperty( Acm.ACM_CONTAINS );
            if ( contains != null ) {
                if ( contains instanceof JSONArray ) return (JSONArray)contains;

                try {
                    JSONArray jarr = new JSONArray( "" + contains );
                    viewablesJson = jarr;
                } catch ( JSONException e ) {
                    e.printStackTrace();
                }
            }
        }
        return viewablesJson;
    }
    
    /**
     * This is a cache of the displayed elements to keep from having to
     * recompute them as is done for allowed elements.
     */
    private Collection<EmsScriptNode> displayedElements = null;
    
    public Collection<EmsScriptNode> getAllowedElements() {
        if ( displayedElements != null ) {
            return displayedElements;
        }
        return getDisplayedElements();
    }

    @Override
    // TODO -- need to support a flag for recursion?
    public Collection<EmsScriptNode> getDisplayedElements() {
        return getDisplayedElements( null, new HashSet<View>() );
    }

    // TODO -- need to support a flag for recursion?
    public Collection<EmsScriptNode> getDisplayedElements( Date dateTime, Set<View> seen) {
        if ( seen.contains( this ) ) return Utils.getEmptySet();
        seen.add( this );
        LinkedHashSet<EmsScriptNode> set = new LinkedHashSet<EmsScriptNode>();
        
        if ( isEmpty() ) {
            generateViewables(); 
        } else if ( displayedElements != null ) {
            return displayedElements;
        }
        
        Collection< EmsScriptNode > versionedElements = super.getDisplayedElements();
        versionedElements = NodeUtil.getVersionAtTime( versionedElements, dateTime );
        set.addAll( versionedElements );
        for ( sysml.view.View< EmsScriptNode > v : getChildViews() ) {
            EmsScriptNode n = NodeUtil.getVersionAtTime( v.getElement(), dateTime );
            if ( n == null ) continue;
            v = new View( n );
            if ( v instanceof View ) {
                set.addAll( ((View)v).getDisplayedElements( dateTime, seen ) );
            } else {
             // REVIEW -- this case seems impossible to reach since v is assigned a View
                Collection<EmsScriptNode> moreElements = v.getDisplayedElements();
                Collection<EmsScriptNode> vElems = NodeUtil.getVersionAtTime( moreElements, dateTime );
                set.addAll( vElems );
            }
        }
        Collection< EmsScriptNode > v2vs = getViewToViewPropertyViews(dateTime);
        for ( EmsScriptNode node : v2vs ) {
            if ( node.isView() ) {
                View v = new View( node );
                set.addAll( v.getDisplayedElements( dateTime, seen ) );
            }
        }
        Object dElems = getElement().getProperty( Acm.ACM_DISPLAYED_ELEMENTS );
        set.addAll( getElementsForJson( dElems, dateTime ) );
        displayedElements = set;
        return set;
    }
    
    public Collection<EmsScriptNode> getElementsForJson( Object obj, Date dateTime ) {
        if ( obj == null ) return Utils.getEmptyList();
        LinkedHashSet<EmsScriptNode> nodes = new LinkedHashSet<EmsScriptNode>();
        if ( obj instanceof JSONArray ) {
            return getElementsForJson( (JSONArray)obj, dateTime );
        }
        if ( obj instanceof JSONObject ) {
            return getElementsForJson( (JSONObject)obj, dateTime );
        }
        String idString = "" + obj;
        if ( idString.length() <= 0 ) return Utils.getEmptyList();
        try {
            if ( idString.trim().charAt( 0 ) == '[' ) {
                JSONArray jArray = new JSONArray( idString );
                return getElementsForJson( jArray, dateTime );
            }
            if ( idString.trim().charAt( 0 ) == '{' ) {
                JSONObject jsonObject =  new JSONObject( idString );
                return getElementsForJson( jsonObject, dateTime );
            }
        } catch ( JSONException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        EmsScriptNode node =
                    EmsScriptNode.convertIdToEmsScriptNode( idString, dateTime,
                                                            getElement().getServices(),
                                                            getElement().getResponse(),
                                                            getElement().getStatus() );
        if ( node != null ) nodes.add( node );
        return nodes;
    }
    
    public Collection<EmsScriptNode> getElementsForJson( JSONArray jsonArray, Date dateTime ) {
        if ( jsonArray == null ) return Utils.getEmptyList();
        LinkedHashSet<EmsScriptNode> nodes = new LinkedHashSet<EmsScriptNode>();
        for ( int i = 0; i < jsonArray.length(); ++i ) {
            Object id = null;
            try {
                id = jsonArray.get(i);
                nodes.addAll(getElementsForJson( id, dateTime ) );
            } catch ( JSONException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return nodes;
    }

    public Collection<EmsScriptNode> getElementsForJson( JSONObject o, Date dateTime ) {
        if ( o == null ) return Utils.getEmptyList();
        LinkedHashSet<EmsScriptNode> nodes = new LinkedHashSet<EmsScriptNode>();
        String[] names = JSONObject.getNames( o );
        if ( names == null ) return nodes;
        for ( String name : names ) {
            try {
                Object ids = o.get( name );
                nodes.addAll( getElementsForJson( ids, dateTime ) );
            } catch ( JSONException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return nodes;
    }
    
    public Collection<EmsScriptNode> getViewToViewPropertyViews( Date dateTime ) {
        JSONArray jarr = getViewToViewPropertyJson();
        Collection< EmsScriptNode > coll = getElementsForJson( jarr, dateTime );
        coll.remove( this );
        return coll;
    }

    public JSONArray getViewToViewPropertyJson() {
        if ( getElement() == null ) return null;
        Object v2vObj = getElement().getProperty( Acm.ACM_VIEW_2_VIEW );
        if ( v2vObj == null ) return null;
        if ( v2vObj instanceof JSONArray ) return (JSONArray)v2vObj;
        String v2vStr = "" + v2vObj;
        JSONArray json = null;
        try {
            json = new JSONArray( v2vStr );
        } catch ( JSONException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return json;
    }

    public Collection<EmsScriptNode> getContainedViews( boolean recurse,
                                                        Date dateTime,
                                                        Set<EmsScriptNode> seen ) {
        if ( getElement() == null ) return null;
        if ( seen == null ) seen = new HashSet<EmsScriptNode>();
        if ( seen.contains( getElement() ) ) return Utils.getEmptyList();
        seen.add( getElement() );
        LinkedHashSet<EmsScriptNode> views = new LinkedHashSet<EmsScriptNode>();
        views.addAll(getViewToViewPropertyViews(dateTime));
        views.addAll(getChildViewElements(dateTime));
        views.remove( getElement() );
        if ( recurse ) {
            for ( EmsScriptNode e :  views ) {
                View v = new View(e);
                views.addAll( v.getContainedViews( recurse, dateTime, seen ) );
            }
        }
        return views;
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
        return toBoringString();
    }

    @Override
    public int compareTo( View o ) {
        return compare( this, o );
    }

    @Override
    public int compare( View o1, View o2 ) {
        if ( o1 == o2 ) return 0;
        if ( o1 == null ) return -1;
        if ( o2 == null ) return 1;
        return o1.getElement().compareTo( o2.getElement() );
    }

}
