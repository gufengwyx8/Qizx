/*
 *    Qizx/open 4.1
 *
 * This code is part of the Qizx application components
 * Copyright (C) 2004-2010 Axyana Software -- All rights reserved.
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
package com.qizx.apps.util;

import com.qizx.api.DataModelException;
import com.qizx.api.Item;
import com.qizx.api.Node;
import com.qizx.api.util.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A metadata property belonging to a LibraryMember.
 */
public class Property
{
    public static final String DATE_TIME = "dateTime";
    public static final String BOOLEAN = "boolean";
    public static final String DOUBLE = "double";
    public static final String INTEGER = "integer";
    public static final String STRING = "string";
    public static final String NODE = "node()";
    public static final String EXPRESSION = "<expression>";
    
    private static SimpleDateFormat dateFormat =
        new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");

    public String name;
    public String type;
    public String value;
    public Node   nodeValue;
    public Item   itemValue;
    
    public Property() {
    }
    
    /**
     * Build from a property returned by {@link LibraryMember}.
     * Convert actual value into a string, except if it is a Node.
     */
    public Property(String name, Object propValue) throws DataModelException
    {
        this.name = name;
        if(propValue instanceof Date) {
            value = new DateTime((Date) propValue, 0).toString();
            type = DATE_TIME;
        }
        else if(propValue instanceof Double) {
            type = DOUBLE;
        }
        else if(propValue instanceof Long || propValue instanceof Integer)
            type = INTEGER;
        else if(propValue instanceof Boolean)
            type = BOOLEAN;
        else if (propValue instanceof Node) {
            nodeValue = (Node) propValue;
            type = nodeValue.getNodeKind() + "()";
        }
        else
            type = STRING;
        if(nodeValue == null && value == null && propValue != null)
            this.value = propValue.toString();
    }
    
    public static boolean isNode(String type)
    {
        return type.endsWith("()");
    }

    /**
     * Converts back into an object acceptable par Library.setProperty
     */
    public Object toObject() throws Exception
    {
        if(nodeValue != null) {
            return nodeValue;
        }
        else if(itemValue != null) {
            return itemValue;
        }
        else if(type.equals(Property.DATE_TIME)) {
            return DateTime.parseDateTime(value.trim());
        }
        else if(type.equals(Property.INTEGER)) {
            return Long.valueOf(value.trim());
        }
        else if(type.equals(Property.DOUBLE)) {
            return Double.valueOf(value.trim());
        }
        else if(type.equals(Property.BOOLEAN)) {
            value = value.trim();
            if(value.equalsIgnoreCase("true"))
                return Boolean.TRUE;
            else if(value.equalsIgnoreCase("false"))
                return Boolean.FALSE;
        }
        else {
            return value;
        }
        return null;
    }
    
    public String toString()
    {
        return "Property('" + name + "', type=" + type + ")";
    }
}
