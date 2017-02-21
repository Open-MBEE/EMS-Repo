/**
 *
 */
package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.ae.event.Expression;
import gov.nasa.jpl.ae.sysml.SystemModelToAeExpression;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.MoreToString;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ModelLoadActionExecuter;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsSystemModel;
import gov.nasa.jpl.view_repo.util.ModelContext;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.ServiceContext;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.UpdateViewHierarchy;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
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

    static Logger logger = Logger.getLogger( View.class );

    protected EmsScriptNode viewNode = null;

    protected EmsSystemModel model = null;

    /**
     * This is a cache of the displayed elements to keep from having to
     * recompute them as is done for allowed elements.
     */
    protected Collection<EmsScriptNode> displayedElements = null;

    protected boolean generate = true;  // turning this off ensures no docgen views are generated

    protected boolean recurse = false;

    public boolean generateViewsOnViewGet = false;

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
        for ( EmsScriptNode node : getChildrenViewElements( getWorkspace(), null ) ) {
            childViews.add( new View( node ) );
        }
        return childViews;
    }

    // TODO -- need to support a flag for recursion
    public Collection< EmsScriptNode >
            getChildrenViewElements( WorkspaceNode workspace, Date dateTime ) {
        Object o = viewNode.getProperty( Acm.ACM_CHILDREN_VIEWS );
        Collection< EmsScriptNode > childViews =
                getElementsForJson( o, workspace, dateTime );

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
        if (Debug.isOn()) Debug.outln( "Got "
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
        LinkedHashSet<EmsScriptNode> exposed = new LinkedHashSet<EmsScriptNode>();

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
        if (!generate) {
            return false;
        }
        // Get the related elements that define the the view.

        EmsScriptNode viewpointOp = getViewpointOperation(dateTime, ws);
        if ( viewpointOp == null ) {
            if (Debug.isOn()) Debug.outln("*** View.toViewJson(): no viewpoint operation! View = " + toBoringString() );
            return false;
        }

        Collection<EmsScriptNode> exposed = getExposedElements(dateTime, ws);

        // Translate the viewpoint Operation/Expression element into an AE Expression:
        Vector<Object> paramValList = new Vector<Object>();
        // This is a List of a collection of nodes, where the value of exposed
        // parameter is a collection of nodes:
        paramValList.add( exposed );
        SystemModelToAeExpression< Object, EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel > sysmlToAeExpr =
                new SystemModelToAeExpression< Object, EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel >( getModel() );
        Expression< Object > aeExpr = sysmlToAeExpr.operationToAeExpression(viewpointOp, paramValList);

        if ( aeExpr == null ) return false;

        // Evaluate the expression to get the Viewables and add them to this View.

        clear(); // make sure we clear out any old information
        Object evalResult;
        try {
            if (Debug.isOn()) Debug.outln("aeExpr = " + aeExpr);
            evalResult = aeExpr.evaluate( true );
            if ( evalResult instanceof Viewable ) {
                Viewable< EmsScriptNode > v = (Viewable< EmsScriptNode >)evalResult;
                add( v );
            } else if ( evalResult instanceof Collection ) {
                Collection< ? > c = (Collection< ? >)evalResult;
                for ( Object o : c ) {
                    if (!(o instanceof Viewable) ) {
                        o = Expression.evaluate( o, Viewable.class, true );
                    }
                    if ( o instanceof Viewable ) {
                        try {
                            Viewable< EmsScriptNode > viewable =
                                    (Viewable< EmsScriptNode >)o;
                            add( viewable );
                        } catch ( ClassCastException e ) {
                            logger.error( "Failed to cast to Viewable: " + o );
                            e.printStackTrace();
                        }
                    }
                }
//                java.util.List< Viewable<EmsScriptNode> > viewables =
//                        (java.util.List< Viewable<EmsScriptNode> >)Utils.asList( c );
//                addAll( viewables );
            }

        // TODO -- figure out why eclipse gives compile errors for
        // including the exceptions while mvn gives errors for not
        // including them.
        } catch ( IllegalAccessException e1 ) {
            // TODO Auto-generated catch block
            //e1.printStackTrace();
        } catch ( InvocationTargetException e1 ) {
            // TODO Auto-generated catch block
            //e1.printStackTrace();
        } catch ( InstantiationException e1 ) {
            // TODO Auto-generated catch block
            //e1.printStackTrace();
        }
        //System.out.println("!!!!!!!!!   done  !!!!!!!!!!");
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
            if (Debug.isOn()) Debug.outln("*** called View.toViewJson() without a view node! View = " + toBoringString() );
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
    
    public NodeRef getContentsNode(Date dateTime, WorkspaceNode ws) {
        return getContentsNode( generate, dateTime, ws );
    }
    
    public NodeRef getContentsNode(boolean generate, Date dateTime, WorkspaceNode ws) {
        // 
        if ( generateViewsOnViewGet  ) {
            // Don't want to change the past.  Check to see if the element has changed since dateTime.
            Date lastModified = getElement().getLastModified( null );
            if ( generate && (dateTime == null || !lastModified.after( dateTime ) ) ) {
                updateViewInstanceSpecification( generate, dateTime, ws );
            }
        }
        NodeRef contentsNode = (NodeRef) getElement().getNodeRefProperty( Acm.ACM_CONTENTS,
                                                                          dateTime, ws);
        return contentsNode;
    }
    
    /**
     * The structure of contents is an Expression with a list of InstanceValues
     * as operands. The "instance" of the InstanceValues is the id of an
     * InstanceSpecification. The "instanceSpecificationSpecification" of the
     * InstanceSpecification is a ValueSpecification.
     * <p>
     * Get the current list of instance values. Create json to create or remove
     * instanceValues to match the number of top-level viewables. Then create
     * the rest of the json and post the json.
     * 
     * @param ws
     */
    public void updateViewInstanceSpecification( boolean doGenerate,
                                                 Date dateTime, WorkspaceNode ws ) {
        if ( getElement() == null ) return;
        String id = getElement().getSysmlId();

        JSONObject json = new JSONObject();
        JSONArray elements = new JSONArray();
        json.put( "elements", elements );
        
        boolean generated = false;
        if ( doGenerate && isEmpty() ) {
            generated = generateViewables(dateTime, ws);
        }
        if (!generated && isEmpty()) return;

        JSONArray viewablesJson = new JSONArray();
        for ( Viewable< EmsScriptNode > viewable : this ) {
            if ( viewable != null ) {
                viewablesJson.put( viewable.toViewJson(dateTime) );
            }
        }

        // Do nothing if there are no viewables. This assumes that the
        // existing contents are consistent.
        if ( viewablesJson.length() == 0 ) {
            return;
        }
        NodeRef contentsRef = 
                (NodeRef) getElement().getNodeRefProperty( Acm.ACM_CONTENTS, null, ws);
        EmsScriptNode contentsNode = null;
        if ( contentsRef != null ) {
            contentsNode = new EmsScriptNode( contentsRef, getElement().getServices() );
        }
        JSONObject contentsExpression =
                updateViewableInstanceJson( id, contentsNode, viewablesJson, elements, ws );
        if ( contentsExpression != null ) {
            JSONObject viewJson = new JSONObject();
            if ( !Utils.isNullOrEmpty( id ) ) {
                viewJson.put("sysmlid", id);
            }
            JSONObject specialization = new JSONObject();
            viewJson.put( "specialization", specialization );
            specialization.put( "contents", contentsExpression );
            elements.put( viewJson );
        }

        if ( logger.isDebugEnabled() ) logger.debug( "(((((((((((((((((((((((CONTENT JSON)))))))))))))))))))))))\n" + json.toString( 4 ) );

        if ( elements.length() > 0 ) {
            // update MMS with json
            ModelContext modelContext = new ModelContext( false, ws, false, null,
                                                          null, null, null, null );
            ServiceContext serviceContext =
                    new ServiceContext( false, false, 0, false, false, null,
                                        getElement().getServices(),
                                        getElement().getResponse(),
                                        getElement().getStatus() );
            ModelLoadActionExecuter.loadJson( json, modelContext, serviceContext );
        }
    }

    /**
     * Create json for the "contents" Expression of a View or the section
     * "instanceSpecificationSpecification" Expression of an
     * InstanceSpecification.
     * 
     * @param id
     *            sysmlid of the View or InstanceSpecification
     * @param expressionNode
     *            the db node of the "contents" or
     *            "instanceSpecificationSpecification" Expression
     * @param viewablesJson
     *            the "contains" json for each presentation element in the
     *            "contents" or "instanceSpecificationSpecification"
     * @param elements
     *            the JSONArray of JSONObjects for the newly generated json
     * @param ws
     *            workspace of the View or InstanceSpecification
     */
    public JSONObject updateViewableInstanceJson( String id,
                                                  EmsScriptNode expressionNode,
                                                  JSONArray viewablesJson,
                                                  JSONArray elements,
                                                  WorkspaceNode ws ) {
        
        // Create json to overwrite the InstanceValue operands of the contents expression

        // First check to see what is already in the database
        java.util.List< String > instanceSpecIds = new ArrayList<String>();
        if ( expressionNode != null ) {
            // Get the operands
            Collection<NodeRef> operands = null;
            Object opsObj = expressionNode.getNodeRefProperty( Acm.ACM_OPERAND, null, ws );
            if ( opsObj instanceof Collection ) {
                operands = Utils.asList((Collection<?>)opsObj, NodeRef.class);
                instanceSpecIds = NodeUtil.getSysmlIdsAsList( operands );
            }
        }
        
        int numPreexistingInstanceSpecIds = instanceSpecIds.size();
        
        JSONObject contentsExpression = null;
        
        // Create ids for any additional viewables/InstanceSpecifications.
        if ( numPreexistingInstanceSpecIds < size() ) {
            for ( int i = 0; i < size() - numPreexistingInstanceSpecIds; ++i ) {
                // Can't number instance values--if some are deleted or
                // reordered, we don't want to preserve the number ordering,
                // so why have it to begin with?
                //String numWithLeadingZeroes = String.format( "%04d", i+numPreexistingInstanceSpecIds );
                //String newId = id + "_" + numWithLeadingZeroes + "_pei";
                String newId = NodeUtil.createId( getElement().getServices() ) + "_pei";
                instanceSpecIds.add( newId );
            }
        }
        // Truncate extra ids
        if ( instanceSpecIds.size() > size() ) {
            for ( int i = instanceSpecIds.size()-1; i > size()-1; --i ) {
                instanceSpecIds.remove( i );
            }
        }

        // Create the json for the updated contents Expression.
        contentsExpression = makeExpressionJsonForInstanceValues(instanceSpecIds);
            
        if (contentsExpression != null && contentsExpression.length() > 0) {
            updateInstanceSpecifications(numPreexistingInstanceSpecIds,
                                         instanceSpecIds, viewablesJson,
                                         elements, ws);
        }
        return contentsExpression;
    }
    
    public void updateInstanceSpecifications(int numPreexistingInstanceSpecIds,
                                             java.util.List< String > instanceSpecIds,
                                             JSONArray viewablesJson, 
                                             JSONArray elements,
                                             WorkspaceNode ws) {
        // Create or update the json for the InstanceSpecifications.
        String ownerId = null;
        int vct = 0;
        for ( int i = 0; i < instanceSpecIds.size() && i < size(); ++i ) {
            Viewable<EmsScriptNode> viewable = this.get( i );
            if ( viewable == null ) continue;
            JSONObject viewableContainsJson = viewablesJson.optJSONObject( vct++ );
            if ( viewableContainsJson == null ) {
                logger.error( "The \"contains\" json is null for presentation element, " + viewable
                              + "; The order of presentation elements may be incorrect!" );
                continue;
            }
            JSONObject instanceSpec = new JSONObject();
            String instanceSpecId = instanceSpecIds.get( i );
            instanceSpec.put( "sysmlid", instanceSpecId );
            JSONObject specialization = new JSONObject();
            instanceSpec.put( "specialization", specialization );
            specialization.put("type", "InstanceSpecification");
            JSONObject instanceSpecSpec = viewableJsonToValueSpecJson(instanceSpecId, viewable, viewableContainsJson, elements, ws);
            if ( instanceSpecSpec != null ) {
                specialization.put( Acm.JSON_INSTANCE_SPECIFICATION_SPECIFICATION, instanceSpecSpec );
                
                // Add the instance specification metatype id.
                JSONArray specAppliedMetatypes = new JSONArray();
                specAppliedMetatypes.put("_9_0_62a020a_1105704885251_933969_7897");
                specialization.put("appliedMetatypes", specAppliedMetatypes);
                
                // add owner for new instanceSpecs
                if ( i >= numPreexistingInstanceSpecIds ) {
                    if ( ownerId == null ) {
                        // view instance specs are stored in holding_bin_<projectId>
                        ownerId = "holding_bin_" + getElement().getProjectId( ws );
                    }
                    instanceSpec.put( "owner", ownerId );
                }
                
                // add classifier
                String classifier = classifierForViewable( viewable );
                if ( !Utils.isNullOrEmpty( classifier ) ) {
                    JSONArray jarr = new JSONArray();
                    jarr.put( classifier );
                    instanceSpec.put( "classifier", jarr );
                }
                elements.put( instanceSpec );
            }
        }
    }

    protected JSONObject makeViewContentsJson( String id,
                                               java.util.List< String > instanceSpecIds ) {
        // Create the json for the updated contents Expression.
        JSONObject contentsExpression = makeExpressionJsonForInstanceValues(instanceSpecIds);
        
        JSONObject viewJson = new JSONObject();
        if ( !Utils.isNullOrEmpty( id ) ) {
            viewJson.put("sysmlid", id);
        }
        JSONObject specialization = new JSONObject();
        viewJson.put( "specialization", specialization );
        specialization.put( "contents", contentsExpression );
        return viewJson;
    }

    protected JSONObject makeExpressionJsonForInstanceValues( java.util.List< String > instanceSpecIds ) {
        JSONObject contentsExpression = new JSONObject();
        contentsExpression.put( "type", "Expression" );
        JSONArray operandJsonArray = new JSONArray();
        for ( int i = 0; i < instanceSpecIds.size() && i < size(); ++i ) {
            JSONObject instanceValue = new JSONObject();
            instanceValue.put( "type", "InstanceValue" );
            instanceValue.put( "instance", instanceSpecIds.get( i ) );
            operandJsonArray.put( instanceValue );
        }
        contentsExpression.put( "operand", operandJsonArray );
        return contentsExpression;
    }

    public static String classifierForViewable( Viewable< EmsScriptNode > viewable ) {
        if ( viewable == null ) return null;
        return classifierForViewableClass( viewable.getClass() );
    }

    public static String classifierForViewableClass( Class<? extends Viewable> cls ) {
        if ( Section.class.isAssignableFrom( cls ) ) {
            return "_17_0_5_1_407019f_1430628211976_255218_12002";
        } else if ( Table.class.isAssignableFrom( cls ) ) {
            return "_17_0_5_1_407019f_1430628178633_708586_11903";
        } else if ( List.class.isAssignableFrom( cls ) ) {
            return "_17_0_5_1_407019f_1430628190151_363897_11927";
        } else if ( Image.class.isAssignableFrom( cls ) ) {
            return "_17_0_5_1_407019f_1430628206190_469511_11978";
        } else if ( Documentation.class.isAssignableFrom( cls ) ) {
            return "_17_0_5_1_407019f_1431903758416_800749_12055";            
        } else if ( Id.class.isAssignableFrom( cls ) ) {
            return "_17_0_5_1_407019f_1431903758416_800749_12055";            
        } else if ( Value.class.isAssignableFrom( cls ) ) {
            return "_17_0_5_1_407019f_1431903758416_800749_12055"; // ???
        } else if ( Type.class.isAssignableFrom( cls ) ) {
            return "_17_0_5_1_407019f_1431903758416_800749_12055";            
        } else if ( Name.class.isAssignableFrom( cls ) ) {
            return "_17_0_5_1_407019f_1431903758416_800749_12055";            
        } else if ( Text.class.isAssignableFrom( cls ) ) {
            return "_17_0_5_1_407019f_1431903758416_800749_12055";            
        } else if ( Evaluate.class.isAssignableFrom( cls ) ) {
            return "_17_0_5_1_407019f_1431903758416_800749_12055";
        }
        return null;
    }

    public JSONObject viewableJsonToValueSpecJson( String instanceSpecId,
                                                   Viewable<EmsScriptNode> v,
                                                   JSONObject viewableContainsJson,
                                                   JSONArray elements,
                                                   WorkspaceNode ws ) {
        if (v == null) return null;
        if ( !( v instanceof Section ) ) {
            JSONObject literalString = new JSONObject();
            literalString.put( "type", "LiteralString" );
            literalString.put( "string", viewableContainsJson.toString() );
            return literalString;
            // return viewableContainsJson;
            // return v.toViewJson();
        }
        
        // v is a Section -- need to create an Expression whose operands are
        // InstanceValues, like the "contents" of a View.
        
        // Get section's viewables.
        JSONArray viewablesJson = viewableContainsJson.optJSONArray( "list" );
        
        // Get the "instanceSpecificationSpecification" of the InstanceSpecification.
        EmsScriptNode instanceSpecSpecNode =
                getInstanceSpecificationSpecification( instanceSpecId, ws );
        
        //makeExpressionJsonForInstanceValues( instanceSpecIds );
        
        JSONObject sectionJson =
                updateViewableInstanceJson( instanceSpecId, instanceSpecSpecNode,
                                            viewablesJson, elements, ws );
        return sectionJson;
    }
    
    public EmsScriptNode getInstanceSpecificationSpecification( String instanceSpecId, WorkspaceNode ws ) {
        NodeRef instanceSpecNodeRef =
                NodeUtil.findNodeRefById( instanceSpecId, false, ws, null,
                                          getElement().getServices(), false );
        EmsScriptNode instanceSpecNode = null;
        EmsScriptNode instanceSpecSpecNode = null;
        if ( instanceSpecNodeRef != null ) {
            instanceSpecNode = 
                    new EmsScriptNode(instanceSpecNodeRef, getElement().getServices());
            Object instanceSpecSpecRefObj =
                    instanceSpecNode.getNodeRefProperty( Acm.ACM_INSTANCE_SPECIFICATION_SPECIFICATION, null, ws );
            if ( instanceSpecSpecRefObj != null && instanceSpecSpecRefObj instanceof NodeRef ) {
                instanceSpecSpecNode = new EmsScriptNode( (NodeRef)instanceSpecSpecRefObj,
                                                          getElement().getServices() );
            }
        }
        return instanceSpecSpecNode;
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
                        if ( Debug.isOn() ) Debug.outln( "Tried to parse \"" + contains
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
            getDisplayedElements( final WorkspaceNode workspace, final Date dateTime,
                                  boolean doGenerate, boolean doRecurse,
                                  Set< View > seen ) {
        if ( doRecurse ) {
            if ( seen == null ) seen = new HashSet<View>();
            if ( seen.contains( this ) ) return Utils.getEmptySet();
            seen.add( this );
        }

        final LinkedHashSet<EmsScriptNode> set = new LinkedHashSet<EmsScriptNode>();

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
        final Object dElems = getElement().getProperty( Acm.ACM_DISPLAYED_ELEMENTS );
        
        new EmsTransaction(getElement().getServices(), getElement().getResponse(),
                           getElement().getStatus(), false, true) {
            
            @Override
            public void run() throws Exception {

                set.addAll( getElementsForJson( dElems, workspace, dateTime ) );
            }
        };
        
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
            getElementsForJson( Object obj, final WorkspaceNode workspace,
                                final Date dateTime ) {
        if ( obj == null ) return Utils.getEmptyList();
        final LinkedHashSet<EmsScriptNode> nodes = new LinkedHashSet<EmsScriptNode>();
        if ( obj instanceof JSONArray ) {
            return getElementsForJson( (JSONArray)obj, workspace, dateTime );
        }
        if ( obj instanceof JSONObject ) {
            return getElementsForJson( (JSONObject)obj, workspace, dateTime );
        }
        final String idString = "" + obj;
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

        new EmsTransaction(getElement().getServices(), getElement().getResponse(),
                           getElement().getStatus(), false, true) {
            @Override
            public void run() throws Exception {
                EmsScriptNode node =
                        EmsScriptNode.convertIdToEmsScriptNode( idString,
                                                                false,
                                                                workspace,
                                                                dateTime,
                                                                getElement().getServices(),
                                                                getElement().getResponse(),
                                                                getElement().getStatus() );
                if ( node != null ) nodes.add( node );
            }
        };

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

    public Collection<EmsScriptNode> getContainedViews( final boolean recurse,
                                                        final WorkspaceNode workspace,
                                                        final Date dateTime,
                                                        Set<EmsScriptNode> seen ) {
        if ( getElement() == null ) return null;
        if ( seen == null ) seen = new HashSet<EmsScriptNode>();
        if ( seen.contains( getElement() ) ) return Utils.getEmptyList();
        seen.add( getElement() );
        final LinkedHashSet<EmsScriptNode> views = new LinkedHashSet<EmsScriptNode>();
        Collection< EmsScriptNode > v2vPropertyViews = getViewToViewPropertyViews(workspace, dateTime);
        views.addAll(v2vPropertyViews);
        if ( v2vPropertyViews == null || v2vPropertyViews.size() == 0 ) {
            Set< EmsScriptNode > childViews = UpdateViewHierarchy.getChildViews(this.getElement(), workspace, dateTime, true);
            views.addAll( childViews );
            if (childViews == null || childViews.size() == 0) {
                views.addAll(getChildrenViewElements(workspace, dateTime));
            }
        }
        views.remove( getElement() );
        ArrayList<EmsScriptNode> viewsCopy = new ArrayList<EmsScriptNode>(views);
        final Set<EmsScriptNode> seenT = seen;
        if ( recurse ) {
            for ( final EmsScriptNode e :  viewsCopy ) {
                
                new EmsTransaction(getElement().getServices(), getElement().getResponse(),
                                   getElement().getStatus(), false, true) {
                    
                    @Override
                    public void run() throws Exception {
                        View v = new View(e);
                        views.addAll( v.getContainedViews( recurse, workspace,
                                                           dateTime, seenT ) );
                    }
                };

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
