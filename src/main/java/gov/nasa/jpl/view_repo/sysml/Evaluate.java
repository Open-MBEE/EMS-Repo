/**
 * 
 */
package gov.nasa.jpl.view_repo.sysml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONObject;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript;
import sysml.view.Viewable;

/**
 * Represent the evaluation of a text expression, UML Expression, or other
 * Object as a Viewable. In general, a non-editable string can be returned in a
 * paragraph.
 */
public class Evaluate implements Viewable< EmsScriptNode > {

    Object object;
    Viewable<?> interpretation = null;
    
    // TODO -- use workspace and dateTime
    WorkspaceNode workspace = null;
    Date dateTime = null;
    boolean ignoreWorkspace = true;
    
    Evaluate( Object object, boolean ignoreWorkspace, WorkspaceNode workspace,
              Date dateTime ) {
        this.object = object;
        this.ignoreWorkspace = ignoreWorkspace;
        this.workspace = workspace;
        this.dateTime = dateTime;
        interpret();
    }
    
    Evaluate( Object object ) {
        this.object = object;
        interpret();
    }
    
    // TODO - Check for infinite recursion?
    protected void interpret() {
        Object resultObj = null;
        boolean gotResult = false;
        if ( object instanceof Viewable ) {
            interpretation = (Viewable<?>)object;
            return;
        }
        if ( object instanceof Collection ) {
            Collection<?> c = (Collection< ? >)object;
            if ( c.size() == 1 ) {
                interpretation =
                        (new Evaluate(c.iterator().next())).interpretation;
            } else {
                gov.nasa.jpl.view_repo.sysml.List list =
                        new gov.nasa.jpl.view_repo.sysml.List();
                for ( Object o : c ) {
                    list.add( new Evaluate( o ) );
                }
                interpretation = list;
            }
            return;
        }
        if ( object instanceof EmsScriptNode ) {
            EmsScriptNode n = (EmsScriptNode)object;
            if ( ignoreWorkspace && dateTime == null ) {
                workspace = n.getWorkspace();
            } else {
                n = n.findScriptNodeByName( n.getName(), ignoreWorkspace,
                                            workspace, dateTime );
            }
            if ( n.hasOrInheritsAspect( "sysml:Expression" ) ) {
                Map< Object, Object > result =
                        AbstractJavaWebScript.evaluate( Utils.newSet( n ),
                                                        workspace );
                resultObj = result;
                gotResult = true;
                if ( result != null ) {
                    if ( result.size() == 1 ) {
                        resultObj = result.values().iterator().next();
                    } else {
                        for ( Entry< Object, Object > e : result.entrySet() ) {
                            if ( e.getKey().equals( n ) ) {
                                resultObj = e.getValue();
                                break;
                            }
                        }
                    }
                }
            } else {
                ArrayList< NodeRef > c =
                        n.getValueSpecOwnedChildren( false, dateTime, workspace );
                if ( c != null ) {
                    List< EmsScriptNode > nodes =
                            EmsScriptNode.toEmsScriptNodeList( c, NodeUtil.getServices(),
                                                               null, null);
                    resultObj = nodes;
                    gotResult = true;
                }
            }
            if ( gotResult ) {
                Evaluate e = new Evaluate(resultObj);
                interpretation = e.interpretation;
                return;
            }
        }
        interpretation = new Text( "" + object );
    }
    
    @Override
    public JSONObject toViewJson( Date dateTime ) {
        if ( interpretation != null ) return interpretation.toViewJson( dateTime );
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getDisplayedElements() {
        if ( interpretation != null ) {
            return Utils.asList( interpretation.getDisplayedElements(),
                                 EmsScriptNode.class );
        }
        return Utils.getEmptyList();
    }

}
