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

/**
 * Message (error or warning) returned by compilation errors.
 */
public class Message
{
    /**
     * Message type returned by {@link #getType()} for an error.
     */
    public static final int ERROR = 1;
    /**
     * Message type returned by  {@link #getType()} for a warning.
     */
    public static final int WARNING = 2;
    /**
     * Message type returned by {@link #getType()} for an auxiliary information
     * message.
     */
    public static final int DETAIL = 3;

    private int type;
    private QName errorCode;
    private String text;
    private String moduleURI;
    private int lineNumber;
    private int columnNumber;
    private int position;
    private String sourceLine;

    /**
     * For internal use.
     * @param type
     * @param code
     * @param text
     * @param moduleURI
     * @param position
     * @param lineNumber
     * @param columnNumber
     * @param srcLine
     */
    public Message(int type, QName code, String text, 
                   String moduleURI,
                   int position, int lineNumber, int columnNumber,
                   String srcLine)
    {
        this.type = type;
        this.errorCode = code;
        this.text = text;
        this.moduleURI = moduleURI;
        this.position = position;
        this.lineNumber = lineNumber;
        this.columnNumber = (columnNumber);
        this.sourceLine = srcLine;
    }

    /**
     * Gets the XQuery error code.
     * @return the code as a Qualified Name. The namespace of this name is
     * "http://www.w3.org/2005/xqt-errors" (See XQuery Specifications 2.3.2)
     */
    public QName getErrorCode()
    {
        return errorCode;
    }

    /**
     * Gets the related position in the line (or column number).
     * The first column is 0.
     * @return the column number
     */
    public int getColumnNumber()
    {
        return columnNumber;
    }

    /**
     * Gets the related line number (first line is 1).
     * @return the line number
     */
    public int getLineNumber()
    {
        return lineNumber;
    }

    /**
     * Gets the URI of the concerned XQuery module.
     * @return the module URI
     */
    public String getModuleURI()
    {
        return moduleURI;
    }

    /**
     * Gets the message text.
     * @return the message text
     */
    public String getText()
    {
        return text;
    }

    /**
     * Gets the message type (ERROR or WARNING).
     * @return the message type
     */
    public int getType()
    {
        return type;
    }

    /**
     * Gets the related character position in the whole source script.
     * @return the character position, starting from 0
     */
    public int getPosition()
    {
        return position;
    }

    /**
     * Gets the text of the related line in the source code.
     * @return the text of the related line without line terminator
     */
    public String getSourceLine()
    {
        return sourceLine;
    }

    /**
     * Prints a single message.
     * @param output print output
     * @param withSource if true, print the concerned source line with a mark
     * under the point where the message is located
     */
    public void print(PrintWriter output, boolean withSource)
    {
        if(type == DETAIL) {
            output.println(getText());
            return;
        }
        output.print(type == ERROR? "* error" : "* warning");
        output.print(" at line " + getLineNumber() 
                     + " column " + (getColumnNumber() + 1));
        if(moduleURI != null)
            output.print(" in " + getModuleURI());
        output.println();
        if(withSource) {
            output.println(getSourceLine());
            for(int i = 0; i < getColumnNumber(); i++)
                output.print('-');
            output.println("^");
        }
        output.println("  " + errorCode.getLocalPart() + ": " + getText());
    }
    
    /**
     * Returns a displayable form of the message.
     * @return displayable string with location and message text
     */
    public String toString()
    {
        if(type == DETAIL)
            return getText();
        return (type == ERROR? "Error" : "Warning")
            + " at line " + getLineNumber() 
            + " column " + (getColumnNumber() + 1)
            + (moduleURI != null? (" in " + getModuleURI()) : "") + "\n  "
            + getText();
    }
}
