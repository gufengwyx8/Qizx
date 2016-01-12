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


import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.ItemType;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.api.util.time.DateTimeBase;
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.CorePushBuilder;
import com.qizx.xdm.IQName;
import com.qizx.xquery.BaseValue;
import com.qizx.xquery.PredefinedModule;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.*;

import java.sql.*;

/**
 *  SQL Database connection via JDBC.
 *  
 *  Wrapper class for static methods.
 */
public class SqlConnection 
{
    public static String NS = "java:com.qizx.xquery.ext.SqlConnection";
    
    public static XQItemType TYPE_CONNECTION =
        new WrappedObjectType(Connection.class);
    public static XQItemType TYPE_RESULTSET =
        new WrappedObjectType(ResultSet.class);
    public static XQItemType TYPE_PSTATEMENT =
        new WrappedObjectType(PreparedStatement.class);
    
    public static void plugHook(PredefinedModule module) throws EvaluationException{
        if(module.simpleFunctionLookup(IQName.get(NS, "execute")) != null)
            return;
        module.declareFunction(new SqlExecQuery());
        module.declareFunction(new SqlExecUpdate());
        module.declareFunction(new SqlRawExec());
    }
    
    /**
     *	Explicitly registers a driver.
     *	(not necessary if system property jdbc.drivers is used).
     */
    public static void registerDriver(String className) throws Exception {
        // The newInstance() call is a work around for some 
        // broken Java implementations (?)
        Class.forName(className).newInstance(); 
    }
    
    /**
     *	Opens a Connection.
     */
    public static Connection getConnection(String url, String user, String passwd)
        throws SQLException
    {
        Connection conn = DriverManager.getConnection(url, user, passwd);
        return conn;
    }
    
    /**
     *	Closes a Connection.
     */
    public static void close( Connection conn )
    throws SQLException {
        conn.close();
    }

    /**
     * Creates a prepared statement. Just to make it easier: write
     * sql:prepare($conn, "...") rather than that:
     * 
     * <pre>
     * declare namespace jdbc = "java:java.sql";
     * jdbc:Connection.prepareStatement($conn, "...")
     * </pre>
     */
    public static PreparedStatement prepare(Connection conn, String statement)
        throws SQLException
    {
        return conn.prepareStatement(statement);
    }


    public static class RawResult extends BaseValue
    {
        static QName ROW = IQName.get("row");
        
        ResultSet rset;
        ResultSetMetaData meta = null;
        QName[] colNames;
        
        RawResult(ResultSet rset) {
            this.rset = rset;
        }
        
        public boolean next() throws EvaluationException
        {
            try {
                return rset.next();
            }
            catch (SQLException e) {
                //e.printStackTrace();
                return false;
            }
        }
        
        public XQItem getItem() {
            return new SingleWrappedObject(rset);
        }
        
        public String  getString() {
            return rset.toString();
        }
        
        public XQValue bornAgain() {
            // cant clone the result set: Problem! might give strange results
            // if reset (beforeFirst) not implemented.
            try {
                rset.beforeFirst();
            }
            catch (SQLException e) { }	// just try
            return new RawResult(rset); 
        }
    }
    
    /**
     *	Node wrapper: presents the result set as a sequence of nodes.
     *	<p>Each node represents a row of data. Its children elements have
     *	the names of fields returned by the query.
     *	<p>Caches the node that represents the current row in result set.
     */
    public static class Result extends RawResult
    {
        CorePushBuilder builder = new CorePushBuilder(null);
        BasicNode currentNode;
      
        Result(ResultSet rset) {
            super(rset);
        }
        
        public boolean next() throws EvaluationException
        {
            try {
                if(!rset.next())
                    return false;

                if (meta == null) {
                    meta = rset.getMetaData();
                    int ccnt = meta.getColumnCount();
                    colNames = new QName[ccnt];
                    for (int c = 1; c <= ccnt; c++) {
                        colNames[c - 1] = IQName.get(meta.getColumnLabel(c));
                        // System.err.println("type "+meta.getColumnType(c)+"
                        // "+meta.getColumnName(c));
                    }
                }
                builder.reset();
                builder.putElementStart(ROW);
                for (int c = 0; c < colNames.length; c++) {
                    builder.putElementStart(colNames[c]);
                    String s = rset.getString(c + 1); // TODO conversion
                    if (s != null)
                        builder.putAtomText(s);
                    builder.putElementEnd(colNames[c]);
                }
                builder.putElementEnd(ROW);
                currentNode = builder.harvest();
                return true;
            }
            catch (SQLException e) {
                throw new EvaluationException("error in row construction", e);
            }
            catch (DataModelException e) {
                return false;    // should not happen
            }
        }

        public ItemType getType() {
            return XQType.NODE;
        }
        
        public XQItem getItem() {
            return (XQItem) getNode();
        }
        
        public boolean  isNode() {
            return true;
        }
        
        public Node getNode()
        {
            return currentNode;
        }
        
        public XQValue bornAgain() {
            // cant clone the result set: Problem! might give strange results
            // if reset (beforeFirst) not implemented.
            try {
                rset.beforeFirst();
            }
            catch (SQLException e) { }	// just try
            return new Result(rset); 
        }
    } // end of class Result
    
    /**
     *	
     */
    static class Stat
    {
        PreparedStatement stat;
        ParameterMetaData meta;

        Stat(PreparedStatement st)
        {
            stat = st;
        }

        void setArg(int rank, XQItem value)
            throws EvaluationException, SQLException
        {
            // -System.err.println(rank+" = "+value);
            if (value == null) {
                if (meta == null)
                    meta = stat.getParameterMetaData();
                stat.setNull(rank, meta.getParameterType(rank));
                return;
            }

            ItemType type = value.getType();
            if (value instanceof StringValue) {
                stat.setString(rank, value.getString());
            }
            else if (type.isSubTypeOf(XQType.INTEGER)) {
                long v = value.getInteger();
                stat.setLong(rank, v);
            }
            else if (type.isSubTypeOf(XQType.NUMERIC)) {
                if (type == XQType.DOUBLE)
                    stat.setDouble(rank, ((DoubleValue) value).getDouble());
                else if (type == XQType.DECIMAL)
                    stat.setBigDecimal(rank, ((DecimalValue) value).getValue());
                else if (type == XQType.FLOAT)
                    stat.setFloat(rank, ((FloatValue) value).getFloat());
                else
                    System.err.println("OOPS NUMERIC: " + value);
            }
            else if (value instanceof MomentValue) {
                DateTimeBase dt = ((MomentValue) value).getValue();
                stat.setDate(rank,
                             new Date((long) dt.getMillisecondsFromEpoch()));
            }
        }
    } // end of class Stat
}
