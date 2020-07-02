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

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.io.*;

import static com.borisfarber.controllers.FileSearch.testLoad;

 public final class Controller implements DocumentListener {

    private JTextComponent resultTextArea;
    private final JTextArea previewTextArea;
    private final JLabel occurrencesLabel;

    private FileSearch search;
    private int selectedGuiIndex = 0;

    public Controller(final JTextComponent resultTextArea,
                      final JTextArea previewArea,
                      final JLabel occurrencesLabel) {
        this.resultTextArea = resultTextArea;
        this.previewTextArea = previewArea;
        this.occurrencesLabel = occurrencesLabel;

        search = new FileSearch();
    }

    public void testCrawl() {
        search.testCrawl(testLoad());
    }

    public final void crawl(final File file) {
        if (file == null || !file.exists()) {
            return;
        }

        search = new FileSearch();
        search.crawl(file);
    }

    private void search(String query) {
        selectedGuiIndex = 0;
        search.search(query);
    }

    public String dump() {
        System.out.println(search.getResults());
        System.out.println(search.getPreview(0));
        System.out.println(search.getResultSetCount());

        return "";
    }

    @Override
    public void insertUpdate(final DocumentEvent evt) {
        // letter
        Document document = evt.getDocument();
        runNewSearch(document);

        updateGUI();
    }

    @Override
    public void removeUpdate(final DocumentEvent evt) {
        // when new folder is loaded, this callback gets called
    }

    @Override
    public void changedUpdate(final DocumentEvent evt) {
        Document document = evt.getDocument();
        runNewSearch(document);
    }

    public void upPressed() {
        if(selectedGuiIndex > 0) {
            selectedGuiIndex--;
        }

        updateGUI();
    }

    public void downPressed() {
        if(selectedGuiIndex < Integer.parseInt(search.getResultSetCount()) - 1) {
            selectedGuiIndex++;
        }

        updateGUI();
    }

    private void updateGUI() {
        // show selector
        int i = 0;
        StringBuilder builder = new StringBuilder();
        for (String res : search.getResultSet()) {
            String resultLine = search.getFileName(res) + " " + res;

            if(i == selectedGuiIndex) {
                builder.append("* " + resultLine);
            } else {
                builder.append(resultLine);
            }
            i++;

            builder.append("\n");
        }
        resultTextArea.setText(builder.toString());
        resultTextArea.setCaretPosition(0);

        // the usual updates
        previewTextArea.setText(search.getPreview(selectedGuiIndex));
        occurrencesLabel.setText(search.getResultSetCount());
    }

    private final void runNewSearch(final Document searchQueryDoc) {
        try {
            final String query = searchQueryDoc.getText(0,
                    searchQueryDoc.getLength());
            search(query);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void repl() {
        Controller controller = new Controller(new JTextArea(),
                new JTextArea(), new JLabel());
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