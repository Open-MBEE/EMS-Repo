package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;

import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;

import sysml.view.Viewable;

/**
 * A {@link Viewable} reference to an attribute of an Element.
 */
public class ElementReference implements Viewable<EmsScriptNode> {

    public enum Attribute { NAME, VALUE, TYPE, DOCUMENTATION };
    // REVIEW -- consider other things from SystemModel.ModelItem
    // We might have used ModelItem in place of Attribute, but it doesn't
    // include DOCUMENTATION or something like that.
    
	protected EmsScriptNode element = null;
	protected Attribute attribute = null; 
	
	public ElementReference(EmsScriptNode element, Attribute attribute) {
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
	public JSONObject toViewJson() {
		
		if (element == null) return null;
		
        JSONObject json = new JSONObject();

        try {
            json.put("type", "Paragraph");
            json.put("sourceType", "reference");
            json.put("source", element.getName()); // this is the sysml id = the alfresco name
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
        return "" + toViewJson();
    }

}
