package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.ae.event.Expression;
import gov.nasa.jpl.mbee.util.ClassUtils;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Seen;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;

import java.util.Collection;
import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONException;
import org.json.JSONObject;

import sysml.view.Viewable;

/**
 * A {@link Viewable} reference to an attribute of an Element.
 */
public class ElementReference implements Viewable<EmsScriptNode> {

    public enum Attribute { NAME, ID, VALUE, TYPE, DOCUMENTATION };//, WORKSPACE, VERSION, MODIFIED, CREATED, CREATOR, MODIFIER };
    // REVIEW -- consider other things from SystemModel.ModelItem
    // We might have used ModelItem in place of Attribute, but it doesn't
    // include DOCUMENTATION or something like that.
    
	protected EmsScriptNode element = null;
    protected Object object = null; // in case we don't get an element
	protected Attribute attribute = null; 
	
	public ElementReference(EmsScriptNode element, Attribute attribute) {
	    init( element, attribute );
	}
	
	public ElementReference( String id, Attribute attribute ) {
	    init( id, attribute );
    }
	
    public ElementReference( Object object, Attribute attribute ) {
        init( object, attribute );
    }
    
    protected void init( Object object, Attribute attribute ) {
        //System.out.println("############  init( Object " + object + ",  " + attribute + " )  ###############");
        if ( object instanceof EmsScriptNode ) {
            init( (EmsScriptNode)object, attribute );
        } else if ( object instanceof String ) {
            init( (String)object, attribute );
        } else {
            if ( object instanceof Expression ) {
                object = ((Expression<?>)object).expression;
            }
            this.object = object;
            
            this.attribute = attribute;
        }
    }

    protected void init( String id, Attribute attribute) {
        //System.out.println("############  init( String " + id + ",  " + attribute + " )  ###############");
        NodeRef ref =
                NodeUtil.findNodeRefById( id, false, null, null,
                                          NodeUtil.getServices(), false );
        EmsScriptNode node = null;
        if ( ref != null ) {
            node = new EmsScriptNode( ref, NodeUtil.getServices() );
        }
        init( node, attribute );
    }
    
    protected void init( EmsScriptNode element, Attribute attribute) {
        init( element, attribute, null );
    }

    /**
     * Same as {@link #init(EmsScriptNode, Attribute)} but protects against
     * cycles in ElementValue references.
     * 
     * @param element
     * @param attribute
     * @param seen
     */
    protected void init( EmsScriptNode element, Attribute attribute,
                         Seen< EmsScriptNode > seen ) {
        
	    //System.out.println("##### DUDE #######  init( EmsScriptNode " + element + ",  " + attribute + " )  ###############");
	    Pair< Boolean, Seen< EmsScriptNode > > p = Utils.seen( element, true, seen );
	    if ( element.getTypeName().equals("ElementValue") && !p.first ) {
	        seen = p.second;
	        NodeRef ref = (NodeRef)element.getNodeRefProperty( Acm.ACM_ELEMENT_VALUE_ELEMENT, null, element.getWorkspace() );
	        if ( NodeUtil.exists( ref ) ) {
	            EmsScriptNode newElement = new EmsScriptNode( ref, element.getServices() );
	            init( newElement, attribute, seen );
	            return;
	        }
	    }
        setElement(element);
        setAttribute(attribute);
    }

    /**
     * @return the element
     */
    public EmsScriptNode getElement() {
        return element;
    }

    /**
     * @param element the element to set
     */
    public void setElement( EmsScriptNode element ) {
        this.element = element;
    }

    /**
     * @return the attribute
     */
    public Attribute getAttribute() {
        return attribute;
    }

    /**
     * @param attribute the attribute to set
     */
    public void setAttribute( Attribute attribute ) {
        this.attribute = attribute;
    }

	/*
	 * <code>
	 *  "sourceType": "reference" 
     *  "source": element id,
     *  "sourceProperty": "name"
     * </code>
     * @returns a JSON object in the format above         
	 * @see sysml.Viewable#toViewJson()
	 */
	@Override
	public JSONObject toViewJson(Date dateTime) {
		
		if (element == null) {
	        if (object == null) {
	            return null;
	        }
	        String text = null;
	        switch ( attribute ) {
	            case NAME:
	                text = "" + ClassUtils.getName( object );
	                break;
	            case VALUE:
                    text = "" + ClassUtils.getValue( object );
                    break;
	            case TYPE:
                    text = "" + ClassUtils.getType( object );
                    break;
                case ID:
                    text = "" + ClassUtils.getId( object );
                    break;
	            case DOCUMENTATION:
	                // TODO
                    text = "" + object;
                    break;
	            default:
	                // TODO -- error!
	        }
	        return (new Text( text )).toViewJson( null );
		}
		
        JSONObject json = new JSONObject();

        try {
            json.put("type", "Paragraph");
            json.put("sourceType", "reference");
            json.put("source", element.getSysmlId()); 
            json.put("sourceProperty", attribute.toString().toLowerCase() );

        } catch ( JSONException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return json;
	}

	/*
	 * (non-Javadoc)
	 * @see sysml.Viewable#getDisplayedElements()
	 */
	@Override
	public Collection<EmsScriptNode> getDisplayedElements() {
		return Utils.asList(element, EmsScriptNode.class);
	}
	
    @Override
    public String toString() {
        return "" + toViewJson(null);
    }

}
