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

import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.util.QNameTable;
import com.qizx.util.basic.XMLUtil;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A fast and memory efficient FONIDocument implementation for parsed documents.
 *  After construction it is readonly and thread safe.
 *  <p>The document is constructed through SAX2. It implements directly the SAX2
 *  handler interfaces.
 *  <p>
 *  Implementation: nodes are stored in blocks of int.
 *  <ul>
 * <li>Each node begins with a header giving its type, and
 * for elements and PI the name id
 * <li>then comes the id of the parent
 * <li>then the id of the next sibling
 * <li>then namespaces if any: a first word codes the node-kind & prefix, 
 *     a 2nd word a pointer to string value (see below).
 * <li>then attributes: a first word codes the node-kind & name,
 *     a 2nd word a pointer to the value.
 * <li>then the contents: 
 *    <ul><li>for elements, the children are directly stored in document order.
 *            A flag on the parent tells if there are not children.
 *       <li>for text nodes, a code describes a pointer to the text value.
 *    </ul>
 * </ul>
 * Text values storage: <ul>
 * <li>Short strings: the characters, prefixed by the length are stored as
 * a chunk in a character block. The pointer (LSbit==1) is the offset of
 * the chunk in the block.
 * <li>Big strings: String objects are accumulated in a vector, the
 * pointer (LSbit==0) is the index of the string object in the vector.
 * </ul>
 * Storing short strings this way saves a lot of space (overhead of a
 * String object: at least 40 bytes.) For most languages (except CJK)
 * it would be possible to compress characters on one byte, but
 * Java lacks efficient interfaces.
 */
public class IDocument extends XDefaultHandler 
    implements FONIDocument
{
    final static int PARENT_OFFSET = 1;
    final static int NEXT_OFFSET = 2;
    final static int ATTR_OFFSET = 3;
    final static int CONTENT_OFFSET = 3;

    final static int KIND_BITS = 3;	// bits for node kind
    final static int KIND_MASK = (1 << KIND_BITS) - 1;
    final static int HAS_CHILDREN = 8;
    final static int PN_LAST = 8;	// flag: last pseudonode

    final static int NAME_SHIFT = KIND_BITS + 1; // reserve bits for flags
    final static int NAME_BITS = 12;		 // bits for name id
    final static int NAME_MASK = (1 << NAME_BITS) - 1;
    
    final static int ATTR_SHIFT = NAME_SHIFT + NAME_BITS;  // 
    final static int ATTR_BITS = 9; 		 // bits for element attr count
    final static int ATTR_MASK = (1 << ATTR_BITS) - 1; 
    
    final static int NS_SHIFT = ATTR_SHIFT + 9;    // the rest for NS node count
    
    final static int BLOCK_SHIFT = 9;
    final static int BLOCK_SIZE = 1 << BLOCK_SHIFT;
    final static int BLOCK_MASK = BLOCK_SIZE - 1;
    
    final static int CHARBLOCK_SHIFT = 12;	// 4K chars. must stay <= 15
    final static int CHARBLOCK_SIZE = 1 << CHARBLOCK_SHIFT;
    final static int CHARBLOCK_MASK = CHARBLOCK_SIZE - 1;
    
    private QNameTable elementNames = new QNameTable();
    private QNameTable otherNames = new QNameTable();
    
    // non-null when the current node has namespace nodes:
    private ArrayList prefixMapping;
    private ArrayList  namespaceStack = new ArrayList();
    private int xml_id;
    
    //private boolean multiblock = !true;   // single block NEVER GOOD
    
    // monoblock version:
    //private int[] data;
    
    // multi-block version
    private int[][] blocks;	//OLD
    private int blockCnt;
    private int/*NId*/docSize;
    private int/*NId*/curNode;
    private int/*NId*/prevNode = 0;
    
    private int charCnt, charChunks, textChunks;
    private int totalAttrCnt, totalAttrChars;
    
    // temporary buffer
    private char[] charBuffer = new char[CHARBLOCK_SIZE / 2];
    private int charPtr;
    private boolean textIsWhite;
    
    // storage blocks for small strings
    private char[][] charBlocks;
    private int lastCharBlock;	// index of current char block
    private int charBlockPtr;	// ptr inside current block
    // Big strings (directly stored in the ArrayList)
    private ArrayList bigStrings = new ArrayList();
    private boolean exported;
    private boolean trace = false;
    private boolean stats = false;
    private HashMap idTable;
    private HashMap idrefTable;
    private Owner owner;
    
    public IDocument()
    {
//      if(multiblock) {
            blocks = new int[8][];
            blocks[0] = new int[BLOCK_SIZE];
            blockCnt = 1;
//        }
//        else {
//            data = new int[BLOCK_SIZE];
//        }
        
        charBlocks = new char[8][];
        charBlocks[0] = new char[CHARBLOCK_SIZE];
        lastCharBlock = 0;
        charBlockPtr = 1;
        docSize = CONTENT_OFFSET; // dummy node at 0
        openNode(Node.DOCUMENT);
        curNode = CONTENT_OFFSET;
        xml_id = otherNames.enter(IQName.XML_ID);
    }

    // -------- FONIDocument interface: -------------

    public String[] getDTDInfo()
    {
        if (dtdName == null && dtdSystemid == null && dtdPublicId == null)
            return null; 
        return new String[] { dtdName, dtdSystemid, dtdPublicId };
    }

    public int/*NId*/  getRootNode()
    {
        return CONTENT_OFFSET;
    }

    public int getKind(int/*NId*/  nodeId)
    {
        return (int) dataAt(nodeId) & KIND_MASK;
    }

    public IQName getName(int/*NId*/  nodeId)
    {
        switch (getKind(nodeId)) {
        case Node.ELEMENT:
            return elementNames.getName(getNameId(nodeId));
        case Node.PROCESSING_INSTRUCTION:
            return otherNames.getName(getNameId(nodeId));
        default:
            return null;
        }
    }

    public int getNameId(int/*NId*/ offset)
    {
        return (intDataAt(offset) >> NAME_SHIFT) & NAME_MASK;
    }

    public IQName pnGetName(int/*NId*/ nodeId)
    {
        return otherNames.getName(pnGetNameId(nodeId));
    }

    public int pnGetNameId(int/*NId*/ nodeId)
    {
        return (intDataAt(nodeId) >> NAME_SHIFT) & NAME_MASK;
    }

    public int/*NId*/ getParent(int/*NId*/ nodeId)
    {
        return dataAt(nodeId + PARENT_OFFSET);
    }

    public int/*NId*/ getNextSibling(int/*NId*/ nodeId)
    {
        return dataAt(nodeId + NEXT_OFFSET);
    }

    public int/*NId*/ getNodeNext(int/*NId*/ nodeId)
    { // in document order
        int/*NId*/ kid = getFirstChild(nodeId);
        return (kid != 0) ? kid : getNodeAfter(nodeId);
    }

    public int/*NId*/ getNodeAfter(int/*NId*/ nodeId)
    {
        int/*NId*/ nxt;
        while ((nxt = getNextSibling(nodeId)) == 0) {
            int/*NId*/ parent = getParent(nodeId);
            if (parent == 0)
                return 0;
            nodeId = parent;
        }
        return nxt;
    }

    public int/*NId*/ getNodeSpan(int/*NId*/ nodeId)
    {
        int/*NId*/ nxt = getNodeAfter(nodeId);
        if (nxt == 0)
            nxt = docSize;
        return nxt - nodeId;
    }

    public int/*NId*/ getFirstChild(int/*NId*/ nodeId)
    {
        int header = intDataAt(nodeId), kind = header & KIND_MASK;
        if (kind != Node.ELEMENT && kind != Node.DOCUMENT
            || (header & HAS_CHILDREN) == 0)
            return 0;
        int/*NId*/ kid =
            nodeId + ATTR_OFFSET + 2
                    * (hAttrCount(header) + hNamespaceCount(header));
        return kid;
    }

    public String getStringValue(int/*NId*/ nodeId)
    {
        switch (getKind(nodeId)) {
        case Node.ELEMENT:
        case Node.DOCUMENT:
            StringBuffer sb = new StringBuffer((int) getNodeSpan(nodeId)); // OOPS
            int/*NId*/ kid = getFirstChild(nodeId);
            for (; kid != 0; kid = getNextSibling(kid)) {
                recStringValue(kid, sb);
            }
            return sb.toString();
        case Node.ATTRIBUTE:
        case Node.NAMESPACE:
            return decodeString(intDataAt(nodeId + 1));
        default:
            return decodeString(intDataAt(nodeId + CONTENT_OFFSET));
        }
    }

    void recStringValue(int/*NId*/ nodeId, StringBuffer sb)
    {
        switch (getKind(nodeId)) {
        case Node.ELEMENT:
        case Node.DOCUMENT:
            int/*NId*/ kid= getFirstChild(nodeId);
        for(; kid != 0; kid=getNextSibling(kid)) {
            recStringValue(kid, sb);
        }
        break;
        case Node.TEXT:
            decodeString( intDataAt(nodeId + CONTENT_OFFSET), sb );
        break;
        }
    }
    
    public int getAttrCount( int/*NId*/  nodeId ) {
        return hAttrCount( intDataAt(nodeId) );
    }
    
    public int/*NId*/    getAttribute( int/*NId*/  nodeId, int nameId )
    {
        int/*NId*/  off = nodeId + ATTR_OFFSET + 2 * getNamespaceCount(nodeId);
        int cnt = getAttrCount(nodeId);
        if(nameId < 0 && cnt > 0)
            return off;
        for(; cnt > 0; --cnt, off += 2)
            if(getNameId(off) == nameId)
                return off;
        return 0;
    }
    
    public int/*NId*/   pnGetNext( int/*NId*/  nodeId ) {
        if(nodeId == 0)
            return 0;
        // bit PN_LAST on header means 'last attribute or NS'
        return ( (dataAt(nodeId) & PN_LAST) != 0 )? 0 : nodeId + 2;
    }
    
    /**
     *	Gets the string value for pseudo-nodes Attributes and Namespaces.
     */
    public String pnGetStringValue( int/*NId*/  nodeId ) {
        return decodeString( intDataAt(nodeId + 1) );
    }
    
    public char[] getCharValue(int/*NId*/ nodeId, int reserve)
    {
        switch (getKind(nodeId)) {
        case Node.ELEMENT:
        case Node.DOCUMENT:
            throw new RuntimeException("not allowed on non-atoms");
        case Node.ATTRIBUTE:
        case Node.NAMESPACE:
            return decodeChars(intDataAt(nodeId + 1), reserve);
        default:
            return decodeChars(intDataAt(nodeId + CONTENT_OFFSET), reserve);
        }
    }

    public char[] pnGetCharValue(int/*NId*/ nodeId, int reserve)
    {
        return decodeChars(intDataAt(nodeId + 1), reserve);
    }

    public int getDefinedNSCount(int/*NId*/ nodeId)
    {
        return getNamespaceCount(nodeId);
    }

    public int/*NId*/ getFirstNSNode(int/*NId*/ nodeId)
    {
        if (nodeId == 0 || getNamespaceCount(nodeId) == 0)
            return 0;
        return nodeId + ATTR_OFFSET;
    }

    // dummy
    public Object getValue(int/*NId*/ nodeId)
    {
        return null;
    }

    public long getIntegerValue(int/*NId*/ nodeId)
    {
        return -1;
    }

    public int getElementNameCount()
    {
        return elementNames.size();
    }

    public IQName getElementName(int nameId)
    {
        return elementNames.getName(nameId);
    }

    public int internElementName(QName name)
    {
        return elementNames.find(name);
    }

    public int getOtherNameCount()
    {
        return otherNames.size();
    }

    public IQName getOtherName(int nameId)
    {
        return otherNames.getName(nameId);
    }

    public int internOtherName(QName name)
    {
        return otherNames.find(name);
    }

    public void close()
        throws IOException, SAXException
    {
    }
    
    // ----- SAX2 construction:
    
    public void endDocument() throws SAXException
    {
        if(stats) {
            estimateMemorySize();
            System.err.println("doc size "+docSize+" chars "+charCnt+
                               " chunks "+charChunks+"/"+textChunks);
//            for(int i = 0; i < docSize; i++)
//                System.err.println(i+" = "+dataAt(i));
//            checkDump(3, 0, 2);
        }
    }
    
    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes attrs)
        throws SAXException
    {
        endText();
        int/*NId*/ nodeId = openNode(Node.ELEMENT);
        if (localName == null || localName.length() == 0) {
            namespaceURI = "";
            localName = qName;
        }
        int nameId = elementNames.enter(namespaceURI, localName);
        int attrCnt = attrs == null ? 0 : attrs.getLength();
        int nsCnt = 0;
        int/*NId*/ stoff = nodeId + CONTENT_OFFSET;
        totalAttrCnt += attrCnt;
        
        
        // namespaces ?
        if (prefixMapping != null) {
            /***** No more merge: straight copy of NS (nov 2005 draft)
            // merge with upper mapping:
            if(namespaceStack.size() > 0) {
                ArrayList upmap =
                    (ArrayList) namespaceStack.get(namespaceStack.size()-1);
                for(int u = upmap.size() - 2, pos; u >= 0; u -= 2) {
                    for(pos = prefixMapping.size() - 2; pos >= 0; pos -= 2)
                        if(upmap.get(u).equals(prefixMapping.get(pos)))
                            break;
                    if(pos < 0) {
                        prefixMapping.add(0, upmap.get(u));
                        prefixMapping.add(1, upmap.get(u+1));
                    }
                }
            }
            ***/
            
            // store:
            nsCnt = prefixMapping.size();
            for(int i = 0; i < nsCnt; i += 2) {
                int nsname = otherNames.enter("", (String) prefixMapping.get(i));
                int lastFlag = (i == nsCnt-2)? PN_LAST : 0;
                setData(stoff ++,
                        Node.NAMESPACE + (nsname << NAME_SHIFT) + lastFlag);
                setData(stoff ++, storeString((String) prefixMapping.get(i+1)));
                
            }
            namespaceStack.add(prefixMapping);
            prefixMapping = null;
        }
        
        // store attributes:
        for (int a = 0; a < attrCnt; a++)
        {
            int id = otherNames.enter(attrs.getURI(a), attrs.getLocalName(a));
            int lastFlag = (a == attrCnt - 1) ? PN_LAST : 0;
            setData(stoff++, Node.ATTRIBUTE + (id << NAME_SHIFT) + lastFlag);
            String value = attrs.getValue(a);
            totalAttrChars += value.length();
            setData(stoff++, storeString(value));
            String atype = attrs.getType(a);
            if(id == xml_id) {
                addId(value, nodeId);
            }
            else if (atype.length() >= 2 
                 && atype.charAt(0) == 'I'
                 && atype.charAt(1) == 'D')
            {
                // System.err.println(attrs.getLocalPart(a)+" = "+value);
                if (atype.length() == 2)
                    addId(value, nodeId);
                else
                    addIdrefs(value, nodeId, a);
            }
            // - System.err.println("ATTR at "+(stoff-2)+" "+id);
        }
        docSize = stoff;
        setData(nodeId, Node.ELEMENT
                        + (nameId << NAME_SHIFT)
                        + (attrCnt << ATTR_SHIFT)
                        + ((nsCnt / 2) << NS_SHIFT));
        if (trace)
            System.err.println("elem " + nodeId + " nameId=" + nameId
                               + " attrCnt=" + attrCnt + " nsCnt=" + nsCnt);
    }
    
    private void addIdrefs(String value, int/*NId*/ nodeId, int attrRank)
    {
        // tokenize value:
        String[] names = XMLUtil.splitList(value);
        for (int i = 0; i < names.length; i++) {
            addIdref(names[i], nodeId, attrRank);
        }
    }
    
    private void addIdref(String id, int/*NId*/ nodeId, int attrRank)
    {
        if(idrefTable == null)
            idrefTable = new HashMap();
        int[] nodes = (int[]) idrefTable.get(id);
        if(nodes == null)
            idrefTable.put(id, new int/*NId*/[] { nodeId, attrRank });
        else {
            int oldLen = nodes.length;
            int/*NId*/[] nnodes = new int/*NId*/[oldLen + 2];
            System.arraycopy(nodes, 0, nnodes, 0, oldLen);
            nnodes[oldLen] = nodeId;
            nnodes[oldLen + 1] = attrRank;
            idrefTable.put(id, nnodes);
        }
    }

    private void addId(String value, int/*NId*/ nodeId)
    {
        if(idTable == null)
            idTable = new HashMap();
        int[] nodes = (int[]) idTable.get(value);
        if(nodes == null) // ignore duplicate id
            idTable.put(value, new int/*NId*/[] { nodeId });
    }
    
    public int/*NId*/ [] getIdMatchingNodes(String id, boolean ref)
    {
        HashMap table = ref? idrefTable : idTable;
        return (int/*NId*/ []) table.get(id);
    }

    public void endElement(String namespaceURI, String localName, String qName)
        throws SAXException
    {
        endText();
        closeNode();
    }
    
    public void characters (char ch[], int start, int length)
    {
        int nPtr = charPtr + length;
        if(nPtr > charBuffer.length) {
            char[] old = charBuffer;
            charBuffer = new char[nPtr + 1000];
            System.arraycopy(old, 0, charBuffer, 0, charPtr);
        }
        // copy with detection of pure whitespace
        if(!whitespaceStripped)
            System.arraycopy(ch, start, charBuffer, charPtr, length);
        else for (int i = length; --i >= 0;) {
            char c = ch[start + i];
            if(c > ' ' || !Character.isWhitespace(c))
                textIsWhite = false;
            charBuffer[charPtr + i] = c;
        }
        if(false && whitespaceStripped) {
            System.err.println("chars |" + new String(ch, start, length) + "| " + textIsWhite);
        }
        charPtr = nPtr;
        ++ charChunks;
        charCnt += length;
    }
    
    public void characters (String chars) { // not meant for speed
        characters(chars.toCharArray(), 0, chars.length());
    }
    
    public void processingInstruction (String target, String data)
        throws SAXException
    {
        endText();
        int/*NId*/  node = openNode(Node.PROCESSING_INSTRUCTION);
        int nameId = otherNames.enter("", target);
        setData( node, dataAt(node) + (nameId << NAME_SHIFT));
        setData( docSize ++, storeString(data));
        closeNode();
    }
        
    public void startPrefixMapping ( String prefix, String uri )
        throws SAXException
    {
        if(prefixMapping == null)
            prefixMapping = new ArrayList();
        // TODO sort by prefix
        prefixMapping.add(prefix);
        prefixMapping.add(uri);
    }
    
    public void endPrefixMapping( String prefix ) 
        throws SAXException {
    }
    
    public void comment(char[] ch, int start, int length)
    {
        // deal with SAX problem: nothing to strip comments which are inside a DTD
        if(!takeComment)    
            return;
        endText();
        openNode(Node.COMMENT);
        setData( docSize ++, storeString(new String(ch, start, length)));
        closeNode();
    }
    
    // ------------------------
    
    public int/*NId*/  getCurrentNode() {
        return curNode;
    }
    
    private int/*NId*/openNode(int impl)
    {
        int/*NId*/nodeId = docSize;
        if(trace) System.err.println("open node "+nodeId+" impl="+impl);
        docSize += CONTENT_OFFSET;
        if (prevNode != 0)
            setData( prevNode + NEXT_OFFSET, (int) nodeId );  // CAUTION cast
        setData(nodeId, impl);
        setData(nodeId + PARENT_OFFSET, (int) curNode);  // CAUTION cast
        setData(nodeId + NEXT_OFFSET, 0);
        curNode = nodeId;
        prevNode = 0;
        return curNode;
    }
    
    private void  closeNode ()
    {
        prevNode = curNode;
        curNode = getParent(curNode);
        if(curNode != 0)
            setFlag( curNode, HAS_CHILDREN );
    }
    
    private void  endText()
    {
        if( charPtr > 0 && !(whitespaceStripped && textIsWhite)) {
            ++ textChunks;
            openNode(Node.TEXT);
            setData( docSize ++, storeString());
            closeNode();
        }
        //textChunk = null;
        charPtr = 0;
        textIsWhite = true;
    }
    
    private void setFlag( int/*NId*/  nodeId, int flag ) {
        setData( nodeId, dataAt(nodeId) | flag );
    }
    
    private void checkDump( int/*NId*/  node, int/*NId*/  parent, int depth )
    {
        System.out.print(node);
        for(int d = 0; d < depth; d++) System.out.print("  ");
        ++ depth;
        switch(getKind(node)) {
        case Node.ELEMENT:
        case Node.DOCUMENT:
            System.out.println("ELEMENT "+getName(node));	
            int/*NId*/  ns = getFirstNSNode(node);
            for( ; ns != 0; ns = pnGetNext(ns)) {
                checkDump(ns, node, depth);
            }
            int/*NId*/  attr = getAttribute(node, -1);
            for( ; attr != 0; attr = pnGetNext(attr)) {
                checkDump(attr, node, depth);
            }
            int/*NId*/  kid = getFirstChild(node);
            for( ; kid != 0; kid = getNextSibling(kid))
                checkDump(kid, node, depth);
            break;
        case Node.ATTRIBUTE:
            System.out.println("  ATTR "+ pnGetName(node) +
                               " |"+pnGetStringValue(node)+"|");
            break;
        case Node.NAMESPACE:
            System.out.println("  NS "+ pnGetName(node) +
                               " |"+pnGetStringValue(node)+"|");
            break;
        case Node.TEXT:
            System.out.println("TEXT |"+getStringValue(node)+"|");
            break;
        case Node.COMMENT:
            System.out.println("COMMENT |"+getStringValue(node)+"|");
            break;
        case Node.PROCESSING_INSTRUCTION:
            System.out.println("PI |"+getStringValue(node)+"|");
            break;
        default:
            System.err.println("*** bad node id="+node+" kind="+getKind(node));
        break;
        }
    }
    
    // ----- implementation specific:
    public boolean isExported()  {
        boolean exp = exported;
        exported = true;
        return exp;
    }
    
    public int/*NId*/ estimateMemorySize()
    {
        int/*NId*/ sizeNodes = blockCnt * (int/*NId*/) BLOCK_SIZE * 4;
        int/*NId*/ sizeChars = lastCharBlock * (int/*NId*/) CHARBLOCK_SIZE * 2;
        int/*NId*/ sizeBigStrings = bigStrings.size() * CHARBLOCK_SIZE; // minimum
        if(stats)
            System.err.println("sizes nodes="+sizeNodes + " chars="+sizeChars + " bigst="+sizeBigStrings);
        return sizeNodes + sizeChars + sizeBigStrings;
    }
    
    public int/*NId*/ virtualSize()
    {
        return estimateMemorySize();
    }

    private final int dataAt(int/*NId*/offset)
    {
//        if(multiblock)
            return blocks[(int) (offset >> BLOCK_SHIFT)][(int) (offset & BLOCK_MASK)];
//        else
//            return data[offset];
    }
    
    private final int intDataAt(int/*NId*/ offset)
    {
        return (int) dataAt(offset);
    }
    
    private void setData(int/*NId*/offset, int value)
    {
//        if(multiblock) {
            int block = (int) (offset >> BLOCK_SHIFT);
            int cell = (int) (offset & BLOCK_MASK);
            if(block >= blockCnt) {
                if(blockCnt >= blocks.length) {
                    int[][] old = blocks;
                    blocks = new int[old.length + 64][];
                    System.arraycopy(old, 0, blocks, 0, old.length);
                }
                blocks[ blockCnt ++ ] = new int[BLOCK_SIZE];
            }
            blocks[block][cell] = value;
//        }
//        else {
//            if(offset >= data.length) {
//                int[] old = data;
//                int incr = old.length; //Math.min(old.length, 1000000);
//                data = new int[ old.length + incr ];
//                System.arraycopy(old, 0, data, 0, old.length);
//                System.out.println("alloc "+data.length);
//            }
//            data[offset] = value;
//        }
    }
    
    private int getNamespaceCount(int/*NId*/ nodeId)
    {
        return hNamespaceCount(dataAt(nodeId));
    }

    private final int hNamespaceCount(int/*NId*/ header)
    {
        return (int) (header >> NS_SHIFT);
    }

    private final int hAttrCount(int header)
    {
        return (header >> ATTR_SHIFT) & ATTR_MASK;
    }

    private int storeString(String str)
    {
        int L = str.length();
        if(L > charBuffer.length)
            return storeBigString(str);
        str.getChars(0, L, charBuffer, 0);	// big enough
        charPtr = L;
        int result = storeString();
        charPtr = 0;
        return result;
    }
    
    private int storeString()
    {
        if (charPtr > CHARBLOCK_SIZE / 2) {
            return storeBigString(new String(charBuffer, 0, charPtr));
        }
        int rlen = charPtr + 1; // required length
        if (charBlockPtr + rlen > CHARBLOCK_SIZE) {
            // spanning blocks is not allowed: allocate a new block
            if (++lastCharBlock >= charBlocks.length) {
                char[][] old = charBlocks;
                charBlocks = new char[old.length + 100][];
                System.arraycopy(old, 0, charBlocks, 0, old.length);
            }
            charBlocks[lastCharBlock] = new char[CHARBLOCK_SIZE];
            charBlockPtr = 0;
        }
        int index = lastCharBlock * CHARBLOCK_SIZE + charBlockPtr;
        char[] chb = charBlocks[lastCharBlock];
        chb[charBlockPtr] = (char) charPtr;
        System.arraycopy(charBuffer, 0, chb, charBlockPtr + 1, charPtr);
        charBlockPtr += rlen;
        return index << 1;
    }
    
    private int storeBigString(String s)
    {
        int index = bigStrings.size();
        bigStrings.add(s);
        return (index << 1) + 1;
    }

    private String decodeString(int code)
    {
        if ((code & 1) != 0)
            // big string:
            return (String) bigStrings.get(code >> 1);
        else {
            code >>= 1;
            char[] block = charBlocks[code >> CHARBLOCK_SHIFT];
            int pos = code & CHARBLOCK_MASK;
            // the first char is the length of the string
            return new String(block, pos + 1, (int) block[pos]);
        }
    }

    private void decodeString(int code, StringBuffer buffer)
    {
        if ((code & 1) != 0)
            // big string:
            buffer.append((String) bigStrings.get(code >> 1));
        else {
            code >>= 1;
            char[] block = charBlocks[code >> CHARBLOCK_SHIFT];
            int pos = code & CHARBLOCK_MASK;
            // the first char is the length of the string
            buffer.append(block, pos + 1, (int) block[pos]);
        }
    }

    private char[] decodeChars(int code, int reserve)
    {
        if ((code & 1) != 0) {
            // big string:
            String s = (String) bigStrings.get(code >> 1);
            char[] res = new char[s.length() + reserve];
            s.getChars(0, s.length(), res, reserve);
            return res;
        }
        else {
            code >>= 1;
            char[] block = charBlocks[code >> CHARBLOCK_SHIFT];
            int pos = code & CHARBLOCK_MASK;
            // the first char is the length of the string
            int L = (int) block[pos];
            char[] res = new char[L + reserve];
            System.arraycopy(block, pos + 1, res, reserve, L);
            return res;
        }
    }

    public Owner getOwner()
    {
        return owner;
    }

    public void setOwner(Owner owner)
    {
        this.owner = owner;
    }

    public DataConversion getDataConversion()
    {
        return null;
    }
}
