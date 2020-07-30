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
package com.borisfarber.search;

import com.borisfarber.controllers.Controller;
import com.borisfarber.data.Pair;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import org.zeroturnaround.zip.ZipUtil;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApkSearch implements Search {

    // TODO small set thing, show results with 0 line search

    private final Controller controller;
    private File zipFile;
    private final ArrayList<String> allLines = new ArrayList<>();
    private ExecutorService executorService =
            Executors.newSingleThreadExecutor();
    private List<ExtractedResult> resultSet;

    public ApkSearch(File newFile, Controller controller) {
        this.zipFile = newFile;
        this.controller = controller;
        allLines.clear();
    }

    @Override
    public void crawl(File file) {
        this.zipFile = file;
        allLines.clear();

        ZipUtil.iterate(file, zipEntry -> allLines.add(zipEntry.getName()));


        Runnable runnable = () -> {
            StringBuilder builder = new StringBuilder();
            for (String fileName : allLines) {
                builder.append(fileName + "\n");
            }

            controller.resultTextPane.setText(builder.toString());
        };
        SwingUtilities.invokeLater(runnable);
    }

    @Override
    public void search(String query) {
             executorService.execute(() -> {
             if(allLines.isEmpty()) {
                 resultSet = new LinkedList<>();
                 return;
             }

             resultSet = me.xdrop.fuzzywuzzy.FuzzySearch.extractTop(query, allLines, 10);
             Runnable runnable = () -> controller.onUpdateGUI();
             SwingUtilities.invokeLater(runnable);
         });
    }

    @Override
    public LinkedList<Pair<String, Integer>> getFileNameAndPosition(String line) {
        System.out.println("line-" + line);
        //line is a file name
        LinkedList<Pair<String, Integer>> result = new LinkedList<>();

        // TODO think of a file, after parsing the line
        // ZipUtil.unpackEntry(new File("/tmp/demo.zip"), "foo.txt", new File("/tmp/bar.txt"));
        Pair<String, Integer> pair = new Pair("ddd", 0);
        result.add(pair);
        return result;
    }

    @Override
    public String getResults() {
        return null;
    }

    @Override
    public String getPreview(String resultLine) {
        if (resultLine.isEmpty()) {
            return "";
        }

        String[] parts  = resultLine.split(":");
        String fileName = parts[0];
        String line = parts[2];

        // remove the new line in the end
        line = line.substring(0, line.length()-1);
        byte[] bytes = ZipUtil.unpackEntry(zipFile, line);

        // todo probably more than 120
        String result = new String(Arrays.copyOfRange(bytes, 0, 120));
        return result;
    }

    @Override
    public List<String> getResultSet() {
        ArrayList<String> result = new ArrayList<>(resultSet.size());

        for (ExtractedResult er : resultSet) {
            result.add(er.getString());
        }

        return result;
    }

    @Override
    public String getResultSetCount() {
        return Integer.toString(getResultSet().size());
    }

    @Override
    public TreeMap<String, Path> getFilenamesToPathes() {
        // TODO stopped here for executing the file viewer
        //  private final TreeMap<String, Path> filenamesToPathes;
        //  think of a JVM file
        return null;
    }

    @Override
    public void testCrawl(ArrayList<String> testLoad) {

    }

    @Override
    public void close() {
        executorService.shutdown();
    }

    public static void main(String[] args) {
        ApkSearch as = new ApkSearch(new File("/home/bfarber/Desktop/shark_v01.apk"), null);
        as.crawl(new File("/home/bfarber/Desktop/shark_v01.apk"));
    }
}
