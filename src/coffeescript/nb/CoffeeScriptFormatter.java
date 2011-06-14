// Copyright 2011 Denis Stepanov
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package coffeescript.nb;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.TreeSet;
import javax.swing.text.BadLocationException;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.csl.api.Formatter;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.editor.indent.api.IndentUtils;
import org.netbeans.modules.editor.indent.spi.Context;
import org.openide.util.Exceptions;

/**
 * @author Denis Stepanov
 */
public class CoffeeScriptFormatter implements Formatter {

    public void reformat(Context context, ParserResult compilationInfo) {
        TokenHierarchy<CoffeeScriptTokenId> th = createTokenHierarchy(context);
        TokenSequence<CoffeeScriptTokenId> ts = th.tokenSequence(CoffeeScriptLanguage.getLanguage());
        Collection<IndentChange> indentChanges = new TreeSet<IndentChange>();
        Deque<Indent> indents = new LinkedList<Indent>();
        try {
            int currentLineStartOffset = 0;
            while (ts.moveNext()) {
                Token<CoffeeScriptTokenId> token = ts.token();
                int tokenOffset = token.offset(th);
                if (tokenOffset >= 0) {
                    int tokenLineStartOffset = context.lineStartOffset(tokenOffset);
                    if (currentLineStartOffset != tokenLineStartOffset) {
                        currentLineStartOffset = tokenLineStartOffset;
                        int tokenLineIndent = context.lineIndent(tokenLineStartOffset);
                        while (!indents.isEmpty() && (indents.peek().getIndent() >= tokenLineIndent)) {
                            indents.pop();
                        }
                        int lineIndents = indents.isEmpty() ? 0 : indents.peek().getIndents();
                        int lineIndent = indents.isEmpty() ? 0 : indents.peek().getIndent();
                        if (lineIndent < tokenLineIndent) {
                            lineIndents++;
                        } else if (lineIndent > tokenLineIndent) {
                            lineIndents--;
                        }
                        indents.push(new Indent(tokenLineIndent, lineIndents));
                        indentChanges.add(new IndentChange(tokenLineStartOffset, IndentUtils.indentLevelSize(context.document()) * (lineIndents < 0 ? 0 : lineIndents)));
                    }
                }
            }
            int offsetChange = 0;
            for (IndentChange indentChange : indentChanges) {
                int indentOffsetChange = indentChange.getIndent() - context.lineIndent(offsetChange + indentChange.getOffset());
                if (indentOffsetChange != 0) {
                    context.modifyIndent(offsetChange + indentChange.getOffset(), indentChange.getIndent());
                    offsetChange += indentOffsetChange;
                }
            }
        } catch (BadLocationException ble) {
            Exceptions.printStackTrace(ble);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected TokenHierarchy<CoffeeScriptTokenId> createTokenHierarchy(Context context) {
        return (TokenHierarchy) TokenHierarchy.get(context.document());
    }

    public void reindent(Context context) {
        try {
            int currentLineStartOffset = context.lineStartOffset(context.startOffset());
            if (currentLineStartOffset > 0) {
                int prevLineIndent = context.lineIndent(context.lineStartOffset(currentLineStartOffset - 1));
                int currentLineIndent = context.lineIndent(currentLineStartOffset);
                if (currentLineIndent != prevLineIndent) {
                    context.modifyIndent(currentLineStartOffset, prevLineIndent);
                }
            }
        } catch (BadLocationException ble) {
            Exceptions.printStackTrace(ble);
        }
    }

    public boolean needsParserResult() {
        return false;
    }

    public int indentSize() {
        return -1;
    }

    public int hangingIndentSize() {
        return -1;
    }

    private static class Indent {

        private final int indent, indents;

        public Indent(int indent, int indents) {
            this.indent = indent;
            this.indents = indents;
        }

        public int getIndent() {
            return indent;
        }

        public int getIndents() {
            return indents;
        }
    }

    private static class IndentChange implements Comparable<IndentChange> {

        private final int offset;
        private int indent;

        public IndentChange(int offset, int indent) {
            this.offset = offset;
            this.indent = indent;
        }

        public int getIndent() {
            return indent < 0 ? 0 : indent;
        }

        public void setIndent(int indent) {
            this.indent = indent;
        }

        public int getOffset() {
            return offset;
        }

        public int compareTo(IndentChange t) {
            return Integer.valueOf(offset).compareTo(t.offset);
        }
    }
}
