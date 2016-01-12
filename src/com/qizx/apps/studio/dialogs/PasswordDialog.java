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

import com.qizx.apps.studio.gui.BasicAction;
import com.qizx.apps.studio.gui.DialogBase;
import com.qizx.apps.studio.gui.GridBagger;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;

import javax.swing.JLabel;
import javax.swing.JPasswordField;

/**
 * Input of a new password. Optionally asks for former password.
 */
public class PasswordDialog extends DialogBase
    implements KeyListener
{
    //private Pattern pattern;
    private JLabel hintArea;
    private JPasswordField formerPass;
    private JPasswordField newPass1;
    private JPasswordField newPass2;
    private boolean wantFormer;
    
    public PasswordDialog(Frame parent, String title, String hint,
                          boolean wantFormer)
    {
        super(parent, title);
        this.wantFormer = wantFormer;
        init(hint);
    }

    public PasswordDialog(Dialog parent, String title, String hint,
                          boolean wantFormer)
    {
        super(parent, title);
        this.wantFormer = wantFormer;
        init(hint);
    }

    private void init(String hint)
    {
        GridBagger grid = new GridBagger(form);
        grid.setInsets(10, 5);
        
        grid.newRow();
        hintArea = new JLabel(hint);
        //hintArea.setOpaque(false);
        grid.add(hintArea, grid.prop("xfill").spans(2, 1));
        
        if(wantFormer) {
            grid.newRow();
            grid.add(new JLabel("Former password: "), grid.prop("right"));
            formerPass = new JPasswordField(10);
            grid.add(formerPass, grid.prop("xfill"));
        }
        
        grid.newRow();
        grid.add(new JLabel("New password: "), grid.prop("right"));
        newPass1 = new JPasswordField(10);
        grid.add(newPass1, grid.prop("xfill"));
        newPass1.addActionListener(new BasicAction("", "cmdPass1Entered", this));
        newPass1.addKeyListener(this);

        grid.newRow();
        grid.add(new JLabel("Confirm password: "), grid.prop("right"));
        newPass2 = new JPasswordField(10);
        grid.add(newPass2, grid.prop("xfill"));
        newPass2.addActionListener(new BasicAction("", "cmdPass2Entered", this));
        newPass2.addKeyListener(this);

        getOkButton().setEnabled(false);
        pack();
    }
    
    public char[] getOldPass()
    {
        if(formerPass == null)
            return null;
        char[] pass = formerPass.getPassword();
        return (pass == null || pass.length == 0)? null : (pass);
    }

    public char[] getNewPass()
    {
        if(isCancelled())
            return null;
        char[] pass = newPass1.getPassword();
        return (pass == null || pass.length == 0)? null : pass;
    }

    public void cmdEnter(ActionEvent e, BasicAction a)
    {
        if (getOkButton().isEnabled())
            cmdOK(e, a);
    }
    
    public void cmdPass1Entered(ActionEvent e, BasicAction a)
    {
        if (newPass2 != null)
            newPass2.grabFocus();
        else
            cmdEnter(e, a);
    }
    
    public void cmdPass2Entered(ActionEvent e, BasicAction a)
    {
        getOkButton().grabFocus();
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e)
    {
        getOkButton().setEnabled(equalPasswords());
    }

    private boolean equalPasswords()
    {
        // check equality of both passwords
        char[] p1 = newPass1.getPassword();
        char[] p2 = newPass2.getPassword();
        if(p1 == null)
            return (p2 == null);
        if(p2 == null)
            return false;
        return Arrays.equals(p1, p2);
    }
}
