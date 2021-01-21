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

import com.borisfarber.instasearch.models.text.BuildVersion;

import javax.swing.*;
import java.awt.*;

public class AboutDialog {
    private static String message;
    private static final String INFO = "\n\nFast incremental search  " +
            "\nby Boris Farber";

    public static void show() {
        ImageIcon aboutIcon = new ImageIcon(AboutDialog.class.getResource("/blue-shark.png"));
        Image image = aboutIcon.getImage();
        Image tempImage = image.getScaledInstance(60, 60,  java.awt.Image.SCALE_SMOOTH);
        aboutIcon = new ImageIcon(tempImage);

        message = "Version: " + BuildVersion.getBuildVersion();
        final JOptionPane pane = new JOptionPane(message + INFO);
        pane.setIcon(aboutIcon);
        final JDialog dialog = pane.createDialog(null, "Insta Search");

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setLocation(dim.width/2  - dialog.getSize().width / 2,
                dim.height / 2 - dialog.getSize().height / 2);
        dialog.setVisible(true);
    }
}
