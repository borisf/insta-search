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

import javax.swing.event.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import javax.swing.text.*;
import java.util.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.*;

import static com.borisfarber.ui.InstaSearch.BACKGROUND_COLOR;
import static com.borisfarber.ui.InstaSearch.FOREGROUND_COLOR;

public final class HexPanel extends JPanel implements CaretListener {
    private static final int DEFAULT_BYTES_PER_LINE = 16;
    private static final String HEX_FORMAT = "%02X ";

    private JTextComponent offsetView;
    private JTextComponent hexView;
    private JTextComponent asciiView;
    private JLabel statusLabel;
    private Color highlightColor;
    private DefaultHighlighter.DefaultHighlightPainter highlighterPainter;
    private int hexLastSelectionStart;
    private int hexLastSelectionEnd;
    private int asciiLastSelectionStart;
    private int asciiLastSelectionEnd;
    private int bytesPerLine;

    private JPopupMenu copyPopupHex = new JPopupMenu();
    private CopyPopupHexListener popupListenerHex = new CopyPopupHexListener();

    private JPopupMenu copyPopupAscii = new JPopupMenu();
    private CopyPopupAsciiListener popupListenerAscii = new CopyPopupAsciiListener();

    public HexPanel(final File file) {
        this(ByteBuffer.allocate(0), DEFAULT_BYTES_PER_LINE);
        fillFromFile(file);
    }

    public HexPanel(final ByteBuffer bytes) {
        this(bytes, DEFAULT_BYTES_PER_LINE);
    }

    public HexPanel(final ByteBuffer bytes, final int bytesPerLine) {
        super(new BorderLayout());
        this.bytesPerLine = bytesPerLine;
        final Font font = new Font("JetBrains Mono", 0, 23);
        offsetView = new JTextArea();
        hexView = new JTextArea();
        asciiView = new JTextArea();
        final JPanel statusView = new JPanel();
        statusView.setBackground(BACKGROUND_COLOR);
        statusView.setForeground(FOREGROUND_COLOR);

        offsetView.setBackground(BACKGROUND_COLOR);
        hexView.setBackground(BACKGROUND_COLOR);
        asciiView.setBackground(BACKGROUND_COLOR);
        offsetView.setForeground(FOREGROUND_COLOR);
        hexView.setForeground(FOREGROUND_COLOR);
        asciiView.setForeground(FOREGROUND_COLOR);
        statusView.setBorder(new BevelBorder(1));
        add(statusView, "South");
        statusView.setPreferredSize(new Dimension(this.getWidth(), 18));
        statusView.setLayout(new BoxLayout(statusView, 0));
        (this.statusLabel = new JLabel("")).setHorizontalAlignment(2);
        statusView.add(this.statusLabel);
        final JSplitPane splitPane = new JSplitPane(1, this.hexView, this.asciiView);
        splitPane.setResizeWeight(0.5);
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);
        final JPanel panes = new JPanel(new BorderLayout());
        panes.add(this.offsetView, "West");
        panes.add(splitPane, "Center");
        final JScrollPane scroller = new JScrollPane(panes);
        add(scroller, "Center");
        offsetView.setFont(font);
        hexView.setFont(font);
        asciiView.setFont(font);
        fillFromByteBuffer(bytes);
        hexView.addCaretListener(this);
        asciiView.addCaretListener(this);

        // copy paste hex
        hexView.add(new JMenuItem(new DefaultEditorKit.CopyAction()));
        copyPopupHex.add(new JMenuItem(new DefaultEditorKit.CopyAction()));
        hexView.addMouseListener(popupListenerHex);

        // copy paste ascii
        asciiView.add(new JMenuItem(new DefaultEditorKit.CopyAction()));
        copyPopupAscii.add(new JMenuItem(new DefaultEditorKit.CopyAction()));
        asciiView.addMouseListener(popupListenerAscii);

        // selection with proper highlighting
        asciiView.setSelectedTextColor(FOREGROUND_COLOR);
        hexView.setSelectedTextColor(FOREGROUND_COLOR);

        asciiView.setSelectionColor(Color.BLACK);
        hexView.setSelectionColor(Color.BLACK);

        this.highlightColor = Color.BLACK;
        this.highlighterPainter = new DefaultHighlighter.DefaultHighlightPainter(this.highlightColor);
    }

    @Override
    public void caretUpdate(final CaretEvent e) {
        if (e.getMark() == e.getDot()) {
            this.clearHighlight();
        }
        if (e.getSource() == asciiView) {
            int startByte = e.getMark();
            int endByte = e.getDot();
            if (startByte > endByte) {
                final int t = endByte;
                endByte = startByte;
                startByte = t;
            }
            final int startRows = (startByte - startByte % bytesPerLine) / bytesPerLine;
            final int endRows = (endByte - endByte % bytesPerLine) / bytesPerLine;
            startByte -= startRows;
            endByte -= endRows;
            if (asciiLastSelectionStart == startByte && asciiLastSelectionEnd == endByte) {
                return;
            }

            this.setSelection(asciiLastSelectionStart = startByte, asciiLastSelectionEnd = endByte);
        }
        else if (e.getSource() == hexView) {
            int startByte = e.getMark();
            int endByte = e.getDot();
            if (startByte > endByte) {
                final int t = endByte;
                endByte = startByte;
                startByte = t;
            }
            final int startRows = (startByte - startByte % bytesPerLine) / (3 * bytesPerLine);
            final int endRows = (endByte - endByte % bytesPerLine) / (3 * bytesPerLine);
            startByte -= startRows;
            startByte /= 3;
            endByte -= endRows;
            endByte /= 3;
            if (hexLastSelectionStart == startByte && hexLastSelectionEnd == endByte) {
                return;
            }
            setSelection(hexLastSelectionStart = startByte, hexLastSelectionEnd = endByte);
        }
        else {
            System.out.println((Object)"from unknown");
        }
    }

    public final void fillFromFile(final File classFile) {
        if (!classFile.exists()) {
            return;
        }
        try {
            final RandomAccessFile aFile = new RandomAccessFile(classFile, "r");
            final FileChannel inChannel = aFile.getChannel();
            final long fileSize = inChannel.size();
            final ByteBuffer buffer = ByteBuffer.allocate((int)fileSize);
            inChannel.read(buffer);
            buffer.flip();
            inChannel.close();
            aFile.close();
            final ByteBuffer byteBuffer = buffer;

            fillFromByteBuffer(byteBuffer);
            asciiView.setCaretPosition(0);
            hexView.setCaretPosition(0);
            offsetView.setCaretPosition(0);
        }
        catch (Exception ex) {}
    }

    private final void fillFromByteBuffer(final ByteBuffer bytes) {
        StringBuilder offsetText = new StringBuilder();
        StringBuilder hexText = new StringBuilder();
        StringBuilder asciiText = new StringBuilder();

        bytes.position(0x0);

        // for large files, prevent out of memory errors, due to awt events
        int limit = bytes.limit();
        if(limit >= 2048) {
            limit = 2048;
        }

        for (int i = 0; i < limit;i++) {
            fillByteToTexts(bytes, offsetText, hexText, asciiText, i, bytesPerLine);
        }

        offsetView.setText(offsetText.toString());
        hexView.setText(hexText.toString());
        asciiView.setText(asciiText.toString());
    }

    private void fillByteToTexts(ByteBuffer bytes,
                                 StringBuilder offsetText,
                                 StringBuilder hexText,
                                 StringBuilder asciiText,
                                 int i,
                                 int bytesPerLine) {


        if (i % bytesPerLine == 0) {
            final String s = "0x%x  \n";
            final Object[] array = { i };
            final String format = s;
            final Object[] original = array;
            final String format2 = String.format(format, Arrays.copyOf(original, original.length));
            offsetText.append(format2);
        }
        final byte b = bytes.get();
        hexText.append(this.byte2hex(b));
        if (b >= 32 && b < 126) {
            asciiText.append((char)b);
        }
        else {
            asciiText.append('.');
        }
        if (i % bytesPerLine == bytesPerLine - 1) {
            hexText.append("\n");
            asciiText.append("\n");
        }
    }

    private final String byte2hex(final byte b) {
        String result = String.format(HEX_FORMAT, b);
        return result;
    }

    private final void clearHighlight() {
        asciiView.getHighlighter().removeAllHighlights();
        hexView.getHighlighter().removeAllHighlights();
    }

    private final void setHighlight(final int startByte, final int endByte) {
        final int startRows = (startByte - startByte % bytesPerLine) / bytesPerLine;
        final int endRows = (endByte - endByte % bytesPerLine) / bytesPerLine;
        clearHighlight();
        try {
            asciiView.getHighlighter().addHighlight(startByte + startRows, endByte + endRows, highlighterPainter);
            hexView.getHighlighter().addHighlight(startByte * 3 + startRows, endByte * 3 + endRows, highlighterPainter);
        }
        catch (BadLocationException e1) {
            System.out.println((Object)"bad location");
        }
    }

    private final void setSelection(final int startByte, final int endByte) {
        this.setHighlight(startByte, endByte);
        if (startByte != endByte) {
            final String statusTemplate = "Selection: %1$d to %2$d (len: %3$d) [0x%1$x to 0x%2$x (len: 0x%3$x)]";
            final JLabel statusLabel = this.statusLabel;
            final Object[] array = { startByte, endByte, endByte - startByte };
            final JLabel label = statusLabel;
            final String format = statusTemplate;
            final Object[] original = array;
            final String format2 = String.format(format, Arrays.copyOf(original, original.length));
            label.setText(format2);
        }
        else {
            final String statusTemplate = "Position: %1$d [0x%1$x]";
            final JLabel statusLabel2 = this.statusLabel;
            final Object[] array2 = { startByte };
            final JLabel label2 = statusLabel2;
            final String format3 = statusTemplate;
            final Object[] original2 = array2;
            final String format4 = String.format(format3, Arrays.copyOf(original2, original2.length));
            label2.setText(format4);
        }
    }

    class CopyPopupHexListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                copyPopupHex.show(e.getComponent(),
                        e.getX(), e.getY());

                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(hexView.getSelectedText()),
                        null);
            }
        }
    }

    class CopyPopupAsciiListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                copyPopupAscii.show(e.getComponent(),
                        e.getX(), e.getY());

                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(asciiView.getSelectedText()),
                        null);
            }
        }
    }

    public static void createJFrameWithHexPanel(File file) {
        HexPanel panel = new HexPanel(file);
        panel.setPreferredSize(new Dimension(1200, 900));

        JFrame jframe = new JFrame(file.getName());
        jframe.setContentPane(panel);

        jframe.setVisible(true);

        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        Dimension dim = defaultToolkit.getScreenSize();
        jframe.setPreferredSize(new Dimension(1200, 900));
        jframe.setLocation(dim.width / 6 - jframe.getSize().width / 4, dim.height / 2 - jframe.getSize().height / 2);
        jframe.pack();
    }

    public static final void main(final String[] args) {
        try {
            // Set System L&F
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            // handle exception
        }

        createJFrameWithHexPanel(new File("/home/bfarber/Development/Test/export.bin"));
    }
}
