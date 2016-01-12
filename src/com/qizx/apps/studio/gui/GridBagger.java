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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * A helper to build a panel using the GridBagLayout.
 * <p>Works by row or columns: manages automatic placement on the grid.
 * For example if nextRow is used, the add methods would increment
 * the gridx constraint (taking into account the gridwidth).
 * <p>Example:
 * <pre>GridBuilder grid = new GridBuilder(panel);
 * grid.newRow();
 * grid.add(new JLabel(...), grid.prop("east"));
 * grid.add(new JTextField(...), grid.prop("xfill").insets(5, 0));
 * grid.newRow(); ...
 * </pre>
 */
public class GridBagger extends GridBagConstraints
{
    private JComponent target;
    private boolean rowing; // working by row...
    
    /**
     * Initiates a grid bag layout on this component.
     */
    public GridBagger(JComponent target) {
        this.target = target;
        target.setLayout(new GridBagLayout());
    }
    /**
     * Initiates a grid bag layout on this component.
     */
    public GridBagger(JComponent target, int hmargin, int vmargin) {
        this.target = target;
        target.setLayout(new GridBagLayout());
        insets = new Insets(vmargin, hmargin, vmargin, hmargin);
    }
    
    public void newRow() {
        gridx = 0;
        ++ gridy;
        rowing = true;
    }
    
    public void newColumn() {
        gridy = 0;
        ++ gridx;
        rowing = false;
    }
    
    /**
     * Skips one or more columns (if in Row mode) or conversely.
     */
    public void skip(int skip) {
        if(rowing)
            gridx += skip;
        else
            gridy += skip;
    }


    /**
     * @param comp
     * @param c constraints used. Attention gridx and gridy are modified.
     */
    public void add(JComponent comp, GridBagConstraints c)
    {
        c.gridx = gridx;
        c.gridy = gridy;
        
        target.add(comp, c);
        if(rowing)
            gridx += c.gridwidth;
        else
            gridy += c.gridheight;
    }

    public void add(JComponent comp) {
        add(comp, this);
    }
    
    public JLabel addLabel(String title, int align) {
        JLabel lab = new JLabel(title);
        if(align != 0)
            add(lab, prop(align < 0 ? "left" : "right"));
        else
            add(lab);            
        return lab;
    }

    private GridBagger privateCopy() {
        if(target == null)
            return this;
        GridBagger clone = (GridBagger) clone();
        clone.target = null;
        return clone;
    }
    
    public GridBagger prop(String prop) {
        GridBagger c = privateCopy();
        if("east".equalsIgnoreCase(prop) || "right".equalsIgnoreCase(prop))
            c.anchor = GridBagConstraints.EAST;
        else if("center".equalsIgnoreCase(prop))
            c.anchor = GridBagConstraints.CENTER;
        else if("west".equalsIgnoreCase(prop) || "left".equalsIgnoreCase(prop))
            c.anchor = GridBagConstraints.WEST;
        else if("north".equalsIgnoreCase(prop) || "top".equalsIgnoreCase(prop))
            c.anchor = GridBagConstraints.NORTH;
        else if("south".equalsIgnoreCase(prop) || "bottom".equalsIgnoreCase(prop))
            c.anchor = GridBagConstraints.SOUTH;
        else if("topleft".equalsIgnoreCase(prop))
            c.anchor = GridBagConstraints.NORTHWEST;
        else if("topright".equalsIgnoreCase(prop))
            c.anchor = GridBagConstraints.NORTHEAST;
        else if("bottomleft".equalsIgnoreCase(prop))
            c.anchor = GridBagConstraints.SOUTHWEST;
        else if("bottomright".equalsIgnoreCase(prop))
            c.anchor = GridBagConstraints.SOUTHEAST;
        //TODO southeast etc
        // fills
        else if("xfill".equalsIgnoreCase(prop)) {
            c.fill = GridBagConstraints.HORIZONTAL;
            if(c.weightx == 0) c.weightx = 1;
        }
        else if("yfill".equalsIgnoreCase(prop)) {
            c.fill = GridBagConstraints.VERTICAL;
            if(c.weighty == 0) c.weighty = 1;
        }
        else if("fill".equalsIgnoreCase(prop)) {
            c.fill = GridBagConstraints.BOTH;
            if(c.weightx == 0) c.weightx = 1;
            if(c.weighty == 0) c.weighty = 1;
        }
        else
            throw new IllegalArgumentException("bad property "+prop);
        return c;
    }

    public GridBagger weighted(double wx, double wy) {
        GridBagger c = privateCopy();
        c.weightx = wx; c.weighty = wy;
        return c;
    }
    
    public GridBagger spans(int w, int h) {
        GridBagger c = privateCopy();
        c.gridheight = h; c.gridwidth = w;
        return c;
    }
    
    public GridBagger fullwidth()
    {
        GridBagger c = privateCopy();
        c.gridwidth = REMAINDER;
        return c;
    }

    public void setInsets(int leftright, int topbottom) {
        insets = new Insets(topbottom, leftright, topbottom, leftright);
    }        
    
    public GridBagger insets(int leftright, int topbottom) {
        GridBagger c = privateCopy();
        c.setInsets(leftright, topbottom);
        return c;
    }        
    
    public GridBagger leftMargin(int margin)
    {
        GridBagger c = privateCopy();
        c.insets = new Insets(0, margin, 0, 0);
        return c;
    }        
    
    public GridBagConstraints rightMargin(int margin)
    {
        GridBagger c = privateCopy();
        c.insets = new Insets(0, 0, 0, margin);
        return c;
    }
    
    public String toString()
    {
        return "Grid(" + "x=" + gridx + ", y=" + gridy
            + ", w=" + gridwidth + ", h=" + gridheight
            + ", anchor=" + anchor + ", fill=" + fill
            + ", wx=" + weightx + ", wy=" + weighty + ")";
    }
}
