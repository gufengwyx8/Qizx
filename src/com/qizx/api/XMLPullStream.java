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
 * Streaming input of XML contents, working in 'pull' mode.
 * <p>
 * An object implementing this interface delivers the contents of an XML Node
 * as a stream of "events", enumerated under control of the calling code (hence
 * the term "pull mode"). An element appears as a pair of events ELEMENT_START,
 * ELEMENT_END enclosing the events corresponding to the contents of the
 * element. Atomic nodes (text, comment, PI) appear as a single event.
 * <p>
 * The stream is forward only. A method {@link #moveToNextEvent()} both moves
 * the stream to the next event and returns the type of this event
 * (ELEMENT_START, TEXT, ELEMENT_END etc.). The current event type is also
 * available through the method {@link #getCurrentEvent()}.
 * <p>
 * Other methods provide access to event-specific values such as element name,
 * attributes, namespace declarations, textual contents.
 * <p>
 * <b>Note:</b> this interface is similar to javax.xml.stream.XMLStreamReader
 * in essence, but it is fully compliant with the XQuery/XPath2 Data Model and
 * it is simpler. The iteration style is slightly different since there is no
 * hasNext() method.
 */
public interface XMLPullStream
{
    /**
     * Value returned by moveToNext and getEvent when the end of stream is
     * reached.
     */
    int END = -1;

    /**
     * Value returned by getEvent before the first call to moveToNext.
     */
    int START = 0;

    /**
     * Value returned by moveToNext and getEvent when the beginning of a
     * document is reached. From that point, the getEncoding() and getDTDxxx()
     * methods return meaningful values. This event is not returned if the
     * stream delivers the contents of a fragment of a document (element or
     * atomic contents).
     */
    int DOCUMENT_START = 1;

    /**
     * Value returned by moveToNext and getEvent when the end of the document
     * is reached. Always followed by END.
     */
    int DOCUMENT_END = 2;

    /**
     * Value returned by moveToNext and getEvent when the beginning of an
     * element is reached. Attributes and namespace declarations are available
     * on this event.
     */
    int ELEMENT_START = 3;

    /**
     * Value returned by moveToNext and getEvent when the end of an element is
     * reached. Attributes and namespace declarations are NOT available on this
     * event.
     */
    int ELEMENT_END = 4;

    /**
     * Value returned by moveToNext and getEvent when a text node is reached.
     * Adjacent text nodes are always coalesced.
     */
    int TEXT = 5;

    /**
     * Value returned by moveToNext and getEvent when a comment is reached.
     */
    int COMMENT = 6;

    /**
     * Value returned by moveToNext and getEvent when a processing-instruction
     * is reached.
     */
    int PROCESSING_INSTRUCTION = 7;

    /**
     * Moves the event stream one step forward.
     * @return the next event. If the stream has reached its end, returns
     *         {@link #END}.
     * @throws DataModelException may be thrown by the stream implementation in 
     * case access to data is impossible (deleted document, closed Library).
     */
    int moveToNextEvent()
        throws DataModelException;

    /**
     * Returns the same value as the latest moveToNextEvent.
     * @return an event code like in moveToNextEvent
     */
    int getCurrentEvent();

    /**
     * Returns the declared encoding of the document, if any. Valid from the
     * moment a DOCUMENT_START event is encountered.
     * @return the declared encoding, or null
     */
    String getEncoding();

    /**
     * Returns the declared DTD name of the document, if any. Valid from the
     * moment a DOCUMENT_START event is encountered.
     * @return the declared DTD name, or null
     */
    String getDTDName();

    /**
     * Returns the declared Public ID of the DTD, if any. Valid from the moment
     * a DOCUMENT_START event is encountered.
     * @return the declared DTD public id, or null
     */
    String getDTDPublicId();

    /**
     * Returns the declared System ID of the DTD, if any. Valid from the moment
     * a DOCUMENT_START event is encountered.
     * @return the declared DTD system id, or null
     */
    String getDTDSystemId();

    /**
     * Returns the source form of internal subset of the DTD, if any. Valid
     * from the moment a DOCUMENT_START event is encountered.
     * @return the internal subset, or null
     */
    String getInternalSubset();

    /**
     * Returns the name of the current element node, or if the node is not an
     * element, returns the name of the parent element.
     * @return the latest element name
     */
    QName getName();

    /**
     * On ELEMENT_START, returns the number of attributes of the element.
     * Otherwise returns -1.
     * @return the number of attributes of the current element, else -1
     */
    int getAttributeCount();

    /**
     * On ELEMENT_START, returns the name of the N-th attribute of the element.
     * @param index of the attribute (starting from 0)
     * @return name of the N-th attribute of the current element
     * @throws IndexOutOfBoundsException if not on an element start, of the
     *         index is invalid.
     */
    QName getAttributeName(int index);

    /**
     * On ELEMENT_START, returns the string-value of the N-th attribute of the
     * element.
     * @param index of the attribute (starting from 0)
     * @return value of the N-th attribute of the current element
     * @throws IndexOutOfBoundsException if not on an element start, of the
     *         index is invalid.
     */
    String getAttributeValue(int index);

    /**
     * On ELEMENT_START, returns the number of namespace declarations of the
     * element itself. Otherwise returns -1.
     * @return number of namespace declarations
     */
    int getNamespaceCount();

    /**
     * On ELEMENT_START, returns the prefix of the N-th namespace declaration
     * of the element.
     * @param index of the namespace declaration (starting from 0)
     * @return prefix of the N-th namespace of the current element
     * @throws IndexOutOfBoundsException if not on an element start, of the
     *         index is invalid.
     */
    String getNamespacePrefix(int index);

    /**
     * On ELEMENT_START, returns the namespace URI of the N-th namespace
     * declaration of the element.
     * @param index of the namespace declaration (starting from 0)
     * @return URI of the N-th namespace of the current element
     * @throws IndexOutOfBoundsException if not on an element start, of the
     *         index is invalid.
     */
    String getNamespaceURI(int index);

    /**
     * Returns the textual contents of an atomic node. On
     * PROCESSING_INSTRUCTION, returns the contents without the target name. On
     * element and document events, return null.
     * @return a String for the direct contents of the current leaf node
     */
    String getText();

    /**
     * Returns the size of the textual contents of an atomic node.
     * @return the number of characters
     * @see #getText
     */
    int getTextLength();

    /**
     * Returns the target name for a PROCESSING_INSTRUCTION.
     * @return a String which is the target name of the PI
     */
    String getTarget();

    /**
     * Returns the current node, if the implementation of this object is able to.
     * Otherwise the null value is returned.
     */
    Node getCurrentNode();
}
