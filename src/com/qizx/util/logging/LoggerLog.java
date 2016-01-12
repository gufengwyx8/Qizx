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

import com.qizx.api.util.logging.Log;
import com.qizx.api.util.logging.Log.Level;

import java.util.logging.Logger;

/**
 * Adapts a Java {@link Logger} to the Log interface.
 */
public class LoggerLog
    implements Log
{
    private Logger logger;
    private String prefix = "";

    private int errorCount;
    private boolean allowDebug;
    private boolean allowInfo;
    
    
    public LoggerLog(Logger logger, String name)
    {
        this.logger = logger;
        if(name != null)
            prefix = "[" + name + "] ";
    }
    
    public String getName()
    {
        return logger.getName();
    }

    public Level getLevel()
    {
        allowInfo = false;
        allowDebug = false;
        java.util.logging.Level lev = logger.getLevel();
        if(lev == java.util.logging.Level.SEVERE)
            return Log.ERROR;
        if(lev == java.util.logging.Level.WARNING)
            return Log.WARNING;
        if(lev == java.util.logging.Level.INFO) {
            allowInfo = true;
            return Log.INFO;
        }
        allowDebug = true;
        return Log.DEBUG;
    }

    public void setLevel(Level level)
    {
        // TODO
    }

    public int getWarningCount()
    {
        return 0;
    }

    public void setWarningCount(int count)
    {
    }

    public int getErrorCount()
    {
        return errorCount;
    }

    public void setErrorCount(int count)
    {
        errorCount = count;
    }

    public int getWorkLevel()
    {
        getLevel();
        return logger.getLevel().intValue();    // ?
    }

    public boolean allowsError()
    {
        return logger.isLoggable(java.util.logging.Level.SEVERE);
    }

    public boolean allowsWarning()
    {
        return logger.isLoggable(java.util.logging.Level.WARNING);
    }

    public boolean allowsInfo()
    {
        return allowInfo;
    }

    public boolean allowsDebug()
    {
        return allowDebug;
    }

    public void error(String message)
    {
        logger.severe(prefix + message);
    }

    public void error(String message, Throwable error)
    {
        logger.log(java.util.logging.Level.SEVERE, prefix + message, error);
    }

    public void warning(String message)
    {
        logger.warning(prefix + message);
    }

    public void warning(String message, Throwable error)
    {
        logger.log(java.util.logging.Level.WARNING, prefix + message, error);
    }

    public void info(String message)
    {
        if(allowInfo)
            logger.info(prefix + message);
    }

    public void info(String message, Throwable error)
    {
        if(allowInfo)
            logger.log(java.util.logging.Level.INFO, prefix + message, error);
    }

    public void debug(String message)
    {
        if(allowDebug)
            logger.fine(prefix + message);
    }

    public void debug(String message, Throwable error)
    {
        if(allowDebug)
            logger.log(java.util.logging.Level.FINE, prefix + message, error);
    }
}
