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
 package com.borisfarber.instasearch.models.search;

 import com.borisfarber.instasearch.contollers.Controller;
 import com.borisfarber.instasearch.models.ResultPresentation;
 import com.borisfarber.instasearch.models.text.HexDump;
 import com.borisfarber.instasearch.contollers.PrivateFolder;
 import com.borisfarber.instasearch.models.Pair;
 import com.github.eugenelesnov.LevenshteinSearch;
 import org.zeroturnaround.zip.ZipUtil;

 import javax.swing.*;
 import java.io.File;
 import java.nio.file.Path;
 import java.util.*;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 import java.util.concurrent.ThreadPoolExecutor;

 public class ZipSearch implements Search {
     protected final Controller controller;
     protected File zipFile;
     private final ArrayList<String> allLines = new ArrayList<>();
     protected final ExecutorService executorService =
             Executors.newFixedThreadPool(4);
     private TreeMap<Integer, String> resultMap;

     public ZipSearch(File zipFile, Controller controller) {
         this.zipFile = zipFile;
         this.controller = controller;
         allLines.clear();
     }

     @Override
     public void crawl(File file) {
         this.zipFile = file;
         allLines.clear();
         ZipUtil.iterate(file, zipEntry -> allLines.add(zipEntry.getName()));
         emptyQuery();
     }

     @Override
     public void search(String query) {
         executorService.execute(() -> {
             if(allLines.isEmpty()) {
                 return;
             }

             Map<String, Integer> matchedSet =
                     LevenshteinSearch.levenshteinSearch(90, query, allLines, String::toString);
             resultMap = new TreeMap<>();
             matchedSet.forEach((k, v) -> resultMap.put(v,k));

             Runnable runnable = controller::onSearchFinish;
             SwingUtilities.invokeLater(runnable);
         });
     }

     @Override
     public LinkedList<Pair<String, Integer>> getFileNameAndPosition(String line) {
         // may be to add mapping to simple file names, not sure
         LinkedList<Pair<String, Integer>> result = new LinkedList<>();

         Pair<String, Integer> pair = new Pair<>(line, Search.NOT_IN_FILE);
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
             String nLine  = ResultPresentation.extractPreviewLine(resultLine);
             byte[] bytes = ZipUtil.unpackEntry(zipFile, nLine);
             int headerSize = bytes.length;

             if(headerSize > 128) {
                 headerSize = 128;
             }

             String header = new String(Arrays.copyOfRange(bytes, 0, headerSize));
             String result = header +  "\n...\n" +
                     HexDump.hexdump(Arrays.copyOfRange(bytes, 0, headerSize)) +
                     "\n...\n";

             Runnable runnable = () -> controller.onUpdatePreview(result);

             SwingUtilities.invokeLater(runnable);
         });

         return "building";
     }

     @Override
     public List<String> getResults() {
         ArrayList<String> result = new ArrayList<>(resultMap.size());
         resultMap.forEach((k, v) -> result.add(v));

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
     public void close() {
         executorService.shutdown();
     }

     @Override
     public void emptyQuery() {
         Runnable runnable = () -> {
             StringBuilder builder = new StringBuilder();
             for (String fileName : allLines) {
                 builder.append(fileName).append("\n");
             }

             controller.onCrawlFinish(allLines);
         };
         SwingUtilities.invokeLater(runnable);
     }

     @Override
     public Comparator<String> getResultsSorter() {
         return (s, t1) -> 1;
     }

     public static void main(String[] args) {
         ZipSearch as = new ZipSearch(new File("/home/bfarber/Desktop/shark_v01.apk"), null);
         as.crawl(new File("/home/bfarber/Desktop/shark_v01.apk"));
     }
 }
