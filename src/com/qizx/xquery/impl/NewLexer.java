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
import com.qizx.util.basic.XMLUtil;
import com.qizx.xquery.ModuleContext;

public class NewLexer
{
    public static final String ERR_SYNTAX = "XPST0003";
    
    public static final int INTEGER_LITERAL = 1;
    public static final int DECIMAL_LITERAL = 2;
    public static final int DOUBLE_LITERAL = 3;
    
    public static int debug = 0;

    protected ModuleContext  currentModule; 
    protected String source;
    protected int   inputLength;
    protected char  curChar;
    protected int   curPtr;    // token start:
    protected boolean allowComments;
    
    // look-ahead position: moved by pickXXX() and next(), reset to curPtr by back()
    protected int   tokenStart; 
    protected int   tokenEnd;   
    protected int   prevTokenLoc;
    private   int   lookaheadPos;
    private boolean skipSpace;
   
    // saved data:
    protected StringBuilder saveBuffer = new StringBuilder();
    protected String savedPrefix;   // of parsed qname
    protected String savedName;     // localName of parsed qname
    protected int    numberToken;   // code of numeric literal (double int dec)
    protected boolean whiteSpace;




    public void startLexer(String input)
    {
        this.source = input;
        inputLength = input.length();
        tokenEnd = 0;
        next();
        if (debug > 0)
            System.err.println("===== Start lexer");
        skipSpace = true;
        allowComments = true;
    }

    protected void want(String pattern)
        throws CompilationException
    {
        if(!eat(pattern)) {
            errorExpect(pattern);
        }
    }

    /**
     * recognizes a pattern
     */
    protected boolean eat(String pattern) throws CompilationException
    {
        eatSpace();
        return eatNoSkip(pattern);
    }
    
    /**
     * recognizes a pattern
     */
    protected boolean eatNoSkip(String pattern) throws CompilationException
    {
        if(debug >= 2)
            System.err.println("try pattern |" + pattern+"| at "+curPtr+" char "+curChar);
        int savePos = curPtr - 1;
        
        if(!lookFor(pattern)) {
            curPtr = savePos;
            next();
            return false;
        }
        
        // matched: 
        prevTokenLoc = tokenStart;
        tokenStart = savePos;
        tokenEnd = curPtr - 1;
        if(lookaheadPos >= 0) {  // met a test instruction ¬?
            tokenEnd = curPtr = lookaheadPos;
            next();
        }
        if(debug >= 1)
            System.err.println("matched pattern |" + pattern + "| at "+tokenStart+"-"+tokenEnd);
        return true;
    }
    
    // look ahead without moving
    protected boolean see(String pattern) throws CompilationException
    {
        int savePos = curPtr - 1;
        boolean ok = lookFor(pattern);
        curPtr = savePos;
        next();
        return ok;
    }
    
    private boolean lookFor(String pattern) throws CompilationException
    {
        lookaheadPos = -1;
        char prevPatternChar = 0;
        
        for(int i = 0, len = pattern.length(); i < len; i++) {
            char rc = pattern.charAt(i);
            if(rc == ' ') { // optional, except if after name
                if(isKeyLetter(prevPatternChar) && isNameChar(curChar))
                    return false;
                prevPatternChar = ' ';
                eatSpace();
            }
            else if(rc != '%') {
                if(rc != curChar) {
                    if(debug >= 2)
                        System.err.println(" fail at "+i+" on "+curChar);
                    return false;
                }
                prevPatternChar = rc;
                next();
            }
            else {
                rc = pattern.charAt(++i);  // no need to test
                prevPatternChar = ' ';
                switch(rc) {
                case 'N':   // simple name
                    if(!pickName())
                        return false;
                    break;
                case 'Q':   // Qname
                    if(!eatQName())
                        return false;
                    break;
                case 'S':   // String literal
                    if(!eatStringLiteral())
                        return false;
                    break;
                case 'u':   // number: type stored into numberToken
                    if(!eatNumber())
                        return false;
                    break;
                case '%':   // used?
                    if(!pick('%'))
                        return false;
                    break;
                case '?':   // enter lookahead mode
                    lookaheadPos = curPtr - 1;
                    break;
                default:
                    lexicalError("bad lex metachar "+(char)rc);
                }
            }
        }
        // end of keyword: must not be on a namechar
        if(isKeyLetter(prevPatternChar) && isNameChar(curChar))
            return false;
        return true;
    }

    // swallows as many simple chars as possible, set whiteSpace
    // Stop on tag, comment, PI, CDATA sections, char refs, 
    protected boolean eatXmlChars(char delimiter)
        throws CompilationException
    {
        whiteSpace = true;
        saveBuffer.setLength(0);
        for( ; curChar != 0 && curChar != '<' && curChar != '&'
               && curChar != delimiter; )
        {
            if(curChar == '{') {
                if(!eatRaw("{{"))
                    break;
                save('{');
                whiteSpace = false;
            }
            else if(curChar == '}') {
                save('}');
                if(!eatRaw("}}")) {
                    syntax("'}' must be escaped by '}}'");
                }
                whiteSpace = false;
            }
            else {
                if(whiteSpace && !Character.isWhitespace(curChar))
                    whiteSpace = false;
                save(curChar);
                next();
            }
        }
        return saveBuffer.length() > 0;
    }

    protected boolean eatStringLiteral() throws CompilationException
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
            else if(curChar == '&') {   // char refs: new in may2003
                eatCharRef();
                continue; // dont do next()
            }
            else save(curChar);
            next();
        }
        if(debug >= 1)
            System.err.println("string literal |"+saveBuffer+"|");
        return true;
    }

    protected void eatSpace() throws CompilationException
    {
        for(; curPtr <= inputLength; ) {
            while(Character.isWhitespace(curChar))
                next();
            if(eatRaw("(:")) {
                if(!allowComments)
                    lexicalError("comment not allowed here");
                eatCommentTail();
            }
            else
                break;
        }
    }

    protected boolean eatCharRef() throws CompilationException
    {
        if(!pick('&'))
            return false;
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
                lexicalError("invalid character reference #" + code);            
            if(!pick(';'))
                lexicalError("character reference not terminated by semicolon");
        }
        else if(eatRaw("lt;"))
            save('<');
        else if(eatRaw("gt;"))
            save('>');
        else if(eatRaw("amp;"))
            save('&');
        else if(eatRaw("quot;"))
            save('"');
        else if(eatRaw("apos;"))
            save('\'');
        else
            lexicalError("invalid character reference");
        return true;
    }

    protected boolean eatNumber()
        throws CompilationException
    {
        saveBuffer.setLength(0);
        numberToken = INTEGER_LITERAL;
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
            if (saveBuffer.length() == 1) { // SPECIAL CASE '.'
                return false;
            }
            numberToken = DECIMAL_LITERAL;
        }
        if (saveBuffer.length() == 0)
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
            numberToken = DOUBLE_LITERAL;
        }
        if (Character.isLetter(curChar))
            lexicalError("numeric literal must not be followed by name");
        if (saveBuffer.length() == 1 && saveBuffer.charAt(0) == '.')
            return false;
        if(debug >= 1)
            System.err.println("Lexer: number literal "+saveBuffer);
        return true;
    }

    // parses a pragma, store QName into prefixValue/localName 
    // and contents into saveBuffer
    protected boolean eatPragma()
        throws CompilationException
    {
        eatSpace();
        if(!eatRaw("(#"))
            return false;
        while(Character.isWhitespace(curChar))
            next();
        if(!eatQName())
            lexicalError("pragma should begin with a QName");
        saveBuffer.setLength(0);
        for(;; next()) {
            if(curChar == 0)
                lexicalError("unclosed pragma");
            else if(eatRaw("#)"))
                break;
            save(curChar);
        }
        if(saveBuffer.length() > 0 && !Character.isWhitespace(saveBuffer.charAt(0)))
            lexicalError("whitespace required before pragma content");
        return true;
    }

    protected boolean eatPI() throws CompilationException 
    {
        if(!eatRaw("<?"))
            return false;
        saveBuffer.setLength(0);
    
        for (;;) {
            while (curChar != '?' && curChar != 0) {
                save(curChar);
                next();
            }
            if (curChar == 0)
                lexicalError("unterminated processing instruction");
            if (eatRaw("?>"))
                break;
            save(curChar);
            next();
        }
        return true;
    }

    protected boolean eatXmlComment() throws CompilationException
    {
        if(!eatRaw("<!--"))
            return false;
        saveBuffer.setLength(0);
        for (;;) {
            while (curChar != '-' && curChar != 0) {
                save(curChar);
                next();
            }
            if (curChar == 0)
                lexicalError("unterminated XML comment");
            if (eatRaw("-->"))
                break;
            save(curChar);
            next();
        }
        return true;
    }
    
    protected boolean eatCDATASection()
        throws CompilationException
    {
        if(!eatRaw("<![CDATA["))
            return false;
        saveBuffer.setLength(0);
        for(;; next()) {
            if(curChar == 0)
                lexicalError("unclosed CDATA section");
            else if(eatRaw("]]>"))
                break;
            save(curChar);
        }        
        return true;
    }
    
    private void eatCommentTail() throws CompilationException
    {
        for(;;) {
            if(curChar == 0)
                lexicalError("unclosed comment");
            else if(eatRaw(":)"))
                break;
            else if(eatRaw("(:"))
                eatCommentTail(); // embedding allowed (May 2003)
            else next();
        }
    }

    protected String extractStringToken()
    {
        return saveBuffer.toString();
    }

    // last token as a string
    protected String lastToken()
    {
        if(tokenStart >= inputLength)
            return "end";
        return source.substring(tokenStart, tokenEnd);
        //return saveBuffer.toString();
    }

    // Look for a QName, store it into prefixValue and localName
    protected boolean eatQName()
    {
        if(!pickName())
            return false;
        savedPrefix = "";
        if(pick(':')) {
            savedPrefix = savedName;
            if(!pickName()) {   
                backChar();
                savedName = savedPrefix;
                savedPrefix = "";   // saveBuffer unaltered
                return true;
            }
        }
        if(debug >= 1)
            System.err.println("QName " + savedPrefix + " : "+savedName);
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
        savedName = saveBuffer.toString();
        return true;
    }

    private boolean pick(char c)
    {
        if (curChar != c)
            return false;
        next();
        return true;
    }

    protected boolean eatRaw(String s)
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

    protected void setSkipSpace(boolean skipSpace)
    {
        this.skipSpace = skipSpace;
    }

    protected boolean atEndOfInput() throws CompilationException
    {
        if(curPtr <= inputLength)
            eatSpace();
        return curPtr >= inputLength && curChar == 0;
    }

    private boolean isNameStart(char c) {
        return c == '_' || Character.isLetter(c);
    }
    
    private boolean isNameChar(char c) {
        return c == '.' || c == '-' ||
               c != 0 && Character.isUnicodeIdentifierPart(c);
    }

    private boolean isKeyLetter(char c) {
        return c >= 'a' && c <= 'z';
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
    
    protected int currentPos()
    {
        return curPtr - 1;  // -1 because curChar / next()
    }

    void backChar()
    {
        --curPtr;
        curChar = curPtr == 0 ? '\n' : source.charAt(curPtr - 1);
    }

    private void save(char c)
    {
        saveBuffer.append(c);
    }

    // syntax error, expect...
    protected void errorExpect(String pattern)
        throws CompilationException
    {
        syntax("expecting " + pattern);
    }


    protected void syntax(String message)
        throws CompilationException
    {
        currentModule.error(ERR_SYNTAX, currentPos(), "syntax error, near '"
                            + lastToken() + "' : " + message);
        throw new CompilationException("syntax error, " + message);
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
}
