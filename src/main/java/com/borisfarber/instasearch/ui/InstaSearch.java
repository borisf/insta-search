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

 import com.borisfarber.instasearch.contollers.Controller;
 import com.borisfarber.instasearch.contollers.PathMatchers;

 import javax.swing.*;
 import javax.swing.text.BadLocationException;
 import javax.swing.text.DefaultEditorKit;
 import java.awt.*;
 import java.awt.event.*;
 import java.io.File;

 import static java.awt.event.KeyEvent.*;

 public final class InstaSearch extends JFrame {
     private final Font textFont;
     private JTextField searchField;

     private JTextPane resultTextPane;
     private JTextPane previewTextPane;
     private JLabel resultCountLabel;
     private final JPopupMenu copyPopup = new JPopupMenu();
     private final Controller controller;

     public static final Color BACKGROUND_COLOR = new Color(0x00, 0x2b, 0x36);
     public static final Color FOREGROUND_COLOR = new Color(0x83, 0x94, 0x96);
     public static final Color RESULT_HIGHLIGHT_COLOR = new Color(0x9b,0, 0x9b);

     public InstaSearch(String file) {
         this();
         controller.onFileOpened(new File(file));
     }

     public InstaSearch() {
         super("ClassyShark Insta Search");
         textFont = new Font("JetBrains Mono", Font.PLAIN, 23);
         buildUI();
         controller =
                 new Controller(searchField, resultTextPane,
                         previewTextPane, resultCountLabel);
         searchField.getDocument().addDocumentListener(this.controller);
         controller.onFileOpened(openFile());
     }

     public final void onFileDragged(final File file) {
         setTitle("ClassySearch - " + file.getName());
         controller.onFileDragged(file);
     }

     private void buildUI() {
         Toolbar toolbar = new Toolbar(this);

         searchField = buildSearchField();
         searchField.setMaximumSize(new Dimension(1400, 30));

         resultTextPane = buildResultTextPane();
         JScrollPane showResultsScrolled = new JScrollPane(resultTextPane);
         showResultsScrolled.addMouseWheelListener(new MouseWheelListener() {
             int currentAnchor = 0;

             @Override
             public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
                 int notches = mouseWheelEvent.getWheelRotation();
                 currentAnchor +=notches;

                 if(currentAnchor < 0) {
                     currentAnchor = 0;
                 }

                 // each notch contributes ~3 lines with say 80 chars each
                 controller.onMouseScrolled(currentAnchor * 240, ResultsHighlighter.HIGHLIGHT_SPAN.LONG);
             }
         });
         previewTextPane = buildPreviewTextPane();
         JScrollPane showFileScrolled = new JScrollPane(previewTextPane);

         JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, showResultsScrolled, showFileScrolled);
         splitPane.setDividerSize(20);
         splitPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
         splitPane.setDividerLocation(400);
         splitPane.setOneTouchExpandable(true);
         splitPane.setContinuousLayout(true);
         // aligns to other size components (box layout)
         splitPane.setAlignmentX(Component.CENTER_ALIGNMENT);

         resultCountLabel = new JLabel("...");
         JPanel statusPanel = new JPanel();
         statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
         resultCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
         statusPanel.add(resultCountLabel);
         statusPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

         getContentPane().add(toolbar);
         getContentPane().add(searchField);
         getContentPane().add(splitPane);
         getContentPane().add(statusPanel);
         getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

         addWindowListener(new WindowAdapter() {
             @Override
             public void windowClosing(WindowEvent windowEvent) {
                 controller.onClose();
                 System.exit(0);
             }
         });

         Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
         Dimension dim = defaultToolkit.getScreenSize();
         setPreferredSize(new Dimension(1200, 900));
         setLocation(dim.width / 6 - this.getSize().width / 4, dim.height / 2 - this.getSize().height / 2);
         setIconImage(new javax.swing.ImageIcon(getClass().getResource("/blue-shark.png")).getImage());

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
                     controller.onUpPressed();
                     return;
                 }

                 if (keyEvent.getKeyCode() == VK_DOWN) {
                     controller.onDownPressed();
                     return;
                 }

                 if (keyEvent.getKeyCode() == VK_ENTER) {
                     controller.onEnterPressed();
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
         result.setDragEnabled(true);
         result.setTransferHandler(new FileDragAndDrop(this));
         result.setEditable(false);

         // copy paste
         result.add(new JMenuItem(new DefaultEditorKit.CopyAction()));
         HexPanel.CopyPopupListener popupListener =
                 new HexPanel.CopyPopupListener(result, copyPopup);
         result.addMouseListener(popupListener);

         result.addMouseListener(new MouseAdapter() {
             String previousLine = "";

             public void mouseClicked(MouseEvent me) {
                 int offset = result.viewToModel2D(me.getPoint());
                 Rectangle rect = null;
                 try {
                     rect = (Rectangle) result.modelToView2D(offset);
                 } catch (BadLocationException e) {
                     e.printStackTrace();
                 }

                 int startRow = result.viewToModel2D(new Point(0, rect.y));
                 int endRow = result.viewToModel2D(new Point(result.getWidth(), rect.y));
                 result.select(startRow, endRow);
                 String line = result.getSelectedText();

                 if(line == null) {
                     return;
                 }

                 if(line.equals(previousLine)) {
                     controller.onMouseDoubleClick(result.getSelectedText());
                 } else {
                     controller.onMouseSingleClick(result.getSelectedText());
                     previousLine = line;
                 }
             }
         });

         result.addKeyListener(new KeyListener() {
             @Override
             public void keyTyped(KeyEvent keyEvent) {

             }

             @Override
             public void keyPressed(KeyEvent keyEvent) {
                 if (keyEvent.getKeyCode() == VK_UP) {
                     controller.onUpPressed();
                     return;
                 }

                 if (keyEvent.getKeyCode() == VK_DOWN) {
                     controller.onDownPressed();
                     return;
                 }

                 if (keyEvent.getKeyCode() == VK_ENTER) {
                     controller.onEnterPressed();
                     return;
                 }
             }

             @Override
             public void keyReleased(KeyEvent keyEvent) {

             }
         });

         return result;
     }

     private JTextPane buildPreviewTextPane() {
         final JTextPane result = new JTextPane();
         result.setFont(textFont);
         result.setBackground(BACKGROUND_COLOR);
         result.setForeground(FOREGROUND_COLOR);
         result.setDragEnabled(true);
         result.setTransferHandler(new FileDragAndDrop(this));
         result.setEditable(false);

         HexPanel.CopyPopupListener popupListener=
                 new HexPanel.CopyPopupListener(result, copyPopup);
         result.addMouseListener(popupListener);

         copyPopup.add(new JMenuItem(new DefaultEditorKit.CopyAction()));
         result.addMouseListener(popupListener);

         return result;
     }

     private File openFile() {
         final JFileChooser fileChooser = new JFileChooser();
         fileChooser.setPreferredSize(new Dimension(700,500));
         fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
         fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

         final int returnVal = fileChooser.showDialog(this, "Search");
         if (returnVal == 0) {
             return fileChooser.getSelectedFile();
         }

         return null;
     }

     public void openFileFromToolbar() {
         controller.onFileOpened(openFile());
     }

     public void updateFolderSearchMode(String searchMode) {
        controller.updateSearchMode(searchMode);
     }

     private static boolean isBinarySupported(String filePath) {
         return PathMatchers.ZIP_MATCHER.matches(new File(filePath).toPath());
     }

     private static boolean isTextSupported(String filePath) {
         return PathMatchers.SOURCE_OR_TEXT_MATCHER.matches(new File(filePath).toPath());
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
             classySearch = new InstaSearch();
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
