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
 package com.borisfarber.instasearch.ui;

 import javax.swing.*;
 import javax.swing.text.*;
 import java.awt.*;

 public class Highlighter {
     public void highlightSearch(JTextPane textPane, String pattern, Color color) {

         DefaultHighlighter.DefaultHighlightPainter previewHighlighter =
                 new DefaultHighlighter.DefaultHighlightPainter(color);

         try {
             javax.swing.text.Highlighter hilite = textPane.getHighlighter();
             Document doc = textPane.getDocument();
             String text = doc.getText(0, doc.getLength());

             /// background
             MutableAttributeSet attrs = textPane.getInputAttributes();
             StyledDocument doc1 = textPane.getStyledDocument();
             int pos = 0;

             // Search for pattern
             while ((pos = text.indexOf(pattern, pos)) >= 0) {
                 // Create highlighter using private painter and apply around pattern
                 hilite.addHighlight(pos, pos + pattern.length(), previewHighlighter);

                 // back
                 StyleConstants.setForeground(attrs, InstaSearch.BACKGROUND_COLOR);
                 doc1.setCharacterAttributes(pos, pos + pattern.length(), attrs, false);
                 StyleConstants.setForeground(attrs, InstaSearch.FOREGROUND_COLOR);
                 doc1.setCharacterAttributes(pos + pattern.length(), text.length(),attrs, false);
                 // end back

                 pos += pattern.length();
             }
         } catch (BadLocationException e) {
             System.err.println("Ignored in highlight results");
         }
     }

     public void highlightPreview(JTextPane previewTextPane, String selectedLine, Color color) {
         if(selectedLine.endsWith("\n\n")) {
             selectedLine = selectedLine.substring(0, selectedLine.length() - 2);
         }

         DefaultHighlighter.DefaultHighlightPainter previewHighlighter =
                 new DefaultHighlighter.DefaultHighlightPainter(color);

         try {
             Document doc = previewTextPane.getDocument();
             String text = doc.getText(0, doc.getLength());

             int pos = text.indexOf(selectedLine);

             if(pos == -1) {
                 // TODO
                 System.err.println("wrong search index");
                 return;
             }

             previewTextPane.getHighlighter().addHighlight(pos,
                     pos + selectedLine.length(), previewHighlighter);

             // back, UX less focus on the preview
             MutableAttributeSet attrs = previewTextPane.getInputAttributes();
             StyledDocument doc1 = previewTextPane.getStyledDocument();
             StyleConstants.setForeground(attrs, InstaSearch.BACKGROUND_COLOR);
             doc1.setCharacterAttributes(pos, pos + selectedLine.length(), attrs, false);
             StyleConstants.setForeground(attrs, InstaSearch.FOREGROUND_COLOR);
             doc1.setCharacterAttributes(pos + selectedLine.length(), text.length(),attrs, false);
             // end back
         } catch (final BadLocationException ble) {
             System.err.println("Ignored in highlight preview");
         }
     }
 }
