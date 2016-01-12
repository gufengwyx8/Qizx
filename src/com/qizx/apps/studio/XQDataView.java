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

import com.qizx.api.Node;
import com.qizx.apps.studio.gui.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

/**
 * Displays XML data or XQuery items.
 */
public class XQDataView extends TreePort
{
    protected AppFrame app;
    protected Node root;
    protected boolean useMarkup = true;
    protected JRadioButtonMenuItem markupButton;
   
    public XQDataView(AppFrame app, String title, boolean showRoot)
    {
        super(title, showRoot, null);
        this.app = app;
        changeFont(Font.BOLD, false);

        setTreeColors(getRenderer());
        
        addTool(displayModeMenu(), -1);
    }
    
    public static void setTreeColors(Renderer renderer)
    {
        renderer.setForeground(new Color(0x606060)); // default
        renderer.setColor(XmlNode.DOCU, new Color(0x7050A0));
        renderer.setColor(XmlNode.TAG, new Color(0x0090A0));
        renderer.setColor(XmlNode.ATTR, new Color(0x009070));
        renderer.setColor(XmlNode.COMMENT_STYLE, new Color(0x90a5a0));
        renderer.setColor(XmlNode.PI_STYLE, new Color(0xea731e));
        renderer.setColor(XmlNode.TEXT_STYLE, new Color(0x505050));
    }

    public Node getRootNode()
    {
        return root;
    }

    public void setRootNode(Node root)
    {
        this.root = root;
        if(root == null)
            changeRoot(null);
        else if(useMarkup)
            changeRoot(new XmlNode(root, this));
        else
            changeRoot(new XdmNode(root));

        expandAllVisible(40);
        getTree().scrollRowToVisible(0);
    }
    
    protected JButton addToolbarButton(BasicAction action, 
                                       String tip, String helpId)
    {
        JButton button = new JButton(action);
        button.setToolTipText(tip);
        Help.setHelpId(button, helpId);
        GUI.iconic(button);
        addTool(button, 0);
        return button;
    }

    MenuButton displayModeMenu()
    {
        MenuButton displayMenu = new MenuButton("View");
        displayMenu.setToolTipText("Display mode");
        GUI.iconic(displayMenu);
        
        JPopupMenu popup = displayMenu.getPopupMenu();
        ButtonGroup grp = new ButtonGroup();
        markupButton = new JRadioButtonMenuItem(
                              new BasicAction("Markup", "cmdMarkupView", this));
        JRadioButtonMenuItem dmButton = new JRadioButtonMenuItem(
                               new BasicAction("Data Model", "cmdDMView", this));
        popup.add(markupButton);
        popup.add(dmButton);
        grp.add(markupButton);
        grp.add(dmButton);
        markupButton.setSelected(true);
        return displayMenu;
    }
    
    public void cmdMarkupView(ActionEvent e, BasicAction a)
    {
        useMarkup = true;
        setRootNode(root);
    }
    
    public void cmdDMView(ActionEvent e, BasicAction a)
    {
        useMarkup = false;
        setRootNode(root);
    }
}
