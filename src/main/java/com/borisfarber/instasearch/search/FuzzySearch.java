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
 import com.borisfarber.instasearch.data.Pair;
 import me.xdrop.fuzzywuzzy.model.ExtractedResult;

 import javax.swing.*;
 import java.io.File;
 import java.io.IOException;
 import java.nio.file.*;
 import java.nio.file.attribute.BasicFileAttributes;
 import java.util.*;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;

 import static java.nio.file.FileVisitResult.CONTINUE;
 import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

 public class FuzzySearch implements Search {
     private final Controller controller;
     // key design idea, no such thing file, it is recreated by line numbers
     private final ArrayList<String> allLines;
     private List<ExtractedResult> resultSet;
     private final TreeMap<String, Path> filenamesToPaths;
     private final ArrayList<Pair<Integer, String>> numLinesToFilenames;
     private final HashMap<String, List<Integer>> occurrences;

     private final ExecutorService executorService =
             Executors.newSingleThreadExecutor();

     public FuzzySearch(Controller controller) {
         this.controller = controller;
         allLines = new ArrayList<>();
         resultSet = new ArrayList<>();
         filenamesToPaths = new TreeMap<>();
         numLinesToFilenames = new ArrayList<>();
         occurrences = new HashMap<>();
     }

     @Override
     public void crawl(File file) {
         allLines.clear();
         numLinesToFilenames.clear();
         filenamesToPaths.clear();
         occurrences.clear();

         if (file == null || !file.exists()) {
             return;
         }

         if(file.isDirectory() && file.list().length ==0) {
             return;
         }

         Path pathString = file.toPath();

         PathMatcher matcher = Controller.SOURCE_OR_TEXT_PATH_MATCHER;

         try {
             Files.walkFileTree(pathString, new SimpleFileVisitor<>() {

                 @Override
                 public FileVisitResult preVisitDirectory(Path dir,
                                                          BasicFileAttributes attrs) {
                     if (dir.getFileName().toString().startsWith(".")) {
                         return SKIP_SUBTREE;
                     }
                     return CONTINUE;
                 }

                 @Override
                 public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                         throws IOException {
                     // one thread, check exception by printing path
                     // System.out.println("Thread:" + Thread.currentThread().getName());
                     if (matcher.matches(path)) {
                         try {
                             List<String> allFileLines = Files.readAllLines(path);
                             allLines.addAll(allFileLines);
                             Pair<Integer, String> pair = new Pair<>(allFileLines.size(),
                                     path.getFileName().toString());
                             numLinesToFilenames.add(pair);
                             filenamesToPaths.put(path.getFileName().toString(), path);
                         } catch (java.nio.charset.MalformedInputException e) {
                             System.out.println("Bad file format " + path.getFileName().toString());
                         }
                     }
                     return CONTINUE;
                 }
             });
         } catch (IOException e) {
             e.printStackTrace();
         }

         processDuplicates(allLines);

         emptyQuery();
     }

     private void processDuplicates(List<String> allLines) {
         for(int index=0; index < allLines.size(); index++){
             if(occurrences.containsKey(allLines.get(index))) {
                 occurrences.get(allLines.get(index)).add(index);
             } else {
                 LinkedList<Integer> list = new LinkedList<>();
                 list.add(index);
                 occurrences.put(allLines.get(index), list);
             }
         }
     }

     @Override
     public void search(String query) {
         executorService.execute(() -> {
             if(allLines.isEmpty()) {
                 resultSet = new LinkedList<>();
                 return;
             }

             resultSet = me.xdrop.fuzzywuzzy.FuzzySearch.extractTop(query, allLines, 50);
             Runnable runnable = controller::onUpdateGUI;
             SwingUtilities.invokeLater(runnable);
         });
     }

     @Override
     public LinkedList <Pair<String,Integer>> getFileNameAndPosition(String line) {
         LinkedList <Pair<String,Integer>> result = new LinkedList<>();

         for (Integer occ : occurrences.get(line)) {
             Pair<String, Integer> pair = getFileNameAndPositionFromLineIndex(occ);
             result.add(pair);
         }

         return result;
     }

     private Pair<String, Integer> getFileNameAndPositionFromLineIndex(int index) {
         int base = 0;
         for (Pair<Integer, String> pair : numLinesToFilenames) {
             if ((index >= base) && index < (base + pair.t - 1)) {
                 return new Pair<>(pair.u, (index - base));
             }
             base += pair.t;
         }
         return new Pair<>("", 0);
     }

     /**
      * 7 lines
      * result
      * 7 more lines
      *
      * @param resultLine the result string
      * @return preview
      */
     @Override
     public String getPreview(String resultLine) {
         if (resultLine.isEmpty()) {
             return "";
         }

         String[] parts  = resultLine.split(":");
         String fileName = parts[0];
         String line = parts[1];

         int bline = 0;

         for(Pair<Integer,String> fData : numLinesToFilenames) {
             if(fData.u.equals(fileName)) {
                 break;
             }
             bline += fData.t;
         }

         StringBuilder builder = new StringBuilder();
         int allLinesIndex = Integer.parseInt(line) + bline;

         int lower = allLinesIndex - 7;
         int upper = allLinesIndex + 7;

         if (lower < 0) {
             lower = 0;
         }

         if (upper >= allLines.size()) {
             upper = allLines.size() - 1;
         }

         for (int i = lower; i < upper; i++) {
             builder.append(allLines.get(i)).append("\n");
         }

         return builder.toString();
     }

     @Override
     public Path getPathPerFileName(String fileName) {
         return filenamesToPaths.get(fileName);
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
     public void testCrawl(ArrayList<String> testLoad) {
         allLines.clear();
         allLines.addAll(testLoad);
         numLinesToFilenames.clear();
     }

     @Override
     public void close() {
         Runtime.getRuntime().addShutdownHook(new Thread(() -> {
             try {
                 if (executorService.isShutdown()) {
                     return;
                 }
                 executorService.shutdownNow();
             } catch (Throwable e) {
             }
         }));
     }

     @Override
     public void emptyQuery() {
         Runnable runnable = () -> {
             StringBuilder builder = new StringBuilder();
             for (String fileName : filenamesToPaths.keySet()) {
                 builder.append(fileName).append("\n");
             }

             controller.resultTextPane.setText(builder.toString());
         };
         SwingUtilities.invokeLater(runnable);
     }

     public String toString() {
         for (String res : this.getResults()) {
             System.out.println(res);
         }
         return "";
     }

     public static ArrayList<String> testLoad() {
         ArrayList<String> allLines = new ArrayList<>();

         allLines.add("Something ....");
         allLines.add("Something else....");
         allLines.add("Incremental Search ....");
         allLines.add("Incremental Search with Preview");
         allLines.add("Something .... other than else");
         allLines.add("Something .... clear");

         return allLines;
     }

     public static void main(String[] args) {
         /*
         System.out.println("Search");
         FuzzySearch search = new FuzzySearch();
         search.testCrawl(testLoad());
         search.search("set");
         System.out.println(search.getResults());
         System.out.println(search.getPreview(0));
         System.out.println(search.getResultSetCount());*/
     }
 }