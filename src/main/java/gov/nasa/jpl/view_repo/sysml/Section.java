package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;

public class Section extends gov.nasa.jpl.view_repo.sysml.List implements sysml.view.Section< EmsScriptNode > {

    private static final long serialVersionUID = 1646140772754541334L;

    protected String title = null;
    
    public Section( String title ) {
        setTitle( title );
    }
    
    @Override
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle( String title ) {
        this.title = title;
    }

}
