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

import com.borisfarber.instasearch.models.ResultModel;
import com.borisfarber.instasearch.models.search.Search;
import com.borisfarber.instasearch.models.search.SearchFactory;
import com.borisfarber.instasearch.ui.PreviewHighlighter;
import com.borisfarber.instasearch.ui.ResultsHighlighter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.borisfarber.instasearch.contollers.FileView.viewFile;
import static com.borisfarber.instasearch.ui.ColorScheme.FOREGROUND_COLOR;
import static com.borisfarber.instasearch.ui.ColorScheme.RESULTS_HIGHLIGHT_COLOR;
import static com.borisfarber.instasearch.ui.InstaSearch.*;

public final class Mediator implements DocumentListener {
    private final JTextField searchField;
    private final JTextPane resultTextPane;
    private final JTextPane previewTextPane;
    private final JLabel resultCountLabel;
    private final ResultsHighlighter resultsHighlighter;
    private final PreviewHighlighter previewHighlighter;
    private String query;
    private Search search;
    private final ResultModel resultModel;
    private String searchMode;
    private File currentFile;

    private final ThreadPoolExecutor searchExecutor =
            (ThreadPoolExecutor)Executors.newFixedThreadPool(1);

    private final ThreadPoolExecutor previewExecutor =
            (ThreadPoolExecutor)Executors.newFixedThreadPool(1);

    private ScheduledExecutorService crawlAnimationExecutor =
            Executors.newScheduledThreadPool(1);

    public Mediator(JTextField searchField,
                    JTextPane resultTextPane,
                    JTextPane previewArea,
                    JLabel resultCountLabel,
                    String searchMode) {
        this.searchField = searchField;
        this.resultTextPane = resultTextPane;
        this.previewTextPane = previewArea;
        this.resultCountLabel = resultCountLabel;
        this.search = SearchFactory.INSTANCE.createMockSearch(this);
        this.resultsHighlighter = new ResultsHighlighter(resultTextPane, RESULTS_HIGHLIGHT_COLOR);
        this.previewHighlighter = new PreviewHighlighter();
        this.resultModel = new ResultModel();
        this.searchMode = searchMode;

        this.resultTextPane.setText(resultModel.getBackground());
    }

    public void onFileOpened(File newFile) {
        crawl(newFile);
    }

    public void updateSearchMode(String searchMode) {
        this.searchMode = searchMode;
        crawl(currentFile);
    }

    private void crawl(final File file) {
        if (file == null || !file.exists()) {
            return;
        }

        searchField.setText("");
        previewTextPane.setText("");
        resultCountLabel.setText("...");
        currentFile = file;
        crawlAnimationExecutor = Executors.newScheduledThreadPool(1);
        crawlAnimationExecutor.scheduleAtFixedRate(
                new CrawlAnimation(this, file.getAbsolutePath()), 0, 200, TimeUnit.MILLISECONDS);

        var crawlWorker = new SwingWorker() {
            // crawl requests either come from either main thread
            // or from EDT when a file is opened from the toolbar
            @Override
            protected String doInBackground() {
                search = SearchFactory.INSTANCE.createSearch(Mediator.this,
                        file, searchMode);
                search.crawl(file);
                return "";
            }
        };

        crawlWorker.execute();
    }

    public void onCrawlUpdate(String text) {
        resultTextPane.setText(text);
    }

    public void onCrawlFinish(java.util.List<String> crawlResults) {
        crawlAnimationExecutor.shutdownNow();

        resultModel.fillFilenameResults(crawlResults);
        resultModel.generateResultView();
        resultTextPane.setText(resultModel.getResultView());
        resultTextPane.setCaretPosition(0);
        previewTextPane.setText("");
        resultCountLabel.setText(""+ crawlResults.size());
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

            if(query.length() > 0) {
                search(query);
            } else {
                // transition from 1 letter to 0 letters
                // in the query
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
        resultModel.increaseSelectedLine();
        updateGUI(ResultsHighlighter.HIGHLIGHT_SPAN.SHORT);
    }

    public void onDownPressed() {
        resultModel.decreaseSelectedLine();
        updateGUI(ResultsHighlighter.HIGHLIGHT_SPAN.SHORT);
    }

    public void onMouseSingleClick(String selectedText) {
        resultModel.setSelectedLine(selectedText);
        updateGUI(ResultsHighlighter.HIGHLIGHT_SPAN.SHORT);
    }

    public void onMouseDoubleClick(String selectedText) {
        resultModel.exportLine(selectedText);
        onEnterPressed();
    }

    public void onMouseScrolled(int from, ResultsHighlighter.HIGHLIGHT_SPAN span) {
        highlightResults(from, span);
    }

    public void onEnterPressed() {
        viewFile(
                search,
                resultModel.getExportedFilename(),
                resultModel.getExportedLineIndex(),
                previewExecutor,
                previewTextPane,
                currentFile);
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
        resultModel.resetSelectedLine();

        Runnable runnableTask = () -> search.search(query);
        long waitingTasksCount = searchExecutor.getActiveCount();
        if(waitingTasksCount < 1) {
            searchExecutor.submit(runnableTask);
        }
    }

    public void onSearchFinish() {
        resultModel.fillSearchResults(search);
        updateGUI(ResultsHighlighter.HIGHLIGHT_SPAN.LONG);
    }

    public File getCurrentFile() {
        return currentFile;
    }

    private void updateGUI(ResultsHighlighter.HIGHLIGHT_SPAN span) {
        resultModel.generateResultView();
        resultTextPane.setText(resultModel.getResultView());
        resultTextPane.setCaretPosition(0);

        if(resultModel.getResultCount() > 0) {
            previewTextPane.setText(search.getPreview(resultModel.getSelectedLine()));
            previewTextPane.setCaretPosition(0);
            highlightPreview();
        }

        if(resultModel.getResultCount() > UI_VIEW_LIMIT) {
            resultCountLabel.setText("...");
        } else {
            resultCountLabel.setText(
                    String.valueOf(resultModel.getResultCount()));
        }

        try {
            int selector = resultModel.getSelectionIndex();
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
            resultsHighlighter.highlight(from, span, query);
        }
    }

    public void onUpdatePreview(String result) {
        previewTextPane.setText(result);
        previewTextPane.setCaretPosition(0);
        highlightPreview();
    }

    private void highlightPreview() {
        if(query != null) {
            previewHighlighter.highlight(previewTextPane,
                    ResultModel.extractPreviewLine(
                            resultModel.getSelectedLine()), FOREGROUND_COLOR);
        }
    }

    public void onClose() {
        searchExecutor.shutdown();
        previewExecutor.shutdown();
        search.close();
        PrivateFolder.INSTANCE.shutdown();
    }
}