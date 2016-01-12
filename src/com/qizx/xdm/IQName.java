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
package com.qizx.xdm;

import com.qizx.api.DataModelException;
import com.qizx.api.QName;
import com.qizx.util.NamespaceContext;
import com.qizx.util.basic.XMLUtil;

import java.util.HashMap;

/**
 * Internal representation of a Qualified Name: Namespace + localName.
 * <p>
 * IQName has a unique representation: comparison can be
 * performed with ==; The prefix is always null.
 */
public final class IQName implements QName, java.io.Serializable
{
    /** the xml:lang qualified name */
    public static final IQName XML_LANG;

    /** the xml:space qualified name */
    public static final IQName XML_SPACE;

    /** the xml:base qualified name */
    public static final IQName XML_BASE;

    /** the xml:id qualified name */
    public static final IQName XML_ID;

    /** the xsi:type qualified name */
    public static final IQName XSI_TYPE;

    /** the xsi:nil qualified name */
    public static final IQName XSI_NIL;

    /** the xsi:schemaLocation qualified name */
    public static final IQName XSI_SCHEMA_LOCATION;

    /** the xsi:noNamespaceSchemaLocation qualified name */
    public static final IQName XSI_NO_NAMESPACE_SCHEMA_LOCATION;

    private static final QName LEX_ERR;

    public static final String NULL_NS = "";
    
    // ------------------------------------------------------------------------

    private static HashMap names = new HashMap();
    private static IQName probe = new IQName("", "");

    static {
        XML_ID = get(NamespaceContext.XML, "id");
        XML_LANG = get(NamespaceContext.XML, "lang");
        XML_SPACE = get(NamespaceContext.XML, "space");
        XML_BASE = get(NamespaceContext.XML, "base");
        XSI_TYPE = get(NamespaceContext.XSI, "type");
        XSI_NIL = get(NamespaceContext.XSI, "nil");
        XSI_SCHEMA_LOCATION = get(NamespaceContext.XSI, "schemaLocation");
        XSI_NO_NAMESPACE_SCHEMA_LOCATION = get(NamespaceContext.XSI,
                                               "noNamespaceSchemaLocation");
        LEX_ERR = IQName.get(NamespaceContext.ERR, "FOCA0002");
    }

    // ------------------------------------------------------------------------
    
    private String namespace;
    private String localName;

    // ------------------------------------------------------------------------

    /**
     * Obtains a unique representation of a QName from a namespace URI and a
     * NCname.
     */
    public static synchronized IQName get(String namespaceURI, String localName)
    {
        if (namespaceURI == null)
            throw new IllegalArgumentException("null namespace");
        if (localName == null)
            throw new IllegalArgumentException("null localName");

        probe.namespace = namespaceURI;
        probe.localName = localName;
        IQName name = (IQName) names.get(probe);
        if (name == null) {
            name = new IQName(namespaceURI, localName);
            names.put(name, name);
        }
        return name;
    }

    /**
     * Obtains a unique representation of a QName with empty namespace.
     */
    public static synchronized IQName get(String localName)
    {
        return get("", localName);
    }

    /**
     * Internalize any QName.
     */
    public static IQName get(QName name)
    {
        if (name == null)
            return null;
        if (name instanceof IQName)
            return (IQName) name;   // what is done is done
        if (name instanceof XQName)
            return ((XQName) name).getIQName();
        return IQName.get(name.getNamespaceURI(), name.getLocalPart());
    }
    
    public boolean equals(Object other)
    {
        if(this == other)
            return true;
        if (other instanceof QName) {
            QName n = (QName) other;
            return (namespace.equals(n.getNamespaceURI())
                    && localName.equals(n.getLocalPart()));
        }
        return false;
    }

    public int hashCode()
    {
        return (namespace.hashCode() ^ localName.hashCode());
    }

    // assumes component are interned
    private IQName(String namespace, String localName)
    {
        this.namespace = NamespaceContext.unique(namespace);
        this.localName = NamespaceContext.unique(localName);
    }
    
    // Deserialization must give unique instance
    Object readResolve() //throws ObjectStreamException
    {
        return get(namespace, localName);
    }

    public String getNamespaceURI()
    {
        return namespace;
    }

    public boolean hasNoNamespace()
    {
        return namespace.length() == 0;
    }
    
    public String getLocalPart()
    {
        return localName;
    }

    public String toString()
    {
        if (namespace == NamespaceContext.EMPTY)
            return localName;
        return '{' + namespace + '}' + localName;
    }

    public int compareTo(IQName other)
    {
        if (namespace == other.namespace)
            return localName.compareTo(other.localName);
        else return namespace.compareTo(other.namespace);
    }

    public String getPrefix()
    {
        return null;
    }
    
    /**
     * Extracts the prefix of a qualified name.
     */
    public static String extractPrefix(String name)
        throws DataModelException
    {
        int colon = name.indexOf(':');
        if (colon == 0)
            throw new DataModelException(LEX_ERR, "illegal QName syntax " + name);
        String prefix = colon < 0 ? "" : name.substring(0, colon);
        if (prefix.length() > 0 && !XMLUtil.isNCName(prefix))
            throw new DataModelException(LEX_ERR, "illegal prefix " + prefix);
        return prefix;
    }

    /**
     * Extracts the local-name of a qualified name.
     */
    public static String extractLocalName(String name)
        throws DataModelException
    {
        int colon = name.indexOf(':');
        if (colon == 0)
            throw new DataModelException(LEX_ERR, "invalid QName syntax " + name);
        String ncname = name.substring(colon + 1);
        if (!XMLUtil.isNCName(ncname))
            throw new DataModelException(LEX_ERR, "invalid local name " + ncname);
        return ncname;
    }


//    /**
//     * Use a custom intern(), because String.intern() is damn slow.
//     */
//    public static synchronized String intern(String namespaceURI)
//    {
//        if(namespaceURI == null)
//            return "";
//        String iuri = (String) namespaces.get(namespaceURI);
//        if(iuri == null)
//            namespaces.put(namespaceURI, iuri = namespaceURI);
//        return iuri;
//    }
}
