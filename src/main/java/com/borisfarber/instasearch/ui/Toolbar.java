/*
 * Copyright 2021 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.borisfarber.instasearch.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

import static com.borisfarber.instasearch.models.search.Search.CONTENT_SEARCH;
import static com.borisfarber.instasearch.models.search.Search.FILENAMES_SEARCH;

public class Toolbar extends JToolBar {
    private final InstaSearch frame;

    public Toolbar(InstaSearch frame) {
        super();
        this.frame = frame;
        JButton openButton = buildOpenButton();
        JButton ignoreButton = buildIgnoreButton();
        JButton aboutButton = buildAboutButton();
        JPanel searchPanel = buildSearchPanel();

        add(openButton);
        add(searchPanel);
        add(Box.createHorizontalGlue());
        add(ignoreButton);
        add(aboutButton);

        setFloatable(false);
        setBorder(BorderFactory.createEmptyBorder());
    }

    private JButton buildOpenButton() {
        ImageIcon aboutIcon = new ImageIcon(getClass().getResource("/open_folder.png"));
        Image image = aboutIcon.getImage();
        Image tempImage = image.getScaledInstance(28, 28,  java.awt.Image.SCALE_SMOOTH);
        aboutIcon = new ImageIcon(tempImage);

        JButton result = new JButton(aboutIcon);
        result.setToolTipText("Open ...");
        result.setContentAreaFilled(false);
        result.setBorderPainted(true);
        result.setFocusPainted(true);

        result.addActionListener(e -> frame.openFileFromToolbar());

        return result;
    }

    private JPanel buildSearchPanel() {
        JRadioButton contentButton = new JRadioButton(CONTENT_SEARCH);
        contentButton.setActionCommand(CONTENT_SEARCH);
        contentButton.setSelected(true);

        JRadioButton filenamesButton = new JRadioButton(FILENAMES_SEARCH);
        filenamesButton.setActionCommand(FILENAMES_SEARCH);

        ButtonGroup group = new ButtonGroup();
        group.add(contentButton);
        group.add(filenamesButton);

        contentButton.addActionListener(
                actionEvent -> frame.updateFolderSearchMode(actionEvent.getActionCommand()));
        filenamesButton.addActionListener(
                actionEvent -> frame.updateFolderSearchMode(actionEvent.getActionCommand()));

        JPanel result = new JPanel(new GridLayout(1, 0));
        result.add(contentButton);
        result.add(filenamesButton);

        return result;
    }

    private JButton buildIgnoreButton() {
        ImageIcon ignoreIcon = new ImageIcon(getClass().getResource("/ignore.png"));
        Image image = ignoreIcon.getImage();
        Image tempImage = image.getScaledInstance(20, 20,  java.awt.Image.SCALE_SMOOTH);
        ignoreIcon = new ImageIcon(tempImage);

        JButton result = new JButton(ignoreIcon);
        result.setToolTipText("Edit global folder ignore list");
        result.setContentAreaFilled(false);
        result.setBorderPainted(true);
        result.setFocusPainted(true);

        result.addActionListener(actionEvent -> {
            Runnable runnable = () -> {
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.open(new File("ignore.txt"));
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Can't open ignore.txt " +
                                    "file from your home folder",
                            "ClassyShark Insta Search",
                            JOptionPane.ERROR_MESSAGE);
                }
            };
            SwingUtilities.invokeLater(runnable);
        });
        return result;
    }

    private JButton buildAboutButton() {
        JButton result = new JButton("?");
        result.setContentAreaFilled(false);
        result.setBorderPainted(true);
        result.setFocusPainted(true);

        result.addActionListener(actionEvent -> AboutDialog.show());

        return result;
    }
}