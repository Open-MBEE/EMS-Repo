package gov.nasa.jpl.view_repo.webscripts;

import org.springframework.extensions.webscripts.WebScriptRequest;

import com.carrotsearch.sizeof.RamUsageEstimator;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.SnapshotArtifactsGenerationActionExecuter;
import gov.nasa.jpl.view_repo.connections.RestPostConnection;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.NodeUtil;

public class AllFlagsGet extends FlagSet {

    public static String[] flags =
            new String[] { "alwaysTurnDebugOff", 
                           "debug", 
                           "fullCache",
                           "nodeAtTimeCache",
                           "heisenCache", 
                           "jsonCache",
                           "jsonDeepCache",
                           "jsonStringCache",
                           "modelPostTimeEvents",
                           "propertyCache",
                           "runWithTransactions", 
                           "simpleCache",
                           "syncTransactions", 
                           "timeEvents",
                           "versionCacheDebug",
                           "versionCache",
                           "versionHistoryCache",
                           "skipWorkspacePermissionCheck", 
                           "optimisticJustFirst",
                           "makeDocBook",
                           "glom",
                           "cleanJson",
                           "diffDefaultIsMerge", 
                           "viewpointExpressions",
                           "diffDefaultIsMerge",
                           "cacheSnapshots",
                           "cacheDynamic",
                           "checkMmsVersions",
                           "graphDb",
                           "postProcessQualified",
                           "doorsSync",
                           "autoBuildGraphDb",
                           "skipQualified",
                           "skipSvgToPng",
                           "restPost",
                           "transactionPeriod"};
    
    public String[] getAllFlags() {
        return flags;
    }

    protected String getPath() {
        String path = req.getPathInfo();
        if (logger.isDebugEnabled()) logger.debug(path);
        String result = path.replace("/flags/","").replace("/","");
        if ( result.equals( "" ) || result.equals( "flags" ) ) result = "all";
        return result;
    }
    
    @Override
    protected boolean set( boolean val ) {
        String path = getPath();
        
        if (path.equalsIgnoreCase( "all" )) {
            return false;
        }
        
        if (path.equalsIgnoreCase( "alwaysTurnDebugOff" )) {
            AbstractJavaWebScript.alwaysTurnOffDebugOut = val;
        } else if (path.equalsIgnoreCase ("debug")) {
            if ( val ) Debug.turnOn();
            else Debug.turnOff(); 
        } else if (path.equalsIgnoreCase("fullCache")) {
            NodeUtil.doFullCaching = val;
        } else if (path.equalsIgnoreCase("nodeAtTimeCache")) {
            NodeUtil.doNodeAtTimeCaching = val;
        } else if (path.equalsIgnoreCase("heisenCache")) {
            NodeUtil.doHeisenCheck = val;
        } else if (path.equalsIgnoreCase("jsonCache")) {
            // if turning on, flush cache since it might be wrong
            if ( !NodeUtil.doJsonCaching && val ) {
                NodeUtil.jsonCache.clear();
                NodeUtil.jsonDeepCache.clear();
            }
            NodeUtil.doJsonCaching = val;
        } else if (path.equalsIgnoreCase("jsonDeepCache")) {
            // if turning on, flush cache since it might be wrong
            if ( !NodeUtil.doJsonDeepCaching && val ) {
                //NodeUtil.jsonCache.clear();  // simple json cache does not depend on deep cache
                NodeUtil.jsonDeepCache.clear();
            }
            NodeUtil.doJsonDeepCaching = val;
        } else if (path.equalsIgnoreCase("jsonStringCache")) {
            NodeUtil.doJsonStringCaching = val;
        } else if (path.equalsIgnoreCase("modelPostTimeEvents")) {
            ModelPost.timeEvents = val;
        } else if (path.equalsIgnoreCase("propertyCache")) {
            if ( !NodeUtil.doPropertyCaching && val ) {
                NodeUtil.propertyCache.clear();
            }
            NodeUtil.doPropertyCaching = val;
        } else if (path.equalsIgnoreCase("runWithTransactions")) {
            AbstractJavaWebScript.defaultRunWithoutTransactions = !val;
        } else if (path.equalsIgnoreCase("simpleCache")) {
            NodeUtil.doSimpleCaching = val;
        } else if (path.equalsIgnoreCase("syncTransactions")) {
            EmsTransaction.syncTransactions = val;
        } else if (path.equalsIgnoreCase("timeEvents")) {
            NodeUtil.timeEvents = val;
        } else if (path.equalsIgnoreCase("versionCacheDebug")) {
            EmsScriptNode.versionCacheDebugPrint = val;
        }  else if (path.equalsIgnoreCase("viewpointExpressions")) {
            if ( val && !EmsScriptNode.expressionStuffDefault ) {
                NodeUtil.jsonCache.clear();
            }
            EmsScriptNode.expressionStuffDefault = val;
            //EmsScriptNode.addingAffectedIds = val;
        } else if (path.equalsIgnoreCase("versionCache")) {
            NodeUtil.doVersionCaching = val;
        } else if (path.equalsIgnoreCase("versionHistoryCache")) {
            NodeUtil.doVersionHistoryCaching = val;
        } else if (path.equalsIgnoreCase("skipWorkspacePermissionCheck")) {
            NodeUtil.skipWorkspacePermissionCheck = val;
        } else if (path.equalsIgnoreCase("optimisticJustFirst")) {
            NodeUtil.doOptimisticJustFirst = val;
        } else if (path.equalsIgnoreCase("makeDocBook")) {
            SnapshotArtifactsGenerationActionExecuter.makeDocBook = val;
        } else if (path.equalsIgnoreCase("glom")) {
        	    MmsDiffGet.glom = val;
        } else if (path.equalsIgnoreCase("cleanJson")) {
        	    CommitUtil.cleanJson = val;
        } else if (path.equalsIgnoreCase("diffDefaultIsMerge")){
        	    MmsDiffGet.diffDefaultIsMerge = val;
        } else if (path.equalsIgnoreCase("cacheSnapshots")) {
            DeclarativeJavaWebScript.cacheSnapshotsFlag = val;
        } else if (path.equalsIgnoreCase("cacheDynamic")) {
            DeclarativeJavaWebScript.cacheDynamicFlag = val;
        } else if (path.equalsIgnoreCase("checkMmsVersions")){
        	    DeclarativeJavaWebScript.checkMmsVersions = val;
        } else if (path.equalsIgnoreCase("graphDb")) {
            NodeUtil.doGraphDb = val;
        } else if (path.equalsIgnoreCase("postProcessQualified")) {
            NodeUtil.doPostProcessQualified = val;
        } else if (path.equalsIgnoreCase("doorsSync")) {
            NodeUtil.doorsSync = val;
        } else if (path.equalsIgnoreCase("autoBuildGraphDb")) {
            NodeUtil.doAutoBuildGraphDb = val;
        } else if (path.equalsIgnoreCase("skipQualified")) {
            NodeUtil.skipQualified = val;
        } else if (path.equalsIgnoreCase("skipSvgToPng")){
            NodeUtil.skipSvgToPng = val;
        } else if (path.equalsIgnoreCase("restPost")) {
            RestPostConnection.setDoRestPost( val );
        } else if (path.equalsIgnoreCase("transactionPeriod")) {
            NodeUtil.transactionPeriod = val ? NodeUtil.defaultTransactionPeriod : 1;
        }
        
        return true;
    }

    @Override
    protected boolean get() {
        String path = getPath();
        return get( path );
    }
    @Override
    protected boolean get( String path ) {
        if (path.equalsIgnoreCase( "all" )) {
            return true;
        }
        
        if (path.equalsIgnoreCase( "alwaysTurnDebugOff" )) {
            return AbstractJavaWebScript.alwaysTurnOffDebugOut;
        } else if (path.equalsIgnoreCase ("debug")) {
            return Debug.isOn();
        } else if (path.equalsIgnoreCase("fullCache")) {
            return NodeUtil.doFullCaching;
        } else if (path.equalsIgnoreCase("nodeAtTimeCache")) {
            return NodeUtil.doNodeAtTimeCaching;
        } else if (path.equalsIgnoreCase("heisenCache")) {
            return NodeUtil.doHeisenCheck;
        } else if (path.equalsIgnoreCase("jsonCache")) {
            return NodeUtil.doJsonCaching;
        } else if (path.equalsIgnoreCase("jsonDeepCache")) {
            return NodeUtil.doJsonDeepCaching;
        } else if (path.equalsIgnoreCase("jsonStringCache")) {
            return NodeUtil.doJsonStringCaching;
        } else if (path.equalsIgnoreCase("modelPostTimeEvents")) {
            return ModelPost.timeEvents;
        } else if (path.equalsIgnoreCase("propertyCache")) {
            return NodeUtil.doPropertyCaching;
        } else if (path.equalsIgnoreCase("runWithTransactions")) {
            return !AbstractJavaWebScript.defaultRunWithoutTransactions;
        } else if (path.equalsIgnoreCase("simpleCache")) {
            return NodeUtil.doSimpleCaching;
        } else if (path.equalsIgnoreCase("syncTransactions")) {
            return EmsTransaction.syncTransactions;
        } else if (path.equalsIgnoreCase("timeEvents")) {
            return NodeUtil.timeEvents;
        } else if (path.equalsIgnoreCase("versionCacheDebug")) {
            return EmsScriptNode.versionCacheDebugPrint;
        }  else if (path.equalsIgnoreCase("viewpointExpressions")) {
            return EmsScriptNode.expressionStuffDefault;
        }  else if (path.equalsIgnoreCase("versionCache")) {
            return NodeUtil.doVersionCaching;
        } else if (path.equalsIgnoreCase("versionHistoryCache")) {
            return NodeUtil.doVersionHistoryCaching;
        } else if (path.equalsIgnoreCase("skipWorkspacePermissionCheck")) {
            return NodeUtil.skipWorkspacePermissionCheck;
        } else if (path.equalsIgnoreCase("optimisticJustFirst")) {
            return NodeUtil.doOptimisticJustFirst;
        } else if (path.equalsIgnoreCase("makeDocBook")) {
            return SnapshotArtifactsGenerationActionExecuter.makeDocBook;
        }else if (path.equalsIgnoreCase("glom")) {
            return MmsDiffGet.glom;
        } else if (path.equalsIgnoreCase("cleanJson")) {
        	    return CommitUtil.cleanJson;
        } else if (path.equalsIgnoreCase("diffDefaultIsMerge")){
            return MmsDiffGet.diffDefaultIsMerge;
        } else if (path.equalsIgnoreCase( "graphDb" )) {
            return NodeUtil.doGraphDb;
        } else if (path.equalsIgnoreCase("checkMmsVersions")){
            return DeclarativeJavaWebScript.checkMmsVersions;
        } else if (path.equalsIgnoreCase( "postProcessQualified" )) {
            return NodeUtil.doPostProcessQualified;
        } else if (path.equalsIgnoreCase( "doorsSync" )) {
            return NodeUtil.doorsSync;
        } else if (path.equalsIgnoreCase( "autoBuildGraphDb" )) {
            return NodeUtil.doAutoBuildGraphDb;
        } else if (path.equalsIgnoreCase( "skipQualified" )) {
            return NodeUtil.skipQualified;
		} else if (path.equalsIgnoreCase( "skipSvgToPng" )) {
			return NodeUtil.skipSvgToPng;
        } else if (path.equalsIgnoreCase("restPost")) {
            return RestPostConnection.getDoRestPost();
        } else if (path.equalsIgnoreCase( "cacheSnapshot" )) {
            return DeclarativeJavaWebScript.cacheSnapshotsFlag;
        } else if (path.equalsIgnoreCase( "cacheDynamic" )) {
            return DeclarativeJavaWebScript.cacheDynamicFlag;
        } else if (path.equalsIgnoreCase( "transactionPeriod" )) {
            return NodeUtil.transactionPeriod > 1;
        }
        return false;
    }
    
    @Override
    protected boolean clear() {
        String path = getPath();

        if (path.equalsIgnoreCase( "all" )) {
            NodeUtil.elementCache.clear();
            NodeUtil.nodeAtTimeCache.clear();
            NodeUtil.jsonCache.clear();
            NodeUtil.jsonDeepCache.clear();
            NodeUtil.jsonStringCache.clear();
            NodeUtil.propertyCache.clear();
            NodeUtil.simpleCache.clear();
            NodeUtil.versionCache.clear();
            NodeUtil.versionHistoryCache.clear();
            return true;
        }
        
        if (path.equalsIgnoreCase( "alwaysTurnDebugOff" )) {
        } else if (path.equalsIgnoreCase("fullCache")) {
            NodeUtil.elementCache.clear();
            return true;
        } else if (path.equalsIgnoreCase("nodeAtTimeCache")) {
            NodeUtil.nodeAtTimeCache.clear();
            return true;
        } else if (path.equalsIgnoreCase("jsonCache")) {
            NodeUtil.jsonCache.clear();
            return true;
        } else if (path.equalsIgnoreCase("jsonDeepCache")) {
            NodeUtil.jsonDeepCache.clear();
            return true;
        } else if (path.equalsIgnoreCase("jsonStringCache")) {
            NodeUtil.jsonStringCache.clear();;
            return true;
        } else if (path.equalsIgnoreCase("propertyCache")) {
            NodeUtil.propertyCache.clear();;
            return true;
        } else if (path.equalsIgnoreCase("simpleCache")) {
            NodeUtil.simpleCache.clear();
            return true;
        } else if (path.equalsIgnoreCase("versionCache")) {
            NodeUtil.versionCache.clear();;
            return true;
        } else if (path.equalsIgnoreCase("versionHistoryCache")) {
            NodeUtil.versionHistoryCache.clear();
            return true;
        } else if (path.equalsIgnoreCase ("debug")) {
            return false;
        } else if (path.equalsIgnoreCase("heisenCheck")) {
            return false;
        } else if (path.equalsIgnoreCase("modelPostTimeEvents")) {
            return false;
        } else if (path.equalsIgnoreCase("runWithTransactions")) {
            return false;
        } else if (path.equalsIgnoreCase("syncTransactions")) {
            return false;
        } else if (path.equalsIgnoreCase("timeEvents")) {
            return false;
        } else if (path.equalsIgnoreCase("versionCacheDebug")) {
            return false;
        } else if (path.equalsIgnoreCase("viewpointExpressions")) {
        	    return false;
        } else if (path.equalsIgnoreCase("skipWorkspacePermissionCheck")) {
            return false;
        } else if (path.equalsIgnoreCase("optimisticJustFirst")) {
            return false;
        } else if (path.equalsIgnoreCase("glom")) {
        	    return false;
        } else if (path.equalsIgnoreCase("cleanJson")) {
        	    return false;
        } else if (path.equalsIgnoreCase("diffDefaultIsMerge")){
        	    return false;
        } else if (path.equalsIgnoreCase("checkMmsVersions")){
        	    return false;
        } else if (path.equalsIgnoreCase( "doGraphDb" )) {
            return false;
        } else if (path.equalsIgnoreCase( "doPostProcessQualified" )) {
            return false;
        } else if (path.equalsIgnoreCase( "doorsSync" )) {
            return false;
        } else if (path.equalsIgnoreCase( "autoBuildGraphDb" )) {
            return false;
        } else if (path.equalsIgnoreCase( "skipQualified" )) {
            return false;
        } else if (path.equalsIgnoreCase( "skipSvgToPng" )) {
        	    return false;
        } else if (path.equalsIgnoreCase("restPost")) {
            return false;
        } else if (path.equalsIgnoreCase( "cacheSnapshot" )) {
            return false;
        } else if (path.equalsIgnoreCase( "cacheDynamic" )) {
            return false;
        } else if (path.equalsIgnoreCase( "transactionPeriod" )) {
            return false;
        } 
        return false;
    };

    
    @Override
    protected String flag() { 
        return getPath();
    };

    @Override
    protected String flagName() {
        String path = getPath();
        
        if (path.equalsIgnoreCase( "all" )) {
            return "all";
        }

        if (path.equalsIgnoreCase( "alwaysTurnDebugOff" )) {
            return "alwaysTurnOffDebugOut";
        } else if (path.equalsIgnoreCase ("debug")) {
            return "debug";
        } else if (path.equalsIgnoreCase("fullCache")) {
            return "doFullCaching";
        } else if (path.equalsIgnoreCase("nodeAtTimeCache")) {
            return "doNodeAtTimeCaching";
        } else if (path.equalsIgnoreCase("heisenCache")) {
            return "doHeisenCheck";
        } else if (path.equalsIgnoreCase("jsonCache")) {
            return "doJsonCaching";
        } else if (path.equalsIgnoreCase("jsonDeepCache")) {
            return "doJsonDeepCaching";
        } else if (path.equalsIgnoreCase("jsonStringCache")) {
            return "doJsonStringCaching";
        } else if (path.equalsIgnoreCase("modelPostTimeEvents")) {
            return "timeEvents";
        } else if (path.equalsIgnoreCase("propertyCache")) {
            return "doPropertyCaching";
        } else if (path.equalsIgnoreCase("runWithTransactions")) {
            return "defaultRunWithoutTransactions";
        } else if (path.equalsIgnoreCase("simpleCache")) {
            return "doSimpleCaching";
        } else if (path.equalsIgnoreCase("syncTransactions")) {
            return "syncTransactions";
        } else if (path.equalsIgnoreCase("timeEvents")) {
            return "timeEvents";
        } else if (path.equalsIgnoreCase("versionCacheDebug")) {
            return "versionCacheDebugPrint";
        } else if (path.equalsIgnoreCase("viewpointExpressions")) {
            return "expressionStuff";
        } else if (path.equalsIgnoreCase("versionCache")) {
            return "doVersionCaching";
        } else if (path.equalsIgnoreCase("versionHistoryCache")) {
            return "doVersionHistoryCaching";
        } else if (path.equalsIgnoreCase("skipWorkspacePermissionCheck")) {
            return "skipWorkspacePermissionCheck";
        } else if (path.equalsIgnoreCase("optimisticJustFirst")) {
            return "doOptimisticJustFirst";
        } else if (path.equalsIgnoreCase("makeDocBook")) {
            return "makeDocBook";
        } else if (path.equalsIgnoreCase("glom")) {
        	    return "glom";
        } else if (path.equalsIgnoreCase("cleanJson")) {
        	    return "cleanJson";
        } else if (path.equalsIgnoreCase("diffDefaultIsMerge")){
            return "diffDefaultIsMerge";
        } else if (path.equalsIgnoreCase("cacheSnapshots")) {
            return "cacheSnapshotsFlag";
        } else if (path.equalsIgnoreCase("cacheDynamic")) {
            return "cacheDynamicFlag";
        } else if (path.equalsIgnoreCase("checkMmsVersions")){
        	    return "checkMmsVersions";
        } else if (path.equalsIgnoreCase("graphDb")) {
            return "graphDb";
        } else if (path.equalsIgnoreCase("postProcessQualified")) {
            return "postProcessQualified";
        } else if (path.equalsIgnoreCase("doorsSync")) {
            return "doorsSync";
        } else if (path.equalsIgnoreCase( "autoBuildGraphDb" )) {
            return "autoBuildGraphDb";
        } else if (path.equalsIgnoreCase("skipQualified")) {
            return "skipQualified";
        } else if (path.equalsIgnoreCase( "skipSvgToPng")) {
        	    return "skipSvgToPg";
        } else if (path.equalsIgnoreCase("restPost")) {
            return "restPost";
        } else if (path.equalsIgnoreCase("transactionPeriod")) {
            return "transactionPeriod";
        }
        return path;
    }

    @Override
    protected long size( String path ) {
        if ( path.equalsIgnoreCase( "fullCache" ) ) {
            return NodeUtil.elementCache.size();
        } else if ( path.equalsIgnoreCase( "nodeAtTimeCache" ) ) {
            return NodeUtil.nodeAtTimeCache.size();
        } else if ( path.equalsIgnoreCase( "jsonCache" ) ) {
            return NodeUtil.jsonCache.size();
        } else if ( path.equalsIgnoreCase( "jsonDeepCache" ) ) {
            return NodeUtil.jsonDeepCache.size();
        } else if ( path.equalsIgnoreCase( "jsonStringCache" ) ) {
            return NodeUtil.jsonStringCache.size();
        } else if ( path.equalsIgnoreCase( "propertyCache" ) ) {
            return NodeUtil.propertyCache.size();
        } else if ( path.equalsIgnoreCase( "simpleCache" ) ) {
            return NodeUtil.simpleCache.size();
        } else if ( path.equalsIgnoreCase( "versionCache" ) ) {
            return NodeUtil.versionCache.size();
        } else if ( path.equalsIgnoreCase( "versionHistoryCache" ) ) {
            return NodeUtil.versionHistoryCache.size();
        }
        return -1;
    }
    
    protected long objectSize( Object o ) {
        return RamUsageEstimator.sizeOf( o );
    }
    
    @Override
    protected long objectSize( String path ) {
        if (path.equalsIgnoreCase("fullCache")) {
            return objectSize( NodeUtil.elementCache );
        } else if ( path.equalsIgnoreCase( "nodeAtTimeCache" ) ) {
            return objectSize( NodeUtil.nodeAtTimeCache );
        } else if ( path.equalsIgnoreCase( "jsonCache" ) ) {
            return objectSize( NodeUtil.jsonCache );
        } else if ( path.equalsIgnoreCase( "jsonDeepCache" ) ) {
            return objectSize( NodeUtil.jsonDeepCache );
        } else if ( path.equalsIgnoreCase( "jsonStringCache" ) ) {
            return objectSize( NodeUtil.jsonStringCache );
        } else if ( path.equalsIgnoreCase( "propertyCache" ) ) {
            return objectSize( NodeUtil.propertyCache );
        } else if ( path.equalsIgnoreCase( "simpleCache" ) ) {
            return objectSize( NodeUtil.simpleCache );
        } else if ( path.equalsIgnoreCase( "versionCache" ) ) {
            return objectSize( NodeUtil.versionCache );
        } else if ( path.equalsIgnoreCase( "versionHistoryCache" ) ) {
            return objectSize( NodeUtil.versionHistoryCache );
        }
        return -1;
    }

    @Override
    protected String size() {
        String path = getPath();
        if ( path.equals( "all" ) || Utils.isNullOrEmpty( path ) ) {
            StringBuffer msg = new StringBuffer();
            msg.append( "elementCache.size() = " + NodeUtil.elementCache.size() );
            msg.append( "\nobject size of elementCache = " + objectSize( NodeUtil.elementCache ) );
            msg.append( "\nnodeAtTimeCache.size() = " + NodeUtil.nodeAtTimeCache.size() );
            msg.append( "\nobject size of nodeAtTimeCache = " + objectSize( NodeUtil.nodeAtTimeCache ) );
            msg.append( "\njsonCache.size() = " + NodeUtil.jsonCache.size() );
            msg.append( "\nobject size of jsonCache = " + objectSize( NodeUtil.jsonCache ) );
            msg.append( "\njsonDeepCache.size() = " + NodeUtil.jsonDeepCache.size() );
            msg.append( "\nobject size of jsonDeepCache = " + objectSize( NodeUtil.jsonDeepCache ) );
            msg.append( "\njsonStringCache.size() = " + NodeUtil.jsonStringCache.size() );
            msg.append( "\nobject size of jsonStringCache = " + objectSize( NodeUtil.jsonStringCache ) );
            msg.append( "\npropertyCache.size() = " + NodeUtil.propertyCache.size() );
            msg.append( "\nobject size of propertyCache = " + objectSize( NodeUtil.propertyCache ) );
            msg.append( "\nsimpleCache.size() = " + NodeUtil.simpleCache.size() );
            msg.append( "\nobject size of simpleCache = " + objectSize( NodeUtil.simpleCache ) );
            msg.append( "\nversionCache.size() = " + NodeUtil.versionCache.size() );
            msg.append( "\nobject size of versionCache = " + objectSize( NodeUtil.versionCache ) );
            msg.append( "\nversionHistoryCache.size() = " + NodeUtil.versionHistoryCache.size() );
            msg.append( "\nobject size of versionHistoryCache = " + objectSize( NodeUtil.versionHistoryCache ) );
            return msg.toString();
        }
        long s = size(path);
        return path + ".size() = " + s;
    }    

    /*
    @Override
    protected Object getValue( String path ) {
        String msg = "";
        if (path.equalsIgnoreCase( "cacheDynamic" ) || path.equalsIgnoreCase( "maxage" )) {
            msg = "maxage = " + DeclarativeJavaWebScript.maxage;
        } else if (path.equalsIgnoreCase( "transactionPeriod" ) || path.equalsIgnoreCase( "n" )) {
            msg = "transactionPeriod = " + NodeUtil.transactionPeriod;
        } else if (path.equalsIgnoreCase( "size" ) ) {
            
        }
        return msg;
    }
    */


    @Override
    protected String handleNonBooleans( WebScriptRequest req ) {
        String msg = "";
        String path = getPath();
        
        if (path.equalsIgnoreCase( "cacheDynamic" )) {
            String maxage = req.getParameter( "maxage" );
            if (maxage != null) {
                DeclarativeJavaWebScript.maxage = Long.parseLong( maxage );
            }
            msg = "maxage = " + DeclarativeJavaWebScript.maxage;
        } else if (path.equalsIgnoreCase( "transactionPeriod" )) {
            String newPeriod = req.getParameter( "n" );
            if (newPeriod != null) {
                NodeUtil.transactionPeriod = Integer.parseInt( newPeriod );
            }
            msg = "transactionPeriod = " + NodeUtil.transactionPeriod;
        }
        return msg;
    }
}
