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
package com.qizx.xdm;

import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.api.QizxException;
import com.qizx.api.XMLPullStream;
import com.qizx.api.util.PushStreamToDOM;
import com.qizx.api.util.time.DateTimeBase;
import com.qizx.api.util.time.DateTimeException;
import com.qizx.api.util.time.Duration;
import com.qizx.util.NamespaceContext;
import com.qizx.util.basic.Comparison;
import com.qizx.util.basic.FileUtil;
import com.qizx.xquery.ComparisonContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.StringValue;
import com.qizx.xquery.dt.UntypedAtomicType;

import java.io.Serializable;
import java.net.URI;
import java.text.Collator;
import java.util.HashMap;

//import javax.xml.stream.events.Namespace;

/**
 * Basic in-core Implementation of XQuery Data Model. Used for constructed tree
 * fragments.
 */
public class CoreDataModel
    implements Serializable
{
    protected String baseURI;
    protected HashMap idMap;

    public CoreDataModel(String baseURI)
    {
        this.baseURI = baseURI;
    }

    public abstract class CoreNode extends BasicNode
    {
        public Element parent;
        public CoreNode nextSibling;

        public int order = -1; // TODO ? seems broken

        public String toString()
        {
            try {
                return "Core " + getNodeKind() + " "
                       + (getNodeName() != null ? (" name=" + getNodeName())
                                                : getStringValue());
            }
            catch (DataModelException e) {
                return "[[" + e + "]]";
            }
        }

        public String getBaseURI()
            throws DataModelException
        {
            IQName base = IQName.get(NamespaceContext.XML, "base");
            int kind = getNodeNature();
            if(kind != DOCUMENT && kind != ELEMENT)
                return null;
            return getBaseUri(this, base);
        }
        
        private String getBaseUri(CoreNode node, IQName base)
            throws DataModelException
        {
            
            for (CoreNode n = node; n != null; n = n.parent) {
                CoreNode attr = (CoreNode) n.getAttribute(base);
                if (attr != null) {
                    String frag = attr.getStringValue();
                    // if absolute URI, that's it:
                    URI uri = FileUtil.uriConvert(frag);
                    if (uri == null || uri.isAbsolute())
                        return frag;
                    // otherwise resolve against baseURI of parent
                    return FileUtil.resolve(getBaseUri(n.parent, base), frag);
                }
                    
            }
            return baseURI;
        }
        

        public String[] getDTDInfo()
        {
            return null;
        }

        // to get rid of 'throws':
        public abstract int getNodeNature();
        // to get rid of 'throws':
        public abstract String getNodeKind();

        public abstract boolean isElement();

        public abstract NodeSequenceBase getChildren();

        public String getDocumentURI()
        {
            return null;
        }

        public QName getNodeName()
        {
            return null;
        }

        public IQName getName()
        {
            return null;
        }

        public Node getParent()
        {
            return parent;
        }

        public String getString()
            throws EvaluationException
        {
            try {
                return getStringValue();
            }
            catch (DataModelException e) {
                throw BasicNode.wrapDMException(e); 
            }
        }

        public BasicNode firstChild()
        {
            return null;
        }
        
        public BasicNode nextSibling()
        {
            return nextSibling;
        }

        public Node getDocumentNode()
        {
            return getTop();
        }

        public int documentOrderCompareTo(Node xnode) throws DataModelException
        {
            if (xnode == this)
                return 0;
            BasicNode node = (BasicNode) xnode;
            if (!(node instanceof CoreNode))
                return Comparison.of(docPosition(), node.docPosition());

            CoreNode that = (CoreNode) node;
            CoreNode top = getTop(), thatTop = that.getTop();
            if (top != thatTop)
                return Comparison.of(top.docPosition(), thatTop.docPosition());
            // if possible, use order stamps:
            if (order >= 0 && that.order >= 0)
                return Comparison.of(order, that.order);
            // inefficient, but not supposed to be used heavily:
            for (; that != null; that = (CoreNode) that.getNextNode())
                if (that == this)
                    return 1;
            return -1;
        }

        public boolean contains(Node node)
        {
            if (!(node instanceof CoreNode))
                return false;

            for (CoreNode n = (CoreNode) node; n != null; n = n.parent)
                if (n == this)
                    return true;
            return false;
        }

        public boolean equals(Object that)
        {
            return this == that;
        }

        // Redefined here just for pleasing code verifiers
        public int hashCode()
        {
            return super.hashCode();
        }

        public boolean deepEquals(XQItem item, ComparisonContext context)
            throws EvaluationException
        {
            if (!(item instanceof Node))
                return false;
            try {
                return deepEq((BasicNode) item.getNode(), context.getCollator());
            }
            catch (DataModelException e) {
                return false; // should not happen
            }
        }

        public boolean deepEquals(Node item, Collator collator)
            throws DataModelException
        {
            return deepEq((BasicNode) item, collator);
        }

        public boolean deepEq(BasicNode that, Collator collator)
            throws DataModelException
        {
            int kind = getNodeNature();
            if (kind != that.getNodeNature() || getName() != that.getNodeName())
                return false;
            if (kind == Node.DOCUMENT)
                return ((Element) this).contentEq(that, collator);
            else if (kind == Node.ELEMENT)
                return ((Element) this).attributesEq(that, collator)
                       && ((Element) this).contentEq(that, collator);
            else
                // compare by string value
                return valueEq(that, collator);
        }

        protected boolean valueEq(Node that, Collator collator)
            throws DataModelException
        {
            return StringValue.compare(getStringValue(),
                                       that.getStringValue(), collator) == Comparison.EQ;
        }

        CoreNode getTop()
        {
            CoreNode n = this;
            for (; n.parent != null;)
                n = n.parent;
            return n;
        }

        public int/*DId*/ docPosition()
        {
            return getTop().hashCode();
        }

        public int getDefinedNSCount()
        {
            return 0;
        }

        // ---- extensions for XPath steps:

        public NodeSequenceBase getAncestors(NodeFilter nodeTest)
        {
            return new Ancestors(this, nodeTest);
        }

        public NodeSequenceBase getAncestorsOrSelf(NodeFilter nodeTest)
        {
            return new AncestorsOrSelf(this, nodeTest);
        }

        public NodeSequenceBase getParent(NodeFilter nodeTest)
        {
            return new Parent(this, nodeTest);
        }

        public NodeSequenceBase getChildren(NodeFilter nodeTest)
        {
//            if(reuse instanceof Children) {
//                return ((Children) reuse).restart(this, nodeTest);
//            }
            return new Children(this, nodeTest);
        }

        public NodeSequenceBase getDescendants(NodeFilter nodeTest)
        {
            return new Descendants(this, nodeTest);
        }

        public NodeSequenceBase getDescendantsOrSelf(NodeFilter nodeTest)
        {
            return new DescendantsOrSelf(this, nodeTest);
        }

        public NodeSequenceBase getAttributes(NodeFilter nodeTest)
        {
            return new Attributes(this, nodeTest);
        }

        public NodeSequenceBase getFollowingSiblings(NodeFilter nodeTest)
        {
            return new FollowingSiblings(this, nodeTest);
        }

        public NodeSequenceBase getPrecedingSiblings(NodeFilter nodeTest)
        {
            return new PrecedingSiblings(this, nodeTest);
        }

        public NodeSequenceBase getFollowing(NodeFilter nodeTest)
        {
            return new Following(this, nodeTest);
        }

        public NodeSequenceBase getPreceding(NodeFilter nodeTest)
        {
            return new Preceding(this, nodeTest);
        }

        public void setNextSibling(CoreNode sibling)
        {
            nextSibling = sibling;
            if (sibling != null)
                sibling.parent = parent;
        }

        public BasicNode[] getIdMatchingNodes(String id, boolean idref)
        {
            if(idref)
                return null;
            return new BasicNode[] { getNodeFromId(id) };
        }

        // -------- implements dm.Node ----------------------------------------

        public void addText(String text)
        {
        }

        public String getNamespacePrefix(String nsuri)
        {
            return null;
        }

        public String getNamespaceUri(String prefix)
        {
            return null;
        }

        public boolean hasLocalNamespaces()
        {
            return false;
        }
        
        public void addInScopeNamespacesTo(NamespaceContext nsContext)
        {
        }

        public int addNamespacesTo(NamespaceContext nsContext)
        {
            return 0;
        }

        public int addUsedNamespacesTo(NamespaceContext nsContext)
            throws DataModelException 
        {
            return 0;
        }        

        public boolean isAtom()
        {
            return false;
        }

        public Node document()
        {
            return getDocumentNode();
        }

        public Node parent()
        {
            return getParent();
        }

        public Node attribute(IQName name) throws DataModelException
        {
            return getAttribute(name);
        }


        // ---- typing extensions:

        public char[] getCharValue()
        {
            throw new RuntimeException("getChars of a non-atom");
        }

        public Object getValue()
        {
            return null; // not implemented
        }

        public long getIntegerValue()
        {
            return -1; // not implemented
        }

        public XMLPullStream exportNode()
        {
            return new NodePullStream(this);
        }

        public Object getObject()
            throws QizxException
        {
            return new PushStreamToDOM().exportNode(this);
        }

        public BasicNode basicNode()
        {
            return this;
        }

        public int compareTo(XQItem that, ComparisonContext context, int flags)
            throws EvaluationException
        {
            // TODO: optimise by traversal comparison (stream on the stringvalue)
            return UntypedAtomicType.comparison(this, that, context, flags);
        }

        public Duration getDuration() throws EvaluationException
        {
            try {
                return Duration.parseDuration(getString());
            }
            catch (DateTimeException e) {
                throw new EvaluationException(e.getMessage());
            }
        }

        public DateTimeBase getMoment() throws EvaluationException
        {
            // Should not be called
            throw new EvaluationException("cannot convert node to date or time");
        }

        // ------------------ node ---------------------------------------
        
        public Node getAttribute(QName name) throws DataModelException
        {
            return null;
        }

        public int getAttributeCount() throws DataModelException
        {
            return 0;
        }

        public Node[] getAttributes() throws DataModelException
        {
            return null;
        }
        public long getLongAtomValue()
        {
            return 0;
        }
    }

    class Element extends CoreNode
    {
        QName name;
        CoreNode firstChild;
        // list of attributes AND NS nodes: attr first, then NS
        Attribute attributes;

        Element(QName name)
        {
            this.name = name;
        }

        public String getNodeKind()
        {
            return "element";
        }

        public String getStringValue() throws DataModelException
        {
            StringBuffer buf = new StringBuffer();
            for (CoreNode kid = firstChild; kid != null; kid = kid.nextSibling)
                recStringValue(kid, buf);
            return buf.toString();
        }

        private void recStringValue(CoreNode node, StringBuffer buf)
            throws DataModelException
        {
            if (node.isElement()) {
                CoreNode kid = ((Element) node).firstChild;
                for (; kid != null; kid = kid.nextSibling)
                    recStringValue(kid, buf);
            }
            else if (node.getNodeNature() == Node.TEXT) // ignore comments and PI
                buf.append(node.getStringValue());
        }

        public NodeSequenceBase getChildren()
        {
            return new SSequence(firstChild);
        }

        public QName getNodeName()
        {
            return name;
        }

        public IQName getName()
        {
            return IQName.get(name);
        }

        public Node getFirstChild()
        {
            return firstChild;
        }
        
        public BasicNode firstChild()
        {
            return firstChild;
        }

        public int getDefinedNSCount()
        {
            int cnt = 0;
            for (CoreNode attr = attributes; attr != null; attr =
                attr.nextSibling)
                if (attr.getNodeNature() == NAMESPACE)
                    ++cnt;
            return cnt;
        }

        // return first NSnode on element
        private CoreNode getFirstNS()
        {
            CoreNode attr = attributes;
            for (; attr != null; attr = attr.nextSibling)
                if (attr instanceof NSNode)
                    return attr;
            return null;
        }

        public String getNamespacePrefix(String nsuri)
        {
            for (Element e = this; e != null; e = e.parent) {
                for (CoreNode attr = e.getFirstNS(); attr != null; attr =
                    attr.nextSibling) {
                    NSNode ns = (NSNode) attr;
                    if (nsuri.equals(ns.strValue))
                        return ns.name.getLocalPart();
                }
            }
            return null;
        }

        public String getNamespaceUri(String prefix)
        {
            for (Element e = this; e != null; e = e.parent) {
                for (CoreNode attr = e.getFirstNS(); attr != null; attr =
                    attr.nextSibling) {
                    NSNode ns = (NSNode) attr;
                    if (prefix.equals(ns.name.getLocalPart()))
                        return ns.getStringValue();
                }
            }
            return null;
        }

        public void addInScopeNamespacesTo(NamespaceContext nsContext)
        {
            recAddInscopeNS(nsContext);
        }

        private void recAddInscopeNS(NamespaceContext nsContext)
        {
             if(parent != null)
                 parent.recAddInscopeNS(nsContext);
//             else if(inScopeNS != null) {
//                 // NS ihnerited from static context
//                 for(int n = 0, cnt = inScopeNS.getLocalSize(); n < cnt; n++)
//                 nsContext.addMapping(inScopeNS.getLocalPrefix(n),
//                                      inScopeNS.getLocalNamespaceURI(n));
//             }
             addNamespacesTo(nsContext);
        }
        
        public boolean hasLocalNamespaces()
        {
            return getFirstNS() != null;
        }

        public int addNamespacesTo(NamespaceContext nsContext)
        {
            int count = 0;
            CoreNode nsNode = getFirstNS();
            for (; nsNode != null; nsNode = nsNode.nextSibling) {
                NSNode ns = (NSNode) nsNode;
                nsContext.addMapping(ns.name.getLocalPart(),
                                     ns.getStringValue());
                ++ count;
            }
            return count;
        }

        public int addUsedNamespacesTo(NamespaceContext nsContext) 
            throws DataModelException
        {
            int count = 0;
            IQName name = getName();
            String ns = name.getNamespaceURI();
            if (ns != "") {
                nsContext.addMapping(getNamespacePrefix(ns), ns);
                ++ count;
            }
            CoreNode attr = attributes;
            for (; attr != null; attr = attr.nextSibling) {
                name = attr.getName();
                ns = name.getNamespaceURI();
                String pref = getNamespacePrefix(ns);
                if (ns != "" && pref != null && 
                        attr.getNodeNature() == Node.ATTRIBUTE) {
                    nsContext.addMapping(pref, ns);
                    ++ count;
                }
            }
            return count;
        }

        // ---- extensions for internal use:

        public int getNodeNature()
        {
            return Node.ELEMENT;
        }

        public boolean isElement()
        {
            return true;
        }

        public boolean isAtom()
        {
            return false;
        }

        public boolean isWhitespace()
        {
            return false;
        }

        boolean attributesEq(BasicNode that, Collator collator)
            throws DataModelException
        {
            Node[] oattr = that.getAttributes();
            if(oattr == null)
                return getAttributeCount() == 0;
            if(oattr.length != getAttributeCount())
                return false;
            for (int a = oattr.length; --a >= 0;) {
                Node oatt = oattr[a];
                QName attrName = oatt.getNodeName();
                CoreNode att = (CoreNode) getAttribute(attrName);
                if (att == null || !att.valueEq(oatt, collator))
                    return false;
            }
            return true;
        }

        boolean contentEq(BasicNode that, Collator collator)
            throws DataModelException
        {
            BasicNode okid = that.firstChild();
            for (CoreNode kid = firstChild; kid != null; kid = kid.nextSibling)
            {
                int kidkind = kid.getNodeNature(), okidkind;
                if (kidkind == Node.COMMENT
                    || kidkind == Node.PROCESSING_INSTRUCTION)
                    continue;
                
                for (;; okid = okid.nextSibling()) {
                    if (okid == null)
                        return false;
                    if ((okidkind = okid.getNodeNature()) != Node.COMMENT
                        && okidkind != Node.PROCESSING_INSTRUCTION)
                        break;
                }
                // we have 2 comparable kids:
                if (!kid.deepEq(okid, collator))
                    return false;
                okid = okid.nextSibling();
            }
            // just check that there are no more kids in 'that'
            return okid == null;
        }

        public int getAttributeCount() throws DataModelException
        {
            int cnt = 0;
            for (CoreNode attr = attributes; attr != null; attr =
                attr.nextSibling)
                if (attr.getNodeNature() == ATTRIBUTE)
                    ++cnt;
            return cnt;
        }

        public Node getAttribute(QName name) throws DataModelException
        {
            IQName iname = IQName.get(name);
            CoreNode attr = attributes;
            for (; attr != null; attr = attr.nextSibling)
                if (attr.getName() == iname
                    && attr.getNodeNature() == Node.ATTRIBUTE)
                    return attr;
            return null;
        }

        public Node[] getAttributes() throws DataModelException
        {
            int cnt = getAttributeCount();
            if(cnt == 0)
                return null;
            Node[] attrs = new Node[cnt];
            CoreNode attr = attributes;
            for (int a = 0; attr != null; attr = attr.nextSibling)
                if (attr.getNodeNature() == ATTRIBUTE)
                    attrs[a++] = attr;            
            return attrs;
        }
        

        public void setAttributes(Attribute attr)
        {
            attributes = attr;
            for (; attr != null; attr = (Attribute) attr.nextSibling)
                attr.parent = this;
        }

        // does not check
        public boolean addNamespace(NSNode ns)
        {
            Attribute last = null, n = attributes;
            // look for first NS, note previous node
            for (; n != null; last = n, n = (Attribute) n.nextSibling) {
                if (n.getNodeNature() == Node.NAMESPACE) {
                    if (n.getName() == ns.getName())
                        return false; // duplicate
                } // dont break: go to end
            }
            if (last == null) { // no attr: insert before other NS
                ns.nextSibling = attributes;
                attributes = ns;
            }
            else {
                ns.nextSibling = last.nextSibling;
                last.nextSibling = ns;
            }
            return true;
        }

        // now only real attribute, no more NS
        // Attributes come first, then NS nodes
        public boolean addAttribute(Attribute attr) throws DataModelException
        {
            Attribute last = null, n = attributes;
            // look for first NS, note previous node
            for (; n != null; last = n, n = (Attribute) n.nextSibling) {
                if (n.getNodeNature() == Node.NAMESPACE)
                    break;
                // check duplicate:
                if (n.name == attr.name) {
                    n.strValue = attr.strValue;
                    return false; // duplicate
                }
            }
            if (last == null) { // no attr: insert before other NS
                attr.nextSibling = attributes;
                attributes = attr;
            }
            else {
                attr.nextSibling = last.nextSibling;
                last.nextSibling = attr;
            }
            attr.parent = this;
            return true;
        }

        public void addText(String value)
        {
            throw new RuntimeException("addText on an element");
        }
    }

    class Document extends Element
    {
        Document()
        {
            super(null);
        }

        public String getNodeKind()
        {
            return "document";
        }

        public int getNodeNature()
        {
            return Node.DOCUMENT;
        }

        public boolean addAttribute(Attribute attr) throws DataModelException
        {
            throw new DataModelException("XPTY0004",
                                         "adding attribute to a document node");
        }
    }

    class TextNode extends CoreNode
    {
        String strValue;
        // efficient handling of concatenations in PushBuilder:
        char[] charValue;
        int    charCount;

        TextNode(String value)
        {
            this.strValue = value;
        }

        TextNode()
        {
            this("");
        }

        public String getNodeKind()
        {
            return "text";
        }

        public int getNodeNature()
        {
            return Node.TEXT;
        }

        public void addText(String value)
        {
            int vlen = value.length();
            if(charValue != null) {
                ensureCapacity(charCount + vlen);
                value.getChars(0, vlen, charValue, charCount);
                charCount += vlen;
            }
            else {
                if (strValue == null || strValue.length() == 0)
                    this.strValue = value;
                else {
                    int oldLen = strValue.length();
                    charValue = new char[vlen + oldLen];
                    strValue.getChars(0, oldLen, charValue, 0);
                    charCount = oldLen;
                    // recursive! but wont loop
                    addText(value);
                }
            }
        }

        private void ensureCapacity(int size)
        {
            if(size > charValue.length) {
                char[] old = charValue;
                charValue = new char[size + size / 4 + 1];
                System.arraycopy(old, 0, charValue, 0, old.length);
            }
        }

        public String getStringValue()
        {
            return charValue != null? new String(charValue, 0, charCount)
                                    : strValue;
        }

        public char[] getCharValue()
        {
            if (charValue == null)
                return strValue.toCharArray();
            if (charValue.length != charCount) {
                char[] old = charValue;
                charValue = new char[charCount];
                System.arraycopy(old, 0, charValue, 0, charCount);
            }
            return charValue;
        }

        public NodeSequenceBase getChildren()
        {
            return SSequence.emptySequence;
        }

        public Node[] getAttributes()
        {
            return null;
        }

        public String getNamespacePrefix(String nsuri)
        {
            return parent == null ? null : parent.getNamespacePrefix(nsuri);
        }

        public String getNamespaceUri(String prefix)
        {
            return parent == null ? null : parent.getNamespaceUri(prefix);
        }

        public boolean isElement()
        {
            return false;
        }

        public boolean isAtom()
        {
            return getNodeNature() >= Node.ATTRIBUTE;
        }

        public boolean isWhitespace()
        {
            return false;
        }

        public Node getAttribute(IQName name)
        {
            return null;
        }
    }

    class PINode extends TextNode
    {
        String target = "";

        PINode(String target, String value)
        {
            super(value);
            this.target = target;
        }

        public String getNodeKind()
        {
            return "processing-instruction";
        }

        public int getNodeNature()
        {
            return Node.PROCESSING_INSTRUCTION;
        }

        public QName getNodeName()
        {
            return getName();
        }

        public IQName getName()
        {
            return IQName.get(target);
        }
    }

    class CommentNode extends TextNode
    {
        CommentNode(String value)
        {
            super(value);
        }

        public String getNodeKind()
        {
            return "comment";
        }

        public int getNodeNature()
        {
            return Node.COMMENT;
        }
    }

    // Attribute and namespace nodes:
    class Attribute extends TextNode
    {
        QName name;

        Attribute(QName name)
        {
            this.name = name;
        }

        public IQName getName()
        {
            return IQName.get(name);
        }
        
        public QName getNodeName()
        {
            return name;
        }

        public String getNodeKind()
        {
            return "attribute";
        }

        public int getNodeNature()
        {
            return Node.ATTRIBUTE;
        }
    }

    // namespace Node:
    class NSNode extends Attribute
    {
        NSNode(IQName name, String value)
        {
            super(name);
            this.strValue = value;
        }

        NSNode(String prefix)
        {
            super(IQName.get(prefix));
        }

        public String getNodeKind()
        {
            return "namespace";
        }

        public int getNodeNature()
        {
            return Node.NAMESPACE;
        }
    }

    // sibling sequence
    static class SSequence extends NodeSequenceBase
    {
        static SSequence emptySequence = new SSequence(null);

        boolean started = false;

        protected CoreNode current, first;

        SSequence(CoreNode first)
        {
            this.first = this.current = first;
        }

        public XQValue bornAgain()
        {
            return new SSequence(first);
        }

        public boolean next() throws EvaluationException
        {
            if (started && current != null)
                current = current.nextSibling;
            started = true;
            return current != null;
        }

        public Node getNode()
        {
            return current;
        }

        public BasicNode getBaseNode()
        {
            return current;
        }

        public BasicNode basicNode()
        {
            return current;
        }

        // ------------ NodeSequence:

        public boolean nextNode() throws EvaluationException
        {
            return next();
        }

        public Node currentNode()
        {
            return current;
        }

        public NodeSequenceBase reborn()
        {
            return (NodeSequenceBase) bornAgain();
        }
    }

    static abstract class TypedSequence extends SSequence
    {
        NodeFilter nodeTest;

        TypedSequence(CoreNode first, NodeFilter nodeTest)
        {
            super(first);
            this.nodeTest = nodeTest;
        }

        boolean checkNode() //throws EvaluationException
        {
            return current != null
                   && (nodeTest == null || nodeTest.accepts(current));
        }

        public NodeSequenceBase restart(CoreNode node, NodeFilter nodeTest)
        {
            first = current = node;
            this.nodeTest = nodeTest;
            started = false;
            return this;
        }
    }

    static class Parent extends TypedSequence
    {
        Parent(CoreNode first, NodeFilter nodeTest)
        {
            super(first, nodeTest);
        }

        public boolean next() throws EvaluationException
        {
            if (started)
                return false;
            started = true;
            current = current.parent;
            return checkNode();
        }

        public XQValue bornAgain()
        {
            return new Parent(first, nodeTest);
        }
    }

    static class AncestorsOrSelf extends TypedSequence
    {

        AncestorsOrSelf(CoreNode first, NodeFilter nodeTest)
        {
            super(first, nodeTest);
        }

        public boolean next() throws EvaluationException
        {
            for (; current != null;) {
                if (started)
                    current = current.parent;
                started = true;
                if (checkNode())
                    return true;
            }
            return false;
        }

        public XQValue bornAgain()
        {
            return new AncestorsOrSelf(first, nodeTest);
        }
    }

    static class Ancestors extends AncestorsOrSelf
    {
        Ancestors(CoreNode first, NodeFilter nodeTest)
        {
            super(first, nodeTest);
            started = true;
        }

        public XQValue bornAgain()
        {
            return new Ancestors(first, nodeTest);
        }
    }

    static class Children extends TypedSequence
    {
        Children(CoreNode first, NodeFilter nodeTest)
        {
            super(first, nodeTest);
        }

        public boolean next() throws EvaluationException
        {
            for (; current != null;) {
                if (!started)
                    current = (CoreNode) current.firstChild();
                else
                    current = current.nextSibling;
                started = true;
                if (checkNode())
                    return true;
            }
            return false;
        }

        public XQValue bornAgain()
        {
            return new Children(first, nodeTest);
        }
    }

    static class DescendantsOrSelf extends TypedSequence
    {
        Node lastNode;

        DescendantsOrSelf(CoreNode first, NodeFilter nodeTest)
        {
            super(first, nodeTest);
            try {
                lastNode = first.getFollowingNode();
            }
            catch (DataModelException snh) { ; // should not happen
            }
        }

        public boolean next() throws EvaluationException
        {
            try {
                for (; current != null;) {
                    if (started)
                        current = (CoreNode) current.nodeNext();
                    started = true;
                    if (current == lastNode)
                        current = null;
                    else if (checkNode())
                        return true;
                }
            }
            catch (DataModelException shouldNotHappen) { ; }
            return false;
        }

        public XQValue bornAgain()
        {
            return new DescendantsOrSelf(first, nodeTest);
        }
    }

    static class Descendants extends DescendantsOrSelf
    {
        Descendants(CoreNode first, NodeFilter nodeTest)
        {
            super(first, nodeTest);
            started = true;
        }

        public XQValue bornAgain()
        {
            return new Descendants(first, nodeTest);
        }
    }

    static class FollowingSiblings extends TypedSequence
    {
        FollowingSiblings(CoreNode first, NodeFilter nodeTest)
        {
            super(first, nodeTest);
        }

        public boolean next() throws EvaluationException
        {
            for (; current != null;) {
                current = current.nextSibling;
                if (checkNode())
                    return true;
            }
            return false;
        }

        public XQValue bornAgain()
        {
            return new FollowingSiblings(first, nodeTest);
        }
    }

    static class Following extends TypedSequence
    {
        Following(CoreNode first, NodeFilter nodeTest)
        {
            super(first, nodeTest);
            // FIX: dont take descendants! (very old bug)
            try {
                if(first != null)
                    this.current = (CoreNode) first.nodeAfter();
            }
            catch (DataModelException shouldNotHappen) { ; }
        }

        public boolean next() throws EvaluationException
        {
            try {
                for (; current != null;) {
                    if(started)
                        current = (CoreNode) current.nodeNext();
                    started = true;
                    if (checkNode())
                        return true;
                }
            }
            catch (DataModelException shouldNotHappen) { ; }
            return false;
        }

        public XQValue bornAgain()
        {
            return new Following(first, nodeTest);
        }
    }

    static class PrecedingSiblings extends TypedSequence
    {
        PrecedingSiblings(CoreNode first, NodeFilter nodeTest)
        {
            super(first, nodeTest);
            current = first.parent == null ? null : first.parent.firstChild;
        }

        public boolean next() throws EvaluationException
        {
            for (; current != null;) {
                if (started)
                    current = current.nextSibling;
                started = true;
                if (current == first) {
                    current = null;
                    return false;
                }
                if (checkNode())
                    return true;
            }
            return false;
        }

        public XQValue bornAgain()
        {
            return new PrecedingSiblings(first, nodeTest);
        }
    }

    static class Preceding extends TypedSequence
    {
        Preceding(CoreNode first, NodeFilter nodeTest)
        {
            super(first, nodeTest);
            current = (CoreNode) first.getDocumentNode();
        }

        public boolean next() throws EvaluationException
        {
            try {
                for (; current != null;) {
                    if (started)
                        current = (CoreNode) current.nodeNext();
                    started = true;
                    if (current == first) {
                        current = null;
                        return false;
                    }
                    // FIX: no ancestors
                    if (!current.contains(first) && checkNode())
                        return true;
                }
            }
            catch (DataModelException shouldNotHappen) { ; }
            return false;
        }

        public XQValue bornAgain()
        {
            return new Preceding(first, nodeTest);
        }
    }

    static class Attributes extends TypedSequence
    {
        Attributes(CoreNode first, NodeFilter nodeTest)
        {
            super(first, nodeTest);
            if (first.getNodeNature() == Node.ELEMENT)
                current = ((Element) first).attributes;
        }

        public XQValue bornAgain()
        {
            return new Attributes(first, nodeTest);
        }

        public boolean next() throws EvaluationException
        {
            for (; current != null;) {
                if (started)
                    current = current.nextSibling;
                started = true;
                if (current == null)
                    break;
                switch (current.getNodeNature()) {
                case Node.ATTRIBUTE:
                    if (checkNode())
                        return true;
                    break;
                case Node.NAMESPACE: // ie no more attribs
                default:
                    current = null;
                    break;
                }
            }
            return false;
        }
    }

    // --------------- static construction ------------------------------------

    public Document newDocumentNode()
    {
        return new Document();
    }

    public Element newElement(QName name)
    {
        return new Element(name);
    }

    public Attribute newAttribute(QName name)
    {
        return new Attribute(name);
    }

    public NSNode newNSNode(String prefix)
    {
        return new NSNode(prefix);
    }

    public TextNode newTextNode(String value)
    {
        return new TextNode(value);
    }

    public TextNode newTextNode()
    {
        return new TextNode();
    }

    public PINode newPINode(String target)
    {
        return new PINode(target, "");
    }

    public PINode newPINode(String target, String value)
    {
        return new PINode(target, value);
    }

    public CommentNode newCommentNode(String value)
    {
        return new CommentNode(value);
    }

    // support of xml:id only
    public void addId(String value, Element elem)
    {
        if (idMap == null)
            idMap = new HashMap();
        idMap.put(value, elem);
    }

    public BasicNode getNodeFromId(String id)
    {
        return idMap == null? null : (BasicNode) idMap.get(id);
    }
}
