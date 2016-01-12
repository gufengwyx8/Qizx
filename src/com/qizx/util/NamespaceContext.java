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

import com.qizx.api.DataModelException;
import com.qizx.api.QName;
import com.qizx.api.QizxException;
import com.qizx.util.basic.Check;
import com.qizx.util.basic.XMLUtil;
import com.qizx.xdm.IQName;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Mapping of Namespace prefixes. Basically, this object maintains a stack of
 * prefix/Namespace pairs and provides lookup methods from prefix to Namespace
 * and conversely.
 * <p>
 * In addition, it manages "Levels" corresponding to the NS defined in a XML
 * element. A Level can be created then popped at once (all NS defined at this
 * level will be discarded).
 * <p>
 * "Local" prefixes or namespaces belong to the top Level.
 */
public class NamespaceContext 
    implements java.io.Serializable
{
    // MUST be at head!
    private static HashMap internedStrings = new HashMap();

    public static final String EMPTY = unique(""); 
    /** the XML namespace: http://www.w3.org/XML/1998/namespace */
    
    public static final String XML =
        unique("http://www.w3.org/XML/1998/namespace");

    /** The XLINK namespace: http://www.w3.org/1999/xlink */
    public static final String XLINK = unique("http://www.w3.org/1999/xlink");

    /** The XSLT namespace: http://www.w3.org/1999/XSL/Transform */
    public static final String XSLT =
        unique("http://www.w3.org/1999/XSL/Transform");

    /** The XSL namespace: "http://www.w3.org/1999/XSL/Format */
    public static final String XSL =
        unique("http://www.w3.org/1999/XSL/Format");

    /** The XML Schema Definition namespace: http://www.w3.org/2001/XMLSchema */
    public static final String XSD =
        unique("http://www.w3.org/2001/XMLSchema");

    /** The XSI namespace: http://www.w3.org/2001/XMLSchema-instance */
    public static final String XSI =
        unique("http://www.w3.org/2001/XMLSchema-instance");

    /** The XQuery functions namespace */
    public static final String FN =
        unique("http://www.w3.org/2005/xpath-functions");

    /** The XPath extended datatypes namespace */
    public static final String XDT =
        unique("http://www.w3.org/2005/xpath-datatypes");

    /** The XQuery operators namespace (extension) */
    public static final String OP =
        unique("http://www.w3.org/2005/xquery-operators");

    public static final String ERR =
        unique("http://www.w3.org/2005/xqt-errors");

    public static final String LOCAL_NS =
        unique("http://www.w3.org/2005/xquery-local-functions");
     
    public static final String OUTPUT_NS = 
         unique("http://www.w3.org/2009/xquery-serialization");


    // This simple implementation with linear search is sufficient if
    // we consider that rarely more than a few NS are defined in practice.
    private int size = 0;
    private String[] prefixes;      // interned for faster lookup
    private String[] namespaces;    // interned for faster lookup
    // separate stack of Levels:
    private int markPtr;
    private int[] marks;
    private int currentMark;

    public NamespaceContext()
    {
        prefixes = new String[8];
        namespaces = new String[prefixes.length];
        marks = new int[16];
    }

    public void clear()
    {
         size = 0;
         markPtr = 0;
         currentMark = 0;
    }

    public NamespaceContext copy()
    {
        NamespaceContext nm = new NamespaceContext();
        nm.size = size;
        nm.prefixes = (String[]) prefixes.clone();
        nm.namespaces = (String[]) namespaces.clone();
        nm.markPtr = markPtr;
        nm.currentMark = currentMark;
        nm.marks = (int[]) marks.clone();
        return nm;
    }

    /**
     * Marks a point corresponding to the beginning of an element. A
     * popMappings() will bring back to that point.
     */
    public void newLevel()
    {
        if (markPtr >= marks.length) {
            int[] old = marks;
            marks = new int[old.length * 2];
            System.arraycopy(old, 0, marks, 0, old.length);
        }
        marks[markPtr++] = currentMark;
        currentMark = size;
    }

    /**
     * Pops namespaces pushed since latest newLevel().
     */
    public void popLevel()
    {
        size = currentMark;
        if(markPtr > 0)
            currentMark = marks[--markPtr];
        else System.err.println("NamespaceContext.popLevel : OOPS");
    }

    /**
     * Adds a new prefix/namespaceURI mapping.
     * <p>
     * Caution: if a prefix is already defined at the same Level, it will be
     * overwritten.
     * <p>
     * A prefix may be the empty string (default namespace).
     * if namespaceURI is the empty string, it has the effect of "blanking" or
     * undefining locally the prefix.
     */
    public void addMapping(String prefix, String namespaceURI)
    {
        Check.nonNull(prefix, "prefix");
        Check.nonNull(namespaceURI, "namespaceURI");

        prefix  = unique(prefix);
        namespaceURI = unique(namespaceURI);
        if(namespaceURI == EMPTY)
            namespaceURI = null;    // to undefine NS
        int inf = currentMark, sup = size - 1;
        if(size > currentMark && prefixes[currentMark] == null)
            ++ inf;
        int mid = inf;
       
        while (inf <= sup) {
            mid = (inf + sup) / 2;
            int cmp = prefix.compareTo(prefixes[mid]);
            if (cmp < 0)
                sup = mid - 1;
            else if (cmp > 0)
                inf = mid + 1;
            else { // found
                namespaces[mid] = namespaceURI;
                return;
            }
        }
        // not found: insert at inf
        enlarge();

        int movedSize = size - inf;
        if(movedSize > 0) {
            System.arraycopy(prefixes, inf, prefixes, inf + 1, movedSize);
            System.arraycopy(namespaces, inf, namespaces, inf + 1, movedSize);
        }
        prefixes[inf] = prefix;
        namespaces[inf] = namespaceURI;
        ++size;
    }

    private void enlarge()
    {
        if (size >= prefixes.length) {
            String[] oldp = prefixes;
            prefixes = new String[2 * oldp.length];
            System.arraycopy(oldp, 0, prefixes, 0, oldp.length);
            String[] oldns = namespaces;
            namespaces = new String[prefixes.length];
            System.arraycopy(oldns, 0, namespaces, 0, oldns.length);
        }
    }

    /**
     * Returns a map of defined prefixes/namespaces, optionally erasing the NS.
     */
    public TreeMap getMappings(boolean erase)
    {
        TreeMap map = new TreeMap();
        for (int p = size; --p >= 0;) {
            String prefix = prefixes[p];
            if(prefix == null)  // previous are hidden 
                break;
            if (namespaces[p] != null && !map.containsKey(prefix))
                map.put(prefix, erase ? "" : namespaces[p]);
        }
        return map;
    }

    public String[] getPrefixes()
    {
        TreeMap prefs = getMappings(false);
        String[] result = new String[prefs.size()];
        int i = 0;
        for (Iterator iter = prefs.keySet().iterator(); iter.hasNext();) {
            result[i++] = (String) iter.next();
        }
        return result;
    }

    /**
     * Masks all mappings in the current level, by redefining them as null URIs
     */
    public void hideAllNamespaces()
    {
        TreeMap map = getMappings(true);
        for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
            String prefix = (String) iter.next();
            addMapping(prefix, "");
        }
        
////    doesnt work: to mask a NS, one needs to *redefine* prefix as blank URI
//        enlarge();
//        prefixes[size] = null;
//        namespaces[size] = null;
//        ++size;
    }

    /**
     * returns the number of defined mappings.
     */
    public int size()
    {
        return size;
    }

    public int mark() // ZAP
    {
        return currentMark;
    }

    /**
     * Returns the first prefix matching the Namespace.
     */
    public String getPrefix(String nsURI)
    {
        String ns = unique(nsURI);
        for (int p = 0; p < size; ++p) {
            String pref = prefixes[p];
            if(pref == null)    // hidden above
                return null;
            if (namespaces[p] == ns) {  // interned
                return prefixes[p];
            }
        }
        return null;
    }

    /**
     * Returns a declared prefix. The latest declared prefix has rank 1, the
     * previous 2 etc.
     */
    public String getPrefix(int index)
    {
        return prefixes[index];
    }

    /**
     * Returns the Namespace matching the prefix.
     */
    public String getNamespaceURI(String prefix)
    {
        for (int p = size; --p >= 0;) {
            String pref = prefixes[p];
            if(pref == null)    // hidden above
                break;
            if (prefix.equals(pref))
                // if undefined: returns null
                return namespaces[p];
        }
        return null;
    }

    /**
     * Returns a declared namespace. 
     */
    public String getNamespaceURI(int index)
    {
        return namespaces[index];
    }

    public int getLocalSize()
    {
        int ls = size - currentMark;
        if(ls > 0 && prefixes[currentMark] == null) // hidden
            return ls - 1;
        return ls;
    }

    /**
     * Returns the Nth local prefix.
     */
    public String getLocalPrefix(int index)
    {
        if(prefixes[currentMark] == null) // hidden
            ++ index;
        return prefixes[currentMark + index];
    }

    /**
     * Returns the Nth local namespace.
     */
    public String getLocalNamespaceURI(int index)
    {
        if(prefixes[currentMark] == null) // hidden
            ++ index;
        return namespaces[currentMark + index];
    }

    /**
     * Prefix defined at top level.
     */
    public boolean isPrefixLocal(String prefix)
    {
        for (int p = size; --p >= currentMark;) {
            String pref = prefixes[p];
            if(pref == null)    // hidden above
                break;
            if (pref.equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts a prefixed name into a QName, using the mappings defined here.
     * 
     * @return null if the prefix is not defined, the expanded QName otherwise.
     */
    public IQName expandName(String name)
        throws DataModelException
    {
        String prefix = IQName.extractPrefix(name);
        String ns = getNamespaceURI(prefix);
        if(ns == null && "".equals(prefix))
            ns = "";
        return ns == null ? null : IQName.get(ns, IQName.extractLocalName(name));
    }
    
    /**
     * Converts a QName into a prefixed name. If no suitable prefix is found,
     * return the QName in the form {namespaceURI}localName.
     */
    public String prefixedName(QName name)
    {
        String uri = name.getNamespaceURI();
        if(uri.length() == 0)
            return name.getLocalPart();
        String prefix = getPrefix(uri);
        return prefix != null 
                    ? (prefix + ":" + name.getLocalPart())
                    : name.toString();  // fallback
    }

    // About 5 times faster than String.intern()
    public static synchronized String unique(String s)
    {
        String r = (String) internedStrings.get(s);
        if(r != null)
            return r;
        internedStrings.put(s, s);
        return s;
    }
}
