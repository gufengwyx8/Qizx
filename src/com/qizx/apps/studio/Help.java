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
package com.qizx.apps.studio;

import com.qizx.apps.studio.gui.DialogBase;
import com.qizx.apps.studio.gui.GUI;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.Locale;

import javax.help.BadIDException;
import javax.help.CSH;
import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.swing.AbstractButton;
import javax.swing.JButton;

public class Help
{
    private static String language = null;
    private static HelpBroker helpBroker = null;

    private static boolean initialized;
    private static CSH.DisplayHelpAfterTracking delegate;
    private static String helpId = "startPoint";
    
    public static void initialize()
    {
        language = Locale.getDefault().getLanguage();

        URL helpSetURL =
            Help.class.getResource("/qizxstudio_help_" + language + "/jhelpset.hs");
        if (helpSetURL == null)
            helpSetURL = Help.class.getResource("/qizxstudio_help/jhelpset.hs");
        if (helpSetURL == null) {
            //GUI.error("Help files not found");
            return;
        }

        try {
            HelpSet helpSet = new HelpSet(null, helpSetURL);
            helpBroker = helpSet.createHelpBroker();
            helpBroker.setSize(new Dimension(800, 800));
        }
        catch (HelpSetException ignored) {
        }
    }

    public static void addHelpSet(String name)
    {
        if (helpBroker == null)
            return;

        URL helpSetURL =
            Help.class.getResource("/" + name + "_" + language
                                   + "/jhelpset.hs");
        if (helpSetURL == null)
            helpSetURL = Help.class.getResource("/" + name + "/jhelpset.hs");
        if (helpSetURL == null)
            return;

        try {
            HelpSet helpSet = new HelpSet(null, helpSetURL);
            helpBroker.getHelpSet().add(helpSet);
        }
        catch (HelpSetException ignored) {
        }
    }

    public static HelpBroker getHelpBroker()
    {
        return helpBroker;
    }

    public static void setHelpId(Component component, String id)
    {
        if (helpBroker != null)
            //CSH.setHelpIDString(component, id);
            helpBroker.enableHelp(component, id, null);
    }

    public static void setHelpButton(AbstractButton button, String id)
    {
        if (helpBroker == null)
            button.setEnabled(false);
        else
            helpBroker.enableHelpOnButton(button, id, null);
    }

    public static void setDialogHelp(DialogBase dialog, String id)
    {
        JButton button = dialog.getHelpButton();
        setHelpButton(button, id);
    }

    public static void setContextualHelpButton(AbstractButton button)
    {
        if (helpBroker == null)
            button.setEnabled(false);
        else
            button.addActionListener(new CSH.DisplayHelpAfterTracking(helpBroker));
    }
    
    
    public static void showHelp()
    {
        HelpBroker helpBroker = getHelpBroker();
        if (helpBroker != null) {
            helpBroker.setDisplayed(true);
            if (helpId != null) {
                try {
                    helpBroker.setCurrentID(helpId);
                }
                catch (BadIDException ignored) {}
            }
        }
    }

    public static void contextualHelp(Object source)
    {

        if (!initialized) {
            initialized = true;

            HelpBroker helpBroker = getHelpBroker();
            if (helpBroker != null) 
                delegate = new CSH.DisplayHelpAfterTracking(helpBroker);
        }

        if (delegate != null) {
            ActionEvent e = new ActionEvent(source, //app.getDialogParent(),
                                            ActionEvent.ACTION_PERFORMED, 
                                            /*command*/ null);
            try {
                delegate.actionPerformed(e);
            }
            catch (BadIDException ex) {
                showHelp(); // fallback
            }
        }
    }
}
