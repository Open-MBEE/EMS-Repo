/**
 * 
 */
package gov.nasa.jpl.view_repo.sysml;

//import java.util.List;

import java.util.Collection;
import java.util.Date;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import sysml.view.List;
import sysml.view.Viewable;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.webscripts.MmsVersion;


/**
 *
 */
public class Table extends gov.nasa.jpl.view_repo.sysml.List implements sysml.view.Table<EmsScriptNode> {//, java.util.List<List> {

    private static Logger logger = Logger.getLogger(MmsVersion.class);
    protected boolean prettyPrint = true;
    private static final long serialVersionUID = 5980032062088714766L;
    
    protected String title = "";
    protected String style = null;
    protected String caption;
    //protected List<List<DocumentElement>> headers;
    protected List< DBColSpec > colspecs;
    protected int cols;
    protected boolean transpose;
    
    public class DBColSpec {
        public int colnum;
        public String colname;
        public String colwidth;
    }
    
    @Override
    public sysml.view.List< EmsScriptNode > getColumns() {
        return this;
//        sysml.view.List< EmsScriptNode > columnList = this;
//        if ( true ) return columnList;
//        //if ( true ) return (java.util.List< sysml.view.List< EmsScriptNode > >)columnList;
//        ArrayList< sysml.view.List< EmsScriptNode > > columns = new ArrayList< sysml.view.List< EmsScriptNode > >();
//        for ( Viewable< EmsScriptNode > col : columnList ) {
//            if ( col instanceof sysml.view.List ) {
//                sysml.view.List< EmsScriptNode > svCol = (sysml.view.List< EmsScriptNode >)col;
//                columns.add( svCol );
//            }
//        }
//        return columns;
//        //return null;//Utils.asList( columnList, sysml.view.List.class );
    }

    public Table() {
        super();
        // TODO Auto-generated constructor stub
    }

    public Table( sysml.view.List< EmsScriptNode > c ) {
        super(c);
        if(logger.isDebugEnabled()) logger.warn("Table(List), yo!");
    }

    public Table( Collection< ? > c ) {
        super( c );
        // TODO Auto-generated constructor stub
    }

    public Table(Object... c) {
        super(c);
    }

    @Override
    public sysml.view.List< EmsScriptNode > getColumn( int i ) {
        if ( i < 0 || i > size() ) {
            // TODO -- ERROR
            return null;
        }
        Viewable< EmsScriptNode > column = get( i );
        if ( column instanceof sysml.view.List ) {
            return (List< EmsScriptNode >)column;
        }
        // TODO -- ERROR
        return null;
    }

    @Override
    public Viewable< EmsScriptNode > getCell( int i, int j ) {
        List< EmsScriptNode > column = getColumn(i);
        if ( column == null ) {
            // TODO -- ERROR
            return null;
        }
        return column.get( j );
    }

    public String getTitle() {
        return title;
    }

    public void setTitle( String title ) {
        this.title = title;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle( String style ) {
        this.style = style;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption( String caption ) {
        this.caption = caption;
    }

    public List< DBColSpec > getColspecs() {
        return colspecs;
    }

    public void setColspecs( List< DBColSpec > colspecs ) {
        this.colspecs = colspecs;
    }

    public int getCols() {
        return cols;
    }

    public void setCols( int cols ) {
        this.cols = cols;
    }

    public boolean isTranspose() {
        return transpose;
    }

    public void setTranspose( boolean transpose ) {
        this.transpose = transpose;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    @Override
    public JSONObject toViewJson( Date dateTime ) {
        JSONObject json = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        try {
            if (getStyle() == null)
                json.put("style", "normal");
            else
                json.put("style", getStyle());
            json.put("title", getTitle());
            json.put("type", "Table");
            //json.put("type", "Table");


            JSONArray body = new JSONArray();
            json.put("body", body);
            
            JSONArray headers = new JSONArray();
            json.put("header", headers);

            boolean first = true;
            
            // Loop through columns
            for ( Viewable<EmsScriptNode> viewable : this ) {
//                if ( viewable != null ) {
//                    jsonArray.put( viewable.toViewJson(dateTime) );
//                }
                
                JSONArray arr = first ? headers : body;
                first = false;
                
                if ( viewable instanceof List ) {
                    JSONArray curRow = new JSONArray();
                    arr.put( curRow );
                    List<?> vList = (List< ? >)viewable;
                    for ( Object o : vList ) {
                        JSONArray curCell = new JSONArray();
                        if ( o instanceof Viewable ) {
                            curCell.put( ( (Viewable)o ).toViewJson( dateTime ) );
                        } else {
                            Evaluate e = new Evaluate( o );
                            curCell.put( e.toViewJson( dateTime ) );
                        }
                        JSONObject entry = new JSONObject();
                        entry.put("content", curCell);
                        entry.put( "colspan", 1 );
                        entry.put( "rowspan", 1 );
                        curRow.put(entry);
                    }
                } else {
                    // ????? TODO  ??????
                }

            }
            
            json.put("ordered", ordered);


        } catch ( JSONException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return json;
    }

    @Override
    public Collection< EmsScriptNode > getDisplayedElements() {
        // List's implementation should work here, too.
        return super.getDisplayedElements();
    }

    @Override
    public String toString() {
        // List's implementation should work here, too.
        return super.toString();
    }
    
    

}
