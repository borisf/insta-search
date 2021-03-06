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

 import static com.borisfarber.instasearch.ui.ColorScheme.BACKGROUND_COLOR;
 import static com.borisfarber.instasearch.ui.ColorScheme.FOREGROUND_COLOR;

 public class PreviewHighlighter {

     public void highlight(JTextPane previewTextPane, String previewLine, Color color) {
         DefaultHighlighter.DefaultHighlightPainter previewHighlighter =
                 new DefaultHighlighter.DefaultHighlightPainter(color);

         try {
             Document doc = previewTextPane.getDocument();
             String text = doc.getText(0, doc.getLength());

             int startPosition = text.toLowerCase().indexOf(previewLine.toLowerCase());

             if(startPosition == -1) {
                 // hex view in the preview, no reason to highlight
                 //System.out.println("here");
                 return;
             }

             while(text.charAt(startPosition) == ' ') {
                 startPosition++;
             }

             int endPosition = startPosition + previewLine.length();

             while(text.charAt(endPosition) == ' ') {
                 endPosition--;
             }

             previewTextPane.getHighlighter().addHighlight(startPosition, endPosition, previewHighlighter);

             // back, UX less focus on the preview
             MutableAttributeSet attrs = previewTextPane.getInputAttributes();
             StyledDocument doc1 = previewTextPane.getStyledDocument();
             StyleConstants.setForeground(attrs, BACKGROUND_COLOR);
             doc1.setCharacterAttributes(startPosition, endPosition, attrs, false);
             StyleConstants.setForeground(attrs, FOREGROUND_COLOR);
             doc1.setCharacterAttributes(endPosition, text.length(),attrs, false);
             // end back
         } catch (final BadLocationException ble) {
             System.err.println("Ignored in highlight preview");
         }
     }
 }
