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
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import gov.nasa.jpl.mbee.util.ClassUtils;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Seen;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ModelLoadActionExecuter;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.ModelContext;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.ServiceContext;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript;
import gov.nasa.jpl.view_repo.webscripts.ModelPost;
import sysml.view.Viewable;

/**
 * Represent the evaluation of a text expression, UML Expression, or other
 * Object as a Viewable. In general, a non-editable Text element can be returned
 * if other evaluations are not appropriate.
 */
public class Evaluate implements Viewable< EmsScriptNode > {
    private static Logger logger;
    public static Level logLevel;

    {
        logger = Logger.getLogger(AbstractJavaWebScript.class);
        logLevel = Level.WARN;
        logger.setLevel( logLevel );
    }
    
    Object object;
    Viewable<?> interpretation = null;
    
    // TODO -- use workspace and dateTime
    ModelContext modelContext = null;
    ServiceContext serviceContext = null;
    
    public Evaluate( Object object, ModelContext modelContext,
                     ServiceContext serviceContext ) {
        this( object, modelContext, serviceContext, null );
    }
    protected Evaluate( Object object, ModelContext modelContext,
                        ServiceContext serviceContext, Seen<Object> seen) {
        super();
        this.object = object;
        this.modelContext = modelContext;
        if ( this.modelContext == null ) this.modelContext = new ModelContext();
        this.serviceContext = serviceContext;
        if ( this.serviceContext == null ) this.serviceContext = new ServiceContext();
        interpret(seen);
    }
    
    public Evaluate( Object object ) {
        this( object, null );
    }
    protected Evaluate( Object object, Seen<Object> seen ) {
        this.object = object; 
        this.modelContext = new ModelContext();
        this.serviceContext = new ServiceContext();
        interpret( seen );
    }
    
    /**
     * Interpret the object as a Viewable. If the object is not otherwise
     * interpreted as a Viewable, simply convert the object to a string and wrap
     * in a Text element.
     * 
     * @param seen
     *            the Set of Objects encountered up the call stack in recursive
     *            calls to interpret(), used to avoid infinite recursion.
     */
    protected void interpret( Seen< Object > seen ) {
        Pair< Boolean, Seen< Object > > p = Utils.seen( object, true, seen );
        if ( p.first ) {
            // End any cycles in recursion by making a simple Text element.
            interpretation = new Text( "" + object);
            return;
        }
        seen = p.second;
        
        if ( object == null || ClassUtils.isPrimitive( object ) ) {
            interpretation = new Text( "" + object);
            return;
        }

        if ( object instanceof Viewable ) {
            interpretation = (Viewable<?>)object;
            return;
        }
        if ( object instanceof Collection ) {
            Collection<?> c = (Collection< ? >)object;
            if ( c.size() == 1 ) {
                interpretation =
                        ( new Evaluate( c.iterator().next(), this.modelContext,
                                        this.serviceContext, seen ) ).interpretation;
            } else {
                gov.nasa.jpl.view_repo.sysml.List list =
                        new gov.nasa.jpl.view_repo.sysml.List();
                for ( Object o : c ) {
                    list.add( new Evaluate( o, this.modelContext,
                                            this.serviceContext, seen ) );
                }
                interpretation = list;
            }
            return;
        }
        Object resultObj = null;
        boolean gotResult = false;
        if ( object instanceof EmsScriptNode ) {
            EmsScriptNode n = (EmsScriptNode)object;
            if ( modelContext.ignoreWorkspaces && modelContext.dateTime == null ) {
                modelContext.workspace = n.getWorkspace();
            } else {
                n = n.findScriptNodeByName( n.getName(), modelContext.ignoreWorkspaces,
                                            modelContext.workspace, modelContext.dateTime );
            }
            if ( n.hasOrInheritsAspect( "sysml:Expression" ) ) {
                Map< Object, Object > result =
                        AbstractJavaWebScript.evaluate( Utils.newSet( n ),
                                                        modelContext.workspace );
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
                        n.getValueSpecOwnedChildren( false, modelContext.dateTime,
                                                     modelContext.workspace );
                if ( !Utils.isNullOrEmpty( c ) ) {
                    List< EmsScriptNode > nodes =
                            EmsScriptNode.toEmsScriptNodeList( c, NodeUtil.getServices(),
                                                               null, null);
                    resultObj = nodes;
                    gotResult = true;
                }
            }
            if ( gotResult ) {
                Evaluate e = new Evaluate(resultObj, this.modelContext,
                                          this.serviceContext, seen);
                interpretation = e.interpretation;
                return;
            }
            if ( n.hasOrInheritsAspect( "sysml:LiteralString" ) ) {
                String s;
                try {
                    s = (String)n.getProperty( "sysml:string" );
                } catch ( ClassCastException e ) {
                    s = null;
                }
                if ( s != null ) {
                    // Evaluate as a text expression
                    resultObj = evaluate( s );
                    gotResult = true;
                }
            }
        }
        if ( object instanceof String ) {
            resultObj = evaluate( (String)object );
            gotResult = true;
        }
        if ( gotResult ) {
            if ( resultObj instanceof Viewable ) {
                interpretation = (Viewable<?>)resultObj;
                return;
            }
            interpretation = new Text( "" + resultObj );
            return;
        }
        interpretation = new Text( "" + object );
    }
    
    /**
     * Try to evaluate the string as a K or Java expression.
     * 
     * @param expression
     * @return the evaluation result or, if the evaluation fails, the input
     *         expression.
     */
    public Object evaluate( String expression ) {
        try {
            JSONObject json = ModelPost.kToJson( expression );
            Set< EmsScriptNode > elements = 
                    ModelLoadActionExecuter.loadJson( json, this.modelContext,
                                                      this.serviceContext );
            if ( Utils.isNullOrEmpty( elements ) ) {
                logger.warn( "Expression \"" + expression + "\" failed to parse!" );
            } else {
                if ( elements.size() > 1 ) {
                    logger.warn( "Expression \"" + expression + "\" generated more than one element!" );
                }
                EmsScriptNode exprNode = elements.iterator().next();
                if ( exprNode == null ) {
                    logger.warn( "Expression \"" + expression + "\" load returned a null element!" );
                    return null;
                }
                String sysmlid = exprNode.getSysmlId();
                Map< Object, Object > results =
                        AbstractJavaWebScript.evaluate( elements, modelContext.workspace );
                if ( results == null || results.isEmpty() ) {
                    logger.warn( "Expression \"" + expression + "\" had an empty evaluation!" );
                } else {
                    Object result = null;
                    if ( results.size() == 1 ) {
                        result = results.values().iterator().next();
                    } else {
                        result = results.get( sysmlid );
                    }
                    logger.warn( "Success!  Evaluated expression \""
                                 + expression + "\" and got " + result );
                    return result;
                }
            }
        } catch (Throwable t) {
            logger.error( "Failed to parse, load, or evaluate expression, \"" + expression + "\"" );
            t.printStackTrace();
        }
        return null;
    }

    @Override
    public JSONObject toViewJson( Date dateTime ) {
        if ( interpretation != null ) return interpretation.toViewJson( dateTime );
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getDisplayedElements() {
        if ( interpretation != null ) {
            Collection< ? > elements = interpretation.getDisplayedElements();
            if ( elements != null ) {
                return Utils.asList( elements, EmsScriptNode.class );
            }
        }
        return Utils.getEmptyList();
    }

}
