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
package com.borisfarber.instasearch.ui;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;

import com.borisfarber.instasearch.controllers.BuildVersion;
import com.borisfarber.instasearch.controllers.Controller;
import com.borisfarber.instasearch.controllers.FileTransfer;

import static java.awt.event.KeyEvent.*;

public final class InstaSearch extends JFrame {
    private final Font textFont;
    private JTextField searchField;
    private JTextPane resultTextPane;
    private JTextPane previewTextPane;
    private JLabel resultCountLabel;
    private final Controller controller;

    private final JPopupMenu copyPopup = new JPopupMenu();
    private final CopyPopupListener popupListener = new CopyPopupListener();

    public static final Color BACKGROUND_COLOR = new Color(0x00, 0x2b, 0x36);
    public static final Color FOREGROUND_COLOR = new Color(0x83, 0x94, 0x96);

    public InstaSearch(String file) {
        this();
        controller.fileOpened(new File(file));
    }

    public InstaSearch() {
        super("ClassyShark Insta Search");
        textFont = new Font("JetBrains Mono", Font.PLAIN, 23);
        buildUI();
        controller =
                new Controller(searchField, resultTextPane,
                        previewTextPane, resultCountLabel);
        searchField.getDocument().addDocumentListener(this.controller);
    }

    public final void onFileDragged(final File file) {
        setTitle("ClassySearch - " + file.getName());
        controller.onFileDragged(file);
    }

    private void buildUI() {
        searchField = buildSearchField();
        searchField.setAlignmentX(0.0f);
        searchField.setMaximumSize(new Dimension(1400, 30));

        resultTextPane = buildResultTextPane();
        JScrollPane showResultsScrolled = new JScrollPane(resultTextPane);

        showResultsScrolled.addMouseWheelListener(new MouseWheelListener() {
            int currentAnchor = 0;

            @Override
            public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
                int notches = mouseWheelEvent.getWheelRotation();
                currentAnchor +=notches;
                controller.highlightResults(currentAnchor * 1000);
            }
        });

        // copy paste
        resultTextPane.add(new JMenuItem(new DefaultEditorKit.CopyAction()));
        HexPanel.CopyPopupListener popupListenerHex =
                new HexPanel.CopyPopupListener(resultTextPane, copyPopup);
        resultTextPane.addMouseListener(popupListenerHex);

        previewTextPane = buildPreviewTextPane();
        JScrollPane showFileScrolled = new JScrollPane(previewTextPane);

        resultCountLabel = new JLabel("...");
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
        resultCountLabel.setAlignmentX(0.5f);
        statusPanel.add(resultCountLabel);
        statusPanel.setAlignmentX(0.0f);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, showResultsScrolled, showFileScrolled);
        splitPane.setDividerSize(20);
        splitPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        splitPane.setDividerLocation(400);
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);
        splitPane.setAlignmentX(0.0f);

        getContentPane().add(searchField);
        getContentPane().add(splitPane);
        getContentPane().add(statusPanel);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        JMenuBar menuBar = new JMenuBar();
        UIManager.put("Menu.font", textFont);
        JMenu menu = new JMenu("File");
        menuBar.add(menu);
        menuBar.add(Box.createHorizontalGlue());
        JMenuItem openFolderItem = new JMenuItem("Open...");
        openFolderItem.setFont(textFont);
        openFolderItem.addActionListener(actionEvent -> {
            try {
                File newFile = open();
                controller.fileOpened(newFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        menu.add(openFolderItem);

        final JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.setFont(textFont);
        aboutItem.addActionListener(
                actionEvent -> JOptionPane.showMessageDialog(this,
                        "Classy Shark Insta Search version " +
                BuildVersion.getBuildVersion()));
        menu.add(aboutItem);

        final JMenuItem closeItem = new JMenuItem("Exit");
        closeItem.setFont(textFont);
        closeItem.addActionListener(actionEvent -> {
            controller.close();
            System.exit(0);
        });
        menu.add(closeItem);
        setJMenuBar(menuBar);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
               controller.close();
               System.exit(0);
            }
        });

        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        Dimension dim = defaultToolkit.getScreenSize();
        setPreferredSize(new Dimension(1200, 900));
        setLocation(dim.width / 6 - this.getSize().width / 4, dim.height / 2 - this.getSize().height / 2);
        pack();
        setVisible(true);
    }

    private JTextField buildSearchField() {
        final JTextField result = new JTextField();
        result.setFont(textFont);
        result.setBackground(BACKGROUND_COLOR);
        result.setForeground(FOREGROUND_COLOR);
        result.setCaretColor(FOREGROUND_COLOR);
        result.getCaret().setBlinkRate(0);

        result.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {

            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == VK_UP) {
                    controller.upPressed();
                    return;
                }

                if (keyEvent.getKeyCode() == VK_DOWN) {
                    controller.downPressed();
                    return;
                }

                if (keyEvent.getKeyCode() == VK_ENTER) {
                    controller.enterPressed();
                    return;
                }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {

            }
        });

        return result;
    }

    private JTextPane buildResultTextPane() {
        final JTextPane result = new JTextPane();
        result.setFont(textFont);
        result.setBackground(BACKGROUND_COLOR);
        result.setForeground(FOREGROUND_COLOR);
        result.setText(Background.INTRO);
        result.setDragEnabled(true);
        result.setTransferHandler(new FileTransfer(this));
        result.setEditable(false);

        return result;
    }

    private JTextPane buildPreviewTextPane() {
        final JTextPane result = new JTextPane();
        result.setFont(textFont);
        result.setBackground(BACKGROUND_COLOR);
        result.setForeground(FOREGROUND_COLOR);
        result.setDragEnabled(true);
        result.setTransferHandler(new FileTransfer(this));
        result.setEditable(false);

        copyPopup.add(new JMenuItem(new DefaultEditorKit.CopyAction()));
        result.addMouseListener(popupListener);

        return result;
    }

    private File open() {
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

    class CopyPopupListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                copyPopup.show(e.getComponent(),
                        e.getX(), e.getY());

                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(previewTextPane.getSelectedText()),
                        null);
            }
        }
    }

    private static boolean isBinarySupported(String filePath) {
        return Controller.ZIP_MATCHER.matches(new File(filePath).toPath());
    }

    private static boolean isTextSupported(String filePath) {
        return Controller.SOURCE_OR_TEXT_PATH_MATCHER.matches(new File(filePath).toPath());
    }

    public static void main(final String[] args) {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
        }

        InstaSearch classySearch;
        if (args.length == 0) {
            classySearch = new InstaSearch(System.getProperty("user.dir"));
            classySearch.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        } else {
            if (isBinarySupported(args[0]) || isTextSupported(args[0])) {
                classySearch = new InstaSearch(args[0]);
                classySearch.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            } else {
                HexPanel.createJFrameWithHexPanel(new File(args[0]));
            }
        }
    }
}