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
package com.qizx.queries;

import com.qizx.api.DataModelException;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.api.fulltext.FullTextFactory;
import com.qizx.queries.FullText.MatchOptions;
import com.qizx.queries.iterators.AllIterator;
import com.qizx.queries.iterators.ContainerIterator;
import com.qizx.queries.iterators.CountIterator;
import com.qizx.queries.iterators.MildNotIterator;
import com.qizx.queries.iterators.OrIterator;
import com.qizx.queries.iterators.PostingIterator;
import com.qizx.queries.iterators.PostingIterator.Filter;
import com.qizx.util.StringPattern;
import com.qizx.util.basic.Check;
import com.qizx.xdm.IQName;
import com.qizx.xdm.FONIDataModel.FONINode;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A family of elementary Query descriptors. Such descriptors are used to build
 * a query passed to the {@link XMLLibraryMember#query(Query)} method. The
 * XQuery engine compiles XQuery path expressions into such descriptors.
 * <p>
 * Concrete subclasses implement basic queries on elements, attributes,
 * content, full-text, metadata, as well as boolean combinations.
 * <p>
 * See the Developer's guide for information about how to use these objects.
 */
public abstract class Query
{
    private PostingIterator.Filter[] filters; // optional
    // General depth test, used by Containment and Descendant
    protected int depthTest = -1; // any depth by default

    public int getDepthTest()
    {
        return depthTest;
    }

    public void setDepthTest(int depthTest)
    {
        this.depthTest = depthTest;
    }

    // public stub
    public static PostingIterator realize(Query query, 
                                          Instantiator factory)
    {
        return query.realize(factory);
    }

    //
    // Returns an iterator for this query on this segment.
    // @param selected : set of documents selected for the query
    // @param deleted : documents marked as deleted
    // @param ctx: some evaluation context
    protected abstract PostingIterator realize(Instantiator factory);
    
    // Copy filters to iterator
    protected PostingIterator filtered(PostingIterator iterator)
    {
        if (getFilters() != null)
            iterator.setFilters(getFilters());
        if(depthTest != -1)
            iterator.setDepthTest(depthTest);
        return iterator;
    }

    /**
     * [internal]
     */
    protected String header()
    {
        String name = getClass().getName();
        String shortName = name.substring(name.lastIndexOf('$') + 1);
        return (depthTest != -1)? (shortName + " d=" + depthTest) : shortName;
    }

    /**
     * [internal]
     */
    abstract public void display(Displayer disp);

    public void display()
    {
        new Displayer().display(this);
    }

    public String toString()
    {
        StringWriter sw = new StringWriter();
        new Displayer(new PrintWriter(sw, true)).display(this);
        return sw.toString();
    }
    
    public PostingIterator.Filter[] getFilters()
    {
        return filters;
    }

    /**
     * [internal]
     */
    public void addFilter(PostingIterator.Filter filter)
    {
        if (filters == null)
            filters = new PostingIterator.Filter[] { filter };
        else {
            PostingIterator.Filter[] old = filters;
            filters = new PostingIterator.Filter[old.length + 1];
            System.arraycopy(old, 0, getFilters(), 0, old.length);
            filters[old.length] = filter;
        }
    }

    public void setFilters(PostingIterator.Filter[] filters)
    {
        this.filters = filters;
    }
    
    // --------------------------------------------------------------------
    //      Construction helper methods
    
    public static Query and(Query predicates, Query pred)
    {
        if(predicates == null)
            return pred;
        if(predicates instanceof Query.And) {
            ((Query.And) predicates).add(pred);
            return predicates;
        }
        else if(pred instanceof Query.And) {
            ((Query.And) pred).add(predicates);
            return pred;
        }
        else if(pred == null)
            return predicates;
        return new Query.And(predicates, pred);
    }

    /**
     * Returns the union of queries q1 and q2, with simplification
     * if one of the arguments is null.
     */
    public static Query union(Query q1, Query q2)
    {
        if (q1 == null)
            return q2;
        if (q2 == null)
            return q1;
        Or u = new Query.Or();
        u.add(q1);
        u.add(q2);
        return u;
    }


    // ------------------ specific implementations --------------------------

    // Abstract superclass for binary Queries.
    //
    static abstract class Binary extends Query
    {
        protected Query q1;
        protected Query q2;
    
        public Binary(Query q1, Query q2)
        {
//            Check.nonNull(q1, "q1");
//            Check.nonNull(q2, "q2");
            this.q1 = q1;
            this.q2 = q2;
        }
    
        public void display(Displayer disp)
        {
            disp.println(header());
            disp.display(q1);
            disp.display(q2);
        }
    
        public Query getQuery1()
        {
            return q1;
        }
    
        public void setQuery1(Query q1)
        {
            Check.nonNull(q1, "query1");
            this.q1 = q1;
        }
    
        public Query getQuery2()
        {
            return q2;
        }
    
        public void setQuery2(Query q2)
        {
            Check.nonNull(q2, "query2");
            this.q2 = q2;
        }
    }

    // Abstract superclass for n-ary Queries.
    //
    static abstract class NAry extends Query
    {
        protected Query[] queries;
    
        public NAry()
        {
        }
    
        public Query getQuery(int index)
        {
            return queries[index];
        }
    
        public void add(Query q)
        {
            if(q == null)
                return;
            if(q.getClass() == getClass()) {
                queries = concatenate(queries, ((NAry) q).queries);
            }
            else {
                Query[] old = queries;
                int len = (old == null) ? 0 : old.length;
                queries = new Query[len + 1];
                if(old != null)
                    System.arraycopy(old, 0, queries, 0, len);
                queries[len] = q;
            }
        }

        public static Query[] concatenate(Query[] q1, Query[] q2)
        {
            if(q1 == null)
                return q2;
            if(q2 == null)
                return q1;
            Query[] list = new Query[q1.length + q2.length];
            System.arraycopy(q1, 0, list, 0, q1.length);
            System.arraycopy(q2, 0, list, q1.length, q2.length);
            return list;
        }

        public void display(Displayer disp)
        {
            disp.println(header());
            for (int q = 0; q < queries.length; q++) {
                disp.display(queries[q]);
            }
        }

        protected PostingIterator[] realizeSubQueries(Instantiator factory)
        {
            int nq = queries.length;
            PostingIterator[] iters = new PostingIterator[nq];
            for (int i = 0; i < nq; i++) {
                iters[i] = queries[i].realize(factory);
            }
            return iters;
        }

        public void setDepthTest(int depthTest)
        {
            for (int i = 0; i < queries.length; i++) {
                queries[i].setDepthTest(depthTest);
            }
        }
    }

    // Abstract superclass for Queries with a name.
    //
    static abstract class WithName extends Query
    {
        /**
         * Element or attribute qualified name.
         */
        public IQName name;
    
        WithName(QName name)
        {
            //Check.nonNull(name, "name");
            this.name = (name == null)? null : IQName.get(name);
        }
    }

    
    /**
     * Represents a And predicate in Containment.
     */
    public static class And extends NAry
    {
        public And() { }

        public And(Query q1, Query q2)
        {
            queries = new Query[] { q1, q2 };
        }

        protected PostingIterator realize(Instantiator factory)
        {
            return filtered(new AllIterator(realizeSubQueries(factory)));
        }    
    }

    /**
     * Represents a Or predicate in Containment.
     */
    public static class Or extends NAry
    {
        public Or() { }
        
        public Or(Query q1, Query q2)
        {
            queries = new Query[] { q1, q2 };
        }

        protected PostingIterator realize(Instantiator factory)
        {
            PostingIterator[] subq = realizeSubQueries(factory);
            // simplify if possible:
            if(subq.length == 1 && getFilters() == null && depthTest == -1)
                return subq[0];
            return filtered(new OrIterator(subq));
        }    
    }

    /**
     * Generalized container, replaces Ancestor.
     * 
     */
    public static class Containment extends Binary
    {
        public Containment(Query container, Query contained)
        {
            super(container, contained);
        }

        protected PostingIterator realize(Instantiator factory)
        {
            PostingIterator contained = q2.realize(factory);
            return filtered(new ContainerIterator(q1.realize(factory), contained));
        }
        
        public int getDepthTest()
        {
            return q1.getDepthTest();
        }

        public void setDepthTest(int depthTest)
        {
            // never set depth test on container itself
            q1.setDepthTest(depthTest);
        }


        public void display(Displayer disp)
        {
            disp.println(header());
            disp.display(q1);
            disp.display(q2);
        }
    }
    
    
    /**
     * Query of elements whose string-value contains a match of the fulltext
     * query.
     */
    public static class Fulltext extends Query
    {
        protected Query container;
        protected FullText.Selection ftSelection;
        protected Query ignored;
       
        /**
         * Query of elements whose string-value contains a match of the
         * fulltext query.
         * 
         * @param container defines nodes whose string value is requested to
         *        match the fulltext query.
         * @param selection a parsed fulltext query.
         */
        public Fulltext(Query container, FullText.Selection selection)
        {
            //Check.nonNull(container, "container");
            Check.nonNull(selection, "selection");
            this.container = container;
            ftSelection = selection;
        }

        public Query getIgnored()
        {
            return ignored;
        }

        public void setIgnored(Query ignored)
        {
            this.ignored = ignored;
        }

        public void display(Displayer disp)
        {
            disp.println("FTContains");
            disp.display(container);
            disp.display(ftSelection);
            if(ignored != null)
                disp.display(ignored);
        }

        protected PostingIterator realize(Instantiator factory)
        {
            PostingIterator ftIterator = ftSelection.realize(factory);
            if(container == null)
            {
                if(ignored == null) // most frequent case: . ftcontains ftsel
                    return filtered(ftIterator);

                // 'ignore' is implemented as "eliminate results that are included
                // in ignored", ie MildNot. Not exact, since we can miss results, 
                // but acceptable, since such missed results are unlikely
                // TODO reimplement by filtering at lowest level
                return filtered(new MildNotIterator(ftIterator,
                                                    ignored.realize(factory)));
            }
            
            // general case:
            ContainerIterator cont =
                new ContainerIterator(container.realize(factory), ftIterator);
            if(ignored != null)
                cont.setIgnored(ignored.realize(factory));
            return filtered(cont);
        }

        protected PostingIterator filtered(PostingIterator iterator)
        {
            PostingIterator it = super.filtered(iterator);
            it.setFulltextSelection(ftSelection);
            return it;
        }
    }


    /**
     * Used as predicate in Containment.
     * <p>Returns one occurrence (dummy item) if the underlying (counted) query
     * matches the count constraints.
     */
    public static class Count extends Query
    {
        Query counted;
        int minCount, maxCount;
        
        public Count(Query counted, int minCount, int maxCount)
        {
            this.counted = counted;
            this.minCount = minCount;
            this.maxCount = maxCount;
        }

        protected PostingIterator realize(Instantiator factory)
        {
            return filtered(new CountIterator(minCount, maxCount,
                                               counted.realize(factory)));
        }

        public void display(Displayer disp)
        {
            disp.println(header() + " " + minCount + " " + maxCount);
            disp.display(counted);
        }
    }


    /**
     * Returns the document-nodes (root nodes) of the query domain.
     * <p>
     * The set of documents is implicitly specified by the actual query
     * operation (which can operate on a collection, the whole library or a
     * single document). Can be used in combination with a structural Ancestor
     * or Descendant query.
     */
    public static class Documents extends Query
    {
        public Documents()
        {
        }

        protected PostingIterator realize(Instantiator factory)
        {
            return filtered(factory.enumDocNodes());
        }

        public void display(Displayer disp)
        {
            disp.println(header());
        }
    }

    /**
     * Returns the Document elements of the query domain
     */
    public static class DocumentElements extends Documents
    {
        protected PostingIterator realize(Instantiator factory)
        {
            return filtered(factory.enumDocElements());
        }
    }

    /**
     * A query restricted to a single Node. Can be used in combination with a
     * Ancestor or Descendant query.
     */
    public static class SingleNode extends Query
    {
        protected FONINode node;

        public SingleNode(FONINode node)
        {
            Check.nonNull(node, "node");
            this.node = node;
        }

        protected PostingIterator realize(Instantiator factory)
        {
            return filtered(factory.singleNodeIterator(node));
        }

        public void display(Displayer disp)
        {
            disp.print(header());
            try {
                disp.println(" " + node.getNodeKind() + " " + node.getNodeName());
            }
            catch (DataModelException e) {
                disp.println("DataModelException: " + e.getMessage());
            }
        }
    }

    // --------------------------------------------------------------------
    
    /**
     * Instantiates a basic Query into an iterator.
     * <p>
     * Implemented by AbstractSegment (for XML Libraries) and by TokenStream for
     * brute-force full-text.
     */
    public interface Instantiator
    {
        PostingIterator singleNodeIterator(FONINode node);
    
        PostingIterator enumDocNodes();
    
        PostingIterator enumDocElements();
    
    
        PostingIterator enumWord(char[] word, MatchOptions matchOptions);
    
        PostingIterator enumWildcard(char[] pattern, MatchOptions matchOptions);
        
        FullTextFactory getScoringFactory();
    }


    /**
     * Displays a Query (debugging utility).
     */
    public static class Displayer
    {
        private PrintWriter out;
        int depth = -1;
        boolean bol = true;

        public Displayer()
        {
            out = new PrintWriter(System.err, true);
        }

        public Displayer(PrintWriter out)
        {
            this.out = out;
        }

        public void display(Query q)
        {
            ++depth;
            if(q == null) {
                indent(); println("null?");
                --depth;
                return;
            }
            q.display(this);
            Filter[] filt = q.getFilters();
            if (filt != null)
                for (int f = 0; f < filt.length; f++)
                    println(" +Filter " + filt[f]);
            --depth;
        }

        public void display(FullText.Selection q)
        {
            ++depth;
            if(q == null) {
                indent(); println("!null?");
                return;
            }
            q.display(this);
            this.println("");
            --depth;
        }

        void print(String piece)
        {
            indent();
            out.print(piece);
        }

        void println(String piece)
        {
            indent();
            out.println(piece);
            bol = true;
        }

        void indent()
        {
            if (bol)
                for (int p = 0; p < depth; p++)
                    out.print("   ");
            bol = false;
        }
    }
}

