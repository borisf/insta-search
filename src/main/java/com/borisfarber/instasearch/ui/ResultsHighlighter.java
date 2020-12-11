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
 import javax.swing.text.BadLocationException;
 import javax.swing.text.DefaultHighlighter;
 import javax.swing.text.Document;
 import javax.swing.text.Highlighter;
 import java.awt.*;

 public class ResultsHighlighter {

     public enum HIGHLIGHT_SPAN {
         LONG(1000),
         SHORT(100);

         private final int value;

         HIGHLIGHT_SPAN(int value) {
             this.value = value;
         }
     }

     private final Document doc;
     private final Highlighter hilite;

     private final DefaultHighlighter.DefaultHighlightPainter previewHighlighter;

     public ResultsHighlighter(JTextPane textPane, Color color) {
         this.previewHighlighter =
                 new DefaultHighlighter.DefaultHighlightPainter(color);

         this.hilite = textPane.getHighlighter();
         this.doc = textPane.getDocument();
     }

     public void highlight(int from, HIGHLIGHT_SPAN span , String pattern) {
         try {
             int interval = span.value;
             // borders to highlight
             from = from - interval;
             if(from < 0) {
                 from = 0;
             }

             int to = (from + interval);
             if (to > doc.getLength()) {
                 to = doc.getLength();
             }
             String text = doc.getText(from, to);

             int pos = 0;

             // Search for the pattern
             String textLowercase = text.toLowerCase();
             String patternLowercase = pattern.toLowerCase();

             while ((pos = textLowercase.indexOf(patternLowercase, pos)) >= 0) {
                 int startMatch = from + pos;
                 hilite.addHighlight(startMatch, startMatch + pattern.length(), previewHighlighter);
                 pos += pattern.length();
             }
         } catch (BadLocationException e) {
             System.err.println("Ignored in highlight results");
         }
     }
 }
