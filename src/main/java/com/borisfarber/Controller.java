package com.borisfarber;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.io.*;

import static com.borisfarber.Search.testLoad;

public final class Controller implements DocumentListener {

    private JTextComponent resultTextArea;
    private final JTextArea previewTextArea;
    private final JLabel occurrencesLabel;

    private File currentFile;
    private Search search;

    public Controller(final JTextComponent resultTextArea, final JTextArea previewArea, final JLabel occurrencesLabel) {
        this.resultTextArea = resultTextArea;
        this.previewTextArea = previewArea;
        this.occurrencesLabel = occurrencesLabel;

        search = new Search();
    }

    public void testCrawl() {
        search.crawl(testLoad());
    }

    private void search(String query) {
        search.search(query);
    }

    public String dump() {
        System.out.println(search.getResults());
        System.out.println(search.getPreview(0));
        System.out.println(search.getResultCount());

        return "";
    }

    // TODO event loop
    @Override
    public void insertUpdate(final DocumentEvent evt) {
        // letter
        Document document = evt.getDocument();
        runNewSearch(document);

        updateGUI();
    }

    @Override
    public void removeUpdate(final DocumentEvent evt) {
        // escape
        search.removeUpdate();
        Document document = evt.getDocument();
        runNewSearch(document);

        updateGUI();
    }

    @Override
    public void changedUpdate(final DocumentEvent evt) {
        Document document = evt.getDocument();
        runNewSearch(document);
    }

    private final void runNewSearch(final Document searchQueryDoc) {
        try {
            final String query = searchQueryDoc.getText(0, searchQueryDoc.getLength());

            // TODO 3 chars optimization
            /*
            if (query.length() < 3) {

                if (this.resultTextArea.getText().equals(SHARK_BG)) {
                    this.resultTextArea.setText(SHARK_BG);
                    this.occurrencesLabel.setText("");
                }
                final JTextArea showFileArea = this.previewTextArea;
                final Search search = this.search;

                showFileArea.setText(search.getResultCount());
                return;
            }*/

            search(query);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateGUI() {
        resultTextArea.setText(search.getResults());
        previewTextArea.setText(search.getPreview(0));
        occurrencesLabel.setText(search.getResultCount());
    }

    public final void testCrawl(final File file) {
        if (file == null) {
            return;
        }

        // TODO create here different search types
        this.search = new Search();

        this.currentFile = file;
        if (this.currentFile != null) {
            final File currentFile = this.currentFile;

            if (currentFile.exists()) {
                final Search search = this.search;

                final File currentFile2 = this.currentFile;

                //search.load(currentFile2);
            }
        }
    }

    public final void updateShowFileArea(final String selectedLine) {
       // TODO implement
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

    public final JTextArea getPreviewTextArea() {
        return this.previewTextArea;
    }

    public static void repl() {
        Controller controller = new Controller(new JTextArea(), new JTextArea(), new JLabel());
        controller.testCrawl();

        Reader inreader = new InputStreamReader(System.in);

        try {
            BufferedReader in = new BufferedReader(inreader);
            String str;
            System.out.print(">>>");
            while ((str = in.readLine()) != null) {
                controller.search(str);
                controller.dump();

                System.out.print(">>>");
            }
            in.close();
        } catch (Exception e) {
        }
    }

    public static void main(String[] args) {
       repl();
    }

    //>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    // todo refactor, make i zero on a new crawl
    int i = 0;

    public void upPressed() {

        if(i>0) {
            i--;
        }

        previewTextArea.setText(search.getPreview(i));
    }

    public void downPressed() {

        if(i < search.resultSet.size()-1) {
            i++;
        }
        previewTextArea.setText(search.getPreview(i));
    }
}
