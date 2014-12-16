package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.NodeUtil;

/**
 * Allows heisenCache to be turned on/off
 */
public class HeisenCacheGet extends FlagSet {

    @Override
    protected void set( boolean val ) {
        NodeUtil.doHeisenCheck = val;
    }

    @Override
    protected boolean get() {
        return NodeUtil.doHeisenCheck;
    }

    @Override
    protected String flagName() {
        return "heisenCache";
    }
        
//    protected Map<String, Object> executeImpl(WebScriptRequest req,
//                                              Status status, Cache cache) {
//        HeisenCacheGet h = new HeisenCacheGet();
//        return h.executeImplImpl( req, status, cache );
//    }
//    protected Map<String, Object> executeImplImpl(WebScriptRequest req,
//                Status status, Cache cache) {
//        Map< String, Object > model = new HashMap< String, Object >();
//
//        String turnOnStr = req.getParameter( "on" );
//        String turnOffStr = req.getParameter( "off" );
//
//        boolean turnOn = !( turnOnStr == null ||
//                            turnOnStr.trim().equalsIgnoreCase( "false" ) ||
//                            ( turnOffStr != null &&
//                              turnOffStr.trim().equalsIgnoreCase( "true" ) ) );
//        turnOnStr = turnOn ? "on" : "off";
//        if ( turnOn == NodeUtil.doHeisenCheck ) {
//            System.out.println( ( new Date() ) + ": heisenCache is already "
//                                + turnOnStr );
//        } else {
//            if ( turnOn ) NodeUtil.doHeisenCheck = true;
//            else NodeUtil.doHeisenCheck = false;
//            System.out.println( ( new Date() ) + ": heisenCache turned " + turnOnStr );
//        }
//        model.put( "res", "heisenCache " + turnOnStr );
//
//        return model;
//    }

}
