 /*
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package com.borisfarber.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import javax.swing.text.Highlighter.HighlightPainter;

public class Highlighter {

    // Creates highlights around all occurrences of pattern in textPane
    public void highlight(JTextPane textPane, String pattern) {
        // First remove all old highlights
        removeHighlights(textPane);

        try {
            javax.swing.text.Highlighter hilite = textPane.getHighlighter();
            Document doc = textPane.getDocument();
            String text = doc.getText(0, doc.getLength());

            /// background
            MutableAttributeSet attrs = textPane.getInputAttributes();
            StyledDocument doc1 = textPane.getStyledDocument();
            ///

            int pos = 0;
            // Search for pattern
            while ((pos = text.indexOf(pattern, pos)) >= 0) {
                // Create highlighter using private painter and apply around pattern
                hilite.addHighlight(pos, pos + pattern.length(), myHighlightPainter);

                // back
                StyleConstants.setForeground(attrs, Colors.BACKGROUND_COLOR);
                doc1.setCharacterAttributes(pos, pos + pattern.length(), attrs, false);
                StyleConstants.setForeground(attrs, Colors.FOREGROUND_COLOR);
                doc1.setCharacterAttributes(pos + pattern.length(), text.length(),attrs, false)  ;
                // end back

                pos += pattern.length();
            }

        } catch (BadLocationException e) {
        }
    }

    // Removes only our private highlights
    private void removeHighlights(JTextComponent textComp) {
        javax.swing.text.Highlighter hilite = textComp.getHighlighter();
        javax.swing.text.Highlighter.Highlight[] hilites = hilite.getHighlights();

        for (int i = 0; i < hilites.length; i++) {
            if (hilites[i].getPainter() instanceof MyHighlightPainter) {
                hilite.removeHighlight(hilites[i]);
            }
        }
    }
    // An instance of the private subclass of the default highlight painter
    // TODO may be yellow
   HighlightPainter myHighlightPainter = new MyHighlightPainter(Colors.FOREGROUND_COLOR);

    // A private subclass of the default highlight painter
    private static class MyHighlightPainter
            extends DefaultHighlighter.DefaultHighlightPainter {

        public MyHighlightPainter(Color color) {
            super(color);
        }
    }

}
