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
package com.qizx.util.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * An implementation of {@link Formatter} for Qizx log files.
 */
public class LogFormatter extends Formatter
{
    private static final int MAX_STACK = 10;
    private int stackMax = MAX_STACK;
    
    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    
    @Override
    public String format(LogRecord record)
    {
        StringBuilder buf = new StringBuilder(64);
        buf.append(dateFormat.format(new Date(record.getMillis())));
        buf.append("  ");
        buf.append(convertLevel(record.getLevel()));
        buf.append(" ");
        buf.append(record.getMessage());
        buf.append("\r\n");
        if(record.getThrown() != null)
            printException(buf, record.getThrown());
        
        return buf.toString();
    }

    public int getStackMax()
    {
        return stackMax;
    }

    public void setStackMax(int stackMax)
    {
        this.stackMax = stackMax;
    }

    public void printException(StringBuilder buf, Throwable error)
    {
        while (error != null) {
            StackTraceElement[] stack = error.getStackTrace();
            for(int s = 0; s < stack.length && s < stackMax; s++) {
                buf.append("        ");
                buf.append(stack[s].toString());
                buf.append("\n");
            }
            error = error.getCause();
            if(error != null)
                buf.append(" caused by:\n");                
        }
    }

    private String convertLevel(Level level)
    {
        if(level == Level.SEVERE)
            return "ERROR";
        if(level == Level.WARNING)
            return "WARN ";
        if(level == Level.INFO)
            return "INFO ";
        if(level == Level.FINE)
            return "DEBUG ";
        return level.toString();
    }

}
