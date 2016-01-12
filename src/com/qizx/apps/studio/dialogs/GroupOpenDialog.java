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
import com.qizx.apps.studio.QizxStudio;
import com.qizx.apps.studio.gui.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 */
public class GroupOpenDialog extends DialogBase
    implements ListSelectionListener, ActionListener
{    
    private static Localization local =
        new Localization(QizxStudio.class, "GroupOpenDialog");

    private AppFrame app;
    private FileSelector fileSelector;

    private HistoryModel recentGroups;
    private JList recentList;

    public GroupOpenDialog(AppFrame app, HistoryModel recentGroups)
    {
        super(app, "Open local Library group");
        this.app = app;
        this.recentGroups = recentGroups;
        buildContents();
        Help.setDialogHelp(this, "open_group_dialog");
    }

    /**
     */
    public void showUp()
    {
        recentList.clearSelection();
        super.showUp();
    }
    
    private void buildContents()
    {
        setHint(local.text("hint"), true);
        
        GridBagger grid = new GridBagger(form, 0, VGAP);
        
        grid.newRow();
        fileSelector = new FileSelector(30, true, true);
        fileSelector.setBorder(new TitledBorder("Library group on disk: "));
        grid.add(fileSelector, grid.prop("xfill").spans(2, 1));
        JFileChooser fileChooser = fileSelector.getFileChooser();
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.addActionListener(this);
        fileSelector.getPathField().addActionListener(this);
                
        grid.newRow();
        recentList = new JList(recentGroups);
        recentList.setVisibleRowCount(5);
        JScrollPane scroll = new JScrollPane(recentList);
        scroll.setBorder(new TitledBorder("Recent Library Groups: "));
        grid.add(scroll, grid.prop("fill").spans(2, 1).weighted(1, 2));
        recentList.addListSelectionListener(this);
        recentList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2)
                    cmdOK(null, null);
            }
        });
    }

    public String getSelected()
    {
        return fileSelector.getPath();
    }

    public void cmdOK(ActionEvent e, BasicAction a)
    {
        String sel = getSelected();
        if (sel == null || sel.length() == 0)
            return;
        try {
            super.cmdOK(e, a);
        }
        catch (Exception ex) {
            app.showError(ex);
        }
    }

    public void addRecent(String recent)
    {
        recentGroups.addItem(recent);
    }

    public void valueChanged(ListSelectionEvent e)
    {
        String selected = (String) recentList.getSelectedValue();
        if(selected != null)
            fileSelector.setSuggestedFile(selected);
    }

    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == fileSelector.getPathField()) { // Enter in field
            cmdOK(e, null);
        }
        else {
            //selected = fileSelector.getFileChooser().getSelectedFile().getPath();
            recentList.clearSelection();
        }
    }
}
