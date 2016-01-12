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

import java.awt.Dimension;
import java.awt.Font;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.*;

/**
 * Displays the memory use at regular intervals.
 */
public class MemoryStatus extends Box
{
    private static final int MB = 1048076;
    private JProgressBar display;
    Runtime runtime = Runtime.getRuntime();
    
    public MemoryStatus()
    {
        this(-1);
    }
    
    public MemoryStatus(int period)
    {
        super(BoxLayout.X_AXIS);
        JLabel label = new JLabel("Memory: ");
        Font smallFont = GUI.changeSize(label.getFont(), -2);
        label.setFont(smallFont);
        add(label);
        
        int maxMemory = (int) (runtime.maxMemory() / MB);

        display = new JProgressBar(0, maxMemory);
        display.setStringPainted(true);
        display.setPreferredSize(new Dimension(100, 15));
        display.setFont(smallFont);
        add(display);

        if(period > 0)
            setBeat(period);
    }
    
    public void displayState()
    {
        long totalMemory = runtime.totalMemory();
        long usedMemory = (totalMemory - runtime.freeMemory()) / MB;
        int maxMemory = (int) (runtime.maxMemory() / MB);
        display.setValue((int) usedMemory);
        display.setString((usedMemory * 100 / maxMemory) + "% of "
                          + maxMemory + "Mb");
    }
    
    public void setBeat(int period)
    {
        TimerTask task = new TimerTask() {
            public void run()
            {
                 displayState();
            }
        };
        new Timer().schedule(task, 0, period);
    }
}
