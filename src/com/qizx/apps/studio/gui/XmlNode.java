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

import com.qizx.api.DataModelException;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.apps.studio.gui.TreePort.NodeAction;
import com.qizx.util.NamespaceContext;
import com.qizx.xdm.BasicNode;

import javax.swing.JPopupMenu;

/**
 * XML node: display colored XML markup.
 */
public class XmlNode extends TreePort.ColoredNode
{
    public static final int DOCU          = 1;
    public static final int TAG           = 2;
    public static final int ATTR          = 3;
    public static final int TEXT_STYLE    = 0;
    public static final int COMMENT_STYLE = 4;
    public static final int PI_STYLE      = 5;
    
    private static final int MAX_LINE = 200;
    private static final int MAX_CHILDREN = 10000;
    
    protected static NamespaceContext namespaces = new NamespaceContext();

    protected Node     node;
    private boolean    leaf;
    private boolean    endTag;
    // String representation of JTree node
    private String     stringRep;
    protected TreePort tree;

    public XmlNode(Node node, TreePort tree)
    {
        this(node, tree, false);
    }

    public XmlNode(Node node, TreePort tree, boolean endTag)
    {
        this.node = node;
        this.tree = tree;
        this.endTag = endTag;
    }

    public String toString()
    {
        try {
            prepare();
            if ("<html>".equals(stringRep)) // funny bug
                stringRep = "<html>&lt;html>";
        }
        catch (Exception e) {
            showException(e);
        }
        return stringRep;
    }

    public boolean isLeaf()
    {
        try {
            prepare();
        }
        catch (Exception e) {
            showException(e);
            return true;
        }
        return leaf || endTag;
    }

    private void showException(Exception e)
    {
        stringRep = ">>> " + e.getMessage() + " <<<";
        sections = null;
    }

    protected void procreate()
    {
        if (!isLeaf()) {
            try {
                Node kid = node.getFirstChild();
                // skip a text node first child (already displayed)
                if(node.getNodeName() != null && 
                        kid.getNodeNature() == Node.TEXT) {
                    kid = kid.getNextSibling();
                }
                int guard = 0;
                for (; kid != null; kid = kid.getNextSibling()) {
                    if (!isWhiteSpace(kid))
                        add(new XmlNode(kid, tree));
                    if(++guard > MAX_CHILDREN) {
                        add(truncationNode());
                        break;  // TODO special 'More' node
                    }
                }
                if (node.getNodeNature() == Node.ELEMENT)
                    add(new XmlNode(node, tree, true)); // end tag
            }
            catch (DataModelException e) {
                add(new TreePort.ErrorNode(e));
            }
        }
    }

    protected JPopupMenu getPopupMenu()
    {
        JPopupMenu pop = new JPopupMenu();
        pop.add(new NodeAction(tree.isExpanded(this)? "Collapse all" : "Expand all",
                this, "cmdExpandAll", this));
        return pop;
    }
    
//    public void cmdExpandAll(XmlNode node)
//    {
//        node.tree.expandOrCollapse(node);
//    }

    // Computes the string representation of the node and
    // optionally colored sections
    private void prepare() throws DataModelException
    {
        if (stringRep != null)
            return;
        StringBuilder buf = new StringBuilder();
        leaf = true;
        switch (node.getNodeNature()) {
        case Node.DOCUMENT:
            leaf = false;
            buf.append("<?xml version='1.0'?>");
            endSection(buf, DOCU);
            break;
        case Node.ELEMENT:
            buf.append(endTag ? "</" : "<");
            putName(node, buf);
            if (endTag) {
                closeTag(buf);
                break;
            }
            endSection(buf, TAG);
            Node[] attrs = node.getAttributes();
            if (attrs != null) {
                for (int a = 0; a < attrs.length; a++) {
                    buf.append(' ');
                    if(attrs[a] == null) {
                        System.err.println("OOPS attr "+a);
                        continue;
                    }
                    putName(attrs[a], buf);
                    buf.append("=\"");
                    buf.append(attrs[a].getStringValue());
                    buf.append('"');
                    endSection(buf, ATTR);
                }
            }
            // namespaces declared on the element:
            namespaces.clear(); // synchronize?
            int nns = ((BasicNode) node).addNamespacesTo(namespaces);
            if(nns > 0)
                for(int ins = 0; ins < nns; ins++) {
                    String nsu = namespaces.getLocalNamespaceURI(ins);
                    String prefix = namespaces.getLocalPrefix(ins);
                    buf.append(" xmlns");
                    if(prefix != null && prefix.length() > 0) {
                        buf.append(':');
                        buf.append(prefix);
                    }
                    buf.append("=\"");
                    buf.append(nsu);
                    buf.append('"');
                    endSection(buf, ATTR);
                }
            
            // inline first text child
            boolean empty = true,
            closed = false;
            Node kid = node.getFirstChild();
            for (; kid != null; kid = kid.getNextSibling()) {
                empty = false;
                if (kid.getNodeNature() != Node.TEXT) {
                    leaf = false;
                    break;
                }
                closeTag(buf);
                closed = true;
                String sval = kid.getStringValue();
                if(sval.length() > MAX_LINE)
                    buf.append(sval.substring(0, MAX_LINE) + "...");
                else
                    buf.append(sval);
                endSection(buf, TEXT_STYLE);
            }
            // end tag?
            if (empty)
                buf.append('/');
            else if (leaf) {    // single text child
                buf.append("</");
                putName(node, buf);
                closed = false;
            }
            if (!closed)
                closeTag(buf);
            endSection(buf, TAG);
            break;
        case Node.TEXT:
            buf.append(node.getStringValue());
            endSection(buf, TEXT_STYLE);
            break;
        case Node.COMMENT:
            buf.append("<!--");
            buf.append(node.getStringValue());
            buf.append("-->");
            endSection(buf, COMMENT_STYLE);
            break;
        case Node.PROCESSING_INSTRUCTION:
            buf.append("<?");
            buf.append(node.getNodeName());
            buf.append(' ');
            buf.append(node.getStringValue());
            buf.append("\n?>");
            endSection(buf, PI_STYLE);
            break;
        case Node.ATTRIBUTE: // stray
            putName(node, buf);
            buf.append("=\"");
            buf.append(node.getStringValue());
            buf.append('"');
            endSection(buf, ATTR);
            break;
        default:
            buf.append("?");
        }
        stringRep = buf.toString();
    }

    private void closeTag(StringBuilder buf)
    {
        int blen = buf.length();
        if (blen > 0 && buf.charAt(blen - 1) == '>')
            return;
        buf.append('>');
        endSection(buf, TAG);
    }

    private XmlNode truncationNode()
    {
        XmlNode node = new XmlNode(null, tree, false);
        node.stringRep = "... more ...";
        node.leaf = true;
        return node;
    }

    // simple management of namespaces:
    private void putName(Node node, StringBuilder out)
        throws DataModelException
    {
        QName name = node.getNodeName();
        String ns = name.getNamespaceURI(); // interned
        if (NamespaceContext.XML.equals(ns)) { // needs not be declared
            out.append("xml:");
            out.append(name.getLocalPart());
        }
        else {
            String prefix = node.getNamespacePrefix(ns);
            if (prefix != null) { //  && prefix.length() > 0
                out.append(prefix);
                if(prefix.length() > 0)
                    out.append(':');
                out.append(name.getLocalPart());
            }
            else {
                out.append(name); // {uri}localpart
            }
        }        
    }

    private boolean isWhiteSpace(Node kid) throws DataModelException
    {
        if (kid.getNodeNature() != Node.TEXT)
            return false;
        String s = kid.getStringValue();
        for (int i = s.length(); --i >= 0;) {
            if (!Character.isWhitespace(s.charAt(i)))
                return false;
        }
        return true;
    }

    protected String getToolTip()
    {
        try {
            return node.getNodeKind() + " node";
        }
        catch (Exception e) {
            return "Error: " + e;
        }
    }
}
