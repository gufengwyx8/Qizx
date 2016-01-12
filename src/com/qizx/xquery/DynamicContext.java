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
import com.qizx.api.fulltext.FullTextFactory;
import com.qizx.api.fulltext.TextTokenizer;
import com.qizx.api.util.NodeSource;
import com.qizx.api.util.PushStreamToSAX;
import com.qizx.api.util.time.DateTime;
import com.qizx.queries.FullText;
import com.qizx.util.basic.FileUtil;
import com.qizx.util.basic.PlatformUtil;
import com.qizx.util.basic.Util;
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.IDocument;
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xquery.dt.ArraySequence;
import com.qizx.xquery.op.Expression;
import com.qizx.xquery.op.GlobalVariable;
import com.qizx.xquery.op.PathExpr;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;

/**
 * Holds the part of the execution context that is shared by all EvalContext's.
 * Provides access to the XLib and to the pool of documents.
 */
public class DynamicContext implements UpdaterFactory 
{
    public static final String XSLT_OUTPUT_FILE = "output-file";

    protected XQuerySessionImpl session;
    // Access to static context: readonly, potentially shared
    protected MainQuery mainQuery;
    protected URI baseURI;

    // provides access to values of global variables
    protected HashMap globals;
    // pool of already loaded documents: guarantees doc("X") is doc("X")
    protected HashMap<String, BasicNode> documents;
    protected XQValue defaultCollection;

    protected TraceObserver traceListener;
    protected PrintWriter compilationTrace;

    private HashMap<String, Object> propertyMap;
    
    // Debugger
    protected volatile int monitorMode; // 0, STOP, TIMEOUT, SUSPEND, TRACE
    // point where the last error happened
    protected EvalContext lastContext;
    protected int timeZone;
    protected TextTokenizer tokenizer;
    
    protected DateTime currentDate;
    //private WordSieve wordSieve;

    protected ModuleManager moduleMan;

    // XQUery Update: stack of Pending Update Lists 
    // copy/modify stacks up a PUL, 
    protected Updates updateStack;
    protected UpdaterFactory updaterFactory = this;


    DynamicContext(MainQuery query, XQuerySessionImpl session)
    {
        this.session = session;
        mainQuery = query;
        globals = new HashMap();
        documents = new HashMap<String, BasicNode>();
        if(session != null) {
            moduleMan = session.getModuleManager();
        }
    }
    
    public Collator getCollator(String uri)
    {
        return mainQuery.getCollator(uri);
    }

    public PrintWriter getTraceOutput()
    {
        return compilationTrace;
    }

    public Object getProperty(String name)
    {
        return propertyMap == null? null : propertyMap.get(name);
    }

    public void setProperty(String name, Object value)
    {
        if (propertyMap == null)
            propertyMap = new HashMap<String, Object>();
        propertyMap.put(name, value);
    }
    
    public Object getSessionProperty(String name)
    {
        return session.getProperty(name);
    }

    public void setSessionProperty(String name, Object value)
    {
        session.setProperty(name, value);
    }
    
    public int getCopyNSMode()
    {
        return mainQuery.getCopyNSMode();
    }

    public FullText.MatchOptions getDefaultFTOptions()
    {
        return mainQuery.getDefaultFTOptions();
    }

    public FullTextFactory getFulltextFactory()
    {
        return session.getFullTextFactory();
    }

    public TextTokenizer getTextTokenizer()
    {
        if(tokenizer == null)
            tokenizer = getFulltextFactory().getTokenizer(
                                               getDefaultFTOptions().language); 
        return tokenizer;
    }

    public XQValue getDefaultCollection()
    {
        if(defaultCollection == null)
            return null;
        return defaultCollection.bornAgain();
    }

    
    // default implementation of fn:collection in Qizx/open:
    public XQValue getCollectionSequence(String uri) throws DataModelException
    {
        return expandBasicCollection(uri);
    }

    public XQValue expandBasicCollection(String pathPattern)
        throws DataModelException
    {
        String[] paths = pathPattern.split("[;,]");
        ArraySequence coll = new ArraySequence(paths.length, null);
        for (int i = 0; i < paths.length; i++) {
            String docPath = paths[i];
            String name = FileUtil.fileBaseName(docPath);
            if(name.indexOf('*') < 0 && name.indexOf('?') < 0) {
                coll.addItem(getDocument(docPath));
            }
            else {  // simple expansion
                File[] files = FileUtil.expandPathPattern(new File(docPath));
                for (int j = 0; j < files.length; j++) {
                    coll.addItem(getDocument(files[j].getAbsolutePath()));
                }
            }
        }
        if(coll.getSize() == 0)
            throw new DataModelException("empty collection " + pathPattern);
        return coll;
    }

    // manage a cache of documents, get documents from session/lib
    public BasicNode getDocument(String uri) throws DataModelException
    {
        try {
            // On windows, normalize
            if(PlatformUtil.IS_WINDOWS)
                uri = uri.replace('\\', '/');
            String ruri = resolveURI(uri);
            
            BasicNode docRoot = documents.get(ruri);
            // if not found, ask to static context
            if (docRoot == null) {
                docRoot = session.getDocument(ruri);

                if(docRoot != null)
                    documents.put(ruri, docRoot);
                else
                    throw new DataModelException("document " + uri + " not found");
            }
            return docRoot;
        }
        catch (URISyntaxException e) {
            throw new DataModelException("invalid document URI: " + uri);
        }
    }

    /**
     * Resolves and escapes a document URI
     * @param uri unescaped URI
     */
    protected String resolveURI(String uri) throws URISyntaxException
    {
        if(baseURI == null) {
            String buri = mainQuery.getBaseURI();
            if(buri == null)
                try {
                    buri = FileUtil.fileToSystemId(new File(".")) + "/";
                }
                catch (IOException e) {
                    buri = "file:///.";
                }
            baseURI = URI.create(Util.escapeURI(buri));
        }
        URI curi = new URI(Util.escapeURI(uri));
        URI ruri = baseURI.resolve(curi);
         
        return ruri.toString();
    }
    
    public String getNSPrefix(String namespaceURI)
    {
        return mainQuery.getNSPrefix(namespaceURI);
    }

    public DateTime getCurrentDate()
    {
        return currentDate;
    }

    public void setDate(Date date, TimeZone implicitTimeZone)
    {
        timeZone = implicitTimeZone.getRawOffset() / 60000;
        currentDate = new DateTime(date == null? new Date() : date, timeZone);
    }

    public int getImplicitTimezone()
    {
        return timeZone;
    }

    public void cancel()
    {
         synchronized (this) { // in case suspended
             monitorMode = EvalContext.STOP;
             this.notify();
         }
    }

    public void setTimedOut()
    {
        monitorMode = EvalContext.TIMEOUT;
    }

    public void suspend()
    {
        monitorMode = EvalContext.SUSPEND;
    }

    public void resume(boolean unleashed)
    {
        synchronized (this) {
            // still monitored => one step
            monitorMode = unleashed ? 0 : EvalContext.SUSPEND;
            this.notify();
        }
    }

    // Basic implementation, without Libraries.
    public XQValue eval(PathExpr path, Focus focus, EvalContext context)
        throws EvaluationException
    {
        XQValue root = path.getStep(0).eval(focus, context);
        return path.evalNextSteps(root, 1, context);
    }

    public Expression checkExpression(Expression expr)
        throws CompilationException
    {
        return session.staticCheck(expr, mainQuery);
    }
    
    // dynamic evaluation: use calling query as context
    public ExpressionImpl compileExpression(String querySrc) 
        throws EvaluationException
    {
        try {
            return session.compile(querySrc, mainQuery);
        }
        catch (CompilationException e) {
            Message[] messages = e.getMessages();
            StringWriter sout = new StringWriter();
            PrintWriter out = new PrintWriter(sout);
            out.print("in eval() function: ");
            out.println(e.getMessage());
            for (int i = 0; i < messages.length; i++)
                messages[i].print(out, false);
            out.flush();
            throw new EvaluationException(sout.toString(), e);
        }
    }

    public void passVariablesTo(ExpressionImpl expr) throws CompilationException
    {
        // globals set from API:
        HashMap globs = expr.externGlobals;
        for (Iterator iter = globs.keySet().iterator(); iter.hasNext();) {
            QName varName = (QName) iter.next();
            expr.rawBindVariable(varName, (XQValue) globs.get(varName));
        }
        // globals defined in parent expression:
        for (Iterator iter = globals.keySet().iterator(); iter.hasNext();) {
            GlobalVariable var = (GlobalVariable) iter.next();
            expr.rawBindVariable(var.name, (XQValue) globals.get(var));
        }
    }

    public IDocument xslTransform(Node source, String templates,
                                  Properties parameters, Properties options)
        throws EvaluationException
    {
        try {
            // cached loading:
            Templates sheet = moduleMan.loadTemplates(templates);
            Transformer trans = sheet.newTransformer();
            Source tSource = null;
            Result result = null;
            IDocument tree = null;

            // use SAX input for the XSLT engine. 
            // This is not optimal (because we copy the tree),
            //  but acceptable for small or medium-size transforms.
            tSource = new NodeSource(source);

            // is there an output file?
            String out = options.getProperty(XSLT_OUTPUT_FILE);
            if (out != null) {
                result = new StreamResult(out);
            }
            else { // build a tree:
                tree = new IDocument();
                SAXResult tresult = new SAXResult();
                tresult.setHandler(tree);
                tresult.setLexicalHandler(tree);
                tresult.setSystemId(FileUtil.fileToURLName(".")); // Saxon wants it!
                result = tresult;
            }
            // options (String value always)
            for (Enumeration enu = options.keys(); enu.hasMoreElements();) {
                String name = (String) enu.nextElement();
                if (name.equals(XSLT_OUTPUT_FILE))
                    continue;
                trans.setOutputProperty(name, options.getProperty(name));
            }
            // parameters (String value always)
            for (Enumeration enu = parameters.keys(); enu.hasMoreElements();) {
                String name = (String) enu.nextElement();
                trans.setParameter(name, parameters.get(name));
            }

            // invoke XSLT engine:
            trans.transform(tSource, result);
            return tree;
        }
        catch (TransformerException e) {
            throw new EvaluationException(e.getMessage(), e);
        }
    }

    
    // ------------------- XQ Update:

    public Updates pushUpdates()    // called by copy/modify
    {
        updateStack = new Updates(this, updateStack);
        return updateStack;
    }
    
    public void popUpdates()    // called by copy/modify
    {
        if(updateStack != null)
            updateStack = updateStack.getEnclosing();
    }
    
    public Updates haveUpdateList()
    {
        // create one on first update op; copy/modify always creates a PUL
        if(updateStack == null)
            updateStack = new Updates(this, updateStack);
        return updateStack;
    }

    public void applyUpdates() throws EvaluationException
    {
        if(updateStack != null) {
            updateStack.apply();
            if(updateStack.getEnclosing() != null)
                System.err.println("OOPS improperly unstacked PUL");
        }
    }    

    public XMLPushStreamBase newLibraryDocument(String uri)
        throws DataModelException
    {
        return null;
    }


    public XMLPushStreamBase newParsedDocument(String uri)
    {
        IDocument odoc = new IDocument();
        odoc.setBaseURI(uri);
        return new PushStreamToSAX(odoc);
    }

    public void endParsedDocument()
        throws DataModelException
    {
    }

    public UpdaterFactory getUpdaterFactory()
    {
        return updaterFactory;
    }

    public void setUpdaterFactory(UpdaterFactory updaterFactory)
    {
        this.updaterFactory = updaterFactory;
    }
}
