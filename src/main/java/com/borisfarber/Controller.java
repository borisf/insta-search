package com.borisfarber;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.io.*;
import java.util.ArrayList;

import static com.borisfarber.Search.testLoad;

public final class Controller implements DocumentListener {

    private JTextComponent resultTextArea;
    private final JTextArea previewTextArea;
    private final JLabel occurrencesLabel;

    private File currentFile;
    private Search search;
    int selectedGuiIndex = 0;

    public Controller(final JTextComponent resultTextArea, final JTextArea previewArea, final JLabel occurrencesLabel) {
        this.resultTextArea = resultTextArea;
        this.previewTextArea = previewArea;
        this.occurrencesLabel = occurrencesLabel;

        search = new Search();
    }

    public void testCrawl() {
        search.crawl(testLoad());
    }

    public final void crawl(final File file) {
        if (file == null) {
            return;
        }

        if (file.exists()) {
            // TODO add optimization for search if needed
            search = new Search();
            try {
                ArrayList<String> a = Search.folderToLines(file);
                search.crawl(a);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // TODO bug with

        System.out.print("here");
    }

    private void search(String query) {
        selectedGuiIndex = 0;
        search.search(query);
    }

    public String dump() {
        System.out.println(search.getResults());
        System.out.println(search.getPreview(0));
        System.out.println(search.getResultCount());

        return "";
    }

    // TODO the following methods are event loop, maybe refactor
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

    public void upPressed() {

        if(selectedGuiIndex >0) {
            selectedGuiIndex--;
        }

        previewTextArea.setText(search.getPreview(selectedGuiIndex));
    }

    public void downPressed() {

        if(selectedGuiIndex < search.getResultSet().size()-1) {
            selectedGuiIndex++;
        }
        previewTextArea.setText(search.getPreview(selectedGuiIndex));
    }

    public void updateShowFileArea(String selectedLine) {
        // TODO implement if needed on selected mouse line
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
        previewTextArea.setText(search.getPreview(selectedGuiIndex));
        occurrencesLabel.setText(search.getResultCount());
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
}