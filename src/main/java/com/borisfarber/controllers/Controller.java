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
import com.borisfarber.search.ZipSearch;
import com.borisfarber.search.GrepSearch;
import com.borisfarber.search.MockSearch;
import com.borisfarber.search.Search;
import com.borisfarber.ui.Background;
import com.borisfarber.ui.HexPanel;
import com.borisfarber.ui.Highlighter;
import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.PlainTextOutput;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
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
    private static final Comparator<String> RESULTS_SORTER = new SearchResultsSorter();

    private final JTextField searchField;
    public JTextPane resultTextPane;
    public JTextPane previewTextPane;
    private final JLabel resultCountLabel;

    private ThreadPoolExecutor executor =
            (ThreadPoolExecutor)Executors.newFixedThreadPool(1);
    private String query;
    private Search search;
    private Pair<String, Integer> editorFilenameAndPosition =
            new Pair<>("test.txt",0);
    private ArrayList<String> searchResults = new ArrayList<>();
    private int selectedGuiIndex = 0;
    private int numLines = 0;
    private String selectedLine = "";

    public Controller(JTextField searchField,
                      JTextPane resultTextPane,
                      JTextPane previewArea,
                      JLabel resultCountLabel) {
        this.searchField = searchField;
        this.resultTextPane = resultTextPane;
        this.previewTextPane = previewArea;
        this.resultCountLabel = resultCountLabel;

        search = new MockSearch(this);
    }

    public void crawl(final File file) {
        if (file == null || !file.exists()) {
            return;
        }

        search = createSearch(file);
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

                search.emptyQuery();
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
                searchField.setText("");
                resultTextPane.setText(Background.INTRO);
                previewTextPane.setText("");
                resultCountLabel.setText("");

                search = createSearch(newFile);
                crawl(newFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    private Search createSearch(File newFile) {
        if (newFile.isDirectory()) {
            //return new FuzzySearch(this);
            return new GrepSearch(this);
        } else {
            if (newFile.getName().endsWith("apk") || newFile.getName().endsWith("zip")
                    || newFile.getName().endsWith("jar")) {
                return new ZipSearch(newFile, this);
            } else {
                return new GrepSearch(this);
            }
        }
    }

    public void upPressed() {
        if(selectedGuiIndex > 0) {
            selectedGuiIndex--;
        }

        onUpdateGUIInternal();
    }

    public void downPressed() {
        if((selectedGuiIndex < (Integer.parseInt(search.getResultSetCount()) - 1))
                && selectedGuiIndex < (numLines - 1)) {
            selectedGuiIndex++;
        }

        onUpdateGUIInternal();
    }

    public void enterPressed() {
        if(search.getPathPerFileName(editorFilenameAndPosition.t) == null) {
            // garbage files
            return;
        }

        Path selectedPath = search.getPathPerFileName(editorFilenameAndPosition.t);

        try {
            String command;

            if(PrivateFolder.INSTANCE.SOURCE_MATCHER.matches(selectedPath)) {
                command = "nvim +\"set number\" +"
                        + Integer.parseInt(String.valueOf(editorFilenameAndPosition.u)) +
                        " " + selectedPath.toString();
                Terminal.executeInLinux(command);
            } else if(PrivateFolder.INSTANCE.CLASS_MATCHER.matches(selectedPath)) {
                String fileNameWithoutExt =
                        new File(selectedPath.toString()).
                                getName().replaceFirst("[.][^.]+$", "");
                String ext = "java";
                StringWriter writer = new StringWriter();

                try {
                    PlainTextOutput pto = new PlainTextOutput(writer);
                    Decompiler.decompile(selectedPath.toString(), pto);
                } finally {
                    writer.flush();
                }

                String content = writer.toString();
                previewTextPane.setText(content);

                File javaFile = PrivateFolder.INSTANCE.getTempFile(fileNameWithoutExt, ext);
                try (PrintWriter out = new PrintWriter(javaFile)) {
                    out.println(content);
                }

                command = "nvim " + javaFile.getAbsolutePath();
                Terminal.executeInLinux(command);
            } else {
                HexPanel.createJFrameWithHexPanel(selectedPath.toFile());
            }
        } catch (Exception e) {
            // follow up on various OSs where nvim not configured
            // Desktop desktop = Desktop.getDesktop();
            // desktop.open(new File(selectedPath.toString())); --- will not work with zip
            e.printStackTrace();
        }
    }

    public void onFileDragged(File file) {
        searchField.setText("");
        resultTextPane.setText(Background.INTRO);
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
        int resultCount = 0;
        boolean isViewLimit = false;
        LinkedList<Pair<String, Integer>> locations;
        searchResults.clear();

        List<String> rawResults = search.getResults();
        while ((resultCount < rawResults.size()) && !isViewLimit) {
            String rawLine = rawResults.get(resultCount);
            locations = search.getFileNameAndPosition(rawLine);

            for(Pair<String, Integer> location : locations) {
                String result = location.t + ":" + location.u +":"
                        + rawLine;

                if(!rawLine.endsWith("\n")) {
                    // optimization GrepSearch lines come with \n
                    // don't want to change the logic there for performance
                    result += "\n";
                }

                searchResults.add(result);
                resultCount++;

                if(resultCount >= UI_VIEW_LIMIT) {
                    isViewLimit = true;
                    break;
                }
            }
        }

        searchResults.sort(RESULTS_SORTER);
        numLines = resultCount;

        onUpdateGUIInternal();
    }

    private void onUpdateGUIInternal() {
        int previewLinesIndex = 0;
        StringBuilder builder = new StringBuilder();
        for (String str : searchResults) {
            if(previewLinesIndex == selectedGuiIndex) {
                builder.append(SELECTOR + str);
                String[] parts  = str.split(":");
                String fileName = parts[0];
                String line = parts[1];
                this.selectedLine = parts[2];

                editorFilenameAndPosition.t = fileName;
                editorFilenameAndPosition.u = Integer.parseInt(line);
            } else {
                builder.append(str);
            }
            previewLinesIndex++;
        }

        resultTextPane.setText(builder.toString());

        if(searchResults.size() > 0) {
            previewTextPane.setText
                    (search.getPreview(searchResults.get(selectedGuiIndex)));
        }

        if(searchResults.size() > UI_VIEW_LIMIT) {
            resultCountLabel.setText("...");
        } else {
            resultCountLabel.setText(String.valueOf(numLines));
        }

        try {
            int selector = builder.toString().indexOf(SELECTOR);
            if(selector != -1) {
                resultTextPane.setCaretPosition(selector);
            }
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
        }

        if(query != null) {
            Highlighter highlighter = new Highlighter();
            highlighter.highlightSearch(resultTextPane, query, Color.ORANGE);
        }
    }

    public void close() {
        executor.shutdown();
        search.close();
    }

    public void highlightPreview() {
        if(query != null) {
            Highlighter highlighter = new Highlighter();
            highlighter.highlightPreview(previewTextPane, selectedLine, FOREGROUND_COLOR);
        }
    }

    public static void main(String[] args) {
        repl();
    }
}
