package gov.nasa.jpl.view_repo.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeMap;

import gov.nasa.jpl.mbee.util.CompareUtils;
import gov.nasa.jpl.mbee.util.FileUtils;



import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class JsonDiff {

    private static Object sort( Object o ) throws JSONException {
        if ( o instanceof JSONArray ) return sort((JSONArray)o);
        if ( o instanceof JSONObject ) return sort((JSONObject)o);
        return o;
    }
    
    private static JSONArray sort( JSONArray o ) throws JSONException {
        ArrayList<Object> list = new ArrayList<Object>( o.length() );
        for ( int i = 0; i < o.length(); ++i ) {
            Object v = o.get( i );
            v = sort( v );
            list.add( v );
        }
        Collections.sort( list, CompareUtils.GenericComparator.instance() );
        JSONArray sorted = new JSONArray( list );
        return sorted;
    }

    private static JSONObject sort( JSONObject o ) {
        TreeMap<String, Object> map = new TreeMap<String, Object>();
        Iterator<?> i = o.keys();
        while ( i.hasNext() ) {
            Object k = i.next();
            if ( k instanceof String ) {
                try {
                    Object v = o.get( (String)k );
                    v = sort( v );
                    map.put( (String)k, v );
                } catch ( JSONException e ) {
                    e.printStackTrace();
                }
            }
        }
        return new JSONObject( map );
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
        try {
            file1 = new File(args[0]);
            file2 = new File(args[1]);
            String jstr1 = FileUtils.fileToString( file1 );
            String jstr2 = FileUtils.fileToString( file2 );
            o1 = new JSONObject( jstr1 );
            o2 = new JSONObject( jstr2 );
        } catch ( Throwable e ) {
            //System.err.println("Error! " + e.getLocalizedMessage() );
            e.printStackTrace();
            return;
        }
        try {
            JSONObject so1 = sort( o1 );
            JSONObject so2 = sort( o2 );
            String parent1 = file1.getParent();
            String parent2 = file2.getParent();
            if ( parent1 == null ) parent1 = ".";
            if ( parent2 == null ) parent2 = ".";
            String fName1 = parent1 + File.separator + "sorted_"
                            + file1.getName();
            String fName2 = parent2 + File.separator + "sorted_"
                            + file2.getName();
            FileUtils.stringToFile( so1.toString( 4 ), fName1 );
            FileUtils.stringToFile( so2.toString( 4 ), fName2 );
            runCommand( "diff -b " + fName1 + " " + fName2 );
        } catch ( Throwable e ) {
            e.printStackTrace();
        }
    }

}
