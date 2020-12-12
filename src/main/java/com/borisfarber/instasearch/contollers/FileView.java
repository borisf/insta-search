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
package com.borisfarber.instasearch.contollers;

import com.borisfarber.instasearch.models.formats.BinaryXml;
import com.borisfarber.instasearch.models.formats.Clazz;
import com.borisfarber.instasearch.models.formats.Dex;
import com.borisfarber.instasearch.models.search.Search;
import com.borisfarber.instasearch.models.Pair;
import com.borisfarber.instasearch.ui.HexPanel;
import dorkbox.notify.Notify;
import dorkbox.notify.Pos;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ThreadPoolExecutor;

public class FileView {

    private FileView(){

    }

    public static void viewFile(Search search,
                                String filename,
                                Integer position,
                                ThreadPoolExecutor previewExecutor,
                                JTextPane previewTextPane,
                                File file) {
        Path viewPath = search.extractSelectedFile(filename);
        if (viewPath == null) {
            // garbage files
            return;
        }

        if (PathMatchers.SOURCE_OR_TEXT_MATCHER.matches(viewPath)) {
            showFileExternally(viewPath, position);
        } else if (PathMatchers.CLASS_MATCHER.matches(viewPath)) {
            previewExecutor.execute(() -> {
                Pair<File, String> result = Clazz.decompile(viewPath);
                showFile(previewTextPane, result);
            });
        } else if (PathMatchers.CLASS_MATCHER.matches(viewPath)) {
            previewExecutor.execute(() -> {
                Pair<File, String> result = Clazz.decompile(viewPath);
                showFile(previewTextPane, result);
            });
        } else if (PathMatchers.DEX_MATCHER.matches(viewPath)) {
            previewExecutor.execute(() -> {
                Pair<File, String> result = Dex.decompile(file.toPath());
                showFile(previewTextPane, result);
            });
        } else if(PathMatchers.ANDROID_BINARY_XML_MATCHER.matches(viewPath)) {
            previewExecutor.execute(() -> {
                Pair<File, String> result = BinaryXml.decompile(viewPath);
                showFile(previewTextPane, result);
            });
        } else {
            HexPanel.createJFrameWithHexPanel(viewPath.toFile());
        }
    }

    private static void showFile(JTextPane previewTextPane, Pair<File, String> result) {
        Runnable runnable = () -> {
            previewTextPane.setText(result.u);
            showFileExternally(result.t.toPath(), 0);
        };
        SwingUtilities.invokeLater(runnable);
    }

    private static void showFileExternally(Path path, int line) {
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.open(new File(path.toString()));

            if(line > 0) {
                Notify.TITLE_TEXT_FONT = "Source Code Pro BOLD 22";
                Notify.MAIN_TEXT_FONT = "Source Code Pro BOLD 22";

                Notify.create()
                        .title("Insta Search")
                        .text("Line " + line)
                        .hideCloseButton()
                        .position(Pos.TOP_RIGHT)
                        .darkStyle()
                        .showInformation();
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
