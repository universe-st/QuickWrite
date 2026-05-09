package net.gsantner.markor.format.plaintext;
/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/

import com.universe_st.markor_editor.EditorConfig;
import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;

public class PlaintextSyntaxHighlighter extends SyntaxHighlighterBase {

    public PlaintextSyntaxHighlighter() {
        super();
    }

    public PlaintextSyntaxHighlighter(final EditorConfig config) {
        super(config);
    }

    @Override
    protected void generateSpans() {
        createTabSpans(_tabSize);
        createUnderlineHexColorsSpans();
        createSmallBlueLinkSpans();
    }
}
