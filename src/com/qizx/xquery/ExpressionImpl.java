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
import com.qizx.api.util.PushStreamToSAX;
import com.qizx.util.basic.Check;
import com.qizx.xquery.dt.JavaMapping;
import com.qizx.xquery.dt.SingleItem;
import com.qizx.xquery.impl.EmptyException;

import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class ExpressionImpl
    implements Expression, Focus
{
    private ItemFactory itemMaker;
    private MainQuery query;
    private DynamicContext dynCtx;
    private EvalContext evalContext;
    protected HashMap externGlobals;
    private XQItem currentItem;
    private int timeOut;
    
    ExpressionImpl(DynamicContext context, MainQuery query)
    {
        this.query = query;
        itemMaker = new ItemFactoryImpl();
        dynCtx = context;
        externGlobals = new HashMap();
    }
    
    public XQueryContext getContext()
    {
        return query;
    }

    public SequenceType getStaticType()
    {
        return query.getType();
    }

    public String getSource()
    {
        return query.getSource();
    }

    public void rawBindVariable(QName varName, Item value)
        throws CompilationException
    {
        Check.nonNull(varName, "varName");
        if(query.getStrictCompliance() && 
             query.lookforGlobalVariable(varName) == null)
                throw new CompilationException("undefined global variable " + varName);
        externGlobals.put(varName, value);
    }

    public void bindVariable(QName varName, Item item)
        throws CompilationException
    {
        rawBindVariable(varName, new SingleItem((XQItem) item));
    }

    public void bindVariable(QName varName, ItemSequence value)
        throws CompilationException
    {
        try {
            Check.implementation(value, ResultSequence.class, ItemSequence.class);
            ResultSequence res = (ResultSequence) value;
            rawBindVariable(varName, res.getValues());
        }
        catch (ClassCastException e) {
            throw new CompilationException("foreign ItemSequence implementation");
        }        
    }

    public void bindVariable(QName varName, boolean value)
        throws CompilationException
    {
        rawBindVariable(varName, createItem(value));
    }

    public void bindVariable(QName varName, long value, ItemType type)
        throws CompilationException, EvaluationException
    {
        rawBindVariable(varName, itemMaker.createItem(value, type));
    }

    public void bindVariable(QName varName, double value)
        throws CompilationException
    {
        rawBindVariable(varName, itemMaker.createItem(value));
    }

    public void bindVariable(QName varName, float value)
        throws CompilationException
    {
        rawBindVariable(varName, itemMaker.createItem(value));
    }

    public void bindVariable(QName varName, Object value, ItemType type)
        throws CompilationException, EvaluationException
    {
        if(type == null && JavaMapping.isSequence(value))
            bindVariable(varName, itemMaker.createSequence(value, type));
        else
            bindVariable(varName, itemMaker.createItem(value, type));
    }


    public void bindImplicitCollection(ItemSequence nodes)
    {
        Check.implementation(nodes, ResultSequence.class, ItemSequence.class);
        dynCtx.defaultCollection = ((ResultSequence) nodes).getValues();
    }

    public Item getCurrentItem()
    {
        return currentItem;
    }

    public void setCurrentItem(Item item)
    {
        //TODO? may be annoying: use a wrapper implementing XQItem?
        if(item != null)
            Check.implementation(item, XQItem.class, Item.class);
        currentItem = (XQItem) item;
    }

    public ItemSequence evaluate()
        throws EvaluationException
    {
        XQValue result = rawEval();
        // wrap raw result
        return new ResultSequence(result);
    }

    public XQValue rawEval()
        throws EvaluationException
    {
        // if current date not set explicitly in ctx, it is computed here
        dynCtx.setDate(query.getCurrentDate(), query.getImplicitTimeZone());
        // Create a new eval context with the runtime (dyn) context:
        evalContext = new EvalContext(dynCtx);
        dynCtx.resume(true);    // reset

        if (timeOut > 0) {
            // manage timeout
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                    public void run()
                    {
                        dynCtx.setTimedOut();
                    }
                }, 
                timeOut);
        }

        // Init globals from bound values and/or from init expressions
        query.initGlobals(evalContext, externGlobals);
        // actually evaluate compiled expression:
        XQValue results = query.eval(this, evalContext);
        if(query.body.isUpdating()) {
            // need to expand: otherwise updates are not generated
            try {
                results = results.checkTypeExpand(null, evalContext, false, true);
            }
            catch(EmptyException ignored) { 
                results = XQValue.empty;
            }
            dynCtx.applyUpdates();
        }
        return results;
    }

    public void cancelEvaluation()
    {
        dynCtx.cancel();
    }

    public int getTimeOut()
    {
        return timeOut;
    }

    public void setTimeOut(int maxTime)
    {
        timeOut = maxTime;
    }

    public TraceObserver getTraceObserver()
    {
        return dynCtx.traceListener;
    }

    public void setTraceObserver(TraceObserver listener)
    {
        dynCtx.traceListener = listener;
    }
    
    public void setCompilationTrace(PrintWriter output) // not in API
    {
        dynCtx.compilationTrace = output;
    }

    public ItemSequence copySequence(ItemSequence sequence)
        throws EvaluationException
    {
        Check.nonNull(sequence, "sequence");
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

    public DynamicContext getDynCtx()
    {
        return dynCtx;
    }

    // --------------- implement Focus ------------------------------------

    public XQItem currentItem()
        throws EvaluationException
    {
        return currentItem;
    }

    public Node currentItemAsNode()
        throws EvaluationException
    {
        return currentItem == null? null : currentItem.getNode();
    }

    public double currentItemAsDouble()
        throws EvaluationException
    {
        return currentItem == null? 0 : currentItem.getDouble();
    }

    public long currentItemAsInteger()
        throws EvaluationException
    {
        return currentItem == null? 0 : currentItem.getInteger();
    }

    public String currentItemAsString()
        throws EvaluationException
    {
        return currentItem == null? null : currentItem.getString();
    }

    public long getLast()
        throws EvaluationException
    {
        return 1;
    }

    public long getPosition()
    {
        return 1;
    }

    public com.qizx.xquery.op.Expression getExpr()
    {
        return query.body;
    }

    // -------- hidden API ---------------------------------------------------

    public void dump(ExprDisplay exprDisplay)
    {
        query.dump(exprDisplay);
        try {
            exprDisplay.flush();
        }
        catch (DataModelException e) {
            e.printStackTrace();
        }
    }

    public void setUpdaterFactory(UpdaterFactory updaterFactory)
    {
        dynCtx.setUpdaterFactory(updaterFactory);
    }

    public Object getProperty(String name)
    {
        return dynCtx.getProperty(name);
    }

    public void setProperty(String name, Object value)
    {
        dynCtx.setProperty(name, value);
    }
}
