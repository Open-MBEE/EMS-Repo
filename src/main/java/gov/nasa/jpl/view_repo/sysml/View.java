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
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

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
     * This is a cache of the displayed elements to keep from having to
     * recompute them as is done for allowed elements.
     */
    protected Collection<EmsScriptNode> displayedElements = null;

    protected boolean generate = true;

    protected boolean recurse = false;



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
     * @return the generate
     */
    public boolean isGenerate() {
        return generate;
    }

    /**
     * @param generate the generate to set
     */
    public void setGenerate( boolean generate ) {
        this.generate = generate;
    }

    /**
     * @return the recurse
     */
    public boolean isRecurse() {
        return recurse;
    }

    /**
     * @param recurse the recurse to set
     */
    public void setRecurse( boolean recurse ) {
        this.recurse = recurse;
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
        for ( EmsScriptNode node : getChildViewElements( getWorkspace(), null ) ) {
            childViews.add( new View( node ) );
        }
        return childViews;
    }

    // TODO -- need to support a flag for recursion
    public Collection< EmsScriptNode >
            getChildViewElements( WorkspaceNode workspace, Date dateTime ) {
        //        ArrayList< EmsScriptNode > childViews =
//                new ArrayList< EmsScriptNode >();
        Object o = viewNode.getProperty( Acm.ACM_CHILDREN_VIEWS );
        Collection< EmsScriptNode > childViews =
                getElementsForJson( o, workspace, dateTime );

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
    public EmsScriptNode getViewpoint(Date dateTime, WorkspaceNode ws) {
        if ( viewNode == null ) return null;
        EmsScriptNode viewpoint = null;

        Set< EmsScriptNode > conformElements =
                this.getElement().getRelationshipsOfType( Acm.JSON_CONFORM, dateTime, ws );
        if ( Utils.isNullOrEmpty( conformElements ) ) {
            return null;
        }

        // Get all elements of Conform type:
        //Collection<EmsScriptNode> conformElements = getModel().getType(null, Acm.JSON_CONFORM);
        if (Debug.isOn()) System.out.println( "Got "
                            + ( conformElements == null ? 0
                                                        : conformElements.size() )
                            + " elements of type " + Acm.JSON_CONFORM );
        for ( EmsScriptNode node : conformElements ) {

            // If the sysml:source of the Compose element is the View:
//            EmsScriptNode source = getSource(node);
//            if ( source != null && source.equals( viewNode ) ) {

                // Get the target of the Conform relationship (the Viewpoint):
                Collection<EmsScriptNode> viewpointNodes = getModel().getTarget(node);

                if (!Utils.isNullOrEmpty(viewpointNodes)) {
                    viewpoint = viewpointNodes.iterator().next();
                }
                break;
//            }
        }

        return viewpoint;
    }

    public Collection< EmsScriptNode > getExposedElements(Date dateTime, WorkspaceNode ws) {
        if ( viewNode == null ) return null;

        Set< EmsScriptNode > exposeElements =
                this.getElement().getRelationshipsOfType( Acm.JSON_EXPOSE, dateTime, ws );
        if ( Utils.isNullOrEmpty( exposeElements ) ) {
            return null;
        }
        Collection<EmsScriptNode> exposed = new ArrayList<EmsScriptNode>();

//        // Get all relationship elements of Expose type:
//        Collection<EmsScriptNode> exposeElements = getModel().getType(null, Acm.JSON_EXPOSE);
//        //Collection<EmsScriptNode> exposeElements = getModel().getRelationship(null, "Expose");  // Can we call this?

        if (Debug.isOn()) Debug.outln( "Expose relationships of " + viewNode + ": "
                     + exposeElements );

        // Check if any of the nodes in the passed collection of Expose or Conform
        // elements have the View as a sysml:source:
        for ( EmsScriptNode node : exposeElements ) {

//            // If the sysml:source of the Expose element is the View, then
//            // add it to our expose list (there can be multiple exposes for
//            // a view):
//            EmsScriptNode source = getSource(node);
//            if (source != null && source.equals( viewNode ) ) {

                // Get the target(s) of the Expose relationship:

                Collection<EmsScriptNode> nodes = getModel().getTarget(node);

                if (!Utils.isNullOrEmpty(nodes)) {
                    exposed.addAll(nodes);
                }
//            }
        }

        return exposed;
    }


    public EmsScriptNode getViewpointOperation(Date dateTime, WorkspaceNode ws) {
        EmsScriptNode viewpoint = getViewpoint(dateTime, ws);
        if ( viewpoint == null ) return null;
        //if ( viewpoint == null || !viewpoint.exists() ) return null;

        // Get the Method property from the ViewPoint element:
        Collection< EmsScriptNode > viewpointMethods =
                getModel().getProperty( viewpoint, "method" );

        EmsScriptNode viewpointMethod = null;

        if ( viewpointMethods.size() > 0 ) {
            viewpointMethod = viewpointMethods.iterator().next();
        }

        //if ( viewpointMethod == null ) viewpointMethod = viewpoint;  // HACK -- TODO

        return viewpointMethod;
    }

    
    /**
     * Get the last modified time of the element based on whether the view
     * contains json is generated. If generated, return the current time since
     * we don't determine when the generated json would have changed, so we
     * can't assume that it hasn't changed.
     * 
     * @param dateTime a timepoint indicating a version of the element
     * @return
     */
    public Date getLastModified( Date dateTime, WorkspaceNode ws ) {
        if ( getElement() == null ) return null;
        if ( !isGeneratedFromViewpoint(dateTime, ws) ) {
//            System.out.println("####  ####  Not auto-generated view " + getElement()  );
            Date date = getElement().getLastModified( dateTime );
            //return new Date(); 
            return date;
        }
//        System.out.println("####  ####  Auto-generated view " + getElement()  );
        return new Date(); 
    }

//    /**
//     * Get the last modified time of self, displayed elements, and contained
//     * views, recursively. <br>
//     * NOTE: This is not being called and is not tested.
//     * 
//     * @param dateTime
//     * @return
//     */
//    public Date getLastModifiedRecursive( Date dateTime ) {
//        if ( getElement() == null ) return null;
//        Date date = getElement().getLastModified( dateTime );
//        if ( !isGeneratedFromViewpoint() ) {
//            return date;
//        }
//        //Set<EmsScriptNode> elems = new LinkedHashSet< EmsScriptNode >();
//        Collection<EmsScriptNode> elems =
//                getDisplayedElements( getWorkspace(), dateTime, true, false, null );
//        for ( EmsScriptNode elem : elems ) {
//            Date d = elem.getLastModified( dateTime );
//            if ( d.after( date ) ) date = d;
//        }
//        elems = getContainedViews( false, getWorkspace(), dateTime, null );
//        for ( EmsScriptNode elem : elems ) {
//            View v = new View( elem );
//            Date d = v.getLastModifiedRecursive( dateTime );
//            if ( d.after( date ) ) date = d;
//        }
//        return date;
//    }

    /**
     * @return whether the contains json is generated from a viewpoint instead
     *         of stored statically.
     */
    public boolean isGeneratedFromViewpoint(Date dateTime, WorkspaceNode ws) {
        if ( getElement() == null ) {
//            System.out.println("####  ####  view has no element "  );
            return false;
        }
        String property = (String) viewNode.getProperty("view2:contains");
        if ( property != null && property.length() > 0 ) {
//            System.out.println("####  ####  has contains " + getElement()  );
            return false;
        }
        EmsScriptNode viewpointOp = getViewpointOperation(dateTime, ws);
        if ( viewpointOp == null ) {
//            System.out.println("####  ####  no viewpoint " + getElement()  );
            return false;
        }
//        System.out.println("####  ####  got viewpoint " + getElement()  );
        return true;
    }
    
    public boolean generateViewables(Date dateTime, WorkspaceNode ws) {
        // Get the related elements that define the the view.

        EmsScriptNode viewpointOp = getViewpointOperation(dateTime, ws);
        if ( viewpointOp == null ) {
            if (Debug.isOn()) System.out.println("*** View.toViewJson(): no viewpoint operation! View = " + toBoringString() );
            return false;
        }

        Collection<EmsScriptNode> exposed = getExposedElements(dateTime, ws);

        // Translate the viewpoint Operation/Expression element into an AE Expression:
        ArrayList<Object> paramValList = new ArrayList<Object>();
        // This is a List of a collection of nodes, where the value of exposed
        // parameter is a collection of nodes:
        paramValList.add( exposed );
        SystemModelToAeExpression< EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel > sysmlToAeExpr =
                new SystemModelToAeExpression< EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel >( getModel() );
        Expression< Object > aeExpr = sysmlToAeExpr.operationToAeExpression(viewpointOp, paramValList);

        if ( aeExpr == null ) return false;

        // Evaluate the expression to get the Viewables and add them to this View.

        clear(); // make sure we clear out any old information
        Object evalResult = aeExpr.evaluate( true );
        if ( evalResult instanceof Viewable ) {
            Viewable< EmsScriptNode > v = (Viewable< EmsScriptNode >)evalResult;
            add( v );
        } else if ( evalResult instanceof Collection ) {
            Collection< ? > c = (Collection< ? >)evalResult;
            for ( Object o : c ) {
                if (!(o instanceof Viewable) ) {
                    o = Expression.evaluate( o, Viewable.class, true );
                }
                try {
                    Viewable< EmsScriptNode > viewable =
                            (Viewable< EmsScriptNode >)o;
                    add( viewable );
                } catch ( ClassCastException e ) {
                    System.out.println( "Failed to cast to Viewable: " + o);
                    e.printStackTrace();
                }
            }
//            java.util.List< Viewable<EmsScriptNode> > viewables =
//                    (java.util.List< Viewable<EmsScriptNode> >)Utils.asList( c );
//            addAll( viewables );
        }
        return !isEmpty();
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
    public JSONObject toViewJson(Date dateTime) {
        return toViewJson( getWorkspace(), generate, recurse, dateTime );
    }

    public WorkspaceNode getWorkspace() {
        if ( getElement() != null && getElement().exists() ) {
            return getElement().getWorkspace();
        }
        return null;
    }

    public JSONObject toViewJson( WorkspaceNode workspace,
                                  boolean doGenerate, boolean doRecurse,
                                  Date dateTime) {

        if ( viewNode == null ) {
            if (Debug.isOn()) System.out.println("*** called View.toViewJson() without a view node! View = " + toBoringString() );
            return null;
        }

        // Get the related elements that define the the view.
        if ( doGenerate ) {
            generateViewables(dateTime, workspace);
        }

        // Generate the JSON for the view now that the View is populated with
        // Viewables.

        JSONObject json = new JSONObject();
        JSONObject viewProperties = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try {
            json.put("views", jsonArray);
            jsonArray.put( viewProperties );
            viewProperties.put("id", viewNode.getSysmlId() );

            JSONArray elements = new JSONArray();
            viewProperties.put("displayedElements", elements );
            for ( EmsScriptNode elem : getDisplayedElements( workspace, null, doGenerate, doRecurse, null ) ) {
                elements.put( elem.getSysmlId() );
            }

            elements = new JSONArray();
            viewProperties.put("allowedElements", elements );
            for ( EmsScriptNode elem : getAllowedElements() ) {
                elements.put( elem.getSysmlId() );
            }

            elements = new JSONArray();
            viewProperties.put("childrenViews", elements );
            for ( sysml.view.View<EmsScriptNode> view : getChildViews() ) {
                if ( view instanceof View ) {
                    elements.put( view.getElement().getSysmlId() );
                }
            }

            JSONArray viewables = getContainsJson( doGenerate, dateTime, workspace );
            viewProperties.put("contains", viewables );

        } catch ( JSONException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return json;
    }

    public JSONArray getContainsJson(Date dateTime, WorkspaceNode ws ) {
        return getContainsJson( generate, dateTime, ws );
    }
    public JSONArray getContainsJson( boolean doGenerate, Date dateTime, WorkspaceNode ws ) {
        JSONArray viewablesJson = new JSONArray();

        if ( doGenerate && isEmpty() ) generateViewables(dateTime, ws);

        for ( Viewable< EmsScriptNode > viewable : this ) {
            if ( viewable != null ) {
                viewablesJson.put( viewable.toViewJson(dateTime) );
            }
        }
        if ( viewablesJson.length() == 0 && getElement() != null ) {
            Object contains = getElement().getProperty( Acm.ACM_CONTAINS );
            if ( contains != null ) {
                if ( contains instanceof JSONArray ) return (JSONArray)contains;

                String containsString = "" + contains;
                if ( containsString.length() > 1 ) {
                    try {
                        JSONArray jarr = new JSONArray( "" + contains );
                        viewablesJson = jarr;
                    } catch ( JSONException e ) {
                        System.out.println( "Tried to parse \"" + contains
                                            + "\" in element "
                                            + getElement().getSysmlName() + "("
                                            + getElement().getSysmlId() + ")" );
                        e.printStackTrace();
                    }
                }
            }
        }
        return viewablesJson;
    }

    public Collection<EmsScriptNode> getAllowedElements() {
        if ( displayedElements != null ) {
            return displayedElements;
        }
        return getDisplayedElements();
    }

    @Override
    // TODO -- need to support a flag for recursion?
    public Collection<EmsScriptNode> getDisplayedElements() {
        return getDisplayedElements( null, null, generate, recurse, null );
    }

    // TODO -- need to support a flag for recursion?
    public Collection< EmsScriptNode >
            getDisplayedElements( WorkspaceNode workspace, Date dateTime,
                                  boolean doGenerate, boolean doRecurse,
                                  Set< View > seen ) {
        if ( doRecurse ) {
            if ( seen == null ) seen = new HashSet<View>();
            if ( seen.contains( this ) ) return Utils.getEmptySet();
            seen.add( this );
        }

        LinkedHashSet<EmsScriptNode> set = new LinkedHashSet<EmsScriptNode>();

        // Generate Viewables from Viewpoint method, but don't overwrite cached
        // values.
        if ( doGenerate && isEmpty() ) {
            generateViewables(dateTime, workspace);
        } else if ( displayedElements != null ) {
            // Return cached value which won't change since it's from hardcoded JSON.
            return displayedElements;
        }

        // get contained Viewables
        // REVIEW -- should this only be done when generate == true?
        Collection< EmsScriptNode > versionedElements = super.getDisplayedElements();
        versionedElements = NodeUtil.getVersionAtTime( versionedElements, dateTime );
        set.addAll( versionedElements );

        // get elements specified in JSON in the displayed elements property
        Object dElems = getElement().getProperty( Acm.ACM_DISPLAYED_ELEMENTS );
        set.addAll( getElementsForJson( dElems, workspace, dateTime ) );

        if ( doRecurse ) {

            // get recursively from child views
            for ( sysml.view.View< EmsScriptNode > v : getChildViews() ) {
                EmsScriptNode n = NodeUtil.getVersionAtTime( v.getElement(), dateTime );
                if ( n == null ) continue;
                v = new View( n );
                if ( v instanceof View ) {
                    set.addAll( ((View)v).getDisplayedElements( workspace,
                                                                dateTime,
                                                                doGenerate,
                                                                doRecurse,
                                                                seen ) );
                } else {
                    // REVIEW -- this case seems impossible to reach since v is assigned a View
                    Collection<EmsScriptNode> moreElements = v.getDisplayedElements();
                    Collection<EmsScriptNode> vElems = NodeUtil.getVersionAtTime( moreElements, dateTime );
                    set.addAll( vElems );
                }
            }

            // get recursively from viewToView property
            Collection< EmsScriptNode > v2vs =
                    getViewToViewPropertyViews( workspace, dateTime );
            for ( EmsScriptNode node : v2vs ) {
                if ( node.isView() ) {
                    View v = new View( node );
                    set.addAll( v.getDisplayedElements( workspace, dateTime,
                                                        doGenerate, doRecurse,
                                                        seen ) );
                }
            }
        }

        displayedElements = set;
        return set;
    }

    public Collection< EmsScriptNode >
            getElementsForJson( Object obj, WorkspaceNode workspace,
                                Date dateTime ) {
        if ( obj == null ) return Utils.getEmptyList();
        LinkedHashSet<EmsScriptNode> nodes = new LinkedHashSet<EmsScriptNode>();
        if ( obj instanceof JSONArray ) {
            return getElementsForJson( (JSONArray)obj, workspace, dateTime );
        }
        if ( obj instanceof JSONObject ) {
            return getElementsForJson( (JSONObject)obj, workspace, dateTime );
        }
        String idString = "" + obj;
        if ( idString.length() <= 0 ) return Utils.getEmptyList();
        try {
            if ( idString.trim().charAt( 0 ) == '[' ) {
                JSONArray jArray = new JSONArray( idString );
                return getElementsForJson( jArray, workspace, dateTime );
            }
            if ( idString.trim().charAt( 0 ) == '{' ) {
                JSONObject jsonObject =  new JSONObject( idString );
                return getElementsForJson( jsonObject, workspace, dateTime );
            }
        } catch ( JSONException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        EmsScriptNode node =
                    EmsScriptNode.convertIdToEmsScriptNode( idString,
                                                            false,
                                                            workspace,
                                                            dateTime,
                                                            getElement().getServices(),
                                                            getElement().getResponse(),
                                                            getElement().getStatus() );
        if ( node != null ) nodes.add( node );
        return nodes;
    }

    public Collection< EmsScriptNode >
            getElementsForJson( JSONArray jsonArray, WorkspaceNode workspace,
                                Date dateTime ) {
        if ( jsonArray == null ) return Utils.getEmptyList();
        LinkedHashSet<EmsScriptNode> nodes = new LinkedHashSet<EmsScriptNode>();
        for ( int i = 0; i < jsonArray.length(); ++i ) {
            Object id = null;
            try {
                id = jsonArray.get(i);
                nodes.addAll(getElementsForJson( id, workspace, dateTime ) );
            } catch ( JSONException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return nodes;
    }

    public Collection< EmsScriptNode >
            getElementsForJson( JSONObject o, WorkspaceNode workspace,
                                Date dateTime ) {
        if ( o == null ) return Utils.getEmptyList();
        LinkedHashSet<EmsScriptNode> nodes = new LinkedHashSet<EmsScriptNode>();
        String[] names = JSONObject.getNames( o );
        if ( names == null ) return nodes;
        for ( String name : names ) {
            try {
                Object ids = o.get( name );
                nodes.addAll( getElementsForJson( ids, workspace, dateTime ) );
            } catch ( JSONException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return nodes;
    }

    public Collection< EmsScriptNode >
            getViewToViewPropertyViews( WorkspaceNode workspace, Date dateTime ) {
        JSONArray jarr = getViewToViewPropertyJson();
        Collection< EmsScriptNode > coll =
                getElementsForJson( jarr, workspace, dateTime );
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
                                                        WorkspaceNode workspace,
                                                        Date dateTime,
                                                        Set<EmsScriptNode> seen ) {
        if ( getElement() == null ) return null;
        if ( seen == null ) seen = new HashSet<EmsScriptNode>();
        if ( seen.contains( getElement() ) ) return Utils.getEmptyList();
        seen.add( getElement() );
        LinkedHashSet<EmsScriptNode> views = new LinkedHashSet<EmsScriptNode>();
        views.addAll(getViewToViewPropertyViews(workspace, dateTime));
        views.addAll(getChildViewElements(workspace, dateTime));
        views.remove( getElement() );
        ArrayList<EmsScriptNode> viewsCopy = new ArrayList<EmsScriptNode>(views);
        if ( recurse ) {
            for ( EmsScriptNode e :  viewsCopy ) {
                View v = new View(e);
                views.addAll( v.getContainedViews( recurse, workspace,
                                                   dateTime, seen ) );
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
