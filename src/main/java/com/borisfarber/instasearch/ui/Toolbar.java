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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Toolbar extends JToolBar {

    private final InstaSearch frame;

    private JButton openButton;
    private JButton filesButton;
    private JButton inFileButton;
    private JButton exportButton;
    private JButton recentArchivesBtn;
    private JButton mappingBtn;

    public Toolbar(InstaSearch frame) {
        super();
        this.frame = frame;

        openButton = buildOpenButton();
        filesButton = buildFilesButton();
        inFileButton = buildInFileButton();

        mappingBtn = buildMappingsButton();
        exportButton = buildExportButton();
        recentArchivesBtn = buildRecentArchivesButton();

        add(openButton);
        String searchModes[] ={ "Content", "Filenames" };
        JComboBox cb = new JComboBox (searchModes);
        cb.setBounds (50, 50, 90, 20);
        add (cb);

        setFloatable(false);
        setBorder(BorderFactory.createEmptyBorder());
    }

     /*
         JMenuBar menuBar = new JMenuBar();
         UIManager.put("Menu.font", textFont);
         JMenu menu = new JMenu("File");
         menuBar.add(menu);
         menuBar.add(Box.createHorizontalGlue());
         JMenuItem openFolderItem = new JMenuItem("Open...");
         openFolderItem.setFont(textFont);
         openFolderItem.addActionListener(actionEvent -> {
             try {
                 File newFile = openFile();
                 controller.onFileOpened(newFile);
             } catch (Exception e) {
                 e.printStackTrace();
             }
         });
         menu.add(openFolderItem);

         final JMenuItem aboutItem = new JMenuItem("About");
         aboutItem.setFont(textFont);


         menu.add(aboutItem);
         setJMenuBar(menuBar);
          */

    private JButton buildOpenButton() {
        ImageIcon aboutIcon = new ImageIcon(getClass().getResource("/open_folder.png"));
        Image image = aboutIcon.getImage();
        Image tempImage = image.getScaledInstance(28, 28,  java.awt.Image.SCALE_SMOOTH);
        aboutIcon = new ImageIcon(tempImage);

        JButton result = new JButton(aboutIcon);
        result.setToolTipText("Open ...");
        //TODO not sure the line below
        result.setContentAreaFilled(false);

        result.addActionListener(e -> frame.openFileGrep());

        result.setBorderPainted(true);
        result.setFocusPainted(true);

        return result;
    }

    private JButton buildInFileButton() {
        JButton result = new JButton("In File");
        result.setToolTipText("In file");

        result.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //toolbarController.onGoBackPressed();
            }
        });

        result.setBorderPainted(true);
        result.setFocusPainted(true);
        result.setEnabled(true);

        return result;
    }

    private JButton buildFilesButton() {
        JButton result = new JButton( "Files");
        result.setToolTipText("Search for files");

        result.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //toolbarController.onViewTopClassPressed();
            }
        });

        result.setBorderPainted(true);
        result.setBackground(InstaSearch.RESULT_HIGHLIGHT_COLOR);
        result.setFocusPainted(true);
        result.setEnabled(true);

        return result;
    }

    private JButton buildExportButton() {
        JButton result = new JButton(/*theme.getExportIcon()*/);

        result.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //toolbarController.onExportButtonPressed();
            }
        });

        result.setToolTipText("Export");
        result.setBorderPainted(false);
        result.setEnabled(false);

        return result;
    }

    private JButton buildMappingsButton() {

        JButton result = new JButton(/*theme.getMappingIcon()*/);

        result.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //toolbarController.onMappingsButtonPressed();
            }
        });

        result.setToolTipText("Import Proguard mapping file");
        result.setBorderPainted(false);
        result.setEnabled(true);

        return result;
    }

    private JButton buildRecentArchivesButton() {
        JButton result = new JButton();
        //result.setPanel(toolbarController);
        return result;
    }

    private JButton buildSettingsButton() {
        JButton button = new JButton(/*theme.getSettingsIcon()*/);

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //toolbarController.onSettingsButtonPressed();
            }
        });
        button.setToolTipText("Settings");
        button.setBorderPainted(false);

        return button;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("ClassyShark");
        Toolbar toolbar = new Toolbar(null);

        frame.getContentPane().add(toolbar);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}