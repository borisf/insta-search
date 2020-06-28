package com.borisfarber;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.borisfarber.Background.SHARK_BG;

public final class Controller implements DocumentListener
{
    private File currentFile;
    private Search search;

    private JTextComponent resultTextArea;
    private final JLabel occurrencesLabel;
    private final JTextArea showFileArea;

    private final boolean isDirectory() {
        if (this.currentFile != null) {

            if (this.currentFile.exists()) {

                return currentFile.isDirectory();
            }

            return false;
        }
        return false;
    }

    private final String getAbsolutePath() {
        if (this.currentFile != null) {
            final File currentFile = this.currentFile;

            if (currentFile.exists()) {
                 return this.currentFile.getAbsolutePath();
            }
        }
        return "";
    }

    public final void crawl(final File file) {
        if (file == null) {
            return;
        }

        // TODO create here different search types
        this.search = new Search(this);

        this.currentFile = file;
        if (this.currentFile != null) {
            final File currentFile = this.currentFile;

            if (currentFile.exists()) {
                final Search search = this.search;

                final File currentFile2 = this.currentFile;

                search.load(currentFile2);
            }
        }
    }

    public final void reCrawl() {
        if (this.currentFile != null) {
            final File currentFile = this.currentFile;

            if (currentFile.exists()) {
                final File currentFile2 = this.currentFile;

                this.crawl(currentFile2);
            }
        }
    }

    @Override
    public void insertUpdate(final DocumentEvent evt) {

        final Document document = evt.getDocument();

        this.runNewSearch(document);
    }

    @Override
    public void removeUpdate(final DocumentEvent evt) {
        final Search search = this.search;

        search.removeUpdate();
        final Document document = evt.getDocument();

        this.runNewSearch(document);
    }

    @Override
    public void changedUpdate(final DocumentEvent evt) {
        final Document document = evt.getDocument();
        this.runNewSearch(document);
    }

    private final Map<String, String> filenamesToPaths() {
        final Search search = this.search;

        return search.filenamesToPaths();
    }

    private final void runNewSearch(final Document searchQueryDoc) {
        try {
            final String query = searchQueryDoc.getText(0, searchQueryDoc.getLength());
            if (query.length() < 3) {

                if (this.resultTextArea.getText().equals(SHARK_BG)) {
                    this.resultTextArea.setText(SHARK_BG);
                    this.occurrencesLabel.setText("");
                }
                final JTextArea showFileArea = this.showFileArea;
                final Search search = this.search;

                showFileArea.setText(search.status());
                return;
            }
            final Search search2 = this.search;

            final String value = query;

            search2.search(value);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public final void updateShowFileArea(final String selectedLine) {
       // TODO implement
    }

    private final int getLineNumber(final String selectedLine) {
       // TODO implement

        return 10;
    }

    private final int countFileCharacters(final String filename, final int tillLine) throws IOException {
       return 0;
    }

    public final JTextComponent getResultTextArea() {
        return this.resultTextArea;
    }

    public final void setResultTextArea(final JTextComponent jt) {

        this.resultTextArea = jt;
    }

    public final JLabel getOccurrencesLabel() {
        return this.occurrencesLabel;
    }

    public final JTextArea getShowFileArea() {
        return this.showFileArea;
    }

    public Controller(final JTextComponent resultTextArea, final JLabel occurrencesLabel, final JTextArea showFileArea) {
        this.resultTextArea = resultTextArea;
        this.occurrencesLabel = occurrencesLabel;
        this.showFileArea = showFileArea;
    }
}
