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
 *   Mike Ang
 *   Igor Bukanov
 *   Yuh-Ruey Chen
 *   Ethan Hugg
 *   Bob Jervis
 *   Terry Lucas
 *   Mike McCabe
 *   Milen Nankov
 *   Norris Boyd
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

import java.io.Reader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// <netbeans>
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
// </netbeans>

/**
 * This class implements the JavaScript parser.
 *
 * It is based on the C source files jsparse.c and jsparse.h
 * in the jsref package.
 *
 * @see TokenStream
 *
 * @author Mike McCabe
 * @author Brendan Eich
 */

public class Parser
{
    // <netbeans>
    // Keep in sync with JsModel.GENERATED_IDENTIFIER and unit tests -
    // but NOTE -- there should be NO SPACES AROUND the identifier here - the
    // parser strips the spaces during parsing.
    private static final String GENERATED_IDENTIFIER = "__UNKNOWN__"; // NOI18N
    // </netbeans>

    // TokenInformation flags : currentFlaggedToken stores them together
    // with token type
// <netbeans>
public
// </netbeans>
    final static int
        CLEAR_TI_MASK  = 0xFFFF,   // mask to clear token information bits
        TI_AFTER_EOL   = 1 << 16,  // first token of the source line
        TI_CHECK_LABEL = 1 << 17;  // indicates to check for label

    CompilerEnvirons compilerEnv;
    private ErrorReporter errorReporter;
    private String sourceURI;
    boolean calledByCompileFunction;

    private TokenStream ts;
    // <netbeans>
    private int peekedTokenStart;
    private int peekedTokenEnd;
    private int matchedTokenStart;
    private int matchedTokenEnd;
    private int matchedToken;
    private boolean jsonMode;
    public void setJsonMode(boolean jsonMode) {
        this.jsonMode = jsonMode;
    }

    // </netbeans>
    private int currentFlaggedToken;
    private int syntaxErrorCount;

    private IRFactory nf;

    private int nestingOfFunction;

    private Decompiler decompiler;
    private String encodedSource;

// The following are per function variables and should be saved/restored
// during function parsing.
// XXX Move to separated class?
    ScriptOrFnNode currentScriptOrFn;
    Node.Scope currentScope;
    private int nestingOfWith;
    private Map<String,Node> labelSet; // map of label names into nodes
    private ObjArray loopSet;
    private ObjArray loopAndSwitchSet;
    private int endFlags;
// end of per function variables
    
    public int getCurrentLineNumber() {
        return ts.getLineno();
    }

    // Exception to unwind
    private static class ParserException extends RuntimeException
    {
        static final long serialVersionUID = 5882582646773765630L;
    }

    public Parser(CompilerEnvirons compilerEnv, ErrorReporter errorReporter)
    {
        this.compilerEnv = compilerEnv;
        this.errorReporter = errorReporter;
    }

    protected Decompiler createDecompiler(CompilerEnvirons compilerEnv)
    {
        // <netbeans>
        // We don't need the decompiler - wen're not going to run the code
        //return new Decompiler();
        return new NoOpDecompiler();
        // </netbeans>
    }

    // <netbeans>
    private class NoOpDecompiler extends Decompiler {
        @Override
        String getEncodedSource()
        {
            return null;
        }


        @Override
        int getCurrentOffset()
        {
            return 0;
        }

        @Override
        int markFunctionStart(int functionType)
        {
            return 0;
        }

        @Override
        int markFunctionEnd(int functionStart)
        {
            return 0;
        }

        @Override
        void addToken(int token)
        {
        }

        @Override
        void addEOL(int token)
        {
        }

        @Override
        void addName(String str)
        {
        }

        @Override
        void addString(String str)
        {
        }

        @Override
        void addRegexp(String regexp, String flags)
        {
        }

        @Override
        void addNumber(double n)
        {
        }
    }
    // </netbeans>

    void addStrictWarning(String messageId, String messageArg
            // <netbeans>
            // Additional error parameters that can be passed to the error handler
            , Object params
            // </netbeans>
            )
    {
        if (compilerEnv.isStrictMode())
            addWarning(messageId, messageArg
                // <netbeans>
                // Additional error parameters that can be passed to the error handler
                , params
                // </netbeans>
                    );
    }

    void addWarning(String messageId, String messageArg
            // <netbeans>
            // Additional error parameters that can be passed to the error handler
            , Object params
            // </netbeans>
            )
    {
        String message = ScriptRuntime.getMessage1(messageId, messageArg);
        if (compilerEnv.reportWarningAsError()) {
            ++syntaxErrorCount;
            errorReporter.error(message, sourceURI, ts.getLineno(),
                                ts.getLine(), ts.getOffset()
                                // <netbeans>
                                , messageId, params
                                // </netbeans>
                                );
        } else
            errorReporter.warning(message, sourceURI, ts.getLineno(),
                                  ts.getLine(), ts.getOffset()
                                // <netbeans>
                                , messageId, params
                                // </netbeans>
                                  );
    }

    void addError(String messageId)
    {
        ++syntaxErrorCount;
        String message = ScriptRuntime.getMessage0(messageId);
        errorReporter.error(message, sourceURI, ts.getLineno(),
                            ts.getLine(), ts.getOffset()
                            // <netbeans>
                            , messageId, null
                            // </netbeans>
                            );
    }

    void addError(String messageId, String messageArg
            // <netbeans>
            // Additional error parameters that can be passed to the error handler
            , Object params
            // </netbeans>
            )
    {
        ++syntaxErrorCount;
        String message = ScriptRuntime.getMessage1(messageId, messageArg);
        errorReporter.error(message, sourceURI, ts.getLineno(),
                            ts.getLine(), ts.getOffset()
                            // <netbeans>
                            , messageId, params
                            // </netbeans>
                            );
    }

    RuntimeException reportError(String messageId)
    {
        addError(messageId);

        // Throw a ParserException exception to unwind the recursive descent
        // parse.
        throw new ParserException();
    }

    private int peekToken()
        throws IOException
    {
        int tt = currentFlaggedToken;
        if (tt == Token.EOF) {
// <netbeans>
            peekedTokenStart = ts.getBufferOffset();
// </netbeans>
            tt = ts.getToken();
            if (tt == Token.EOL) {
                do {
// <netbeans>
                    peekedTokenStart = ts.getBufferOffset();
// </netbeans>
                    tt = ts.getToken();
                } while (tt == Token.EOL);
                tt |= TI_AFTER_EOL;
            }
            currentFlaggedToken = tt;
// <netbeans>
            peekedTokenStart += ts.seenSpaces();
            peekedTokenEnd = ts.getBufferOffset();
// </netbeans>
        }
        return tt & CLEAR_TI_MASK;
    }

    private int peekFlaggedToken()
        throws IOException
    {
        peekToken();
        return currentFlaggedToken;
    }

    private void consumeToken()
    {
// <netbeans>
        // Consume token gets called as part of nextToken() for example so doesn't help us very much
        //currentFlaggedTokenOffset = -1;
        matchedToken = currentFlaggedToken;
        matchedTokenStart = peekedTokenStart;
        matchedTokenEnd = peekedTokenEnd;
// </netbeans>
        currentFlaggedToken = Token.EOF;
    }

    private int nextToken()
        throws IOException
    {
        int tt = peekToken();
        consumeToken();
        return tt;
    }

    private int nextFlaggedToken()
        throws IOException
    {
        peekToken();
        int ttFlagged = currentFlaggedToken;
        consumeToken();
        return ttFlagged;
    }

    private boolean matchToken(int toMatch)
        throws IOException
    {
        int tt = peekToken();
        if (tt != toMatch) {
            return false;
        }
        consumeToken();
        return true;
    }

    private int peekTokenOrEOL()
        throws IOException
    {
        int tt = peekToken();
        // Check for last peeked token flags
        if ((currentFlaggedToken & TI_AFTER_EOL) != 0) {
            tt = Token.EOL;
        }
        return tt;
    }

    private void setCheckForLabel()
    {
        if ((currentFlaggedToken & CLEAR_TI_MASK) != Token.NAME)
            throw Kit.codeBug();
        currentFlaggedToken |= TI_CHECK_LABEL;
    }

    private void mustMatchToken(int toMatch, String messageId)
        throws IOException, ParserException
    {
        if (!matchToken(toMatch)) {
            reportError(messageId);
        }
    }

    private void mustHaveXML()
    {
        if (!compilerEnv.isXmlAvailable()) {
            reportError("msg.XML.not.available");
        }
    }

    public String getEncodedSource()
    {
        return encodedSource;
    }

    public boolean eof()
    {
        return ts.eof();
    }

    boolean insideFunction()
    {
        return nestingOfFunction != 0;
    }
    
    void pushScope(Node node) {
        Node.Scope scopeNode = (Node.Scope) node;
        if (scopeNode.getParentScope() != null) throw Kit.codeBug();
        scopeNode.setParent(currentScope);
        currentScope = scopeNode;
    }
    
    void popScope() {
        currentScope = currentScope.getParentScope();
    }

    private Node enterLoop(Node loopLabel, boolean doPushScope)
    {
        Node loop = nf.createLoopNode(loopLabel, ts.getLineno());
        // <netbeans>
        int startOffset = getStartOffset();
        // TODO: Compute end position, perhaps in exitLoop?
        loop.setSourceBounds( startOffset, startOffset);
        // </netbeans>
        if (loopSet == null) {
            loopSet = new ObjArray();
            if (loopAndSwitchSet == null) {
                loopAndSwitchSet = new ObjArray();
            }
        }
        loopSet.push(loop);
        loopAndSwitchSet.push(loop);
        if (doPushScope) {
            pushScope(loop);
        }
        return loop;
    }

    private void exitLoop(boolean doPopScope)
    {
        loopSet.pop();
        loopAndSwitchSet.pop();
        if (doPopScope) {
            popScope();
        }
    }

    private Node enterSwitch(Node switchSelector, int lineno)
    {
        Node switchNode = nf.createSwitch(switchSelector, lineno);
        if (loopAndSwitchSet == null) {
            loopAndSwitchSet = new ObjArray();
        }
        loopAndSwitchSet.push(switchNode);
        return switchNode;
    }

    private void exitSwitch()
    {
        // <netbeans>
        //loopAndSwitchSet.pop();
        Node switchNode = (Node) loopAndSwitchSet.pop();
        setSourceOffsets(switchNode, switchNode.getSourceStart());
        // </netbeaans>
    }

    /*
     * Build a parse tree from the given sourceString.
     *
     * @return an Object representing the parsed
     * program.  If the parse fails, null will be returned.  (The
     * parse failure will result in a call to the ErrorReporter from
     * CompilerEnvirons.)
     */
    public ScriptOrFnNode parse(String sourceString,
                                String sourceURI, int lineno)
    {
        this.sourceURI = sourceURI;
        this.ts = new TokenStream(this, null, sourceString, lineno);
        try {
            // <netbeans>
            setJsonMode(false);
            // </netbeans>
            return parse();
        } catch (IOException ex) {
            // Should never happen
            throw new IllegalStateException();
        }
    }

    /*
     * Build a parse tree from the given sourceString.
     *
     * @return an Object representing the parsed
     * program.  If the parse fails, null will be returned.  (The
     * parse failure will result in a call to the ErrorReporter from
     * CompilerEnvirons.)
     */
    public ScriptOrFnNode parse(Reader sourceReader,
                                String sourceURI, int lineno)
        throws IOException
    {
        this.sourceURI = sourceURI;
        this.ts = new TokenStream(this, sourceReader, null, lineno);
        return parse();
    }

    private ScriptOrFnNode parse()
        throws IOException
    {
        this.decompiler = createDecompiler(compilerEnv);
        this.nf = new IRFactory(this);
        currentScriptOrFn = nf.createScript();
        currentScope = currentScriptOrFn;
        int sourceStartOffset = decompiler.getCurrentOffset();
// <netbeans>
        int realSourceStartOffset = ts.getBufferOffset();
// </netbeans>
        this.encodedSource = null;
        decompiler.addToken(Token.SCRIPT);

        this.currentFlaggedToken = Token.EOF;
        this.syntaxErrorCount = 0;

        int baseLineno = ts.getLineno();  // line number where source starts

        /* so we have something to add nodes to until
         * we've collected all the source */
        Node pn = nf.createLeaf(Token.BLOCK);

        try {
            for (;;) {
                int tt = peekToken();

                if (tt <= Token.EOF) {
                    break;
                }

                Node n;
                if (tt == Token.FUNCTION) {
// <netbeans>
                    int startOffset = peekedTokenStart;
// </netbeans>
                    consumeToken();
                    try {
                        n = function(calledByCompileFunction
                                     ? FunctionNode.FUNCTION_EXPRESSION
                                     : FunctionNode.FUNCTION_STATEMENT);
// <netbeans>
                        setSourceOffsets(n, startOffset);
// </netbeans>
                    } catch (ParserException e) {
                        break;
                    }
                } else {
                    n = statement();
                }
                nf.addChildToBack(pn, n);
            }
        } catch (StackOverflowError ex) {
            String msg = ScriptRuntime.getMessage0(
                "msg.too.deep.parser.recursion");
            throw Context.reportRuntimeError(msg, sourceURI,
                                             ts.getLineno(), null, 0);
        }

        if (this.syntaxErrorCount != 0) {
            String msg = String.valueOf(this.syntaxErrorCount);
            msg = ScriptRuntime.getMessage1("msg.got.syntax.errors", msg);
// <netbeans>
            //throw errorReporter.runtimeError(msg, sourceURI, baseLineno,
            //                                 null, 0);
            // Don't show an error for this at all!
            //EvaluatorException exc = errorReporter.runtimeError(msg, sourceURI, baseLineno,
            //                                 null, 0);
            //if (exc != null) {
            //    throw exc;
            //} else {
//                currentScriptOrFn.setSourceName(sourceURI);
//                currentScriptOrFn.setBaseLineno(baseLineno);
//                currentScriptOrFn.setEndLineno(ts.getLineno());
//
//                return currentScriptOrFn;
return null;
            //}
// </netbeans>
        }

        currentScriptOrFn.setSourceName(sourceURI);
        currentScriptOrFn.setBaseLineno(baseLineno);
        currentScriptOrFn.setEndLineno(ts.getLineno());

        int sourceEndOffset = decompiler.getCurrentOffset();
        currentScriptOrFn.setEncodedSourceBounds(sourceStartOffset,
                                                 sourceEndOffset);
// <netbeans>
        int realSourceEndOffset = getEndOffset();
        currentScriptOrFn.setSourceBounds(realSourceStartOffset, realSourceEndOffset);
// </netbeans>


        nf.initScript(currentScriptOrFn, pn);

        if (compilerEnv.isGeneratingSource()) {
            encodedSource = decompiler.getEncodedSource();
        }
        this.decompiler = null; // It helps GC

        return currentScriptOrFn;
    }

    /*
     * The C version of this function takes an argument list,
     * which doesn't seem to be needed for tree generation...
     * it'd only be useful for checking argument hiding, which
     * I'm not doing anyway...
     */
    private Node parseFunctionBody()
        throws IOException
    {
        ++nestingOfFunction;
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>
        Node pn = nf.createBlock(ts.getLineno());
        try {
            bodyLoop: for (;;) {
                Node n;
                int tt = peekToken();
                switch (tt) {
                  case Token.ERROR:
                  case Token.EOF:
                  case Token.RC:
                    break bodyLoop;

                  case Token.FUNCTION:
                    consumeToken();
                    n = function(FunctionNode.FUNCTION_STATEMENT);
                    break;
                  default:
                    n = statement();
                    break;
                }
                nf.addChildToBack(pn, n);
            }
        } catch (ParserException e) {
            // Ignore it
        } finally {
            --nestingOfFunction;
        }

        // <netbeans>
        //setSourceOffsets(pn, startOffset);
        pn.setSourceBounds(startOffset, getEndOffset());
        // </netbeans>
        return pn;
    }

    private Node function(int functionType)
        throws IOException, ParserException
    {
        int syntheticType = functionType;
        int baseLineno = ts.getLineno();  // line number where source starts

        int functionSourceStart = decompiler.markFunctionStart(functionType);
// <netbeans>
        int realSourceStartOffset = getStartOffset();
        int realSourceEndOffset = realSourceStartOffset;
        Node funcNameNode = null;
// </netbeans>
        String name;
        Node memberExprNode = null;
        if (matchToken(Token.NAME)) {
            name = ts.getString();
// <netbeans>
            funcNameNode = Node.newString(Token.FUNCNAME, name);
            setSourceOffsets(funcNameNode, getStartOffset());
// </netbeans>
            decompiler.addName(name);
            if (!matchToken(Token.LP)) {
                if (compilerEnv.isAllowMemberExprAsFunctionName()) {
                    // Extension to ECMA: if 'function <name>' does not follow
                    // by '(', assume <name> starts memberExpr
                    Node memberExprHead = nf.createName(name);
                    name = "";
                    memberExprNode = memberExprTail(false, memberExprHead);
                }
                // <netbeans>
                // Detect and prevent scenarios like issue 133469
                int peekedToken = peekToken();
                if (peekedToken != Token.LP && GENERATED_IDENTIFIER.equals(name)) {
                    if (matchToken(Token.NAME)) {
                        name = ts.getString();
                        funcNameNode = Node.newString(Token.FUNCNAME, name);
                        setSourceOffsets(funcNameNode, getStartOffset());
                    }
                }
                // </netbeans>
                mustMatchToken(Token.LP, "msg.no.paren.parms");
            }
        } else if (matchToken(Token.LP)) {
            // Anonymous function
            name = "";
        } else {
            name = "";
            if (compilerEnv.isAllowMemberExprAsFunctionName()) {
                // Note that memberExpr can not start with '(' like
                // in function (1+2).toString(), because 'function (' already
                // processed as anonymous function
                memberExprNode = memberExpr(false);
            }
            mustMatchToken(Token.LP, "msg.no.paren.parms");
        }

        if (memberExprNode != null) {
            syntheticType = FunctionNode.FUNCTION_EXPRESSION;
        } 
        
        if (syntheticType != FunctionNode.FUNCTION_EXPRESSION && 
            name.length() > 0)
        {
            // Function statements define a symbol in the enclosing scope
            defineSymbol(Token.FUNCTION, false, name
                    // <netbeans>
                    , funcNameNode
                    // </netbeans>
                    );
        }

        boolean nested = insideFunction();

        FunctionNode fnNode = nf.createFunction(name);
        // <netbeans>
        // Add a special node for the function name with proper offsets etc.
        // for IDE usage in refactoring, highlighting etc.
        if (funcNameNode != null) {
            fnNode.addChildToFront(funcNameNode);
        }
        // </netbeans>

        if (nested || nestingOfWith > 0) {
            // 1. Nested functions are not affected by the dynamic scope flag
            // as dynamic scope is already a parent of their scope.
            // 2. Functions defined under the with statement also immune to
            // this setup, in which case dynamic scope is ignored in favor
            // of with object.
            fnNode.itsIgnoreDynamicScope = true;
        }
        int functionIndex = currentScriptOrFn.addFunction(fnNode);

        int functionSourceEnd;

        ScriptOrFnNode savedScriptOrFn = currentScriptOrFn;
        currentScriptOrFn = fnNode;
        Node.Scope savedCurrentScope = currentScope;
        currentScope = fnNode;
        int savedNestingOfWith = nestingOfWith;
        nestingOfWith = 0;
        Map<String,Node> savedLabelSet = labelSet;
        labelSet = null;
        ObjArray savedLoopSet = loopSet;
        loopSet = null;
        ObjArray savedLoopAndSwitchSet = loopAndSwitchSet;
        loopAndSwitchSet = null;
        int savedFunctionEndFlags = endFlags;
        endFlags = 0;

        Node destructuring = null;
        Node body;
        try {
            decompiler.addToken(Token.LP);
            if (!matchToken(Token.RP)) {
                boolean first = true;
                do {
                    if (!first)
                        decompiler.addToken(Token.COMMA);
                    first = false;
                    int tt = peekToken();
                    if (tt == Token.LB || tt == Token.LC) {
                        // Destructuring assignment for parameters: add a 
                        // dummy parameter name, and add a statement to the
                        // body to initialize variables from the destructuring
                        // assignment
                        if (destructuring == null) {
                            destructuring = new Node(Token.COMMA);
                        }
                        String parmName = currentScriptOrFn.getNextTempName();
                        defineSymbol(Token.LP, false, parmName
                                // <netbeans>
                                , null
                                // </netbeans>
                                );
                        destructuring.addChildToBack(
                            nf.createDestructuringAssignment(Token.VAR,
                                primaryExpr(), nf.createName(parmName)));
// XXX <netbeans> TODO - something about positions here?
                    } else {
                        mustMatchToken(Token.NAME, "msg.no.parm");
                        String s = ts.getString();
                        // <netbeans>
                        Node paramNode = Node.newString(Token.PARAMETER, s);
                        setSourceOffsets(paramNode, getStartOffset());
                        fnNode.addChildToBack(paramNode);
                        // </netbeans>
                        defineSymbol(Token.LP, false, s
                            // <netbeans>
                            , paramNode
                            // </netbeans>
                                );
                        decompiler.addName(s);
                        // <netbeans>
                        // Skip extra __UNKNOWN__ tokens
                        while (peekToken() == Token.NAME && GENERATED_IDENTIFIER.equals(ts.getString())) {
                            consumeToken();
                        }
                        // </netbeans>
                    }
                } while (matchToken(Token.COMMA));

                mustMatchToken(Token.RP, "msg.no.paren.after.parms");
            }
            decompiler.addToken(Token.RP);

            mustMatchToken(Token.LC, "msg.no.brace.body");
            decompiler.addEOL(Token.LC);
            body = parseFunctionBody();
            if (destructuring != null) {
                body.addChildToFront(
                    new Node(Token.EXPR_VOID, destructuring, ts.getLineno()));
            }
            mustMatchToken(Token.RC, "msg.no.brace.after.body");

            if (compilerEnv.isStrictMode() && !body.hasConsistentReturnUsage())
            {
              String msg = name.length() > 0 ? "msg.no.return.value"
                                             : "msg.anon.no.return.value";
              addStrictWarning(msg, name
                            // <netbeans> - pass in additional parameters for the error
                            , fnNode
                            // </netbeans>
                      );
            }
            
            if (syntheticType == FunctionNode.FUNCTION_EXPRESSION &&
                name.length() > 0 && currentScope.getSymbol(name) == null) 
            {
                // Function expressions define a name only in the body of the 
                // function, and only if not hidden by a parameter name
                defineSymbol(Token.FUNCTION, false, name
                            // <netbeans>
                            , funcNameNode
                            // </netbeans>
                        );
            }
            
            decompiler.addToken(Token.RC);
            functionSourceEnd = decompiler.markFunctionEnd(functionSourceStart);
// <netbeans>
            realSourceEndOffset = getEndOffset();
// </netbeans>

            if (functionType != FunctionNode.FUNCTION_EXPRESSION) {
                // Add EOL only if function is not part of expression
                // since it gets SEMI + EOL from Statement in that case
                decompiler.addToken(Token.EOL);
            }
        }
        finally {
            endFlags = savedFunctionEndFlags;
            loopAndSwitchSet = savedLoopAndSwitchSet;
            loopSet = savedLoopSet;
            labelSet = savedLabelSet;
            nestingOfWith = savedNestingOfWith;
            currentScriptOrFn = savedScriptOrFn;
            currentScope = savedCurrentScope;
        }

        fnNode.setEncodedSourceBounds(functionSourceStart, functionSourceEnd);
        fnNode.setSourceName(sourceURI);
        fnNode.setBaseLineno(baseLineno);
        fnNode.setEndLineno(ts.getLineno());
// <netbeans>
        fnNode.setSourceBounds(realSourceStartOffset, realSourceEndOffset);
// </netbeans>

        Node pn = nf.initFunction(fnNode, functionIndex, body, syntheticType);
        if (memberExprNode != null) {
            // <netbeans>
            // TODO - how should I update the pn offsets based on this?
            // </netbeans>
            pn = nf.createAssignment(Token.ASSIGN, memberExprNode, pn);
            if (functionType != FunctionNode.FUNCTION_EXPRESSION) {
                // XXX check JScript behavior: should it be createExprStatement?
                pn = nf.createExprStatementNoReturn(pn, baseLineno);
            }
        }

        // <netbeans>
        // TODO - how does memberExprNode deal with this?
        pn.setSourceBounds(fnNode.getSourceStart(), fnNode.getSourceEnd());
        // </netbeans>
        return pn;
    }

    private Node statements(Node scope)
        throws IOException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>
        Node pn = scope != null ? scope : nf.createBlock(ts.getLineno());

        int tt;
        while ((tt = peekToken()) > Token.EOF && tt != Token.RC) {
            nf.addChildToBack(pn, statement());
        }

        // <netbeans>
        //setSourceOffsets(pn, startOffset);
        pn.setSourceBounds(startOffset, peekedTokenEnd);
        // </netbeans>

        return pn;
    }

    private Node condition()
        throws IOException, ParserException
    {
        mustMatchToken(Token.LP, "msg.no.paren.cond");
        decompiler.addToken(Token.LP);
        // <netbeans>
        // NOTE - the END offset is AFTER the left parenthesis! We don't
        // want to include it since we're not creating a node wrapping the
        // parens!
        peekToken();
        int startOffset = peekedTokenStart;
        // </netbeans>
        Node pn = expr(false);
        // <netbeans>
        int endOffset = getEndOffset();
        pn.setSourceBounds(startOffset, endOffset);
        // </netbeans>
        mustMatchToken(Token.RP, "msg.no.paren.after.cond");
        decompiler.addToken(Token.RP);

        // Report strict warning on code like "if (a = 7) ...". Suppress the
        // warning if the condition is parenthesized, like "if ((a = 7)) ...".
        if (pn.getProp(Node.PARENTHESIZED_PROP) == null &&
            (pn.getType() == Token.SETNAME || pn.getType() == Token.SETPROP ||
             pn.getType() == Token.SETELEM))
        {
            addStrictWarning("msg.equal.as.assign", ""
                            // <netbeans> - pass in additional parameters for the error
                            , pn
                            // </netbeans>
                    );
        }
        return pn;
    }

    // match a NAME; return null if no match.
    private Node matchJumpLabelName()
        throws IOException, ParserException
    {
// TODO - handle positions here?
        Node label = null;

        int tt = peekTokenOrEOL();
        if (tt == Token.NAME) {
            consumeToken();
            String name = ts.getString();
            decompiler.addName(name);
            if (labelSet != null) {
                label = labelSet.get(name);
            }
            if (label == null) {
                reportError("msg.undef.label");
            }
        }

        return label;
    }

    private Node statement()
        throws IOException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>
        try {
            Node pn = statementHelper(null);
            if (pn != null) {
                if (compilerEnv.isStrictMode() && !pn.hasSideEffects())
                    addStrictWarning("msg.no.side.effects", ""
                            // <netbeans> - pass in additional parameters for the error
                            , pn
                            // </netbeans>
                            );
                return pn;
            }
        } catch (ParserException e) { }

        // skip to end of statement
        int lineno = ts.getLineno();
        guessingStatementEnd: for (;;) {
            int tt = peekTokenOrEOL();
            consumeToken();
            switch (tt) {
              case Token.ERROR:
              case Token.EOF:
              case Token.EOL:
              case Token.SEMI:
                break guessingStatementEnd;
            }
        }
        // <netbeans>
        //return nf.createExprStatement(nf.createName("error"), lineno);
        return setSourceOffsets(nf.createExprStatement(nf.createName("error"), lineno), startOffset);
        // </netbeans>
    }

    private Node statementHelper(Node statementLabel)
        throws IOException, ParserException
    {
        Node pn = null;
        int tt = peekToken();
        // <netbeans>
        int startOffset = peekedTokenStart;
        // </netbeans>

        switch (tt) {
          case Token.IF: {
            consumeToken();

            decompiler.addToken(Token.IF);
            int lineno = ts.getLineno();
            Node cond = condition();
            decompiler.addEOL(Token.LC);
            Node ifTrue = statement();
            Node ifFalse = null;
            if (matchToken(Token.ELSE)) {
                decompiler.addToken(Token.RC);
                decompiler.addToken(Token.ELSE);
                decompiler.addEOL(Token.LC);
                ifFalse = statement();
            }
            decompiler.addEOL(Token.RC);
            pn = nf.createIf(cond, ifTrue, ifFalse, lineno);
            // <netbeans>
            pn.setSourceBounds(startOffset, getEndOffset());
            // </netbeans>
            return pn;
          }

          case Token.SWITCH: {
            consumeToken();

            decompiler.addToken(Token.SWITCH);
            int lineno = ts.getLineno();
            mustMatchToken(Token.LP, "msg.no.paren.switch");
            decompiler.addToken(Token.LP);
            pn = enterSwitch(expr(false), lineno);
            try {
                mustMatchToken(Token.RP, "msg.no.paren.after.switch");
                decompiler.addToken(Token.RP);
                mustMatchToken(Token.LC, "msg.no.brace.switch");
                decompiler.addEOL(Token.LC);

                boolean hasDefault = false;
                switchLoop: for (;;) {
                    tt = nextToken();
                    Node caseExpression;
                    // <netbeans>
                    // We need to correct the AST offsets of the case blocks
                    int caseStart = peekedTokenStart;
                    // </netbeans>
                    switch (tt) {
                      case Token.RC:
                        break switchLoop;

                      case Token.CASE:
                        decompiler.addToken(Token.CASE);
                        caseExpression = expr(false);
                        mustMatchToken(Token.COLON, "msg.no.colon.case");
                        decompiler.addEOL(Token.COLON);
                        break;

                      case Token.DEFAULT:
                        if (hasDefault) {
                            reportError("msg.double.switch.default");
                        }
                        decompiler.addToken(Token.DEFAULT);
                        hasDefault = true;
                        caseExpression = null;
                        mustMatchToken(Token.COLON, "msg.no.colon.case");
                        decompiler.addEOL(Token.COLON);
                        break;

                      default:
                        reportError("msg.bad.switch");
                        break switchLoop;
                    }

                    Node block = nf.createLeaf(Token.BLOCK);
                    // <netbeans>
                    int blockStart = getStartOffset();
                    pn.setSourceBounds(blockStart, blockStart);
                    // </netbeans>

                    while ((tt = peekToken()) != Token.RC
                           && tt != Token.CASE
                           && tt != Token.DEFAULT
                           && tt != Token.EOF)
                    {
                        nf.addChildToBack(block, statement());
                    }

                    // caseExpression == null => add default label
                    // <netbeans>
                    //nf.addSwitchCase(pn, caseExpression, block);
                    nf.addSwitchCase(pn, caseExpression, block, caseStart);
                    // </netbeans>
                }
                decompiler.addEOL(Token.RC);
                nf.closeSwitch(pn);
            } finally {
                exitSwitch();
            }
            // <netbeans>
            int endOffset = matchedTokenEnd;
            pn.setSourceBounds(startOffset, endOffset);
            // </netbeans>
            return pn;
          }

          case Token.WHILE: {
            consumeToken();
            decompiler.addToken(Token.WHILE);

            Node loop = enterLoop(statementLabel, true);
            try {
                Node cond = condition();
                decompiler.addEOL(Token.LC);
                Node body = statement();
                decompiler.addEOL(Token.RC);
                pn = nf.createWhile(loop, cond, body);
                // <netbeans>
                setSourceOffsets(pn, startOffset);
                // </netbeans>
            } finally {
                exitLoop(true);
            }
            return pn;
          }

          case Token.DO: {
            consumeToken();
            decompiler.addToken(Token.DO);
            decompiler.addEOL(Token.LC);

            Node loop = enterLoop(statementLabel, true);
            try {
                Node body = statement();
                decompiler.addToken(Token.RC);
                mustMatchToken(Token.WHILE, "msg.no.while.do");
                decompiler.addToken(Token.WHILE);
                Node cond = condition();
                pn = nf.createDoWhile(loop, body, cond);
            } finally {
                exitLoop(true);
            }
            // Always auto-insert semicolon to follow SpiderMonkey:
            // It is required by ECMAScript but is ignored by the rest of
            // world, see bug 238945
            matchToken(Token.SEMI);
            decompiler.addEOL(Token.SEMI);
            // <netbeans>
            setSourceOffsets(pn, startOffset);
            // </netbeans>
            return pn;
          }

          case Token.FOR: {
            consumeToken();
            boolean isForEach = false;
            decompiler.addToken(Token.FOR);

            Node loop = enterLoop(statementLabel, true);
            try {
                Node init;  // Node init is also foo in 'foo in object'
                Node cond;  // Node cond is also object in 'foo in object'
                Node incr = null;
                Node body;
                int declType = -1;

                // See if this is a for each () instead of just a for ()
                if (matchToken(Token.NAME)) {
                    decompiler.addName(ts.getString());
                    if (ts.getString().equals("each")) {
                        isForEach = true;
                    } else {
                        reportError("msg.no.paren.for");
                    }
                }

                mustMatchToken(Token.LP, "msg.no.paren.for");
                decompiler.addToken(Token.LP);
// <netbeans>
                int realSourceStartOffset = getStartOffset();
// </netbeans>
                tt = peekToken();
                if (tt == Token.SEMI) {
                    init = nf.createLeaf(Token.EMPTY);
                    // <netbeans>
                    int pos = getStartOffset();
                    init.setSourceBounds(pos, pos);
                    // </netbeans>
                } else {
                    if (tt == Token.VAR || tt == Token.LET) {
                        // set init to a var list or initial
                        consumeToken();    // consume the token
                        decompiler.addToken(tt);
                        init = variables(true, tt);
// <netbeans>
                        // TODO - This isn't right, there could be MULTIPLE variables... they should
                        // all be marked somehow
                        int realSourceEndOffset = getEndOffset();
                        init.setSourceBounds(realSourceStartOffset, realSourceEndOffset);
// </netbeans>
                        declType = tt;
                    }
                    else {
                        init = expr(true);
                    }
                }

                if (matchToken(Token.IN)) {
                    decompiler.addToken(Token.IN);
                    // 'cond' is the object over which we're iterating
                    cond = expr(false);
                } else {  // ordinary for loop
                    mustMatchToken(Token.SEMI, "msg.no.semi.for");
                    decompiler.addToken(Token.SEMI);
                    if (peekToken() == Token.SEMI) {
                        // no loop condition
                        cond = nf.createLeaf(Token.EMPTY);
                        // <netbeans>
                        int pos = getStartOffset();
                        cond.setSourceBounds(pos, pos);
                        // </netbeans>
                    } else {
                        cond = expr(false);
                    }

                    mustMatchToken(Token.SEMI, "msg.no.semi.for.cond");
                    decompiler.addToken(Token.SEMI);
                    if (peekToken() == Token.RP) {
                        incr = nf.createLeaf(Token.EMPTY);
                        // <netbeans>
                        int pos = getStartOffset();
                        incr.setSourceBounds(pos, pos);
                        // </netbeans>
                    } else {
                        incr = expr(false);
                    }
                }

                mustMatchToken(Token.RP, "msg.no.paren.for.ctrl");
                decompiler.addToken(Token.RP);
                decompiler.addEOL(Token.LC);
                body = statement();
                decompiler.addEOL(Token.RC);

                if (incr == null) {
                    // cond could be null if 'in obj' got eaten
                    // by the init node.
                    pn = nf.createForIn(declType, loop, init, cond, body,
                                        isForEach);
                } else {
                    pn = nf.createFor(loop, init, cond, incr, body);
                }
            } finally {
                exitLoop(true);
            }
            // <netbeans>
            setSourceOffsets(pn, startOffset);
            // </netbeans>
            return pn;
          }

          case Token.TRY: {
            consumeToken();
            // <netbeans>
            startOffset = getStartOffset();
            // </netbeans>
            int lineno = ts.getLineno();

            Node tryblock;
            Node catchblocks = null;
            Node finallyblock = null;

            decompiler.addToken(Token.TRY);
            if (peekToken() != Token.LC) {
                reportError("msg.no.brace.try");
            }
            decompiler.addEOL(Token.LC);
            tryblock = statement();
            decompiler.addEOL(Token.RC);

            catchblocks = nf.createLeaf(Token.BLOCK);

            boolean sawDefaultCatch = false;
            int peek = peekToken();
            if (peek == Token.CATCH) {
                while (matchToken(Token.CATCH)) {
                    if (sawDefaultCatch) {
                        reportError("msg.catch.unreachable");
                    }
                    // <netbeans>
                    int catchStart = getStartOffset();
                    // </netbeans>
                    decompiler.addToken(Token.CATCH);
                    mustMatchToken(Token.LP, "msg.no.paren.catch");
                    decompiler.addToken(Token.LP);

                    mustMatchToken(Token.NAME, "msg.bad.catchcond");
                    // <netbeans>
                    int varStart = getStartOffset();
                    int varEnd = matchedTokenEnd;
                    // </netbeans>
                    String varName = ts.getString();
                    decompiler.addName(varName);

                    Node catchCond = null;
                    if (matchToken(Token.IF)) {
                        decompiler.addToken(Token.IF);
                        catchCond = expr(false);
                    } else {
                        sawDefaultCatch = true;
                    }

                    mustMatchToken(Token.RP, "msg.bad.catchcond");
                    decompiler.addToken(Token.RP);
                    mustMatchToken(Token.LC, "msg.no.brace.catchblock");
                    decompiler.addEOL(Token.LC);

                    // <netbeans>
                    catchblocks.setSourceBounds(startOffset, getEndOffset());
//                    nf.addChildToBack(catchblocks,
//                        nf.createCatch(varName, catchCond,
//                                       statements(),
//                                       ts.getLineno()));
                    if (catchCond == null) {
                        // Avoid having an uninitialized EMPTY block created
                        // by the node factory without positions
                        catchCond = new Node(Token.EMPTY);
                        catchCond.setSourceBounds(varEnd, varEnd);
                    }
                    Node catchNode = nf.createCatch(varName, catchCond,
                                       statements(null),
                                       ts.getLineno());
                    setSourceOffsets(catchNode, catchStart);
                    // <netbeans>
                    peekToken();
                    catchNode.setSourceBounds(catchStart, peekedTokenEnd);
                    catchblocks.setSourceBounds(startOffset, peekedTokenEnd);
                    // </netbeans>
                    nf.addChildToBack(catchblocks, catchNode);
                    // </netbeans>
                    Node varNode = catchblocks.getLastChild().getFirstChild();
                    varNode.setSourceBounds(varStart, varEnd);
                    // </netbeans>

                    mustMatchToken(Token.RC, "msg.no.brace.after.body");
                    decompiler.addEOL(Token.RC);
                }
            } else if (peek != Token.FINALLY) {
                mustMatchToken(Token.FINALLY, "msg.try.no.catchfinally");
            }

            if (matchToken(Token.FINALLY)) {
                // <netbeans>
                int finallyBegin = getStartOffset();
                // </netbeans>
                decompiler.addToken(Token.FINALLY);
                decompiler.addEOL(Token.LC);
                finallyblock = statement();
                // <netbeans>
                // Set statement offset to include the finally block
                Node fBlock = new Node(Token.FINALLY, finallyblock);
                fBlock.setSourceBounds(finallyBegin, getEndOffset());
                finallyblock = fBlock;
                // </netbeans>
                decompiler.addEOL(Token.RC);
            }

            pn = nf.createTryCatchFinally(tryblock, catchblocks,
                                          finallyblock, lineno);

            // <netbeans>
            //setSourceOffsets(pn, startOffset);
            pn.setSourceBounds(startOffset, getEndOffset());
            // </netbeans>
            return pn;
          }

          case Token.THROW: {
            consumeToken();
            if (peekTokenOrEOL() == Token.EOL) {
                // ECMAScript does not allow new lines before throw expression,
                // see bug 256617
                reportError("msg.bad.throw.eol");
            }

            int lineno = ts.getLineno();
            decompiler.addToken(Token.THROW);
            pn = nf.createThrow(expr(false), lineno);
            // <netbeans>
            pn.setSourceBounds(startOffset, getEndOffset());
            // </netbeans>
            break;
          }

          case Token.BREAK: {
            consumeToken();
            int lineno = ts.getLineno();

            decompiler.addToken(Token.BREAK);

            // matchJumpLabelName only matches if there is one
            Node breakStatement = matchJumpLabelName();
            if (breakStatement == null) {
                if (loopAndSwitchSet == null || loopAndSwitchSet.size() == 0) {
                    reportError("msg.bad.break");
                    return null;
                }
                breakStatement = (Node)loopAndSwitchSet.peek();
            }
            pn = nf.createBreak(breakStatement, lineno);
            // <netbeans>
            setSourceOffsets(pn, startOffset);
            // </netbeans>
            break;
          }

          case Token.CONTINUE: {
            consumeToken();
            int lineno = ts.getLineno();

            decompiler.addToken(Token.CONTINUE);

            Node loop;
            // matchJumpLabelName only matches if there is one
            Node label = matchJumpLabelName();
            if (label == null) {
                if (loopSet == null || loopSet.size() == 0) {
                    reportError("msg.continue.outside");
                    return null;
                }
                loop = (Node)loopSet.peek();
            } else {
                loop = nf.getLabelLoop(label);
                if (loop == null) {
                    reportError("msg.continue.nonloop");
                    return null;
                }
            }
            pn = nf.createContinue(loop, lineno);
            // <netbeans>
            setSourceOffsets(pn, startOffset);
            // </netbeans>
            break;
          }

          case Token.WITH: {
            consumeToken();

            decompiler.addToken(Token.WITH);
            int lineno = ts.getLineno();
            mustMatchToken(Token.LP, "msg.no.paren.with");
            decompiler.addToken(Token.LP);
            Node obj = expr(false);
            mustMatchToken(Token.RP, "msg.no.paren.after.with");
            decompiler.addToken(Token.RP);
            decompiler.addEOL(Token.LC);

            ++nestingOfWith;
            Node body;
            try {
                body = statement();
            } finally {
                --nestingOfWith;
            }

            decompiler.addEOL(Token.RC);

            pn = nf.createWith(obj, body, lineno);
            // <netbeans>
            setSourceOffsets(pn, startOffset);
            // </netbeans>
            return pn;
          }

          case Token.CONST:
          case Token.VAR: {
            consumeToken();
            decompiler.addToken(tt);
            pn = variables(false, tt);
           // <netbeans>
            setSourceOffsets(pn, startOffset);
            // </netbeans>
            break;
          }
          
          case Token.LET: {
            consumeToken();
            decompiler.addToken(Token.LET);
            if (peekToken() == Token.LP) {
                return let(true);
            } else {
                pn = variables(false, tt);
                if (peekToken() == Token.SEMI)
                    break;
                return pn;
            }
          }

          case Token.RETURN: 
          case Token.YIELD: {
            pn = returnOrYield(tt, false);
            break;
          }

          case Token.DEBUGGER:
            consumeToken();
            decompiler.addToken(Token.DEBUGGER);
            pn = nf.createDebugger(ts.getLineno());
            break;

          case Token.LC:
            consumeToken();
            if (statementLabel != null) {
                decompiler.addToken(Token.LC);
            }
            Node scope = nf.createScopeNode(Token.BLOCK, ts.getLineno());
            pushScope(scope);
            try {
                statements(scope);
// XXX <netbeans> -- shouldn't I store the statements() call into pn= and set like this:
//            pn.setSourceBounds(startOffset, getEndOffset());

                mustMatchToken(Token.RC, "msg.no.brace.block");
                if (statementLabel != null) {
                    decompiler.addEOL(Token.RC);
                }
                return scope;
            } finally {
                popScope();
            }

          case Token.ERROR:
            // Fall thru, to have a node for error recovery to work on
          case Token.SEMI:
            consumeToken();
            pn = nf.createLeaf(Token.EMPTY);
            // <netbeans>
            int pos = getStartOffset();
            pn.setSourceBounds(pos, pos);
            // </netbeans>
            return pn;

          case Token.FUNCTION: {
            consumeToken();
            pn = function(FunctionNode.FUNCTION_EXPRESSION_STATEMENT);
            // <netbeans>
            setSourceOffsets(pn, startOffset);
            // </netbeans>
            return pn;
          }

          case Token.DEFAULT :
            consumeToken();
            mustHaveXML();

            decompiler.addToken(Token.DEFAULT);
            int nsLine = ts.getLineno();

            if (!(matchToken(Token.NAME)
                  && ts.getString().equals("xml")))
            {
                reportError("msg.bad.namespace");
            }
            decompiler.addName(" xml");

            if (!(matchToken(Token.NAME)
                  && ts.getString().equals("namespace")))
            {
                reportError("msg.bad.namespace");
            }
            decompiler.addName(" namespace");

            if (!matchToken(Token.ASSIGN)) {
                reportError("msg.bad.namespace");
            }
            decompiler.addToken(Token.ASSIGN);

            Node expr = expr(false);
            pn = nf.createDefaultNamespace(expr, nsLine);
            // <netbeans>
            setSourceOffsets(pn, startOffset);
            // </netbeans>
            break;

          case Token.NAME: {
            int lineno = ts.getLineno();
            String name = ts.getString();
            setCheckForLabel();
            pn = expr(false);
            if (pn.getType() != Token.LABEL) {
                pn = nf.createExprStatement(pn, lineno);
            } else {
                // Parsed the label: push back token should be
                // colon that primaryExpr left untouched.
                if (peekToken() != Token.COLON) Kit.codeBug();
                consumeToken();
                // depend on decompiling lookahead to guess that that
                // last name was a label.
                decompiler.addName(name);
                decompiler.addEOL(Token.COLON);

                if (labelSet == null) {
                    labelSet = new HashMap<String,Node>();
                } else if (labelSet.containsKey(name)) {
                    reportError("msg.dup.label");
                }

                boolean firstLabel;
                if (statementLabel == null) {
                    firstLabel = true;
                    statementLabel = pn;
                } else {
                    // Discard multiple label nodes and use only
                    // the first: it allows to simplify IRFactory
                    firstLabel = false;
                }
                labelSet.put(name, statementLabel);
                try {
                    pn = statementHelper(statementLabel);
                } finally {
                    labelSet.remove(name);
                }
                if (firstLabel) {
                    pn = nf.createLabeledStatement(statementLabel, pn);
                }
                // <netbeans>
                setSourceOffsets(pn, startOffset);
                // </netbeans>
                return pn;
            }
            // <netbeans>
            setSourceOffsets(pn, startOffset);
            // </netbeans>
            break;
          }

          default: {
            int lineno = ts.getLineno();
            pn = expr(false);
            pn = nf.createExprStatement(pn, lineno);
            // <netbeans>
            setSourceOffsets(pn, startOffset);
            // </netbeans>
            break;
          }
        }

        int ttFlagged = peekFlaggedToken();
        switch (ttFlagged & CLEAR_TI_MASK) {
          case Token.SEMI:
            // Consume ';' as a part of expression
            consumeToken();
            break;
          case Token.ERROR:
          case Token.EOF:
          case Token.RC:
            // Autoinsert ;
            break;
          default:
            if ((ttFlagged & TI_AFTER_EOL) == 0) {
                // Report error if no EOL or autoinsert ; otherwise
                reportError("msg.no.semi.stmt");
            }
            break;
        }
        decompiler.addEOL(Token.SEMI);

        // <netbeans>
        setSourceOffsets(pn, startOffset);
        // </netbeans>

        return pn;
    }

    /**
     * Returns whether or not the bits in the mask have changed to all set.
     * @param before bits before change
     * @param after bits after change
     * @param mask mask for bits
     * @return true if all the bits in the mask are set in "after" but not 
     *              "before"
     */
    private static final boolean nowAllSet(int before, int after, int mask)
    {
        return ((before & mask) != mask) && ((after & mask) == mask);
    }
    
    private Node returnOrYield(int tt, boolean exprContext)
        throws IOException, ParserException
    {
        if (!insideFunction()) {
            reportError(tt == Token.RETURN ? "msg.bad.return"
                                           : "msg.bad.yield");
        }
        consumeToken();

        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        decompiler.addToken(tt);
        int lineno = ts.getLineno();

        Node e;
        /* This is ugly, but we don't want to require a semicolon. */
        switch (peekTokenOrEOL()) {
          case Token.SEMI:
          case Token.RC:
          case Token.EOF:
          case Token.EOL:
          case Token.ERROR:
          case Token.RB:
          case Token.RP:
          case Token.YIELD:
            e = null;
            break;
          default:
            e = expr(false);
            break;
        }

        int before = endFlags;
        Node ret;

        if (tt == Token.RETURN) {
            if (e == null ) {
                endFlags |= Node.END_RETURNS;
            } else {
                endFlags |= Node.END_RETURNS_VALUE;
            }
            ret = nf.createReturn(e, lineno);
            
            // see if we need a strict mode warning
            if (nowAllSet(before, endFlags, 
                          Node.END_RETURNS|Node.END_RETURNS_VALUE))
            {
                addStrictWarning("msg.return.inconsistent", ""
                    // <netbeans>
                        , ret
                    // </netbeans>
                        );
            }
        } else {
            endFlags |= Node.END_YIELDS;
            ret = nf.createYield(e, lineno);
            // <netbeans>
            int endOffset = matchedTokenEnd;
            ret.setSourceBounds(startOffset, endOffset);
            // </netbeans>
            if (!exprContext)
                ret = new Node(Token.EXPR_VOID, ret, lineno);
        }

        // see if we are mixing yields and value returns.
        if (nowAllSet(before, endFlags, 
                      Node.END_YIELDS|Node.END_RETURNS_VALUE))
        {
            String name = ((FunctionNode)currentScriptOrFn).getFunctionName();
            if (name.length() == 0)
                addError("msg.anon.generator.returns", ""
                    // <netbeans>
                        , ret
                    // </netbeans>
                        );
            else
                addError("msg.generator.returns", name
                    // <netbeans>
                        , ret
                    // </netbeans>
                        );
        }

        // <netbeans>
        //setSourceOffsets(ret, startOffset);
        int endOffset = matchedTokenEnd;
        ret.setSourceBounds(startOffset, endOffset);
        // </netbeans>

        return ret;
    }

    /**
     * Parse a 'var' or 'const' statement, or a 'var' init list in a for
     * statement.
     * @param inFor true if we are currently in the midst of the init
     * clause of a for.
     * @param declType A token value: either VAR, CONST, or LET depending on
     * context.
     * @return The parsed statement
     * @throws IOException
     * @throws ParserException
     */
    private Node variables(boolean inFor, int declType)
        throws IOException, ParserException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>
        Node result = nf.createVariables(declType, ts.getLineno());
        boolean first = true;
        for (;;) {
            Node destructuring = null;
            String s = null;
            int tt = peekToken();
            // <netbeans>
            int nameStart = peekedTokenStart;
            int nameEnd = peekedTokenEnd;
            Node name = null;

            // </netbeans>
            if (tt == Token.LB || tt == Token.LC) {
                // Destructuring assignment, e.g., var [a,b] = ...
                destructuring = primaryExpr();
            } else {
                // Simple variable name
                mustMatchToken(Token.NAME, "msg.bad.var");
                s = ts.getString();
    
                if (!first)
                    decompiler.addToken(Token.COMMA);
                first = false;
    
                decompiler.addName(s);

                // <netbeans>
                //defineSymbol(declType, inFor, s);
                name = nf.createName(s);
                defineSymbol(declType, inFor, s, name);
                // </netbeans>
            }
    
            Node init = null;
            if (matchToken(Token.ASSIGN)) {
                decompiler.addToken(Token.ASSIGN);
                init = assignExpr(inFor);
            }
    
            if (destructuring != null) {
                if (init == null) {
                    if (!inFor)
                        reportError("msg.destruct.assign.no.init");
                    nf.addChildToBack(result, destructuring);
                } else {
                    nf.addChildToBack(result,
                        nf.createDestructuringAssignment(declType,
                            destructuring, init));
                }
            } else {
                // <netbeans>
                // Already added above in the non-destructuring branch
                // such that I could pass the node to the defineSymbol call
                //Node name = nf.createName(s);
                name.setSourceBounds(nameStart, nameEnd);
                // </netbeans>
                if (init != null)
                    nf.addChildToBack(name, init);
                nf.addChildToBack(result, name);
            }
    
            if (!matchToken(Token.COMMA))
                break;
        }

        // <netbeans>
        result.setSourceBounds(startOffset, getEndOffset());
        // </netbeans>

        return result;
    }

    
    private Node let(boolean isStatement)
        throws IOException, ParserException
    {
        mustMatchToken(Token.LP, "msg.no.paren.after.let");
        decompiler.addToken(Token.LP);
        Node result = nf.createScopeNode(Token.LET, ts.getLineno());
        pushScope(result);
        try {
              Node vars = variables(false, Token.LET);
              nf.addChildToBack(result, vars);
              mustMatchToken(Token.RP, "msg.no.paren.let");
              decompiler.addToken(Token.RP);
              if (isStatement && peekToken() == Token.LC) {
                  // let statement
                  consumeToken();
                  decompiler.addEOL(Token.LC);
                  nf.addChildToBack(result, statements(null));
                  mustMatchToken(Token.RC, "msg.no.curly.let");
                  decompiler.addToken(Token.RC);
              } else {
                  // let expression
                  result.setType(Token.LETEXPR);
                  nf.addChildToBack(result, expr(false));
                  if (isStatement) {
                      // let expression in statement context
                      result = nf.createExprStatement(result, ts.getLineno());
                  }
              }
        } finally {
            popScope();
        }
        return result;
    }
    
    void defineSymbol(int declType, boolean ignoreNotInBlock, String name
                    // <netbeans>
                    , Node associatedNode
                    // </netbeans>
            ) {
        Node.Scope definingScope = currentScope.getDefiningScope(name);
        Node.Scope.Symbol symbol = definingScope != null 
                                  ? definingScope.getSymbol(name)
                                  : null;
        boolean error = false;
        if (symbol != null && (symbol.declType == Token.CONST ||
            declType == Token.CONST))
        {
            error = true;
        } else {
            switch (declType) {
              case Token.LET:
                if (symbol != null && definingScope == currentScope) {
                    error = symbol.declType == Token.LET;
                }
                int currentScopeType = currentScope.getType();
                if (!ignoreNotInBlock && 
                    ((currentScopeType == Token.LOOP) ||
                     (currentScopeType == Token.IF)))
                {
                    addError("msg.let.decl.not.in.block");
                }
                currentScope.putSymbol(name, 
                    new Node.Scope.Symbol(declType, name));
                break;
                
              case Token.VAR:
              case Token.CONST:
              case Token.FUNCTION:
                if (symbol != null) {
                    if (symbol.declType == Token.VAR)
                        addStrictWarning("msg.var.redecl", name
                    // <netbeans>
                        , associatedNode
                    // </netbeans>
                                );
                    else if (symbol.declType == Token.LP) {
                        addStrictWarning("msg.var.hides.arg", name
                    // <netbeans>
                        , associatedNode
                    // </netbeans>
                                );
                    }
                } else {
                    currentScriptOrFn.putSymbol(name, 
                        new Node.Scope.Symbol(declType, name));
                }
                break;
                
              case Token.LP:
                if (symbol != null) {
                    // must be duplicate parameter. Second parameter hides the 
                    // first, so go ahead and add the second pararameter
                    addWarning("msg.dup.parms", name
                    // <netbeans>
                        , associatedNode
                    // </netbeans>
                            );
                }
                currentScriptOrFn.putSymbol(name, 
                    new Node.Scope.Symbol(declType, name));
                break;
                
              default:
                throw Kit.codeBug();
            }
        }
        if (error) {
            addError(symbol.declType == Token.CONST ? "msg.const.redecl" :
                     symbol.declType == Token.LET ? "msg.let.redecl" :
                     symbol.declType == Token.VAR ? "msg.var.redecl" :
                     symbol.declType == Token.FUNCTION ? "msg.fn.redecl" :
                     "msg.parm.redecl", name
                    // <netbeans>
                        , associatedNode
                    // </netbeans>
                     );
        }
    }

    private Node expr(boolean inForInit)
        throws IOException, ParserException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        Node pn = assignExpr(inForInit);
        while (matchToken(Token.COMMA)) {
            decompiler.addToken(Token.COMMA);
            if (compilerEnv.isStrictMode() && !pn.hasSideEffects())
                addStrictWarning("msg.no.side.effects", ""
                    // <netbeans>
                        , pn
                    // </netbeans>
                        );
            if (peekToken() == Token.YIELD) {
              reportError("msg.yield.parenthesized");
            }
            pn = nf.createBinary(Token.COMMA, pn, assignExpr(inForInit));
        }

        // <netbeans>
        setSourceOffsets(pn, startOffset);
        // </netbeans>

        return pn;
    }

    private Node assignExpr(boolean inForInit)
        throws IOException, ParserException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        int tt = peekToken();
        if (tt == Token.YIELD) {
            consumeToken();
            return returnOrYield(tt, true);
        }

        Node pn = condExpr(inForInit);

        tt = peekToken();
        // <netbeans>
        if (tt == Token.NAME && GENERATED_IDENTIFIER.equals(ts.getString())) {
            // One or more extra __UNKNOWN__ tokens in there
            while (true) {
                if ((currentFlaggedToken & TI_AFTER_EOL) != 0) {
                    break;
                }
                if (peekToken() == Token.NAME && GENERATED_IDENTIFIER.equals(ts.getString())) {
                    consumeToken();
                } else {
                    break;
                }
            }
            tt = peekToken();
        }
        // </netbeans>
        if (Token.FIRST_ASSIGN <= tt && tt <= Token.LAST_ASSIGN) {
            consumeToken();
            decompiler.addToken(tt);
            pn = nf.createAssignment(tt, pn, assignExpr(inForInit));
        }

        // <netbeans>
        setSourceOffsets(pn, startOffset);
        // </netbeans>

        return pn;
    }

    private Node condExpr(boolean inForInit)
        throws IOException, ParserException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        Node pn = orExpr(inForInit);

        if (matchToken(Token.HOOK)) {
            decompiler.addToken(Token.HOOK);
            Node ifTrue = assignExpr(false);
            mustMatchToken(Token.COLON, "msg.no.colon.cond");
            decompiler.addToken(Token.COLON);
            Node ifFalse = assignExpr(inForInit);
            return nf.createCondExpr(pn, ifTrue, ifFalse);
        }

        // <netbeans>
        setSourceOffsets(pn, startOffset);
        // </netbeans>

        return pn;
    }

    private Node orExpr(boolean inForInit)
        throws IOException, ParserException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        Node pn = andExpr(inForInit);
        if (matchToken(Token.OR)) {
            decompiler.addToken(Token.OR);
            pn = nf.createBinary(Token.OR, pn, orExpr(inForInit));
        }

        // <netbeans>
        setSourceOffsets(pn, startOffset);
        // </netbeans>

        return pn;
    }

    private Node andExpr(boolean inForInit)
        throws IOException, ParserException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        Node pn = bitOrExpr(inForInit);
        if (matchToken(Token.AND)) {
            decompiler.addToken(Token.AND);
            pn = nf.createBinary(Token.AND, pn, andExpr(inForInit));
        }

        // <netbeans>
        setSourceOffsets(pn, startOffset);
        // </netbeans>

        return pn;
    }

    private Node bitOrExpr(boolean inForInit)
        throws IOException, ParserException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        Node pn = bitXorExpr(inForInit);
        while (matchToken(Token.BITOR)) {
            decompiler.addToken(Token.BITOR);
            pn = nf.createBinary(Token.BITOR, pn, bitXorExpr(inForInit));
        }

        // <netbeans>
        setSourceOffsets(pn, startOffset);
        // </netbeans>

        return pn;
    }

    private Node bitXorExpr(boolean inForInit)
        throws IOException, ParserException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        Node pn = bitAndExpr(inForInit);
        while (matchToken(Token.BITXOR)) {
            decompiler.addToken(Token.BITXOR);
            pn = nf.createBinary(Token.BITXOR, pn, bitAndExpr(inForInit));
        }

        // <netbeans>
        setSourceOffsets(pn, startOffset);
        // </netbeans>

        return pn;
    }

    private Node bitAndExpr(boolean inForInit)
        throws IOException, ParserException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        Node pn = eqExpr(inForInit);
        while (matchToken(Token.BITAND)) {
            decompiler.addToken(Token.BITAND);
            pn = nf.createBinary(Token.BITAND, pn, eqExpr(inForInit));
        }

        // <netbeans>
        setSourceOffsets(pn, startOffset);
        // </netbeans>

        return pn;
    }

    private Node eqExpr(boolean inForInit)
        throws IOException, ParserException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        Node pn = relExpr(inForInit);
        for (;;) {
            int tt = peekToken();
            switch (tt) {
              case Token.EQ:
              case Token.NE:
              case Token.SHEQ:
              case Token.SHNE:
                consumeToken();
                int decompilerToken = tt;
                int parseToken = tt;
                if (compilerEnv.getLanguageVersion() == Context.VERSION_1_2) {
                    // JavaScript 1.2 uses shallow equality for == and != .
                    // In addition, convert === and !== for decompiler into
                    // == and != since the decompiler is supposed to show
                    // canonical source and in 1.2 ===, !== are allowed
                    // only as an alias to ==, !=.
                    switch (tt) {
                      case Token.EQ:
                        parseToken = Token.SHEQ;
                        break;
                      case Token.NE:
                        parseToken = Token.SHNE;
                        break;
                      case Token.SHEQ:
                        decompilerToken = Token.EQ;
                        break;
                      case Token.SHNE:
                        decompilerToken = Token.NE;
                        break;
                    }
                }
                decompiler.addToken(decompilerToken);
                pn = nf.createBinary(parseToken, pn, relExpr(inForInit));
                continue;
            }
            break;
        }

        // <netbeans>
        setSourceOffsets(pn, startOffset);
        // </netbeans>

        return pn;
    }

    private Node relExpr(boolean inForInit)
        throws IOException, ParserException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        Node pn = shiftExpr();
        for (;;) {
            int tt = peekToken();
            switch (tt) {
              case Token.IN:
                if (inForInit)
                    break;
                // fall through
              case Token.INSTANCEOF:
              case Token.LE:
              case Token.LT:
              case Token.GE:
              case Token.GT:
                consumeToken();
                decompiler.addToken(tt);
                pn = nf.createBinary(tt, pn, shiftExpr());
                continue;
            }
            break;
        }

        // <netbeans>
        setSourceOffsets(pn, startOffset);
        // </netbeans>

        return pn;
    }

    private Node shiftExpr()
        throws IOException, ParserException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        Node pn = addExpr();
        for (;;) {
            int tt = peekToken();
            switch (tt) {
              case Token.LSH:
              case Token.URSH:
              case Token.RSH:
                consumeToken();
                decompiler.addToken(tt);
                pn = nf.createBinary(tt, pn, addExpr());
                continue;
            }
            break;
        }

        // <netbeans>
        setSourceOffsets(pn, startOffset);
        // </netbeans>

        return pn;
    }

    private Node addExpr()
        throws IOException, ParserException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        Node pn = mulExpr();
        for (;;) {
            int tt = peekToken();
            if (tt == Token.ADD || tt == Token.SUB) {
                consumeToken();
                decompiler.addToken(tt);
                // flushNewLines
                pn = nf.createBinary(tt, pn, mulExpr());
                continue;
            }
            break;
        }

        // <netbeans>
        setSourceOffsets(pn, startOffset);
        // </netbeans>

        return pn;
    }

    private Node mulExpr()
        throws IOException, ParserException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        Node pn = unaryExpr();
        for (;;) {
            int tt = peekToken();
            switch (tt) {
              case Token.MUL:
              case Token.DIV:
              case Token.MOD:
                consumeToken();
                decompiler.addToken(tt);
                pn = nf.createBinary(tt, pn, unaryExpr());
                continue;
            }
            break;
        }

        // <netbeans>
        setSourceOffsets(pn, startOffset);
        // </netbeans>

        return pn;
    }

    private Node unaryExpr()
        throws IOException, ParserException
    {
        int tt;

        tt = peekToken();

        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        switch(tt) {
        case Token.VOID:
        case Token.NOT:
        case Token.BITNOT:
        case Token.TYPEOF:
            consumeToken();
            decompiler.addToken(tt);
            // <netbeans>
            //return nf.createUnary(tt, unaryExpr());
            return setSourceOffsets(nf.createUnary(tt, unaryExpr()), startOffset);
            // </netbeans>

        case Token.ADD:
            consumeToken();
            // Convert to special POS token in decompiler and parse tree
            decompiler.addToken(Token.POS);
            // <netbeans>
            //return nf.createUnary(Token.POS, unaryExpr());
            return setSourceOffsets(nf.createUnary(Token.POS, unaryExpr()), startOffset);
            // </netbeans>

        case Token.SUB:
            consumeToken();
            // Convert to special NEG token in decompiler and parse tree
            decompiler.addToken(Token.NEG);
            // <netbeans>
            //return nf.createUnary(Token.NEG, unaryExpr());
            return setSourceOffsets(nf.createUnary(Token.NEG, unaryExpr()), startOffset);
            // </netbeans>

        case Token.INC:
        case Token.DEC:
            consumeToken();
            decompiler.addToken(tt);
            // <netbeans>
            //return nf.createIncDec(tt, false, memberExpr(true));
            return setSourceOffsets(nf.createIncDec(tt, false, memberExpr(true)), startOffset);
            // </netbeans>

        case Token.DELPROP:
            consumeToken();
            decompiler.addToken(Token.DELPROP);
            // <netbeans>
            //return nf.createUnary(Token.DELPROP, unaryExpr());
            return setSourceOffsets(nf.createUnary(Token.DELPROP, unaryExpr()), startOffset);
            // </netbeans>

        case Token.ERROR:
            consumeToken();
            break;

        // XML stream encountered in expression.
        case Token.LT:
            if (compilerEnv.isXmlAvailable()) {
                consumeToken();
                Node pn = xmlInitializer();
                // <netbeans>
                //return memberExprTail(true, pn);
                return setSourceOffsets(memberExprTail(true, pn), startOffset);
                // </netbeans>
            }
            // Fall thru to the default handling of RELOP

        default:
            Node pn = memberExpr(true);

            // Don't look across a newline boundary for a postfix incop.
            tt = peekTokenOrEOL();
            if (tt == Token.INC || tt == Token.DEC) {
                consumeToken();
                decompiler.addToken(tt);
                // <netbeans>
                //return nf.createIncDec(tt, true, pn);
                return setSourceOffsets(nf.createIncDec(tt, true, pn), startOffset);
                // </netbeans>
            }

            // <netbeans>
            setSourceOffsets(pn, startOffset);
            // </netbeans>

            return pn;
        }
        // <netbeans>
        return nf.createName("err"); // Only reached on error.  Try to continue.
        // </netbeans>

    }

    // <netbeans>
    // TODO - rename to something like setSourceOffsets
    Node setSourceOffsets(Node n, int startOffset) {
        if (n.getSourceEnd() != 0) {
            return n;
        }
        int endOffset = matchedTokenEnd;
        n.setSourceBounds(startOffset, endOffset);
        // Return n such that expressions can be chained, e.g
        // return factory.createNumber(42)
        //   can be written as
        // return factory.createNumber(42).setSourceOffsets(n, startOffset)
        return n;
    }

    static Node setSourceOffsets(Node n, int startOffset, int endOffset) {
//        if (n.getSourceEnd() != 0) {
//            return n;
//        }
        n.setSourceBounds(startOffset, endOffset);
        // Return n such that expressions can be chained, e.g
        // return factory.createNumber(42)
        //   can be written as
        // return factory.createNumber(42).setSourceOffsets(n, startOffset)
        return n;
    }

    int getStartOffset() {
        return matchedTokenStart;
    }

    int getEndOffset() {
        return matchedTokenEnd;
    }
    // </netbeans>

    private Node xmlInitializer() throws IOException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        int tt = ts.getFirstXMLToken();
        if (tt != Token.XML && tt != Token.XMLEND) {
            reportError("msg.syntax");
            return null;
        }

        /* Make a NEW node to append to. */
// <netbeans>
// Change the E4X parse tree representation. We don't need to execute
// it; we mostly want a simple AST representation with accurate AST offsets.
//        Node pnXML = nf.createLeaf(Token.NEW);
Node pnXML = nf.createLeaf(Token.E4X);
Node pn = null;

        String xml = ts.getString();
//        boolean fAnonymous = xml.trim().startsWith("<>");
//        Node pn = nf.createName(fAnonymous ? "XMLList" : "XML");
//        nf.addChildToBack(pnXML, pn);
        pn = null;
        Node expr;
        for (;;tt = ts.getNextXMLToken()) {
            switch (tt) {
            case Token.XML: {
                xml = ts.getString();
                 // <netbeans>
                int endOffset = ts.getBufferOffset();
                startOffset = endOffset-xml.length();
                // </netbeans>
                decompiler.addName(xml);
                mustMatchToken(Token.LC, "msg.syntax");
                decompiler.addToken(Token.LC);
                // <netbeans>
                //expr = (peekToken() == Token.RC)
                //    ? nf.createString("")
                //    : expr(false);
                if (peekToken() == Token.RC) {
                    expr = nf.createString("");
                    expr.setSourceBounds(startOffset, startOffset); // empty
                } else {
                    expr = expr(false);
                }
                // </netbeans>
                mustMatchToken(Token.RC, "msg.syntax");
                decompiler.addToken(Token.RC);
                if (pn == null) {
                    pn = nf.createString(xml);
                    // <netbeans>
                    pn.setSourceBounds(startOffset, endOffset);
                    // </netbeans>
                } else {
                    // <netbeans>
                    //pn = nf.createBinary(Token.ADD, pn, nf.createString(xml));
                    Node stringNode = nf.createString(xml);
                    stringNode.setSourceBounds(startOffset, endOffset);
                    pn = nf.createBinary(Token.ADD, pn, stringNode);
                    pn.setSourceBounds(pn.getSourceStart(), endOffset);
                    // </netbeans>
                }
// NetBeans: We don't care about the exact encoding of the XML parts since
// we're not executing the AST. The most important part is having accurate
// AST offsets and getting the embedded code fragments included in the AST
// such that variables and such show up for renaming, highlighting, etc.
//                if (ts.isXMLAttribute()) {
//                    /* Need to put the result in double quotes */
//                    expr = nf.createUnary(Token.ESCXMLATTR, expr);
//                    Node prepend = nf.createBinary(Token.ADD,
//                                                   nf.createString("\""),
//                                                   expr);
//                    expr = nf.createBinary(Token.ADD,
//                                           prepend,
//                                           nf.createString("\""));
//                } else {
                    expr = nf.createUnary(Token.ESCXMLTEXT, expr);
//                }
                pn = nf.createBinary(Token.ADD, pn, expr);
                // <netbeans>
                pn.setSourceBounds(pn.getSourceStart(), expr.getSourceEnd());
                // </netbeans>
                break;
            }
            case Token.XMLEND:
                xml = ts.getString();
                decompiler.addName(xml);
                // <netbeans>
                int endOffset = ts.getBufferOffset();
                startOffset = endOffset-xml.length();
                // </netbeans>
                if (pn == null) {
                    pn = nf.createString(xml);
                    // <netbeans>
                    pn.setSourceBounds(startOffset, endOffset);
                    // </netbeans>
                } else {
                    // <netbeans>
                    //pn = nf.createBinary(Token.ADD, pn, nf.createString(xml));
                    Node stringNode = nf.createString(xml);
                    stringNode.setSourceBounds(startOffset, endOffset);
                    pn = nf.createBinary(Token.ADD, pn, stringNode);
                    // </netbeans>
                }

                nf.addChildToBack(pnXML, pn);

                // <netbeans>
                //setSourceOffsets(pnXML, startOffset);
                pnXML.setSourceBounds(pnXML.getSourceStart(), endOffset);
                // The XML processing handles the token input stream differently;
                // it uses dedicated TokenStream methods instead of the normal
                // peekToken()/matchToken() mechanism, which means the
                // matchedTokenStart/matchedTokenEnd/peekedTokenSTart/peekedTokenEnd
                // variables aren't kept up to date. We need matchedTokenEnd
                // to be updated after this or the caller will reset the end
                // to exclude the proper end.
                matchedTokenEnd = endOffset;
                // </netbeans>

                return pnXML;
            default:
                reportError("msg.syntax");
                return null;
            }
        }
    }

    private void argumentList(Node listNode)
        throws IOException, ParserException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        boolean matched;
        matched = matchToken(Token.RP);
        if (!matched) {
            boolean first = true;
            // <netbeans>
            boolean seenGenerated = false;
            // </netbeans>
            do {
                if (!first)
                    decompiler.addToken(Token.COMMA);
                first = false;
                if (peekToken() == Token.YIELD) {
                    reportError("msg.yield.parenthesized");
                }
                // <netbeans>
                if (seenGenerated) {
                    int peekToken = peekToken();
                    if (peekToken == Token.RP) {
                        break;
                    }
                }

                //nf.addChildToBack(listNode, assignExpr(false));
                Node assignExpr = assignExpr(false);
                int peekToken = peekToken();
                // Prevent scenarios like issue 120499:
                // If I have "extra" tokens here, drop them
                while (peekToken == Token.NAME || peekToken == Token.STRING) {
                    if (peekToken == Token.NAME && GENERATED_IDENTIFIER.equals(ts.getString())) {
                        seenGenerated = true;
                        nextToken();
                    } else if (peekToken == Token.STRING && ts.getString().indexOf(GENERATED_IDENTIFIER) != -1) {
                        seenGenerated = true;
                        nextToken();
                    } else {
                        break;
                    }
                    peekToken = peekToken();
                }
                peekToken = peekToken();
                if (peekToken != Token.COMMA && peekToken != Token.RP) {
                    if (assignExpr.getType() == Token.NAME &&
                            GENERATED_IDENTIFIER.equals(assignExpr.getString())) {
                        assignExpr = assignExpr(false);
                    }
                }
                nf.addChildToBack(listNode, assignExpr);
                // </netbeans>
            } while (matchToken(Token.COMMA));

            mustMatchToken(Token.RP, "msg.no.paren.arg");
        }

        // <netbeans>
        setSourceOffsets(listNode, startOffset);
        // </netbeans>

        decompiler.addToken(Token.RP);
    }

    private Node memberExpr(boolean allowCallSyntax)
        throws IOException, ParserException
    {
        int tt;

        Node pn;

        /* Check for new expressions. */
        tt = peekToken();

        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        if (tt == Token.NEW) {
            /* Eat the NEW token. */
            consumeToken();
            decompiler.addToken(Token.NEW);

            // <netbeans>
            // include beginning of "new"
            startOffset = getStartOffset();
            // </netbeans>

            /* Make a NEW node to append to. */
            pn = nf.createCallOrNew(Token.NEW, memberExpr(false));

            // <netbeans>
            pn.setSourceBounds(startOffset, pn.getSourceEnd());
            // </netbeans>

            if (matchToken(Token.LP)) {
                decompiler.addToken(Token.LP);
                /* Add the arguments to pn, if any are supplied. */
                argumentList(pn);
            }

            /* XXX there's a check in the C source against
             * "too many constructor arguments" - how many
             * do we claim to support?
             */

            /* Experimental syntax:  allow an object literal to follow a new expression,
             * which will mean a kind of anonymous class built with the JavaAdapter.
             * the object literal will be passed as an additional argument to the constructor.
             */
            tt = peekToken();
            if (tt == Token.LC) {
                nf.addChildToBack(pn, primaryExpr());
            }
        } else {
            pn = primaryExpr();
        }

        // <netbeans>
        setSourceOffsets(pn, startOffset);
        // </netbeans>

        return memberExprTail(allowCallSyntax, pn);
    }

    private Node memberExprTail(boolean allowCallSyntax, Node pn)
        throws IOException, ParserException
    {
        // <netbeans>
        //int startOffset = getStartOffset();
        assert pn.getSourceStart() <= getStartOffset() : pn.getSourceStart() +"," + getStartOffset();
        int startOffset = pn.getSourceStart();
        // </netbeans>

      tailLoop:
        for (;;) {
            int tt = peekToken();
            switch (tt) {

              case Token.DOT:
              case Token.DOTDOT:
                {
                    int memberTypeFlags;
                    String s;

                    consumeToken();
                    decompiler.addToken(tt);
                    memberTypeFlags = 0;
                    if (tt == Token.DOTDOT) {
                        mustHaveXML();
                        memberTypeFlags = Node.DESCENDANTS_FLAG;
                    }
                    if (!compilerEnv.isXmlAvailable()) {
                        mustMatchToken(Token.NAME, "msg.no.name.after.dot");
                        s = ts.getString();
                        decompiler.addName(s);
                        pn = nf.createPropertyGet(pn, null, s, memberTypeFlags);
                        break;
                    }

                    tt = nextToken();
                    switch (tt) {
                    
                      // needed for generator.throw();
                      case Token.THROW:
                        decompiler.addName("throw");
                        pn = propertyName(pn, "throw", memberTypeFlags);
                       // <netbeans>
                        setSourceOffsets(pn.getLastChild(), getStartOffset());
                        // </netbeans>
                        break;

                      // handles: name, ns::name, ns::*, ns::[expr]
                      case Token.NAME:
                        s = ts.getString();
                        decompiler.addName(s);
                        pn = propertyName(pn, s, memberTypeFlags);
                        // <netbeans>
                        setSourceOffsets(pn.getLastChild(), getStartOffset());
                        // </netbeans>
                        break;

                      // handles: *, *::name, *::*, *::[expr]
                      case Token.MUL:
                        decompiler.addName("*");
                        pn = propertyName(pn, "*", memberTypeFlags);
                        break;

                      // handles: '@attr', '@ns::attr', '@ns::*', '@ns::*',
                      //          '@::attr', '@::*', '@*', '@*::attr', '@*::*'
                      case Token.XMLATTR:
                        decompiler.addToken(Token.XMLATTR);
                        pn = attributeAccess(pn, memberTypeFlags);
                        break;

                      default:
                          // <netbeans>
                        //reportError("msg.no.name.after.dot");
                        //addWarning("msg.no.name.after.dot");
                        String message = ScriptRuntime.getMessage0("msg.no.name.after.dot");
                        errorReporter.error(message, sourceURI, ts.getLineno(),
                                              ts.getLine(), getStartOffset()/*ts.getOffset()*/
                                            // <netbeans>
                                            , "msg.no.name.after.dot", null
                                            // </netbeans>
                                              );
                        pn = propertyName(pn, "@", memberTypeFlags); // NOI18N
                        Node nextChild = pn.getFirstChild().getNext();
                        if (nextChild != null) {
                            nextChild.setType(Token.MISSING_DOT);
                        }
                        setSourceOffsets(pn.getLastChild(), getStartOffset());
                        break;
                          // </netbeans>
                    }
                }
                break;

              case Token.DOTQUERY:
                consumeToken();
                mustHaveXML();
                decompiler.addToken(Token.DOTQUERY);
                pn = nf.createDotQuery(pn, expr(false), ts.getLineno());
                mustMatchToken(Token.RP, "msg.no.paren");
                decompiler.addToken(Token.RP);
                break;

              case Token.LB:
                consumeToken();
                decompiler.addToken(Token.LB);
                pn = nf.createElementGet(pn, null, expr(false), 0);
                mustMatchToken(Token.RB, "msg.no.bracket.index");
                decompiler.addToken(Token.RB);
                break;

              case Token.LP:
                if (!allowCallSyntax) {
                    break tailLoop;
                }
                consumeToken();
                decompiler.addToken(Token.LP);
                pn = nf.createCallOrNew(Token.CALL, pn);
                /* Add the arguments to pn, if any are supplied. */
                argumentList(pn);
                break;

              default:
                break tailLoop;
            }
        }
        // <netbeans>
        pn.setSourceBounds(startOffset, getEndOffset());
        // </netbeans>

        return pn;
    }

    /*
     * Xml attribute expression:
     *   '@attr', '@ns::attr', '@ns::*', '@ns::*', '@*', '@*::attr', '@*::*'
     */
    private Node attributeAccess(Node pn, int memberTypeFlags)
        throws IOException
    {
        // <netbeans>
        int startOffset = getStartOffset();
        // </netbeans>

        memberTypeFlags |= Node.ATTRIBUTE_FLAG;
        int tt = nextToken();

        switch (tt) {
          // handles: @name, @ns::name, @ns::*, @ns::[expr]
          case Token.NAME:
            {
                String s = ts.getString();
                decompiler.addName(s);
                pn = propertyName(pn, s, memberTypeFlags);
            }
            break;

          // handles: @*, @*::name, @*::*, @*::[expr]
          case Token.MUL:
            decompiler.addName("*");
            pn = propertyName(pn, "*", memberTypeFlags);
            break;

          // handles @[expr]
          case Token.LB:
            decompiler.addToken(Token.LB);
            pn = nf.createElementGet(pn, null, expr(false), memberTypeFlags);
            mustMatchToken(Token.RB, "msg.no.bracket.index");
            decompiler.addToken(Token.RB);
            break;

          default:
            reportError("msg.no.name.after.xmlAttr");
            pn = nf.createPropertyGet(pn, null, "?", memberTypeFlags);
            break;
        }

        // <netbeans>
        setSourceOffsets(pn, startOffset);
        // </netbeans>

        return pn;
    }

    /**
     * Check if :: follows name in which case it becomes qualified name
     */
    private Node propertyName(Node pn, String name, int memberTypeFlags)
        throws IOException, ParserException
    {
        // <netbeans>
        //int startOffset = getStartOffset();
        int startOffset = matchedTokenStart;

        // Issue 149019: Handle spurious extra __UNKNOWN__ tokens
        // in a property name
        if (GENERATED_IDENTIFIER.equals(name)) {
            int peek = peekToken();
            if (peek == Token.NAME) {
                nextToken();
                name = ts.getString();
            }
        }
        // </netbeans>

        String namespace = null;
        if (matchToken(Token.COLONCOLON)) {
            decompiler.addToken(Token.COLONCOLON);
            namespace = name;

            int tt = nextToken();
            switch (tt) {
              // handles name::name
              case Token.NAME:
                name = ts.getString();
                decompiler.addName(name);
                break;

              // handles name::*
              case Token.MUL:
                decompiler.addName("*");
                name = "*";
                break;

              // handles name::[expr]
              case Token.LB:
                decompiler.addToken(Token.LB);
                pn = nf.createElementGet(pn, namespace, expr(false),
                                         memberTypeFlags);
                mustMatchToken(Token.RB, "msg.no.bracket.index");
                decompiler.addToken(Token.RB);

                // <netbeans>
                setSourceOffsets(pn, startOffset);
                // </netbeans>

                return pn;

              default:
                reportError("msg.no.name.after.coloncolon");
                name = "?";
            }
        }

        pn = nf.createPropertyGet(pn, namespace, name, memberTypeFlags);

        // <netbeans>
        setSourceOffsets(pn, startOffset);
        // </netbeans>

        return pn;
    }

    private Node arrayComprehension(String arrayName, Node expr)
        throws IOException, ParserException
    {
        // <netbeans>
        //int startOffset = getStartOffset();
        int startOffset = matchedTokenStart;
        // </netbeans>

        if (nextToken() != Token.FOR)
            throw Kit.codeBug(); // shouldn't be here if next token isn't 'for'
        decompiler.addName(" "); // space after array literal expr
        decompiler.addToken(Token.FOR);
        boolean isForEach = false;
        if (matchToken(Token.NAME)) {
            decompiler.addName(ts.getString());
            if (ts.getString().equals("each")) {
                isForEach = true;
            } else {
                reportError("msg.no.paren.for");
            }
        }
        mustMatchToken(Token.LP, "msg.no.paren.for");
        decompiler.addToken(Token.LP);
        String name;
        int tt = peekToken();
        if (tt == Token.LB || tt == Token.LC) {
            // handle destructuring assignment
            name = currentScriptOrFn.getNextTempName();
            defineSymbol(Token.LP, false, name
                        // <netbeans>
                        , null
                        // </netbeans>
                    );
            // <netbeans>
            //expr = nf.createBinary(Token.COMMA,
            //    nf.createAssignment(Token.ASSIGN, primaryExpr(),
            //                        nf.createName(name)),
            //    expr);
            Node nameNode = nf.createName(name);
            nameNode.setSourceBounds(startOffset, startOffset);
            expr = nf.createBinary(Token.COMMA,
                nf.createAssignment(Token.ASSIGN, primaryExpr(),
                                    nameNode),
                expr);
            // </netbeans>
        } else if (tt == Token.NAME) {
            consumeToken();
            name = ts.getString();
            decompiler.addName(name);
        } else {
            reportError("msg.bad.var");
            return nf.createNumber(0);
        }

        Node init = nf.createName(name);
        // <netbeans>
        if (tt == Token.NAME) {
            init.setSourceBounds(getStartOffset(), getEndOffset());
        } else {
            init.setSourceBounds(startOffset, startOffset); // tempname, not in source
        }
        // </netbeans>

        // Define as a let since we want the scope of the variable to
        // be restricted to the array comprehension
        defineSymbol(Token.LET, false, name
                        // <netbeans>
                        , init
                        // </netbeans>
                );
        
        mustMatchToken(Token.IN, "msg.in.after.for.name");
        decompiler.addToken(Token.IN);
        Node iterator = expr(false);
        mustMatchToken(Token.RP, "msg.no.paren.for.ctrl");
        decompiler.addToken(Token.RP);
        
        Node body;
        tt = peekToken();
        if (tt == Token.FOR) {
            body = arrayComprehension(arrayName, expr);
        } else {
            Node call = nf.createCallOrNew(Token.CALL,
                nf.createPropertyGet(nf.createName(arrayName), null,
                                     "push", 0));
            call.addChildToBack(expr);
            body = new Node(Token.EXPR_VOID, call, ts.getLineno());
            // <netbeans>
            // All these nodes are synthetic
            call.setSourceBounds(startOffset, startOffset);
            body.setSourceBounds(startOffset, startOffset);
            // </netbeans>
            if (tt == Token.IF) {
                consumeToken();
                decompiler.addToken(Token.IF);
                int lineno = ts.getLineno();
                Node cond = condition();
                body = nf.createIf(cond, body, null, lineno);
            }
            mustMatchToken(Token.RB, "msg.no.bracket.arg");
            decompiler.addToken(Token.RB);
        }

        Node loop = enterLoop(null, true);
        try {
            // <netbeans>
            //return nf.createForIn(Token.LET, loop, init, iterator, body,
            //                      isForEach);
            Node pn = nf.createForIn(Token.LET, loop, init, iterator, body,
                                  isForEach);
            setSourceOffsets(pn, startOffset);
            return pn;
            // </netbeans>

        } finally {
            exitLoop(false);
        }
    }
    
    private Node primaryExpr()
        throws IOException, ParserException
    {
        Node pn;

        int ttFlagged = nextFlaggedToken();
        int tt = ttFlagged & CLEAR_TI_MASK;
        // <netbeans>
        int startOffset = matchedTokenStart;
        // </netbeans>

        switch(tt) {

          case Token.FUNCTION: {
            // <netbeans>
            //return function(FunctionNode.FUNCTION_EXPRESSION);
            Node fn = function(FunctionNode.FUNCTION_EXPRESSION);
            if (jsonMode) {
                addError("msg.json.error", Token.fullName(tt), fn);
            }
            return fn;
            // </netbeans>
          }

          case Token.LB: {
            ObjArray elems = new ObjArray();
            int skipCount = 0;
            int destructuringLen = 0;
            decompiler.addToken(Token.LB);
            boolean after_lb_or_comma = true;
            // <netbeans>
            Node prevExpr = null;
            // </netbeans>
            for (;;) {
                tt = peekToken();

                if (tt == Token.COMMA) {
                    consumeToken();
                    decompiler.addToken(Token.COMMA);
                    if (!after_lb_or_comma) {
                        after_lb_or_comma = true;
                    } else {
                        elems.add(null);
                        ++skipCount;
                    }
                } else if (tt == Token.RB) {
                    consumeToken();
                    decompiler.addToken(Token.RB);
                    // for ([a,] in obj) is legal, but for ([a] in obj) is 
                    // not since we have both key and value supplied. The
                    // trick is that [a,] and [a] are equivalent in other
                    // array literal contexts. So we calculate a special
                    // length value just for destructuring assignment.
                    destructuringLen = elems.size() + 
                                       (after_lb_or_comma ? 1 : 0);
                    break;
                } else if (skipCount == 0 && elems.size() == 1 &&
                           tt == Token.FOR)
                {
                    Node scopeNode = nf.createScopeNode(Token.ARRAYCOMP, 
                                                        ts.getLineno());
                    String tempName = currentScriptOrFn.getNextTempName();
                    pushScope(scopeNode);
                    try {
                        defineSymbol(Token.LET, false, tempName
                                // <netbeans>
                                , null // tempName shouldn't have duplicates
                                // </netbeans>
                                );
                        Node expr = (Node) elems.get(0);
                        Node block = nf.createBlock(ts.getLineno());
                        // <netbeans>
                        //Node init = new Node(Token.EXPR_VOID,
                        //    nf.createAssignment(Token.ASSIGN,
                        //        nf.createName(tempName),
                        //        nf.createCallOrNew(Token.NEW,
                        //            nf.createName("Array"))), ts.getLineno());
                        // These nodes are all synthetic
                        int offset = peekedTokenStart;
                        Node init = new Node(Token.EXPR_VOID,
                            nf.createAssignment(Token.ASSIGN,
                                setSourceOffsets(nf.createName(tempName), offset, offset),
                                nf.createCallOrNew(Token.NEW,
                                    setSourceOffsets(nf.createName("Array"), offset, offset))), ts.getLineno());
                        init.setSourceBounds(offset, offset);
                        block.setSourceBounds(offset, offset);
                        // </netbeans>
                        block.addChildToBack(init);
                        block.addChildToBack(arrayComprehension(tempName, 
                            expr));
                        scopeNode.addChildToBack(block);
                        // <netbeans>
                        //scopeNode.addChildToBack(nf.createName(tempName));
                        scopeNode.addChildToBack(setSourceOffsets(nf.createName(tempName), offset, offset));
                        // </netbeans>
                        return scopeNode;
                    } finally {
                        popScope();
                    }
                } else {
                    if (!after_lb_or_comma) {
                        reportError("msg.no.bracket.arg");
                    }
                    // <netbeans>
                    //elems.add(assignExpr(false));
                    prevExpr = assignExpr(false);
                    elems.add(prevExpr);
                    // </netbeans>
                    after_lb_or_comma = false;
                }
            }
            // <netbeans>
            //return nf.createArrayLiteral(elems, skipCount, destructuringLen);
            return setSourceOffsets(nf.createArrayLiteral(elems, skipCount, destructuringLen), startOffset);
            // </netbeans>
          }

          case Token.LC: {
            ObjArray elems = new ObjArray();
            // <netbeans>
            List<Node> nameNodes = new ArrayList<Node>();
            // </netbeans>
            decompiler.addToken(Token.LC);
            if (!matchToken(Token.RC)) {

                boolean first = true;
            commaloop:
                do {
                    Object property;

                    // <netbeans>
                    int trailingCommaOffset = getStartOffset();
                    // </netbeans>

                    if (!first)
                        decompiler.addToken(Token.COMMA);
                    else
                        first = false;

                    tt = peekToken();
                    switch(tt) {
                      case Token.NAME:
                        // <netbeans>
                        if (jsonMode) {
                            addError("msg.json.error", Token.fullName(tt), null);
                        }
                        // </netbeans>

                      case Token.STRING: {
                        consumeToken();
                        // map NAMEs to STRINGs in object literal context
                        // but tell the decompiler the proper type
                        String s = ts.getString();
                        if (tt == Token.NAME) {
                            if (s.equals("get") &&
                                peekToken() == Token.NAME) {
                                decompiler.addToken(Token.GET);
                                consumeToken();
                                s = ts.getString();
                                decompiler.addName(s);
                                property = ScriptRuntime.getIndexObject(s);
                                if (!getterSetterProperty(elems, property,
                                                          true))
                                    break commaloop;
                                break;
                            } else if (s.equals("set") &&
                                       peekToken() == Token.NAME) {
                                decompiler.addToken(Token.SET);
                                consumeToken();
                                s = ts.getString();
                                decompiler.addName(s);
                                property = ScriptRuntime.getIndexObject(s);
                                if (!getterSetterProperty(elems, property,
                                                          false))
                                    break commaloop;
                                break;
                            }
                            decompiler.addName(s);
                        } else {
                            decompiler.addString(s);
                        }
                        // <netbeans>
                        int nameStart = matchedTokenStart;
                        // </netbeans>
                        property = ScriptRuntime.getIndexObject(s);
                        plainProperty(elems, property);
                        // <netbeans>
                        if (elems.size() > 0) {
                            //Names of the class properties can be in '' and then
                            // the start of the possition doesn't match the string token.
                            // see issue #159083
                            if (tt == Token.STRING) {
                                nameStart++;
                            }
                            Node rhs = (Node) elems.get(elems.size()-1);
                            Node objLitName = new Node.LabelledNode(s, rhs);
                            objLitName.setSourceBounds(nameStart, nameStart + s.length());
                            nameNodes.add(objLitName);
                        }
                        // </netbeans>
                        break;
                      }

                      case Token.NUMBER:
                        consumeToken();
                        // <netbeans>
                        int nameStart = matchedTokenStart;
                        int nameEnd = matchedTokenEnd;
                        // </netbeans>
                        double n = ts.getNumber();
                        decompiler.addNumber(n);
                        property = ScriptRuntime.getIndexObject(n);
                        plainProperty(elems, property);
                        // <netbeans>
                        if (elems.size() > 0) {
                           Node rhs = (Node) elems.get(elems.size()-1);
                           String s = Double.toString(n);
                           if (s.endsWith(".0")) { // NOI18N
                               s = s.substring(0, s.length()-2);
                           }
                           Node objLitName = new Node.LabelledNode(s, rhs);
                           objLitName.setSourceBounds(nameStart, nameEnd);
                           nameNodes.add(objLitName);
                        }
                        // </netbeans>
                        break;

                      case Token.RC:
                        // trailing comma is OK.
                        // <netbeans>
                        // ...but not on all browsers - IE for example doesn't like it
                        if (compilerEnv.isStrictMode()) {
//                            addStrictWarning("msg.trailing.comma", ""
//                                    // <netbeans> - pass in additional parameters for the error
//                                    , Integer.valueOf(trailingCommaOffset)
//                                    // </netbeans>
//                                    );
                            String message = ScriptRuntime.getMessage0("msg.trailing.comma");
                            errorReporter.warning(message, sourceURI, ts.getLineno(),
                                                  ts.getLine(), trailingCommaOffset/*ts.getOffset()*/
                                                // <netbeans>
                                                , "msg.trailing.comma", Integer.valueOf(trailingCommaOffset)
                                                // </netbeans>
                                                  );

                        }
                        // </netbeans>

                        break commaloop;
                    default:
                        reportError("msg.bad.prop");
                        break commaloop;
                    }
                } while (matchToken(Token.COMMA));

                mustMatchToken(Token.RC, "msg.no.brace.prop");
            }
            decompiler.addToken(Token.RC);
            // <netbeans>
            //return nf.createObjectLiteral(elems);
            Node literal = nf.createObjectLiteral(elems);
            if (literal.hasChildren()) {
                Node child = literal.getFirstChild();
                Iterator<Node> it = nameNodes.iterator();
                do {
                    literal.addChildBefore(it.next(), child);
                    child = child.getNext();
                } while (child != null);
            }
            literal.setSourceBounds(startOffset, getEndOffset());
            return literal;
            // </netbeans>
          }
          
          case Token.LET:
            decompiler.addToken(Token.LET);
            return let(false);

          case Token.LP:
            // <netbeans>
            if (jsonMode) {
              addError("msg.json.error", Token.fullName(tt), null);
            }
            // </netbeans>

            /* Brendan's IR-jsparse.c makes a new node tagged with
             * TOK_LP here... I'm not sure I understand why.  Isn't
             * the grouping already implicit in the structure of the
             * parse tree?  also TOK_LP is already overloaded (I
             * think) in the C IR as 'function call.'  */
            decompiler.addToken(Token.LP);
            pn = expr(false);
            pn.putProp(Node.PARENTHESIZED_PROP, Boolean.TRUE);
            decompiler.addToken(Token.RP);
            mustMatchToken(Token.RP, "msg.no.paren");
            // <netbeans>
            setSourceOffsets(pn, startOffset);
            // </netbeans>
            return pn;

          case Token.XMLATTR:
            mustHaveXML();
            decompiler.addToken(Token.XMLATTR);
            pn = attributeAccess(null, 0);
            // <netbeans>
            setSourceOffsets(pn, startOffset);
            // </netbeans>
            return pn;

          case Token.NAME: {
            // <netbeans>
            if (jsonMode) {
              addError("msg.json.error", Token.fullName(tt), null);
            }
            // </netbeans>
            String name = ts.getString();
// <netbeans>
            if (GENERATED_IDENTIFIER.equals(name)) {
                int peek = peekToken();
                boolean inMiddle = false;
                switch (peek) {
                    case Token.FUNCTION:
                    case Token.LB:
                    case Token.LC:
                    case Token.LP:
                    case Token.XMLATTR:
                    case Token.NAME:
                    case Token.NUMBER:
                    case Token.STRING:
                    case Token.DIV:
                    case Token.ASSIGN_DIV:
                    case Token.NULL:
                    case Token.THIS:
                    case Token.FALSE:
                    case Token.TRUE:
                        inMiddle = true;
                }
                if (inMiddle) {
                    // The __UNKNOWN__ identifier is in the middle; we don't
                    // want it so skip it and resume
                    pn = primaryExpr();
                    // <netbeans>
                    setSourceOffsets(pn, startOffset);
                    // </netbeans>
                    return pn;
                }
            }
// </netbeans>
            if ((ttFlagged & TI_CHECK_LABEL) != 0) {
                if (peekToken() == Token.COLON) {
                    // Do not consume colon, it is used as unwind indicator
                    // to return to statementHelper.
                    // XXX Better way?
        // TODO - create position?
                    return nf.createLabel(ts.getLineno());
                }
            }

            decompiler.addName(name);
            if (compilerEnv.isXmlAvailable()) {
                pn = propertyName(null, name, 0);
            } else {
                pn = nf.createName(name);
            }
            // <netbeans>
            setSourceOffsets(pn, startOffset);
            // </netbeans>
            return pn;
          }

          case Token.NUMBER: {
            double n = ts.getNumber();
            decompiler.addNumber(n);
            // <netbeans>
            //return nf.createNumber(n);
            return setSourceOffsets(nf.createNumber(n), startOffset);
            // </netbeans>
          }

          case Token.STRING: {
            String s = ts.getString();
            decompiler.addString(s);
            // <netbeans>
            //return nf.createString(s);
            return setSourceOffsets(nf.createString(s), startOffset);
            // </netbeans>
          }

          case Token.DIV:
          case Token.ASSIGN_DIV: {
            // Got / or /= which should be treated as regexp in fact
            ts.readRegExp(tt);
            String flags = ts.regExpFlags;
            ts.regExpFlags = null;
            String re = ts.getString();
            decompiler.addRegexp(re, flags);
            int index = currentScriptOrFn.addRegexp(re, flags);
            // <netbeans>
            //return nf.createRegExp(index);
            // Regexp node processing doesn't use the match token calls,
            // it's character based, so I need to do manual arithmetic to
            // get the right results here rather than my usual
            //  setSourceOffsets(node, startOffset) call
            Node rn = nf.createRegExp(index);
            int endOffset = matchedTokenEnd+re.length()+flags.length()+1; // +1: closing /
            rn.setSourceBounds(startOffset, endOffset);
            matchedTokenEnd = endOffset;
            return rn;
            // </netbeans>
          }

          case Token.NULL:
          case Token.THIS:
          case Token.FALSE:
          case Token.TRUE:
            decompiler.addToken(tt);
            // <netbeans>
            //return nf.createLeaf(tt);
            return setSourceOffsets(nf.createLeaf(tt), startOffset);
            // </netbeans>

          case Token.RESERVED:
            reportError("msg.reserved.id");
            break;

          case Token.ERROR:
            /* the scanner or one of its subroutines reported the error. */
            break;

          case Token.EOF:
            reportError("msg.unexpected.eof");
            break;

          default:
            reportError("msg.syntax");
            break;
        }
        return null;    // should never reach here
    }

    private void plainProperty(ObjArray elems, Object property)
            throws IOException {
        mustMatchToken(Token.COLON, "msg.no.colon.prop");

        // OBJLIT is used as ':' in object literal for
        // decompilation to solve spacing ambiguity.
        decompiler.addToken(Token.OBJECTLIT);
        elems.add(property);
        elems.add(assignExpr(false));
    }

    private boolean getterSetterProperty(ObjArray elems, Object property,
                                         boolean isGetter) throws IOException {
        Node f = function(FunctionNode.FUNCTION_EXPRESSION);
        if (f.getType() != Token.FUNCTION) {
            reportError("msg.bad.prop");
            return false;
        }
        int fnIndex = f.getExistingIntProp(Node.FUNCTION_PROP);
        FunctionNode fn = currentScriptOrFn.getFunctionNode(fnIndex);
        if (fn.getFunctionName().length() != 0) {
            reportError("msg.bad.prop");
            return false;
        }
        elems.add(property);
        if (isGetter) {
            elems.add(nf.createUnary(Token.GET, f));
        } else {
            elems.add(nf.createUnary(Token.SET, f));
        }
        return true;
    }
// <netbeans>
    public TokenStream getTokenStream() {
        return ts;
    }

    public void setTokenStream(TokenStream ts) {
        this.ts = ts;
    }

    public FunctionNode parseFunction(String sourceString,
                                String sourceURI, int lineno)
    {
        this.sourceURI = sourceURI;
        this.ts = new TokenStream(this, null, sourceString, lineno);
        setJsonMode(false);

        this.decompiler = createDecompiler(compilerEnv);
        this.nf = new IRFactory(this);
        currentScriptOrFn = nf.createScript();
        currentScope = currentScriptOrFn;
        this.encodedSource = null;
        this.currentFlaggedToken = Token.EOF;

        // TODO - what about syntaxErrorCount?
        this.syntaxErrorCount = 0;

        try {
            int tt = peekToken();

            if (tt <= Token.EOF) {
                return null;
            }

            if (tt == Token.FUNCTION) {
                int startOffset = peekedTokenStart;
                consumeToken();
                try {
                    Node n = function(calledByCompileFunction
                                 ? FunctionNode.FUNCTION_EXPRESSION
                                 : FunctionNode.FUNCTION_STATEMENT);
                    setSourceOffsets(n, startOffset);
                    return this.syntaxErrorCount == 0 ? (FunctionNode)n : null;
                } catch (ParserException e) {
                    return null;
                }
            }
        } catch (IOException ex) {
            // Should never happen in the IDE
            throw new IllegalStateException();
        }


        return null;
    }

    public ScriptOrFnNode parseJson(String sourceString,
                                String sourceURI, int lineno)
    {
        this.sourceURI = sourceURI;
        this.ts = new TokenStream(this, null, sourceString, lineno);
        try {
            setJsonMode(true);
            return parseJson();
        } catch (IOException ex) {
            // Should never happen in the IDE
            throw new IllegalStateException();
        }
    }

    private ScriptOrFnNode parseJson()
        throws IOException
    {
        assert jsonMode;
        this.decompiler = createDecompiler(compilerEnv);
        this.nf = new IRFactory(this);
        currentScriptOrFn = nf.createScript();
        int sourceStartOffset = decompiler.getCurrentOffset();
// <netbeans>
        int realSourceStartOffset = ts.getBufferOffset();
// </netbeans>
        this.encodedSource = null;
        decompiler.addToken(Token.SCRIPT);

        this.currentFlaggedToken = Token.EOF;
        this.syntaxErrorCount = 0;

        int baseLineno = ts.getLineno();  // line number where source starts

        /* so we have something to add nodes to until
         * we've collected all the source */
        Node pn = nf.createLeaf(Token.BLOCK);

        try {
            mustMatchToken(Token.LC, "msg.json.expectedlc");
            int startOffset = getStartOffset();
            ObjArray elems = new ObjArray();
            // <netbeans>
            List<Node> nameNodes = new ArrayList<Node>();
            // </netbeans>
            decompiler.addToken(Token.LC);
            if (!matchToken(Token.RC)) {

                boolean first = true;
            commaloop:
                do {
                    Object property;

                    if (!first)
                        decompiler.addToken(Token.COMMA);
                    else
                        first = false;

                    int tt = peekToken();
                    switch(tt) {
                      case Token.NAME:
                      case Token.STRING: {
                        consumeToken();
                        // map NAMEs to STRINGs in object literal context
                        // but tell the decompiler the proper type
                        String s = ts.getString();
                        if (tt == Token.NAME) {
                            if (s.equals("get") &&
                                peekToken() == Token.NAME) {
                                decompiler.addToken(Token.GET);
                                consumeToken();
                                s = ts.getString();
                                decompiler.addName(s);
                                property = ScriptRuntime.getIndexObject(s);
                                if (!getterSetterProperty(elems, property,
                                                          true))
                                    break commaloop;
                                break;
                            } else if (s.equals("set") &&
                                       peekToken() == Token.NAME) {
                                decompiler.addToken(Token.SET);
                                consumeToken();
                                s = ts.getString();
                                decompiler.addName(s);
                                property = ScriptRuntime.getIndexObject(s);
                                if (!getterSetterProperty(elems, property,
                                                          false))
                                    break commaloop;
                                break;
                            }
                            decompiler.addName(s);
                        } else {
                            decompiler.addString(s);
                        }
                        // <netbeans>
                        int nameStart = matchedTokenStart;
                        // </netbeans>
                        property = ScriptRuntime.getIndexObject(s);
                        plainProperty(elems, property);
                        // <netbeans>
                        if (elems.size() > 0) {
                            Node rhs = (Node) elems.get(elems.size()-1);
                            Node objLitName = new Node.LabelledNode(s, rhs);
                            objLitName.setSourceBounds(nameStart, nameStart + s.length());
                            nameNodes.add(objLitName);
                        }
                        // </netbeans>
                        break;
                      }

                      case Token.NUMBER:
                        consumeToken();
                        // <netbeans>
                        int nameStart = matchedTokenStart;
                        int nameEnd = matchedTokenEnd;
                        // </netbeans>
                        double n = ts.getNumber();
                        decompiler.addNumber(n);
                        property = ScriptRuntime.getIndexObject(n);
                        plainProperty(elems, property);
                        // <netbeans>
                        if (elems.size() > 0) {
                           Node rhs = (Node) elems.get(elems.size()-1);
                           String s = Double.toString(n);
                           if (s.endsWith(".0")) { // NOI18N
                               s = s.substring(0, s.length()-2);
                           }
                           Node objLitName = new Node.LabelledNode(s, rhs);
                           objLitName.setSourceBounds(nameStart, nameEnd);
                           nameNodes.add(objLitName);
                        }
                        // </netbeans>
                        break;

                      case Token.RC:
                        // trailing comma is OK.
                        break commaloop;
                    default:
                        reportError("msg.bad.prop");
                        break commaloop;
                    }
                } while (matchToken(Token.COMMA));

                mustMatchToken(Token.RC, "msg.no.brace.prop");
            }
            decompiler.addToken(Token.RC);
            // <netbeans>
            //return nf.createObjectLiteral(elems);
            Node literal = nf.createObjectLiteral(elems);
            if (literal.hasChildren()) {
                Node child = literal.getFirstChild();
                Iterator<Node> it = nameNodes.iterator();
                do {
                    literal.addChildBefore(it.next(), child);
                    child = child.getNext();
                } while (child != null);
            }
            literal.setSourceBounds(startOffset, getEndOffset());
            pn.addChildToBack(literal);
        } catch (StackOverflowError ex) {
            String msg = ScriptRuntime.getMessage0(
                "msg.too.deep.parser.recursion");
            throw Context.reportRuntimeError(msg, sourceURI,
                                             ts.getLineno(), null, 0);
        }

        if (this.syntaxErrorCount != 0) {
            String msg = String.valueOf(this.syntaxErrorCount);
            msg = ScriptRuntime.getMessage1("msg.got.syntax.errors", msg);
// <netbeans>
            //throw errorReporter.runtimeError(msg, sourceURI, baseLineno,
            //                                 null, 0);
            // Don't show an error for this at all!
            //EvaluatorException exc = errorReporter.runtimeError(msg, sourceURI, baseLineno,
            //                                 null, 0);
            //if (exc != null) {
            //    throw exc;
            //} else {
//                currentScriptOrFn.setSourceName(sourceURI);
//                currentScriptOrFn.setBaseLineno(baseLineno);
//                currentScriptOrFn.setEndLineno(ts.getLineno());
//
//                return currentScriptOrFn;
return null;
            //}
// </netbeans>
        }

        currentScriptOrFn.setSourceName(sourceURI);
        currentScriptOrFn.setBaseLineno(baseLineno);
        currentScriptOrFn.setEndLineno(ts.getLineno());

        int sourceEndOffset = decompiler.getCurrentOffset();
        currentScriptOrFn.setEncodedSourceBounds(sourceStartOffset,
                                                 sourceEndOffset);
// <netbeans>
        int realSourceEndOffset = getEndOffset();
        currentScriptOrFn.setSourceBounds(realSourceStartOffset, realSourceEndOffset);
// </netbeans>


        nf.initScript(currentScriptOrFn, pn);

        if (compilerEnv.isGeneratingSource()) {
            encodedSource = decompiler.getEncodedSource();
        }
        this.decompiler = null; // It helps GC

        return currentScriptOrFn;
    }
// </netbeans>
}
