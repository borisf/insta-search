package com.borisfarber;

import javax.swing.*;
import javax.swing.text.Utilities;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class ClassySharkInstaSearch extends JFrame {
    private final Font textFont;
    private JTextField searchField;
    private JTextArea resultTextArea;
    private JTextArea showFileArea;
    private JLabel occurrencesLabel;
    private Controller controller;

    private static final Color BACKGROUND_COLOR = new Color(7, 54, 66);
    private static final Color FOREGROUND_COLOR = new Color(147, 161, 161);;

    public ClassySharkInstaSearch() {
        super("ClassyShark Insta Search");
        this.textFont = new Font("Monospaced", 0, 23);
        this.buildUI();
        final File file = new File(System.getProperty("user.dir"));
        final Controller controller = this.controller;
        if (controller == null) {

        }
        controller.crawl(file);
    }

    public ClassySharkInstaSearch(final File file) {
        super("ClassyShark Insta Search");
        this.textFont = new Font("Monospaced", 0, 23);
        this.buildUI();
        controller.crawl(file);
    }

    public final void fileDragged(final File file) {

        final JTextArea showFileArea = this.showFileArea;
        if (showFileArea == null) {

        }
        showFileArea.setText("");
        final JTextArea resultTextArea = this.resultTextArea;
        if (resultTextArea == null) {

        }
        resultTextArea.setText(Background.SHARK_BG);
        this.setTitle("ClassySearch - " + file.getName());
        final Controller controller = this.controller;
        if (controller == null) {

        }
        controller.crawl(file);
    }

    private final void buildUI() {
        this.occurrencesLabel = new JLabel("");
        this.resultTextArea = this.buildResultTextArea();
        final JScrollPane showResultsScrolled = new JScrollPane(this.resultTextArea);
        this.showFileArea = this.buildShowFileArea();
        final JScrollPane showFileScrolled = new JScrollPane(this.showFileArea);
        final JTextArea resultTextArea = this.resultTextArea;
        if (resultTextArea == null) {

        }
        final JTextArea resultTextArea2 = resultTextArea;
        final JLabel occurrencesLabel = this.occurrencesLabel;
        if (occurrencesLabel == null) {

        }
        final JTextArea showFileArea = this.showFileArea;
        if (showFileArea == null) {

        }
        this.controller = new Controller(resultTextArea2, occurrencesLabel, showFileArea);
        this.searchField = this.buildSearchField();
        final JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, 0));
        final JLabel occurrencesLabel2 = this.occurrencesLabel;

        if (occurrencesLabel2 == null) {

        }
        occurrencesLabel2.setAlignmentX(0.5f);
        statusPanel.add(this.occurrencesLabel);
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
                showFileArea.setText("");
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
        result.getDocument().addDocumentListener(this.controller);
        result.setFont(this.textFont);
        result.setBackground(ClassySharkInstaSearch.BACKGROUND_COLOR);
        result.setForeground(ClassySharkInstaSearch.FOREGROUND_COLOR);

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

    private final JTextArea buildShowFileArea() {
        this.showFileArea = new JTextArea(30, 80);
        final JTextArea showFileArea = this.showFileArea;
        if (showFileArea == null) {

        }
        showFileArea.setFont(this.textFont);
        final JTextArea showFileArea2 = this.showFileArea;
        if (showFileArea2 == null) {

        }
        showFileArea2.setBackground(ClassySharkInstaSearch.BACKGROUND_COLOR);
        final JTextArea showFileArea3 = this.showFileArea;
        if (showFileArea3 == null) {

        }
        showFileArea3.setForeground(ClassySharkInstaSearch.FOREGROUND_COLOR);
        final JTextArea showFileArea4 = this.showFileArea;
        if (showFileArea4 == null) {

        }
        return showFileArea4;
    }

    private final File open() throws Exception {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setCurrentDirectory(new File("."));
        final Component[] components = fileChooser.getComponents();

        this.setFileChooserFont(components);
        final int returnVal = fileChooser.showDialog(this, "Open");
        if (returnVal == 0) {
            return fileChooser.getSelectedFile();
        }
        return this.downloadedTempTextFile();
    }

    private final File downloadedTempTextFile() throws Exception {
        final URL website = new URL("http://www.gutenberg.org/cache/epub/18362/pg18362.txt");
        final File target = File.createTempFile("tempDict", ".txt");
        final InputStream ll = website.openStream();
        Files.copy(ll, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private final void setFileChooserFont(final Component[] comp) {
        // TODO fill in
    }

    public static void main(final String[] args) {
        final ClassySharkInstaSearch classySearch = (args.length != 0) ? new ClassySharkInstaSearch(new File(args[1])) : new ClassySharkInstaSearch();
        classySearch.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
