/**
 * 
 */
package gov.nasa.jpl.view_repo.sysml;

import java.util.Collection;
import java.util.Date;

import org.json.JSONObject;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import sysml.view.Viewable;

/**
 * An image viewable in a View.
 */
public class Image implements sysml.view.Image< EmsScriptNode > {
    String imageId = null;
    String title = null;

    
    
    /**
     * @param imageId
     * @param title
     */
    public Image( String imageId, String title ) {
        this.imageId = imageId;
        this.title = title;
    }

    /* (non-Javadoc)
     * @see sysml.view.Viewable#toViewJson(java.util.Date)
     */
    @Override
    public JSONObject toViewJson( Date dateTime ) {
        JSONObject entry = new JSONObject();
        entry.put("type", "Image");
        entry.put("sysmlid", imageId);
        entry.put("title", title);
        return entry;
    }

    /* (non-Javadoc)
     * @see sysml.view.Viewable#getDisplayedElements()
     */
    @Override
    public Collection< EmsScriptNode > getDisplayedElements() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getTitle() {
        return title;
    }

}
