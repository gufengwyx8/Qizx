/*
 *    Qizx/open 4.1
 *
 * This code is the open-source version of Qizx.
 * Copyright (C) 2004-2009 Axyana Software -- All rights reserved.
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
package com.qizx.xquery;

import com.qizx.api.*;
import com.qizx.util.NamespaceContext;
import com.qizx.util.basic.HTable;
import com.qizx.util.basic.XMLUtil;
import com.qizx.util.basic.HTable.Key;
import com.qizx.xdm.*;
import com.qizx.xdm.FONIDataModel.FONINode;
import com.qizx.xquery.dt.ArraySequence;
import com.qizx.xquery.op.DeleteExpr;
import com.qizx.xquery.op.Expression;
import com.qizx.xquery.op.InsertExpr;
import com.qizx.xquery.op.RenameExpr;
import com.qizx.xquery.op.ReplaceExpr;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Implementation of XQuery Update. A list of updates associated with an
 * evaluation context.
 */
public class Updates
{
    private DynamicContext dynamicContext;
    private int nsCopyMode;
    private UpdaterFactory updaterFactory;

    private EvalContext transformContext; // non null for 'transform' only
    private Updates enclosing;

    // Maps nodes to their update list
    private HTable nodeUpdates;
    private NodeUpdates probe;
    // map nodes stored by put():
    private HashMap<String, Node> putMap;
    private HashMap<Node, String> revPutMap;
    // Set of updated documents (not used by transform)
    private HashSet<FONIDataModel> docs;
    // List of copied roots for transform
    private Node[] roots;
    // temporary list for non-attribute nodes (clist)
    private ArrayList<Node> otherNodes = new ArrayList<Node>();
    private CoreDataModel coreBuilder = new CoreDataModel("");

    private NamespaceContext tmpNS = new NamespaceContext();
    private int trace = 0;
    private boolean pedantic;
    private boolean nsInherit;
    
    /**
     * Used for normal operations: delete, insert, replace, rename
     * Used for a 'transform' expression: a temporary PUL with roots. Any
     * updated node must be a descendant of one of the roots. Each root is
     * associated with a local variable of the context.
     */
    public Updates(DynamicContext context, Updates previous)
    {
        this.enclosing = previous;
        init(context);
    }

    public Updates getEnclosing()
    {
        return enclosing;
    }

    public void addRoot(Node root)
    {
        if (roots == null)
            roots = new Node[] {
                root
            };
        else {
            Node[] old = roots;
            roots = new Node[old.length + 1];
            System.arraycopy(old, 0, roots, 0, old.length);
            roots[old.length] = root;
        }
    }

    // Executes an 'insert nodes' operation
    public void insertNodes(InsertExpr insert, Focus focus, EvalContext context)
        throws EvaluationException, DataModelException
    {
        boolean sibling =
          (insert.mode == InsertExpr.BEFORE) || (insert.mode == InsertExpr.AFTER);
        Node target = evalSingleNode(insert.where, focus, context,
                                     sibling ? "XUDY0009" : null, "XUTY0005");
        // checks:
        int targetKind = target.getNodeNature();
        if(sibling) {
            if(targetKind == Node.ATTRIBUTE || targetKind == Node.DOCUMENT)
                context.error("XUTY0006", insert.where);
        }
        else {
            if(targetKind != Node.ELEMENT && targetKind != Node.DOCUMENT)
                context.error("XUTY0005", insert.where);
        }
        
        NodeUpdates up = getNodeUpdates(target, context, insert);

        // nodes to insert: separate attrs and other nodes;
        // Non-node items are converted to text node
        XQValue nodes = insert.what.eval(focus, context);
        otherNodes.clear();
        boolean acceptAttr = true;

        StringBuffer textBuilder = null;
        for (; nodes.next();) {
            if (!nodes.isNode()) {
                if (textBuilder == null)
                    textBuilder = new StringBuffer(nodes.getString());
                else {
                    textBuilder.append(' ');
                    textBuilder.append(nodes.getString());
                }
            }
            else {
                if (textBuilder != null) {
                    addTextNode(textBuilder);
                    textBuilder = null;
                }

                Node inserted = nodes.getNode();
                QName iname = inserted.getNodeName();
                if (inserted.getNodeNature() == Node.ATTRIBUTE)
                {
                    if(targetKind == Node.DOCUMENT)
                        context.error("XUTY0022", insert);
                    else if(sibling && target.getParent() != null && 
                            target.getParent().getNodeNature() != Node.ELEMENT)
                        context.error("XUDY0030", insert);
                    if (!acceptAttr) // attribute node after non-attr nodes
                        context.error("XUTY0004", insert);
                    
                    // that is not very consistent: shouldnt it be in apply()?
//                    if(target.getAttribute(iname) != null)
//                        context.error("XUDY0021", insert,
//                                      "Duplicate attribute '" + iname + "'");
                    
                    ArrayList<Node> insertedAttrs;
                    if (sibling) {
                        NodeUpdates pup = getNodeUpdates(target.getParent(),
                                                         context, insert);
                        
                        pup.insertedAttrs = addNode(inserted, pup.insertedAttrs);
                        insertedAttrs = pup.insertedAttrs;
                    }
                    else {
                        up.insertedAttrs = addNode(inserted, up.insertedAttrs);  
                        insertedAttrs = up.insertedAttrs;
                    }
                    
//                    // that is not very consistent: shouldnt it be in apply()?
//                    if(insertedAttrs != null) {
//                        // Attention: ignore the last (just inserted...)
//                        for(int a = insertedAttrs.size()  - 1; --a >= 0; ) {
//                            Node attr = insertedAttrs.get(a);
//                            if(attr.getNodeName() == iname)
//                                context.error("XUDY0021", insert,
//                                              "duplicate attribute '" + iname + "'");                               
//                        }
//                        if(target.getAttribute(iname) != null)
//                            context.error("XUDY0021", insert,
//                                          "duplicate attribute '" + iname + "'");    
//                    }
                    // QName prefix clashes (what a crap):
                    if(iname.getPrefix() != null) {
                        checkPrefixClash(iname, target, context, insert);
                    }
                }
                else {
                    acceptAttr = false;
                    otherNodes.add(inserted);
                }
            }
        }
        if (textBuilder != null)
            addTextNode(textBuilder);

        context.at(insert);

        switch (insert.mode) {
        case InsertExpr.BEFORE:
            up.insertedBefore = addNodes(up.insertedBefore);
            break;
        case InsertExpr.FIRST:
            up.insertedFirst = addNodes(up.insertedFirst);
            break;
        case InsertExpr.INTO:
            up.insertedInto = addNodes(up.insertedInto);
            break;
        case InsertExpr.LAST:
            up.insertedLast = addNodes(up.insertedLast);
            break;
        case InsertExpr.AFTER:
            up.insertedAfter = addNodes(up.insertedAfter);
            break;
        }
    }

    private void addTextNode(StringBuffer textBuilder)
    {
        otherNodes.add(coreBuilder.newTextNode(textBuilder.toString()));
    }

    private ArrayList<Node> addNode(Node node, ArrayList<Node> list)
        throws EvaluationException
    {
        if (list == null)
            list = new ArrayList<Node>();
        list.add(node);
        return list;
    }

    private ArrayList<Node> addNodes(ArrayList<Node> list)
        throws EvaluationException
    {
        if (list == null)
            list = new ArrayList<Node>();
        list.addAll(otherNodes);
        return list;
    }

    // Check that the name (with prefix) doesnt clash with in-scope NS of target
    private void checkPrefixClash(QName name, Node target,
                                  EvalContext context, Expression where)
        throws DataModelException, EvaluationException
    {
        tmpNS.clear();
        ((BasicNode) target).addInScopeNamespacesTo(tmpNS);
        String prefix = name.getPrefix();
        String uri = tmpNS.getNamespaceURI(prefix);
        if(uri != null && !uri.equals(name.getNamespaceURI()))
            context.error("XUDY0023", where);
    }

    public void deleteNodes(DeleteExpr delete, Focus focus, EvalContext context)
        throws EvaluationException
    {
        XQValue nodes = delete.where.eval(focus, context);
        context.at(delete);

        for (; nodes.next();) {
            if (!nodes.isNode())
                context.error("XUTY0007", delete);
            Node node = nodes.getNode();
            NodeUpdates up = getNodeUpdates(node, context, delete);
            up.deleted = true;
        }
    }

    public void renameNode(RenameExpr rename, Focus focus, EvalContext context)
        throws EvaluationException, DataModelException
    {
        Node node = evalSingleNode(rename.where, focus, context, null, "XUTY0008");
        int nature = node.getNodeNature();
        if(nature == Node.DOCUMENT || nature >= Node.COMMENT)
            context.error("XUDY0015", rename); // invalid node type
        
        QName name = computeName(rename.what, focus, context);
        context.at(rename);
        if(name.getPrefix() != null)
            checkPrefixClash(name, node, context, rename);
        
        NodeUpdates up = getNodeUpdates(node, context, rename);
        if (up.newName != null) {
            context.error("XUDY0015", rename); // conflict
        }
        up.newName = name;
    }

    public void replaceNodes(ReplaceExpr replace, Focus focus,
                             EvalContext context)
        throws EvaluationException, DataModelException
    {
        Node node = evalSingleNode(replace.where, focus, context, null, "XUTY0012");
        int targetNature = node.getNodeNature();

        XQValue newValue = replace.what.eval(focus, context);
        context.at(replace);

        NodeUpdates up = getNodeUpdates(node, context, replace);

        if (replace.mode == ReplaceExpr.VALUE) {
            if (up.replacedValue != null)
                context.error("XUDY0017", replace); // conflict
            up.replacedValue = buildText(newValue);
            
            // need to check some cases:
            if (targetNature == Node.DOCUMENT)
                context.error("XUTY0008", replace.where,
                              "replace value of document is not allowed"); // spec??
            // check value for some node types:
            if(targetNature == Node.COMMENT &&
                    !XMLUtil.checkComment(up.replacedValue))
                context.error("XQDY0072", replace); // 
            else if(targetNature == Node.PROCESSING_INSTRUCTION &&
                    up.replacedValue != null &&
                    up.replacedValue.indexOf("?>") >= 0)
                context.error("XQDY0026", replace); // 
        }
        else { // replace node by zero, one or several nodes
            if (up.replacement != null)
                context.error("XUDY0016", replace); // conflict
            if (node.getParent() == null)
                context.error("XUDY0009", replace); // need parent
            Item[] items = up.replacement = ArraySequence.expand(newValue);
            try {
                boolean wantAttr = (targetNature == Node.ATTRIBUTE);

                int nit = 0;
                for (int i = 0, itemCnt = items.length; i < itemCnt; i++) {
                    boolean gotAttr = false;
                    Node rn;
                    if (items[i].isNode()) {
                        rn = items[i].getNode();
                        gotAttr = (rn.getNodeNature() == Node.ATTRIBUTE);
                        QName rname = rn.getNodeName();
                        if(rname != null && rname.getPrefix() != null)
                            checkPrefixClash(rname, node, context, replace);
                    }
                    else {
                        StringBuffer txt =
                            new StringBuffer(items[i].getString());
                        for (; i + 1 < itemCnt && !items[i + 1].isNode(); i++) {
                            txt.append(' ');
                            txt.append(items[i + 1].getString());
                        }
                        rn = coreBuilder.newTextNode(txt.toString());
                    }

                    if (gotAttr != wantAttr)
                        context.error(wantAttr ? "XUTY0011" : "XUTY0010",
                                      replace.what);
                    items[nit++] = rn;
                }
                if (nit != items.length) {  // possible?
                    Item[] old = items;
                    items = new Item[nit];
                    System.arraycopy(old, 0, items, 0, nit);
                }
                up.replacement = items;
            }
            catch (DataModelException e) {
                context.error(e.getErrorCode(), replace.what, e.getMessage());
            }
        }
    }

    private Node evalSingleNode(Expression expr, Focus focus, EvalContext context,
                                String parentError, String nonNodeError)
        throws EvaluationException, DataModelException
    {
        XQValue v = expr.eval(focus, context);
        if (!v.next())
            context.error("XUDY0027", expr); // must not be empty
        if (!v.isNode())
            context.error(nonNodeError, expr);
        Node node = v.getNode();
        if (v.next())
            context.error(nonNodeError, expr); // single node
        if (parentError != null) {
            Node parent = node.getParent();
            if(parent == null)
                context.error(parentError, expr);
        }
        return node;
    }

    private QName computeName (Expression expr, Focus focus,
                               EvalContext context)
        throws EvaluationException
    {
        XQItem nameItem = expr.evalAsItem(focus, context);
        BasicNode node = null;
        int qtype = nameItem.getItemType().quickCode();
        if (nameItem.isNode()) {
            node = nameItem.basicNode();
            qtype = XQType.QT_UNTYPED;
        }
        context.at(expr);
        switch (qtype) {
        case XQType.QT_UNTYPED:
        case XQType.QT_STRING:
            try {
                String pname = nameItem.getString();
                String prefix = IQName.extractPrefix(pname);
                String ncname = IQName.extractLocalName(pname);
                if (prefix.length() == 0)
                    return XQName.get("", ncname, "");
                QName qname = null;
                String uri = null;
                if (node != null)
                    uri = node.getNamespaceUri(prefix);
                if (uri != null)
                    qname = XQName.get(uri, ncname, prefix);
                else
                    qname = context.getInScopeNS().expandName(pname);
                if (qname == null)
                    context.error("XQDY0074", expr,
                                  "no namespace found for prefix " + prefix);
                return qname;
            }
            catch (Exception e) {
                context.error("XQDY0074", expr,
                              "error converting string to QName: "
                                      + e.getMessage());
                return null;
            }
        case XQType.QT_QNAME:
            return nameItem.getQName();

        default:
            context.badTypeForArg(nameItem.getItemType(), expr, 0,
                                  "QName or string");
            return null;
        }
    }

    public Node applyTransformUpdates(int rootIndex)
        throws EvaluationException
    {
        Node root = roots[rootIndex];
        CorePushBuilder stream = new CorePushBuilder("");
        try {
            rebuild(root, stream);
        }
        catch (DataModelException e) {
            throw BasicNode.wrapDMException(e);
        }
        return stream.harvest();
    }

    /**
     * Implements fn:put() : add a node for the uri. 
     * Checks that the uri has not yet been used.
     */
    public void addPut(String uri, Node root,
                       Expression putCall, EvalContext context)
        throws DataModelException, EvaluationException
    {
        if(root.getNodeNature() != Node.DOCUMENT &&
           root.getNodeNature() != Node.ELEMENT)
            context.error("FOUP0001", putCall,
                          "first operand of fn:put is not a proper node");
        if ((uri = validatedURI(uri)) == null)
            context.error("FOUP0002", putCall);
        if(putMap != null && putMap.get(uri) != null)
            context.error("XUDY0031", putCall,
                          "put URI " + uri + " has already been used in this snapshot");
        if(putMap == null) {
            putMap = new HashMap<String, Node>();
            revPutMap = new HashMap<Node, String>();
        }
        putMap.put(uri, root);
        revPutMap.put(root, uri);
    }
    
    private void doActualPut(Node root, String uri)
        throws DataModelException, EvaluationException
    {
        {
            // store on file only: TODO or not? 
            throw new EvaluationException("fn:put() supported only on XML Libraries");
        }
    }

    static String validatedURI(String s) throws DataModelException
    {
        try {
            // accepted by java.net.URI: considered ok
            URI uri = new URI(s);
            if(uri.getScheme() != null || uri.getAuthority() != null)
                throw new DataModelException("invalid document path: " + s);
            return uri.getPath();
        }
        catch (URISyntaxException e) {
            return null;
        }
    }

    public void apply()
        throws EvaluationException
    {
        if (trace > 0) {
            System.err.println("=== Updates.apply ===");
            NodeUpdates[] ups =
                (NodeUpdates[]) nodeUpdates.getKeys(new NodeUpdates[nodeUpdates.getSize()]);
            if (trace >= 2) {
                for (int i = 0; i < ups.length; i++) {
                    System.err.println(" " + ups[i]); // rubups[i];
                }
                System.err.println("Updated docs: " + docs); 
            }
        }

        try {
            for (Iterator<FONIDataModel> iter = docs.iterator(); iter.hasNext();) {
                FONIDataModel dm = iter.next();
                FONIDocument doc = dm.getDom();
                BasicNode root = dm.getDocumentNode();

                // output appropriate for the doc:
                if (doc instanceof IDocument) {
                    IDocument idoc = (IDocument) doc;
                    XMLPushStreamBase pdoc =
                        updaterFactory.newParsedDocument(idoc.getBaseURI());
                    pdoc.setCheckNS(true);
                    rebuild(root, pdoc);
                    updaterFactory.endParsedDocument();
                    // put it back to the document pool of the session:
                    // TODO or not
                }
                else { // Library doc
                }
            }
            
            if(putMap != null) {
                // actual puts to Library
                for (Iterator<String> iter = putMap.keySet().iterator(); iter.hasNext();) {
                    String uri = iter.next();
                    Node root = putMap.get(uri);
                    doActualPut(root, uri);
                }
            }
            
        }
        catch (DataModelException e) {
            throw BasicNode.wrapDMException(e);
        }
    }

    private void rebuild(Node node, XMLPushStream stream)
        throws DataModelException, EvaluationException
    {
        NodeUpdates nu = rawNodeUpdates(node);
        boolean deleted = false;

//        String putUri = revPutMap == null? null : (String) revPutMap.get(node);
//        if(putUri != null) {
//            // not same as simple put: need to rebuild
//            putMap.remove(putUri);  // that first, to avoid recursive loop...
//            revPutMap.remove(node);
//            XMLPushStream out = updaterFactory.newLibraryDocument(putUri);
//            rebuild(node, (XMLPushStreamBase) out);
//            //updaterFactory.endLibraryDocument();
//        }
        
        // insertion before
        if (nu != null) {
            if(trace > 0)
                System.err.println("Updated node "+node);
            putNodeList(stream, nu.insertedBefore);
            deleted = nu.deleted;
        }

        switch (node.getNodeNature()) {
        case Node.DOCUMENT: {
            stream.putDocumentStart();

            if (nu != null)
                putNodeList(stream, nu.insertedFirst);

            Node kid = node.getFirstChild();
            for (; kid != null; kid = kid.getNextSibling())
                rebuild(kid, stream);

            if (nu != null) {
                putNodeList(stream, nu.insertedInto);
                putNodeList(stream, nu.insertedLast);
            }

            stream.putDocumentEnd();
            break;
        }
        case Node.ELEMENT:
            if (!deleted) {
                QName name = (nu != null && nu.newName != null) ?
                                    nu.newName : node.getNodeName();
                if (nu == null || nu.replacement == null) {
                    stream.putElementStart(name);
                    // namespaces are not edited
                    stream.putNamespaces(node, 0);
                    // attributes
                    Node[] attrs = node.getAttributes();
                    if (attrs != null)
                        for (int a = 0; a < attrs.length; a++) {
                            Node attr = attrs[a];
                            NodeUpdates anu = rawNodeUpdates(attr);
                            if (anu == null)
                                stream.putAttribute(attr.getNodeName(),
                                                    attr.getStringValue(),
                                                    null);
                            else {
                                if(trace > 0)
                                    System.err.println("Update attr "+attr);
                                buildAttributes(anu, stream);
                            }
                        }

                    if (nu != null) {
                        ArrayList<Node> insAttrs = nu.insertedAttrs;
                        if (insAttrs != null) {
                            for (int a = 0, asize = insAttrs.size(); a < asize; a++) {
                                Node attr = insAttrs.get(a);
                                if(trace > 0)
                                    System.err.println("New attr "+attr);
                                QName attrName = attr.getNodeName();
                                stream.putAttribute(attrName, attr.getStringValue(), null);
                                if(nsInherit)
                                    ensureMapping(stream, attrName);
                            }
                        }
                        putNodeList(stream, nu.insertedFirst);
                    }
                    
                    if(nsInherit)
                        ensureMapping(stream, name);

                    if (nu != null && nu.replacedValue != null) {
                        if(trace > 0)
                            System.err.println("Replace node value "+node);
                        stream.putText(nu.replacedValue);
                    }
                    else {
                        Node kid = node.getFirstChild();
                        for (; kid != null; kid = kid.getNextSibling())
                            rebuild(kid, stream);
                    }

                    if (nu != null) {
                        putNodeList(stream, nu.insertedInto);
                        putNodeList(stream, nu.insertedLast);
                    }
                    stream.putElementEnd(name);
                }
                else {  // replace
                    putNodeList(stream, nu.replacement);
                }
            }
            else {
                QName dupName = nu.checkDupAttribute(node); // bullshit! on deleted nodes!
                if(dupName != null)
                    throw new DataModelException("XUDY0021",
                                       "Duplicate attribute '" + dupName + "'");
                // a bit weird, but replace has precedence over delete:
                putNodeList(stream, nu.replacement);
            }
            break;

        case Node.ATTRIBUTE:    // happens in Transform
            NodeUpdates anu = rawNodeUpdates(node);
            if (anu == null)    // unmodified
                stream.putAttribute(node.getNodeName(), node.getStringValue(), null);
            else {
                buildAttributes(anu, stream);
            }
            break;
            
        case Node.TEXT:
            if (nu != null) {
                if (nu.replacement != null)
                    putNodeList(stream, nu.replacement);
                else if (!deleted)
                    stream.putText((nu.replacedValue != null) ?
                                    nu.replacedValue : node.getStringValue());
            }
            else
                stream.putText(node.getStringValue());
            break;

        case Node.COMMENT:
            if (nu != null) {
                if (nu.replacement != null)
                    putNodeList(stream, nu.replacement);
                else if (!deleted)
                    stream.putComment((nu.replacedValue != null) ?
                                     nu.replacedValue : node.getStringValue());
            }
            else
                stream.putComment(node.getStringValue());
            break;

        case Node.PROCESSING_INSTRUCTION:
            if (nu == null)
                stream.putProcessingInstruction(node.getNodeName().getLocalPart(),
                                                node.getStringValue());
            else {
                if (nu.replacement != null)
                    putNodeList(stream, nu.replacement);
                else if (!deleted) {
                    QName name =
                        (nu.newName != null) ? nu.newName : node.getNodeName();
                    String value = (nu.replacedValue == null) ?
                                      node.getStringValue() : nu.replacedValue;
                    stream.putProcessingInstruction(name.getLocalPart(), value);
                }
            }
            break;
        }

        // insertion after
        if (nu != null)
            putNodeList(stream, nu.insertedAfter);
    }

    private void buildAttributes(NodeUpdates anu, XMLPushStream stream)
        throws DataModelException, EvaluationException
    {
        if (anu.deleted)
            return;
        QName nm = (anu.newName == null) ? anu.node.getNodeName() : anu.newName;
        Item[] rep = anu.replacement;
        if (rep != null) {
            for (int i = 0; i < rep.length; i++) {
                if (rep[i].isNode()) {
                    Node attr = rep[i].getNode();
                    QName attrName = attr.getNodeName();
                    stream.putAttribute(attrName, attr.getStringValue(), null);
                    if(nsInherit)
                        ensureMapping(stream, attrName);
                }
            }
        }
        else if (anu.replacedValue != null) {
            stream.putAttribute(nm, anu.replacedValue, null);
        }
        else {
            // must be rename
            stream.putAttribute(nm, anu.node.getStringValue(), null);
            if(nsInherit)
                ensureMapping(stream, nm);
        }
    }

    private void ensureMapping(XMLPushStream stream, QName name)
        throws DataModelException
    {
        if(name == null || name.hasNoNamespace())
            return;
        String prefix = stream.getNSPrefix(name.getNamespaceURI());
        if(prefix != null)
            return; // OK defined
        prefix = name.getPrefix();
        if(prefix == null)
            prefix = dynamicContext.getNSPrefix(name.getNamespaceURI());
        if(prefix != null)
            stream.putNamespace(prefix, name.getNamespaceURI());
    }

    private String buildText(XQValue items)
        throws EvaluationException, DataModelException
    {
        StringBuffer b = new StringBuffer(20);
        for (boolean first = true; items.next(); first = false) {
            if (!first)
                b.append(' ');
            b.append(items.getString());
        }
        return b.toString();
    }

    private void putNodeList(XMLPushStream stream, ArrayList<Node> list)
        throws DataModelException
    {
        if (list != null)
            for (int i = 0, asize = list.size(); i < asize; i++) {
                Node n = list.get(i);
                stream.putNodeCopy(n, nsCopyMode);
                //ensureMapping(stream, n.getNodeName(), false);
            }
    }

    private void putNodeList(XMLPushStream stream, Item[] list)
        throws DataModelException
    {
        if (list != null)
            for (int i = 0; i < list.length; i++) {
                Node n = (Node) list[i];
                stream.putNodeCopy(n, nsCopyMode);
                //ensureMapping(stream, n.getNodeName(), false);
            }
    }

    private void init(DynamicContext dynamicContext)
    {
        this.dynamicContext = dynamicContext;
        nsCopyMode = dynamicContext.mainQuery.getCopyNSMode();
        nsInherit = nsCopyMode == ModuleContext.NS_PRES_INHERIT ||
                    nsCopyMode == ModuleContext.NS_NOPRES_INHERIT;
        nodeUpdates = new HTable();
        probe = new NodeUpdates(null);
        docs = new HashSet<FONIDataModel>();
        updaterFactory = dynamicContext.getUpdaterFactory();
        pedantic = dynamicContext.mainQuery.sObs();
    }

    private NodeUpdates getNodeUpdates(Node node, EvalContext context,
                                       Expression expr)
        throws EvaluationException
    {
        NodeUpdates nu = rawNodeUpdates(node);
        if (nu == null)
            nodeUpdates.directPut(nu = new NodeUpdates(node));

        if (transformContext != null) {
            // if in 'transform' mode, check the node is contained within one
            // of the copied roots
            int r = roots.length;
            try {
                for (; --r >= 0;) {
                    if (roots[r].contains(node))
                        break;
                }
            }
            catch (DataModelException e) {
                context.error(e.getErrorCode(), expr, e.getMessage());
            }
            if (r < 0)
                context.error("XUDY0014", expr);
        }
        else {
            // normal mode: maintain a list of updated documents
            if (node instanceof FONINode) {
                FONINode fnode = (FONINode) node;
                docs.add(fnode.getDM());
            }
            else
                ; // updating core nodes: just don't care
        }
        return nu;
    }

    private NodeUpdates rawNodeUpdates(Node node)
    {
        probe.node = node;
        NodeUpdates nu = (NodeUpdates) nodeUpdates.get(probe);
        return nu;
    }

    public void setTransformContext(EvalContext context)
    {
        transformContext = context;
    }


    private static class NodeUpdates extends HTable.Key
    {
        Node node;
        boolean deleted;
        QName newName;
        String  replacedValue;  // value replacement
        Item[]  replacement;    // node replacement
        ArrayList<Node> insertedBefore;
        ArrayList<Node> insertedFirst;
        ArrayList<Node> insertedInto;
        ArrayList<Node> insertedLast;
        ArrayList<Node> insertedAfter;
        ArrayList<Node> insertedAttrs;

        NodeUpdates(Node node)
        {
            this.node = node;
        }

        public Key duplicate()
        {
            return new NodeUpdates(node);
        }

        public String toString()
        {
            return "Update node="
                   + node
                   + ": "
                   + (newName != null ? "renamed " + newName + ", " : "")
                   + (replacedValue != null ? "replaced value "
                                              + replacedValue + ", " : "")
                   + (replacement != null ? "replaced by " + replacement
                                            + ", " : "")
                   + (insertedBefore != null ? "ins B " + insertedBefore
                                               + ", " : "")
                   + (insertedFirst != null ? "ins F " + insertedFirst + ", "
                           : "")
                   + (insertedInto != null ? "ins I " + insertedInto + ", "
                           : "")
                   + (insertedLast != null ? "ins L " + insertedLast + ", "
                           : "")
                   + (insertedAfter != null ? "ins A " + insertedAfter + ", "
                           : "")
                   + (insertedAttrs != null ? "ins Attr " + insertedAttrs
                                              + ", " : "")
                   + (deleted ? "deleted" : "");
        }
        
        QName checkDupAttribute(Node parent) throws DataModelException
        {
            if(insertedAttrs != null)
                for(int a = insertedAttrs.size(); --a >= 0; ) {
                    Node attr = insertedAttrs.get(a);
                    QName name = attr.getNodeName();
                    if(parent != null && parent.getAttribute(name) != null)
                        return name;
                    for(int a2 = a; --a2 >= 0; ) {
                        Node attr2 = insertedAttrs.get(a2);
                        if(name == attr2.getNodeName())
                            return name;
                    }
                }
            return null;
        }

        public boolean equals(Object obj)
        {
            if (obj instanceof NodeUpdates)
                return node.equals(((NodeUpdates) obj).node);
            return false;
        }

        public int hashCode()
        {
            return node.hashCode();
        }
    }
}
