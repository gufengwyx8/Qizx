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

import com.qizx.api.*;
import com.qizx.api.util.XMLSerializer;
import com.qizx.restclient.RESTConnection;
import com.qizx.util.basic.Check;
import com.qizx.util.basic.FileUtil;
import com.qizx.util.basic.Util;
import com.qizx.xdm.Conversion;
import com.qizx.xdm.DocumentParser;
import com.qizx.xdm.IQName;
import com.qizx.xquery.ExpressionImpl;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQTypeException;

import org.xml.sax.InputSource;

import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Abstraction of access to a Qizx database, either local or remote.
 * Most operations are performed with a library name and a library member path.
 */
public class QizxConnector
{
    private static final long MAX_IMPORT_SIZE = 1024 * 2000;
    private static final int MAX_IMPORT_DOCS = 100;
    private static final QName TYPE = IQName.get("type");

    private RESTConnection serverCx;
    private XQuerySessionManager plainSessionManager;
    private XQuerySession plainSession;

    private long totalImportSize;
    private int importCount;
    private boolean putNonXML;

    
    public interface MemberIterator
    {
        boolean next();
        
        String getPath();
        
        boolean isDocument();
        boolean isNonXML();
    }

    public QizxConnector(String moduleBaseURI)
    {
        // use a plain XQuerySession
        URL baseURL = FileUtil.fileToURL(moduleBaseURI == null? "." : moduleBaseURI);
        plainSessionManager = new XQuerySessionManager(baseURL);
        plainSession = plainSessionManager.createSession();
    }

    public QizxConnector(RESTConnection server) throws IOException
    {
        this.serverCx = server;
        serverCx.login();
    }


    public void close() throws Exception
    {
        serverCx = null;
    }

    public boolean isClosed()
    {
        return 
                serverCx == null ;
    }

    public String getDisplay()
    {
        if(serverCx != null)
            return "Server at " + serverCx.getBaseURL();
            return "[No Library Group]";
    }

    /**
     * works only in remote mode
     */
    public List<Property> getInfo() throws Exception
    {
        if(serverCx != null)
            return serverCx.info();
        return null;
    }

    public boolean isLocal()
    {
        return serverCx == null;
    }


    public String serverCommand(String command)
        throws Exception
    {
        if (serverCx == null)
            throw new IllegalStateException("not connected to a server");
        return serverCx.serverControl(command);
    }

    /**
     * Execute on server and return directly the response as a stream.
     * @param format can be only "items", "xml", "xhtml", or "html"
     */
    public InputStream executeRemote(String query, String libraryName,
                                     String format, String encoding)
        throws QizxException, IOException
    {
        if (serverCx != null) {
            serverCx.setLibraryName(libraryName);
            serverCx.eval(query, format, encoding, -1, -1, 0);
            return serverCx.getResultStream();
        }
        return null;
    }
    
    /**
     * Execute and return items as Nodes.
     * An item in remote mode is special: only its string value is available.
     */
    public List<Item> execute(Query query, int startItem, int count)
        throws QizxException, IOException
    {
        ArrayList<Item> result = new ArrayList<Item>(count > 0 ? count : 16);
        if (serverCx != null) {
            // send query here:
            serverCx.setLibraryName(query.libraryName);
            serverCx.evalAsItems(query.query, count, startItem);
            for (Node item; (item = serverCx.nextItem()) != null;) {
                Node value = item.getFirstChild();
                Node typeAttr = item.getAttribute(TYPE);
                String type = typeAttr == null? "string" : typeAttr.getStringValue();
                if (Property.isNode(type))
                    result.add(value);
                else
                    result.add(new RItem(type, value.getStringValue()));
            }
            // retrieve total item count from attribute on root node:
            Node node = serverCx.getNode();
            if (node != null)
                node = node.getFirstChild();
            if (node != null)
                node = node.getAttribute(IQName.get("total-count"));
            query.totalItemCount =
                (node == null) ? -1 : Integer.parseInt(node.getStringValue());
        }
        else {
            XQuerySession session = null;
            {
                session = plainSession;
            }

            // execute query if not cached:
            ItemSequence rs = query.results;
            if(rs == null)
            {
                Expression exp = query.expr =
                    session.compileExpression(query.query);
                if(query.domain != null) {
                    // Any expression that can be passed to fn:collection
                    // Valid both for Qizx/db and Qizx/open
                    Expression ic = 
                      session.compileExpression("fn:collection('" + query.domain + "')");
                    ItemSequence seq = ic.evaluate();
                    exp.bindImplicitCollection(seq);
                }

                query.results = rs = exp.evaluate();
                query.totalItemCount = rs.countItems();
            }
            
            // get items
            query.results.moveTo(startItem);
            int it = 0;
            for (; (count < 0 || it < count)
                        && rs.moveToNextItem(); it++) {
                result.add(rs.getCurrentItem());
            }
        }
        return result;
    }
    
    /**
     * Enables or disables binding of a Java class in XQuery
     * @param javaClass full name of a class, or null to enable all classes
     */
    public void enableBinding(String javaClass) throws QizxException
    {
        if(plainSession != null)
            plainSession.enableJavaBinding(javaClass);
    }

    /**
     * Query with results.
     * In local mode, the result ItemSequence is cached and reused.
     */
    public static class Query
    {
        private String query;
        private String libraryName;
        private String domain;
        
        private long totalItemCount;
        private Expression expr;
        private ItemSequence results;

        public Query(String query, String libraryName)
        {
            this.query = query;
            this.libraryName = libraryName;
        }

        public String getQuery() {
            return query;
        }

        public long getTotalItemCount() {
            return totalItemCount;
        }

        public String getQueryDomain()
        {
            return domain;
        }

        public void setQueryDomain(String queryDomain)
        {
            this.domain = queryDomain;
        }

        public void cancel()
        {
            if(expr != null) {
                expr.cancelEvaluation();
            }
        }
    }
    
    /**
     * Used for non-node remote items
     */
    static class RItem implements Item
    {
        private ItemType type;
        private String value;
        
        RItem(String type, String value) {
            this.type = XQType.findItemType(type);  // not in API
            this.value = value;
        }
        
        public ItemType getType() {
            return type;
        }

        public boolean isNode() {
            return false;
        }

        public String getString() {
            return value;
        }

        public boolean getBoolean() throws XQTypeException {
            return Conversion.toBoolean(value);
        }

        public BigDecimal getDecimal() throws XQTypeException {
            return Conversion.toDecimal(value, true);
        }

        public double getDouble() throws XQTypeException {
            return Conversion.toDouble(value);
        }

        public float getFloat() throws XQTypeException {
            return Conversion.toFloat(value);
        }

        public long getInteger() throws XQTypeException {
            return Conversion.toInteger(value);
        }

        public Node getNode() {
            return null; // TODO
        }

        public Object getObject() {
            return null; // TODO
        }

        public QName getQName()
            throws EvaluationException
        {
            return null; // TODO
        }

        public XMLPullStream exportNode() {
            return null; // TODO
        }

        public void export(XMLPushStream writer) {
             // TODO
        }
    }

    public String[] listLibraries()
        throws Exception
    {
        String[] libs = null;
        if(serverCx != null)
            libs = serverCx.listLibraries();
        return libs;
    }
    
    
    public void createLibrary(String libraryName) throws Exception
    {
        if (serverCx != null) {
            serverCx.createLibrary(libraryName);
        }
    }

    public void deleteLibrary(String libraryName)
        throws Exception
    {
        if (serverCx != null) {
            serverCx.deleteLibrary(libraryName);
        }
    }

    public MemberIterator collectionIterator(String libaryName, String path)
        throws Exception
    {
        return new CollIterImpl(libaryName, path);
    }
    
    public class CollIterImpl
        implements MemberIterator
    {
        ArrayList<Member> members;
        int index;
        boolean isNonXML;
        
        public CollIterImpl(String libraryName, String collPath)
            throws Exception
        {
            members = new ArrayList<Member>();
            if(serverCx != null)
            {
                serverCx.setLibraryName(libraryName);
                serverCx.get(collPath);
                for (;;) {
                    String path = serverCx.getNextLine();
                    if (path == null)
                        break;
                    if(path.endsWith("/")) {
                        path = path.substring(0, path.length() - 1);
                        members.add(new Member(path, false));
                    }
                    else {
                        members.add(new Member(path, true));
                    }
                }
            }
            Collections.sort(members, memberComparator);
            index = -1;
        }

        public String getPath()
        {
            return members.get(index).path;
        }
    
        public boolean isDocument()
        {
            return members.get(index).isDoc;
        }
    
        public boolean next()
        {
            ++index;
            return index < members.size();
        }

        public boolean isNonXML()
        {
            return members.get(index).isNonXML;
        }    
    }
    
    private static class Member
    {
        String path;
        boolean isDoc;
        boolean isNonXML;

        Member(String path, boolean isDoc) {
            this(path, isDoc, false);
        }

        public Member(String path, boolean isDoc, boolean isNonXML)
        {
            this.path = path;
            this.isDoc = isDoc;
            this.isNonXML = isNonXML;
        }
    }
    
    private Comparator<Member> memberComparator = new Comparator<Member>() {
        public int compare(Member o1, Member o2)
        {
            if(o1.isDoc != o2.isDoc)
                return o1.isDoc? 1 : -1;
            return o1.path.compareTo(o2.path);
        }
    };

    public void commit(String libraryName) throws Exception
    {
            importFlush();
    }
    
    public void rollback(String libraryName) throws Exception
    {
    }

    public void refresh(String libraryName) throws Exception
    {
    }

    public Node getDocumentTree(String libraryName, String docPath)
        throws Exception
    {
        if (serverCx != null) {
            serverCx.setLibraryName(libraryName);
            serverCx.get(docPath);
            return serverCx.getNode();
        }
        return null;
    }
    
    public InputStream getNonXMLStream(String libraryName, String docPath)
        throws Exception
    {
        if (serverCx != null) {
            serverCx.setLibraryName(libraryName);
            serverCx.get(docPath);
            return serverCx.getResultStream();
        }
        return null;
    }

    /**
     * Returns "collection", "document", "non-xml", or null
     */
    public String getMemberNature(String libraryName, String path)
        throws Exception
    {
        List<Property> props = getMemberProperties(libraryName, path);
        if(props != null)
            for(Property p : props) {
                if(p.name.equals("nature"))
                    return p.value;
        }
        return null;
    }
    
    public List<Property> getMemberProperties(String libraryName, String path)
        throws Exception
    {
        if (serverCx != null) {
            serverCx.setLibraryName(libraryName);
            serverCx.getProp(path, 1, null);
            return serverCx.nextPropSet();
        }
        return null;
    }

    /**
     * Stores a property of a Library member
     */
    public void setMemberProperty(String libraryName, String path,
                                  Property property)
        throws Exception
    {
        Check.nonNull(path, "path");
        Check.nonNull(property, "property");
        
        if (serverCx != null) {
            serverCx.setLibraryName(libraryName);
            // TODO? serialize actual node to String
            // pseudo-types node() and "<expression>" processed in server
            serverCx.startSetProp(path, property.name, property.type, property.value);
            serverCx.send();
        }
    }

    
    public String copyMember(String libraryName, String path, String newPath)
        throws Exception
    {
        if (serverCx != null) {
            serverCx.setLibraryName(libraryName);
            return serverCx.copy(path, newPath);
        }
        return null;
    }
    
    public String renameMember(String libraryName, String path, String newPath)
        throws Exception
    {
        if (serverCx != null) {
            serverCx.setLibraryName(libraryName);
            return serverCx.move(path, newPath);
        }
        return null;
    }

    public void createCollection(String libraryName, String path)
        throws Exception
    {
        if (serverCx != null) {
            serverCx.setLibraryName(libraryName);
            serverCx.createCollection(path, true);
        }
    }
    
    public boolean deleteMember(String libraryName, String path)
        throws Exception
    {
        if (serverCx != null) {
            serverCx.setLibraryName(libraryName);
            return serverCx.delete(path);
        }
        return false;
    }
    
    
    public void importStart(String libraryName)
        throws Exception
    {
        putNonXML = false;
        if (serverCx != null) {
            serverCx.startPut();
        }
    }
    
    public void importNonXMLStart(String libraryName)
        throws Exception
    {
        putNonXML = true;
        if (serverCx != null) {
            serverCx.startPutNonXML();
        }
    }
    
    /**
     * Import a document from a file.
     * @param docPath path in the Library
     * @param file source file
     * @throws LibraryException messagewith a list of documents that generated an error
     * each on a line, with
     */
    public void importDocument(String docPath, File file)
        throws Exception
    {
        if(serverCx != null) {
            serverPut(docPath, file, null);
        }
    }

    /**
     * Import a non-XML document from a file.
     * @param docPath path in the Library
     * @param file source file
     * @param contentType mime type such as "image/jpeg". Can be null, but it is
     * strongly recommended to provide a value.
     * @throws LibraryException messagewith a list of documents that generated an error
     * each on a line, with
     */
    public void importNonXMLDocument(String docPath, File file, String contentType)
        throws Exception
    {
        if(serverCx != null) {
            serverPut(docPath, file, contentType);   // same as XML
        }
    }

    private void serverPut(String docPath, File file, String contentType)
        throws IOException, Exception
    {
        long fileSize = file.length();
        serverCx.addDocument(docPath, file, contentType);
        totalImportSize += fileSize;
        ++importCount;
        
        if (totalImportSize + fileSize >= MAX_IMPORT_SIZE
            || importCount >= MAX_IMPORT_DOCS)
            importFlush();
    }
    
    /**
     * Import a document from data.
     * @param docPath path in the Library
     * @param data XML data as String
     * @param sourceURL optional URL, ignored in remote mode
     */
    public void importDocument(String docPath, String data, String sourceURL)
        throws Exception
    {
        if(serverCx != null) {
            long fileSize = data.length();
            if (totalImportSize + fileSize >= MAX_IMPORT_SIZE
                     || importCount >= MAX_IMPORT_DOCS)
                importFlush();
            serverCx.addDocument(docPath, data);
            totalImportSize += fileSize;
            ++importCount;
        }
    }
    
    public void importFlush() throws Exception
    {
        if(serverCx != null && importCount > 0) {
            try {
                serverCx.send();
                String messages = serverCx.getResultString();
                String[] errors = messages.split("\n");
                if(errors[0].startsWith("IMPORT ERRORS 0"))
                    return;
                // TODO trim last one
                throw new ImportException(errors);
            }
            finally {
                if(putNonXML)
                    serverCx.startPutNonXML();
                else
                    serverCx.startPut();
                totalImportSize = 0;
                importCount = 0;
            }
        }
    }
    
    public static class ImportException extends QizxException
    {
        private String[] errors;

        public ImportException(String[] errors)
        {
            super("Parsing");
            this.errors = errors;
        }

        public String[] getErrors()
        {
            return errors;
        }
    }

    
    public String getACL(String libraryName, String path, boolean inherited)
        throws Exception
    {
        if (serverCx != null) {
            serverCx.setLibraryName(libraryName);
            serverCx.getAcl(path, inherited);
            return serverCx.getResultString();
        }
        return null;
    }

    public void setACL(String libraryName, String acls)
        throws Exception
    {
        if (serverCx != null) {
            serverCx.setLibraryName(libraryName);
            serverCx.setAcl(acls);
        }
    }

}
