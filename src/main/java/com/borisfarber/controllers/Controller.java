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
 import com.borisfarber.ui.Background;
 import com.borisfarber.ui.Highlighter;

 import javax.swing.*;
 import javax.swing.event.DocumentEvent;
 import javax.swing.event.DocumentListener;
 import javax.swing.text.*;
 import java.awt.*;
 import java.io.*;
 import java.nio.charset.StandardCharsets;
 import java.nio.file.Files;
 import java.nio.file.Paths;
 import java.util.Set;
 import java.util.TreeSet;

 import static com.borisfarber.controllers.FuzzySearch.testLoad;
 import static com.borisfarber.ui.Repl.repl;

 public final class Controller implements DocumentListener {
     public static final String SELECTOR = "==> ";
     public static final int UI_VIEW_LIMIT = 50;
     public JTextPane resultTextPane;
     private final JTextArea previewTextArea;
     private final JLabel resultCountLabel;

     private String query;
     private Search search;
     private Pair<String, Integer> editorFilenameAndPosition =
             new Pair<>("test.txt",0);
     private int selectedGuiIndex = 0;

     public Controller(final JTextPane resultTextPane,
                       final JTextArea previewArea,
                       final JLabel resultCountLabel) {
         this.resultTextPane = resultTextPane;
         this.previewTextArea = previewArea;
         this.resultCountLabel = resultCountLabel;

         search = new GrepSearch(this);
         //search = new FuzzySearch(this);
     }

     public void crawl(final File file) {
         if (file == null || !file.exists()) {
             return;
         }

         search = new GrepSearch(this);
         //search = new FuzzySearch(this);
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

     public void fileOpened(File newFile) {
         try {
             if(newFile != null) {
                 resultTextPane.setText(Background.SHARK_BG);
                 previewTextArea.setText("");
                 resultCountLabel.setText("");
                 crawl(newFile);
             }
         } catch (Exception e) {
             e.printStackTrace();
         }
         return;
     }

     public void upPressed() {
         if(selectedGuiIndex > 0) {
             selectedGuiIndex--;
         }

         onUpdateGUI();
     }

     public void downPressed() {
         if((selectedGuiIndex < (Integer.parseInt(search.getResultSetCount()) - 1))
                 && selectedGuiIndex < (UI_VIEW_LIMIT - 1)) {
             selectedGuiIndex++;
         }

         onUpdateGUI();
     }

     public void enterPressed() {
         String fullPath = search.getNameToPaths().get(editorFilenameAndPosition.t).toString();

         try {
             String command = "nvim +\"set number\" +"
                     + Integer.parseInt(String.valueOf(editorFilenameAndPosition.u)) +
                     " " + fullPath;
             Terminal.executeInLinux(command);
         } catch (Exception e) {
             try {
                 Desktop desktop = Desktop.getDesktop();
                 desktop.open(new File(fullPath));
             } catch (IOException ioException) {
                 try {
                     String content =
                             Files.readString(Paths.get(editorFilenameAndPosition.t), StandardCharsets.US_ASCII);
                     previewTextArea.setText(content);
                 } catch (IOException exception) {
                     previewTextArea.setText("Something is wrong with the file " + exception.getMessage());
                 }
             }
         }
     }

     public void onFileDragged(File file) {
         previewTextArea.setText("");
         resultTextPane.setText(Background.SHARK_BG);
         crawl(file);
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

     public synchronized void onUpdateGUI() {
         int i = 0;
         StringBuilder builder = new StringBuilder();
         Pair<String, Integer> filenameAndPosition;

         Set<String> resultPreview = new TreeSet<>(new ResultsSorter());

         for (String res : search.getResultSet()) {
             filenameAndPosition = search.getFileNameAndPosition(res);
             String line = filenameAndPosition.t + ":" +
                     filenameAndPosition.u +":" + res + "\n";

             if(i == selectedGuiIndex) {
                 editorFilenameAndPosition.t = filenameAndPosition.t;
                 editorFilenameAndPosition.u = filenameAndPosition.u;
             }
             resultPreview.add(line);
             i++;

             if (i >= UI_VIEW_LIMIT) {
                 // at most show 50 results

                 // TODO stopped here issue with same lines
                 // TODO need to keep ui_view_limit as a class member
                 break;
             }
         }

         i = 0;
         for (String str : resultPreview) {
             if(i == selectedGuiIndex) {
                 builder.append(SELECTOR + str);
             } else {
                 builder.append(str);
             }
             i++;
         }

         resultTextPane.setText(builder.toString());
         try {
             int selector = builder.toString().indexOf(SELECTOR);
             resultTextPane.setCaretPosition(selector);
         } catch (IllegalArgumentException iae) {

         }

         if(query != null) {
             Highlighter highlighter = new Highlighter();
             highlighter.highlight(resultTextPane, query);
         }

         // the usual updates
         previewTextArea.setText(search.getPreview(selectedGuiIndex));
         resultCountLabel.setText(search.getResultSetCount());
     }

     public void close() {
         search.close();
     }

     public static void main(String[] args) {
         repl();
     }
 }