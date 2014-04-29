package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import java.util.Collection;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * An implementation of a {@link Name}
 * 
 * @see sysml.Name
 * 
 */
public class Name implements sysml.Name<EmsScriptNode> {

	private EmsScriptNode reference = null;
	
	public Name(EmsScriptNode ref) {
		setReference(ref);
	}
	
	public EmsScriptNode getReference() {
		return reference;
	}

	public void setReference(EmsScriptNode reference) {
		this.reference = reference;
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
		
		if (reference == null) return null;
		
        JSONObject json = new JSONObject();

        try {
        	
            json.put("sourceType", "reference");
            json.put("source", reference.getName());
            json.put("sourceProperty", "name");

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
		return Utils.asList(reference, EmsScriptNode.class);
	}
	
    @Override
    public String toString() {
        return toViewJson().toString();
    }

}
