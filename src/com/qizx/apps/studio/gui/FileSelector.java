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

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.help.event.EventListenerList;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A group that allows to select a file or directory, either
 * by typing its path in a text field or by browsing.
 * 
 * @author xfra date: 16 avr. 2005
 */
public class FileSelector extends JPanel
{
    private boolean plainFile;
    private JTextField pathField;
    private JButton browse;
    private JFileChooser fc;
    
    public FileSelector(int fieldSize, boolean plainFile, boolean save)
    {
        this.plainFile = plainFile;
        GridBagger grid = new GridBagger(this);
        
        pathField = new JTextField(fieldSize);
        grid.add(pathField, grid.prop("xfill"));      
        
        String title = GUI.loc("Browse...");
        browse = new JButton(new BasicAction(title, "cmdBrowse", this));
        grid.add(browse, grid.leftMargin(DialogBase.HGAP));
        GUI.betterLookButton(browse);
    }
    
    public void setEnabled(boolean e)
    {
        pathField.setEnabled(e);
        browse.setEnabled(e);
    }

    public String getPath()
    {
        String path = pathField.getText().trim();
        return path.length() > 0 ? path : null;
    }

    public void setPath(String filePath)
    {
        if(filePath == null)
            return;
        getFileChooser();
        fc.setSelectedFile(new File(fc.getSelectedFile(), filePath));
        pathField.setText(filePath);
    }

    public void setSuggestedName(String fileName)
    {
        getFileChooser();
        fc.setSelectedFile(new File(fc.getCurrentDirectory(), fileName));
        pathField.setText(fc.getSelectedFile().getPath());
    }

    public void setSuggestedFile(String path)
    {
        getFileChooser();
        fc.setSelectedFile(new File(path));
        pathField.setText(path);
    }

    public void cmdBrowse(ActionEvent e, BasicAction a)
    {
        getFileChooser();

        fc.setSelectedFile(new File(pathField.getText()));
        if (fc.showOpenDialog(getParent()) == JFileChooser.APPROVE_OPTION)
            pathField.setText(fc.getSelectedFile().getPath());
        
        
    }

    public JFileChooser getFileChooser()
    {
        if (fc == null) {
            fc = new JFileChooser(new File("."));
            fc.setFileSelectionMode(plainFile ? JFileChooser.FILES_ONLY
                                              : JFileChooser.DIRECTORIES_ONLY);
        }
        return fc;
    }

    public JTextField getPathField()
    {
        return pathField;
    }

    private EventListenerList listOfActionListeners = new EventListenerList();

    public void addActionListener(ActionListener listener)
    {
        listOfActionListeners.add(ActionListener.class, listener);
    }
}
