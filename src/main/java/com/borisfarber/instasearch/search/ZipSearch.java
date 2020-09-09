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
 package com.borisfarber.instasearch.search;

 import com.borisfarber.instasearch.controllers.Controller;
 import com.borisfarber.instasearch.ui.Hexdump;
 import com.borisfarber.instasearch.controllers.PrivateFolder;
 import com.borisfarber.instasearch.controllers.Pair;
 import me.xdrop.fuzzywuzzy.model.ExtractedResult;
 import org.zeroturnaround.zip.ZipUtil;

 import javax.swing.*;
 import java.io.File;
 import java.nio.file.Path;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 import java.util.concurrent.ThreadPoolExecutor;

 public class ZipSearch implements Search {
     protected final Controller controller;
     protected File zipFile;
     private final ArrayList<String> allLines = new ArrayList<>();
     protected final ExecutorService executorService =
             Executors.newFixedThreadPool(4);
     private List<ExtractedResult> resultSet = new ArrayList<>();

     public ZipSearch(File zipFile, Controller controller) {
         this.zipFile = zipFile;
         this.controller = controller;
         allLines.clear();
     }

     @Override
     public void crawl(File file) {
         this.zipFile = file;
         allLines.clear();
         resultSet.clear();
         ZipUtil.iterate(file, zipEntry -> allLines.add(zipEntry.getName()));
         emptyQuery();
     }

     @Override
     public void search(String query) {
         executorService.execute(() -> {
             if(allLines.isEmpty()) {
                 resultSet = new LinkedList<>();
                 return;
             }

             resultSet = me.xdrop.fuzzywuzzy.FuzzySearch.extractSorted(query, allLines, 50);
             Runnable runnable = controller::onUpdateGUI;
             SwingUtilities.invokeLater(runnable);
         });
     }

     @Override
     public LinkedList<Pair<String, Integer>> getFileNameAndPosition(String line) {
         // may be to add mapping to simple file names, not sure
         LinkedList<Pair<String, Integer>> result = new LinkedList<>();

         Pair<String, Integer> pair = new Pair<>(line, 0);
         result.add(pair);
         return result;
     }

     @Override
     public String getPreview(String resultLine) {
         if (resultLine.isEmpty()) {
             return "";
         }

         long waitingTasksCount = ((ThreadPoolExecutor)(executorService)).getActiveCount();
         if(waitingTasksCount > 1) {
             return "";
         }

         executorService.execute(() -> {
             String[] parts  = resultLine.split(":");
             String fileName = parts[0];
             String line = parts[2];

             String nLine = line;

             // remove the new line in the end
             if(nLine.endsWith("\n")) {
                 nLine = nLine.substring(0, nLine.length() - 1);
             }

             byte[] bytes = ZipUtil.unpackEntry(zipFile, nLine);
             int headerSize = bytes.length;

             if(headerSize > 128) {
                 headerSize = 128;
             }

             String header = new String(Arrays.copyOfRange(bytes, 0, headerSize));
             String result = header +  "\n...\n" +
                     Hexdump.hexdump(Arrays.copyOfRange(bytes, 0, headerSize)) +
                     "\n...\n";

             Runnable runnable = () -> {
                 controller.previewTextPane.setText(result);
                 controller.highlightPreview();
             };

             SwingUtilities.invokeLater(runnable);
         });

         return "building";
     }

     @Override
     public List<String> getResults() {
         ArrayList<String> result = new ArrayList<>(resultSet.size());

         for (ExtractedResult er : resultSet) {
             result.add(er.getString());
         }

         return result;
     }

     @Override
     public String getResultSetCount() {
         return Integer.toString(this.getResults().size());
     }

     @Override
     public Path getPathPerFileName(String fileName) {
         String simpleName = new File(fileName).getName();
         File tempFile = PrivateFolder.INSTANCE.getTempFile(simpleName);
         ZipUtil.unpackEntry(zipFile, fileName, tempFile);

         return Path.of(tempFile.toURI());
     }

     @Override
     public void testCrawl(ArrayList<String> testLoad) {

     }

     @Override
     public void close() {
         executorService.shutdown();
         PrivateFolder.INSTANCE.shutdown();
     }

     @Override
     public void emptyQuery() {
         Runnable runnable = () -> {
             StringBuilder builder = new StringBuilder();
             for (String fileName : allLines) {
                 builder.append(fileName).append("\n");
             }

             controller.resultTextPane.setText(builder.toString());
         };
         SwingUtilities.invokeLater(runnable);
     }

     public static void main(String[] args) {
         ZipSearch as = new ZipSearch(new File("/home/bfarber/Desktop/shark_v01.apk"), null);
         as.crawl(new File("/home/bfarber/Desktop/shark_v01.apk"));
     }
 }
