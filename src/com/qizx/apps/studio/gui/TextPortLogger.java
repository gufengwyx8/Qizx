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
package com.qizx.apps.studio.gui;

import com.qizx.util.logging.LogFormatter;

import java.awt.Color;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.text.Style;


public class TextPortLogger extends Handler
{
    protected TextPort textPort;
    protected Style errorStyle;
    protected Style warningStyle;

    public TextPortLogger(TextPort textPort)
    {
        this.textPort = textPort;
        LogFormatter formatter = new LogFormatter();
        formatter.setStackMax(0);
        setFormatter(formatter);
        errorStyle = textPort.addStyle("error", Color.red, false);
        warningStyle = textPort.addStyle("warning", new Color(240, 100, 0), false);
    }

    protected void newEvent(Level level)
    {
         // redefinable
    }

    @Override
    public void publish(LogRecord record)
    {
        if(!isLoggable(record))
            return;
        Style style = null;
        if(record.getLevel() == Level.SEVERE)
            style = errorStyle;
        else if(record.getLevel() == Level.WARNING)
            style = warningStyle;
        textPort.appendText(getFormatter().format(record), style);
        newEvent(record.getLevel());
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
