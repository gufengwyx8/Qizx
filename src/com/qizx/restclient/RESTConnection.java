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
package com.qizx.restclient;

import com.qizx.api.DataModelException;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.apps.util.Property;
import com.qizx.xdm.IQName;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


/**
 * Low-level HTTP client API to Qizx REST Server.
 * <p>
 * Implemented on top of Java HttpURLConnection, uses 
 */
public class RESTConnection extends ClientConnection
{
    public static final QName NAME = IQName.get("name");
    public static final QName TYPE = IQName.get("type");
    public static final QName PROPERTY = IQName.get("property");
    
    protected String libName;
    public RESTConnection(String url) throws MalformedURLException
    {
        setBaseURL(url);
    }
    
    protected void startGet(String op, String path)
    {
        super.startGet();
        curGet.setParameter("op", op);
        if(path != null)
            curGet.setParameter("path", path);
        if(libName != null)
            curGet.setParameter("library", libName);
    }

    protected void startPost(String op, String path)
    {
        super.startPost();
        curPost.setParameter("op", op);
        if(path != null)
            curPost.setParameter("path", path);
        if(libName != null)
            curPost.setParameter("library", libName);
    }


    public void setLibraryName(String name)
    {
        libName = name;
    }

    public List<Property> nextPropSet()
        throws DataModelException
    {
        if (!nextResultNode())
            return null;
        // get properties
        return getProperties(curNode);
    }

    public Node nextItem()
        throws DataModelException
    {
        if (!nextResultNode())
            return null;
        return curNode;
    }

    private boolean nextResultNode()
        throws DataModelException
    {
        if (curNode == null) {
            getNode();
            if (curRoot == null)
                return false;
            curNode = curRoot.getFirstChild();
            if (curNode != null)
                curNode = curNode.getFirstChild();
        }
        else {
            curNode = curNode.getNextSibling();
        }
        return curNode != null;
    }

    // ---------- requests ---------------------------------------

    public void login()
        throws IOException
    {
        startGet("server", null);
        curGet.setParameter("command", "status");
        send();
        gotError();
        String r = getNextLine();
        if (!"online".equals(r) && !"offline".equals(r))
            throw new IOException("invalid response from REST server: " + r);
    }

    public void get(String path)
        throws IOException
    {
        get(path, null);
    }

    public void get(String path, String options)
        throws IOException
    {
        startGet("get", path);
        if(options != null)
            curGet.setParameter("options", options);
        send();
    }

    public void eval(String query, String format)
        throws IOException
    {
        startPost("eval", null);
        curPost.setParameter("format", format);
        curPost.setParameter("query", query);
        send();
    }

    public void evalAsItems(String query, int itemCount, int firstItem)
        throws IOException
    {
        eval(query, "items", null, -1, itemCount, firstItem);
    }

    public void eval(String query, String format, String encoding,
                     int maxTime, int itemCount, int firstItem)
        throws IOException
    {
        startPost("eval", null);
        curPost.setParameter("query", query);
        curPost.setParameter("format", format);
        if (encoding != null)
            curPost.setParameter("encoding", encoding);
        if (itemCount > 0)
            curPost.setParameter("count", Integer.toString(itemCount));
        curPost.setParameter("first", Integer.toString(firstItem));
        if (maxTime > 0)
            curPost.setParameter("maxtime", Integer.toString(maxTime));
        send();
    }

    public void startPut()
    {
        startPost("put", null);
        paramRank = 0;
    }

    public void startPutNonXML()
    {
        startPost("putnonxml", null);
        paramRank = 0;
    }

    public void addDocument(String path, File contents, String contentType)
        throws IOException
    {
        if(curPost == null)
            throw new IOException("no startPut");
        String pathParam = rankedParam("path");
        curPost.addParameter(pathParam, path);
        curPost.addParameter(rankedParam("data"), contents, contentType);
        paramRank = Math.max(2, paramRank + 1);        
    }

    public void addDocument(String path, String contents) throws IOException
    {
        if(curPost == null)
            throw new IOException("no startPut");
        curPost.addParameter(rankedParam("path"), path);
        curPost.addParameter(rankedParam("data"), contents);
        paramRank = Math.max(2, paramRank + 1);
    }

    public String createCollection(String path, boolean createParents)
        throws IOException
    {
        startPost("mkcol", path);
        curPost.setParameter("parents", createParents);
        send();
        return getNextLine();
    }

    public String move(String src, String dst)
        throws IOException
    {
        startPost("move", null);
        curPost.setParameter("src", src);
        curPost.setParameter("dst", dst);
        send();
        return getNextLine();
    }

    public String copy(String src, String dst)
        throws IOException
    {
        startPost("copy", null);
        curPost.setParameter("src", src);
        curPost.setParameter("dst", dst);
        send();
        return getNextLine();
    }

    public boolean delete(String path)
        throws IOException
    {
        startPost("delete", path);
        send();
        String s = getNextLine();
        return s != null && s.length() > 0;
    }

    public void getProp(String path, int depth, String[] properties)
        throws IOException
    {
        startGet("getprop", path);
        curGet.setParameter("depth", Integer.toString(depth));
        if (properties != null)
            curGet.setParameter("properties", nameList(properties));
        send();
    }

    public void startSetProp(String path, String name, String type, String value)
    {
        startPost("setprop", path);
        paramRank = 0;
        addProperty(name, type, value);
    }

    public void addProperty(String name, String type, String value)
    {
        curPost.addParameter(rankedParam("name"), name);
        if (type != null)
            curPost.addParameter(rankedParam("type"), type);
        if(value != null)
            curPost.addParameter(rankedParam("value"), value);
        ++paramRank;
    }

    public void queryProp(String path, String query, String[] properties)
        throws IOException
    {
        startGet("queryprop", path);
        if (query != null)
            curGet.setParameter("query", query);
        if (properties != null)
            curGet.setParameter("properties", nameList(properties));
        send();
    }

    public List<Property> info()
        throws DataModelException, IOException
    {
        startGet("info", null);
        send();
        getNode();
        if (curRoot == null)
            return null;
        curNode = curRoot.getFirstChild();
        return getProperties(curNode);
    }
    
    private List<Property> getProperties(Node props)
        throws DataModelException
    {
        ArrayList<Property> list = new ArrayList<Property>();
        Node child = props.getFirstChild();
        for (; child != null; child = child.getNextSibling()) {
            if(child.getNodeName() == null)
                continue;
            Node attr = child.getAttribute(NAME);
            if (attr == null || child.getNodeName() != PROPERTY)
                throw new DataModelException("improper property node " + child);
            Property prop = new Property();
            prop.name = attr.getStringValue();
            Node typeAttr = child.getAttribute(TYPE);
            prop.type = (typeAttr == null)? "string" : typeAttr.getStringValue();
            Node value = child.getFirstChild();
            if(prop.type.endsWith("()"))
                prop.nodeValue = value;
            else
                prop.value = value.getStringValue();
            list.add(prop);
        }
        return list;
    }
    
    public String serverControl(String command)
        throws IOException
    {
        startPost("server", null);
        curPost.setParameter("command", command);
        send();
        return getNextLine();
    }

    public String[] listLibraries()
        throws IOException
    {
        startGet("listlib", null);
        send();
        String resp = getResultString();
        
        StringTokenizer tokens = new StringTokenizer(resp);
        String[] split = new String[tokens.countTokens()];
        for (int i = 0; i < split.length; ++i)
            split[i] = tokens.nextToken();
        return split;
    }

    public void createLibrary(String name)
        throws IOException
    {
        startPost("mklib", null);
        curPost.setParameter("name", name);
        send();
    }

    public void deleteLibrary(String name)
        throws IOException
    {
        startPost("dellib", null);
        curPost.setParameter("name", name);
        send();
    }

    public void getIndexing()
        throws IOException
    {
        startGet("getindexing", null);
        send();
    }

    public void setIndexing(File spec)
        throws IOException
    {
        startPost("setindexing", null);
        curPost.setParameter("indexing", spec, null);
        send();
    }

    public void setIndexing(String spec)
        throws IOException
    {
        startPost("setindexing", null);
        curPost.setParameter("indexing", spec);
        send();
    }

    public String reindex()
        throws IOException
    {
        startPost("reindex", null);
        send();
        return getNextLine(); // progress id
    }

    public String optimize()
        throws IOException
    {
        startPost("optimize", null);
        send();
        return getNextLine(); // progress id
    }

    public String backup(String path)
        throws IOException
    {
        startPost("backup", path);
        send();
        return getNextLine(); // progress id
    }

    public double getProgress(String id)
        throws IOException
    {
        startGet("progress", null);
        curGet.setParameter("id", id);
        send();
        String s = getNextLine(); // skip
        s = getNextLine();
        return (s == null)? Double.NaN : Double.parseDouble(s);
    }

    /**
     * Returns the ACL information for a Library member.
     * Can be followed by getResultString() or getNode().
     * @param path
     * @param inherited
     */
    public void getAcl(String path, boolean inherited)
        throws IOException
    {
        startGet("getacl", path);
        curGet.setParameter("inherited", Boolean.toString(inherited));
        send();
    }

    public void setAcl(String data)
        throws IOException
    {
        startPost("setacl", null);
        curPost.setParameter("acl", data);
        send();
    }
}
