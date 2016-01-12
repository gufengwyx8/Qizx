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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A labelled combo-box, which remembers typed-in values if editable.
 */
public class ComboSelector extends JPanel implements ActionListener
{
    private JLabel label;
    private JComboBox comboBox;

    public ComboSelector(String title, Object[] values, boolean editable)
    {
//        GridBagger grid = new GridBagger(this);
//        grid.newRow();
        new BoxLayout(this, BoxLayout.LINE_AXIS);

        label = new JLabel(title);
        add(label);

        comboBox = new JComboBox(values);
        comboBox.setEditable(editable);
        comboBox.addActionListener(this);
        add(comboBox);
    }

    public void setEnabled(boolean e)
    {
        comboBox.setEnabled(e);
    }

    /**
     * Returns the selected or edited value. If the text field is editable
     */
    public Object getValue()
    {
        return comboBox.getSelectedItem();
    }

    public void actionPerformed(ActionEvent e)
    {
        String value = (String) comboBox.getSelectedItem();
        addItem(value);
    }

    public void addItem(String value)
    {
        DefaultComboBoxModel model = (DefaultComboBoxModel) comboBox.getModel();
        for (int j = model.getSize(); --j >= 0;) {
            Object item = model.getElementAt(j);
            if(value.equals(item))
                return;
        }
        model.addElement(value);
    }

    public JComboBox getComboBox()
    {
        return comboBox;
    }
}
