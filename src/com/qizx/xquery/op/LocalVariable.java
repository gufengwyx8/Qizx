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
package com.qizx.xquery.op;

import com.qizx.api.QName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQType;

/**
 * Local variable declaration.
 */
public class LocalVariable extends LeafExpression
{
    public QName name;
    // expression that declares the variable (for formal optimisations)
    public Expression owner;
    public int uses; // how many references
    public int id;   // unique id in context // debug
    
    public int address; // allocation in eval context

    // Variable declarations are managed as a tree for address allocation:
    // - a 'child' is a variable declared after, active at the same time,
    // hence using a different address. A parent is a variable declared before.
    // - A 'sibling' represents a parallel variable (not active at the same 
    // time) possibly using the same address (if same type category).
    // The order of siblings is not relevant.
    //   
    public LocalVariable pred; // parent in tree terms (var declared before)
    public LocalVariable succ; // child in tree terms (var declared after)
    public LocalVariable replacing; // or 'replaced by' (sibling relationship)

    public boolean grouped; // overloaded for group-by (key or other used var)

    
    public LocalVariable(QName name, XQType type, Expression owner)
    {
        this.name = name;
        this.type = type;
        this.owner = owner;
    }

    // declares a variable after another: insert a last child of predecessor
    public void addAfter(LocalVariable successor)
    {
        successor.replacing = this.succ; // null in general
        this.succ = successor;
        successor.pred = this;
    }

    public void removeAfter(LocalVariable var)
    {
        if (succ == var)
            succ = var.replacing;
        else
            for (LocalVariable slot = succ; slot != null; slot = slot.replacing)
                if (slot.replacing == var) {
                    slot.replacing = var.replacing;
                    break;
                }
        var.replacing = null;
    }

    // declare this variable just before 'var'
    // in terms of tree operations, this is equivalent to wrap/surround 'var'
    // inside this var.
    public void declareBefore(LocalVariable var)
    {
        // disconnect 'var':
        LocalVariable previous = var.pred;
        previous.removeAfter(var);
        // insert this as child of previous:
        previous.addAfter(this);
        // and 'var' as child of this:
        this.addAfter(var);
    }

    public void use()
    {
        ++ uses;
    }

    private boolean isItem(XQType type)
    {
        return type != null
               && type.getOccurrence() == XQType.OCC_EXACTLY_ONE
               && !type.equals(XQType.INTEGER)
               && !type.equals(XQType.DOUBLE)
               && !type.equals(XQType.STRING);
    }

    /**
     * Redefines the type of the variable, and marks the variable for register
     * allocation: if too many variables want the same kind of register,
     * outermost variables will be allocated in stack.
     * @param context TODO
     */
    public void storageType(XQType type, ModuleContext context)
    {
        this.type = type;
        if(context.trace) System.err.println(" study "+name+" "+type);
        
        // if can be allocated in a register, disable upper variables in excess
        int registerMax = EvalContext.MAX_REGISTER;
        if (type.getOccurrence() == XQType.OCC_EXACTLY_ONE) {
            boolean isItem = isItem(type);
            if (isItem) {
                // item registers: different count
                registerMax =
                    EvalContext.LAST_REGISTER - EvalContext.ITEM_REGISTER;
                type = XQType.ITEM; // / CAUTION
            }
            if(context.trace) System.err.println(name+" isItem "+isItem+" "+registerMax);
            // - System.err.println(" var "+name+" "+type+" "+isItem);
            LocalVariable prev = this.pred;
            for (; prev != null; prev = prev.pred) {
                if (type.equals(prev.getType())
                        || (isItem && isItem(prev.getType()))) {
                    //
                    --registerMax;
                    if (registerMax <= 0) {
                        if(context.trace) System.err.println(" zap var "+prev.name);
                        prev.address = -1; // non register
                    }
                }
            }
        }
    }

//    DON'T implement hash/equals: each var has its own identity
//
//    public int hashCode()
//    {
//        return address; // simply
//    }
//    
//    public boolean equals(Object obj)
//    {
//        if(obj instanceof LocalVariable) {
//            return address == ((LocalVariable) obj).address;
//        }
//        return false;
//    }
    
    public String toString() {
        return "Local " + name + " addr " + address + " type " + type +" uses "+uses;
    }
}
