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

import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.MutableComboBoxModel;

/**
 *  History combo-box model.
 *  Items must implement the equals method; 
 *  their toString method is used for display, unless there is a renderer.
 */
public class HistoryModel extends AbstractListModel 
    implements MutableComboBoxModel
{
    private Object selected;
    private ArrayList<Object> history;
    private int maxSize = 20;
    private String prefKey;
    
    public HistoryModel(int maxSize, String prefKey)
    {
        this.maxSize = maxSize;
        this.prefKey = prefKey;
        history = new ArrayList<Object>();
    }
    
    public boolean contains(Object item)
    {
        return history.contains(item);
    }

    public void rawAddItem(Object item)
    {
        history.add(item);
    }

    public void addItem(Object item)
    {
        int pos = history.indexOf(item);
        if(pos >= 0)
            history.remove(pos);
        
        // add at beginning:
        history.add(0, item);
        
        // limit the size:
        while(history.size() > maxSize) {
            history.remove(history.size() - 1);
        }
        changed();
    }
    
    public Object getElementAt(int index)
    {
        Object item = history.get(index);
        return item;
    }
    
    public int getSize()
    {
        
        return history.size();
    }
    
    public Object getSelectedItem()
    {
        return selected;
    }
    
    public void setSelectedItem(Object anItem)
    {
        selected = anItem;
    }
    
    private void changed() {
        fireContentsChanged(this, 0, getSize() - 1);
    }

    public void removeElementAt(int index)
    {   
    }

    public void addElement(Object obj)
    {
        addItem(obj);
    }

    public void removeElement(Object obj)
    {
    }

    public void insertElementAt(Object obj, int index)
    {
    }
    
    public void loadFromPrefs(PreferenceKeeper prefs)
    {
        for(int i = 0; ; i++) {
            String key = prefKey + i;
            if(prefs.get(key) == null)
                break;
            rawAddItem(prefs.get(key));
        }
    }
    
    public void saveToPrefs(PreferenceKeeper prefs)
    {
        for (int h = 0, asize = getSize(); h < asize; h++) {
            String key = prefKey + h;
            try {
                prefs.put(key, getElementAt(h).toString());
            }
            catch (IllegalArgumentException e) {
                System.err.println("Cannot save setting " + key +": " + e);
            }
        }
    }    
}

