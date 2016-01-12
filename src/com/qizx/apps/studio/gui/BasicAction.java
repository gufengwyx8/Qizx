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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.AbstractAction;
import javax.swing.Icon;

/**
 * Action based on reflection...
 */
public class BasicAction extends AbstractAction
{   
    protected Method action;
    protected Object target;
    
    public BasicAction(String label, String action, Object target)
    {
        this(label, null, action, target);
    }
    
    /**
     * Creates an action that calls the specified method on the object.
     * @param label
     * @param icon
     * @param method name of a public method in the target object. Must have
     * one argument of class ActionEvent.
     * @param target target of this action.
     */
    public BasicAction(String label, Icon icon, String method, Object target) 
    {
        super(label, icon);
        this.target = target;
        try {
            this.action = target.getClass().getMethod(method,
                                  new Class[] { ActionEvent.class, getClass() });
        }
        catch (Exception e) {   // not runtime
            System.err.println("*** "+e);
        }
    }
    
    public void actionPerformed(ActionEvent e)
    {
        Throwable caught;
        try {
            if(action != null)
                action.invoke(target, new Object[] { e, this });
            return;
        }
        catch (InvocationTargetException e1) {
            caught = e1.getCause();
        }
        catch (Exception e1) {
            caught = e1;
        }
        // signal error: TODO log
        String msg = "<html><b>" + caught.toString() + "</b>";
        StackTraceElement[] st = caught.getStackTrace();
        for (int s = 0; s < st.length; s++) {
            msg += "<br>" + st[s];
        }
        GUI.message("Internal Error", msg);
        caught.printStackTrace();
    }
}
