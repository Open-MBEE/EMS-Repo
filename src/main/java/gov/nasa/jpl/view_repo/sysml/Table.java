/**
 * 
 */
package gov.nasa.jpl.view_repo.sysml;

//import java.util.List;

import sysml.view.List;
import sysml.view.Viewable;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;


/**
 *
 */
public class Table extends gov.nasa.jpl.view_repo.sysml.List implements sysml.view.Table<EmsScriptNode> {//, java.util.List<List> {

    private static final long serialVersionUID = 5980032062088714766L;

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

    @Override
    public sysml.view.List< EmsScriptNode > getColumn( int i ) {
        Viewable< EmsScriptNode > column = get( i );
        if ( column instanceof sysml.view.List ) {
            return (List< EmsScriptNode >)column;
        }
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Viewable< EmsScriptNode > getCell( int i, int j ) {
        // TODO Auto-generated method stub
        return null;
    }

}
