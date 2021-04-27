/*
 * Copyright 2020 Google, Inc.
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

package com.borisfarber.instasearch.models.search;

import com.borisfarber.instasearch.models.Pair;
import com.borisfarber.instasearch.models.text.ResultModel;
import com.borisfarber.instasearch.models.formats.BinaryXml;
import com.borisfarber.instasearch.contollers.Mediator;
import com.borisfarber.instasearch.models.text.HexDump;
import org.zeroturnaround.zip.ZipUtil;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ThreadPoolExecutor;

public class APKSearch extends ZipSearch {
    public APKSearch(File zipFile, Mediator mediator) {
        super(zipFile, mediator);
    }

    @Override
    public String getPreview(String resultLine) {
        if (resultLine.isEmpty()) {
            return "";
        }

        long waitingTasksCount = ((ThreadPoolExecutor) (executorService)).getActiveCount();
        if (waitingTasksCount > 1) {
            return "";
        }

        executorService.execute(() -> {
            Pair<String, String> pair = ResultModel.extractFilenameAndLineNumber(resultLine);

            String fileName = pair.t;
            byte[] bytes = ZipUtil.unpackEntry(zipFile, fileName);
            int headerSize = bytes.length;

            String result = "";

            // or any other interesting APK components
            if(fileName.equals("AndroidManifest.xml")) {
                BinaryXml binaryXml = new BinaryXml();

                try {
                    result = binaryXml.decompressXml(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                if (headerSize > 128) {
                    headerSize = 128;
                }

                String header = new String(Arrays.copyOfRange(bytes, 0, headerSize));
                result = header + "\n...\n" +
                        HexDump.hexdump(Arrays.copyOfRange(bytes, 0, headerSize)) +
                        "\n...\n";
            }

            String finalResult = result;
            Runnable runnable = () -> mediator.onUpdatePreview(finalResult);

            SwingUtilities.invokeLater(runnable);
        });

        return "building";
    }

    @Override
    public Comparator<String> getResultsSorter() {
        return (s, t1) -> 1;
    }
}
