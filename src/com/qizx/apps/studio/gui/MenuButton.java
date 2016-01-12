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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;

/**
 * Drop-down menu. A button that brings up a popup menu.
 * Different Look and feel than JMenu in JMenubar.
 */
public class MenuButton extends JButton
{
    private static ImageIcon dropIcon = GUI.getIcon("dropdown.png");
    private JPopupMenu popup;
    
    public MenuButton(Action a)
    {
        super(a);
        init();
    }

    public MenuButton(Icon icon)
    {
        super(icon);
        init();
    }

    public MenuButton(String text, Icon icon)
    {
        super(text, icon);
        init();
    }

    public MenuButton(String text)
    {
        super(text);
        init();
    }

    public MenuButton() {
        init();
    }
    
    private void init()
    {
        popup = new JPopupMenu();
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                popupHook();
                popup.show(MenuButton.this, 0, getHeight());
            }
        });
        setHorizontalTextPosition(SwingConstants.LEFT);
        setIcon(dropIcon);
        setIconTextGap(3);
    }

    public JPopupMenu getPopupMenu() {
        return popup;
    }
    
    /**
     * Called just before the popup appears. Can be redefined for updating
     * the menu.
     */
    protected void popupHook() { }
    
}
