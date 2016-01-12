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
import com.qizx.xdm.DocumentParser;

import com.xmlmind.netutil.GetRequest;
import com.xmlmind.netutil.PostRequest;
import com.xmlmind.netutil.Request;

import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class ClientConnection
{

    protected String host;
    protected URL baseURL;
    protected Request curRequest;
    protected GetRequest curGet;
    protected PostRequest curPost;
    protected int paramRank;
    private HttpURLConnection curCx;
    private InputStream curStream;
    private BufferedReader curReader;
    protected Node curRoot;
    protected Node curNode;

    public ClientConnection()
    {
        super();
    }

    public String getBaseURL()
    {
        return baseURL.toString();
    }

    public void setBaseURL(String url)
        throws MalformedURLException
    {
        host = url;
        baseURL = new URL(url);
    }

    public void logout()
        throws IOException
    {
        HttpURLConnection conx = Request.openConnection(baseURL);
        conx.disconnect();  // doesnt work
        Authenticator.setDefault(null);
    }

    public GetRequest startGet()
    {
        curRequest = curGet = new GetRequest();
        curPost = null;
        return curGet;
    }

    public PostRequest startPost()
    {
        curRequest = curPost = new PostRequest();
        curGet = null;
        return curPost;
    }

    /**
     * Send the request.
     * <p>
     * Useful for requests built incrementally. Often called automatically.
     */
    public void send()
        throws IOException
    {
        if(curGet != null)
            curCx = curGet.get(host);
        else if(curPost != null)
            curCx = curPost.post(baseURL);
        else
            throw new IllegalStateException("no request initialized");
        curStream = null;
        curReader = null;
        curRoot = null;
        curNode = null;
        checkError();
    }

    public String getResultMimeType()
    {
        return curRequest.getResultMimeType();
    }

    public void checkError()
        throws IOException
    {
        curRequest.checkError();
    }

    public boolean gotError()
        throws IOException
    {
        return curRequest.isError();
    }

    /**
     * Return response as a simple string.
     * @throws IOException 
     */
    public String getErrorString()
        throws IOException
    {
        return curRequest.getErrorString();
    }

    /**
     * Return response as a simple string.
     * @throws IOException 
     */
    public String getResultString()
        throws IOException
    {
        return curRequest.getStringResponse();
    }

    /**
     * Get result as a list of text lines. Iterate on each line.
     */
    public String getNextLine()
        throws IOException
    {
        if (curReader == null) {
            getResultStream();
            curReader = new BufferedReader(new InputStreamReader(curStream,
                                                Request.DEFAULT_CHAR_ENCODING));
        }
        return curReader.readLine();
    }

    public InputStream getResultStream()
        throws IOException
    {
        if (curStream == null)
            curStream = curRequest.getStreamResponse();
        return curStream;
    }
    
    public Node getNode()
        throws DataModelException
    {
        if (curRoot == null)
            try {
                curRoot =
                    DocumentParser.parse(new InputSource(getResultStream()));
            }
        catch (Exception e) {
            throw new DataModelException(e.getMessage(), e);
        }
        return curRoot;
    }

    protected String rankedParam(String name)
    {
        return (paramRank == 0) ? name : (name + paramRank);
    }

    protected String nameList(String[] names)
    {
        StringBuilder buf = new StringBuilder(names.length * 8);
        for (int i = 0; i < names.length; i++) {
            buf.append(names[i]);
        }
        return buf.toString();
    }

}
