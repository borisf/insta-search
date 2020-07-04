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
 package com.borisfarber.controllers;

 import com.borisfarber.data.Pair;
 import com.borisfarber.ui.Highlighter;

 import javax.swing.*;
 import javax.swing.event.DocumentEvent;
 import javax.swing.event.DocumentListener;
 import javax.swing.text.*;
 import java.io.*;

 import static com.borisfarber.controllers.FileSearch.testLoad;
 import static com.borisfarber.ui.Repl.repl;

 public final class Controller implements DocumentListener {
     private JTextPane resultTextPane;
     private final JTextArea previewTextArea;
     private final JLabel resultCountLabel;

     private String query;
     private FileSearch search;
     private Pair<String, Integer> filenameAndPosition =
             new Pair<>("test.txt",0);
     private int selectedGuiIndex = 0;

     public Controller(final JTextPane resultTextPane,
                       final JTextArea previewArea,
                       final JLabel resultCountLabel) {
         this.resultTextPane = resultTextPane;
         this.previewTextArea = previewArea;
         this.resultCountLabel = resultCountLabel;

         search = new FileSearch();
     }

     public void crawl(final File file) {
         if (file == null || !file.exists()) {
             return;
         }

         search = new FileSearch();
         search.crawl(file);
     }

     public void testCrawl() {
         search.testCrawl(testLoad());
     }

     @Override
     public void insertUpdate(final DocumentEvent evt) {
         // letter
         Document document = evt.getDocument();
         runNewSearch(document);

         updateGUI();
     }

     @Override
     public void removeUpdate(final DocumentEvent evt) {
         // when new folder is loaded, this callback gets called
         Document document = evt.getDocument();

         try {
             final String query = document.getText(0,
                     document.getLength());

             if(query.length() > 1) {
                 search(query);
                 updateGUI();
             } else {
                 resultTextPane.setText("");
                 previewTextArea.setText("");
                 resultCountLabel.setText("");
             }

         } catch (BadLocationException e) {
             e.printStackTrace();
         }
     }

     @Override
     public void changedUpdate(final DocumentEvent evt) {
         Document document = evt.getDocument();
         runNewSearch(document);
     }

     public void upPressed() {
         if(selectedGuiIndex > 0) {
             selectedGuiIndex--;
         }

         updateGUI();
     }

     public void downPressed() {
         if(selectedGuiIndex < (Integer.parseInt(search.getResultSetCount()) - 1)) {
             selectedGuiIndex++;
         }

         updateGUI();
     }

     public void enterPressed() {
         try {
             String fullPath = search.getNameToPaths().get(filenameAndPosition.t).toString();
             String command = "nvim +" + Integer.parseInt(String.valueOf(filenameAndPosition.u)) +
                     " " + fullPath;

             Terminal.executeInTerminal(command);
         } catch (Exception e) {
             e.printStackTrace();
             // TODO workaround for no vim
             //Desktop desktop = Desktop.getDesktop();
             //desktop.open(filenameAndPosition.u);

         }
     }

     public void search(String query) {
         selectedGuiIndex = 0;
         search.search(query);
         this.query = query;
     }

     public String dump() {
         System.out.println(search.getResults());
         System.out.println(search.getPreview(0));
         System.out.println(search.getResultSetCount());

         return "";
     }

     private void runNewSearch(final Document searchQueryDoc) {
         try {
             final String query = searchQueryDoc.getText(0,
                     searchQueryDoc.getLength());
             search(query);
         }
         catch (Exception ex) {
             ex.printStackTrace();
         }
     }

     private void updateGUI() {
         // show selector
         int i = 0;
         Highlighter hl = new Highlighter();
         StringBuilder builder = new StringBuilder();
         for (String res : search.getResultSet()) {
             filenameAndPosition = search.getFileNameAndPosition(res);

             String resultLine = filenameAndPosition.t + ":" + filenameAndPosition.u + ":" + res;

             if(i == selectedGuiIndex) {
                 builder.append("==> " + resultLine);
             } else {
                 builder.append(resultLine);
             }
             i++;

             builder.append("\n");
         }
         resultTextPane.setText(builder.toString());
         resultTextPane.setCaretPosition(0);

         hl.highlight(resultTextPane, query);

         // the usual updates
         previewTextArea.setText(search.getPreview(selectedGuiIndex));
         resultCountLabel.setText(search.getResultSetCount());
     }

     public static void main(String[] args) {
         repl();
     }
 }