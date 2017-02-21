package gov.nasa.jpl.view_repo.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import gov.nasa.jpl.mbee.util.CompareUtils;
import gov.nasa.jpl.mbee.util.FileUtils;
import gov.nasa.jpl.mbee.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class JsonDiff {

    
    private static String toString( Object o, String firstKey, int indent, int indentIncrement,
                                    boolean indentFirst ) throws JSONException {
        if ( o instanceof List ) return toString((List<?>)o, firstKey, indent, indentIncrement, indentFirst);
        if ( o instanceof Map ) return toString((Map<?,?>)o, firstKey, indent, indentIncrement, indentFirst);
        StringBuffer sb = new StringBuffer();
        if ( indentFirst ) sb.append( Utils.repeat( " ", indent ) ) ;
        if ( o instanceof String ) {
            sb.append( "\"" + o + "\"" );
        } else {
            sb.append( "" + o );
        }
        return sb.toString();
    }
    
    private static String toString( List< ? > list, String firstKey, int indent, int indentIncrement,
                                    boolean indentFirst ) throws JSONException {
        StringBuffer sb = new StringBuffer();
        if ( indentFirst ) sb.append( Utils.repeat( " ", indent ) );
        sb.append( "[" );
        boolean first = true;
        for ( Object o : list ) {
            if ( first ) first = false;
            else sb.append( "," );
            sb.append( "\n" + toString(o, firstKey, indent+indentIncrement, indentIncrement, true) );
        }
        sb.append( "\n" + Utils.repeat( " ", indent ) + "]" );
        return sb.toString();
    }
    
    private static String toString( Map< ?, ? > map, String firstKey, int indent, int indentIncrement,
                                    boolean indentFirst ) throws JSONException {
        StringBuffer sb = new StringBuffer();
        if ( indentFirst ) sb.append( Utils.repeat( " ", indent ) );
        sb.append( "{" );
        boolean first = true;
        if ( firstKey != null ) {
            first = false;
            sb.append( "\n" + toString( firstKey, null, indent + indentIncrement, indentIncrement, true ) + ": "
                    + toString( map.get( firstKey ), firstKey, indent + indentIncrement, indentIncrement, false ) );
        }
        for ( Entry< ?, ? > e : map.entrySet() ) {
            if ( firstKey != null && e.getKey().equals(firstKey) ) continue;
            if ( first ) first = false;
            else sb.append( "," );
            sb.append( "\n" + toString( e.getKey(), firstKey, indent + indentIncrement, indentIncrement, true ) + ": "
                       + toString( e.getValue(), firstKey, indent + indentIncrement, indentIncrement, false ) );
        }
        sb.append( "\n" + Utils.repeat( " ", indent ) + "}" );
        return sb.toString();
    }
    
    private static Object sort( Object o, final String firstKey ) throws JSONException {
        if ( o instanceof JSONArray ) return sortList((JSONArray)o, firstKey);
        if ( o instanceof JSONObject ) return sortMap((JSONObject)o, firstKey);
        return o;
    }
    
    private static JSONArray sort( JSONArray o, final String firstKey ) throws JSONException {
        ArrayList<?> list = sortList( o, firstKey );
        return new JSONArray( list );
    }
    private static class DumbComparator<T> implements Comparator<T> {
        public static DumbComparator instance = new DumbComparator();
        @Override
        public int compare( T o1, T o2 ) {
            return CompareUtils.compare( o1, o2, false, false );
        }
    }
    private static ArrayList<?> sortList( JSONArray o, final String firstKey ) throws JSONException {
        ArrayList<Object> list = new ArrayList<Object>( o.length() );
        for ( int i = 0; i < o.length(); ++i ) {
            Object v = o.get( i );
            v = sort( v, firstKey );
            list.add( v );
        }
        Collections.sort( list, DumbComparator.instance );
        return list;
    }

    private static JSONObject sort( JSONObject o, final String firstKey ) {
        Map<?,?> map = sortMap(o, null);
        return new JSONObject(map);
    }
    private static Map<?,?> sortMap( JSONObject o, final String firstKey ) {
        TreeMap<String, Object> map = new TreeMap<String, Object>(new Comparator< String >() {
            @Override
            public int compare( String o1, String o2 ) {
                 if ( o1 == o2 ) return 0;
                 if ( o1 == null ) return -1;
                 if ( o2 == null ) return 1;
                 if ( firstKey != null ) {
                     if ( o1.equals( o2 ) ) return 0;
                     if ( o1.equals( firstKey ) ) return -1;
                     if ( o2.equals( firstKey ) ) return 1;
                 }
                 return o1.compareTo( o2 );
            }
        });
        Iterator<?> i = o.keys();
        while ( i.hasNext() ) {
            Object k = i.next();
            if ( k instanceof String ) {
                try {
                    Object v = o.get( (String)k );
                    v = sort( v, firstKey );
                    map.put( (String)k, v );
                } catch ( JSONException e ) {
                    e.printStackTrace();
                }
            }
        }
        return map;
    }
    
    
    public static void runCommand(String cmd) throws IOException {
        final Process proc = Runtime.getRuntime().exec( cmd );
        if ( proc == null ) return;
        Thread stdoutThread = new Thread( new Runnable() {
            public BufferedReader reader = null;

            @Override
            public void run() {
                InputStream is = proc.getInputStream();
                reader = new java.io.BufferedReader(new InputStreamReader(is));
                // And print each line
                String s = null;
                try {
                  while ((s = reader.readLine()) != null) {
                      System.out.println(s);
                  }
                  reader.close();
                } catch ( IOException e ) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
            }
        });
        Thread stderrThread = new Thread( new Runnable() {
            public BufferedReader reader = null;

            @Override
            public void run() {
                InputStream is = proc.getErrorStream();
                reader = new BufferedReader(new InputStreamReader(is));
                // And print each line
                String s = null;
                try {
                  while ((s = reader.readLine()) != null) {
                      System.out.println(s);
                  }
                  reader.close();
                } catch ( IOException e ) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
            }
        });
        stdoutThread.start();
        stderrThread.start();
    }
    
    public static void main( String[] args ) {
        JSONObject o1=null, o2=null;
        String usage = "Usage: JSONDiff file1.json file2.json";
        if ( args.length < 2 ) {
            System.err.println(usage);
            return;
        }
        File file1;
        File file2;
        String jstr1 = null;
        String jstr2 = null;
        try {
            file1 = new File(args[0]);
            file2 = new File(args[1]);
            jstr1 = FileUtils.fileToString( file1 );
            jstr2 = FileUtils.fileToString( file2 );
            try {
                o1 = new JSONObject( jstr1 );
            } catch ( JSONException e ) {}
            try {
                o2 = new JSONObject( jstr2 );
            } catch ( JSONException e ) {}
        } catch ( Throwable e ) {
            //System.err.println("Error! " + e.getLocalizedMessage() );
            e.printStackTrace();
            return;
        }
        try {
            String s = null;
            if ( o1 == null ) {
                s = jstr1;
            } else {
                Map<?,?> so1 = sortMap( o1, "sysmlid" );
                s = toString( so1, "sysmlid", 0, 4, false );
            }
            String parent1 = file1.getParent();
            if ( parent1 == null ) parent1 = ".";
            String fName1 = parent1 + File.separator + "sorted_"
                    + file1.getName();
            FileUtils.stringToFile( s, fName1 );
            
            if ( o2 == null) {
                s = jstr2;
            } else {
                Map<?,?> so2 = sortMap( o2, "sysmlid" );
                s = toString( so2, "sysmlid", 0, 4, false );
            }
            String parent2 = file2.getParent();
            if ( parent2 == null ) parent2 = ".";
            String fName2 = parent2 + File.separator + "sorted_"
                            + file2.getName();
            FileUtils.stringToFile( s, fName2 );
            runCommand( "diff -b --minimal " + fName1 + " " + fName2 );
        } catch ( Throwable e ) {
            e.printStackTrace();
        }
    }

}
