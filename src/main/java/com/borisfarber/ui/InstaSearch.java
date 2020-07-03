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
package com.borisfarber.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import com.borisfarber.controllers.Controller;
import com.borisfarber.controllers.FileTransfer;

import static java.awt.event.KeyEvent.*;

public final class InstaSearch extends JFrame {
    private final Font textFont;
    private JTextField searchField;
    private JTextArea resultTextArea;
    private JTextArea previewArea;
    private JLabel resultCountLabel;
    private Controller controller;

    public InstaSearch() {
        super("ClassyShark Insta Search");
        textFont = new Font("JetBrains Mono", 0, 23);
        buildUI();
        controller = new Controller(resultTextArea, previewArea, resultCountLabel);
        searchField.getDocument().addDocumentListener(this.controller);

        File file = open();

        if(file != null) {
            controller.crawl(file);
        }
    }

    // TODO move to controller
    public final void fileDragged(final File file) {
        final JTextArea showFileArea = this.previewArea;

        showFileArea.setText("");
        final JTextArea resultTextArea = this.resultTextArea;

        resultTextArea.setText(Background.SHARK_BG);
        this.setTitle("ClassySearch - " + file.getName());
        final Controller controller = this.controller;
        if (controller == null) {

        }
        controller.crawl(file);
    }

    private final void buildUI() {
        resultCountLabel = new JLabel("");
        resultTextArea = this.buildResultTextArea();
        JScrollPane showResultsScrolled = new JScrollPane(this.resultTextArea);
        previewArea = this.buildPreviewArea();
        JScrollPane showFileScrolled = new JScrollPane(this.previewArea);

        searchField = this.buildSearchField();
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, 0));

        resultCountLabel.setAlignmentX(0.5f);
        statusPanel.add(resultCountLabel);
        JSplitPane splitPane = new JSplitPane(0, showResultsScrolled, showFileScrolled);
        splitPane.setDividerSize(20);
        splitPane.setBorder(BorderFactory.createLineBorder(Color.ORANGE));
        splitPane.setDividerLocation(400);
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);
        final Container contentPane = this.getContentPane();

        contentPane.setLayout(new BoxLayout(this.getContentPane(), 1));

        searchField.setAlignmentX(0.0f);
        splitPane.setAlignmentX(0.0f);
        statusPanel.setAlignmentX(0.0f);
        this.getContentPane().add(searchField);
        this.getContentPane().add(splitPane);
        this.getContentPane().add(statusPanel);
        final JMenuBar menuBar = new JMenuBar();
        final Font f = this.textFont;
        UIManager.put("Menu.font", f);
        final JMenu menu = new JMenu("File");
        menuBar.add(menu);
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(Box.createHorizontalGlue());
        final JMenuItem openFolderItem = new JMenuItem("Open...");
        openFolderItem.setFont(f);
        openFolderItem.addActionListener(actionEvent -> {
            try {
                File newFile = open();
                if (newFile != null) {
                    resultTextArea.setText(Background.SHARK_BG);
                    previewArea.setText("");
                    searchField.setText("");
                    controller.crawl(newFile);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        menu.add(openFolderItem);
        final JMenuItem closeItem = new JMenuItem("Exit");
        closeItem.setFont(f);
        closeItem.addActionListener(actionEvent -> {
            System.exit(0);
        });
        menu.add(closeItem);
        this.setJMenuBar(menuBar);
        final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();

        final Dimension dim = defaultToolkit.getScreenSize();
        setPreferredSize(new Dimension(1200, 900));
        setLocation(dim.width / 4 - this.getSize().width / 4, dim.height / 2 - this.getSize().height / 2);
        pack();
        setVisible(true);
    }

    private final JTextField buildSearchField() {
        final JTextField result = new JTextField();
        result.setFont(this.textFont);
        result.setBackground(Colors.BACKGROUND_COLOR);
        result.setForeground(Colors.FOREGROUND_COLOR);
        result.setCaretColor(Colors.FOREGROUND_COLOR);
        result.getCaret().setBlinkRate(0);

        // // todo maybe put to controller, same as with doc update, in constructor
        result.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {

            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {

                if (keyEvent.getKeyCode() == VK_RIGHT) {
                    result.setText("");
                    try {
                        File newFile = open();
                        if(newFile != null) {
                            controller.crawl(newFile);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                }

                if (keyEvent.getKeyCode() == VK_UP) {
                    controller.upPressed();
                    return;
                }

                if (keyEvent.getKeyCode() == VK_DOWN) {
                    controller.downPressed();
                    return;
                }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {

            }
        });

        return result;
    }

    private final JTextArea buildResultTextArea() {
        final JTextArea result = new JTextArea(10, 80);
        result.setFont(this.textFont);
        result.setBackground(Colors.BACKGROUND_COLOR);
        result.setForeground(Colors.FOREGROUND_COLOR);
        result.setText(Background.SHARK_BG);
        result.setDragEnabled(true);
        result.setTransferHandler(new FileTransfer(this));
        result.setEditable(false);

        return result;
    }

    private final JTextArea buildPreviewArea() {
        previewArea = new JTextArea(30, 80);
        previewArea.setFont(this.textFont);
        previewArea.setBackground(Colors.BACKGROUND_COLOR);
        previewArea.setForeground(Colors.FOREGROUND_COLOR);
        previewArea.setEditable(false);

        return previewArea;
    }

    public final File open() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setPreferredSize(new Dimension(700,500));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

        final int returnVal = fileChooser.showDialog(this, "Open");
        if (returnVal == 0) {
            return fileChooser.getSelectedFile();
        }

        return null;
    }

    public static void main(final String[] args) {
        try {
            // Set System L&F
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        }
        catch (UnsupportedLookAndFeelException e) {
            // handle exception
        }
        catch (ClassNotFoundException e) {
            // handle exception
        }
        catch (InstantiationException e) {
            // handle exception
        }
        catch (IllegalAccessException e) {
            // handle exception
        }

        InstaSearch classySearch = new InstaSearch();
        classySearch.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}