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

public final class HexPanel extends JPanel implements CaretListener
{
    public static final Color  BACKGROUND = new Color(46, 48, 50);
    private static final int DEFAULT_BYTES_PER_LINE = 16;
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
    private  int bytesPerLine;

    public HexPanel(final ByteBuffer bytes, final int bytesPerLine) {
        super(new BorderLayout());
        this.bytesPerLine = bytesPerLine;
        final Font font = new Font("JetBrains Mono", 0, 23);
        this.offsetView = new JTextArea();
        this.hexView = new JTextArea();
        this.asciiView = new JTextArea();
        final JPanel statusView = new JPanel();
        this.offsetView.setBackground(BACKGROUND_COLOR);
        this.hexView.setBackground(BACKGROUND_COLOR);
        this.asciiView.setBackground(BACKGROUND_COLOR);
        this.offsetView.setForeground(FOREGROUND_COLOR);
        this.hexView.setForeground(FOREGROUND_COLOR);
        this.asciiView.setForeground(FOREGROUND_COLOR);
        statusView.setBorder(new BevelBorder(1));
        this.add(statusView, "South");
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
        this.add(scroller, "Center");
        this.offsetView.setFont(font);
        this.hexView.setFont(font);
        this.asciiView.setFont(font);
        this.fillFromByteBuffer(bytes);
        this.hexView.addCaretListener(this);
        this.asciiView.addCaretListener(this);
        this.asciiView.setSelectedTextColor(this.asciiView.getForeground());
        this.hexView.setSelectedTextColor(this.asciiView.getForeground());
        final Color selectionColor = this.hexView.getSelectionColor();
        this.highlightColor = selectionColor;
        this.highlighterPainter = new DefaultHighlighter.DefaultHighlightPainter(this.highlightColor);
    }

    public HexPanel(final ByteBuffer bytes) {
        this(bytes, DEFAULT_BYTES_PER_LINE);
    }

    public HexPanel(final File file) {
        this(ByteBuffer.allocate(0), DEFAULT_BYTES_PER_LINE);
        this.fillFromFile(file);
    }

    @Override
    public void caretUpdate(final CaretEvent e) {
        if (e.getMark() == e.getDot()) {
            this.clearHighlight();
        }
        if (e.getSource() == this.asciiView) {
            int startByte = e.getMark();
            int endByte = e.getDot();
            if (startByte > endByte) {
                final int t = endByte;
                endByte = startByte;
                startByte = t;
            }
            final int startRows = (startByte - startByte % this.bytesPerLine) / this.bytesPerLine;
            final int endRows = (endByte - endByte % this.bytesPerLine) / this.bytesPerLine;
            startByte -= startRows;
            endByte -= endRows;
            if (this.asciiLastSelectionStart == startByte && this.asciiLastSelectionEnd == endByte) {
                return;
            }
            this.setSelection(this.asciiLastSelectionStart = startByte, this.asciiLastSelectionEnd = endByte);
        }
        else if (e.getSource() == this.hexView) {
            int startByte = e.getMark();
            int endByte = e.getDot();
            if (startByte > endByte) {
                final int t = endByte;
                endByte = startByte;
                startByte = t;
            }
            final int startRows = (startByte - startByte % this.bytesPerLine) / (3 * this.bytesPerLine);
            final int endRows = (endByte - endByte % this.bytesPerLine) / (3 * this.bytesPerLine);
            startByte -= startRows;
            startByte /= 3;
            endByte -= endRows;
            endByte /= 3;
            if (this.hexLastSelectionStart == startByte && this.hexLastSelectionEnd == endByte) {
                return;
            }
            this.setSelection(this.hexLastSelectionStart = startByte, this.hexLastSelectionEnd = endByte);
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
            this.fillFromByteBuffer(byteBuffer);
            this.asciiView.setCaretPosition(0);
            this.hexView.setCaretPosition(0);
            this.offsetView.setCaretPosition(0);
        }
        catch (Exception ex) {}
    }

    private final void fillFromByteBuffer(final ByteBuffer bytes) {
        StringBuilder offsetText = new StringBuilder();
        StringBuilder hexText = new StringBuilder();
        StringBuilder asciiText = new StringBuilder();

        bytes.position(0x0);
        for (int i = 0; i < bytes.limit();i++) {
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

        final String s = "%02X ";
        final Object[] array = { b };
        final String format = s;
        final Object[] original = array;
        final String format2 = String.format(format, Arrays.copyOf(original, original.length));

        return format2;
    }

    private final void clearHighlight() {
        this.asciiView.getHighlighter().removeAllHighlights();
        this.hexView.getHighlighter().removeAllHighlights();
    }

    private final void setHighlight(final int startByte, final int endByte) {
        final int startRows = (startByte - startByte % this.bytesPerLine) / this.bytesPerLine;
        final int endRows = (endByte - endByte % this.bytesPerLine) / this.bytesPerLine;
        this.clearHighlight();
        try {
            this.asciiView.getHighlighter().addHighlight(startByte + startRows, endByte + endRows, this.highlighterPainter);
            this.hexView.getHighlighter().addHighlight(startByte * 3 + startRows, endByte * 3 + endRows, this.highlighterPainter);
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
            label2.setFont(new Font("JetBrains Mono", 0, 16));
            label2.setText(format4);
        }
    }


    public static void openPanel(File file) {
        //HexPanel panel = new HexPanel(ByteBuffer.wrap("test".getBytes()));
        HexPanel panel = new HexPanel(file);
        panel.setPreferredSize(new Dimension(1200, 900));

        JFrame jframe = new JFrame();
        jframe.setContentPane(panel);

        jframe.setVisible(true);

        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        Dimension dim = defaultToolkit.getScreenSize();
        jframe.setPreferredSize(new Dimension(1200, 900));
        jframe.setLocation(dim.width / 6 - jframe.getSize().width / 4, dim.height / 2 - jframe.getSize().height / 2);
        jframe.pack();
    }

    public static final void main(final String[] args) {
        openPanel(new File("/home/bfarber/Desktop/Archive_1_2020_08_07.zip"));
    }
}
