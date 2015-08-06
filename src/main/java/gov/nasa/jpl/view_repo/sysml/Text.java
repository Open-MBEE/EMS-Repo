/**
 * 
 */
package gov.nasa.jpl.view_repo.sysml;

import java.util.Collection;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import sysml.view.Viewable;

/**
 * Simple text for a paragraph.
 */
public class Text implements Viewable< EmsScriptNode > {

    protected String text;
    
    public Text( String text ) {
        this.text = text;
    }

//    public Text( Object o ) {
//        this.text = "" + o;
//    }
    
    /* (non-Javadoc)
     * @see sysml.view.Viewable#toViewJson(java.util.Date)
     */
    @Override
    public JSONObject toViewJson( Date dateTime ) {
        JSONObject json = new JSONObject();

        try {
            json.put("type", "Paragraph");
            json.put("sourceType", "text");
            json.put("text", text);

        } catch ( JSONException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return json;
    }

    /* (non-Javadoc)
     * @see sysml.view.Viewable#getDisplayedElements()
     */
    @Override
    public Collection< EmsScriptNode > getDisplayedElements() {
        return Utils.getEmptyList();
    }

}
