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
package com.borisfarber.instasearch.controllers;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.borisfarber.instasearch.formats.Clazz;
import com.borisfarber.instasearch.formats.Dex;
import com.borisfarber.instasearch.search.*;
import com.borisfarber.instasearch.ui.Background;
import com.borisfarber.instasearch.ui.HexPanel;
import com.borisfarber.instasearch.ui.Highlighter;

import static com.borisfarber.instasearch.ui.InstaSearch.FOREGROUND_COLOR;

public final class Controller implements DocumentListener {
    public static final String SELECTOR = "==> ";
    public static final int UI_VIEW_LIMIT = 1000;

    public static final PathMatcher SOURCE_MATCHER =
            FileSystems.getDefault().getPathMatcher("glob:**.{java,kt,md,h,c,cpp,gradle,rs,cs}");

    public static final PathMatcher SOURCE_OR_TEXT_PATH_MATCHER =
            FileSystems.getDefault().getPathMatcher("glob:**.{java,kt,md,h,c,cpp,gradle,rs,txt,cs}");

    public static final PathMatcher CLASS_MATCHER =
            FileSystems.getDefault().getPathMatcher("glob:**.{class}");

    public static final PathMatcher ZIP_MATCHER =
            FileSystems.getDefault().getPathMatcher("glob:**.{zip,jar}");

    public static final PathMatcher APK_MATCHER =
            FileSystems.getDefault().getPathMatcher("glob:**.{apk}");

    public static final PathMatcher
            DEX_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.{dex}");

    private final JTextField searchField;
    public JTextPane resultTextPane;
    public JTextPane previewTextPane;
    private final JLabel resultCountLabel;

    private final ThreadPoolExecutor searchTasksExecutor =
            (ThreadPoolExecutor)Executors.newFixedThreadPool(1);

    private final ThreadPoolExecutor previewTasksExecutor =
            (ThreadPoolExecutor)Executors.newFixedThreadPool(1);

    private String query;
    private Search search;
    private final Pair<String, Integer> editorFilenameAndPosition =
            new Pair<>("test.txt",0);
    private final ArrayList<String> searchResults = new ArrayList<>();
    private int selectedGuiIndex = 0;
    private int numLines = 0;
    private String selectedLine = "";
    private File file;

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

        this.file = file;
        search = createSearch(file);
        search.crawl(file);
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

                crawl(newFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Search createSearch(File newFile) {
        if (newFile.isDirectory()) {
            if(PrivateFolder.isSourceFolder(newFile)) {
                return new FuzzySearch(this);
            } else {
                return new GrepSearch(this);
            }
        } else {
            if (ZIP_MATCHER.matches(Path.of(newFile.toURI()))) {
                return new ZipSearch(newFile, this);
            } else if (APK_MATCHER.matches(Path.of(newFile.toURI()))) {
                return new APKSearch(newFile, this);
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

        if(Controller.SOURCE_OR_TEXT_PATH_MATCHER.matches(selectedPath)) {
            DesktopAdaptor.openFileOnDesktop(selectedPath, editorFilenameAndPosition.u);
        } else if(Controller.CLASS_MATCHER.matches(selectedPath)) {
            previewTasksExecutor.execute(() -> {
                Pair<File, String> result = Clazz.decompile(selectedPath);
                Runnable runnable = () -> {
                    previewTextPane.setText(result.u);
                    DesktopAdaptor.openFileOnDesktop(result.t.toPath(), 0);
                };
                SwingUtilities.invokeLater(runnable);
            });
        } else if(Controller.CLASS_MATCHER.matches(selectedPath)) {
            previewTasksExecutor.execute(() -> {
                Pair<File, String> result = Clazz.decompile(selectedPath);
                Runnable runnable = () -> {
                    previewTextPane.setText(result.u);
                    DesktopAdaptor.openFileOnDesktop(result.t.toPath(), 0);
                };
                SwingUtilities.invokeLater(runnable);
            });
        } else if(Controller.DEX_MATCHER.matches(selectedPath)) {
            previewTasksExecutor.execute(() -> {
                Pair<File, String> result = Dex.decompile(file.toPath());
                Runnable runnable = () -> {
                    previewTextPane.setText(result.u);
                    DesktopAdaptor.openFileOnDesktop(result.t.toPath(), 0);
                };
                SwingUtilities.invokeLater(runnable);
            });
        } else {
            HexPanel.createJFrameWithHexPanel(selectedPath.toFile());
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
        long waitingTasksCount = searchTasksExecutor.getActiveCount();
        if(waitingTasksCount < 1) {
            searchTasksExecutor.submit(runnableTask);
        }
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

            if(rawLine == null) {
                // if the UI is drawing the previous search results
                // while the results are updated
                return;
            }

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

        searchResults.sort(search.getResultsSorter());
        numLines = resultCount;
        onUpdateGUIInternal();
    }

    private void onUpdateGUIInternal() {
        int previewLinesIndex = 0;
        StringBuilder builder = new StringBuilder();
        for (String str : searchResults) {
            if(previewLinesIndex == selectedGuiIndex) {
                builder.append(SELECTOR).append(str);
                String[] parts  = str.split(":");
                String fileName = parts[0];
                String position = parts[1];

                // the text line starts after 2 :s
                this.selectedLine = str.substring(parts[0].length() + parts[1].length() + 2);

                editorFilenameAndPosition.t = fileName;
                editorFilenameAndPosition.u = Integer.parseInt(position);
            } else {
                builder.append(str);
            }
            previewLinesIndex++;
        }

        resultTextPane.setText(builder.toString());

        if(searchResults.size() > 0) {
            previewTextPane.setText
                    (search.getPreview(searchResults.get(selectedGuiIndex)));
            highlightPreview();
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

                highlightResults(selector);
            }
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
        }
    }

    public void highlightResults(int selector) {
        if(query != null) {
            Highlighter highlighter = new Highlighter();

            // TODO uncomment, still stuck with results
            //highlighter.highlightSearch(resultTextPane, selector, query, Color.ORANGE);
        }
    }

    public void highlightPreview() {
        if(query != null) {
            Highlighter highlighter = new Highlighter();
            highlighter.highlightPreview(previewTextPane, selectedLine, FOREGROUND_COLOR);
        }
    }

    public void close() {
        searchTasksExecutor.shutdown();
        search.close();

        previewTasksExecutor.shutdown();
    }
}