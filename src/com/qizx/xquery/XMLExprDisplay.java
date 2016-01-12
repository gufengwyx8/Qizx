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

import com.qizx.api.DataModelException;
import com.qizx.api.QName;
import com.qizx.api.XMLPushStream;
import com.qizx.api.util.XMLSerializer;
import com.qizx.util.basic.Util;
import com.qizx.xdm.IQName;
import com.qizx.xquery.op.Expression;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Displays a compiled expression as a XML document. 
 * This is NOT a XQueryX generator! (we'll rather use a Visitor)
 */
public class XMLExprDisplay extends ExprDisplay
{
    private XMLPushStream out;
    private ArrayList nameStack = new ArrayList();
    private boolean displayTypes = true;
    private boolean displayFlags = true;
    
    public XMLExprDisplay(XMLPushStream output)
    {
        this.out = output;
    }

    public XMLExprDisplay()
    {
        XMLSerializer ser = new XMLSerializer(new PrintWriter(System.err, true));
        ser.setIndent(2);
        out = ser;
    }

    // @see com.qizx.xquery.XDisplay#header(com.qizx.api.QName)
    public void header(QName name)
    {
        nameStack.add(name);
        try {
            out.putElementStart(name);
        }
        catch (DataModelException e) {
            e.printStackTrace();
        }
    }

    // @see com.qizx.xquery.XDisplay#header(java.lang.String)
    public void header(String name)
    {
        header(IQName.get(name));
    }

    // @see com.qizx.xquery.XDisplay#header(com.qizx.xquery.op.Expression)
    public void header(Expression expr)
    {
        QName name = IQName.get(Util.shortClassName(expr.getClass()));
        header(name);
        headerInfo(expr);
    }

    public void headerInfo(Expression expr)
    {
        // flags etc
        if(displayTypes)
            property("type", expr.getType());
        int flags = expr.getFlags();
        if(displayFlags && flags != 0) {
            StringBuffer b = new StringBuffer();
            if((flags & Expression.UNORDERED) != 0)
                b.append("Un,");
            if((flags & Expression.DOCUMENT_ORDER) != 0)
                b.append("DO,");
            if((flags & Expression.SAME_DEPTH) != 0)
                b.append("=D,");
            if((flags & Expression.CONSTANT) != 0)
                b.append("Cst,");
            if((flags & Expression.NUMERIC) != 0)
                b.append("Nu,");
            if((flags & Expression.UPDATING) != 0)
                b.append("Up,");
            if((flags & Expression.WITHIN_NODE) != 0)
                b.append("inN,");
            if((flags & Expression.WITHIN_SUBTREE) != 0)
                b.append("inT,");
            property("flags", b.toString());
        }
    }

    // @see com.qizx.xquery.XDisplay#end()
    public void end()
    {
        QName name = (QName) nameStack.remove(nameStack.size() - 1);
        try {
            out.putElementEnd(name);
        }
        catch (DataModelException e) {
            reportError(e);
        }
    }

    // @see com.qizx.xquery.XDisplay#property(java.lang.String, java.lang.String)
    public void property(String prop, String value)
    {
        try {
            if(value == null)
                value = "<null>";
            out.putAttribute(IQName.get(prop), value, null);
        }
        catch (DataModelException e) {
            reportError(e);
        }
    }

    public void property(String prop, XQType targetType)
    {
        if(targetType != null)
            property(prop, targetType.toString());
    }

    // wrapped Child expression
    public void child(String role, Expression expr)
    {
        QName roleName = IQName.get(role);
        try {
            out.putElementStart(roleName);
            child(expr);
            out.putElementEnd(roleName);
        }
        catch (DataModelException e) {
            reportError(e);
        }
    }

    public void children(String role, Expression[] args)
    {
        QName roleName = IQName.get(role);
        try {
            out.putElementStart(roleName);
            children(args);
            out.putElementEnd(roleName);
        }
        catch (DataModelException e) {
            reportError(e);
        }
    }

    // pseudo Child
    public void child(String role, String value)
    {
        QName roleName = IQName.get(role);
        try {
            out.putElementStart(roleName);
            out.putText(value);
            out.putElementEnd(roleName);
        }
        catch (DataModelException e) {
            reportError(e);
        }
    }


    private void reportError(DataModelException e)
    {
         e.printStackTrace();
    }

    public XMLPushStream getOutput()
    {
        return out;
    }

    public void setOutput(XMLPushStream out)
    {
        this.out = out;
    }

    public void flush() throws DataModelException
    {
         out.flush();
    }
}
