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
package com.qizx.xquery.impl;

import com.qizx.api.CompilationException;
import com.qizx.util.CharTable;
import com.qizx.util.basic.XMLUtil;
import com.qizx.xquery.ModuleContext;

import java.util.ArrayList;

/**
 *	Lexical analysis (undocumented internal class).
 *	<p>This code was generated from the XML specification, then
 *	completed and cleaned.
 */
public class Lexer
{
    public static final String ERR_SYNTAX = "XPST0003";
    
    private static final char REQ_SKIP = '\u00a0';
    private static final char METACHAR = '¬';
    private static final char META = 128;

    static final int NUMBER = -2;
    static final int SKIP = -1;

    // tokens:
    public static final int T_END = 0;
    
    static final int T_And = 1;
    static final int T_As = 2;
    static final int T_Ascending = 3;
    static final int T_AssignEquals = 4;
    static final int T_At = 5;
    static final int T_AtStringLiteral = 6;
    static final int T_AttributeQNameLbrace = 7;
    static final int T_AttributeLbrace = 8;
    static final int T_AxisAncestor = 9;
    static final int T_AxisAncestorOrSelf = 10;
    static final int T_AxisAttribute = 11;
    static final int T_AxisChild = 12;
    static final int T_AxisDescendant = 13;
    static final int T_AxisDescendantOrSelf = 14;
    static final int T_AxisFollowing = 15;
    static final int T_AxisFollowingSibling = 16;
    static final int T_AxisNamespace = 17;
    static final int T_AxisParent = 18;
    static final int T_AxisPreceding = 19;
    static final int T_AxisPrecedingSibling = 20;
    static final int T_AxisSelf = 21;
    static final int T_Case = 22;
    static final int T_CastAs = 23;
    static final int T_CastableAs = 24;
    static final int T_Char = 25;
    static final int T_CharRef = 26;
    static final int T_CloseApos = 27;
    static final int T_CloseQuot = 28;
    static final int T_Collation = 29;
    static final int T_ColonEquals = 30;
    static final int T_Comma = 31;
    static final int T_Comment = 32;
    static final int T_CommentLpar = 33;
    static final int T_DecimalLiteral = 34;
    static final int T_DeclareNamespace = 35;
    static final int T_Default = 36;
    static final int T_DefaultCollation = 37;
    static final int T_DefaultElement = 38;
    static final int T_DefaultFunction = 39;
    static final int T_DeclareFunction = 40;
    static final int T_Descending = 41;
    static final int T_Div = 42;
    static final int T_DocumentLbrace = 43;
    static final int T_Dot = 44;
    static final int T_DotDot = 45;
    static final int T_DoubleLiteral = 46;
    static final int T_ElementLbrace = 47;
    static final int T_ElementQNameLbrace = 48;
    static final int T_ElementType = 49;
    static final int T_Else = 50;
    static final int T_Empty = 51;
    static final int T_EmptyGreatest = 52;
    static final int T_EmptyLeast = 53;
    static final int T_EmptyTagClose = 54;
    static final int T_EndTag = 55;
    static final int T_Equals = 56;
    static final int T_Every = 57;
    static final int T_Except = 58;
    static final int T_ForVariable = 59;
    static final int T_FortranEq = 60;
    static final int T_FortranGe = 61;
    static final int T_FortranGt = 62;
    static final int T_FortranLe = 63;
    static final int T_FortranLt = 64;
    static final int T_FortranNe = 65;
    static final int T_Gt = 66;
    static final int T_GtEquals = 67;
    static final int T_GtGt = 68;
    static final int T_Global = 69;
    static final int T_Idiv = 70;
    static final int T_IfLpar = 71;
    static final int T_ImportSchema = 72;
    static final int T_In = 73;
    static final int T_InContext = 74;
    static final int T_Instanceof = 75;
    static final int T_IntegerLiteral = 76;
    static final int T_Intersect = 77;
    static final int T_Is = 78;
    static final int T_IsNot = 79;
    static final int T_Item = 80;
    static final int T_Lbrace = 81;
    static final int T_LbraceExprEnclosure = 82;
    static final int T_Lbrack = 83;
    static final int T_LetVariable = 84;
    static final int T_Lpar = 85;
    static final int T_Lt = 86;
    static final int T_LtEquals = 87;
    static final int T_LtLt = 88;
    static final int T_Minus = 89;
    static final int T_Mod = 90;
    static final int T_Multiply = 91;
    static final int T_NCName = 92;
    static final int T_NCNameColonStar = 93;
    static final int T_Namespace = 94;
    static final int T_Node = 95;
    static final int T_NodeLpar = 96;
    static final int T_NotEquals = 97;
    static final int T_OfType = 98;
    static final int T_OpenApos = 99;
    static final int T_OpenQuot = 100;
    static final int T_Or = 101;
    static final int T_OrderBy = 102;
    static final int T_OrderByStable = 103;
    static final int T_Plus = 104;
    static final int T_Prefix = 105;
    static final int T_ProcessingInstruction = 106;
    static final int T_ProcessingInstructionLpar = 107;
    static final int T_Pragma = 108;
    static final int T_Extension = 109;
    static final int T_QMark = 110;
    static final int T_QName = 111;
    static final int T_QNameLpar = 112;
    static final int T_Rbrace = 113;
    static final int T_Rbrack = 114;
    static final int T_Return = 115;
    static final int T_Root = 116;
    static final int T_RootDescendants = 117;
    static final int T_Rpar = 118;
    static final int T_RparAs = 119;
    static final int T_S = 120;
    static final int T_Satisfies = 121;
    static final int T_SemiColon = 122;
    static final int T_Slash = 123;
    static final int T_SlashSlash = 124;
    static final int T_Some = 125;
    static final int T_Star = 126;
    static final int T_StarColonNCName = 127;
    static final int T_TagClose = 128;
    static final int T_StartTagOpen = 129;
    static final int T_StartTagOpenRoot = 130;
    static final int T_StringLiteral = 131;
    static final int T_TagQName = 132;
    static final int T_Text = 133;
    static final int T_TextLbrace = 134;
    static final int T_TextLpar = 135;
    static final int T_Then = 136;
    static final int T_To = 137;
    static final int T_TreatAs = 138;
    static final int T_TypeQName = 139;
    static final int T_TypeswitchLpar = 140;
    static final int T_URLLiteral = 141;
    static final int T_Union = 142;
    static final int T_Untyped = 143;
    static final int T_ValidateContext = 144;
    static final int T_Validate = 145;
    static final int T_ValueIndicator = 146;
    static final int T_VarName = 147;
    static final int T_Vbar = 148;
    static final int T_Where = 149;
    static final int T_WhitespaceChar = 150;
    static final int T_XmlComment = 151;
    static final int T_Cdata = 152;     // fix: distinguish from plain Chars
    
    // May 2003 draft: 
    static final int T_Module = 153;
    static final int T_DocumentNodeLpar = 154;
    static final int T_ElementLpar = 155;
    static final int T_AttributeLpar = 156;
    static final int T_ItemLparRpar = 157;
    static final int T_XQueryVersion = 158;
    static final int T_ImportModule = 159;
    static final int T_DeclareVariable = 160;
    static final int T_External = 161;
    static final int T_ValidationStrict = 162;
    static final int T_ValidationSkip = 163;
    static final int T_ValidationLax = 164;
    
    // Aug 2003 draft:
    static final int T_CommentLbrace = 166;
    static final int T_NamespaceLbrace = 167;
    static final int T_PILbrace = 168;
    static final int T_PINameLbrace = 169;
    
    // 2004 drafts: 
    static final int T_DeclareBaseURI = 171;
    static final int T_DeclareConstruction = 172;
    static final int T_DeclareOrdering = 173;
    static final int T_Ordered = 174;
    static final int T_Unordered = 175;
    
    // Heretic extensions: 
    static final int T_Template = 179;
    static final int T_CallTemplate = 180;
    
    // nov 2005 Candidate Recommendation 
    static final int T_DeclareOption = 182;
    static final int T_DeclareBoundarySpace = 183;
    static final int T_DeclareDefaultOrder = 184;
    static final int T_DeclareCopyNamespaces = 185;
    
    // New full-text: 
    static final int T_FTContains = 187;
    static final int T_FTOr = 188;
    static final int T_FTAnd = 189;
    static final int T_FTNotIn = 190;
    static final int T_FTNot = 191;
    static final int T_All = 192;
    static final int T_AllWords = 193;
    static final int T_Any = 194;
    static final int T_AnyWord = 195;
    static final int T_Phrase = 196;
    static final int T_AtEnd = 197;
    static final int T_AtLeast = 198;
    static final int T_AtMost = 199;
    static final int T_AtStart = 200;
    static final int T_CaseInsensitive = 201;
    static final int T_CaseSensitive = 202;
    static final int T_DiacriticsInsensitive = 203;
    static final int T_DiacriticsSensitive = 204;
    static final int T_Times = 205;
    static final int T_Distance = 206;
    static final int T_EntireContent = 207;
    static final int T_Exactly = 208;
    static final int T_From = 209;
    static final int T_Language = 210;
    static final int T_Levels = 211;
    static final int T_Lowercase = 212;
    static final int T_Occurs = 213;
    static final int T_Relationship = 214;
    static final int T_SameParagraph = 215;
    static final int T_SameSentence = 216;
    static final int T_DifferentParagraph = 217;
    static final int T_DifferentSentence = 218;
    static final int T_Paragraphs = 219;
    static final int T_Sentences = 220;
    static final int T_Uppercase = 221;
    static final int T_Window = 222;
    static final int T_WithDefaultStopWords = 223;
    static final int T_WithStopWords = 224;
    static final int T_WithThesaurus = 225;
    static final int T_WithWildcards = 226;
    static final int T_WithStemming = 227;
    static final int T_WithoutContent = 228;
    static final int T_WithoutStemming = 229;
    static final int T_WithoutStopWords = 230;
    static final int T_WithoutThesaurus = 231;
    static final int T_WithoutWildcards = 232;
    static final int T_Words = 233;
    static final int T_Weight = 234;
    static final int T_Scope = 235;
    static final int T_DeclareFTOption = 236;
    static final int T_Score = 237;
    static final int T_LetScoreVariable = 238;
    static final int T_FTExtension = 239;
    
    // XQuery Update 
    static final int T_InsertNodes = 240;
    static final int T_DeleteNodes = 241;
    static final int T_ReplaceNode = 242;
    static final int T_ReplaceValueOfNode = 243;
    static final int T_RenameNode = 244;
    static final int T_Copy = 245;
    static final int T_Modify = 246;
    static final int T_AsFirstInto = 247;
    static final int T_AsLastInto = 248;
    static final int T_Into = 249;
    static final int T_After = 250;
    static final int T_Before = 251;
    static final int T_With = 252;
    static final int T_DeclareUpdatingFunction = 253;
    static final int T_Dummy = 254;
    
    // XQuery 1.1
    static final int T_ForTumbling = 255;
    static final int T_ForSliding = 256;
    static final int T_Start = 257;
    static final int T_End = 258;
    static final int T_OnlyEnd = 259;
    static final int T_When = 260;
    static final int T_Previous = 261;
    static final int T_Next = 262;
    static final int T_GroupBy = 263;
    static final int T_Try = 264;
    static final int T_Catch = 265;
    static final int T_Switch = 266;
    static final int T_Function = 267;
    static final int T_AllowingEmpty = 268;
    static final int T_Count = 269;
    static final int T_ContextItem = 270;
    
    // XQuery Scripting
    static final int T_Declare = 280;   // now generic
    static final int T_VarAssign = 281;
    static final int T_While = 282;
    static final int T_Exit = 283;
    static final int T_Block = 284;
    static final int T_AssignableFlag = 285;
    static final int T_DeterministicFlag = 286;
    static final int T_UndeterministicFlag = 287;
    static final int T_PrivateFlag = 288;
    static final int T_PublicFlag = 289;
    static final int T_SimpleFlag = 290;
    static final int T_UnassignableFlag = 291;
    static final int T_UpdatingFlag = 292;
    static final int T_SequentialFlag = 293;
    
    // -----------------------------------------------------------------------
    
    static CharTable tokenNames = new CharTable();
    
    static State defState, declareState, opState, vardeclState, itemTypeState;
    static State nsDeclState, occIndState, kindTestState;
    static State tagState, contentState, attrApostState, attrQuoteState;
    static State thesaurusState;

    // special state meaning 'pop current state'
    static State POP_STATE = new State("pop!");
    static State ERROR_STATE = new State("error!");

    // lexical categories:
    public final static int LC_TAG = 1;
    public final static int LC_SPACE = 2;
    public final static int LC_NUMBER = 3;
    public final static int LC_STRING = 4;
    public final static int LC_MISC = 5;
    public final static int LC_NAME = 6;
    public final static int LC_KEYWORD = 7;
    public final static int LC_COMMENT = 8;
    public final static int LC_PRAGMA = 9;
    public final static int LC_FUNCTION = 10;


    public static boolean debug = false;
    protected static boolean trace = false;

    protected boolean showComments = false; // show comments as tokens

    /* package private */ ModuleContext  currentModule;
    protected String source;
    protected int   inputLength;
    protected char  curChar;
    protected int   curPtr;    // token start:
    
    // look-ahead position: moved by pickXXX() and next(), reset to curPtr by back()
    protected int   curToken = -1;
    protected int   tokenStart;	
    protected int   tokenEnd;	
    protected int   prevTokenLoc;

    protected State lexState;
    protected int   stateSP;
    protected State[] states = new State[8];
    
    protected StringBuffer saveBuffer = new StringBuffer();
    protected String prefixValue;   // of parsed qname
    protected String localName;     // of parsed qname
    protected int    numberToken;   // code of numeric literal (double int dec)

    
    /**
     * Lexical state. Pure set of rules created once and for all.
     *  Readonly, doesnt contain any runtime state.
     */
    private static class State
    {
        String name;
        CharTable mainSwitch;
        Rule fallbackRule;
        boolean optSkip;
        boolean allowsComments;
        
        State(String name)
        {
            this.name = name;
            mainSwitch = new CharTable();
            optSkip = allowsComments = true;
        }

        void addRule(String form, State nextState, int token)
        {
            addRule(form, nextState, token, null, null);
        }

        void addRule(String form, State nextState, int token,
                            State pushedState)
        {
            addRule(form, nextState, token, pushedState, null);
        }

        void addRule(String form, State nextState, int token,
                            State pushedState, String argument)
        {
            Rule rule = new Rule(form, nextState, pushedState, token, argument);
            if(form.length() == 0) {
                // special rule used when no match is found
                fallbackRule = rule;
            }
            else {
                char first = form.charAt(0);
                if(first == METACHAR || first == ' ')
                    first = META;
                ArrayList rules = (ArrayList) mainSwitch.get(first);
                if(rules == null)
                    mainSwitch.put(first, rules = new ArrayList());
                rules.add(rule);
            }
        }
        
        boolean scan(Lexer lexer) throws CompilationException
        {
            if(this.optSkip)
                lexer.optSkip(allowsComments);
            lexer.tokenStart = lexer.curPtr - 1;
            char first = lexer.curChar;
            // try specific rules:
            ArrayList rules = (ArrayList) mainSwitch.get(first);
            if(rules != null) {
                for (int i = 0; i < rules.size(); i++) {
                    Rule rule = (Rule) rules.get(i);
                    if(lexer.match(rule, this)) {
                        return true;
                    }
                }
            }
            // try generic rules
            rules = (ArrayList) mainSwitch.get(META);
            if(rules != null) {
                for (int i = 0; i < rules.size(); i++) {
                    Rule rule = (Rule) rules.get(i);
                    if(lexer.match(rule, this)) {
                        return true;
                    }
                }
            }
            if(fallbackRule != null)
                return lexer.match(fallbackRule, this);
            return false;   // no match
        }

        public String toString() {
            return "State "+name;
        }        
    }

    /**
     * A rule is a simplified regexp with state transition instructions
     */
    private static class Rule
    {
        String form;        // sequence of immediate chars or special codes
        State nextState;    // when matched; special case POP_STATE
        State pushedState;  // if not null, push a state
        int token;
        String argument;    //
        
        public Rule(String form, State nextState, State pushedState,
                    int token, String argument)
        {
            this.nextState = nextState;
            this.pushedState = pushedState;
            this.token = token;
            this.argument = argument;
            
            // some preprocessing: 
            // - opt space between 2 letters: rewritten as a required space
            // - namechar at end: add a test ¬k
            int flen = form.length();
            StringBuffer tmp = new StringBuffer(flen);
            for(int i = 0, len = flen; i < len; i++) {
                char c = form.charAt(i);
                if(c == ' ' 
                   && i > 1 && Character.isLetter(form.charAt(i - 1))
                   && !(i > 2 && form.charAt(i - 2) == METACHAR)
                   && i+1 < len && Character.isLetter(form.charAt(i + 1)))
                    c = REQ_SKIP;
                tmp.append(c);
            }
            if(flen >= 1 && Character.isLetter(form.charAt(flen-1))
               && !(flen >= 2 && form.charAt(flen - 2) == METACHAR))
                tmp.append("¬k");
            this.form = tmp.toString();
            if(token > 0)
                tokenNames.put(token, form);
        }
    }

    
    public Lexer()
    {
    }

    public void startLexer(String input)
    {
        this.source = input;
        inputLength = input.length();
        lexState = defState;
        stateSP = 0;
        tokenEnd = 0;
        next();
        if (debug)
            System.err.println("===== Start lexer");
    }

    

    protected void wantToken(int token)
        throws CompilationException
    {
        checkToken(token);
        nextToken();
    }

    protected void checkToken(int token)
        throws CompilationException
    {
        if (curToken != token)
            syntax("expecting " + tokenName(token));
    }

    protected boolean pickToken(int token)
        throws CompilationException
    {
        if (curToken != token)
            return false;
        nextToken();
        return true;
    }

    protected int nextToken()
        throws CompilationException
    {
        prevTokenLoc = tokenStart;
        return curToken = getToken();
    }

    protected String tokenName(int token)
    {
        String t = (String) tokenNames.get(token);
        if(t == null)
            return "";
        return t.replaceAll("¬Q", "[qname]").replaceAll("¬N", "[name]")
                .replaceAll("¬\\?", "").replaceAll("¬S", "[string]");
    }

    protected int getToken()
        throws CompilationException
    {
        for(;;) {
            if (debug) {
                System.err.print("In " + lexState.name
                                 + "\tat " + curPtr + " ");
                if (curChar <= ' ')
                    System.err.print("#" + (int) curChar + " ");
                else
                    System.err.print(((char) curChar) + "   ");
            }

            saveBuffer.setLength(0);
            tokenStart = tokenEnd;
            if (curChar == 0) {
                tokenEnd = inputLength;
                return T_END;
            }
            if(!lexState.scan(this))
                lexicalError();
            if(curToken == NUMBER)
                return storeToken(numberToken);
            else if(curToken != SKIP)
                return storeToken(curToken);
            // else try again
        }
    }
    

    private int storeToken( int token )
    {
        if(debug) 
            System.err.println("\t=> Token " + token + " `"
                               + tokenName(token) + "' ["
                               + tokenStart + "-" + curPtr + "]"
                               + (saveBuffer.length() > 0 ? 
                                      (" '" + saveBuffer + "'") : "")                               
                               + " new state " +
                                 (lexState == null? null : lexState.name));
        return curToken = token;
    }

    // match a rule against the input
    public boolean match(Rule rule, State state) throws CompilationException
    {
        String form = rule.form;
        if(trace)
            System.err.println("try rule " + rule.form);
        curPtr = tokenStart;    // restart from here on each rule
        int lastPos = -1;
        next();
        for(int i = 0, len = form.length(); i < len; i++) {
            char rc = form.charAt(i);
            if(rc == ' ') {
                if(!optSkip(state.allowsComments))
                    return false;
            }
            else if(rc == REQ_SKIP) {
                if(!reqSkip(state.allowsComments))
                    return false;
            }
            else if(rc != METACHAR) {
                if(rc != curChar) {
                    if(trace)
                        System.err.println(" fail at "+i+" on "+curChar);
                    return false;
                }
                next();
            }
            else {
                rc = form.charAt(++i);  // no need to test
                switch(rc) {
                case 'C':   // comment tail: only in 'showComments' mode
                    if(!showComments)
                        return false;
                    parseComment();
                    break;
                case 'c':   // cdata section tail
                    parseCDATASection();
                    break;
                case 'u':   // number: actual token stored into numberToken
                    if(!pickNumber())
                        return false;
                    break;
                case 'N':   // simple name
                    if(!pickName())
                        return false;
                    break;
                case 'P':   // Pragma tail
                    parsePragma();
                    break;
                case 'p':   // PI tail
                    parseProcessingInstruction();
                    break;
                case 'Q':   // Qname
                    if(!pickQName())
                        return false;
                    break;
                case 'S':   // String literal
                    if(!pickStringLiteral())
                        return false;
                    break;
                case 'E':   // after a &
                    parseCharRef();
                    break;
                case 't':   //  sequence of content chars
                    if(!pickContent((char) 0))
                        return false;
                    break;
                case 'a':   //  sequence of attr chars
                case 'A':   //  
                    if(!pickContent(rc == 'A'? '"' : '\''))
                        return false;
                    break;
                case 'X':   // XML comment
                    parseXMLComment();
                    break;
                case '?':   // enter test mode
                    lastPos = curPtr - 1;
                    break;
                case 'k':   // test keyword, ie end of name
                    if(curChar != 0 && isNameChar(curChar))
                        return false;
                    break;
                case '+':   // save latest char
                    if(curPtr >= 2)
                        saveBuffer.append(source.charAt(curPtr - 2));
                    break;
                default:
                    lexicalError("no such instr "+(char)rc);
                }
            }
        }
        // matched: store token from rule
        curToken = rule.token;
        if(rule.nextState == ERROR_STATE) {
            backChar();
            lexicalError(rule.argument);
        }
        if(rule.nextState == POP_STATE)
            popState();
        else
            lexState = rule.nextState;
        if(rule.pushedState != null)
            pushState(rule.pushedState);
        tokenEnd = curPtr - 1;
        if(lastPos >= 0) {  // met a test instruction ¬?
            curPtr = lastPos;
            next();
        }
        if(trace)
            System.err.println("matched rule " + rule.form);
        return true;
    }
    
    private int parseProcessingInstruction()
        throws CompilationException
    {
        saveBuffer.setLength(0);

        for (;;) {
            while (curChar != '?' && curChar != 0) {
                save(curChar);
                next();
            }
            if (curChar == 0)
                lexicalError("unterminated processing instruction");
            if (pick("?>"))
                break;
            save(curChar);
            next();
        }
        return T_ProcessingInstruction;
    }

    private int parseXMLComment()
        throws CompilationException
    {
        saveBuffer.setLength(0);
        for (;;) {
            while (curChar != '-' && curChar != 0) {
                save(curChar);
                next();
            }
            if (curChar == 0)
                lexicalError("unterminated XML comment");
            if (pick("-->"))
                break;
            save(curChar);
            next();
        }
        return T_XmlComment;
    }
    
    private void parseCDATASection() throws CompilationException
    {
        saveBuffer.setLength(0);
        for (;;) {
            while (curChar != ']' && curChar != 0) {
                save(curChar);
                next();
            }
            if (curChar == 0)
                lexicalError("unterminated CDATA section");
            if (pick("]]>"))
                break;
            save(curChar);
            next();
        }
    }
    
    private boolean pickNumber()
        throws CompilationException
    {
        saveBuffer.setLength(0);
        numberToken = T_IntegerLiteral;
        while (Character.isDigit(curChar)) {
            save(curChar);
            next();
        }
        if (curChar == '.') {
            save(curChar);
            next();
            while (Character.isDigit(curChar)) {
                save(curChar);
                next();
            }
            if(saveBuffer.length() == 1) {  // SPECIAL CASE
                numberToken = T_Dot;
                return true;
            }
            numberToken = T_DecimalLiteral;
        }
        if(saveBuffer.length() == 0)
            return false;
        if (curChar == 'e' || curChar == 'E') {
            save(curChar);
            next();
            if (curChar == '+' || curChar == '-') {
                save(curChar);
                next();
            }
            if (!Character.isDigit(curChar))
                lexicalError("expecting digits in exponent");
            while (Character.isDigit(curChar)) {
                save(curChar);
                next();
            }
            numberToken = T_DoubleLiteral;
        }
        if (Character.isLetter(curChar))
            lexicalError("numeric literal must not be followed by name");
        if (saveBuffer.length() == 1 && saveBuffer.charAt(0) == '.')
            return false;
        return true;
    }

    private boolean pickStringLiteral() throws CompilationException
    {
        if(curChar != '"' && curChar != '\'')
            return false;
        saveBuffer.setLength(0);
        char delim = curChar;
        next();
        for( ; ; ) {
            if(curChar == 0) {
                lexicalError("unclosed string literal");
                return true;
            }
            else if(curChar == delim) {
                next();
                if(curChar != delim)
                    break;
                else save(delim);
            }
            else if(curChar == '&') {	// char refs: new in may2003
                next();
                parseCharRef();
                continue; // dont do next()
            }
            else save(curChar);
            next();
        }
        return true;
    }

    // sequence of characters, except char refs
    private boolean pickContent(char delim) throws CompilationException
    {
        saveBuffer.setLength(0);
        while(curChar != 0 && curChar != '<' && curChar != '{' && curChar != '}'
              && curChar != '&' && curChar != delim)
        {
            save(curChar);
            next();
        }
        return saveBuffer.length() > 0;
    }

    private void parseCharRef() throws CompilationException
    {
        if(pick('#')) {
            int code = 0, digit;
            if(pick('x')) {
                while( (digit = Character.digit(curChar, 16)) >= 0 ) {
                    code = 16 * code + digit; next();
                }
            }
            else while( (digit = Character.digit(curChar, 10)) >= 0 ) {
                code = 10 * code + Character.digit(curChar, 10); next();
            }
            if(code > 0 && code < 0xfffe)
                save( (char) code );
            else if(XMLUtil.isSupplementalChar(code)) {
                save(XMLUtil.highSurrogate(code));
                save(XMLUtil.lowSurrogate(code));
            }
            else
                lexicalError("illegal character reference #" + code);            
        }
        else if(pickName("lt"))
            save('<');
        else if(pickName("gt"))
            save('>');
        else if(pickName("amp"))
            save('&');
        else if(pickName("quot"))
            save('"');
        else if(pickName("apos"))
            save('\'');
        else lexicalError("illegal character reference");
        if(!pick(';'))
            lexicalError("character reference not terminated by semicolon");
    }

    private void parseComment() throws CompilationException {
        for(;;) {
            if(curChar == 0)
                lexicalError("unclosed comment");
            else if(pick(":)"))
                break;
            else if(pick("(:"))
                parseComment();	// embedding allowed (May 2003)
            else next();
        }
    }

    // parses a pragma, store QName into prefixValue/localName 
    // and contents into saveBuffer
    private int parsePragma() throws CompilationException
    {
        while(Character.isWhitespace(curChar))
            next();
        if(!pickQName())
            lexicalError("pragma should begin with a QName");
        saveBuffer.setLength(0);
        for(;; next()) {
            if(curChar == 0)
                lexicalError("unclosed pragma");
            else if(pick("#)"))
                break;
            save(curChar);
        }
        if(saveBuffer.length() > 0 && !Character.isWhitespace(saveBuffer.charAt(0)))
            lexicalError("whitespace required before pragma content");
        return T_Pragma;
    }

    // Look for a QName, store it into prefixValue and localName
    private boolean pickQName()
    {
        if(!pickName())
            return false;
        prefixValue = "";
        if(pick(':')) {
            prefixValue = saveBuffer.toString();
            if(!pickName()) {   
                backChar();
                prefixValue = "";   // saveBuffer unaltered
                return true;
            }
        }
        localName = saveBuffer.toString();
        return true;
    }
    
    // Gets a NCname, store it into saveBuffer
    private boolean pickName()
    {
        if(!isNameStart(curChar))
            return false;
        saveBuffer.setLength(0);
        do {
            save(curChar);
            next();
        }
        while(curChar > ' ' && isNameChar(curChar));
        return true;
    }
    
    // check for a ncname, if found then skip
    private boolean pickName(String name)
    {
        int save = curPtr;
        if (!pick(name)) {
            return false;
        }
        // check that there is no namechar:
        if (isNameChar(curChar)) {
            curPtr = save - 1;
            next();
            return false;
        }
        return true;
    }
    
    private boolean pick(char c)
    {
        if (curChar != c)
            return false;
        next();
        return true;
    }

    private boolean pick(String s)
    {
        // - System.out.println("?pick "+s+" at "+curPtr+" "+(int)curChar);
        if (curChar != s.charAt(0) || curPtr + s.length() - 1 > inputLength)
            return false;
        int save = curPtr;
        for(int i = s.length(); --i >= 1; )
            if(source.charAt(curPtr + i - 1) != s.charAt(i)) {
                curPtr = save - 1; next();
                return false;
            }
        curPtr += s.length() - 1;
        next();
        
        return true;
    }
    
    private boolean isNameStart(char c) {
        return c == '_' || Character.isLetter(c);
    }
    
    private boolean isNameChar(char c) {
        return Character.isUnicodeIdentifierPart(c) || c == '.' || c == '-';
    }
    
   
    private int next()
    {
        if (curPtr < inputLength)
            curChar = source.charAt(curPtr++);
        else {
            curChar = 0;
            curPtr = inputLength + 1; // as if there were a EOF char
        }
        return curChar;
    }
    
    void backChar()
    {
        --curPtr;
        curChar = curPtr == 0 ? '\n' : source.charAt(curPtr - 1);
    }

    private boolean optSkip(boolean allowsComments) throws CompilationException
    {
        for(;;) {
            while(Character.isWhitespace(curChar))
                next();
            if(pick("{--")) {
                lexicalError("old-style comment");
            }
            if(!showComments && pick("(:") && allowsComments)
                parseComment();
            else
                return true;
        }
    }
    
    private boolean reqSkip(boolean allowsComments) throws CompilationException
    {
        if(!Character.isWhitespace(curChar)) {
            if(pick("{--"))  // keep old comments for compatibility
                lexicalError("old-style comment");
            if(!showComments && pick("(:") && allowsComments)
                parseComment();
            else
                return false;
        }
        return optSkip(allowsComments);	// always returns true
    }

    private void save(char c)
    {
        saveBuffer.append(c);
    }
    
    private void pushState(State state)
    {
        if (stateSP >= states.length) {
            State[] old = states;
            states = new State[2 * old.length];
            System.arraycopy(old, 0, states, 0, old.length);
        }
        states[stateSP++] = state;
        if (debug) {
            System.err.print("push " + state.name + " -> ");
            dumpStates();
        }
    }
    
    private void popState()
    {
        if (debug) {
            System.err.print("pop ");
            dumpStates();
        }
        if (stateSP > 0)
            lexState = states[--stateSP];
        else if (debug)
            System.err.println("*** sp=0");
    }

    private void dumpStates()
    {
        System.err.print("in state " + lexState.name);
        System.err.print("  stack ");
        for (int s = 0; s < stateSP; s++)
            System.err.print(" " + states[s].name);
        System.err.println();
    }

    private void lexicalWarning(String msg)
    {
        if (currentModule != null)
            currentModule.warning(curPtr - 1, "lexical warning: " + msg);
    }

    private void lexicalError()
        throws CompilationException
    {
        lexicalError("illegal character");
    }

    private boolean lexicalError(String msg)
        throws CompilationException
    {
        String cc =
            curChar == 0 ? "<end-of-text>" : curChar <= ' '
                ? ("#" + (int) curChar) : ("'" + curChar + "'");
        if (currentModule != null)
            currentModule.error(ERR_SYNTAX, curPtr - 1,
                                "lexical error on character " + cc + ": " + msg);
        throw new CompilationException(msg + " " + cc);
    } 
    
    protected void syntax(String message)
        throws CompilationException
    {
        currentModule.error(ERR_SYNTAX, tokenStart, "syntax error, near "
                            + tokenName(curToken)
                            + " : " + message);
        throw new CompilationException("syntax error, " + message);
    }

    
    static {        
        // states created all together because of references
        defState = new State("DEFAULT");
        declareState = new State("DECLARE");
        opState = new State("OPERATOR");
        itemTypeState = new State("ITEMTYPE");
        tagState = new State("TAG");
        tagState.allowsComments = false;
        contentState = new State("ECONTENT");
        contentState.allowsComments = false;
        contentState.optSkip = false;
        attrApostState = new State("ATTR'");
        attrApostState.optSkip = false;
        attrQuoteState = new State("ATTR\"");
        attrQuoteState.optSkip = false;
        kindTestState = new State("KINDTEST");
        nsDeclState = new State("NSDECL");
        vardeclState = new State("VARDECL");
        occIndState = new State("OCCINDIC");
        thesaurusState = new State("THESAURUS");
        
        // rules: order is important, a more specific rule must come first
        
        defState.addRule("\0", null, T_END);
        defState.addRule("¬S", opState, T_StringLiteral);
        defState.addRule("¬u",  opState, NUMBER);
        defState.addRule("¬N:*", opState, T_NCNameColonStar);
        defState.addRule("¬Q (", defState, T_QNameLpar);
        defState.addRule("¬Q", opState, T_QName);
        //defState.addRule("$ ¬Q :=", defState, T_VarAssign);
        defState.addRule("$ ¬Q", opState, T_VarName);
        defState.addRule("(#¬P", opState, T_Pragma);
        defState.addRule("(:¬C", defState, T_Comment);   // conditional
        defState.addRule("(", defState, T_Lpar);
         // bloody ambiguities here:
        defState.addRule(") ¬?as first ", opState, T_Rpar);
        defState.addRule(") ¬?as last ", opState, T_Rpar);
        defState.addRule(") as", itemTypeState, T_RparAs);
        defState.addRule(")",    opState, T_Rpar);
        
        defState.addRule("*:¬N", opState, T_StarColonNCName);
        defState.addRule("*", opState, T_Star);
        defState.addRule("+", defState, T_Plus);
        defState.addRule(",", defState, T_Comma);
        defState.addRule("-", defState, T_Minus);
        defState.addRule("..",  opState, T_DotDot);
        //defState.addRule(".",  opState, T_Dot); // parsed by NUMBER ¬u
        defState.addRule("//", defState, T_RootDescendants);
        defState.addRule("/", defState, T_Root);
        defState.addRule(";", defState, T_SemiColon);
        defState.addRule("<!--¬X", opState, T_XmlComment);
        defState.addRule("<![CDATA[¬c", opState, T_Cdata);
        defState.addRule("<?¬p", opState, T_ProcessingInstruction);
        defState.addRule("<¬Q", tagState, T_StartTagOpen, opState); // push
        defState.addRule("?", opState, T_QMark);
        defState.addRule("@", defState, T_At);
        defState.addRule("[", defState, T_Lbrack);
        defState.addRule("]", opState, T_Rbrack);
        defState.addRule("{", defState, T_LbraceExprEnclosure, opState); // push
        defState.addRule("}", POP_STATE, T_Rbrace);
        
        defState.addRule("ancestor ::", defState, T_AxisAncestor);
        defState.addRule("ancestor-or-self ::", defState, T_AxisAncestorOrSelf);
        defState.addRule("at ¬S", opState, T_AtStringLiteral);
        defState.addRule("at least", defState, T_AtLeast);
        defState.addRule("at most", defState, T_AtMost);
        defState.addRule("attribute ::", defState, T_AxisAttribute);
        defState.addRule("attribute ¬Q {", defState, T_AttributeQNameLbrace, opState); // push
        defState.addRule("attribute {", defState, T_AttributeLbrace, opState);
        defState.addRule("attribute (", defState, T_AttributeLpar);

        defState.addRule("block {", defState, T_Block, opState);
        
        defState.addRule("child ::", defState, T_AxisChild);
        defState.addRule("comment (", defState, T_CommentLpar);
        defState.addRule("comment {", defState, T_CommentLbrace, opState);
        defState.addRule("call template", opState, T_CallTemplate);
        defState.addRule("copy ¬?$", vardeclState, T_Copy); // look ahead

        // declare -> DECLARE_STATE then all declare XX in that state
        defState.addRule("declare ¬?¬Q", declareState, T_Declare);   // *****
        defState.addRule("declare ¬?$", vardeclState, T_Declare);   // *****
        
        defState.addRule("descendant-or-self ::", defState, T_AxisDescendantOrSelf);
        defState.addRule("descendant ::", defState, T_AxisDescendant);
        defState.addRule("document {", defState, T_DocumentLbrace, opState); // push
        defState.addRule("document-node (", defState, T_DocumentNodeLpar);
        defState.addRule("delete nodes", defState, T_DeleteNodes);
        defState.addRule("delete node", defState, T_DeleteNodes);
        
        defState.addRule("element ¬Q {", defState, T_ElementQNameLbrace, opState);
        defState.addRule("element {", defState, T_ElementLbrace, opState);
        defState.addRule("element (", defState, T_ElementLpar);
        defState.addRule("every ¬?$", vardeclState, T_Every);
        defState.addRule("empty-sequence ( )", opState, T_Empty);
        defState.addRule("exactly", defState, T_Exactly);
        defState.addRule("exit returning", defState, T_Exit);
        
        defState.addRule("following ::", defState, T_AxisFollowing);
        defState.addRule("following-sibling ::", defState, T_AxisFollowingSibling);
        defState.addRule("for ¬?$", vardeclState, T_ForVariable);
        defState.addRule("for tumbling window", vardeclState, T_ForTumbling);
        defState.addRule("for sliding window", vardeclState, T_ForSliding);
        defState.addRule("from", defState, T_From);
        defState.addRule("ftnot", defState, T_FTNot);

        defState.addRule("group by", defState, T_GroupBy);

        defState.addRule("if (", defState, T_IfLpar);
        defState.addRule("import schema", nsDeclState, T_ImportSchema);
        defState.addRule("import module", nsDeclState, T_ImportModule);
        defState.addRule("insert nodes", defState, T_InsertNodes);
        defState.addRule("insert node", defState, T_InsertNodes);
        defState.addRule("item ( )", defState, T_ItemLparRpar);
        
        defState.addRule("let ¬?$", vardeclState, T_LetVariable);
        defState.addRule("let score ¬?$", vardeclState, T_LetScoreVariable);
        
        defState.addRule("module namespace", nsDeclState, T_Module);
        
        defState.addRule("namespace ::", defState, T_AxisNamespace);
        defState.addRule("namespace ¬N {", defState, T_NamespaceLbrace, opState);
        defState.addRule("node (", defState, T_NodeLpar);
        defState.addRule("not in", defState, T_FTNotIn);

        defState.addRule("order by", defState, T_OrderBy);
        defState.addRule("ordered {", defState, T_Ordered, opState);
        
        defState.addRule("parent ::", defState, T_AxisParent);
        defState.addRule("preceding-sibling ::", defState, T_AxisPrecedingSibling);
        defState.addRule("preceding ::", defState, T_AxisPreceding);
        defState.addRule("processing-instruction (", defState, T_ProcessingInstructionLpar);
        defState.addRule("processing-instruction {", defState, T_PILbrace, opState);
        defState.addRule("processing-instruction ¬N {", defState, T_PINameLbrace, opState);
        
        defState.addRule("replace value of node", defState, T_ReplaceValueOfNode);
        defState.addRule("replace node", defState, T_ReplaceNode);
        defState.addRule("rename node", defState, T_RenameNode);
        
        defState.addRule("self ::", defState, T_AxisSelf);
        defState.addRule("some ¬?$", vardeclState, T_Some);
        defState.addRule("stable order by", defState, T_OrderByStable);
        defState.addRule("switch (", defState, T_Switch);
        
        defState.addRule("text {",      defState, T_TextLbrace, opState);
        defState.addRule("text (",      defState, T_TextLpar);
        defState.addRule("typeswitch (", defState, T_TypeswitchLpar);
        defState.addRule("try ¬?{",      defState, T_Try);
        
        defState.addRule("unordered {", defState, T_Unordered, opState);
        defState.addRule("validate {", defState, T_Validate);
//        defState.addRule("validation strict", defState, T_ValidationStrict);
//        defState.addRule("validation skip", defState, T_ValidationSkip);
//        defState.addRule("validation lax", defState, T_ValidationLax);
        defState.addRule("xquery version ¬S", defState, T_XQueryVersion);
        defState.addRule("while (", defState, T_While);
    }

    static { // after 'declare'
        declareState.addRule("assignable", declareState, T_AssignableFlag);
        declareState.addRule("base-uri", nsDeclState, T_DeclareBaseURI);
        declareState.addRule("boundary-space", defState, T_DeclareBoundarySpace);
        declareState.addRule("construction", defState, T_DeclareConstruction);
        declareState.addRule("context item", defState, T_ContextItem);
        declareState.addRule("copy-namespaces", defState, T_DeclareCopyNamespaces);
        declareState.addRule("default collation", nsDeclState, T_DefaultCollation);
        declareState.addRule("default element namespace", defState, T_DefaultElement);
        declareState.addRule("default function namespace", defState, T_DefaultFunction);
        declareState.addRule("default order empty", defState, T_DeclareDefaultOrder);
        declareState.addRule("deterministic", declareState, T_DeterministicFlag);
        declareState.addRule("ft-option", opState, T_DeclareFTOption);
        declareState.addRule("function", defState, T_DeclareFunction);
        declareState.addRule("namespace", nsDeclState, T_DeclareNamespace);
        declareState.addRule("nondeterministic", declareState, T_UndeterministicFlag);
        declareState.addRule("option", defState, T_DeclareOption);
        declareState.addRule("ordering", defState, T_DeclareOrdering);
        declareState.addRule("private", declareState, T_PrivateFlag);
        declareState.addRule("public", declareState, T_PublicFlag);
        declareState.addRule("revalidation lax", defState, T_ValidationLax);
        declareState.addRule("revalidation skip", defState, T_ValidationSkip);
        declareState.addRule("revalidation strict", defState, T_ValidationStrict);
        declareState.addRule("sequential", declareState, T_SequentialFlag);
        declareState.addRule("simple", declareState, T_SimpleFlag);
        declareState.addRule("template", defState, T_Template);
        declareState.addRule("unassignable", declareState, T_UnassignableFlag);
        declareState.addRule("updating", declareState, T_UpdatingFlag);
        declareState.addRule("variable ¬?$", vardeclState, T_DeclareVariable);
        declareState.addRule("¬Q (", defState, T_QNameLpar);
        declareState.addRule("¬Q", defState, T_QName);
        declareState.addRule("(:¬C", declareState, T_Comment);   
    }
    
    static {    // =========== Operator state: after an expression
        
        opState.addRule("\0", defState, T_END);
        opState.addRule("¬S", opState, T_StringLiteral);
        opState.addRule("¬u", opState, NUMBER);
        opState.addRule("¬Q", defState, T_Dummy);

        opState.addRule("!=", defState, T_NotEquals);
        opState.addRule("(:¬C", opState, T_Comment);    // only if enabled
        opState.addRule("(#¬P", opState, T_Pragma);
        opState.addRule("(", defState, T_Lpar);
        opState.addRule(")", opState, T_Rpar);
        opState.addRule("*:¬N", opState, T_StarColonNCName);
        opState.addRule("*", defState, T_Multiply);
        opState.addRule("+", defState, T_Plus);
        opState.addRule("-", defState, T_Minus);
        opState.addRule(",", defState, T_Comma);
        opState.addRule("//", defState, T_SlashSlash);
        opState.addRule("/", defState, T_Slash);
        opState.addRule(":=", defState, T_ColonEquals);
        opState.addRule(";", defState, T_SemiColon);
        opState.addRule("<<", defState, T_LtLt);
        opState.addRule("<=", defState, T_LtEquals);
        opState.addRule("<", defState, T_Lt);
        opState.addRule("=", defState, T_Equals);
        opState.addRule(">=", defState, T_GtEquals);
        opState.addRule(">>", defState, T_GtGt);
        opState.addRule(">", defState, T_Gt);
        opState.addRule("?", defState, T_QMark);
        opState.addRule("|", defState, T_Vbar);
        opState.addRule("[", defState, T_Lbrack);
        opState.addRule("]", opState, T_Rbrack);
        opState.addRule("..",  opState, T_DotDot);
        //opState.addRule(".",   opState, T_Dot); // parsed by NUMBER ¬u
        
        opState.addRule("after", defState, T_After);
        opState.addRule("all words", opState, T_AllWords);
        opState.addRule("all", opState, T_All);
        opState.addRule("any word", opState, T_AnyWord);
        opState.addRule("any", opState, T_Any);
        opState.addRule("and", defState, T_And);
        opState.addRule("ancestor ::", defState, T_AxisAncestor);
        opState.addRule("ascending", opState, T_Ascending);
        opState.addRule("as first into", defState, T_AsFirstInto);
        opState.addRule("as last into", defState, T_AsLastInto);
        opState.addRule("as", defState, T_As);
        opState.addRule("at ¬S", defState, T_AtStringLiteral);
        opState.addRule("at start", opState, T_AtStart);
        opState.addRule("at end", opState, T_AtEnd);
        opState.addRule("at ¬?$", defState, T_At);
        opState.addRule("at least", defState, T_AtLeast);
        opState.addRule("at most", defState, T_AtMost);

        opState.addRule("before", defState, T_Before);
        
        opState.addRule("case insensitive", opState, T_CaseInsensitive);
        opState.addRule("case sensitive", opState, T_CaseSensitive);
        opState.addRule("case ¬?$", vardeclState, T_Case);
        opState.addRule("case", itemTypeState, T_Case);
        opState.addRule("cast as", itemTypeState, T_CastAs);
        opState.addRule("castable as", itemTypeState, T_CastableAs);
        opState.addRule("catch ¬?¬Q", defState, T_Catch); // look ahead
        opState.addRule("catch ¬?(", defState, T_Catch);
        opState.addRule("catch ¬?*", defState, T_Catch);
        opState.addRule("collation", defState, T_Collation);
        opState.addRule("contains text", opState, T_FTContains);
        opState.addRule("count ¬?$", defState, T_Count);

        opState.addRule("default", vardeclState, T_Default);
        opState.addRule("delete nodes", defState, T_DeleteNodes);
        opState.addRule("delete node", defState, T_DeleteNodes);
        opState.addRule("descending", opState, T_Descending);
        opState.addRule("diacritics insensitive", opState, T_DiacriticsInsensitive);
        opState.addRule("diacritics sensitive", opState, T_DiacriticsSensitive);
        opState.addRule("div", defState, T_Div);
        opState.addRule("different sentence", opState, T_DifferentSentence);
        opState.addRule("different paragraph", opState, T_DifferentParagraph);
        opState.addRule("distance", defState, T_Distance);
        
        opState.addRule("else", defState, T_Else);
        opState.addRule("empty greatest", opState, T_EmptyGreatest);
        opState.addRule("empty least", opState, T_EmptyLeast);
        opState.addRule("end ¬?$", vardeclState, T_End);
        opState.addRule("end ¬?¬N", opState, T_End);
        opState.addRule("eq", defState, T_FortranEq);
        opState.addRule("every ¬?$", opState, T_Every);
        opState.addRule("exactly", defState, T_Exactly);
        opState.addRule("except", defState, T_Except);
        opState.addRule("external", defState, T_External);
        opState.addRule("entire content", opState, T_EntireContent);

        opState.addRule("for ¬?$", vardeclState, T_ForVariable);
        opState.addRule("for tumbling window", vardeclState, T_ForTumbling);
        opState.addRule("for sliding window", vardeclState, T_ForSliding);
        opState.addRule("from", defState, T_From);
        opState.addRule("ftcontains", opState, T_FTContains);
        opState.addRule("ftor", opState, T_FTOr);
        opState.addRule("ftand", opState, T_FTAnd);
        opState.addRule("ftnot", opState, T_FTNot);

        opState.addRule("ge", defState, T_FortranGe);
        opState.addRule("gt", defState, T_FortranGt);
        opState.addRule("global", defState, T_Global);
        opState.addRule("group by", defState, T_GroupBy);

        opState.addRule("idiv", defState, T_Idiv);
        opState.addRule("into", defState, T_Into);
        opState.addRule("instance of", itemTypeState, T_Instanceof);
        opState.addRule("intersect", defState, T_Intersect);
        opState.addRule("in", defState, T_In);
        opState.addRule("is", defState, T_Is);
        opState.addRule("isnot", defState, T_IsNot);
        opState.addRule("item ( )", defState, T_ItemLparRpar);
        
        opState.addRule("let ¬?$", vardeclState, T_LetVariable);
        opState.addRule("let score ¬?$", vardeclState, T_LetScoreVariable);
        opState.addRule("le", defState, T_FortranLe);
        opState.addRule("lt", defState, T_FortranLt);
        opState.addRule("language", defState, T_Language);
        opState.addRule("lowercase", opState, T_Lowercase);
        opState.addRule("levels", opState, T_Levels);
        
        opState.addRule("modify", defState, T_Modify);
        opState.addRule("mod", defState, T_Mod);

        opState.addRule("ne", defState, T_FortranNe);
        opState.addRule("not in", defState, T_FTNotIn);
        opState.addRule("next ¬?$", vardeclState, T_Next);
        
        opState.addRule("only end ¬?$", vardeclState, T_OnlyEnd);
        opState.addRule("only end ¬?¬N", opState, T_OnlyEnd);
        opState.addRule("order by", defState, T_OrderBy);
        opState.addRule("or", defState, T_Or);
        opState.addRule("occurs", defState, T_Occurs);
        opState.addRule("ordered", opState, T_Ordered);
        opState.addRule("option ¬Q", defState, T_FTExtension);

        opState.addRule("phrase", opState, T_Phrase);
        opState.addRule("paragraphs", opState, T_Paragraphs);
        opState.addRule("previous ¬?$", vardeclState, T_Previous);
 
        opState.addRule("relationship ¬S", opState, T_Relationship);
        opState.addRule("return", defState, T_Return);

        opState.addRule("same sentence", opState, T_SameSentence);
        opState.addRule("same paragraph", opState, T_SameParagraph);
        opState.addRule("satisfies", defState, T_Satisfies);
        opState.addRule("score ¬?$", vardeclState, T_Score);
        opState.addRule("sentences", opState, T_Sentences);
        opState.addRule("some ¬?$", opState, T_Some);
        opState.addRule("stable order by", defState, T_OrderByStable);
        opState.addRule("start ¬?¬N", opState, T_Start);
        opState.addRule("start ¬?$", vardeclState, T_Start);
        
        opState.addRule("then", defState, T_Then);
        opState.addRule("to", defState, T_To);
        opState.addRule("treat as", itemTypeState, T_TreatAs);
        opState.addRule("typeswitch (", defState, T_TypeswitchLpar);
        opState.addRule("times", opState, T_Times);
        
        opState.addRule("union", defState, T_Union);
        opState.addRule("uppercase", opState, T_Uppercase);
        // latest ridiculous FT syntax:
        opState.addRule("using case insensitive", opState, T_CaseInsensitive);
        opState.addRule("using case sensitive", opState, T_CaseSensitive);
        opState.addRule("using diacritics insensitive", opState, T_DiacriticsInsensitive);
        opState.addRule("using diacritics sensitive", opState, T_DiacriticsSensitive);
        opState.addRule("using language", defState, T_Language);
        opState.addRule("using option ¬Q", defState, T_FTExtension);
        opState.addRule("using stemming", opState, T_WithStemming);
        opState.addRule("using wildcards", opState, T_WithWildcards);
        opState.addRule("using thesaurus", thesaurusState, T_WithThesaurus);
        opState.addRule("using default stop words", opState, T_WithDefaultStopWords);
        opState.addRule("using stop words", opState, T_WithStopWords);
        opState.addRule("using no content", defState, T_WithoutContent);
        opState.addRule("using no stemming", opState, T_WithoutStemming);
        opState.addRule("using no wildcards", opState, T_WithoutWildcards);
        opState.addRule("using no thesaurus", opState, T_WithoutThesaurus);
        opState.addRule("using no stop words", opState, T_WithoutStopWords);
        opState.addRule("using lowercase", opState, T_Lowercase);
        opState.addRule("using uppercase", opState, T_Uppercase);

        opState.addRule("validate {", opState, T_Validate);
        
        opState.addRule("weight", defState, T_Weight);
        opState.addRule("when", defState, T_When);
        opState.addRule("where", defState, T_Where);
        opState.addRule("window", defState, T_Window);
        opState.addRule("with stemming", opState, T_WithStemming);
        opState.addRule("with wildcards", opState, T_WithWildcards);
        opState.addRule("with thesaurus", thesaurusState, T_WithThesaurus);
        opState.addRule("with default stop words", opState, T_WithDefaultStopWords);
        opState.addRule("with stop words", opState, T_WithStopWords);
        opState.addRule("with", defState, T_With);
        opState.addRule("without content", defState, T_WithoutContent);
        opState.addRule("without stemming", opState, T_WithoutStemming);
        opState.addRule("without wildcards", opState, T_WithoutWildcards);
        opState.addRule("without thesaurus", opState, T_WithoutThesaurus);
        opState.addRule("without stop words", opState, T_WithoutStopWords);
        opState.addRule("words", opState, T_Words);

        opState.addRule("{", defState, T_LbraceExprEnclosure, opState);
        opState.addRule("}", POP_STATE, T_Rbrace);

        thesaurusState.addRule("default", defState, T_Default);
        thesaurusState.addRule("at ¬S", opState, T_AtStringLiteral);
        thesaurusState.addRule("(", thesaurusState, T_Lpar);
    }
    
    static {
        // =========== Type state
        
        itemTypeState.addRule("\0", null, T_END);
        itemTypeState.addRule("(:¬C", itemTypeState, T_Comment);  
        itemTypeState.addRule("¬Q", occIndState, T_QName);
        itemTypeState.addRule(") as", itemTypeState, T_RparAs);
        itemTypeState.addRule(")", opState, T_Rpar);
        itemTypeState.addRule("*:¬N", opState, T_StarColonNCName);
        itemTypeState.addRule("..", opState, T_DotDot);
        itemTypeState.addRule(".", opState, T_Dot);
        itemTypeState.addRule("{", defState, T_LbraceExprEnclosure, defState);

        itemTypeState.addRule("attribute (", kindTestState,
                              T_AttributeLpar, occIndState);
        itemTypeState.addRule("comment (", kindTestState,
                              T_CommentLpar, occIndState);
        itemTypeState.addRule("document-node (", kindTestState, 
                              T_DocumentNodeLpar, occIndState);
        itemTypeState.addRule("element (", kindTestState, 
                              T_ElementLpar, occIndState);
        itemTypeState.addRule("empty-sequence ( )", opState, T_Empty);
        
        itemTypeState.addRule("item ( )", occIndState, T_ItemLparRpar);
        itemTypeState.addRule("node (", kindTestState, T_NodeLpar, occIndState);
        itemTypeState.addRule("processing-instruction (", kindTestState,
                              T_ProcessingInstructionLpar, occIndState);
        itemTypeState.addRule("text (", kindTestState, T_TextLpar, occIndState);
        itemTypeState.addRule("untyped", defState, T_Untyped);
        itemTypeState.addRule("when", defState, T_When);

        occIndState.addRule("?", opState, T_QMark);
        occIndState.addRule("+", opState, T_Plus);
        occIndState.addRule("*", opState, T_Star);
        occIndState.addRule("", opState, SKIP);   // retry

        // =========== Start tag state
        
        tagState.addRule("\0", null, T_END);
        tagState.addRule("\"", attrQuoteState, T_OpenQuot);
        tagState.addRule("\'", attrApostState, T_OpenApos);
        tagState.addRule(">", contentState, T_TagClose);
        tagState.addRule("/>", POP_STATE, T_EmptyTagClose);
        tagState.addRule("=", tagState, T_ValueIndicator);
        tagState.addRule("¬Q", tagState, T_TagQName);

        // =========== Element content state

        contentState.addRule("\0", null, T_END);

        contentState.addRule("<!--¬X", contentState, T_XmlComment);
        contentState.addRule("<![CDATA[¬c", contentState, T_Cdata);
        contentState.addRule("<?¬p", contentState, T_ProcessingInstruction);
        contentState.addRule("</¬Q >", POP_STATE, T_EndTag);
        contentState.addRule("<¬Q", tagState, T_StartTagOpen, contentState);
        contentState.addRule("{{¬+", contentState, T_Char);
        contentState.addRule("{", defState, T_Lbrace, contentState);
        contentState.addRule("}}¬+", contentState, T_Char, null);
        contentState.addRule("}", ERROR_STATE, 0, null, "'}' must be escaped by '}}'");
        contentState.addRule("¬t", contentState, T_Char); // seq of chars
        contentState.addRule("&¬E", contentState, T_CharRef); 
        
        // =========== Attribute value state
        
        attrApostState.addRule("''¬+", attrApostState, T_Char);
        attrApostState.addRule("'", tagState, T_CloseApos);
        attrApostState.addRule("{{¬+", defState, T_Char);
        attrApostState.addRule("{", defState, T_Lbrace, attrApostState);
        attrApostState.addRule("}}¬+", attrApostState, T_Char);
        attrApostState.addRule("}", ERROR_STATE, 0, null, "'}' must be escaped by '}}'");
        attrApostState.addRule("<", ERROR_STATE, 0, null, "illegal character in attribute");
        attrApostState.addRule("&¬E", attrApostState, T_CharRef); 
        attrApostState.addRule("¬a", attrApostState, T_Char); 

        attrQuoteState.addRule("\"\"¬+", attrQuoteState, T_Char);
        attrQuoteState.addRule("\"", tagState, T_CloseQuot);
        attrQuoteState.addRule("{{¬+", defState, T_Char);
        attrQuoteState.addRule("{", defState, T_Lbrace, attrQuoteState);
        attrQuoteState.addRule("}}¬+", attrQuoteState, T_Char);
        attrQuoteState.addRule("}", ERROR_STATE, 0, null, "'}' must be escaped by '}}'");
        attrQuoteState.addRule("<", ERROR_STATE, 0, null, "illegal character in attribute");
        attrQuoteState.addRule("¬A", attrQuoteState, T_Char); 
        attrQuoteState.addRule("&¬E", attrQuoteState, T_CharRef); 

        // ========== 
        kindTestState.addRule("\0", null, T_END);
        kindTestState.addRule("(:¬C", kindTestState, T_Comment);
        kindTestState.addRule("{", kindTestState, T_Lbrace);
        kindTestState.addRule(")", POP_STATE, T_Rpar);
        kindTestState.addRule("*", kindTestState, T_Star);
        kindTestState.addRule("@", kindTestState, T_At);
        kindTestState.addRule(",", kindTestState, T_Comma);
        kindTestState.addRule("element (", kindTestState, T_ElementLpar, kindTestState);
        kindTestState.addRule("¬Q", kindTestState, T_QName);
        kindTestState.addRule("¬S", kindTestState, T_StringLiteral);
        // in case no match
        kindTestState.addRule("", opState, SKIP);   // retry in op state
                            
        // ========== Namespace declaration      
        
        nsDeclState.addRule("\0", null, T_END);
        nsDeclState.addRule("(:¬C", nsDeclState, T_Comment);  
        nsDeclState.addRule("=", nsDeclState, T_AssignEquals);
        nsDeclState.addRule("¬S", defState, T_URLLiteral);
        nsDeclState.addRule("¬N", nsDeclState, T_NCName);
        nsDeclState.addRule("namespace", nsDeclState, T_Namespace);

        // ==========         
        vardeclState.addRule("\0", null, T_END);
        vardeclState.addRule("$ ¬Q", vardeclState, T_VarName);
        vardeclState.addRule(":=", defState, T_ColonEquals);
        vardeclState.addRule(",", defState, T_Comma);
        vardeclState.addRule(";", defState, T_SemiColon);
        vardeclState.addRule("{", defState, T_Lbrace, opState);
        vardeclState.addRule("(:¬C", vardeclState, T_Comment);
        vardeclState.addRule(")", vardeclState, T_Rpar);
        vardeclState.addRule("as ", itemTypeState, T_As);
        vardeclState.addRule("at ¬?$", defState, T_At);
        vardeclState.addRule("external", defState, T_External);
        vardeclState.addRule("in", defState, T_In);
        vardeclState.addRule("return", defState, T_Return);
        vardeclState.addRule("score ¬?$", defState, T_Score);
        
        vardeclState.addRule("previous ¬?$", vardeclState, T_Previous);
        vardeclState.addRule("next ¬?$", vardeclState, T_Next);
        vardeclState.addRule("when", defState, T_When);

        // ===== Special token names. After rules
        tokenNames.put(T_END, "end");
        tokenNames.put(T_DoubleLiteral, "[double]");
        tokenNames.put(T_IntegerLiteral, "[integer]");
        tokenNames.put(T_DecimalLiteral, "[decimal]");
        tokenNames.put(T_QName, "[qname]");
        tokenNames.put(T_Char, "[text]");
    }
    
    protected int getTokenCategory(int token)
    {
        switch (token) {
        case T_S:
            return LC_SPACE;
        case T_DecimalLiteral:
        case T_DoubleLiteral:
        case T_IntegerLiteral:
            return LC_NUMBER;
        case T_StringLiteral:
        case T_URLLiteral:
            return LC_STRING;
        case T_StartTagOpen:
        case T_StartTagOpenRoot:
        case T_EndTag:
        case T_TagQName:
        case T_TagClose:
        case T_EmptyTagClose:
        case T_ValueIndicator:
        case T_OpenQuot:
        case T_OpenApos:
        case T_CloseQuot:
        case T_CloseApos:
        case T_Char:
        case T_XmlComment:
        case T_ProcessingInstruction:
            return LC_TAG;
        case T_Lbrace:
        case T_LbraceExprEnclosure:
        case T_Equals:
        case T_Lpar:
        case T_Rpar:
        case T_Lt:
        case T_LtEquals:
        case T_LtLt:
        case T_Gt:
        case T_GtEquals:
        case T_GtGt:
        case T_Minus:
        case T_Plus:
        case T_Multiply:
        case T_Star:
        case T_QMark:
        case T_Slash:
        case T_Vbar:
            return LC_MISC;
        case T_QNameLpar:
        case T_Rbrace:
            return LC_FUNCTION;
        case T_QName:
        case T_NCName:
        case T_NCNameColonStar:
        case T_StarColonNCName:
        case T_TypeQName:
        case T_VarName:
            return LC_NAME;
        case T_Comment:
            return LC_COMMENT;
        case T_Pragma:
        case T_Extension:
            return LC_PRAGMA;
        default:
            return LC_KEYWORD;
        }
    }
    
//    public static void main(String[] args)
//    {
//        try {
//            long t0 = System.currentTimeMillis();
//            Lexer lex = new Lexer();
//            System.err.println("init "+(System.currentTimeMillis() - t0));
//            String q1 = "declare namespace x='toto';\n" +
//            		"for $ x in (1) + x:x return " +
//            		"<a u='a{$i}gg&lt;'>{$x }ii<?pi pi?>oo<!--coco--></a>";
//            
//            String q2 = "for tumbling window $w in 1 to 10\n" +
//            		"start at $s when true() only end at $e when $e - $s = 2\n" +
//            		"return <win>{ $w }</win>";
//            
//            lex.startLexer(q1);
//            debug = true;
//            int token = lex.getToken();
//            for(; token != T_END; ) {
//                token = lex.getToken();
//            }
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static void usage()
//    {
//        System.err.println("Lexer2 usage: ");
//        System.exit(1);
//    }
}
