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
import com.qizx.apps.studio.gui.AppFrame;
import com.qizx.apps.studio.gui.BasicAction;
import com.qizx.apps.studio.gui.DialogBase;
import com.qizx.apps.studio.gui.GridBagger;
import com.qizx.apps.studio.gui.HistoryModel;
import com.qizx.apps.studio.gui.Localization;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 */
public class ServerConnectDialog extends DialogBase
    implements ListSelectionListener, ActionListener
{    
    private static Localization local =
        new Localization(QizxStudio.class, "ServerConnectDialog");

    private AppFrame app;
    private HistoryModel recentServers;
    private JTextField urlField;
    private JList recentList;

    public ServerConnectDialog(AppFrame app, HistoryModel recentServers)
    {
        super(app, "Connect to Server");
        this.app = app;
        this.recentServers = recentServers;
        buildContents();
        Help.setDialogHelp(this, "open_server_dialog");
    }

    /**
     */
    public void showUp()
    {
        recentList.clearSelection();
        super.showUp();
    }
    
    public String getSelected()
    {
        return urlField.getText().trim();
    }

    private void buildContents()
    {
        setHint(local.text("hint"), true);

        GridBagger grid = new GridBagger(form, 0, VGAP);

        grid.newRow();
        urlField = new JTextField("", 30);
        urlField.addActionListener(this);
        JPanel wrapper = new JPanel(new GridLayout());
        wrapper.add(urlField);
        wrapper.setBorder(new TitledBorder("Qizx Server: "));
        grid.add(wrapper, grid.prop("xfill").spans(2, 1));

        grid.newRow();
        recentList = new JList(recentServers);
        recentList.setVisibleRowCount(5);
        JScrollPane scroll = new JScrollPane(recentList);
        scroll.setBorder(new TitledBorder("Recent Servers: "));
        grid.add(scroll, grid.prop("fill").spans(2, 1).weighted(1, 2));
        recentList.addListSelectionListener(this);
        recentList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() >= 2)
                    cmdOK(null, null);
            } 
        });
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
        recentServers.addItem(recent);
    }

    public void valueChanged(ListSelectionEvent e)
    {
        String selected = (String) recentList.getSelectedValue();
        if(selected != null)
            urlField.setText(selected);
    }

    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == urlField) {
            cmdOK(e, null);
        }
    }
}
