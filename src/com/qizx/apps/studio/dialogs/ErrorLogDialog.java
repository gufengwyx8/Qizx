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
package com.qizx.apps.studio.dialogs;

import com.qizx.apps.studio.Help;
import com.qizx.apps.studio.gui.DialogBase;
import com.qizx.apps.studio.gui.GridBagger;
import com.qizx.apps.studio.gui.TextPort;
import com.qizx.apps.studio.gui.TextPortLogger;

import java.awt.Dimension;
import java.awt.Frame;
import java.util.logging.Level;

public class ErrorLogDialog extends DialogBase
{
    private TextPort textArea;
    private TextPortLogger appender;
    protected boolean autoShow = true;
    
    public ErrorLogDialog(Frame parent)
    {
        super(parent, parent.getTitle() + " Error log");
        haveOnlyCloseButton();
        
        setModal(false);
        Help.setDialogHelp(this, "error_dialog");
        
        textArea = new TextPort("Error messages", 60);
        GridBagger grid = new GridBagger(form, 0, 0);
        grid.add(textArea, grid.prop("fill"));
        textArea.setPreferredSize(new Dimension(600, 400));
        appender = new TextPortLogger(textArea) {
            protected void newEvent(Level level)
            {
                if(autoShow && level.intValue() >= Level.WARNING.intValue())
                    showUp();
            }
        };

        pack();
    }

    public boolean isAutoShowing()
    {
        return autoShow;
    }

    public void setAutoShowing(boolean autoShow)
    {
        this.autoShow = autoShow;
    }

    public TextPortLogger getLogger()
    {
        return appender;
    }

}
