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
package com.qizx.util;

import com.qizx.api.QName;
import com.qizx.xdm.IQName;

import java.util.*;

/**
 * Management of a local table of qualified names (for documents or
 * collections).
 * <p>
 * Names are accessed by index or by value (namespace+localname).
 */
public class QNameTable
{
    // instance variables:
    protected ArrayList names_ = new ArrayList(32);
    protected HashMap nameMap_ = new HashMap();
    protected Key probe = new Key(null, null);

    /**
     * Searches a name and returns its index in the table. If the name is not
     * found, it is inserted.
     * 
     * @param uri Namespace URI.
     * @param localName
     */
    public int enter(String uri, String localName)
    {
        probe.uri = uri;
        probe.localName = localName;
        Key key = (Key) nameMap_.get(probe);
        if (key != null)
            return key.code;
        return addName(uri, localName);
    }

    public int enter(QName name)
    {
        return enter(name.getNamespaceURI(), name.getLocalPart());
    }

    /**
     * Lookup of a qualified name.
     * 
     * @return the index of the name in the table, or -1 if not found.
     */
    public int find(String uri, String localName)
    {
        probe.uri = uri;
        probe.localName = localName;
        Key key = (Key) nameMap_.get(probe);
        return key != null ? key.code : -1;
    }

    /**
     * Lookup of a qualified name.
     * 
     * @return the index of the name in the table, or -1 if not found.
     */
    public int find(QName name)
    {
        return find(name.getNamespaceURI(), name.getLocalPart());
    }

    /**
     * Inserts a name without check (for loading).
     */
    public int addName(String uri, String localName)
    {
        Key key = new Key(uri, localName);
        key.code = names_.size();
        names_.add(IQName.get(uri, localName));
        nameMap_.put(key, key);
        // System.out.println("addName "+uri+" "+localName+" "+key.code);
        return key.code;
    }

    /**
     * Gets the unique name associated with an index.
     */
    public IQName getName(int rank)
    {
        if (rank < 0 || rank >= names_.size())
            return null;
        return (IQName) names_.get(rank);
    }

    /**
     * Returns the number of names stored in this table.
     */
    public int size()
    {
        return names_.size();
    }

    /**
     * Clears all the contents.
     */
    public void clear()
    {
        names_.clear();
        nameMap_ = new HashMap();
    }

    public String[] getNamespaces()
    {
        Vector nst = new Vector();
        for (int n = names_.size(), ns; --n >= 0;) {
            String ens = getName(n).getNamespaceURI();
            for (ns = nst.size(); --ns >= 0;)
                if (ens == nst.elementAt(ns))
                    break;
            if (ns < 0)
                nst.addElement(ens);
        }
        return (String[]) nst.toArray(new String[nst.size()]);
    }

    static class Key
    {
        String uri;
        String localName;

        int code;

        Key(String uri, String localName)
        {
            this.uri = uri;
            this.localName = localName;
        }

        public int hashCode()
        {
            return uri.hashCode() ^ localName.hashCode();
        }

        public boolean equals(Object other)
        {
            if (other == null || !(other instanceof Key))
                return false;
            Key n = (Key) other;
            // System.out.println("equals {"+uri+"}"+localName+"
            // to{"+n.uri+"}"+n.localName);
            return uri.equals(n.uri) && localName.equals(n.localName);
        }
    }
}
