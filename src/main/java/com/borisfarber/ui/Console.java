package com.borisfarber;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Console extends JPanel implements KeyListener, CaretListener {

    final static Color BACKGROUND_COLOR = new Color(0x07, 0x36, 0x42);
    final static Color FOREGROUND_COLOR = new Color(0x93, 0xa1, 0xa1);

    private static final String PROMPT = ">>";
    private static final int FRAME_WIDTH = 900;
    private static final int FRAME_HEIGHT = 600;
    private static final Dimension FRAME_DIMENSION = new Dimension(FRAME_WIDTH, FRAME_HEIGHT);
    private static final Dimension INTERNAL_DIMENSION = new Dimension(FRAME_WIDTH-10, FRAME_HEIGHT-10);

    private JScrollPane scrollPane;
    private JTextArea consoleTextPane;

    private int startIndex;

    public Console() {
        super();

        // Create a text area
        consoleTextPane = new JTextArea();
        consoleTextPane.setText(PROMPT);
        consoleTextPane.setBorder(null);
        // Wraps the text if it goes longer than a line, but NOT on word boundary
        // like a normal console
        consoleTextPane.setLineWrap(true);
        consoleTextPane.setWrapStyleWord(false);

        // Set the initial caret position
        startIndex = consoleTextPane.getText().length();
        consoleTextPane.setCaretPosition(startIndex);

        // Add the caret and key listeners
        consoleTextPane.addCaretListener(this);
        consoleTextPane.addKeyListener(this);

        // Colors & fonts
        consoleTextPane.setBackground(BACKGROUND_COLOR);
        consoleTextPane.setForeground(FOREGROUND_COLOR);

        Font font = new Font("JetBrains Mono", Font.PLAIN, 25);
        consoleTextPane.setFont(font);

        // Scrollbar, always show the vertical one
        scrollPane = new JScrollPane(consoleTextPane);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(null);

        JPanel panelCenter = new JPanel(new BorderLayout());
        panelCenter.setPreferredSize(INTERNAL_DIMENSION);
        panelCenter.add(scrollPane, BorderLayout.CENTER);

        add(panelCenter, BorderLayout.CENTER);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // All processing in keyPressed
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // All processing in keyPressed
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // TODO read file from resources
        // https://stackoverflow.com/questions/16953897/how-to-read-a-text-file-inside-a-jar#:~:text=You%20cannot%20use%20File%20inside,(%22%2Fresources%2Fmytextfile.

        switch(e.getKeyCode()) {
            case KeyEvent.VK_ENTER:
                // ENTER key was pressed
                // Get the "Command"
                String command = consoleTextPane.getText().substring(startIndex);

                if (!command.isEmpty()) {
                    // TODO: do something with the command
                    consoleTextPane.append(System.lineSeparator()
                            + "Command Entered: " + command);
                }

                // Update the start index and append a new prompt
                consoleTextPane.append(System.lineSeparator() + PROMPT);
                startIndex = consoleTextPane.getText().length();

                // Consume the ENTER key event so further processing is not
                // performed
                e.consume();
                break;
            case KeyEvent.VK_BACK_SPACE:
                // Make sure this is a valid delete
                if (consoleTextPane.getCaretPosition() <= startIndex) {
                    e.consume();
                    Toolkit.getDefaultToolkit().beep();
                }

                break;
            // TODO: add key presses here as desired
            default:
                //System.out.println("Unhandled: " + e.getKeyCode());
                break;
        }
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        // Ensure that the caret position can only be a valid location
        if (e.getDot() < startIndex) {
            consoleTextPane.setCaretPosition(startIndex);
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Console");
        frame.setPreferredSize(FRAME_DIMENSION);

        /*
        TODO fix

           val dim = Toolkit.getDefaultToolkit().screenSize
        setLocation(dim.width / 2 - this.size.width / 2,
                dim.height / 2 - this.size.height / 2)
         */

        frame.setLocation(FRAME_DIMENSION.width / 2, FRAME_DIMENSION.height / 2);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(true);
        frame.add(new Console());
        frame.pack();
        frame.setVisible(true);
    }
}