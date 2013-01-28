/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Roger Lawrence
 *   Mike McCabe
 *   Igor Bukanov
 *   Ethan Hugg
 *   Bob Jervis
 *   Terry Lucas
 *   Milen Nankov
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.nb.javascript;

import java.io.*;
import org.netbeans.spi.lexer.LexerInput;

/**
 * This class implements the JavaScript scanner.
 *
 * It is based on the C source files jsscan.c and jsscan.h
 * in the jsref package.
 *
 * @see org.mozilla.nb.javascript.Parser
 *
 * @author Mike McCabe
 * @author Brendan Eich
 */

// <netbeans>
public
// </netbeans>
class TokenStream
{
    /*
     * For chars - because we need something out-of-range
     * to check.  (And checking EOF by exception is annoying.)
     * Note distinction from EOF token type!
     */
    private final static int
        EOF_CHAR = -1;

// <netbeans>
    private LexerInput lexerInput;
    
    public void setInput(LexerInput lexerInput) {
        this.lexerInput = lexerInput;
    }

    /** Construct a tokenstream suitable for syntax highlighting lexing (returns
     * space and comment tokens, uses a LexerInput, etc. */
    public TokenStream(Parser parser, LexerInput lexerInput, Reader sourceReader, String sourceString,
                int lineno) {
        this(parser, sourceReader, sourceString, lineno);
        this.lexerInput = lexerInput;
        this.syntaxLexing = true;
    }
// </netbeans>
    
    TokenStream(Parser parser, Reader sourceReader, String sourceString,
                int lineno)
    {
        this.parser = parser;
        this.lineno = lineno;
        if (sourceReader != null) {
            if (sourceString != null) Kit.codeBug();
            this.sourceReader = sourceReader;
            this.sourceBuffer = new char[512];
            this.sourceEnd = 0;
        } else {
            if (sourceString == null) Kit.codeBug();
            this.sourceString = sourceString;
            this.sourceEnd = sourceString.length();
        }
        this.sourceCursor = 0;
    }

    /* This function uses the cached op, string and number fields in
     * TokenStream; if getToken has been called since the passed token
     * was scanned, the op or string printed may be incorrect.
     */
    String tokenToString(int token)
    {
        if (Token.printTrees) {
            String name = Token.name(token);

            switch (token) {
            case Token.STRING:
            case Token.REGEXP:
            case Token.NAME:
                return name + " `" + this.string + "'";

            case Token.NUMBER:
                return "NUMBER " + this.number;
            }

            return name;
        }
        return "";
    }

    static boolean isKeyword(String s)
    {
        return Token.EOF != stringToKeyword(s);
    }

    private static int stringToKeyword(String name)
    {
// #string_id_map#
// The following assumes that Token.EOF == 0
        final int
            Id_break         = Token.BREAK,
            Id_case          = Token.CASE,
            Id_continue      = Token.CONTINUE,
            Id_default       = Token.DEFAULT,
            Id_delete        = Token.DELPROP,
            Id_do            = Token.DO,
            Id_else          = Token.ELSE,
            Id_export        = Token.EXPORT,
            Id_false         = Token.FALSE,
            Id_for           = Token.FOR,
            Id_function      = Token.FUNCTION,
            Id_if            = Token.IF,
            Id_in            = Token.IN,
            Id_let           = Token.LET,
            Id_new           = Token.NEW,
            Id_null          = Token.NULL,
            Id_return        = Token.RETURN,
            Id_switch        = Token.SWITCH,
            Id_this          = Token.THIS,
            Id_true          = Token.TRUE,
            Id_typeof        = Token.TYPEOF,
            Id_var           = Token.VAR,
            Id_void          = Token.VOID,
            Id_while         = Token.WHILE,
            Id_with          = Token.WITH,
            Id_yield         = Token.YIELD,

            // the following are #ifdef RESERVE_JAVA_KEYWORDS in jsscan.c
            Id_abstract      = Token.RESERVED,
            Id_boolean       = Token.RESERVED,
            Id_byte          = Token.RESERVED,
            Id_catch         = Token.CATCH,
            Id_char          = Token.RESERVED,
            Id_class         = Token.RESERVED,
            Id_const         = Token.CONST,
            Id_debugger      = Token.DEBUGGER,
            Id_double        = Token.RESERVED,
            Id_enum          = Token.RESERVED,
            Id_extends       = Token.RESERVED,
            Id_final         = Token.RESERVED,
            Id_finally       = Token.FINALLY,
            Id_float         = Token.RESERVED,
            Id_goto          = Token.RESERVED,
            Id_implements    = Token.RESERVED,
            Id_import        = Token.IMPORT,
            Id_instanceof    = Token.INSTANCEOF,
            Id_int           = Token.RESERVED,
            Id_interface     = Token.RESERVED,
            Id_long          = Token.RESERVED,
            Id_native        = Token.RESERVED,
            Id_package       = Token.RESERVED,
            Id_private       = Token.RESERVED,
            Id_protected     = Token.RESERVED,
            Id_public        = Token.RESERVED,
            Id_short         = Token.RESERVED,
            Id_static        = Token.RESERVED,
            Id_super         = Token.RESERVED,
            Id_synchronized  = Token.RESERVED,
            Id_throw         = Token.THROW,
            Id_throws        = Token.RESERVED,
            Id_transient     = Token.RESERVED,
            Id_try           = Token.TRY,
            Id_volatile      = Token.RESERVED;

        int id;
        String s = name;
// #generated# Last update: 2007-04-18 13:53:30 PDT
        L0: { id = 0; String X = null; int c;
            L: switch (s.length()) {
            case 2: c=s.charAt(1);
                if (c=='f') { if (s.charAt(0)=='i') {id=Id_if; break L0;} }
                else if (c=='n') { if (s.charAt(0)=='i') {id=Id_in; break L0;} }
                else if (c=='o') { if (s.charAt(0)=='d') {id=Id_do; break L0;} }
                break L;
            case 3: switch (s.charAt(0)) {
                case 'f': if (s.charAt(2)=='r' && s.charAt(1)=='o') {id=Id_for; break L0;} break L;
                case 'i': if (s.charAt(2)=='t' && s.charAt(1)=='n') {id=Id_int; break L0;} break L;
                case 'l': if (s.charAt(2)=='t' && s.charAt(1)=='e') {id=Id_let; break L0;} break L;
                case 'n': if (s.charAt(2)=='w' && s.charAt(1)=='e') {id=Id_new; break L0;} break L;
                case 't': if (s.charAt(2)=='y' && s.charAt(1)=='r') {id=Id_try; break L0;} break L;
                case 'v': if (s.charAt(2)=='r' && s.charAt(1)=='a') {id=Id_var; break L0;} break L;
                } break L;
            case 4: switch (s.charAt(0)) {
                case 'b': X="byte";id=Id_byte; break L;
                case 'c': c=s.charAt(3);
                    if (c=='e') { if (s.charAt(2)=='s' && s.charAt(1)=='a') {id=Id_case; break L0;} }
                    else if (c=='r') { if (s.charAt(2)=='a' && s.charAt(1)=='h') {id=Id_char; break L0;} }
                    break L;
                case 'e': c=s.charAt(3);
                    if (c=='e') { if (s.charAt(2)=='s' && s.charAt(1)=='l') {id=Id_else; break L0;} }
                    else if (c=='m') { if (s.charAt(2)=='u' && s.charAt(1)=='n') {id=Id_enum; break L0;} }
                    break L;
                case 'g': X="goto";id=Id_goto; break L;
                case 'l': X="long";id=Id_long; break L;
                case 'n': X="null";id=Id_null; break L;
                case 't': c=s.charAt(3);
                    if (c=='e') { if (s.charAt(2)=='u' && s.charAt(1)=='r') {id=Id_true; break L0;} }
                    else if (c=='s') { if (s.charAt(2)=='i' && s.charAt(1)=='h') {id=Id_this; break L0;} }
                    break L;
                case 'v': X="void";id=Id_void; break L;
                case 'w': X="with";id=Id_with; break L;
                } break L;
            case 5: switch (s.charAt(2)) {
                case 'a': X="class";id=Id_class; break L;
                case 'e': c=s.charAt(0);
                    if (c=='b') { X="break";id=Id_break; }
                    else if (c=='y') { X="yield";id=Id_yield; }
                    break L;
                case 'i': X="while";id=Id_while; break L;
                case 'l': X="false";id=Id_false; break L;
                case 'n': c=s.charAt(0);
                    if (c=='c') { X="const";id=Id_const; }
                    else if (c=='f') { X="final";id=Id_final; }
                    break L;
                case 'o': c=s.charAt(0);
                    if (c=='f') { X="float";id=Id_float; }
                    else if (c=='s') { X="short";id=Id_short; }
                    break L;
                case 'p': X="super";id=Id_super; break L;
                case 'r': X="throw";id=Id_throw; break L;
                case 't': X="catch";id=Id_catch; break L;
                } break L;
            case 6: switch (s.charAt(1)) {
                case 'a': X="native";id=Id_native; break L;
                case 'e': c=s.charAt(0);
                    if (c=='d') { X="delete";id=Id_delete; }
                    else if (c=='r') { X="return";id=Id_return; }
                    break L;
                case 'h': X="throws";id=Id_throws; break L;
                case 'm': X="import";id=Id_import; break L;
                case 'o': X="double";id=Id_double; break L;
                case 't': X="static";id=Id_static; break L;
                case 'u': X="public";id=Id_public; break L;
                case 'w': X="switch";id=Id_switch; break L;
                case 'x': X="export";id=Id_export; break L;
                case 'y': X="typeof";id=Id_typeof; break L;
                } break L;
            case 7: switch (s.charAt(1)) {
                case 'a': X="package";id=Id_package; break L;
                case 'e': X="default";id=Id_default; break L;
                case 'i': X="finally";id=Id_finally; break L;
                case 'o': X="boolean";id=Id_boolean; break L;
                case 'r': X="private";id=Id_private; break L;
                case 'x': X="extends";id=Id_extends; break L;
                } break L;
            case 8: switch (s.charAt(0)) {
                case 'a': X="abstract";id=Id_abstract; break L;
                case 'c': X="continue";id=Id_continue; break L;
                case 'd': X="debugger";id=Id_debugger; break L;
                case 'f': X="function";id=Id_function; break L;
                case 'v': X="volatile";id=Id_volatile; break L;
                } break L;
            case 9: c=s.charAt(0);
                if (c=='i') { X="interface";id=Id_interface; }
                else if (c=='p') { X="protected";id=Id_protected; }
                else if (c=='t') { X="transient";id=Id_transient; }
                break L;
            case 10: c=s.charAt(1);
                if (c=='m') { X="implements";id=Id_implements; }
                else if (c=='n') { X="instanceof";id=Id_instanceof; }
                break L;
            case 12: X="synchronized";id=Id_synchronized; break L;
            }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
        }
// #/generated#
// #/string_id_map#
        if (id == 0) { return Token.EOF; }
        return id & 0xff;
    }

    // <netbeans>
    public
    // </netbeans>
    final int getLineno() { return lineno; }

    // <netbeans>
    public
    // </netbeans>
    final String getString() { return string; }

    // <netbeans>
    public
    // </netbeans>
    final double getNumber() { return number; }

    // <netbeans>
    public
    // </netbeans>
    final boolean eof() { return hitEOF; }

    // <netbeans>
    public int seenSpaces() {
        return seenSpaces;
    }
    private int seenSpaces;
    // </netbeans>
    
    // <netbeans>
    public int getToken() throws IOException {
        // Split strings and regexps into separate begin, literal, end tokens
        if (syntaxLexing && stringMode != NO_LITERAL) {
            switch (stringMode) {
            case IN_ERROR: {
                for (int i = 1, n = string.length(); i < n; i++) {
                    int c = lexerInput.read();
                    assert c == string.charAt(i) : string + i + ":" + (char)c;
                }
                stringMode = NO_LITERAL;
                return Token.ERROR;
            }
            case IN_STRING: {
                if (string.length() == 2) {
                    int c = lexerInput.read();
                    assert c == '"' || c == '\'' : (char)c;
                    stringMode = NO_LITERAL;
                    return Token.STRING_END;
                } else {
                    for (int i = 1, n = string.length()-1; i < n; i++) {
                        int c = lexerInput.read();
                        assert c == string.charAt(i) : string + i + ":" + (char)c;
                    }
                    stringMode = END_STRING;
                    return Token.STRING;
                }
            }
            case END_STRING: {
                int c = lexerInput.read();
                assert c == '"' || c == '\'' : (char)c;
                stringMode = NO_LITERAL;
                return Token.STRING_END;
            }
            case IN_REGEXP: {
                if (string.length() == 2) {
                    int c = lexerInput.read();
                    assert c == '/' : (char)c;
                    stringMode = NO_LITERAL;
                    return Token.REGEXP_END;
                } else {
                    int last = string.lastIndexOf('/');
                    for (int i = 1; i < last; i++) {
                        int c = lexerInput.read();
                        assert c == string.charAt(i) : string + i + ":" + (char)c;
                    }
                    stringMode = END_REGEXP;
                    return Token.REGEXP;
                }
            }
            case END_REGEXP: {
                int last = string.lastIndexOf('/');
                for (int i = last, n = string.length(); i < n; i++) {
                    int c = lexerInput.read();
                    assert c == string.charAt(i) : string + i + ":" + (char)c;
                }
                stringMode = NO_LITERAL;
                return Token.REGEXP_END;
            }
            }
            assert false : stringMode;
        }
        
        int token = privateGetToken();
        if (syntaxLexing) {
            if (token != Token.WHITESPACE && token != Token.EOL) {
                // Update divIsRegexp state
                // THIS IS NOT COMPLETE; there are other possible constructions with operators but covers 90+% of cases
                divIsRegexp = (token == Token.LP || token == Token.COMMA || token == Token.NOT || token == Token.ASSIGN || token == Token.SEMI ||
                        token == Token.COLON);
            }
            if (token == Token.STRING) {
                assert stringMode == NO_LITERAL;
                // TODO - can I just use string?
                //assert string == string && string.equals(string);
                string = lexerInput.readText().toString();
                int restLength = string.length()-1;
                if (restLength > 0) {
                    lexerInput.backup(restLength);
                    stringMode = IN_STRING;
                } else {
                    stringMode = NO_LITERAL;
                }
                token = Token.STRING_BEGIN;
            } else if (token == Token.REGEXP) {
                assert stringMode == NO_LITERAL;
                // TODO - can I just use string?
                //assert string == string && string.equals(string);
                string = lexerInput.readText().toString();
                int restLength = string.length()-1;
                if (restLength > 0) {
                    lexerInput.backup(restLength);
                    stringMode = IN_REGEXP;
                } else {
                    stringMode = NO_LITERAL;
                }
                token = Token.REGEXP_BEGIN;
            } else if (token == Token.REGEXP_ERROR) {
                assert stringMode == NO_LITERAL;
                string = lexerInput.readText().toString();
                int restLength = string.length()-1;
                if (restLength > 0) {
                    lexerInput.backup(restLength);
                    stringMode = IN_ERROR;
                } else {
                    stringMode = NO_LITERAL;
                }
                token = Token.REGEXP_BEGIN;
            } else if (token == Token.STRING_ERROR) {
                assert stringMode == NO_LITERAL;
                string = lexerInput.readText().toString();
                int restLength = string.length()-1;
                if (restLength > 0) {
                    lexerInput.backup(restLength);
                    stringMode = IN_ERROR;
                } else {
                    stringMode = NO_LITERAL;
                }
                token = Token.STRING_BEGIN;
            }
        }
        
        return token;
    }
    // </netbeans>
    final int privateGetToken() throws IOException
    {
        int c;
        // <netbeans>
        seenSpaces = 0;
        // </netbeans>
    retry:
        for (;;) {
            // <netbeans>
            //int initialPos = getBufferOffset();
            // </netbeans>
            // Eat whitespace, possibly sensitive to newlines.
            for (;;) {
                c = getChar();
                if (c == EOF_CHAR) {
                    return Token.EOF;
                } else if (c == '\n') {
                    dirtyLine = false;
    // <netbeans>
                    seenSpaces++;
                    // TODO -- annotate whitespace block here
    // </netbeans>
                    return Token.EOL;
                } else if (!isJSSpace(c)) {
                    if (c != '-') {
                        dirtyLine = true;
                    }
                    break;
            // <netbeans>
                } else {
                    seenSpaces++;
            // </netbeans>
                }
            }

    // <netbeans>
            // Possibly return whitespace tokens
            if (syntaxLexing && seenSpaces > 0) {
                ungetChar(c);
                return Token.WHITESPACE;
            }
    // </netbeans>
            
            if (c == '@') return Token.XMLATTR;

            // identifier/keyword/instanceof?
            // watch out for starting with a <backslash>
            boolean identifierStart;
            boolean isUnicodeEscapeStart = false;
            if (c == '\\') {
                c = getChar();
                if (c == 'u') {
                    identifierStart = true;
                    isUnicodeEscapeStart = true;
                    stringBufferTop = 0;
                } else {
                    identifierStart = false;
                    ungetChar(c);
                    c = '\\';
                }
            } else {
                identifierStart = Character.isJavaIdentifierStart((char)c);
                if (identifierStart) {
                    stringBufferTop = 0;
                    addToString(c);
                }
            }

            if (identifierStart) {
                boolean containsEscape = isUnicodeEscapeStart;
                for (;;) {
                    if (isUnicodeEscapeStart) {
                        // strictly speaking we should probably push-back
                        // all the bad characters if the <backslash>uXXXX
                        // sequence is malformed. But since there isn't a
                        // correct context(is there?) for a bad Unicode
                        // escape sequence in an identifier, we can report
                        // an error here.
                        int escapeVal = 0;
                        for (int i = 0; i != 4; ++i) {
                            c = getChar();
                            escapeVal = Kit.xDigitToInt(c, escapeVal);
                            // Next check takes care about c < 0 and bad escape
                            if (escapeVal < 0) { break; }
                        }
                        if (escapeVal < 0) {
                            parser.addError("msg.invalid.escape");
                            return Token.ERROR;
                        }
                        addToString(escapeVal);
                        isUnicodeEscapeStart = false;
                    } else {
                        c = getChar();
                        if (c == '\\') {
                            c = getChar();
                            if (c == 'u') {
                                isUnicodeEscapeStart = true;
                                containsEscape = true;
                            } else {
                                parser.addError("msg.illegal.character");
                                return Token.ERROR;
                            }
                        } else {
                            if (c == EOF_CHAR
                                || !Character.isJavaIdentifierPart((char)c))
                            {
                                break;
                            }
                            addToString(c);
                        }
                    }
                }
                ungetChar(c);

                String str = getStringFromBuffer();
                if (!containsEscape) {
                    // OPT we shouldn't have to make a string (object!) to
                    // check if it's a keyword.

                    // Return the corresponding token if it's a keyword
                    int result = stringToKeyword(str);
                    if (result != Token.EOF) {
                        if ((result == Token.LET || result == Token.YIELD) && 
                            parser.compilerEnv.getLanguageVersion() 
                               < Context.VERSION_1_7)
                        {
                            // LET and YIELD are tokens only in 1.7 and later
                            string = result == Token.LET ? "let" : "yield";
                            result = Token.NAME;
                        }
                        if (result != Token.RESERVED) {
                            return result;
                        } else if (!parser.compilerEnv.
                                        isReservedKeywordAsIdentifier())
                        {
                            return result;
                        } else {
                            // If implementation permits to use future reserved
                            // keywords in violation with the EcmaScript,
                            // treat it as name but issue warning
                            parser.addWarning("msg.reserved.keyword", str
                                    // <netbeans>
                                    , new Object[] { null, str }
                                    // </netbeans>
                                    );
                        }
                    }
                }
// <netbeans>
                if (syntaxLexing && "undefined".equals(str)) {
                    return Token.UNDEFINED;
                }
// </netbeans>                
                this.string = (String)allStrings.intern(str);
                return Token.NAME;
            }

            // is it a number?
            if (isDigit(c) || (c == '.' && isDigit(peekChar()))) {

                stringBufferTop = 0;
                int base = 10;

                if (c == '0') {
                    c = getChar();
                    if (c == 'x' || c == 'X') {
                        base = 16;
                        c = getChar();
                    } else if (isDigit(c)) {
                        base = 8;
                    } else {
                        addToString('0');
                    }
                }

                if (base == 16) {
                    while (0 <= Kit.xDigitToInt(c, 0)) {
                        addToString(c);
                        c = getChar();
                    }
                } else {
                    while ('0' <= c && c <= '9') {
                        /*
                         * We permit 08 and 09 as decimal numbers, which
                         * makes our behavior a superset of the ECMA
                         * numeric grammar.  We might not always be so
                         * permissive, so we warn about it.
                         */
                        if (base == 8 && c >= '8') {
                            parser.addWarning("msg.bad.octal.literal",
                                              c == '8' ? "8" : "9"
                                              // <netbeans>
                                              , null
                                              // </netbeans>
                                              );
                            base = 10;
                        }
                        addToString(c);
                        c = getChar();
                    }
                }

                boolean isInteger = true;

                if (base == 10 && (c == '.' || c == 'e' || c == 'E')) {
                    isInteger = false;
                    if (c == '.') {
                        do {
                            addToString(c);
                            c = getChar();
                        } while (isDigit(c));
                    }
                    if (c == 'e' || c == 'E') {
                        addToString(c);
                        c = getChar();
                        if (c == '+' || c == '-') {
                            addToString(c);
                            c = getChar();
                        }
                        if (!isDigit(c)) {
                            parser.addError("msg.missing.exponent");
                            return Token.ERROR;
                        }
                        do {
                            addToString(c);
                            c = getChar();
                        } while (isDigit(c));
                    }
                }
                ungetChar(c);
                String numString = getStringFromBuffer();

                double dval;
                if (base == 10 && !isInteger) {
                    try {
                        // Use Java conversion to number from string...
                        dval = Double.valueOf(numString).doubleValue();
                    }
                    catch (NumberFormatException ex) {
                        parser.addError("msg.caught.nfe");
                        return Token.ERROR;
                    }
                } else {
                    dval = ScriptRuntime.stringToNumber(numString, 0, base);
                }

                this.number = dval;
                return Token.NUMBER;
            }

            // is it a string?
            if (c == '"' || c == '\'') {
                // We attempt to accumulate a string the fast way, by
                // building it directly out of the reader.  But if there
                // are any escaped characters in the string, we revert to
                // building it out of a StringBuffer.

                int quoteChar = c;
                stringBufferTop = 0;

                c = getChar();
            strLoop: while (c != quoteChar) {
                    if (c == '\n' || c == EOF_CHAR) {
                        ungetChar(c);
                        parser.addError("msg.unterminated.string.lit");
// <netbeans>
//                        return Token.ERROR;
                        return Token.STRING_ERROR;
// </netbeans>
                    }

                    if (c == '\\') {
                        // We've hit an escaped character
                        int escapeVal;

                        c = getChar();
                        switch (c) {
                        case 'b': c = '\b'; break;
                        case 'f': c = '\f'; break;
                        case 'n': c = '\n'; break;
                        case 'r': c = '\r'; break;
                        case 't': c = '\t'; break;

                        // \v a late addition to the ECMA spec,
                        // it is not in Java, so use 0xb
                        case 'v': c = 0xb; break;

                        case 'u':
                            // Get 4 hex digits; if the u escape is not
                            // followed by 4 hex digits, use 'u' + the
                            // literal character sequence that follows.
                            int escapeStart = stringBufferTop;
                            addToString('u');
                            escapeVal = 0;
                            for (int i = 0; i != 4; ++i) {
                                c = getChar();
                                escapeVal = Kit.xDigitToInt(c, escapeVal);
                                if (escapeVal < 0) {
                                    continue strLoop;
                                }
                                addToString(c);
                            }
                            // prepare for replace of stored 'u' sequence
                            // by escape value
                            stringBufferTop = escapeStart;
                            c = escapeVal;
                            break;
                        case 'x':
                            // Get 2 hex digits, defaulting to 'x'+literal
                            // sequence, as above.
                            c = getChar();
                            escapeVal = Kit.xDigitToInt(c, 0);
                            if (escapeVal < 0) {
                                addToString('x');
                                continue strLoop;
                            } else {
                                int c1 = c;
                                c = getChar();
                                escapeVal = Kit.xDigitToInt(c, escapeVal);
                                if (escapeVal < 0) {
                                    addToString('x');
                                    addToString(c1);
                                    continue strLoop;
                                } else {
                                    // got 2 hex digits
                                    c = escapeVal;
                                }
                            }
                            break;

                        case '\n':
                            // Remove line terminator after escape to follow
                            // SpiderMonkey and C/C++
                            c = getChar();
                            continue strLoop;

                        default:
                            if ('0' <= c && c < '8') {
                                int val = c - '0';
                                c = getChar();
                                if ('0' <= c && c < '8') {
                                    val = 8 * val + c - '0';
                                    c = getChar();
                                    if ('0' <= c && c < '8' && val <= 037) {
                                        // c is 3rd char of octal sequence only
                                        // if the resulting val <= 0377
                                        val = 8 * val + c - '0';
                                        c = getChar();
                                    }
                                }
                                ungetChar(c);
                                c = val;
                            }
                        }
                    }
                    addToString(c);
                    c = getChar();
                }

                String str = getStringFromBuffer();
                this.string = (String)allStrings.intern(str);
                return Token.STRING;
            }

            switch (c) {
            case ';': return Token.SEMI;
            case '[': return Token.LB;
            case ']': return Token.RB;
            case '{': return Token.LC;
            case '}': return Token.RC;
            case '(': return Token.LP;
            case ')': return Token.RP;
            case ',': return Token.COMMA;
            case '?': return Token.HOOK;
            case ':':
                if (matchChar(':')) {
                    return Token.COLONCOLON;
                } else {
                    return Token.COLON;
                }
            case '.':
                if (matchChar('.')) {
                    return Token.DOTDOT;
                } else if (matchChar('(')) {
                    return Token.DOTQUERY;
                } else {
                    return Token.DOT;
                }

            case '|':
                if (matchChar('|')) {
                    return Token.OR;
                } else if (matchChar('=')) {
                    return Token.ASSIGN_BITOR;
                } else {
                    return Token.BITOR;
                }

            case '^':
                if (matchChar('=')) {
                    return Token.ASSIGN_BITXOR;
                } else {
                    return Token.BITXOR;
                }

            case '&':
                if (matchChar('&')) {
                    return Token.AND;
                } else if (matchChar('=')) {
                    return Token.ASSIGN_BITAND;
                } else {
                    return Token.BITAND;
                }

            case '=':
                if (matchChar('=')) {
                    if (matchChar('='))
                        return Token.SHEQ;
                    else
                        return Token.EQ;
                } else {
                    return Token.ASSIGN;
                }

            case '!':
                if (matchChar('=')) {
                    if (matchChar('='))
                        return Token.SHNE;
                    else
                        return Token.NE;
                } else {
                    return Token.NOT;
                }

            case '<':
                /* NB:treat HTML begin-comment as comment-till-eol */
                if (matchChar('!')) {
                    if (matchChar('-')) {
                        if (matchChar('-')) {
                            skipLine();
                            continue retry;
                        }
                        ungetCharIgnoreLineEnd('-');
                    }
                    ungetCharIgnoreLineEnd('!');
                }
                if (matchChar('<')) {
                    if (matchChar('=')) {
                        return Token.ASSIGN_LSH;
                    } else {
                        return Token.LSH;
                    }
                } else {
                    if (matchChar('=')) {
                        return Token.LE;
                    } else {
                        return Token.LT;
                    }
                }

            case '>':
                if (matchChar('>')) {
                    if (matchChar('>')) {
                        if (matchChar('=')) {
                            return Token.ASSIGN_URSH;
                        } else {
                            return Token.URSH;
                        }
                    } else {
                        if (matchChar('=')) {
                            return Token.ASSIGN_RSH;
                        } else {
                            return Token.RSH;
                        }
                    }
                } else {
                    if (matchChar('=')) {
                        return Token.GE;
                    } else {
                        return Token.GT;
                    }
                }

            case '*':
                if (matchChar('=')) {
                    return Token.ASSIGN_MUL;
                } else {
                    return Token.MUL;
                }

            case '/':
                // is it a // comment?
                if (matchChar('/')) {
                    skipLine();
                    // <netbeans>
                    // Rhino doesn't return comment tokens
                    // ...but I will!
                    //continue retry;
                    if (syntaxLexing) {
                        return Token.LINE_COMMENT;
                    } else {
                        continue retry;
                    }
                    // </netbeans>
                }
                if (matchChar('*')) {
                    boolean lookForSlash = false;
                    for (;;) {
                        c = getChar();
                        if (c == EOF_CHAR) {
                            parser.addError("msg.unterminated.comment");
                            return Token.ERROR;
                        } else if (c == '*') {
                            lookForSlash = true;
                        } else if (c == '/') {
                            if (lookForSlash) {
                                // <netbeans>
                                // Rhino doesn't return comment tokens
                                // ...but I will!
                                //continue retry;
                                if (syntaxLexing) {
                                    return Token.BLOCK_COMMENT;
                                } else {
                                    continue retry;
                                }
                                // </netbeans>
                            }
                        } else {
                            lookForSlash = false;
                        }
                    }
                }

                // <netbeans>
                if (syntaxLexing && divIsRegexp) {
                    try {
                        readRegExp(Token.DIV);
                    } catch (Throwable t) {
                        return Token.REGEXP_ERROR;
                    }
                    return Token.REGEXP;
                }
                // </netbeans>
                
                if (matchChar('=')) {
                    return Token.ASSIGN_DIV;
                } else {
                    return Token.DIV;
                }

            case '%':
                if (matchChar('=')) {
                    return Token.ASSIGN_MOD;
                } else {
                    return Token.MOD;
                }

            case '~':
                return Token.BITNOT;

            case '+':
                if (matchChar('=')) {
                    return Token.ASSIGN_ADD;
                } else if (matchChar('+')) {
                    return Token.INC;
                } else {
                    return Token.ADD;
                }

            case '-':
                if (matchChar('=')) {
                    c = Token.ASSIGN_SUB;
                } else if (matchChar('-')) {
                    if (!dirtyLine) {
                        // treat HTML end-comment after possible whitespace
                        // after line start as comment-utill-eol
                        if (matchChar('>')) {
                            skipLine();
                            continue retry;
                        }
                    }
                    c = Token.DEC;
                } else {
                    c = Token.SUB;
                }
                dirtyLine = true;
                return c;

            default:
                parser.addError("msg.illegal.character");
                return Token.ERROR;
            }
        }
    }

    private static boolean isAlpha(int c)
    {
        // Use 'Z' < 'a'
        if (c <= 'Z') {
            return 'A' <= c;
        } else {
            return 'a' <= c && c <= 'z';
        }
    }

    static boolean isDigit(int c)
    {
        return '0' <= c && c <= '9';
    }

    /* As defined in ECMA.  jsscan.c uses C isspace() (which allows
     * \v, I think.)  note that code in getChar() implicitly accepts
     * '\r' == \u000D as well.
     */
    static boolean isJSSpace(int c)
    {
        if (c <= 127) {
            return c == 0x20 || c == 0x9 || c == 0xC || c == 0xB;
        } else {
            return c == 0xA0
                || Character.getType((char)c) == Character.SPACE_SEPARATOR;
        }
    }

    private static boolean isJSFormatChar(int c)
    {
        return c > 127 && Character.getType((char)c) == Character.FORMAT;
    }

    /**
     * Parser calls the method when it gets / or /= in literal context.
     */
// <netbeans>
    public
// </netbeans>
    void readRegExp(int startToken)
        throws IOException
    {
        stringBufferTop = 0;
        if (startToken == Token.ASSIGN_DIV) {
            // Miss-scanned /=
            addToString('=');
        } else {
            if (startToken != Token.DIV) Kit.codeBug();
        }

        boolean inCharSet = false; // true if inside a '['..']' pair
        int c;
        while ((c = getChar()) != '/' || inCharSet) {
            if (c == '\n' || c == EOF_CHAR) {
                ungetChar(c);
                throw parser.reportError("msg.unterminated.re.lit");
            }
            if (c == '\\') {
                addToString(c);
                c = getChar();
            } else if (c == '[') {
                inCharSet = true;
            } else if (c == ']') {
                inCharSet = false;
            }
            addToString(c);
        }
        int reEnd = stringBufferTop;

        while (true) {
            if (matchChar('g'))
                addToString('g');
            else if (matchChar('i'))
                addToString('i');
            else if (matchChar('m'))
                addToString('m');
            else
                break;
        }

        if (isAlpha(peekChar())) {
            throw parser.reportError("msg.invalid.re.flag");
        }

        this.string = new String(stringBuffer, 0, reEnd);
        this.regExpFlags = new String(stringBuffer, reEnd,
                                      stringBufferTop - reEnd);
    }

    boolean isXMLAttribute()
    {
        return xmlIsAttribute;
    }
    
    // <netbeans>
    private boolean skipUngetCheck;
    // </netbeans>

    int getFirstXMLToken() throws IOException
    {
        xmlOpenTagsCount = 0;
        xmlIsAttribute = false;
        xmlIsTagContent = false;
        // <netbeans>
        try {
            // If we try to unget the < when the previous character was a newline, the
            // ungetchar call will abort. However, we're using unget in a very specific
            // case here - we KNOW we're just going to read it again in the getNextXMLToken
            // call so we don't have to abort in ungetchar for this case, so set a flag
            // which suppresses the newline check in ungetchar. See issue #131832 for 
            // a testcase where this fix was needed.
            skipUngetCheck = true;
        // </netbeans>
        ungetChar('<');
        // <netbeans>
        } finally {
            skipUngetCheck = false;
        }
        // </netbeans>
        
        return getNextXMLToken();
    }

    int getNextXMLToken() throws IOException
    {
        stringBufferTop = 0; // remember the XML

        for (int c = getChar(); c != EOF_CHAR; c = getChar()) {
            if (xmlIsTagContent) {
                switch (c) {
                case '>':
                    addToString(c);
                    xmlIsTagContent = false;
                    xmlIsAttribute = false;
                    break;
                case '/':
                    addToString(c);
                    if (peekChar() == '>') {
                        c = getChar();
                        addToString(c);
                        xmlIsTagContent = false;
                        xmlOpenTagsCount--;
                    }
                    break;
                case '{':
                    ungetChar(c);
                    this.string = getStringFromBuffer();
                    return Token.XML;
                case '\'':
                case '"':
                    addToString(c);
                    if (!readQuotedString(c)) return Token.ERROR;
                    break;
                case '=':
                    addToString(c);
                    xmlIsAttribute = true;
                    break;
                case ' ':
                case '\t':
                case '\r':
                case '\n':
                    addToString(c);
                    break;
                default:
                    addToString(c);
                    xmlIsAttribute = false;
                    break;
                }

                if (!xmlIsTagContent && xmlOpenTagsCount == 0) {
                    this.string = getStringFromBuffer();
                    return Token.XMLEND;
                }
            } else {
                switch (c) {
                case '<':
                    addToString(c);
                    c = peekChar();
                    switch (c) {
                    case '!':
                        c = getChar(); // Skip !
                        addToString(c);
                        c = peekChar();
                        switch (c) {
                        case '-':
                            c = getChar(); // Skip -
                            addToString(c);
                            c = getChar();
                            if (c == '-') {
                                addToString(c);
                                if(!readXmlComment()) return Token.ERROR;
                            } else {
                                // throw away the string in progress
                                stringBufferTop = 0;
                                this.string = null;
                                parser.addError("msg.XML.bad.form");
                                return Token.ERROR;
                            }
                            break;
                        case '[':
                            c = getChar(); // Skip [
                            addToString(c);
                            if (getChar() == 'C' &&
                                getChar() == 'D' &&
                                getChar() == 'A' &&
                                getChar() == 'T' &&
                                getChar() == 'A' &&
                                getChar() == '[')
                            {
                                addToString('C');
                                addToString('D');
                                addToString('A');
                                addToString('T');
                                addToString('A');
                                addToString('[');
                                if (!readCDATA()) return Token.ERROR;

                            } else {
                                // throw away the string in progress
                                stringBufferTop = 0;
                                this.string = null;
                                parser.addError("msg.XML.bad.form");
                                return Token.ERROR;
                            }
                            break;
                        default:
                            if(!readEntity()) return Token.ERROR;
                            break;
                        }
                        break;
                    case '?':
                        c = getChar(); // Skip ?
                        addToString(c);
                        if (!readPI()) return Token.ERROR;
                        break;
                    case '/':
                        // End tag
                        c = getChar(); // Skip /
                        addToString(c);
                        if (xmlOpenTagsCount == 0) {
                            // throw away the string in progress
                            stringBufferTop = 0;
                            this.string = null;
                            parser.addError("msg.XML.bad.form");
                            return Token.ERROR;
                        }
                        xmlIsTagContent = true;
                        xmlOpenTagsCount--;
                        break;
                    default:
                        // Start tag
                        xmlIsTagContent = true;
                        xmlOpenTagsCount++;
                        break;
                    }
                    break;
                case '{':
                    ungetChar(c);
                    this.string = getStringFromBuffer();
                    return Token.XML;
                default:
                    addToString(c);
                    break;
                }
            }
        }

        stringBufferTop = 0; // throw away the string in progress
        this.string = null;
        parser.addError("msg.XML.bad.form");
        return Token.ERROR;
    }

    /**
     *
     */
    private boolean readQuotedString(int quote) throws IOException
    {
        for (int c = getChar(); c != EOF_CHAR; c = getChar()) {
            addToString(c);
            if (c == quote) return true;
        }

        stringBufferTop = 0; // throw away the string in progress
        this.string = null;
        parser.addError("msg.XML.bad.form");
        return false;
    }

    /**
     *
     */
    private boolean readXmlComment() throws IOException
    {
        for (int c = getChar(); c != EOF_CHAR;) {
            addToString(c);
            if (c == '-' && peekChar() == '-') {
                c = getChar();
                addToString(c);
                if (peekChar() == '>') {
                    c = getChar(); // Skip >
                    addToString(c);
                    return true;
                } else {
                    continue;
                }
            }
            c = getChar();
        }

        stringBufferTop = 0; // throw away the string in progress
        this.string = null;
        parser.addError("msg.XML.bad.form");
        return false;
    }

    /**
     *
     */
    private boolean readCDATA() throws IOException
    {
        for (int c = getChar(); c != EOF_CHAR;) {
            addToString(c);
            if (c == ']' && peekChar() == ']') {
                c = getChar();
                addToString(c);
                if (peekChar() == '>') {
                    c = getChar(); // Skip >
                    addToString(c);
                    return true;
                } else {
                    continue;
                }
            }
            c = getChar();
        }

        stringBufferTop = 0; // throw away the string in progress
        this.string = null;
        parser.addError("msg.XML.bad.form");
        return false;
    }

    /**
     *
     */
    private boolean readEntity() throws IOException
    {
        int declTags = 1;
        for (int c = getChar(); c != EOF_CHAR; c = getChar()) {
            addToString(c);
            switch (c) {
            case '<':
                declTags++;
                break;
            case '>':
                declTags--;
                if (declTags == 0) return true;
                break;
            }
        }

        stringBufferTop = 0; // throw away the string in progress
        this.string = null;
        parser.addError("msg.XML.bad.form");
        return false;
    }

    /**
     *
     */
    private boolean readPI() throws IOException
    {
        for (int c = getChar(); c != EOF_CHAR; c = getChar()) {
            addToString(c);
            if (c == '?' && peekChar() == '>') {
                c = getChar(); // Skip >
                addToString(c);
                return true;
            }
        }

        stringBufferTop = 0; // throw away the string in progress
        this.string = null;
        parser.addError("msg.XML.bad.form");
        return false;
    }

    private String getStringFromBuffer()
    {
        return new String(stringBuffer, 0, stringBufferTop);
    }

    private void addToString(int c)
    {
        int N = stringBufferTop;
        if (N == stringBuffer.length) {
            char[] tmp = new char[stringBuffer.length * 2];
            System.arraycopy(stringBuffer, 0, tmp, 0, N);
            stringBuffer = tmp;
        }
        stringBuffer[N] = (char)c;
        stringBufferTop = N + 1;
    }

    private void ungetChar(int c)
    {
// <netbeans>
        if (lexerInput != null) {
            lexerInput.backup(1);
            return;
        }
// </netbeans>
        
        // can not unread past across line boundary
        // <netbeans>
        // For the XML processing use of ungetChar we need this -- see for example issue 131832
        if (!skipUngetCheck)
        // </netbeans>
          if (ungetCursor != 0 && ungetBuffer[ungetCursor - 1] == '\n')
            Kit.codeBug();
        ungetBuffer[ungetCursor++] = c;
    }

    private boolean matchChar(int test) throws IOException
    {
        int c = getCharIgnoreLineEnd();
        if (c == test) {
            return true;
        } else {
            ungetCharIgnoreLineEnd(c);
            return false;
        }
    }

    private int peekChar() throws IOException
    {
        int c = getChar();
        ungetChar(c);
        return c;
    }

    private int getChar() throws IOException
    {
// <netbeans>
        if (lexerInput != null) {
            int c = lexerInput.read();
            //if (c == LexerInput.EOF) {
            //    return EOF_CHAR;
            //} else {
            //    return c;
            //}
            assert LexerInput.EOF == EOF_CHAR;            
            return c;
        }
// </netbeans>

        if (ungetCursor != 0) {
            return ungetBuffer[--ungetCursor];
        }

        for(;;) {
            int c;
            if (sourceString != null) {
                if (sourceCursor == sourceEnd) {
                    hitEOF = true;
                    return EOF_CHAR;
                }
                c = sourceString.charAt(sourceCursor++);
            } else {
                if (sourceCursor == sourceEnd) {
                    if (!fillSourceBuffer()) {
                        hitEOF = true;
                        return EOF_CHAR;
                    }
                }
                c = sourceBuffer[sourceCursor++];
            }

            if (lineEndChar >= 0) {
                if (lineEndChar == '\r' && c == '\n') {
                    lineEndChar = '\n';
                    continue;
                }
                lineEndChar = -1;
                lineStart = sourceCursor - 1;
                lineno++;
            }

            if (c <= 127) {
                if (c == '\n' || c == '\r') {
                    lineEndChar = c;
                    c = '\n';
                }
            } else {
                if (isJSFormatChar(c)) {
                    continue;
                }
                if (ScriptRuntime.isJSLineTerminator(c)) {
                    lineEndChar = c;
                    c = '\n';
                }
            }
            return c;
        }
    }
    
    private int getCharIgnoreLineEnd() throws IOException
    {
// <netbeans>
        if (lexerInput != null) {
            for(;;) {
                int c = lexerInput.read();
                if (c == LexerInput.EOF) {
                    hitEOF = true;
                    return EOF_CHAR;
                }
                assert LexerInput.EOF == EOF_CHAR;

                if (c <= 127) {
                    if (c == '\n' || c == '\r') {
                        lineEndChar = c;
                        c = '\n';
                    }
                } else {
                    if (isJSFormatChar(c)) {
                        continue;
                    }
                    if (ScriptRuntime.isJSLineTerminator(c)) {
                        lineEndChar = c;
                        c = '\n';
                    }
                }
                return c;
            }
        }
// </netbeans>

        if (ungetCursor != 0) {
            return ungetBuffer[--ungetCursor];
        }

        for(;;) {
            int c;
            if (sourceString != null) {
                if (sourceCursor == sourceEnd) {
                    hitEOF = true;
                    return EOF_CHAR;
                }
                c = sourceString.charAt(sourceCursor++);
            } else {
                if (sourceCursor == sourceEnd) {
                    if (!fillSourceBuffer()) {
                        hitEOF = true;
                        return EOF_CHAR;
                    }
                }
                c = sourceBuffer[sourceCursor++];
            }

            if (c <= 127) {
                if (c == '\n' || c == '\r') {
                    lineEndChar = c;
                    c = '\n';
                }
            } else {
                if (isJSFormatChar(c)) {
                    continue;
                }
                if (ScriptRuntime.isJSLineTerminator(c)) {
                    lineEndChar = c;
                    c = '\n';
                }
            }
            return c;
        }
    }
    
    private void ungetCharIgnoreLineEnd(int c)
    {
// <netbeans>
        if (lexerInput != null) {
            lexerInput.backup(1);
            return;
        }
// </netbeans>
        
        ungetBuffer[ungetCursor++] = c;
    }
    
    private void skipLine() throws IOException
    {
        // skip to end of line
        int c;
        while ((c = getChar()) != EOF_CHAR && c != '\n') { }
        ungetChar(c);
    }

    final int getOffset()
    {
        // XXX This is not right for the new lexer approach
 // <netbeans>: Shouldn't this subtract the ungetCursor?  </netbeans>
        int n = sourceCursor - lineStart;
        if (lineEndChar >= 0) { --n; }
        return n;
    }

// <netbeans>
    public final int getBufferOffset() {
        int n = sourceCursor - ungetCursor;
        return n;
    }
// </netbeans>
    
    final String getLine()
    {
// <netbeans>
        if (lexerInput != null) {
            // We should never be calling getLine() when syntax lexing, this
            // is used for parser error messages
            //Kit.codeBug();
            return "?";
        }
// </netbeans>
        if (sourceString != null) {
            // String case
            int lineEnd = sourceCursor;
            if (lineEndChar >= 0) {
                --lineEnd;
            } else {
                for (; lineEnd != sourceEnd; ++lineEnd) {
                    int c = sourceString.charAt(lineEnd);
                    if (ScriptRuntime.isJSLineTerminator(c)) {
                        break;
                    }
                }
            }
            return sourceString.substring(lineStart, lineEnd);
        } else {
            // Reader case
            int lineLength = sourceCursor - lineStart;
            if (lineEndChar >= 0) {
                --lineLength;
            } else {
                // Read until the end of line
                for (;; ++lineLength) {
                    int i = lineStart + lineLength;
                    if (i == sourceEnd) {
                        try {
                            if (!fillSourceBuffer()) { break; }
                        } catch (IOException ioe) {
                            // ignore it, we're already displaying an error...
                            break;
                        }
                        // i recalculuation as fillSourceBuffer can move saved
                        // line buffer and change lineStart
                        i = lineStart + lineLength;
                    }
                    int c = sourceBuffer[i];
                    if (ScriptRuntime.isJSLineTerminator(c)) {
                        break;
                    }
                }
            }
            return new String(sourceBuffer, lineStart, lineLength);
        }
    }

    private boolean fillSourceBuffer() throws IOException
    {
// <netbeans>
        if (lexerInput != null) {
            return true;
        }
// </netbeans>
        
        if (sourceString != null) Kit.codeBug();
        if (sourceEnd == sourceBuffer.length) {
            if (lineStart != 0) {
                System.arraycopy(sourceBuffer, lineStart, sourceBuffer, 0,
                                 sourceEnd - lineStart);
                sourceEnd -= lineStart;
                sourceCursor -= lineStart;
                lineStart = 0;
            } else {
                char[] tmp = new char[sourceBuffer.length * 2];
                System.arraycopy(sourceBuffer, 0, tmp, 0, sourceEnd);
                sourceBuffer = tmp;
            }
        }
        int n = sourceReader.read(sourceBuffer, sourceEnd,
                                  sourceBuffer.length - sourceEnd);
        if (n < 0) {
            return false;
        }
        sourceEnd += n;
        return true;
    }

// <netbeans>
    /** Set whether comment tokens should be included as return values
     * from getToken() or not. In parsing mode, it should not. But when
     * doing lexical analysis as part of syntax highlighting for example,
     * it should.
     */
    public void setSyntaxLexing(boolean syntaxLexing) {
        this.syntaxLexing = syntaxLexing;
    }

    private boolean syntaxLexing;
    
    
    /** Restore state from the given Object which was earlier created via {@link toState} */
    public void fromState(Object object) {
        if (object == null) {
            return;
        }
        LexingState ls = (LexingState)object;
        ls.restore(this);
    }
    
    /** Record all state that needs to be restored to resume lexing from the same input position */
    public Object toState() {
        LexingState ls = new LexingState(this);
        
        return ls;
    }
    
    private static class LexingState {
        private static final int DIV_REGEXP = 1 << 0;
        private static final int DIRTY = 1 << 1;
        private static final int EOF = 1 << 2;
        private static final int XML_ATTR = 1 << 3;
        private static final int XML_TAG = 1 << 4;
        private static final int STRING_MODE_SHIFT = 5;

        private int flags;
        private String regExpFlags;
        private String bufferedString;
        private int xmlOpenTagsCount;
        private String string;

        LexingState(TokenStream stream) {
            this.regExpFlags = stream.regExpFlags;
            if (stream.stringBufferTop > 0) {
                this.bufferedString = new String(TokenStream.stringBuffer, 0, stream.stringBufferTop);
            }
            this.xmlOpenTagsCount = stream.xmlOpenTagsCount;
            this.string = stream.string;
            int state = 0;
            if (stream.dirtyLine) {
                state += DIRTY;
            }
            if (stream.divIsRegexp) {
                state += DIV_REGEXP;
            }
            if (stream.hitEOF) {
                state += EOF;
            }
            if (stream.xmlIsAttribute) {
                state += XML_ATTR;
            }
            if (stream.xmlIsTagContent) {
                state += XML_TAG;
            }
            if (stream.stringMode != 0) {
                state += (stream.stringMode << STRING_MODE_SHIFT);
            }
            this.flags = state;
        }

        public void restore(TokenStream stream) {
            stream.regExpFlags = this.regExpFlags;
            stream.dirtyLine =  ((flags & DIRTY) != 0);
            stream.divIsRegexp = ((flags & DIV_REGEXP) != 0);            
            stream.hitEOF = ((flags & EOF) != 0);
            stream.xmlIsAttribute = ((flags & XML_ATTR) != 0);
            stream.xmlIsTagContent = ((flags & XML_TAG) != 0);
            if (this.bufferedString != null) {
                assert TokenStream.stringBuffer.length >= this.bufferedString.length();
                char[] chars = this.bufferedString.toCharArray();
                System.arraycopy(chars, 0, TokenStream.stringBuffer, 0, chars.length);
                stream.stringBufferTop = bufferedString.length();
            } else {
                stream.stringBufferTop = 0;
            }
            stream.xmlOpenTagsCount = this.xmlOpenTagsCount;
            stream.stringMode = (short) (flags >> STRING_MODE_SHIFT);
            stream.string = this.string;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TokenStream.LexingState other = (TokenStream.LexingState) obj;
            if (this.flags != other.flags) {
                return false;
            }
            if (this.regExpFlags != other.regExpFlags && (this.regExpFlags == null || !this.regExpFlags.equals(other.regExpFlags))) {
                return false;
            }
            if (this.bufferedString != other.bufferedString && (this.bufferedString == null || !this.bufferedString.equals(other.bufferedString))) {
                return false;
            }
            if (this.string != other.string && (this.string == null || !this.string.equals(other.string))) {
                return false;
            }
            if (this.xmlOpenTagsCount != other.xmlOpenTagsCount) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + (this.regExpFlags != null ? this.regExpFlags.hashCode() : 0);
            hash = 37 * hash + (this.bufferedString != null ? this.bufferedString.hashCode() : 0);
            hash = 37 * hash + this.flags;
            hash = 37 * hash + (this.string != null ? this.string.hashCode() : 0);
            hash = 37 * hash + (this.regExpFlags != null ? this.regExpFlags.hashCode() : 0);
            hash = 37 * hash + this.xmlOpenTagsCount;
            return hash;
        }
        
        private String toStateString(int localState) {
            StringBuilder sb = new StringBuilder();

            if ((localState & DIV_REGEXP) != 0) {
                sb.append("divregexp|");
            }

            if ((localState & DIRTY) != 0) {
                sb.append("dirty|");
            }

            if ((localState & EOF) != 0) {
                sb.append("eof|");
            }

            if ((localState & XML_TAG) != 0) {
                sb.append("xmltag|");
            }

            if ((localState & XML_ATTR) != 0) {
                sb.append("xmlattr|");
            }

            sb.append("stringmode="+(localState >> STRING_MODE_SHIFT));
            
            String s = sb.toString();

            return s;
        }
        
        @Override 
        public String toString() {
            return "LS(regexp=" + this.regExpFlags + ",string=" + this.bufferedString + 
                    "xmlOpen=" + this.xmlOpenTagsCount + ",flags=" + toStateString(this.flags) + ",stringlit=" + this.string + ")";
        }
    }
    
    // Regular expressions in JavaScript are tricky to detect at the lexical level.
    // Rhino usually detects this at parse time. However, that won't do - if I have code
    // like this:
    //      foo(/ba))/)
    // then I'll end up with some extra )'s that the lexer think are unbalanced right parens,
    // rather than regexp literal contents. With quotes in there, I can also end up with
    // unterminated strings etc -- all of these confused features driven on lexical features:
    // indentation, bracket matching, etc.
    private boolean divIsRegexp = true;

    // I need to split strings and regexps up into begin, literal, end tokens
    // (for bracket matching and completion purposes).
    private static final int NO_LITERAL = 0;
    private static final int IN_ERROR   = 1;
    private static final int IN_STRING  = 2;
    private static final int END_STRING = 3;
    private static final int IN_REGEXP  = 4;
    private static final int END_REGEXP = 5;
    private short stringMode = NO_LITERAL;
// </netbeans>
    
    // stuff other than whitespace since start of line
    private boolean dirtyLine;

    String regExpFlags;

    // Set this to an initial non-null value so that the Parser has
    // something to retrieve even if an error has occurred and no
    // string is found.  Fosters one class of error, but saves lots of
    // code.
    private String string = "";
    private double number;

// XXX <netbeans> I changed this from instance to static but that may be risky? Lexing same time as parsing?
    private static char[] stringBuffer = new char[128];
    private int stringBufferTop;
    private ObjToIntMap allStrings = new ObjToIntMap(50);

    // Room to backtrace from to < on failed match of the last - in <!--
    private final int[] ungetBuffer = new int[3];
    private int ungetCursor;

    private boolean hitEOF = false;

    private int lineStart = 0;
    private int lineno;
    private int lineEndChar = -1;

    private String sourceString;
    private Reader sourceReader;
    private char[] sourceBuffer;
    private int sourceEnd;
    private int sourceCursor;

    // for xml tokenizer
    private boolean xmlIsAttribute;
    private boolean xmlIsTagContent;
    private int xmlOpenTagsCount;

    private Parser parser;
}