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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;


/**
 * Generic tree view with title and tools.
 */
public class TreePort extends ToolView
{
    private JTree tree;
    private DefaultTreeModel model;
    private Renderer renderer = new Renderer();

    public TreePort(String title, boolean showRoot, TNode root)
    {
        super(title, null);
        model = new DefaultTreeModel(root, !true);
        tree = new JTree(model);        
        tree.setRootVisible(showRoot);
        tree.setCellRenderer(renderer);
        tree.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        // to have tooltips:
        ToolTipManager.sharedInstance().registerComponent(tree);

        setView(tree);

        // mouse events:        
        tree.setToggleClickCount(Integer.MAX_VALUE); // handle it ourselves
        tree.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e)
                {
                    showPopup(e);

                    if (e.getClickCount() >= 2) {
                        TreePath path =
                            tree.getPathForLocation(e.getX(), e.getY());
                        if (path == null) {
                            return;
                        }

                        TNode node = (TNode) path.getLastPathComponent();
                        node.doubleClick(e, TreePort.this);
                    }
                }

                public void mouseReleased(MouseEvent e)
                {
                    showPopup(e);
                }

                private void showPopup(MouseEvent e)
                {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());

                    if (path == null) {
                        return;
                    }
                    if(!e.isShiftDown())
                        tree.setSelectionPath(path);

                    if (e.isPopupTrigger()) {
                        TNode node = (TNode) path.getLastPathComponent();
                        JPopupMenu menu = node.getPopupMenu();

                        if (menu != null) {
                            menu.show(tree, e.getX(), e.getY());
                        }
                    }
                }
            });
        // selection:
        tree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e)
                {
                    TNode node = (TNode) e.getPath().getLastPathComponent();

                    if (node != null) {
                        node.selected(e);
                    }
                }
            });
    }

    public void changeFont(int style, boolean fixedWidth)
    {
        Font ft = tree.getFont();

        if (fixedWidth) {
            ft = new Font("Monospaced", style, ft.getSize());
        } else {
            ft = ft.deriveFont(style);
        }

        tree.setFont(ft);
    }

    public void changeRoot(TNode root)
    {
        model.setRoot(root);
        tree.revalidate();
        tree.repaint();
    }

    public TNode getRoot()
    {
        return (TNode) model.getRoot();
    }

    public void removeNode(TNode node)
    {
        model.removeNodeFromParent(node);
    }

    public void refreshNode(TNode node)
    {
        model.nodeChanged(node);
    }

    public void rebuildNode(TNode node)
    {
        if(node != null) {
            node.removeAllChildren();
            model.nodeStructureChanged(node);
            model.nodeChanged(node);
        }
    }

    public void removeAllChildren(TNode node)
    {
        //        // trick to help redisplay
        //        if(model.getChildCount(node) != 0)
        //            model.removeNodeFromParent( (MutableTreeNode) node.getChildAt(0)); 
        node.removeAllChildren();
        model.nodeStructureChanged(node);

        //model.reload(node);
    }

    public void didChange()
    {
        tree.treeDidChange();
    }

    public void expandAll()
    {
        TreeNode root = (TreeNode) model.getRoot();
        expandOrCollapseAll(new TreePath(root), true);
    }

    /**
     * Try to expand so to roughly fill the view.
     */
    public void expandAllVisible(int lineCount)
    {
        // no simple way to know if a row is visible
        for (int r = 0, asize = lineCount; r < asize; r++) {
            tree.expandRow(r);
        }
    }

    public void expandOrCollapse(TNode node)
    {
        node.getParent();
        TreePath path = buildTreePath(node);
        expandOrCollapseAll(path, tree.isCollapsed(path));
        tree.treeDidChange();
    }

    public TreePath buildTreePath(TreeNode node)
    {
        if(model.getRoot() == node)
            return new TreePath(node);
        TreePath up = buildTreePath(node.getParent());
        return up.pathByAddingChild(node);
    }
    
    public boolean isExpanded(TNode node)
    {
        TreePath path = buildTreePath(node);
        return tree.isExpanded(path);
    }

    private void expandOrCollapseAll(TreePath parent, boolean expand)
    {
 
        // Traverse children
        TNode node = (TNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration<TreeNode> e = node.children(); e.hasMoreElements();) {
                TreeNode n = e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandOrCollapseAll(path, expand);
            }
        }

        // Expansion or collapse must be done bottom-up
        if (expand) {
            node.getChildCount();
            tree.expandPath(parent);
        }
        else {
            tree.collapsePath(parent);
        }
   }

    public TreePath convertPath(TreePath path)
    {
        TNode root = getRoot();
        if(root == null)
            return null;
        int plen = path.getPathCount();
        ArrayList<TNode> lpath = new ArrayList<TNode>(plen);
        lpath.add(root);
        TNode parent = root;
        for (int i = 1; i < plen; i++) {
            TNode kid = (TNode) path.getPathComponent(i);
            TNode newKid = parent.getChild(kid); // implem dependent
            if(newKid == null)
                break;
            lpath.add(newKid);
            parent = newKid;
        }
        return new TreePath(lpath.toArray());
    }
    
    /**
     * Rebuilds and redisplays this TreePort, keeping the expanded nodes when
     * possible. NOT VERY EFFICIENT!
     */
    public void smartRefresh(TNode rootNode)
    {
        // redisplay: first remember expanded nodes
        Enumeration<TreePath>  enu = 
            tree.getExpandedDescendants(tree.getPathForRow(0));
        ArrayList<TreePath> expanded = new ArrayList<TreePath>();

        
        while (enu != null && enu.hasMoreElements()) {
            TreePath treepath = enu.nextElement();
             
            expanded.add(treepath);
        }

        if(rootNode == null)
            rootNode = (TNode) model.getRoot();
        if(rootNode != null)
            rootNode.removeAllChildren();
        model.nodeStructureChanged(rootNode);
            
        /// reexpand nodes:
        for (int i = 0; i < expanded.size(); ++i) {
            TreePath path = expanded.get(i);
            tree.expandPath(convertPath(path));
        }
    }
    
    public void collapseSelectedPath()
    {
        tree.collapsePath(tree.getSelectionPath());
    }

    public JTree getTree()
    {
        return tree;
    }

    public Renderer getRenderer()
    {
        return renderer;
    }

    // Node superclass: 
    // redefines a few methods for bulk creation and display
    public abstract static class TNode extends DefaultMutableTreeNode
        implements ActionListener
    {
        // expand children if not yet done
        protected abstract void procreate();

        protected Icon getIcon(boolean selected, boolean expanded)
        {
            return null; // use default icon
        }

        protected String getToolTip()
        {
            return null;
        }

        protected void selected(TreeSelectionEvent e)
        {
        }

        protected void doubleClick(MouseEvent e, TreePort tree)
        {
            // default is to expand / collapse all
            tree.expandOrCollapse(this);
        }

        protected JPopupMenu getPopupMenu()
        {
            return null;
        }

        public void actionPerformed(ActionEvent e)
        {
            System.err.println("menu action " + e);
        }

        public TreeNode getChildAt(int childIndex)
        {
            if (children == null) {
                children = new Vector();
                try {
                    procreate();
                }
                catch (Throwable e) {
                    GUI.error(e);
                }
            }

            return super.getChildAt(childIndex);
        }

        public int getChildCount()
        {
            if (children == null) {
                children = new Vector();

                try {
                    procreate();
                }
                catch (Throwable e) {
                    GUI.error(e);
                }
            }
            return super.getChildCount();
        }

        /**
         * Gets the child matching a node model. This is used to expand
         * an old TreePath after rebuilding a tree.
         * @param child can be an old model for the same node
         * @return the current model
         */
        public TNode getChild(TNode child)
        {
            int cnt = getChildCount();
            for(; --cnt >= 0; ) {
                TNode kid = (TNode) super.getChildAt(cnt);
                if(child.equals(kid))
                    return kid;
            }
            return null;
        }

        public TNode getAncestor(Class classe)
        {
            TreeNode a = getParent();
            for(; a != null; a = a.getParent())
                if(classe.isInstance(a))
                    return (TNode) a;
            return null;
        }
        
        public boolean isLeaf()
        {
            return false;

            // procreate();
            // return children == null || children.size() == 0;
        }

        public void removeAllChildren()
        {
            super.removeAllChildren();
            children = null;
        }

        // intentionally redefined like this to force reimplementation
        public boolean equals(Object obj)
        {
            return this == obj;
        }

        // For special display: may compute the size of renderer,
        // define colored sections in text
        public void render(Renderer renderer, boolean selected, boolean expanded)
        {
        }

        public void paint(Renderer renderer, Graphics g)
        {
            // 
        }
    }

    public abstract static class TLeaf extends TNode
    {
        public boolean isLeaf()
        {
            return true;
        }

        public void procreate()
        {
        }
    }

    public abstract static class ColoredNode extends TNode
    {
        // If not null, defines sections of text with color
        // pairs (style, length)
        protected StringBuffer sections;

        protected void endSection(StringBuilder buf, int style)
        {
            if (sections == null) {
                sections = new StringBuffer();
            }
            else if (sections.charAt(sections.length() - 2) == style) {
                sections.setLength(sections.length() - 2);
            }

            sections.append((char) style);
            sections.append((char) buf.length());
        }

        public void paint(Renderer renderer, Graphics g)
        {
            try {
                renderer.paintSections(toString(), sections, g);
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static class ErrorNode extends ColoredNode
    {
        private Throwable err;

        public ErrorNode(Throwable t)
        {
            err = t;
        }

        protected void procreate()
        {
        }

        public boolean isLeaf()
        {
            return true;
        }

        public String toString()
        {
            return "*** " + err.getMessage() + " ***";
        }
    }

    // Action associated with tree nodes: invokes a method of XQuestGUI
    // passing the source tree Node.
    public static class NodeAction extends AbstractAction
    {
        Object app;
        String method;
        TreePort.TNode[] source;

        public NodeAction(String title, Object app, String method,
                          TreePort.TNode source)
        {
            super(title);
            this.app = app;
            this.method = method;
            this.source = new TreePort.TNode[] { source };
        }

        public void actionPerformed(ActionEvent e)
        {
            try {
                Method m = app.getClass().getMethod(method, new Class[] {
                    source[0].getClass()
                });
                m.invoke(app, (java.lang.Object[]) source);
            }
            catch (InvocationTargetException ex) {
                System.err.println("error in " + method + " " + ex.getCause());
                GUI.error(ex.getCause());
            }
            catch (Throwable e1) {
                GUI.error(e1);
            }
        }
    }

    public static class Renderer extends DefaultTreeCellRenderer
    {
        private TNode currentNode;
        private Color[] colors = new Color[10];

        Renderer()
        {
            colors[0] = Color.black;
            colors[1] = Color.red;
            colors[2] = Color.green;
            colors[3] = Color.blue;
            colors[4] = Color.gray;
            colors[5] = Color.orange;
        }

        public void setColor(int style, Color color)
        {
            if ((style >= 0) && (style < colors.length)) {
                colors[style] = color;
            }
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean sel, boolean expanded, boolean leaf, int row,
            boolean hasFocus)
        {
            setToolTipText(null);
            setIcon(null);
            currentNode = (TNode) value;
            // default rendering: if the node implements render(), 
            // it can redefine toString() for efficiency
            super.getTreeCellRendererComponent(tree, currentNode, sel,
                                               expanded, leaf, row, hasFocus);
            setIcon(currentNode.getIcon(selected, expanded));

            String toolTip = currentNode.getToolTip();

            if (toolTip != null) {
                setToolTipText(toolTip);
            }

            // for special display (void by default)
            currentNode.render(this, sel, expanded);

            
            return this;
        }

        public void paint(Graphics g)
        {
            if (!(currentNode instanceof ColoredNode)) {
                super.paint(g);
            }
            else {
                // only background for selection
                Color bColor;

                // copied from superclass:
                if (selected) {
                    bColor = Color.lightGray;
                }
                else {
                    bColor = getBackgroundNonSelectionColor();

                    if (bColor == null) {
                        bColor = getBackground();
                    }
                }

                int imageOffset = -1;

                if (bColor != null) {
                    // Icon currentI = getIcon();
                    g.setColor(bColor);

                    if (getComponentOrientation().isLeftToRight()) {
                        g.fillRect(imageOffset, 0, getWidth() - 1
                                                   - imageOffset, getHeight());
                    }
                    else {
                        g.fillRect(0, 0, getWidth() - 1 - imageOffset,
                                   getHeight());
                    }
                }
            }

            currentNode.paint(this, g);
        }

        public void paintSections(String text, StringBuffer sections, Graphics g)
        {
            int start = 0;
            int x = 0;
            Graphics2D g2d = (Graphics2D) g;
            FontRenderContext frc = g2d.getFontRenderContext();
            Font font = getFont();

            // compute bounds from font metrics:
            LineMetrics lmet = font.getLineMetrics("m", frc);
            int ascent = 1 + (int) lmet.getAscent();

            if(sections != null) {
                for (int s = 0; s < sections.length(); s += 2) {
                    int style = sections.charAt(s);
                    g.setColor(colors[style]);

                    int end = sections.charAt(s + 1);
                    String frag = text.substring(start, end);
                    g.drawString(frag, x, ascent);

                    Rectangle2D lm1 = font.getStringBounds(frag, frc);
                    double width = lm1.getWidth() + 0.5;
                    x += (int) width;
                    
                    start = end;
                }
            }
            else {
                g.setColor(Color.red);
                g.drawString(text, 0, ascent);
            }
        }
    }
}
