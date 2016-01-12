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
package com.qizx.xquery.ext;

import com.qizx.api.EvaluationException;
import com.qizx.xdm.IQName;
import com.qizx.xquery.DynamicContext;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Prototype;

import java.sql.SQLException;

public class SqlConnect  extends Function
{
    private static final String SQL_CONNECTOR = "sql-connector";
    static final IQName fname = IQName.get(SqlConnection.NS, "connect");
    static Prototype[] protos = { 
        new Prototype(fname, XQType.NODE.star, RT.class, true)
            .arg("driver-URL", XQType.STRING)
            // .arg("parameters...", Type.ITEM)
    };
    
    public Prototype[] getProtos() { return protos; }

    
    public static class RT extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            try {                
                String cxId = args[0].evalAsString(focus, context);
                String url = args[1].evalAsString(focus, context);
                
                return getConnector(context).connect(cxId, url);
            }
            catch (SQLException e) {
                return context.error("FXSQ0003", this,
                                     new EvaluationException(e.getMessage(), e));
            }
        }
    }
    
    static SQLConnector getConnector(EvalContext context)
    {
        DynamicContext dcx = context.dynamicContext();
        SQLConnector cx = (SQLConnector) dcx.getSessionProperty(SQL_CONNECTOR);
        if(cx == null) {
            cx = new SQLConnector();
            dcx.setSessionProperty(SQL_CONNECTOR, cx);
        }
        return cx;
    }


    /**
     * Pool of JDBC connections and statements, attached to a XQuery session.
     * Allows reuse of 
     */
    public static class SQLConnector
    {
        
        
        
        public XQValue connect(String cxId, String url)
            throws SQLException
        {
            return null; // TODO
        }

        public XQValue execute(String cxId, String stmt)
            throws SQLException
        {
            return null; // TODO
        }

    }
}
