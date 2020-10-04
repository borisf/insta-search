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

import com.borisfarber.instasearch.search.MockSearch;
import com.borisfarber.instasearch.search.Search;
import com.borisfarber.instasearch.search.SearchFactory;
import com.borisfarber.instasearch.textblocks.Background;
import com.borisfarber.instasearch.ui.PreviewHighlighter;
import com.borisfarber.instasearch.ui.ResultsHighlighter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static com.borisfarber.instasearch.controllers.FullFilePreview.fullFilePreview;
import static com.borisfarber.instasearch.ui.InstaSearch.FOREGROUND_COLOR;

public final class Controller implements DocumentListener {
    private static final String SELECTOR = "==> ";
    private static final int UI_VIEW_LIMIT = 1000;

    private final JTextField searchField;
    private final JTextPane resultTextPane;
    private final JTextPane previewTextPane;
    private final JLabel resultCountLabel;
    private final ResultsHighlighter resultsHighlighter;
    private final PreviewHighlighter previewHighlighter;
    private File file;
    private String query;
    private Search search;
    private final ArrayList<String> searchResults = new ArrayList<>();
    private int searchResultsCount = 0;
    private final Pair<String, Integer> selectedFilenameAndPosition =
            new Pair<>("test.txt",0);
    private int selectedSearchResultIndex = 0;
    private String selectedLine = "";

    private final ThreadPoolExecutor searchTasksExecutor =
            (ThreadPoolExecutor)Executors.newFixedThreadPool(1);

    private final ThreadPoolExecutor previewTasksExecutor =
            (ThreadPoolExecutor)Executors.newFixedThreadPool(1);

    public Controller(JTextField searchField,
                      JTextPane resultTextPane,
                      JTextPane previewArea,
                      JLabel resultCountLabel) {
        this.searchField = searchField;
        this.resultTextPane = resultTextPane;
        this.previewTextPane = previewArea;
        this.resultCountLabel = resultCountLabel;
        this.search = new MockSearch(this);
        this.resultsHighlighter = new ResultsHighlighter(resultTextPane, Color.BLACK);
        this.previewHighlighter = new PreviewHighlighter();
    }

    // region Open file events
    public void onFileOpened(File newFile) {
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

    public void onFileDragged(File file) {
        searchField.setText("");
        resultTextPane.setText(Background.INTRO);
        previewTextPane.setText("");
        crawl(file);
    }

    private void crawl(final File file) {
        if (file == null || !file.exists()) {
            return;
        }

        this.file = file;
        search = SearchFactory.INSTANCE.createSearch(file, this);
        search.crawl(file);
    }
    // endregion

    // region Input events
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

    public void onUpPressed() {
        if(selectedSearchResultIndex > 0) {
            selectedSearchResultIndex--;
        }

        updateGUI(ResultsHighlighter.HIGHLIGHT_SPAN.SHORT);
    }

    public void onDownPressed() {
        if((selectedSearchResultIndex <
                (Integer.parseInt(search.getResultSetCount()) - 1))
                && selectedSearchResultIndex < (searchResultsCount - 1)) {
            selectedSearchResultIndex++;
        }

        updateGUI(ResultsHighlighter.HIGHLIGHT_SPAN.SHORT);
    }

    public void onMouseSingleClick(String selectedText) {
        int index = searchResults.indexOf((selectedText + "\n"));

        if(index == -1) {
            // hack when the ui screen is small and the selected line
            // is smaller than the result line
            return;
        }

        selectedSearchResultIndex = index;
        updateGUI(ResultsHighlighter.HIGHLIGHT_SPAN.SHORT);
    }

    public void onMouseDoubleClick(String selectedText) {
        String[] parts  = selectedText.split(":");
        String fileName = parts[0];
        String position = parts[1];

        if(fileName.startsWith(SELECTOR)) {
            fileName = fileName.substring(4);
        }

        selectedFilenameAndPosition.t = fileName;
        selectedFilenameAndPosition.u = Integer.parseInt(position);

        onEnterPressed();
    }

    public void onMouseScrolled(int from, ResultsHighlighter.HIGHLIGHT_SPAN span) {
        highlightResults(from, span);
    }

    public void onEnterPressed() {
        fullFilePreview(search, selectedFilenameAndPosition, previewTasksExecutor, previewTextPane, file);
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

    private void search(String query) {
        selectedSearchResultIndex = 0;
        this.query = query;

        Runnable runnableTask = () -> search.search(query);
        long waitingTasksCount = searchTasksExecutor.getActiveCount();
        if(waitingTasksCount < 1) {
            searchTasksExecutor.submit(runnableTask);
        }
    }
    // endregion

    // region Search events
    public void onSearchFinish() {
        int resultCount = 0;
        boolean isViewLimitReached = false;
        LinkedList<Pair<String, Integer>> locations;
        searchResults.clear();
        List<String> rawResults = search.getResults();

        while ((resultCount < rawResults.size()) && !isViewLimitReached) {
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
                    isViewLimitReached = true;
                    break;
                }
            }
        }

        searchResults.sort(search.getResultsSorter());
        searchResultsCount = resultCount;
        updateGUI(ResultsHighlighter.HIGHLIGHT_SPAN.LONG);
    }

    private void updateGUI(ResultsHighlighter.HIGHLIGHT_SPAN span) {
        int previewLinesIndex = 0;
        StringBuilder builder = new StringBuilder();
        for (String str : searchResults) {
            if(previewLinesIndex == selectedSearchResultIndex) {
                builder.append(SELECTOR).append(str);
                String[] parts  = str.split(":");
                String fileName = parts[0];
                String position = parts[1];

                // the text line starts after 2 :s
                this.selectedLine = str.substring(parts[0].length() + parts[1].length() + 2);

                selectedFilenameAndPosition.t = fileName;
                selectedFilenameAndPosition.u = Integer.parseInt(position);
            } else {
                builder.append(str);
            }
            previewLinesIndex++;
        }

        resultTextPane.setText(builder.toString());

        if(searchResults.size() > 0) {
            previewTextPane.setText
                    (search.getPreview(searchResults.get(selectedSearchResultIndex)));
            highlightPreview();
        }

        if(searchResults.size() > UI_VIEW_LIMIT) {
            resultCountLabel.setText("...");
        } else {
            resultCountLabel.setText(String.valueOf(searchResultsCount));
        }

        try {
            int selector = builder.toString().indexOf(SELECTOR);
            if(selector != -1) {
                resultTextPane.setCaretPosition(selector);
                highlightResults(selector, span);

            }
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
        }
    }

    public void onCrawlFinish(String toString) {
        resultTextPane.setText(toString);
    }

    private void highlightResults(int from, ResultsHighlighter.HIGHLIGHT_SPAN span) {
        if(query != null) {
            resultsHighlighter.highlightSearch(from, span, query);
        }
    }

    public void onUpdatePreview(String result) {
        previewTextPane.setText(result);
        highlightPreview();
    }

    private void highlightPreview() {
        if(query != null) {
            previewHighlighter.highlightPreview(previewTextPane, selectedLine, FOREGROUND_COLOR);
        }
    }
    // endregion

    public void onClose() {
        searchTasksExecutor.shutdown();
        previewTasksExecutor.shutdown();
        search.close();
        PrivateFolder.INSTANCE.shutdown();
    }
}