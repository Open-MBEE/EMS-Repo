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
package gov.nasa.jpl.view_repo;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;

/**
 * A generic interface for talking to models.  Looking to be sufficient for simplified SysML (without UML).
 * REVIEW -- What else might this need to be compatible with other things, like CMIS, OSLC, EMF, etc.  
 */
public interface ModelInterface<E, C, T, P, N, I, R, V> {
    public enum ModelItem { ELEMENT, CONTEXT, TYPE, PROPERTY, NAME, IDENTIFIER, VALUE, RELATIONSHIP, VERSION };
    public enum Operation { GET, CREATE, UPDATE, DELETE };

    public class Item {
        public ModelItem kind;
        public Object obj;
        public Item(Object obj, ModelItem kind) { this.kind = kind; this.obj = obj; }
    }
    
    /**
     * Perform an Operation on something as specified by the input arguments.
     * Null values are interpreted as "unknown," "don't care," or
     * "not applicable." Multiple specifiers of the same kind of ModelItem are
     * interpreted as "or." For example,
     * {@code specifier = (("Fred", NAME), ("Wilma",
     * NAME))} means that the name may be either "Fred" or "Wilma."
     * <p>
     * Examples:
     * <ol>
     * <li> {@code op(GET, (ELEMENT), null, ("123", IDENTIFIER), null)} returns
     * the element(s) with ID = "123."
     * <li>
     * {@code op(UPDATE, (PROPERTY), ((o1, ELEMENT),(o2, ELEMENT)), (("mass", NAME), ("kg", TYPE)), 1.0)}
     * returns a collection of the "mass" properties of o1 and o2 with values
     * updated to 1.0kg.
     * <li>
     * {@code op(CREATE, (VERSION), ((v1, VERSION)), (("v2", IDENTIFIER)), v1)}
     * creates and returns a new version "v2" that is a copy of v1 and
     * follows/branches v1.
     * </ol>
     * 
     * @param operation
     *            whether to get, create, delete, or update the item
     * @param itemTypes
     *            the kind of item it may be
     * @param context
     *            the items within which the operation is performed
     * @param specifier
     *            possible characteristics of the item
     * @param newValue
     *            a new value for the item, as applicable for operation = CREATE
     *            or UPDATE
     * @param failForMultipleItemMatches
     *            if true and multiple items are identified by the specifier for
     *            a GET, UPDATE, or DELETE operation, then do not perform the
     *            operation, and return null.
     * @return the item(s) specified in a collection or null if the operation is
     *         prohibited or inconsistent. See {@link isAllowed}.
     */
    public Collection< Object > op( Operation operation,
                                    Collection< ModelItem > itemTypes,
                                    Collection< Item > context,
                                    Collection< Item > specifier,
                                    Object newValue,
                                    Boolean failForMultipleItemMatches );

    /**
     * Specifies whether it is feasible to call op() with the non-null
     * arguments. A null argument here is interpreted as "some." The newValue
     * argument is an Item instead of an Object (as in op()) in order to test
     * whether a certain kind of value can be assigned.
     * <p>
     * Examples:
     * <ol>
     * <li> {@code isAllowed(GET, (VERSION), null, null, null)} returns true iff
     * the ModelInterface supports getting versions.
     * <li> {@code isAllowed(UPDATE, (PROPERTY), ((null, TYPE)), null, null)}
     * returns true iff the ModelInterface supports updating type properties.
     * <li>
     * {@code isAllowed(CREATE, (VERSION), ((null, VERSION)), (("x y", IDENTIFIER)), (null, VERSION))}
     * returns true iff the ModelInterface supports creating and copying a
     * version from the context of a version and specifying its identifier as
     * "x y". This may return false for "x y" and true for "xy" if spaces are
     * not allowed in identifiers.
     * <li> {@code isAllowed(null, (IDENTIFIER), null, null, ("x y", VALUE))}
     * returns true iff "x y" is a legal identifier.
     * <li> {@code isAllowed(CREATE, (ELEMENT), null, null, (null, TYPE))}
     * returns true iff an element can be assigned a type.
     * </ol>
     * 
     * @param operation
     *            whether to get, create, delete, or update the item
     * @param itemTypes
     *            the kind of item it may be
     * @param context
     *            the items within which the operation is performed
     * @param specifier
     *            possible characteristics of the item
     * @param newValue
     *            a new value for the item, as applicable for operation = CREATE
     *            or UPDATE
     * @param failForMultipleItemMatches
     *            if true and multiple items are identified by the specifier for
     *            a GET, UPDATE, or DELETE operation, then return false.
     * @return whether some operations of the kinds specified by the arguments
     *         are consistent, legal, and feasible.
     */
     public boolean isAllowed( Operation operation,
                               Collection< ModelItem > itemTypes,
                               Collection< Item > context,
                               Collection< Item > specifier,
                               Item newValue,
                               Boolean failForMultipleItemMatches );

    /**
     * Either get, create, or delete something as specified by the input
     * arguments. Null values are interpreted as "unknown," "don't care," or
     * "not applicable." There may be multiple
     * 
     * @param op
     *            whether to get, create, or delete the item
     * @param itemTypes
     *            the kind of item
     * @param context
     *            the objects within which the operation is performed
     * @param identifier
     *            the identifier of the item
     * @param name
     * @param version
     * @return
     */
    public Collection< Object > op( Operation operation,
                                    Collection< ModelItem > itemTypes,
                                    Collection< C > context,
                                    I identifier,
                                    N name,
                                    V version,
                                    boolean failForMultipleItemMatches );

    // general functions that may implement others
    /**
     * Get the model item(s) identified by or matching the input arguments.  Null values are interpreted as "unknown," "don't care," or "not applicable."  
     * @param kindOfItem the item(s) must be of one of these specified kinds
     * @param context the elements to which the sought item(s),
     * @param identifier 
     * @param name
     * @param version
     * @return the matching items
     */
    public Collection<Object> get( Collection<ModelItem> itemTypes, Collection<C> context, I identifier, N name, V version );
    public Collection<Object> create( ModelItem item, Collection<C> context, I identifier, N name, V version );
    public Collection<Object> delete( ModelItem item, Collection<C> context, I identifier, N name, V version );
    // TODO -- update args and add update() and maybe copy()/clone()

    // accessors for class/object/element
    public E get( C context, I identifier, V version );
    public Collection<E> getRootObjects( V version );
    public I getObjectId( E element, V version );
    public N getName( E element, V version );
    public T getTypeOf( E element, V version );
    public T getType( C context, N name, V version );
    public Collection<P> getTypeProperties( T type, V version );
    public Collection<P> getProperties( E element, V version );
    public P getProperty( E element, N propertyName, V version );
    public Collection<R> getRelationships( E element, V version );
    public Collection<R> getRelationships( E element, N relationshipName, V version );
    public Collection<E> getRelated( E element, N relationshipName, V version );

    public boolean latestVersion( Collection<C> context );

    // general edit policies
    public boolean idsAreSettable();
    public boolean namesAreSettable();
    public boolean elementsMayBeChangedForVersion( V version );
    public boolean typesMayBeChangedForVersion( V version );
    public boolean propertiesMayBeChangedForVersion( V version );
    public boolean elementsMayBeCreatedForVersion( V version );
    public boolean typesMayBeCreatedForVersion( V version );
    public boolean propertiesMayBeCreatedForVersion( V version );
    public boolean elementsMayBeDeletedForVersion( V version );
    public boolean typesMayBeDeletedForVersion( V version );
    public boolean propertiesMayBeDeletedForVersion( V version );

    // create fcns
    // TODO
    public E createObject( I identifier, V version );
    public boolean setIdentifier( E element, V version );
    public boolean setName( E element, V version );
    public boolean setType( E element, V version );

    // delete fcns
    // TODO
    E deleteObject( I identifier, V version );
    T deleteType( E element, V version );
	
	// query functions
    /**
     * Apply the method to each of the elements and return results. Subclasses
     * implementing map() may employ utilities for functional Java provided in
     * FunctionalUtils (TODO).
     * 
     * @param elements
     * @param method
     *            Java method with a parameter that is or extends E (element).
     *            Imagine that this method is a call to op() or a call to a
     *            custom function that includes calls to various ModelInterface
     *            methods.
     * @param indexOfElementArgument
     *            where in the list of arguments an element from the collection
     *            belongs (0 to total number of args - 1)
     * @param otherArguments
     *            arguments to be combined with an element in the collection in
     *            a method call
     * @return null if the method call returns void; otherwise, a return value
     *         for each element
     * @throws java.lang.reflect.InvocationTargetException
     */
    public Collection< Object > map( Collection< E > elements,
                                     Method method,
                                     int indexOfElementArgument,
                                     Object... otherArguments )
                                             throws java.lang.reflect.InvocationTargetException;
    
    /**
     * Filter out the elements for which the method does not return true.
     * Subclasses implementing filter() may employ utilities for functional Java
     * provided in FunctionalUtils (TODO).
     * 
     * @param elements
     * @param method
     *            Java method with a parameter that is or extends E (element)
     *            and a return value that can be interpreted as a Boolean by
     *            utils.isTrue().
     * @param indexOfElementArgument
     *            where in the list of arguments an element from the collection
     *            belongs (0 to total number of args - 1)
     * @param otherArguments
     *            arguments to be combined with an element in the collection in
     *            a method call
     * @return null if the function returns void; otherwise, a return value for
     *         each element
     * @throws java.lang.reflect.InvocationTargetException
     */
    public Collection< Object > filter( Collection< E > elements,
                                        Method method,
                                        int indexOfElementArgument,
                                        Object... otherArguments )
                                                throws java.lang.reflect.InvocationTargetException;

    /**
     * Check whether the method returns true for each element. Subclasses
     * implementing forAll() may employ utilities for functional Java provided
     * in FunctionalUtils (TODO).
     * 
     * @param elements
     * @param method
     *            Java method with a parameter that is or extends E (element)
     *            and a return value that can be interpreted as a Boolean by
     *            utils.isTrue().
     * @param indexOfElementArgument
     *            where in the list of arguments an element from the collection
     *            belongs (0 to total number of args - 1)
     * @param otherArguments
     *            arguments to be combined with an element in the collection in
     *            a method call
     * @return true iff all method calls can clearly be interpreted as true
     *         (consistent with Utils.isTrue())
     * @throws java.lang.reflect.InvocationTargetException
     */
    public boolean forAll( Collection< E > elements,
                           Method method,
                           int indexOfElementArgument,
                           Object... otherArguments )
                                   throws java.lang.reflect.InvocationTargetException;

    /**
     * Check whether the method returns true for some element. Subclasses
     * implementing thereExists() may employ utilities for functional Java
     * provided in FunctionalUtils (TODO).
     * 
     * @param elements
     * @param method
     *            Java method with a parameter that is or extends E (element)
     *            and a return value that can be interpreted as a Boolean by
     *            utils.isTrue().
     * @param indexOfElementArgument
     *            where in the list of arguments an element from the collection
     *            belongs (0 to total number of args - 1)
     * @param otherArguments
     *            arguments to be combined with an element in the collection in
     *            a method call
     * @return true iff any method call return value can clearly be interpreted
     *         as true (consistent with Utils.isTrue())
     * @throws java.lang.reflect.InvocationTargetException
     */
    public boolean thereExists( Collection< E > elements,
                                Method method,
                                int indexOfElementArgument,
                                Object... otherArguments )
                                        throws java.lang.reflect.InvocationTargetException;

    /**
     * Inductively combine the results of applying the method to each of the
     * elements and the return results for the prior element. Subclasses 
     * implementing fold() may employ utilities for functional Java provided in
     * FunctionalUtils (TODO).
     * <p>
     * For example, fold() is used below to sum an array of numbers.<br>
     * {@code int plus(int a, int b) ( return a+b; )} <br>
     * {@code int[] array = new int[] ( 2, 5, 6, 5 );}<br>
     * {@code int result = fold(Arrays.asList(array), 0.0, ClassUtils.getMethodForName(this.getClass(), "plus"), 0, 1, null); // result = 18}
     * 
     * @param elements
     * @param initialValue
     *            An initial value to act as the first argument to first
     *            invocation of the method
     * @param method
     *            Java method with two or more parameters, one of which can be
     *            assigned a value of the same type as the method's return type
     *            and another which is or extends E (element)
     * @param indexOfElementArgument
     *            where in the list of arguments an element from the collection
     *            belongs (0 to total number of args - 1)
     * @param indexOfPriorResultArgument
     *            where in the list of arguments the prior result value
     *            belongs (0 to total number of args - 1)
     * @param otherArguments
     *            arguments to be combined with an element in the collection in
     *            a method call
     * @return the result of calling the method on the last element after
     *         calling the method on each prior element (in order), passing the
     *         prior return value into the call on each element.
     * @throws java.lang.reflect.InvocationTargetException
     */
    public Object fold( Collection< E > elements,
                        Object initialValue,
                        Method method,
                        int indexOfElementArgument,
                        int indexOfPriorResultArgument,
                        Object... otherArguments )
                                throws java.lang.reflect.InvocationTargetException;
    
    /**
     * Apply the method to each of the elements and return results. Subclasses
     * implementing sort() may employ utilities for functional Java provided in
     * FunctionalUtils (TODO).
     * 
     * @param elements to be sorted
     * @param comparator specifies precedence relation on a pair of return values
     * @param method
     *            Java method with a parameter that is or extends E (element)
     * @return the input elements in a new Collection sorted according to the method and comparator
     * @throws java.lang.reflect.InvocationTargetException
     */
    public Collection< E > sort( Collection< E > elements,
                                 Comparator< ? > comparator,
                                 Method method,
                                 int indexOfElementArgument,
                                 Object... otherArguments )
                                         throws java.lang.reflect.InvocationTargetException;     

}
