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
package com.borisfarber.instasearch.contollers;

import com.borisfarber.instasearch.models.ResultPresentation;
import com.borisfarber.instasearch.models.search.Search;
import com.borisfarber.instasearch.models.search.SearchFactory;
import com.borisfarber.instasearch.models.text.Background;
import com.borisfarber.instasearch.ui.PreviewHighlighter;
import com.borisfarber.instasearch.ui.ResultsHighlighter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static com.borisfarber.instasearch.contollers.FullFilePreview.fullFilePreview;
import static com.borisfarber.instasearch.ui.InstaSearch.FOREGROUND_COLOR;

public final class Controller implements DocumentListener {
    public static final int UI_VIEW_LIMIT = 1000;

    private final JTextField searchField;
    private final JTextPane resultTextPane;
    private final JTextPane previewTextPane;
    private final JLabel resultCountLabel;
    private final ResultsHighlighter resultsHighlighter;
    private final PreviewHighlighter previewHighlighter;
    private File file;
    private String query;
    private Search search;
    private ResultPresentation resultPresentation;

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
        this.search = SearchFactory.INSTANCE.createMockSearch(this);
        this.resultsHighlighter = new ResultsHighlighter(resultTextPane, Color.BLACK);
        this.previewHighlighter = new PreviewHighlighter();
        this.resultPresentation = new ResultPresentation();

        this.resultTextPane.setText(resultPresentation.getBackground());
    }

    public void onFileOpened(File newFile) {
        if(newFile != null) {
            searchField.setText("");
            resultTextPane.setText(Background.INTRO);
            previewTextPane.setText("");
            resultCountLabel.setText("");

            crawl(newFile);
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
        resultPresentation.increaseSelectedLine();
        updateGUI(ResultsHighlighter.HIGHLIGHT_SPAN.SHORT);
    }

    public void onDownPressed() {
        resultPresentation.decreaseSelectedLine();
        updateGUI(ResultsHighlighter.HIGHLIGHT_SPAN.SHORT);
    }

    public void onMouseSingleClick(String selectedText) {
        resultPresentation.setSelectedLine(selectedText);
        updateGUI(ResultsHighlighter.HIGHLIGHT_SPAN.SHORT);
    }

    public void onMouseDoubleClick(String selectedText) {
        resultPresentation.exportLine(selectedText);
        onEnterPressed();
    }

    public void onMouseScrolled(int from, ResultsHighlighter.HIGHLIGHT_SPAN span) {
        highlightResults(from, span);
    }

    public void onEnterPressed() {
        fullFilePreview(
                search,
                resultPresentation.getExportedFilename(),
                resultPresentation.getExportedLineIndex(),
                previewTasksExecutor,
                previewTextPane,
                file);
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
        this.query = query;
        resultPresentation.resetSelectedLine();

        Runnable runnableTask = () -> search.search(query);
        long waitingTasksCount = searchTasksExecutor.getActiveCount();
        if(waitingTasksCount < 1) {
            searchTasksExecutor.submit(runnableTask);
        }
    }

    public void onCrawlFinish(java.util.List<String> crawlResults) {
        resultPresentation.fillFilenameResults(crawlResults);
        resultPresentation.generateResultView();
        resultTextPane.setText(resultPresentation.getResultView());
        resultTextPane.setCaretPosition(0);
    }

    public void onSearchFinish() {
        resultPresentation.fillSearchResults(search);
        updateGUI(ResultsHighlighter.HIGHLIGHT_SPAN.LONG);
    }

    private void updateGUI(ResultsHighlighter.HIGHLIGHT_SPAN span) {
        resultPresentation.generateResultView();
        resultTextPane.setText(resultPresentation.getResultView());
        resultTextPane.setCaretPosition(0);

        if(resultPresentation.getResultCount() > 0) {
            previewTextPane.setText(search.getPreview(resultPresentation.getSelectedLine()));
            previewTextPane.setCaretPosition(0);
            highlightPreview();
        }

        if(resultPresentation.getResultCount() > UI_VIEW_LIMIT) {
            resultCountLabel.setText("...");
        } else {
            resultCountLabel.setText(
                    String.valueOf(resultPresentation.getResultCount()));
        }

        try {
            int selector = resultPresentation.getSelectionIndex();
            if(selector != -1) {
                resultTextPane.setCaretPosition(selector);
                highlightResults(selector, span);
            }
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
        }
    }

    private void highlightResults(int from, ResultsHighlighter.HIGHLIGHT_SPAN span) {
        if(query != null) {
            resultsHighlighter.highlightSearch(from, span, query);
        }
    }

    public void onUpdatePreview(String result) {
        previewTextPane.setText(result);
        previewTextPane.setCaretPosition(0);
        highlightPreview();
    }

    private void highlightPreview() {
        if(query != null) {
            previewHighlighter.highlightPreview(previewTextPane,
                    ResultPresentation.extractPreviewLine(
                            resultPresentation.getSelectedLine()),
                    FOREGROUND_COLOR);
        }
    }

    public void onClose() {
        searchTasksExecutor.shutdown();
        previewTasksExecutor.shutdown();
        search.close();
        PrivateFolder.INSTANCE.shutdown();
    }
}