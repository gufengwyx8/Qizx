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
package com.qizx.xquery;

import com.qizx.api.*;
import com.qizx.api.SequenceType;
import com.qizx.api.Indexing.WordSieve;
import com.qizx.api.fulltext.FullTextFactory;
import com.qizx.api.util.XMLSerializer;
import com.qizx.api.util.fulltext.DefaultFullTextFactory;
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.FONIDataModel;
import com.qizx.xquery.PredefinedModule.BasicFunctionPlugger;
import com.qizx.xquery.ext.ExtensionFunction;
import com.qizx.xquery.ext.SqlConnection;
import com.qizx.xquery.impl.NewParser;

import org.xml.sax.InputSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


public class XQuerySessionImpl
    implements XQuerySession, MessageReceiver
{
    private XQuerySessionManager manager;
    private BasicStaticContext xqContext;
    private ItemFactory itemMaker;
    private int errorCount;
    private ArrayList messages = new ArrayList();
    private QName firstErrorCode;
    private QName latestErrorCode;
    private ModuleResolver moduleResolver;  // optional
    private HashMap properties;
    
    protected FullTextFactory fulltextProvider;
    
    public XQuerySessionImpl(XQuerySessionManager manager)
    {
        this.manager = manager;
        xqContext = new BasicStaticContext();
        itemMaker = new ItemFactoryImpl();
        fulltextProvider = new DefaultFullTextFactory();
        // plug extension functions:
        XQuerySessionImpl.plugBuiltins(xqContext);
    }

    public static PredefinedModule plugBuiltins(BasicStaticContext xqContext)
    {
        // plug extension functions:
        PredefinedModule predef = xqContext.getPredefinedModule();
        
        String xuri = ExtensionFunction.EXTENSION_NS;
        xqContext.declarePrefix("x", xuri);
        xqContext.declarePrefix("qizx", xuri);
        predef.registerFunctionPlugger(new BasicFunctionPlugger(
                                           xuri, "com.qizx.xquery.ext.Xfn%C"));
        
        // full-text namespace ft: no more a mere alias of x
        xqContext.declarePrefix("ft", ExtensionFunction.FULLTEXT_EXT_NS);
        predef.registerFunctionPlugger(new BasicFunctionPlugger(
                ExtensionFunction.FULLTEXT_EXT_NS, "com.qizx.xquery.ext.FT_%C"));
        // full-text namespace sql:
        xqContext.declarePrefix("sql", SqlConnection.NS);
        predef.registerFunctionPlugger(new BasicFunctionPlugger(
                                SqlConnection.NS, "com.qizx.xquery.ext.Sql%C"));
        
        // dummy:
        xqContext.declarePrefix("xlib", "com.qizx.functions.xlib");

        return predef;
    }

    public XQueryContext getContext()
    {
        return xqContext;
    }

    public BasicStaticContext getBasicContext()
    {
        return xqContext;
    }

    public ModuleManager getModuleManager()
    {
        return manager.getModuleManager();
    }


    public com.qizx.xquery.op.Expression
              staticCheck(com.qizx.xquery.op.Expression expr, MainQuery context)
        throws CompilationException
    {
        errorCount = 0;
        messages.clear();
        com.qizx.xquery.op.Expression cexpr = context.simpleStaticCheck(expr, 0);
        if (errorCount > 0) {
            compilError();
        }
        return cexpr;
    }

    public Expression compileExpression(String xquerySource)
        throws CompilationException
    {
        return compile(xquerySource, xqContext);
    }

    ExpressionImpl compile(String xquerySource, BasicStaticContext context)
        throws CompilationException
    {
        // create a context:
        MainQuery query = new MainQuery(context);
        query.setMessageTarget(this);
        firstErrorCode = latestErrorCode = null;
        errorCount = 0;
        messages.clear();
        
        ModuleManager modMan = manager.getModuleManager();
        // parse the source code:
//        Parser parser = new Parser(modMan);
        NewParser parser = new NewParser(modMan);
        if(moduleResolver != null)
            parser.setModuleResolver(moduleResolver);
        query.setFulltextProvider(fulltextProvider);

        try {
            parser.parseQuery(query, xquerySource, null);
        }
        catch (CompilationException err) {
            Message[] msgs = new Message[messages.size()];
            err.setMessages((Message[]) messages.toArray(msgs));
            throw err;
        }
        
//        // Static checking:
//        XQItem currentItem = null; // todo ?
//
//        if (currentItem != null)
//            query.pushDotType(currentItem.getItemType());

        if (errorCount == 0)
            query.staticCheck(modMan);

//        if (currentItem != null)
//            query.popDotType();

        if (errorCount > 0) {
//            try {
//                query.dump(new XMLExprDisplay(new XMLSerializer(System.err, "UTF-8")));
//            }
//            catch (DataModelException e) {
//                e.printStackTrace();
//            }
            compilError();
        }
        
        ExpressionImpl expr = 
            new ExpressionImpl(new DynamicContext(query, this), query);
        return expr;
    }

    private void compilError()
        throws CompilationException
    {
        CompilationException err =
            new CompilationException(errorCount + " error(s) in static analysis");
        if (firstErrorCode != null)
            err.setErrorCode(firstErrorCode);
        Message[] msgs = new Message[messages.size()];
        err.setMessages((Message[]) messages.toArray(msgs));
        throw err;
    }

    public void receive(Message message)
    {
        messages.add(message);
        if(message.getType() == Message.ERROR) {
            ++ errorCount;
            if(message.getErrorCode() != null) {
                if(firstErrorCode == null)
                    firstErrorCode = message.getErrorCode();
                latestErrorCode = message.getErrorCode();
            }
        }
    }

    public ItemSequence copySequence(ItemSequence sequence)
        throws EvaluationException
    {
        return itemMaker.copySequence(sequence);
    }

    public Item createItem(boolean value)
    {
        return itemMaker.createItem(value);
    }

    public Item createItem(double value)
    {
        return itemMaker.createItem(value);
    }

    public Item createItem(float value)
    {
        return itemMaker.createItem(value);
    }

    public Item createItem(InputSource source)
        throws EvaluationException, IOException
    {
        return itemMaker.createItem(source);
    }

    public Item createItem(long value, ItemType type)
        throws EvaluationException
    {
        return itemMaker.createItem(value, type);
    }

    public Item createItem(Object value, ItemType type)
        throws EvaluationException
    {
        return itemMaker.createItem(value, type);
    }

    public Item createItem(XMLPullStream source)
        throws EvaluationException
    {
        return itemMaker.createItem(source);
    }

    public ItemSequence createSequence(Object object, SequenceType type)
        throws EvaluationException
    {
        return itemMaker.createSequence(object, type);
    }

    public QName getQName(String localName, String namespaceURI, String prefix)
    {
        return itemMaker.getQName(localName, namespaceURI, prefix);
    }

    public QName getQName(String localName, String namespaceURI)
    {
        return itemMaker.getQName(localName, namespaceURI);
    }

    public QName getQName(String localName)
    {
        return itemMaker.getQName(localName);
    }

    public ItemType getType(String name)
    {
        return itemMaker.getType(name);
    }

    public ItemType getNodeType(int nodeKind, QName name)
    {
        return itemMaker.getNodeType(nodeKind, name);
    }

    public BasicNode getDocument(String uri) throws DataModelException
    {
        FONIDataModel doc = manager.getDocumentCache().findDocument(uri);
        return doc == null? null : doc.getDocumentNode();
    }

    public ModuleResolver getModuleResolver()
    {
        return moduleResolver;
    }

    public void setModuleResolver(ModuleResolver resolver)
    {
         moduleResolver = resolver;
    }

    public void enableJavaBinding(String className)
    {
        xqContext.getPredefinedModule().authorizeJavaClass(className);
    }

    public WordSieve getWordSieve()
    {
        return null;
    }

    public void setWordSieve(WordSieve wordSieve)
    {
    }

    public FullTextFactory getFullTextFactory()
    {
        return fulltextProvider;
    }

    public void setFullTextFactory(FullTextFactory fulltextProvider)
    {
        this.fulltextProvider = fulltextProvider;
    }

    public Object getProperty(String name)
    {
        return properties == null? null : properties.get(name);
    }

    public void setProperty(String name, Object value)
    {
        if (properties == null)
            properties = new HashMap();
        properties.put(name, value);
    }
}
