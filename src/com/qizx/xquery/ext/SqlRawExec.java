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
import com.qizx.api.QName;
import com.qizx.xdm.IQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Prototype;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *  Implementation of function sql:rawExecute(PreparedStatement, parameters...).
 */
public class SqlRawExec extends Function
{
    static QName qname = IQName.get(SqlConnection.NS, "rawExec");
    static QName qname2 = IQName.get(SqlConnection.NS, "rawExecStatement");
    static Prototype[] protos = { 
        new Prototype(qname, XQType.NODE.star, RT1.class)
        .arg("connection", SqlConnection.TYPE_CONNECTION)
        .arg("statement", XQType.STRING),
        new Prototype(qname2, SqlConnection.TYPE_RESULTSET, RT2.class, true)
        .arg("statement", SqlConnection.TYPE_PSTATEMENT) 
        // .arg("parameters...", Type.ITEM)
    };

    public Prototype[] getProtos()
    {
        return protos;
    }


    public static class RT1 extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            try {
                Connection con = (Connection) objArg(args, 0, Connection.class,
                                                     focus, context);
                Statement stat = con.createStatement();
                String src = args[1].evalAsString(focus, context);
                ResultSet rs = stat.executeQuery(src);
                return new SqlConnection.RawResult(rs);
            }
            catch (SQLException e) {
                context.error("FXSQ0005", this,
                              new EvaluationException(e.getMessage(), e));
                return null; // dummy
            }
        }
    }


    public static class RT2 extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            PreparedStatement statement =
                (PreparedStatement) objArg(args, 0, PreparedStatement.class,
                                           focus, context);
            try {
                SqlConnection.Stat stat = new SqlConnection.Stat(statement);
                for (int p = 1; p < args.length; p++) {
                    XQItem param = args[p].evalAsOptItem(focus, context);
                    stat.setArg(p, param);
                }
                ResultSet rs = statement.executeQuery();
                return new SqlConnection.RawResult(rs);
            }
            catch (SQLException e) {
                context.error("FXSQ0006", this,
                              new EvaluationException(e.getMessage(), e));
                return null; // dummy
            }
        }
    }
}
