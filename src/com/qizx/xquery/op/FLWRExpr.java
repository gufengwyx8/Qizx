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

import com.qizx.api.EvaluationException;
import com.qizx.api.QName;
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xquery.*;
import com.qizx.xquery.dt.*;
import com.qizx.xquery.dt.StringValue;
import com.qizx.xquery.impl.EmptyException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * The FLOWER expression. Features detection and optimization of joins.
 */
public class FLWRExpr extends Expression
{
    // Debug or emergency fallback: disable joins
    static boolean noJoins = System.getProperty("xquest.nojoins") != null;
    static int genId;
    private static final XQItem EMPTY_ITEM = new SingleString("");
    
    public Expression where;
    public Expression expr;     // return expr
    private Expression[] clauses = new Expression[0];
    private OrderSpec[] orderSpecs;
    public boolean stableOrder;
    public boolean checked;
    protected boolean hasScore;
    
    public GroupingVariable[] groupingKeys;
    public LetClause[] postGroupingLets;
    public Expression postGroupingWhere;    // or final where

    // bound local variables (before group-by)
    private LocalVariable[] nonGroupingVars; // other vars used
    // redeclared local variables (after group-by)
    private LocalVariable[] outGroupingVars;
    private LocalVariable[] outNonGroupingVars; // other vars used
    

    public FLWRExpr()
    {
    }

    public VarClause addClause(VarClause clause)
    {
        clauses = addExpr(clauses, clause);
        return clause;
    }
    
    public VarClause addPostLetClause(LetClause clause)
    {
        postGroupingLets = (LetClause[]) addExpr(postGroupingLets, clause);
        return clause;
    }

    // adds before position
    public void addClause(VarClause clause, VarClause position)
    {
        clauses = addExpr(clauses, clause);
        for (int c = clauses.length; --c > 0;) {
            clauses[c] = clauses[c - 1];
            if (clauses[c] == position) {
                clauses[c - 1] = clause;
                break;
            }
        }
    }

    VarClause getClause(int rank)
    {
        return rank < 0 || rank >= clauses.length
            ? null : (VarClause) clauses[rank];
    }

    public void addOrderSpec(OrderSpec orderSpec)
    {
        if (orderSpecs == null)
            orderSpecs = new OrderSpec[] { orderSpec };
        else {
            OrderSpec[] old = orderSpecs;
            orderSpecs = new OrderSpec[old.length + 1];
            System.arraycopy(old, 0, orderSpecs, 0, old.length);
            orderSpecs[old.length] = orderSpec;
        }
    }

    OrderSpec getOrderSpec(int rank)
    {
        return (rank < 0 || orderSpecs == null || rank >= orderSpecs.length)
                    ? null
                    : orderSpecs[rank];
    }

    public Expression child(int rank)
    {
        if (rank < clauses.length)
            return clauses[rank];
        rank -= clauses.length;
        if (where != null)
            if (rank == 0)
                return where;
            else
                --rank;
        if(orderSpecs != null) {
            if (rank < orderSpecs.length)
                return orderSpecs[rank];
            rank -= orderSpecs.length;
        }
        return rank == 0 ? expr : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.children(clauses);
        if (where != null)
            d.child("where", where);
        // d.display("stableOrder", ""+stableOrder);
        
        if(groupingKeys != null) {
            for (int i = 0; i < groupingKeys.length; i++) {
                d.child(groupingKeys[i]); 
                d.child("out", "" + outGroupingVars[i]); 
            }
            for (int i = 0; i < nonGroupingVars.length; i++) {
                d.child("nonGroupingVar", "" + nonGroupingVars[i]); 
                d.child("out", "" + outNonGroupingVars[i]); 
            }
            for (int i = 0; i < postGroupingLets.length; i++) {
                d.child("post-let", postGroupingLets[i]);                
            }
            if (postGroupingWhere != null)
                d.child("where", postGroupingWhere);
        }
        
        if (orderSpecs != null)
            d.children("orderBy", orderSpecs);
        d.child("return", expr);
    }

    public int getFlags()
    {
        return expr.getFlags(); 
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        LocalVariable mark = context.latestLocalVariable();
        
        // for and let clauses, declare local variables
        for (int c = 0; c < clauses.length; c++) {
            ((VarClause) clauses[c]).owner = this;
            context.staticCheck(clauses[c], 0);
            // is there a FT score? then sorting will need an extra 
            // array of double to store scores
            if(clauses[c] instanceof ForClause) {
                ForClause forc = (ForClause) clauses[c];
                if(forc.scoreDecl != null)
                    hasScore = true;
            }
        }
        // -((Module) context).dumpLocals("----- flower vars:");

        // where clause:
        if (where != null) {
            where = context.staticCheck(where, 0);
        }                

        // new in 2.2: group by
        if(groupingKeys != null) {
            checkGroupBy(context);
        }

        // order by:
        if (orderSpecs != null)
            for (int os = 0; os < orderSpecs.length; os++) {
                context.staticCheck(orderSpecs[os], 0);
            }
        
        // returned expression:
        expr = context.simpleStaticCheck(expr, 0);
        type = expr.getType().itemType().star;

        // join detection: TODO iterate if nested loops
        // TODO could generalize to a let
        if (where != null && !noJoins) {
            JoinInfo jinfo = joinablePredicate(where);
            if (jinfo != null) {
                // we have a join:
                // 1- suppress the where or replace it by predicate remainder
                where = jinfo.remainder;

                // 2- insert the join creation as a 'let' before the
                // 'for' clause of the outerloop
                ForClause inner = (ForClause) jinfo.innerVar.owner;

                // actually it is much better to try to put the join creation
                // outside of as many loops as possible
                // CAUTION: must start from the inner loop, not the outer,
                // because there can be a dependence of the inner expression to
                // an intermediary variable
                LocalVariable outerVar = jinfo.innerVar, prev;
                // LocalVariable outerVar = jinfo.outerVar, prev;
                for (; (prev = outerVar.pred) != null; outerVar = prev)
                    if (prev.owner == null || // function arg
                         prev.name == null ||
                         Expr.depends(inner.expr, prev))
                        break;

                // a special local to store the join table:
                LocalVariable joinVar =
                    context.newLocal(null, XQType.WRAPPED_OBJECT, null);
                joinVar.declareBefore(outerVar);
                // special local vars cannot be in a register (BUG FIX):
                joinVar.address = -1;
                LetClause newLet = new LetClause(joinVar.name);
                joinVar.owner = newLet;
                newLet.varDecl = joinVar;
                // a temporary for creating the join:
                // replaces $innerVar in innerExpr
                LocalVariable tmpVar =
                    context.newLocal(null, XQType.NODE, null);
                joinVar.addAfter(tmpVar);
                // special local vars cannot be in a register (BUG FIX):
                tmpVar.address = -1;
                // Replace $innerVar by $tmpVar in innerExpr:
                Expr.VarReplacer replacer =
                    new Expr.VarReplacer(jinfo.innerVar, tmpVar);
                replacer.visit(jinfo.innerExpr);
                // expression that builds the join table:
                newLet.expr =
                    new Join.Maker(inner.expr, jinfo.innerExpr, tmpVar,
                                   jinfo.type);

                VarClause outer = (VarClause) outerVar.owner;
                FLWRExpr outerFlwr = (FLWRExpr) outer.owner;
                newLet.owner = outerFlwr;
                outerFlwr.addClause(newLet, outer);

                // 3- transform the source expression of the inner loop into
                // an access to the join table: join-get(join-table, outer-key)
                inner.expr = new Join.Get(jinfo.outerExpr, joinVar,
                                     jinfo.comparison);
            }
        }
        context.popLocalVariables(mark);
        // try to transform a where clause into a predicate
        // (implies that this is not a join)
        FLWRExpr reduced = tryToRemoveWhere(this);
        // simplify dummy loop?
        return tryToRemoveLoop(reduced);
    }

    private void checkGroupBy(ModuleContext context)
    {
        // Build a list of "grouping variables" (keys) and
        //  a list of other local variables actually used after 'group by'
        for (int i = 0; i < groupingKeys.length; i++) {
            groupingKeys[i] =
                (GroupingVariable) context.staticCheck(groupingKeys[i], 0);
        }

        VarRefCollector usedVars = new VarRefCollector();
        for (int i = 0; i < postGroupingLets.length; i++)
            usedVars.visit(postGroupingLets[i]);
        if (postGroupingWhere != null)
            usedVars.visit(postGroupingWhere);

        // scan other parts to find *names* of used variables
        // Caution: those parts are not yet staticChecked
        if (orderSpecs != null)
            for (int os = 0; os < orderSpecs.length; os++)
                usedVars.visit(orderSpecs[os]);
        usedVars.visit(expr);
        
        // create list of non-grouping variables:
        ArrayList ngVars = new ArrayList();
         // follow declaration order to cope with overloading
        LocalVariable lv = context.firstLocalVariable();
        LocalVariable last = context.latestLocalVariable();
     localLoop:
        for(; lv != null; lv = lv.succ) {
            QName name = lv.name;
            if(name == null || !usedVars.vars.contains(name) || lv.grouped)
                continue;
            for (int g = groupingKeys.length; --g >= 0;) {
                if(groupingKeys[g].decl == lv)
                    continue localLoop;
            }
            ngVars.add(lv);
            if(lv == last)
                break;
        }
        nonGroupingVars =
            (LocalVariable[]) ngVars.toArray(new LocalVariable[ngVars.size()]);
        
        // Each variable of both lists is redeclared after group-by: (output var)
        outGroupingVars = new LocalVariable[groupingKeys.length];
        for (int i = 0; i < groupingKeys.length; i++) {
            LocalVariable var = groupingKeys[i].decl;
            if(var != null) {
                outGroupingVars[i] =
                    context.defineLocalVariable(var.name, var.type, var.owner);
                var.grouped = true;
            }
        }
        
        outNonGroupingVars = new LocalVariable[nonGroupingVars.length];
        for (int i = 0; i < nonGroupingVars.length; i++) {
            LocalVariable var = nonGroupingVars[i];
            if(var != null) {
                XQItemType varType = (XQItemType) var.type.getItemType();
                outNonGroupingVars[i] = // change type to item-type*
                    context.defineLocalVariable(var.name, varType.star, var.owner);
                var.grouped = true;
            }
        }
        
        // static check of parts following 'group by': 
        // bind with 'output variables'
        for (int i = 0; i < postGroupingLets.length; i++) {
            context.staticCheck(postGroupingLets[i], 0);
        }
        if (postGroupingWhere != null) {
            postGroupingWhere = context.staticCheck(postGroupingWhere, 0);
        }
    }

    // Returns non-null info if the predicate represents a join.
    // The predicate is normalized.
    JoinInfo joinablePredicate(Expression pred)
    {
        // accept AND, comparison:
        if (pred instanceof AndExpr) {
            AndExpr and = (AndExpr) pred;
            int argc = and.args.length;
            // look for an AND member that is "joinable":
            // TODO: select best (eg equality is better than inequality)
            for (int a = 0; a < argc; a++) {
                JoinInfo info = joinablePredicate(and.args[a]);
                if (info != null) {
                    // remove first operand:
                    if (argc == 2)
                        info.remainder = and.args[1 - a];
                    else {
                        AndExpr nand = new AndExpr();
                        nand.args = new Expression[argc - 1];
                        System.arraycopy(and.args, 0, nand.args, 0, a);
                        System.arraycopy(and.args, a + 1, nand.args, a,
                                         argc - a - 1);
                        info.remainder = nand;
                    }
                    return info;
                }
            }
            return null;
        }
        if (!(pred instanceof Comparison.Exec))
            return null;

        Comparison.Exec comp = (Comparison.Exec) pred;
        Expression left = comp.args[0], right = comp.args[1];
        // - ignore node sorts for this purpose:
        if (left instanceof NodeSortExpr)
            left = ((NodeSortExpr) left).expr;
        if (right instanceof NodeSortExpr)
            right = ((NodeSortExpr) right).expr;
        // - dependencies on *for* variables:
        Expr.ForVarCollector vcol = new Expr.ForVarCollector();
        vcol.visit(left);
        if (vcol.found == null || vcol.found2 != null) // one var exactly
            return null;
        LocalVariable var1 = vcol.found;
        vcol.reset();
        vcol.visit(right);
        if (vcol.found == null || vcol.found2 != null)
            return null;
        LocalVariable var2 = vcol.found;
        // to be a join, the 2 variables must be different and the comparison
        // must be equality or order comparison, but not inequality.
        if (var1 == var2 || comp.test == ValueNeOp.TEST)
            return null;

        // Supplementary conditions (bugs)
        XQItemType t1 = var1.getType().itemType();
        XQItemType t2 = var2.getType().itemType();
        // if var1 and var2 atomic (ie not node) give up
        if (XQType.ATOM.isSuperType(t1) && XQType.ATOM.isSuperType(t2)) {
            return null;
        }
        // we want at least one path expression inside where
        // TODO

        // we have a comparison between two expressions which each have one
        // 'for'
        // variable reference.
        // Find which var corresponds to the innermost loop,
        // normalize so that the innermost is the 2nd
        JoinInfo info = new JoinInfo();
        info.outerVar = var1;
        info.innerVar = var2;
        info.outerExpr = left;
        info.innerExpr = right;
        LocalVariable var2in = var2;
        for (; var2in != null && var2in != var1;)   //TODO method
            var2in = var2in.pred;
        if (var2in == null) { // ie var1 after var2
            comp.args[0] = right;
            comp.args[1] = left;
            comp.test = comp.test.reverse();

            info.outerVar = var2;
            info.innerVar = var1;
            info.outerExpr = right;
            info.innerExpr = left;
        }
        info.comparison = comp.test;

        // determine the type of keys in the join table:
        // ATTENTION: use types of expressions in 'where', not var types
        XQItemType typeInner = info.innerExpr.getType().itemType();
        XQItemType typeOuter = info.outerExpr.getType().itemType();

        if (XQType.NUMERIC.isSuperType(typeInner)
            || XQType.NUMERIC.isSuperType(typeOuter))
            info.type = XQType.NUMERIC;
        // else if(XQType.NODE.isSuperType(typeInner) &&
        // XQType.NODE.isSuperType(typeOuter))
        // info.type = XQType.NODE; // node identity
        else
            // atomization:
            info.type = XQType.STRING;
        return info;
    }

    // collects info about the join
    public static class JoinInfo
    {
        LocalVariable outerVar; // variable of outer loop
        LocalVariable innerVar; // variable of inner loop

        Expression outerExpr; // comparison member containing outer var
        Expression innerExpr; // comparison member containing inner var
        Expression remainder; // non optimised remainder of a predicate
        Comparison.Test comparison;

        XQType type; // type of keys in join table (NUMERIC, STRING or default)
    }

    // in a simple FOR, try to transform a WHERE into a predicate of the source
    // expr:
    // for $x in EXPR where pred($x) return $x
    // ==> for $x in EXPR [ pred(.) ] return $x
    //
    static FLWRExpr tryToRemoveWhere(FLWRExpr flower)
    {
        if (flower.clauses.length != 1 || flower.where == null
            || flower.orderSpecs != null)
            return flower; // 
        VarClause clause = flower.getClause(0);
        if (!(clause instanceof ForClause))
            return flower;
        // if 'at' variable, give up (TODO check that 'where' depends on it)
        if (((ForClause) clause).position != null)
            return flower;
        // is the source a path ?
        Expression src = clause.expr;
        if (src instanceof NodeSortExpr)
            src = ((NodeSortExpr) src).expr;
        
        // TODO: questionable; can be any expression?
        if (!(src instanceof PathExpr))
            return flower; // fail
        
        LocalVariable loopVar = clause.varDecl;
        // does the where depend on loop variable ? but no ref inside a predicate!
        Expr.DottingVarFinder finder = new Expr.DottingVarFinder(loopVar);
        if (!finder.visit(flower.where))
            return flower;
        // replace loopVar by null (i.e '.') in WHERE:
        new Expr.VarReplacer(loopVar, null).visit(flower.where);
        clause.expr = Expr.addPredicate(clause.expr, flower.where);
        flower.where = null;
        return flower;
    }

    // Flower cutter.
    // for $x in EXPR return $x ==> EXPR
    // This can likely happen after other transformations
    // (or in case of dumb XQ programmers!).
    static Expression tryToRemoveLoop(FLWRExpr flower)
    {
        if (flower.orderSpecs != null || flower.where != null
            || flower.clauses.length != 1 || flower.hasScore)
            return flower; // fails: no change
        VarClause clause = flower.getClause(0);
        if (!(clause instanceof ForClause)
            || !(flower.expr instanceof VarReference.Local)
            || clause.declaredType != null)
            return flower;
        LocalVariable loopVar = clause.varDecl;
        VarReference.Local ret = (VarReference.Local) flower.expr;
        if (ret.decl != loopVar)
            return flower;
        return clause.expr;
    }

    // -----------------------------------------------------------------------------

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        // pipe the iterators of the different clauses to get source seq:
        VarClause.SingleDummy src = new VarClause.SingleDummy(focus, context);// dummy
        for (int c = 0; c < clauses.length; c++) {
            VarClause.SingleDummy newSrc =
                (VarClause.SingleDummy) clauses[c].eval(focus, context);
            newSrc.setSource(src);
            src = newSrc;
        }
        // optimization by specialized sequences:
        if (orderSpecs == null && groupingKeys == null
            && !(src instanceof WindowClause.Sequence))
        {
            XQItemType itemType = expr.getType().itemType();
            int occ = expr.getType().getOccurrence();
            if (itemType == XQType.INTEGER && !XQType.isRepeatable(occ))
                return new IntSequence(src, focus, context);
            else if (itemType == XQType.STRING && !XQType.isRepeatable(occ))
                return new StringSequence(src, focus, context);
            else if (occ == com.qizx.api.SequenceType.OCC_EXACTLY_ONE)
                return new ItemSequence(src, focus, context);
        }
        
        // grouping: handles first 'where'
        Expression finalWhere = where;
        if(groupingKeys != null) {
            // TODO OPTIM if no post-grouping let/where, and if order-by equiv
            // to group-by then we can use a TreeMap + order comparator 
            GroupBySequence gs = new GroupBySequence(src, where, context, null);
            src = gs;
            for (int c = 0; c < postGroupingLets.length; c++) {
                VarClause.SingleDummy newSrc =
                    (VarClause.SingleDummy) postGroupingLets[c].eval(focus, context);
                newSrc.setSource(src);
                src = newSrc;
            }
            finalWhere = postGroupingWhere;
        }
        
//        if(UpdatingExpr.isUpdating(this))
//            System.err.println("updating flower");
        
        // final sequence (handles 'return' and final where):
        XQValue v = new Sequence(src, finalWhere, focus, context);
        if (orderSpecs != null)
            // TODO lazier: sort only when required
            return sorted(v, focus, context);
        return v;
    }

    public void evalAsEvents(XMLPushStreamBase output, Focus focus,
                             EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        // if sorted or grouped, there is no gain to recursively evalAsEvents:
        // use std method
        if (orderSpecs != null || groupingKeys != null) {
            super.evalAsEvents(output, focus, context);
            return;
        }

        VarClause.SingleDummy src = new VarClause.SingleDummy(focus, context);// dummy
        for (int c = 0; c < clauses.length; c++) {
            VarClause.SingleDummy newSrc =
                (VarClause.SingleDummy) clauses[c].eval(focus, context);
            newSrc.setSource(src);
            src = newSrc;
        }

        for (; src.next();) {
            if (where != null
                && !where.evalEffectiveBooleanValue(focus, context))
                continue;
            context.at(expr); // for timeout
            expr.evalAsEvents(output, focus, context);
        }
    }

    /**
     * Final stage of the pipeline: handles the evaluation of 'return' and the
     * preceding 'where'. Can be followed by a sort (order by).
     */
    public class Sequence extends SingleSourceSequence
    {
        Focus focus;
        EvalContext context;
        Expression whereFilter;

        XQValue current; // evaluated 'return' expression

        Sequence(XQValue source, Expression whereFilter,
                 Focus focus, EvalContext context)
        {
            super(source);
            this.whereFilter = whereFilter;
            this.focus = focus;
            this.context = context;
            current = XQValue.empty;
        }

        public XQValue bornAgain()
        {
            return new Sequence(source.bornAgain(), whereFilter, focus, context);
        }

        public boolean next()
            throws EvaluationException
        {
            for (;;) {
                // context.at(expr); // for timeout
                if (current.next()) { // pump from 'return' expression
                    item = current.getItem();
                    return true;
                }
                // need to get next item from source
                for (;;) {
                    if (!source.next())
                        return false;
                    if (whereFilter == null
                        || whereFilter.evalEffectiveBooleanValue(focus, context))
                        break;
                }
                // use item from source as current item to eval 'return'
                current = expr.eval(focus, context);
            }
        }
    }

    public class ItemSequence extends SingleSourceSequence
    {
        Focus focus;
        EvalContext context;

        ItemSequence(XQValue source, Focus focus, EvalContext context)
        {
            super(source);
            this.focus = focus;
            this.context = context;
        }

        public XQValue bornAgain()
        {
            return new ItemSequence(source.bornAgain(), focus, context);
        }

        public boolean next()
            throws EvaluationException
        {
            for (;;) {
                if (!source.next())
                    return false;
                if (where == null
                    || where.evalEffectiveBooleanValue(focus, context))
                    break;
            }
            context.at(expr); // for timeout
            // expr evaluates as a single item:
            item = expr.evalAsItem(focus, context);
            return true;
        }

        // Qizx:
        public boolean nextCollection()
            throws EvaluationException
        {
            for (;;) {
                if (!source.nextCollection())
                    return false;
                if (where == null
                    || where.evalEffectiveBooleanValue(focus, context))
                    break;
            }
            // expr evaluates as a single itemegr item:
            item = expr.evalAsItem(focus, context);
            return true;
        }
    }

    public class IntSequence extends IntegerValue
    {
        Focus focus;
        EvalContext context;
        XQValue source; // innermost clause

        long curItem;

        IntSequence(XQValue source, Focus focus, EvalContext context)
        {
            this.source = source;
            this.focus = focus;
            this.context = context;
        }

        public XQValue bornAgain()
        {
            return new IntSequence(source.bornAgain(), focus, context);
        }

        public long getInteger()
        {
            return curItem;
        }

        protected long getValue()
        {
            return curItem;
        }

        public boolean next()
            throws EvaluationException
        {
            for (;;) {
                for (;;) {
                    if (!source.next())
                        return false;
                    if (where == null
                        || where.evalEffectiveBooleanValue(focus, context))
                        break;
                }
                context.at(expr); // for timeout
                try {
                    // expr evaluates as a single integer item: (empty allowed)
                    curItem = expr.evalAsOptInteger(focus, context);
                    // System.err.println("curItem "+curItem);
                    return true;
                }
                catch (EmptyException e) {
                }
            }
        }
    }

    protected class StringSequence extends StringValue
    {
        Focus focus;
        EvalContext context;
        XQValue source; // innermost clause
        String curItem;

        StringSequence(XQValue source, Focus focus, EvalContext context)
        {
            this.source = source;
            this.focus = focus;
            this.context = context;
        }

        public XQValue bornAgain()
        {
            return new StringSequence(source.bornAgain(), focus, context);
        }

        public String getString()
        {
            return curItem;
        }

        protected String getValue()
        {
            return curItem;
        }

        public boolean next()
            throws EvaluationException
        {
            for (;;) {
                for (;;) {
                    if (!source.next())
                        return false;
                    if (where == null
                        || where.evalEffectiveBooleanValue(focus, context))
                        break;
                }
                context.at(expr); // for timeout
                try {
                    // expr evaluates as a single String item: (empty allowed)
                    curItem = expr.evalAsOptString(focus, context);
                    if(curItem != null)
                    	return true;
                }
                catch (EmptyException e) {
                }
            }
        }
    }

    private final class OrderComparator
        implements Comparator
    {
        private final EvalContext context;
    
        private OrderComparator(EvalContext context)
        {
            this.context = context;
        }
    
        public int compare(Object o1, Object o2)
        {
            try {
                XQItem[] tuple1 = (XQItem[]) o1, tuple2 = (XQItem[]) o2;
                int cmp = 0;
                for (int k = 1; k <= orderSpecs.length; k++) {
                    OrderSpec ospec = orderSpecs[k - 1];
                    cmp = ospec.compare(tuple1[k], tuple2[k], context);
                    if (cmp != 0)
                        break;
                }
                return cmp;
            }
            catch (EvaluationException e) {
                throw new RuntimeException(e); // cannot change this interface
            }
        }
    }

    // Acquire all items from the source and sort them in an array.
    // Make each tuple - formed with the return value and the K sort keys -
    // as an array of size 1+K, gather the tuples into the big array,
    // then sort the array and return it.
    private ArraySequence sorted(XQValue source, Focus focus,
                                 final EvalContext context)
        throws EvaluationException
    {
        Object[] items = new Object[8];
        int size = 0;
        int nonKeys = hasScore? 2 : 1;
        
        int K = orderSpecs.length;
        // System.err.println("sort "+source);
        for (; source.next();) {
            XQItem[] tuple = new XQItem[K + nonKeys];
            tuple[0] = source.getItem();
            // System.out.println(size+" sort item "+tuple[0]);
            for (int k = 0; k < K; k++) {
                XQValue key = orderSpecs[k].key.eval(focus, context);
                if (!key.next())
                    tuple[k + 1] = null; // empty
                else {
                    // Atomization must return a single item:
                    tuple[k + 1] = Atomizer.toSingleAtom(key.getItem());
                    if (key.next())
                        context.error(ERRC_BADTYPE, orderSpecs[k],
                                      "order key must be atomic");
                }
            }
            if(hasScore) // OPTIM detect if a key is the score (typical)
                tuple[K + 1] = new SingleDouble(source.getFulltextScore(tuple[0]));
            
            // add to items:
            if (size >= items.length) {
                Object[] old = items;
                items = new Object[old.length * 2];
                System.arraycopy(old, 0, items, 0, old.length);
            }
            items[size++] = tuple;
        }
        try {
            if (size > 1)
                Arrays.sort(items, 0, size, new OrderComparator(context));
        }
        catch (RuntimeException e) {
            if (e.getCause() instanceof EvaluationException)
                throw (EvaluationException) e.getCause();
            throw e;
        }
        
        // replace tuples by the main item:
        double[] scores = hasScore ? new double[size] : null;
        for (int v = size; --v >= 0;) {
            XQItem[] tuple = (XQItem[]) items[v];
            items[v] = tuple[0];
            if(hasScore)
                scores[v] = tuple[K + 1].getDouble();
        }
        ArraySequence asq = new ArraySequence(items, size);
        asq.setOrigin(source);
        asq.setScores(scores);
        return asq;
    }

    /**
     * Implements the grouping.
     * Handles the *preceding* 'where': the post-grouping where is handled by 
     * the normal 'return' sequence.
     */
    private class GroupBySequence extends VarClause.SingleDummy
    {        
        private XQValue input;
        private Expression where;
        private HashMap groups;
        private Iterator groupIterator;

        public GroupBySequence(XQValue input, Expression where,
                               EvalContext context, HashMap groups)
        {
            super(null, context);
            this.input = input;
            this.where = where;
            this.groups = groups;
            if(groups != null)
                groupIterator = groups.keySet().iterator();
        }

        public XQValue bornAgain()
        {
            return new GroupBySequence(input.bornAgain(), where, context, groups);
        }

        public boolean next()
            throws EvaluationException
        {
            if (groups == null) {
                expandAndGroup();
                groupIterator = groups.keySet().iterator();
            }
            if (!groupIterator.hasNext())
                return false;
            // current item is irrelevant
            Group g = (Group) groupIterator.next();
            // store keys and other values into their respective variables:
            for (int i = groupingKeys.length; --i >= 0;) {
                context.storeLocalItem(outGroupingVars[i].address,
                                       g.keyValues[i]);
            }
            for (int i = outNonGroupingVars.length; --i >= 0;) {
                ArraySequence values = g.values[i];
                values.pack();
                context.storeLocal(outNonGroupingVars[i].address,
                                   values, false, null);
            }

            return true;
        }

        private void expandAndGroup() throws EvaluationException
        {
            groups = new HashMap();
            int grKeyCount = groupingKeys.length;
            int ngCount = nonGroupingVars.length;
            Group probe = new Group(grKeyCount, ngCount);
            
            for(; input.next(); ) {
                // get grouping var values into probe
                for(int i = 0; i < grKeyCount; i++) {
                    XQItem it = context.loadLocalItem(groupingKeys[i].decl.address);
                    if(it == null)
                        // hack: not sure what to do
                        it = EMPTY_ITEM;
                    probe.keyValues[i] = it;
                    probe.keys[i] = Atomizer.toSingleAtom(it);
                    
                }
                // lookup in groups
                Group g = (Group) groups.get(probe);
                if(g == null) {
                    g = new Group(grKeyCount, ngCount);
                    for(int i = 0; i < grKeyCount; i++) {
                        g.keys[i] = probe.keys[i];
                        g.keyValues[i] = probe.keyValues[i];
                    }
                    groups.put(g, g);   // AFTER copying keys
                }
                // get values of other vars and append to group
                for (int i = 0; i < ngCount; i++) {
                    XQValue v = context.loadLocal(nonGroupingVars[i].address);
                    if(g.values[i] == null)
                        g.values[i] = new ArraySequence(4, v);
                    g.values[i].append(v);
                }
            }
        }
        
        private class Group
        {
            // key: a list of *atoms*, same order as groupKeys
            XQItem[] keys;
            // original key: a list of *items*, same order as groupKeys
            // mightbe different than keys because of atomization
            XQItem[] keyValues;
            // value: list of sequences, same order as nonGroupingVars
            ArraySequence[] values;
            
            Group(int keyCount, int valueCount) {
                keys = new XQItem[keyCount];
                keyValues = new XQItem[keyCount];
                values = new ArraySequence[valueCount];
            }
            
            public int hashCode()
            {
                int h = 0;
                for (int k = keys.length; --k >= 0; ) {
                    h ^= keys[k].hashCode();
                }
                return h;
            }
            
            public boolean equals(Object obj)
            {
                if(obj instanceof Group) {
                    Group g = (Group) obj;
                    try {
                        for (int k = keys.length; --k >= 0; ) {
                            // TODO? empty / NaN
                            if(keys[k].compareTo(g.keys[k], groupingKeys[k], 0) != 0)
                                return false;
                        }
                    }
                    catch (EvaluationException e) {
                        return false;
                    }
                }
                return true;
            }
        }
    }
    
    // Collects used variables (only the name)
    private static class VarRefCollector extends Visitor
    {
        public HashSet vars = new HashSet();
        
        public boolean preTest(Expression focus)
        {
            if (!(focus instanceof VarReference))
                return true;
            VarReference local = (VarReference) focus;
            vars.add(local.name);
            return true;
        }
    }
}
