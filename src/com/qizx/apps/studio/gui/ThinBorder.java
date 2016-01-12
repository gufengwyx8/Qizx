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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.AbstractBorder;

/**
 * A raised or sunken 1-pixel wide border (Swing BevelBorder is 2-pixel wide).
 */
public class ThinBorder extends AbstractBorder
{
    private Color dark;
    private Color light;
    private boolean raised;
    private int marginWidth, marginHeight;
    
    public ThinBorder(Color color, boolean raised)
    {
        this(color, raised, -1, -1);
    }
    
    public ThinBorder(Color color, boolean raised, 
                      int marginWidth, int marginHeight)
    {
        if (marginWidth < 0)
            marginWidth = 2;
        if (marginHeight < 0)
            marginHeight = 2;
        
        dark = color.darker();
        light = color.brighter();
        this.raised = raised;
        this.marginWidth = marginWidth;
        this.marginHeight = marginHeight;
    }
    
    public void paintBorder(Component c, Graphics g, int x, int y, 
                            int width, int height)
    {
        // Graphics is in fact a SwingGraphics where draw3DRect has been
        // overridden.
        
        g.setColor(raised? dark : light);
        g.drawLine(x, y + height - 1,
                   x + width - 1, y + height - 1);
        g.drawLine(x + width - 1, y + height - 1,
                   x + width - 1, y);
        g.setColor(raised? light : dark);
        g.drawLine(x, y, x + width - 1, y);
        g.drawLine(x, y, x, y + height - 1);
    }
    
    public Insets getBorderInsets(Component c)
    {
        return new Insets(1 + marginHeight, 1 + marginWidth, 
                          1 + marginHeight, 1 + marginWidth);
    }
}
