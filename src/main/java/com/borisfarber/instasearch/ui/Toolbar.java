/*
 * Copyright 2015 Google, Inc.
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

import com.borisfarber.instasearch.models.text.BuildVersion;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

import static com.borisfarber.instasearch.models.search.Search.*;

public class Toolbar extends JToolBar {
    private final InstaSearch frame;
    private final JComboBox searchModeCombo;

    public Toolbar(InstaSearch frame) {
        super();
        this.frame = frame;
        JButton openButton = buildOpenButton();
        JButton ignoreButton = buildIgnoreButton();
        JButton aboutButton = buildAboutButton();
        searchModeCombo = buildSearchModeCombo();

        add(openButton);
        add(searchModeCombo);
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

    private JComboBox buildSearchModeCombo() {
        String[] modes ={CONTENT_SEARCH,
                FILENAMES_SEARCH};
        JComboBox<String> result = new JComboBox<>(modes);

        result.addActionListener(actionEvent -> {
            JComboBox cb = (JComboBox)actionEvent.getSource();
            String selection = (String)cb.getSelectedItem();
            frame.updateFolderSearchMode(selection);
        });

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

        // action listener
        result.addActionListener(actionEvent -> {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.open(new File("ignore.txt"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return result;
    }

    private JButton buildAboutButton() {
        JButton result = new JButton("?");
        result.setContentAreaFilled(false);
        result.setBorderPainted(true);
        result.setFocusPainted(true);

        ImageIcon aboutIcon = new ImageIcon(getClass().getResource("/blue-shark.png"));
        Image image = aboutIcon.getImage();
        Image tempImage = image.getScaledInstance(60, 60,  java.awt.Image.SCALE_SMOOTH);
        aboutIcon = new ImageIcon(tempImage);
        ImageIcon finalAboutIcon = aboutIcon; // to capture in the lambda
        result.addActionListener(
                actionEvent -> JOptionPane.showMessageDialog(
                        this,
                        "Version " + BuildVersion.getBuildVersion(),
                        "ClassyShark Insta Search", JOptionPane.PLAIN_MESSAGE,
                        finalAboutIcon));

        return result;
    }
}