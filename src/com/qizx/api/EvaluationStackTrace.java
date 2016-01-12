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
package com.qizx.api;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A level in an evaluation Stack Trace. Points to a location in a source
 * query expression or module.
 * <p>
 * Returned by an EvaluationException or by the future debugger interface.
 */
public class EvaluationStackTrace
{
    private String signature;
    private String moduleURI;
    private String sourceLine;

    private int lineNumber;
    private int columnNumber;
    private int position;

    /**
     * For internal use.
     * @param signature
     * @param moduleURI
     * @param lineNumber
     * @param srcLine
     * @param columnNumber
     * @param position
     */
    public EvaluationStackTrace(String signature, String moduleURI,
                                int lineNumber, String srcLine, 
                                int columnNumber, int position)
    {
        this.signature = signature;
        this.moduleURI = moduleURI;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.position = position;
        this.sourceLine = srcLine;
    }

    /**
     * Returns the signature of the called function, or null in the outermost
     * stack level (main query).
     * @return a printable form of the function signature (name, arguments,
     *         returned type)
     */
    public String getSignature()
    {
        return signature;
    }

    /**
     * Returns the URI of the module location for this stack level.
     * @return the physical URI of the module location. Can be null for the
     *         main query.
     */
    public String getModuleURI()
    {
        return moduleURI;
    }

    /**
     * Gets the character position of the evaluation point in this stack level.
     * @return the character position of the evaluation point in the source
     *         XQuery expression.
     */
    public int getPosition()
    {
        return position;
    }

    /**
     * Gets the column number of the evaluation point in this stack level.
     * @return the column number of the evaluation point in the source XQuery
     *         expression.
     */
    public int getColumnNumber()
    {
        return columnNumber;
    }

    /**
     * Gets the line number of the evaluation point in this stack level.
     * @return the line number of the evaluation point in the source XQuery
     *         expression.
     */
    public int getLineNumber()
    {
        return lineNumber;
    }

    /**
     * Gets the text of the line in the source code where the evaluation pointer sits.
     * @return the source line without line terminator
     */
    public String getSourceLine()
    {
        return sourceLine;
    }

    /**
     * Prints the location and the function signature if any.
     * @param output a PrintWriter output
     */
    public void print(PrintWriter output)
    {
        String sig = getSignature();
        if(sig == null)
            sig = "main query";
        output.print(" in " + sig +
                     " at line " + lineNumber + " column " + columnNumber);
        if(moduleURI != null)
            output.print(" in " + moduleURI);
        output.println();
    }
    
    
    public String toString()
    {
        StringWriter out = new StringWriter();
        print(new PrintWriter(out));
        return out.toString();
    }

}
