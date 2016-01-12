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
package com.qizx.api.util.logging;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Implementation of {@link Log} on top of an OutputStream.
 */
public class StreamLog
    implements Log
{
    private static final int MAX_STACK = 10;
    
    private OutputStream output;
    private PrintStream printer;
    private Level level;
    private int ilevel;
    private int errCnt;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    public StreamLog(OutputStream output)
    {
        this.output = output;
        this.printer = new PrintStream(output, true);
    }
    
    public String getName()
    {
        return "streamlog";
    }

    public int getWorkLevel()
    {
        return ilevel;
    }

    public Level getLevel()
    {
        return level;
    }

    public void setLevel(Level level)
    {
        this.level = level;
        this.ilevel = level.level;
    }

    public int getErrorCount()
    {
        return errCnt;
    }

    public void setErrorCount(int count)
    {
        errCnt = count;
    }

    public int getWarningCount()
    {
        return 0;
    }

    public void setWarningCount(int count)
    {
    }

    public boolean allowsDebug()
    {
        return ilevel >= LOG_DEBUG;
    }

    public boolean allowsError()
    {
        return ilevel >= LOG_ERROR;
    }

    public boolean allowsInfo()
    {
        return ilevel >= LOG_INFO;
    }

    public boolean allowsWarning()
    {
        return ilevel >= LOG_WARNING;
    }

    public void error(String message)
    {
        error(message, null);
    }

    public void error(String message, Throwable error)
    {
        ++ errCnt;
        output("ERROR", message, error);
    }

    public void warning(String message)
    {
        warning(message, null);
    }

    public void warning(String message, Throwable error)
    {
        output("WARN ", message, error);
    }

    public void info(String message)
    {
        output("INFO ", message, null);
    }

    public void info(String message, Throwable error)
    {
        output("INFO ", message, error);
    }

    public void debug(String message)
    {
        output("DEBUG", message, null);
    }

    public void debug(String message, Throwable error)
    {
        output("DEBUG", message, error);
    }

    protected void output(String lev, String message, Throwable error)
    {
        printer.print(dateFormat.format(new Date()));
        printer.print("  ");
        printer.print(lev);
        printer.print(" ");
        printer.print(message);
        printer.println();
        if(error != null)
            printException(error);
        printer.flush();
    }
    
    public void printException(Throwable error)
    {
        while (error != null) {
            StackTraceElement[] stack = error.getStackTrace();
            for(int s = 0; s < stack.length && s < MAX_STACK; s++) {
                printer.append("        ");
                printer.append(stack[s].toString());
                printer.append("\n");
            }
            error = error.getCause();
            if(error != null)
                printer.append(" caused by:\n");                
        }
    }
}
