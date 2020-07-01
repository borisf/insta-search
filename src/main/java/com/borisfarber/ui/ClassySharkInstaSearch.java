package com.borisfarber.ui;

import com.borisfarber.controllers.Controller;
import com.borisfarber.controllers.FileTransferHandler;

import javax.swing.*;
import javax.swing.text.Utilities;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_UP;

// todo convert this class to builder
public final class ClassySharkInstaSearch extends JFrame {
    private final Font textFont;
    private JTextField searchField;
    private JTextArea resultTextArea;
    private JTextArea previewArea;
    private JLabel resultCountLabel;
    private Controller controller;

    private static final Color BACKGROUND_COLOR = new Color(7, 54, 66);
    private static final Color FOREGROUND_COLOR = new Color(147, 161, 161);

    public ClassySharkInstaSearch() {
        super("ClassyShark Insta Search");
        textFont = new Font("JetBrains Mono", 0, 23);
        buildUI();
        controller = new Controller(resultTextArea, previewArea, resultCountLabel);
        searchField.getDocument().addDocumentListener(this.controller);
        controller.testCrawl();
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
        this.resultCountLabel = new JLabel("");
        this.resultTextArea = this.buildResultTextArea();
        final JScrollPane showResultsScrolled = new JScrollPane(this.resultTextArea);
        this.previewArea = this.buildPreviewArea();
        final JScrollPane showFileScrolled = new JScrollPane(this.previewArea);
        final JTextArea resultTextArea = this.resultTextArea;

        this.searchField = this.buildSearchField();
        final JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, 0));
        final JLabel occurrencesLabel2 = this.resultCountLabel;

        occurrencesLabel2.setAlignmentX(0.5f);
        statusPanel.add(this.resultCountLabel);
        final JSplitPane splitPane = new JSplitPane(0, showResultsScrolled, showFileScrolled);
        splitPane.setResizeWeight(0.5);
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);
        final Container contentPane = this.getContentPane();

        contentPane.setLayout(new BoxLayout(this.getContentPane(), 1));
        final JTextField searchField = this.searchField;
        if (searchField == null) {

        }
        searchField.setAlignmentX(0.0f);
        splitPane.setAlignmentX(0.0f);
        statusPanel.setAlignmentX(0.0f);
        this.getContentPane().add(this.searchField);
        this.getContentPane().add(splitPane);
        this.getContentPane().add(statusPanel);
        final JMenuBar menuBar = new JMenuBar();
        final Font f = this.textFont;
        UIManager.put("Menu.font", f);
        final JMenu menu = new JMenu("File");
        menuBar.add(menu);
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(Box.createHorizontalGlue());
        final JMenuItem openFolderItem = new JMenuItem("Open Folder/File");
        openFolderItem.setFont(f);
        openFolderItem.addActionListener(actionEvent -> {

            try {
                File currentFile = open();
                resultTextArea.setText(Background.SHARK_BG);
                previewArea.setText("");
                searchField.setText("");
                controller.crawl(currentFile);
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
        this.setLocation(dim.width / 4 - this.getSize().width / 4, dim.height / 2 - this.getSize().height / 2);
        this.pack();
        this.setVisible(true);
    }

    private final JTextField buildSearchField() {
        final JTextField result = new JTextField();
        result.setFont(this.textFont);
        result.setBackground(ClassySharkInstaSearch.BACKGROUND_COLOR);
        result.setForeground(ClassySharkInstaSearch.FOREGROUND_COLOR);

        // // todo maybe put to controller, same as with doc update, in constructor
        result.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {

            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {

                if (keyEvent.getKeyCode() == 39) {
                    result.setText("");
                    try {
                        controller.crawl(open());
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
        result.setBackground(ClassySharkInstaSearch.BACKGROUND_COLOR);
        result.setForeground(ClassySharkInstaSearch.FOREGROUND_COLOR);
        result.setText(Background.SHARK_BG);
        result.setDragEnabled(true);
        result.setTransferHandler(new FileTransferHandler(this));
        result.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {

                // TODO not sure I need it, can do with arrows

                if (mouseEvent.getButton() != MouseEvent.BUTTON1) {
                    return;
                }

                if (mouseEvent.getClickCount() != 2) {
                    return;
                }

                try {
                    int offset = resultTextArea.viewToModel(mouseEvent.getPoint());
                    int rowStart = Utilities.getRowStart(resultTextArea, offset);
                    int rowEnd = Utilities.getRowEnd(resultTextArea, offset);
                    String selectedLine = resultTextArea.getText().substring(rowStart, rowEnd);

                    controller.updateShowFileArea(selectedLine);

                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {

            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {

            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {

            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {

            }
        });
        return result;
    }

    private final JTextArea buildPreviewArea() {
        this.previewArea = new JTextArea(30, 80);
        final JTextArea showFileArea = this.previewArea;
        if (showFileArea == null) {

        }
        showFileArea.setFont(this.textFont);
        final JTextArea showFileArea2 = this.previewArea;
        if (showFileArea2 == null) {

        }
        showFileArea2.setBackground(ClassySharkInstaSearch.BACKGROUND_COLOR);
        final JTextArea showFileArea3 = this.previewArea;
        if (showFileArea3 == null) {

        }
        showFileArea3.setForeground(ClassySharkInstaSearch.FOREGROUND_COLOR);
        final JTextArea showFileArea4 = this.previewArea;
        if (showFileArea4 == null) {

        }
        return showFileArea4;
    }

    public final File open() throws Exception {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setCurrentDirectory(new File("."));

        final int returnVal = fileChooser.showDialog(this, "Open");
        if (returnVal == 0) {
            return fileChooser.getSelectedFile();
        }

        return null;
    }

    public static void main(final String[] args) {
        final ClassySharkInstaSearch classySearch = new ClassySharkInstaSearch();
        classySearch.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}