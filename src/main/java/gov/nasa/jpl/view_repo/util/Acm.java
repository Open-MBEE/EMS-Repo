/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").  
 * U.S. Government sponsorship acknowledged.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met:
 * 
 *  - Redistributions of source code must retain the above copyright notice, this list of 
 *    conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list 
 *    of conditions and the following disclaimer in the documentation and/or other materials 
 *    provided with the distribution.
 *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, 
 *    nor the names of its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER  
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.util;


import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Simple static class for keeping track of Alfresco Content Model types and JSON mappings
 * @author cinyoung
 *
 */
public class Acm {
    // JSON types
    public static final String JSON_COMMENT = "Comment";
    public static final String JSON_CONSTRAINT = "Constraint";
    public static final String JSON_CONSTRAINT_SPECIFICATION = "constraintSpecification";
    public static final String JSON_CONFORM = "Conform";
    public static final String JSON_DEPENDENCY = "Dependency";
    public static final String JSON_DIRECTED_RELATIONSHIP = "DirectedRelationship";
    public static final String JSON_ELEMENT = "Element";
    public static final String JSON_EXPOSE = "Expose";
    public static final String JSON_GENERALIZATION = "Generalization";
    public static final String JSON_PACKAGE = "Package";
    public static final String JSON_PROPERTY = "Property";
    public static final String JSON_VIEWPOINT = "Viewpoint";
//    public static final String JSON_PARAMETER = "Parameter";
//    public static final String JSON_OPERATION = "Operation";
    public static final String JSON_IS_DERIVED = "isDerived";
    public static final String JSON_IS_SLOT = "isSlot";
    public static final String JSON_DOCUMENTATION = "documentation";
    public static final String JSON_ID = "id";
    public static final String JSON_NAME = "name";
    public static final String JSON_SOURCE = "source";
    public static final String JSON_TARGET = "target";
    public static final String JSON_VALUE_TYPE = "valueType";
    public static final String JSON_VALUE = "value";
//    public static final String JSON_VALUE_EXPRESSION = "valueExpression";
    public static final String JSON_BODY = "body";
    public static final String JSON_EXPRESSION_BODY = "expressionBody";
    public static final String JSON_ANNOTATED_ELEMENTS = "annotatedElements";
    
//    public static final String JSON_TIME_MAX = "timeMax";
//    public static final String JSON_TIME_MIN = "timeMin";
//    public static final String JSON_DURATION_MAX = "durationMax";
//    public static final String JSON_DURATION_MIN = "durationMin";
    
    
    public static final String JSON_ALLOWED_ELEMENTS = "allowedElements";
    public static final String JSON_CHILDREN_VIEWS = "childrenViews";
    public static final String JSON_CONTAINS = "contains";
    public static final String JSON_DISPLAYED_ELEMENTS = "displayedElements";
    public static final String JSON_NO_SECTIONS = "noSections";
    public static final String JSON_VIEW_2_VIEW = "view2view";
    
//    public static final String JSON_EXPRESSION = "Expression";
//    public static final String JSON_LITERAL_BOOLEAN = "LiteralBoolean";
//    public static final String JSON_LITERAL_INTEGER = "LiteralInteger";
//    public static final String JSON_LITERAL_REAL = "LiteralReal";
//    public static final String JSON_LITERAL_UNLIMITED_NATURAL = "LiteralUnlimitedNatural";
//    public static final String JSON_LITERAL_STRING = "LiteralString";
//    public static final String JSON_ELEMENT_VALUE = "ElementValue";

    public static final String JSON_BOOLEAN = "boolean";
    public static final String JSON_DOUBLE = "double";
    public static final String JSON_INTEGER = "integer";
    public static final String JSON_REAL = "real";
    public static final String JSON_NATURAL_VALUE = "naturalValue";
    public static final String JSON_STRING = "string";
    
    public static final String JSON_TYPE = "type";
    public static final String JSON_OWNER = "owner";
    public static final String JSON_LAST_MODIFIED = "lastModified";
    public static final String JSON_AUTHOR = "author";
    public static final String JSON_PROPERTY_TYPE = "propertyType";

    // Value spec additions
    public static final String JSON_VALUE_SPECIFICATION = "ValueSpecification";
    public static final String JSON_VALUE_EXPRESSION = "valueExpression";
    public static final String JSON_DURATION = "Duration";
    public static final String JSON_DURATION_INTERVAL = "DurationInterval";
    public static final String JSON_DURATION_MAX = "durationMax";
    public static final String JSON_DURATION_MIN = "durationMin";
    public static final String JSON_ELEMENT_VALUE = "ElementValue";
    public static final String JSON_ELEMENT_VALUE_ELEMENT = "elementValueElement";
    public static final String JSON_EXPRESSION = "expression";
    public static final String JSON_OPERAND = "operand";
    public static final String JSON_INSTANCE_VALUE = "InstanceValue";
    public static final String JSON_INSTANCE = "instance";
    public static final String JSON_INTERVAL = "interval";
    public static final String JSON_LITERAL_BOOLEAN = "LiteralBoolean";
    public static final String JSON_LITERAL_INTEGER = "LiteralInteger";
    public static final String JSON_LITERAL_NULL = "LiteralNull";
    public static final String JSON_LITERAL_REAL = "LiteralReal";
    public static final String JSON_LITERAL_UNLIMITED_NATURAL = "LiteralUnlimitedNatural";
    public static final String JSON_LITERAL_STRING = "LiteralString";
    public static final String JSON_OPAQUE_EXPRESSION = "OpaqueExpression";
    public static final String JSON_STRING_EXPRESSION = "StringExpression";
    public static final String JSON_TIME_EXPRESSION = "TimeExpression";
    public static final String JSON_TIME_INTERVAL = "TimeInterval";
    public static final String JSON_TIME_INTERVAL_MAX = "timeIntervalMax";
    public static final String JSON_TIME_INTERVAL_MIN = "timeIntervalMin";
    public static final String JSON_OPERATION = "Operation";
    public static final String JSON_OPERATION_PARAMETER = "operationParameter";
    public static final String JSON_INSTANCE_SPECIFICATION = "InstanceSpecification";
    public static final String JSON_INSTANCE_SPECIFICATION_SPECIFICATION = "instanceSpecificationSpecification";
    public static final String JSON_PARAMETER = "Parameter";
    public static final String JSON_PARAMETER_DIRECTION = "parameterDirection";
    public static final String JSON_PARAMETER_DEFAULT_VALUE = "parameterDefaultValue";
    

    // ACM types with the different name spaces
    public static final String SYSML = "sysml:";
    public static final String VIEW = "view2:";
    public static final String CM = "cm:";
    
    public static final String ACM_COMMENT = SYSML + JSON_COMMENT;
    public static final String ACM_CONSTRAINT = SYSML + JSON_CONSTRAINT;
    public static final String ACM_CONSTRAINT_SPECIFICATION = SYSML + JSON_CONSTRAINT_SPECIFICATION;
    public static final String ACM_CONFORM = SYSML + JSON_CONFORM;
    public static final String ACM_DEPENDENCY = SYSML + JSON_DEPENDENCY;
    public static final String ACM_DIRECTED_RELATIONSHIP = SYSML + JSON_DIRECTED_RELATIONSHIP;
    public static final String ACM_ELEMENT = SYSML + JSON_ELEMENT;
    public static final String ACM_EXPOSE = SYSML + JSON_EXPOSE;
    public static final String ACM_GENERALIZATION = SYSML + JSON_GENERALIZATION;
    public static final String ACM_PACKAGE = SYSML + JSON_PACKAGE;
    public static final String ACM_PROPERTY = SYSML + JSON_PROPERTY;
    public static final String ACM_VIEWPOINT = SYSML + JSON_VIEWPOINT;
//    public static final String ACM_PARAMETER = SYSML + JSON_PARAMETER;
//    public static final String ACM_OPERATION = SYSML + JSON_OPERATION;
    public static final String ACM_IS_DERIVED = SYSML + JSON_IS_DERIVED;
    public static final String ACM_IS_SLOT = SYSML + JSON_IS_SLOT;
    public static final String ACM_DOCUMENTATION = SYSML + JSON_DOCUMENTATION;
    public static final String ACM_IDENTIFIABLE = SYSML + "Identifiable";
    public static final String ACM_ID = SYSML + JSON_ID;
    public static final String ACM_NAME = SYSML + JSON_NAME;
    public static final String ACM_SOURCE = SYSML + JSON_SOURCE;
    public static final String ACM_TARGET = SYSML + JSON_TARGET;
    public static final String ACM_VALUE_TYPE = SYSML + JSON_VALUE_TYPE;
    public static final String ACM_VALUE = SYSML + JSON_VALUE;
//    public static final String ACM_VALUE_EXPRESSION = SYSML + JSON_VALUE_EXPRESSION;
//    public static final String ACM_OWNER = SYSML + JSON_OWNER;
    public static final String ACM_TYPE = SYSML + JSON_TYPE;
    public static final String ACM_REIFIED_CONTAINMENT = SYSML + "reifiedContainment";
    public static final String ACM_VIEW = SYSML + "View";
    public static final String ACM_BODY = SYSML + JSON_BODY;
    public static final String ACM_EXPRESSION_BODY = SYSML + JSON_EXPRESSION_BODY;
    public static final String ACM_ANNOTATED_ELEMENTS = SYSML + JSON_ANNOTATED_ELEMENTS;
    
//    public static final String ACM_TIME_MAX = SYSML + JSON_TIME_MAX;    
//    public static final String ACM_TIME_MIN = SYSML + JSON_TIME_MIN;    
//    public static final String ACM_DURATION_MAX = SYSML + JSON_DURATION_MAX;    
//    public static final String ACM_DURATION_MIN = SYSML + JSON_DURATION_MIN;    
    
    
    public static final String ACM_ALLOWED_ELEMENTS = VIEW + JSON_ALLOWED_ELEMENTS;
    public static final String ACM_CHILDREN_VIEWS = VIEW + JSON_CHILDREN_VIEWS;
    public static final String ACM_CONTAINS = VIEW + JSON_CONTAINS;
    public static final String ACM_DISPLAYED_ELEMENTS = VIEW + JSON_DISPLAYED_ELEMENTS;
    public static final String ACM_NO_SECTIONS = VIEW + JSON_NO_SECTIONS;
    public static final String ACM_VIEW_2_VIEW = VIEW + JSON_VIEW_2_VIEW;
    public static final String ACM_PRODUCT = VIEW + "Product";
    
//    public static final String ACM_EXPRESSION = SYSML + JSON_EXPRESSION;
//    public static final String ACM_LITERAL_BOOLEAN = SYSML + JSON_LITERAL_BOOLEAN;
//    public static final String ACM_LITERAL_INTEGER = SYSML + JSON_LITERAL_INTEGER;
//    public static final String ACM_LITERAL_REAL = SYSML + JSON_LITERAL_REAL;
//    public static final String ACM_LITERAL_UNLIMITED_NATURAL = SYSML + JSON_LITERAL_UNLIMITED_NATURAL;
//    public static final String ACM_LITERAL_STRING = SYSML + JSON_LITERAL_STRING;

    public static final String ACM_BOOLEAN = SYSML + JSON_BOOLEAN;
    public static final String ACM_DOUBLE = SYSML + JSON_DOUBLE;
    public static final String ACM_INTEGER = SYSML + JSON_INTEGER;
    public static final String ACM_REAL = SYSML + JSON_REAL;
    public static final String ACM_NATURAL_VALUE = SYSML + JSON_NATURAL_VALUE;
    public static final String ACM_STRING = SYSML + JSON_STRING;

    
//    public static final String ACM_ELEMENT_VALUE = SYSML + JSON_ELEMENT_VALUE;
    public static final String ACM_PROPERTY_TYPE = SYSML + JSON_PROPERTY_TYPE;
    
    // Value spec additions
    public static final String ACM_VALUE_SPECIFICATION = SYSML + JSON_VALUE_SPECIFICATION;
    public static final String ACM_VALUE_EXPRESSION = SYSML + JSON_VALUE_EXPRESSION;
    public static final String ACM_DURATION = SYSML + JSON_DURATION;
    public static final String ACM_DURATION_INTERVAL = SYSML + JSON_DURATION_INTERVAL;
    public static final String ACM_DURATION_MAX = SYSML + JSON_DURATION_MAX;
    public static final String ACM_DURATION_MIN = SYSML + JSON_DURATION_MIN;
    public static final String ACM_ELEMENT_VALUE = SYSML + JSON_ELEMENT_VALUE;
    public static final String ACM_ELEMENT_VALUE_ELEMENT = SYSML + JSON_ELEMENT_VALUE_ELEMENT;
    public static final String ACM_EXPRESSION = SYSML + JSON_EXPRESSION;
    public static final String ACM_OPERAND = SYSML + JSON_OPERAND;
    public static final String ACM_INSTANCE_VALUE = SYSML + JSON_INSTANCE_VALUE;
    public static final String ACM_INSTANCE = SYSML + JSON_INSTANCE;
    public static final String ACM_INTERVAL = SYSML + JSON_INTERVAL;
    public static final String ACM_LITERAL_BOOLEAN = SYSML + JSON_LITERAL_BOOLEAN;
    public static final String ACM_LITERAL_INTEGER = SYSML + JSON_LITERAL_INTEGER;
    public static final String ACM_LITERAL_NULL = SYSML + JSON_LITERAL_NULL; 
    public static final String ACM_LITERAL_REAL = SYSML + JSON_LITERAL_REAL;
    public static final String ACM_LITERAL_UNLIMITED_NATURAL = SYSML + JSON_LITERAL_UNLIMITED_NATURAL;
    public static final String ACM_LITERAL_STRING = SYSML + JSON_LITERAL_STRING;
    public static final String ACM_OPAQUE_EXPRESSION = SYSML + JSON_OPAQUE_EXPRESSION;
    public static final String ACM_STRING_EXPRESSION = SYSML + JSON_STRING_EXPRESSION;
    public static final String ACM_TIME_EXPRESSION = SYSML + JSON_TIME_EXPRESSION;
    public static final String ACM_TIME_INTERVAL = SYSML + JSON_TIME_INTERVAL;
    public static final String ACM_TIME_INTERVAL_MAX = SYSML + JSON_TIME_INTERVAL_MAX;
    public static final String ACM_TIME_INTERVAL_MIN = SYSML + JSON_TIME_INTERVAL_MIN;
    public static final String ACM_OPERATION = SYSML + JSON_OPERATION;
    public static final String ACM_OPERATION_PARAMETER = SYSML + JSON_OPERATION_PARAMETER;
    public static final String ACM_INSTANCE_SPECIFICATION = SYSML + JSON_INSTANCE_SPECIFICATION;
    public static final String ACM_INSTANCE_SPECIFICATION_SPECIFICATION = SYSML + JSON_INSTANCE_SPECIFICATION_SPECIFICATION;
    public static final String ACM_PARAMETER = SYSML + JSON_PARAMETER;
    public static final String ACM_PARAMETER_DIRECTION = SYSML + JSON_PARAMETER_DIRECTION;
    public static final String ACM_PARAMETER_DEFAULT_VALUE = SYSML + JSON_PARAMETER_DEFAULT_VALUE;
 
    
    public static final String ACM_ELEMENT_FOLDER = SYSML + "ElementFolder";
    public static final String ACM_PROJECT = SYSML + "Project";

    public static final String ACM_LAST_MODIFIED = CM + "modified";
    public static final String ACM_AUTHOR = CM + "modifier";
    
    public static final String ACM_CM_NAME = CM + "name";
    public static final String ACM_CM_TITLE = CM + "title";
    
    
    /**
     *  JSON to Alfresco Content Model mapping
     */
    public static Map<String, String> JSON2ACM = null; //getJSON2ACM();
    /**
     *  Alfresco Content Model 2 JSON types
     */
    public static Map<String, String> ACM2JSON = null; //getACM2JSON();
    
    {init();}
    
        //private static final long serialVersionUID = -5467934440503910163L;
    public static Map<String, String> getACM2JSON() { 
        if ( ACM2JSON == null || ACM2JSON.size() <= 0 ) {
            init();
        }
        return ACM2JSON;
    }
    public static Map<String, String> getJSON2ACM() { 
        if ( JSON2ACM == null || JSON2ACM.size() <= 0 ) {
            init();
        }
        return JSON2ACM;
    }
    public static void init() { 
        try {
            ACM2JSON = new HashMap<String, String>();
            JSON2ACM = new HashMap<String, String>();
            for ( Field f : Acm.class.getDeclaredFields() ) {
                if ( f.getName().startsWith( "JSON_" ) ) {
                    String acmName = f.getName().replace( "JSON", "ACM" );
                    try {
                        Field f2 = Acm.class.getField( acmName );
                        
                        if ( f2 != null ) {
                            String jsonVal = (String)f.get(null);
                            String acmVal = (String)f2.get(null);
                            if ( !f.getName().equals("JSON_VALUE") ) {
                                JSON2ACM.put( jsonVal, acmVal);
                                if ( !f.getName().equals("JSON_VALUE_TYPE") ) {
                                    // this is parsed differently so don't include it
                                    ACM2JSON.put( acmVal, jsonVal);
                                }
                            }
                        }
                    } catch (Throwable t) {
                        if ( t instanceof NoSuchFieldException ) {
                            System.out.println( t.getLocalizedMessage() );
                        } else {
                            t.printStackTrace();
                        }
                    }
                }
            }
        } catch ( Throwable t ) {
            t.printStackTrace();
        }
    }
//            put(JSON_COMMENT, ACM_COMMENT);
//            put(JSON_CONFORM, ACM_CONFORM);
//            put(JSON_DEPENDENCY, ACM_DEPENDENCY);
//            put(JSON_DIRECTED_RELATIONSHIP, ACM_DIRECTED_RELATIONSHIP);
//            put(JSON_ELEMENT, ACM_ELEMENT);
//            put(JSON_EXPOSE, ACM_EXPOSE);
//            put(JSON_GENERALIZATION, ACM_GENERALIZATION);
//            put(JSON_PACKAGE, ACM_PACKAGE);
//            put(JSON_PROPERTY, ACM_PROPERTY);
//            put(JSON_VIEWPOINT, ACM_VIEWPOINT);
//            put(JSON_IS_DERIVED, ACM_IS_DERIVED);
//            put(JSON_IS_SLOT, ACM_IS_SLOT);
//            put(JSON_DOCUMENTATION, ACM_DOCUMENTATION);
//            put(JSON_ID, ACM_ID);
//            put(JSON_NAME, ACM_NAME);
//            put(JSON_SOURCE, ACM_SOURCE);
//            put(JSON_TARGET, ACM_TARGET);
//            put(JSON_VALUE_TYPE, ACM_VALUE_TYPE);
//            put(JSON_BODY, ACM_BODY);
//            
//            put(JSON_ALLOWED_ELEMENTS, ACM_ALLOWED_ELEMENTS);
//            put(JSON_CHILDREN_VIEWS, ACM_CHILDREN_VIEWS);
//            put(JSON_CONTAINS, ACM_CONTAINS);
//            put(JSON_DISPLAYED_ELEMENTS, ACM_DISPLAYED_ELEMENTS);
//            put(JSON_NO_SECTIONS, ACM_NO_SECTIONS);
//            put(JSON_VIEW_2_VIEW, ACM_VIEW_2_VIEW);
//            
//            put(JSON_EXPRESSION, ACM_EXPRESSION);
//            put(JSON_LITERAL_BOOLEAN, ACM_LITERAL_BOOLEAN);
//            put(JSON_LITERAL_INTEGER, ACM_LITERAL_INTEGER);
//            put(JSON_LITERAL_REAL, ACM_LITERAL_REAL);
//            put(JSON_LITERAL_STRING, ACM_LITERAL_STRING);
//            put(JSON_ELEMENT_VALUE, ACM_ELEMENT_VALUE);
    
//    /**
//     *  Alfresco Content Model 2 JSON types
//     */
//    public static final Map<String, String> ACM2JSON = new HashMap<String, String>() {
//        private static final long serialVersionUID = -4682311676740055702L;
//        {
//            try {
//                for ( Field f : Acm.class.getDeclaredFields() ) {
//                    if ( f.getName().startsWith( "ACM_" ) ) {
//                        String acmName = f.getName().replace( "ACM", "JSON" );
//                        try {
//                            Field f2 = Acm.class.getField( acmName );
//                            
//                            if ( f2 != null ) {
//                                put( (String)f.get(null), (String)f2.get(null));
//                            }
//                        } catch (Throwable t) {
//                            t.printStackTrace();
//                        }
//                    }
//                }
//            } catch ( Throwable t ) {
//                t.printStackTrace();
//            }
//            put(ACM_COMMENT, JSON_COMMENT);
//            put(ACM_CONFORM, JSON_CONFORM);
//            put(ACM_DEPENDENCY, JSON_DEPENDENCY);
//            put(ACM_DIRECTED_RELATIONSHIP, JSON_DIRECTED_RELATIONSHIP);
//            put(ACM_ELEMENT, JSON_ELEMENT);
//            put(ACM_EXPOSE, JSON_EXPOSE);
//            put(ACM_GENERALIZATION, JSON_GENERALIZATION);
//            put(ACM_PACKAGE, JSON_PACKAGE);
//            put(ACM_PROPERTY, JSON_PROPERTY);
//            put(ACM_VIEWPOINT, JSON_VIEWPOINT);
//            put(ACM_IS_DERIVED, JSON_IS_DERIVED);
//            put(ACM_IS_SLOT, JSON_IS_SLOT);
//            put(ACM_DOCUMENTATION, JSON_DOCUMENTATION);
//            put(ACM_ID, JSON_ID);
//            put(ACM_NAME, JSON_NAME);
//            put(ACM_SOURCE, JSON_SOURCE);
//            put(ACM_TARGET, JSON_TARGET);
////            put(ACM_VALUE_TYPE, JSON_VALUE_TYPE); // this is parsed differently so don't leave it in
//            put(ACM_BODY, JSON_BODY);
//            
//            put(ACM_ALLOWED_ELEMENTS, JSON_ALLOWED_ELEMENTS);
//            put(ACM_CHILDREN_VIEWS, JSON_CHILDREN_VIEWS);
//            put(ACM_CONTAINS, JSON_CONTAINS);
//            put(ACM_DISPLAYED_ELEMENTS, JSON_DISPLAYED_ELEMENTS);
//            put(ACM_NO_SECTIONS, JSON_NO_SECTIONS);
//            put(ACM_VIEW_2_VIEW, JSON_VIEW_2_VIEW);
//
//            put(ACM_LAST_MODIFIED, JSON_LAST_MODIFIED);
//            put(ACM_AUTHOR, JSON_AUTHOR);
//        }
//    };

    
    /**
     * Properties that are JSONArrays rather than primitive types, so parsing is differnt
     */
    protected static final Set<String> JSON_ARRAYS = new HashSet<String>() {
         private static final long serialVersionUID = -2080928480362524333L;
        {
            add(JSON_DISPLAYED_ELEMENTS);
            add(JSON_ALLOWED_ELEMENTS);
            add(JSON_CHILDREN_VIEWS);
            add(JSON_CONTAINS);
            add(JSON_VIEW_2_VIEW);
            add(JSON_NO_SECTIONS);
        }
    };
    
    /**
     * Properties that are always serialized in JSON
     */
    protected static final Set<String> COMMON_JSON = new HashSet<String>() {
        private static final long serialVersionUID = 8715041115029041344L;
        {
            add(JSON_ID);
            add(JSON_AUTHOR);
            add(JSON_LAST_MODIFIED);
        }
    };
    
    /**
     * Properties that are serialized when requesting Comments
     */
    public static final Set<String> COMMENT_JSON = new HashSet<String>() {
        private static final long serialVersionUID = -6102902765527909734L;
        {
            add(JSON_BODY);
            addAll(COMMON_JSON);
        }
    };
    
    /**
     * Properties that are serialized when when requesting Elements
     */
    protected static final Set<String> ELEMENT_JSON = new HashSet<String>() {
        private static final long serialVersionUID = -6771999751087714932L;
        {
            add(JSON_BODY);
            add(JSON_TYPE);
            add(JSON_NAME);
            add(JSON_DOCUMENTATION);
            add(JSON_PROPERTY_TYPE);
            add(JSON_IS_DERIVED);
            add(JSON_SOURCE);
            add(JSON_TARGET);
            add(JSON_PROPERTY_TYPE);

            add(JSON_OWNER);
            add(JSON_VALUE_TYPE);
            add(JSON_COMMENT);

            addAll(COMMON_JSON);
        }
    };
    
    /**
     * Properties that are serialized when requesting Products
     */
    protected static final Set<String> PRODUCT_JSON = new HashSet<String>() {
        private static final long serialVersionUID = 3335972461663141541L;
        {
            add(JSON_VIEW_2_VIEW);
            add(JSON_NO_SECTIONS);

            addAll(COMMON_JSON);
        }
    };
    
    /**
     * Properties that are serialized when requesting Views
     */
    protected static final Set<String> VIEW_JSON = new HashSet<String>() {
        private static final long serialVersionUID = -2080928480362524333L;
        {
            add(JSON_DISPLAYED_ELEMENTS);
            add(JSON_ALLOWED_ELEMENTS);
            add(JSON_CHILDREN_VIEWS);
            add(JSON_CONTAINS);
            addAll(COMMON_JSON);
        }
    };
    
    /**
     * Serialize all properties
     */
    public static final Set<String> ALL_JSON = new HashSet<String>() {
        private static final long serialVersionUID = 494169408514256049L;
        {
            addAll(COMMENT_JSON);
            addAll(ELEMENT_JSON);
            addAll(PRODUCT_JSON);
            addAll(VIEW_JSON);
        }
    };
    
    /**
     * Enumeration for specifying which JSON serialization property set to use
     *
     */
    public static enum JSON_TYPE_FILTER {
        VIEW, ELEMENT, PRODUCT, COMMENT, ALL;
     }

    /**
     * Map to filter the JSON keys for display purposes inside of EmsScriptNode
     */
    public static final Map<JSON_TYPE_FILTER, Set<String>> JSON_FILTER_MAP = new HashMap<JSON_TYPE_FILTER, Set<String>>() {
        private static final long serialVersionUID = -2080928480362524333L;
        {
            put(JSON_TYPE_FILTER.ALL, ALL_JSON);
            put(JSON_TYPE_FILTER.COMMENT, COMMENT_JSON);
            put(JSON_TYPE_FILTER.ELEMENT, ELEMENT_JSON);
            put(JSON_TYPE_FILTER.PRODUCT, PRODUCT_JSON);
            put(JSON_TYPE_FILTER.VIEW, VIEW_JSON);
        }
    };
}
