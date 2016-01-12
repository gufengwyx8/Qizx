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
import com.qizx.xdm.DocumentParser;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class CatalogDialog extends DialogBase
{
    public static final String CATALOG_PROPERTY = "xml.catalog.files";

    private static Localization local =
        new Localization(QizxStudio.class, "CatalogDialog");
    
    private JButton addFileButton;
    private JButton addUrlButton;
    private JButton delButton;

    private DefaultListModel listModel;
    private JList listView;

    private JFileChooser fileChooser;
    

    public CatalogDialog(AppFrame parent)
    {
        super(parent, local.text("XML_Catalogs"));
        buildContents();
        
        Help.setDialogHelp(this, "catalog_dialog");
    }


    private void buildContents()
    {
        setHint(local.text("hint"), true);
        
        GridBagger grid = new GridBagger(form, 0, VGAP);

        grid.newRow();
        listModel = new DefaultListModel();
        listView = new JList(listModel);
        JScrollPane scroll = new JScrollPane(listView);
        scroll.setPreferredSize(new Dimension(300, 100));
        grid.add(scroll, grid.prop("fill").rightMargin(HGAP));
        
        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        grid.add(buttons, grid.prop("center"));
        
        addFileButton = new JButton(new BasicAction(local.text("Add_File..."),
                                                "cmdAddFile", this));
        GUI.betterLookButton(addFileButton);
        buttons.add(addFileButton);       
        
        addUrlButton = new JButton(new BasicAction(local.text("Add_URL..."),
                                                "cmdAddUrl", this));
        GUI.betterLookButton(addUrlButton);
        buttons.add(addUrlButton);       
        
        delButton = new JButton(new BasicAction(local.text("Remove"),
                                                "cmdRemove", this));
        GUI.betterLookButton(delButton);
        buttons.add(delButton);
    }
    
    public void showUp()
    {
        listModel.clear();

        String prop = getCatalogProperty();
        if(prop != null) {
            String[] files = prop.split(";");

            for (int s = 0; s < files.length; s++) {
                String cata = files[s];
                listModel.add(listModel.getSize(), cata);
            }
        }
        super.showUp();
    }


    public void cmdAddFile(ActionEvent e, BasicAction a)
    {
        if(fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select a XML catalog file");
        }
        if(fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        
        listModel.addElement(fileChooser.getSelectedFile().getPath());
    }

    public void cmdAddUrl(ActionEvent e, BasicAction a)
    {
        ValueChooser vc = new ValueChooser(this, "Enter URL",
                                           "Enter the URL of an XML catalog:");
        vc.showUp();
        if(vc.getInput() == null)
            return;
        listModel.addElement(vc.getInput());
    }
    
    public void cmdRemove(ActionEvent e, BasicAction a)
    {
        int[] indices = listView.getSelectedIndices();
        for(int i = indices.length; --i >= 0; )
            listModel.remove(indices[i]);
    }


    public void cmdOK(ActionEvent e, BasicAction a)
    {
        // rebuild the system property from list items:
        StringBuffer buf = new StringBuffer(50 * listModel.getSize());
        for(int i = 0, size = listModel.getSize(); i < size; i++) {
            if(i > 0)
                buf.append(";");
            buf.append(listModel.getElementAt(i));
        }
        setCatalogProperty(buf.toString());
        
        super.cmdOK(e, a);
    }


    public static String getCatalogProperty()
    {
        return System.getProperty(CATALOG_PROPERTY);
    }


    public static void setCatalogProperty(String value)
    {
        System.setProperty(CATALOG_PROPERTY, value);
        DocumentParser.resetXMLCatalogs();
    }
}
