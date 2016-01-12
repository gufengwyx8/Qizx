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
 * Qualified name for XML elements and attributes.
 * <p>
 * The {@link ItemFactory} interface is normally invoked for creating the
 * QName's used in the API. It is recommended to cache QNames when possible for
 * better performance.
 * <p>
 * Mandatory requirements for an implementation of this interface:
 * <ul>
 * <li>The hashCode() and equals() methods must be properly implemented.
 * </ul>
 */
public interface QName
{
    /**
     * Returns the local part of the qualified name.
     * @return a String representing the local part value
     */
    public String getLocalPart();

    /**
     * Returns the namespace URI of the QName. 
     * <p>
     * If the QName has no namespace, this value is the blank string (not null).
     * @return a String representing the namespace URI value
     */
    public String getNamespaceURI();

    /**
     * Returns an optional prefix associated with the QName (may be null).
     * @return a String representing the prefix value
     */
    public String getPrefix();

    /**
     * Returns true if this QName has the empty namespace URI.
     * @return true if this QName has the empty namespace URI.
     */
    public boolean hasNoNamespace();
}
