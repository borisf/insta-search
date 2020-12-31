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

import javax.swing.*;
import java.awt.*;

public class Toolbar extends JToolBar {
    private final InstaSearch frame;
    private JButton openButton;
    private JComboBox searchModeCombo;

    public Toolbar(InstaSearch frame) {
        super();
        this.frame = frame;
        openButton = buildOpenButton();
        searchModeCombo = buildSearchModeCombo();

        add(searchModeCombo);
        add(openButton);
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
        //TODO not sure the line below
        result.setContentAreaFilled(false);

        result.addActionListener(e -> frame.openFileFromToolbar());

        result.setBorderPainted(true);
        result.setFocusPainted(true);

        return result;
    }

    private JComboBox buildSearchModeCombo() {
        String modes[] ={"Content", "Filenames"};
        JComboBox<String> result = new JComboBox<>(modes);

        result.addActionListener(actionEvent -> {
            JComboBox cb = (JComboBox)actionEvent.getSource();
            String selection = (String)cb.getSelectedItem();
            frame.updateFolderSearchMode(selection);
        });

        return result;
    }
}