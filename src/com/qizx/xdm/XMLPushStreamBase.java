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
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.api.XMLPushStream;
import com.qizx.util.NamespaceContext;
import com.qizx.util.basic.Check;
import com.qizx.util.basic.XMLUtil;
import com.qizx.xdm.FONIDataModel.FONINode;

/**
 * A Base implementation of XMLPushStream.
 */
public abstract class XMLPushStreamBase extends AttributeList
    implements XMLPushStream
{
    protected final static String VOLUME_LIMIT = "volume limit reached";
    protected final static int NSCOPY_SIMPLY = 0;
    
    protected String baseURI;
    protected String dtdName = null;
    protected String dtdPublicId = null;
    protected String dtdSystemId = null;
    protected String dtdInternalSubset;
    
    protected IQName  elementName;   // current
    protected NamespaceContext contextualMappings;
    protected NamespaceContext localNS;  // to check NS clashes
    
    protected boolean elementStarted, spaceNeeded, docStarted;
    protected boolean startDocumentDone, endDocumentDone;
    protected boolean trace = false;
    protected long maxVolume = -1, volume = 0;
    private boolean noLocalPutNS;

    // ------------------------------------------------------------------------
    
    public void putDTD(String name, String publicId, String systemId,
                       String internalSubset)
        throws DataModelException
    {
        if(name != null)
            dtdName = name;
        if(publicId != null)
            dtdPublicId = publicId;
        if(systemId != null)
            dtdSystemId = systemId;
        if(internalSubset != null)
            this.dtdInternalSubset = internalSubset;
    }

    public boolean putDocumentStart()
        throws DataModelException
    {
        if (attrNames == null)
            reset(); // for absent-minded people (eg me)
        spaceNeeded = false;
        if (startDocumentDone || elementStarted)
            return false;
        startDocumentDone = true;
        return true;
    }

    public void putDocumentEnd()
        throws DataModelException
    {
        if(endDocumentDone)
            return;
        endDocumentDone = true;
        spaceNeeded = false;
        flush();
    }

    public void putElementStart(QName name)
        throws DataModelException
    {
        if (!startDocumentDone) {
            putDocumentStart();
            startDocumentDone = true;
        }
        if (trace)
            System.err.println("--- start elem " + name);

        docStarted = true;
        if (elementStarted)
            flushElementStart(false);

        attrCnt = 0;
        if(name == null)
            System.err.println("XMLPushStreamBase.putElementStart : OOPS"); //rub
        elementName = IQName.get(name);
        elementStarted = true;
        spaceNeeded = false;
        // marks the namespace stack
        nsContext.newLevel();
        if(localNS != null) {
            localNS.clear();
            if(name.getPrefix() != null)
                localNS.addMapping(name.getPrefix(), name.getNamespaceURI());
        }
    }

    public void putElementEnd(QName name)
        throws DataModelException
    {
        if (trace)
            System.err.println("--- end elem " + name);
        // if(elementStarted)
        // flushElement(true);
        nsContext.popLevel();
        spaceNeeded = false;
    }

    //
    public boolean putNamespace(String prefix, String namespaceURI)
        throws DataModelException
    {
        Check.nonNull(prefix, "prefix");
        Check.nonNull(namespaceURI, "namespaceURI");
        
        if (trace)
            System.err.println("--- namespace " + prefix + " = "
                               + namespaceURI);
        boolean needed = false;
        if(!noLocalPutNS) {
            String curi = nsContext.getNamespaceURI(prefix);
            // Add it only if necessary (ignore empty prefix and uri if undefined)
            // return true if actually pushed
            if(curi == null) {
                if(prefix.length() != 0 || namespaceURI.length() != 0)
                    needed = true;
            }
            else if(!namespaceURI.equals(nsContext.getNamespaceURI(prefix)))
                needed = true;
            if(needed)
                nsContext.addMapping(prefix, namespaceURI);
        }
        
        if(localNS != null) {
            checkNSClash(prefix, namespaceURI);
        }
        
        if (!elementStarted) { // stray namespace
            putText("xmlns:" + prefix + " = " + namespaceURI);
        }
        return needed;
    }

    public void noSpace()
    {
        spaceNeeded = false;
    }
    
    public void putChars(char[] text, int start, int textLength)
        throws DataModelException
    {
        // default implementation
        putText(new String(text, start, textLength));
    }

    /**
     * Puts the value of an atomic item. Differs from putText by a space added
     * automatically if needed.
     * @param value
     * @throws DataModelException
     */
    public void putAtomText(String value)
        throws DataModelException
    {
        if (spaceNeeded)
            putText(" ");
        putText(value);
        spaceNeeded = true;
    }

    public void putLongAtom(long value) throws DataModelException
    {
        throw new DataModelException("unimplemented operation");
    }
    
    public void putAtom(Object value) throws DataModelException
    {
        throw new DataModelException("unimplemented operation");
    }

    public void putAttribute(QName name, String value, String type)
        throws DataModelException
    {
        Check.nonNull(name, "name");
        Check.nonNull(value, "value");
        if (trace)
            System.err.println("--- attribute " + name + " = " + value);
        if (name == IQName.XML_ID)
            value = XMLUtil.collapseWhiteSpace(value);
        if (!elementStarted) { // stray attribute
            putText(name + " = " + value);
            return;
        }
        if(localNS != null && name.getPrefix() != null) {
            checkNSClash(name.getPrefix(), name.getNamespaceURI());
        }
        if (!addAttribute(name, value, type))
            throw new DataModelException("Duplicate attribute " + name);
    }

    public void putProcessingInstruction(String target, String value)
        throws DataModelException
    {
        checkPIValue(value);
        if (elementStarted)
            flushElementStart(false);
    }

    public void putComment(String value)
        throws DataModelException
    {
        checkCommentValue(value);
        if (elementStarted)
            flushElementStart(false);
    }

    /**
     * Implementation of processing of "end of start tag", triggered by 
     * element content or end of element.
     * <p>Output is normally delayed until this method is called. 
     * @param isEmpty true if element has no contents (triggered by elementEnd)
     */
    protected abstract void flushElementStart(boolean isEmpty)
        throws DataModelException;

    public void setCheckNS(boolean value)
    {
        localNS = value? new NamespaceContext() : null;
    }

    private void checkNSClash(String prefix, String nsURI)
        throws DataModelException
    {
        String prevUri = localNS.getNamespaceURI(prefix);
        if(prevUri == null) {
            localNS.addMapping(prefix, nsURI);
        }
        else if(!prevUri.equals(nsURI))
            throw new DataModelException("XUDY0024",
                                         "incompatible NS prefix " + prefix);
    }

    public void setTrace(boolean value)
    {
        trace = value;
    }

    public void setMaxVolume(int volume)
    {
        maxVolume = volume;
    }

    public boolean maxVolumeReached()
    {
        return volume >= maxVolume;
    }

    /**
     * Resets the state before a new use.
     * <p>Should always be called before using the object again.
     */
    public void reset()
    {
        elementStarted = docStarted = false;
        startDocumentDone = endDocumentDone = false;
        spaceNeeded = false;
        attrCnt = 0;
        nsContext = new NamespaceContext();
        attrNames = new IQName[8];
        attrValues = new String[attrNames.length];
        attrTypes = new String[attrNames.length];
    }

    public boolean isAtRoot()
    {
        return !startDocumentDone;
    }

    public void flush()
        throws DataModelException
    {
        if (!endDocumentDone)
            putDocumentEnd();
    }

    public void abort()
        throws DataModelException
    {
    }

    public void defineContextMappings(NamespaceContext mappings)
    {
        contextualMappings = mappings;
    }

    // On flush of element start, check that all used NS are mapped
    protected void completeNameMappings() throws DataModelException
    {
        // need to add mappings for NS that are borrowed from context:
        checkNameMapping(elementName, true);
        for (int a = 0; a < attrCnt; a++)
            checkNameMapping(attrNames[a], false);
    }

    // Checks that the NS used by this name has a mapping
    // If not, look for a mapping in context, if it fails create one
    protected void checkNameMapping(IQName name, boolean isElemName)
        throws DataModelException
    {
        String ns = name.getNamespaceURI();
        String prefix = nsContext.getPrefix(ns);
        if (prefix != null || // OK for == : comes from IQName
              ns == NamespaceContext.EMPTY || ns == NamespaceContext.XML)
            return; // known
        // is there a prefix in the static context?
        if(contextualMappings != null)
            prefix = contextualMappings.getPrefix(ns);
        if(prefix == null) {
            // if element, try to use default ns:
            prefix = "";
            if (!isElemName || nsContext.getNamespaceURI(prefix) != null) {
                // nope, generate a brand new prefix:
                for (int id = 1;; id++) {
                    prefix = "ns" + id;
                    if (nsContext.getNamespaceURI(prefix) == null)
                        break;
                }
            }
        }
        nsContext.addMapping(prefix, ns);
        putNamespace(prefix, ns);
    }

    public String getNSPrefix(String namespaceURI)
    {
        return nsContext.getPrefix(namespaceURI);
    }

    public String getNSURI(String prefix)
    {
        String ns = nsContext.getNamespaceURI(prefix);
        return ns;
    }

    protected void checkPIValue(String value)
        throws DataModelException
    {
        if (XMLUtil.normalizePI(value) == null)
            throw new DataModelException("invalid PI contents");
    }

    protected void checkCommentValue(String value)
        throws DataModelException
    {
        if (!XMLUtil.checkComment(value))
            throw new DataModelException("invalid comment");
    }

    /**
     * Traverse and generate a subtree.
     */
    public void putNodeCopy(Node node, int copyNsMode)
        throws DataModelException
    {
        if(node instanceof FONIDataModel.FONINode &&
           !(node instanceof FONIDataModel.ANode)) {
            // optimization: avoid creating many node objects
            FONIDataModel.FONINode fnode = (FONINode) node;
            putNodeCopy(fnode.getNodeId(), fnode.getDom(), copyNsMode);
        }
        else if(node != null)
            recCopyNode(copyNsMode, node);
    }
    
    public void putNamespaces(Node element, int copyNsMode)
        throws DataModelException
    {
        // copy namespaces:
        BasicNode bnode = (BasicNode) element;
        switch (copyNsMode) {
        default:    // just copy
            bnode.addNamespacesTo(nsContext);
            break;
        case NSCOPY_PRESERVE_INHERIT:
            // Add In-scope NS FIX: not only defined on the element 
            bnode.addInScopeNamespacesTo(nsContext);
             //bnode.addNamespacesTo(nsContext);
            break;
        case NSCOPY_NOPRESERVE_INHERIT:
            // add only NS used on the element itself
            bnode.addUsedNamespacesTo(nsContext);
            break;
        case NSCOPY_PRESERVE_NOINHERIT:
            nsContext.hideAllNamespaces();  // of current constructor
            bnode.addInScopeNamespacesTo(nsContext);
            ///bnode.addNamespacesTo(nsContext);
            break;
        case NSCOPY_NOPRESERVE_NOINHERIT:
            nsContext.hideAllNamespaces();  // of current constructor
            bnode.addUsedNamespacesTo(nsContext);
            break;
        }

        // Generate NS: block putNamespace() in this class 
        // in case it is called by subclass, to avoid redefining NS...
        noLocalPutNS = true;
        int localNSCount = nsContext.getLocalSize();
        for(int p = 0; p < localNSCount; p++) {
            String uri = nsContext.getLocalNamespaceURI(p);
            if(uri == null)
                uri = "";
            putNamespace(nsContext.getLocalPrefix(p), uri);
        }
        noLocalPutNS = false;
    }
    
    private void recCopyNode(int copyNsMode, Node node)
        throws DataModelException
    {
        switch (node.getNodeNature()) {
        case Node.DOCUMENT: {
            boolean ok = putDocumentStart();
            for (Node kid = node.getFirstChild(); 
                    kid != null; kid = kid.getNextSibling())
                recCopyNode(copyNsMode, kid);
            if (ok)
                putDocumentEnd();
            break;
        }
        case Node.ELEMENT: {
            putElementStart(node.getNodeName());

            putNamespaces(node, copyNsMode);
            
            // Attributes:
            Node[] attr = node.getAttributes();    // ? OPTIM
            if(attr != null)
                for (int a = 0; a < attr.length; a++) {
                    putAttribute(attr[a].getNodeName(),
                                 attr[a].getStringValue(), null);
                }
            
            Node kid = node.getFirstChild();
            for (; kid != null; kid = kid.getNextSibling())
                recCopyNode(NSCOPY_SIMPLY, kid);    // nscopy mode only on root

            putElementEnd(node.getNodeName());
            break;
        }
        case Node.TEXT:
            putText(node.getStringValue());
            break;

        case Node.PROCESSING_INSTRUCTION:
            putProcessingInstruction(node.getNodeName().toString(),
                                     node.getStringValue());
            break;

        case Node.COMMENT:
            putComment(node.getStringValue());
            break;

        case Node.ATTRIBUTE:
            // -text( node.getNodeName().toString() );
            // -text("= "); text(node.getStringValue());
            putAttribute(node.getNodeName(), node.getStringValue(), null);
            break;

        case Node.NAMESPACE:
            // -text("xmlns:"); text( node.getNodeName().getLocalPart() );
            // -text("= "); text(node.getStringValue());
            putNamespace(node.getNodeName().getLocalPart(),
                         node.getStringValue());
            break;

        default:
            throw new DataModelException("illegal node kind "
                                    + node.getNodeNature() + " " + node);
        }
    }

    // Optimized version for FONI documents (no node creation)
    //
    public void putNodeCopy(int/*NId*/ nodeId, FONIDocument dm, int copyNsMode)
        throws DataModelException
    {
        QName name;
        int/*NId*/ kid;
        switch (dm.getKind(nodeId)) {
        case Node.DOCUMENT:
            boolean ok = putDocumentStart();
            kid = dm.getFirstChild(nodeId);
            for (; kid != 0; kid = dm.getNextSibling(kid))
                putNodeCopy(kid, dm, copyNsMode);
            if (ok)
                putDocumentEnd();
            break;

        case Node.ELEMENT:
            putElementStart(name = dm.getName(nodeId));

            // copy namespaces:
            switch (copyNsMode) {
            default:    // just copy NS defined on the element
                FONIDataModel.addNsTo(dm, nodeId, nsContext);
                break;                
            case NSCOPY_PRESERVE_INHERIT:
                // Add NS in scope
                FONIDataModel.addInscopeNsTo(dm, nodeId, nsContext);
                //FONIDataModel.addNsTo(dm, nodeId, nsContext);
                break;
            case NSCOPY_PRESERVE_NOINHERIT:
                nsContext.hideAllNamespaces();
                FONIDataModel.addInscopeNsTo(dm, nodeId, nsContext);
                break;
            case NSCOPY_NOPRESERVE_INHERIT:
                // add only NS used on the element itself
                FONIDataModel.addUsedNsTo(dm, nodeId, nsContext);
                break;
            case NSCOPY_NOPRESERVE_NOINHERIT:
                nsContext.hideAllNamespaces();
                FONIDataModel.addUsedNsTo(dm, nodeId, nsContext);
                break;
            }

            // Generate NS: block putNamespace() in this class 
            // in case it is called by subclass, to avoid redefining NS...
            noLocalPutNS = true;
            int localNSCount = nsContext.getLocalSize();
            for(int p = 0; p < localNSCount; p++) {
                String uri = nsContext.getLocalNamespaceURI(p);
                if(uri == null)
                    uri = "";
                putNamespace(nsContext.getLocalPrefix(p), uri);
            }
            noLocalPutNS = false;

            int/*NId*/ attr = dm.getAttribute(nodeId, -1);
            for (; attr != 0; attr = dm.pnGetNext(attr)) {
                putAttribute(dm.pnGetName(attr),
                             dm.pnGetStringValue(attr), null);
            }

            kid = dm.getFirstChild(nodeId);
            int/*NId*/ next;
            for (; kid != 0; kid = next) {
                next = dm.getNextSibling(kid);
                putNodeCopy(kid, dm, NSCOPY_SIMPLY);
            }
            putElementEnd(name);
            break;

        case Node.TEXT:
            putText(dm.getStringValue(nodeId));
            break;

        case Node.PROCESSING_INSTRUCTION:
            putProcessingInstruction(dm.getName(nodeId).getLocalPart(),
                                     dm.getStringValue(nodeId));
            break;

        case Node.COMMENT:
            putComment(dm.getStringValue(nodeId));
            break;

        case Node.ATTRIBUTE:
            putAttribute(dm.pnGetName(nodeId), 
                         dm.pnGetStringValue(nodeId), null);
            break;

        case Node.NAMESPACE:
            // -text("xmlns:"); text( node.getNodeName().getLocalPart() );
            // -text("= "); text(node.getStringValue());
            putNamespace(dm.pnGetName(nodeId).getLocalPart(), dm
                .pnGetStringValue(nodeId));
            break;
            
        case Node.ATOM_BOOLEAN:
        case Node.ATOM_LONG:
        case Node.ATOM_DOUBLE:
        case Node.ATOM_DATE:
        case Node.ATOM_ANY:
            putAtom(dm.getValue(nodeId));
            break;

        default:
            throw new DataModelException("illegal node kind "
                                          + dm.getKind(nodeId));
        }
    }

    /**
     * Outputs the text contents (string-value) of the node.
     * The depth can be limited.
     */
    public void putNodeText(BasicNode node, int maxDepth)
        throws DataModelException
    {
        if(node instanceof FONIDataModel.FONINode &&
                !(node instanceof FONIDataModel.ANode)) {
            // optimization: avoid creating many node objects
            FONIDataModel.FONINode fnode = (FONINode) node;
            recPutNodeText(fnode.getNodeId(), fnode.getDom(), maxDepth);
        }
        else
            recPutNodeText(node, maxDepth);
    }

    private void recPutNodeText(Node node, int maxDepth)
        throws DataModelException
    {
        if(--maxDepth < 0)
            return;
        switch (node.getNodeNature()) {
        case Node.DOCUMENT:
        case Node.ELEMENT:
            Node kid = node.getFirstChild();
            for (; kid != null; kid = kid.getNextSibling())
                recPutNodeText(kid, maxDepth);
            break;

        case Node.TEXT:
            putText(node.getStringValue());
            break;
        }       
    }

    private void recPutNodeText(int/*NId*/ nodeId, FONIDocument dm, int maxDepth)
        throws DataModelException
    {
        
        if(--maxDepth < 0)
            return;
        int/*NId*/ kid;
        switch (dm.getKind(nodeId)) {
        case Node.DOCUMENT:
        case Node.ELEMENT:
            kid = dm.getFirstChild(nodeId);
            int/*NId*/ next;
            for (; kid != 0; kid = next) {
                next = dm.getNextSibling(kid);
                recPutNodeText(kid, dm, maxDepth);
            }
            break;

        case Node.TEXT:
            putText(dm.getStringValue(nodeId));
            break;
        }       
    }

}
