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

import com.qizx.apps.studio.Help;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.text.JTextComponent;

/**
 * Input of a string value. 
 */
public class ValueChooser extends DialogBase
    implements KeyListener
{
    //private Pattern pattern;
    private JLabel hintArea;
    private JLabel messageArea;
    private JTextField inputField;
    private JTextArea inputArea;
    
    private JTextComponent activeEditor;
    private Validator validator;
    
    public ValueChooser(Frame parent, String title, String hint,
                        int columns, int rows)
    {
        super(parent, title);
        init(hint, columns, rows);
    }

    public ValueChooser(Dialog parent, String title, String hint,
                        int columns, int rows)
    {
        super(parent, title);
        init(hint, columns, rows);
    }

    public ValueChooser(Frame parent, String title, String hint)
    {
        super(parent, title);
        init(hint, -1, -1);
    }

    public ValueChooser(Dialog parent, String title, String hint)
    {
        super(parent, title);
        init(hint, -1, -1);
    }

    public void setHelpId(String id)
    {
         Help.setHelpId(getHelpButton(), id);
    }

    public void setVisible(boolean visible)
    {
        getOkButton().setEnabled(false);
        super.setVisible(visible); 
    }

    private void init(String hint, int columns, int rows)
    {
        setHint(hint, (hint != null));
        
        GridBagger grid = new GridBagger(form, 0, VGAP);
        
        grid.newRow();
        if(columns > 0 && rows > 0) {
            inputArea = new JTextArea(rows, columns) {
                // the obvious way to avoid line wrapping...
                public boolean getScrollableTracksViewportWidth()
                {
                    return true;
                }
            };
            JScrollPane scroll = new JScrollPane(inputArea);
            scroll.revalidate();
            scroll.setMinimumSize(scroll.getSize());
            
            grid.add(scroll, grid.prop("fill"));
            inputArea.addKeyListener(this);
            activeEditor = inputArea;
        }
        else {
            inputField = new JTextField(40);
            grid.add(inputField, grid.prop("xfill"));
            inputField.addKeyListener(this);
            inputField.addActionListener(new BasicAction("", "cmdEnter", this));
            activeEditor = inputField;
        }
        activeEditor.setBorder(new EtchedBorder());

        grid.newRow();
        messageArea = new JLabel("\u00a0");
        messageArea.setForeground(Color.red.darker());
        GUI.keepMinHeight(messageArea);
        grid.add(messageArea, grid.prop("xfill"));
        pack();
    }

    public String getInput()
    {
        String value = activeEditor.getText();
        return isCancelled() ? null : value;
    }

    public void setInput(String input)
    {
        activeEditor.setText(input);
        activeEditor.requestFocus();
        validateOKButton();
        messageArea.setText("");
    }

    /**
     * Shows an input Dialog and returns the input value.
     * @param initialValue
     * @return the value entered, or null if cancelled.
     */
    public String enterString(String initialValue)
    {
        setInput(initialValue);
        showUp();
        return isCancelled() ? null : getInput();
    }

    /**
     * Shows an input Dialog and returns the input value.
     * @param title of dialog
     * @param hint amessage for the user
     * @param initialValue
     * @param parent parent component, can be null
     * @return the value entered, or null if cancelled.
     */
    public static String enterString(JComponent parent, String title, String hint,
                                     String initialValue, Validator validator)
    {
        Window top = getToplevelContainer(parent);
        ValueChooser vc = (top instanceof Frame)?
                new ValueChooser((Frame) top, title, hint)
             :  new ValueChooser((Dialog) top, title, hint);

        vc.setTitle(title);
        vc.setHint(hint, hint != null);
        vc.setValidator(validator);
        vc.setInput(initialValue);
        vc.showUp();
        if (vc.isCancelled())
            return null;
        return vc.getInput();
    }
    
    /**
     * Shows an input Dialog with a text area and returns the input value.
     * @param title of dialog
     * @param hint amessage for the user
     * @param initialValue
     * @param parent parent component, can be null
     * @return the value entered, or null if cancelled.
     */
    public static String enterText(JComponent parent, 
                                   String title, int columns, int rows,
                                   String hint, String initialValue,
                                   Validator validator)
    {
        Window top = getToplevelContainer(parent);
        ValueChooser vc = (top instanceof Frame)?
                new ValueChooser((Frame) top, title, hint, columns, rows)
             :  new ValueChooser((Dialog) top, title, hint, columns, rows);
        vc.setTitle(title);
        vc.setHint(hint, hint != null);
        vc.setValidator(validator);
        vc.setInput(initialValue);
        vc.showUp();
        if(vc.isCancelled())
            return null;
        return vc.getInput();
    }
    
    public interface Validator {
        boolean accepts(String value);    
    }

    public Validator getValidator() {
        return validator;
    }

    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    public void setRegexpValidator(final String regexp)
    {
        this.validator = new Validator() {
            public boolean accepts(String value) {
                return Pattern.matches(regexp, value);
            }
        };
    }

    public void cmdEnter(ActionEvent e, BasicAction a)
    {
        if(getOkButton().isEnabled())
            cmdOK(e, a);
    }
    
    public void keyReleased(KeyEvent e)
    {
        boolean ok = validateOKButton();
        messageArea.setText(ok? "" : "Invalid value");
        pack();
    }

    private boolean validateOKButton()
    {
        boolean ok = validator == null || validator.accepts(getInput());
        getOkButton().setEnabled(ok);
        return ok;
    }

    public void keyPressed(KeyEvent e) { }

    public void keyTyped(KeyEvent e) { }
}
