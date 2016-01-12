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

import java.math.BigDecimal;

/**
 * An Item of a Sequence returned by the evaluation of an XQuery Expression.
 */
public interface Item
{
    /**
     * Returns the XQuery type of the item.
     * @return XQuery type of the item
     * @throws EvaluationException if access to the item is not possible
     */
    ItemType getType()
        throws EvaluationException;

    /**
     * Gets the item's boolean value.
     * @return the boolean value. If the item is a node, an attempt to convert
     *         its string value is made.
     * @throws EvaluationException if the item cannot be accessed or is not
     *         boolean and not convertible
     */
    boolean getBoolean()
        throws EvaluationException;

    /**
     * Gets the item's float value.
     * @return the float value. If the item is a node, an attempt to convert
     *         its string value is made.
     * @throws EvaluationException if the item cannot be accessed or is not
     *         float and not convertible
     */
    float getFloat()
        throws EvaluationException;

    /**
     * Gets the item's double value.
     * @return the double value. If the item is a node, an attempt to convert
     *         its string value is made.
     * @throws EvaluationException if the item cannot be accessed or is not
     *         double and not convertible
     */
    double getDouble()
        throws EvaluationException;

    /**
     * Gets the item's integer value.
     * @return the long integer value. If the item is a node, an attempt to
     *         convert its string value is made.
     * @throws EvaluationException if the item cannot be accessed or is not
     *         integer and not convertible
     */
    long getInteger()
        throws EvaluationException;

    /**
     * Gets the item's decimal value.
     * @return the decimal value as a BigDecimal. If the item is a node, an
     *         attempt to convert its string value is made.
     * @throws EvaluationException if the item cannot be accessed or is not
     *         decimal and not convertible
     */
    BigDecimal getDecimal()
        throws EvaluationException;

    /**
     * Gets the item's String value.
     * @return the string value
     * @throws EvaluationException if the item cannot be accessed
     */
    String getString()
        throws EvaluationException;

    /**
     * Gets the item's QName value.
     * @return the QName value
     * @throws EvaluationException if the item cannot be accessed or is not a QName
     */
    QName getQName()
        throws EvaluationException;

    /**
     * Tests whether the item is a Node.
     * @return true if the item is an accessible node
     */
    boolean isNode();

    /**
     * Gets the item's Node value.
     * @return the item as a Node
     * @throws EvaluationException if the item cannot be accessed or is not a Node
     */
    Node getNode()
        throws EvaluationException;

    /**
     * Streaming export of a Node, in "pull" mode.
     * @return a XMLPullStream that delivers the contents of the node item.
     * @throws EvaluationException if the item cannot be accessed or is not a node
     */
    XMLPullStream exportNode()
        throws EvaluationException;

    /**
     * Streaming export of a Node in "push" mode. Can use for example
     * XMLSerializer, PushStreamToSAX, PushStreamToDOM, or a custom adapter to
     * any other representation.
     * @param writer a push stream output
     * @throws QizxException if not a node, or not accessible, or
     *         a DataModelException is thrown by the writer.
     */
    void export(XMLPushStream writer)
        throws QizxException;

    /**
     * Converts the item value to a Java object, according to its actual type.
     * Nodes are converted to w3c DOM nodes.
     * <p>The precise mapping of XQuery types to Java types is described in
     * the documentation of <a href='../../../java_binding.html'>Java Binding</a>.
     * @return the item converted into a Java object
     * @throws QizxException if the item cannot be accessed or if the conversion fails
     */
    Object getObject()
        throws QizxException;
}
