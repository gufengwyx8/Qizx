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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Action based on reflection...
 */
public abstract class FormController implements ChangeListener
{
    private static Class[] EVENT_ARG = { ActionEvent.class };
    private static Class[] BOOLEAN_ARG = { boolean.class };
    private static Class[] OBJECT_ARG = { Object.class };
    private static Class[] STRING_ARG = { String.class };
    private static Class[] INT_ARG = { int.class };
    
    protected Method action;
    protected Object target;
    
    protected HashMap<String, JComponent> controls;
    protected HashSet<Object> modifiedFields = new HashSet<Object>();
    
    public FormController()
    {
        controls = new HashMap<String, JComponent>();
    }
    
    public void modelChanged()
    {
        for (Iterator<String> iter = controls.keySet().iterator(); iter.hasNext();) {
            String name = iter.next();
            JComponent c = controls.get(name);
            // try to call the form's get method with this name:
            try {
                String methodName = "get" + name;
                Method meth = getClass().getMethod(methodName, (Class[]) null);
                meth.setAccessible(true);
                Object value = meth.invoke(this, (Object[]) null);

                if(c instanceof JCheckBox) {
                    JCheckBox box = (JCheckBox) c;
                    box.setSelected(Boolean.TRUE.equals(value));
                }
                else if (c instanceof JSpinner) {
                    JSpinner box = (JSpinner) c;
                    box.setValue(value);
                }
                else if (c instanceof JTextField) {
                    JTextField box = (JTextField) c;
                    box.setText((value == null)? "" : value.toString());
                }
            }
            catch(NoSuchMethodException ex) {
                
            }
            catch (InvocationTargetException e1) {
                showHandlerError(e1.getCause());
            }
            catch (Exception e1) {
                showHandlerError(e1);
            }
        }
    }
    
    
    public void addControl(String name, JComponent control)
    {
        control.setName(name);
        controls.put(name, control);
    }

    public JCheckBox addCheckbox(String name)
    {
        JCheckBox c = new JCheckBox(new BasicAction(null, "checkboxCB", this));
        addControl(name, c);
        return c;
    }

    public JTextField addTextField(String name, int columns)
    {
        JTextField c = new JTextField(columns);
        addControl(name, c);
        c.addActionListener(new BasicAction("", "textFieldCB", this));
        c.addFocusListener(textFocusListener);
        c.addKeyListener(keyListener);
        return c;
    }
    
    public FileSelector addFileField(String name, boolean directory, boolean open)
    {
        FileSelector f = new FileSelector(30, !directory, !open);
        f.addActionListener(new BasicAction("", "fileCB", this));
        return f;
    }
    
    private KeyListener keyListener = new KeyListener() {

        public void keyReleased(KeyEvent e)
        {
             modifiedFields.add(e.getSource());
        }

        public void keyPressed(KeyEvent e) { }
        public void keyTyped(KeyEvent e) { }
    };

    private FocusListener textFocusListener = new FocusListener() {
        public void focusGained(FocusEvent e) { }

        public void focusLost(FocusEvent e)
        {
             if(modifiedFields.contains(e.getSource())) {
                 textFieldCB(new ActionEvent(e.getSource(), 0, ""), null);
                 modifiedFields.remove(e.getSource());
             }
        }
    };
    
    public JComponent addIntTextField(String name, int min, int max)
    {
        JTextField c = new JTextField(5);
        addControl(name, c);
        return c;
    }

    public JComponent addIntSpinner(String name, int min, int max, int step)
    {
        JSpinner c = new JSpinner(new SpinnerNumberModel(0, min, max, step));
        addControl(name, c);
        c.addChangeListener(this);
        return c;
    }

    public JComponent addDoubleTextField(String name, double min, double max)
    {
        JTextField c = new JTextField(8);
        addControl(name, c);
        return c;
    }

    public void enableControl(String name, boolean enabled)
    {
         JComponent c = controls.get(name);
         if(c != null)
             c.setEnabled(enabled);
    }

    public void textFieldCB(ActionEvent e, BasicAction a)
    {
        JTextField c = (JTextField) e.getSource();
        callSetMethod(c.getName(), STRING_ARG, c.getText());
    }

    public void checkboxCB(ActionEvent e, BasicAction a)
    {
        JCheckBox c = (JCheckBox) e.getSource();
        callSetMethod(c.getName(), BOOLEAN_ARG, Boolean.valueOf(c.isSelected()));
    }

    public void stateChanged(ChangeEvent e)
    {
        JSpinner c = (JSpinner) e.getSource();
        if(callSetMethod(c.getName(), OBJECT_ARG, c.getValue()))
            return;
        callSetMethod(c.getName(), INT_ARG, c.getValue());       
    }

    private boolean callSetMethod(String name, Class[] prototype, Object value)
    {
        try {
            String methodName = "set" + name;
            Method meth = getClass().getMethod(methodName, prototype);
            meth.setAccessible(true);
            meth.invoke(this, new Object[] { value });
            return true;
        }
        catch(NoSuchMethodException ex) {
            
        }
        catch (InvocationTargetException e1) {
            GUI.error(e1.getCause().getMessage());
        }
        catch (Exception e1) {
            showHandlerError(e1);
        }
        return false;
    }
    
    private void showHandlerError(Throwable caught)
    {
        // signal error:
        String msg = "<html><b>" + caught.toString() + "</b>";
        StackTraceElement[] st = caught.getStackTrace();
        for (int s = 0; s < st.length && s < 15; s++) {
            msg += "<br>" + st[s];
        }
        GUI.message("Internal Error", msg);
        caught.printStackTrace();
    }

    public void compCB(ActionEvent e, BasicAction a)
    {
        Throwable caught;
        try {
            if(action != null)
                action.invoke(target, new Object[] { e });
            return;
        }
        catch (InvocationTargetException e1) {
            caught = e1.getCause();
        }
        catch (Exception e1) {
            caught = e1;
        }
        showHandlerError(caught);
    }
}
