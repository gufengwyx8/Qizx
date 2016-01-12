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
import com.qizx.apps.studio.QizxStudio.PropertyAction;
import com.qizx.apps.studio.gui.AppFrame;
import com.qizx.apps.studio.gui.BasicAction;
import com.qizx.apps.studio.gui.DialogBase;
import com.qizx.apps.studio.gui.GridBagger;
import com.qizx.apps.util.Property;

import java.awt.event.ActionEvent;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 */
public class MetaEditDialog extends DialogBase
{
    private static final String[] TYPES = new String[] {
        Property.STRING, 
        Property.INTEGER, Property.DOUBLE, 
        Property.BOOLEAN,
        Property.DATE_TIME, 
        Property.NODE,
        Property.EXPRESSION
    };

    private AppFrame app;
    private PropertyAction target;

    private JLabel messageBox;
    private JTextField nameField;
    private JTextArea valueField;
    private JComboBox typeField;

    public MetaEditDialog(AppFrame app)
    {
        super(app);
        this.app = app;
        setTitle("Edit Metadata Property");

        GridBagger grid = new GridBagger(form, 0, VGAP);

        grid.newRow();
        messageBox = new JLabel();
        grid.add(messageBox, grid.prop("xfill").spans(2, 1));

        grid.newRow();
        addLabel(grid, "Property name");
        nameField = new JTextField(20);
        grid.add(nameField, grid.prop("xfill"));

        grid.newRow();
        addLabel(grid, "Property value");
        valueField = new JTextArea(2, 20);
        grid.add(new JScrollPane(valueField), grid.prop("fill"));

        grid.newRow();
        addLabel(grid, "Property type");
        typeField = new JComboBox(TYPES);
        grid.add(typeField, grid.prop("left"));

        Help.setDialogHelp(this, "metadata_edit_dialog");
    }

    public void showUp(PropertyAction target)
        throws Exception
    {
        this.target = target;
        Property prop = target.property;
        String propertyName = (prop == null) ? null : prop.name;
        messageBox.setText("");
        if (propertyName == null) { // add
            nameField.setText("");
            nameField.setEditable(true);
            valueField.setText("");
        }
        else { // edit
            nameField.setText(propertyName);
            nameField.setEditable(false);

            typeField.setSelectedItem(prop.type);
            if(prop.nodeValue != null) {
                XMLSerializer s = new XMLSerializer();
                String sf = s.serializeToString(prop.nodeValue);
                valueField.setText(sf);
            }
            else valueField.setText(prop.value);
        }
        showUp();
    }
    
    public void cmdOK(ActionEvent e, BasicAction a)
    {
        try {
            String type = (String) typeField.getSelectedItem();
            if (type == null) {
                messageBox.setText("a type should be selected");
                return;
            }

            String value = valueField.getText();
            String name = nameField.getText();
            Property prop = new Property(name, value);
            prop.type = type; // will force conversion in local mode
            target.savePropertyValue(prop);

            super.cmdOK(e, a);
        }
        catch (Exception ex) {
            app.showError(ex);
        }
    }

    private void addLabel(GridBagger grid, String label)
    {
        grid.add(new JLabel(label + ": "), grid.prop("right"));
    }
}
