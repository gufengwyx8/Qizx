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

import com.qizx.api.util.XMLSerializer;
import com.qizx.apps.studio.Help;
import com.qizx.apps.studio.gui.*;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.border.TitledBorder;

/**
 */
public class ExportDialog extends DialogBase
    implements ActionListener
{
    private static final String[] ENCODINGS = new String[] {
        "ISO8859-1", "ISO8859-2", "ISO8859-3", "ISO8859-4", "ISO8859-5", 
        "ISO8859-7", "ISO8859-9", "ISO8859-13", "ISO8859-15",
        "US-ASCII", "UTF-8", "UTF-16", "Windows-1250", "Windows-1251",
        "Windows-1252", "Windows-1253", "Windows-1257"
    };

    private static final String[] METHODS = new String[] {
        "XML", "HTML", "XHTML", "Text"
    };
    
    private AppFrame app;
    private FileSelector fileSelector;
    private JLabel messageBox;

    private ComboSelector encodingField;
    private JComboBox methodField;
    private JSpinner indentValueOption;
    private JCheckBox omitDeclOption;
    private JCheckBox indentOption;

    private XMLSerializer serializer;


    public ExportDialog(AppFrame app)
    {
        super(app);
        this.app = app;
        setTitle("Export");
        buildContents();
        Help.setDialogHelp(this, "export_document_dialog");
    }

    /**
     * @param suggestedPath
     * @param message
     * @param dialogTitle 
     * @param wellFormed
     * @return null if cancelled, otherwise a XMLSerializer prepared with
     * options specified.
     */
    public XMLSerializer showUp(String suggestedPath, boolean wellFormed, 
                                String dialogTitle)
    {
        fileSelector.setPath(suggestedPath);
        File suggestedFile = new File(suggestedPath);
        fileSelector.setSuggestedName(suggestedFile.getName());
        String message = wellFormed ? null :
            "Caution: saved output will not be well-formed XML";
        messageBox.setText(message);
        setTitle(dialogTitle);
        showUp();
        return isCancelled()? null : serializer;
    }
    
    public JFileChooser getFileChooser()
    {
        return fileSelector.getFileChooser();
    }

    public String getFilePath()
    {
        return fileSelector.getPath();
    }

    private void buildContents()
    {
        GridBagger grid = new GridBagger(form, 0, VGAP);
        
        grid.newRow();
        messageBox = new JLabel();
        messageBox.setForeground(new Color(255, 90, 0));
        grid.add(messageBox, grid.prop("xfill").spans(2, 1));
        
        grid.newRow();
        fileSelector = new FileSelector(30, true, true);
        fileSelector.setBorder(new TitledBorder("Output file: "));
        grid.add(fileSelector, grid.prop("xfill").spans(2, 1));
        JFileChooser fileChooser = fileSelector.getFileChooser();
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        
        grid.newRow();
        addLabel(grid, "Encoding");
        encodingField = new ComboSelector("", ENCODINGS, false);
        addOption(grid, encodingField.getComboBox());
        
        
        grid.newRow();
        addLabel(grid, "Method");
        methodField = new JComboBox(METHODS);
        methodField.addActionListener(this);
        addOption(grid, methodField);
        
        grid.newRow();
        addLabel(grid, "Omit XML Declaration");
        omitDeclOption = new JCheckBox();
        addOption(grid, omitDeclOption);

        grid.newRow();
        addLabel(grid, "Indent");
        indentOption = new JCheckBox(new BasicAction(null, "cmdIndent", this));
        addOption(grid, indentOption);
        indentOption.setSelected(false);

        grid.newRow();
        addLabel(grid, "Indentation");
        indentValueOption = new JSpinner();
        GUI.setPreferredWidth(indentValueOption, 40);
        indentValueOption.setValue(Integer.valueOf(2));
        indentValueOption.setEnabled(false);        
        addOption(grid, indentValueOption);
    }
   
    private void addLabel(GridBagger grid, String label)
    {
         grid.add(new JLabel(label + ":  "), grid.prop("right"));
    }

    private void addOption(GridBagger grid, JComponent compo)
    {
         grid.add(compo, grid.prop("left"));
    }

    public void cmdIndent(ActionEvent e, BasicAction a)
    {
        indentValueOption.setEnabled(indentOption.isSelected());
    }
    
    public void cmdOK(ActionEvent e, BasicAction a)
    {
        String path = fileSelector.getPath();
        if(path == null)
            return;
        fileSelector.setSuggestedFile(path);
        
        // build a serializer using selected options:
        serializer = new XMLSerializer();
        try {
            String method = (String) methodField.getSelectedItem();
            serializer.setOption(XMLSerializer.METHOD, method);
            String encoding = (String) encodingField.getValue();
            serializer.setOption(XMLSerializer.ENCODING, 
                                 encoding);
            serializer.setOption(XMLSerializer.OMIT_XML_DECLARATION,
                                 Boolean.toString(omitDeclOption.isSelected()));
            //path = replaceExtension(path, method);
            FileOutputStream fout = new FileOutputStream(path);
            serializer.setOutput(fout, encoding);
            if(indentOption.isSelected()) {
                Integer value = (Integer) indentValueOption.getValue();
                serializer.setIndent(value.intValue());
            }
            super.cmdOK(e, a);
        }
        catch (Exception ex) {
            app.showError(ex);
        }
    }

    private String replaceExtension(String path, String method)
    {
        int dot = path.lastIndexOf('.');
        
        if(dot > 0) 
            path = path.substring(0, dot);
        if("XML".equals(method))
            return path + ".xml";
        else if("Text".equals(method))
            return path + ".txt";
        else
            return path + ".html";
    }

    public void actionPerformed(ActionEvent e)
    {
        String method = (String) methodField.getSelectedItem();
        String npath = replaceExtension(fileSelector.getPath(), method);
        fileSelector.setPath(npath);
    }
}
