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

import javax.swing.text.BadLocationException;
import org.netbeans.modules.csl.api.Formatter;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.editor.indent.spi.Context;
import org.openide.util.Exceptions;

/**
 * @author Denis Stepanov
 */
public class CoffeeScriptFormatter implements Formatter {

    public void reformat(Context context, ParserResult compilationInfo) {
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
}
