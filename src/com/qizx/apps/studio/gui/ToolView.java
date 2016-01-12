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
import java.awt.Dimension;

import javax.swing.*;

/**
 * General scrolling view, with title and tool area.
 */
public class ToolView extends JPanel
{
    private JPanel header;
    private JComponent title;
    private JToolBar tools;
    protected JScrollPane scrollPane;

    private JComponent view;
    private GridBagger headerGrid;
    
    
    public ToolView() {
        this("View", null);
    }


    public ToolView(String titleText, JComponent view)
    {
        GridBagger grid = new GridBagger(this);
        grid.newRow();
        header = new JPanel();
        grid.add(header, grid.prop("xfill"));

        headerGrid = new GridBagger(header);
        headerGrid.newRow();
        title = new JLabel(titleText == null? null : (" " + titleText));
        title.setMinimumSize(new Dimension(10, 15));
        // tools on the right: xfill
        headerGrid.add(title, headerGrid.prop("xfill"));
        
        grid.newRow();
        scrollPane = new JScrollPane();
        if(view != null)
            scrollPane.setViewportView(view);
        grid.add(scrollPane, grid.prop("fill"));
    }


    public JComponent getTitle()
    {
        return title;
    }

    public void setTitle(JComponent title)
    {
        header.remove(this.title);
        this.title = title;
        header.add(title, new GridBagger(header).prop("xfill"), 0);
    }

    public void changeTitle(String titleText)
    {
        ((JLabel) title).setText(titleText);
    }

    public JComponent getTools()
    {
        return tools;
    }

    public JComponent getView()
    {
        return view;
    }

    public void setView(JComponent newView)
    {
        if(view != null)
            scrollPane.remove(view);
        scrollPane.setViewportView(newView);
        this.view = newView;
    }

    public void addToolSpace(int width, int position)
    {
        haveToolBar();
        tools.add(Box.createHorizontalStrut(width), position);
    }
    
    public void addTool(JComponent tool, int position)
    {
        haveToolBar();
        tools.add(tool, position);
    }

    private void haveToolBar()
    {
        if(tools == null) {
            tools = new JToolBar(); // new Box(BoxLayout.LINE_AXIS);
            tools.setFloatable(false);
            headerGrid.add(tools);
        }
    }

    public void removeTool(JComponent tool)
    {
        if(tools != null)
            tools.remove(tool);
    }
}
