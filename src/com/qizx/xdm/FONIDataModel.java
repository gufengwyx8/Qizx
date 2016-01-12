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

import com.qizx.api.*;
import com.qizx.api.util.PushStreamToDOM;
import com.qizx.api.util.time.Date;
import com.qizx.api.util.time.DateTime;
import com.qizx.api.util.time.DateTimeBase;
import com.qizx.api.util.time.DateTimeException;
import com.qizx.api.util.time.Duration;
import com.qizx.queries.iterators.PostingIterator;
import com.qizx.util.Collations;
import com.qizx.util.NamespaceContext;
import com.qizx.util.basic.Comparison;
import com.qizx.util.basic.FileUtil;
import com.qizx.xdm.FONIDocument.Owner;
import com.qizx.xquery.ComparisonContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQTypeException;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.NodeType;
import com.qizx.xquery.dt.UntypedAtomicType;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Collator;

/**
 * Implementation of XQuery Data Model on top of Fully-Ordered documents. 
 * <p>Used for both indexed and parsed documents.
 * <p>In principle attached to a particular XQuerySession.
 */
public class FONIDataModel
{
    private static NodeSequenceBase empty = null;

    private FONIDocument dom;
    private FONINode root;
    private DataConversion dataConversion;

    public FONIDataModel(FONIDocument dom)
    {
        this.dom = dom;
        if(dom.getOwner() != null)
            dataConversion = dom.getOwner().getDataConversion();
    }

    public FONIDocument getDom()
    {
        return dom;
    }

    public BasicNode getDocumentNode()
        throws DataModelException
    {
        if (root == null)
            root = newNode(dom.getRootNode());
        return root;
    }

    public FONINode newNode(int/*NId*/ id)
    {
        if (id == 0)
            return null;
        return new FONINode(id, this);
    }

    public Node newDmNode(int/*NId*/ id)
    {
        return newNode(id);
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof FONIDataModel))
            return false;
        FONIDataModel odoc = (FONIDataModel) obj;
        return dom.equals(odoc.dom);            
    }

    public int hashCode()
    {
        return dom.hashCode();  // may have several DM for one doc
    }

    public static class FONINode extends BasicNode
    {
        public static final int ROOT_DEPTH = -1;
        
        protected FONIDataModel dm;
        protected int/*NId*/ id;

        public FONINode(int/*NId*/ id, FONIDataModel dm)
        {
            this.id = id;
            this.dm = dm;
        }

        public FONIDataModel getDM()
        {
            return dm;
        }

        public FONIDocument getDom()
        {
            return dm.dom;
        }

        public Owner getOwner()
        {
            return getDom().getOwner();
        }
        
        public int/*DId*/ getDocId()
        {
            return getDom().getDocumentId();
        }
        
        public String toString()
        {
            return id + ":" + getDom().getDocumentId() + ":" + super.toString();
        }

        public Node getParent()
            throws DataModelException
        {
            return getDM().newNode(getDom().getParent(id));
        }

        public BasicNode firstChild()
            throws DataModelException
        {
            return getDM().newNode(getDom().getFirstChild(id));
        }

        public BasicNode nextSibling()
            throws DataModelException
        {
            return getDM().newNode(getDom().getNextSibling(id));
        }

        public BasicNode basicNode()
        {
            return this;
        }

        public XQValue getTypedValue()
        {
            return null;
        }

        public QName getNodeName()
            throws DataModelException
        {
            return getDom().getName(id);
        }

        public String getStringValue()
            throws DataModelException
        {
            return getDom().getStringValue(id);
        }
        
        public char[] getCharValue()
            throws DataModelException
        {
            return getDom().getCharValue(id, 0);
        }

        public double getDoubleByRules()
        {
            if(dm.dataConversion != null) {
                return dm.dataConversion.convertNumber(this);
            }
            return Double.NaN;  
        }

        public Date getDate()
            throws EvaluationException, DateTimeException
        {
            try {
                return Date.parseDate(getStringValue());
            }
            catch(DateTimeException dex) {
                if(dm.dataConversion != null) {
                    double t = dm.dataConversion.convertDate(this);
                    if(t == t)
                        return new Date((long) t);
                }
            }
            catch(DataModelException e) { }
            throw new XQTypeException(Conversion.ERR_CAST,
                                      "cannot cast to xs:date: invalid value");
        }

        public DateTime getDateTime()
            throws EvaluationException, DateTimeException
        {
            try {
                return DateTime.parseDateTime(getStringValue());
            }
            catch(DateTimeException dex) {
                if(dm.dataConversion != null) {
                    double t = dm.dataConversion.convertDate(this);
                    if(t == t)
                        return new DateTime((long) t);
                }
            }
            catch(DataModelException e) { }
            throw new XQTypeException(Conversion.ERR_CAST,
                                      "cannot cast to xs:dateTime: invalid value");
        }

        public int getNodeNature()
            throws DataModelException
        {
            return getDom().getKind(id);
        }

        public String getNodeKind()
            throws DataModelException
        {
            switch (getDom().getKind(id)) {
            case Node.DOCUMENT:
                return "document";
            case Node.ELEMENT:
                return "element";
            case Node.ATTRIBUTE:
                return "attribute";
            case Node.TEXT:
                return "text";
            case Node.PROCESSING_INSTRUCTION:
                return "processing-instruction";
            case Node.COMMENT:
                return "comment";
            case Node.NAMESPACE:
                return "namespace";
            default:
                throw new DataModelException("wrong FONInode, id=" + id);
            }
        }

        public String getDocumentURI()
            throws DataModelException
        {
            return getDom().getBaseURI();
        }

        public String getBaseURI()
            throws DataModelException
        {
            FONIDocument dom = getDom();
            int nameid = dom.internOtherName(IQName.XML_BASE);
            if (nameid >= 0)
                try {
                    return getBaseUri(dom, id, nameid);
                }
                catch (URISyntaxException e) {
                    throw new DataModelException(e.getMessage());
                }
            return dom.getBaseURI();
        }

        private static String getBaseUri(FONIDocument dom, int/*NId*/ node,
                                         int nameid)
            throws URISyntaxException, DataModelException
        {
            for (int/*NId*/ pid = node; pid != 0; pid = dom.getParent(pid)) {
                int/*NId*/ attrId = dom.getAttribute(pid, nameid);
                if (attrId != 0) {
                    String frag = dom.pnGetStringValue(attrId);
                    // if absolute URI, that's it:
                    URI uri = FileUtil.uriConvert(frag);
                    if (uri != null && uri.isAbsolute())
                        return frag;
                    // otherwise resolve against baseURI of parent
                    return FileUtil.resolve(getBaseUri(dom,
                                                       dom.getParent(pid),
                                                       nameid), frag);
                }
            }
            return dom.getBaseURI();
        }
        
        public String[] getDTDInfo() throws DataModelException
        {
            return getDom().getDTDInfo(); // in IDocument only
        }      

        public int/*DId*/ docPosition()
        {
            Owner doc = dm.getDom().getOwner();
            if (doc != null) // part of a library: use the docIds
                return doc.hashCode() + getDom().getDocumentId();
            return dm.hashCode();
        }

        public boolean contains(Node node)
            throws DataModelException
        {
            if (!(node instanceof FONINode))
                return false; // or exception?
            int/*NId*/ delta = ((FONINode) node).id - id;
            return delta >= 0 && delta < getDom().getNodeSpan(id);
        }

        public int/*NId*/ getNodeSpan()
            throws DataModelException
        {
            return getDom().getNodeSpan(id);
        }

        /**
         * By convention, the root/document node has depth -1,
         * the root element has depth 0
         */
        public int getNodeDepth()
            throws DataModelException
        {
            int depth = ROOT_DEPTH;
            int/*NId*/ nId = getDom().getParent(id);
            for (; nId != 0; nId = getDom().getParent(nId))
                ++depth;
            return depth;
        }

        public Node[] getAttributes()
            throws DataModelException
        {
            int count = getAttributeCount();
            if (count == 0)
                return null;
            Node[] attrs = new Node[count];
            int/*NId*/ attr = getDom().getAttribute(id, -1);
            for (int a = 0; attr != 0; attr = getDom().pnGetNext(attr)) {
                attrs[a++] = new ANode(id, attr, dm);
            }
            return attrs;
        }

        public int getAttributeCount()
            throws DataModelException
        {
            return getDom().getAttrCount(id);
        }

        public boolean deepEquals(XQItem item, ComparisonContext context)
            throws EvaluationException
        {
            if (!(item instanceof BasicNode))
                return false;

            // System.out.println("TREE: "); dumpTree(System.out, id, 1);
            try {
                return deepEq(id, (BasicNode) item, context.getCollator());
            }
            catch (DataModelException e) {
                throw wrapDMException(e);
            }
        }

        public boolean deepEquals(Node item, Collator collator)
            throws DataModelException
        {
            return deepEq(id, (BasicNode) item, collator);
        }

        protected boolean deepEq(int/*NId*/ id, BasicNode that,
                                 Collator collator)
            throws DataModelException
        {
            int kind = getDom().getKind(id);
            // System.out.println(id+" deepEq? "+that);
            if (kind != that.getNodeNature()
                || getDom().getName(id) != that.getNodeName())
                return false;
            if (kind == Node.DOCUMENT)
                return contentEq(id, that, collator);
            else if (kind == Node.ELEMENT)
                return attributesEq(id, that, collator)
                       && contentEq(id, that, collator);
            else { // TODO OPTIM
                return Collations.compare(getDom().getStringValue(id),
                                          that.getStringValue(), collator) == 0;
            }
        }

        boolean attributesEq(int/*NId*/ id, Node that, Collator collator)
            throws DataModelException
        {
            FONIDocument dom = getDom();
            int/*NId*/ attrId = dom.getAttribute(id, -1);
            Node[] oattrs = that.getAttributes();

            if (attrId == 0)
                return (oattrs == null);
            if (oattrs == null)
                return false;

            for (int i = 0; i < oattrs.length; i++) {
                Node oattr = oattrs[i];
                int onameId = dom.internOtherName(oattr.getNodeName());
                int/*NId*/ attr = dom.getAttribute(id, onameId);
                if (attr == 0
                    || Collations.compare(dom.pnGetStringValue(attr),
                                          oattr.getStringValue(), collator) != 0)
                    return false;
            }
            return dom.getAttrCount(id) == oattrs.length;
        }

        boolean contentEq(int/*NId*/ id, BasicNode that, Collator collator)
            throws DataModelException
        {
            FONIDocument dom = getDom();
            int/*NId*/ kid = dom.getFirstChild(id);
            BasicNode okid = that.firstChild();
            for (; kid != 0; kid = dom.getNextSibling(kid), okid =
                (BasicNode) okid.getNextSibling()) {
                int kidKind = dom.getKind(kid), okidKind;
                if (kidKind == Node.COMMENT
                    || kidKind == Node.PROCESSING_INSTRUCTION)
                    continue;
                for (;;) {
                    if (okid == null)
                        return false;
                    if ((okidKind = okid.getNodeNature()) != Node.COMMENT
                        && okidKind != Node.PROCESSING_INSTRUCTION)
                        break;
                }
                // we have 2 comparable children:
                if (!deepEq(kid, okid, collator))
                    return false;
            }
            // just check that there are no more kids in 'that'
            return okid == null;
        }

        public int compareStringValues(Node node, Collator collator)
            throws DataModelException
        {
            String s1 = this.getStringValue(), s2 = node.getStringValue();
            return collator != null ? collator.compare(s1, s2)
                    : s1.compareTo(s2);
        }

        /**
         * Value comparison: equivalent to (untypedAtomic, string-value).
         * schema import
         */
        public int compareTo(XQItem that, ComparisonContext context, int flags)
            throws EvaluationException
        {
            // TODO: optimise by traversal comparison (stream on the
            // stringvalue)
            return UntypedAtomicType.comparison(this, that, context, flags);
        }

        public int documentOrderCompareTo(Node node)
        {
            if (node instanceof FONINode) {
                FONINode inode = (FONINode) node;
                if (inode.getDom().equals( this.getDom() )) { // FIX: NOT == !
                    
                    return id < inode.id ? -1 : id > inode.id ? 1 : 0;
                }
            }
            return Comparison.of(docPosition(), ((BasicNode) node).docPosition());
        }

        // ---- extensions for internal use:

        public Node getDocumentNode()
            throws DataModelException
        {
            return getDM().newNode(getDom().getRootNode());
        }

        public Node getAttribute(QName name)
            throws DataModelException
        {
            int nameId = getDom().internOtherName(name);
            if (nameId < 0)
                return null;
            int/*NId*/ aid = getDom().getAttribute(id, nameId);
            return aid == 0 ? null : new ANode(id, aid, dm);
        }

        public String getNamespacePrefix(String nsuri)
            throws DataModelException
        {
            return getNsPrefix(getDom(), id, nsuri);
        }

        public String getNamespaceUri(String prefix)
            throws DataModelException
        {
            return getNsUri(getDom(), id, prefix);
        }

        public boolean hasLocalNamespaces()
            throws DataModelException
        {
            return getDom().getFirstNSNode(id) != 0;
        }

        public int addNamespacesTo(NamespaceContext nsContext)
            throws DataModelException
        {
            return addNsTo(getDom(), id, nsContext);
        }

        public int addUsedNamespacesTo(NamespaceContext nsContext)
            throws DataModelException
        {
            return addUsedNsTo(getDom(), id, nsContext);
        }

        public void addInScopeNamespacesTo(NamespaceContext nsContext)
            throws DataModelException
        {
            addInscopeNsTo(getDom(), id, nsContext);
        }

        public void hideContextNamespacesIn(NamespaceContext nsContext)
            throws DataModelException
        {
            // irrelevant: no context NS here
        }

        // ---- extensions for XPath steps:

        public NodeSequenceBase getAncestors(NodeFilter nodeTest)
        {
            return new Ancestors(id, nodeTest, dm);
        }

        public NodeSequenceBase getAncestorsOrSelf(NodeFilter nodeTest)
        {
            return new AncestorsOrSelf(id, nodeTest, dm);
        }

        public NodeSequenceBase getParent(NodeFilter nodeTest)
        {
            return new Parent(id, nodeTest, dm);
        }

        public NodeSequenceBase getChildren(NodeFilter nodeTest)
            throws DataModelException
        {
            return new Children(id, nodeTest, dm);
        }

        public NodeSequenceBase getDescendants(NodeFilter nodeTest)
            throws DataModelException
        {
            return new Descendants(id, nodeTest, dm);
        }

        public NodeSequenceBase getDescendantsOrSelf(NodeFilter nodeTest)
            throws DataModelException
        {
            return new DescendantsOrSelf(id, nodeTest, dm);
        }

        public NodeSequenceBase getAttributes(NodeFilter nodeTest)
            throws DataModelException
        {
            return new Attributes(id, nodeTest, dm);
        }

        public NodeSequenceBase getFollowingSiblings(NodeFilter nodeTest)
        {
            return new FollowingSiblings(id, nodeTest, dm);
        }

        public NodeSequenceBase getPrecedingSiblings(NodeFilter nodeTest)
            throws DataModelException
        {
            return new PrecedingSiblings(id, nodeTest, dm);
        }

        public NodeSequenceBase getFollowing(NodeFilter nodeTest)
        {
            return new Following(id, nodeTest, dm);
        }

        public NodeSequenceBase getPreceding(NodeFilter nodeTest)
            throws DataModelException
        {
            return new Preceding(id, nodeTest, dm);
        }

        // --------------- Item ------------------------------

        public boolean isNode()
        {
            return true;
        }

        public Node getNode()
        {
            return this;
        }

        public XQItem getItem()
        {
            return this;
        }

        public String getString()
            throws EvaluationException
        {
            try {
                return getStringValue();
            }
            catch (DataModelException e) {
                throw wrapDMException(e);
            }
        }

        public boolean getBoolean()
            throws EvaluationException
        {
            return getString().length() != 0; // OPTIM
        }

        public long getInteger()
            throws EvaluationException
        {
            return Conversion.toInteger(getString());
        }

        public BigDecimal getDecimal()
            throws EvaluationException
        {
            return Conversion.toDecimal(getString(), true);
        }

        public float getFloat()
            throws EvaluationException
        {
            return Conversion.toFloat(getString());
        }

        public double getDouble()
            throws EvaluationException
        {
            return Conversion.toDouble(getString());
        }

        public Duration getDuration()
            throws EvaluationException
        {
            try {
                return Duration.parseDuration(getString());
            }
            catch (DateTimeException e) {
                throw new EvaluationException(e.getMessage());
            }
        }

        public DateTimeBase getMoment()
            throws EvaluationException
        { // Should not be called
            throw new EvaluationException("cannot convert node to date or time");
        }

        public BasicNode[] getIdMatchingNodes(String id, boolean idref)
            throws DataModelException
        {
            FONIDataModel dm = getDM();
            int/*NId*/ [] ids = getDom().getIdMatchingNodes(id, idref);
            if (ids == null)
                return null;
            int count = idref ? ids.length / 2 : ids.length;
            BasicNode[] nodes = new BasicNode[count];
            for (int i = 0; i < count; i++) {
                if (idref) {
                    int/*NId*/elemId = ids[2 * i];
                    nodes[i] = new ANode(elemId, 
                                         attrByRank(elemId, (int) ids[2 * i + 1]),
                                         dm);
                }
                else {
                    nodes[i] = dm.newNode(ids[i]);
                }
            }
            return nodes;
        }

        private int/*NId*/ attrByRank(int/*NId*/elemId, int rank)
            throws DataModelException
        {
            int/*NId*/ attrId = getDom().getAttribute(elemId, -1);
            for (; attrId != 0 && rank > 0; --rank)
                attrId = getDom().pnGetNext(attrId);
            return attrId;
        }

        public int/*NId*/ getNodeId()
        {
            return id;
        }

        public XMLPullStream exportNode()
        {
            return new NodePullStream(this);
            // TODO optimize (no Node objects)
        }

        public Object getObject()
            throws QizxException
        {
            return new PushStreamToDOM().exportNode(this);
        }

        public boolean equals(Object that)
        {
            if (!(that instanceof FONINode))
                return false;
            FONINode thatNode = (FONINode) that;
            if(thatNode.id != id || thatNode.getClass() != getClass())
                return false;
            // Attention the FONIDocument can change in Xlibs!
            FONIDocument d1 = thatNode.getDom();
            FONIDocument d2 = getDom();
            if(d1 == null || d2 == null) // just for safety
                return false;
           return d1.equals(d2);
        }

        public int hashCode()
        {
            return (int) id;
        }
    }


    // Attribute and namespace nodes:
    // the id is the owner node's, an additional offset points the value
    static class ANode extends FONINode
    {
        int/*NId*/ offset;

        ANode(int/*NId*/ id, int/*NId*/ offset, FONIDataModel dm)
        {
            super(id, dm);
            this.offset = offset;
        }

        public boolean equals(Object that)
        {
            if (!(that instanceof ANode))
                return false;
            ANode thatNode = (ANode) that;
            return thatNode.id == id && thatNode.offset == offset
                   && thatNode.getDom().equals( getDom() ); // FIX
        }

        public int hashCode()
        {
            return (int) (id * offset); // CAUTION id and offset are close to
            // each other
        }

        public int documentOrderCompareTo(Node node)
        {
            int cmp = super.documentOrderCompareTo(node);
            if (cmp != 0)
                return cmp;
            if (!(node instanceof ANode))
                return 1; // after owner
            // attributes of the same node:
            ANode anode = (ANode) node;
            int/*NId*/ diff = offset - anode.offset;
            return diff < 0 ? -1 : diff > 0 ? 1 : 0;
        }

        public int getNodeNature()
        {
            return Node.ATTRIBUTE;
        }

        public String getNodeKind()
        {
            return "attribute";
        }

        public ItemType getType()
        {
            return XQType.ATTRIBUTE;
        }

        public QName getNodeName()
            throws DataModelException
        {
            QName nm = getDom().getOtherName(getDom().pnGetNameId(offset));
            return nm;
        }

        public String getStringValue()
            throws DataModelException
        {
            return getDom().pnGetStringValue(offset);
        }

        public char[] getCharValue()
            throws DataModelException
        {
            return getDom().pnGetCharValue(offset, 0);
        }

        public double getDoubleByRules()
        {
            if(dm.dataConversion != null) {
                return dm.dataConversion.convertNumber(this);
            }
            return Double.NaN;  
        }

        public Node getParent()
        {
            return getDM().newNode(id);
        }

        public NodeSequenceBase getParent(NodeFilter nodeTest)
        {
            // tricky: return a single node sequence of the owner node
            return new SingleNode(id, nodeTest, dm);
        }

        public BasicNode firstChild()
            throws DataModelException
        {
            return null;
        }

        public NodeSequenceBase getChildren()
        {
            return NodeSequenceBase.noNodes;
        }

        public NodeSequenceBase getDescendants(NodeFilter nodeTest)
        {
            return empty;
        }

        public NodeSequenceBase getDescendantsOrSelf(NodeFilter nodeTest)
        {
            if (nodeTest == null || nodeTest.accepts(this))
                return new Parent(id, null, dm);
            return NodeSequenceBase.noNodes;
        }

        public Node[] getAttributes()
        {
            return null;
        }
    }


    // namespace Node:
    static class NSNode extends ANode
    {
        NSNode(int/*NId*/ id, int/*NId*/ offset, FONIDataModel dm)
        {
            super(id, offset, dm);
        }

        public String getNodeKind()
        {
            return "namespace";
        }

        public int getNodeNature()
        {
            return Node.NAMESPACE;
        }

        public ItemType getType()
        {
            return XQType.NAMESPACE;
        }
    }


    // Sequence on INodes:
    static abstract class ISequence extends NodeSequenceBase
    {
        protected FONIDataModel dm;

        int/*NId*/ startId, curId;

        ISequence(int/*NId*/ startId, FONIDataModel dm)
        {
            this.dm = dm;
            curId = this.startId = startId;
            itemType = XQType.NODE;
        }

//        public XQValue bornAgain()
//        {
//            return new ISequence(startId, dm);
//        }

//        public boolean next()
//            throws EvaluationException
//        {
//            if (curId < 0)
//                curId = -curId; // trick for children iterator
//            else
//                try {
//                    curId = getDom().getNextSibling(curId);
//                }
//                catch (DataModelException e) {
//                    BasicNode.wrapDMException(e);
//                }
//            return curId != 0;
//        }

        public int/*NId*/ currentId()
        {
            return curId;
        }

        public BasicNode basicNode()
        {
            return curId == 0 ? null : ((FONIDataModel) dm).newNode(curId);
        }

        public ItemType getType()
            throws EvaluationException
        {
            try {
                return NodeType.getTypeByKind(getDom().getKind(curId));
            }
            catch (DataModelException e) {
                throw BasicNode.wrapDMException(e);
            }
        }

        public final FONIDocument getDom()
        {
            return dm.dom;
        }

        public double getFulltextScore(Item item)
            throws EvaluationException
        {
            return -1;
        }
    }


    abstract static class TypedSequence extends ISequence
    {
        NodeFilter nodeTest;
        boolean started = false;

        TypedSequence(int/*NId*/ id, NodeFilter nodeTest, FONIDataModel dm)
        {
            super(id, dm);
            this.nodeTest = nodeTest;
        }

        boolean checkNode()
        {
            if (curId == 0)
                return false;
            try {
                return (nodeTest == null || (nodeTest.needsNode()
                        ? nodeTest.accepts(getNode())
                        : nodeTest.accepts(getDom().getKind(curId),
                                           getDom().getName(curId))));
            }
            catch (DataModelException e) {
                return false;
            }
        }

        public NodeSequenceBase restart(int/*NId*/ id, FONIDataModel dm,
                                        NodeFilter nodeTest)
        {
            this.started = false;
            this.dm = dm;
            this.startId = this.curId = id;
            this.nodeTest = nodeTest;
            return this;
        }
    }


    static class Parent extends TypedSequence
    {
        Parent(int/*NId*/ id, NodeFilter nodeTest, FONIDataModel dm)
        {
            super(id, nodeTest, dm);
        }

        public boolean next()
            throws EvaluationException
        {
            if (started)
                return false;
            started = true;
            try {
                curId = getDom().getParent(curId);
            }
            catch (DataModelException e) {
                BasicNode.wrapDMException(e);
            }
            return checkNode();
        }

        public XQValue bornAgain()
        {
            return new Parent(startId, nodeTest, dm);
        }
    }


    static class SingleNode extends TypedSequence
    {
        SingleNode(int/*NId*/ id, NodeFilter nodeTest, FONIDataModel dm)
        {
            super(id, nodeTest, dm);
        }

        public boolean next()
            throws EvaluationException
        {
            if (started)
                return false;
            started = true;
            return checkNode();
        }

        public XQValue bornAgain()
        {
            return new SingleNode(startId, nodeTest, dm);
        }
    }


    static class AncestorsOrSelf extends TypedSequence
    {
        AncestorsOrSelf(int/*NId*/ id, NodeFilter nodeTest, FONIDataModel dm)
        {
            super(id, nodeTest, dm);
        }

        public boolean next()
            throws EvaluationException
        {
            for (; curId != 0;) {
                if (started)
                    try {
                        curId = getDom().getParent(curId);
                    }
                    catch (DataModelException e) {
                        BasicNode.wrapDMException(e);
                    }
                started = true;
                if (curId != 0 && checkNode())
                    return true;
            }
            return false;
        }

        public XQValue bornAgain()
        {
            return new AncestorsOrSelf(startId, nodeTest, dm);
        }
    }


    static class Ancestors extends AncestorsOrSelf
    {
        Ancestors(int/*NId*/ id, NodeFilter nodeTest, FONIDataModel dm)
        {
            super(id, nodeTest, dm);
            started = true;
        }

        public XQValue bornAgain()
        {
            return new Ancestors(startId, nodeTest, dm);
        }
    }


    static class Children extends TypedSequence
    {
        Children(int/*NId*/ id, NodeFilter nodeTest, FONIDataModel dm)
            throws DataModelException
        {
            super(id, null, dm);
            this.nodeTest = nodeTest;
            curId = getDom().getFirstChild(id);
        }

        public XQValue bornAgain()
        {
            try {
                return new Children(startId, nodeTest, dm);
            }
            catch (DataModelException e) {
                return XQValue.empty; // should not happen ?
            }
        }

        public boolean next()
            throws EvaluationException
        {
            for (; curId != 0;) {
                if (started)
                    try {
                        curId = getDom().getNextSibling(curId);
                    }
                    catch (DataModelException e) {
                        BasicNode.wrapDMException(e);
                    }
                started = true;
                if (curId != 0 && checkNode())
                    return true;
            }
            return false;
        }
    }


    static class DescendantsOrSelf extends TypedSequence
    {
        int/*NId*/ lastNode;

        DescendantsOrSelf(int/*NId*/ id, NodeFilter nodeTest, FONIDataModel dm)
            throws DataModelException
        {
            super(id, nodeTest, dm);
            lastNode = getDom().getNodeAfter(Math.abs(id));
            if (lastNode == 0)
                lastNode = PostingIterator.MAX_NODEID;
        }

        DescendantsOrSelf(int/*NId*/ startId, NodeFilter nodeTest, FONIDataModel dm,
                          int/*NId*/ lastNode)
        {
            super(startId, nodeTest, dm);
            this.lastNode = lastNode;
        }

        public XQValue bornAgain()
        {
            return new DescendantsOrSelf(startId, nodeTest, dm, lastNode);
        }

        public boolean next()
            throws EvaluationException
        {
            for (; curId != 0;) {
                if (started)
                    try {
                        curId = getDom().getNodeNext(curId);
                    }
                    catch (DataModelException e) {
                        BasicNode.wrapDMException(e);
                    }
                started = true;
                if (curId >= lastNode)
                    curId = 0;
                else if (curId != 0 && checkNode())
                    return true;
            }
            return false;
        }
    }


    static class Descendants extends DescendantsOrSelf
    {
        Descendants(int/*NId*/ id, NodeFilter nodeTest, FONIDataModel dm)
            throws DataModelException
        {
            super(id, nodeTest, dm);
            started = true;
        }

        public XQValue bornAgain()
        {
            try {
                return new Descendants(startId, nodeTest, dm);
            }
            catch (DataModelException e) {
                return XQValue.empty;
            }
        }
    }


    static class FollowingSiblings extends TypedSequence
    {
        FollowingSiblings(int/*NId*/ id, NodeFilter nodeTest, FONIDataModel dm)
        {
            super(id, nodeTest, dm);
        }

        public XQValue bornAgain()
        {
            return new FollowingSiblings(startId, nodeTest, dm);
        }

        public boolean next()
            throws EvaluationException
        {
            for (; curId != 0;) {
                try {
                    curId = getDom().getNextSibling(curId);
                }
                catch (DataModelException e) {
                    BasicNode.wrapDMException(e);
                }
                if (curId != 0 && checkNode())
                    return true;
            }
            return false;
        }
    }


    static class Following extends TypedSequence
    {
        Following(int/*NId*/ id, NodeFilter nodeTest, FONIDataModel dm)
        {
            super(id, nodeTest, dm);
            // FIX: dont take descendants! (very old bug)
            try {
                curId = getDom().getNodeAfter(id);
            }
            catch (DataModelException ignored) { ; }
        }

        public XQValue bornAgain()
        {
            return new Following(startId, nodeTest, dm);
        }

        public boolean next()
            throws EvaluationException
        {
            for (; curId != 0;) {
                try {
                    if(started)
                        curId = getDom().getNodeNext(curId);
                    started = true;
                }
                catch (DataModelException e) {
                    BasicNode.wrapDMException(e);
                }
                if (curId != 0 && checkNode())
                    return true;
            }
            return false;
        }
    }


    static class PrecedingSiblings extends TypedSequence
    {
        PrecedingSiblings(int/*NId*/ id, NodeFilter nodeTest, FONIDataModel dm)
            throws DataModelException
        {
            super(id, nodeTest, dm);
            curId = getDom().getFirstChild(getDom().getParent(id));
        }

        public XQValue bornAgain()
        {
            try {
                return new PrecedingSiblings(startId, nodeTest, dm);
            }
            catch (DataModelException e) {
                return XQValue.empty;
            }
        }

        public boolean next()
            throws EvaluationException
        {
            for (; curId != 0;) {
                if (started)
                    try {
                        curId = getDom().getNextSibling(curId);
                    }
                    catch (DataModelException e) {
                        BasicNode.wrapDMException(e);
                    }
                started = true;
                if (curId == startId) {
                    curId = 0;
                    return false;
                }
                if (checkNode())
                    return true;
            }
            return false;
        }
    }


    static class Preceding extends PrecedingSiblings
    {
        Preceding(int/*NId*/ id, NodeFilter nodeTest, FONIDataModel dm)
            throws DataModelException
        {
            super(id, nodeTest, dm);
            curId = getDom().getRootNode();
        }

        public XQValue bornAgain()
        {
            try {
                return new Preceding(startId, nodeTest, dm);
            }
            catch (DataModelException e) {
                return XQValue.empty;
            }
        }

        public boolean next()
            throws EvaluationException
        {
            try {
                for (; curId != 0;) {
                    if (started)
                        curId = getDom().getNodeNext(curId);
                    started = true;
                    if (curId == startId) {
                        curId = 0;
                        return false;
                    }
                    // FIX: no ancestors
                    if(!isAncestor(curId) && checkNode())
                        return true;
                }
            }
            catch (DataModelException e) {
                BasicNode.wrapDMException(e);
            }
            return false;
        }

        // ancestor of start node?
        private boolean isAncestor(int/*NId*/ node) throws DataModelException
        {
            int/*NId*/ delta = startId - node;
            return delta >= 0 && delta < getDom().getNodeSpan(node);
        }
    }


    static class ASequence extends NodeSequenceBase
    {
        FONIDataModel dm;
        int/*NId*/ ownerId;
        int/*NId*/ firstId;
        int/*NId*/ curId;

        ASequence(int/*NId*/ ownerId, int/*NId*/ firstId, FONIDataModel dm)
        {
            this.ownerId = ownerId;
            this.firstId = firstId;
            this.curId = -firstId;
            this.dm = dm;
        }

        public boolean next()
            throws EvaluationException
        {
            if (curId <= 0)
                curId = -curId;
            else
                try {
                    curId = dm.dom.pnGetNext(curId);
                }
                catch (DataModelException e) {
                    BasicNode.wrapDMException(e);
                }
            if (curId != 0)
                return true;
            return false;
        }

        // public Node getNode()
        // {
        // return curId == 0 ? null : (Node) makeNode(curId);
        // }

        public BasicNode basicNode()
        {
            return curId == 0 ? null : makeNode(curId);
        }

        public XQItemType getItemType()
        {
            return XQType.ATTRIBUTE;
        }

        ANode makeNode(int/*NId*/ id)
        {
            return new ANode(ownerId, id, dm);
        }

        public XQValue bornAgain()
        {
            return new ASequence(ownerId, firstId, dm);
        }

        public final FONIDocument getDom()
        {
            return dm.dom;
        }
    }


    static class Attributes extends ASequence
    {
        NodeFilter nodeTest;

        Attributes(int/*NId*/ ownerId, NodeFilter nodeTest, FONIDataModel dm)
            throws DataModelException
        {
            super(ownerId, 0, dm);
            this.firstId = dm.dom.getAttribute(ownerId, -1);

            this.curId = -firstId;
            this.nodeTest = nodeTest;
        }

        boolean checkNode()
            throws EvaluationException
        {
            if (curId == 0)
                return false;
            try {
                return (nodeTest == null || (nodeTest.needsNode()
                        ? nodeTest.accepts(makeNode(curId))
                        : nodeTest.accepts(Node.ATTRIBUTE,
                                           getDom().pnGetName(curId))));
            }
            catch (DataModelException e) {
                throw BasicNode.wrapDMException(e);
            }
        }

        public XQValue bornAgain()
        {
            try {
                return new Attributes(ownerId, nodeTest, dm);
            }
            catch (DataModelException e) {
                return XQValue.empty;
            }
        }

        public boolean next()
            throws EvaluationException
        {
            for (; super.next();)
                if (checkNode())
                    return true;
            return false;
        }
    }

    // -------------- Utilities ------------------------

    public static String getNsPrefix(FONIDocument doc, int/*NId*/ nodeId, 
                                     String nsuri)
        throws DataModelException
    {
        for (; nodeId != 0; nodeId = doc.getParent(nodeId)) {
            int/*NId*/ ns = doc.getFirstNSNode(nodeId);
            for (; ns != 0; ns = doc.pnGetNext(ns))
                if (nsuri.equals(doc.pnGetStringValue(ns)))
                    return doc.pnGetName(ns).getLocalPart();
        }
        return null;
    }

    public static String getNsUri(FONIDocument doc, int/*NId*/ nodeId, 
                                  String prefix)
        throws DataModelException
    {
        for (; nodeId != 0; nodeId = doc.getParent(nodeId)) {
            int/*NId*/ ns = doc.getFirstNSNode(nodeId);
            for (; ns != 0; ns = doc.pnGetNext(ns))
                if (prefix.equals(doc.pnGetName(ns).getLocalPart()))
                    return doc.pnGetStringValue(ns);
        }
        return null;
    }

    public static int addNsTo(FONIDocument doc, int/*NId*/ nodeId,
                              NamespaceContext nsContext)
        throws DataModelException
    {
        int count = 0;
        int/*NId*/ ns = doc.getFirstNSNode(nodeId);
        for (; ns != 0; ns = doc.pnGetNext(ns)) {
            nsContext.addMapping(doc.pnGetName(ns).getLocalPart(),
                                 doc.pnGetStringValue(ns));
            ++count;
        }
        return count;
    }

    public static int addUsedNsTo(FONIDocument doc, int/*NId*/ nodeId,
                                  NamespaceContext nsContext)
        throws DataModelException
    {
        int count = 0;
        IQName name = doc.getName(nodeId);
        String ns = name.getNamespaceURI();
        if (ns != "") {
            nsContext.addMapping(getNsPrefix(doc, nodeId, ns), ns);
            ++count;
        }
        int/*NId*/ attr = doc.getAttribute(nodeId, -1);
        for (; attr != 0; attr = doc.pnGetNext(attr)) {
            name = doc.pnGetName(attr);
            ns = name.getNamespaceURI();
            if (ns != "") {
                nsContext.addMapping(getNsPrefix(doc, nodeId, ns), ns);
                ++count;
            }
        }
        return count;
    }

    // Use recursion to add NS of ancestors before NS of current node
    public static void addInscopeNsTo(FONIDocument document, int/*NId*/ id,
                                      NamespaceContext nsContext)
        throws DataModelException
    {
        int/*NId*/ parent = document.getParent(id);
        if (parent != 0)
            addInscopeNsTo(document, parent, nsContext);
        addNsTo(document, id, nsContext);
    }
}
