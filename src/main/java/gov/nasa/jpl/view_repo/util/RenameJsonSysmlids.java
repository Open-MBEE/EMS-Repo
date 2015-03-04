package gov.nasa.jpl.view_repo.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import gov.nasa.jpl.mbee.util.FileUtils;
import gov.nasa.jpl.mbee.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class RenameJsonSysmlids {

    public static class ReverseLengthComparator implements Comparator< String > {

        public static ReverseLengthComparator instance = new ReverseLengthComparator(); 
        
        @Override
        public int compare( String o1, String o2 ) {
            return 0;
        }
        
    }
    
    public static Collection<String> getSysmlIds( JSONObject json ) {
        TreeSet<String> ids = new TreeSet< String >();
        getSysmlIds( json, ids );
        return ids;
    }
    public static void getSysmlIds( JSONObject json, Set<String> ids ) {
        //final String key = "sysmlid";
        String id = json.optString( "sysmlid" );
        if ( id != null ) ids.add( id );
        
        // dig deeper for ids
        for ( Object k : json.keySet() ) {
            if ( k instanceof String ) {
                String key = (String)k;
                JSONObject o = json.optJSONObject( key );
                if ( o != null ) {
                    getSysmlIds( o, ids );
                    continue;
                }
                JSONArray a = json.optJSONArray( key );
                if ( a != null ) {
                    getSysmlIds( a, ids );
                    continue;
                }
            }
        }
    }
    public static void getSysmlIds( JSONArray json, Set<String> ids ) {
        for ( int i = 0; i < json.length(); ++i ) {
            JSONObject o = json.optJSONObject( i );
            if ( o != null ) {
                getSysmlIds( o, ids );
                continue;
            }
            JSONArray a = json.optJSONArray( i );
            if ( a != null ) {
                getSysmlIds( a, ids );
                continue;
            }
        }
    }
    
    public static String renameSysmlIds( File file1, File file2, String prefix )
                                                                   throws JSONException,
                                                                   FileNotFoundException {
        if ( file1 == null || !file1.exists() ) {
            System.err.println( "File " + file1 + " does not exist!" );
            return null;
        }
        if ( file2 == null ) {
            String ext = FileUtils.getExtension( file1.getAbsolutePath() );
            String outputFileName = FileUtils.removeFileExtension( file1.getAbsolutePath() ) + "_" + prefix + ext;
            file2 = new File( outputFileName );
        }
        
        // Get the input
        String jstr1 = FileUtils.fileToString( file1 );

        jstr1 = renameSysmlIds( jstr1, prefix );

        // Now write out the new file.
        FileUtils.stringToFile( jstr1, file2.getAbsolutePath() );

        return jstr1;
    }
    
    public static String renameSysmlIds( String jstr1, String prefix ) throws JSONException {

        // check input
        if ( Utils.isNullOrEmpty( jstr1 ) ) return null;
        if ( prefix == null ) prefix = "X_000_X_";

        JSONObject o1 = new JSONObject( jstr1 );
        
        // Gather the sysml ids.
        // By ordering these in order of reverse length, ids that are
        // substrings of other ids can't be a problem. This shouldn't be a
        // problem anyway since we replace with the quotes around the ids,
        // but maybe the replacements should be made without the quotes
        // given that a slot id is a concatenations of two ids.
        Set<String> ids = new TreeSet<String>( ReverseLengthComparator.instance );
        getSysmlIds( o1, ids );
        
        // add prefixes to sysml ids using search and replace  
        for ( String id : ids ) {
            jstr1 = jstr1.replace( "\"" + id + "\"", "\"" + prefix + id + "\"" );
        }
        return jstr1;
    }
    
    public static void main( String[] args ) {
        String usage = "Usage: RenameJsonSysmlids file.json [file2.json] [prefixToAdd]";
        if ( args.length < 1 || args.length > 3 ) {
            System.err.println( "Incorrect number of arguments.  Passed in "
                                + args.length + " arguments.  Expected 3." );
            System.err.println(usage);
            return;
        }
        File file1;
        File file2 = null;
        try {
            // get files from command line arguments
            file1 = new File(args[0]); // better exist
            // Figure out which of the remaining args is the second file and
            // which is the prefix.
            boolean gotArg2 = args.length > 1 && !Utils.isNullOrEmpty( args[1] );
            boolean gotArg3 = args.length > 2 && !Utils.isNullOrEmpty( args[2] );
            boolean hasJsonExt2 = gotArg2 && args[1].toLowerCase().endsWith( ".json" );
            boolean hasJsonExt3 = gotArg3 && args[2].toLowerCase().endsWith( ".json" );
            String prefix = null;
            if ( gotArg2 || gotArg3 ) {
                // Figure out which argument is the prefix
                if ( gotArg2 && ( hasJsonExt3 || !hasJsonExt2 ) ) {
                    prefix = args[1];
                    if ( gotArg3 ) file2 = new File( args[2] );
                    // else file2 is null
                } else if ( gotArg3 ) {
                    prefix = args[2];
                    file2 = new File( args[1] );
                } else {
                    // in this case, !gotArg3 && gotArg2 && hasJsonExt2
                    file2 = new File( args[1] );
                }
            }

            // Do all the work!
            renameSysmlIds( file1, file2, prefix );
            
        } catch ( Throwable e ) {
            //System.err.println("Error! " + e.getLocalizedMessage() );
            e.printStackTrace();
            System.exit( 1 );
        }
    }

}
