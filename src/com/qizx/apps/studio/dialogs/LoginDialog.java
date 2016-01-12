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

import com.qizx.apps.studio.gui.AppFrame;
import com.qizx.apps.studio.gui.BasicAction;
import com.qizx.apps.studio.gui.DialogBase;
import com.qizx.apps.studio.gui.GridBagger;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginDialog extends DialogBase
    implements KeyListener
{
    private JLabel messageField;
    private JTextField nameField;
    private JPasswordField passwordField;
    
    public LoginDialog(AppFrame parent)
    {
        super(parent, "Login");
        buildContents();
    }
    
    protected void buildContents()
    {
        setHint("User authentication:", false);
        
        GridBagger grid = new GridBagger(form, 0, VGAP);

        grid.newRow();
        messageField = new JLabel();
        grid.add(messageField, grid.spans(2, 1).prop("center"));
        
        grid.newRow();
        grid.add(new JLabel("Login: "), grid.prop("right"));
        nameField = new JTextField();
        BasicAction cmdOK = new BasicAction("", "cmdOK", this);
        nameField.addActionListener(cmdOK);
        nameField.addKeyListener(this);
        nameField.setColumns(16);
        grid.add(nameField, grid.prop("xfill").leftMargin(HGAP * 2));
        
        grid.newRow();
        grid.add(new JLabel("Password: "), grid.prop("right"));
        passwordField = new JPasswordField();
        passwordField.addActionListener(cmdOK);
        passwordField.addKeyListener(this);
        grid.add(passwordField, grid.prop("xfill").leftMargin(HGAP * 2));
    }

    public String getLogin() {
        return isCancelled()? null : nameField.getText().trim();
    }
    
    public void setLogin(String login) {
        nameField.setText(login);
    }
    
    public char[] getPassword() {
        return isCancelled()? null : passwordField.getPassword();
    }

    public void showUp(String title, String message)
    {
        messageField.setText(message);
        getOkButton().setEnabled(false);
        pack();
        showUp(title);
    }

    public void enableLogin(boolean enabled)
    {
         nameField.setEnabled(enabled);
    }

    @Override
    public void cmdOK(ActionEvent e, BasicAction a)
    {
        if(validated())
            super.cmdOK(e, a);
    }

    private boolean validated()
    {
        String login = nameField.getText();
        if(login == null || login.length() == 0)
            return false;
        char[] pwd = passwordField.getPassword();
        return pwd != null && pwd.length > 0;
    }

    public void keyReleased(KeyEvent e)
    {
        getOkButton().setEnabled(validated());
    }

    public void keyPressed(KeyEvent e) { }

    public void keyTyped(KeyEvent e) { }
    
}
