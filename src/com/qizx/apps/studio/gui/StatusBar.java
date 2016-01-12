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

import java.util.Timer;
import java.util.TimerTask;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Status bar placed at the bottom of a NGIS app.
 * <p>Supports basically a message display area.
 * Other areas can be added on both sides of the message area.
 */
public class StatusBar extends JPanel
{
    private static final long DEFAULT_DURATION = 10000;  // ms
    private Box leftSide;
    private JLabel messageArea;
    private Box rightSide;
    private TimerTask clearTask;
    private Timer timer = new Timer();
    private long tmDuration = DEFAULT_DURATION;
    
    public StatusBar()
    {
        setBorder(null); //BorderFactory.createEtchedBorder());
        GridBagger bag = new GridBagger(this);
        bag.insets(8, 0);
        bag.newRow();

        leftSide = Box.createHorizontalBox();
        bag.add(leftSide, bag.prop("center"));
        
        bag.add((JComponent) Box.createHorizontalStrut(10));
        
        messageArea = new JLabel(" ");
        GUI.keepMinHeight(messageArea);
        JPanel msgBox = new JPanel();
        GridBagger bag2 = new GridBagger(msgBox);
        bag2.add(messageArea, bag.prop("fill"));
        bag.add(msgBox, bag.prop("xfill"));
        msgBox.setBorder(new ThinBorder(getBackground(),
                                        /*raised*/ false, 0, 0));
        
        rightSide = Box.createHorizontalBox();
        bag.add(rightSide, bag.prop("center"));
        GUI.keepMinHeight(this);
    }
    
    /**
     * Adds a component in the left or the right side area.
     * @param tool
     * @param lefts true if on left.
     */
    public void addArea(JComponent tool, boolean lefts)
    {
        if(lefts)
             leftSide.add(tool);
        else rightSide.add(tool);
    }
    
    /**
     * Displays a permanent message.
     */
    public void displayMessage(String message)
    {
        messageArea.setText(message);
        GUI.paintImmediately(messageArea);
    }
    
    /**
     * Displays a message which is erased automatically after a certain time.
     * <p>The duration can be defined by setMessageDuration.
     */
    public void transientMessage(String message)
    {
        messageArea.setText(message);
        GUI.paintImmediately(messageArea);
        if(clearTask != null)
            clearTask.cancel();
        clearTask = new TimerTask() {
            public void run()
            {
                messageArea.setText("");
            }
        };
        timer.schedule(clearTask, tmDuration);
    }

    /**
     * Defines the duration for transient messages.
     * @param tmDuration duration in milliseconds.
     */
    public void setMessageDuration(long tmDuration)
    {
        this.tmDuration = tmDuration;
    }
}
