/*
 *    Qizx/open 4.1
 *
 * This code is part of the Qizx application components
 * Copyright (C) 2004-2010 Axyana Software -- All rights reserved.
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
package com.qizx.apps.util;

import com.qizx.util.logging.LogFormatter;

import java.util.logging.Handler;
import java.util.logging.LogRecord;


public class ConsoleLogger extends Handler
{
    public ConsoleLogger()
    {
        setFormatter(new LogFormatter());
    }

    @Override
    public void publish(LogRecord record)
    {
        if (!isLoggable(record))
            return;

        System.err.print(getFormatter().format(record));
        System.err.flush();
    }

    @Override
    public void flush()
    {
    }

    @Override
    public void close()
        throws SecurityException
    {
    }
}
