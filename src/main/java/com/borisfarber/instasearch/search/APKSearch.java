package com.borisfarber.instasearch.search;

import com.borisfarber.instasearch.binary.XmlDecompressor;
import com.borisfarber.instasearch.controllers.Controller;
import com.borisfarber.instasearch.controllers.Hexdump;
import org.zeroturnaround.zip.ZipUtil;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ThreadPoolExecutor;

public class APKSearch extends ZipSearch {
    public APKSearch(File zipFile, Controller controller) {
        super(zipFile, controller);
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
            String[] parts = resultLine.split(":");
            String fileName = parts[0];
            String line = parts[2];

            String nLine = line;

            // remove the new line in the end
            if (nLine.endsWith("\n")) {
                nLine = nLine.substring(0, nLine.length() - 1);
            }

            byte[] bytes = ZipUtil.unpackEntry(zipFile, nLine);
            int headerSize = bytes.length;

            String result = "";

            // or any other interesting APK components
            if(fileName.equals("AndroidManifest.xml")) {
                XmlDecompressor xmlDecompressor = new XmlDecompressor();

                try {
                    result = xmlDecompressor.decompressXml(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                if (headerSize > 128) {
                    headerSize = 128;
                }

                String header = new String(Arrays.copyOfRange(bytes, 0, headerSize));
                result = header + "\n...\n" +
                        Hexdump.hexdump(Arrays.copyOfRange(bytes, 0, headerSize)) +
                        "\n...\n";
            }

            String finalResult = result;
            Runnable runnable = () -> {
                controller.previewTextPane.setText(finalResult);
                controller.highlightPreview();
            };

            SwingUtilities.invokeLater(runnable);
        });

        return "building";
    }
}
