package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.NodeUtil;

public class AllFlagsGet extends FlagSet {
    protected String getPath() {
        String path = req.getPathInfo();
        System.out.println(path);
        return path.replace("/flags/","").replace("/","");
    }
    
    @Override
    protected void set( boolean val ) {
        String path = getPath();
        
        if (path.equalsIgnoreCase( "alwaysTurnDebugOff" )) {
            AbstractJavaWebScript.alwaysTurnOffDebugOut = val;
        } else if (path.equalsIgnoreCase ("debug")) {
            if ( val ) Debug.turnOn();
            else Debug.turnOff(); 
        } else if (path.equalsIgnoreCase("fullCache")) {
            NodeUtil.doFullCaching = val;
        } else if (path.equalsIgnoreCase("heisenCache")) {
            NodeUtil.doHeisenCheck = val;
        } else if (path.equalsIgnoreCase("jsonCache")) {
            // if turning on, flush cache since it might be wrong
            if ( !NodeUtil.doJsonCaching && val ) {
                NodeUtil.jsonCache.clear();
                NodeUtil.jsonDeepCache.clear();
            }
            NodeUtil.doJsonCaching = val;
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
        } else if (path.equalsIgnoreCase("versionCache")) {
            NodeUtil.doVersionCaching = val;
        } else if (path.equalsIgnoreCase("skipWorkspacePermissionCheck")) {
            NodeUtil.skipWorkspacePermissionCheck = val;
        } 
    }

    @Override
    protected boolean get() {
        String path = getPath();
        
        if (path.equalsIgnoreCase( "alwaysTurnDebugOff" )) {
            return AbstractJavaWebScript.alwaysTurnOffDebugOut;
        } else if (path.equalsIgnoreCase ("debug")) {
            return Debug.isOn();
        } else if (path.equalsIgnoreCase("fullCache")) {
            return NodeUtil.doFullCaching;
        } else if (path.equalsIgnoreCase("heisenCache")) {
            return NodeUtil.doHeisenCheck;
        } else if (path.equalsIgnoreCase("jsonCache")) {
            return NodeUtil.doJsonCaching;
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
        } else if (path.equalsIgnoreCase("versionCache")) {
            return NodeUtil.doVersionCaching;
        } else if (path.equalsIgnoreCase("skipWorkspacePermissionCheck")) {
            return NodeUtil.skipWorkspacePermissionCheck;
        } 
        return false;
    }

    @Override
    protected String flagName() {
        String path = getPath();
        
        if (path.equalsIgnoreCase( "alwaysTurnDebugOff" )) {
            return "alwaysTurnOffDebugOut";
        } else if (path.equalsIgnoreCase ("debug")) {
            return "debug";
        } else if (path.equalsIgnoreCase("fullCache")) {
            return "doFullCaching";
        } else if (path.equalsIgnoreCase("heisenCache")) {
            return "doHeisenCheck";
        } else if (path.equalsIgnoreCase("jsonCache")) {
            return "doJsonCaching";
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
        } else if (path.equalsIgnoreCase("versionCache")) {
            return "doVersionCaching";
        } else if (path.equalsIgnoreCase("skipWorkspacePermissionCheck")) {
            return "skipWorkspacePermissionCheck";
        } 
        return null;
    }

}
