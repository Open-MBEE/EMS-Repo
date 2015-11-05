package gov.nasa.jpl.view_repo.db;

import gov.nasa.jpl.mbee.util.Pair;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class PostgresHelper {
    static Logger logger = Logger.getLogger( PostgresHelper.class );
    
    private Connection conn;
    private String host;
    private String dbName;
    private String user;
    private String pass;
    private String workspaceName;

    public static enum DbEdgeTypes {
        REGULAR( 1 ), DOCUMENT( 2 );

        private final int id;

        DbEdgeTypes( int id ) {
            this.id = id;
        }

        public int getValue() {
            return id;
        }
    }

    public PostgresHelper( String workspaceName ) {
        this.host = DbContract.HOST;
        this.dbName = DbContract.DB_NAME;
        this.user = DbContract.USERNAME;
        this.pass = DbContract.PASSWORD;
        this.workspaceName = workspaceName.split( "_" )[ 0 ].replace( "-", "_" );
    }

    public void close() throws SQLException {
        conn.close();
    }

    public boolean connect() throws SQLException, ClassNotFoundException {
        if ( host.isEmpty() || dbName.isEmpty() || user.isEmpty()
             || pass.isEmpty() ) {
            throw new SQLException( "Database credentials missing" );
        }

        Class.forName( "org.postgresql.Driver" );
        this.conn =
                DriverManager.getConnection( this.host + this.dbName,
                                             this.user, this.pass );
        return true;
    }

    public void execUpdate( String query ) throws SQLException {
        System.out.println( "Query: " + query );
        this.conn.createStatement().executeUpdate( query );
    }

    public ResultSet execQuery( String query ) throws SQLException {
        System.out.println( "Query: " + query );
        return this.conn.createStatement().executeQuery( query );
    }

    public
            int
            insert( String table, Map< String, String > values )
                                                                throws SQLException {

        StringBuilder columns = new StringBuilder();
        StringBuilder vals = new StringBuilder();

        try {
            for ( String col : values.keySet() ) {
                columns.append( col ).append( "," );

                if ( values.get( col ) instanceof String ) {
                    vals.append( "'" ).append( values.get( col ) )
                        .append( "'," );
                } else vals.append( values.get( col ) ).append( "," );
            }

            columns.setLength( columns.length() - 1 );
            vals.setLength( vals.length() - 1 );

            String query =
                    String.format( "INSERT INTO %s (%s) VALUES (%s)", table,
                                   columns.toString(), vals.toString() );

            return this.conn.createStatement().executeUpdate( query );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return -1;
    }

    public List< EdgeTypes > getEdgeTypes() {
        List< EdgeTypes > result = new ArrayList< EdgeTypes >();
        try {
            ResultSet rs = execQuery( "SELECT * FROM edgeTypes" );

            while ( rs.next() ) {
                result.add( new EdgeTypes( rs.getInt( 1 ), rs.getString( 2 ) ) );
            }

        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return result;
    }

    public Node getNodeFromNodeRefId( String nodeRefId ) {
        try {
            ResultSet rs =
                    execQuery( "SELECT * FROM nodes" + workspaceName
                               + " where nodeRefId = '" + nodeRefId + "'" );

            if ( rs.next() ) {
                return new Node( rs.getInt( 1 ), rs.getString( 2 ),
                                 rs.getString( 3 ), rs.getInt( 4 ),
                                 rs.getString( 5 ) );
            } else return null;
        } catch ( SQLException e ) {
            e.printStackTrace();
        }
        return null;
    }

    public Node getNode( int id ) {
        try {
            ResultSet rs =
                    execQuery( "SELECT * FROM nodes" + workspaceName
                               + " where id = " + id );
            if ( rs.next() ) {
                return new Node( rs.getInt( 1 ), rs.getString( 2 ),
                                 rs.getString( 3 ), rs.getInt( 4 ),
                                 rs.getString( 5 ) );
            } else return null;
        } catch ( SQLException e ) {
            e.printStackTrace();
        }
        return null;
    }

    public Node getNodeFromSysmlId( String sysmlId ) {
        try {
            ResultSet rs =
                    execQuery( "SELECT * FROM nodes" + workspaceName
                               + " where sysmlid = '" + sysmlId + "'" );

            if ( rs.next() ) {
                return new Node( rs.getInt( 1 ), rs.getString( 2 ),
                                 rs.getString( 3 ), rs.getInt( 4 ),
                                 rs.getString( 5 ) );
            } else return null;
        } catch ( SQLException e ) {
            e.printStackTrace();
        }
        return null;
    }

    public void insertNode( String nodeRefId, String versionedRefId,
                            String sysmlId ) {
        try {
            Node n = getNodeFromNodeRefId( nodeRefId );
            if ( n != null ) return;

            Map< String, String > map = new HashMap< String, String >();
            map.put( "nodeRefId", nodeRefId );
            map.put( "versionedRefId", versionedRefId );
            map.put( "sysmlId", sysmlId );
            map.put( "nodeType", "1" );
            insert( "nodes" + workspaceName, map );
        } catch ( SQLException e ) {
            e.printStackTrace();
        }
    }

    public void updateNodeRefIds( String sysmlId, String versionedRefId,
                                  String nodeRefId ) {
        if ( sysmlId == null || versionedRefId == null ) return;
        try {
            execUpdate( "update nodes" + workspaceName + " set nodeRefId = '"
                        + nodeRefId + "'," + " versionedRefId = '"
                        + versionedRefId + "' where sysmlid='" + sysmlId + "'" );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public void deleteNode( String sysmlId ) {
        try {
            execUpdate( "delete from nodes" + workspaceName
                        + " where sysmlid = '" + sysmlId + "'" );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public void deleteNodeBySysmlId( String sysmlId ) {
        try {
            execUpdate( "delete from nodes" + workspaceName
                        + " where sysmlId = " + sysmlId );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public void insertEdge( String parentSysmlId, String childSysmlId,
                            DbEdgeTypes edgeType ) {

        if ( parentSysmlId.isEmpty() || childSysmlId.isEmpty() ) return;
        try {
            execUpdate( "insert into edges" + workspaceName
                        + " values((select id from nodes" + workspaceName
                        + " where sysmlId = '" + parentSysmlId + "'),"
                        + "(select id from nodes" + workspaceName
                        + " where sysmlId = '" + childSysmlId + "'), "
                        + edgeType.getValue() + ")" );
        } catch ( Exception e ) {
            if (e.getMessage().contains("duplicate key")) {
                if (logger.isInfoEnabled()) e.printStackTrace();              
            } else {
                e.printStackTrace();
            }
        }

    }

    public Set< String > getRootParents( String sysmlId, DbEdgeTypes et ) {
        Set< String > result = new HashSet< String >();
        try {
            Node n = getNodeFromSysmlId( sysmlId );

            if ( n == null ) return result;

            String query =
                    "select * from get_root_parents(%s, %d, '%s')";
            ResultSet rs =
                    execQuery( String.format( query, n.getId(),
                                              et.getValue(), workspaceName ) );

            while ( rs.next() ) {
                result.add( rs.getString( 1 ) );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return result;
    }

    
    public Set<String> getImmediateParents(String sysmlId, DbEdgeTypes et, int height){
        Set<String> result = new HashSet<String>();
        try {
            Node n = getNodeFromSysmlId( sysmlId );

            if ( n == null ) return result;

            String query = "select * from get_immediate_parents(%s, %d, '%s')";
            ResultSet rs =
                    execQuery( String.format( query, workspaceName, n.getId(),
                                              et.getValue(), workspaceName ) );

            while ( rs.next() ) {
                result.add(rs.getString( 1 ));
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return result;
    }
    
    public Set<String> getRootParents(String sysmlId, DbEdgeTypes et, int height){
        Set<String> result = new HashSet<String>();
        try {
            Node n = getNodeFromSysmlId( sysmlId );

            if ( n == null ) return result;

            String query = "select * from get_root_parents(%s, %d, '%s')";
            ResultSet rs =
                    execQuery( String.format( query, workspaceName, n.getId(),
                                              et.getValue(), workspaceName ) );

            while ( rs.next() ) {
                result.add(rs.getString( 1 ));
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return result;
    }
    
    public Set< String >
            getParents( String sysmlId, DbEdgeTypes et, int height ) {
        Set< String > result = new HashSet< String >();
        try {
            Node n = getNodeFromSysmlId( sysmlId );

            if ( n == null ) return result;

            String query =
                    "select sysmlid from nodes%s where id in "
                            + "(select id from get_parents(%s, %d, '%s'))";
            ResultSet rs =
                    execQuery( String.format( query, workspaceName, n.getId(),
                                              et.getValue(), workspaceName ) );

            while ( rs.next() ) {
                result.add(rs.getString( 1 ));
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return result;
    }

    // returns list of nodeRefIds
    public List< Pair< String, String >>
            getChildren( String sysmlId, DbEdgeTypes et, int depth ) {
        List< Pair< String, String >> result =
                new ArrayList< Pair< String, String >>();
        try {
            Node n = getNodeFromSysmlId( sysmlId );

            if ( n == null ) return result;

            ResultSet rs =
                    execQuery( "select nodeRefId,versionedRefId from nodes"
                               + workspaceName
                               + " where id in (select * from get_children("
                               + n.getId() + ", " + et.getValue() + ", '"
                               + workspaceName + "', " + depth + "))" );

            while ( rs.next() ) {
                result.add( new Pair< String, String >( rs.getString( 1 ),
                                                        rs.getString( 2 ) ) );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return result;
    }

    public List< Pair< String, String >>
            getImmediateChildren( String sysmlId, DbEdgeTypes edgeType ) {
        List< Pair< String, String >> result =
                new ArrayList< Pair< String, String >>();
        try {
            Node n = getNodeFromSysmlId( sysmlId );

            if ( n == null ) return result;

            ResultSet rs =
                    execQuery( "select noderefid,versionedrefid from nodes"
                               + workspaceName
                               + " where id in (select child from edges where parent = "
                               + n.getId() + " and edgeType = "
                               + edgeType.getValue() + ")" );

            while ( rs.next() ) {
                result.add( new Pair< String, String >( rs.getString( 1 ),
                                                        rs.getString( 2 ) ) );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return result;
    }

    public List< Node > getChildren( String nodeRefId, int edgeType ) {
        List< Node > result = new ArrayList< Node >();
        try {
            Node n = getNodeFromNodeRefId( nodeRefId );
            if ( n == null ) return result;

            ResultSet rs =
                    execQuery( "select * from get_children(" + n.getId() + ", "
                               + edgeType + ", " + workspaceName + ")" );

            while ( rs.next() ) {
                result.add( getNode( rs.getInt( 1 ) ) );
            }

        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return result;
    }

    public void deleteEdgesForNode( String sysmlId ) {
        try {
            Node n = getNodeFromSysmlId( sysmlId );

            if ( n == null ) return;

            execUpdate( "delete from edges" + workspaceName
                        + " where parent = " + n.getId() + " or child = "
                        + n.getId() );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public void deleteEdgesForChildNode( String sysmlId, DbEdgeTypes edgeType ) {
        try {
            Node n = getNodeFromSysmlId( sysmlId );

            if ( n == null ) return;

            execUpdate( "delete from edges" + workspaceName + " where child = "
                        + n.getId() + " and edgeType = " + edgeType.getValue() );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public void deleteEdges( String parentSysmlId, String childSysmlId ) {
        try {
            Node pn = getNodeFromSysmlId( parentSysmlId );
            Node cn = getNodeFromSysmlId( childSysmlId );

            if ( pn == null || cn == null ) return;

            execUpdate( "delete from edges where parent = " + pn.getId()
                        + " and child = " + cn.getId() );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public void deleteEdges( String parentSysmlId, String childSysmlId,
                             int edgeType ) {
        try {
            Node pn = getNodeFromSysmlId( parentSysmlId );
            Node cn = getNodeFromSysmlId( childSysmlId );

            if ( pn == null || cn == null ) return;

            execUpdate( "delete from edges" + workspaceName
                        + " where parent = " + pn.getId() + " and child = "
                        + cn.getId() + " and edgeType = " + edgeType );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public void createBranchFromWorkspace( String childWorkspaceName ) {
        try {

            execUpdate( "create table nodes"
                        + childWorkspaceName.replace( "-", "_" )
                        + " (id bigserial primary key, noderefid text not null unique, versionedrefid text not null, "
                        + "nodetype integer references nodetypes(id) not null, sysmlid text not null unique)" );
            execUpdate( "insert into nodes"
                        + childWorkspaceName.replace( "-", "_" )
                        + "(id, nodetype, noderefid, versionedrefid, sysmlid) select id, nodetype,noderefid,versionedrefid,sysmlid from nodes"
                        + workspaceName );
            execQuery( "select setval('nodes"
                       + childWorkspaceName.replace( "-", "_" )
                       + "_id_seq', coalesce((select max(id)+1 from nodes"
                       + childWorkspaceName.replace( "-", "_" )
                       + "), 1), false)" );

            execUpdate( "create table edges"
                        + childWorkspaceName.replace( "-", "_" )
                        + " as select * from edges" + workspaceName );
        } catch ( SQLException e ) {
            e.printStackTrace();
        }
    }

    public List< String > filterNodesByWorkspace( List< String > noderefs,
                                                  String workspace ) {
        List< String > result = new ArrayList< String >();
        String query =
                "select noderefid from nodes" + workspace
                        + " where noderefid in (";
        for ( int i = 0; i < noderefs.size(); i++ ) {
            noderefs.set( i, "'" + noderefs.get( i ) + "'" );
        }
        query += StringUtils.join( noderefs, "," );
        query += ");";

        ResultSet rs;
        try {
            rs = execQuery( query );

            while ( rs.next() ) {
                result.add( rs.getString( 1 ) );
            }
        } catch ( SQLException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return result;
    }
}
