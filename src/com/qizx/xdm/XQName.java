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

import com.qizx.api.QName;

import java.util.HashMap;

/**
 * EXternal representation of a QName.
 * Not guaranteed unique, comparison cannot be performed by ==.
 */
public class XQName
    implements QName
{
    private static HashMap prefixedNameCache = new HashMap();

    private IQName iname;
    private String prefix;

    public XQName(String namespaceURI, String localName, String prefix)
    {
        if (namespaceURI == null)
            throw new IllegalArgumentException("null namespace");
        if (localName == null)
            throw new IllegalArgumentException("null localName");
        this.iname = IQName.get(namespaceURI, localName);
        this.prefix = prefix;
        if(prefix == null)
           System.err.println("OOPS no prefix "+iname);
    }
    

    public String getLocalPart()
    {
        return iname.getLocalPart();
    }

    public String getNamespaceURI()
    {
        return iname.getNamespaceURI();
    }

    public String getPrefix()
    {
        return prefix;
    }
    
    public IQName getIQName()
    {
        return iname;
    }

    /**
     * Obtains a cached representation of a prefixed QName from a
     * QName and a prefix.
     */
    public static synchronized XQName get(QName name, String prefix)
    {
        XQName n = (XQName) prefixedNameCache.get(name);
        if(n != null && prefix != null) {
            String prefix2 = n.getPrefix();
            if(prefix2 != null && prefix.equals(prefix2))
                return n;
        }
        XQName res =
            new XQName(name.getNamespaceURI(), name.getLocalPart(), prefix);
        prefixedNameCache.put(res, res);
        return res;
    }

    public static synchronized XQName get(String namespaceURI, String localPart,
                                          String prefix)
    {
        return get(IQName.get(namespaceURI, localPart), prefix);
    }
    
    public String toString()
    {
        return prefix + ":" + iname;
    }

    // for hashing: equivalent to IQName
    public boolean equals(Object other)
    {
        if (other instanceof QName) {
            return iname == IQName.get((QName) other);
        }
        return false;
    }

    public int hashCode()
    {
        return iname.hashCode();
    }


    public boolean hasNoNamespace()
    {
        return iname.hasNoNamespace();
    }
}
