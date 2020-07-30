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
 import com.borisfarber.search.*;
 import com.borisfarber.ui.Background;
 import com.borisfarber.ui.Highlighter;

 import javax.swing.*;
 import javax.swing.event.DocumentEvent;
 import javax.swing.event.DocumentListener;
 import javax.swing.text.*;
 import java.awt.*;
 import java.io.File;
 import java.io.IOException;
 import java.nio.charset.StandardCharsets;
 import java.nio.file.Files;
 import java.nio.file.Paths;
 import java.util.ArrayList;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.concurrent.Executors;
 import java.util.concurrent.ThreadPoolExecutor;

 import static com.borisfarber.search.FuzzySearch.testLoad;
 import static com.borisfarber.ui.InstaSearch.FOREGROUND_COLOR;
 import static com.borisfarber.ui.Repl.repl;

 public final class Controller implements DocumentListener {
     public static final String SELECTOR = "==> ";
     public static final int UI_VIEW_LIMIT = 50;
     public JTextPane resultTextPane;
     private JTextPane previewTextPane;
     private final JLabel resultCountLabel;

     private ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(1);
     private String query;
     private Search search;
     private Pair<String, Integer> editorFilenameAndPosition =
             new Pair<>("test.txt",0);
     private int selectedGuiIndex = 0;
     private int numLines = 0;

     public Controller(JTextPane resultTextPane,
                       JTextPane previewArea,
                       JLabel resultCountLabel) {
         this.resultTextPane = resultTextPane;
         this.previewTextPane = previewArea;
         this.resultCountLabel = resultCountLabel;

         search = new MockSearch(this);
     }

     public void crawl(final File file) {
         if (file == null || !file.exists()) {
             return;
         }

         createSearch(file);

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
                 previewTextPane.setText("");
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
                 previewTextPane.setText("");
                 resultCountLabel.setText("");

                 createSearch(newFile);

                 crawl(newFile);
             }
         } catch (Exception e) {
             e.printStackTrace();
         }
         return;
     }

     private void createSearch(File newFile) {
         if (newFile.isDirectory()) {
             search = new FuzzySearch(this);
         } else {
             if (newFile.getName().endsWith("apk") || newFile.getName().endsWith("zip")) {
                 search = new ApkSearch(newFile, this);
             } else {
                 search = new GrepSearch(this);
             }
         }
     }

     public void upPressed() {
         if(selectedGuiIndex > 0) {
             selectedGuiIndex--;
         }

         onUpdateGUI();
     }

     public void downPressed() {
         if((selectedGuiIndex < (Integer.parseInt(search.getResultSetCount()) - 1))
                 && selectedGuiIndex < (numLines - 1)) {
             selectedGuiIndex++;
         }

         onUpdateGUI();
     }

     public void enterPressed() {
         String fullPath = search.getFilenamesToPathes().get(editorFilenameAndPosition.t).toString();

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
                     previewTextPane.setText(content);
                 } catch (IOException exception) {
                     previewTextPane.setText("Something is wrong with the file " + exception.getMessage());
                 }
             }
         }
     }

     public void onFileDragged(File file) {
         resultTextPane.setText(Background.SHARK_BG);
         previewTextPane.setText("");
         crawl(file);
     }

     public void search(String query) {
         selectedGuiIndex = 0;
         this.query = query;

         Runnable runnableTask = () -> search.search(query);
         long waitingTasksCount = executor.getActiveCount();
         if(waitingTasksCount < 1) {
             executor.submit(runnableTask);
         }
     }

     public String dump() {
         System.out.println(search.getResults());
         System.out.println(search.getPreview(""));
         System.out.println(search.getResultSetCount());

         return "";
     }

     private void runNewSearch(final Document searchQueryDoc) {
         try {
             final String query = searchQueryDoc.getText(0,
                     searchQueryDoc.getLength());
             this.query = query;
             search(query);
         }
         catch (Exception ex) {
             ex.printStackTrace();
         }
     }

     public void onUpdateGUI() {
         int previewLinesIndex = 0;
         StringBuilder builder = new StringBuilder();
         LinkedList<Pair<String, Integer>> filenamesAndPositions;
         ArrayList<String> searchPreview = new ArrayList<>();
         String selectedLine = "";
         boolean isViewLimitReached = false;

         List<String> searchResults = search.getResultSet();
         while ((previewLinesIndex < searchResults.size()) && !isViewLimitReached) {
             String rawLine = searchResults.get(previewLinesIndex);
             filenamesAndPositions = search.getFileNameAndPosition(rawLine);

             for(Pair<String, Integer> currentSearch : filenamesAndPositions) {
                 String line = currentSearch.t + ":" +
                         currentSearch.u +":" + rawLine + "\n";

                 searchPreview.add(line);
                 previewLinesIndex++;

                 if(previewLinesIndex >= UI_VIEW_LIMIT) {
                     isViewLimitReached = true;
                     break;
                 }
             }
         }

         searchPreview.sort(new SearchResultsSorter());
         numLines = previewLinesIndex;
         previewLinesIndex = 0;

         for (String str : searchPreview) {
             if(previewLinesIndex == selectedGuiIndex) {
                 builder.append(SELECTOR + str);

                 String[] parts  = str.split(":");
                 String fileName = parts[0];
                 String line = parts[1];
                 selectedLine = parts[2];

                 editorFilenameAndPosition.t = fileName;
                 editorFilenameAndPosition.u = Integer.parseInt(line);
             } else {
                 builder.append(str);
             }
             previewLinesIndex++;
         }

         resultTextPane.setText(builder.toString());

         try {
             int selector = builder.toString().indexOf(SELECTOR);
             resultTextPane.setCaretPosition(selector);
         } catch (IllegalArgumentException iae) {

         }

         if(query != null) {
             Highlighter highlighter = new Highlighter();
             highlighter.highlightSearch(resultTextPane, query, Color.ORANGE);
         }

         if(searchPreview.size() > 0) {
             previewTextPane.setText(search.getPreview(searchPreview.get(selectedGuiIndex)));
         }

         if(query != null) {
            Highlighter highlighter = new Highlighter();
            highlighter.highlightPreview(previewTextPane, selectedLine, FOREGROUND_COLOR);
         }

         if(isViewLimitReached) {
             resultCountLabel.setText("...");
         } else {
             resultCountLabel.setText(String.valueOf(numLines));
         }
     }
     
     public void close() {
         executor.shutdown();
         search.close();
     }

     public static void main(String[] args) {
         repl();
     }

 }
