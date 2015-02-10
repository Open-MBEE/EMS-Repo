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

import gov.nasa.jpl.view_repo.util.JsonArray;
import org.json.JSONException;
import gov.nasa.jpl.view_repo.util.JsonObject;


public class JsonDiff {

    private static Object sort( Object o ) throws JSONException {
        if ( o instanceof JsonArray ) return sort((JsonArray)o);
        if ( o instanceof JsonObject ) return sort((JsonObject)o);
        return o;
    }
    
    private static JsonArray sort( JsonArray o ) throws JSONException {
        ArrayList<Object> list = new ArrayList<Object>( o.length() );
        for ( int i = 0; i < o.length(); ++i ) {
            Object v = o.get( i );
            v = sort( v );
            list.add( v );
        }
        Collections.sort( list, CompareUtils.GenericComparator.instance() );
        JsonArray sorted = new JsonArray( list );
        return sorted;
    }

    private static JsonObject sort( JsonObject o ) {
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
        return new JsonObject( map );
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
        JsonObject o1=null, o2=null;
        String usage = "Usage: JsonDiff file1.json file2.json";
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
            o1 = new JsonObject( jstr1 );
            o2 = new JsonObject( jstr2 );
        } catch ( Throwable e ) {
            //System.err.println("Error! " + e.getLocalizedMessage() );
            e.printStackTrace();
            return;
        }
        try {
            JsonObject so1 = sort( o1 );
            JsonObject so2 = sort( o2 );
            String fName1 = file1.getParent() + File.separator + "sorted_"
                            + file1.getName();
            String fName2 = file2.getParent() + File.separator + "sorted_"
                            + file2.getName();
            FileUtils.stringToFile( so1.toString(4), fName1 );
            FileUtils.stringToFile( so2.toString(4), fName2 );
            runCommand( "diff -b " + fName1 + " " + fName2 );
        } catch ( Throwable e ) {
            e.printStackTrace();
        }
    }

}
