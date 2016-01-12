/*
 *    Qizx/open 4.1
 *
 * This code is the open-source version of Qizx.
 * Copyright (C) 2004-2009 Axyana Software -- All rights reserved.
 *
 * The contents of this file are subject to the Mozilla Public License 
 *  Version 1.1 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 * The Initial Developer of the Original Code is Xavier Franc - Axyana Software.
 *
 */
package com.qizx.api;

/**
 * Creates Items and related resources (ItemTypes and QNames).
 */
public interface ItemFactory
{
    /**
     * Returns a QName without namespace and without prefix.
     * @param localName a name without colon.
     * @return a QName instance
     */
    QName getQName(String localName);

    /**
     * Returns a QName without prefix.
     * @param localName a name without colon.
     * @param namespaceURI namespace URI
     * @return a QName instance
     */
    QName getQName(String localName, String namespaceURI);

    /**
     * Returns a QName with prefix.
     * @param localName a simple name without colon
     * @param namespaceURI namespace URI
     * @param prefix optional prefix, can be null.
     * @return a QName instance
     */
    QName getQName(String localName, String namespaceURI, String prefix);

    /**
     * Returns a predefined item Type from its name. To get a node type, use
     * {@link #getNodeType(int, QName)}.
     * <p>
     * The name may be a short name without the "xs:" prefix. For example
     * getType("xs:double") and getType("double") both return a representation
     * of the XQuery type xs:double.
     * @param name type name. Predefined short names: ENTITY, ID,
     *        IDREF, NCName, NMTOKEN, NOTATION, Name, QName, anyAtomicType,
     *        anySimpleType (item), anyURI, base64Binary, boolean, byte, date,
     *        dateTime, dayTimeDuration, decimal, double, duration, empty,
     *        float, gDay, gMonth, gMonthDay, gYear, gYearMonth, hexBinary,
     *        int, integer, language, long, negativeInteger, node,
     *        nonNegativeInteger, nonPositiveInteger, normalizedString,
     *        positiveInteger, short, string, time, token, unsignedByte,
     *        unsignedInt, unsignedLong, unsignedShort, untypedAtomic,
     *        yearMonthDuration
     * @return a representation of an XQuery item type
     */
    ItemType getType(String name);
    
    /**
     * Returns a node Type from a node-kind and an optional element or
     * attribute name.
     * @param nodeKind the node kind code: possible values are Node.ELEMENT,
     *        Node.ATTRIBUTE etc.
     *        <p>
     *        To get the type node(), use ItemType.NODE_TYPE as argument.
     * @param name optional name restricting the node type. For example
     *        factory.getNodeType(Node.ELEMENT, factory.getQName("x")) return
     *        the XQuery type <code>element(x)</code>.
     * @return a node type
     * @since 3.1
     */
    ItemType getNodeType(int nodeKind, QName name);
    

    /**
     * Creates an item from a given object according to the general Java to
     * XQuery type mapping, also used in the Java Binding extension.
     * 
     * @param value any object that can be converted to a XQuery Item.
     * @param type type of the created item. Can be null. If it is null, the
     *        type will be the "natural type" of the actual object (see the
     *        Java to XQuery Mapping specification in the Qizx documentation).
     * @return a new Item of the specified type.
     * @throws EvaluationException if the conversion was not possible.
     */
    Item createItem(Object value, ItemType type)
        throws EvaluationException;

    /**
     * Creates an Item of type xs:boolean.
     * @param value a boolean value
     * @return an Item of type xs:boolean
     */
    Item createItem(boolean value);

    /**
     * Creates an Item of type xs:double.
     * @param value a double value
     * @return an Item of type xs:double
     */
    Item createItem(double value);

    /**
     * Creates an Item of type xs:float.
     * @param value a float value
     * @return an Item of type xs:float
     */
    Item createItem(float value);

    /**
     * Creates an Item of type xs:integer, or one of its sub-types. If not
     * specified (null), the type is xs:integer.
     * 
     * @param value any long
     * @param type desired item type: must be null or xs:integer or a subtype
     *        of xs:integer.
     * @return an Item of the integer sub-type specified
     * @throws EvaluationException if the type is not a subtype of xs:integer.
     */
    Item createItem(long value, ItemType type)
        throws EvaluationException;

    /**
     * Creates a Document Node item by parsing a document.
     * @param source a SAX input source
     * @return a Node item
     * @throws EvaluationException wraps a parsing exception
     * @throws java.io.IOException if the source cannot be read
     */
    Item createItem(org.xml.sax.InputSource source)
        throws EvaluationException, java.io.IOException;

    /**
     * Creates a Node item by reading a XML stream.
     * <p>
     * The resulting item can be any kind of node, depending on the XML
     * event(s) encountered.
     * <p>
     * The source will be read until a complete Node is formed, but no further.
     * Therefore it is possible to call repeatedly this method on the same
     * source, provided it is correctly positioned initially.
     * @param source a XML pull stream
     * @return a Node item
     * @throws EvaluationException
     */
    Item createItem(XMLPullStream source)
        throws EvaluationException;

    /**
     * Creates a copy of the specified Sequence.
     * @param sequence an input sequence of items. The position is left
     *        untouched
     * @return a copy of the input sequence, at position 0
     * @throws EvaluationException if the enumeration of the input sequence
     *         caused an error
     */
    ItemSequence copySequence(ItemSequence sequence)
        throws EvaluationException;

    /**
     * Creates a sequence from a Java object.
     * <ol>
     * <li>A null object converts to an empty sequence.
     * <li>If the type is non-null, it will be used as an indication of the
     * result type:
     * <ul>
     * <li>A type equivalent to xdt:wrappedObject will produce a sequence made
     * of a single item (the wrapped object) whether it is an array or not.
     * <li>Otherwise if the object is an array, a <b>java.util.Iterator</b>,
     * a <b>java.util.Enumeration</b>, or a <b>java.util.Collection</b>, the
     * method builds a sequence by converting each element of the
     * array/collection etc. as per {@link #createItem(Object, ItemType)},
     * using the specified type. For example if the type argument is
     * xs:double*, the method will attempt to convert each item to a double.
     * <li>Otherwise (not enumerable) the resulting sequence will have one
     * item obtained by converting the object to the specified <i>item type</i>.
     * For example, passing a java.lang.String with a type xs:double, (or
     * xs:double?, xs:double*, xs:double+) generates a single item sequence,
     * where the item is obtained by converting the string to a double.
     * </ul>
     * <li>If the type is null, it will be inferred from the actual type of
     * the object. If the object is an array, the type will be
     * <i>component_type</i>*, where component_type is the mapping of the
     * array component type. For example, the type for an object of type
     * String[] will be xs:string*. <br>
     * If the object is a collection/iterator, the type is item()*. <br>
     * Then the conversion proceeds like in case 2.
     * </ol>
     * 
     * @param object Java object to convert
     * @param type optional type hint
     * @return a sequence of Items.
     * @throws EvaluationException
     */
    ItemSequence createSequence(Object object, SequenceType type)
        throws EvaluationException;
}
